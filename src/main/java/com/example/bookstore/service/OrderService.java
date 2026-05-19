package com.example.bookstore.service;

import com.example.bookstore.dto.CheckoutRequest;
import com.example.bookstore.dto.CheckoutResponse;
import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.dto.SubOrderSummaryResponse;
import com.example.bookstore.dto.OrderFilterRequest;
import com.example.bookstore.dto.OrderFilterResponse;
import com.example.bookstore.dto.OrderSummaryResponse;
import com.example.bookstore.dto.SubOrderFilterRequest;
import com.example.bookstore.dto.SubOrderFilterResponse;
import com.example.bookstore.model.*;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.SubOrderRepository;
import com.example.bookstore.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final CouponService couponService;
    private final com.example.bookstore.service.NotificationService notificationService;

    @Transactional
    public CheckoutResponse checkoutFromCart(CheckoutRequest request) {
        return checkoutInternal(request.getBuyerId(), request.getShippingAddress(), request.getCouponCode());
    }

    @Transactional
    public CheckoutResponse checkoutFromCurrentBuyer(Long buyerId, String shippingAddress) {
        return checkoutInternal(buyerId, shippingAddress, null);
    }

    private CheckoutResponse checkoutInternal(Long buyerId, String shippingAddress, String couponCode) {
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
                .discountAmount(0.0)
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

        // Handle Coupon
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Integer discount = couponService.calculateDiscount(couponCode, (int) orderTotal);
            order.setCouponCode(couponCode.trim().toUpperCase());
            order.setDiscountAmount((double) discount);
            orderTotal = Math.max(0, orderTotal - discount);
            
            // Mark coupon as used
            couponService.useCoupon(couponCode);
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

    @Transactional
    public OrderSummaryResponse cancelCurrentBuyerOrder(Long buyerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!canBuyerCancelOrder(order)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending orders can be cancelled");
        }

        if (order.getSubOrders() != null) {
            for (SubOrder subOrder : order.getSubOrders()) {
                subOrder.setStatus(OrderStatus.CANCELLED);
            }
            subOrderRepository.saveAll(order.getSubOrders());
        }

        return toOrderSummary(order);
    }

    @Transactional
    public OrderDetailResponse getCurrentBuyerOrderDetail(Long buyerId, Long orderId) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        List<OrderDetailResponse.SubOrderDetail> subOrderDetails = new ArrayList<>();

        if (order.getSubOrders() != null) {
            for (SubOrder subOrder : order.getSubOrders()) {
                List<OrderDetailResponse.OrderItemDetail> items = new ArrayList<>();
                if (subOrder.getItems() != null) {
                    for (OrderItem item : subOrder.getItems()) {
                        items.add(OrderDetailResponse.OrderItemDetail.builder()
                                .id(item.getId())
                                .bookTitle(item.getBook() != null ? item.getBook().getTitle() : "N/A")
                                .price(item.getUnitPrice())
                                .quantity(item.getQuantity())
                                .subtotal(item.getUnitPrice() * item.getQuantity())
                                .build());
                    }
                }

                subOrderDetails.add(OrderDetailResponse.SubOrderDetail.builder()
                        .id(subOrder.getId())
                        .sellerName(subOrder.getSeller() != null ? subOrder.getSeller().getUsername() : "N/A")
                        .status(subOrder.getStatus().toString())
                        .subTotal(subOrder.getSubTotal())
                        .items(items)
                        .build());
            }
        }

        return OrderDetailResponse.builder()
                .id(order.getId())
                .buyerName(buyer.getUsername())
                .buyerEmail(buyer.getUsername() + "@bookom.vn")
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .subOrders(subOrderDetails)
                .build();
    }

    /**
     * Lấy đơn hàng với filters (for admin)
     */
    public Page<Order> getOrdersWithFilters(
            int page, int size, String q, String status,
            LocalDate dateFrom, LocalDate dateTo
    ) {
        Pageable pageable = PageRequest.of(page, size);

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
                                    .price(item.getUnitPrice())
                                    .quantity(item.getQuantity())
                                    .subtotal(item.getUnitPrice() * item.getQuantity())
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

    @Transactional
    public List<SubOrderSummaryResponse> getSellerSubOrders(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        return subOrderRepository.findBySellerOrderByIdDesc(seller).stream()
            .map(this::toSubOrderSummary)
                .toList();
    }

    @Transactional
    public SubOrderSummaryResponse updateSubOrderStatusForSeller(Long sellerId, Long subOrderId, OrderStatus status) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub order not found"));

        subOrder.setStatus(status);
        SubOrder saved = subOrderRepository.save(subOrder);

        // Send notification to buyer about sub-order status change
        try {
            if (saved.getParentOrder() != null && saved.getParentOrder().getBuyer() != null) {
                Long buyerId = saved.getParentOrder().getBuyer().getId();
                com.example.bookstore.dto.NotificationCreateRequest req = new com.example.bookstore.dto.NotificationCreateRequest();
                req.setUserId(buyerId);
                req.setType(com.example.bookstore.model.enums.NotificationType.SUB_ORDER_STATUS_CHANGED);
                req.setTitle("Trạng thái đơn hàng thay đổi");
                req.setMessage(String.format("Sub-order #%d của đơn #%d đã chuyển sang %s", saved.getId(),
                        saved.getParentOrder().getId(), status == null ? "UNKNOWN" : status.name()));
                req.setPayloadJson(String.format("{\"subOrderId\":%d,\"orderId\":%d,\"status\":\"%s\"}",
                        saved.getId(), saved.getParentOrder().getId(), status == null ? "UNKNOWN" : status.name()));
                req.setPriority(com.example.bookstore.model.enums.NotificationPriority.NORMAL);

                // fire-and-forget; NotificationService will persist and enqueue delivery with retry
                try {
                    notificationService.createNotification(sellerId, buyerId, req);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return toSubOrderSummary(saved);
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

    /**
     * Filter buyer orders with flexible filtering options
     */
    @Transactional
    public OrderFilterResponse filterBuyerOrders(Long buyerId, OrderFilterRequest filter) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        // Set default pagination values
        int page = filter.getPage() != null ? filter.getPage() : 0;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        
        // Validate pagination
        if (page < 0) page = 0;
        if (pageSize <= 0 || pageSize > 100) pageSize = 10;

        // Build sort
        Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection()) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        // Fetch filtered orders
        Page<Order> pageResult = orderRepository.findByBuyerWithFilters(
            buyer,
            filter.getCreatedFrom(),
            filter.getCreatedTo(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            pageable
        );

        // Convert to response DTOs
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

    /**
     * Filter seller's sub-orders with flexible filtering options
     */
    @Transactional
    public SubOrderFilterResponse filterSellerSubOrders(Long sellerId, SubOrderFilterRequest filter) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        // Set default pagination values
        int page = filter.getPage() != null ? filter.getPage() : 0;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        
        // Validate pagination
        if (page < 0) page = 0;
        if (pageSize <= 0 || pageSize > 100) pageSize = 10;

        // Build sort
        Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection()) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "id";
        
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        // Fetch filtered sub-orders
        Page<SubOrder> pageResult = subOrderRepository.findBySellerWithFilters(
            seller,
            filter.getStatus(),
            filter.getCreatedFrom(),
            filter.getCreatedTo(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            pageable
        );

        // Convert to response DTOs
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

    /**
     * Search seller's sub-orders by buyer name
     */
    @Transactional
    public List<SubOrderSummaryResponse> searchSellerSubOrdersByBuyer(Long sellerId, String buyerName) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        if (buyerName == null || buyerName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer name cannot be empty");
        }

        return subOrderRepository.findBySellerAndBuyerNameContaining(seller, buyerName.trim())
            .stream()
            .map(this::toSubOrderSummary)
            .collect(Collectors.toList());
    }

    /**
     * Get orders by status (for buyers)
     */
    public List<OrderSummaryResponse> getBuyerOrdersByStatus(Long buyerId, OrderStatus status) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        List<Order> orders = orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
        
        return orders.stream()
            .filter(order -> hasOrderStatus(order, status))
            .map(this::toOrderSummary)
            .collect(Collectors.toList());
    }

    /**
     * Get sub-orders by status (for sellers)
     */
    public List<SubOrderSummaryResponse> getSellerSubOrdersByStatus(Long sellerId, OrderStatus status) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        return subOrderRepository.findBySellerAndStatusOrdered(seller, status)
            .stream()
            .map(this::toSubOrderSummary)
            .collect(Collectors.toList());
    }

    /**
     * Helper: Convert Order to OrderSummaryResponse
     */
    private OrderSummaryResponse toOrderSummary(Order order) {
        OrderStatus overallStatus = determineOverallOrderStatus(order);
        
        return OrderSummaryResponse.builder()
            .orderId(order.getId())
            .buyerId(order.getBuyer() == null ? null : order.getBuyer().getId())
            .buyerUsername(order.getBuyer() == null ? null : order.getBuyer().getUsername())
            .totalAmount(order.getTotalAmount())
            .createdAt(order.getCreatedAt())
            .subOrderCount(order.getSubOrders() == null ? 0 : order.getSubOrders().size())
            .overallStatus(overallStatus)
            .shippingAddress(order.getShippingAddress())
            .build();
    }

    /**
     * Helper: Determine overall order status based on sub-orders
     */
    private OrderStatus determineOverallOrderStatus(Order order) {
        if (order.getSubOrders() == null || order.getSubOrders().isEmpty()) {
            return OrderStatus.PENDING_PAYMENT;
        }

        if (order.getSubOrders().stream().allMatch(so -> so.getStatus() == OrderStatus.CANCELLED)) {
            return OrderStatus.CANCELLED;
        }

        // If all sub-orders are completed, order is completed
        if (order.getSubOrders().stream().allMatch(so -> so.getStatus() == OrderStatus.COMPLETED)) {
            return OrderStatus.COMPLETED;
        }

        // If any sub-order is shipping, order is being shipped
        if (order.getSubOrders().stream().anyMatch(so -> so.getStatus() == OrderStatus.SHIPPING)) {
            return OrderStatus.SHIPPING;
        }

        // If any sub-order is processing, order is processing
        if (order.getSubOrders().stream().anyMatch(so -> so.getStatus() == OrderStatus.PROCESSING)) {
            return OrderStatus.PROCESSING;
        }

        return OrderStatus.PENDING_PAYMENT;
    }

    /**
     * Helper: Check if order has a specific status (by checking sub-orders)
     */
    private boolean hasOrderStatus(Order order, OrderStatus status) {
        if (order.getSubOrders() == null || order.getSubOrders().isEmpty()) {
            return status == OrderStatus.PENDING_PAYMENT;
        }

        if (status == OrderStatus.CANCELLED || status == OrderStatus.COMPLETED || status == OrderStatus.PENDING_PAYMENT) {
            return order.getSubOrders().stream()
                .allMatch(subOrder -> subOrder.getStatus() == status);
        }

        return order.getSubOrders().stream()
            .anyMatch(subOrder -> subOrder.getStatus() == status);
    }

    private boolean canBuyerCancelOrder(Order order) {
        if (order.getSubOrders() == null || order.getSubOrders().isEmpty()) {
            return true;
        }

        return order.getSubOrders().stream()
            .allMatch(subOrder -> subOrder.getStatus() == OrderStatus.PENDING_PAYMENT);
    }
}
