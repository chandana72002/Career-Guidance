package com.datavalley.careerguidance.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.datavalley.careerguidance.entity.AssessmentResult;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.Recommendation;
import com.datavalley.careerguidance.entity.Role;
import com.datavalley.careerguidance.entity.SavedCareer;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.entity.UserSkill;
import com.datavalley.careerguidance.repository.AssessmentResultRepository;
import com.datavalley.careerguidance.repository.CareerRepository;
import com.datavalley.careerguidance.repository.RecommendationRepository;
import com.datavalley.careerguidance.repository.SavedCareerRepository;
import com.datavalley.careerguidance.repository.SkillRepository;
import com.datavalley.careerguidance.repository.UserProfileRepository;
import com.datavalley.careerguidance.repository.UserRepository;
import com.datavalley.careerguidance.repository.UserSkillRepository;

@Service
public class WorkspaceService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSkillRepository userSkillRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final RecommendationRepository recommendationRepository;
    private final SavedCareerRepository savedCareerRepository;
    private final CareerRepository careerRepository;
    private final SkillRepository skillRepository;
    private final ProfileService profileService;

    public WorkspaceService(UserRepository userRepository,
                            UserProfileRepository userProfileRepository,
                            UserSkillRepository userSkillRepository,
                            AssessmentResultRepository assessmentResultRepository,
                            RecommendationRepository recommendationRepository,
                            SavedCareerRepository savedCareerRepository,
                            CareerRepository careerRepository,
                            SkillRepository skillRepository,
                            ProfileService profileService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSkillRepository = userSkillRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.recommendationRepository = recommendationRepository;
        this.savedCareerRepository = savedCareerRepository;
        this.careerRepository = careerRepository;
        this.skillRepository = skillRepository;
        this.profileService = profileService;
    }

    public Map<String, Object> getCounselorOverview() {
        List<User> learners = userRepository.findAll().stream()
            .filter(user -> user.getRole() == Role.ROLE_USER)
            .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .toList();

        Map<Long, UserProfile> profilesByUserId = userProfileRepository.findAll().stream()
            .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<UserSkill>> skillsByUserId = userSkillRepository.findAll().stream()
            .collect(Collectors.groupingBy(userSkill -> userSkill.getUser().getId()));
        Map<Long, AssessmentResult> latestAssessments = latestAssessmentsByUserId();
        Map<Long, List<Recommendation>> recommendationsByUserId = recommendationsByUserId();
        Map<Long, Long> savedCareerCounts = savedCareerRepository.findAll().stream()
            .collect(Collectors.groupingBy(savedCareer -> savedCareer.getUser().getId(), Collectors.counting()));

        long profileReadyCount = 0;
        List<Map<String, Object>> learnersPayload = new ArrayList<>();
        for (User learner : learners) {
            UserProfile profile = profilesByUserId.get(learner.getId());
            List<UserSkill> skills = skillsByUserId.getOrDefault(learner.getId(), List.of());
            int completion = profile == null ? 0 : profileService.calculateProfileCompletion(profile, skills);
            if (completion >= 70) {
                profileReadyCount++;
            }

            AssessmentResult assessment = latestAssessments.get(learner.getId());
            Recommendation topRecommendation = recommendationsByUserId.getOrDefault(learner.getId(), List.of()).stream().findFirst().orElse(null);

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("name", learner.getFullName());
            snapshot.put("email", learner.getEmail());
            snapshot.put("course", profile != null ? profile.getCourse() : null);
            snapshot.put("educationLevel", profile != null && profile.getEducationLevel() != null ? profile.getEducationLevel().name() : null);
            snapshot.put("profileCompletion", completion);
            snapshot.put("latestAssessment", assessment != null ? assessment.getResultSummary() : "Assessment pending");
            snapshot.put("topCareer", topRecommendation != null ? topRecommendation.getCareer().getName() : "No recommendations yet");
            snapshot.put("topScore", topRecommendation != null ? Math.round(topRecommendation.getCompatibilityScore()) : 0);
            snapshot.put("savedPaths", savedCareerCounts.getOrDefault(learner.getId(), 0L));
            snapshot.put("createdAt", learner.getCreatedAt());
            learnersPayload.add(snapshot);
        }

        List<Map<String, Object>> topDomains = recommendationsByUserId.values().stream()
            .map(items -> items.stream().findFirst().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(item -> item.getCareer().getDomain(), LinkedHashMap::new, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> insight(entry.getKey(), entry.getValue().intValue(), "learners currently align with this domain"))
            .toList();

        List<Map<String, Object>> skillAlerts = recommendationRepository.findAll().stream()
            .flatMap(recommendation -> recommendation.getMissingSkills().stream())
            .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(6)
            .map(entry -> insight(entry.getKey(), entry.getValue().intValue(), "recurring missing skill across learner recommendations"))
            .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("metrics", List.of(
            metric("Active learners", learners.size(), "Students currently using the platform"),
            metric("Profile ready", profileReadyCount, "Learners with 70%+ profile completeness"),
            metric("Assessments done", latestAssessments.size(), "Learners with at least one submitted assessment"),
            metric("Recommendation-ready", recommendationsByUserId.size(), "Learners with generated guidance")
        ));
        payload.put("learners", learnersPayload.stream().limit(8).toList());
        payload.put("careerSignals", topDomains);
        payload.put("skillAlerts", skillAlerts);
        payload.put("actionNotes", List.of(
            "Reach out first to learners with strong profile completion but no assessment result.",
            "Use the top domain signal to organize group counseling or workshop sessions.",
            "Track repeated missing skills to suggest shared learning plans for multiple students."
        ));
        return payload;
    }

    public Map<String, Object> getAdminOverview() {
        List<User> users = userRepository.findAll().stream()
            .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .toList();
        List<Career> careers = careerRepository.findAll().stream()
            .sorted(Comparator.comparing(Career::getName))
            .toList();
        List<Recommendation> recommendations = recommendationRepository.findAll();
        List<SavedCareer> savedCareers = savedCareerRepository.findAll();

        Map<Role, Long> roleCounts = users.stream()
            .collect(Collectors.groupingBy(User::getRole, () -> new java.util.EnumMap<>(Role.class), Collectors.counting()));
        Map<Long, Long> recommendationCountsByCareer = recommendations.stream()
            .collect(Collectors.groupingBy(recommendation -> recommendation.getCareer().getId(), Collectors.counting()));
        Map<Long, Long> savedCountsByCareer = savedCareers.stream()
            .collect(Collectors.groupingBy(savedCareer -> savedCareer.getCareer().getId(), Collectors.counting()));

        List<Map<String, Object>> recentUsers = users.stream()
            .limit(8)
            .map(user -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", user.getFullName());
                item.put("email", user.getEmail());
                item.put("role", user.getRole().name());
                item.put("createdAt", user.getCreatedAt());
                return item;
            })
            .toList();

        List<Map<String, Object>> careerHealth = careers.stream()
            .map(career -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", career.getId());
                item.put("name", career.getName());
                item.put("domain", career.getDomain());
                item.put("requiredEducation", career.getRequiredEducation().name());
                item.put("recommendationCount", recommendationCountsByCareer.getOrDefault(career.getId(), 0L));
                item.put("savedCount", savedCountsByCareer.getOrDefault(career.getId(), 0L));
                return item;
            })
            .toList();

        List<Map<String, Object>> skillTrends = userSkillRepository.findAll().stream()
            .collect(Collectors.groupingBy(userSkill -> userSkill.getSkill().getName(), LinkedHashMap::new, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(6)
            .map(entry -> insight(entry.getKey(), entry.getValue().intValue(), "learners currently list this as an active skill"))
            .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("metrics", List.of(
            metric("Users", users.size(), "Total registered accounts"),
            metric("Counselors", roleCounts.getOrDefault(Role.ROLE_COUNSELOR, 0L), "Active counselor accounts"),
            metric("Careers", careers.size(), "Available mapped career tracks"),
            metric("Skills", skillRepository.count(), "Skills available in the catalog"),
            metric("Recommendations", recommendations.size(), "Generated recommendation records")
        ));
        payload.put("recentUsers", recentUsers);
        payload.put("careerHealth", careerHealth);
        payload.put("roleMix", List.of(
            insight("Students", roleCounts.getOrDefault(Role.ROLE_USER, 0L).intValue(), "role distribution"),
            insight("Counselors", roleCounts.getOrDefault(Role.ROLE_COUNSELOR, 0L).intValue(), "role distribution"),
            insight("Admins", roleCounts.getOrDefault(Role.ROLE_ADMIN, 0L).intValue(), "role distribution")
        ));
        payload.put("skillTrends", skillTrends);
        payload.put("systemNotes", List.of(
            "Career management changes affect both recommendation generation and saved path views.",
            "High-saved careers usually indicate where roadmap quality matters most.",
            "Keep skill mappings current so counselor guidance and recommendation quality stay aligned."
        ));
        return payload;
    }

    private Map<Long, AssessmentResult> latestAssessmentsByUserId() {
        return assessmentResultRepository.findAll().stream()
            .collect(Collectors.toMap(
                assessment -> assessment.getUser().getId(),
                Function.identity(),
                (left, right) -> left.getCreatedAt().isAfter(right.getCreatedAt()) ? left : right
            ));
    }

    private Map<Long, List<Recommendation>> recommendationsByUserId() {
        Map<Long, List<Recommendation>> grouped = recommendationRepository.findAll().stream()
            .collect(Collectors.groupingBy(recommendation -> recommendation.getUser().getId()));
        grouped.values().forEach(items -> items.sort(Comparator.comparing(Recommendation::getCompatibilityScore).reversed()));
        return grouped;
    }

    private Map<String, Object> metric(String label, long value, String detail) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("detail", detail);
        return item;
    }

    private Map<String, Object> insight(String label, int value, String detail) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("detail", detail);
        return item;
    }
}
