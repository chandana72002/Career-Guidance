package com.datavalley.careerguidance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.LearningResource;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    List<LearningResource> findByCareer(Career career);

    void deleteByCareer(Career career);
}
