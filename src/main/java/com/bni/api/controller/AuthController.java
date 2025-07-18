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

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil; // Injeksi JwtUtil

    @Autowired
    private StringRedisTemplate redisTemplate; // Injeksi StringRedisTemplate

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) throws Exception {

        userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPhone(),
                request.getFullName(),
                request.getPassword());

        RegisterResponse response = new RegisterResponse(
                200,
                "Account registered successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = userService.authenticateUser(loginRequest.getUsername(),
                loginRequest.getPassword());
        response.put("status", 200);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<LoginResponse> verifyFirebaseIdToken(@RequestBody Map<String, String> body) {
        String firebaseIdToken = body.get("idToken");

        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
        }

        LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);
        // LoginResponse sudah berisi access token dan refresh token dari UserService
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve username from authentication principal.");
        }

        UserProfileResponse userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    // Endpoint baru untuk refresh token
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshTokens(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required.");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            if (!jwtUtil.validateToken(refreshToken, username)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token.");
            }

            // Periksa apakah refresh token ada di Redis dan cocok
            String storedRefreshToken = redisTemplate.opsForValue().get("refreshToken:" + username);
            if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or revoked refresh token. Please log in again.");
            }

            // Hasilkan access token baru
            String newAccessToken = jwtUtil.generateToken(username);
            // Hasilkan refresh token baru (untuk rotating refresh tokens, disarankan)
            String newRefreshToken = jwtUtil.generateRefreshToken(username);
            // Perbarui refresh token di Redis dengan yang baru
            redisTemplate.opsForValue().set("refreshToken:" + username, newRefreshToken, Duration.ofDays(7));

            Map<String, Object> response = new HashMap<>();
            response.put("status", 200);
            response.put("message", "Tokens refreshed successfully.");
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error refreshing token: " + e.getMessage());
        }
    }

    // Endpoint baru untuk logout
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve username for logout.");
            }
            // Hapus refresh token dari Redis saat logout
            redisTemplate.delete("refreshToken:" + username);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        response.put("status", "200");
        return ResponseEntity.ok(response);
    }
}