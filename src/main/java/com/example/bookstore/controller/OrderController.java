package com.example.bookstore.controller;

import com.example.bookstore.dto.CheckoutMeRequest;
import com.example.bookstore.dto.CheckoutRequest;
import com.example.bookstore.dto.CheckoutResponse;
import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.dto.SubOrderSummaryResponse;
import com.example.bookstore.dto.OrderFilterRequest;
import com.example.bookstore.dto.OrderFilterResponse;
import com.example.bookstore.dto.OrderSummaryResponse;
import com.example.bookstore.dto.SubOrderFilterRequest;
import com.example.bookstore.dto.SubOrderFilterResponse;
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

    @PostMapping("/me/filter/summary")
    public List<OrderSummaryResponse> getCurrentBuyerOrderSummaries(@RequestHeader("X-User-Id") Long buyerId) {
        return orderService.getCurrentBuyerOrderSummaries(buyerId);
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

    @PatchMapping("/me/{orderId}/cancel")
    public OrderSummaryResponse cancelCurrentBuyerOrder(
            @RequestHeader("X-User-Id") Long buyerId,
            @PathVariable Long orderId
    ) {
        return orderService.cancelCurrentBuyerOrder(buyerId, orderId);
    }

    /**
     * Filter buyer's orders with flexible filtering options
     * 
     * Example: GET /api/orders/me/filter?page=0&pageSize=10&sortBy=createdAt&sortDirection=DESC
     */
    @PostMapping("/me/filter")
    public OrderFilterResponse filterMyOrders(
            @RequestHeader("X-User-Id") Long buyerId,
            @RequestBody(required = false) OrderFilterRequest filter
    ) {
        if (filter == null) {
            filter = new OrderFilterRequest();
        }
        return orderService.filterBuyerOrders(buyerId, filter);
    }

    /**
     * Get buyer's orders by specific status
     * 
     * Example: GET /api/orders/me/status/COMPLETED
     */
    @GetMapping("/me/status/{status}")
    public List<OrderSummaryResponse> getMyOrdersByStatus(
            @RequestHeader("X-User-Id") Long buyerId,
            @PathVariable OrderStatus status
    ) {
        return orderService.getBuyerOrdersByStatus(buyerId, status);
    }

    /**
     * Filter seller's sub-orders with flexible filtering options
     * 
     * Example: POST /api/orders/seller/me/filter
     */
    @PostMapping("/seller/me/filter")
    public SubOrderFilterResponse filterMySubOrders(
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestBody(required = false) SubOrderFilterRequest filter
    ) {
        if (filter == null) {
            filter = new SubOrderFilterRequest();
        }
        return orderService.filterSellerSubOrders(sellerId, filter);
    }

    /**
     * Get seller's sub-orders by specific status
     * 
     * Example: GET /api/orders/seller/me/status/CONFIRMED
     */
    @GetMapping("/seller/me/status/{status}")
    public List<SubOrderSummaryResponse> getMySubOrdersByStatus(
            @RequestHeader("X-User-Id") Long sellerId,
            @PathVariable OrderStatus status
    ) {
        return orderService.getSellerSubOrdersByStatus(sellerId, status);
    }

    /**
     * Search seller's sub-orders by buyer name
     * 
     * Example: GET /api/orders/seller/me/search?buyerName=john
     */
    @GetMapping("/seller/me/search")
    public List<SubOrderSummaryResponse> searchMySubOrdersByBuyer(
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestParam(required = true) String buyerName
    ) {
        return orderService.searchSellerSubOrdersByBuyer(sellerId, buyerName);
    }
}
