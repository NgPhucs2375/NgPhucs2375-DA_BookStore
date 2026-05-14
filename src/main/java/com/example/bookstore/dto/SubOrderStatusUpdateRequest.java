package com.example.bookstore.dto;

import com.example.bookstore.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating sub-order status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubOrderStatusUpdateRequest {
    @NotNull
    private OrderStatus status;

    private String note; // Optional note for status change
}
