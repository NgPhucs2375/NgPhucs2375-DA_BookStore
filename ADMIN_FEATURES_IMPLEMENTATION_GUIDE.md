# 📋 ADMIN FEATURES IMPLEMENTATION GUIDE - BOOKOM BOOKSTORE

## 📊 PHÂN TÍCH CODEBASE HIỆN TẠI

### 1. **KIẾN TRÚC DỰ ÁN**
```
src/main/java/com/example/bookstore/
├── controller/
│   ├── AdminBookController.java       ✓ (Kiểm duyệt sách pending)
│   ├── AuthController.java           (Xác thực)
│   ├── BookController.java           (Lấy/tìm sách)
│   ├── CartController.java           (Giỏ hàng)
│   ├── OrderController.java          (Đơn hàng)
│   ├── PanelController.java          (API /api/panel - dữ liệu dashboard)
│   └── PanelPageController.java      (Định tuyến trang admin/seller)
├── model/
│   ├── User.java                     (Người dùng - cần thêm isActive)
│   ├── Book.java                     (Sách - cần thêm isActive)
│   ├── Order.java                    (Đơn hàng)
│   ├── SubOrder.java                 (Chi tiết đơn hàng từng seller)
│   ├── OrderItem.java                (Mục trong đơn hàng)
│   └── enums/
│       ├── UserRole.java             (BUYER, SELLER, ADMIN)
│       ├── ApprovalStatus.java       (PENDING, APPROVED, REJECTED)
│       └── OrderStatus.java          (PENDING_PAYMENT, PROCESSING, etc)
├── repository/
│   ├── UserRepository.java
│   ├── BookRepository.java
│   ├── OrderRepository.java
│   ├── SubOrderRepository.java
│   └── CategoryRepository.java
├── service/
│   ├── BookService.java              (Lôgic sách)
│   ├── OrderService.java             (Lôgic đơn hàng)
│   ├── AuthService.java              (Xác thực)
│   └── CategoryService.java
└── security/
    └── JwtTokenProvider.java         (JWT authentication)

src/main/resources/
├── templates/
│   ├── admin/
│   │   ├── Admin.html                ✓ (Dashboard)
│   │   ├── Admin_Books.html          ✓ (Kiểm duyệt sách)
│   │   ├── Admin_Users.html          ✓ (Xem người dùng)
│   │   └── Admin_Shops.html          ✓ (Xem shop)
│   └── fragments/
│       ├── admin_layout.html         (Sidebar + Header)
│       └── panel_head.html           (Meta tags)
└── static/
    └── js/
        ├── panel-data.js             (Init admin pages)
        └── api-service.js            (HTTP utilities)
```

---

## ✅ CHỨC NĂNG HIỆN CÓ

### **AdminBookController.java** (Đã có)
- `GET /api/admin/books/pending` → Lấy sách chờ duyệt
- `PUT /api/admin/books/{id}/status` → Duyệt/Từ chối sách

### **PanelController.java** (API dữ liệu)
- `GET /api/panel/summary` → Metrics tổng quan
- `GET /api/panel/books` → Danh sách sách có filter
- `GET /api/panel/users` → Danh sách người dùng
- `GET /api/panel/shops` → Danh sách shop

### **PanelPageController.java** (Định tuyến trang)
- `GET /admin` → Admin dashboard
- `GET /admin/users` → Trang quản lý người dùng
- `GET /admin/books` → Trang kiểm duyệt sách
- `GET /admin/shops` → Trang xem shop

---

## 🎯 CÁC CHỨC NĂNG CẦN BỔ SUNG

### **A01: DASHBOARD TỔNG QUAN** ✨
**Mục tiêu:** Cải tiến dashboard hiện tại với metrics chi tiết hơn

**Backend cần thêm:**
1. Mở rộng `GET /api/panel/summary`
   - Tổng đơn hàng, đơn chờ xử lý, đơn hoàn thành
   - Tổng người dùng, người dùng mới hôm nay
   - Tổng doanh thu (GMV)
   - Số sách bị khóa / sách mới
   - Biểu đồ doanh thu theo ngày/tuần/tháng

**Frontend:**
- Thêm cards thống kê mới
- Biểu đồ line chart (doanh thu theo thời gian)
- Biểu đồ pie chart (tỉ lệ đơn hàng)

---

### **A02: KHÓA/MỞ USER** 🔒
**Mục tiêu:** Admin có thể khóa/mở tài khoản người dùng

**Bước 1: Thêm field vào User model**
```java
@Column(columnDefinition = "BIT DEFAULT 1")
private boolean isActive = true;  // true = mở, false = khóa
```

**Bước 2: Thêm Repository method**
```java
// UserRepository.java
List<User> findByRoleAndIsActive(UserRole role, boolean isActive);
```

**Bước 3: Tạo Service method**
```java
// UserService.java (cần tạo file này)
public User lockUser(Long userId);
public User unlockUser(Long userId);
public List<User> getUsersByStatus(UserRole role, boolean isActive);
```

