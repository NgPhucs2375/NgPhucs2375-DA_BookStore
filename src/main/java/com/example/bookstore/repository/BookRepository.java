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

    // S02: Lấy sách theo trạng thái (Có phân trang cho Admin)
    Page<Book> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    List<Book> findByApprovalStatus(ApprovalStatus approvalStatus);

        @Query("""
            SELECT b FROM Book b
            LEFT JOIN b.category c
            WHERE b.approvalStatus = :status
              AND (
                :q IS NULL
                OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (
                :categoryId IS NULL
                OR c.id = :categoryId
              )
            """)
        Page<Book> searchApprovedBooks(
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("status") ApprovalStatus status,
            Pageable pageable
        );
}
