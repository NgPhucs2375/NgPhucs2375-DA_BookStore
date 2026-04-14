package com.example.bookstore.model;

import com.example.bookstore.model.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity //danh dau day la 1 bang trong DB
@Table(name="books")
@Data // tu dong tao Getter,Setter,toString,equals,hashCode
@NoArgsConstructor // Auto tạo Constructor không tham số
@AllArgsConstructor // Auto tao Constructor co tham so
@DynamicUpdate // hỗ trợ để update động

public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto tang id theo index
    private Long id;
    @Column(nullable = false, length = 500)
    private String title;
    @Column(nullable = false)
    private String author;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;
    private Double price;
    private Integer stockQuantity; //
    @Column(length = 500)
    private String imageUrl; // link ảnh lấy từ CSV
    private String publisher;  // Nhà xuất bản
    private String publishYear; // Năm xuất bản

    @ManyToOne
    @JoinColumn(name = "category_id") // Tên cột khóa ngoại trong SSMS
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnore
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    @PrePersist
    public void onCreate() {
        if (approvalStatus == null) {
            approvalStatus = ApprovalStatus.PENDING;
        }
    }
}
