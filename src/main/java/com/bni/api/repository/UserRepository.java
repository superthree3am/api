// src/main/java/com/bni/api/repository/UserRepository.java
package com.bni.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.bni.api.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone); // Tambahkan ini
}