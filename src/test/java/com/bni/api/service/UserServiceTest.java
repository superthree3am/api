package com.bni.api.service;

import com.bni.api.dto.LoginResponse;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;
import com.github.f4b6a3.ulid.UlidCreator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;

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

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String testUserUlid;

    @BeforeEach
    void setUp() {
        testUserUlid = UlidCreator.getUlid().toString();

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
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.register("testuser", "test@example.com", "+6281234567890", "Test User", "password"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Username already exists"));
    }

    @Test
    void testAuthenticateUserSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("password", "hashedpassword")).thenReturn(true);

        Map<String, Object> response = userService.authenticateUser("testuser", "password");

        assertEquals("Authentication successful. Proceed with OTP verification.", response.get("message"));
        assertEquals("testuser", response.get("username"));
        assertEquals(testUser.getPhone(), response.get("phoneNumber"));
    }

    @Test
    void testAuthenticateUserInvalidUsername() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.authenticateUser("unknown", "password"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Invalid username or password"));
    }

    @Test
    void testAuthenticateUserWrongPasswordIncrementsAttempt() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrongpass", "hashedpassword")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.authenticateUser("testuser", "wrongpass"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Invalid username or password"));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    void testAuthenticateUserExpiredLockShouldResetAttempts() {
        LoginAttempt expiredAttempt = new LoginAttempt();
        expiredAttempt.setUserId(testUserUlid);
        expiredAttempt.setFailedAttempts(3);
        expiredAttempt.setLockedUntil(LocalDateTime.now().minusHours(1));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.of(expiredAttempt));
        when(passwordEncoder.matches("password", "hashedpassword")).thenReturn(true);

        Map<String, Object> response = userService.authenticateUser("testuser", "password");

        assertEquals("Authentication successful. Proceed with OTP verification.", response.get("message"));
        verify(loginAttemptRepository, times(2)).save(any(LoginAttempt.class));

    }

    @Test
    void testAuthenticateUserThirdFailureLocksAccount() {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setUserId(testUserUlid);
        loginAttempt.setFailedAttempts(2); // this is the 3rd fail
        loginAttempt.setLockedUntil(null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.of(loginAttempt));
        when(passwordEncoder.matches("wrongpass", "hashedpassword")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.authenticateUser("testuser", "wrongpass");
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Fail Login 3 times. Your account has been blocked"));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
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
        lockedAttempt.setLockedUntil(LocalDateTime.now().plusMinutes(65));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.of(lockedAttempt));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.authenticateUser("testuser", "wrongpassword");
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getReason().startsWith("Your account is blocked. Try again after "));
    }

    @Test
    void testAuthenticateUserAccountLockedTimeMessageFormatting() {
        LoginAttempt lockedAttempt = new LoginAttempt();
        lockedAttempt.setUserId(testUserUlid);
        lockedAttempt.setFailedAttempts(3);

        // Locked for 2 hours and 30 minutes from now
        LocalDateTime lockedUntil = LocalDateTime.now().plusHours(2).plusMinutes(30);
        lockedAttempt.setLockedUntil(lockedUntil);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.of(lockedAttempt));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.authenticateUser("testuser", "wrongpassword");
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        String message = exception.getReason();
        assertNotNull(message);
        assertTrue(message.startsWith("Your account is blocked. Try again after"));

        // Check that it includes hours and minutes, e.g., "2 hours 30 minutes"
        assertTrue(message.matches(".*\\d+ hours.*\\d+ minutes.*"));
    }

    @Test
    void testAuthenticateUserAccountLockedOnlyMinutes() {
        LoginAttempt lockedAttempt = new LoginAttempt();
        lockedAttempt.setUserId(testUserUlid);
        lockedAttempt.setFailedAttempts(3);
        lockedAttempt.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.findByUserId(testUserUlid)).thenReturn(Optional.of(lockedAttempt));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.authenticateUser("testuser", "wrongpassword");
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        String message = exception.getReason();
        assertNotNull(message);
        assertTrue(message.matches(".*\\d+ minutes.*"));
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

    @Test
    void testVerifyFirebaseIdToken_success() throws Exception {
        String fakeFirebaseToken = "fake-firebase-token";
        String phoneNumber = "+6281234567890";

        // Step 1: Mock FirebaseToken and its claims
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        Map<String, Object> claims = new HashMap<>();
        claims.put("phone_number", phoneNumber);
        when(firebaseToken.getClaims()).thenReturn(claims);

        // Step 2: Static mock FirebaseAuth.getInstance().verifyIdToken(...)
        try (MockedStatic<FirebaseAuth> firebaseAuthStaticMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
            firebaseAuthStaticMock.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);
            when(mockFirebaseAuth.verifyIdToken(fakeFirebaseToken)).thenReturn(firebaseToken);

            // Step 3: Mock userRepository.findByPhone(...) returns testUser
            when(userRepository.findByPhone(phoneNumber)).thenReturn(Optional.of(testUser));

            // Step 4: Mock loginAttemptRepository.findByUserId(...) returns existing
            // attempt
            LoginAttempt loginAttempt = new LoginAttempt();
            loginAttempt.setUserId(testUser.getId());
            loginAttempt.setFailedAttempts(3);
            loginAttempt.setLockedUntil(LocalDateTime.now().plusMinutes(10));
            when(loginAttemptRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(loginAttempt));

            // Step 5: Mock jwtUtil token generation
            when(jwtUtil.generateToken(testUser.getUsername())).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(testUser.getUsername())).thenReturn("refresh-token");

            // Step 6: Mock Redis ops
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // Step 7: Run the method
            LoginResponse response = userService.verifyFirebaseIdToken(fakeFirebaseToken);

            // Step 8: Assert result
            assertNotNull(response);
            assertEquals(200, response.getStatus());
            assertEquals("Login successful", response.getMessage());
            assertEquals("access-token", response.getToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals(testUser.getUsername(), response.getUsername());

            // Step 9: Verify repository and Redis usage
            verify(userRepository).findByPhone(phoneNumber);
            verify(loginAttemptRepository).save(any(LoginAttempt.class));
            verify(jwtUtil).generateToken(testUser.getUsername());
            verify(jwtUtil).generateRefreshToken(testUser.getUsername());
            verify(valueOperations).set(
                    "refreshToken:" + testUser.getUsername(),
                    "refresh-token",
                    Duration.ofDays(7));
        }
    }

}