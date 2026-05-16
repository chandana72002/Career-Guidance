package com.datavalley.careerguidance.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotEmpty;

public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    public record AssessmentOptionResponse(
        String key,
        String text
    ) {
    }

    public record AssessmentQuestionResponse(
        Long id,
        String questionText,
        String traitCategory,
        List<AssessmentOptionResponse> options
    ) {
    }

    public record AssessmentSubmissionRequest(
        @NotEmpty(message = "At least one answer is required")
        Map<Long, String> answers
    ) {
    }

    public record AssessmentResultResponse(
        LocalDateTime createdAt,
        String summary,
        Map<String, Integer> scores
    ) {
    }
}
