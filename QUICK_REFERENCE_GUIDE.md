# 🚀 QUICK REFERENCE GUIDE - ADMIN FEATURES

## 📌 ONE-PAGE IMPLEMENTATION SUMMARY

### **Feature Matrix**

| Feature | Status | Priority | Est. Time | Complexity |
|---------|--------|----------|-----------|-----------|
| **A01: Dashboard** | 🟡 Partial | HIGH | 4h | Medium |
| **A02: Lock/Unlock User** | 🔴 New | HIGH | 3h | Low |
| **A03: Book Review (Expand)** | 🟢 Partial | HIGH | 5h | Medium |
| **A04: View All Orders** | 🔴 New | HIGH | 4h | Medium |
| **Total** | - | - | **16h** | - |

---

## 🎯 CORE CHANGES SUMMARY

### **Database Changes**
```sql
ALTER TABLE users ADD COLUMN is_active BIT DEFAULT 1;
ALTER TABLE books ADD COLUMN is_active BIT DEFAULT 1;
```

### **Model Changes**
```java
// User.java
private boolean isActive = true;

// Book.java
private boolean isActive = true;
```

### **New API Endpoints**
```
POST   /api/admin/users/{id}/lock
POST   /api/admin/users/{id}/unlock
DELETE /api/admin/books/{id}
PUT    /api/admin/books/{id}
PUT    /api/admin/books/{id}/lock
PUT    /api/admin/books/{id}/unlock
GET    /api/admin/orders
GET    /api/admin/orders/{id}
```

### **New Files to Create**
1. `AdminUserController.java`
2. `AdminOrderController.java`
3. `UserService.java`
4. DTOs: `BookUpdateDto.java`, `OrderDetailResponse.java`
5. `Admin_Orders.html`

### **Files to Modify**
1. `User.java` - Add `isActive`
2. `Book.java` - Add `isActive`
3. `UserRepository.java` - Add query methods
4. `BookRepository.java` - Add query methods
5. `OrderRepository.java` - Add query methods
6. `AdminBookController.java` - Add delete/update/lock
7. `BookService.java` - Add service methods
8. `OrderService.java` - Add service methods
9. `PanelController.java` - Mở rộng APIs
10. `admin_layout.html` - Add Orders menu item
11. `Admin_Users.html` - Add lock/unlock actions
12. `Admin_Books.html` - Add delete/update/lock actions
13. `panel-data.js` - Add functions for orders
14. `PanelPageController.java` - Add orders route

---

## 🔑 KEY IMPLEMENTATION STEPS

### **Step 1: Backend Models** (30 min)
```java
// Add to User.java & Book.java
@Column(columnDefinition = "BIT DEFAULT 1", nullable = false)
private boolean isActive = true;
```

### **Step 2: Service Layer** (1.5 hours)
- Create `UserService.java` with lock/unlock methods
- Extend `BookService.java` with CRUD methods
- Extend `OrderService.java` with query methods

### **Step 3: Controllers** (2 hours)
- Create `AdminUserController.java`
- Create `AdminOrderController.java`
- Extend `AdminBookController.java`
- Extend `PanelController.java`

### **Step 4: Frontend** (2 hours)
- Create `Admin_Orders.html`
- Update sidebar & menu
- Update table UIs with action buttons
- Add JS functions in `panel-data.js`

### **Step 5: Testing** (1 hour)
- Test each endpoint with Postman
- Test UI interactions
- Security validation

---

## 📊 DATABASE SCHEMA

```
users
├── id (PK)
├── username
├── password_hash
├── role (BUYER|SELLER|ADMIN)
├── is_active ✨ NEW
└── ...

books
├── id (PK)
├── title
├── author
├── price
├── approval_status (PENDING|APPROVED|REJECTED)
├── is_active ✨ NEW
├── seller_id (FK)
└── ...

orders_master
├── id (PK)
├── buyer_id (FK)
├── total_amount
├── shipping_address
├── created_at
└── ...

sub_orders
├── id (PK)
├── order_id (FK)
├── seller_id (FK)
├── status
└── ...
```

---

## 🔐 ROLE-BASED ACCESS

### **Admin Permissions**
- ✅ View all users & lock/unlock
- ✅ View all books & delete/update/lock
- ✅ View all orders & details
- ✅ Approve/reject pending books
- ✅ View system dashboard

### **Security Checks**
```java
// Every admin endpoint must have this
String role = (String) request.getAttribute("CURRENT_USER_ROLE");
if (!"ADMIN".equals(role)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body("Access Denied!");
}
```

---

## 📋 CODE SNIPPETS

### **Lock User**
```java
@PutMapping("/{id}/lock")
public ResponseEntity<?> lockUser(@PathVariable Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));
    user.setActive(false);
    return ResponseEntity.ok(userRepository.save(user));
}
```

