package com.datavalley.careerguidance.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datavalley.careerguidance.ai.RecommendationEngine;
import com.datavalley.careerguidance.dto.ProfileDtos;
import com.datavalley.careerguidance.dto.RecommendationDtos;
import com.datavalley.careerguidance.entity.AssessmentResult;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.Recommendation;
import com.datavalley.careerguidance.entity.SavedCareer;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.entity.UserSkill;
import com.datavalley.careerguidance.exception.ResourceNotFoundException;
import com.datavalley.careerguidance.repository.AssessmentResultRepository;
import com.datavalley.careerguidance.repository.CareerRepository;
import com.datavalley.careerguidance.repository.CareerSkillRepository;
import com.datavalley.careerguidance.repository.RecommendationRepository;
import com.datavalley.careerguidance.repository.SavedCareerRepository;
import com.datavalley.careerguidance.repository.UserProfileRepository;
import com.datavalley.careerguidance.repository.UserSkillRepository;

@Service
public class RecommendationService {

    private final RecommendationEngine recommendationEngine;
    private final UserProfileRepository userProfileRepository;
    private final UserSkillRepository userSkillRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final CareerRepository careerRepository;
    private final CareerSkillRepository careerSkillRepository;
    private final RecommendationRepository recommendationRepository;
    private final SavedCareerRepository savedCareerRepository;
    private final ProfileService profileService;

    public RecommendationService(RecommendationEngine recommendationEngine,
                                 UserProfileRepository userProfileRepository,
                                 UserSkillRepository userSkillRepository,
                                 AssessmentResultRepository assessmentResultRepository,
                                 CareerRepository careerRepository,
                                 CareerSkillRepository careerSkillRepository,
                                 RecommendationRepository recommendationRepository,
                                 SavedCareerRepository savedCareerRepository,
                                 ProfileService profileService) {
        this.recommendationEngine = recommendationEngine;
        this.userProfileRepository = userProfileRepository;
        this.userSkillRepository = userSkillRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.careerRepository = careerRepository;
        this.careerSkillRepository = careerSkillRepository;
        this.recommendationRepository = recommendationRepository;
        this.savedCareerRepository = savedCareerRepository;
        this.profileService = profileService;
    }

    @Transactional
    public List<RecommendationDtos.RecommendationResponse> generateRecommendations(User user) {
        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
        List<UserSkill> userSkills = userSkillRepository.findByUser(user);
        AssessmentResult assessmentResult = assessmentResultRepository.findTopByUserOrderByCreatedAtDesc(user).orElse(null);

        List<RecommendationEngine.RecommendationInsight> insights = careerRepository.findAll()
            .stream()
            .map(career -> recommendationEngine.evaluate(
                profile,
                userSkills,
                assessmentResult,
                career,
                careerSkillRepository.findByCareer(career)
            ))
            .sorted(Comparator.comparingDouble(RecommendationEngine.RecommendationInsight::score).reversed())
            .limit(5)
            .toList();

        recommendationRepository.deleteByUser(user);
        List<Recommendation> savedRecommendations = new ArrayList<>();
        for (RecommendationEngine.RecommendationInsight insight : insights) {
            Recommendation recommendation = new Recommendation();
            recommendation.setUser(user);
            recommendation.setCareer(insight.career());
            recommendation.setCompatibilityScore(insight.score());
            recommendation.setExplanation(insight.explanation());
            recommendation.setMatchedSkills(insight.matchedSkills());
            recommendation.setMissingSkills(insight.missingSkills());
            recommendation.setRoadmap(insight.roadmap());
            savedRecommendations.add(recommendationRepository.save(recommendation));
        }
        return savedRecommendations.stream().map(this::mapRecommendation).toList();
    }

    public List<RecommendationDtos.RecommendationResponse> getLatestRecommendations(User user) {
        return recommendationRepository.findByUserOrderByCompatibilityScoreDesc(user)
            .stream()
            .map(this::mapRecommendation)
            .toList();
    }

    @Transactional
    public RecommendationDtos.SavedCareerResponse saveCareer(User user, Long careerId) {
        Career career = careerRepository.findById(careerId)
            .orElseThrow(() -> new ResourceNotFoundException("Career not found"));
        SavedCareer existing = savedCareerRepository.findByUserAndCareer(user, career).orElse(null);
        if (existing != null) {
            return mapSaved(existing);
        }

        SavedCareer savedCareer = new SavedCareer();
        savedCareer.setUser(user);
        savedCareer.setCareer(career);
        SavedCareer saved = savedCareerRepository.save(savedCareer);
        return mapSaved(saved);
    }

    public List<RecommendationDtos.SavedCareerResponse> getSavedCareers(User user) {
        return savedCareerRepository.findByUserOrderBySavedAtDesc(user)
            .stream()
            .map(this::mapSaved)
            .toList();
    }

    public ProfileDtos.DashboardResponse getDashboard(User user) {
        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
        List<UserSkill> skills = userSkillRepository.findByUser(user);
        int completion = profile == null ? 0 : profileService.calculateProfileCompletion(profile, skills);
        List<Recommendation> recommendations = recommendationRepository.findByUserOrderByCompatibilityScoreDesc(user);
        AssessmentResult latestAssessment = assessmentResultRepository.findTopByUserOrderByCreatedAtDesc(user).orElse(null);

        return new ProfileDtos.DashboardResponse(
            completion,
            latestAssessment != null ? latestAssessment.getResultSummary() : "Assessment not completed",
            latestAssessment != null ? 6 : 0,
            recommendations.isEmpty() ? null : recommendations.get(0).getCareer().getName(),
            recommendations.isEmpty() ? null : recommendations.get(0).getCompatibilityScore(),
            savedCareerRepository.findByUserOrderBySavedAtDesc(user).size(),
            recommendations.size()
        );
    }

    private RecommendationDtos.RecommendationResponse mapRecommendation(Recommendation recommendation) {
        return new RecommendationDtos.RecommendationResponse(
            recommendation.getCareer().getId(),
            recommendation.getCareer().getName(),
            recommendation.getCareer().getDomain(),
            recommendation.getCompatibilityScore(),
            recommendation.getExplanation(),
            new ArrayList<>(recommendation.getMatchedSkills()),
            new ArrayList<>(recommendation.getMissingSkills()),
            new ArrayList<>(recommendation.getRoadmap()),
            recommendation.getCreatedAt()
        );
    }

    private RecommendationDtos.SavedCareerResponse mapSaved(SavedCareer savedCareer) {
        return new RecommendationDtos.SavedCareerResponse(
            savedCareer.getId(),
            savedCareer.getCareer().getId(),
            savedCareer.getCareer().getName(),
            savedCareer.getCareer().getDomain(),
            savedCareer.getSavedAt()
        );
    }
}
