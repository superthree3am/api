// src/main/java/com/bni/api/service/UserService.java
package com.bni.api.service;

import com.bni.api.dto.LoginResponse;
import com.bni.api.entity.User;
import com.bni.api.repository.UserRepository;
import com.bni.api.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.google.firebase.auth.FirebaseAuth; // Tambahkan ini
import com.google.firebase.auth.FirebaseAuthException; // Tambahkan ini
import com.google.firebase.auth.FirebaseToken; // Tambahkan ini

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // FirebaseOtpService tidak lagi diperlukan untuk pengiriman/verifikasi OTP langsung
    // @Autowired
    // private FirebaseOtpService firebaseOtpService; // Hapus atau biarkan jika Anda ingin fungsionalitas lain

    // Hapus injeksi RedisTemplate jika hanya digunakan untuk OTP sessions
    // @Autowired
    // private StringRedisTemplate redisTemplate;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(String username, String email, String phone, String password, String fullName) throws Exception {
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
        User user = new User(username, email, phone, fullName, hashedPassword);
        return userRepository.save(user);
    }

    // --- Modifikasi Fungsi Login ---
    // Sekarang hanya memverifikasi kredensial dan mengembalikan nomor telepon untuk OTP di frontend
    public Map<String, Object> authenticateUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        User user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or password");
        }

        // Kredensial benar, beritahu frontend untuk melanjutkan dengan OTP Firebase Phone Auth
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Authentication successful. Proceed with OTP verification.");
        response.put("phoneNumber", user.getPhone()); // Kirim nomor telepon pengguna
        response.put("username", user.getUsername()); // Juga kirim username agar frontend tahu siapa
        return response;
    }

    // --- Fungsi Baru: Memverifikasi Firebase ID Token ---
    public LoginResponse verifyFirebaseIdToken(String firebaseIdToken) {
        try {
            // Verifikasi ID Token yang diterima dari frontend
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
            String phoneNumber = (String) decodedToken.getClaims().get("phone_number");
            String firebaseUid = decodedToken.getUid(); // Dapatkan UID Firebase

            // Pastikan nomor telepon dari token sesuai dengan yang terdaftar di sistem Anda
            // Atau Anda bisa mencari user berdasarkan Firebase UID jika Anda menyimpannya
            Optional<User> userOptional = userRepository.findByPhone(phoneNumber); // Asumsi Anda punya findByPhone

            if (userOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User associated with Firebase token not found in your system.");
            }

            User user = userOptional.get();

            // Jika semua verifikasi berhasil, generate JWT token Anda sendiri
            String appToken = jwtUtil.generateToken(user.getUsername());

            return new LoginResponse(200, "Login successful", appToken, user.getUsername());

        } catch (FirebaseAuthException e) {
            // Tangani error dari Firebase Auth (misalnya, token tidak valid, kadaluarsa)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase ID Token invalid or expired: " + e.getMessage());
        } catch (Exception e) {
            // Tangani error lainnya
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error verifying Firebase ID Token: " + e.getMessage());
        }
    }
}
