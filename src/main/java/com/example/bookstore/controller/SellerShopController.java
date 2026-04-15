package com.example.bookstore.controller;

import com.example.bookstore.dto.SellerShopResponse;
import com.example.bookstore.dto.SellerShopUpsertRequest;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.service.SellerShopService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SellerShopController {

    private final SellerShopService shopService;

    // ==========================================
    // ENDPOINTS DÀNH CHO SELLER QUẢN LÝ SHOP
    // ==========================================

    @GetMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> getMyShop(HttpServletRequest request) {
        Long sellerId = getCurrentSellerId(request);
        SellerShopResponse response = shopService.getMyShop(sellerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> createMyShop(
            @Valid @RequestBody SellerShopUpsertRequest request,
            HttpServletRequest httpRequest
    ) {
        Long sellerId = getCurrentSellerId(httpRequest);
        SellerShopResponse response = shopService.createMyShop(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> updateMyShop(
            @Valid @RequestBody SellerShopUpsertRequest request,
            HttpServletRequest httpRequest
    ) {
        Long sellerId = getCurrentSellerId(httpRequest);
        SellerShopResponse response = shopService.updateMyShop(sellerId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/seller/me/shop/status")
    public ResponseEntity<SellerShopResponse> changeStatus(
            @RequestParam ApprovalStatus status,
            HttpServletRequest httpRequest
    ) {
        Long sellerId = getCurrentSellerId(httpRequest);
        SellerShopResponse response = shopService.changeStatus(sellerId, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/seller/me/shop")
    public ResponseEntity<Void> deleteMyShop(HttpServletRequest request) {
        Long sellerId = getCurrentSellerId(request);
        shopService.deleteMyShop(sellerId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // PUBLIC ENDPOINTS DÀNH CHO KHÁCH HÀNG
    // ==========================================

    @GetMapping("/shops/{slug}")
    public ResponseEntity<SellerShopResponse> getPublicShopBySlug(@PathVariable String slug) {
        SellerShopResponse response = shopService.getPublicShopBySlug(slug);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Hàm giả lập để lấy ID của Seller đang đăng nhập.
     * TODO: Thay thế bằng logic thực tế từ Spring Security (VD: SecurityContextHolder hoặc @AuthenticationPrincipal)
     */
    private Long getCurrentSellerId(HttpServletRequest request) {
        Long sellerId = (Long) request.getAttribute("CURRENT_USER_ID");
        if (sellerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return sellerId;
    }
}