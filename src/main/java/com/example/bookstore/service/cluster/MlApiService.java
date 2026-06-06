package com.example.bookstore.service.cluster;

import com.example.bookstore.dto.ml.CustomerMLInput;
import com.example.bookstore.dto.ml.PredictionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service gọi HTTP đến Python ML API để dự đoán cluster & churn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MlApiService {

    private final RestTemplate restTemplate;

    @Value("${ml.api.url}")
    private String mlApiUrl;

    /**
     * Gửi thông tin khách hàng lên ML API và nhận kết quả dự đoán.
     *
     * @param input thông tin khách hàng cần phân tích
     * @return kết quả dự đoán (clusterId, churnProbability, ...)
     */
    public PredictionResult predictCustomer(CustomerMLInput input) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CustomerMLInput> request = new HttpEntity<>(input, headers);

        log.info("Calling ML API at {} with input: {}", mlApiUrl, input);
        try {
            PredictionResult result = restTemplate.postForObject(mlApiUrl, request, PredictionResult.class);
            log.info("ML API response: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Failed to call ML API: {}", e.getMessage());
            throw new RuntimeException("ML API call failed: " + e.getMessage(), e);
        }
    }
}
