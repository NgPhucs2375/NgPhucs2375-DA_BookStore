package com.example.bookstore;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private BookRepository bookRepository;

    // ✨ Lấy Key từ application.properties
    @Value("${google.gemini.api-key:MISSING_KEY}")
    private String apiKey;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Dang don dep kho sach cu...");
        bookRepository.deleteAll();

        System.out.println("--- PHẦN 1: NẠP DỮ LIỆU TỪ CSV ---");
        java.io.InputStream is = getClass().getResourceAsStream("/Books.csv");
        if (is == null) { System.out.println("❌ Không thấy file Books.csv!"); return; }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            int count = 0;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) { isFirstLine = false; continue; }
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (data.length >= 8) {
                    try {
                        Book book = new Book();
                        book.setTitle(data[1].replace("\"", "").trim());
                        book.setAuthor(data[2].replace("\"", "").trim());
                        book.setPublishYear(data[3].replace("\"", "").trim());
                        book.setPublisher(data[6].replace("\"", "").trim());
                        book.setImageUrl(data[7].replace("\"", "").trim());

                        double randomPrice = Math.round((Math.random() * 200000) + 50000) / 1000 * 1000;
                        book.setPrice(randomPrice);
                        book.setStockQuantity((int)(Math.random() * 90) + 10);

                        bookRepository.save(book);
                        count++;
                        if (count >= 300) { break; }
                    } catch (Exception e) {}
                }
            }
            System.out.println("✅ Nạp xong 300 cuốn sách!");
        }

        System.out.println("\n--- PHẦN 2: DÙNG RESTCLIENT GỌI THẲNG GEMINI FLASH ---");
        List<Book> booksNeedDesc = bookRepository.findAll().stream().limit(10).toList();

        RestClient restClient = RestClient.create();
        String geminiUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        for (Book book : booksNeedDesc) {
            try {
                System.out.println("Dang xin Gemini Flash viet mo ta cho: " + book.getTitle());

                String prompt = "Viết một đoạn mô tả ngắn gọn, hấp dẫn bằng tiếng Việt (khoảng 2 câu) giới thiệu về cuốn sách '"
                        + book.getTitle() + "' của tác giả " + book.getAuthor() + ". Không dùng markdown.";

                // Đóng gói dữ liệu gửi đi
                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
                );

                // Gọi API và lấy JSON trả về
                JsonNode responseNode = restClient.post()
                        .uri(geminiUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class);

                // Bóc tách câu trả lời của AI từ JSON
                String aiDescription = responseNode.at("/candidates/0/content/parts/0/text").asText();

                book.setDescription(aiDescription.trim() + " (Mô tả bởi AI)");
                bookRepository.save(book);

                Thread.sleep(1500); // Đợi 1.5s cho an toàn

            } catch (Exception e) {
                System.out.println("❌ Lỗi AI cuốn " + book.getTitle() + ": " + e.getMessage());
            }
        }
        System.out.println("✅ HOÀN THÀNH DỰ ÁN AI SIÊU MƯỢT!");
    }
}