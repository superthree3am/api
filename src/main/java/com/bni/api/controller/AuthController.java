package com.bni.api.controller;

import com.bni.api.dto.*;
import com.bni.api.service.UserService;
import com.bni.api.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) throws Exception {
        try {
            userService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getFullName(),
                    request.getPassword());

            logger.info("REGISTER | Username: {}", request.getUsername());

            RegisterResponse response = new RegisterResponse(200, "Account registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("REGISTER FAILED | Username: {} | Error: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Map<String, Object> response = userService.authenticateUser(
                    loginRequest.getUsername(),
                    loginRequest.getPassword());

            logger.info("LOGIN | Username: {}", loginRequest.getUsername());

            response.put("status", 200);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("LOGIN FAILED | Username: {} | Error: {}", loginRequest.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<LoginResponse> verifyFirebaseIdToken(@RequestBody Map<String, String> body) {
        String firebaseIdToken = body.get("idToken");

        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            logger.warn("VERIFY FAILED | Reason: Missing Firebase ID Token");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
        }

        try {
            LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);
            logger.info("VERIFY | Firebase token verified successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("VERIFY FAILED | Error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification failed: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("PROFILE ACCESS DENIED | Reason: Unauthenticated user");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve username.");
        }

        logger.info("PROFILE ACCESS | Username: {}", username);
        UserProfileResponse userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshTokens(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            logger.warn("REFRESH FAILED | Reason: Missing token");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required.");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);

            if (!jwtUtil.validateToken(refreshToken, username)) {
                logger.warn("REFRESH FAILED | Username: {} | Reason: Invalid token", username);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token.");
            }

            String storedRefreshToken = redisTemplate.opsForValue().get("refreshToken:" + username);
            if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
                logger.warn("REFRESH FAILED | Username: {} | Reason: Token mismatch or revoked", username);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or revoked refresh token.");
            }

            String newAccessToken = jwtUtil.generateToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);
            redisTemplate.opsForValue().set("refreshToken:" + username, newRefreshToken, Duration.ofDays(7));

            logger.info("REFRESH SUCCESS | Username: {}", username);

            Map<String, Object> response = new HashMap<>();
            response.put("status", 200);
            response.put("message", "Tokens refreshed successfully.");
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("REFRESH FAILED | Error: {}", e.getMessage());
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve username.");
            }

            redisTemplate.delete("refreshToken:" + username);
            logger.info("LOGOUT | Username: {}", username);
        } else {
            logger.warn("LOGOUT FAILED | Reason: Unauthenticated user");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        response.put("status", "200");
        return ResponseEntity.ok(response);
    }
}
