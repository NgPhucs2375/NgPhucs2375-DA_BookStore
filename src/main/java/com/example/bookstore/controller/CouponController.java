package com.example.bookstore.controller;

import com.example.bookstore.model.Coupon;
import com.example.bookstore.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * Validate coupon for checkout (Public endpoint)
     * GET /api/coupons/{code}/validate?orderAmount=500000
     */
    @GetMapping("/{code}/validate")
    public ResponseEntity<?> validateCoupon(
            @PathVariable String code,
            @RequestParam Integer orderAmount
    ) {
        try {
            // Validate coupon
            Coupon coupon = couponService.validateCoupon(code, orderAmount);

            // Calculate discount
            Integer discount = coupon.calculateDiscount(orderAmount);
            Integer finalAmount = orderAmount - discount;

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "code", coupon.getCode(),
                    "description", coupon.getDescription(),
                    "type", coupon.getType().toString(),
                    "originalAmount", orderAmount,
                    "discount", discount,
                    "finalAmount", finalAmount,
                    "message", "Mã giảm giá hợp lệ"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false,
                    "code", code,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get coupon details (Public endpoint)
     * GET /api/coupons/{code}
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getCouponByCode(@PathVariable String code) {
        try {
            Optional<Coupon> coupon = couponService.getCouponByCode(code);
            if (coupon.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "Mã giảm giá không tồn tại"
                ));
            }

            Coupon c = coupon.get();
            return ResponseEntity.ok(Map.of(
                    "code", c.getCode(),
                    "description", c.getDescription(),
                    "type", c.getType().toString(),
                    "amount", c.getAmount(),
                    "minOrderAmount", c.getMinOrderAmount(),
                    "isValid", c.isValid()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * Apply coupon to order (use coupon)
     * POST /api/coupons/{code}/apply
     */
    @PostMapping("/{code}/apply")
    public ResponseEntity<?> applyCoupon(@PathVariable String code) {
        try {
            couponService.useCoupon(code);
            return ResponseEntity.ok(Map.of(
                    "message", "Mã giảm giá đã được sử dụng"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
