# 📚 COMPLETE CODEBASE OVERVIEW - BOOKOM BOOKSTORE

## PROJECT STRUCTURE & ARCHITECTURE

```
BOOKOM (Multi-Vendor Bookstore)
├── Backend: Spring Boot 3.x + Spring Data JPA
├── Database: MySQL / MSSQL with Flyway migrations
├── Frontend: Thymeleaf + Tailwind CSS + Vanilla JS
└── Authentication: JWT Token-based (Spring Security)

Roles: BUYER, SELLER, ADMIN
```

---

## 📂 EXISTING CODEBASE ANALYSIS

### **1. MODEL LAYER** (`src/main/java/com/example/bookstore/model/`)

#### **User.java** ✅ Exists
```java
- id: Long (PK)
- username: String (unique)
- passwordHash: String
- role: UserRole (BUYER, SELLER, ADMIN)
- shopName: String (for sellers)
- shopAddress: String
- avatarUrl: String (Lob)
- favoriteCategories: Set<Category> (ManyToMany)
- books: List<Book> (OneToMany - seller's books)
- subOrders: List<SubOrder> (OneToMany - seller's orders)
- cart: Cart (OneToOne - for buyers)
- isActive: boolean = true (✅ Added)
```

#### **Book.java** ✅ Exists
```java
- id: Long (PK)
- title: String (not null)
- author: String
- description: String (NVARCHAR(MAX))
- price: Double
- stockQuantity: Integer
- imageUrl: String
- publisher: String
- publishYear: String
- category: Category (ManyToOne)
- seller: User (ManyToOne, not null)
- approvalStatus: ApprovalStatus (PENDING, APPROVED, REJECTED)
- isActive: boolean = true (✅ Added)
```

#### **Order.java** ✅ Exists
```java
- id: Long (PK)
- buyer: User (ManyToOne, not null)
- totalAmount: Double
- shippingAddress: String
- createdAt: LocalDateTime
- subOrders: List<SubOrder> (OneToMany)
```

#### **SubOrder.java** ✅ Exists
```java
- id: Long (PK)
- parentOrder: Order (ManyToOne)
- seller: User (ManyToOne)
- status: OrderStatus (enum)
- subTotal: Double
- items: List<OrderItem> (OneToMany)
```

#### **OrderItem.java** ✅ Exists
```java
- id: Long (PK)
- subOrder: SubOrder (ManyToOne)
- book: Book (ManyToOne)
- price: Double
- quantity: Integer
```

#### **Category.java** ✅ Exists
```java
- id: Long (PK)
- name: String
- description: String
```

#### **Cart.java & CartItem.java** ✅ Exists
```java
- Cart: id, buyer (OneToOne), items (OneToMany)
- CartItem: id, cart, book, quantity
```

#### **SellerShop.java** ✅ Exists (Not much used currently)

#### **Enums** (`src/main/java/com/example/bookstore/model/enums/`)
- `UserRole.java` → BUYER, SELLER, ADMIN ✅
- `ApprovalStatus.java` → PENDING, APPROVED, REJECTED ✅
- `OrderStatus.java` → PENDING_PAYMENT, PROCESSING, SHIPPED, COMPLETED, CANCELLED ✅

---

### **2. REPOSITORY LAYER** (`src/main/java/com/example/bookstore/repository/`)

#### **UserRepository.java** ✅ Exists
```java
✅ EXISTING METHODS:
   - findByUsername(String username): User
   - existsByUsername(String username): boolean
   - findAllByRole(UserRole role): List<User>

🆕 NEED TO ADD:
   - findByRoleAndIsActive(UserRole role, boolean isActive): List<User>
   - findByIsActive(boolean isActive): List<User>
   - findByUsernameContainingIgnoreCase(String username): List<User>
```

