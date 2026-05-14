package com.example.bookstore.model;

import com.example.bookstore.model.enums.ChangedByRole;
import com.example.bookstore.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử thay đổi trạng thái của SubOrder.
 * Mỗi lần sub_order thay đổi trạng thái, một bản ghi mới được tạo.
 * Dùng để đối chiếu, kiểm tra khi có tranh chấp.
 */
@Entity
@Table(name = "sub_order_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_order_id", nullable = false)
    private SubOrder subOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30, columnDefinition = "NVARCHAR(30)")
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private OrderStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_role", length = 20, columnDefinition = "NVARCHAR(20)")
    private ChangedByRole changedByRole;

    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
