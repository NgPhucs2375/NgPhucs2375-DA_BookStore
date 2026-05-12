# Order Filtering Feature - Documentation

## Overview

The Order Filtering feature provides flexible and powerful filtering capabilities for both buyers and sellers in the BookStore multi-vendor application. This feature allows users to search, filter, and sort orders based on multiple criteria.

---

## 1. Architecture Overview

### Components Created

#### DTOs (Data Transfer Objects)
- **`OrderFilterRequest`**: Request model for filtering buyer orders
- **`OrderFilterResponse`**: Response model with paginated order list
- **`OrderSummaryResponse`**: Summary view of a single order
- **`SubOrderFilterRequest`**: Request model for filtering seller sub-orders
- **`SubOrderFilterResponse`**: Response model with paginated sub-order list

#### Repository Extensions
- **`OrderRepository`**: Added JPA query methods for complex filtering
- **`SubOrderRepository`**: Added JPA query methods for complex filtering

#### Service Layer
- **`OrderService`**: Added 8 new filtering and search methods
  - `filterBuyerOrders()`: Main filtering for buyer orders
  - `filterSellerSubOrders()`: Main filtering for seller sub-orders
  - `searchSellerSubOrdersByBuyer()`: Search by buyer name
  - `getBuyerOrdersByStatus()`: Filter by status
  - `getSellerSubOrdersByStatus()`: Filter by status
  - Helper methods for conversion and status determination

#### Controller Endpoints
- **`OrderController`**: Added 5 new REST endpoints for filtering operations

---

## 2. API Endpoints

### For Buyers

#### 1. Filter Orders (Advanced)
**Endpoint:** `POST /api/orders/me/filter`