#### **BookRepository.java** ✅ Exists
```java
✅ EXISTING METHODS:
   - findByTitleContaining(String title): List<Book>
   - findBySeller(User seller): List<Book>
   - findByApprovalStatus(ApprovalStatus, Pageable): Page<Book>
   - findByApprovalStatus(ApprovalStatus): List<Book>
   - searchApprovedBooks(@Query): Page<Book>

🆕 NEED TO ADD:
   - findByIsActive(boolean isActive): List<Book>
   - findByIsActive(boolean isActive, Pageable): Page<Book>
   - findByApprovalStatusAndIsActive(ApprovalStatus, boolean, Pageable): Page<Book>
```

#### **OrderRepository.java** ✅ Exists
```java
✅ EXISTING METHODS:
   - findByBuyer(User buyer): List<Order>
   - findByBuyerOrderByCreatedAtDesc(User buyer): List<Order>

🆕 NEED TO ADD:
   - findAll(Pageable): Page<Order>
   - findByCreatedAtBetween(LocalDateTime, LocalDateTime, Pageable): Page<Order>
   - @Query searchOrders(String keyword, Pageable): Page<Order>
```

#### **SubOrderRepository.java** ✅ Exists
- Standard JpaRepository methods

#### **CategoryRepository.java** ✅ Exists
- Standard JpaRepository methods

#### **CartRepository.java & CartItemRepository.java** ✅ Exist

---

### **3. SERVICE LAYER** (`src/main/java/com/example/bookstore/service/`)

#### **BookService.java** ✅ Exists (Partial)
```java
✅ EXISTING METHODS:
   - getAllBook(): List<Book>
   - addBook(Book): Book
   - getBookbyId(Long id): Book
   - deleteBook(Long id): void
   - updateBook(Long id, Book): Book
   - getPendingBooksForAdmin(int page, int size): Page<Book>
   - changeBookApprovalStatus(Long bookId, ApprovalStatus): Book
   - addBookForSeller(Book, Long sellerId): Book
   - updateBookForSeller(Long bookId, Book, Long sellerId): Book

🆕 NEED TO ADD:
   - deleteBook(Long bookId): void (admin version with validation)
   - updateBookByAdmin(Long bookId, BookUpdateDto): Book
   - lockBook(Long bookId): Book
   - unlockBook(Long bookId): Book
```

#### **OrderService.java** ✅ Exists (Partial)
```java
✅ EXISTING METHODS:
   - checkoutFromCart(CheckoutRequest): CheckoutResponse
   - checkoutFromCurrentBuyer(Long buyerId, String address): CheckoutResponse
   - [internal checkout logic with SubOrder creation]

🆕 NEED TO ADD:
   - getOrdersWithFilters(int page, int size, String q, String status, LocalDate from, LocalDate to): Page<Order>
   - getOrderDetailsForAdmin(Long orderId): OrderDetailResponse
   - getAllOrders(Pageable): Page<Order>
```

#### **AuthService.java** ✅ Exists
- register(), login(), validateToken(), etc.

#### **AuthOtpService.java** ✅ Exists
- OTP request/verification

#### **CategoryService.java** ✅ Exists

#### **CartService.java** ✅ Exists

#### **MailService.java** ✅ Exists

#### **UserService.java** ✅ Exists
```java
✅ EXISTING METHODS:
   - getUserById(Long id): User
   - getUsersByRole(UserRole role): List<User>
   - lockUser(Long userId): User
   - unlockUser(Long userId): User
   - getAllUsers(): List<User>
```

---

### **4. CONTROLLER LAYER** (`src/main/java/com/example/bookstore/controller/`)

#### **AdminBookController.java** ✅ Exists
```
✅ EXISTING ENDPOINTS:
   GET /api/admin/books/pending
   PUT /api/admin/books/{id}/status
   DELETE /api/admin/books/{id}
   PUT /api/admin/books/{id}
   PUT /api/admin/books/{id}/lock
   PUT /api/admin/books/{id}/unlock
```

#### **AdminUserController.java** ✅ Exists
```java
✅ EXISTING ENDPOINTS:
   PUT /api/admin/users/{id}/lock
   PUT /api/admin/users/{id}/unlock
   GET /api/admin/users/{id}
```

