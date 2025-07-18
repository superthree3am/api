package com.bni.api;

import com.bni.api.util.JwtUtil;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private JwtUtil jwtUtil;

    private String encodedKey;
    private final int testExpiration = 1000 * 60 * 60;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // ✅ Generate secure secret key using jjwt
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        encodedKey = Encoders.BASE64.encode(key.getEncoded());

        // ✅ Inject encoded key string into JwtUtil
        ReflectionTestUtils.setField(jwtUtil, "secret", encodedKey);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", testExpiration);
    }

    @Test
    void testGenerateAndExtractUsername() {
        String username = "roland";
        String token = jwtUtil.generateToken(username);
        assertNotNull(token);

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void testTokenValidationSuccess() {
        String username = "roland";
        String token = jwtUtil.generateToken(username);
        assertTrue(jwtUtil.validateToken(token, username));
    }

    @Test
    void testTokenValidationFailsOnWrongUsername() {
        String token = jwtUtil.generateToken("roland");
        assertFalse(jwtUtil.validateToken(token, "wrongUser"));
    }

    @Test
    void testExtractExpiration() {
        String token = jwtUtil.generateToken("roland");
        assertNotNull(jwtUtil.extractExpiration(token));
        assertTrue(jwtUtil.extractExpiration(token).after(new java.util.Date()));
    }
}