### **Delete Book**
```java
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteBook(@PathVariable Long id) {
    bookRepository.deleteById(id);
    return ResponseEntity.ok("Book deleted");
}
```

### **Get Orders**
```java
@GetMapping()
public ResponseEntity<?> getOrders(
    @RequestParam int page,
    @RequestParam int size
) {
    Page<Order> orders = orderRepository.findAll(
        PageRequest.of(page, size)
    );
    return ResponseEntity.ok(orders);
}
```

### **Filter Books**
```javascript
function initAdminBooks() {
    var url = "/api/panel/books-all?" + qs({
        q: searchInput,
        status: approvalStatus,
        active: activeStatus
    });
    return getJson(url).then(data => {
        renderTable(data);
    });
}
```

---

## 🧪 POSTMAN TEST CASES

### **Test 1: Lock User**
```
PUT /api/admin/users/5/lock
Header: Authorization: Bearer TOKEN
Response: 200 OK
{
  "id": 5,
  "username": "john_doe",
  "isActive": false
}
```

### **Test 2: Delete Book**
```
DELETE /api/admin/books/10
Header: Authorization: Bearer TOKEN
Response: 200 OK
{ "message": "Book deleted" }
```

### **Test 3: Get Orders**
```
GET /api/admin/orders?page=0&size=20
Header: Authorization: Bearer TOKEN
Response: 200 OK
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8
}
```

### **Test 4: Get Order Details**
```
GET /api/admin/orders/1
Header: Authorization: Bearer TOKEN
Response: 200 OK
{
  "id": 1,
  "buyer": "customer_name",
  "total": 500000,
  "status": "PROCESSING",
  "subOrders": [...]
}
```

---

## ⚡ PERFORMANCE TIPS

1. **Add Pagination** - Limit to 20 items per page
2. **Use Indexes** - Create DB indexes on `is_active`, `approval_status`
3. **Caching** - Cache category list on admin page load
4. **Lazy Loading** - Use JPA FetchType.LAZY for relations
5. **Query Optimization** - Use projections instead of full entities

---

## 🐛 COMMON ISSUES & FIXES

### **Issue: Can't lock user**
```
Error: "Column 'is_active' doesn't exist"
Solution: Run migration V3__add_isactive_fields.sql
```

### **Issue: Admin can't see orders**
```
Error: 403 Forbidden
Solution: Check JWT token contains ADMIN role
```

### **Issue: Book update doesn't work**
```
Error: Validation failed
Solution: Validate CategoryId exists before saving
```

### **Issue: Frontend buttons not showing**
```
Error: Buttons not rendered
Solution: Check panel-data.js is loaded, no console errors
```

---

## 📱 RESPONSIVE DESIGN

### **Mobile Layout**
- Stack filters vertically
- Hide columns: Author, Description
- Keep: Title, Price, Status, Actions
- Action buttons: Collapse to "..." menu

### **Tablet Layout**
- Show all columns except Description
- Filters in 2 columns
- Full action buttons

### **Desktop Layout**
- All columns visible
- Filters in single row
- Full-width tables

---

## 🎨 UI COMPONENTS

### **Status Badge**
```html
<span class="rounded px-2 py-1 text-xs font-black bg-emerald-100 text-emerald-700">
  Active
</span>

<span class="rounded px-2 py-1 text-xs font-black bg-rose-100 text-rose-700">
  Locked
</span>
```

### **Action Buttons**
```html
<!-- Lock Button -->
<button onclick="lockUser(5)" class="rounded border border-amber-300 px-3 py-1 text-xs font-black hover:bg-amber-50">
  Khóa
</button>

<!-- Delete Button -->
<button onclick="deleteBook(10)" class="rounded border border-rose-300 px-3 py-1 text-xs font-black hover:bg-rose-50">
  Xóa
</button>

<!-- View Details -->
<button onclick="viewDetails(1)" class="rounded border border-brand-accent px-3 py-1 text-xs font-black">
  Chi tiết
</button>
```

---

## 📞 SUPPORT & DEBUGGING

### **Enable Debug Logging**
```properties
# application.properties
logging.level.com.example.bookstore=DEBUG
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### **Check Endpoints**
```bash
# Get all active endpoints
curl http://localhost:8080/actuator/mappings

# Test API
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/admin/users
```

### **Database Verification**
```sql
-- Check if columns exist
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME='users' AND COLUMN_NAME='is_active';

-- Check data
SELECT id, username, is_active FROM users LIMIT 10;
```

---

## 🚀 DEPLOYMENT CHECKLIST

- [ ] Run all migrations
- [ ] Test all endpoints
- [ ] Update frontend assets
- [ ] Clear browser cache
- [ ] Test on staging
- [ ] Backup database
- [ ] Monitor logs after deploy
- [ ] Test with real admin account

---

**Quick Reference Version:** 1.0  
**Last Updated:** May 16, 2026  
**For:** BOOKOM Admin Panel Enhancement
