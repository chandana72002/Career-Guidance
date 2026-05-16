package com.datavalley.careerguidance.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUser(User user);
}
