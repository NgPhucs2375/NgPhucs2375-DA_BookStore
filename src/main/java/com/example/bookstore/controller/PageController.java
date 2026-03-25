package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @Autowired
    private BookRepository bookRepository;

    // Khi người dùng gõ link: localhost:8080/book/1
    @GetMapping("/book/{id}")
    public String viewBookDetail(@PathVariable Long id, Model model) {

        // 1. Chui vào kho tìm sách theo ID
        Book book = bookRepository.findById(id).orElse(null);

        // 2. Không thấy sách thì đá về trang chủ
        if (book == null) {
            return "redirect:/";
        }

        // 3. Lấy sách bỏ vào "giỏ hàng" Model để shipper mang sang file HTML
        model.addAttribute("book", book);

        // 4. Mở file BookDetail.html (Spring Boot tự động đi tìm file này trong thư mục templates)
        return "main/Details_Produce";
    }
}