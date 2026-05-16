package com.datavalley.careerguidance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.CareerSkill;

public interface CareerSkillRepository extends JpaRepository<CareerSkill, Long> {

    List<CareerSkill> findByCareer(Career career);

    void deleteByCareer(Career career);
}
