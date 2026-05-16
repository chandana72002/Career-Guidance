package com.datavalley.careerguidance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.AssessmentResult;
import com.datavalley.careerguidance.entity.User;

public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {

    Optional<AssessmentResult> findTopByUserOrderByCreatedAtDesc(User user);

    List<AssessmentResult> findByUserOrderByCreatedAtDesc(User user);
}
