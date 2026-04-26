package com.example.bookstore.repository;

import com.example.bookstore.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Lấy danh sách các cặp (OrderId, BookId) để xây dựng Baskets cho thuật toán FP-Growth.
     * Mỗi Order có thể có nhiều SubOrder, mỗi SubOrder có nhiều OrderItem (cuốn sách).
     * Bằng cách join này, ta gộp tất cả sách mua trong cùng 1 lần thanh toán thành 1 giỏ hàng (basket).
     */
    @Query("SELECT o.subOrder.parentOrder.id, o.book.id FROM OrderItem o WHERE o.subOrder.parentOrder IS NOT NULL")
    List<Object[]> findAllOrderBookPairs();

}
