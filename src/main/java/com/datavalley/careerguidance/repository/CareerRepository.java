package com.datavalley.careerguidance.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.Career;

public interface CareerRepository extends JpaRepository<Career, Long> {

    Optional<Career> findByNameIgnoreCase(String name);
}