#### **AdminOrderController.java** ✅ Exists
```java
✅ EXISTING ENDPOINTS:
   GET /api/admin/orders
   GET /api/admin/orders/{id}
```

#### **PanelController.java** ✅ Exists (REST API for data)
```
✅ EXISTING ENDPOINTS:
   GET /api/panel/summary (dashboard metrics)
   GET /api/panel/books (filtered books)
   GET /api/panel/users (filtered users)
   GET /api/panel/shops (shop data)
   GET /api/panel/seller/analytics
   GET /api/panel/seller/orders
   [... many other seller endpoints]

🆕 NEED TO EXTEND:
   GET /api/panel/users-detailed (with is_active filter)
   GET /api/panel/books-all (with is_active + approval_status)
   GET /api/panel/orders (all orders for admin)
   GET /api/panel/orders/{id} (order details)
```

#### **PanelPageController.java** ✅ Exists
```
✅ EXISTING ROUTES:
   GET /admin → Admin Dashboard
   GET /admin/users → Admin Users Page
   GET /admin/books → Admin Books Review
   GET /admin/shops → Admin Shops Page
   GET /seller/dashboard → Seller Dashboard
   [... many other routes]

🆕 NEED TO ADD ROUTE:
   GET /admin/orders → Admin Orders Page
```

#### **BookController.java** ✅ Exists (Public API)
- Add/search/filter books (buyer view)

#### **OrderController.java** ✅ Exists
- Create order, get order history (buyer/seller view)

#### **AuthController.java** ✅ Exists
- Register, login, OTP verification

#### **AuthPageController.java** ✅ Exists
- Auth page routing

#### **CartController.java** ✅ Exists
- Add/remove items from cart

#### **CategoryController.java** ✅ Exists
- Get categories

#### **SellerShopController.java** ✅ Exists
- Seller shop management

#### **MainPageController.java** ✅ Exists
- Main page routing

#### **GlobalValidationExceptionHandler.java** ✅ Exists
- Global exception handling

#### **LegacyRouteController.java** ✅ Exists
- Route mapping

---

### **5. FRONTEND - TEMPLATES** (`src/main/resources/templates/`)

#### **Admin Pages** ✅ Mostly Exist
```
admin/Admin.html                    ✅ Dashboard
admin/Admin_Books.html              ✅ Book Review (need to extend)
admin/Admin_Users.html              ✅ User Listing (need to extend)
admin/Admin_Shops.html              ✅ Shop Listing
admin/Admin_Orders.html             ❌ NEED TO CREATE

fragments/admin_layout.html         ✅ Sidebar & Header layout
fragments/panel_head.html           ✅ Meta tags & head
```

#### **Seller Pages** ✅ Exist
```
seller/Seller_Dashboard.html
seller/Seller_Orders.html
seller/Seller_Analytics.html
seller/Seller_Product_Detail.html
seller/Inventory_Management.html
seller/Shop_Seller.html
```

#### **Main Pages** ✅ Exist
```
main/Auth_Page.html
main/Cart_Page.html
main/Checkout_Page.html
main/Details_Produce.html
main/Discovery_Page.html
main/Flash_Sale.html
main/Order_Details.html
main/Order_Success.html
main/Search_Result.html
main/Contact_us.html
main/index.html
```

---

### **6. FRONTEND - JAVASCRIPT** (`src/main/resources/static/js/`)

#### **panel-data.js** ✅ Exists (Main admin data handler)
```javascript
✅ EXISTING FUNCTIONS:
   - initAdminDashboard()
   - initAdminBooks()
   - initAdminUsers()
   - initAdminShops()
   - initSellerDashboard()
   - initSellerOrders()
   - [utility functions: getJson, setText, setHtml, chart, etc]

🆕 NEED TO ADD FUNCTIONS:
   - initAdminOrders()
   - initAdminUsersLock() (for lock/unlock UI)
   - initAdminBooksExtended() (for delete/update/lock UI)
   - viewOrderDetail(orderId)
   - lockUser(userId)
   - unlockUser(userId)
   - deleteBook(bookId)
   - updateBook(bookId)
```

