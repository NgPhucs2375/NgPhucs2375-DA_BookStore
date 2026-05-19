package com.example.bookstore.service;

import com.example.bookstore.model.Coupon;
import com.example.bookstore.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * Get coupon by code with validation
     */
    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCodeIgnoreCase(code);
    }

    /**
     * Validate coupon for order
     * Returns coupon if valid, throws exception if invalid
     */
    public Coupon validateCoupon(String code, Integer orderAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        // Check if coupon is active
        if (!coupon.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã bị vô hiệu hóa");
        }

        // Check if coupon is expired
        if (coupon.getExpiresAt() != null && LocalDateTime.now().isAfter(coupon.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Mã giảm giá đã hết hạn");
        }

        // Check if coupon has exceeded usage limit
        if (coupon.getTotalQuantity() > 0 && coupon.getUsedCount() >= coupon.getTotalQuantity()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Mã giảm giá đã hết lượt sử dụng");
        }

        // Check minimum order amount
        if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn hàng tối thiểu " + coupon.getMinOrderAmount() + " VND");
        }

        return coupon;
    }

    /**
     * Calculate discount for order
     */
    public Integer calculateDiscount(String code, Integer orderAmount) {
        Coupon coupon = validateCoupon(code, orderAmount);
        return coupon.calculateDiscount(orderAmount);
    }

    /**
     * Use coupon (increment usedCount)
     */
    @Transactional
    public void useCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        coupon.setUpdatedAt(LocalDateTime.now());
        couponRepository.save(coupon);
    }

    /**
     * Get all valid coupons (admin listing)
     */
    public List<Coupon> getAllValidCoupons() {
        return couponRepository.findAllValidCoupons();
    }

    /**
     * Get all active coupons
     */
    public List<Coupon> getAllActiveCoupons() {
        return couponRepository.findByIsActiveTrue();
    }

    /**
     * Get coupons with pagination
     */
    public Page<Coupon> getCouponsPageable(int page, int size, boolean isActive) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return couponRepository.findByIsActive(isActive, pageable);
    }

    /**
     * Search coupons
     */
    public Page<Coupon> searchCoupons(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return couponRepository.searchCoupons(keyword, pageable);
    }

    /**
     * Create new coupon (Admin)
     */
    public Coupon createCoupon(Coupon coupon) {
        // Check code uniqueness
        if (couponRepository.existsByCodeIgnoreCase(coupon.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã tồn tại");
        }

        // Validate coupon data
        if (coupon.getAmount() == null || coupon.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền giảm phải > 0");
        }

        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUsedCount(0);
        return couponRepository.save(coupon);
    }

    /**
     * Update existing coupon (Admin)
     */
    @Transactional
    public Coupon updateCoupon(Long id, Coupon updates) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        // Only allow updating certain fields
        if (updates.getDescription() != null) {
            coupon.setDescription(updates.getDescription());
        }
        if (updates.getAmount() != null && updates.getAmount() > 0) {
            coupon.setAmount(updates.getAmount());
        }
        if (updates.getMinOrderAmount() != null) {
            coupon.setMinOrderAmount(updates.getMinOrderAmount());
        }
        if (updates.getExpiresAt() != null) {
            coupon.setExpiresAt(updates.getExpiresAt());
        }
        if (updates.getTotalQuantity() != null) {
            coupon.setTotalQuantity(updates.getTotalQuantity());
        }
        coupon.setActive(updates.isActive());
        coupon.setUpdatedAt(LocalDateTime.now());

        return couponRepository.save(coupon);
    }

    /**
     * Delete coupon (Admin) - soft delete by setting isActive=false
     */
    @Transactional
    public void deactivateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        coupon.setActive(false);
        coupon.setUpdatedAt(LocalDateTime.now());
        couponRepository.save(coupon);
    }

    /**
     * Permanently delete coupon (Admin)
     */
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại");
        }
        couponRepository.deleteById(id);
    }

    /**
     * Get coupon by ID (Admin)
     */
    public Coupon getCouponById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));
    }

    /**
     * Get all coupons pageable (Admin)
     */
    public Page<Coupon> getAllCouponsPageable(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return couponRepository.findAll(pageable);
    }
}
