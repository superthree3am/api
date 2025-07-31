package com.bni.api.controller;

import com.bni.api.dto.LoginRequest;
import com.bni.api.dto.LoginResponse;
import com.bni.api.dto.RegisterRequest;
import com.bni.api.dto.RegisterResponse;
import com.bni.api.dto.UserProfileResponse;
import com.bni.api.service.UserService;
import com.bni.api.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) throws Exception {
        try {
            MDC.put("eventType", "registration");
            MDC.put("user", request.getUsername());

            log.info("Attempt to register new account");

            userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPhone(),
                request.getFullName(),
                request.getPassword());

            log.info("Registration successful");

            RegisterResponse response = new RegisterResponse(
                200,
                "Account registered successfully");
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        try {
            MDC.put("eventType", "login");
            MDC.put("user", loginRequest.getUsername());

            log.info("Login attempt");

            Map<String, Object> response = userService.authenticateUser(
                loginRequest.getUsername(),
                loginRequest.getPassword());

            log.info("Login successful");

            response.put("status", 200);
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<LoginResponse> verifyFirebaseIdToken(
            @RequestBody Map<String, String> body) {
        String firebaseIdToken = body.get("idToken");
        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
        }
        LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not retrieve username from authentication principal."
            );
        }

        UserProfileResponse userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshTokens(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required.");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            if (!jwtUtil.validateToken(refreshToken, username)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token.");
            }

            String stored = redisTemplate.opsForValue().get("refreshToken:" + username);
            if (stored == null || !stored.equals(refreshToken)) {
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or revoked refresh token. Please log in again."
                );
            }

            String newAccessToken = jwtUtil.generateToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);
            redisTemplate.opsForValue()
                         .set("refreshToken:" + username, newRefreshToken, Duration.ofDays(7));

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", 200);
            resp.put("message", "Tokens refreshed successfully.");
            resp.put("accessToken", newAccessToken);
            resp.put("refreshToken", newRefreshToken);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error refreshing token: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null &&
            authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            String username;
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                username = (String) principal;
            } else {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not retrieve username for logout."
                );
            }
            redisTemplate.delete("refreshToken:" + username);
        }

        Map<String, String> resp = new HashMap<>();
        resp.put("status", "200");
        resp.put("message", "Logout successful");
        return ResponseEntity.ok(resp);
    }
}
