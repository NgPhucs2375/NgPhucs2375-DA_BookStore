# 📋 PHÂN TÍCH YÊU CẦU ĐỒ ÁN - So Sánh Project Hiện Tại vs Yêu Cầu

**Ngày phân tích**: 17/05/2026  
**Project**: BOOKOM BookStore  
**Trạng thái**: ~85% hoàn thành, cần làm thêm 15%

---

## 🎯 SECTION A: CHỨC NĂNG CHÍNH CỦA WEBSITE BÁN HÀNG

### **A.1 PHÂN HỆ DÀNH CHO KHÁCH HÀNG (USER)**

#### ✅ Chức năng YÊU CẦU vs HIỆN TẠI

| # | Chức năng | Yêu cầu | Hiện tại | Ghi chú |
|---|----------|--------|---------|---------|
| 1 | **Đăng ký / Đăng nhập** | ✅ Có | ✅ **ĐỦ** | AuthController, AuthPageController - có Auth/Register, Password Reset (Email OTP) |
| 2 | **Trang chủ** | ✅ Có | ✅ **ĐỦ** | index.html - hiển thị danh mục, sản phẩm mới, Flash Sale |
| 3 | **Tìm kiếm & Lọc** | ✅ Có | ✅ **ĐỦ** | Discovery_Page.html - Lọc theo giá, loại, đánh giá, sắp xếp |
| 4 | **Chi tiết sản phẩm** | ✅ Có | ✅ **ĐỦ** | Details_Produce.html - Ảnh, mô tả, giá, tồn kho, đánh giá, sản phẩm mua kèm |
| 5 | **Giỏ hàng** | ✅ Có | ✅ **ĐỦ** | Cart_Page.html - Thêm/xóa/sửa số lượng, hiệu suất grouping theo seller |
| 6 | **Thanh toán** | ✅ Có | ✅ **ĐỦ** | Checkout_Page.html - Nhập địa chỉ, chọn phương thức, áp dụng voucher, tính phí vận chuyển |
| 7 | **Lịch sử đơn hàng** | ✅ Có | ✅ **ĐỦ** | Buyer_DashBoard.html - Xem đơn, trạng thái (Chờ duyệt/Đang giao/Đã giao) |

**Status**: ✅ **100% - HOÀN THÀNH**

---

### **A.2 PHÂN HỆ DÀNH CHO QUẢN TRỊ VIÊN (ADMIN)**

#### ✅ Chức năng YÊU CẦU vs HIỆN TẠI

| # | Chức năng | Yêu cầu | Hiện tại | Ghi chú |
|---|----------|--------|---------|---------|
| 1 | **Dashboard** | ✅ Có | ⚠️ **THIẾU** | Không có thống kê doanh thu/đơn hàng/khách hàng |
| 2 | **Quản lý danh mục** | ✅ CRUD | ❌ **THIẾU** | Không có CRUD danh mục, chỉ có danh sách hiển thị |
| 3 | **Quản lý sản phẩm** | ✅ CRUD + upload | ✅ **ĐỦ** | Admin_Books.html - CRUD sách, upload ảnh cover |
| 4 | **Quản lý tồn kho** | ✅ Theo dõi | ✅ **ĐỦ** | Book có stockQuantity, self-decrement khi order |
| 5 | **Quản lý khuyến mãi** | ✅ Có | ⚠️ **THIẾU** | Có voucher cứng (BOOKOM15K, SAVE10) nhưng không có giao diện admin tạo mã |
| 6 | **Quản lý đơn hàng** | ✅ Xem + cập nhật trạng thái | ✅ **ĐỦ** | Admin_Orders.html - Xem danh sách order, update SubOrder status |
| 7 | **Quản lý người dùng** | ✅ Khóa/mở khóa | ⚠️ **THIẾU** | Có Admin_Users.html nhưng không có chức năng khóa/mở khóa tài khoản |
| 8 | **Quản lý đánh giá** | ✅ Xóa/duyệt comment | ❌ **THIẾU** | Không có hệ thống đánh giá (rating/review) |

**Status**: ⚠️ **~60% - THIẾU NHIỀU CHỨC NĂNG**