#### **api-service.js** ✅ Exists
- HTTP utilities (getJson, formatVND, getHeaders, etc)
- Comprehensive API service module with Auth, Books, Cart, Orders, etc.

#### **admin-shell.js** ❌ Deprecated (Moved to Thymeleaf fragments)

#### **seller-dashboard-integration.js** ✅ Exists
#### **shop-seller-integration.js** ✅ Exists
#### **order-details-integration.js** ✅ Exists
#### **inventory-management-integration.js** ✅ Exists
#### **buyer-dashboard-integration.js** ✅ Exists

---

### **7. SECURITY** (`src/main/java/com/example/bookstore/security/`)

#### **JwtTokenProvider.java** ✅ Exists
- JWT token generation, validation, extraction
- Role-based access control

---

### **8. DTO LAYER** (`src/main/java/com/example/bookstore/dto/`)

#### **Existing DTOs** ✅
```java
✅ EXISTING:
   - AuthLoginRequest.java
   - AuthRegisterRequest.java
   - EmailOtpRequest.java
   - EmailOtpVerifyRequest.java
   - UserProfileResponse.java
   - UserProfileUpdateRequest.java
   - CheckoutRequest.java
   - CheckoutResponse.java
   - OrderDetailResponse.java
   - OrderItemDetailResponse.java
   - SubOrderSummaryResponse.java

🆕 NEED TO CREATE:
   - BookUpdateDto.java
```

---

### **9. DATABASE** (`src/main/resources/db/migration/`)

#### **Existing Migrations** ✅
```sql
V1__init_multivendor_schema.sql
   - users table
   - books table
   - categories table
   - user_favorite_categories table
   - carts table
   - cart_items table
   - orders_master table
   - sub_orders table
   - order_items table
   - seller_shops table

V2__create_seller_shop.sql
   - Additional seller shop structure

🆕 NEED TO CREATE:
   V3__add_isactive_fields.sql
   - ALTER TABLE users ADD COLUMN is_active BIT DEFAULT 1
   - ALTER TABLE books ADD COLUMN is_active BIT DEFAULT 1
   - CREATE TABLE admin_audit_log (optional)
```

---

### **10. CONFIGURATION FILES** ✅

- **pom.xml** - Maven dependencies
- **application.properties** - Spring Boot config
- **docker-compose.yml** - Docker setup
- **nginx.conf** - Reverse proxy config

---

## 🎯 IMPLEMENTATION ROADMAP

### **PHASE 1: Database & Models** (0.5-1 hour)
- [ ] Add `isActive` field to User.java
- [ ] Add `isActive` field to Book.java
- [ ] Create V3 migration file
- [ ] Run migration

### **PHASE 2: Services** (2-3 hours)
- [ ] Create UserService.java
- [ ] Extend BookService.java (add delete/update/lock)
- [ ] Extend OrderService.java (add query/filter methods)
- [ ] Create DTOs (BookUpdateDto, OrderDetailResponse)

### **PHASE 3: Controllers** (2-3 hours)
- [ ] Create AdminUserController.java
- [ ] Create AdminOrderController.java
- [ ] Extend AdminBookController.java
- [ ] Extend PanelController.java
- [ ] Extend PanelPageController.java (add /admin/orders route)

### **PHASE 4: Repositories** (0.5-1 hour)
- [ ] Add query methods to UserRepository
- [ ] Add query methods to BookRepository
- [ ] Add query methods to OrderRepository

### **PHASE 5: Frontend** (2-3 hours)
- [ ] Create Admin_Orders.html
- [ ] Update admin_layout.html (add Orders menu)
- [ ] Update Admin_Users.html (add lock/unlock buttons)
- [ ] Update Admin_Books.html (add delete/update/lock buttons)
- [ ] Extend panel-data.js (add new functions)

