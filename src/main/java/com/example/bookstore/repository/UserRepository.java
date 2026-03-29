package com.example.bookstore.repository;

import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Báo cho Spring Boot biết đây là Thợ mỏ đào dữ liệu
public interface UserRepository extends JpaRepository<User, Long> {

    // Ảo ma chưa: Chỉ cần gõ đúng tên hàm, Spring Boot TỰ ĐỘNG sinh ra câu lệnh SQL:
    // SELECT * FROM users WHERE username = ?
    User findByUsername(String username);

    // Tự động kiểm tra xem username đã có trong DB chưa
    boolean existsByUsername(String username);

    List<User> findAllByRole(UserRole role);
}