package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/books") // Đường dẫn xịn dành riêng cho Admin
public class AdminBookController {

    @Autowired
    private BookService bookService;

    // Lấy danh sách sách chờ duyệt (PENDING)
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Book> pendingBooks = bookService.getPendingBooksForAdmin(page, size);
        return ResponseEntity.ok(pendingBooks);
    }

    // Duyệt hoặc từ chối sách
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBookStatus(
            @PathVariable Long id,
            @RequestParam ApprovalStatus status // Truyền status lên URL, ví dụ: ?status=APPROVED
    ) {
        try {
            Book updatedBook = bookService.changeBookApprovalStatus(id, status);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Xóa sách
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBook(
            @PathVariable Long id
    ) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok("Xóa sách thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin sách
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBook(
            @PathVariable Long id,
            @RequestBody com.example.bookstore.dto.BookUpdateDto dto
    ) {
        try {
            Book updatedBook = bookService.updateBookByAdmin(id, dto);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Khóa sách (không cho phép hiển thị)
     */
    @PutMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> lockBook(
            @PathVariable Long id
    ) {
        try {
            Book book = bookService.lockBook(id);
            return ResponseEntity.ok(book);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mở khóa sách
     */
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockBook(
            @PathVariable Long id
    ) {
        try {
            Book book = bookService.unlockBook(id);
            return ResponseEntity.ok(book);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}