package com.bni.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        LoginResponse response = new LoginResponse();
        response.setStatus(200);
        response.setMessage("Login successful");
        response.setToken("access-token");
        response.setUsername("testuser");
        response.setRefreshToken("refresh-token");

        assertEquals(200, response.getStatus());
        assertEquals("Login successful", response.getMessage());
        assertEquals("access-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void testAllArgsConstructor() {
        LoginResponse response = new LoginResponse(
            201,
            "Created",
            "access-token-123",
            "john_doe",
            "refresh-token-456"
        );

        assertEquals(201, response.getStatus());
        assertEquals("Created", response.getMessage());
        assertEquals("access-token-123", response.getToken());
        assertEquals("john_doe", response.getUsername());
        assertEquals("refresh-token-456", response.getRefreshToken());
    }
}

