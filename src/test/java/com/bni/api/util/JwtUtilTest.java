package com.bni.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // ✅ Tambahan ini untuk mengatasi unnecessary stubbing
class JwtUtilTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtUtil jwtUtil;
    private final String testSecret = "testSecretKey123456789012345678901234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ7890";
    private final int testExpiration = 3600000; // 1 hour
    private final int testRefreshExpiration = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        // Global mock setup - akan diabaikan jika tidak digunakan (karena LENIENT mode)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // Default: token not blacklisted
        
        // Create JwtUtil with mocked Redis
        jwtUtil = new JwtUtil(redisTemplate);
        
        // Set private fields using reflection
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
    void testValidateTokenWithBlacklistedToken() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        
        // Override default behavior for this specific test
        when(redisTemplate.hasKey("blacklisted_token:" + token)).thenReturn(true);
        
        assertFalse(jwtUtil.validateToken(token, username));
    }

    @Test
    void testIsTokenBlacklisted() {
        String token = "some.jwt.token";
        
        // Test token not blacklisted (uses default setup)
        assertFalse(jwtUtil.isTokenBlacklisted(token));
        
        // Test token is blacklisted (override for this test)
        when(redisTemplate.hasKey("blacklisted_token:" + token)).thenReturn(true);
        assertTrue(jwtUtil.isTokenBlacklisted(token));
    }

    @Test
    void testIsTokenBlacklistedWithNullToken() {
        assertFalse(jwtUtil.isTokenBlacklisted(null));
        assertFalse(jwtUtil.isTokenBlacklisted(""));
        assertFalse(jwtUtil.isTokenBlacklisted("   "));
    }

    @Test
    void testIsTokenBlacklistedWithRedisException() {
        String token = "some.jwt.token";
        
        when(redisTemplate.hasKey("blacklisted_token:" + token))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        assertFalse(jwtUtil.isTokenBlacklisted(token));
    }

    @Test
    void testValidateTokenWithRedisException() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        
        when(redisTemplate.hasKey("blacklisted_token:" + token))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        assertTrue(jwtUtil.validateToken(token, username));
    }

    @Test
    void testTokenExpirationDifference() {
        String username = "testuser";
        String accessToken = jwtUtil.generateToken(username);
        String refreshToken = jwtUtil.generateRefreshToken(username);
        
        Date accessExpiration = jwtUtil.extractExpiration(accessToken);
        Date refreshExpiration = jwtUtil.extractExpiration(refreshToken);
        
        assertTrue(refreshExpiration.after(accessExpiration));
        
        long timeDiff = refreshExpiration.getTime() - accessExpiration.getTime();
        long expectedDiff = testRefreshExpiration - testExpiration;
        
        assertTrue(Math.abs(timeDiff - expectedDiff) < 1000);
    }

    @Test
    void testValidateTokenWithExpiredToken() throws InterruptedException {
        JwtUtil shortExpiryJwtUtil = new JwtUtil(redisTemplate);
        ReflectionTestUtils.setField(shortExpiryJwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(shortExpiryJwtUtil, "jwtExpiration", 1);
        ReflectionTestUtils.setField(shortExpiryJwtUtil, "jwtRefreshExpiration", testRefreshExpiration);
        
        String username = "testuser";
        String token = shortExpiryJwtUtil.generateToken(username);
        
        Thread.sleep(10);
        
        assertFalse(shortExpiryJwtUtil.validateToken(token, username));
    }

    @Test
    void testValidateTokenWithInvalidToken() {
        String username = "testuser";
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtUtil.validateToken(invalidToken, username));
    }

    @Test
    void testExtractUsernameFromInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> {
            jwtUtil.extractUsername(invalidToken);
        });
    }
}