package com.datavalley.careerguidance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserSkill;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByUser(User user);

    void deleteByUser(User user);
}
