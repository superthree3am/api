// package com.bni.api.controller;

// import com.bni.api.dto.LoginRequest;
// import com.bni.api.dto.LoginResponse;
// import com.bni.api.dto.RegisterRequest;
// import com.bni.api.dto.RegisterResponse;
// import com.bni.api.dto.UserProfileResponse;
// import com.bni.api.service.UserService;
// import com.bni.api.util.JwtUtil;
// import jakarta.validation.Valid;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.slf4j.MDC;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.server.ResponseStatusException;

// import java.time.Duration;
// import java.util.HashMap;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/v1")
// public class AuthController {
//     private static final Logger log = LoggerFactory.getLogger(AuthController.class);

//     @Autowired
//     private UserService userService;

//     @Autowired
//     private JwtUtil jwtUtil;

//     @Autowired
//     private StringRedisTemplate redisTemplate;

//     @PostMapping("/register")
//     public ResponseEntity<RegisterResponse> register(
//             @Valid @RequestBody RegisterRequest request) throws Exception {
//         try {
//             MDC.put("eventType", "registration");
//             MDC.put("user", request.getUsername());

//             log.info("Attempt to register new account");

//             userService.register(
//                 request.getUsername(),
//                 request.getEmail(),
//                 request.getPhone(),
//                 request.getFullName(),
//                 request.getPassword());

//             log.info("Registration successful");

//             RegisterResponse response = new RegisterResponse(
//                 200,
//                 "Account registered successfully");
//             return ResponseEntity.ok(response);
//         } finally {
//             MDC.clear();
//         }
//     }

//     @PostMapping("/login")
//     public ResponseEntity<Map<String, Object>> login(
//             @Valid @RequestBody LoginRequest loginRequest) {
//         try {
//             MDC.put("eventType", "login");
//             MDC.put("user", loginRequest.getUsername());

//             log.info("Login attempt");

//             Map<String, Object> response = userService.authenticateUser(
//                 loginRequest.getUsername(),
//                 loginRequest.getPassword());

//             log.info("Login successful");

//             response.put("status", 200);
//             return ResponseEntity.ok(response);
//         } finally {
//             MDC.clear();
//         }
//     }

//     @PostMapping("/verify")
//     public ResponseEntity<LoginResponse> verifyFirebaseIdToken(
//             @RequestBody Map<String, String> body) {
//         String firebaseIdToken = body.get("idToken");
//         if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
//         }
//         LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);
//         return ResponseEntity.ok(response);
//     }

//     @GetMapping("/profile")
//     public ResponseEntity<UserProfileResponse> getProfile() {
//         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//         if (authentication == null ||
//             !authentication.isAuthenticated() ||
//             "anonymousUser".equals(authentication.getPrincipal())) {
//             throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
//         }

//         String username;
//         Object principal = authentication.getPrincipal();
//         if (principal instanceof UserDetails) {
//             username = ((UserDetails) principal).getUsername();
//         } else if (principal instanceof String) {
//             username = (String) principal;
//         } else {
//             throw new ResponseStatusException(
//                 HttpStatus.INTERNAL_SERVER_ERROR,
//                 "Could not retrieve username from authentication principal."
//             );
//         }

//         UserProfileResponse userProfile = userService.getUserProfile(username);
//         return ResponseEntity.ok(userProfile);
//     }

//     @PostMapping("/refresh-token")
//     public ResponseEntity<Map<String, Object>> refreshTokens(
//             @RequestBody Map<String, String> body) {
//         String refreshToken = body.get("refreshToken");
//         if (refreshToken == null || refreshToken.isEmpty()) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required.");
//         }

//         try {
//             String username = jwtUtil.extractUsername(refreshToken);
//             if (!jwtUtil.validateToken(refreshToken, username)) {
//                 throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token.");
//             }

//             String stored = redisTemplate.opsForValue().get("refreshToken:" + username);
//             if (stored == null || !stored.equals(refreshToken)) {
//                 throw new ResponseStatusException(
//                     HttpStatus.UNAUTHORIZED,
//                     "Invalid or revoked refresh token. Please log in again."
//                 );
//             }

//             String newAccessToken = jwtUtil.generateToken(username);
//             String newRefreshToken = jwtUtil.generateRefreshToken(username);
//             redisTemplate.opsForValue()
//                          .set("refreshToken:" + username, newRefreshToken, Duration.ofDays(7));

//             Map<String, Object> resp = new HashMap<>();
//             resp.put("status", 200);
//             resp.put("message", "Tokens refreshed successfully.");
//             resp.put("accessToken", newAccessToken);
//             resp.put("refreshToken", newRefreshToken);
//             return ResponseEntity.ok(resp);
//         } catch (Exception e) {
//             throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error refreshing token: " + e.getMessage());
//         }
//     }

//     @PostMapping("/logout")
//     public ResponseEntity<Map<String, String>> logout() {
//         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//         if (authentication != null &&
//             authentication.isAuthenticated() &&
//             !"anonymousUser".equals(authentication.getPrincipal())) {
//             String username;
//             Object principal = authentication.getPrincipal();
//             if (principal instanceof UserDetails) {
//                 username = ((UserDetails) principal).getUsername();
//             } else if (principal instanceof String) {
//                 username = (String) principal;
//             } else {
//                 throw new ResponseStatusException(
//                     HttpStatus.INTERNAL_SERVER_ERROR,
//                     "Could not retrieve username for logout."
//                 );
//             }
//             redisTemplate.delete("refreshToken:" + username);
//         }

