package com.example.bookstore.service.recommendation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class RecommendationCache {

    private final Map<Long, List<Long>> boughtTogether;
    private final Map<Long, List<Long>> similarBooks;

    public RecommendationCache(Map<Long, List<Long>> boughtTogether, Map<Long, List<Long>> similarBooks) {
        this.boughtTogether = boughtTogether == null ? Collections.emptyMap() : Collections.unmodifiableMap(boughtTogether);
        this.similarBooks = similarBooks == null ? Collections.emptyMap() : Collections.unmodifiableMap(similarBooks);
    }

    public static RecommendationCache empty() {
        return new RecommendationCache(Collections.emptyMap(), Collections.emptyMap());
    }

    public List<Long> getBoughtTogether(Long bookId) {
        return boughtTogether.getOrDefault(bookId, Collections.emptyList());
    }

    public List<Long> getSimilarBooks(Long bookId) {
        return similarBooks.getOrDefault(bookId, Collections.emptyList());
    }

    public Map<Long, List<Long>> getAllBoughtTogether() { return boughtTogether; }
    public Map<Long, List<Long>> getAllSimilarBooks() { return similarBooks; }
}
