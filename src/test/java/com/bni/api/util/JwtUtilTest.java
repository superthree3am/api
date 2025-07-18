package com.bni.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "testSecretKey123456789012345678901234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ7890";
    private final int testExpiration = 3600000; // 1 hour
    private final int testRefreshExpiration = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", testExpiration);
        ReflectionTestUtils.setField(jwtUtil, "jwtRefreshExpiration", testRefreshExpiration);
    }

    @Test
    void testGenerateToken() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(username, jwtUtil.extractUsername(token));
    }

    @Test
    void testGenerateRefreshToken() {
        String username = "testuser";
        String refreshToken = jwtUtil.generateRefreshToken(username);
        
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertEquals(username, jwtUtil.extractUsername(refreshToken));
    }

    @Test
    void testExtractUsername() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        
        assertEquals(username, jwtUtil.extractUsername(token));
    }

    @Test
    void testExtractExpiration() {
        String token = jwtUtil.generateToken("testuser");
        Date expiration = jwtUtil.extractExpiration(token);
        
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void testValidateToken() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        
        assertTrue(jwtUtil.validateToken(token, username));
        assertFalse(jwtUtil.validateToken(token, "wronguser"));
    }

    @Test
    void testTokenExpirationDifference() {
        String username = "testuser";
        String accessToken = jwtUtil.generateToken(username);
        String refreshToken = jwtUtil.generateRefreshToken(username);
        
        Date accessExpiration = jwtUtil.extractExpiration(accessToken);
        Date refreshExpiration = jwtUtil.extractExpiration(refreshToken);
        
        assertTrue(refreshExpiration.after(accessExpiration));
    }
}