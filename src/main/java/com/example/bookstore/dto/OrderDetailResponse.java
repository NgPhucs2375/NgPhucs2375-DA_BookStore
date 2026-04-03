package com.example.bookstore.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {
    private Long orderId;
    private Long buyerId;
    private String buyerUsername;
    private String shippingAddress;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private Integer subOrderCount;
    private Integer totalItems;
    private List<OrderItemDetailResponse> items;
}
