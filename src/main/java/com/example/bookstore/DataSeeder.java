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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeForComparison(String s) {
        if (s == null) return "";
        // Chỉ giữ lại ký tự chữ và số cơ bản (a-z, 0-9) để so sánh tối giản nhất
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String bookKey(String title, String author) {
        return normalizeKey(title) + "::" + normalizeKey(author);
    }

    private User ensureUser(String username, UserRole role, String rawPassword, String shopName, String shopAddress) {
        User existing = userRepository.findByUsername(username);
        if (existing != null) {
            return existing;
        }

        User.UserBuilder builder = User.builder()
            .username(username)
            .passwordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)))
            .role(role);

        if (shopName != null) {
            builder.shopName(shopName);
        }
        if (shopAddress != null) {
            builder.shopAddress(shopAddress);
        }

        return userRepository.save(builder.build());
    }

    @Override
    public void run(String... args) throws Exception {
        long userCount = userRepository.count();
        long categoryCount = categoryRepository.count();
        long bookCount = bookRepository.count();

        System.out.println("⚡ Khoi dong DataSeeder (User: " + userCount + ", Category: " + categoryCount + ", Book: " + bookCount + ")");
        System.out.println("✅ Chi bo sung du lieu chua co, KHONG xoa du lieu cu.");

        List<Category> orderedCategories = new ArrayList<>();
        List<Category> seededCategories = List.of(
            new Category(null, "Tiểu thuyết - Văn học", "Tác phẩm văn học trong và ngoài nước", null),
            new Category(null, "Tâm lý - Kỹ năng sống", "Sách phát triển bản thân", null),
            new Category(null, "Kinh tế - Quản lý", "Kiến thức kinh doanh và tài chính", null),
            new Category(null, "Sách Thiếu nhi", "Truyện cổ tích, truyện tranh", null),
            new Category(null, "Lịch sử - Địa lý", "Tìm hiểu về thế giới và nhân loại", null),
            new Category(null, "Khoa học - Viễn tưởng", "Sách về khoa học khám phá", null),
            new Category(null, "Truyện tranh (Manga/Comic)", "Các bộ truyện tranh nổi tiếng", null),
            new Category(null, "Ngoại ngữ", "Tài liệu học tiếng Anh, Nhật, Hàn", null)
        );

        // --- FIX START: Efficient Category Seeding ---
        // 1. Fetch all existing categories ONCE and put them into a map for fast lookup.
        Map<String, Category> existingCategoriesMap = new HashMap<>();
        // The error happens on this line, so we apply the fix here
        try {
            for (Category existing : categoryRepository.findAll()) {
                existingCategoriesMap.put(normalizeForComparison(existing.getName()), existing);
            }
        } catch (Exception e) {
            System.out.println("⚠️ A known error occurred while fetching categories, attempting to proceed with an empty map. The error was: " + e.getMessage());
            // Proceed with an empty map, the logic below will handle saving the seeds.
        }

        // 2. Iterate through the hardcoded seeds and check against the map.
        for (Category seed : seededCategories) {
            String seedNorm = normalizeForComparison(seed.getName());
            Category existing = existingCategoriesMap.get(seedNorm);

            if (existing == null) {
                // This category is new, save it.
                try {
                    existing = categoryRepository.save(seed);
                    System.out.println("✅ Added new category: " + seed.getName());
                } catch (Exception e) {
                    System.out.println("⚠️ Warning: Could not save new category: " + seed.getName());
                    continue; // Skip to next seed
                }
            } else {
                // Category exists, check if name/description needs updating for consistency.
                boolean isNameDiff = existing.getName() == null || !existing.getName().equals(seed.getName());
                boolean isDescDiff = existing.getDescription() == null || !existing.getDescription().equals(seed.getDescription());

                if (isNameDiff || isDescDiff) {
                    existing.setName(seed.getName());
                    existing.setDescription(seed.getDescription());
                    try {
                        existing = categoryRepository.save(existing);
                        System.out.println("🔧 Updated category: " + seed.getName());
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not update category: " + existing.getName());
                    }
                }
            }
            orderedCategories.add(existing);
        }
        // --- FIX END ---

        Category[] categories = orderedCategories.toArray(new Category[0]);

        User admin = ensureUser("admin@gmail.com", UserRole.ADMIN, "admin123", null, null);
        User sellerNhaNam = ensureUser("shop_nha_nam@gmail.com", UserRole.SELLER, "seller123", "Nha Nam Official", "Quang Trung Software Park, HCMC");
        User sellerTre = ensureUser("shop_tre@gmail.com", UserRole.SELLER, "seller123", "NXB Tre Official", "District 3, HCMC");

        System.out.println("✅ Da tao admin va 2 seller mac dinh: " + admin.getUsername());

        System.out.println("--- PHẦN 1: NẠP DỮ LIỆU TỪ CSV ---");
        java.io.InputStream is = getClass().getResourceAsStream("/Books.csv");
        if (is == null) { System.out.println("❌ Không thấy file Books.csv!"); return; }

        Set<String> existingBookKeys = new HashSet<>();
        for (Book existing : bookRepository.findAll()) {
            existingBookKeys.add(bookKey(existing.getTitle(), existing.getAuthor()));
        }

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
                        String title = data[1].replace("\"", "").trim();
                        String author = data[2].replace("\"", "").trim();
                        String uniqueKey = bookKey(title, author);
                        if (existingBookKeys.contains(uniqueKey)) {
                            continue;
                        }

                        Book book = new Book();
                        book.setTitle(title);
                        book.setAuthor(author);
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
                        existingBookKeys.add(uniqueKey);
                        count++;
                        if (count >= 300) { break; }
                    } catch (Exception e) {}
                }
            }
            System.out.println("✅ Bo sung xong " + count + " cuon sach (khong trung lap)!");
        }

        boolean hasAiKey = apiKey != null && !apiKey.isBlank() && !"MISSING_KEY".equals(apiKey);
        if (!hasAiKey) {
            System.out.println("⚠️ Bo qua buoc AI mo ta vi chua cau hinh GOOGLE_AI_KEY.");
            return;
        }

        System.out.println("\n--- PHẦN 2: DÙNG RESTCLIENT GỌI THẲNG GEMINI FLASH ---");
        List<Book> booksNeedDesc = bookRepository.findAll().stream()
            .filter(book -> book.getDescription() == null || book.getDescription().isBlank())
            .limit(10)
            .toList();

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