**THIẾU CẦN THÊM (URGENT)**:
- ❌ Dashboard với thống kê
- ❌ CRUD danh mục sản phẩm
- ❌ Admin quản lý tạo mã khuyến mãi (không hardcode)
- ❌ Khóa/mở khóa người dùng (lock account)
- ❌ Hệ thống đánh giá sản phẩm (rating/review/comments)

---

## 🚀 SECTION B: CHỨC NĂNG NÂNG CAO

### **B.1 YÊU CẦU BACKEND LOGIC**

#### ✅ Quản lý Kho (Inventory Management)

| Chức năng | Yêu cầu | Hiện tại | Ghi chú |
|-----------|--------|---------|---------|
| Trừ tồn kho tự động | ✅ Khi đặt hàng | ✅ **ĐỦ** | OrderService - tự động trừ book.stockQuantity |
| Cảnh báo sắp hết hàng | ✅ Thông báo admin khi < 5 | ⚠️ **THIẾU** | Không có cảnh báo tự động |
| Ngăn chặn vượt số lượng | ✅ Validate đặt hàng | ✅ **ĐỦ** | CartService - check stockQuantity vs requested qty |

**Status**: ⚠️ **67% - Thiếu cảnh báo**

---

#### ✅ Hệ thống Khuyến mãi (Promotion & Coupons)

| Chức năng | Yêu cầu | Hiện tại | Ghi chú |
|-----------|--------|---------|---------|
| Tạo mã giảm giá | ✅ % hoặc số tiền | ❌ **HARDCODE** | Chỉ có BOOKOM15K (15k), SAVE10 (10%) cứng trong checkout-page.js |
| Điều kiện áp dụng | ✅ Đơn tối thiểu | ⚠️ **THIẾU** | Checkout page chưa validate đơn tối thiểu |
| Hiển thị giá gốc/giá mới | ✅ Rõ ràng | ✅ **ĐỦ** | Cart_Page & Checkout_Page hiển thị giá gốc và voucher discount |

**Status**: ❌ **33% - Cần tạo hệ thống admin tạo coupon**

---

#### ✅ Trạng thái Đơn hàng Logic

| Chức năng | Yêu cầu | Hiện tại | Ghi chú |
|-----------|--------|---------|---------|
| Luồng chuyển trạng thái | ✅ Chờ xác nhận → Xác nhận → Đang giao → Hoàn thành | ✅ **ĐỦ** | SubOrder.ApprovalStatus enum: PENDING, APPROVED, REJECTED, SHIPPED, DELIVERED |
| Chỉ hủy khi "Chờ xác nhận" | ✅ Validate hủy | ✅ **ĐỦ** | OrderService.cancelCurrentBuyerOrder() - check PENDING status |

**Status**: ✅ **100% - HOÀN THÀNH**

---

### **B.2 YÊU CẦU TRẢI NGHIỆM NGƯỜI DÙNG (UX/FRONTEND)**

| Chức năng | Yêu cầu | Hiện tại | Ghi chú |
|-----------|--------|---------|---------|
| **Multi-filtering** | ✅ Lọc kết hợp nhiều | ✅ **ĐỦ** | Discovery_Page - Lọc giá, loại, đánh giá cùng lúc |
| **Sắp xếp** | ✅ Giá/phổ biến/mới | ✅ **ĐỦ** | Discovery_Page buttons - Phổ biến, Mới, Giá tăng/giảm |
| **Quick View** | ✅ Modal xem nhanh | ❌ **THIẾU** | Chỉ có modal cho address, không có quick view sản phẩm |
| **Remember Me** | ✅ Lưu phiên đăng nhập | ❌ **THIẾU** | Auth page chưa có "Remember me" checkbox |
| **Toast messages** | ✅ Thông báo thành công/lỗi | ⚠️ **THIẾU** | Alert() cơ bản chứ không có toast UI beautiful |

**Status**: ⚠️ **60% - Cần thêm Quick View, Remember Me, Toast UI**

---

### **B.3 YÊU CẦU BẢO MẬT & HỆ THỐNG**

