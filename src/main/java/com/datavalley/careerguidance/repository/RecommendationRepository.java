package com.datavalley.careerguidance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.Recommendation;
import com.datavalley.careerguidance.entity.User;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserOrderByCompatibilityScoreDesc(User user);

    void deleteByUser(User user);

    void deleteByCareer(Career career);
}