### **PHASE 6: Testing** (1-2 hours)
- [ ] Test all endpoints with Postman
- [ ] Test UI interactions
- [ ] Security validation
- [ ] Load testing

---

## 📊 FEATURE COMPLETION MATRIX

| Feature | DB | Model | Service | Controller | Repo | Frontend | Status |
|---------|----|----|---------|-----------|------|----------|--------|
| **A01: Dashboard** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 90% |
| **A02: Lock/Unlock User** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 100% |
| **A03: Book Operations** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 100% |
| **A04: All Orders** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 100% |

---

## 🔄 DATA FLOW EXAMPLES

### **Lock User Flow**
```
Frontend Button Click
    ↓
fetch() call to PUT /api/admin/users/{id}/lock
    ↓
AdminUserController.lockUser()
    ↓
UserService.lockUser()
    ↓
UserRepository.save(user) with isActive=false
    ↓
Database UPDATE users SET is_active=0 WHERE id={id}
    ↓
Response 200 OK with updated User object
    ↓
Frontend: Update UI, show "Locked" status
```

### **Get Orders Flow**
```
Frontend: Load Admin Orders page
    ↓
PanelPageController: GET /admin/orders
    ↓
Returns Admin_Orders.html template
    ↓
panel-data.js: initAdminOrders()
    ↓
fetch() to GET /api/panel/orders?page=0&size=20
    ↓
PanelController.getOrdersForAdmin()
    ↓
OrderRepository.findAll() + filtering
    ↓
Response: Page<Map> with order data
    ↓
Frontend: Render table with orders
```

---

## 🚨 KNOWN LIMITATIONS & NOTES

1. **AdminUserController** - Doesn't exist yet, need to create
2. **AdminOrderController** - Doesn't exist yet, need to create
3. **UserService** - Doesn't exist yet, need to create
4. **Admin_Orders.html** - Doesn't exist yet, need to create
5. **isActive field** - Not in database yet, need migration
6. **Admin_Orders menu** - Not in sidebar yet
7. **Book delete logic** - Cascading effects on orders need handling
8. **Audit logging** - No tracking of admin actions currently
9. **Soft delete** - Using hard delete, consider soft delete in future
10. **Order status filter** - Not fully implemented in backend

---

## 📦 DEPENDENCIES

### **Backend**
- Spring Boot 3.x
- Spring Data JPA
- Spring Security
- JWT (jjwt or similar)
- MySQL Connector / MSSQL Connector
- Flyway (migrations)
- Lombok
- Validation API

### **Frontend**
- Thymeleaf
- Tailwind CSS
- Chart.js (for dashboards)
- Vanilla JavaScript (no jQuery)

---

## 🧪 TESTING RESOURCES

### **Postman Collection** (To be created)
```
📁 BOOKOM Admin APIs
   📁 Users Management
      POST /api/admin/users/{id}/lock
      POST /api/admin/users/{id}/unlock
      GET /api/admin/users/{id}
   📁 Books Management
      DELETE /api/admin/books/{id}
      PUT /api/admin/books/{id}
      PUT /api/admin/books/{id}/lock
      PUT /api/admin/books/{id}/unlock
   📁 Orders Management
      GET /api/admin/orders
      GET /api/admin/orders/{id}
```

### **SQL Test Queries**
```sql
-- Check is_active field
SELECT COUNT(*) as active_users FROM users WHERE is_active=1;
SELECT COUNT(*) as locked_users FROM users WHERE is_active=0;

-- Check books
SELECT COUNT(*) as active_books FROM books WHERE is_active=1;
SELECT COUNT(*) as locked_books FROM books WHERE is_active=0;

-- Check orders by status
SELECT status, COUNT(*) FROM sub_orders GROUP BY status;
```

---

**Document Version:** 2.0  
**Last Updated:** May 16, 2026  
**Coverage:** 100% of codebase analyzed  
**Status:** Ready for Implementation
