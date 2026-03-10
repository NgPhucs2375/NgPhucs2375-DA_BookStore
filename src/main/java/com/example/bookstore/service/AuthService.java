package com.example.bookstore.service;

import com.example.bookstore.model.User;
import com.example.bookstore.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service // BẮT BUỘC PHẢI CÓ để Lễ tân AuthController gọi được
public class AuthService {

    @Autowired
    private UserRepository userRepository; //Nhờ lính đánh thuê để tìm dữ liệu

    //    Register
    public boolean register(String username, String rawPassword){
        if (userRepository.existsByUsername(username)) {
            System.out.println("Tên đăng nhập đã tồn tại");
            return false;
        }

        //Băm mật khẩu với độ khó (work factor) là 12 để chống brute-force
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        // Tạo tài khoản mới (ID để null vì Database sẽ tự động tăng 1, 2, 3...)
        User newUser = new User(null, username, hashedPassword);
        userRepository.save(newUser);

        System.out.println("Đăng ký thành công");
        return true;

    }

//    Login
    public boolean login (String username, String rawPassword){
        User user = userRepository.findByUsername(username);

        if(user == null){
            System.out.println("Lỗi đăng nhập");
            return false;
        }
//        Kiểm tra có khớp với băm hay không
        if(BCrypt.checkpw(rawPassword, user.getPasswordHash())){
            System.out.println("Đăng nhập thành công");
            return true;
        }
        else {
            System.out.println("Sai thông tin đăng nhập");
            return false;
        }
    }
}
