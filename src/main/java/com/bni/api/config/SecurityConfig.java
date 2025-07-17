package com.bni.api.config;

import com.bni.api.filter.JwtRequestFilter; // Import filter JWT Anda
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import ini
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Import ini

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter; // Injeksi filter JWT Anda

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Nonaktifkan CSRF untuk API stateless
            .authorizeHttpRequests(auth -> auth
                // Izinkan semua permintaan OPTIONS (preflight CORS) tanpa autentikasi
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Izinkan endpoint publik tanpa autentikasi
                .requestMatchers("/api/v1/login", "/api/v1/register", "/api/v1/verify").permitAll() // Perbarui "/api/v1/verify-firebase-id-token" menjadi "/api/v1/verify" sesuai AuthController
                // Amankan endpoint profil dan semua endpoint lain yang tidak secara eksplisit diizinkan
                .requestMatchers("/api/v1/profile").authenticated() // Membutuhkan autentikasi untuk endpoint profil
                .anyRequest().authenticated() // Semua permintaan lain membutuhkan autentikasi
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Konfigurasi sesi sebagai stateless
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable());

        // Tambahkan JWT filter sebelum UsernamePasswordAuthenticationFilter
        // Ini memastikan JWT divalidasi sebelum autentikasi username/password standar
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Anda mungkin memerlukan ini jika Anda menggunakan Spring Security 6+ dan ingin mengautentikasi secara manual
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
