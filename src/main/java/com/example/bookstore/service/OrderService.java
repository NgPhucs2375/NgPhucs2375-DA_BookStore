package com.example.bookstore.service;

import com.example.bookstore.dto.CheckoutRequest;
import com.example.bookstore.dto.CheckoutResponse;
import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.dto.OrderItemDetailResponse;
import com.example.bookstore.dto.SellerAnalyticsResponse;
import com.example.bookstore.dto.SubOrderSummaryResponse;
import com.example.bookstore.model.*;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.SubOrderRepository;
import com.example.bookstore.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final BookRepository bookRepository;

    @Transactional
    public CheckoutResponse checkoutFromCart(CheckoutRequest request) {
        return checkoutInternal(request.getBuyerId(), request.getShippingAddress());
    }

    @Transactional
    public CheckoutResponse checkoutFromCurrentBuyer(Long buyerId, String shippingAddress) {
        return checkoutInternal(buyerId, shippingAddress);
    }

    private CheckoutResponse checkoutInternal(Long buyerId, String shippingAddress) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a buyer");
        }

        Cart cart = cartRepository.findByBuyerId(buyer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer has no cart"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Map<User, List<CartItem>> itemsBySeller = new LinkedHashMap<>();
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            if (book == null || book.getSeller() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book or seller data is invalid in cart");
            }
            itemsBySeller.computeIfAbsent(book.getSeller(), key -> new ArrayList<>()).add(item);
        }

        Order order = Order.builder()
                .buyer(buyer)
            .shippingAddress(shippingAddress)
                .totalAmount(0.0)
                .build();

        List<SubOrder> subOrders = new ArrayList<>();
        double orderTotal = 0.0;

        for (Map.Entry<User, List<CartItem>> entry : itemsBySeller.entrySet()) {
            User seller = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();

            SubOrder subOrder = SubOrder.builder()
                    .parentOrder(order)
                    .seller(seller)
                    .status(OrderStatus.PENDING_PAYMENT)
                    .subTotal(0.0)
                    .build();

            List<OrderItem> orderItems = new ArrayList<>();
            double subTotal = 0.0;

            for (CartItem cartItem : sellerItems) {
                Book book = cartItem.getBook();
                Integer quantity = cartItem.getQuantity();
                if (quantity == null || quantity <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cart item quantity");
                }

                if (book.getApprovalStatus() != ApprovalStatus.APPROVED) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart contains unapproved book");
                }

                if (book.getStockQuantity() == null || quantity > book.getStockQuantity()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity exceeds stock");
                }

                double unitPrice = book.getPrice() == null ? 0.0 : book.getPrice();
                subTotal += unitPrice * quantity;

                OrderItem orderItem = OrderItem.builder()
                        .subOrder(subOrder)
                        .book(book)
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .build();
                orderItems.add(orderItem);
            }

            subOrder.setSubTotal(subTotal);
            subOrder.setItems(orderItems);
            subOrders.add(subOrder);
            orderTotal += subTotal;
        }

        order.setTotalAmount(orderTotal);
        order.setSubOrders(subOrders);

        Order saved = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return CheckoutResponse.builder()
                .orderId(saved.getId())
                .buyerId(buyer.getId())
                .shippingAddress(saved.getShippingAddress())
                .totalAmount(saved.getTotalAmount())
                .subOrderCount(saved.getSubOrders() == null ? 0 : saved.getSubOrders().size())
                .build();
    }

    public List<Order> getBuyerOrders(Long buyerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a buyer");
        }

        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public List<Order> getCurrentBuyerOrders(Long buyerId) {
        return getBuyerOrders(buyerId);
    }

    @Transactional
    public OrderDetailResponse getCurrentBuyerOrderDetail(Long buyerId, Long orderId) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a buyer");
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getBuyer() == null || !buyerId.equals(order.getBuyer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyer cannot access this order");
        }

        List<OrderItemDetailResponse> items = new ArrayList<>();
        int totalItems = 0;

        if (order.getSubOrders() != null) {
            for (SubOrder subOrder : order.getSubOrders()) {
                User seller = subOrder.getSeller();
                String sellerName = seller == null
                    ? null
                    : (seller.getShopName() == null ? seller.getUsername() : seller.getShopName());

                if (subOrder.getItems() != null) {
                    for (OrderItem orderItem : subOrder.getItems()) {
                        Book book = orderItem.getBook();
                        int qty = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
                        double unitPrice = orderItem.getUnitPrice() == null ? 0.0 : orderItem.getUnitPrice();
                        double lineTotal = unitPrice * qty;
                        totalItems += qty;

                        items.add(OrderItemDetailResponse.builder()
                            .subOrderId(subOrder.getId())
                            .subOrderStatus(subOrder.getStatus())
                            .sellerId(seller == null ? null : seller.getId())
                            .sellerName(sellerName)
                            .bookId(book == null ? null : book.getId())
                            .title(book == null ? null : book.getTitle())
                            .author(book == null ? null : book.getAuthor())
                            .unitPrice(unitPrice)
                            .quantity(qty)
                            .lineTotal(lineTotal)
                            .build());
                    }
                }
            }
        }

        return OrderDetailResponse.builder()
            .orderId(order.getId())
            .buyerId(buyer.getId())
            .buyerUsername(buyer.getUsername())
            .shippingAddress(order.getShippingAddress())
            .totalAmount(order.getTotalAmount())
            .createdAt(order.getCreatedAt())
            .subOrderCount(order.getSubOrders() == null ? 0 : order.getSubOrders().size())
            .totalItems(totalItems)
            .items(items)
            .build();
    }

    @Transactional
    public List<SubOrderSummaryResponse> getSellerSubOrders(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        return subOrderRepository.findBySellerOrderByIdDesc(seller).stream()
            .map(this::toSubOrderSummary)
                .toList();
    }

    @Transactional
    public SubOrderSummaryResponse updateSubOrderStatusForSeller(Long sellerId, Long subOrderId, OrderStatus status) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub order not found"));

        if (subOrder.getSeller() == null || !sellerId.equals(subOrder.getSeller().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller cannot update this sub order");
        }

        subOrder.setStatus(status);
        SubOrder saved = subOrderRepository.save(subOrder);

        return toSubOrderSummary(saved);
    }

    @Transactional
    public SellerAnalyticsResponse getSellerAnalytics(Long sellerId, int days) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        int effectiveDays = Math.max(days, 1);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(effectiveDays - 1L);
        LocalDateTime cutoff = startDate.atStartOfDay();

        List<SubOrder> sellerSubOrders = subOrderRepository.findBySellerOrderByIdDesc(seller);
        List<SubOrder> scopedSubOrders = sellerSubOrders.stream()
                .filter(subOrder -> subOrder.getParentOrder() != null)
                .filter(subOrder -> subOrder.getParentOrder().getCreatedAt() != null)
                .filter(subOrder -> !subOrder.getParentOrder().getCreatedAt().isBefore(cutoff))
                .toList();

        List<SubOrder> completedSubOrders = scopedSubOrders.stream()
                .filter(subOrder -> subOrder.getStatus() == OrderStatus.COMPLETED)
                .toList();

        Map<LocalDate, RevenueBucket> revenueBuckets = new LinkedHashMap<>();
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            revenueBuckets.put(cursor, new RevenueBucket());
        }

        Map<String, CategoryBucket> categoryBuckets = new LinkedHashMap<>();
        Map<Long, ProductBucket> productBuckets = new HashMap<>();
        List<SellerAnalyticsResponse.TransactionRow> transactions = new ArrayList<>();

        for (SubOrder subOrder : completedSubOrders) {
            LocalDate bucketDate = subOrder.getParentOrder().getCreatedAt().toLocalDate();
            RevenueBucket revenueBucket = revenueBuckets.get(bucketDate);
            if (revenueBucket == null) {
                continue;
            }

            revenueBucket.orderCount += 1L;
            revenueBucket.revenue += safeAmount(subOrder.getSubTotal());

            User buyer = subOrder.getParentOrder().getBuyer();
            String customerName = buyer == null ? "Không rõ" : buyer.getUsername();

            List<OrderItem> items = subOrder.getItems() == null ? List.of() : subOrder.getItems();
            for (OrderItem item : items) {
                Book book = item.getBook();
                int quantity = safeQuantity(item.getQuantity());
                double amount = safeAmount(item.getUnitPrice()) * quantity;

                revenueBucket.soldUnits += quantity;

                String categoryName = resolveCategoryName(book);
                CategoryBucket categoryBucket = categoryBuckets.computeIfAbsent(categoryName, key -> new CategoryBucket());
                categoryBucket.revenue += amount;
                categoryBucket.soldUnits += quantity;

                if (book != null) {
                    ProductBucket productBucket = productBuckets.computeIfAbsent(book.getId(), key -> new ProductBucket(book));
                    productBucket.soldUnits += quantity;
                    productBucket.revenue += amount;
                }

                transactions.add(SellerAnalyticsResponse.TransactionRow.builder()
                        .transactionId("GD-" + subOrder.getId() + "-" + item.getId())
                        .orderId(subOrder.getParentOrder().getId())
                        .subOrderId(subOrder.getId())
                        .createdAt(subOrder.getParentOrder().getCreatedAt())
                        .customerName(customerName)
                        .bookId(book == null ? null : book.getId())
                        .productName(book == null ? "Không rõ" : book.getTitle())
                        .quantity(quantity)
                        .amount(amount)
                        .paymentMethod("COD")
                        .build());
            }
        }

        double totalRevenue = completedSubOrders.stream()
                .mapToDouble(subOrder -> safeAmount(subOrder.getSubTotal()))
                .sum();
        long completedOrders = completedSubOrders.size();
        long totalOrders = scopedSubOrders.size();
        long soldUnits = completedSubOrders.stream()
                .mapToLong(subOrder -> subOrder.getItems() == null ? 0L : subOrder.getItems().stream().mapToLong(item -> safeQuantity(item.getQuantity())).sum())
                .sum();
        double averageOrderValue = completedOrders == 0 ? 0.0 : totalRevenue / completedOrders;
        double completionRate = totalOrders == 0 ? 0.0 : (completedOrders * 100.0) / totalOrders;

        List<SellerAnalyticsResponse.RevenuePoint> revenueTimeline = revenueBuckets.entrySet().stream()
                .map(entry -> SellerAnalyticsResponse.RevenuePoint.builder()
                        .label(entry.getKey().format(DateTimeFormatter.ofPattern("dd/MM")))
                        .revenue(entry.getValue().revenue)
                        .orderCount(entry.getValue().orderCount)
                        .soldUnits(entry.getValue().soldUnits)
                        .build())
                .toList();

        double categoryRevenueTotal = categoryBuckets.values().stream()
                .mapToDouble(bucket -> bucket.revenue)
                .sum();

        List<SellerAnalyticsResponse.CategoryRevenue> categoryRevenue = categoryBuckets.entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue().revenue, left.getValue().revenue))
                .map(entry -> SellerAnalyticsResponse.CategoryRevenue.builder()
                        .categoryName(entry.getKey())
                        .revenue(entry.getValue().revenue)
                        .soldUnits(entry.getValue().soldUnits)
                        .sharePercent(categoryRevenueTotal == 0 ? 0.0 : (entry.getValue().revenue * 100.0) / categoryRevenueTotal)
                        .build())
                .toList();

        List<ProductBucket> sortedProductBuckets = productBuckets.values().stream()
                .sorted(Comparator.comparingLong(ProductBucket::getSoldUnits).reversed()
                        .thenComparing(Comparator.comparingDouble(ProductBucket::getRevenue).reversed()))
                .limit(5)
                .toList();

        long topSoldUnits = sortedProductBuckets.isEmpty() ? 0L : sortedProductBuckets.get(0).getSoldUnits();
        List<SellerAnalyticsResponse.ProductPerformance> topSellingProducts = sortedProductBuckets.stream()
                .map(bucket -> bucket.toResponse(topSoldUnits))
                .toList();

        List<Book> sellerBooks = bookRepository.findBySeller(seller);
        List<SellerAnalyticsResponse.StockAlert> lowStockProducts = sellerBooks.stream()
                .sorted(Comparator.comparing((Book book) -> safeStock(book.getStockQuantity()))
                        .thenComparing(book -> safeText(book.getTitle())))
                .limit(5)
                .map(book -> {
                    long soldForBook = productBuckets.containsKey(book.getId()) ? productBuckets.get(book.getId()).getSoldUnits() : 0L;
                    int stockQuantity = safeStock(book.getStockQuantity());
                    return SellerAnalyticsResponse.StockAlert.builder()
                            .bookId(book.getId())
                            .title(book.getTitle())
                            .imageUrl(book.getImageUrl())
                            .stockQuantity(stockQuantity)
                            .soldUnits(soldForBook)
                            .needReorder(stockQuantity <= 5)
                            .note(stockQuantity <= 5 ? "Cần nhập hàng" : "Theo dõi tồn kho")
                            .build();
                })
                .toList();

        List<SellerAnalyticsResponse.TransactionRow> recentTransactions = transactions.stream()
                .sorted(Comparator.comparing(SellerAnalyticsResponse.TransactionRow::getCreatedAt).reversed())
                .limit(10)
                .toList();

        return SellerAnalyticsResponse.builder()
                .sellerId(seller.getId())
                .sellerName(resolveSellerName(seller))
                .days(effectiveDays)
                .periodLabel(effectiveDays + " ngày gần nhất")
                .generatedAt(LocalDateTime.now())
                .totalRevenue(totalRevenue)
                .completedOrders(completedOrders)
                .averageOrderValue(averageOrderValue)
                .completionRate(completionRate)
                .soldUnits(soldUnits)
                .revenueTimeline(revenueTimeline)
                .categoryRevenue(categoryRevenue)
                .topSellingProducts(topSellingProducts)
                .lowStockProducts(lowStockProducts)
                .recentTransactions(recentTransactions)
                .build();
    }

    private double safeAmount(Double value) {
        return value == null ? 0.0 : value;
    }

    private int safeQuantity(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeStock(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String resolveSellerName(User seller) {
        if (seller == null) {
            return null;
        }
        return seller.getShopName() == null || seller.getShopName().isBlank()
                ? seller.getUsername()
                : seller.getShopName();
    }

    private String resolveCategoryName(Book book) {
        if (book == null || book.getCategory() == null || book.getCategory().getName() == null || book.getCategory().getName().isBlank()) {
            return "Chưa phân loại";
        }
        return book.getCategory().getName();
    }

    private static final class RevenueBucket {
        private double revenue = 0.0;
        private long orderCount = 0L;
        private long soldUnits = 0L;
    }

    private static final class CategoryBucket {
        private double revenue = 0.0;
        private long soldUnits = 0L;
    }

    private static final class ProductBucket {
        private final Book book;
        private long soldUnits = 0L;
        private double revenue = 0.0;

        private ProductBucket(Book book) {
            this.book = book;
        }

        private Long getSoldUnits() {
            return soldUnits;
        }

        private double getRevenue() {
            return revenue;
        }

        private SellerAnalyticsResponse.ProductPerformance toResponse(long topSoldUnits) {
            double progressPercent = topSoldUnits == 0 ? 0.0 : (soldUnits * 100.0) / topSoldUnits;
            return SellerAnalyticsResponse.ProductPerformance.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .imageUrl(book.getImageUrl())
                    .stockQuantity(book.getStockQuantity())
                    .soldUnits(soldUnits)
                    .revenue(revenue)
                    .progressPercent(progressPercent)
                    .build();
        }
    }

    private SubOrderSummaryResponse toSubOrderSummary(SubOrder subOrder) {
        User seller = subOrder.getSeller();
        String sellerName = seller == null ? null : (seller.getShopName() == null ? seller.getUsername() : seller.getShopName());

        User buyer = subOrder.getParentOrder() == null ? null : subOrder.getParentOrder().getBuyer();
        String buyerUsername = buyer == null ? null : buyer.getUsername();

        int itemCount = 0;
        List<String> titles = new ArrayList<>();
        List<OrderItem> items = subOrder.getItems();
        if (items != null) {
            for (OrderItem item : items) {
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                itemCount += qty;

                if (titles.size() < 3) {
                    Book book = item.getBook();
                    String title = book == null || book.getTitle() == null ? "Không rõ" : book.getTitle();
                    titles.add(title);
                }
            }
        }

        String itemSummary = titles.isEmpty() ? null : String.join(", ", titles);
        if (items != null && items.size() > titles.size()) {
            itemSummary = itemSummary + " ...";
        }

        return SubOrderSummaryResponse.builder()
                .subOrderId(subOrder.getId())
                .orderId(subOrder.getParentOrder() == null ? null : subOrder.getParentOrder().getId())
                .sellerId(seller == null ? null : seller.getId())
                .sellerName(sellerName)
                .buyerUsername(buyerUsername)
                .itemSummary(itemSummary)
                .itemCount(itemCount)
                .status(subOrder.getStatus())
                .subTotal(subOrder.getSubTotal())
                .build();
    }
}