**Request Body:**
```json
{
  "status": "COMPLETED",
  "createdFrom": "2024-01-01T00:00:00",
  "createdTo": "2024-12-31T23:59:59",
  "minPrice": 100000,
  "maxPrice": 500000,
  "sellerName": "Fahasa",
  "page": 0,
  "pageSize": 10,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

**Response:**
```json
{
  "orders": [
    {
      "orderId": 1,
      "buyerId": 5,
      "buyerUsername": "john_doe",
      "totalAmount": 350000,
      "createdAt": "2024-06-15T10:30:00",
      "subOrderCount": 2,
      "overallStatus": "COMPLETED",
      "shippingAddress": "123 Main St, District 1, HCM"
    }
  ],
  "totalCount": 25,
  "currentPage": 0,
  "pageSize": 10,
  "totalPages": 3
}
```

**Query Parameters:**
- `status`: Filter by order status (PENDING_PAYMENT, CONFIRMED, SHIPPED, COMPLETED, CANCELLED)
- `createdFrom`: Orders created after this date
- `createdTo`: Orders created before this date
- `minPrice`: Minimum order total amount
- `maxPrice`: Maximum order total amount
- `page`: Page number (0-based, default: 0)
- `pageSize`: Items per page (default: 10, max: 100)
- `sortBy`: Sort field (createdAt, totalAmount, status)
- `sortDirection`: Sort direction (ASC, DESC)

**All parameters are optional.**

---

#### 2. Get Orders by Status
**Endpoint:** `GET /api/orders/me/status/{status}`

**Example:**
```
GET /api/orders/me/status/COMPLETED
```

**Response:**
```json
[
  {
    "orderId": 1,
    "buyerId": 5,
    "buyerUsername": "john_doe",
    "totalAmount": 350000,
    "createdAt": "2024-06-15T10:30:00",
    "subOrderCount": 2,
    "overallStatus": "COMPLETED",
    "shippingAddress": "123 Main St, District 1, HCM"
  },
  {
    "orderId": 3,
    "buyerId": 5,
    "buyerUsername": "john_doe",
    "totalAmount": 125000,
    "createdAt": "2024-06-20T14:45:00",
    "subOrderCount": 1,
    "overallStatus": "COMPLETED",
    "shippingAddress": "456 Oak Ave, District 2, HCM"
  }
]
```

---

### For Sellers

#### 1. Filter Sub-Orders (Advanced)
**Endpoint:** `POST /api/orders/seller/me/filter`

**Request Body:**
```json
{
  "status": "CONFIRMED",
  "createdFrom": "2024-01-01T00:00:00",
  "createdTo": "2024-12-31T23:59:59",
  "minPrice": 50000,
  "maxPrice": 300000,
  "buyerName": "john",
  "page": 0,
  "pageSize": 10,
  "sortBy": "subTotal",
  "sortDirection": "DESC"
}
```

**Response:**
```json
{
  "subOrders": [
    {
      "subOrderId": 101,
      "orderId": 1,
      "sellerId": 2,
      "sellerName": "Fahasa",
      "buyerUsername": "john_doe",
      "itemSummary": "Harry Potter, The Hobbit, ...",
      "itemCount": 5,
      "status": "CONFIRMED",
      "subTotal": 250000
    }
  ],
  "totalCount": 15,
  "currentPage": 0,
  "pageSize": 10,
  "totalPages": 2
}
```

**Query Parameters:**
- `status`: Filter by sub-order status
- `createdFrom`: Orders created after this date
- `createdTo`: Orders created before this date
- `minPrice`: Minimum sub-order total amount
- `maxPrice`: Maximum sub-order total amount
- `buyerName`: Search by buyer username
- `page`: Page number (0-based, default: 0)
- `pageSize`: Items per page (default: 10, max: 100)
- `sortBy`: Sort field (createdAt, subTotal, status, id)
- `sortDirection`: Sort direction (ASC, DESC)

**All parameters are optional.**

---

#### 2. Get Sub-Orders by Status
**Endpoint:** `GET /api/orders/seller/me/status/{status}`

**Example:**
```
GET /api/orders/seller/me/status/CONFIRMED
```

**Response:**
```json
[
  {
    "subOrderId": 101,
    "orderId": 1,
    "sellerId": 2,
    "sellerName": "Fahasa",
    "buyerUsername": "john_doe",
    "itemSummary": "Harry Potter, The Hobbit, ...",
    "itemCount": 5,
    "status": "CONFIRMED",
    "subTotal": 250000
  },
  {
    "subOrderId": 105,
    "orderId": 4,
    "sellerId": 2,
    "sellerName": "Fahasa",
    "buyerUsername": "jane_smith",
    "itemSummary": "The Great Gatsby, ...",
    "itemCount": 2,
    "status": "CONFIRMED",
    "subTotal": 180000
  }
]
```

---

#### 3. Search Sub-Orders by Buyer Name
**Endpoint:** `GET /api/orders/seller/me/search?buyerName={name}`

**Example:**
```
GET /api/orders/seller/me/search?buyerName=john
```

**Response:**
```json
[
  {
    "subOrderId": 101,
    "orderId": 1,
    "sellerId": 2,
    "sellerName": "Fahasa",
    "buyerUsername": "john_doe",
    "itemSummary": "Harry Potter, The Hobbit, ...",
    "itemCount": 5,
    "status": "CONFIRMED",
    "subTotal": 250000
  }
]
```

---

## 3. Order Status Enumeration

```
PENDING_PAYMENT   - Order waiting for payment confirmation
PROCESSING        - Order is being prepared for shipment
SHIPPING          - Order is on the way to buyer
COMPLETED         - Order has been received
CANCELLED         - Order was cancelled
```

---

## 4. Key Features

### ✅ For Buyers
1. **Filter by Status** - View orders in specific status
2. **Filter by Date Range** - Find orders created within specific period
3. **Filter by Price Range** - Find orders within budget
4. **Combined Filters** - Apply multiple filters simultaneously
5. **Pagination** - Efficiently navigate large result sets
6. **Flexible Sorting** - Sort by date, amount, or status

### ✅ For Sellers
1. **Filter Sub-Orders by Status** - Monitor order fulfillment
2. **Filter by Date Range** - View orders from specific period
3. **Filter by Price Range** - Analyze sales performance
4. **Search by Buyer Name** - Find specific customer orders
5. **Combined Filters** - Apply multiple filters simultaneously
6. **Pagination** - Efficiently navigate large result sets
7. **Flexible Sorting** - Sort by amount, date, or status

---

## 5. Code Examples

### Example 1: Get All Completed Orders
```bash
curl -X POST http://localhost:8080/api/orders/me/filter \
  -H "X-User-Id: 5" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "COMPLETED"
  }'
