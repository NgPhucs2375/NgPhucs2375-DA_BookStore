package com.example.bookstore;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.model.Category;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // ✨ Lấy Key từ application.properties
    @Value("${google.gemini.api-key:MISSING_KEY}")
    private String apiKey;

    private String buildFallbackDescription(Book book) {
        return String.format(
            "%s la mot tua sach cua %s, phu hop cho ban doc muon mo rong kien thuc va trai nghiem doc sach chat luong. Phien ban hien tai dang duoc phan phoi boi he thong BOOKOM.",
            book.getTitle(),
            book.getAuthor()
        );
    }

    @Override
    public void run(String... args) throws Exception {
        // ⚠️ KIỂM TRA CHẶT CHẼ: Nếu cả User + Category + Book đều có dữ liệu 
        // thì Seeder đã chạy từ trước, không chạy lại
        long userCount = userRepository.count();
        long categoryCount = categoryRepository.count();
        long bookCount = bookRepository.count();
        
        if (userCount > 0 && categoryCount > 0 && bookCount > 0) {
            System.out.println("✅ Du lieu da co? nguoi dung: " + userCount + ", danh muc: " + categoryCount + ", sach: " + bookCount);
            System.out.println("✅ DataSeeder da chay, bo qua tao du lieu moi...");
            return;
        }
        
        System.out.println("⚡ Dung canh bao: Du lieu khong day du (User: " + userCount + ", Category: " + categoryCount + ", Book: " + bookCount + ")");
        System.out.println("Dang don dep kho sach cu va tao du lieu moi...");
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        Category cat1 = categoryRepository.save(new Category(null, "Tiểu thuyết - Văn học", "Tác phẩm văn học trong và ngoài nước", null));
        Category cat2 = categoryRepository.save(new Category(null, "Tâm lý - Kỹ năng sống", "Sách phát triển bản thân", null));
        Category cat3 = categoryRepository.save(new Category(null, "Kinh tế - Quản lý", "Kiến thức kinh doanh và tài chính", null));
        Category cat4 = categoryRepository.save(new Category(null, "Sách Thiếu nhi", "Truyện cổ tích, truyện tranh", null));
        Category cat5 = categoryRepository.save(new Category(null, "Lịch sử - Địa lý", "Tìm hiểu về thế giới và nhân loại", null));
        Category cat6 = categoryRepository.save(new Category(null, "Khoa học - Viễn tưởng", "Sách về khoa học khám phá", null));
        Category cat7 = categoryRepository.save(new Category(null, "Truyện tranh (Manga/Comic)", "Các bộ truyện tranh nổi tiếng", null));
        Category cat8 = categoryRepository.save(new Category(null, "Ngoại ngữ", "Tài liệu học tiếng Anh, Nhật, Hàn", null));
        
        // Random assign category to book later
        Category[] categories = {cat1, cat2, cat3, cat4, cat5, cat6, cat7, cat8};

        User admin = userRepository.save(User.builder()
            .username("admin@gmail.com")
            .passwordHash(BCrypt.hashpw("admin123", BCrypt.gensalt(10)))
            .role(UserRole.ADMIN)
            .build());

        User sellerNhaNam = userRepository.save(User.builder()
            .username("shop_nha_nam@gmail.com")
            .passwordHash(BCrypt.hashpw("seller123", BCrypt.gensalt(10)))
            .role(UserRole.SELLER)
            .shopName("Nha Nam Official")
            .shopAddress("Quang Trung Software Park, HCMC")
            .build());

        User sellerTre = userRepository.save(User.builder()
            .username("shop_tre@gmail.com")
            .passwordHash(BCrypt.hashpw("seller123", BCrypt.gensalt(10)))
            .role(UserRole.SELLER)
            .shopName("NXB Tre Official")
            .shopAddress("District 3, HCMC")
            .build());

        System.out.println("✅ Da tao admin va 2 seller mac dinh: " + admin.getUsername());

        System.out.println("--- PHẦN 1: NẠP DỮ LIỆU TỪ CSV ---");
        java.io.InputStream is = getClass().getResourceAsStream("/Books.csv");
        if (is == null) { System.out.println("❌ Không thấy file Books.csv!"); return; }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            int count = 0;
            Random random = new Random();

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
                        book.setApprovalStatus(ApprovalStatus.APPROVED);
                        // Chia ngau nhien quyen so huu sach cho 2 seller
                        book.setSeller(random.nextBoolean() ? sellerNhaNam : sellerTre);
                        // Gan danh muc ngau nhien 
                        book.setCategory(categories[random.nextInt(categories.length)]);
                        // Luon co mo ta co ban de UI khong bi trong khi AI gap loi
                        book.setDescription(buildFallbackDescription(book));

                        bookRepository.save(book);
                        count++;
                        if (count >= 300) { break; }
                    } catch (Exception e) {}
                }
            }
            System.out.println("✅ Nạp xong 300 cuốn sách!");
        }

        boolean hasAiKey = apiKey != null && !apiKey.isBlank() && !"MISSING_KEY".equals(apiKey);
        if (!hasAiKey) {
            System.out.println("⚠️ Bo qua buoc AI mo ta vi chua cau hinh GOOGLE_AI_KEY.");
            return;
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
                if (aiDescription != null && !aiDescription.isBlank()) {
                    book.setDescription(aiDescription.trim() + " (Mo ta boi AI)");
                }
                bookRepository.save(book);

                Thread.sleep(1500); // Đợi 1.5s cho an toàn

            } catch (Exception e) {
                System.out.println("❌ Lỗi AI cuốn " + book.getTitle() + ": " + e.getMessage());
            }
        }
        System.out.println("✅ HOÀN THÀNH DỰ ÁN AI SIÊU MƯỢT!");
    }
}