package com.datavalley.careerguidance.ai;
// javid
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import com.datavalley.careerguidance.entity.AssessmentResult;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.CareerSkill;
import com.datavalley.careerguidance.entity.PersonalityType;
import com.datavalley.careerguidance.entity.TraitCategory;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.entity.UserSkill;

@Component
public class CareerDatasetModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CareerDatasetModel.class);

    private static final String COURSE_COLUMN = "What was your course in UG?";
    private static final String SPECIALIZATION_COLUMN = "What is your UG specialization? Major Subject (Eg; Mathematics)";
    private static final String INTERESTS_COLUMN = "What are your interests?";
    private static final String SKILLS_COLUMN = "What are your skills ? (Select multiple if necessary)";
    private static final String CGPA_COLUMN = "What was the average CGPA or Percentage obtained in under graduation?";
    private static final String CERTIFIED_COLUMN = "Did you do any certification courses additionally?";
    private static final String CERTIFICATE_TITLE_COLUMN = "If yes, please specify your certificate course title.";
    private static final String JOB_TITLE_COLUMN = "If yes, then what is/was your first Job title in your current field of work? If not applicable, write NA.               ";
    private static final String MASTERS_COLUMN = "Have you done masters after undergraduation? If yes, mention your field of masters.(Eg; Masters in Mathematics)";

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[,;|/\\n]+");
    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9+.#& ]+");

    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "been", "by", "for", "from", "has", "have", "in", "into",
        "is", "it", "job", "of", "on", "or", "the", "their", "then", "this", "to", "was", "were", "with", "yes",
        "your", "field", "current", "first", "title", "work", "applicable", "write", "not", "if", "under",
        "graduate", "graduation", "course", "major", "subject", "select", "multiple", "necessary", "additional",
        "specify", "what", "did", "do", "done", "after", "mention"
    );

    private static final Set<String> EMPTY_OUTCOMES = Set.of(
        "", "na", "n/a", "no", "student", "student unemployed", "unemployed"
    );

    private static final Map<String, List<String>> LABEL_ALIASES = Map.of(
        "software_developer", List.of(
            "software developer", "software engineer", "computer software engineer", "java developer",
            "web developer", "back end developer", "backend developer", "front end developer",
            "frontend developer", "full stack developer", "application developer", "programmer",
            "programmer analyst", "software tester", "software testing"
        ),
        "data_analyst", List.of(
            "data analyst", "business analyst", "financial analyst", "data engineer",
            "data scientist", "associate data scientist", "database administrator",
            "data management", "power bi", "analytics"
        ),
        "ui_ux_designer", List.of(
            "ux designer", "ui designer", "product designer", "graphic designer", "graphics designer",
            "web designer", "designer"
        ),
        "cybersecurity_analyst", List.of(
            "cyber security analyst", "cybersecurity analyst", "information security analyst",
            "security analyst", "cyber security engineer", "security engineer"
        ),
        "cloud_engineer", List.of(
            "cloud engineer", "devops engineer", "associate engineer devops", "linux administrator",
            "site reliability engineer", "platform engineer"
        ),
        "digital_marketer", List.of(
            "digital marketer", "digital marketing", "digital marketing executive", "marketing manager",
            "marketing executive", "marketing head", "content marketing", "social media marketing",
            "web developer and digital marketer"
        )
    );

    private final ResourceLoader resourceLoader;
    private final String datasetPath;
    private final TrainingSnapshot trainingSnapshot;

    public CareerDatasetModel(ResourceLoader resourceLoader,
                              @Value("${app.ai.dataset.path:data/career_recommender.csv}") String datasetPath) {
        this.resourceLoader = resourceLoader;
        this.datasetPath = datasetPath;
        this.trainingSnapshot = loadTrainingSnapshot();
    }

    public DatasetInsight score(UserProfile profile,
                                List<UserSkill> userSkills,
                                AssessmentResult assessmentResult,
                                Career career,
                                List<CareerSkill> careerSkills) {
        if (career == null) {
            return DatasetInsight.empty();
        }

        List<String> userTokens = buildUserTokens(profile, userSkills, assessmentResult);
        List<String> metadataTokens = buildCareerMetadataTokens(career, careerSkills);
        if (userTokens.isEmpty() || metadataTokens.isEmpty()) {
            return DatasetInsight.empty();
        }

        Map<String, Double> userVector = vectorize(userTokens);
        Map<String, Double> metadataVector = vectorize(metadataTokens);
        String canonicalCareerLabel = mapCareerToLabel(career);

        Map<String, Double> targetVector = metadataVector;
        int supportCount = 0;
        if (canonicalCareerLabel != null) {
            Prototype prototype = trainingSnapshot.prototypes().get(canonicalCareerLabel);
            if (prototype != null) {
                targetVector = blendVectors(prototype.centroid(), 0.72, metadataVector, 0.28);
                supportCount = prototype.support();
            }
        }

        double cosineSimilarity = cosineSimilarity(userVector, targetVector);
        if (cosineSimilarity <= 0) {
            return DatasetInsight.empty();
        }

        List<String> evidenceTokens = extractEvidenceTokens(userVector, targetVector);
        String reason = buildReason(career, supportCount, evidenceTokens);
        return new DatasetInsight(round(cosineSimilarity * 100.0), supportCount, evidenceTokens, reason);
    }

    TrainingSnapshot snapshot() {
        return trainingSnapshot;
    }

    private TrainingSnapshot loadTrainingSnapshot() {
        try (Reader reader = openDatasetReader()) {
            List<Map<String, String>> rows = readCsv(reader);
            Map<String, List<List<String>>> groupedTokens = new LinkedHashMap<>();

            for (Map<String, String> row : rows) {
                String label = mapJobTitleToLabel(row.get(JOB_TITLE_COLUMN));
                if (label == null) {
                    continue;
                }

                List<String> tokens = buildDatasetTokens(row);
                if (tokens.isEmpty()) {
                    continue;
                }

                groupedTokens.computeIfAbsent(label, ignored -> new ArrayList<>()).add(tokens);
            }

            Map<String, Integer> documentFrequency = new HashMap<>();
            int totalDocuments = 0;
            for (List<List<String>> examples : groupedTokens.values()) {
                for (List<String> example : examples) {
                    totalDocuments++;
                    Set<String> unique = new LinkedHashSet<>(example);
                    for (String token : unique) {
                        documentFrequency.merge(token, 1, Integer::sum);
                    }
                }
            }

            if (totalDocuments == 0) {
                LOGGER.warn("Career dataset model did not find labeled samples in {}", datasetPath);
                return TrainingSnapshot.empty();
            }

            Map<String, Double> idf = new HashMap<>();
            double defaultIdf = Math.log(totalDocuments + 1.0) + 1.0;
            for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
                idf.put(entry.getKey(), Math.log((totalDocuments + 1.0) / (entry.getValue() + 1.0)) + 1.0);
            }

            Map<String, Prototype> prototypes = new LinkedHashMap<>();
            for (Map.Entry<String, List<List<String>>> entry : groupedTokens.entrySet()) {
                List<Map<String, Double>> vectors = entry.getValue().stream()
                    .map(tokens -> vectorize(tokens, idf, defaultIdf))
                    .toList();
                prototypes.put(entry.getKey(), new Prototype(averageVectors(vectors), vectors.size()));
            }

            LOGGER.info(
                "Career dataset model loaded {} labeled samples from {}: {}",
                totalDocuments,
                datasetPath,
                prototypes.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue().support())
                    .collect(Collectors.joining(", "))
            );
            return new TrainingSnapshot(Collections.unmodifiableMap(idf), defaultIdf, Collections.unmodifiableMap(prototypes));
        } catch (IOException exception) {
            LOGGER.warn("Career dataset model could not load dataset from {}: {}", datasetPath, exception.getMessage());
            return TrainingSnapshot.empty();
        }
    }

    private Reader openDatasetReader() throws IOException {
        Path filesystemPath = Path.of(datasetPath);
        if (Files.exists(filesystemPath)) {
            return Files.newBufferedReader(filesystemPath, StandardCharsets.UTF_8);
        }

        List<String> candidates = List.of(
            "file:" + datasetPath,
            "classpath:" + datasetPath,
            "classpath:/" + datasetPath,
            "classpath:/data/career_recommender.csv"
        );

        for (String candidate : candidates) {
            Resource resource = resourceLoader.getResource(candidate);
            if (resource.exists()) {
                return new BufferedReader(new java.io.InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            }
        }

        throw new IOException("Dataset resource not found");
    }

    private List<String> buildDatasetTokens(Map<String, String> row) {
        List<String> tokens = new ArrayList<>();
        addFieldTokens(tokens, row.get(COURSE_COLUMN), 2);
        addFieldTokens(tokens, row.get(SPECIALIZATION_COLUMN), 2);
        addFieldTokens(tokens, row.get(INTERESTS_COLUMN), 3);
        addFieldTokens(tokens, row.get(SKILLS_COLUMN), 3);
        addFieldTokens(tokens, row.get(CERTIFICATE_TITLE_COLUMN), 2);
        addFieldTokens(tokens, row.get(MASTERS_COLUMN), 2);

        String cgpaBucket = cgpaBucket(row.get(CGPA_COLUMN));
        if (cgpaBucket != null) {
            tokens.add(cgpaBucket);
        }
        if (hasAffirmative(row.get(CERTIFIED_COLUMN))) {
            tokens.add("has_certification");
        }
        if (hasMeaningfulText(row.get(MASTERS_COLUMN))) {
            tokens.add("has_masters");
        }
        return tokens;
    }

    private List<String> buildUserTokens(UserProfile profile,
                                         List<UserSkill> userSkills,
                                         AssessmentResult assessmentResult) {
        List<String> tokens = new ArrayList<>();
        if (profile != null) {
            addFieldTokens(tokens, profile.getCourse(), 2);
            addFieldTokens(tokens, profile.getPreferredIndustry(), 2);
            addFieldTokens(tokens, profile.getLongTermGoal(), 2);
            addFieldTokens(tokens, profile.getInterests(), 3);
            addFieldTokens(tokens, profile.getStrengths(), 2);
            addFieldTokens(tokens, profile.getWeaknesses(), 1);

            if (profile.getEducationLevel() != null) {
                tokens.add("education_" + profile.getEducationLevel().name().toLowerCase(Locale.ROOT));
            }
            if (profile.getPersonalityType() != null) {
                tokens.add("personality_" + profile.getPersonalityType().name().toLowerCase(Locale.ROOT));
                addPersonalityHints(tokens, profile.getPersonalityType());
            }
            String cgpaBucket = cgpaBucket(profile.getCgpa());
            if (cgpaBucket != null) {
                tokens.add(cgpaBucket);
            }
        }

        for (UserSkill userSkill : userSkills) {
            String skillName = userSkill.getSkill() == null ? null : userSkill.getSkill().getName();
            int repetitions = Math.max(1, Math.min(5, userSkill.getProficiencyLevel() == null ? 1 : userSkill.getProficiencyLevel()));
            addFieldTokens(tokens, skillName, repetitions);
        }

        addAssessmentTokens(tokens, assessmentResult);
        return tokens;
    }

    private List<String> buildCareerMetadataTokens(Career career, List<CareerSkill> careerSkills) {
        List<String> tokens = new ArrayList<>();
        addFieldTokens(tokens, career.getName(), 4);
        addFieldTokens(tokens, career.getDomain(), 3);
        addFieldTokens(tokens, career.getDescription(), 2);
        addFieldTokens(tokens, career.getFutureScope(), 1);
        addFieldTokens(tokens, career.getRelatedIndustries(), 2);
        addFieldTokens(tokens, career.getRecommendedCertifications(), 2);
        addFieldTokens(tokens, career.getRoadmapSteps(), 1);

        if (career.getRequiredEducation() != null) {
            tokens.add("education_" + career.getRequiredEducation().name().toLowerCase(Locale.ROOT));
        }
        if (career.getPreferredPersonality() != null) {
            tokens.add("personality_" + career.getPreferredPersonality().name().toLowerCase(Locale.ROOT));
            addPersonalityHints(tokens, career.getPreferredPersonality());
        }

        for (CareerSkill careerSkill : careerSkills) {
            String skillName = careerSkill.getSkill() == null ? null : careerSkill.getSkill().getName();
            int repetitions = Math.max(1, Math.min(4, (careerSkill.getImportanceWeight() == null ? 10 : careerSkill.getImportanceWeight()) / 10));
            addFieldTokens(tokens, skillName, repetitions);
        }
        return tokens;
    }

    private void addAssessmentTokens(List<String> tokens, AssessmentResult assessmentResult) {
        if (assessmentResult == null) {
            return;
        }

        Map<TraitCategory, Integer> normalized = normalizeAssessmentScores(assessmentResult);
        normalized.entrySet().stream()
            .sorted(Map.Entry.<TraitCategory, Integer>comparingByValue().reversed())
            .limit(3)
            .filter(entry -> entry.getValue() > 0)
            .forEach(entry -> {
                tokens.add("trait_" + entry.getKey().name().toLowerCase(Locale.ROOT));
                addTraitHints(tokens, entry.getKey(), entry.getValue() >= 75 ? 3 : 2);
            });
    }

    private Map<TraitCategory, Integer> normalizeAssessmentScores(AssessmentResult assessmentResult) {
        Map<TraitCategory, Integer> scores = new EnumMap<>(TraitCategory.class);
        scores.put(TraitCategory.ANALYTICAL_THINKING, safeInt(assessmentResult.getAnalyticalThinking()));
        scores.put(TraitCategory.CREATIVITY, safeInt(assessmentResult.getCreativity()));
        scores.put(TraitCategory.LEADERSHIP, safeInt(assessmentResult.getLeadership()));
        scores.put(TraitCategory.TECHNICAL_INCLINATION, safeInt(assessmentResult.getTechnicalInclination()));
        scores.put(TraitCategory.COMMUNICATION, safeInt(assessmentResult.getCommunication()));
        scores.put(TraitCategory.PROBLEM_SOLVING, safeInt(assessmentResult.getProblemSolving()));

        int max = scores.values().stream().max(Integer::compareTo).orElse(1);
        if (max <= 0) {
            return scores;
        }

        Map<TraitCategory, Integer> normalized = new EnumMap<>(TraitCategory.class);
        for (Map.Entry<TraitCategory, Integer> entry : scores.entrySet()) {
            normalized.put(entry.getKey(), (int) Math.round((entry.getValue() * 100.0) / max));
        }
        return normalized;
    }

    private void addTraitHints(List<String> tokens, TraitCategory category, int repetitions) {
        List<String> hints = switch (category) {
            case ANALYTICAL_THINKING -> List.of("analysis", "analytics", "logic");
            case CREATIVITY -> List.of("design", "creative", "content");
            case LEADERSHIP -> List.of("leadership", "management", "coordination");
            case TECHNICAL_INCLINATION -> List.of("technology", "software", "engineering");
            case COMMUNICATION -> List.of("communication", "marketing", "presentation");
            case PROBLEM_SOLVING -> List.of("problem solving", "debugging", "investigation");
        };
        for (String hint : hints) {
            addFieldTokens(tokens, hint, repetitions);
        }
    }

    private void addPersonalityHints(List<String> tokens, PersonalityType personalityType) {
        List<String> hints = switch (personalityType) {
            case ANALYTICAL -> List.of("analysis", "structured", "logic");
            case CREATIVE -> List.of("design", "creative", "ideas");
            case SOCIAL -> List.of("communication", "people", "collaboration");
            case LEADERSHIP -> List.of("leadership", "management", "strategy");
            case STRUCTURED -> List.of("structured", "process", "planning");
        };
        addFieldTokens(tokens, hints, 2);
    }

    private void addFieldTokens(List<String> target, Collection<String> values, int repetitions) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addFieldTokens(target, value, repetitions);
        }
    }

    private void addFieldTokens(List<String> target, String value, int repetitions) {
        if (value == null || repetitions <= 0) {
            return;
        }

        for (String segment : SPLIT_PATTERN.split(value)) {
            List<String> words = normalizeWords(segment);
            if (words.isEmpty()) {
                continue;
            }

            List<String> generatedTokens = new ArrayList<>(words);
            if (words.size() > 1) {
                generatedTokens.add(String.join("_", words));
                for (int index = 0; index < words.size() - 1; index++) {
                    generatedTokens.add(words.get(index) + "_" + words.get(index + 1));
                }
            }

            for (int count = 0; count < repetitions; count++) {
                target.addAll(generatedTokens);
            }
        }
    }

    private List<String> normalizeWords(String input) {
        if (input == null) {
            return List.of();
        }

        String normalized = input
            .replace('\u00a0', ' ')
            .replace("&", " and ")
            .replace("-", " ")
            .replace("_", " ")
            .toLowerCase(Locale.ROOT)
            .trim();

        normalized = NON_ALNUM_PATTERN.matcher(normalized).replaceAll(" ");
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> words = new ArrayList<>();
        for (String word : normalized.split("\\s+")) {
            String cleaned = canonicalizeToken(word);
            if (cleaned == null) {
                continue;
            }
            words.add(cleaned);
        }
        return words;
    }

    private String canonicalizeToken(String word) {
        if (word == null) {
            return null;
        }
        String cleaned = word.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isBlank() || STOP_WORDS.contains(cleaned)) {
            return null;
        }

        return switch (cleaned) {
            case "js", "javascript" -> "javascript";
            case "ux", "ui" -> cleaned;
            case "ml" -> "machinelearning";
            case "ai" -> "artificialintelligence";
            case "cse" -> "computerscience";
            case "btech", "b.tech" -> "btech";
            case "mtech", "m.tech" -> "mtech";
            case "mba", "mca", "bca", "bba", "bcom", "bsc", "be" -> cleaned;
            default -> cleaned.length() < 2 ? null : cleaned;
        };
    }

    private Map<String, Double> vectorize(List<String> tokens) {
        return vectorize(tokens, trainingSnapshot.idfByToken(), trainingSnapshot.defaultIdf());
    }

    private Map<String, Double> vectorize(List<String> tokens,
                                          Map<String, Double> idfByToken,
                                          double defaultIdf) {
        if (tokens == null || tokens.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                counts.merge(token, 1, Integer::sum);
            }
        }

        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double tf = 1.0 + Math.log(entry.getValue());
            double idf = idfByToken.getOrDefault(entry.getKey(), defaultIdf);
            vector.put(entry.getKey(), tf * idf);
        }
        return vector;
    }

    private Map<String, Double> averageVectors(List<Map<String, Double>> vectors) {
        if (vectors.isEmpty()) {
            return Map.of();
        }

        Map<String, Double> totals = new HashMap<>();
        for (Map<String, Double> vector : vectors) {
            for (Map.Entry<String, Double> entry : vector.entrySet()) {
                totals.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        int size = vectors.size();
        totals.replaceAll((token, value) -> value / size);
        return Collections.unmodifiableMap(totals);
    }

    private Map<String, Double> blendVectors(Map<String, Double> primary,
                                             double primaryWeight,
                                             Map<String, Double> secondary,
                                             double secondaryWeight) {
        Map<String, Double> blended = new HashMap<>();
        for (Map.Entry<String, Double> entry : primary.entrySet()) {
            blended.merge(entry.getKey(), entry.getValue() * primaryWeight, Double::sum);
        }
        for (Map.Entry<String, Double> entry : secondary.entrySet()) {
            blended.merge(entry.getKey(), entry.getValue() * secondaryWeight, Double::sum);
        }
        return blended;
    }

    private double cosineSimilarity(Map<String, Double> left, Map<String, Double> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }

        Map<String, Double> smaller = left.size() <= right.size() ? left : right;
        Map<String, Double> larger = smaller == left ? right : left;

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : smaller.entrySet()) {
            dotProduct += entry.getValue() * larger.getOrDefault(entry.getKey(), 0.0);
        }

        if (dotProduct == 0.0) {
            return 0.0;
        }

        double leftMagnitude = Math.sqrt(left.values().stream().mapToDouble(value -> value * value).sum());
        double rightMagnitude = Math.sqrt(right.values().stream().mapToDouble(value -> value * value).sum());
        if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
            return 0.0;
        }
        return dotProduct / (leftMagnitude * rightMagnitude);
    }

    private List<String> extractEvidenceTokens(Map<String, Double> userVector, Map<String, Double> targetVector) {
        return userVector.entrySet().stream()
            .filter(entry -> isDisplayToken(entry.getKey()) && targetVector.containsKey(entry.getKey()))
            .map(entry -> Map.entry(
                entry.getKey(),
                entry.getValue() * targetVector.getOrDefault(entry.getKey(), 0.0)
            ))
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .distinct()
            .limit(3)
            .map(token -> token.replace('_', ' '))
            .toList();
    }

    private boolean isDisplayToken(String token) {
        return token != null
            && !token.startsWith("education_")
            && !token.startsWith("personality_")
            && !token.startsWith("trait_")
            && !token.startsWith("cgpa_")
            && !token.startsWith("has_");
    }

    private String buildReason(Career career, int supportCount, List<String> evidenceTokens) {
        if (supportCount > 0 && !evidenceTokens.isEmpty()) {
            return "the dataset model found similar alumni profiles for this path, especially around "
                + joinNaturalLanguage(evidenceTokens);
        }
        if (supportCount > 0) {
            return "the dataset model found similar alumni outcomes for this path";
        }
        if (!evidenceTokens.isEmpty()) {
            return "your profile text is semantically close to this path around " + joinNaturalLanguage(evidenceTokens);
        }
        return "the dataset-driven model found partial semantic overlap with this career";
    }

    private String joinNaturalLanguage(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        if (values.size() == 2) {
            return values.get(0) + " and " + values.get(1);
        }
        return String.join(", ", values.subList(0, values.size() - 1)) + ", and " + values.get(values.size() - 1);
    }

    private String mapJobTitleToLabel(String rawJobTitle) {
        String normalizedJobTitle = normalizeText(rawJobTitle);
        if (EMPTY_OUTCOMES.contains(normalizedJobTitle)) {
            return null;
        }

        return findBestLabel(normalizedJobTitle);
    }

    private String mapCareerToLabel(Career career) {
        String normalizedCareerText = normalizeText(String.join(" ",
            List.of(career.getName(), career.getDomain())
                .stream()
                .filter(Objects::nonNull)
                .toList()
        ));
        return findBestLabel(normalizedCareerText);
    }

    private String findBestLabel(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return null;
        }

        String bestLabel = null;
        int bestScore = 0;
        for (Map.Entry<String, List<String>> entry : LABEL_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                String normalizedAlias = normalizeText(alias);
                if (normalizedText.contains(normalizedAlias)) {
                    int score = normalizedAlias.split("\\s+").length * 4;
                    if (score > bestScore) {
                        bestScore = score;
                        bestLabel = entry.getKey();
                    }
                } else {
                    int score = tokenOverlapScore(normalizedText, normalizedAlias);
                    if (score > bestScore) {
                        bestScore = score;
                        bestLabel = entry.getKey();
                    }
                }
            }
        }
        return bestScore >= 4 ? bestLabel : null;
    }

    private int tokenOverlapScore(String left, String right) {
        Set<String> leftTokens = new LinkedHashSet<>(normalizeWords(left));
        Set<String> rightTokens = new LinkedHashSet<>(normalizeWords(right));
        leftTokens.retainAll(rightTokens);
        return leftTokens.size() * 2;
    }

    private String normalizeText(String value) {
        return String.join(" ", normalizeWords(value));
    }

    private String cgpaBucket(String rawCgpa) {
        if (rawCgpa == null || rawCgpa.isBlank()) {
            return null;
        }
        String digitsOnly = rawCgpa.trim().replaceAll("[^0-9.]+", "");
        if (digitsOnly.isBlank()) {
            return null;
        }

        try {
            double score = Double.parseDouble(digitsOnly);
            if (score >= 80) {
                return "cgpa_high";
            }
            if (score >= 65) {
                return "cgpa_mid";
            }
            return "cgpa_low";
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String cgpaBucket(BigDecimal cgpa) {
        if (cgpa == null) {
            return null;
        }
        return cgpaBucket(cgpa.toPlainString());
    }

    private boolean hasAffirmative(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.replace('\u00a0', ' ').trim().toLowerCase(Locale.ROOT);
        return normalized.equals("yes") || normalized.startsWith("yes ");
    }

    private boolean hasMeaningfulText(String value) {
        String normalized = normalizeText(value);
        return normalized != null && !normalized.isBlank() && !EMPTY_OUTCOMES.contains(normalized);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private List<Map<String, String>> readCsv(Reader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        int raw;
        while ((raw = reader.read()) != -1) {
            char ch = (char) raw;

            if (rows.isEmpty() && currentRow.isEmpty() && currentField.length() == 0 && ch == '\ufeff') {
                continue;
            }

            if (ch == '"') {
                if (inQuotes) {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {
                        currentField.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) {
                            reader.reset();
                        }
                    }
                } else {
                    inQuotes = true;
                }
                continue;
            }

            if (ch == ',' && !inQuotes) {
                currentRow.add(currentField.toString());
                currentField.setLength(0);
                continue;
            }

            if ((ch == '\n' || ch == '\r') && !inQuotes) {
                if (ch == '\r') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next != '\n' && next != -1) {
                        reader.reset();
                    }
                }

                currentRow.add(currentField.toString());
                currentField.setLength(0);
                if (currentRow.stream().anyMatch(value -> value != null && !value.isBlank())) {
                    rows.add(new ArrayList<>(currentRow));
                }
                currentRow.clear();
                continue;
            }

            currentField.append(ch);
        }

        if (currentField.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentField.toString());
            if (currentRow.stream().anyMatch(value -> value != null && !value.isBlank())) {
                rows.add(currentRow);
            }
        }

        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> headers = rows.get(0);
        List<Map<String, String>> mappedRows = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            Map<String, String> mapped = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                String header = headers.get(column);
                String value = column < row.size() ? row.get(column) : "";
                mapped.put(header, value == null ? "" : value.trim());
            }
            mappedRows.add(mapped);
        }
        return mappedRows;
    }

    public record DatasetInsight(
        double score,
        int supportCount,
        List<String> evidenceTokens,
        String reason
    ) {
        static DatasetInsight empty() {
            return new DatasetInsight(0.0, 0, List.of(), "");
        }
    }

    record Prototype(
        Map<String, Double> centroid,
        int support
    ) {
    }

    record TrainingSnapshot(
        Map<String, Double> idfByToken,
        double defaultIdf,
        Map<String, Prototype> prototypes
    ) {
        static TrainingSnapshot empty() {
            return new TrainingSnapshot(Map.of(), 1.0, Map.of());
        }
    }
}
