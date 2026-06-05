package com.example.bookstore.repository;

import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>{
    // Ví dụ: Tìm sách theo tiêu đề để hỗ trợ gợi ý sản phẩm
    List<Book> findByTitleContaining(String title);

    List<Book> findBySeller(User seller);

    @Query("SELECT b.approvalStatus FROM Book b WHERE b.id = :bookId")
    ApprovalStatus findApprovalStatusById(@Param("bookId") Long bookId);

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END
            FROM Book b
            WHERE b.id = :bookId
              AND b.seller.id = :sellerId
            """)
    boolean existsByIdAndSellerId(@Param("bookId") Long bookId, @Param("sellerId") Long sellerId);

    // S02: Lấy sách theo trạng thái (Có phân trang cho Admin)
    Page<Book> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    List<Book> findByApprovalStatus(ApprovalStatus approvalStatus);

    // 🆕 NEW: Các phương thức hỗ trợ Admin quản lý
    List<Book> findByIsActive(boolean isActive);

    Page<Book> findByIsActive(boolean isActive, Pageable pageable);

    Page<Book> findByApprovalStatusAndIsActive(
            ApprovalStatus status,
            boolean isActive,
            Pageable pageable
    );

    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("""
        SELECT b FROM Book b
        LEFT JOIN b.category c
        LEFT JOIN b.seller s
        WHERE b.approvalStatus = :status
          AND b.isActive = true
          AND (
            :q IS NULL
            OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(b.publisher, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
          AND (:categoryIds IS NULL OR c.id IN :categoryIds)
          AND (:sellerIds IS NULL OR s.id IN :sellerIds)
          AND (:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:minPrice IS NULL OR b.price >= :minPrice)
          AND (:maxPrice IS NULL OR b.price <= :maxPrice)
          AND (:minRating IS NULL OR b.averageRating >= :minRating)
          AND (:inStock IS NULL OR :inStock = false OR b.stockQuantity > 0)
          AND (:publishYearFrom IS NULL OR b.publishYear >= :publishYearFrom)
          AND (:publishYearTo IS NULL OR b.publishYear <= :publishYearTo)
        """)
    Page<Book> searchApprovedBooks(
            @Param("q") String q,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("sellerIds") List<Long> sellerIds,
            @Param("author") String author,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minRating") Double minRating,
            @Param("inStock") Boolean inStock,
            @Param("publishYearFrom") Integer publishYearFrom,
            @Param("publishYearTo") Integer publishYearTo,
            @Param("status") ApprovalStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT b FROM Book b
            WHERE b.approvalStatus = :status
              AND (
                :q IS NULL
                OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY
              CASE
                WHEN :q IS NOT NULL AND LOWER(b.title) LIKE LOWER(CONCAT(:q, '%')) THEN 0
                WHEN :q IS NOT NULL AND LOWER(b.author) LIKE LOWER(CONCAT(:q, '%')) THEN 1
                ELSE 2
              END,
              b.title ASC
            """)
    List<Book> findSuggestions(@Param("q") String q,
                               @Param("status") ApprovalStatus status,
                               Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            JOIN OrderItem oi ON oi.book = b
            JOIN oi.subOrder so
            JOIN so.parentOrder o
            WHERE b.approvalStatus = :status
            GROUP BY b
            ORDER BY SUM(oi.quantity) DESC, MAX(o.createdAt) DESC, b.id DESC
            """)
    List<Book> findBestSellingBooks(@Param("status") ApprovalStatus status, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            JOIN OrderItem oi ON oi.book = b
            JOIN oi.subOrder so
            JOIN so.parentOrder o
            WHERE b.approvalStatus = :status
              AND o.createdAt >= :since
            GROUP BY b
            ORDER BY SUM(oi.quantity) DESC, MAX(o.createdAt) DESC, b.id DESC
            """)
    List<Book> findTrendingBooks(@Param("status") ApprovalStatus status,
                                 @Param("since") java.time.LocalDateTime since,
                                 Pageable pageable);

    /**
     * Tìm sách của seller (dùng cho Inventory Management - S03)
     * Bao gồm tất cả trạng thái: PENDING, APPROVED, REJECTED
     */
    @Query("""
            SELECT b FROM Book b
            LEFT JOIN b.category c
            WHERE b.seller.id = :sellerId
              AND (
                :q IS NULL
                OR :q = ''
                OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (
                :categoryId IS NULL
                OR c.id = :categoryId
              )
            """)
    Page<Book> findBySellerIdAndKeywordAndCategory(
            @Param("sellerId") Long sellerId,
            @Param("q") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}