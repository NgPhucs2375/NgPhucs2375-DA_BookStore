package com.example.bookstore.service.recommendation;

import com.example.bookstore.config.RecommendationConfig;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.model.Category;
import com.example.bookstore.service.recommendation.pairmining.AssociationRule;
import com.example.bookstore.service.recommendation.pairmining.PairMiningAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationJob {

    @Autowired
    private RecommendationConfig config;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private BookRepository bookRepository;


    @Autowired
    private RecommendationCacheHolder cacheHolder;

    // Run at startup and every hour
    @Scheduled(initialDelay = 5000, fixedRateString = "${recommendation.refresh-rate-ms:3600000}")
    public void recompute() {
        System.out.println("[RecommendationJob] Starting precompute job...");

        Map<Long, List<Long>> boughtTogether = computeBoughtTogether();
        Map<Long, List<Long>> similarBooks = computeSimilarBooks();

        RecommendationCache snapshot = new RecommendationCache(boughtTogether, similarBooks);
        cacheHolder.swap(snapshot);

        System.out.println("[RecommendationJob] Precompute finished. Snapshot size: boughtTogether=" + boughtTogether.size() + ", similar=" + similarBooks.size());
    }

    private Map<Long, List<Long>> computeBoughtTogether() {
        // Only include orders that are paid/processed or completed (exclude PENDING_PAYMENT and CANCELLED)
        List<Object[]> pairs = orderItemRepository.findAllOrderBookPairsByStatuses(Arrays.asList(OrderStatus.PROCESSING, OrderStatus.COMPLETED));
        Map<Long, List<Long>> transactionsMap = new HashMap<>();

        for (Object[] row : pairs) {
            Long orderId = (Long) row[0];
            Long bookId = (Long) row[1];

            transactionsMap.putIfAbsent(orderId, new ArrayList<>());
            transactionsMap.get(orderId).add(bookId);
        }

        List<List<Long>> transactions = new ArrayList<>(transactionsMap.values());
        if (transactions.isEmpty()) return Collections.emptyMap();

        // Use pair-mining (frequent pairs) rather than a full FP-Growth implementation
        PairMiningAlgorithm pairMiner = new PairMiningAlgorithm(
            config.getMinSupport(), config.getMinConfidence(), config.getMinLift());

        Map<Long, List<AssociationRule>> rulesMap = pairMiner.mineRules(transactions);

        Map<Long, List<Long>> newCache = new HashMap<>();
        for (Map.Entry<Long, List<AssociationRule>> entry : rulesMap.entrySet()) {
            List<Long> recommendedIds = entry.getValue().stream()
                .map(AssociationRule::getConsequent)
                .limit(config.getMaxBoughtTogether())
                .collect(Collectors.toList());
            newCache.put(entry.getKey(), recommendedIds);
        }
        return newCache;
    }

    private Map<Long, List<Long>> computeSimilarBooks() {
        List<Book> allBooks = bookRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        if (allBooks.isEmpty()) return Collections.emptyMap();

        // Build lookup by author and category for quick grouping
        Map<String, List<Book>> byAuthor = new HashMap<>();
        Map<Long, List<Book>> byCategory = new HashMap<>();
        for (Book b : allBooks) {
            if (b.getAuthor() != null) byAuthor.computeIfAbsent(b.getAuthor(), k -> new ArrayList<>()).add(b);
            Category c = b.getCategory();
            if (c != null) byCategory.computeIfAbsent(c.getId(), k -> new ArrayList<>()).add(b);
        }

        Map<Long, List<Long>> newCache = new HashMap<>();

        for (Book source : allBooks) {
            List<Long> picks = new ArrayList<>();
            // 1) Same author (exclude self)
            if (source.getAuthor() != null) {
                List<Book> sameAuthor = byAuthor.getOrDefault(source.getAuthor(), Collections.emptyList());
                for (Book b : sameAuthor) {
                    if (picks.size() >= config.getMaxSimilar()) break;
                    if (b.getId().equals(source.getId())) continue;
                    picks.add(b.getId());
                }
            }

            // 2) Same category (if still need more)
            if (picks.size() < config.getMaxSimilar()) {
                Category c = source.getCategory();
                if (c != null) {
                    List<Book> sameCat = byCategory.getOrDefault(c.getId(), Collections.emptyList());
                    for (Book b : sameCat) {
                        if (picks.size() >= config.getMaxSimilar()) break;
                        if (b.getId().equals(source.getId())) continue;
                        if (picks.contains(b.getId())) continue;
                        picks.add(b.getId());
                    }
                }
            }

            newCache.put(source.getId(), picks);
        }
        return newCache;
    }
}
