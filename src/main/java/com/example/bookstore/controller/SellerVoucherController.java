package com.example.bookstore.controller;

import com.example.bookstore.dto.SellerVoucherCreateDTO;
import com.example.bookstore.dto.SellerVoucherResponseDTO;
import com.example.bookstore.model.Coupon;
import com.example.bookstore.model.User;
import com.example.bookstore.service.CouponService;
import com.example.bookstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/seller/vouchers")
@RequiredArgsConstructor
public class SellerVoucherController {

    private final CouponService couponService;
    private final UserService userService;

    /**
     * Get seller's current user from authentication
     */
    private Long getCurrentSellerId(Authentication authentication) {
        String username = authentication.getName();
        User seller = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return seller.getId();
    }

    /**
     * List seller's vouchers with pagination
     * GET /api/seller/vouchers?page=0&size=10&search=SAVE
     */
    @GetMapping
    public ResponseEntity<Page<?>> listSellerVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        Long sellerId = getCurrentSellerId(authentication);

        Page<Coupon> result = search != null && !search.isEmpty()
                ? couponService.searchSellerCoupons(sellerId, search, page, size)
                : couponService.listSellerCoupons(sellerId, page, size);

        // Convert to SellerVoucherResponseDTO
        Page<SellerVoucherResponseDTO> response = result.map(this::mapToResponseDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific voucher details
     * GET /api/seller/vouchers/{voucherId}
     */
    @GetMapping("/{voucherId}")
    public ResponseEntity<SellerVoucherResponseDTO> getVoucher(
            @PathVariable Long voucherId,
            Authentication authentication) {

        Long sellerId = getCurrentSellerId(authentication);
        Coupon coupon = couponService.getSellerVoucher(voucherId, sellerId);
        return ResponseEntity.ok(mapToResponseDTO(coupon));
    }

    /**
     * Create new voucher
     * POST /api/seller/vouchers
     */
    @PostMapping
    public ResponseEntity<SellerVoucherResponseDTO> createVoucher(
            @RequestBody SellerVoucherCreateDTO createDTO,
            Authentication authentication) {

        Long sellerId = getCurrentSellerId(authentication);
        User seller = userService.getUserById(sellerId);

        // Map DTO to entity
        Coupon coupon = new Coupon();
        coupon.setCode(createDTO.getCode().toUpperCase());
        coupon.setDescription(createDTO.getDescription());
        coupon.setType(createDTO.getDiscountType());
        coupon.setAmount(createDTO.getDiscountValue().intValue());
        coupon.setMinOrderAmount(createDTO.getMinOrderAmount() != null ? createDTO.getMinOrderAmount().intValue() : null);
        coupon.setMaxDiscountAmount(createDTO.getMaxDiscountAmount());
        coupon.setStartDate(createDTO.getStartDate());
        coupon.setExpiresAt(createDTO.getEndDate());
        coupon.setTotalQuantity(createDTO.getUsageLimit() != null ? createDTO.getUsageLimit() : -1);
        coupon.setSeller(seller);
        coupon.setActive(true);

        Coupon created = couponService.createSellerCoupon(coupon, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponseDTO(created));
    }

    /**
     * Update voucher details (only certain fields)
     * PUT /api/seller/vouchers/{voucherId}
     */
    @PutMapping("/{voucherId}")
    public ResponseEntity<SellerVoucherResponseDTO> updateVoucher(
            @PathVariable Long voucherId,
            @RequestBody SellerVoucherCreateDTO updateDTO,
            Authentication authentication) {

        Long sellerId = getCurrentSellerId(authentication);

        Coupon updates = new Coupon();
        updates.setDescription(updateDTO.getDescription());

        Coupon updated = couponService.updateSellerCoupon(voucherId, sellerId, updates);
        return ResponseEntity.ok(mapToResponseDTO(updated));
    }

    /**
     * Deactivate voucher (soft delete)
     * DELETE /api/seller/vouchers/{voucherId}
     */
    @DeleteMapping("/{voucherId}")
    public ResponseEntity<Void> deactivateVoucher(
            @PathVariable Long voucherId,
            Authentication authentication) {

        Long sellerId = getCurrentSellerId(authentication);
        couponService.deactivateSellerCoupon(voucherId, sellerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to map Coupon to SellerVoucherResponseDTO with statistics
     */
    private SellerVoucherResponseDTO mapToResponseDTO(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt());
        boolean isNotStarted = coupon.getStartDate() != null && now.isBefore(coupon.getStartDate());
        
        String status;
        if (!coupon.isActive()) {
            status = "INACTIVE";
        } else if (isNotStarted) {
            status = "NOT_STARTED";
        } else if (isExpired) {
            status = "EXPIRED";
        } else if (coupon.getTotalQuantity() > 0 && coupon.getUsedCount() >= coupon.getTotalQuantity()) {
            status = "EXHAUSTED";
        } else {
            status = "ACTIVE";
        }

        Integer remainingUsage = null;
        if (coupon.getTotalQuantity() > 0) {
            remainingUsage = coupon.getTotalQuantity() - coupon.getUsedCount();
        }

        return SellerVoucherResponseDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getType())
                .discountValue(coupon.getAmount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getExpiresAt())
                .usageLimit(coupon.getTotalQuantity() > 0 ? coupon.getTotalQuantity() : null)
                .usedCount(coupon.getUsedCount())
                .isActive(coupon.isActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .remainingUsage(remainingUsage)
                .isExpired(isExpired)
                .isNotStarted(isNotStarted)
                .status(status)
                .build();
    }
}
