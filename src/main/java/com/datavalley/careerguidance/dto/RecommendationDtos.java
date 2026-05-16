package com.datavalley.careerguidance.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    public record RecommendationResponse(
        Long careerId,
        String careerName,
        String domain,
        Double compatibilityScore,
        String explanation,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> roadmap,
        LocalDateTime createdAt
    ) {
    }

    public record SavedCareerResponse(
        Long savedId,
        Long careerId,
        String careerName,
        String domain,
        LocalDateTime savedAt
    ) {
    }
}
