package com.example.bookstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/seller/book")
public class SellerViewController {

    @GetMapping("/form")
    public String showBookForm(@RequestParam(required = false) Long id, Model model) {
        // Đưa cái ID vào model để Thymeleaf hoặc JS có thể lấy nếu cần (không bắt buộc vì mình lấy qua URL)
        model.addAttribute("bookId", id);

        // Trả về tên file HTML của bro trong thư mục src/main/resources/templates
        // Ví dụ: product_detail.html -> trả về "product_detail"
        return "seller/Seller_Product_Detail";
    }
}