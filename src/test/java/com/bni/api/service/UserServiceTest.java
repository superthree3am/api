package com.bni.api.service;

import com.bni.api.dto.UserProfileResponse;
import com.bni.api.entity.LoginAttempt;
import com.bni.api.entity.User;
import com.bni.api.repository.LoginAttemptRepository;
import com.bni.api.repository.UserRepository;
import com.bni.api.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;
import com.github.f4b6a3.ulid.UlidCreator;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String testUserUlid; 
    private String testLoginAttemptUlid; 

    @BeforeEach
    void setUp() {
        testUserUlid = UlidCreator.getUlid().toString(); 
        testLoginAttemptUlid = UlidCreator.getUlid().toString();

        testUser = new User("testuser", "test@example.com", "+6281234567890", "Test User", "hashedpassword");
        testUser.setId(testUserUlid); 

    }

    @Test
    void testRegisterSuccess() throws Exception {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+6281234567890")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.register("testuser", "test@example.com", "+6281234567890", "Test User", "password");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUsernameExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> {
            userService.register("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        });
    }

    @Test
    void testRegisterEmailExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> {
            userService.register("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        });
    }

    @Test
    void testRegisterPhoneExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+6281234567890")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> {
            userService.register("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        });
    }

    @Test
    void testAuthenticateUserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            userService.authenticateUser("testuser", "password");
        });
    }

    @Test
    void testAuthenticateUserAccountLocked() {
        LoginAttempt lockedAttempt = new LoginAttempt();
        lockedAttempt.setUserId(testUserUlid);
        lockedAttempt.setFailedAttempts(3);
        lockedAttempt.setLockedUntil(LocalDateTime.now().plusHours(1));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.of(lockedAttempt));

        assertThrows(ResponseStatusException.class, () -> {
            userService.authenticateUser("testuser", "wrongpassword");
        });
    }

    @Test
    void testGetUserProfile() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserProfileResponse result = userService.getUserProfile("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("+6281234567890", result.getPhone());
        assertEquals("Test User", result.getFullName());
    }

    @Test
    void testGetUserProfileNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            userService.getUserProfile("testuser");
        });
    }

    @Test
    void testLoadUserByUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails result = userService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("hashedpassword", result.getPassword());
    }

    @Test
    void testLoadUserByUsernameNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("testuser");
        });
    }
}