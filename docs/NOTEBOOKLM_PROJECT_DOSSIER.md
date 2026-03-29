# BOOKSTORE MULTI-VENDOR - NOTEBOOKLM PROJECT DOSSIER

Cap nhat: 2026-03-29
Muc tieu tai lieu: 1 file tong hop day du de dua vao NotebookLM cho viec hoc, bao cao do an, Q&A, va review tien do.

---

## 1) Executive Summary

Day la he thong Spring Boot duoc nang cap tu mo hinh ban hang don le sang mo hinh san thuong mai dien tu multi-vendor (nhieu Seller cung ban tren 1 san).

### Gia tri chinh da dat duoc
- Co cau role ro rang: BUYER, SELLER, ADMIN.
- Don hang da duoc tach theo Seller (Order tong + SubOrder).
- Da co nen tang du lieu cho Cart, CartItem, Order, SubOrder, OrderItem.
- Da co API checkout backend tach don theo seller.
- Da co migration script bang Flyway cho schema multi-vendor.
- Da co bo UC feasibility de danh gia kha nang trien khai theo tung nhom nguoi dung.

### Tinh trang tong quan
- Muc do san sang hien tai: backend foundation((n): nền móng)  da co, chua full end-to-end production.
- Danh gia nhanh: ~70% cho phase foundation.

---

## 2) Scope va Muc tieu He thong

## 2.1 Muc tieu nghiep vu
- Mot Buyer co the mua nhieu sach tu nhieu Seller trong cung 1 lan checkout.
- He thong tu dong tach 1 Order tong thanh nhieu SubOrder, moi SubOrder thuoc 1 Seller.
- Seller quan ly don cua rieng minh.
- Admin co the duyet noi dung sach va giam sat toan bo he thong.

## 2.2 Muc tieu ky thuat
- Chuan hoa domain model theo JPA(Jakarta Persistence): quản lý dữ liệu quan hệ (ORM - Object Relational Mapping).
- Dua migration schema vao version control.
- Tach API theo nghiep vu Buyer/Seller/Admin.
- Tien toi auth + authorization theo role.

---

## 3) Tech Stack

- Java 17
- Spring Boot 4.0.3
- Spring Web, Spring Data JPA, Thymeleaf
- SQL Server (runtime), co san mysql connector runtime
- Lombok
- BCrypt
- Flyway (da them de migration schema)

Build tool:
- Maven Wrapper (mvnw.cmd)

---

## 4) Project Structure (phan lien quan multi-vendor)

### 4.1 Model
- User
- Book
- Category
- Cart
- CartItem
- Order (orders_master)
- SubOrder
- OrderItem
- enums:
  - UserRole
  - ApprovalStatus
  - OrderStatus

### 4.2 Repository
- UserRepository
- BookRepository
- CartRepository
- CartItemRepository
- OrderRepository
- SubOrderRepository

### 4.3 Service
- AuthService
- OrderService

### 4.4 Controller
- AuthController
- BookController
- OrderController
- PanelController / PanelPageController / MainPageController

### 4.5 Data + Migration
- DataSeeder (tao user mau, seed sach)
- db/migration/V1__init_multivendor_schema.sql

---

## 5) Domain Model Chi tiet

## 5.1 Enums

### UserRole
- BUYER
- SELLER
- ADMIN

### ApprovalStatus
- PENDING
- APPROVED
- REJECTED

### OrderStatus
- PENDING_PAYMENT
- PROCESSING
- SHIPPING
- COMPLETED
- CANCELLED

## 5.2 Entity: User
Truong chinh:
- id
- username
- passwordHash
- role
- shopName
- shopAddress

Quan he:
- 1 User (Seller) co nhieu Book
- 1 User (Seller) co nhieu SubOrder
- 1 User (Buyer) co 1 Cart

## 5.3 Entity: Book
Truong chinh:
- id, title, author, description, price, stockQuantity, imageUrl, publisher, publishYear
- approvalStatus

Quan he:
- Book thuoc 1 Category
- Book thuoc 1 Seller (User)

## 5.4 Cart va CartItem
- Cart: moi Buyer co 1 gio
- CartItem: lien ket Cart - Book - quantity

## 5.5 Order model 2 tang

### Order (orders_master)
- Don tong cua Buyer
- Chua totalAmount, shippingAddress, createdAt
- Chua danh sach SubOrder

