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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkoutFromCart(request);
    }

    @PostMapping("/me/checkout")
        @PreAuthorize("hasRole('BUYER')")
    public CheckoutResponse checkoutForCurrentBuyer(
            Authentication authentication,
            @Valid @RequestBody CheckoutMeRequest request
    ) {
        Long buyerId = currentUserId(authentication);
        return orderService.checkoutFromCurrentBuyer(buyerId, request.getShippingAddress());
    }

    @GetMapping("/buyer/{buyerId}")
    @PreAuthorize("hasPermission(#buyerId, 'User', 'read')")
    public List<Order> getBuyerOrders(@PathVariable Long buyerId) {
        return orderService.getBuyerOrders(buyerId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public List<Order> getCurrentBuyerOrders(Authentication authentication) {
        Long buyerId = currentUserId(authentication);
        return orderService.getCurrentBuyerOrders(buyerId);
    }

    @PostMapping("/me/filter/summary")
    @PreAuthorize("hasRole('BUYER')")
    public List<OrderSummaryResponse> getCurrentBuyerOrderSummaries(Authentication authentication) {
        Long buyerId = currentUserId(authentication);
        return orderService.getCurrentBuyerOrderSummaries(buyerId);
    }

    @GetMapping("/me/{orderId}")
    @PreAuthorize("hasPermission(#orderId, 'Order', 'read')")
    public OrderDetailResponse getCurrentBuyerOrderDetail(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long buyerId = currentUserId(authentication);
        return orderService.getCurrentBuyerOrderDetail(buyerId, orderId);
    }

    @GetMapping("/seller/{sellerId}/sub-orders")
    @PreAuthorize("hasPermission(#sellerId, 'User', 'read')")
    public List<SubOrderSummaryResponse> getSellerSubOrders(
            @PathVariable Long sellerId,
            Authentication authentication
    ) {
        return orderService.getSellerSubOrders(sellerId);
    }

    @GetMapping("/seller/me/sub-orders")
    @PreAuthorize("hasRole('SELLER')")
    public List<SubOrderSummaryResponse> getCurrentSellerSubOrders(Authentication authentication) {
        Long sellerId = currentSellerId(authentication);
        return orderService.getSellerSubOrders(sellerId);
    }

    @PatchMapping("/sub-orders/{subOrderId}/status")
    @PreAuthorize("hasPermission(#subOrderId, 'SubOrder', 'status')")
    public SubOrderSummaryResponse updateSubOrderStatus(
            Authentication authentication,
            @PathVariable Long subOrderId,
            @RequestParam OrderStatus status
    ) {
        Long sellerId = currentSellerId(authentication);
        return orderService.updateSubOrderStatusForSeller(sellerId, subOrderId, status);
    }

    @PatchMapping("/me/{orderId}/cancel")
    @PreAuthorize("hasPermission(#orderId, 'Order', 'cancel')")
    public OrderSummaryResponse cancelCurrentBuyerOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long buyerId = currentUserId(authentication);
        return orderService.cancelCurrentBuyerOrder(buyerId, orderId);
    }

    /**
     * Filter buyer's orders with flexible filtering options
     * 
     * Example: GET /api/orders/me/filter?page=0&pageSize=10&sortBy=createdAt&sortDirection=DESC
     */
    @PostMapping("/me/filter")
        @PreAuthorize("hasRole('BUYER')")
    public OrderFilterResponse filterMyOrders(
            Authentication authentication,
            @RequestBody(required = false) OrderFilterRequest filter
    ) {
        Long buyerId = currentUserId(authentication);
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
        @PreAuthorize("hasRole('BUYER')")
    public List<OrderSummaryResponse> getMyOrdersByStatus(
            Authentication authentication,
            @PathVariable OrderStatus status
    ) {
        Long buyerId = currentUserId(authentication);
        return orderService.getBuyerOrdersByStatus(buyerId, status);
    }

    /**
     * Filter seller's sub-orders with flexible filtering options
     * 
     * Example: POST /api/orders/seller/me/filter
     */
    @PostMapping("/seller/me/filter")
    @PreAuthorize("hasRole('SELLER')")
    public SubOrderFilterResponse filterMySubOrders(
            Authentication authentication,
            @RequestBody(required = false) SubOrderFilterRequest filter
    ) {
        Long sellerId = currentSellerId(authentication);
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
    @PreAuthorize("hasRole('SELLER')")
    public List<SubOrderSummaryResponse> getMySubOrdersByStatus(
            Authentication authentication,
            @PathVariable OrderStatus status
    ) {
        Long sellerId = currentSellerId(authentication);
        return orderService.getSellerSubOrdersByStatus(sellerId, status);
    }

    /**
     * Search seller's sub-orders by buyer name
     * 
     * Example: GET /api/orders/seller/me/search?buyerName=john
     */
    @GetMapping("/seller/me/search")
    @PreAuthorize("hasRole('SELLER')")
    public List<SubOrderSummaryResponse> searchMySubOrdersByBuyer(
            Authentication authentication,
            @RequestParam(required = true) String buyerName
    ) {
        Long sellerId = currentSellerId(authentication);
        return orderService.searchSellerSubOrdersByBuyer(sellerId, buyerName);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.example.bookstore.security.JwtAuthenticatedPrincipal principal) {
            return principal.userId();
        }

        return null;
    }

    private Long currentSellerId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.example.bookstore.security.JwtAuthenticatedPrincipal principal) {
            return principal.sellerId() != null ? principal.sellerId() : principal.userId();
        }

        return null;
    }
}
