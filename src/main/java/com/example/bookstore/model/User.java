package com.example.bookstore.model;

import com.example.bookstore.model.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString; // <-- Nhớ phải có import này nha bro

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity // Đánh dấu đây là 1 bảng trong DB
@Table(name="users") // Tên bảng dưới Database sẽ là 'users'
@Data
@NoArgsConstructor // Tự tạo Constructor không tham số
@AllArgsConstructor // Tự tạo Constructor có đủ tham số
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng ID
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(length = 255)
    private String shopName;

    @Column(length = 500)
    private String shopAddress;

    @Lob
    @Column(name = "avatar_url")
    private String avatarUrl;

    @ToString.Exclude // <--- Bùa chống Lazy cho Danh mục
    @ManyToMany
    @JoinTable(
            name = "user_favorite_categories",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> favoriteCategories = new LinkedHashSet<>();

    @ToString.Exclude // <--- Chống sập khi gọi tới Sách
    @OneToMany(mappedBy = "seller")
    @JsonIgnore
    @Builder.Default
    private List<Book> books = new ArrayList<>();

    @ToString.Exclude // <--- Chống sập khi gọi tới Đơn hàng
    @OneToMany(mappedBy = "seller")
    @JsonIgnore
    @Builder.Default
    private List<SubOrder> subOrders = new ArrayList<>();

    @ToString.Exclude // <--- Chống sập khi gọi tới Giỏ hàng
    @OneToOne(mappedBy = "buyer")
    @JsonIgnore
    private Cart cart;

    @PrePersist
    public void onCreate() {
        if (role == null) {
            role = UserRole.BUYER;
        }
    }
}