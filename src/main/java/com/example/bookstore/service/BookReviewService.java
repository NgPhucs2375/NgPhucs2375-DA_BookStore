package com.example.bookstore.service;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.BookReview;
import com.example.bookstore.model.User;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.BookReviewRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookReviewService {

    @Autowired
    private BookReviewRepository reviewRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Thêm đánh giá mới với kiểm tra điều kiện khắt khe (Dùng userId)
     */
    @Transactional
    public BookReview addReview(Long userId, Long bookId, Integer rating, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return addReview(user, bookId, rating, comment);
    }

    /**
     * Thêm đánh giá mới với kiểm tra điều kiện khắt khe
     */
    @Transactional
    public BookReview addReview(User user, Long bookId, Integer rating, String comment) {
        // 1. Kiểm tra sách có tồn tại không
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + bookId));

        // 2. Kiểm tra điều kiện: Đã mua và đơn hàng COMPLETED
        boolean hasPurchased = orderItemRepository.hasUserPurchasedBook(user, bookId);
        if (!hasPurchased) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá những cuốn sách đã mua và giao hàng thành công.");
        }

        // 3. Kiểm tra xem đã đánh giá cuốn này chưa (tránh spam)
        if (reviewRepository.existsByBookAndUser(book, user)) {
            throw new IllegalStateException("Bạn đã đánh giá cuốn sách này rồi.");
        }

        // 4. Tạo và lưu đánh giá
        BookReview review = BookReview.builder()
                .book(book)
                .user(user)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .isHidden(false)
                .build();

        return reviewRepository.save(review);
    }

    /**
     * Lấy danh sách đánh giá của một cuốn sách (có phân trang)
     */
    public Page<BookReview> getBookReviews(Long bookId, Pageable pageable) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
        return reviewRepository.findByBookAndIsHiddenFalse(book, pageable);
    }

    /**
     * Tính điểm trung bình sao cho sách
     * Nếu không có đánh giá nào, trả về 0.0
     */
    public Double getAverageRating(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

        Double avg = reviewRepository.findAverageRatingByBook(book);
        return (avg != null) ? Math.round(avg * 10.0) / 10.0 : 0.0; // Làm tròn 1 chữ số thập phân
    }

    /**
     * Lấy tổng số lượt đánh giá
     */
    public long getTotalReviews(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
        return reviewRepository.countByBookAndIsHiddenFalse(book);
    }

    /**
     * Ẩn hoặc hiện một đánh giá (Dành cho Admin kiểm duyệt)
     */
    @Transactional
    public BookReview toggleReviewVisibility(Long reviewId) {
        BookReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId));
        
        review.setHidden(!review.isHidden());
        return reviewRepository.save(review);
    }

    /**
     * Lấy tất cả đánh giá của một cuốn sách (bao gồm cả bị ẩn - dành cho Admin)
     */
    public Page<BookReview> getAllBookReviewsForAdmin(Long bookId, Pageable pageable) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
        // Ta cần thêm method này vào Repository nếu chưa có, hoặc dùng Example/Specification
        // Nhưng đơn giản nhất là findByBook
        return reviewRepository.findAllByBook(book, pageable);
    }
}
