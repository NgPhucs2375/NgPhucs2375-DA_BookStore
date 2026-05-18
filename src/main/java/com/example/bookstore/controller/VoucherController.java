package com.example.bookstore.controller;

import com.example.bookstore.dto.VoucherCreateDTO;
import com.example.bookstore.dto.VoucherValidateResponse;
import com.example.bookstore.model.User;
import com.example.bookstore.model.Voucher;
import com.example.bookstore.model.enums.VoucherStatus;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final UserRepository userRepository;

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // ========================================================================
    // SELLER ENDPOINTS
    // ========================================================================

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<Voucher>> getSellerVouchers(
            @PathVariable Long sellerId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) VoucherStatus status) {
        User seller = getUser(sellerId);
        List<Voucher> vouchers;
        if (query != null && !query.isEmpty()) {
            vouchers = voucherService.searchVouchers(seller, query);
        } else if (status != null) {
            vouchers = voucherService.filterVouchersByStatus(seller, status);
        } else {
            vouchers = voucherService.getSellerVouchers(seller);
        }
        return ResponseEntity.ok(vouchers);
    }

    @PostMapping
    public ResponseEntity<Voucher> createVoucher(
            @RequestBody VoucherCreateDTO dto,
            @RequestParam Long sellerId) {
        User seller = getUser(sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucher(dto, seller));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id, @RequestParam Long sellerId) {
        User seller = getUser(sellerId);
        voucherService.deleteVoucher(id, seller);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // ADMIN ENDPOINTS
    // ========================================================================

    @GetMapping("/admin/all")
    public ResponseEntity<List<Voucher>> getAllVouchers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) VoucherStatus status) {
        List<Voucher> vouchers;
        if (query != null && !query.isEmpty()) {
            vouchers = voucherService.searchAllVouchers(query);
        } else if (status != null) {
            vouchers = voucherService.filterAllVouchersByStatus(status);
        } else {
            vouchers = voucherService.getAllVouchers();
        }
        return ResponseEntity.ok(vouchers);
    }

    @PostMapping("/admin")
    public ResponseEntity<Voucher> createVoucherAsAdmin(
            @RequestBody VoucherCreateDTO dto,
            @RequestHeader("X-User-Id") Long adminId) {
        User admin = getUser(adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucher(dto, admin));
    }

    @PatchMapping("/admin/{id}/toggle")
    public ResponseEntity<Voucher> toggleVoucherStatus(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.toggleVoucherStatus(id));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteVoucherAsAdmin(@PathVariable Long id) {
        voucherService.deleteVoucherAsAdmin(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // BUYER CHECKOUT ENDPOINT
    // ========================================================================

    @GetMapping("/validate")
    public ResponseEntity<VoucherValidateResponse> validateVoucher(
            @RequestParam String code,
            @RequestParam Long userId,
            @RequestParam Double amount) {
        User user = getUser(userId);
        VoucherValidateResponse result = voucherService.validateAndPreview(code, user, amount);
        return ResponseEntity.ok(result);
    }
}
