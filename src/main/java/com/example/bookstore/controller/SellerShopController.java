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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SellerShopController {

    private final SellerShopService shopService;

    // ==========================================
    // ENDPOINTS DÀNH CHO SELLER QUẢN LÝ SHOP
    // ==========================================

    @GetMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> getMyShop() {
        Long sellerId = getCurrentSellerId();
        SellerShopResponse response = shopService.getMyShop(sellerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> createMyShop(@Valid @RequestBody SellerShopUpsertRequest request) {
        Long sellerId = getCurrentSellerId();
        SellerShopResponse response = shopService.createMyShop(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/seller/me/shop")
    public ResponseEntity<SellerShopResponse> updateMyShop(@Valid @RequestBody SellerShopUpsertRequest request) {
        Long sellerId = getCurrentSellerId();
        SellerShopResponse response = shopService.updateMyShop(sellerId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/seller/me/shop/status")
    public ResponseEntity<SellerShopResponse> changeStatus(@RequestParam ApprovalStatus status) {
        Long sellerId = getCurrentSellerId();
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
     * Hàm giả lập để lấy ID của Seller đang đăng nhập.
     * TODO: Thay thế bằng logic thực tế từ Spring Security (VD: SecurityContextHolder hoặc @AuthenticationPrincipal)
     */
    private Long getCurrentSellerId() {
        // Ví dụ nếu dùng Spring Security:
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        // return userDetails.getId();
        
        return 1L; // Tạm thời trả về 1L để code không bị lỗi khi test
    }
}