package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.bookstore.repository.BookRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.bookstore.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile; 

@RestController // Bao cho SB biet class nay chuyen dung de tao API tra ve du lieu(thuong la dinh dang JSON) chu khong phai tra ve giao dien HTML
@CrossOrigin("*") // thẻ VIP để có thể ra vào dữ liệu
@RequestMapping("/api/books") // Dat "dia chi goc" cho toan bo cac API trong class nay
public class BookController {


    @Autowired // Day chinh la co che Dependency Injection quen thuoc
    // (tuong tu nhu cach lam viec voi interface trong cac project .NET). Spring Boot se tu dong "tiem" BookService vao de dung ma khong can thiet phai viet
    //API lay danh sach toan bo sach
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping // Bao hieu rang ham getAllBooks() se duoc chay khi co ai do truy cap vao dia chi goc bang phuong thuc Get(nhu khi go link tren trinh duyet)
    public Page<Book> getBooks(
            @RequestParam(defaultValue = "0") int page, // Trang số mấy (Mặc định trang 0)
            @RequestParam(defaultValue = "20") int size // Lấy bao nhiêu cuốn (Mặc định 20)
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepository.findAll(pageable);
    }

    @GetMapping("/search")
    public Page<Book> searchApprovedBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        return bookRepository.searchApprovedBooks(keyword, categoryId, ApprovalStatus.APPROVED, pageable);
    }

    // --- API add new book ---
    // @RequestBody : khi gui 1 cuc dl Json chua thong tin sach SB auto "nan" JSON do thanh 1 Doi tuong "Object" Book in Java tinh nang nay same [FromBody] trong .Net API
    @PostMapping
    public Book createBook(@RequestBody Book book){
        return bookService.addBook(book);
    }

    //API take one book by id
    //Dau ngoac nhon {id} nghia la gia tri nay se thay doi theo tren Url (vd: /api/books/1)
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id){
        return bookService.getBookbyId(id);
    }

    //API delete book by id
    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable Long id){
        bookService.deleteBoook(id);
    }
    //API update
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        return bookService.updateBook(id, bookDetails);
    }

    // API add new book cho Seller (S03)
    @PostMapping("/seller")
    public ResponseEntity<?> createBookForSeller(
        @RequestBody Book book,
        HttpServletRequest request // Lấy request để đọc ID đã decode
    ){
        //1 lấy ID người bán từ request (đã được decode trong filter)
        Long sellerId = (long) request.getAttribute("CURRENT_USER_ID");
        if(sellerId == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thực hiện hành động này");
        }

        //2 chặn 1 số thẻ HTML nguy hiểm bằng replace từ thư viện StringEscapeUtils
        book.setTitle(book.getTitle().replaceAll("<", "&lt;").replaceAll(">", "&gt;")); //&lt; = <, &gt; = >
        book.setDescription(securityUtils.sanitizeHtml(book.getDescription())); // Chỉ cho phép một số thẻ HTML cơ bản, chặn <script> và các thẻ nguy hiểm khác

        // 3 đẩy dữ liệu vào service để xử lý logic thêm sách mới
        try {
            Book createdBook = bookService.addBookForSeller(book, sellerId);
            return ResponseEntity.ok(createdBook);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
    }

    // API Update book cho Seller (S03)
    @PutMapping("/seller/{id}")
    public ResponseEntity<?> updateBookForSeller(
        @PathVariable Long id,
        @RequestBody Book bookDetails,
        HttpServletRequest request
    ){
        Long sellerId = (long) request.getAttribute("CURRENT_USER_ID");
        if(sellerId == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
             // Bắt lỗi IDOR từ Service ném lên (IDOR: như việc vượt quyền từ user mà nhảy dc vào seller căng lắm là admin để sửa hoặc thêm sách )
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
        // API upload ảnh cho sách (S03)
        @PostMapping("/seller/{id}/upload-cover")
        public ResponseEntity<?> upLoadBookCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
        ){
            Long sellerId = (Long) request.getAttribute("CURRENT_USER_ID");
            if(sellerId == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            try{
                // Check file có phải là ảnh ko bằng file signature 
                String imageUrl = bookService.uploadAndVerifyCoverImage(id, file, sellerId);
                return ResponseEntity.ok(imageUrl);
            } catch (RuntimeException e){
                return ResponseEntity.badRequest().body("Upload thất bại: " + e.getMessage());
            }
        }
}
