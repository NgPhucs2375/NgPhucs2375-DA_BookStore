package com.example.bookstore.service;

import com.example.bookstore.dto.VoucherCreateDTO;
import com.example.bookstore.dto.VoucherValidateResponse;
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

    // ========================================================================
    // SELLER OPERATIONS
    // ========================================================================

    public List<Voucher> getSellerVouchers(User seller) {
        return voucherRepository.findBySeller(seller);
    }

    public List<Voucher> searchVouchers(User seller, String query) {
        return voucherRepository.findBySellerAndNameContainingIgnoreCase(seller, query);
    }

    public List<Voucher> filterVouchersByStatus(User seller, VoucherStatus status) {
        return voucherRepository.findBySellerAndStatus(seller, status);
    }

    // ========================================================================
    // ADMIN OPERATIONS
    // ========================================================================

    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Voucher> searchAllVouchers(String query) {
        return voucherRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(query, query);
    }

    public List<Voucher> filterAllVouchersByStatus(VoucherStatus status) {
        return voucherRepository.findByStatus(status);
    }

    @Transactional
    public Voucher toggleVoucherStatus(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
        if (voucher.getStatus() == VoucherStatus.ACTIVE) {
            voucher.setStatus(VoucherStatus.DISABLED);
        } else if (voucher.getStatus() == VoucherStatus.DISABLED) {
            voucher.setStatus(VoucherStatus.ACTIVE);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể thay đổi trạng thái voucher này");
        }
        return voucherRepository.save(voucher);
    }

    // ========================================================================
    // CREATE VOUCHER (FROM DTO)
    // ========================================================================

    @Transactional
    public Voucher createVoucher(VoucherCreateDTO dto, User seller) {
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher không được để trống");
        }
        if (voucherRepository.findByCode(dto.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher đã tồn tại");
        }
        if (dto.getUsageLimit() == null || dto.getUsageLimit() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới hạn sử dụng không được để trống");
        }
        if (dto.getDiscountValue() == null || dto.getDiscountValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị giảm không hợp lệ");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu và kết thúc không được để trống");
        }

        Voucher voucher = Voucher.builder()
                .code(dto.getCode().trim().toUpperCase())
                .name(dto.getName())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .minOrderAmount(dto.getMinOrderAmount() != null ? dto.getMinOrderAmount() : 0.0)
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .usageLimit(dto.getUsageLimit())
                .usedCount(0)
                .remainingQuantity(dto.getUsageLimit())
                .seller(seller)
                .status(VoucherStatus.ACTIVE)
                .build();

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

    @Transactional
    public void deleteVoucherAsAdmin(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
        voucherRepository.delete(voucher);
    }

    // ========================================================================
    // VALIDATE & APPLY VOUCHER (CHECKOUT)
    // ========================================================================

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

    public Double calculateDiscount(Voucher voucher, Double orderAmount) {
        Double discount = 0.0;
        if (voucher.getDiscountType() == VoucherType.PERCENTAGE) {
            discount = orderAmount * (voucher.getDiscountValue() / 100.0);
            if (voucher.getMaxDiscountAmount() != null && discount > voucher.getMaxDiscountAmount()) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            discount = voucher.getDiscountValue();
        }
        return Math.min(discount, orderAmount);
    }

    @Transactional
    public Double applyVoucher(String code, User user, Double orderAmount) {
        Voucher voucher = validateVoucher(code, user, orderAmount);
        return calculateDiscount(voucher, orderAmount);
    }

    public VoucherValidateResponse validateAndPreview(String code, User user, Double orderAmount) {
        Voucher voucher = validateVoucher(code, user, orderAmount);
        Double discount = calculateDiscount(voucher, orderAmount);

        return VoucherValidateResponse.builder()
                .code(voucher.getCode())
                .name(voucher.getName())
                .discountType(voucher.getDiscountType().name())
                .discountValue(voucher.getDiscountValue())
                .discountAmount(discount)
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .build();
    }

    // ========================================================================
    // USE VOUCHER (POST-CHECKOUT)
    // ========================================================================

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
