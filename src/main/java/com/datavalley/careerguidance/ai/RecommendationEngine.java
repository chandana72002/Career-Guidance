package com.datavalley.careerguidance.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.datavalley.careerguidance.entity.AssessmentResult;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.CareerSkill;
import com.datavalley.careerguidance.entity.PersonalityType;
import com.datavalley.careerguidance.entity.TraitCategory;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.entity.UserSkill;

@Component
public class RecommendationEngine {

    private final CareerDatasetModel careerDatasetModel;

    public RecommendationEngine(CareerDatasetModel careerDatasetModel) {
        this.careerDatasetModel = careerDatasetModel;
    }

    public RecommendationInsight evaluate(UserProfile profile,
                                          List<UserSkill> userSkills,
                                          AssessmentResult assessmentResult,
                                          Career career,
                                          List<CareerSkill> careerSkills) {
        double skillMatch = calculateSkillMatch(userSkills, careerSkills);
        double interestMatch = calculateInterestMatch(profile, career);
        double assessmentMatch = calculateAssessmentMatch(assessmentResult, career);
        double educationMatch = calculateEducationMatch(profile, career);
        double personalityMatch = calculatePersonalityMatch(profile, career);

        double heuristicScore = (skillMatch * 0.35)
            + (interestMatch * 0.25)
            + (assessmentMatch * 0.20)
            + (educationMatch * 0.10)
            + (personalityMatch * 0.10);
        CareerDatasetModel.DatasetInsight datasetInsight = careerDatasetModel.score(
            profile,
            userSkills,
            assessmentResult,
            career,
            careerSkills
        );
        double totalScore = datasetInsight.score() > 0
            ? (heuristicScore * 0.55) + (datasetInsight.score() * 0.45)
            : heuristicScore;

        Set<String> matchedSkills = getMatchedSkills(userSkills, careerSkills);
        Set<String> missingSkills = getMissingSkills(userSkills, careerSkills);
        String explanation = buildExplanation(profile, assessmentResult, career, matchedSkills, missingSkills,
            skillMatch, assessmentMatch, interestMatch, datasetInsight);

        return new RecommendationInsight(
            career,
            round(totalScore),
            explanation,
            matchedSkills,
            missingSkills,
            new LinkedHashSet<>(career.getRoadmapSteps())
        );
    }

    private double calculateSkillMatch(List<UserSkill> userSkills, List<CareerSkill> careerSkills) {
        if (careerSkills.isEmpty()) {
            return 50.0;
        }

        Map<Long, Integer> userSkillMap = userSkills.stream()
            .collect(Collectors.toMap(skill -> skill.getSkill().getId(), UserSkill::getProficiencyLevel));

        double matchedWeight = 0;
        double totalWeight = 0;
        for (CareerSkill careerSkill : careerSkills) {
            int importance = defaultWeight(careerSkill.getImportanceWeight());
            totalWeight += importance;
            Integer proficiency = userSkillMap.get(careerSkill.getSkill().getId());
            if (proficiency != null) {
                matchedWeight += importance * Math.min(5, Math.max(1, proficiency)) / 5.0;
            }
        }
        return totalWeight == 0 ? 50.0 : (matchedWeight / totalWeight) * 100.0;
    }

    private double calculateInterestMatch(UserProfile profile, Career career) {
        if (profile == null) {
            return 40.0;
        }

        List<String> profileKeywords = new ArrayList<>();
        profileKeywords.addAll(clean(profile.getInterests()));
        profileKeywords.addAll(clean(Set.of(profile.getPreferredIndustry())));
        profileKeywords.addAll(clean(Set.of(profile.getCourse())));
        profileKeywords.addAll(clean(Set.of(profile.getLongTermGoal())));

        if (profileKeywords.isEmpty()) {
            return 50.0;
        }

        String careerText = (career.getName() + " " + career.getDomain() + " " + career.getDescription() + " "
            + String.join(" ", career.getRelatedIndustries())).toLowerCase(Locale.ROOT);
        long matches = profileKeywords.stream()
            .filter(keyword -> careerText.contains(keyword))
            .count();
        return Math.min(100.0, (matches * 100.0) / profileKeywords.size());
    }

    private double calculateAssessmentMatch(AssessmentResult assessmentResult, Career career) {
        if (assessmentResult == null) {
            return 50.0;
        }

        Map<TraitCategory, Integer> normalizedScores = normalizeAssessmentScores(assessmentResult);
        int totalWeight = defaultWeight(career.getAnalyticalWeight())
            + defaultWeight(career.getCreativityWeight())
            + defaultWeight(career.getLeadershipWeight())
            + defaultWeight(career.getTechnicalWeight())
            + defaultWeight(career.getCommunicationWeight())
            + defaultWeight(career.getProblemSolvingWeight());

        if (totalWeight == 0) {
            return 50.0;
        }

        double weighted = normalizedScores.get(TraitCategory.ANALYTICAL_THINKING) * defaultWeight(career.getAnalyticalWeight());
        weighted += normalizedScores.get(TraitCategory.CREATIVITY) * defaultWeight(career.getCreativityWeight());
        weighted += normalizedScores.get(TraitCategory.LEADERSHIP) * defaultWeight(career.getLeadershipWeight());
        weighted += normalizedScores.get(TraitCategory.TECHNICAL_INCLINATION) * defaultWeight(career.getTechnicalWeight());
        weighted += normalizedScores.get(TraitCategory.COMMUNICATION) * defaultWeight(career.getCommunicationWeight());
        weighted += normalizedScores.get(TraitCategory.PROBLEM_SOLVING) * defaultWeight(career.getProblemSolvingWeight());
        return weighted / totalWeight;
    }

