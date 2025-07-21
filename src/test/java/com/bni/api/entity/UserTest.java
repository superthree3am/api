package com.bni.api.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void testUserCreation() {
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("+6281234567890");
        user.setFull_name("Test User");
        user.setPassword("hashedpassword");

        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("+6281234567890", user.getPhone());
        assertEquals("Test User", user.getFull_name());
        assertEquals("hashedpassword", user.getPassword());
    }

    @Test
    void testUserConstructor() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("+6281234567890", user.getPhone());
        assertEquals("Test User", user.getFull_name());
        assertEquals("password", user.getPassword());
    }

    @Test
    void testPrePersist() {
        LocalDateTime before = LocalDateTime.now();
        user.onCreate();
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(user.getCreatedAt());
        assertTrue(user.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(user.getCreatedAt().isBefore(after.plusSeconds(1)));
    }
}