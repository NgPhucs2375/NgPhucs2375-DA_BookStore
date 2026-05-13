package com.example.bookstore.repository;

import com.example.bookstore.model.SubOrder;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {
    List<SubOrder> findBySeller(User seller);

    List<SubOrder> findBySellerOrderByIdDesc(User seller);

    List<SubOrder> findBySellerAndStatus(User seller, OrderStatus status);

    /**
     * Find sub-orders by seller and status
     */
    @Query("SELECT so FROM SubOrder so WHERE so.seller = :seller AND so.status = :status " +
           "ORDER BY so.id DESC")
    List<SubOrder> findBySellerAndStatusOrdered(@Param("seller") User seller,
                                                @Param("status") OrderStatus status);

    /**
     * Find sub-orders by seller with date range filter
     */
    @Query("SELECT so FROM SubOrder so WHERE so.seller = :seller " +
           "AND so.parentOrder.createdAt >= :createdFrom AND so.parentOrder.createdAt <= :createdTo " +
           "ORDER BY so.parentOrder.createdAt DESC")
    List<SubOrder> findBySellerAndDateRange(@Param("seller") User seller,
                                            @Param("createdFrom") LocalDateTime createdFrom,
                                            @Param("createdTo") LocalDateTime createdTo);

    /**
     * Find sub-orders by seller with price range filter
     */
    @Query("SELECT so FROM SubOrder so WHERE so.seller = :seller " +
           "AND so.subTotal >= :minPrice AND so.subTotal <= :maxPrice " +
           "ORDER BY so.id DESC")
    List<SubOrder> findBySellerAndPriceRange(@Param("seller") User seller,
                                             @Param("minPrice") Double minPrice,
                                             @Param("maxPrice") Double maxPrice);

    /**
     * Find sub-orders with combined filters for seller
     */
    @Query("SELECT so FROM SubOrder so WHERE so.seller = :seller " +
           "AND (:status IS NULL OR so.status = :status) " +
           "AND (:createdFrom IS NULL OR so.parentOrder.createdAt >= :createdFrom) " +
           "AND (:createdTo IS NULL OR so.parentOrder.createdAt <= :createdTo) " +
           "AND (:minPrice IS NULL OR so.subTotal >= :minPrice) " +
           "AND (:maxPrice IS NULL OR so.subTotal <= :maxPrice) " +
           "ORDER BY so.id DESC")
    Page<SubOrder> findBySellerWithFilters(@Param("seller") User seller,
                                           @Param("status") OrderStatus status,
                                           @Param("createdFrom") LocalDateTime createdFrom,
                                           @Param("createdTo") LocalDateTime createdTo,
                                           @Param("minPrice") Double minPrice,
                                           @Param("maxPrice") Double maxPrice,
                                           Pageable pageable);

    /**
     * Find sub-orders by seller and buyer name pattern
     */
    @Query("SELECT so FROM SubOrder so WHERE so.seller = :seller " +
           "AND LOWER(so.parentOrder.buyer.username) LIKE LOWER(CONCAT('%', :buyerName, '%')) " +
           "ORDER BY so.id DESC")
    List<SubOrder> findBySellerAndBuyerNameContaining(@Param("seller") User seller,
                                                      @Param("buyerName") String buyerName);
}
