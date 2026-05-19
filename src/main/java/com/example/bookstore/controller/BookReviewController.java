package com.example.bookstore.controller;

import com.example.bookstore.dto.ReviewRequest;
import com.example.bookstore.model.BookReview;
import com.example.bookstore.security.JwtAuthenticatedPrincipal;
import com.example.bookstore.service.BookReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin("*")
public class BookReviewController {

    @Autowired
    private BookReviewService reviewService;

    /**
     * Lấy danh sách đánh giá của một cuốn sách (Public)
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<?> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BookReview> reviews = reviewService.getBookReviews(bookId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Lấy thống kê đánh giá của một cuốn sách (Public)
     */
    @GetMapping("/book/{bookId}/stats")
    public ResponseEntity<?> getBookReviewStats(@PathVariable Long bookId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", reviewService.getAverageRating(bookId));
        stats.put("totalReviews", reviewService.getTotalReviews(bookId));
        return ResponseEntity.ok(stats);
    }

    /**
     * Thêm đánh giá mới (Yêu cầu đăng nhập và đã mua hàng)
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<?> addReview(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestBody ReviewRequest request
    ) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập để đánh giá");
            }
            
            BookReview review = reviewService.addReview(
                    principal.userId(),
                    request.getBookId(),
                    request.getRating(),
                    request.getComment()
            );
            return ResponseEntity.ok(review);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
