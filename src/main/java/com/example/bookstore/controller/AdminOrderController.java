package com.example.bookstore.controller;

import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Lấy tất cả đơn hàng (có phân trang và filter)
     */
    @GetMapping()
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            HttpServletRequest request
    ) {
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối: Chỉ Admin mới có quyền!");
        }

        try {
            LocalDate from = (dateFrom != null && !dateFrom.isEmpty()) ? LocalDate.parse(dateFrom) : null;
            LocalDate to = (dateTo != null && !dateTo.isEmpty()) ? LocalDate.parse(dateTo) : null;

            Page<?> orders = orderService.getOrdersWithFilters(page, size, q, status, from, to);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy chi tiết đơn hàng
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetails(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String role = (String) request.getAttribute("CURRENT_USER_ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Truy cập bị từ chối!");
        }

        try {
            OrderDetailResponse detail = orderService.getOrderDetailsForAdmin(id);
            if (detail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy đơn hàng");
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}
