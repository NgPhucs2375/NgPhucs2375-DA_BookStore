package com.example.bookstore.repository;

import com.example.bookstore.model.Order;
import com.example.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyer(User buyer);

    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);

    // 🆕 NEW: Các phương thức hỗ trợ Admin quản lý đơn hàng
    Page<Order> findAll(Pageable pageable);

    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Tìm kiếm đơn hàng theo từ khóa trong tên người mua hoặc mã đơn hàng
    @Query("""
        SELECT o FROM Order o
        WHERE CAST(o.id AS string) LIKE %:keyword% 
        OR LOWER(o.buyer.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY o.createdAt DESC
    """)
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
}
