package com.example.bookstore.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutResponse {
    private Long orderId;
    private Long buyerId;
    private String shippingAddress;
    private Double totalAmount;
    private Integer subOrderCount;
}
