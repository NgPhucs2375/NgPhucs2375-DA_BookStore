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

              @Query("""
                                          SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END
                                          FROM Order o
                                          WHERE o.id = :orderId
                                                 AND o.buyer.id = :buyerId
                                          """)
              boolean existsByIdAndBuyerId(@Param("orderId") Long orderId, @Param("buyerId") Long buyerId);

    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);

    /**
     * Find orders by buyer with date range filter
     */
    @Query("SELECT o FROM Order o WHERE o.buyer = :buyer " +
           "AND o.createdAt >= :createdFrom AND o.createdAt <= :createdTo " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByBuyerAndDateRange(@Param("buyer") User buyer,
                                        @Param("createdFrom") LocalDateTime createdFrom,
                                        @Param("createdTo") LocalDateTime createdTo);

    /**
     * Find orders by buyer with price range filter
     */
    @Query("SELECT o FROM Order o WHERE o.buyer = :buyer " +
           "AND o.totalAmount >= :minPrice AND o.totalAmount <= :maxPrice " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByBuyerAndPriceRange(@Param("buyer") User buyer,
                                         @Param("minPrice") Double minPrice,
                                         @Param("maxPrice") Double maxPrice);

    /**
     * Find orders with combined filters
     */
    @Query("SELECT o FROM Order o WHERE o.buyer = :buyer " +
           "AND (:createdFrom IS NULL OR o.createdAt >= :createdFrom) " +
           "AND (:createdTo IS NULL OR o.createdAt <= :createdTo) " +
           "AND (:minPrice IS NULL OR o.totalAmount >= :minPrice) " +
           "AND (:maxPrice IS NULL OR o.totalAmount <= :maxPrice) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findByBuyerWithFilters(@Param("buyer") User buyer,
                                       @Param("createdFrom") LocalDateTime createdFrom,
                                       @Param("createdTo") LocalDateTime createdTo,
                                       @Param("minPrice") Double minPrice,
                                       @Param("maxPrice") Double maxPrice,
                                       Pageable pageable);
}
