package com.example.bookstore.model;

import com.example.bookstore.model.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity // Đánh dấu đây là 1 bảng trong DB
@Table(name="users") // Tên bảng dưới Database sẽ là 'users'
@Getter
@Setter
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
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private UserRole role;

    @Column(length = 255)
    private String shopName;

    @Column(length = 500)
    private String shopAddress;

    @Lob
    @Column(name = "avatar_url")
    private String avatarUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_favorite_categories",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> favoriteCategories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "seller")
    @JsonIgnore
    @Builder.Default
    private List<Book> books = new ArrayList<>();

    @OneToMany(mappedBy = "seller")
    @JsonIgnore
    @Builder.Default
    private List<SubOrder> subOrders = new ArrayList<>();

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