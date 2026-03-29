package com.example.bookstore.model;

import com.example.bookstore.model.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_orders")
@Data
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
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Double subTotal;

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = OrderStatus.PENDING_PAYMENT;
        }
    }
}
