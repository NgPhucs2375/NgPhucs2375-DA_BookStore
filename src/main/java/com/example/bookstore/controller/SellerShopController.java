package com.example.bookstore.controller;

import com.example.bookstore.dto.SellerShopResponse;
import com.example.bookstore.dto.SellerShopUpsertRequest;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.service.SellerShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SellerShopController {

    private final SellerShopService shopService;

    // ==========================================
    // ENDPOINTS DÀNH CHO SELLER QUẢN LÝ SHOP
    // ==========================================

    @GetMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> getMyShop(
            @RequestHeader("X-User-Id") Long sellerId
    ) {
        SellerShopResponse response = shopService.getMyShop(sellerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> createMyShop(
            @RequestHeader("X-User-Id") Long sellerId,
            @Valid @RequestBody SellerShopUpsertRequest request
    ) {
        SellerShopResponse response = shopService.createMyShop(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> updateMyShop(
            @RequestHeader("X-User-Id") Long sellerId,
            @Valid @RequestBody SellerShopUpsertRequest request
    ) {
        SellerShopResponse response = shopService.updateMyShop(sellerId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/seller/me/shop/status")
    public ResponseEntity<SellerShopResponse> changeStatus(
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestParam ApprovalStatus status
    ) {
        SellerShopResponse response = shopService.changeStatus(sellerId, status);
        return ResponseEntity.ok(response);
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
     * Seller ID được truyền từ X-User-Id header (được set bởi JwtAuthenticationFilter)
     * Không cần lấy từ SecurityContext vì đã được xác thực ở filter level
     */

}