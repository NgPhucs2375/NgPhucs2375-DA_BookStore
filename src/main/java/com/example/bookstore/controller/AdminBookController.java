package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.service.BookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/books") // Đường dẫn xịn dành riêng cho Admin
public class AdminBookController {

    @Autowired
    private BookService bookService;

    // Lấy danh sách sách chờ duyệt (PENDING)
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        // Kiểm tra Role Admin
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Truy cập bị từ chối: Chỉ Admin mới có quyền xem danh sách này!");
        }

        Page<Book> pendingBooks = bookService.getPendingBooksForAdmin(page, size);
        return ResponseEntity.ok(pendingBooks);
    }

    // Duyệt hoặc từ chối sách
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBookStatus(
            @PathVariable Long id,
            @RequestParam ApprovalStatus status, // Truyền status lên URL, ví dụ: ?status=APPROVED
            HttpServletRequest request
    ) {
        // PENTESTER SHIELD: Kiểm tra Role Admin
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Truy cập bị từ chối: Chỉ Admin mới có quyền duyệt sách!");
        }

        try {
            Book updatedBook = bookService.changeBookApprovalStatus(id, status);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}