//         Map<String, String> resp = new HashMap<>();
//         resp.put("status", "200");
//         resp.put("message", "Logout successful");
//         return ResponseEntity.ok(resp);
//     }
// }


package com.bni.api.controller;

import com.bni.api.dto.LoginRequest;
import com.bni.api.dto.LoginResponse;
import com.bni.api.dto.RegisterRequest;
import com.bni.api.dto.RegisterResponse;
import com.bni.api.dto.UserProfileResponse;
import com.bni.api.service.UserService;
import com.bni.api.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Date;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public AuthController(UserService userService, JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    } 

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) throws Exception {
        try {
            // Menambahkan context ke MDC untuk melacak event dan user
            MDC.put("eventType", "registration");
            MDC.put("user", request.getUsername());
            log.info("Starting new user registration attempt.");

            userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPhone(),
                request.getFullName(),
                request.getPassword());

            log.info("Registration successful for user '{}'.", request.getUsername());
            
            RegisterResponse response = new RegisterResponse(
                200,
                "Account registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log error secara spesifik saat terjadi kegagalan
            log.error("Registration failed for user '{}' with error: {}", request.getUsername(), e.getMessage(), e);
            throw e;
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
            log.info("Starting login attempt for user '{}'.", loginRequest.getUsername());

            Map<String, Object> response = userService.authenticateUser(
                loginRequest.getUsername(),
                loginRequest.getPassword());

            log.info("Login successful for user '{}'.", loginRequest.getUsername());

            response.put("status", 200);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            log.warn("Login failed for user '{}' with status: {} and message: {}", loginRequest.getUsername(), e.getStatusCode(), e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user '{}': {}", loginRequest.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.", e);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/verify")
public ResponseEntity<LoginResponse> verifyFirebaseIdToken(
        @RequestBody Map<String, String> body) {
    String firebaseIdToken = body.get("idToken");
    try {
        // Set event type
        MDC.put("eventType", "firebase-verify");

        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            log.warn("Firebase ID Token is missing in verify request.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
        }

        log.info("Verifying Firebase ID Token.");

        // Lakukan verifikasi dan dapatkan response yang berisi username
        LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);

        // Ambil username dari response, lalu masukkan ke MDC
        String username = response.getUsername();
        MDC.put("user", username);

        // Log dengan mencantumkan username
        log.info("Firebase ID Token verified successfully for user '{}'.", username);

        return ResponseEntity.ok(response);
    } finally {
        // Pastikan MDC dibersihkan agar tidak bocor ke request lain
        MDC.clear();
    }
}


    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        MDC.put("eventType", "get-profile");
        String username;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
                log.warn("Unauthorized access attempt to profile.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated.");
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                username = (String) principal;
            } else {
                log.error("Could not retrieve username from authentication principal. Principal type: {}", principal.getClass().getName());
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not retrieve username from authentication principal."
                );
            }
            MDC.put("user", username);
            log.info("Fetching user profile for '{}'.", username);
            UserProfileResponse userProfile = userService.getUserProfile(username);
            log.info("Successfully fetched profile for user '{}'.", username);
            return ResponseEntity.ok(userProfile);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching user profile.", e);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshTokens(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        MDC.put("eventType", "refresh-token");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh token is missing in refresh request.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required.");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            MDC.put("user", username);
            log.info("Refreshing token for user '{}'.", username);

            if (!jwtUtil.validateToken(refreshToken, username)) {
                log.warn("Invalid or expired refresh token for user '{}'.", username);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token.");
            }

            String stored = redisTemplate.opsForValue().get("refreshToken:" + username);
            if (stored == null || !stored.equals(refreshToken)) {
                log.warn("Revoked or invalid refresh token used by user '{}'.", username);
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or revoked refresh token. Please log in again."
                );
            }

            String newAccessToken = jwtUtil.generateToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);
            redisTemplate.opsForValue()
                         .set("refreshToken:" + username, newRefreshToken, Duration.ofDays(7));

            log.info("Tokens refreshed successfully for user '{}'.", username);

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", 200);
            resp.put("message", "Tokens refreshed successfully.");
            resp.put("accessToken", newAccessToken);
            resp.put("refreshToken", newRefreshToken);
            return ResponseEntity.ok(resp);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error refreshing token: " + e.getMessage());
        } finally {
            MDC.clear();
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        MDC.put("eventType", "logout");
        String username = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails) {
                    username = ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    username = (String) principal;
                }
            }

            if (username != null) {
                MDC.put("user", username);
                log.info("Logging out user '{}'.", username);
                redisTemplate.delete("refreshToken:" + username);
                
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String accessToken = authHeader.substring(7);
                    
                    // Hitung sisa waktu expired token
                    Date expiration = jwtUtil.extractExpiration(accessToken);
                    long ttl = expiration.getTime() - System.currentTimeMillis();
                    
                    if (ttl > 0) {
                        // Simpan token ke blacklist dengan TTL sesuai sisa waktu expired
                        redisTemplate.opsForValue().set(
                            "blacklisted_token:" + accessToken, 
                            "true", 
                            Duration.ofMillis(ttl)
                        );
                        log.info("Access token blacklisted for user '{}'.", username);
                    }
                }
                
                log.info("Successfully logged out user '{}' and invalidated tokens.", username);
            } else {
                log.info("Logout request from an unauthenticated user.");
            }

            Map<String, String> resp = new HashMap<>();
            resp.put("status", "200");
            resp.put("message", "Logout successful");
            return ResponseEntity.ok(resp);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during logout for user '{}': {}", username, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not complete logout.", e);
        } finally {
            MDC.clear();
        }
    }
}
