package com.example.bookstore;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.model.Category;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private String bookKey(String title, String author) {
        return normalizeKey(title) + "::" + normalizeKey(author);
    }

    private User ensureUser(
            String username,
            UserRole role,
            String rawPassword,
            String shopName,
            String shopAddress
    ) {

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

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Override
    public void run(String... args) {

        long userCount = userRepository.count();
        long categoryCount = categoryRepository.count();
        long bookCount = bookRepository.count();

        System.out.println(
                "⚡ Khoi dong DataSeeder (User: " +
                        userCount +
                        ", Category: " +
                        categoryCount +
                        ", Book: " +
                        bookCount +
                        ")"
        );

        System.out.println(
                "✅ Chi bo sung du lieu chua co, KHONG xoa du lieu cu."
        );

        boolean needCategorySeed = categoryCount == 0;
        boolean needBookSeed = bookCount == 0;

        if (!needCategorySeed && !needBookSeed) {
            System.out.println("✅ Du lieu da co san, bo qua DataSeeder de tranh tre may khi khoi dong.");
            return;
        }

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

        // =====================================================
        // CATEGORY SEEDING
        // =====================================================

        Map<String, Category> existingCategoriesMap = new HashMap<>();

        try {
            for (Category existing : categoryRepository.findAll()) {
                if (existing.getName() != null) {
                    existingCategoriesMap.put(
                            normalizeForComparison(existing.getName()),
                            existing
                    );
                }
            }
        } catch (Exception e) {
            logger.error("⚠️ Loi khi load category tu database: {}", e.getMessage(), e);
        }

        if (needCategorySeed) {
            for (Category seed : seededCategories) {
                try {
                    String seedNorm = normalizeForComparison(seed.getName());

                    Category existing = existingCategoriesMap.get(seedNorm);

                    if (existing == null) {
                        existing = categoryRepository.save(seed);
                        System.out.println("✅ Added new category: " + seed.getName());
                    }

                    if (existing != null && existing.getId() != null) {
                        orderedCategories.add(existing);
                    } else {
                        logger.warn("❌ Invalid category skipped: {}", seed.getName());
                    }

                } catch (Exception e) {
                    logger.error("❌ Loi khi xu ly category: {}", seed.getName(), e);
                }
            }
        }

        // Refresh categories from database to ensure all IDs are persisted
        List<Category> allCategoriesFromDb = categoryRepository.findAll();
        if (allCategoriesFromDb.isEmpty()) {
            throw new RuntimeException(
                    "❌ No categories found in database after seeding. Cannot proceed."
            );
        }

        Category[] categories = allCategoriesFromDb.toArray(new Category[0]);

        System.out.println("✅ Categories loaded from database: " + categories.length);

        // =====================================================
        // VALIDATE CATEGORY ARRAY
        // =====================================================

        if (categories.length == 0) {

            throw new RuntimeException(
                    "❌ No valid categories found. Cannot seed books."
            );
        }

        // Validate all categories have valid IDs
        for (Category cat : categories) {
            if (cat.getId() == null) {
                logger.warn("⚠️ Category {} has null ID", cat.getName());
            } else {
                logger.debug("✓ Category {} has ID: {}", cat.getName(), cat.getId());
            }
        }

        // =====================================================
        // CREATE USERS
        // =====================================================

        User admin = ensureUser(
                "admin@gmail.com",
                UserRole.ADMIN,
                "admin123",
                null,
                null
        );

        User sellerNhaNam = ensureUser(
                "shop_nha_nam@gmail.com",
                UserRole.SELLER,
                "seller123",
                "Nha Nam Official",
                "Quang Trung Software Park, HCMC"
        );

        User sellerTre = ensureUser(
                "shop_tre@gmail.com",
                UserRole.SELLER,
                "seller123",
                "NXB Tre Official",
                "District 3, HCMC"
        );

        System.out.println(
                "✅ Da tao admin va 2 seller mac dinh: " +
                        admin.getUsername()
        );

        // =====================================================
        // IMPORT BOOKS FROM CSV
        // =====================================================

        if (!needBookSeed) {
            System.out.println("✅ Book da co san, bo qua buoc import CSV.");
            return;
        }

        System.out.println(
                "--- PHẦN 1: NẠP DỮ LIỆU TỪ CSV ---"
        );

        java.io.InputStream is =
                getClass().getResourceAsStream("/Books.csv");

        if (is == null) {

            System.out.println("❌ Không thấy file Books.csv!");

            return;
        }

        Set<String> existingBookKeys = new HashSet<>();

        for (Book existing : bookRepository.findAll()) {

            existingBookKeys.add(
                    bookKey(existing.getTitle(), existing.getAuthor())
            );
        }

        try (
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8)
                )
        ) {

            String line;

            boolean isFirstLine = true;

            int count = 0;

            Random random = new Random();

            while ((line = br.readLine()) != null) {

                if (isFirstLine) {

                    isFirstLine = false;

                    continue;
                }

                String[] data =
                        line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (data.length >= 8) {

                    String title = "";
                    String author = "";

                    try {

                        title =
                                data[1].replace("\"", "").trim();

                        author =
                                data[2].replace("\"", "").trim();

                        String uniqueKey =
                                bookKey(title, author);

                        if (existingBookKeys.contains(uniqueKey)) {

                            continue;
                        }

                        Category selectedCategory =
                                categories[random.nextInt(categories.length)];

                        // =====================================================
                        // VALIDATE CATEGORY BEFORE USING
                        // =====================================================

                        if (selectedCategory == null) {
                            logger.error("❌ Selected category is null for book: {}", title);
                            continue;
                        }

                        if (selectedCategory.getId() == null) {
                            logger.error("❌ Selected category has null ID for book: {} (category: {})", title, selectedCategory.getName());
                            continue;
                        }

                        // Fetch category from repository to ensure it's managed in current session
                        Category managedCategory = categoryRepository.getReferenceById(selectedCategory.getId());
                        if (managedCategory == null) {
                            logger.error("❌ Category with ID {} not found in database for book: {}", selectedCategory.getId(), title);
                            continue;
                        }

                        Book book = new Book();

                        book.setTitle(title);

                        book.setAuthor(author);

                        book.setPublishYear(
                                data[3].replace("\"", "").trim()
                        );

                        book.setPublisher(
                                data[6].replace("\"", "").trim()
                        );

                        book.setImageUrl(
                                data[7].replace("\"", "").trim()
                        );

                        double randomPrice = Math.round(((Math.random() * 200000) + 50000) / 1000.0) * 1000.0;
                        book.setPrice(randomPrice);

                        book.setStockQuantity((int) (Math.random() * 90) + 10);

                        book.setApprovalStatus(
                                ApprovalStatus.APPROVED
                        );

                        // Chia ngau nhien quyen so huu sach cho 2 seller va reload tu database
                        User selectedSeller = random.nextBoolean() ? sellerNhaNam : sellerTre;
                        User managedSeller = userRepository.findById(selectedSeller.getId()).orElse(null);
                        if (managedSeller == null) {
                            logger.error("❌ Seller not found for book: {}", title);
                            continue;
                        }
                        book.setSeller(managedSeller);

                        // Gan danh muc da validate va managed

                        book.setCategory(managedCategory);

                        // Luon co mo ta co ban de UI khong bi trong khi AI gap loi

                        book.setDescription(
                                buildFallbackDescription(book)
                        );

                        System.out.println(
                                "📚 Saving Book: " +
                                        title +
                                        " | Category ID: " +
                                        managedCategory.getId()
                        );

                        bookRepository.save(book);

                        existingBookKeys.add(uniqueKey);

                        count++;

                        if (count >= 300) {

                            break;
                        }

                    } catch (Exception e) {
                        logger.error("❌ Loi khi save sach: {} - {}", title, e.getMessage(), e);
                    }
                }
            }

            System.out.println(
                    "✅ Bo sung xong " +
                            count +
                            " cuon sach (khong trung lap)!"
            );

        } catch (Exception e) {
            logger.error("❌ Loi khi doc file CSV", e);
        }

        System.out.println("✅ Hoan thanh seed sach tu CSV.");
    }
}