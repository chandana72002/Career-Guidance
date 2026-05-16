package com.datavalley.careerguidance.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datavalley.careerguidance.dto.CareerDtos;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.CareerSkill;
import com.datavalley.careerguidance.entity.LearningResource;
import com.datavalley.careerguidance.entity.Skill;
import com.datavalley.careerguidance.exception.ResourceNotFoundException;
import com.datavalley.careerguidance.repository.CareerRepository;
import com.datavalley.careerguidance.repository.CareerSkillRepository;
import com.datavalley.careerguidance.repository.LearningResourceRepository;
import com.datavalley.careerguidance.repository.RecommendationRepository;
import com.datavalley.careerguidance.repository.SavedCareerRepository;
import com.datavalley.careerguidance.repository.SkillRepository;

@Service
public class CareerService {

    private final CareerRepository careerRepository;
    private final CareerSkillRepository careerSkillRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final SkillRepository skillRepository;
    private final RecommendationRepository recommendationRepository;
    private final SavedCareerRepository savedCareerRepository;

    public CareerService(CareerRepository careerRepository,
                         CareerSkillRepository careerSkillRepository,
                         LearningResourceRepository learningResourceRepository,
                         SkillRepository skillRepository,
                         RecommendationRepository recommendationRepository,
                         SavedCareerRepository savedCareerRepository) {
        this.careerRepository = careerRepository;
        this.careerSkillRepository = careerSkillRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.skillRepository = skillRepository;
        this.recommendationRepository = recommendationRepository;
        this.savedCareerRepository = savedCareerRepository;
    }

    public List<CareerDtos.CareerResponse> getAllCareers() {
        return careerRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(Career::getName))
            .map(this::mapCareer)
            .toList();
    }

    public CareerDtos.CareerResponse getCareer(Long careerId) {
        Career career = careerRepository.findById(careerId)
            .orElseThrow(() -> new ResourceNotFoundException("Career not found"));
        return mapCareer(career);
    }

    public List<CareerDtos.LearningResourceResponse> getResources(Long careerId) {
        Career career = careerRepository.findById(careerId)
            .orElseThrow(() -> new ResourceNotFoundException("Career not found"));
        return learningResourceRepository.findByCareer(career)
            .stream()
            .map(this::mapResource)
            .toList();
    }

    @Transactional
    public CareerDtos.CareerResponse createCareer(CareerDtos.CareerUpsertRequest request) {
        Career career = new Career();
        applyCareerRequest(career, request);
        Career saved = careerRepository.save(career);
        syncCareerSkills(saved, request.skills());
        return mapCareer(saved);
    }

    @Transactional
    public CareerDtos.CareerResponse updateCareer(Long careerId, CareerDtos.CareerUpsertRequest request) {
        Career career = careerRepository.findById(careerId)
            .orElseThrow(() -> new ResourceNotFoundException("Career not found"));
        applyCareerRequest(career, request);
        Career saved = careerRepository.save(career);
        syncCareerSkills(saved, request.skills());
        return mapCareer(saved);
    }

    @Transactional
    public void deleteCareer(Long careerId) {
        Career career = careerRepository.findById(careerId)
            .orElseThrow(() -> new ResourceNotFoundException("Career not found"));
        recommendationRepository.deleteByCareer(career);
        savedCareerRepository.deleteByCareer(career);
        learningResourceRepository.deleteByCareer(career);
        careerSkillRepository.deleteByCareer(career);
        careerRepository.delete(career);
    }

    private void applyCareerRequest(Career career, CareerDtos.CareerUpsertRequest request) {
        career.setName(request.name().trim());
        career.setDomain(request.domain().trim());
        career.setDescription(request.description().trim());
        career.setRequiredEducation(request.requiredEducation());
        career.setFutureScope(request.futureScope().trim());
        career.setSalaryRange(request.salaryRange().trim());
        career.setPreferredPersonality(request.preferredPersonality());
        career.setAnalyticalWeight(defaultWeight(request.analyticalWeight()));
        career.setCreativityWeight(defaultWeight(request.creativityWeight()));
        career.setLeadershipWeight(defaultWeight(request.leadershipWeight()));
        career.setTechnicalWeight(defaultWeight(request.technicalWeight()));
        career.setCommunicationWeight(defaultWeight(request.communicationWeight()));
        career.setProblemSolvingWeight(defaultWeight(request.problemSolvingWeight()));
        career.setRelatedIndustries(cleanSet(request.relatedIndustries()));
        career.setRoadmapSteps(cleanSet(request.roadmapSteps()));
        career.setRecommendedCertifications(cleanSet(request.recommendedCertifications()));
    }

    private void syncCareerSkills(Career career, List<CareerDtos.CareerSkillWeightRequest> skillRequests) {
        careerSkillRepository.deleteByCareer(career);
        if (skillRequests == null) {
            return;
        }
        for (CareerDtos.CareerSkillWeightRequest request : skillRequests) {
            Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + request.skillId()));
            CareerSkill careerSkill = new CareerSkill();
            careerSkill.setCareer(career);
            careerSkill.setSkill(skill);
            careerSkill.setImportanceWeight(defaultWeight(request.importanceWeight()));
            careerSkillRepository.save(careerSkill);
        }
    }

    private CareerDtos.CareerResponse mapCareer(Career career) {
        List<CareerDtos.CareerSkillResponse> skills = careerSkillRepository.findByCareer(career)
            .stream()
            .sorted(Comparator.comparing(careerSkill -> careerSkill.getSkill().getName()))
            .map(careerSkill -> new CareerDtos.CareerSkillResponse(
                careerSkill.getSkill().getId(),
                careerSkill.getSkill().getName(),
                careerSkill.getSkill().getCategory(),
                careerSkill.getImportanceWeight()
            ))
            .toList();

        List<CareerDtos.LearningResourceResponse> resources = learningResourceRepository.findByCareer(career)
            .stream()
            .map(this::mapResource)
            .toList();

        return new CareerDtos.CareerResponse(
            career.getId(),
            career.getName(),
            career.getDomain(),
            career.getDescription(),
            career.getRequiredEducation(),
            career.getFutureScope(),
            career.getSalaryRange(),
            career.getPreferredPersonality(),
            career.getAnalyticalWeight(),
            career.getCreativityWeight(),
            career.getLeadershipWeight(),
            career.getTechnicalWeight(),
            career.getCommunicationWeight(),
            career.getProblemSolvingWeight(),
            new ArrayList<>(career.getRelatedIndustries()),
            new ArrayList<>(career.getRoadmapSteps()),
            new ArrayList<>(career.getRecommendedCertifications()),
            skills,
            resources
        );
    }

    private CareerDtos.LearningResourceResponse mapResource(LearningResource resource) {
        return new CareerDtos.LearningResourceResponse(
            resource.getId(),
            resource.getTitle(),
            resource.getType(),
            resource.getUrl(),
            resource.getDescription()
        );
    }

    private Set<String> cleanSet(List<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private int defaultWeight(Integer value) {
        return value == null ? 0 : value;
    }
}
