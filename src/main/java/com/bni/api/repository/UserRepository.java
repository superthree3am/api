package com.bni.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.bni.api.entity.User;

// Finding existing data(s)
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByUsername(String username);
}