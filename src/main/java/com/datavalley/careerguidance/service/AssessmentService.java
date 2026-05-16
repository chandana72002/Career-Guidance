package com.datavalley.careerguidance.service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datavalley.careerguidance.dto.AssessmentDtos;
import com.datavalley.careerguidance.entity.AssessmentQuestion;
import com.datavalley.careerguidance.entity.AssessmentResult;
import com.datavalley.careerguidance.entity.TraitCategory;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.exception.BadRequestException;
import com.datavalley.careerguidance.exception.ResourceNotFoundException;
import com.datavalley.careerguidance.repository.AssessmentQuestionRepository;
import com.datavalley.careerguidance.repository.AssessmentResultRepository;

@Service
public class AssessmentService {

    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentResultRepository resultRepository;

    public AssessmentService(AssessmentQuestionRepository questionRepository,
                             AssessmentResultRepository resultRepository) {
        this.questionRepository = questionRepository;
        this.resultRepository = resultRepository;
    }

    public List<AssessmentDtos.AssessmentQuestionResponse> getQuestions() {
        return questionRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(AssessmentQuestion::getId))
            .map(question -> new AssessmentDtos.AssessmentQuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getTraitCategory().name(),
                List.of(
                    new AssessmentDtos.AssessmentOptionResponse("A", question.getOptionAText()),
                    new AssessmentDtos.AssessmentOptionResponse("B", question.getOptionBText()),
                    new AssessmentDtos.AssessmentOptionResponse("C", question.getOptionCText()),
                    new AssessmentDtos.AssessmentOptionResponse("D", question.getOptionDText())
                )
            ))
            .toList();
    }

    @Transactional
    public AssessmentDtos.AssessmentResultResponse submit(User user, AssessmentDtos.AssessmentSubmissionRequest request) {
        if (request.answers() == null || request.answers().isEmpty()) {
            throw new BadRequestException("Answers are required");
        }

        Map<TraitCategory, Integer> scores = new EnumMap<>(TraitCategory.class);
        for (TraitCategory category : TraitCategory.values()) {
            scores.put(category, 0);
        }

        for (Map.Entry<Long, String> entry : request.answers().entrySet()) {
            AssessmentQuestion question = questionRepository.findById(entry.getKey())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + entry.getKey()));
            int weight = resolveWeight(question, entry.getValue());
            scores.put(question.getTraitCategory(), scores.get(question.getTraitCategory()) + weight);
        }

        AssessmentResult result = new AssessmentResult();
        result.setUser(user);
        result.setAnalyticalThinking(scores.get(TraitCategory.ANALYTICAL_THINKING));
        result.setCreativity(scores.get(TraitCategory.CREATIVITY));
        result.setLeadership(scores.get(TraitCategory.LEADERSHIP));
        result.setTechnicalInclination(scores.get(TraitCategory.TECHNICAL_INCLINATION));
        result.setCommunication(scores.get(TraitCategory.COMMUNICATION));
        result.setProblemSolving(scores.get(TraitCategory.PROBLEM_SOLVING));
        result.setResultSummary(buildSummary(scores));
        AssessmentResult saved = resultRepository.save(result);
        return mapResult(saved);
    }

    public Optional<AssessmentDtos.AssessmentResultResponse> getLatest(User user) {
        return resultRepository.findTopByUserOrderByCreatedAtDesc(user).map(this::mapResult);
    }

    public Optional<AssessmentResult> getLatestEntity(User user) {
        return resultRepository.findTopByUserOrderByCreatedAtDesc(user);
    }

    private int resolveWeight(AssessmentQuestion question, String selectedOption) {
        if (selectedOption == null) {
            throw new BadRequestException("Missing answer for question " + question.getId());
        }
        return switch (selectedOption.trim().toUpperCase(Locale.ROOT)) {
            case "A" -> question.getOptionAWeight();
            case "B" -> question.getOptionBWeight();
            case "C" -> question.getOptionCWeight();
            case "D" -> question.getOptionDWeight();
            default -> throw new BadRequestException("Invalid option for question " + question.getId());
        };
    }

    private String buildSummary(Map<TraitCategory, Integer> scores) {
        List<TraitCategory> topTraits = scores.entrySet()
            .stream()
            .sorted(Map.Entry.<TraitCategory, Integer>comparingByValue().reversed())
            .limit(2)
            .map(Map.Entry::getKey)
            .toList();

        if (topTraits.isEmpty()) {
            return "Assessment pending";
        }
        if (topTraits.size() == 1) {
            return "Your strongest trait is " + formatTrait(topTraits.get(0)) + ".";
        }
        return "Your strongest traits are " + formatTrait(topTraits.get(0)) + " and " + formatTrait(topTraits.get(1)) + ".";
    }

    private String formatTrait(TraitCategory category) {
        return category.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private AssessmentDtos.AssessmentResultResponse mapResult(AssessmentResult result) {
        return new AssessmentDtos.AssessmentResultResponse(
            result.getCreatedAt(),
            result.getResultSummary(),
            Map.of(
                "analyticalThinking", result.getAnalyticalThinking(),
                "creativity", result.getCreativity(),
                "leadership", result.getLeadership(),
                "technicalInclination", result.getTechnicalInclination(),
                "communication", result.getCommunication(),
                "problemSolving", result.getProblemSolving()
            )
        );
    }
}
