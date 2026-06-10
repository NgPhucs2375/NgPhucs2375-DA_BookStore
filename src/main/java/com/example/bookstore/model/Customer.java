package com.example.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin khách hàng phục vụ ML churn prediction (mô hình final-gauss-lightgbm).
 * Quan hệ 1-1 với User, tách biệt để không ảnh hưởng bảng users.
 * Input: 10 raw features
 * Output: predicted_label (0/1), churn_probability, risk_level (LOW/HIGH)
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
    // ML Input Features (10 raw features)
    // ========================

    @Column(nullable = false)
    private Double accountAgeMonths;

    @Column(nullable = false)
    private Double avgOrderValue;

    @Column(nullable = false)
    private Double totalOrders;

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
    private Double satisfactionScore;

    @Column(nullable = false)
    private Double priceSensitivityIndex;

    // ========================
    // ML Output Results
    // ========================

    @Column
    private Integer predictedLabel; // 0 = Stay (Ở lại), 1 = Churn (Rời bỏ)

    @Column
    private Double churnProbability;

    @Column(length = 10)
    private String riskLevel; // "LOW" hoặc "HIGH"

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
