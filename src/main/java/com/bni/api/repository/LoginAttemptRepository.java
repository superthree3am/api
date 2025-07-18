package com.bni.api.repository;

import com.bni.api.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    Optional<LoginAttempt> findByUserId(Long userId);
}