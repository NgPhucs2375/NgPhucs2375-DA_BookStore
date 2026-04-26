package com.example.bookstore.service.recommendation;

import com.example.bookstore.config.RecommendationConfig;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.service.recommendation.cosine.CosineSimilarityAlgorithm;
import com.example.bookstore.service.recommendation.fpgrowth.AssociationRule;
import com.example.bookstore.service.recommendation.fpgrowth.FPGrowthAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private RecommendationConfig config;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CosineSimilarityAlgorithm cosineAlgorithm;

    // Cache kết quả: Truyền vào BookId -> Danh sách các sách thường mua cùng
    private Map<Long, List<Long>> boughtTogetherCache = new HashMap<>();

    // Cache kết quả: Truyền vào BookId -> Danh sách các sách tương tự về nội dung
    private Map<Long, List<Long>> similarBooksCache = new HashMap<>();

    // Khởi chạy khi Spring Boot khởi động và cứ mỗi giờ (1 tiếng = 3600000 ms) chạy lại
    @PostConstruct
    @Scheduled(fixedRate = 3600000)
    public void refreshRecommendations() {
        System.out.println("🔄 Bắt đầu phân tích dữ liệu Gợi ý sản phẩm...");
        
        refreshBoughtTogether();
        refreshSimilarBooks();
        
        System.out.println("✅ Đã cập nhật xong bộ nhớ đệm cho Hệ Thống Gợi Ý!");
    }

    /**
     * Cập nhật Cache: Sách thường được mua cùng nhau (FP-Growth)
     */
    private void refreshBoughtTogether() {
        // 1. Phân nhóm giỏ hàng (Basket) theo OrderId
        List<Object[]> pairs = orderItemRepository.findAllOrderBookPairs();
        Map<Long, List<Long>> transactionsMap = new HashMap<>();
        
        for (Object[] row : pairs) {
            Long orderId = (Long) row[0];
            Long bookId = (Long) row[1];
            
            transactionsMap.putIfAbsent(orderId, new ArrayList<>());
            transactionsMap.get(orderId).add(bookId);
        }

        List<List<Long>> transactions = new ArrayList<>(transactionsMap.values());
        
        // 2. Thuật toán FP-Growth / Association Rule Mining
        if (transactions.isEmpty()) return;
        
        FPGrowthAlgorithm fpGrowth = new FPGrowthAlgorithm(
            config.getMinSupport(), 
            config.getMinConfidence(), 
            config.getMinLift()
        );
        
        Map<Long, List<AssociationRule>> rulesMap = fpGrowth.mineRules(transactions);
        
        // 3. Cache danh sách BookId thường mua cùng
        Map<Long, List<Long>> newCache = new HashMap<>();
        for (Map.Entry<Long, List<AssociationRule>> entry : rulesMap.entrySet()) {
            List<Long> recommendedIds = entry.getValue().stream()
                .map(AssociationRule::getConsequent)
                .limit(config.getMaxBoughtTogether())
                .collect(Collectors.toList());
            newCache.put(entry.getKey(), recommendedIds);
        }
        
        this.boughtTogetherCache = newCache;
        System.out.println("   -> Cập nhật FP-Growth (Thường Mua Cùng) hoàn tất: " + newCache.size() + " luật sách");
    }

    /**
     * Cập nhật Cache: Sách tương tự nhau về nội dung (Cosine Similarity)
     */
    private void refreshSimilarBooks() {
        List<Book> allBooks = bookRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        if (allBooks.isEmpty()) return;

        Map<Long, List<Long>> newCache = new HashMap<>();

        for (int i = 0; i < allBooks.size(); i++) {
            Book bookA = allBooks.get(i);
            
            class BookScore {
                final Long id;
                final double score;
                BookScore(Long id, double score) { this.id = id; this.score = score; }
            }
            List<BookScore> scores = new ArrayList<>();

            for (int j = 0; j < allBooks.size(); j++) {
                if (i == j) continue;
                Book bookB = allBooks.get(j);
                
                double simScore = cosineAlgorithm.calculateSimilarity(bookA, bookB);
                if (simScore > 0) {
                    scores.add(new BookScore(bookB.getId(), simScore));
                }
            }

            // Sắp xếp theo độ tương đồng giảm dần
            scores.sort((s1, s2) -> Double.compare(s2.score, s1.score));

            // Lấy ra maxSimilar cuốn sách cao điểm nhất
            List<Long> topSimilarIds = scores.stream()
                .limit(config.getMaxSimilar())
                .map(s -> s.id)
                .collect(Collectors.toList());

            newCache.put(bookA.getId(), topSimilarIds);
        }

        this.similarBooksCache = newCache;
        System.out.println("   -> Cập nhật Cosine Similarity (Sách Tương Tự) hoàn tất: Đã tính cho " + newCache.size() + " sách");
    }

    /**
     * Lấy các sách "tương tự" về nội dung (Tác giả, thể loại, từ khóa tiêu đề)
     */
    public List<Book> getSimilarBooks(Long bookId) {
        List<Long> cachedIds = similarBooksCache.getOrDefault(bookId, new ArrayList<>());
        
        List<Book> result = new ArrayList<>();
        if (!cachedIds.isEmpty()) {
            result = bookRepository.findAllById(cachedIds).stream()
                .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                .collect(Collectors.toList());
        }

        // --- FALLBACK (nếu tương tự không đủ, lấy sách cùng tác giả/thể loại) ---
        if (result.size() < config.getMaxSimilar()) {
            Optional<Book> currentBookOpt = bookRepository.findById(bookId);
            if (currentBookOpt.isPresent()) {
                List<Book> fallback = getFallbackSameAuthorOrCategory(currentBookOpt.get(), config.getMaxSimilar() - result.size(), result);
                result.addAll(fallback);
            }
        }
        return result;
    }

    /**
     * Tìm các sách "thường được mua cùng" với cuốn sách hiện tại.
     * Có fallback nếu không đủ dữ liệu.
     */
    public List<Book> getBoughtTogetherBooks(Long bookId) {
        List<Long> cachedIds = boughtTogetherCache.getOrDefault(bookId, new ArrayList<>());
        
        List<Book> result = new ArrayList<>();
        if (!cachedIds.isEmpty()) {
            result = bookRepository.findAllById(cachedIds).stream()
                .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                .collect(Collectors.toList());
        }

        // --- FALLBACK (nếu chưa có ai mua chung, hiển thị cùng tác giả) ---
        if (result.size() < config.getMaxBoughtTogether()) {
            Optional<Book> currentBookOpt = bookRepository.findById(bookId);
            if (currentBookOpt.isPresent()) {
                Book current = currentBookOpt.get();
                List<Book> fallback = getFallbackSameAuthorOrCategory(current, config.getMaxBoughtTogether() - result.size(), result);
                result.addAll(fallback);
            }
        }
        return result;
    }

    // Hàm phụ trợ Fallback
    private List<Book> getFallbackSameAuthorOrCategory(Book sourceBook, int limit, List<Book> excludeList) {
        List<Book> candidates = bookRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        
        // Bỏ qua cuốn hiện tại và các cuốn đã có sẵn trong danh sách loại trừ
        Set<Long> excludedIds = excludeList.stream().map(Book::getId).collect(Collectors.toSet());
        excludedIds.add(sourceBook.getId());

        // Ưu tiên cùng tác giả trước, nếu vẫn thiếu thì cùng mảng (Category)
        return candidates.stream()
            .filter(b -> !excludedIds.contains(b.getId()))
            .sorted((b1, b2) -> {
                boolean sameAuthor1 = Objects.equals(b1.getAuthor(), sourceBook.getAuthor());
                boolean sameAuthor2 = Objects.equals(b2.getAuthor(), sourceBook.getAuthor());
                if (sameAuthor1 && !sameAuthor2) return -1;
                if (!sameAuthor1 && sameAuthor2) return 1;
                
                boolean sameCat1 = Objects.equals(b1.getCategory().getId(), sourceBook.getCategory().getId());
                boolean sameCat2 = Objects.equals(b2.getCategory().getId(), sourceBook.getCategory().getId());
                if (sameCat1 && !sameCat2) return -1;
                if (!sameCat1 && sameCat2) return 1;
                return 0;
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

}
