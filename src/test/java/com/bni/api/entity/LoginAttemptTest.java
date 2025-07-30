package com.bni.api.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptTest {

    private LoginAttempt loginAttempt;

    @BeforeEach
    void setUp() {
        loginAttempt = new LoginAttempt();
    }

    @Test
    void testSettersAndGetters() {
        String id = "abc123";
        String userId = "user1";
        int failedAttempts = 3;
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(15);
        LocalDateTime lastFailedAttempt = LocalDateTime.now();

        loginAttempt.setId(id);
        loginAttempt.setUserId(userId);
        loginAttempt.setFailedAttempts(failedAttempts);
        loginAttempt.setLockedUntil(lockedUntil);
        loginAttempt.setLastFailedAttempt(lastFailedAttempt);

        assertEquals(id, loginAttempt.getId());
        assertEquals(userId, loginAttempt.getUserId());
        assertEquals(failedAttempts, loginAttempt.getFailedAttempts());
        assertEquals(lockedUntil, loginAttempt.getLockedUntil());
        assertEquals(lastFailedAttempt, loginAttempt.getLastFailedAttempt());
    }

    @Test
    void testAllArgsConstructor() {
        String userId = "user2";
        int failedAttempts = 5;
        LocalDateTime lockedUntil = LocalDateTime.now().plusHours(1);
        LocalDateTime lastFailedAttempt = LocalDateTime.now().minusMinutes(2);

        LoginAttempt attempt = new LoginAttempt(userId, failedAttempts, lockedUntil, lastFailedAttempt);

        assertEquals(userId, attempt.getUserId());
        assertEquals(failedAttempts, attempt.getFailedAttempts());
        assertEquals(lockedUntil, attempt.getLockedUntil());
        assertEquals(lastFailedAttempt, attempt.getLastFailedAttempt());
    }
}

