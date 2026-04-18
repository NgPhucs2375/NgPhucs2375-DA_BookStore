đ# BOOKSTORE MULTI-VENDOR - NOTEBOOKLM PROJECT DOSSIER

Cập nhật: 2026-04-15
Mục tiêu tài liệu: tệp tổng hợp để dùng trong NotebookLM cho Q&A kỹ thuật, báo cáo đồ án và theo dõi tiến độ.

---

## 1) Tóm tắt điều hành

Đây là hệ thống Spring Boot đã chuyển từ mô hình bán lẻ đơn người bán sang mô hình multi-vendor.

Giá trị đã đạt được:
- Phân vai rõ ràng: BUYER, SELLER, ADMIN.
- Đã có mô hình đơn hàng 2 tầng: Order tổng + SubOrder theo seller.
- Đã có dữ liệu và nghiệp vụ cho Cart, Checkout, Order, OrderItem.
- Đã có Flyway migration cho schema multi-vendor.
- Đã có validation đầu vào cho auth (regex, độ dài), OTP qua SMTP.
- Đã lưu avatar + thể loại yêu thích xuống DB.
- Đã bind frontend cho các luồng quan trọng: index/discovery/details, cart, checkout, order success, order details.

Đánh giá hiện tại:
- Backend foundation: tốt.
- Buyer flow chính (cart -> checkout -> order pages): chạy end-to-end ở mức nghiệp vụ.
- Mức sẵn sàng tương đối: ~86% cho foundation + buyer storefront`/order flow.

---

## 2) Mục tiêu hệ thống

Mục tiêu nghiệp vụ:
- Buyer mua nhiều sách từ nhiều seller trong một lần checkout.
- Hệ thống tự tách đơn thành các SubOrder theo seller.
- Seller xử lý đơn của chính mình.
- Admin giám sát và kiểm duyệt nội dung.

Mục tiêu kỹ thuật:
- Chuẩn hóa domain model theo JPA.
- Quản lý schema bằng Flyway.
- Tách API theo vai trò và nghiệp vụ.
- Tiến tới authorization đầy đủ cho toàn hệ thống.

---

## 3) Tech Stack

- Java 17
- Spring Boot 4.0.3
- Spring Web, Spring Data JPA, Thymeleaf
- Spring Security + JWT filter
- Spring Validation
- Spring Mail (SMTP)
- SQL Server runtime (có MySQL connector runtime)
- Lombok, BCrypt, Flyway
- Maven Wrapper (`mvnw.cmd`)

---

## 4) Cấu trúc chính của dự án

Model:
- User, Book, Category, Cart, CartItem, Order, SubOrder, OrderItem
- Enums: UserRole, ApprovalStatus, OrderStatus

Repository:
- UserRepository, BookRepository, CartRepository, CartItemRepository, OrderRepository, SubOrderRepository

Service:
- AuthService, AuthOtpService, MailService, CartService, OrderService

Controller:
- AuthController, BookController, CartController, OrderController, MainPageController

Data/Migration:
- DataSeeder
- `src/main/resources/db/migration/V1__init_multivendor_schema.sql`

---

## 5) Domain model nổi bật

User:
- Có role, shop info, avatarUrl
- Quan hệ favoriteCategories (ManyToMany)

Book:
- Có approvalStatus
- Thuộc seller và category

Order 2 tầng:
- Order tổng: buyer, shippingAddress, totalAmount, createdAt
- SubOrder: seller, status, subTotal
- OrderItem: book, unitPrice, quantity

---

## 6) Trạng thái seed dữ liệu

DataSeeder hiện:
- Xóa dữ liệu user/book rồi seed lại
- Tạo admin + 2 seller mẫu
- Nạp ~300 sách từ CSV
- Chia seller ngẫu nhiên
- Set sách APPROVED để lên storefront

Lưu ý:
- Phù hợp demo/dev
- Không phù hợp production nếu cần giữ dữ liệu thật

---

## 7) API Inventory

Auth (`/api/auth`):
- `POST /register`
- `POST /login`
- `POST /login-jwt`
- `POST /otp/request`
- `POST /otp/verify`
- `GET /profile/{userId}`
- `PUT /profile/{userId}`

Cart (`/api/carts`):
- `GET /buyer/{buyerId}`
- `POST /buyer/{buyerId}/items`
- `PATCH /buyer/{buyerId}/items/{itemId}?quantity=`
- `DELETE /buyer/{buyerId}/items/{itemId}`

Orders (`/api/orders`):
- `POST /checkout` (legacy)
- `POST /me/checkout`
- `GET /buyer/{buyerId}`
- `GET /me`
- `GET /me/{orderId}`
- `GET /seller/{sellerId}/sub-orders`
- `PATCH /sub-orders/{subOrderId}/status?status=`

---

## 8) Luồng Checkout tách đơn

OrderService xử lý:
1. Validate buyer tồn tại và đúng role BUYER.
2. Lấy cart, kiểm tra không rỗng.
3. Group cart items theo seller.
4. Tạo Order tổng, rồi tạo SubOrder và OrderItem cho từng seller.
5. Tính tổng tiền, lưu cascade.
6. Xóa cart items sau khi checkout thành công.
7. Trả `CheckoutResponse`.

---

## 9) Flyway migration

Đang dùng:
- `spring.flyway.enabled=true`
- `spring.flyway.baseline-on-migrate=true`
- `spring.flyway.locations=classpath:db/migration`

Migration chính:
- `V1__init_multivendor_schema.sql`

---

## 10) Mapping Use Case

Buyer:
- B01 Auth/profile: đã có, kèm OTP SMTP
- B02 Search/filter: đã có
- B03 Details page: đã có
- B04 Cart management: đã có API + UI bind
- B05 Checkout split order: đã có API + UI bind
- B06 Order tracking: đã có API + success/details bind

Seller:
- Có nền tảng xử lý sub-order status + ownership guard
- Cần hoàn thiện thêm dashboard/ownership cho book operations

Admin:
- Có nền tảng dashboard
- Cần thêm moderation, lock/unlock user, all-orders view đầy đủ

---

## 11) Gap Analysis

Cần ưu tiên:
- Mở rộng security coverage cho toàn bộ endpoint (không chỉ carts/orders)
- Hoàn thiện API admin/seller còn thiếu
- Bổ sung E2E test tự động cho toàn luồng UI
- Chuẩn hóa quy trình migration theo môi trường

---

## 12) Roadmap đề xuất

1. Security hardening toàn tuyến
2. Hoàn thiện buyer experience
3. Hoàn thiện seller ownership-safe operations
4. Hoàn thiện admin moderation & user management
5. Quality gate: integration + E2E + migration rehearsal

---

## 13) Delta mới nhất (2026-04-15)

- Thêm OTP qua SMTP cho auth
- Register yêu cầu email đã verify OTP
- Lưu avatar + favorite categories xuống DB
- Bind Cart_Page với API cart (list/update/remove/recalc)
- Bind Checkout_Page với API cart và `/api/orders/me/checkout`
- Thêm API `GET /api/orders/me/{orderId}`
- Bind Order_Success và Order_Details theo orderId
- Cập nhật tài liệu UC feasibility (nhánh `origin/Scu` đã hợp nhất)
- Admin: chuẩn hóa layout/head theo `pageTitle`, thêm loader + nút refresh, toast thông báo, và xử lý auth header cho API admin
- Admin: giao diện sidebar TailAdmin có thu gọn/mở rộng, theme CSS riêng
- Compile và regression tests hiện có đều pass

---

## 14) File quan trọng

- `src/main/java/com/example/bookstore/controller/AuthController.java`
- `src/main/java/com/example/bookstore/controller/CartController.java`
- `src/main/java/com/example/bookstore/controller/OrderController.java`
- `src/main/java/com/example/bookstore/service/AuthService.java`
- `src/main/java/com/example/bookstore/service/AuthOtpService.java`
- `src/main/java/com/example/bookstore/service/MailService.java`
- `src/main/java/com/example/bookstore/service/CartService.java`
- `src/main/java/com/example/bookstore/service/OrderService.java`
- `src/main/resources/db/migration/V1__init_multivendor_schema.sql`
- `docs/UC_MULTIVENDOR_FEASIBILITY.md`

---

## 15) Kết luận

Dự án đã hoàn thành bước chuyển đổi quan trọng sang multi-vendor ở tầng dữ liệu và nghiệp vụ cốt lõi. Luồng buyer chính đã vận hành được end-to-end cho mục tiêu demo kỹ thuật. Để production-ready, cần tăng coverage security, hoàn thiện seller/admin UC, và đẩy mạnh test tự động.
