package com.example.bookstore.service.recommendation;

import com.example.bookstore.config.RecommendationConfig;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.service.recommendation.cosine.CosineSimilarityAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RecommendationService {

    @Autowired
    private RecommendationConfig config;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RecommendationCacheHolder cacheHolder;

    @Autowired
    private RecommendationFallbackEngine fallbackEngine;

    @Autowired
    private CosineSimilarityAlgorithm cosineAlgorithm; // may still be used elsewhere

    public List<Book> getSimilarBooks(Long bookId) {
        List<Long> cachedIds = cacheHolder.get().getSimilarBooks(bookId);
        List<Book> result = new ArrayList<>();
        if (!cachedIds.isEmpty()) {
            result = bookRepository.findAllById(cachedIds).stream()
                    .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .toList();
        }

        if (result.size() < config.getMaxSimilar()) {
            Optional<Book> currentBookOpt = bookRepository.findById(bookId);
            if (currentBookOpt.isPresent()) {
                List<Book> fallback = fallbackEngine.fallbackSameAuthorOrCategory(currentBookOpt.get(), config.getMaxSimilar() - result.size(), result);
                result.addAll(fallback);
            }
        }
        return result;
    }

    public List<Book> getBoughtTogetherBooks(Long bookId) {
        List<Long> cachedIds = cacheHolder.get().getBoughtTogether(bookId);
        List<Book> result = new ArrayList<>();
        if (!cachedIds.isEmpty()) {
            result = bookRepository.findAllById(cachedIds).stream()
                    .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .toList();
        }

        if (result.size() < config.getMaxBoughtTogether()) {
            Optional<Book> currentBookOpt = bookRepository.findById(bookId);
            if (currentBookOpt.isPresent()) {
                List<Book> fallback = fallbackEngine.fallbackSameAuthorOrCategory(currentBookOpt.get(), config.getMaxBoughtTogether() - result.size(), result);
                result.addAll(fallback);
            }
        }
        return result;
    }

}
