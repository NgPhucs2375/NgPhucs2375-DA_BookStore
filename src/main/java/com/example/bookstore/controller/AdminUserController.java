package com.example.bookstore.controller;

import com.example.bookstore.model.User;
import com.example.bookstore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/users")
public class AdminUserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * Khóa tài khoản người dùng
     * Endpoint: PUT /api/admin/users/{id}/lock
     */
    @PutMapping("/{id}/lock")
    public ResponseEntity<?> lockUser(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        // 🔐 SECURITY CHECK: Kiểm tra role Admin
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối: Chỉ Admin mới có quyền!");
        }
        
        try {
            User user = userService.lockUser(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
    
    /**
     * Mở khóa tài khoản người dùng
     * Endpoint: PUT /api/admin/users/{id}/unlock
     */
    @PutMapping("/{id}/unlock")
    public ResponseEntity<?> unlockUser(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        // 🔐 SECURITY CHECK: Kiểm tra role Admin
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối: Chỉ Admin mới có quyền!");
        }
        
        try {
            User user = userService.unlockUser(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
    
    /**
     * Lấy thông tin người dùng theo ID
     * Endpoint: GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        // 🔐 SECURITY CHECK: Kiểm tra role Admin
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối!");
        }
        
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User không tồn tại");
        }
        return ResponseEntity.ok(user);
    }
}