| Chức năng | Yêu cầu | Hiện tại | Ghi chú |
|-----------|--------|---------|---------|
| **Phân quyền (Authorization)** | ✅ Middleware/Filter chặn | ✅ **ĐỦ** | SecurityConfig + JwtAuthenticationFilter - chặn /admin access |
| **Validation** | ✅ Email, mật khẩu, số ĐT | ✅ **ĐỦ** | AuthService, BuyerProfileService - validate input |
| **Lưu trữ hình ảnh** | ✅ Lưu file, đường dẫn DB | ✅ **ĐỦ** | BookService - upload cover vào `uploads/covers/`, lưu path DB |

**Status**: ✅ **100% - HOÀN THÀNH**

---

## 📦 SECTION C: YÊU CẦU NỘP BÀI

### **Nội dung Báo cáo Cần Có**

| # | Yêu cầu nộp | Hiện tại | Status |
|---|------------|---------|--------|
| 1 | **Giới thiệu đề tài** | ❌ Không có | ❌ **CẦN TẠO** |
| 2 | **Phân tích yêu cầu hệ thống** | ⚠️ Có docs nhưng không formal | ⚠️ **CẦN FORMAL** |
| 3 | **Use Case Diagram** | ❌ Không có | ❌ **CẦN TẠO** |
| 4 | **ERD Database** | ✅ Chi tiết trong docs | ✅ **ĐỦ** |
| 5 | **Thiết kế giao diện** | ✅ HTML/CSS Tailwind hoàn chỉnh | ✅ **ĐỦ** |
| 6 | **Mô tả chức năng** | ✅ Trong ADMIN_FEATURES_IMPLEMENTATION_GUIDE.md | ✅ **ĐỦ** |
| 7 | **Hướng dẫn chạy chương trình** | ⚠️ Có docs nhưng không chi tiết | ⚠️ **CẦN CHI TIẾT** |
| 8 | **Kết quả đạt được** | ❌ Không có | ❌ **CẦN TẠO** |
| 9 | **Hướng phát triển** | ⚠️ Có nhưng ở docs rời rạc | ⚠️ **CẦN TỤC LẠI** |

### **Yêu cầu nộp bài**

| Item | Hiện tại | Status |
|------|---------|--------|
| ✅ Source code | ✅ Có | ✅ **READY** |
| ✅ Database script SQL | ⚠️ Có DataSeeder nhưng không export .sql | ⚠️ **CẦN EXPORT** |
| ❌ Báo cáo PDF | Không có | ❌ **CẦN TẠO** |
| ❌ Slide thuyết trình | Không có | ❌ **CẦN TẠO** |
| ❌ Video demo 5-10 phút | Không có | ❌ **CẦN QUAY** |
| ✅ GitHub link | Có | ✅ **READY** |

**Status**: ⚠️ **40% - Thiếu lớn phần tài liệu**

---

## 🔍 SUMMARY: THIẾU GÌ - CẦN GÌ - THỪA GÌ

### **❌ THIẾU (URGENT - 必须做)**

**1. Admin Features** (High Priority)
   - [ ] **Dashboard** - Thống kê doanh thu, số đơn hàng, số khách, bestseller books
   - [ ] **CRUD Danh mục** - Tạo/sửa/xóa loại sản phẩm
   - [ ] **Quản lý Khuyến mãi** - Admin giao diện tạo/sửa/xóa coupon code
   - [ ] **Lock/Unlock User** - Admin khóa tài khoản người dùng
   - [ ] **Quản lý Đánh giá** - Duyệt/xóa comment, rating sản phẩm

**2. Frontend Features** (Medium Priority)
   - [ ] **Quick View Modal** - Xem nhanh sản phẩm không chuyển trang
   - [ ] **Remember Me** - Checkbox lưu phiên đăng nhập
   - [ ] **Toast Notifications** - UI đẹp cho feedback (không dùng alert())

**3. Backend Logic** (Medium Priority)
   - [ ] **Cảnh báo tồn kho** - Auto notify admin khi book < 5 items
   - [ ] **Coupon Validation** - Validate đơn tối thiểu, hạn sử dụng
   - [ ] **Rating/Review System** - Hệ thống đánh giá sản phẩm

