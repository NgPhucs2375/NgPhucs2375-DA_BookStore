package com.example.bookstore.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private Resource firebaseResource;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        // Kiểm tra nếu đã có FirebaseApp nào chưa (tránh lỗi duplicate)
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream serviceAccount = firebaseResource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                return FirebaseApp.initializeApp(options);
            }
        }
        return FirebaseApp.getInstance();
    }
}
