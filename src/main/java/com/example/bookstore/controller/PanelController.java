package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.Category;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.User;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/panel")
public class PanelController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String lower(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(String source, String keyword) {
        if (safe(keyword).isEmpty()) return true;
        return lower(source).contains(lower(keyword));
    }

    private static String stockBucket(Integer stockQuantity) {
        int stock = stockQuantity == null ? 0 : stockQuantity;
        if (stock < 20) return "low";
        if (stock < 60) return "normal";
        return "high";
    }

    private static String generatedStatus(String seed, int index) {
        int mod = Math.abs(Objects.hash(seed, index)) % 5;
        return mod == 0 ? "Inactive" : "Active";
    }

    private List<Map<String, Object>> buildShops(List<Book> books) {
        Map<String, Integer> byAuthor = new LinkedHashMap<>();
        for (Book book : books) {
            String owner = safe(book.getAuthor());
            if (owner.isEmpty()) owner = "BOOKOM Seller";
            byAuthor.put(owner, byAuthor.getOrDefault(owner, 0) + 1);
        }

        List<Map<String, Object>> shops = new ArrayList<>();
        int idx = 1000;
        for (Map.Entry<String, Integer> entry : byAuthor.entrySet()) {
            idx += 1;
            Map<String, Object> shop = new LinkedHashMap<>();
            shop.put("shopName", "Shop " + entry.getKey());
            shop.put("owner", entry.getKey());
            shop.put("legal", "MST 0" + idx);
            shop.put("products", entry.getValue());
            shop.put("joined", "2026-03-" + String.format("%02d", (idx % 27) + 1));
            shops.add(shop);
        }
        return shops;
    }

    private List<Map<String, Object>> buildUsers(List<Book> books, List<User> users) {
        List<Map<String, Object>> mapped = new ArrayList<>();

        int i = 0;
        for (User user : users) {
            i += 1;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", user.getUsername());
            row.put("email", user.getUsername() + "@bookom.vn");
            row.put("role", i % 3 == 0 ? "Seller" : "Buyer");
            row.put("status", generatedStatus(user.getUsername(), i));
            row.put("joined", "2026-02-" + String.format("%02d", (i % 27) + 1));
            mapped.add(row);
        }

        // Add synthetic sellers from authors to help admin operations when users table is still minimal.
        List<Map<String, Object>> sellerFromBooks = buildShops(books).stream().limit(20).map(shop -> {
            Map<String, Object> row = new LinkedHashMap<>();
            String owner = String.valueOf(shop.get("owner"));
            row.put("name", owner);
            row.put("email", lower(owner).replace(" ", ".") + "@seller.bookom.vn");
            row.put("role", "Seller");
            row.put("status", generatedStatus(owner, owner.length()));
            row.put("joined", shop.get("joined"));
            return row;
        }).collect(Collectors.toList());

        mapped.addAll(sellerFromBooks);
        return mapped;
    }

    private List<Map<String, Object>> buildOrders(List<Book> books) {
        String[] statuses = new String[]{"Cho xac nhan", "Dang giao", "Hoan tat"};
        List<Map<String, Object>> orders = new ArrayList<>();

        for (int i = 0; i < books.size() && i < 60; i++) {
            Book b = books.get(i);
            int qty = (i % 3) + 1;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "BK-2026-" + (100 + i));
            row.put("customer", "Khach " + (i + 1));
            row.put("item", b.getTitle());
            row.put("value", (b.getPrice() == null ? 0d : b.getPrice()) * qty);
            row.put("status", statuses[i % statuses.length]);
            orders.add(row);
        }

        return orders;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Book> books = bookRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        double gmv = books.stream()
                .mapToDouble(b -> (b.getPrice() == null ? 0d : b.getPrice()) * (b.getStockQuantity() == null ? 0 : b.getStockQuantity()))
                .sum();

        Map<String, Long> categoryStats = books.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategory() != null && safe(b.getCategory().getName()).length() > 0 ? b.getCategory().getName() : "Chua phan loai",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> stockBuckets = books.stream()
                .collect(Collectors.groupingBy(
                        b -> stockBucket(b.getStockQuantity()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gmv", gmv);
        response.put("books", books.size());
        response.put("categories", categories.size());
        response.put("shops", buildShops(books).size());
        response.put("categoryStats", categoryStats);
        response.put("stockBuckets", stockBuckets);
        return response;
    }

    @GetMapping("/books")
    public List<Map<String, Object>> books(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "all") String stock
    ) {
        return bookRepository.findAll().stream()
                .filter(b -> containsIgnoreCase(b.getTitle(), q) || containsIgnoreCase(b.getAuthor(), q))
                .filter(b -> safe(category).isEmpty() || "all".equalsIgnoreCase(category)
                        || (b.getCategory() != null && category.equalsIgnoreCase(safe(b.getCategory().getName()))))
                .filter(b -> {
                    String bucket = stockBucket(b.getStockQuantity());
                    return "all".equalsIgnoreCase(stock) || bucket.equalsIgnoreCase(stock);
                })
                .sorted(Comparator.comparing(Book::getId))
                .map(b -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", b.getId());
                    row.put("title", b.getTitle());
                    row.put("author", b.getAuthor());
                    row.put("price", b.getPrice());
                    row.put("stock", b.getStockQuantity());
                    row.put("stockBucket", stockBucket(b.getStockQuantity()));
                    row.put("category", b.getCategory() != null ? b.getCategory().getName() : "Chua phan loai");
                    return row;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(defaultValue = "all") String status
    ) {
        List<Map<String, Object>> rows = buildUsers(bookRepository.findAll(), userRepository.findAll());

        return rows.stream()
                .filter(u -> containsIgnoreCase(String.valueOf(u.get("name")), q)
                        || containsIgnoreCase(String.valueOf(u.get("email")), q))
                .filter(u -> "all".equalsIgnoreCase(role) || String.valueOf(u.get("role")).equalsIgnoreCase(role))
                .filter(u -> "all".equalsIgnoreCase(status) || String.valueOf(u.get("status")).equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    @GetMapping("/shops")
    public List<Map<String, Object>> shops(@RequestParam(defaultValue = "") String q) {
        return buildShops(bookRepository.findAll()).stream()
                .filter(s -> containsIgnoreCase(String.valueOf(s.get("shopName")), q)
                        || containsIgnoreCase(String.valueOf(s.get("owner")), q)
                        || containsIgnoreCase(String.valueOf(s.get("legal")), q))
                .collect(Collectors.toList());
    }

    @GetMapping("/seller/orders")
    public List<Map<String, Object>> sellerOrders(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String status
    ) {
        return buildOrders(bookRepository.findAll()).stream()
                .filter(o -> containsIgnoreCase(String.valueOf(o.get("id")), q)
                        || containsIgnoreCase(String.valueOf(o.get("customer")), q)
                        || containsIgnoreCase(String.valueOf(o.get("item")), q))
                .filter(o -> "all".equalsIgnoreCase(status) || String.valueOf(o.get("status")).equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    @GetMapping("/seller/analytics")
    public Map<String, Object> sellerAnalytics() {
        List<Book> books = bookRepository.findAll();
        List<Map<String, Object>> orders = buildOrders(books);

        double estimatedRevenue = orders.stream()
                .mapToDouble(o -> (Double) o.get("value"))
                .sum();

        double averagePrice = books.stream()
                .mapToDouble(b -> b.getPrice() == null ? 0d : b.getPrice())
                .average()
                .orElse(0d);

        long lowStock = books.stream().filter(b -> "low".equals(stockBucket(b.getStockQuantity()))).count();

        Map<String, Long> categoryCounts = books.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategory() != null && safe(b.getCategory().getName()).length() > 0 ? b.getCategory().getName() : "Chua phan loai",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Double> categoryRevenue = books.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategory() != null && safe(b.getCategory().getName()).length() > 0 ? b.getCategory().getName() : "Chua phan loai",
                        LinkedHashMap::new,
                        Collectors.summingDouble(b -> b.getPrice() == null ? 0d : b.getPrice())
                ));

        Map<String, Long> orderStatusCounts = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> String.valueOf(o.get("status")),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("estimatedRevenue", estimatedRevenue);
        response.put("averagePrice", averagePrice);
        response.put("bookCount", books.size());
        response.put("lowStock", lowStock);
        response.put("categoryCounts", categoryCounts);
        response.put("categoryRevenue", categoryRevenue);
        response.put("orderStatusCounts", orderStatusCounts);
        return response;
    }

        /**
         * Lightweight dashboard summary for admin (orders counts, revenue, new users, low-stock)
         */
        @GetMapping("/dashboard")
        public Map<String, Object> dashboardSummary() {
                Map<String, Object> resp = new LinkedHashMap<>();

                List<Order> orders = orderRepository.findAll();
                List<User> users = userRepository.findAll();
                List<Book> books = bookRepository.findAll();

                // total orders
                resp.put("ordersCount", orders.size());

                // revenue (sum of totalAmount)
                double revenue = orders.stream()
                                .mapToDouble(o -> o.getTotalAmount() == null ? 0d : o.getTotalAmount())
                                .sum();
                resp.put("revenue", revenue);

                        // new users (best-effort): if User has createdAt field, count those; otherwise return total users
                        long newUsers;
                        try {
                                newUsers = users.stream().filter(u -> {
                                        try {
                                                java.lang.reflect.Field f = u.getClass().getDeclaredField("createdAt");
                                                f.setAccessible(true);
                                                Object val = f.get(u);
                                                if (val instanceof java.time.temporal.TemporalAccessor) return true; // best-effort
                                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
                                        return false;
                                }).count();
                                if (newUsers == 0) newUsers = users.size();
                        } catch (Exception ex) {
                                newUsers = users.size();
                        }
                resp.put("newUsers", newUsers);

                // low stock books (<20)
                        long lowStock = books.stream().filter(b -> {
                                Integer qty = b.getStockQuantity();
                                return qty != null && qty < 20;
                        }).count();
                resp.put("lowStock", lowStock);

                return resp;
        }

    /**
     * 🆕 NEW: Lấy danh sách người dùng chi tiết với trạng thái active/lock
     * GET /api/panel/users-detailed?q=&role=all&status=all
     */
    @GetMapping("/users-detailed")
    public List<Map<String, Object>> getUsersDetailed(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return userRepository.findAll().stream()
                .filter(u -> q == null || q.isEmpty() || 
                        lower(u.getUsername()).contains(lower(q)))
                .filter(u -> role == null || role.equals("all") || 
                        u.getRole().toString().equalsIgnoreCase(role))
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
                .collect(Collectors.toList());
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
                    map.put("category", b.getCategory() != null ? b.getCategory().getName() : "N/A");
                    map.put("stock", b.getStockQuantity());
                    map.put("approvalStatus", b.getApprovalStatus().toString());
                    map.put("active", b.isActive() ? "Active" : "Locked");
                    map.put("seller", b.getSeller() != null ? b.getSeller().getUsername() : "System");
                    return map;
                })
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        
        List<Map<String, Object>> paged = (start > filtered.size()) 
            ? new ArrayList<>() 
            : filtered.subList(start, end);

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
                    map.put("buyer", o.getBuyer() != null ? o.getBuyer().getUsername() : "Unknown");
                    map.put("total", o.getTotalAmount());
                    map.put("address", o.getShippingAddress());
                    map.put("date", o.getCreatedAt());
                    // Get first sub-order status as main status
                    String orderStatus = o.getSubOrders() == null || o.getSubOrders().isEmpty()
                            ? "N/A"
                            : o.getSubOrders().get(0).getStatus().toString();
                    map.put("status", orderStatus);
                    return map;
                })
                .filter(m -> q == null || q.isEmpty() || 
                        String.valueOf(m.get("id")).contains(q) || 
                        lower(String.valueOf(m.get("buyer"))).contains(lower(q)))
                .filter(m -> status == null || status.equals("all") || 
                        String.valueOf(m.get("status")).equals(status))
                // Note: Date filtering can be added here if needed, 
                // but for now keeping it simple as per initial requirements
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        
        List<Map<String, Object>> paged = (start > filtered.size()) 
            ? new ArrayList<>() 
            : filtered.subList(start, end);

        return new PageImpl<>(paged, pageable, filtered.size());
    }
}