**4. Documentation** (High Priority - Nộp Bài)
   - [ ] **Báo cáo PDF** - Formal documentation với mục lục, hình ảnh
   - [ ] **Use Case Diagram** - Visio/Lucidchart diagram cho từng actor
   - [ ] **Video Demo** - 5-10 phút demo toàn bộ features
   - [ ] **Slide Thuyết Trình** - PowerPoint 10-15 slides
   - [ ] **Database SQL Script** - Export script từ DataSeeder hoặc schema

---

### **⚠️ CẦN HOÀN THIỆN**

**1. Validation & Error Handling**
   - Coupon validation (đơn tối thiểu, hạn hạn sử dụng)
   - Form validation messages (toasts thay alert)
   - Error handling tốt hơn trong API

**2. Documentation**
   - Hướng dẫn chạy chi tiết (setup database, run app, test login)
   - README với hình ảnh screenshot
   - API documentation

**3. Tests**
   - Test files hiện tại đang bị comment out vì signature change
   - Cần update tests với @AuthenticationPrincipal

---

### **✅ THỪA / TỪ ĐỦ**

Không có gì thừa, project được thiết kế tối ưu. Có một vài điểm advanced nhưng hợp lý:

✅ **Good Design Decisions:**
- Real-time Notifications (SSE) - advanced nhưng useful
- Recommendation System (Association Rules) - advanced nhưng relevant
- Multi-seller architecture - phù hợp requirement
- JWT Security - enterprise-grade security
- Responsive Design (Tailwind) - modern UI/UX

---

## 📊 ĐỌC SỬ HOÀN THÀNH TỔNG THỂ

```
┌─────────────────────────────────────────────────────────┐
│          COMPLETION STATUS BY CATEGORY                 │
├─────────────────────────────────────────────────────────┤
│ User Features (A.1)        ████████████████████ 100%   │
│ Admin Features (A.2)       ████████░░░░░░░░░░░  60%    │
│ Backend Logic (B.1)        ███████░░░░░░░░░░░░  75%    │
│ UX/Frontend (B.2)          ██████████░░░░░░░░░  60%    │
│ Security (B.3)             ████████████████████ 100%   │
│ Documentation (C)          ████░░░░░░░░░░░░░░░  40%    │
├─────────────────────────────────────────────────────────┤
│ TỔNG CỘNG                  ████████░░░░░░░░░░░  76%    │
└─────────────────────────────────────────────────────────┘

CẦN HOÀN THÀNH: 24% = ~6-8 tuần làm việc
```

---

## 🎯 KHUYẾN NGHỊ ƯPRIORIT

### **TUẦN 1-2: URGENT (Nộp bài)**
1. ✅ Tạo Dashboard Admin (thống kê cơ bản)
2. ✅ Export Database SQL Script
3. ✅ Viết Báo cáo PDF + Use Case Diagram

### **TUẦN 3: IMPORTANT (Features)**
4. ✅ CRUD Danh mục
5. ✅ Admin Quản lý Khuyến mãi (tạo coupon)
6. ✅ Toast notifications UI

### **TUẦN 4-5: NICE-TO-HAVE**
7. ✅ Quick View Modal
8. ✅ Remember Me (Login)
9. ✅ Lock/Unlock User
10. ✅ Rating/Review System
11. ✅ Cảnh báo tồn kho

### **TUẦN 6: FINALIZE**
12. ✅ Video Demo + Slide Thuyết Trình
13. ✅ Test toàn bộ hệ thống
14. ✅ Fix bugs từ testing

---

## 📝 CHI TIẾT TỪNG CHỨC NĂNG CẦN THÊM

### **1. Dashboard Admin** ❌

**URL**: `/admin/dashboard` (PanelPageController)

**Cần hiển thị**:
- Total Revenue (tổng doanh thu theo tháng/năm)
- Total Orders (số đơn hàng, grouped by status)
- Active Sellers (số shop hoạt động)
- Top 5 Books (sách bán chạy nhất)
- Recent Orders (đơn hàng gần đây)

