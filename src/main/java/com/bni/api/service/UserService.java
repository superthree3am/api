// src/main/java/com/bni/api/service/UserService.java
package com.bni.api.service;

import com.bni.api.dto.LoginResponse;
import com.bni.api.dto.UserProfileResponse;
import com.bni.api.entity.User;
import com.bni.api.repository.UserRepository;
import com.bni.api.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.bni.api.entity.LoginAttempt;
import com.bni.api.repository.LoginAttemptRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails; 
import org.springframework.data.redis.core.StringRedisTemplate; // Import Redis
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.time.Duration; // Import Duration
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
public class UserService implements UserDetailsService {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_MINUTES = 24 * 60;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private StringRedisTemplate redisTemplate; // Injeksi StringRedisTemplate

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(String username, String email, String phone, String full_name, String password) throws Exception {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, email, phone, full_name, hashedPassword);
        user.setId(UlidCreator.getUlid().toString());
        return userRepository.save(user);
    }

    public Map<String, Object> authenticateUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or password");
        }

        User user = userOptional.get();

        Optional<LoginAttempt> loginAttemptOptional = loginAttemptRepository.findByUserId(user.getId());
        if (loginAttemptOptional.isPresent()) {
            LoginAttempt loginAttempt = loginAttemptOptional.get();
            if (loginAttempt.getLockedUntil() != null && loginAttempt.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Akun diblokir. Silakan coba lagi setelah " + loginAttempt.getLockedUntil());
            }
            if (loginAttempt.getLockedUntil() != null && loginAttempt.getLockedUntil().isBefore(LocalDateTime.now())) {
                loginAttempt.setFailedAttempts(0);
                loginAttempt.setLockedUntil(null);
                loginAttempt.setLastFailedAttempt(null);
                loginAttemptRepository.save(loginAttempt);
            }
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            LoginAttempt loginAttempt = loginAttemptOptional.orElseGet(() -> {
                LoginAttempt newLoginAttempt = new LoginAttempt(user.getId(), 0, null, null);
                newLoginAttempt.setId(UlidCreator.getUlid().toString()); // Tetapkan ULID di sini
                return newLoginAttempt;
            });
            loginAttempt.setFailedAttempts(loginAttempt.getFailedAttempts() + 1);
            loginAttempt.setLastFailedAttempt(LocalDateTime.now());

            if (loginAttempt.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                loginAttempt.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
                loginAttemptRepository.save(loginAttempt);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login gagal 3 kali. Akun Anda telah diblokir selama 24 jam.");
            }
            loginAttemptRepository.save(loginAttempt);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or password. Percobaan ke-" + loginAttempt.getFailedAttempts());
        }

        if (loginAttemptOptional.isPresent()) {
            LoginAttempt loginAttempt = loginAttemptOptional.get();
            loginAttempt.setFailedAttempts(0);
            loginAttempt.setLockedUntil(null);
            loginAttempt.setLastFailedAttempt(null);
            loginAttemptRepository.save(loginAttempt);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Authentication successful. Proceed with OTP verification.");
        response.put("phoneNumber", user.getPhone());
        response.put("username", user.getUsername());
        return response;
    }

    public LoginResponse verifyFirebaseIdToken(String firebaseIdToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
            String phoneNumber = (String) decodedToken.getClaims().get("phone_number");
            // String firebaseUid = decodedToken.getUid(); // Tidak digunakan saat ini

            Optional<User> userOptional = userRepository.findByPhone(phoneNumber);

            if (userOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User associated with Firebase token not found in your system.");
            }

            User user = userOptional.get();

            Optional<LoginAttempt> loginAttemptOptional = loginAttemptRepository.findByUserId(user.getId());
            if (loginAttemptOptional.isPresent()) {
                LoginAttempt loginAttempt = loginAttemptOptional.get();
                loginAttempt.setFailedAttempts(0);
                loginAttempt.setLockedUntil(null);
                loginAttempt.setLastFailedAttempt(null);
                loginAttemptRepository.save(loginAttempt);
            }

            String appAccessToken = jwtUtil.generateToken(user.getUsername());
            String appRefreshToken = jwtUtil.generateRefreshToken(user.getUsername()); // Hasilkan refresh token

            // Simpan refresh token di Redis dengan TTL
            redisTemplate.opsForValue().set("refreshToken:" + user.getUsername(), appRefreshToken, Duration.ofDays(7)); // Contoh 7 hari

            return new LoginResponse(200, "Login successful", appAccessToken, user.getUsername(), appRefreshToken); // Kirim access dan refresh token

        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase ID Token invalid or expired: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error verifying Firebase ID Token: " + e.getMessage());
        }
    }

     public UserProfileResponse getUserProfile(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return new UserProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getFull_name()
            );
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.bni.api.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}