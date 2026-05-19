# 📝 Kế hoạch triển khai Hệ thống Đánh giá & Nhận xét (Rating & Review)

Tài liệu này mô tả chi tiết kế hoạch thiết kế và triển khai tính năng Đánh giá sản phẩm cho hệ thống BookStore.

## 1. Mục tiêu
- Cho phép người mua thực tế đánh giá sách (1-5 sao) và để lại nhận xét.
- Đảm bảo tính xác thực: Chỉ người đã mua và nhận hàng mới được đánh giá.
- Cung cấp giao diện xem đánh giá trực quan tại trang chi tiết sản phẩm.
- Cho phép Admin quản lý/ẩn các đánh giá không phù hợp.

## 2. Thiết kế Cơ sở dữ liệu (V17__create_book_reviews.sql)
Bảng `book_reviews`:
- `id` (BIGINT, PK, Identity)
- `book_id` (BIGINT, FK -> books.id)
- `user_id` (BIGINT, FK -> users.id)
- `rating` (INT, 1-5)
- `comment` (NVARCHAR(MAX))
- `created_at` (DATETIME)
- `is_hidden` (BIT, Default 0)

## 3. Thành phần Backend (Java)

### 3.1. Entity & Repository
- `BookReview.java`: Mapping JPA.
- `BookReviewRepository.java`:
    - `findByBookIdAndIsHiddenFalseOrderByCreatedAtDesc(Long bookId, Pageable pageable)`
    - `countByBookIdAndRating(Long bookId, int rating)`
    - `calculateAverageRating(Long bookId)` (Custom Query)

### 3.2. DTOs
- `ReviewRequest`: `rating`, `comment`, `bookId`.
- `ReviewResponse`: `id`, `username`, `rating`, `comment`, `createdAt`, `isPurchased` (True).

### 3.3. Service (`BookReviewService.java`)
- `addReview(User user, ReviewRequest req)`:
    - Kiểm tra user đã mua sách này chưa (Check `Order` & `OrderItem` status).
    - Lưu vào DB.
- `getReviewsByBook(Long bookId, int page, int size)`: Trả về danh sách review.
- `getReviewStats(Long bookId)`: Trả về tổng số review và điểm trung bình.

### 3.4. Controller (`BookReviewController.java`)
- `GET /api/reviews/book/{bookId}`: Công khai.
- `POST /api/reviews`: Bảo mật (Role: BUYER).
- `DELETE /api/admin/reviews/{id}`: Bảo mật (Role: ADMIN) - Ẩn review.

## 4. Giao diện Frontend (Web)

### 4.1. Trang Chi tiết sản phẩm (`Details_Produce.html`)
- Hiển thị điểm trung bình và biểu đồ sao (5 sao, 4 sao...).
- Danh sách nhận xét có phân trang.
- Form "Viết nhận xét" chỉ hiện nếu:
    - User đã đăng nhập.
    - User đã mua sách (Gọi API kiểm tra).

### 4.2. Logic JavaScript (`details-page.js`)
- `loadReviews(bookId)`: Gọi API và render HTML.
- `submitReview()`: Gửi data lên server.

## 5. Kế hoạch thực hiện (Các bước)
1. **Bước 1**: Chạy SQL Migration V17.
2. **Bước 2**: Viết Entity, Repository, DTO.
3. **Bước 3**: Viết Service logic (bao gồm kiểm tra quyền mua hàng).
4. **Bước 4**: Viết API Controller.
5. **Bước 5**: Cập nhật giao diện Thymeleaf và JS.
6. **Bước 6**: Kiểm thử (Unit test & Integration test).