**DB Queries cần**:
```sql
SELECT SUM(total_amount) FROM orders WHERE MONTH(created_at) = MONTH(NOW());
SELECT COUNT(*) FROM orders WHERE MONTH(created_at) = MONTH(NOW());
SELECT b.* FROM books b ORDER BY (SELECT COUNT(*) FROM order_items oi WHERE oi.book_id = b.id) DESC LIMIT 5;
```

---

### **2. CRUD Danh mục** ❌

**Entities cần**:
```java
@Entity
public class Category {
    @Id @GeneratedValue
    private Long id;
    private String name;        // "Văn học", "Kinh tế", etc.
    private String description;
    private String imageUrl;    // category thumbnail
    private Integer displayOrder;
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "category")
    private List<Book> books;
}
```

**Controller methods**:
- `GET /api/admin/categories` - Danh sách
- `POST /api/admin/categories` - Tạo mới
- `PUT /api/admin/categories/{id}` - Cập nhật
- `DELETE /api/admin/categories/{id}` - Xóa

**Admin UI**: AdminCategoryController + Admin_Categories.html

---

### **3. Admin Quản lý Coupon** ❌

**Entities cần**:
```java
@Entity
public class Coupon {
    @Id @GeneratedValue
    private Long id;
    private String code;           // "BOOKOM15K"
    private String description;    
    private CouponType type;       // FIXED, PERCENT
    private Integer amount;        // 15000 or 10 (%)
    private Integer minOrderAmount; // 150000
    private LocalDateTime expiresAt;
    private Integer quantity;      // số lượng có hạn
    private Integer usedCount;
    private Boolean isActive = true;
}
```

**API endpoints**:
- `GET /api/admin/coupons` - Danh sách
- `POST /api/admin/coupons` - Tạo
- `PUT /api/admin/coupons/{id}` - Update
- `DELETE /api/admin/coupons/{id}` - Xóa
- `GET /api/coupons/{code}/validate` - Validate (for checkout)

---

### **4. Lock/Unlock User** ❌

**Update User entity**:
```java
@Entity
public class User {
    // ... existing fields
    private Boolean isLocked = false;  // new field
    private LocalDateTime lockedAt;
    private String lockReason;
}
```

**Admin Controller**:
```java
@PostMapping("/api/admin/users/{id}/lock")
public ResponseEntity<?> lockUser(@PathVariable Long id, @RequestBody LockReasonRequest req) {
    // Set user.isLocked = true, update DB
}

@PostMapping("/api/admin/users/{id}/unlock")
public ResponseEntity<?> unlockUser(@PathVariable Long id) {
    // Set user.isLocked = false
}
```

**Middleware check** in JwtAuthenticationFilter:
```java
if (user.getIsLocked()) {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account locked");
}
```

---

### **5. Rating/Review System** ❌

**Entities cần**:
```java
@Entity
public class BookReview {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne
    private Book book;
    
    @ManyToOne
    private User reviewer;
    
    private Integer rating;        // 1-5
    private String comment;        // <= 500 chars
    private LocalDateTime createdAt;
    private Boolean isApproved = false;  // admin duyệt
    private Integer helpfulCount = 0;
}
```

**API endpoints**:
- `POST /api/books/{bookId}/reviews` - Thêm review (must have bought)
- `GET /api/books/{bookId}/reviews` - Xem reviews
- `DELETE /api/admin/reviews/{id}` - Admin xóa
- `PUT /api/admin/reviews/{id}/approve` - Admin duyệt

---

### **6. Quick View Modal** ⚠️

**Current**: Discovery_Page chỉ có link `/book/{id}`

**Add**: Modal hiển thị:
```html
<div id="quickViewModal" class="hidden fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg max-w-2xl w-full p-6 flex gap-6">
        <!-- Ảnh bên trái -->
        <div class="w-1/3">
            <img id="qv-image" src="" alt="">
        </div>
        <!-- Info bên phải -->
        <div class="w-2/3">
            <h3 id="qv-title"></h3>
            <p id="qv-price"></p>
            <p id="qv-description"></p>
            <button onclick="addToCart(bookId)">Thêm vào giỏ</button>
            <button onclick="goToDetails(bookId)">Xem chi tiết</button>
            <button onclick="closeQuickView()">Đóng</button>
        </div>
    </div>
</div>
```

