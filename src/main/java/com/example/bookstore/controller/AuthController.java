package com.example.bookstore.controller;

import com.example.bookstore.dto.AuthLoginRequest;
import com.example.bookstore.dto.AuthRegisterRequest;
import com.example.bookstore.dto.EmailOtpRequest;
import com.example.bookstore.dto.EmailOtpVerifyRequest;
import com.example.bookstore.dto.UserProfileResponse;
import com.example.bookstore.dto.UserProfileUpdateRequest;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.security.JwtTokenProvider;
import com.example.bookstore.service.AuthOtpService;
import com.example.bookstore.service.AuthService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    private AuthOtpService authOtpService;

    @PostMapping("/otp/request")
    public ResponseEntity<?> requestRegisterOtp(@Valid @RequestBody EmailOtpRequest request) {
        String fallbackOtp = authOtpService.requestOtp(request.getEmail());
        if (fallbackOtp != null) {
            // SMTP not configured or email send failed - return OTP for dev/test
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "[DEV MODE] SMTP chua config. OTP trong response (production se gui email)",
                "otp", fallbackOtp
            ));
        }
        // Email sent successfully
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "OTP da duoc gui. Vui long kiem tra email."
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<String> verifyRegisterOtp(@Valid @RequestBody EmailOtpVerifyRequest request) {
        boolean isValid = authOtpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP khong dung hoac da het han");
        }
        return ResponseEntity.ok("Xac thuc OTP thanh cong");
    }

//    API đăng ký
//    đường dẫn là POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthRegisterRequest request){
        boolean isSuccess = authService.register(
            request.getUsername(),
            request.getPassword(),
            request.getAvatarUrl(),
            request.getFavoriteCategoryIds()
        );
        if (isSuccess){
//            tra ve 200
            return ResponseEntity.ok("Đăng kí thành công");
        }
        else {
//            Trả về 400 nếu trùng tên
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên đăng nhập đã tồn tại vui lòng thử lại");
        }
    }

    @PostMapping("/register-seller")
    public ResponseEntity<String> registerSeller(@Valid @RequestBody AuthRegisterRequest request) {
        boolean isSuccess = authService.registerWithRole(
            request.getUsername(),
            request.getPassword(),
            request.getAvatarUrl(),
            request.getFavoriteCategoryIds(),
            UserRole.SELLER
        );

        if (isSuccess){
            return ResponseEntity.ok("Dang ky seller thanh cong");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ten dang nhap da ton tai vui long thu lai");
    }

    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody AuthRegisterRequest request) {
        boolean isSuccess = authService.registerWithRole(
            request.getUsername(),
            request.getPassword(),
            request.getAvatarUrl(),
            request.getFavoriteCategoryIds(),
            UserRole.ADMIN
        );

        if (isSuccess){
            return ResponseEntity.ok("Dang ky admin thanh cong");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ten dang nhap da ton tai vui long thu lai");
    }

//    API đăng nhập
//    /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody AuthLoginRequest request){
        User user = authService.authenticateUser(request.getUsername(), request.getPassword());

        if(user != null){
            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản bị từ chối đăng nhập");
            }
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

        if (!authenticated.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản bị từ chối đăng nhập");
        }

        Long sellerId = authenticated.getRole() == UserRole.SELLER
            ? authenticated.getId()
            : null;
        java.util.List<String> roles = java.util.List.of(authenticated.getRole().name());
        String token = jwtTokenProvider.createToken(authenticated.getId(), roles, sellerId);

        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("tokenType", "Bearer");
        response.put("accessToken", token);
        response.put("userId", authenticated.getId());
        response.put("role", authenticated.getRole().name());
        response.put("roles", roles);
        response.put("sellerId", sellerId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/profile/{userId}")
    public UserProfileResponse getProfile(@PathVariable Long userId) {
        return authService.getProfile(userId);
    }

    @PutMapping("/profile/{userId}")
    @PreAuthorize("hasPermission(#userId, 'User', 'update')")
    public ResponseEntity<?> updateProfile(
        @PathVariable Long userId,
        @Valid @RequestBody UserProfileUpdateRequest request
    ) {

        // 2 Chống thêm các thể html 
        if (request.getShopName() != null) {
            request.setShopName(request.getShopName().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
        }
        // 3. Đẩy xuống Service xử lý
        try {
            UserProfileResponse response = authService.updateProfile(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
