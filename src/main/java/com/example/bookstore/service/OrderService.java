package com.example.bookstore.service;

import com.example.bookstore.dto.CheckoutRequest;
import com.example.bookstore.dto.CheckoutResponse;
import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.dto.OrderItemDetailResponse;
import com.example.bookstore.dto.SubOrderSummaryResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;

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

    public List<SubOrderSummaryResponse> getSellerSubOrders(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        return subOrderRepository.findBySellerOrderByIdDesc(seller).stream()
                .map(subOrder -> SubOrderSummaryResponse.builder()
                        .subOrderId(subOrder.getId())
                        .orderId(subOrder.getParentOrder() == null ? null : subOrder.getParentOrder().getId())
                        .sellerId(seller.getId())
                        .sellerName(seller.getShopName() == null ? seller.getUsername() : seller.getShopName())
                        .status(subOrder.getStatus())
                        .subTotal(subOrder.getSubTotal())
                        .build())
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

        User savedSeller = saved.getSeller();
        String sellerName = savedSeller == null ? null : (savedSeller.getShopName() == null ? savedSeller.getUsername() : savedSeller.getShopName());

        return SubOrderSummaryResponse.builder()
                .subOrderId(saved.getId())
                .orderId(saved.getParentOrder() == null ? null : saved.getParentOrder().getId())
                .sellerId(savedSeller == null ? null : savedSeller.getId())
                .sellerName(sellerName)
                .status(saved.getStatus())
                .subTotal(saved.getSubTotal())
                .build();
    }
}
