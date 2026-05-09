package com.example.bookstore.controller;

import com.example.bookstore.dto.CheckoutMeRequest;
import com.example.bookstore.dto.CheckoutResponse;
import com.example.bookstore.dto.SellerAnalyticsResponse;
import com.example.bookstore.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldCheckoutWithCurrentBuyerContext() throws Exception {
        CheckoutMeRequest request = new CheckoutMeRequest();
        request.setShippingAddress("Q1, HCM");

        CheckoutResponse response = CheckoutResponse.builder()
            .orderId(101L)
            .buyerId(1L)
            .shippingAddress("Q1, HCM")
            .totalAmount(320000.0)
            .subOrderCount(2)
            .build();

        when(orderService.checkoutFromCurrentBuyer(eq(1L), eq("Q1, HCM"))).thenReturn(response);

        mockMvc.perform(post("/api/orders/me/checkout")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(101))
            .andExpect(jsonPath("$.subOrderCount").value(2));
    }

    @Test
    void shouldGetCurrentBuyerOrders() throws Exception {
        when(orderService.getCurrentBuyerOrders(1L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/orders/me").header("X-User-Id", "1"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldGetCurrentSellerAnalytics() throws Exception {
        SellerAnalyticsResponse response = SellerAnalyticsResponse.builder()
                .sellerId(7L)
                .sellerName("Shop 7")
                .days(30)
                .periodLabel("30 ngày gần nhất")
                .totalRevenue(120000.0)
                .completedOrders(4L)
                .averageOrderValue(30000.0)
                .completionRate(80.0)
                .soldUnits(6L)
                .build();

        when(orderService.getSellerAnalytics(eq(7L), eq(30))).thenReturn(response);

        mockMvc.perform(get("/api/orders/seller/me/analytics").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(7))
                .andExpect(jsonPath("$.totalRevenue").value(120000.0));
    }
}
