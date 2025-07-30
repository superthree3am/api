package com.bni.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.*;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount = getClass()
                    .getClassLoader()
                    .getResourceAsStream("firebase/serviceAccountKey.json");
            if (serviceAccount == null) {
                // Fallback: Coba file system (untuk secret/config di production/container)
                File file = new File("/app/firebase/serviceAccountKey.json");
                if (file.exists()) {
                    serviceAccount = new FileInputStream(file);
                } else {
                    throw new FileNotFoundException("serviceAccountKey.json tidak ditemukan di classpath maupun /app/firebase");
                }
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            System.out.println("Firebase initialized!");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
        }
    }
}