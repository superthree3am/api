package com.bni.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileResponseTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        UserProfileResponse response = new UserProfileResponse();
        response.setUsername("johndoe");
        response.setEmail("john@example.com");
        response.setPhone("08123456789");
        response.setFullName("John Doe");

        assertEquals("johndoe", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("08123456789", response.getPhone());
        assertEquals("John Doe", response.getFullName());
    }

    @Test
    void testAllArgsConstructor() {
        UserProfileResponse response = new UserProfileResponse(
            "janedoe",
            "jane@example.com",
            "08234567890",
            "Jane Doe"
        );

        assertEquals("janedoe", response.getUsername());
        assertEquals("jane@example.com", response.getEmail());
        assertEquals("08234567890", response.getPhone());
        assertEquals("Jane Doe", response.getFullName());
    }
}