### SubOrder
- Moi SubOrder thuoc 1 Seller duy nhat
- Chua status va subTotal
- Chua danh sach OrderItem

### OrderItem
- Thuoc 1 SubOrder
- Chua Book, unitPrice tai thoi diem mua, quantity

---

## 6) Data Seeder Strategy hien tai

DataSeeder da duoc cap nhat theo huong multi-vendor:
- Xoa sach cu va user cu (trong profile seed hien tai)
- Tao:
  - admin (role ADMIN)
  - shop_nha_nam (role SELLER)
  - shop_tre (role SELLER)
- Nap 300 sach tu CSV
- Chia seller ngau nhien cho moi sach (nha nam / tre)
- Dat approvalStatus = APPROVED de xuat hien tren storefront ngay
- Co buoc tao mo ta AI cho mot tap sach

Luu y:
- Seeder hien tai phu hop demo/dev. Neu production can profile hoac flag de khong ghi de du lieu.

---

## 7) API Inventory (trong pham vi multi-vendor)

## 7.1 Auth
Base: /api/auth
- POST /register
- POST /login
- POST /login-jwt
  - Tra ve accessToken Bearer token

Trang thai:
- Co xu ly co ban.
- Da co endpoint cap token va bo loc JwtAuthenticationFilter o muc co ban.

## 7.2 Books
Base: /api/books
- GET /api/books?page=&size=
- POST /api/books
- GET /api/books/{id}
- PUT /api/books/{id}
- DELETE /api/books/{id}

Trang thai:
- CRUD co ban da co.
- Chua phan role runtime.
- Chua filter approved-only cho buyer.

## 7.3 Orders (moi)
Base: /api/orders
- POST /checkout
  - Input: buyerId, shippingAddress
  - Xu ly: group cart items theo seller -> tao Order + SubOrder + OrderItem
- POST /me/checkout
  - Input: shippingAddress
  - User context tam thoi qua header X-User-Id (cho den khi co JWT)
- GET /buyer/{buyerId}
- GET /me
  - User context tam thoi qua header X-User-Id
- GET /seller/{sellerId}/sub-orders
- PATCH /sub-orders/{subOrderId}/status?status=

Trang thai:
- Da co backend logic nen tang.
- Da co role guard runtime qua Security Filter cho carts/orders.

---

## 8) Checkout Split Flow (Core Logic)

Buoc xu ly trong OrderService:
1. Validate buyer ton tai.
2. Validate role cua user la BUYER.
3. Lay Cart cua buyer.
4. Validate cart khong rong.
5. Group CartItem theo seller cua Book.
6. Tao Order tong.
7. Moi seller:
   - Tao SubOrder
   - Tao danh sach OrderItem
   - Tinh subTotal
8. Gan danh sach SubOrder vao Order, tinh totalAmount tong.
9. Save Order (cascade save SubOrder + OrderItem).
10. Xoa cart items sau checkout thanh cong.
11. Tra ve CheckoutResponse.

Gia tri nghiep vu:
- Dam bao 1 checkout co the phat sinh nhieu don con theo tung seller.

---

## 9) Flyway Migration Strategy

File migration:
- V1__init_multivendor_schema.sql

Noi dung chinh:
- Bo sung cot role/shop_name/shop_address cho users.
- Bo sung seller_id/approval_status cho books.
- Tao bang carts, cart_items.
- Tao bang orders_master, sub_orders, order_items.
- Tao FK lien quan.

Config lien quan:
- spring.flyway.enabled=true
- spring.flyway.baseline-on-migrate=true
- spring.flyway.locations=classpath:db/migration

Canh bao van hanh:
- baseline-on-migrate can duoc quan tri chat tren DB da co du lieu.
- Nen chuan hoa quy trinh migration theo moi truong (local/staging/prod).

---

## 10) Use Case Mapping (Business)

## 10.1 Buyer UC
- B01 Dang ky/dang nhap/ho so: da co API profile co ban
- B02 Tim kiem/loc sach: da co endpoint search approved + filter
- B03 Xem chi tiet + mo ta AI: kha tot
- B04 Quan ly gio hang: da co API CRUD cart cho buyer
- B05 Dat hang + tach don seller: da co checkout API (legacy + user context)
- B06 Theo doi don ca nhan: da co endpoint theo user context (/api/orders/me)

