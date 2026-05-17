package com.example.bookstore.service.recommendation;

import com.example.bookstore.config.RecommendationConfig;
import com.example.bookstore.model.AssociationRule;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.repository.AssociationRuleRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.service.recommendation.pairmining.PairMiningAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * RecommendationJob: Background job for computing association rules from customer purchase patterns.
 * 
 * Changes from in-memory cache approach:
 * - Uses Sliding Window (last 30 days) to prevent OOM from loading all-time data
 * - Computes "bought together" rules using PairMiningAlgorithm
 * - Persists results to database instead of in-memory cache
 * - Removes expensive O(N²) computeSimilarBooks() logic (replaced by CosineSimilarity in service layer)
 * - Uses @Transactional to manage DB operations atomically
 */
@Service
public class RecommendationJob {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationJob.class);
    private static final int SLIDING_WINDOW_DAYS = 30;

    @Autowired
    private RecommendationConfig config;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AssociationRuleRepository associationRuleRepository;

    /**
     * Run at startup (5s delay) and every hour (configurable via recommendation.refresh-rate-ms)
     */
    @Scheduled(initialDelay = 5000, fixedRateString = "${recommendation.refresh-rate-ms:3600000}")
    @Transactional
    public void recompute() {
        logger.info("[RecommendationJob] Starting precompute job with {} day sliding window...", SLIDING_WINDOW_DAYS);
        
        try {
            // Step 1: Fetch order-book pairs within sliding window
            logger.debug("[RecommendationJob] Fetching order-book pairs from last {} days...", SLIDING_WINDOW_DAYS);
            LocalDateTime windowStart = LocalDateTime.now().minusDays(SLIDING_WINDOW_DAYS);
            
            List<Object[]> pairs = orderItemRepository.findOrderBookPairsByStatusesAndDateRange(
                Arrays.asList(OrderStatus.PROCESSING, OrderStatus.COMPLETED),
                windowStart
            );
            logger.info("[RecommendationJob] Fetched {} order-book pairs", pairs.size());

            if (pairs.isEmpty()) {
                logger.warn("[RecommendationJob] No recent orders found. Skipping mining.");
                return;
            }

            // Step 2: Aggregate pairs into transactions (basket per order)
            Map<Long, List<Long>> transactionsMap = new HashMap<>();
            for (Object[] row : pairs) {
                Long orderId = (Long) row[0];
                Long bookId = (Long) row[1];
                transactionsMap.putIfAbsent(orderId, new ArrayList<>());
                transactionsMap.get(orderId).add(bookId);
            }

            List<List<Long>> transactions = new ArrayList<>(transactionsMap.values());
            logger.info("[RecommendationJob] Aggregated into {} transactions (baskets)", transactions.size());

            // Step 3: Mine association rules using PairMiningAlgorithm
            logger.debug("[RecommendationJob] Mining rules with minSupport={}, minConfidence={}, minLift={}",
                config.getMinSupport(), config.getMinConfidence(), config.getMinLift());
            
            PairMiningAlgorithm pairMiner = new PairMiningAlgorithm(
                config.getMinSupport(), 
                config.getMinConfidence(), 
                config.getMinLift()
            );

            Map<Long, List<com.example.bookstore.service.recommendation.pairmining.AssociationRule>> rulesMap = 
                pairMiner.mineRules(transactions);
            
            logger.info("[RecommendationJob] Mined {} unique antecedents with rules", rulesMap.size());

            // Step 4: Convert to database entities and prepare for bulk insert
            List<AssociationRule> dbRules = convertToDatabaseEntities(rulesMap);
            logger.info("[RecommendationJob] Converted to {} database rules", dbRules.size());

            // Step 5: Clear old rules and insert new ones atomically
            logger.debug("[RecommendationJob] Clearing old association rules...");
            int deletedCount = associationRuleRepository.deleteAllRules();
            logger.info("[RecommendationJob] Deleted {} old rules", deletedCount);

            if (!dbRules.isEmpty()) {
                logger.debug("[RecommendationJob] Bulk inserting {} new rules...", dbRules.size());
                associationRuleRepository.saveAll(dbRules);
                logger.info("[RecommendationJob] Successfully inserted {} new rules", dbRules.size());
            }

            logger.info("[RecommendationJob] Precompute finished successfully");
            
        } catch (Exception e) {
            logger.error("[RecommendationJob] Error during recomputation", e);
            throw new RuntimeException("RecommendationJob failed", e);
        }
    }

    /**
     * Convert PairMiningAlgorithm outputs (Map<Long, List<AssociationRule>>) to database entities.
     * Filters top recommendations per antecedent and loads Book entities.
     * 
     * @param rulesMap Output from PairMiningAlgorithm
     * @return List of database AssociationRule entities ready for batch insert
     */
    private List<AssociationRule> convertToDatabaseEntities(
            Map<Long, List<com.example.bookstore.service.recommendation.pairmining.AssociationRule>> rulesMap) {
        
        List<AssociationRule> dbRules = new ArrayList<>();
        int maxRecommendations = config.getMaxBoughtTogether();
        
        for (Map.Entry<Long, List<com.example.bookstore.service.recommendation.pairmining.AssociationRule>> entry : rulesMap.entrySet()) {
            Long bookIdA = entry.getKey();
            List<com.example.bookstore.service.recommendation.pairmining.AssociationRule> rules = entry.getValue();
            
            // Load bookA entity
            Optional<Book> bookAOpt = bookRepository.findById(bookIdA);
            if (bookAOpt.isEmpty()) {
                logger.warn("[RecommendationJob] Book {} not found, skipping", bookIdA);
                continue;
            }
            Book bookA = bookAOpt.get();
            
            // Take top N rules by lift/confidence
            rules.stream()
                .limit(maxRecommendations)
                .forEach(rule -> {
                    // Load bookB entity
                    Optional<Book> bookBOpt = bookRepository.findById(rule.getConsequent());
                    if (bookBOpt.isEmpty()) {
                        logger.warn("[RecommendationJob] Book {} not found, skipping rule", rule.getConsequent());
                        return;
                    }
                    Book bookB = bookBOpt.get();
                    
                    // Create database entity
                    AssociationRule dbRule = new AssociationRule();
                    dbRule.setBookA(bookA);
                    dbRule.setBookB(bookB);
                    
                    // Convert double to BigDecimal with proper scale
                    dbRule.setSupport(BigDecimal.valueOf(calculateSupport(rule)).setScale(4, java.math.RoundingMode.HALF_UP));
                    dbRule.setConfidence(BigDecimal.valueOf(rule.getConfidence()).setScale(4, java.math.RoundingMode.HALF_UP));
                    dbRule.setLift(BigDecimal.valueOf(rule.getLift()).setScale(4, java.math.RoundingMode.HALF_UP));
                    dbRule.setUpdatedAt(LocalDateTime.now());
                    
                    dbRules.add(dbRule);
                    logger.debug("[RecommendationJob] Rule: {} -> {} (conf={}, lift={})", 
                        bookIdA, rule.getConsequent(), rule.getConfidence(), rule.getLift());
                });
        }
        
        return dbRules;
    }

    /**
     * Calculate support value from confidence and lift (approximation)
     * In a real scenario, this should come from the mining algorithm's internal state.
     * For now, we derive it: support_AB ≈ confidence * P(A)
     * As a conservative estimate: support = min(confidence * average_frequency, 1.0)
     */
    private double calculateSupport(com.example.bookstore.service.recommendation.pairmining.AssociationRule rule) {
        // Conservative estimate: support is at most the minimum confidence and 0.1 (10%)
        return Math.min(rule.getConfidence() * 0.1, 0.1);
    }

}

