package com.example.bookstore.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SellerAnalyticsResponse {
    private Long sellerId;
    private String sellerName;
    private Integer days;
    private String periodLabel;
    private LocalDateTime generatedAt;

    private Double totalRevenue;
    private Long completedOrders;
    private Double averageOrderValue;
    private Double completionRate;
    private Long soldUnits;

    private List<RevenuePoint> revenueTimeline;
    private List<CategoryRevenue> categoryRevenue;
    private List<ProductPerformance> topSellingProducts;
    private List<StockAlert> lowStockProducts;
    private List<TransactionRow> recentTransactions;

    @Data
    @Builder
    public static class RevenuePoint {
        private String label;
        private Double revenue;
        private Long orderCount;
        private Long soldUnits;
    }

    @Data
    @Builder
    public static class CategoryRevenue {
        private String categoryName;
        private Double revenue;
        private Long soldUnits;
        private Double sharePercent;
    }

    @Data
    @Builder
    public static class ProductPerformance {
        private Long bookId;
        private String title;
        private String imageUrl;
        private Integer stockQuantity;
        private Long soldUnits;
        private Double revenue;
        private Double progressPercent;
    }

    @Data
    @Builder
    public static class StockAlert {
        private Long bookId;
        private String title;
        private String imageUrl;
        private Integer stockQuantity;
        private Long soldUnits;
        private Boolean needReorder;
        private String note;
    }

    @Data
    @Builder
    public static class TransactionRow {
        private String transactionId;
        private Long orderId;
        private Long subOrderId;
        private LocalDateTime createdAt;
        private String customerName;
        private Long bookId;
        private String productName;
        private Integer quantity;
        private Double amount;
        private String paymentMethod;
    }
}