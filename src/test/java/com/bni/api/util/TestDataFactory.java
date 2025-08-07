package com.bni.api.util;

import com.bni.api.dto.LoginRequest;
import com.bni.api.dto.RegisterRequest;
import com.bni.api.entity.LoginAttempt;
import com.bni.api.entity.User;

import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createValidUser() {
        return new User("testuser", "test@example.com", "+6281234567890", "Test User", "hashedpassword");
    }

    public static User createUserWithId(String id) {
        User user = createValidUser();
        user.setId(id);
        return user;
    }

    public static RegisterRequest createValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPhone("+6281234567890");
        request.setFullName("Test User");
        request.setPassword("Password123!");
        return request;
    }

    public static RegisterRequest createInvalidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab"); // Too short
        request.setEmail("invalid-email");
        request.setPhone("081234567890"); // Missing +62
        request.setFullName("Test User");
        request.setPassword("weak");
        return request;
    }

    public static LoginRequest createValidLoginRequest() {
        return new LoginRequest("testuser", "Password123!");
    }

    public static LoginRequest createInvalidLoginRequest() {
        return new LoginRequest("", "weak");
    }

    public static LoginAttempt createLoginAttempt(String userId, int failedAttempts) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(userId);
        attempt.setFailedAttempts(failedAttempts);
        attempt.setLastFailedAttempt(LocalDateTime.now());
        return attempt;
    }

    public static LoginAttempt createLockedLoginAttempt(String userId) {
        LoginAttempt attempt = createLoginAttempt(userId, 3);
        attempt.setLockedUntil(LocalDateTime.now().plusHours(24));
        return attempt;
    }
}
