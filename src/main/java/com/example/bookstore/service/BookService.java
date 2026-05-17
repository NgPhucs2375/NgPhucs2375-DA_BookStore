package com.example.bookstore.service;

import com.example.bookstore.dto.BookUpdateDto;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service // Danh dau lai lop nay la Logic nghiep vu nhan yeu cau tu roi xu lys
public class BookService {
    @Autowired // keu SB tu dong "Tiem" dl Repository vao day
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.uploads.covers-dir:uploads/covers}")
    private String coversDir;

    // Function lay all book
    public List<Book> getAllBook() {
        // Sau nay co the them Logic o day (VD : Where : ....)
        return bookRepository.findAll();
    }

    // Function add new book
    public Book addBook(Book book) {
        // nho Repository save new book into SSMS
        return bookRepository.save(book);
    }

    // Function get 1 book theo ID
    public Book getBookbyId(Long id) {
        // Ham findById tra ve kieu Optional(co the co hoac khong co du lieu)
        // Dung orElse(null) nghia la : Neu khong tim thay sach thi tra ve null
        return bookRepository.findById(id).orElse(null);
    }

    /**
     * Xóa sách (Admin)
     */
    public void deleteBook(Long bookId) {
        bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        bookRepository.deleteById(bookId);
    }

    // Function Update info 1 book
    public Book updateBook(Long id, Book bookDetails) {
        // 1. Find old book in DB
        Book existingBook = bookRepository.findById(id).orElse(null);

        // 2. Iffind, process force new db into
        if (existingBook != null) {
            existingBook.setTitle(bookDetails.getTitle());
            existingBook.setAuthor(bookDetails.getAuthor());
            existingBook.setDescription(bookDetails.getDescription());
            existingBook.setPrice(bookDetails.getPrice());
            existingBook.setStockQuantity(bookDetails.getStockQuantity());
            // 3. Save into DB
            return bookRepository.save(existingBook);
        }
        // return null if can't find id
        return null;
    }

    /**
     * Cập nhật thông tin sách (Admin)
     */
    public Book updateBookByAdmin(Long bookId, BookUpdateDto dto) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        if (dto.getTitle() != null && !dto.getTitle().isEmpty()) {
            book.setTitle(dto.getTitle());
        }
        if (dto.getAuthor() != null && !dto.getAuthor().isEmpty()) {
            book.setAuthor(dto.getAuthor());
        }
        if (dto.getDescription() != null) {
            book.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            book.setPrice(dto.getPrice());
        }
        if (dto.getStockQuantity() != null) {
            book.setStockQuantity(dto.getStockQuantity());
        }
        if (dto.getPublisher() != null) {
            book.setPublisher(dto.getPublisher());
        }
        if (dto.getPublishYear() != null) {
            try {
                book.setPublishYear(Integer.parseInt(dto.getPublishYear()));
            } catch (NumberFormatException e) {
                // Ignore invalid year format
            }
        }

        return bookRepository.save(book);
    }

    /**
     * Khóa sách
     */
    public Book lockBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        book.setActive(false);
        return bookRepository.save(book);
    }

    /**
     * Mở khóa sách
     */
    public Book unlockBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        book.setActive(true);
        return bookRepository.save(book);
    }
    // API dành cho admin S02
    // 1. lấy danh sách chờ duyệt
    public org.springframework.data.domain.Page<Book> getPendingBooksForAdmin(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return bookRepository.findByApprovalStatus(ApprovalStatus.PENDING, pageable);
    }
    // 2. Admin duyệt hoặc từ chối sách
    public Book changeBookApprovalStatus(Long bookId, ApprovalStatus newStatus) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + bookId));
        
        // Cập nhật trạng thái
        book.setApprovalStatus(newStatus);
        
        return bookRepository.save(book);
    }

    // --- BỔ SUNG CÁC HÀM BẢO MẬT DÀNH RIÊNG CHO SELLER (S03) ---

    public Book addBookForSeller(Book book, Long sellerId) {
        // 1. Tìm Seller từ ID lấy từ Token (Cực kỳ an toàn, không lo ID ảo)
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller không tồn tại"));

        // 2. TỰ ĐỘNG GÁN CHỦ SỞ HỮU (Dynamic)
        // Dòng này giúp Seller 81 thêm sẽ có ID 81, 82 có ID 82
        book.setSeller(seller);

        // 3. GIÁP CHỐNG LỖI SQL SERVER (Chặn đứng NULL cho các cột NOT NULL)
        // Tác giả
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            book.setAuthor("Đang cập nhật");
        }
        // Nhà xuất bản (Tôi thấy trong ảnh DB của bro có cột này và nó đang có data)
        if (book.getPublisher() == null || book.getPublisher().trim().isEmpty()) {
            book.setPublisher("NXB Mới");
        }
        // Năm xuất bản
        if (book.getPublishYear() == null) {
            book.setPublishYear(2026);
        }
        // Giá và số lượng (Tránh NULL gây lỗi tính toán)
        if (book.getPrice() == null) book.setPrice(0.0);
        if (book.getStockQuantity() == null) book.setStockQuantity(0);

        // 4. Trạng thái chờ duyệt
        book.setApprovalStatus(ApprovalStatus.PENDING);

        // 5. LƯU VÀO DATABASE
        return bookRepository.save(book);
    }

    public Book updateBookForSeller(Long bookId, Book bookDetails, Long sellerId) {
        Book existingBook = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

        // Kiểm tra quyền sở hữu: chỉ seller chủ sở hữu mới được sửa
        if (existingBook.getSeller() == null || !existingBook.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Không có quyền cập nhật sách này");
        }

        // Cập nhật thông tin an toàn
        if (bookDetails.getTitle() != null) existingBook.setTitle(bookDetails.getTitle());
        if (bookDetails.getDescription() != null) existingBook.setDescription(bookDetails.getDescription());
        if (bookDetails.getPrice() != null) existingBook.setPrice(bookDetails.getPrice());

        // --- THÊM 2 DÒNG NÀY VÀO ĐỂ UPDATE TỒN KHO VÀ TÁC GIẢ ---
        if (bookDetails.getStockQuantity() != null) existingBook.setStockQuantity(bookDetails.getStockQuantity());
        if (bookDetails.getAuthor() != null) existingBook.setAuthor(bookDetails.getAuthor());

        existingBook.setApprovalStatus(ApprovalStatus.PENDING); // Sửa xong bắt duyệt lại

        return bookRepository.save(existingBook);
    }

    public String uploadAndVerifyCoverImage(Long bookId, MultipartFile file, Long sellerId) throws java.io.IOException {
        Book existingBook = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

        // Kiểm tra quyền sở hữu: chỉ seller chủ sở hữu mới được upload ảnh
        if (existingBook.getSeller() == null || !existingBook.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Không có quyền upload ảnh cho sách này");
        }

        // Chống RCE bằng cách check File Signature
        org.apache.tika.Tika tika = new org.apache.tika.Tika();
        String mimeType = tika.detect(file.getInputStream());
        if (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png") && !mimeType.equals("image/webp")) {
            throw new RuntimeException("File tải lên không phải là định dạng ảnh hợp lệ!");
        }

        // Sinh tên file mới chống Path Traversal
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String safeFileName = java.util.UUID.randomUUID().toString() + extension;

        // --- ĐOẠN LƯU FILE THẬT VÀO Ổ CỨNG ---
        java.nio.file.Path uploadPath = java.nio.file.Paths.get(coversDir);
        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }
        java.nio.file.Path filePath = uploadPath.resolve(safeFileName);
        java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Lưu URL thật vào DB
        String realFileUrl = "/images/covers/" + safeFileName;
        existingBook.setImageUrl(realFileUrl);
        bookRepository.save(existingBook);

        return realFileUrl;
    }

    public void deleteBookForSeller(Long bookId, Long sellerId) {
        Book existingBook = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

        // Kiểm tra quyền sở hữu: chỉ seller chủ sở hữu mới được xóa
        if (existingBook.getSeller() == null || !existingBook.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Không có quyền xóa sách này");
        }

        bookRepository.delete(existingBook);
    }

}
