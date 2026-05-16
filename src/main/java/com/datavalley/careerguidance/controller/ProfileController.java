package com.datavalley.careerguidance.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datavalley.careerguidance.dto.ProfileDtos;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.service.ProfileService;

@Validated
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/skills")
    public List<ProfileDtos.SkillResponse> getSkills() {
        return profileService.getAllSkills();
    }

    @GetMapping("/profile/me")
    public Map<String, Object> getProfile(@AuthenticationPrincipal User user) {
        return toFrontendProfile(profileService.getProfile(user));
    }

    @PutMapping("/profile/me")
    public Map<String, Object> updateProfile(@AuthenticationPrincipal User user,
                                             @RequestBody ProfileDtos.FreeFormProfileRequest request) {
        return toFrontendProfile(profileService.updateProfile(user, request));
    }

    private Map<String, Object> toFrontendProfile(ProfileDtos.ProfileResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", response.fullName());
        payload.put("email", response.email());
        payload.put("age", response.age());
        payload.put("educationLevel", response.educationLevel());
        payload.put("course", response.course());
        payload.put("currentYear", response.currentYear());
        payload.put("cgpa", response.cgpa());
        payload.put("preferredWorkType", response.preferredWorkType());
        payload.put("preferredIndustry", response.preferredIndustry());
        payload.put("personalityType", response.personalityType());
        payload.put("longTermGoal", response.longTermGoal());
        payload.put("interests", response.interests());
        payload.put("skills", response.skills().stream().map(ProfileDtos.SelectedSkillResponse::name).toList());
        payload.put("strengths", String.join(", ", response.strengths()));
        payload.put("weaknesses", String.join(", ", response.weaknesses()));
        payload.put("profileCompletion", response.profileCompletion());
        return payload;
    }
}
