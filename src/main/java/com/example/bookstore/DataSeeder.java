package com.example.bookstore;

import com.example.bookstore.dto.SeedRequest;
import com.example.bookstore.service.DatabaseSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DatabaseSeederService databaseSeederService;

    @Value("${app.seeder.ai-enabled:false}")
    private boolean aiEnabled;

    @Value("${app.seeder.max-books:300}")
    private int maxBooks;

    @Override
    public void run(String... args) {
        SeedRequest request = new SeedRequest();
        request.setIncludeAi(aiEnabled);
        request.setMaxBooks(maxBooks);
        databaseSeederService.seedData(request);
    }
}