---

### **7. Remember Me** ⚠️

**Update Auth_Page.html**:
```html
<input type="checkbox" id="rememberMe" name="remember" value="true">
<label for="rememberMe">Ghi nhớ tôi trong 30 ngày</label>
```

**Frontend JS** (login-page.js):
```javascript
if (rememberMe.checked) {
    localStorage.setItem('savedEmail', email); // save email only (NOT password)
    // Set cookie with expiry 30 days
    document.cookie = `auth_email=${email}; max-age=${30*24*60*60}; path=/`;
}

// On page load, restore email
window.addEventListener('load', () => {
    const saved = localStorage.getItem('savedEmail');
    if (saved) emailInput.value = saved;
});
```

---

### **8. Toast Notifications** ⚠️

**Add library** (simple toast.js):
```html
<script src="https://cdnjs.cloudflare.com/ajax/libs/toastr.js/latest/toastr.min.js"></script>
<link href="https://cdnjs.cloudflare.com/ajax/libs/toastr.js/latest/toastr.min.css" rel="stylesheet">
```

**Usage**:
```javascript
// Replace alert()
toastr.success('Đã thêm vào giỏ hàng!', 'Thành công');
toastr.error('Mã khuyến mãi không hợp lệ!', 'Lỗi');
toastr.info('Thông tin cập nhật...', 'Thông báo');
```

---

### **9. Inventory Alert** ⚠️

**Add scheduled job** (InventoryAlertService):
```java
@Scheduled(fixedDelay = 3600000) // 1 hour
public void checkLowStockBooks() {
    List<Book> lowStockBooks = bookRepository.findByStockQuantityLessThan(5);
    for (Book book : lowStockBooks) {
        // Create admin notification
        notificationService.createForAdmin(
            "Low Stock Alert",
            book.getTitle() + " chỉ còn " + book.getStockQuantity() + " cuốn"
        );
    }
}
```

---

## 📋 CHECKLIST HOÀN THÀNH

```
ADMIN FEATURES:
☐ Dashboard (50% effort)
☐ CRUD Categories (30% effort)
☐ Coupon Management (40% effort)
☐ Lock/Unlock Users (20% effort)
☐ Rating/Review Management (35% effort)
  Subtotal: 175 effort points

FRONTEND:
☐ Quick View Modal (20% effort)
☐ Remember Me (10% effort)
☐ Toast Notifications (15% effort)
  Subtotal: 45 effort points

BACKEND:
☐ Inventory Alert Job (15% effort)
☐ Coupon Validation (20% effort)
☐ Rating/Review APIs (25% effort)
  Subtotal: 60 effort points

DOCUMENTATION:
☐ PDF Report (50% effort)
☐ Use Case Diagrams (30% effort)
☐ SQL Database Export (10% effort)
☐ Setup Guide (20% effort)
☐ Video Demo (60% effort)
☐ Presentation Slides (30% effort)
  Subtotal: 200 effort points

TESTING:
☐ Update Test Files (30% effort)
☐ Full System Testing (50% effort)
  Subtotal: 80 effort points

TOTAL: 560 effort points (~5-6 weeks for 1 developer)
```

---

## 🎓 KẾT LUẬN

**Project hiện tại đã rất tốt - 76% hoàn thành**, nhưng thiếu **những chức năng admin quan trọng** và **tài liệu nộp bài**. 

**Ưu tiên**:
1. **TUẦN 1**: Admin Dashboard + Database Script + PDF Report (để hoàn thành nộp bài)
2. **TUẦN 2-3**: CRUD danh mục + Coupon Management (core business features)
3. **TUẦN 4-5**: Lock User + Review System + UX improvements
4. **TUẦN 6**: Video Demo + Testing + Finalize

**Dự kiến nộp bài** trong **6-8 tuần** nếu follow roadmap này.

---

**Ghi chú**: Report này được tạo dựa trên phân tích static codebase. Đề nghị demo project với giáo viên để xác nhận requirements cụ thể.
