package com.datavalley.careerguidance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.AssessmentQuestion;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {
}
