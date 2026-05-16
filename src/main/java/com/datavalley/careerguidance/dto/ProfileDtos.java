package com.datavalley.careerguidance.dto;

import java.math.BigDecimal;
import java.util.List;
import com.datavalley.careerguidance.entity.EducationLevel;
import com.datavalley.careerguidance.entity.PersonalityType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record SkillSelectionRequest(
        @NotNull(message = "Skill id is required")
        Long skillId,
        @NotNull(message = "Proficiency is required")
        @Min(value = 1, message = "Proficiency must be between 1 and 5")
        @Max(value = 5, message = "Proficiency must be between 1 and 5")
        Integer proficiencyLevel
    ) {
    }

    public record SkillResponse(
        Long id,
        String name,
        String category
    ) {
    }

    public record SelectedSkillResponse(
        Long id,
        String name,
        String category,
        Integer proficiencyLevel
    ) {
    }

    public record ProfileRequest(
        Integer age,
        EducationLevel educationLevel,
        String course,
        String currentYear,
        BigDecimal cgpa,
        String preferredWorkType,
        String preferredIndustry,
        PersonalityType personalityType,
        String longTermGoal,
        List<String> interests,
        List<String> strengths,
        List<String> weaknesses,
        List<SkillSelectionRequest> skills
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FreeFormProfileRequest(
        String fullName,
        Integer age,
        EducationLevel educationLevel,
        String course,
        String currentYear,
        BigDecimal cgpa,
        String preferredWorkType,
        String preferredIndustry,
        PersonalityType personalityType,
        String longTermGoal,
        List<String> interests,
        List<String> skills,
        String strengths,
        String weaknesses
    ) {
    }

    public record ProfileResponse(
        String fullName,
        String email,
        Integer age,
        EducationLevel educationLevel,
        String course,
        String currentYear,
        BigDecimal cgpa,
        String preferredWorkType,
        String preferredIndustry,
        PersonalityType personalityType,
        String longTermGoal,
        List<String> interests,
        List<String> strengths,
        List<String> weaknesses,
        List<SelectedSkillResponse> skills,
        Integer profileCompletion
    ) {
    }

    public record DashboardResponse(
        Integer profileCompletion,
        String latestAssessmentSummary,
        Integer latestAssessmentQuestionsAttempted,
        String topCareer,
        Double topCareerScore,
        Integer savedCareerCount,
        Integer recommendationCount
    ) {
    }
}
