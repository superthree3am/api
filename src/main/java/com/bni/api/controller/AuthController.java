// src/main/java/com/bni/api/controller/AuthController.java
package com.bni.api.controller;

import com.bni.api.dto.LoginRequest;
import com.bni.api.dto.LoginResponse;
import com.bni.api.dto.RegisterRequest;
import com.bni.api.dto.RegisterResponse;
import com.bni.api.dto.UserProfileResponse;
// import com.bni.api.dto.OtpVerificationRequest; // Hapus ini jika dihapus DTO-nya
import com.bni.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Tambahkan ini
import org.springframework.http.HttpStatus; 

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private UserService userService;

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

    // --- Modifikasi Endpoint Login ---
    // Sekarang merespons dengan nomor telepon pengguna jika autentikasi berhasil
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = userService.authenticateUser(loginRequest.getUsername(),
                loginRequest.getPassword());
        response.put("status", 200); // Status HTTP OK
        return ResponseEntity.ok(response);
    }

    // --- Endpoint Baru: Menerima dan Memverifikasi Firebase ID Token ---
    // Asumsi request body hanya berisi idToken dari frontend
    @PostMapping("/verify") // Endpoint baru
    public ResponseEntity<LoginResponse> verifyFirebaseIdToken(@RequestBody Map<String, String> body) {
        String firebaseIdToken = body.get("idToken"); // Frontend akan mengirim idToken

        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
        }

        LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);
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

        // Gunakan metode getUserProfile yang baru
        UserProfileResponse userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }
}