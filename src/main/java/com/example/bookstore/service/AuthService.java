package com.example.bookstore.service;

import com.example.bookstore.dto.UserProfileResponse;
import com.example.bookstore.dto.UserProfileUpdateRequest;
import com.example.bookstore.model.Category;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service // BẮT BUỘC PHẢI CÓ để Lễ tân AuthController gọi được
public class AuthService {

    @Autowired
    private UserRepository userRepository; //Nhờ lính đánh thuê để tìm dữ liệu

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthOtpService authOtpService;

    //    Register
    public boolean register(String username, String rawPassword, String avatarUrl, List<Long> favoriteCategoryIds){
        if (!authOtpService.consumeVerifiedEmail(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email chua xac thuc OTP");
        }

        if (userRepository.existsByUsername(username)) {
            System.out.println("Tên đăng nhập đã tồn tại");
            return false;
        }

        //Băm mật khẩu với độ khó (work factor) là 12 để chống brute-force
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        // Tạo tài khoản Buyer mặc định cho luồng đăng ký thường
        User newUser = User.builder()
            .username(username)
            .passwordHash(hashedPassword)
            .role(UserRole.BUYER)
            .avatarUrl(normalizeAvatar(avatarUrl))
            .favoriteCategories(resolveFavoriteCategories(favoriteCategoryIds))
            .build();
        userRepository.save(newUser);

        System.out.println("Đăng ký thành công");
        return true;

    }

//    Login
    public boolean login (String username, String rawPassword){
        return authenticateUser(username, rawPassword) != null;
    }

    public User authenticateUser(String username, String rawPassword) {
        User user = userRepository.findByUsername(username);

        if(user == null){
            System.out.println("Lỗi đăng nhập");
            return null;
        }
//        Kiểm tra có khớp với băm hay không
        if(BCrypt.checkpw(rawPassword, user.getPasswordHash())){
            System.out.println("Đăng nhập thành công");
            return user;
        }
        else {
            System.out.println("Sai thông tin đăng nhập");
            return null;
        }
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return toUserProfileResponse(user);
    }

    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            User existing = userRepository.findByUsername(request.getUsername());
            if (existing != null && !existing.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
            }
            user.setUsername(request.getUsername().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeAvatar(request.getAvatarUrl()));
        }

        if (request.getFavoriteCategoryIds() != null) {
            user.setFavoriteCategories(resolveFavoriteCategories(request.getFavoriteCategoryIds()));
        }

        // Shop info is only meaningful for seller/admin storefront management.
        if (user.getRole() == UserRole.SELLER || user.getRole() == UserRole.ADMIN) {
            if (request.getShopName() != null) {
                user.setShopName(request.getShopName().trim());
            }
            if (request.getShopAddress() != null) {
                user.setShopAddress(request.getShopAddress().trim());
            }
        }

        userRepository.save(user);
        return toUserProfileResponse(user);
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .role(user.getRole())
            .shopName(user.getShopName())
            .shopAddress(user.getShopAddress())
            .avatarUrl(user.getAvatarUrl())
            .favoriteCategoryIds(user.getFavoriteCategories().stream().map(Category::getId).toList())
            .build();
    }

    private Set<Category> resolveFavoriteCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Long> normalizedIds = categoryIds.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .collect(Collectors.toList());

        if (normalizedIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Category> categories = categoryRepository.findAllById(normalizedIds);
        if (categories.size() != normalizedIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Co the loai yeu thich khong ton tai");
        }

        return new LinkedHashSet<>(categories);
    }

    private String normalizeAvatar(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String trimmed = avatarUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
