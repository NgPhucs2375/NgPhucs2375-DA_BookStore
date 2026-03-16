package com.example.bookstore.controller;

import com.example.bookstore.model.User;
import com.example.bookstore.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Vẫn là báo cho Spring Boot biết đây là chỗ tạo API
@RequestMapping("/api/auth") // Đặt địa chỉ gốc là /api/auth
public class AuthController {

    @Autowired
    private AuthService authService; //Thêm bộ não xử lý

//    API đăng ký
//    đường dẫn là POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user){
        boolean isSuccess = authService.register(user.getUsername(), user.getPasswordHash());
        if (isSuccess){
//            tra ve 200
            return ResponseEntity.ok("Đăng kí thành công");
        }
        else {
//            Trả về 400 nếu trùng tên
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên đăng nhập đã tồn tại vui lòng thử lại");
        }
    }

//    API đăng nhập
//    /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user){
        boolean isValid = authService.login(user.getUsername(), user.getPasswordHash());

        if(isValid){
            return ResponseEntity.ok("Đăng nhập thành công");
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sai tên đăng nhập hoặc mật khẩu");
        }
    }
}
