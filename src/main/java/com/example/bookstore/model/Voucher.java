package com.example.bookstore.model;

import com.example.bookstore.model.enums.VoucherStatus;
import com.example.bookstore.model.enums.VoucherType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private VoucherType discountType;

    @Column(name = "discount_value", nullable = false)
    private Double discountValue;

    @Column(name = "min_order_amount", nullable = false)
    private Double minOrderAmount;

    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount;

    @Column(name = "start_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    @Column(name = "usage_limit", nullable = false)
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "remaining_quantity")
    private Integer remainingQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnore
    private User seller;

    @JsonProperty("sellerId")
    public Long getSellerId() {
        return seller != null ? seller.getId() : null;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return status == VoucherStatus.ACTIVE &&
               usedCount < usageLimit &&
               remainingQuantity > 0 &&
               now.isAfter(startDate) &&
               now.isBefore(endDate);
    }
}
