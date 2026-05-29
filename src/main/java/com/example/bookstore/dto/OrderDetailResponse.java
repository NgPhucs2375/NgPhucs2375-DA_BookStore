package com.example.bookstore.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {
    private Long id;
    private String buyerName;
    private String buyerEmail;
    private Double totalAmount;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private List<SubOrderDetail> subOrders;

    @Data
    @Builder
    public static class SubOrderDetail {
        private Long id;
        private String sellerName;
        private String status;
        private Double subTotal;
        private List<OrderItemDetail> items;
    }

    @Data
    @Builder
    public static class OrderItemDetail {
        private Long id;
        private String bookTitle;
        private Double price;
        private Integer quantity;
        private Double subtotal;
    }
}