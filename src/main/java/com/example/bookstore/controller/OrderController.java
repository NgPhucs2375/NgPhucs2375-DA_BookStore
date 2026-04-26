package com.example.bookstore.controller;

import com.example.bookstore.dto.CheckoutMeRequest;
import com.example.bookstore.dto.CheckoutRequest;
import com.example.bookstore.dto.CheckoutResponse;
import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.dto.SubOrderSummaryResponse;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkoutFromCart(request);
    }

    @PostMapping("/me/checkout")
    public CheckoutResponse checkoutForCurrentBuyer(
            @RequestHeader("X-User-Id") Long buyerId,
            @Valid @RequestBody CheckoutMeRequest request
    ) {
        // Transitional user context before JWT is integrated.
        return orderService.checkoutFromCurrentBuyer(buyerId, request.getShippingAddress());
    }

    @GetMapping("/buyer/{buyerId}")
    public List<Order> getBuyerOrders(@PathVariable Long buyerId) {
        return orderService.getBuyerOrders(buyerId);
    }

    @GetMapping("/me")
    public List<Order> getCurrentBuyerOrders(@RequestHeader("X-User-Id") Long buyerId) {
        return orderService.getCurrentBuyerOrders(buyerId);
    }

    @GetMapping("/me/{orderId}")
    public OrderDetailResponse getCurrentBuyerOrderDetail(
            @RequestHeader("X-User-Id") Long buyerId,
            @PathVariable Long orderId
    ) {
        return orderService.getCurrentBuyerOrderDetail(buyerId, orderId);
    }

    @GetMapping("/seller/{sellerId}/sub-orders")
    public List<SubOrderSummaryResponse> getSellerSubOrders(
            @PathVariable Long sellerId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        if (currentUserId != null && !currentUserId.equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller cannot access other seller orders");
        }
        return orderService.getSellerSubOrders(sellerId);
    }

    @GetMapping("/seller/me/sub-orders")
    public List<SubOrderSummaryResponse> getCurrentSellerSubOrders(
            @RequestHeader("X-User-Id") Long sellerId
    ) {
        return orderService.getSellerSubOrders(sellerId);
    }

    @PatchMapping("/sub-orders/{subOrderId}/status")
    public SubOrderSummaryResponse updateSubOrderStatus(
            @RequestHeader("X-User-Id") Long sellerId,
            @PathVariable Long subOrderId,
            @RequestParam OrderStatus status
    ) {
        return orderService.updateSubOrderStatusForSeller(sellerId, subOrderId, status);
    }
}
