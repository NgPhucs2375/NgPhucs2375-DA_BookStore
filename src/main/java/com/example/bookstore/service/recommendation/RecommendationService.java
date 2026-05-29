package com.example.bookstore.service.recommendation;

import com.example.bookstore.config.RecommendationConfig;
import com.example.bookstore.model.AssociationRule;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.repository.AssociationRuleRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.service.recommendation.cosine.CosineSimilarityAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RecommendationService: API layer for book recommendations.
 * 
 * Changes from in-memory cache approach:
 * - getBoughtTogetherBooks(): Query database association_rules table
 * - getSimilarBooks(): Use CosineSimilarityAlgorithm on books in same category
 * - Removed dependency on RecommendationCacheHolder (no more in-memory state)
 * - Added fallback engine for graceful degradation
 */
@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final BigDecimal MIN_CONFIDENCE = BigDecimal.valueOf(0.3);  // 30% minimum

    @Autowired
    private RecommendationConfig config;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AssociationRuleRepository associationRuleRepository;

    @Autowired
    private RecommendationFallbackEngine fallbackEngine;

    @Autowired
    private CosineSimilarityAlgorithm cosineAlgorithm;

    /**
     * Get "bought together" recommendations from mined association rules.
     * 
     * Algorithm:
     * 1. Query database for rules where bookA = given bookId
     * 2. Filter by confidence >= 30% and lift > 1.0
     * 3. Return top N books sorted by confidence DESC, lift DESC
     * 4. If insufficient results, fallback to same author/category
     * 
     * @param bookId Source book ID
     * @return List of recommended books (up to maxBoughtTogether)
     */
    public List<Book> getBoughtTogetherBooks(Long bookId) {
        logger.debug("[RecommendationService] Getting 'bought together' for bookId={}", bookId);
        
        List<Book> result = new ArrayList<>();
        
        try {
            // Step 1: Query database for association rules
            List<AssociationRule> rules = associationRuleRepository.findBoughtTogetherByBookId(
                bookId, 
                MIN_CONFIDENCE
            );
            
            logger.debug("[RecommendationService] Found {} rules for bookId={}", rules.size(), bookId);
            
            // Step 2: Convert rules to Book objects (limit to maxBoughtTogether)
            if (!rules.isEmpty()) {
                result = rules.stream()
                    .limit(config.getMaxBoughtTogether())
                    .map(rule -> rule.getBookB())
                    .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .collect(Collectors.toList());
                
                logger.info("[RecommendationService] Returning {} 'bought together' books for bookId={}", 
                    result.size(), bookId);
            }
            
        } catch (Exception e) {
            logger.error("[RecommendationService] Error querying association rules for bookId={}", bookId, e);
        }

        // Step 3: Fallback if insufficient results
        if (result.size() < config.getMaxBoughtTogether()) {
            Optional<Book> currentBookOpt = bookRepository.findById(bookId);
            if (currentBookOpt.isPresent()) {
                int needed = config.getMaxBoughtTogether() - result.size();
                logger.debug("[RecommendationService] Fallback: need {} more recommendations", needed);
                
                List<Book> fallback = fallbackEngine.fallbackSameAuthorOrCategory(
                    currentBookOpt.get(), 
                    needed, 
                    result
                );
                result.addAll(fallback);
                logger.info("[RecommendationService] After fallback: {} recommendations", result.size());
            }
        }

        return result;
    }

    /**
     * Get "similar books" recommendations using CosineSimilarity algorithm.
     * 
     * Algorithm:
     * 1. Fetch source book from database
     * 2. Get all approved books in same category
     * 3. Calculate cosine similarity (author + category + text TF-IDF)
     * 4. Return top N by similarity score
     * 5. If insufficient, fallback to same author/category
     * 
     * @param bookId Source book ID
     * @return List of similar books (up to maxSimilar)
     */
    public List<Book> getSimilarBooks(Long bookId) {
        logger.debug("[RecommendationService] Getting 'similar books' for bookId={}", bookId);
        
        List<Book> result = new ArrayList<>();
        
        try {
            // Step 1: Fetch source book
            Optional<Book> sourceBookOpt = bookRepository.findById(bookId);
            if (sourceBookOpt.isEmpty()) {
                logger.warn("[RecommendationService] Source book {} not found", bookId);
                return result;
            }
            
            Book sourceBook = sourceBookOpt.get();
            
            // Step 2: Get candidate books (same category, approved)
            List<Book> candidates = new ArrayList<>();
            if (sourceBook.getCategory() != null) {
                // FIX: BookRepository may not define findByCategory(Category).
                // Use findAll() and filter by category to avoid compile error.
                candidates = bookRepository.findAll().stream()
                    .filter(b -> b.getCategory() != null && b.getCategory().equals(sourceBook.getCategory()))
                    .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .filter(b -> !b.getId().equals(bookId))  // Exclude self
                    .collect(Collectors.toList());
                
                logger.debug("[RecommendationService] Found {} candidates in same category", candidates.size());
            }
            
            // Step 3: Score each candidate using cosine similarity
            Map<Book, Double> scoredBooks = new HashMap<>();
            for (Book candidate : candidates) {
                double similarity = cosineAlgorithm.calculateSimilarity(sourceBook, candidate);
                scoredBooks.put(candidate, similarity);
                logger.trace("[RecommendationService] Similarity({}->{}): {}", 
                    bookId, candidate.getId(), similarity);
            }
            
            // Step 4: Sort by similarity score DESC and take top N
            result = scoredBooks.entrySet().stream()
                .filter(e -> e.getValue() > 0.0)  // Only include positive scores
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(config.getMaxSimilar())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            logger.info("[RecommendationService] Cosine similarity returned {} books for bookId={}", 
                result.size(), bookId);
            
        } catch (Exception e) {
            logger.error("[RecommendationService] Error calculating similar books for bookId={}", bookId, e);
        }

        // Step 5: Fallback if insufficient results
        if (result.size() < config.getMaxSimilar()) {
            Optional<Book> currentBookOpt = bookRepository.findById(bookId);
            if (currentBookOpt.isPresent()) {
                int needed = config.getMaxSimilar() - result.size();
                logger.debug("[RecommendationService] Fallback: need {} more similar books", needed);
                
                List<Book> fallback = fallbackEngine.fallbackSameAuthorOrCategory(
                    currentBookOpt.get(), 
                    needed, 
                    result
                );
                result.addAll(fallback);
                logger.info("[RecommendationService] After fallback: {} similar books", result.size());
            }
        }

        return result;
    }

}