## 10.2 Seller UC
- S01 Ho so shop: model da co, API chua day du
- S02 Dang ban sach cho duyet: model da co, flow role chua khoa
- S03 Quan ly kho: can ownership + hide/show
- S04 Xu ly don: da co endpoint status, can auth ownership
- S05 Dashboard doanh thu: co panel tong hop, chua sat du lieu don completed

## 10.3 Admin UC
- A01 Dashboard tong quan: co nen tang panel
- A02 Khoa/mo user: chua co field + API
- A03 Duyet sach: co status model, chua co moderation API hoan chinh
- A04 Xem toan bo don: can endpoint admin order listing

---

## 11) Gap Analysis (Nhung gi con thieu de len production)

## 11.1 Security
- Da them Spring Security + JwtAuthenticationFilter cho /api/carts/** va /api/orders/**.
- Ho tro Bearer token, dong thoi con fallback X-User-Id trong giai doan chuyen doi client.
- Da co ownership validation cho seller update suborder status.

## 11.2 API completeness
- Chua co AdminModerationController (approve/reject + reason).
- Chua co AdminUserController (lock/unlock).
- Chua co AdminOrderController (all orders + filters).
- Chua co SellerBookController ownership-safe.

## 11.3 Frontend integration
- Template dang o muc giao dien manh, nhung chua bind full API moi.
- Checkout UI chua submit dung payload checkout backend theo session user.

## 11.4 Testing
- Da co test controller-level cho B04/B05/B06 (CartControllerTest, OrderControllerTest).
- Da co test transition + ownership cho seller status update (OrderServiceSecurityAndOwnershipTest).
- Chua co migration smoke test tren DB moi.

---

## 12) Roadmap de hoan thien

## Phase 1 - Security First (uu tien cao nhat)
- Them Spring Security + JWT.
- Inject role va user context vao request.
- Khoa endpoint theo BUYER/SELLER/ADMIN.

## Phase 2 - Buyer end-to-end
- Cart API CRUD.
- Checkout API voi user context (khong truyen buyerId tu client sau khi co JWT).
- Buyer order history + order detail.

## Phase 3 - Seller operations
- Seller profile update API.
- Seller book create/update/hide/show with ownership.
- Seller suborder status transition policy.

## Phase 4 - Admin operations
- Duyet sach pending.
- Khoa/mo user.
- Xem all orders + search/filter.

## Phase 5 - Quality gate
- Integration test + API contract test.
- Data migration rehearsal.
- Perf va logging baseline.

---

## 12.1) UC Implementation Log (da lam trong sprint hien tai)

Muc tieu user yeu cau: "lam UC dau tien va cap nhat lien tuc vao NotebookLM".

Trang thai cap nhat den hien tai:
- B01 (Dang ky/dang nhap/ho so): DA NANG CAP THEM profile API.
- B02 (Tim kiem/loc sach): DA THEM endpoint search approved-only + filter.
- B04 (Quan ly gio hang): DA THEM CartController + CartService day du cac thao tac chinh.
- B05 (Dat hang + tach don seller): DA THEM checkout theo user context (khong can buyerId trong body).
- B06 (Theo doi don ca nhan): DA THEM endpoint /api/orders/me theo user context.
- Security: DA NANG CAP sang Spring Security + JwtAuthenticationFilter (co fallback X-User-Id de chuyen doi client).
- Seller ownership: DA THEM validate seller chi duoc update sub-order cua chinh minh.
- Test: DA THEM controller/service tests cho B04/B05/B06 va seller status transition.

Chi tiet ky thuat vua implement:
- B01:
  - GET /api/auth/profile/{userId}
  - PUT /api/auth/profile/{userId}
  - Logic o AuthService: kiem tra user ton tai, tranh username trung, cap nhat shop info cho role SELLER/ADMIN.
- B02:
  - GET /api/books/search?q=&categoryId=&page=&size=
  - Chi tra sach co approvalStatus = APPROVED.
  - Ho tro tim theo title/author va loc category.
- B04:
  - GET /api/carts/buyer/{buyerId}
  - POST /api/carts/buyer/{buyerId}/items
  - PATCH /api/carts/buyer/{buyerId}/items/{itemId}?quantity=
  - DELETE /api/carts/buyer/{buyerId}/items/{itemId}
  - Rule quan trong: chi BUYER duoc thao tac cart; chi duoc them sach APPROVED; so luong khong vuot ton kho.
- B05:
  - POST /api/orders/checkout (legacy: buyerId + shippingAddress)
  - POST /api/orders/me/checkout (moi: shippingAddress + header X-User-Id)
  - Refactor OrderService dung 1 core flow checkoutInternal.
  - Them validate tai checkout: sach APPROVED va quantity khong vuot stock.
- B06:
  - GET /api/orders/me (header X-User-Id)
  - Service validate user ton tai + role BUYER truoc khi tra lich su don.
- Security tam thoi:
  - SecurityConfig + JwtAuthenticationFilter + JwtTokenProvider
  - Khoa role BUYER/SELLER theo endpoint pattern cua /api/carts/** va /api/orders/**
  - Chan truy cap cheo userId tren route buyer/seller co path id.
  - Co endpoint /api/auth/login-jwt de cap Bearer token.
- Seller ownership:
  - Endpoint update status bat buoc header X-User-Id (seller caller)
  - Service check sellerId trong token/header phai trung seller cua sub-order.
- Testing:
  - CartControllerTest (B04)
  - OrderControllerTest (B05/B06)
  - OrderServiceSecurityAndOwnershipTest (B06 role check + seller ownership transition)

Kiem chung build:
- Da chay compile Maven thanh cong sau khi them UC B01/B02/B04/B05.
- Da chay test muc tieu cho B04/B05/B06 + ownership transition thanh cong.

## 12.2) UC Flows (chi tiet de NotebookLM Q&A)

### Flow B01 - Quan ly ho so nguoi dung
1. Client gui userId vao endpoint profile.
2. Service tim user theo id, neu khong co -> 404.
3. Tra profile (id, username, role, shopName, shopAddress).
4. Khi update:
  - Neu co username moi thi check trung username.
  - Neu role la SELLER/ADMIN thi cho cap nhat thong tin shop.
5. Luu DB va tra profile moi.

Ghi chu nghiep vu:
- BUYER van co the doi username.
- Shop metadata chi co y nghia cho SELLER/ADMIN.

### Flow B02 - Tim kiem va loc sach cho Buyer
1. Client goi /api/books/search voi q/category/paging.
2. Controller chuan hoa input (q rong -> null).
3. Repository query voi dieu kien:
  - approvalStatus = APPROVED (bat buoc)
  - keyword match title/author (neu co)
  - category filter (neu co)
4. Tra Page<Book> cho frontend phan trang.

Ghi chu nghiep vu:
- Endpoint nay phuc vu storefront, khong show sach pending/rejected.

### Flow B04 - Quan ly gio hang
1. Buyer mo gio hang:
  - Neu chua co cart thi tao cart moi.
2. Buyer them item:
  - Kiem tra buyer role.
  - Kiem tra book ton tai + APPROVED + con hang.
  - Neu item da co trong gio -> cong don so luong.
  - Kiem tra tong so luong khong vuot ton kho.
3. Buyer doi so luong:
  - Kiem tra item thuoc cart cua buyer.
  - Kiem tra quantity > 0 va <= stock.
4. Buyer xoa item:
  - Kiem tra ownership cart item roi xoa.
5. Moi thao tac deu tra CartResponse da tinh:
  - totalItems
  - totalAmount
  - thong tin seller cho tung dong hang

Ghi chu nghiep vu:
- Cac check ownership/cart scope giup tranh sua du lieu cua nguoi khac.

### Flow B05 - Checkout tach don theo seller
1. Buyer goi checkout:
  - Cach cu: POST /api/orders/checkout voi buyerId.
  - Cach moi: POST /api/orders/me/checkout voi header X-User-Id.
2. Service xac thuc buyer ton tai va role BUYER.
3. Lay cart cua buyer va kiem tra cart khong rong.
4. Group cart items theo seller.
5. Validate tung item truoc khi dat:
  - quantity hop le (>0)
  - sach dang APPROVED
  - quantity khong vuot stock hien tai
6. Tao Order tong + tao SubOrder cho moi seller + tao OrderItem.
7. Tinh tong tien theo subTotal cua cac sub-order.
8. Luu order (cascade), xoa cart items sau checkout thanh cong.
9. Tra CheckoutResponse (orderId, totalAmount, subOrderCount).

Ghi chu nghiep vu:
- Endpoint /me/checkout la buoc chuyen tiep de frontend quen voi user context, truoc khi thay bang JWT chinh thuc.

### Flow B06 - Theo doi don ca nhan theo user context
1. Buyer goi GET /api/orders/me voi header X-User-Id.
2. Interceptor validate:
  - JwtAuthenticationFilter validate Bearer token (hoac fallback header trong giai doan chuyen doi).
  - User ton tai trong DB.
  - Role cua user la BUYER cho nhom endpoint buyer.
3. Service getCurrentBuyerOrders -> getBuyerOrders.
4. Service kiem tra user ton tai + role BUYER.
5. Tra danh sach orders sap xep moi nhat truoc.

Ghi chu nghiep vu:
- Luong nay giam phu thuoc buyerId do client truyen tren URL va la buoc dem truoc JWT.

### Flow Seller status transition co ownership guard
1. Seller goi PATCH /api/orders/sub-orders/{subOrderId}/status?status=... va gui danh tinh.
2. JwtAuthenticationFilter xac thuc danh tinh + role SELLER.
3. Service tim sub-order va doi chieu seller caller voi seller so huu sub-order.
4. Neu khong dung owner -> tra 403.
5. Neu dung owner -> cap nhat status va tra SubOrderSummaryResponse.

## 13) Demo Script de bao cao do an

Kich ban de demo trong 10-15 phut:
1. Chay app + seed data.
2. Show buyer browse sach.
3. Cho vao cart 2 sach cua 2 seller khac nhau.
4. Goi checkout API.
5. Show ket qua:
   - 1 Order tong
   - 2 SubOrder tuong ung 2 seller
6. Seller A update status suborder.
7. Buyer xem lai don va trang thai.
8. Admin xem dashboard tong quan (hien trang).

---

## 14) Risk Register

- R1: Chua co auth role runtime -> nguy co truy cap trai phep endpoint.
- R2: baseline flyway tren DB co du lieu -> co the skip migration khong mong muon.
- R3: Seeder ghi de du lieu -> anh huong moi truong test chia se.
- R4: Frontend va backend chua dong bo payload/auth.
- R5: Chua co test E2E -> de loi khi doi schema.

Giam thieu:
- Uu tien security phase 1.
- Chuan hoa migration process.
- Tach profile seeder dev/prod.
- Viet integration tests truoc khi mo rong them.

---

## 15) Prompt mau cho NotebookLM

Ban co the copy cac cau sau vao NotebookLM:

1. "Tom tat kien truc multi-vendor cua du an nay bang 10 gach dau dong, tap trung vao luong checkout split order."
2. "Giai thich su khac nhau giua Order va SubOrder trong project nay va vi sao can tach."
3. "Liet ke cac endpoint hien co cho buyer/seller/admin va endpoint nao con thieu de dat production-ready."
4. "Tao checklist test integration cho checkout multi-seller dua tren tai lieu nay."
5. "Danh gia rui ro migration Flyway voi SQL Server va de xuat quy trinh rollback an toan."
6. "Viet plan 2 sprint de hoan thien cac UC BUYER B01-B06."
7. "Viet plan 2 sprint de hoan thien UC SELLER va ADMIN con thieu."

---

## 16) Appendix - Danh sach file quan trong

Core backend:
- src/main/java/com/example/bookstore/model/User.java
- src/main/java/com/example/bookstore/model/Book.java
- src/main/java/com/example/bookstore/model/Cart.java
- src/main/java/com/example/bookstore/model/CartItem.java
- src/main/java/com/example/bookstore/model/Order.java
- src/main/java/com/example/bookstore/model/SubOrder.java
- src/main/java/com/example/bookstore/model/OrderItem.java

Enums:
- src/main/java/com/example/bookstore/model/enums/UserRole.java
- src/main/java/com/example/bookstore/model/enums/ApprovalStatus.java
- src/main/java/com/example/bookstore/model/enums/OrderStatus.java

API/Service:
- src/main/java/com/example/bookstore/controller/OrderController.java
- src/main/java/com/example/bookstore/service/OrderService.java

Migration:
- src/main/resources/db/migration/V1__init_multivendor_schema.sql

Feasibility matrix:
- docs/UC_MULTIVENDOR_FEASIBILITY.md

---

## 17) Ket luan

Project da dat duoc buoc nhay quan trong tu single-seller sang multi-vendor o tang du lieu va nghiep vu checkout.
De dat muc san sang trien khai that, can uu tien security role-based, bo sung API con thieu theo UC, va dong bo frontend + testing.

Tai lieu nay duoc viet de dung truc tiep trong NotebookLM cho cac tac vu:
- Q&A ky thuat
- Tong hop bao cao
- Lap ke hoach sprint
- Chuan bi demo va bao ve do an