    private double calculateEducationMatch(UserProfile profile, Career career) {
        if (profile == null || profile.getEducationLevel() == null) {
            return 50.0;
        }
        int diff = profile.getEducationLevel().ordinal() - career.getRequiredEducation().ordinal();
        if (diff >= 0) {
            return 100.0;
        }
        if (diff == -1) {
            return 65.0;
        }
        return 35.0;
    }

    private double calculatePersonalityMatch(UserProfile profile, Career career) {
        if (profile == null || profile.getPersonalityType() == null) {
            return 50.0;
        }
        PersonalityType personalityType = profile.getPersonalityType();
        if (personalityType == career.getPreferredPersonality()) {
            return 100.0;
        }
        if ((personalityType == PersonalityType.ANALYTICAL && career.getPreferredPersonality() == PersonalityType.STRUCTURED)
            || (personalityType == PersonalityType.STRUCTURED && career.getPreferredPersonality() == PersonalityType.ANALYTICAL)
            || (personalityType == PersonalityType.SOCIAL && career.getPreferredPersonality() == PersonalityType.LEADERSHIP)
            || (personalityType == PersonalityType.LEADERSHIP && career.getPreferredPersonality() == PersonalityType.SOCIAL)
            || (personalityType == PersonalityType.CREATIVE && career.getPreferredPersonality() == PersonalityType.SOCIAL)) {
            return 70.0;
        }
        return 45.0;
    }

    private Map<TraitCategory, Integer> normalizeAssessmentScores(AssessmentResult assessmentResult) {
        Map<TraitCategory, Integer> scores = new EnumMap<>(TraitCategory.class);
        scores.put(TraitCategory.ANALYTICAL_THINKING, safeInt(assessmentResult.getAnalyticalThinking()));
        scores.put(TraitCategory.CREATIVITY, safeInt(assessmentResult.getCreativity()));
        scores.put(TraitCategory.LEADERSHIP, safeInt(assessmentResult.getLeadership()));
        scores.put(TraitCategory.TECHNICAL_INCLINATION, safeInt(assessmentResult.getTechnicalInclination()));
        scores.put(TraitCategory.COMMUNICATION, safeInt(assessmentResult.getCommunication()));
        scores.put(TraitCategory.PROBLEM_SOLVING, safeInt(assessmentResult.getProblemSolving()));

        int max = scores.values().stream().max(Comparator.naturalOrder()).orElse(1);
        if (max == 0) {
            max = 1;
        }

        Map<TraitCategory, Integer> normalized = new EnumMap<>(TraitCategory.class);
        for (Map.Entry<TraitCategory, Integer> entry : scores.entrySet()) {
            normalized.put(entry.getKey(), (int) Math.round((entry.getValue() * 100.0) / max));
        }
        return normalized;
    }

    private Set<String> getMatchedSkills(List<UserSkill> userSkills, List<CareerSkill> careerSkills) {
        Map<Long, Integer> userSkillMap = userSkills.stream()
            .collect(Collectors.toMap(skill -> skill.getSkill().getId(), UserSkill::getProficiencyLevel, Integer::max));
        return careerSkills.stream()
            .filter(careerSkill -> userSkillMap.getOrDefault(careerSkill.getSkill().getId(), 0) >= 2)
            .map(careerSkill -> careerSkill.getSkill().getName())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> getMissingSkills(List<UserSkill> userSkills, List<CareerSkill> careerSkills) {
        Map<Long, Integer> userSkillMap = userSkills.stream()
            .collect(Collectors.toMap(skill -> skill.getSkill().getId(), UserSkill::getProficiencyLevel, Integer::max));
        return careerSkills.stream()
            .filter(careerSkill -> userSkillMap.getOrDefault(careerSkill.getSkill().getId(), 0) < 2)
            .map(careerSkill -> careerSkill.getSkill().getName())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildExplanation(UserProfile profile,
                                    AssessmentResult assessmentResult,
                                    Career career,
                                    Set<String> matchedSkills,
                                    Set<String> missingSkills,
                                    double skillMatch,
                                    double assessmentMatch,
                                    double interestMatch,
                                    CareerDatasetModel.DatasetInsight datasetInsight) {
        List<String> reasons = new ArrayList<>();
        if (skillMatch >= 60 && !matchedSkills.isEmpty()) {
            reasons.add("your current skills already align with " + String.join(", ", matchedSkills));
        }
        if (assessmentMatch >= 65 && assessmentResult != null) {
            reasons.add("your assessment profile matches the demands of this domain");
        }
        if (interestMatch >= 50 && profile != null && !clean(profile.getInterests()).isEmpty()) {
            reasons.add("your interests overlap with " + career.getDomain() + " work");
        }
        if (datasetInsight.score() >= 45 && datasetInsight.reason() != null && !datasetInsight.reason().isBlank()) {
            reasons.add(datasetInsight.reason());
        }
        if (!missingSkills.isEmpty()) {
            reasons.add("learning " + String.join(", ", missingSkills.stream().limit(3).toList()) + " can improve your fit");
        }
        if (reasons.isEmpty()) {
            reasons.add("your profile shows partial alignment with this career path");
        }
        return Character.toUpperCase(reasons.get(0).charAt(0)) + reasons.get(0).substring(1) + ". "
            + reasons.stream().skip(1).collect(Collectors.joining(". "));
    }

    private List<String> clean(Set<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .flatMap(value -> List.of(value.toLowerCase(Locale.ROOT).split("[,\\s]+")).stream())
            .filter(token -> token.length() > 2)
            .distinct()
            .toList();
    }

    private int defaultWeight(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record RecommendationInsight(
        Career career,
        double score,
        String explanation,
        Set<String> matchedSkills,
        Set<String> missingSkills,
        Set<String> roadmap
    ) {
    }
}
