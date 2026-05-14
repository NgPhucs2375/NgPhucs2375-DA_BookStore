package com.example.bookstore.dto;

import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chi tiết đầy đủ của một SubOrder cho Seller view
 */
@Data
@Builder
public class SubOrderDetailResponse {
    private Long subOrderId;
    private Long orderId;
    private Long sellerId;
    private String sellerName;
    private Long buyerId;
    private String buyerUsername;
    private String shippingAddress;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private Double subTotal;
    private Double refundAmount;
    private String refundReason;
    private LocalDateTime refundedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private LocalDateTime createdAt;
    private Integer itemCount;
    private List<SubOrderItemResponse> items;
    private List<StatusHistoryResponse> statusHistory;

    @Data
    @Builder
    public static class SubOrderItemResponse {
        private Long itemId;
        private Long bookId;
        private String title;
        private String author;
        private String imageUrl;
        private Double unitPrice;
        private Integer quantity;
        private Double lineTotal;
        private Boolean stockDeducted;
    }

    @Data
    @Builder
    public static class StatusHistoryResponse {
        private Long historyId;
        private String fromStatus;
        private String toStatus;
        private String changedBy;
        private String changedByRole;
        private String note;
        private LocalDateTime createdAt;
    }
}
