package com.example.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin khách hàng phục vụ ML churn prediction (mô hình gau-gbt6000).
 * Quan hệ 1-1 với User, tách biệt để không ảnh hưởng bảng users.
 */
@Entity
@Table(name = "customer_ml")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ========================
    // ML Input Features (14 raw features)
    // ========================

    @Column(nullable = false)
    private Double accountAgeMonths;

    @Column(nullable = false)
    private Double avgOrderValue;

    @Column(nullable = false)
    private Double totalOrders;

    @Column(nullable = false)
    private Double daysSinceLastPurchase;

    @Column
    private Double discountUsageRate;

    @Column(nullable = false)
    private Double returnRate;

    @Column(nullable = false)
    private Double customerSupportTickets;

    @Column(nullable = false, length = 10)
    private String loyaltyMember; // "Yes" / "No"

    @Column(nullable = false)
    private Double browsingFrequencyPerWeek;

    @Column(nullable = false)
    private Double cartAbandonmentRate;

    @Column(nullable = false)
    private Double productReviewScoreAvg;

    @Column(nullable = false)
    private Double engagementScore;

    @Column(nullable = false)
    private Double satisfactionScore;

    @Column(nullable = false)
    private Double priceSensitivityIndex;

    // ========================
    // ML Output Results
    // ========================

    @Column
    private Integer predictedClass; // 0 = An toàn, 1 = Trung bình, 2 = Cao

    @Column
    private Double churnProbability;

    @Column(length = 30)
    private String riskLevel; // "LOW (An toàn)" / "MEDIUM (Trung bình)" / "HIGH (Nguy cơ cao)"

    @Column
    private LocalDateTime lastAnalyzedAt;

    // ========================
    // Timestamps
    // ========================

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
