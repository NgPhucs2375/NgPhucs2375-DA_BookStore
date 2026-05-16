package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.bookstore.repository.BookRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.example.bookstore.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController // Bao cho SB biet class nay chuyen dung de tao API tra ve du lieu thuong la dinh dang JSON chu khong phai tra ve giao dien HTML
@CrossOrigin("*") // thẻ VIP để có thể ra vào dữ liệu
@RequestMapping("/api/books") // Dat dia chi goc cho toan bo cac API trong class nay
public class BookController {

    @Autowired // Day chinh la co che Dependency Injection quen thuoc tuong tu nhu cach lam viec voi interface trong cac project .NET. Spring Boot se tu dong tiem BookService vao de dung ma khong can thiet phai viet
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping // Bao hieu rang ham getAllBooks se duoc chay khi co ai do truy cap vao dia chi goc bang phuong thuc Get nhu khi go link tren trinh duyet
    public Page<Book> getBooks(
            @RequestParam(defaultValue = "0") int page, // Trang số mấy - Mặc định trang 0
            @RequestParam(defaultValue = "20") int size // Lấy bao nhiêu cuốn - Mặc định 20
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepository.findAll(pageable);
    }

    @GetMapping("/search")
    public Page<Book> searchApprovedBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String publishYearFrom,
            @RequestParam(required = false) String publishYearTo,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        String authorKeyword = (author == null || author.isBlank()) ? null : author.trim();
        Integer yearFrom = parseYearBound(publishYearFrom);
        Integer yearTo = parseYearBound(publishYearTo);
        return bookRepository.searchApprovedBooks(
                keyword,
                categoryId,
                authorKeyword,
                minPrice,
                maxPrice,
                yearFrom,
                yearTo,
                ApprovalStatus.APPROVED,
                pageable
        );
    }

    private Integer parseYearBound(String rawYear) {
        if (rawYear == null || rawYear.isBlank()) {
            return null;
        }
        return Integer.valueOf(rawYear.trim());
    }

    @GetMapping("/suggestions")
    public List<Book> getSearchSuggestions(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "8") int size) {
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        if (keyword == null) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(size, 12)));
        return bookRepository.findSuggestions(keyword, ApprovalStatus.APPROVED, pageable);
    }

    @GetMapping("/discovery/best-sellers")
    public List<Book> getBestSellingBooks(@RequestParam(defaultValue = "8") int size) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(size, 12)));
        return bookRepository.findBestSellingBooks(ApprovalStatus.APPROVED, pageable);
    }

    @GetMapping("/discovery/trending")
    public List<Book> getTrendingBooks(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "8") int size) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(size, 12)));
        LocalDateTime since = LocalDateTime.now().minusDays(Math.max(1, days));
        return bookRepository.findTrendingBooks(ApprovalStatus.APPROVED, since, pageable);
    }

    /**
     * API lấy danh sách sách của seller bao gồm PENDING, APPROVED, REJECTED
     * Dùng cho trang quản lý kho - S03
     */
    @GetMapping("/seller/me")
    public Page<Book> getSellerBooks(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Lấy ID từ Attribute cho giống mấy hàm Add Update - Đồng bộ với logic mới
        Long sellerId = (Long) request.getAttribute("CURRENT_USER_ID");

        if (sellerId == null) return Page.empty();

        String keyword = (q == null || q.trim().isEmpty()) ? null : q.trim();
        Pageable pageable = PageRequest.of(page, size);

        return bookRepository.findBySellerIdAndKeywordAndCategory(sellerId, keyword, categoryId, pageable);
    }

    // API take one book by id
    // Dau ngoac nhon id nghia la gia tri nay se thay doi theo tren Url vd: /api/books/1
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookbyId(id);
    }

    // --- API add new book cho Seller - S03 ---
    // @RequestBody : khi gui 1 cuc dl Json chua thong tin sach SB auto nan JSON do thanh 1 Doi tuong Object Book in Java tinh nang nay same FromBody trong .Net API
    @PostMapping({"", "/seller"})
    public ResponseEntity<?> createBookForSeller(
            @RequestBody Book book,
            HttpServletRequest request
    ) {
        Long sellerId = (Long) request.getAttribute("CURRENT_USER_ID");
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thực hiện hành động này");
        }

        // Chặn đóng mở thẻ - Chống XSS cơ bản
        if (book.getTitle() != null) {
            book.setTitle(book.getTitle().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
        }

        if (book.getDescription() != null) {
            book.setDescription(securityUtils.sanitizeHtml(book.getDescription()));
        }

        try {
            Book createdBook = bookService.addBookForSeller(book, sellerId);
            return ResponseEntity.ok(createdBook);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API Update book cho Seller - S03
    @PutMapping({"/{id}", "/seller/{id}"})
    public ResponseEntity<?> updateBookForSeller(
            @PathVariable Long id,
            @RequestBody Book bookDetails,
            HttpServletRequest request) {
        Long sellerId = (Long) request.getAttribute("CURRENT_USER_ID");
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Chặn XSS
        if (bookDetails.getTitle() != null) {
            bookDetails.setTitle(bookDetails.getTitle().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
        }

        if (bookDetails.getDescription() != null) {
            bookDetails.setDescription(securityUtils.sanitizeHtml(bookDetails.getDescription()));
        }

        try {
            Book updatedBook = bookService.updateBookForSeller(id, bookDetails, sellerId);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            // Bắt lỗi IDOR từ Service ném lên - IDOR: như việc vượt quyền từ user mà nhảy dc vào seller căng lắm là admin để sửa hoặc thêm sách
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // API upload ảnh cho sách - S03
    @PostMapping({"/{id}/upload-cover", "/seller/{id}/upload-cover"})
    public ResponseEntity<?> upLoadBookCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long sellerId = (Long) request.getAttribute("CURRENT_USER_ID");
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            // Check file signature chống RCE - Bảo mật tầng cao
            String imageUrl = bookService.uploadAndVerifyCoverImage(id, file, sellerId);
            return ResponseEntity.ok(imageUrl);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload thất bại: " + e.getMessage());
        }
    }

    // Thêm API xóa sách cho Seller (S03)
    @DeleteMapping({"/{id}", "/seller/{id}"})
    public ResponseEntity<?> deleteBookForSeller(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long sellerId = (Long) request.getAttribute("CURRENT_USER_ID");
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            bookService.deleteBookForSeller(id, sellerId);

            // SỬA DÒNG NÀY: Trả về JSON thay vì Text thuần
            return ResponseEntity.ok(java.util.Map.of("message", "Xóa sách thành công!"));

        } catch (RuntimeException e) {

            // SỬA DÒNG NÀY: Bọc cái lỗi vào JSON luôn
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("message", e.getMessage()));

        }
    }

    private Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "id");
        }

        return switch (sort.toLowerCase()) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            case "title-asc" -> Sort.by(Sort.Direction.ASC, "title");
            case "author-asc" -> Sort.by(Sort.Direction.ASC, "author");
            case "latest" -> Sort.by(Sort.Direction.DESC, "id");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }
}