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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(userRepository, cartRepository, orderRepository, subOrderRepository);
    }

    @Test
    void getCurrentBuyerOrders_shouldRejectNonBuyer() {
        User seller = User.builder()
            .id(2L)
            .username("seller")
            .passwordHash("x")
            .role(UserRole.SELLER)
            .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> orderService.getCurrentBuyerOrders(2L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateSubOrderStatusForSeller_shouldRejectOwnershipMismatch() {
        User callerSeller = User.builder()
            .id(11L)
            .username("seller-a")
            .passwordHash("x")
            .role(UserRole.SELLER)
            .build();

        User realOwner = User.builder()
            .id(22L)
            .username("seller-b")
            .passwordHash("x")
            .role(UserRole.SELLER)
            .build();

        SubOrder subOrder = SubOrder.builder()
            .id(100L)
            .seller(realOwner)
            .status(OrderStatus.PROCESSING)
            .subTotal(120000.0)
            .parentOrder(Order.builder().id(1L).build())
            .build();

        when(userRepository.findById(11L)).thenReturn(Optional.of(callerSeller));
        when(subOrderRepository.findById(100L)).thenReturn(Optional.of(subOrder));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> orderService.updateSubOrderStatusForSeller(11L, 100L, OrderStatus.SHIPPING)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateSubOrderStatusForSeller_shouldUpdateWhenOwnerMatches() {
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
}
