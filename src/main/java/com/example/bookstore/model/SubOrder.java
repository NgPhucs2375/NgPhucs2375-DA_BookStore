package com.example.bookstore.model;

import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.model.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_orders")
@Data
@ToString(exclude = {"parentOrder", "seller", "items", "statusHistories"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order parentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private OrderStatus status;

    @Column(nullable = false)
    private Double subTotal;

    // ===== Payment & Refund fields =====
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20, columnDefinition = "NVARCHAR(20)")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "refund_reason", length = 500, columnDefinition = "NVARCHAR(500)")
    private String refundReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    // ===== Timestamp tracking =====
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 20, columnDefinition = "NVARCHAR(20)")
    private String cancelledBy; // BUYER, SELLER, SYSTEM, ADMIN

    // ===== Relationships =====
    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<SubOrderStatusHistory> statusHistories = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = OrderStatus.PENDING_PAYMENT;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }
    }
}
