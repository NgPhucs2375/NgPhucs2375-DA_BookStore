# 🚀 ADMIN FEATURES - DETAILED TECHNICAL SPECIFICATIONS

## PHASE 1: DATABASE & MODELS

### Step 1.1: Update User.java
**File:** `src/main/java/com/example/bookstore/model/User.java`

**Add this field:**
```java
@Column(columnDefinition = "BIT DEFAULT 1", nullable = false)
private boolean isActive = true;  // true = hoạt động, false = khóa
```

---

### Step 1.2: Update Book.java  
**File:** `src/main/java/com/example/bookstore/model/Book.java`

**Add this field:**
```java
@Column(columnDefinition = "BIT DEFAULT 1", nullable = false)
private boolean isActive = true;  // true = khả dụng, false = khóa
```

---

### Step 1.3: Create UserService.java
**File:** `src/main/java/com/example/bookstore/service/UserService.java`

```java
package com.example.bookstore.service;

import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Lấy người dùng theo ID
     */
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    /**
     * Lấy danh sách người dùng theo role
     */
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findAllByRole(role);
    }
    
    /**
     * Khóa tài khoản người dùng
     */
    public User lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + userId));
        user.setActive(false);
        return userRepository.save(user);
    }
    
    /**
     * Mở khóa tài khoản người dùng
     */
    public User unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + userId));
        user.setActive(true);
        return userRepository.save(user);
    }
    
    /**
     * Lấy tất cả người dùng (for admin listing)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

---

### Step 1.4: Extend UserRepository.java
**File:** `src/main/java/com/example/bookstore/repository/UserRepository.java`

**Add these methods:**
```java
List<User> findByRoleAndIsActive(UserRole role, boolean isActive);

List<User> findByIsActive(boolean isActive);

List<User> findByUsernameContainingIgnoreCase(String username);
```

---

### Step 1.5: Extend BookRepository.java
**File:** `src/main/java/com/example/bookstore/repository/BookRepository.java`

**Add these methods:**
```java
List<Book> findByIsActive(boolean isActive);

Page<Book> findByIsActive(boolean isActive, Pageable pageable);

Page<Book> findByApprovalStatusAndIsActive(
    ApprovalStatus status, 
    boolean isActive, 
    Pageable pageable
);

List<Book> findBySeller(User seller);

Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
```

---

### Step 1.6: Extend OrderRepository.java
**File:** `src/main/java/com/example/bookstore/repository/OrderRepository.java`

**Add these methods:**
```java
Page<Order> findAll(Pageable pageable);

Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

// Find orders by keyword in buyer name or order ID
@Query("""
    SELECT o FROM Order o
    WHERE CAST(o.id AS string) LIKE %:keyword% 
    OR LOWER(o.buyer.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY o.createdAt DESC
""")
Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
```

---

### Step 1.7: Create DTOs
**File:** `src/main/java/com/example/bookstore/dto/BookUpdateDto.java`

```java
package com.example.bookstore.dto;

import lombok.Data;

@Data
public class BookUpdateDto {
    private String title;
    private String author;
    private String description;
    private Double price;
    private Integer stockQuantity;
    private String publisher;
    private String publishYear;
    private Long categoryId;
}
```

**File:** `src/main/java/com/example/bookstore/dto/OrderDetailResponse.java`

```java
package com.example.bookstore.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {
    private Long id;
    private String buyerName;
    private String buyerEmail;
    private Double totalAmount;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private List<SubOrderDetail> subOrders;
    
    @Data
    @Builder
    public static class SubOrderDetail {
        private Long id;
        private String sellerName;
        private String status;
        private Double subTotal;
        private List<OrderItemDetail> items;
    }
    
    @Data
    @Builder
    public static class OrderItemDetail {
        private Long id;
        private String bookTitle;
        private Double price;
        private Integer quantity;
        private Double subtotal;
    }
}
```

---

## PHASE 2: BACKEND CONTROLLERS & SERVICES

### Step 2.1: Create AdminUserController.java
**File:** `src/main/java/com/example/bookstore/controller/AdminUserController.java`

```java
package com.example.bookstore.controller;

