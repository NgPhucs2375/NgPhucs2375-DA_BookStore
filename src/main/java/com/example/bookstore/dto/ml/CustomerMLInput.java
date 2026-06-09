package com.example.bookstore.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO gửi lên Python ML API để dự đoán hành vi khách hàng (mô hình gau-gbt6000).
 * Gồm 14 raw features bắt buộc, Python sẽ tự động engineering thêm 4 derived features.
 * Các field dùng @JsonProperty để map camelCase Java sang snake_case Python.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMLInput {
    @JsonProperty("account_age_months")
    private Double accountAgeMonths;

    @JsonProperty("avg_order_value")
    private Double avgOrderValue;

    @JsonProperty("total_orders")
    private Double totalOrders;

    @JsonProperty("days_since_last_purchase")
    private Double daysSinceLastPurchase;

    @JsonProperty("discount_usage_rate")
    private Double discountUsageRate;

    @JsonProperty("return_rate")
    private Double returnRate;

    @JsonProperty("customer_support_tickets")
    private Double customerSupportTickets;

    @JsonProperty("loyalty_member")
    private Object loyaltyMember;        // "Yes" / "No" / true / false

    @JsonProperty("browsing_frequency_per_week")
    private Double browsingFrequencyPerWeek;

    @JsonProperty("cart_abandonment_rate")
    private Double cartAbandonmentRate;

    @JsonProperty("product_review_score_avg")
    private Double productReviewScoreAvg;

    @JsonProperty("engagement_score")
    private Double engagementScore;

    @JsonProperty("satisfaction_score")
    private Double satisfactionScore;

    @JsonProperty("price_sensitivity_index")
    private Double priceSensitivityIndex;
}
