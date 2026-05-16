package com.datavalley.careerguidance.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datavalley.careerguidance.dto.ProfileDtos;
import com.datavalley.careerguidance.entity.Skill;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.entity.UserSkill;
import com.datavalley.careerguidance.exception.ResourceNotFoundException;
import com.datavalley.careerguidance.repository.SkillRepository;
import com.datavalley.careerguidance.repository.UserProfileRepository;
import com.datavalley.careerguidance.repository.UserRepository;
import com.datavalley.careerguidance.repository.UserSkillRepository;

@Service
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    public ProfileService(UserProfileRepository userProfileRepository,
                          UserSkillRepository userSkillRepository,
                          SkillRepository skillRepository,
                          UserRepository userRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userSkillRepository = userSkillRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
    }

    public List<ProfileDtos.SkillResponse> getAllSkills() {
        return skillRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(Skill::getName))
            .map(skill -> new ProfileDtos.SkillResponse(skill.getId(), skill.getName(), skill.getCategory()))
            .toList();
    }

    public ProfileDtos.ProfileResponse getProfile(User user) {
        UserProfile profile = userProfileRepository.findByUser(user)
            .orElseGet(() -> createEmptyProfile(user));
        List<UserSkill> userSkills = userSkillRepository.findByUser(user);
        return mapProfile(profile, userSkills);
    }

    @Transactional
    public ProfileDtos.ProfileResponse updateProfile(User user, ProfileDtos.ProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUser(user)
            .orElseGet(() -> createEmptyProfile(user));

        profile.setAge(request.age());
        profile.setEducationLevel(request.educationLevel());
        profile.setCourse(trimToNull(request.course()));
        profile.setCurrentYear(trimToNull(request.currentYear()));
        profile.setCgpa(request.cgpa());
        profile.setPreferredWorkType(trimToNull(request.preferredWorkType()));
        profile.setPreferredIndustry(trimToNull(request.preferredIndustry()));
        profile.setPersonalityType(request.personalityType());
        profile.setLongTermGoal(trimToNull(request.longTermGoal()));
        profile.setInterests(toCleanSet(request.interests()));
        profile.setStrengths(toCleanSet(request.strengths()));
        profile.setWeaknesses(toCleanSet(request.weaknesses()));
        userProfileRepository.save(profile);

        userSkillRepository.deleteByUser(user);
        List<UserSkill> savedSkills = new ArrayList<>();
        if (request.skills() != null) {
            for (ProfileDtos.SkillSelectionRequest selection : request.skills()) {
                Skill skill = skillRepository.findById(selection.skillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + selection.skillId()));
                UserSkill userSkill = new UserSkill();
                userSkill.setUser(user);
                userSkill.setSkill(skill);
                userSkill.setProficiencyLevel(selection.proficiencyLevel());
                savedSkills.add(userSkillRepository.save(userSkill));
            }
        }

        return mapProfile(profile, savedSkills);
    }

    @Transactional
    public ProfileDtos.ProfileResponse updateProfile(User user, ProfileDtos.FreeFormProfileRequest request) {
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
            userRepository.save(user);
        }

        UserProfile profile = userProfileRepository.findByUser(user)
            .orElseGet(() -> createEmptyProfile(user));

        profile.setAge(request.age());
        profile.setEducationLevel(request.educationLevel());
        profile.setCourse(trimToNull(request.course()));
        profile.setCurrentYear(trimToNull(request.currentYear()));
        profile.setCgpa(request.cgpa());
        profile.setPreferredWorkType(trimToNull(request.preferredWorkType()));
        profile.setPreferredIndustry(trimToNull(request.preferredIndustry()));
        profile.setPersonalityType(request.personalityType());
        profile.setLongTermGoal(trimToNull(request.longTermGoal()));
        profile.setInterests(toCleanSet(request.interests()));
        profile.setStrengths(splitText(request.strengths()));
        profile.setWeaknesses(splitText(request.weaknesses()));
        userProfileRepository.save(profile);

        userSkillRepository.deleteByUser(user);
        List<UserSkill> savedSkills = new ArrayList<>();
        if (request.skills() != null) {
            for (String skillName : request.skills()) {
                String normalized = trimToNull(skillName);
                if (normalized == null) {
                    continue;
                }
                Skill skill = skillRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> {
                        Skill newSkill = new Skill();
                        newSkill.setName(normalized);
                        newSkill.setCategory("User Added");
                        return skillRepository.save(newSkill);
                    });

                UserSkill userSkill = new UserSkill();
                userSkill.setUser(user);
                userSkill.setSkill(skill);
                userSkill.setProficiencyLevel(3);
                savedSkills.add(userSkillRepository.save(userSkill));
            }
        }

        return mapProfile(profile, savedSkills);
    }

    private UserProfile createEmptyProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        return userProfileRepository.save(profile);
    }

    private ProfileDtos.ProfileResponse mapProfile(UserProfile profile, List<UserSkill> userSkills) {
        List<ProfileDtos.SelectedSkillResponse> skillResponses = userSkills.stream()
            .sorted(Comparator.comparing(userSkill -> userSkill.getSkill().getName()))
            .map(userSkill -> new ProfileDtos.SelectedSkillResponse(
                userSkill.getSkill().getId(),
                userSkill.getSkill().getName(),
                userSkill.getSkill().getCategory(),
                userSkill.getProficiencyLevel()
            ))
            .toList();

        return new ProfileDtos.ProfileResponse(
            profile.getUser().getFullName(),
            profile.getUser().getEmail(),
            profile.getAge(),
            profile.getEducationLevel(),
            profile.getCourse(),
            profile.getCurrentYear(),
            profile.getCgpa(),
            profile.getPreferredWorkType(),
            profile.getPreferredIndustry(),
            profile.getPersonalityType(),
            profile.getLongTermGoal(),
            new ArrayList<>(profile.getInterests()),
            new ArrayList<>(profile.getStrengths()),
            new ArrayList<>(profile.getWeaknesses()),
            skillResponses,
            calculateProfileCompletion(profile, userSkills)
        );
    }

    public int calculateProfileCompletion(UserProfile profile, List<UserSkill> userSkills) {
        int total = 10;
        int completed = 0;
        completed += profile.getAge() != null ? 1 : 0;
        completed += profile.getEducationLevel() != null ? 1 : 0;
        completed += profile.getCourse() != null ? 1 : 0;
        completed += profile.getCgpa() != null ? 1 : 0;
        completed += profile.getPreferredWorkType() != null ? 1 : 0;
        completed += profile.getPreferredIndustry() != null ? 1 : 0;
        completed += profile.getPersonalityType() != null ? 1 : 0;
        completed += !profile.getInterests().isEmpty() ? 1 : 0;
        completed += !profile.getStrengths().isEmpty() ? 1 : 0;
        completed += !userSkills.isEmpty() ? 1 : 0;
        return (int) Math.round((completed * 100.0) / total);
    }

    private Set<String> toCleanSet(List<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> splitText(String value) {
        if (value == null || value.isBlank()) {
            return new LinkedHashSet<>();
        }
        String[] parts = value.split("[,\\n]+");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            values.add(part);
        }
        return toCleanSet(values);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