```

### Example 2: Get Recent Orders Between Price Range
```bash
curl -X POST http://localhost:8080/api/orders/me/filter \
  -H "X-User-Id: 5" \
  -H "Content-Type: application/json" \
  -d '{
    "minPrice": 100000,
    "maxPrice": 500000,
    "createdFrom": "2024-06-01T00:00:00",
    "createdTo": "2024-06-30T23:59:59",
    "sortBy": "createdAt",
    "sortDirection": "DESC",
    "page": 0,
    "pageSize": 20
  }'
```

### Example 3: Seller - Filter Confirmed Orders
```bash
curl -X POST http://localhost:8080/api/orders/seller/me/filter \
  -H "X-User-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED",
    "page": 0,
    "pageSize": 10
  }'
```

### Example 4: Seller - Search Orders by Buyer
```bash
curl -X GET "http://localhost:8080/api/orders/seller/me/search?buyerName=john" \
  -H "X-User-Id: 2"
```

---

## 6. Overall Order Status Determination

The overall order status is determined by analyzing all sub-orders:

```
IF all sub-orders are COMPLETED 
  → Order Status: COMPLETED
ELSE IF any sub-order is SHIPPED 
  → Order Status: SHIPPED
ELSE IF any sub-order is CONFIRMED 
  → Order Status: CONFIRMED
ELSE 
  → Order Status: PENDING_PAYMENT
```

---

## 7. Database Queries (HQL/JPQL)

### OrderRepository Query Example
```sql
SELECT o FROM Order o 
WHERE o.buyer = :buyer 
  AND o.createdAt >= :createdFrom 
  AND o.createdAt <= :createdTo 
  AND o.totalAmount >= :minPrice 
  AND o.totalAmount <= :maxPrice 
ORDER BY o.createdAt DESC
```

### SubOrderRepository Query Example
```sql
SELECT so FROM SubOrder so 
WHERE so.seller = :seller 
  AND so.status = :status 
  AND so.parentOrder.createdAt >= :createdFrom 
  AND so.parentOrder.createdAt <= :createdTo 
  AND so.subTotal >= :minPrice 
  AND so.subTotal <= :maxPrice 
ORDER BY so.id DESC
```

---

## 8. Error Handling

### Common Error Responses

**400 Bad Request - User is not a buyer/seller:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "User is not a buyer"
}
```

**404 Not Found - User not found:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Buyer not found"
}
```

**403 Forbidden - Access denied:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Seller cannot access this order"
}
```

---

## 9. Performance Considerations

- **Pagination**: All list endpoints support pagination to avoid memory issues
- **Default Page Size**: 10 items (maximum 100)
- **Database Indexes**: Ensure indexes on:
  - `orders.buyer_id`, `orders.created_at`, `orders.total_amount`
  - `sub_orders.seller_id`, `sub_orders.status`, `sub_orders.created_at`

---

## 10. Testing the Feature

### Test Scenarios

1. **Basic Filtering**
   - Filter orders by status only
   - Filter orders by date range only
   - Filter orders by price range only

2. **Combined Filtering**
   - Filter by status + date range + price range
   - Apply multiple filters together

3. **Pagination**
   - Test different page numbers
   - Test different page sizes
   - Verify totalPages calculation

4. **Sorting**
   - Sort by different fields
   - Test ASC and DESC directions

5. **Edge Cases**
   - Empty filter request (should return all)
   - Invalid date ranges
   - Negative or zero prices
   - Out-of-range page numbers

---

## 11. Future Enhancements

- **Export Orders**: Export filtered results to CSV/PDF
- **Bulk Actions**: Perform actions on multiple orders
- **Advanced Search**: Full-text search on order items
- **Analytics Dashboard**: Generate reports from filtered data
- **Order Notifications**: Real-time alerts for status changes
- **Saved Filters**: Save custom filter configurations

---

## Summary

The Order Filtering feature provides a comprehensive solution for:
- ✅ Buyers to find, track, and organize their orders
- ✅ Sellers to manage and fulfill customer orders efficiently
- ✅ Flexible filtering with multiple criteria
- ✅ Scalable pagination for large datasets
- ✅ Role-based access control and security
- ✅ RESTful API design following best practices
