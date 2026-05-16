package com.datavalley.careerguidance.dto;

import java.util.List;
import com.datavalley.careerguidance.entity.EducationLevel;
import com.datavalley.careerguidance.entity.PersonalityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class CareerDtos {

    private CareerDtos() {
    }

    public record CareerSkillWeightRequest(
        @NotNull(message = "Skill id is required")
        Long skillId,
        @NotNull(message = "Importance weight is required")
        Integer importanceWeight
    ) {
    }

    public record CareerSkillResponse(
        Long skillId,
        String skillName,
        String category,
        Integer importanceWeight
    ) {
    }

    public record LearningResourceResponse(
        Long id,
        String title,
        String type,
        String url,
        String description
    ) {
    }

    public record CareerResponse(
        Long id,
        String name,
        String domain,
        String description,
        EducationLevel requiredEducation,
        String futureScope,
        String salaryRange,
        PersonalityType preferredPersonality,
        Integer analyticalWeight,
        Integer creativityWeight,
        Integer leadershipWeight,
        Integer technicalWeight,
        Integer communicationWeight,
        Integer problemSolvingWeight,
        List<String> relatedIndustries,
        List<String> roadmapSteps,
        List<String> recommendedCertifications,
        List<CareerSkillResponse> skills,
        List<LearningResourceResponse> resources
    ) {
    }

    public record CareerUpsertRequest(
        @NotBlank(message = "Career name is required")
        String name,
        @NotBlank(message = "Career domain is required")
        String domain,
        @NotBlank(message = "Career description is required")
        String description,
        @NotNull(message = "Required education is required")
        EducationLevel requiredEducation,
        @NotBlank(message = "Future scope is required")
        String futureScope,
        @NotBlank(message = "Salary range is required")
        String salaryRange,
        @NotNull(message = "Preferred personality is required")
        PersonalityType preferredPersonality,
        Integer analyticalWeight,
        Integer creativityWeight,
        Integer leadershipWeight,
        Integer technicalWeight,
        Integer communicationWeight,
        Integer problemSolvingWeight,
        List<String> relatedIndustries,
        List<String> roadmapSteps,
        List<String> recommendedCertifications,
        List<CareerSkillWeightRequest> skills
    ) {
    }
}