import com.example.bookstore.model.User;
import com.example.bookstore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/users")
public class AdminUserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * Khóa tài khoản người dùng
     */
    @PutMapping("/{id}/lock")
    public ResponseEntity<?> lockUser(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối: Chỉ Admin mới có quyền!");
        }
        
        try {
            User user = userService.lockUser(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
    
    /**
     * Mở khóa tài khoản người dùng
     */
    @PutMapping("/{id}/unlock")
    public ResponseEntity<?> unlockUser(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối: Chỉ Admin mới có quyền!");
        }
        
        try {
            User user = userService.unlockUser(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
    
    /**
     * Lấy thông tin người dùng theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối!");
        }
        
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User không tồn tại");
        }
        return ResponseEntity.ok(user);
    }
}
```

---

### Step 2.2: Extend AdminBookController.java
**File:** `src/main/java/com/example/bookstore/controller/AdminBookController.java`

**Add these methods:**
```java
/**
 * Xóa sách
 */
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteBook(
        @PathVariable Long id,
        HttpServletRequest request
) {
    String role = (String) request.getAttribute("CURRENT_USER_ROLE");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Truy cập bị từ chối!");
    }
    
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
public ResponseEntity<?> updateBook(
        @PathVariable Long id,
        @RequestBody BookUpdateDto dto,
        HttpServletRequest request
) {
    String role = (String) request.getAttribute("CURRENT_USER_ROLE");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Truy cập bị từ chối!");
    }
    
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
public ResponseEntity<?> lockBook(
        @PathVariable Long id,
        HttpServletRequest request
) {
    String role = (String) request.getAttribute("CURRENT_USER_ROLE");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Truy cập bị từ chối!");
    }
    
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
public ResponseEntity<?> unlockBook(
        @PathVariable Long id,
        HttpServletRequest request
) {
    String role = (String) request.getAttribute("CURRENT_USER_ROLE");
    if (!"ADMIN".equals(role)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Truy cập bị từ chối!");
    }
    
    try {
        Book book = bookService.unlockBook(id);
        return ResponseEntity.ok(book);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

---

### Step 2.3: Create AdminOrderController.java
**File:** `src/main/java/com/example/bookstore/controller/AdminOrderController.java`

```java
package com.example.bookstore.controller;

import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * Lấy tất cả đơn hàng (có phân trang và filter)
     */
    @GetMapping()
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            HttpServletRequest request
    ) {
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối!");
        }
        
        try {
            LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : null;
            LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : null;
            
            Page<?> orders = orderService.getOrdersWithFilters(page, size, q, status, from, to);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * Lấy chi tiết đơn hàng
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetails(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối!");
        }
        
        try {
            OrderDetailResponse detail = orderService.getOrderDetailsForAdmin(id);
            if (detail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy đơn hàng");
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}
```

---

### Step 2.4: Extend BookService.java
**File:** `src/main/java/com/example/bookstore/service/BookService.java`

**Add these methods:**
```java
/**
 * Xóa sách (Admin)
 */
public void deleteBook(Long bookId) {
    Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
    bookRepository.deleteById(bookId);
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
        book.setPublishYear(dto.getPublishYear());
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
```

---

### Step 2.5: Extend OrderService.java
**File:** `src/main/java/com/example/bookstore/service/OrderService.java`

**Add these methods:**
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Lấy đơn hàng với filters (for admin)
 */
public Page<Order> getOrdersWithFilters(
        int page, int size, String q, String status,
        LocalDate dateFrom, LocalDate dateTo
) {
    Pageable pageable = PageRequest.of(page, size);
    
    LocalDateTime startDateTime = dateFrom != null 
        ? dateFrom.atStartOfDay() 
        : null;
    LocalDateTime endDateTime = dateTo != null 
        ? dateTo.atTime(LocalTime.MAX) 
        : null;
    
    // Implement complex query based on filters
    if (q != null && !q.isEmpty()) {
        return orderRepository.searchOrders(q, pageable);
    }
    
    return orderRepository.findAll(pageable);
}

/**
 * Lấy chi tiết đơn hàng (for admin)
 */
public OrderDetailResponse getOrderDetailsForAdmin(Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
    
    List<OrderDetailResponse.SubOrderDetail> subOrderDetails = order.getSubOrders()
            .stream()
            .map(subOrder -> {
                List<OrderDetailResponse.OrderItemDetail> items = subOrder.getItems()
                        .stream()
                        .map(item -> OrderDetailResponse.OrderItemDetail.builder()
                                .id(item.getId())
                                .bookTitle(item.getBook().getTitle())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .subtotal(item.getPrice() * item.getQuantity())
                                .build())
                        .toList();
                
                return OrderDetailResponse.SubOrderDetail.builder()
                        .id(subOrder.getId())
                        .sellerName(subOrder.getSeller().getUsername())
                        .status(subOrder.getStatus().toString())
                        .subTotal(subOrder.getSubTotal())
                        .items(items)
                        .build();
            })
            .toList();
    
    return OrderDetailResponse.builder()
            .id(order.getId())
            .buyerName(order.getBuyer().getUsername())
            .buyerEmail(order.getBuyer().getUsername() + "@bookom.vn")
            .totalAmount(order.getTotalAmount())
            .shippingAddress(order.getShippingAddress())
            .createdAt(order.getCreatedAt())
            .subOrders(subOrderDetails)
            .build();
}
```

---

### Step 2.6: Extend PanelController.java
**File:** `src/main/java/com/example/bookstore/controller/PanelController.java`

**Add these methods (in addition to existing ones):**
```java
/**
 * Lấy chi tiết người dùng với filter trạng thái
 */
@GetMapping("/users-detailed")
public List<Map<String, Object>> getUsersDetailed(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status
) {
    List<User> users = userRepository.findAll();
    
    return users.stream()
            .filter(u -> q == null || q.isEmpty() || 
                    lower(u.getUsername()).contains(lower(q)))
            .filter(u -> role == null || role.equals("all") || 
                    u.getRole().toString().equals(role))
            .filter(u -> status == null || status.equals("all") ||
                    (status.equals("Active") && u.isActive()) ||
                    (status.equals("Inactive") && !u.isActive()))
            .map(u -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", u.getId());
                map.put("name", u.getUsername());
                map.put("email", u.getUsername() + "@bookom.vn");
                map.put("role", u.getRole().toString());
                map.put("status", u.isActive() ? "Active" : "Inactive");
                map.put("joined", "2026-02-01");
                map.put("action", u.isActive() ? "lock" : "unlock");
                return map;
            })
            .toList();
}

/**
 * Lấy tất cả sách (với filter trạng thái duyệt và hoạt động)
 */
@GetMapping("/books-all")
public Page<Map<String, Object>> getAllBooksForAdmin(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String approvalStatus,
        @RequestParam(required = false) String active
) {
    Pageable pageable = PageRequest.of(page, size);
    List<Book> books = bookRepository.findAll();
    
    List<Map<String, Object>> filtered = books.stream()
            .filter(b -> q == null || q.isEmpty() ||
                    lower(b.getTitle()).contains(lower(q)) ||
                    lower(b.getAuthor()).contains(lower(q)))
            .filter(b -> approvalStatus == null || approvalStatus.equals("all") ||
                    b.getApprovalStatus().toString().equals(approvalStatus))
            .filter(b -> active == null || active.equals("all") ||
                    (active.equals("active") && b.isActive()) ||
                    (active.equals("locked") && !b.isActive()))
            .map(b -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", b.getId());
                map.put("title", b.getTitle());
                map.put("author", b.getAuthor());
                map.put("price", b.getPrice());
                map.put("category", b.getCategory().getName());
                map.put("stock", b.getStockQuantity());
                map.put("approvalStatus", b.getApprovalStatus().toString());
                map.put("active", b.isActive() ? "Active" : "Locked");
                map.put("seller", b.getSeller().getUsername());
                return map;
            })
            .toList();
    
    int start = Math.min(page * size, filtered.size());
    int end = Math.min((page + 1) * size, filtered.size());
    List<Map<String, Object>> paged = filtered.subList(start, end);
    
    return new PageImpl<>(paged, pageable, filtered.size());
}

/**
 * Lấy đơn hàng cho admin
 */
@GetMapping("/orders")
public Page<Map<String, Object>> getOrdersForAdmin(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo
) {
    Pageable pageable = PageRequest.of(page, size);
    List<Order> orders = orderRepository.findAll();
    
    List<Map<String, Object>> filtered = orders.stream()
            .map(o -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", o.getId());
                map.put("buyer", o.getBuyer().getUsername());
                map.put("total", o.getTotalAmount());
                map.put("address", o.getShippingAddress());
                map.put("date", o.getCreatedAt());
                // Get first sub-order status as main status
                String orderStatus = o.getSubOrders().isEmpty() 
                    ? "N/A" 
                    : o.getSubOrders().get(0).getStatus().toString();
                map.put("status", orderStatus);
                return map;
            })
            .toList();
    
    int start = Math.min(page * size, filtered.size());
    int end = Math.min((page + 1) * size, filtered.size());
    List<Map<String, Object>> paged = filtered.subList(start, end);
    
    return new PageImpl<>(paged, pageable, filtered.size());
}
```

---

## PHASE 3: FRONTEND PAGES

### Step 3.1: Create Admin_Orders.html
**File:** `src/main/resources/templates/admin/Admin_Orders.html`

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/panel_head :: head(${pageTitle}, false)}"></head>
<body class="font-sans bg-[#fbfaf8] text-[#5D4037]">
<div class="flex min-h-screen">
    <div th:replace="~{fragments/admin_layout :: sidebar(${activeMenu})}"></div>
    <div class="flex-1 min-w-0">
        <div th:replace="~{fragments/admin_layout :: header(${pageTitle}, ${pageSubtitle})}"></div>
        <main class="p-4 md:p-8 space-y-6">
            <!-- Filters -->
            <section class="rounded-xl border border-[#E5CBB5] bg-white p-4 grid grid-cols-1 md:grid-cols-4 gap-3">
                <input id="orders-q" class="rounded-lg border border-[#E5CBB5] px-3 py-2 font-bold" placeholder="Tìm mã đơn/tên khách">
                <select id="orders-status" class="rounded-lg border border-[#E5CBB5] px-3 py-2 font-bold">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="PENDING_PAYMENT">Chờ thanh toán</option>
                    <option value="PROCESSING">Đang xử lý</option>
                    <option value="SHIPPED">Đã gửi</option>
                    <option value="COMPLETED">Hoàn thành</option>
                    <option value="CANCELLED">Đã hủy</option>
                </select>
                <input id="orders-date-from" type="date" class="rounded-lg border border-[#E5CBB5] px-3 py-2 font-bold">
                <input id="orders-date-to" type="date" class="rounded-lg border border-[#E5CBB5] px-3 py-2 font-bold">
            </section>
            
            <!-- Orders Table -->
            <section class="rounded-xl border border-[#E5CBB5] bg-white overflow-hidden">
                <div class="overflow-x-auto">
                    <table class="w-full text-left text-sm">
                        <thead class="bg-[#FAF5E8] text-[#5D4037]/70">
                        <tr>
                            <th class="px-4 py-3 font-black">Mã đơn</th>
                            <th class="px-4 py-3 font-black">Khách hàng</th>
                            <th class="px-4 py-3 font-black">Tổng tiền</th>
                            <th class="px-4 py-3 font-black">Địa chỉ</th>
                            <th class="px-4 py-3 font-black">Trạng thái</th>
                            <th class="px-4 py-3 font-black">Ngày tạo</th>
                            <th class="px-4 py-3 font-black text-right">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody id="admin-orders-body" class="divide-y divide-[#E5CBB5]"></tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>
</div>

<script th:src="@{/js/panel-data.js}"></script>
<script>BookomPanelData.initAdminOrders();</script>
</body>
</html>
```

---

### Step 3.2: Update admin_layout.html
**File:** `src/main/resources/templates/fragments/admin_layout.html`

**Add menu item in sidebar nav:**
```html
<a th:href="@{/admin/orders}" class="flex items-center gap-3 rounded-lg px-4 py-3 transition"
   th:classappend="${activeMenu == 'admin-orders'} ? ' bg-brand-50 text-brand-500 shadow-sm' : ' text-gray-700 hover:bg-gray-100'">
    <span class="h-2 w-2 rounded-full shrink-0" th:classappend="${activeMenu == 'admin-orders'} ? ' bg-brand-500' : ' bg-gray-300'"></span>
    <span class="menu-label">Đơn hàng</span>
</a>
```

---

### Step 3.3: Update Admin_Users.html
**File:** `src/main/resources/templates/admin/Admin_Users.html`

Replace the table to include lock/unlock buttons:
```html
<!-- Update table body -->
<tbody id="admin-users-body" class="divide-y divide-[#E5CBB5]"></tbody>
```

---

### Step 3.4: Update Admin_Books.html
**File:** `src/main/resources/templates/admin/Admin_Books.html`

Mở rộng để show tất cả sách + thêm nút delete/update/lock:
```html
<!-- Add status filter -->
<select id="books-approval" class="rounded-lg border border-[#E5CBB5] px-3 py-2 font-bold">
    <option value="all">Tất cả trạng thái</option>
    <option value="PENDING">Chờ duyệt</option>
    <option value="APPROVED">Đã duyệt</option>
    <option value="REJECTED">Từ chối</option>
</select>

<select id="books-active" class="rounded-lg border border-[#E5CBB5] px-3 py-2 font-bold">
    <option value="all">Tất cả trạng thái</option>
    <option value="active">Hoạt động</option>
    <option value="locked">Bị khóa</option>
</select>
```

---

### Step 3.5: Extend panel-data.js
**File:** `src/main/resources/static/js/panel-data.js`

**Add these functions:**
```javascript
  function initAdminOrders() {
    var qEl = document.getElementById("orders-q");
    var sEl = document.getElementById("orders-status");
    var dfEl = document.getElementById("orders-date-from");
    var dtEl = document.getElementById("orders-date-to");

    function load() {
      var url = API_ROOT + "/orders?" + qs({
        q: qEl ? qEl.value : "",
        status: sEl ? sEl.value : "all",
        dateFrom: dfEl ? dfEl.value : "",
        dateTo: dtEl ? dtEl.value : ""
      });

      return getJson(url).then(function (page) {
        var rows = (page.content || []).map(function (o) {
          var statusClass = o.status === "PENDING_PAYMENT" ? "text-amber-600" 
            : o.status === "PROCESSING" ? "text-indigo-600"
            : o.status === "SHIPPED" ? "text-blue-600"
            : o.status === "COMPLETED" ? "text-emerald-600"
            : "text-rose-600";
          
          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">#' + esc(o.id) + "</td>" +
            '<td class="px-4 py-3">' + esc(o.buyer) + "</td>" +
            '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(o.total) + "</td>" +
            '<td class="px-4 py-3 text-xs">' + esc(o.address) + "</td>" +
            '<td class="px-4 py-3 font-black ' + statusClass + '">' + esc(o.status) + "</td>" +
            '<td class="px-4 py-3">' + esc(o.date) + "</td>" +
            '<td class="px-4 py-3 text-right"><button onclick="viewOrderDetail(' + o.id + ')" class="rounded border border-brand-accent px-3 py-1 text-xs font-black">Chi tiết</button></td>' +
            "</tr>"
          );
        }).join("");

        setHtml("admin-orders-body", rows || '<tr><td class="px-4 py-3" colspan="7">Không có dữ liệu</td></tr>');
      });
    }

    [qEl, sEl, dfEl, dtEl].forEach(function (el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

    return load();
  }

  function viewOrderDetail(orderId) {
    // Navigate to detail page or open modal
    window.location.href = "/admin/orders/" + orderId;
  }

  // Export the init function
  window.BookomPanelData.initAdminOrders = initAdminOrders;
```

---

### Step 3.6: PanelPageController - Add Orders route
**File:** `src/main/java/com/example/bookstore/controller/PanelPageController.java`

**Add this method:**
```java
@GetMapping("/admin/orders")
public String adminOrders(Model model) {
    model.addAttribute("pageTitle", "Quản lý đơn hàng");
    model.addAttribute("pageSubtitle", "Xem toàn bộ đơn hàng trên hệ thống");
    model.addAttribute("activeMenu", "admin-orders");
    return "admin/Admin_Orders";
}
```

---

## 📝 DATABASE MIGRATION

Create migration file:
**File:** `src/main/resources/db/migration/V3__add_isactive_fields.sql`

```sql
-- Add isActive column to users table
ALTER TABLE users ADD COLUMN is_active BIT NOT NULL DEFAULT 1;

-- Add isActive column to books table
ALTER TABLE books ADD COLUMN is_active BIT NOT NULL DEFAULT 1;

-- Create audit log table (optional - for tracking admin actions)
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_value NVARCHAR(MAX),
    new_value NVARCHAR(MAX),
    action_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES users(id)
);
```

---

## ✅ TESTING CHECKLIST

### Backend Testing (Postman/Insomnia)
- [ ] Test lock user API
- [ ] Test unlock user API
- [ ] Test delete book API
- [ ] Test update book API
- [ ] Test lock/unlock book API
- [ ] Test get all orders API
- [ ] Test get order details API
- [ ] Verify admin role check on all endpoints

### Frontend Testing
- [ ] Admin Users page - lock/unlock buttons work
- [ ] Admin Books page - delete/update/lock buttons work
- [ ] Admin Orders page - loads all orders
- [ ] Filters work on all pages
- [ ] Pagination works
- [ ] Error messages display properly
- [ ] Responsive on mobile/tablet

### Security Testing
- [ ] Non-admin cannot access admin endpoints
- [ ] Cannot lock/unlock own user
- [ ] Cannot access other admin's audit logs
- [ ] SQL injection protection on search inputs

---

**Document Version:** 1.0  
**Last Updated:** May 16, 2026  
**Status:** Ready for Implementation
