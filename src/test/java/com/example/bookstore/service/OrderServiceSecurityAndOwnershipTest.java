package com.example.bookstore.service;

import com.example.bookstore.dto.SubOrderSummaryResponse;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.SubOrder;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.SubOrderRepository;
import com.example.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceSecurityAndOwnershipTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SubOrderRepository subOrderRepository;

    @Mock
    private NotificationService notificationService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(userRepository, cartRepository, orderRepository, subOrderRepository, notificationService);
    }

    @Test
    void getCurrentBuyerOrders_shouldReturnOrdersForBuyer() {
        User buyer = User.builder()
            .id(2L)
            .username("buyer")
            .passwordHash("x")
            .role(UserRole.BUYER)
            .build();

        Order order = Order.builder()
            .id(10L)
            .buyer(buyer)
            .totalAmount(100000.0)
            .shippingAddress("HCM")
            .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(orderRepository.findByBuyerOrderByCreatedAtDesc(buyer)).thenReturn(java.util.List.of(order));

        assertEquals(1, orderService.getCurrentBuyerOrders(2L).size());
    }

    @Test
    void updateSubOrderStatusForSeller_shouldUpdateStatus() {
        User owner = User.builder()
            .id(33L)
            .username("seller-ok")
            .passwordHash("x")
            .role(UserRole.SELLER)
            .shopName("Shop OK")
            .build();

        SubOrder subOrder = SubOrder.builder()
            .id(200L)
            .seller(owner)
            .status(OrderStatus.PROCESSING)
            .subTotal(210000.0)
            .parentOrder(Order.builder().id(2L).build())
            .build();

        when(userRepository.findById(33L)).thenReturn(Optional.of(owner));
        when(subOrderRepository.findById(200L)).thenReturn(Optional.of(subOrder));
        when(subOrderRepository.save(subOrder)).thenReturn(subOrder);

        SubOrderSummaryResponse response = orderService.updateSubOrderStatusForSeller(33L, 200L, OrderStatus.SHIPPING);

        assertEquals(OrderStatus.SHIPPING, response.getStatus());
        assertEquals(33L, response.getSellerId());
        assertEquals(2L, response.getOrderId());
    }

    @Test
    void cancelCurrentBuyerOrder_shouldCancelSubOrdersWhenOrderIsPending() {
        User buyer = User.builder()
            .id(44L)
            .username("buyer-ok")
            .passwordHash("x")
            .role(UserRole.BUYER)
            .build();

        SubOrder subOrder = SubOrder.builder()
            .id(301L)
            .status(OrderStatus.PENDING_PAYMENT)
            .seller(User.builder().id(55L).username("seller").passwordHash("x").role(UserRole.SELLER).build())
            .subTotal(45000.0)
            .parentOrder(Order.builder().id(9L).buyer(buyer).build())
            .build();

        Order order = Order.builder()
            .id(9L)
            .buyer(buyer)
            .totalAmount(45000.0)
            .shippingAddress("Hanoi")
            .subOrders(java.util.List.of(subOrder))
            .build();

        when(userRepository.findById(44L)).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(subOrderRepository.saveAll(order.getSubOrders())).thenReturn(order.getSubOrders());

        orderService.cancelCurrentBuyerOrder(44L, 9L);

        assertEquals(OrderStatus.CANCELLED, subOrder.getStatus());
    }
}
