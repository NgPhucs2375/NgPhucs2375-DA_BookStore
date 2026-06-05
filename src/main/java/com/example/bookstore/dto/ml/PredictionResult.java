package com.example.bookstore.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO nhận kết quả dự đoán từ Python ML API (mô hình gau-gbt6000).
 * Response thực tế từ Python API:
 * {
 *   "cluster_id": 3,
 *   "churn_probability": 0.9073,
 *   "is_churn_predicted": 1,
 *   "risk_level": "CRITICAL (Nguy cấp)"
 * }
 */
@Data
public class PredictionResult {
    @JsonProperty("cluster_id")
    private Integer predictedClass; // 0 = An toàn, 1 = Trung bình, 2 = Cao, 3 = Nguy cấp

    @JsonProperty("churn_probability")
    private Double churnProbability; // Xác suất rời bỏ (0.0 - 1.0)

    @JsonProperty("is_churn_predicted")
    private Integer isChurnPredicted; // 1 nếu có nguy cơ, 0 nếu không

    @JsonProperty("risk_level")
    private String riskLevel; // "LOW (An toàn)" / "MEDIUM (Trung bình)" / "HIGH (Nguy cơ cao)" / "CRITICAL (Nguy cấp)"
}
