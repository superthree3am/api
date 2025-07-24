// src/main/java/com/bni/api/controller/AuthController.java
package com.bni.api.controller;

import com.bni.api.dto.LoginRequest;
import com.bni.api.dto.LoginResponse;
import com.bni.api.dto.RegisterRequest;
import com.bni.api.dto.RegisterResponse;
import com.bni.api.dto.UserProfileResponse;
import com.bni.api.service.UserService;
import com.bni.api.util.JwtUtil; // Import JwtUtil
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate; // Import Redis
import java.time.Duration;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil; // Injeksi JwtUtil

    @Autowired
    private StringRedisTemplate redisTemplate; // Injeksi StringRedisTemplate

@PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) throws Exception {
        logger.info("Register request received for username: {}, email: {}", request.getUsername(), request.getEmail());

        userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPhone(),
                request.getFullName(),
                request.getPassword());

        logger.info("User registered successfully: {}", request.getUsername());

        RegisterResponse response = new RegisterResponse(200, "Account registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login request received for username: {}", loginRequest.getUsername());

        Map<String, Object> response = userService.authenticateUser(
                loginRequest.getUsername(),
                loginRequest.getPassword());

        logger.info("User login successful: {}", loginRequest.getUsername());

        response.put("status", 200);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<LoginResponse> verifyFirebaseIdToken(@RequestBody Map<String, String> body) {
        String firebaseIdToken = body.get("idToken");
        logger.info("Firebase token verification request received.");

        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            logger.warn("Firebase ID Token is missing");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
        }

        LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);

        logger.info("Firebase token verified for user: {}", response.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("Unauthorized access to profile");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            logger.error("Unknown principal type in authentication");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve username.");
        }

        logger.info("Profile request for user: {}", username);
        UserProfileResponse userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshTokens(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        logger.info("Refresh token request received");

        if (refreshToken == null || refreshToken.isEmpty()) {
            logger.warn("Missing refresh token in request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required.");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            if (!jwtUtil.validateToken(refreshToken, username)) {
                logger.warn("Invalid refresh token for user: {}", username);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token.");
            }

            String storedRefreshToken = redisTemplate.opsForValue().get("refreshToken:" + username);
            if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
                logger.warn("Refresh token not found or mismatched in Redis for user: {}", username);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or revoked refresh token.");
            }

            String newAccessToken = jwtUtil.generateToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);

            redisTemplate.opsForValue().set("refreshToken:" + username, newRefreshToken, Duration.ofDays(7));

            Map<String, Object> response = new HashMap<>();
            response.put("status", 200);
            response.put("message", "Tokens refreshed successfully.");
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);

            logger.info("Tokens refreshed successfully for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error refreshing token: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username;
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                username = (String) principal;
            } else {
                logger.error("Failed to extract username for logout");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve username.");
            }

            redisTemplate.delete("refreshToken:" + username);
            logger.info("User logged out: {}", username);
        } else {
            logger.warn("Logout request from unauthenticated user");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        response.put("status", "200");
        return ResponseEntity.ok(response);
    }
}