**Bước 4: Tạo Admin Controller**
```java
// AdminUserController.java (cần tạo)
@PutMapping("/{id}/lock")
public ResponseEntity<?> lockUser(@PathVariable Long id);

@PutMapping("/{id}/unlock")
public ResponseEntity<?> unlockUser(@PathVariable Long id);
```

**Bước 5: API dữ liệu**
```java
// Thêm vào PanelController.java
@GetMapping("/users-detailed")
public List<Map<String, Object>> getUsersDetailed(
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String role,
    @RequestParam(required = false) String status
);
```

**Frontend:**
- Thêm cột "Trạng thái" (Hoạt động/Khóa) vào bảng Admin_Users.html
- Thêm nút "Khóa" / "Mở" trong cột Actions
- Xử lý API gọi lock/unlock user

---

### **A03: KIỂM DUYỆT SÁCH (Mở rộng)** 📚
**Mục tiêu:** Review, Delete, Update, Lock books

**Bước 1: Thêm field vào Book model**
```java
@Column(columnDefinition = "BIT DEFAULT 1")
private boolean isActive = true;  // true = khả dụng, false = khóa
```

**Bước 2: Mở rộng BookRepository**
```java
// BookRepository.java
Page<Book> findByApprovalStatusAndIsActive(
    ApprovalStatus status, 
    boolean isActive,
    Pageable pageable
);
```

**Bước 3: Mở rộng BookService**
```java
// BookService.java
public Book deleteBook(Long bookId, Long adminId);
public Book updateBookByAdmin(Long bookId, BookUpdateDto dto);
public Book lockBook(Long bookId);
public Book unlockBook(Long bookId);
```

**Bước 4: Mở rộng AdminBookController**
```java
// AdminBookController.java
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteBook(@PathVariable Long id);

@PutMapping("/{id}")
public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody BookUpdateDto dto);

@PutMapping("/{id}/lock")
public ResponseEntity<?> lockBook(@PathVariable Long id);

@PutMapping("/{id}/unlock")
public ResponseEntity<?> unlockBook(@PathVariable Long id);
```

**Bước 5: API dữ liệu**
```java
// PanelController.java
@GetMapping("/books-all")
public Page<Map<String, Object>> getAllBooksForAdmin(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String status,  // all/pending/approved/rejected
    @RequestParam(required = false) String active    // all/active/locked
);
```

**Frontend:**
- Tạo trang Admin_Books_Detailed.html (thay thế hoặc mở rộng Admin_Books.html)
- Hiển thị tất cả sách (không chỉ pending)
- Thêm cột: Trạng thái duyệt, Trạng thái hoạt động, Seller
- Thêm nút Actions: Duyệt, Từ chối, Sửa, Xóa, Khóa, Mở

---

### **A04: XEM TOÀN BỘ ĐƠN HÀNG** 📦
**Mục tiêu:** Admin xem tất cả đơn hàng, filter, xem chi tiết

**Bước 1: Mở rộng OrderRepository**
```java
// OrderRepository.java
Page<Order> findAll(Pageable pageable);
Page<Order> findByBuyer(User buyer, Pageable pageable);
// Thêm query tìm kiếm theo trạng thái, ngày
```

**Bước 2: Tạo AdminOrderController**
```java
// AdminOrderController.java (cần tạo)
@GetMapping()
public ResponseEntity<?> getAllOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String q,           // search
    @RequestParam(required = false) String status,      // pending/processing/completed/cancelled
    @RequestParam(required = false) String dateFrom,
    @RequestParam(required = false) String dateTo
);

@GetMapping("/{id}")
public ResponseEntity<?> getOrderDetails(@PathVariable Long id);
```

**Bước 3: Mở rộng OrderService**
```java
// OrderService.java
public Page<Order> getOrdersWithFilters(
    int page, int size, String q, String status, 
    LocalDate dateFrom, LocalDate dateTo
);
public OrderDetailResponse getOrderDetails(Long orderId);
```

**Bước 4: API dữ liệu**
```java
// PanelController.java
@GetMapping("/orders")
public Page<Map<String, Object>> getOrdersForAdmin(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String dateFrom,
    @RequestParam(required = false) String dateTo
);

@GetMapping("/orders/{id}")
public Map<String, Object> getOrderDetailsForAdmin(@PathVariable Long id);
```

**Frontend:**
- Tạo trang Admin_Orders.html
- Bảng hiển thị: Order ID, Buyer, Total, Status, Date, Actions
- Filter: Trạng thái, Ngày, Tìm kiếm
- Modal/Page chi tiết đơn hàng
- Nút thao tác: Xem chi tiết, Hủy đơn (nếu chưa xử lý)

---

## 🛠️ CÁC FILE CẦN TẠOVÀ CHỈNH SỬA

