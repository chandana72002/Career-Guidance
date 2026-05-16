package com.datavalley.careerguidance.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datavalley.careerguidance.dto.AssessmentDtos;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.service.AssessmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/default/questions")
    public List<Map<String, Object>> getDefaultQuestions() {
        return assessmentService.getQuestions().stream().map(question -> Map.of(
            "id", question.id(),
            "prompt", question.questionText(),
            "category", question.traitCategory().toLowerCase(Locale.ROOT).replace('_', ' '),
            "options", question.options().stream().map(option -> Map.of(
                "key", option.key(),
                "label", option.text(),
                "description", "Contributes to " + question.traitCategory().toLowerCase(Locale.ROOT).replace('_', ' ')
            )).toList()
        )).toList();
    }

    @PostMapping("/default/submit")
    public Map<String, Object> submitDefaultAssessment(@AuthenticationPrincipal User user,
                                                       @Valid @RequestBody FrontendAssessmentSubmission request) {
        Map<Long, String> answers = new LinkedHashMap<>();
        for (FrontendAnswer answer : request.answers()) {
            answers.put(answer.questionId(), answer.selectedOption());
        }
        AssessmentDtos.AssessmentResultResponse response = assessmentService.submit(
            user,
            new AssessmentDtos.AssessmentSubmissionRequest(answers)
        );
        return toFrontendPayload(response);
    }

    @GetMapping("/results/latest")
    public Map<String, Object> getLatestResult(@AuthenticationPrincipal User user) {
        Optional<AssessmentDtos.AssessmentResultResponse> result = assessmentService.getLatest(user);
        return result.map(this::toFrontendPayload)
            .orElse(Map.of("summary", "Assessment not completed", "overallScore", 0, "traits", Map.of()));
    }

    private Map<String, Object> toFrontendPayload(AssessmentDtos.AssessmentResultResponse response) {
        Map<String, Integer> traits = Map.of(
            "analytical", response.scores().getOrDefault("analyticalThinking", 0),
            "creativity", response.scores().getOrDefault("creativity", 0),
            "leadership", response.scores().getOrDefault("leadership", 0),
            "technical", response.scores().getOrDefault("technicalInclination", 0),
            "communication", response.scores().getOrDefault("communication", 0),
            "problemSolving", response.scores().getOrDefault("problemSolving", 0)
        );
        double average = traits.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return Map.of(
            "summary", response.summary(),
            "overallScore", Math.round(average),
            "traits", traits,
            "createdAt", response.createdAt()
        );
    }

    public record FrontendAssessmentSubmission(
        @NotNull(message = "Answers are required")
        List<FrontendAnswer> answers
    ) {
    }

    public record FrontendAnswer(
        @NotNull(message = "Question id is required")
        Long questionId,
        @NotNull(message = "Selected option is required")
        String selectedOption
    ) {
    }
}
