// src/main/java/com/bni/api/controller/AuthController.java
package com.bni.api.controller;

import com.bni.api.dto.LoginRequest;
import com.bni.api.dto.LoginResponse;
import com.bni.api.dto.RegisterRequest;
import com.bni.api.dto.RegisterResponse;
import com.bni.api.entity.User;
import com.bni.api.service.UserService;
import com.bni.api.service.FirebaseAuthService;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Tambahkan ini
import org.springframework.http.HttpStatus; // Tambahkan ini


import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) throws Exception {

        userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                request.getFullName());

        RegisterResponse response = new RegisterResponse(
                200,
                "Account registered successfully");

        return ResponseEntity.ok(response);
    }

    // --- Modifikasi Endpoint Login ---
    // Sekarang merespons dengan nomor telepon pengguna jika autentikasi berhasil
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
        response.put("status", 200); // Status HTTP OK
        return ResponseEntity.ok(response);
    }

    // --- Endpoint Baru: Menerima dan Memverifikasi Firebase ID Token ---
    // Asumsi request body hanya berisi idToken dari frontend
    @PostMapping("/verify-firebase-id-token") // Endpoint baru
    public ResponseEntity<LoginResponse> verifyFirebaseIdToken(@RequestBody Map<String, String> body) {
        String firebaseIdToken = body.get("idToken"); // Frontend akan mengirim idToken

        if (firebaseIdToken == null || firebaseIdToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase ID Token is required.");
        }

        LoginResponse response = userService.verifyFirebaseIdToken(firebaseIdToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("idToken");
        String username = payload.get("username");

        try {
            FirebaseToken decoded = firebaseAuthService.verifyIdToken(idToken);
            String uid = decoded.getUid();

            // Fetch user details from Firebase to get phone number
            UserRecord userRecord = firebaseAuthService.getUserByUid(uid);
            String phoneNumber = userRecord.getPhoneNumber();

            if (phoneNumber == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "No phone number found"));
            }

            // Mark phone as verified in local DB
            User user = userService.markPhoneAsVerified(username, phoneNumber);

            // Issue JWT
            LoginResponse response = userService.generateLoginResponse(user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired token"));
        }
    }
}