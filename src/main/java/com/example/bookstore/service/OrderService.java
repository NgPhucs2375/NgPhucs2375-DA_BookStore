package com.example.bookstore.service;

import com.example.bookstore.dto.*;
import com.example.bookstore.model.*;
import com.example.bookstore.model.enums.*;
import com.example.bookstore.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;
    private final SubOrderStatusHistoryRepository statusHistoryRepository;
    private final NotificationService notificationService;
    private final VoucherService voucherService;

    // ========================================================================
    // CHECKOUT
    // ========================================================================

    @Transactional
    public CheckoutResponse checkoutFromCart(CheckoutRequest request) {
        return checkoutInternal(request.getBuyerId(), request.getShippingAddress(), null);
    }

    @Transactional
    public CheckoutResponse checkoutFromCurrentBuyer(Long buyerId, String shippingAddress, String voucherCode) {
        return checkoutInternal(buyerId, shippingAddress, voucherCode);
    }

    private CheckoutResponse checkoutInternal(Long buyerId, String shippingAddress, String voucherCode) {
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
                    .paymentStatus(PaymentStatus.UNPAID)
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

                // Kiểm tra stock tại thời điểm đặt hàng (chưa trừ)
                if (book.getStockQuantity() == null || quantity > book.getStockQuantity()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm \"" + book.getTitle() + "\" không đủ hàng. Còn: " + book.getStockQuantity());
                }

                double unitPrice = book.getPrice() == null ? 0.0 : book.getPrice();
                subTotal += unitPrice * quantity;

                OrderItem orderItem = OrderItem.builder()
                        .subOrder(subOrder)
                        .book(book)
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .stockDeducted(false) // Chưa trừ stock
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

        // Apply Voucher if provided
        Voucher appliedVoucher = null;
        Double discountAmount = 0.0;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            appliedVoucher = voucherService.validateVoucher(voucherCode, buyer, orderTotal);
            discountAmount = voucherService.applyVoucher(voucherCode, buyer, orderTotal);
            order.setTotalAmount(orderTotal - discountAmount);
        }

        Order saved = orderRepository.save(order);

        // Ghi log trạng thái ban đầu cho mỗi sub_order
        if (saved.getSubOrders() != null) {
            for (SubOrder so : saved.getSubOrders()) {
                logStatusChange(so, null, OrderStatus.PENDING_PAYMENT, buyer, ChangedByRole.SYSTEM,
                    "Đơn hàng được tạo từ giỏ hàng");
            }
        }

        // Record Voucher Usage
        if (appliedVoucher != null) {
            voucherService.useVoucher(appliedVoucher, buyer, saved, discountAmount);
        }

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

    // ========================================================================
    // BUYER ORDER QUERIES
    // ========================================================================

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
    public List<OrderSummaryResponse> getCurrentBuyerOrderSummaries(Long buyerId) {
        return getBuyerOrders(buyerId).stream()
            .map(this::toOrderSummary)
            .collect(Collectors.toList());
    }

    // ========================================================================
    // BUYER ORDER CANCELLATION
    // ========================================================================

    @Transactional
    public OrderSummaryResponse cancelCurrentBuyerOrder(Long buyerId, Long orderId) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a buyer");
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getBuyer() == null || !buyerId.equals(order.getBuyer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyer cannot modify this order");
        }

        if (!canBuyerCancelOrder(order)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Chỉ có thể hủy đơn hàng ở trạng thái chờ xử lý");
        }

        if (order.getSubOrders() != null) {
            for (SubOrder subOrder : order.getSubOrders()) {
                cancelSubOrder(subOrder, buyer, ChangedByRole.BUYER, "Người mua yêu cầu hủy đơn hàng #" + orderId);
            }
            subOrderRepository.saveAll(order.getSubOrders());
        }

        return toOrderSummary(order);
    }

    // ========================================================================
    // BUYER ORDER DETAIL
    // ========================================================================

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

    // ========================================================================
    // SELLER SUB-ORDER QUERIES
    // ========================================================================

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

    /**
     * Lấy chi tiết đầy đủ của một sub-order cho seller
     */
    @Transactional
    public SubOrderDetailResponse getSellerSubOrderDetail(Long sellerId, Long subOrderId) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        SubOrder subOrder = subOrderRepository.findById(subOrderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub order not found"));

        if (subOrder.getSeller() == null || !sellerId.equals(subOrder.getSeller().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller cannot access this sub order");
        }

        return toSubOrderDetail(subOrder);
    }

    // ========================================================================
    // SELLER STATUS UPDATE - STATE MACHINE
    // ========================================================================

    /**
     * Cập nhật trạng thái sub-order cho seller với state machine validation.
     * 
     * Flow hợp lệ:
        *   PENDING_PAYMENT -> PROCESSING (đã xác nhận, trừ stock)
        *   PROCESSING -> SHIPPING (đang giao)
     *   SHIPPING -> COMPLETED (hoàn thành)
     *   PROCESSING -> CANCELLED (hủy đơn, hoàn stock, hoàn tiền nếu đã thanh toán)
     *   PENDING_PAYMENT -> CANCELLED (hủy đơn)
     */
    @Transactional
    public SubOrderSummaryResponse updateSubOrderStatusForSeller(Long sellerId, Long subOrderId,
                                                                   SubOrderStatusUpdateRequest request) {
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

        if (subOrder.getStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Seller không thể chỉnh sửa đơn hàng đã hoàn thành");
        }

        OrderStatus newStatus = request.getStatus();
        OrderStatus currentStatus = subOrder.getStatus();

        // Validate state transition
        validateStatusTransition(currentStatus, newStatus, "SELLER");

        // Execute transition with side effects
        executeStatusTransition(subOrder, newStatus, seller, ChangedByRole.SELLER, request.getNote());

        SubOrder saved = subOrderRepository.save(subOrder);

        // Send notification to buyer
        sendStatusChangeNotification(saved, seller);

        return toSubOrderSummary(saved);
    }

    /**
     * Seller hủy sub-order (chỉ khi đang ở PROCESSING hoặc PENDING_PAYMENT)
     */
    @Transactional
    public SubOrderSummaryResponse cancelSubOrderBySeller(Long sellerId, Long subOrderId, String reason) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        SubOrder subOrder = subOrderRepository.findById(subOrderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub order not found"));

        if (subOrder.getSeller() == null || !sellerId.equals(subOrder.getSeller().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller cannot cancel this sub order");
        }

        OrderStatus currentStatus = subOrder.getStatus();
        if (currentStatus != OrderStatus.PENDING_PAYMENT && currentStatus != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Chỉ có thể hủy đơn ở trạng thái chờ xử lý hoặc đang xử lý");
        }

        cancelSubOrder(subOrder, seller, ChangedByRole.SELLER,
            reason != null ? reason : "Người bán hủy đơn hàng #" + subOrder.getParentOrder().getId());

        SubOrder saved = subOrderRepository.save(subOrder);

        sendStatusChangeNotification(saved, seller);

        return toSubOrderSummary(saved);
    }

    // ========================================================================
    // REFUND PROCESSING
    // ========================================================================

    /**
     * Xử lý hoàn tiền cho sub-order đã hủy (nếu đã thanh toán)
     */
    @Transactional
    public SubOrderSummaryResponse processRefund(Long sellerId, Long subOrderId, RefundRequest request) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        SubOrder subOrder = subOrderRepository.findById(subOrderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub order not found"));

        if (subOrder.getSeller() == null || !sellerId.equals(subOrder.getSeller().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller cannot refund this sub order");
        }

        if (subOrder.getStatus() != OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Chỉ có thể hoàn tiền cho đơn đã hủy");
        }

        if (subOrder.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Đơn này chưa được thanh toán, không cần hoàn tiền");
        }

        if (subOrder.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Đơn này đã được hoàn tiền trước đó");
        }

        Double refundAmount = request.getRefundAmount();
        if (refundAmount == null || refundAmount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền hoàn không hợp lệ");
        }

        if (refundAmount > subOrder.getSubTotal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Số tiền hoàn không thể lớn hơn tổng giá trị đơn");
        }

        subOrder.setRefundAmount(refundAmount);
        subOrder.setRefundReason(request.getReason());
        subOrder.setRefundedAt(LocalDateTime.now());

        if (refundAmount >= subOrder.getSubTotal()) {
            subOrder.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            subOrder.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }

        SubOrder saved = subOrderRepository.save(subOrder);

        // Ghi log hoàn tiền
        logStatusChange(saved, saved.getStatus(), saved.getStatus(), seller, ChangedByRole.SELLER,
            "Hoàn tiền: " + refundAmount + "đ. Lý do: " + (request.getReason() != null ? request.getReason() : ""));

        return toSubOrderSummary(saved);
    }

    // ========================================================================
    // STATUS HISTORY
    // ========================================================================

    /**
     * Lấy lịch sử thay đổi trạng thái của một sub-order
     */
    @Transactional
    public List<SubOrderDetailResponse.StatusHistoryResponse> getSubOrderStatusHistory(Long sellerId, Long subOrderId) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        SubOrder subOrder = subOrderRepository.findById(subOrderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub order not found"));

        if (subOrder.getSeller() == null || !sellerId.equals(subOrder.getSeller().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller cannot access this sub order");
        }

        return statusHistoryRepository.findBySubOrderIdOrderByCreatedAtDesc(subOrderId)
            .stream()
            .map(h -> SubOrderDetailResponse.StatusHistoryResponse.builder()
                .historyId(h.getId())
                .fromStatus(h.getFromStatus() != null ? h.getFromStatus().name() : null)
                .toStatus(h.getToStatus().name())
                .changedBy(h.getChangedBy() != null ? h.getChangedBy().getUsername() : null)
                .changedByRole(h.getChangedByRole() != null ? h.getChangedByRole().name() : null)
                .note(h.getNote())
                .createdAt(h.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }

    // ========================================================================
    // SELLER ANALYTICS
    // ========================================================================

    @Transactional
    public SellerAnalyticsResponse getSellerAnalytics(Long sellerId, int days) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);

        List<SubOrder> subOrders = subOrderRepository.findBySellerAndDateRange(seller, start, end);

        List<SubOrder> completedSubOrders = subOrders.stream()
                .filter(so -> so.getStatus() == OrderStatus.COMPLETED)
                .toList();

        double totalRevenue = completedSubOrders.stream()
                .mapToDouble(SubOrder::getSubTotal)
                .sum();

        long completedCount = completedSubOrders.size();
        double avgOrderValue = completedCount > 0 ? totalRevenue / completedCount : 0.0;
        double completionRate = subOrders.isEmpty() ? 0.0 : (double) completedCount / subOrders.size() * 100.0;

        long soldUnits = completedSubOrders.stream()
                .flatMap(so -> so.getItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();

        Map<Book, Long> productSales = completedSubOrders.stream()
                .flatMap(so -> so.getItems().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::getBook,
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        Map<Book, Double> productRevenue = completedSubOrders.stream()
                .flatMap(so -> so.getItems().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::getBook,
                        Collectors.summingDouble(item -> item.getUnitPrice() * item.getQuantity())
                ));

        List<SellerAnalyticsResponse.ProductPerformance> topSellingProducts = productSales.entrySet().stream()
                .sorted(Map.Entry.<Book, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Book book = entry.getKey();
                    long units = entry.getValue();
                    double revenue = productRevenue.getOrDefault(book, 0.0);
                    return SellerAnalyticsResponse.ProductPerformance.builder()
                            .bookId(book.getId())
                            .title(book.getTitle())
                            .imageUrl(book.getImageUrl())
                            .stockQuantity(book.getStockQuantity())
                            .soldUnits(units)
                            .revenue(revenue)
                            .progressPercent(0.0)
                            .build();
                })
                .toList();

        List<SellerAnalyticsResponse.StockAlert> lowStockProducts = bookRepository.findBySeller(seller).stream()
                .filter(book -> book.getStockQuantity() != null && book.getStockQuantity() < 10)
                .sorted((b1, b2) -> b1.getStockQuantity().compareTo(b2.getStockQuantity()))
                .limit(5)
                .map(book -> SellerAnalyticsResponse.StockAlert.builder()
                        .bookId(book.getId())
                        .title(book.getTitle())
                        .imageUrl(book.getImageUrl())
                        .stockQuantity(book.getStockQuantity())
                        .soldUnits(productSales.getOrDefault(book, 0L))
                        .needReorder(book.getStockQuantity() < 5)
                        .note(book.getStockQuantity() < 5 ? "Critical low stock" : "Low stock")
                        .build())
                .toList();

        List<SellerAnalyticsResponse.TransactionRow> recentTransactions = subOrders.stream()
                .sorted((so1, so2) -> {
                    LocalDateTime d1 = so1.getParentOrder() != null ? so1.getParentOrder().getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime d2 = so2.getParentOrder() != null ? so2.getParentOrder().getCreatedAt() : LocalDateTime.MIN;
                    return d2.compareTo(d1);
                })
                .limit(10)
                .flatMap(so -> so.getItems().stream().map(item -> SellerAnalyticsResponse.TransactionRow.builder()
                        .transactionId("TRX-" + so.getId() + "-" + item.getId())
                        .orderId(so.getParentOrder() != null ? so.getParentOrder().getId() : null)
                        .subOrderId(so.getId())
                        .createdAt(so.getParentOrder() != null ? so.getParentOrder().getCreatedAt() : null)
                        .customerName(so.getParentOrder() != null && so.getParentOrder().getBuyer() != null ? so.getParentOrder().getBuyer().getUsername() : "Unknown")
                        .bookId(item.getBook() != null ? item.getBook().getId() : null)
                        .productName(item.getBook() != null ? item.getBook().getTitle() : "Unknown")
                        .quantity(item.getQuantity())
                        .amount(item.getUnitPrice() * item.getQuantity())
                        .paymentMethod("COD")
                        .build()))
                .limit(10)
                .toList();

        return SellerAnalyticsResponse.builder()
                .sellerId(seller.getId())
                .sellerName(seller.getShopName() != null ? seller.getShopName() : seller.getUsername())
                .days(days)
                .periodLabel("Last " + days + " Days")
                .generatedAt(LocalDateTime.now())
                .totalRevenue(totalRevenue)
                .completedOrders(completedCount)
                .averageOrderValue(avgOrderValue)
                .completionRate(completionRate)
                .soldUnits(soldUnits)
                .topSellingProducts(topSellingProducts)
                .lowStockProducts(lowStockProducts)
                .recentTransactions(recentTransactions)
                .revenueTimeline(new ArrayList<>())
                .categoryRevenue(new ArrayList<>())
                .build();
    }

    // ========================================================================
    // FILTERING & SEARCH
    // ========================================================================

    @Transactional
    public OrderFilterResponse filterBuyerOrders(Long buyerId, OrderFilterRequest filter) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a buyer");
        }

        int page = filter.getPage() != null ? filter.getPage() : 0;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        if (page < 0) page = 0;
        if (pageSize <= 0 || pageSize > 100) pageSize = 10;

        Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection())
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        Page<Order> pageResult = orderRepository.findByBuyerWithFilters(
            buyer,
            filter.getCreatedFrom(),
            filter.getCreatedTo(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            pageable
        );

        List<OrderSummaryResponse> summaries = pageResult.getContent().stream()
            .map(this::toOrderSummary)
            .collect(Collectors.toList());

        return OrderFilterResponse.builder()
            .orders(summaries)
            .totalCount(pageResult.getTotalElements())
            .currentPage(page)
            .pageSize(pageSize)
            .totalPages(pageResult.getTotalPages())
            .build();
    }

    @Transactional
    public SubOrderFilterResponse filterSellerSubOrders(Long sellerId, SubOrderFilterRequest filter) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        int page = filter.getPage() != null ? filter.getPage() : 0;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        if (page < 0) page = 0;
        if (pageSize <= 0 || pageSize > 100) pageSize = 10;

        Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection())
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "id";
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        Page<SubOrder> pageResult = subOrderRepository.findBySellerWithFilters(
            seller,
            filter.getStatus(),
            filter.getCreatedFrom(),
            filter.getCreatedTo(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            pageable
        );

        List<SubOrderSummaryResponse> summaries = pageResult.getContent().stream()
            .map(this::toSubOrderSummary)
            .collect(Collectors.toList());

        return SubOrderFilterResponse.builder()
            .subOrders(summaries)
            .totalCount(pageResult.getTotalElements())
            .currentPage(page)
            .pageSize(pageSize)
            .totalPages(pageResult.getTotalPages())
            .build();
    }

    @Transactional
    public List<SubOrderSummaryResponse> searchSellerSubOrdersByBuyer(Long sellerId, String buyerName) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        if (buyerName == null || buyerName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer name cannot be empty");
        }

        return subOrderRepository.findBySellerAndBuyerNameContaining(seller, buyerName.trim())
            .stream()
            .map(this::toSubOrderSummary)
            .collect(Collectors.toList());
    }

    public List<OrderSummaryResponse> getBuyerOrdersByStatus(Long buyerId, OrderStatus status) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a buyer");
        }

        List<Order> orders = orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);

        return orders.stream()
            .filter(order -> hasOrderStatus(order, status))
            .map(this::toOrderSummary)
            .collect(Collectors.toList());
    }

    public List<SubOrderSummaryResponse> getSellerSubOrdersByStatus(Long sellerId, OrderStatus status) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a seller");
        }

        return subOrderRepository.findBySellerAndStatusOrdered(seller, status)
            .stream()
            .map(this::toSubOrderSummary)
            .collect(Collectors.toList());
    }

    // ========================================================================
    // PRIVATE HELPERS - STATE MACHINE & SIDE EFFECTS
    // ========================================================================

    /**
     * Validate state transition based on current status and requester role.
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus target, String role) {
        switch (current) {
            case PENDING_PAYMENT:
                if (target != OrderStatus.PROCESSING && target != OrderStatus.CANCELLED) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đơn chờ xác nhận chỉ có thể chuyển sang Đã xác nhận hoặc Hủy");
                }
                break;
            case PROCESSING:
                if (target != OrderStatus.SHIPPING && target != OrderStatus.CANCELLED) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đơn đã xác nhận chỉ có thể chuyển sang Đang giao hoặc Hủy");
                }
                break;
            case SHIPPING:
                if (target != OrderStatus.COMPLETED) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đơn đang giao chỉ có thể chuyển sang Hoàn thành");
                }
                break;
            case COMPLETED:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn đã hoàn thành không thể thay đổi trạng thái");
            case CANCELLED:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn đã hủy không thể thay đổi trạng thái");
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trạng thái không hợp lệ: " + current);
        }
    }

    /**
     * Execute status transition with side effects (stock, timestamps, logging).
     */
    private void executeStatusTransition(SubOrder subOrder, OrderStatus newStatus,
                                          User changedBy, ChangedByRole role, String note) {
        OrderStatus oldStatus = subOrder.getStatus();
        subOrder.setStatus(newStatus);

        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case PROCESSING:
                subOrder.setConfirmedAt(now);
                // Trừ stock khi xác nhận đơn
                deductStock(subOrder);
                break;
            case SHIPPING:
                subOrder.setShippedAt(now);
                break;
            case COMPLETED:
                subOrder.setCompletedAt(now);
                break;
            case CANCELLED:
                subOrder.setCancelledAt(now);
                subOrder.setCancelledBy(role.name());
                // Hoàn stock nếu đã trừ
                restoreStock(subOrder);
                // Xử lý hoàn tiền nếu đã thanh toán
                if (subOrder.getPaymentStatus() == PaymentStatus.PAID) {
                    subOrder.setRefundAmount(subOrder.getSubTotal());
                    subOrder.setRefundReason("Hoàn tiền tự động do hủy đơn");
                    subOrder.setRefundedAt(now);
                    subOrder.setPaymentStatus(PaymentStatus.REFUNDED);
                }
                break;
            default:
                break;
        }

        // Ghi log thay đổi trạng thái
        logStatusChange(subOrder, oldStatus, newStatus, changedBy, role, note);
    }

    /**
     * Hủy sub-order với đầy đủ side effects
     */
    private void cancelSubOrder(SubOrder subOrder, User changedBy, ChangedByRole role, String reason) {
        OrderStatus oldStatus = subOrder.getStatus();
        subOrder.setStatus(OrderStatus.CANCELLED);
        subOrder.setCancelledAt(LocalDateTime.now());
        subOrder.setCancelledBy(role.name());

        // Hoàn stock nếu đã trừ
        restoreStock(subOrder);

        // Xử lý hoàn tiền nếu đã thanh toán
        if (subOrder.getPaymentStatus() == PaymentStatus.PAID) {
            subOrder.setRefundAmount(subOrder.getSubTotal());
            subOrder.setRefundReason(reason);
            subOrder.setRefundedAt(LocalDateTime.now());
            subOrder.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        // Ghi log
        logStatusChange(subOrder, oldStatus, OrderStatus.CANCELLED, changedBy, role, reason);
    }

    /**
     * Trừ stock khi xác nhận đơn (PROCESSING)
     */
    private void deductStock(SubOrder subOrder) {
        if (subOrder.getItems() == null) return;

        for (OrderItem item : subOrder.getItems()) {
            if (Boolean.TRUE.equals(item.getStockDeducted())) {
                continue; // Đã trừ rồi, không trừ lại
            }

            Book book = item.getBook();
            if (book == null) continue;

            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantity <= 0) continue;

            Integer currentStock = book.getStockQuantity();
            if (currentStock == null || currentStock < quantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sản phẩm \"" + book.getTitle() + "\" không đủ hàng. Còn: " +
                    (currentStock != null ? currentStock : 0) + ", cần: " + quantity);
            }

            book.setStockQuantity(currentStock - quantity);
            bookRepository.save(book);

            item.setStockDeducted(true);
            item.setStockDeductedAt(LocalDateTime.now());
        }
    }

    /**
     * Hoàn stock khi hủy đơn (nếu đã trừ)
     */
    private void restoreStock(SubOrder subOrder) {
        if (subOrder.getItems() == null) return;

        for (OrderItem item : subOrder.getItems()) {
            if (!Boolean.TRUE.equals(item.getStockDeducted())) {
                continue; // Chưa trừ, không cần hoàn
            }

            Book book = item.getBook();
            if (book == null) continue;

            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantity <= 0) continue;

            Integer currentStock = book.getStockQuantity();
            book.setStockQuantity((currentStock != null ? currentStock : 0) + quantity);
            bookRepository.save(book);

            item.setStockDeducted(false);
            item.setStockDeductedAt(null);
        }
    }

    /**
     * Ghi log thay đổi trạng thái
     */
    private void logStatusChange(SubOrder subOrder, OrderStatus fromStatus, OrderStatus toStatus,
                                  User changedBy, ChangedByRole role, String note) {
        SubOrderStatusHistory history = SubOrderStatusHistory.builder()
            .subOrder(subOrder)
            .fromStatus(fromStatus)
            .toStatus(toStatus)
            .changedBy(changedBy)
            .changedByRole(role)
            .note(note)
            .createdAt(LocalDateTime.now())
            .build();
        statusHistoryRepository.save(history);
    }

    /**
     * Gửi thông báo cho buyer khi trạng thái sub-order thay đổi
     */
    private void sendStatusChangeNotification(SubOrder subOrder, User seller) {
        try {
            Order parentOrder = subOrder.getParentOrder();
            if (parentOrder == null || parentOrder.getBuyer() == null) return;

            User buyer = parentOrder.getBuyer();
            String message = "Đơn hàng #" + parentOrder.getId() +
                " (mã shop: " + subOrder.getId() + ") đã chuyển sang trạng thái: " +
                getStatusLabel(subOrder.getStatus());

            NotificationCreateRequest req = NotificationCreateRequest.builder()
                    .type(NotificationType.SUB_ORDER_STATUS_CHANGED)
                    .title("Cập nhật đơn hàng")
                    .message(message)
                    .build();

            notificationService.createNotification(seller.getId(), buyer.getId(), req);
        } catch (Exception e) {
            // Không throw exception nếu gửi notification lỗi
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra buyer có thể hủy order không
     */
    private boolean canBuyerCancelOrder(Order order) {
        if (order.getSubOrders() == null || order.getSubOrders().isEmpty()) return false;

        // Buyer chỉ có thể hủy nếu tất cả sub-orders đều ở PENDING_PAYMENT
        return order.getSubOrders().stream()
            .allMatch(so -> so.getStatus() == OrderStatus.PENDING_PAYMENT);
    }

    /**
     * Kiểm tra order có sub-order với status cụ thể không
     */
    private boolean hasOrderStatus(Order order, OrderStatus status) {
        if (order.getSubOrders() == null) return false;
        return order.getSubOrders().stream().anyMatch(so -> so.getStatus() == status);
    }

    // ========================================================================
    // PRIVATE HELPERS - DTO CONVERSION
    // ========================================================================

    private OrderSummaryResponse toOrderSummary(Order order) {
        if (order == null) return null;

        int totalItems = 0;
        int subOrderCount = 0;
        double totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;

        if (order.getSubOrders() != null) {
            subOrderCount = order.getSubOrders().size();
            for (SubOrder so : order.getSubOrders()) {
                if (so.getItems() != null) {
                    totalItems += so.getItems().stream()
                        .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                        .sum();
                }
            }
        }

        return OrderSummaryResponse.builder()
            .orderId(order.getId())
            .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
            .buyerUsername(order.getBuyer() != null ? order.getBuyer().getUsername() : null)
            .shippingAddress(order.getShippingAddress())
            .totalAmount(totalAmount)
            .createdAt(order.getCreatedAt())
            .subOrderCount(subOrderCount)
            .totalItems(totalItems)
            .build();
    }

    private SubOrderSummaryResponse toSubOrderSummary(SubOrder subOrder) {
        if (subOrder == null) return null;

        Order parentOrder = subOrder.getParentOrder();
        User buyer = parentOrder != null ? parentOrder.getBuyer() : null;
        User seller = subOrder.getSeller();

        int itemCount = 0;
        if (subOrder.getItems() != null) {
            itemCount = subOrder.getItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        }

        return SubOrderSummaryResponse.builder()
            .subOrderId(subOrder.getId())
            .orderId(parentOrder != null ? parentOrder.getId() : null)
            .sellerId(seller != null ? seller.getId() : null)
            .sellerName(seller != null ? (seller.getShopName() != null ? seller.getShopName() : seller.getUsername()) : null)
            .buyerId(buyer != null ? buyer.getId() : null)
            .buyerUsername(buyer != null ? buyer.getUsername() : null)
            .shippingAddress(parentOrder != null ? parentOrder.getShippingAddress() : null)
            .status(subOrder.getStatus())
            .paymentStatus(subOrder.getPaymentStatus())
            .subTotal(subOrder.getSubTotal())
            .refundAmount(subOrder.getRefundAmount())
            .refundReason(subOrder.getRefundReason())
            .refundedAt(subOrder.getRefundedAt())
            .confirmedAt(subOrder.getConfirmedAt())
            .shippedAt(subOrder.getShippedAt())
            .completedAt(subOrder.getCompletedAt())
            .cancelledAt(subOrder.getCancelledAt())
            .cancelledBy(subOrder.getCancelledBy())
            .createdAt(parentOrder != null ? parentOrder.getCreatedAt() : null)
            .itemCount(itemCount)
            .build();
    }

    private SubOrderDetailResponse toSubOrderDetail(SubOrder subOrder) {
        if (subOrder == null) return null;

        Order parentOrder = subOrder.getParentOrder();
        User buyer = parentOrder != null ? parentOrder.getBuyer() : null;
        User seller = subOrder.getSeller();

        // Convert items
        List<SubOrderDetailResponse.SubOrderItemResponse> itemResponses = new ArrayList<>();
        int itemCount = 0;
        if (subOrder.getItems() != null) {
            for (OrderItem item : subOrder.getItems()) {
                Book book = item.getBook();
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0.0;
                itemCount += qty;

                itemResponses.add(SubOrderDetailResponse.SubOrderItemResponse.builder()
                    .itemId(item.getId())
                    .bookId(book != null ? book.getId() : null)
                    .title(book != null ? book.getTitle() : null)
                    .author(book != null ? book.getAuthor() : null)
                    .imageUrl(book != null ? book.getImageUrl() : null)
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .lineTotal(unitPrice * qty)
                    .stockDeducted(item.getStockDeducted())
                    .build());
            }
        }

        // Convert status history
        List<SubOrderDetailResponse.StatusHistoryResponse> historyResponses = new ArrayList<>();
        if (subOrder.getStatusHistories() != null) {
            historyResponses = subOrder.getStatusHistories().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(h -> SubOrderDetailResponse.StatusHistoryResponse.builder()
                    .historyId(h.getId())
                    .fromStatus(h.getFromStatus() != null ? h.getFromStatus().name() : null)
                    .toStatus(h.getToStatus().name())
                    .changedBy(h.getChangedBy() != null ? h.getChangedBy().getUsername() : null)
                    .changedByRole(h.getChangedByRole() != null ? h.getChangedByRole().name() : null)
                    .note(h.getNote())
                    .createdAt(h.getCreatedAt())
                    .build())
                .collect(Collectors.toList());
        }

        return SubOrderDetailResponse.builder()
            .subOrderId(subOrder.getId())
            .orderId(parentOrder != null ? parentOrder.getId() : null)
            .sellerId(seller != null ? seller.getId() : null)
            .sellerName(seller != null ? (seller.getShopName() != null ? seller.getShopName() : seller.getUsername()) : null)
            .buyerId(buyer != null ? buyer.getId() : null)
            .buyerUsername(buyer != null ? buyer.getUsername() : null)
            .shippingAddress(parentOrder != null ? parentOrder.getShippingAddress() : null)
            .status(subOrder.getStatus())
            .paymentStatus(subOrder.getPaymentStatus())
            .subTotal(subOrder.getSubTotal())
            .refundAmount(subOrder.getRefundAmount())
            .refundReason(subOrder.getRefundReason())
            .refundedAt(subOrder.getRefundedAt())
            .confirmedAt(subOrder.getConfirmedAt())
            .shippedAt(subOrder.getShippedAt())
            .completedAt(subOrder.getCompletedAt())
            .cancelledAt(subOrder.getCancelledAt())
            .cancelledBy(subOrder.getCancelledBy())
            .createdAt(parentOrder != null ? parentOrder.getCreatedAt() : null)
            .itemCount(itemCount)
            .items(itemResponses)
            .statusHistory(historyResponses)
            .build();
    }

    /**
     * Lấy nhãn tiếng Việt cho trạng thái
     */
    private String getStatusLabel(OrderStatus status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case PENDING_PAYMENT: return "Chờ xác nhận";
            case PROCESSING: return "Đã xác nhận";
            case SHIPPING: return "Đang giao";
            case COMPLETED: return "Hoàn thành";
            case CANCELLED: return "Đã hủy";
            default: return status.name();
        }
    }
}
