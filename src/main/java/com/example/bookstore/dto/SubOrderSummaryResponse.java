package com.example.bookstore.dto;

import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubOrderSummaryResponse {
    private Long subOrderId;
    private Long orderId;
    private Long sellerId;
    private String sellerName;
    private Long buyerId;
    private String buyerUsername;
    private String shippingAddress;
    private String itemSummary;
    private Integer itemCount;
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
}
