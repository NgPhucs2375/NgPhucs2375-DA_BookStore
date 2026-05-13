package com.example.bookstore.controller;

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

    private User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<Voucher>> getSellerVouchers(
            @PathVariable Long sellerId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) VoucherStatus status) {
        User seller = getCurrentUser(sellerId);
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
    public ResponseEntity<Voucher> createVoucher(@RequestBody Voucher voucher, @RequestParam Long sellerId) {
        User seller = getCurrentUser(sellerId);
        voucher.setSeller(seller);
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucher(voucher));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id, @RequestParam Long sellerId) {
        User seller = getCurrentUser(sellerId);
        voucherService.deleteVoucher(id, seller);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Double> validateVoucher(
            @RequestParam String code,
            @RequestParam Long userId,
            @RequestParam Double amount) {
        User user = getCurrentUser(userId);
        Double discount = voucherService.applyVoucher(code, user, amount);
        return ResponseEntity.ok(discount);
    }
}
