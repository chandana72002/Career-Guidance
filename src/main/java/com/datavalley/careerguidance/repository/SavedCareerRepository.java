package com.datavalley.careerguidance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.SavedCareer;
import com.datavalley.careerguidance.entity.User;

public interface SavedCareerRepository extends JpaRepository<SavedCareer, Long> {

    List<SavedCareer> findByUserOrderBySavedAtDesc(User user);

    Optional<SavedCareer> findByUserAndCareer(User user, Career career);

    void deleteByCareer(Career career);
}
