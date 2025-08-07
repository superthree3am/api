package com.bni.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterResponseTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        RegisterResponse response = new RegisterResponse();
        response.setStatus(201);
        response.setMessage("Registered successfully");

        assertEquals(201, response.getStatus());
        assertEquals("Registered successfully", response.getMessage());
    }

    @Test
    void testAllArgsConstructor() {
        RegisterResponse response = new RegisterResponse(400, "User already exists");

        assertEquals(400, response.getStatus());
        assertEquals("User already exists", response.getMessage());
    }
}
