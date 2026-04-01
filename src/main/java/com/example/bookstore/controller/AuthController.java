package com.example.bookstore.controller;

import com.example.bookstore.dto.AuthLoginRequest;
import com.example.bookstore.dto.AuthRegisterRequest;
import com.example.bookstore.dto.UserProfileResponse;
import com.example.bookstore.dto.UserProfileUpdateRequest;
import com.example.bookstore.model.User;
import com.example.bookstore.security.JwtTokenProvider;
import com.example.bookstore.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController // Vẫn là báo cho Spring Boot biết đây là chỗ tạo API
@CrossOrigin("*") // kiểu cấp thẻ VIP để auth được quyền trỏ vô data của SP vậy á
@RequestMapping("/api/auth") // Đặt địa chỉ gốc là /api/auth
public class AuthController {

    @Autowired
    private AuthService authService; //Thêm bộ não xử lý

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

//    API đăng ký
//    đường dẫn là POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthRegisterRequest request){
        boolean isSuccess = authService.register(request.getUsername(), request.getPassword());
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
    public ResponseEntity<String> login(@Valid @RequestBody AuthLoginRequest request){
        boolean isValid = authService.login(request.getUsername(), request.getPassword());

        if(isValid){
            return ResponseEntity.ok("Đăng nhập thành công");
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sai tên đăng nhập hoặc mật khẩu");
        }
    }

    @PostMapping("/login-jwt")
    public ResponseEntity<?> loginJwt(@Valid @RequestBody AuthLoginRequest request) {
        User authenticated = authService.authenticateUser(request.getUsername(), request.getPassword());
        if (authenticated == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sai tên đăng nhập hoặc mật khẩu");
        }

        String token = jwtTokenProvider.createToken(authenticated.getId(), authenticated.getRole().name());
        return ResponseEntity.ok(Map.of(
            "tokenType", "Bearer",
            "accessToken", token,
            "userId", authenticated.getId(),
            "role", authenticated.getRole().name()
        ));
    }

    @GetMapping("/profile/{userId}")
    public UserProfileResponse getProfile(@PathVariable Long userId) {
        return authService.getProfile(userId);
    }

    @PutMapping("/profile/{userId}")
    public UserProfileResponse updateProfile(
        @PathVariable Long userId,
        @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return authService.updateProfile(userId, request);
    }
}
