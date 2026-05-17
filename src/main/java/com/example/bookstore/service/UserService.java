package com.example.bookstore.service;

import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Lấy người dùng theo ID
     */
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    /**
     * Lấy danh sách người dùng theo role
     */
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findAllByRole(role);
    }
    
    /**
     * Khóa tài khoản người dùng (Admin action)
     */
    public User lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + userId));
        user.setActive(false);
        return userRepository.save(user);
    }
    
    /**
     * Mở khóa tài khoản người dùng (Admin action)
     */
    public User unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + userId));
        user.setActive(true);
        return userRepository.save(user);
    }
    
    /**
     * Lấy tất cả người dùng
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Lấy người dùng theo trạng thái active
     */
    public List<User> getUsersByActiveStatus(boolean isActive) {
        return userRepository.findByIsActive(isActive);
    }
    
    /**
     * Lấy người dùng theo role và trạng thái active
     */
    public List<User> getUsersByRoleAndStatus(UserRole role, boolean isActive) {
        return userRepository.findByRoleAndIsActive(role, isActive);
    }
}
