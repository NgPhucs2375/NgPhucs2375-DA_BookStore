@Bean
public FirebaseApp firebaseApp() throws Exception {
    if (FirebaseApp.getApps().isEmpty()) {
        if (!firebaseResource.exists()) {
            System.out.println("⚠️ Firebase config not found, skipping Firebase initialization.");
            return null;
        }
        try (InputStream serviceAccount = firebaseResource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
    return FirebaseApp.getInstance();
}