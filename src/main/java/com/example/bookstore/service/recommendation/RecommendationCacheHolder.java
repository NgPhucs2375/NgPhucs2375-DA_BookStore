package com.example.bookstore.service.recommendation;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class RecommendationCacheHolder {

    private final AtomicReference<RecommendationCache> ref = new AtomicReference<>(RecommendationCache.empty());

    public RecommendationCache get() {
        return ref.get();
    }

    public void swap(RecommendationCache newCache) {
        if (newCache == null) return;
        ref.set(newCache);
    }
}
