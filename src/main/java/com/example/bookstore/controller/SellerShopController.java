package com.example.bookstore.controller;

import com.example.bookstore.dto.SellerShopResponse;
import com.example.bookstore.dto.SellerShopUpsertRequest;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.security.JwtAuthenticatedPrincipal;
import com.example.bookstore.service.SellerShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
 

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SellerShopController {

    private final SellerShopService shopService;

    // ==========================================
    // ENDPOINTS DÀNH CHO SELLER QUẢN LÝ SHOP
    // ==========================================

    @GetMapping("/seller/me/shop")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerShopResponse> getMyShop(@org.springframework.security.core.annotation.AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        Long sellerId = getCurrentSellerId(principal);
        SellerShopResponse response = shopService.getMyShop(sellerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seller/me/shop")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerShopResponse> createMyShop(
            @Valid @RequestBody SellerShopUpsertRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal JwtAuthenticatedPrincipal principal
    ) {
        Long sellerId = getCurrentSellerId(principal);
        SellerShopResponse response = shopService.createMyShop(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/seller/me/shop")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerShopResponse> updateMyShop(
            @Valid @RequestBody SellerShopUpsertRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal JwtAuthenticatedPrincipal principal
    ) {
        Long sellerId = getCurrentSellerId(principal);
        SellerShopResponse response = shopService.updateMyShop(sellerId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/seller/me/shop/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerShopResponse> changeStatus(
            @RequestParam ApprovalStatus status,
            @org.springframework.security.core.annotation.AuthenticationPrincipal JwtAuthenticatedPrincipal principal
    ) {
        Long sellerId = getCurrentSellerId(principal);
        SellerShopResponse response = shopService.changeStatus(sellerId, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/seller/me/shop")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteMyShop(@org.springframework.security.core.annotation.AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        Long sellerId = getCurrentSellerId(principal);
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

    private Long getCurrentSellerId(JwtAuthenticatedPrincipal principal) {
        if (principal != null) {
            if (principal.sellerId() != null) return principal.sellerId();
            return principal.userId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}