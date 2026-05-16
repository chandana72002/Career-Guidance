package com.datavalley.careerguidance.controller;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datavalley.careerguidance.dto.CareerDtos;
import com.datavalley.careerguidance.dto.RecommendationDtos;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.service.CareerService;
import com.datavalley.careerguidance.service.RecommendationService;

@RestController
@RequestMapping("/api/careers")
public class CareerController {

    private final CareerService careerService;
    private final RecommendationService recommendationService;

    public CareerController(CareerService careerService, RecommendationService recommendationService) {
        this.careerService = careerService;
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<CareerDtos.CareerResponse> getCareers() {
        return careerService.getAllCareers();
    }

    @GetMapping("/{careerId}")
    public CareerDtos.CareerResponse getCareer(@PathVariable Long careerId) {
        return careerService.getCareer(careerId);
    }

    @GetMapping("/{careerId}/resources")
    public List<CareerDtos.LearningResourceResponse> getResources(@PathVariable Long careerId) {
        return careerService.getResources(careerId);
    }

    @PostMapping("/{careerId}/save")
    public RecommendationDtos.SavedCareerResponse saveCareer(@AuthenticationPrincipal User user,
                                                             @PathVariable Long careerId) {
        return recommendationService.saveCareer(user, careerId);
    }
}
