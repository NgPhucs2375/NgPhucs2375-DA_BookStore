package com.example.bookstore.service;

import com.example.bookstore.model.User;
import com.example.bookstore.model.Voucher;
import com.example.bookstore.model.VoucherUsage;
import com.example.bookstore.model.enums.VoucherStatus;
import com.example.bookstore.model.enums.VoucherType;
import com.example.bookstore.repository.VoucherRepository;
import com.example.bookstore.repository.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    public List<Voucher> getSellerVouchers(User seller) {
        return voucherRepository.findBySeller(seller);
    }

    public List<Voucher> searchVouchers(User seller, String query) {
        return voucherRepository.findBySellerAndNameContainingIgnoreCase(seller, query);
    }

    public List<Voucher> filterVouchersByStatus(User seller, VoucherStatus status) {
        return voucherRepository.findBySellerAndStatus(seller, status);
    }

    @Transactional
    public Voucher createVoucher(Voucher voucher) {
        if (voucher.getCode() == null || voucher.getCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher không được để trống");
        }
        if (voucherRepository.findByCode(voucher.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher đã tồn tại");
        }
        
        // Khởi tạo các giá trị mặc định nếu chưa có
        if (voucher.getUsedCount() == null) {
            voucher.setUsedCount(0);
        }
        if (voucher.getUsageLimit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới hạn sử dụng không được để trống");
        }
        if (voucher.getRemainingQuantity() == null) {
            voucher.setRemainingQuantity(voucher.getUsageLimit());
        }
        if (voucher.getStatus() == null) {
            voucher.setStatus(VoucherStatus.ACTIVE);
        }
        
        return voucherRepository.save(voucher);
    }

    @Transactional
    public void deleteVoucher(Long id, User seller) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
        
        if (!voucher.getSeller().getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa voucher này");
        }
        
        voucherRepository.delete(voucher);
    }

    public Voucher validateVoucher(String code, User user, Double orderAmount) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không hợp lệ"));

        if (!voucher.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn hoặc hết lượt dùng");
        }

        if (orderAmount < voucher.getMinOrderAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinOrderAmount());
        }

        if (voucherUsageRepository.existsByUserAndVoucher(user, voucher)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã sử dụng mã giảm giá này rồi");
        }

        return voucher;
    }

    @Transactional
    public Double applyVoucher(String code, User user, Double orderAmount) {
        Voucher voucher = validateVoucher(code, user, orderAmount);
        
        Double discount = 0.0;
        if (voucher.getDiscountType() == VoucherType.PERCENTAGE) {
            discount = orderAmount * (voucher.getDiscountValue() / 100.0);
            if (voucher.getMaxDiscountAmount() != null && discount > voucher.getMaxDiscountAmount()) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            discount = voucher.getDiscountValue();
        }

        return discount;
    }

    @Transactional
    public void useVoucher(Voucher voucher, User user, com.example.bookstore.model.Order order, Double discountAmount) {
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
        if (voucher.getUsedCount() >= voucher.getUsageLimit() || voucher.getRemainingQuantity() <= 0) {
            voucher.setStatus(VoucherStatus.EXHAUSTED);
        }
        voucherRepository.save(voucher);

        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .user(user)
                .order(order)
                .discountAmount(discountAmount)
                .build();
        voucherUsageRepository.save(usage);
    }
}
