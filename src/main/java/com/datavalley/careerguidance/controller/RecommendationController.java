package com.datavalley.careerguidance.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datavalley.careerguidance.dto.CareerDtos;
import com.datavalley.careerguidance.dto.RecommendationDtos;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.service.CareerService;
import com.datavalley.careerguidance.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CareerService careerService;

    public RecommendationController(RecommendationService recommendationService, CareerService careerService) {
        this.recommendationService = recommendationService;
        this.careerService = careerService;
    }

    @GetMapping("/me")
    public List<Map<String, Object>> getLatest(@AuthenticationPrincipal User user) {
        return toFrontendPayload(recommendationService.getLatestRecommendations(user), recommendationService.getSavedCareers(user));
    }

    @PostMapping("/generate")
    public List<Map<String, Object>> generate(@AuthenticationPrincipal User user) {
        return toFrontendPayload(recommendationService.generateRecommendations(user), recommendationService.getSavedCareers(user));
    }

    @GetMapping("/saved")
    public List<RecommendationDtos.SavedCareerResponse> getSaved(@AuthenticationPrincipal User user) {
        return recommendationService.getSavedCareers(user);
    }

    private List<Map<String, Object>> toFrontendPayload(List<RecommendationDtos.RecommendationResponse> recommendations,
                                                        List<RecommendationDtos.SavedCareerResponse> savedCareers) {
        Set<Long> savedCareerIds = savedCareers.stream()
            .map(RecommendationDtos.SavedCareerResponse::careerId)
            .collect(Collectors.toSet());

        return recommendations.stream().map(item -> {
            List<CareerDtos.LearningResourceResponse> resources = careerService.getResources(item.careerId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", item.careerId());
            payload.put("careerId", item.careerId());
            payload.put("careerName", item.careerName());
            payload.put("domain", item.domain());
            payload.put("compatibilityScore", item.compatibilityScore());
            payload.put("explanation", item.explanation());
            payload.put("matchedSkills", item.matchedSkills());
            payload.put("missingSkills", item.missingSkills());
            payload.put("roadmapSteps", item.roadmap());
            payload.put("learningResources", resources);
            payload.put("saved", savedCareerIds.contains(item.careerId()));
            payload.put("createdAt", item.createdAt());
            return payload;
        }).toList();
    }
}