### **Backend - Tạo file mới:**
1. `AdminUserController.java` - API quản lý user
2. `AdminOrderController.java` - API quản lý đơn hàng
3. `UserService.java` - Business logic user
4. `AdminOrderService.java` - Business logic order cho admin
5. DTO files:
   - `BookUpdateDto.java`
   - `UserStatusUpdateDto.java`
   - `OrderDetailResponse.java`

### **Backend - Chỉnh sửa file:**
1. `User.java` - Thêm field `isActive`
2. `Book.java` - Thêm field `isActive`
3. `UserRepository.java` - Thêm query methods
4. `BookRepository.java` - Thêm query methods
5. `OrderRepository.java` - Thêm query methods
6. `AdminBookController.java` - Thêm DELETE, UPDATE, LOCK endpoints
7. `BookService.java` - Thêm methods lock/unlock/update
8. `PanelController.java` - Thêm API lấy dữ liệu chi tiết

### **Frontend - Tạo file mới:**
1. `Admin_Orders.html` - Trang quản lý đơn hàng
2. `Admin_Books_Review.html` (hoặc mở rộng Admin_Books.html)

### **Frontend - Chỉnh sửa file:**
1. `admin_layout.html` - Thêm menu "Đơn hàng" vào sidebar
2. `Admin_Users.html` - Thêm cột trạng thái + nút lock/unlock
3. `Admin_Books.html` - Thêm cột trạng thái + nút delete/update/lock
4. `panel-data.js` - Thêm functions initAdminOrders, initAdminUsersLock, etc
5. `pom.xml` - Kiểm tra dependencies

---

## 📡 API ENDPOINTS DESIGN

### **A01: Dashboard**
```
GET /api/panel/summary
Response:
{
  "gmv": 150000000,
  "books": 5432,
  "categories": 45,
  "shops": 234,
  "totalOrders": 12345,
  "pendingOrders": 234,
  "completedOrders": 11000,
  "totalUsers": 5000,
  "newUsersToday": 45,
  "revenueChart": { ... },
  "categoryStats": { ... },
  "stockBuckets": { ... }
}
```

### **A02: User Management**
```
GET /api/panel/users-detailed?q=&role=all&status=all
PUT /api/admin/users/{id}/lock
PUT /api/admin/users/{id}/unlock
```

### **A03: Book Management**
```
GET /api/panel/books-all?page=0&size=20&status=all&active=all
PUT /api/admin/books/{id}         { title, author, price, ... }
DELETE /api/admin/books/{id}
PUT /api/admin/books/{id}/lock
PUT /api/admin/books/{id}/unlock
```

### **A04: Orders**
```
GET /api/admin/orders?page=0&size=20&q=&status=all&dateFrom=&dateTo=
GET /api/admin/orders/{id}
```

---

## 🔐 SECURITY CONSIDERATIONS

1. **Role Check:** Tất cả endpoints phải check `CURRENT_USER_ROLE == "ADMIN"`
2. **Logging:** Ghi lại mọi thao tác thay đổi dữ liệu (delete, lock, update)
3. **Audit Trail:** Thêm `adminId`, `actionTime`, `actionType` để tracking
4. **Rate Limiting:** Cân nhắc giới hạn API calls
5. **Input Validation:** Validate tất cả input từ request

---

## 📋 IMPLEMENTATION ORDER

1. **Phase 1: Database & Models**
   - ✓ Add `isActive` to User & Book
   - ✓ Create UserService
   - ✓ Create DTO classes

2. **Phase 2: Backend API**
   - ✓ Create AdminUserController
   - ✓ Create AdminOrderController
   - ✓ Mở rộng BookService
   - ✓ Mở rộng PanelController

3. **Phase 3: Frontend Pages**
   - ✓ Create Admin_Orders.html
   - ✓ Update Admin_Books.html
   - ✓ Update Admin_Users.html
   - ✓ Update admin_layout.html

4. **Phase 4: Testing & Deployment**
   - ✓ Test APIs với Postman/Thunder Client
   - ✓ Test UI functionality
   - ✓ Load testing
   - ✓ Security review

---

## 💡 TIPS & BEST PRACTICES

1. **Use Spring Data JPA** - Annotations @Query, @Param tiết kiệm code
2. **Pagination** - Tất cả list endpoints phải có phân trang
3. **Error Handling** - Trả về HTTP status codes chính xác
4. **Frontend State** - Dùng sessionStorage/localStorage lưu filters
5. **Debouncing** - Input search nên debounce 300-500ms
6. **Responsive UI** - Kiểm tra trên mobile/tablet

---

## 📚 REFERENCE DOCUMENTS

- [Existing Admin Book Controller](AdminBookController.java)
- [Existing Panel Controller](PanelController.java)
- [Database Schema](db/migration/V*.sql)
- [Frontend Architecture](panel-data.js)

---

**Created:** May 16, 2026  
**Version:** 1.0  
**Status:** Ready for Implementation
