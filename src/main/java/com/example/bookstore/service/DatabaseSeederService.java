package com.example.bookstore.service;

import com.example.bookstore.dto.SeedRequest;
import com.example.bookstore.dto.SeedResult;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Category;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeederService {
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final GeminiService geminiService;

    @Value("${app.seeder.max-books:300}")
    private int defaultMaxBooks;

    @Value("${app.seeder.ai-enabled:false}")
    private boolean defaultAiEnabled;

    @Transactional
    public SeedResult seedData(SeedRequest request) {
        SeedRequest options = request != null ? request : new SeedRequest();

        boolean seedCategories = getOrDefault(options.getSeedCategories(), true);
        boolean seedUsers = getOrDefault(options.getSeedUsers(), true);
        boolean seedBooks = getOrDefault(options.getSeedBooks(), true);
        boolean includeAi = getOrDefault(options.getIncludeAi(), defaultAiEnabled);

        int maxBooks = options.getMaxBooks() != null && options.getMaxBooks() > 0
                ? options.getMaxBooks()
                : defaultMaxBooks;

        SeedResult result = new SeedResult();
        result.setWarnings(new ArrayList<>());

        log.info("Seeding started (categories={}, users={}, books={}, maxBooks={}, ai={})",
                seedCategories, seedUsers, seedBooks, maxBooks, includeAi);

        List<Category> categories = seedCategories ? seedCategories(result) : categoryRepository.findAll();
        if (categories.isEmpty()) {
            result.getWarnings().add("No categories available; book seeding skipped.");
        }

        List<User> sellers = seedUsers ? seedUsers(result) : userRepository.findAllByRole(UserRole.SELLER);
        if (sellers.isEmpty()) {
            result.getWarnings().add("No sellers available; book seeding skipped.");
        }

        List<Book> newBooks = List.of();
        if (seedBooks && !categories.isEmpty() && !sellers.isEmpty()) {
            newBooks = readAndSaveFromCsv(categories, sellers, maxBooks, result);
        } else if (seedBooks) {
            result.getWarnings().add("Skipped book seeding because categories or sellers are missing.");
        }

        if (includeAi) {
            if (geminiService.isEnabled()) {
                result.setAiEnqueued(enrichBooksWithAI(newBooks));
            } else {
                result.setAiEnqueued(false);
                result.getWarnings().add("Gemini API key missing; AI enrichment skipped.");
            }
        }

        log.info("Seeding finished (categoriesAdded={}, categoriesUpdated={}, usersAdded={}, booksAdded={}, booksSkipped={})",
                result.getCategoriesAdded(), result.getCategoriesUpdated(), result.getUsersAdded(),
                result.getBooksAdded(), result.getBooksSkipped());

        return result;
    }

    private boolean getOrDefault(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private List<Category> seedCategories(SeedResult result) {
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

        Map<String, Category> existingCategoriesMap = new HashMap<>();
        try {
            for (Category existing : categoryRepository.findAll()) {
                existingCategoriesMap.put(normalizeForComparison(existing.getName()), existing);
            }
        } catch (Exception e) {
            result.getWarnings().add("Failed to load existing categories: " + e.getMessage());
        }

        int added = 0;
        int updated = 0;

        for (Category seed : seededCategories) {
            String seedNorm = normalizeForComparison(seed.getName());
            Category existing = existingCategoriesMap.get(seedNorm);

            if (existing == null) {
                try {
                    existing = categoryRepository.save(seed);
                    added++;
                } catch (Exception e) {
                    result.getWarnings().add("Failed to add category: " + seed.getName());
                    continue;
                }
            } else {
                boolean isNameDiff = existing.getName() == null || !existing.getName().equals(seed.getName());
                boolean isDescDiff = existing.getDescription() == null || !existing.getDescription().equals(seed.getDescription());

                if (isNameDiff || isDescDiff) {
                    existing.setName(seed.getName());
                    existing.setDescription(seed.getDescription());
                    try {
                        existing = categoryRepository.save(existing);
                        updated++;
                    } catch (Exception e) {
                        result.getWarnings().add("Failed to update category: " + seed.getName());
                    }
                }
            }
            orderedCategories.add(existing);
        }

        result.setCategoriesAdded(added);
        result.setCategoriesUpdated(updated);
        return orderedCategories;
    }

    private List<User> seedUsers(SeedResult result) {
        AtomicInteger added = new AtomicInteger(0);

        User admin = ensureUser("admin@gmail.com", UserRole.ADMIN, "admin123", null, null, added);
        User sellerNhaNam = ensureUser(
                "shop_nha_nam@gmail.com",
                UserRole.SELLER,
                "seller123",
                "Nha Nam Official",
                "Quang Trung Software Park, HCMC",
                added
        );
        User sellerTre = ensureUser(
                "shop_tre@gmail.com",
                UserRole.SELLER,
                "seller123",
                "NXB Tre Official",
                "District 3, HCMC",
                added
        );

        result.setUsersAdded(added.get());
        return List.of(sellerNhaNam, sellerTre).stream().filter(u -> u != null).toList();
    }

    private User ensureUser(
            String username,
            UserRole role,
            String rawPassword,
            String shopName,
            String shopAddress,
            AtomicInteger added
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

        User saved = userRepository.save(builder.build());
        added.incrementAndGet();
        return saved;
    }

    private List<Book> readAndSaveFromCsv(
            List<Category> categories,
            List<User> sellers,
            int maxBooks,
            SeedResult result
    ) {
        List<Book> addedBooks = new ArrayList<>();
        java.io.InputStream is = getClass().getResourceAsStream("/Books.csv");
        if (is == null) {
            result.getWarnings().add("Books.csv not found in resources.");
            return addedBooks;
        }

        Set<String> existingBookKeys = new HashSet<>();
        for (Book existing : bookRepository.findAll()) {
            existingBookKeys.add(bookKey(existing.getTitle(), existing.getAuthor()));
        }

        int added = 0;
        int skipped = 0;
        Random random = new Random();
        Category[] categoryArray = categories.toArray(new Category[0]);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) { isFirstLine = false; continue; }
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (data.length < 8) {
                    skipped++;
                    continue;
                }

                String title = data[1].replace("\"", "").trim();
                String author = data[2].replace("\"", "").trim();
                String uniqueKey = bookKey(title, author);
                if (existingBookKeys.contains(uniqueKey)) {
                    skipped++;
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

                User seller = sellers.get(random.nextInt(sellers.size()));
                book.setSeller(seller);
                book.setCategory(categoryArray[random.nextInt(categoryArray.length)]);
                book.setDescription(buildFallbackDescription(book));

                bookRepository.save(book);
                existingBookKeys.add(uniqueKey);
                addedBooks.add(book);
                added++;

                if (added >= maxBooks) {
                    break;
                }
            }
        } catch (Exception e) {
            result.getWarnings().add("Failed to read Books.csv: " + e.getMessage());
        }

        result.setBooksAdded(added);
        result.setBooksSkipped(skipped);
        return addedBooks;
    }

    private boolean enrichBooksWithAI(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return false;
        }

        for (Book book : books) {
            geminiService.generateDescription(book.getTitle(), book.getAuthor())
                    .thenAccept(description -> {
                        if (description == null || description.isBlank()) {
                            return;
                        }
                        book.setDescription(description.trim() + " (Mo ta boi AI)");
                        bookRepository.save(book);
                        log.info("Updated AI description for: {}", book.getTitle());
                    });
        }
        return true;
    }

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
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String bookKey(String title, String author) {
        return normalizeKey(title) + "::" + normalizeKey(author);
    }
}
