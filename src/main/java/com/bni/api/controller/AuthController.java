package com.bni.api.controller;

import com.bni.api.dto.LoginRequest;
import com.bni.api.dto.LoginResponse;
import com.bni.api.entity.User;
import com.bni.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;

// Receives input and registers the data
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) throws Exception {
        String username = body.get("username");
        String email = body.get("email");
        String phone = body.get("phone");
        String password = body.get("password");
        String fullName = body.get("full_name");

        // Call service to register the user
        userService.register(username, email, phone, password, fullName);

        // Build custom JSON response
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Register berhasil");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok(response);
    }
}