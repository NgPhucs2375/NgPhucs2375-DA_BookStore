package com.example.bookstore.service.cluster;

import com.example.bookstore.dto.ml.CustomerMLInput;
import com.example.bookstore.model.Customer;
import com.example.bookstore.model.User;
import com.example.bookstore.repository.BookReviewRepository;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.CustomerRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý entity Customer (ML features & kết quả phân tích).
 * Chịu trách nhiệm CRUD và tính toán features từ dữ liệu có sẵn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final BookReviewRepository bookReviewRepository;
    private final CartRepository cartRepository;

    /**
     * Tìm Customer theo user ID.
     */
    public Optional<Customer> findByUserId(Long userId) {
        return customerRepository.findByUserId(userId);
    }

    /**
     * Kiểm tra đã có Customer record cho user này chưa.
     */
    public boolean existsByUserId(Long userId) {
        return customerRepository.existsByUserId(userId);
    }

    /**
     * Lấy tất cả Customer records.
     */
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    /**
     * Lưu hoặc cập nhật Customer.
     */
    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    /**
     * Xoá Customer record theo ID.
     */
    @Transactional
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    /**
     * Chuẩn hoá riskLevel từ Python API (VD: "LOW" hoặc "HIGH")
     * để đồng bộ với giao diện frontend.
     */
    public String normalizeRiskLevel(String rawRiskLevel) {
        if (rawRiskLevel == null || rawRiskLevel.isBlank()) {
            return null;
        }
        String trimmed = rawRiskLevel.trim().toUpperCase();
        // Nếu đã là dạng thuần (LOW/HIGH) thì giữ nguyên
        if (trimmed.equals("LOW") || trimmed.equals("HIGH")) {
            return trimmed;
        }
        // Nếu có dạng "LOW (Tiếng Việt)" → lấy phần CODE
        if (trimmed.startsWith("LOW")) return "LOW";
        if (trimmed.startsWith("HIGH")) return "HIGH";
        // Fallback: giữ nguyên
        return trimmed;
    }

    /**
     * Map dữ liệu từ entity Customer sang DTO gửi lên ML API.
     *
     * @param customer entity Customer từ database
     * @return CustomerMLInput DTO (10 raw features)
     */
    public CustomerMLInput toMlInput(Customer customer) {
        return CustomerMLInput.builder()
                .accountAgeMonths(customer.getAccountAgeMonths())
                .avgOrderValue(customer.getAvgOrderValue())
                .totalOrders(customer.getTotalOrders())
                .customerSupportTickets(customer.getCustomerSupportTickets())
                .loyaltyMember(customer.getLoyaltyMember())
                .browsingFrequencyPerWeek(customer.getBrowsingFrequencyPerWeek())
                .cartAbandonmentRate(customer.getCartAbandonmentRate())
                .productReviewScoreAvg(customer.getProductReviewScoreAvg())
                .satisfactionScore(customer.getSatisfactionScore())
                .priceSensitivityIndex(customer.getPriceSensitivityIndex())
                .build();
    }

    /**
     * Cập nhật kết quả ML vào entity Customer.
     * Tự động chuẩn hoá riskLevel để đồng bộ với giao diện frontend.
     *
     * @param customer         entity cần cập nhật
     * @param predictedLabel   nhãn dự đoán (0 = Stay, 1 = Churn)
     * @param churnProbability xác suất rời bỏ
     * @param riskLevel        mức độ rủi ro (LOW/HIGH)
     */
    public void updateMlResult(Customer customer, Integer predictedLabel,
                               Double churnProbability, String riskLevel) {
        customer.setPredictedLabel(predictedLabel);
        customer.setChurnProbability(churnProbability);
        customer.setRiskLevel(normalizeRiskLevel(riskLevel));
        customer.setLastAnalyzedAt(java.time.LocalDateTime.now());
    }

    /**
     * Tạo entity Customer mới từ User (với giá trị mặc định).
     * Dùng khi lần đầu phân tích khách hàng.
     */
    public Customer createDefault(User user) {
        Customer customer = Customer.builder()
                .user(user)
                .accountAgeMonths(0.0)
                .avgOrderValue(0.0)
                .totalOrders(0.0)
                .customerSupportTickets(0.0)
                .loyaltyMember("No")
                .browsingFrequencyPerWeek(0.0)
                .cartAbandonmentRate(0.0)
                .productReviewScoreAvg(0.0)
                .satisfactionScore(0.0)
                .priceSensitivityIndex(0.0)
                .build();
        return customerRepository.save(customer);
    }

    /**
     * Tính toán các ML features từ dữ liệu thực tế của user (orders, reviews, cart).
     * Dùng khi lần đầu phân tích hoặc khi cần refresh dữ liệu.
     *
     * @param user User cần tính features
     * @return Customer entity đã được gán features (chưa save)
     */
    public Customer computeFeatures(User user) {
        // ====================================================================
        // 1. Order-based features (THẬT)
        // ====================================================================
        long totalOrders = orderRepository.countByBuyer(user);
        Double totalSpent = orderRepository.sumTotalAmountByBuyer(user);
        if (totalSpent == null) totalSpent = 0.0;
        Double avgOrderValue = totalOrders > 0 ? totalSpent / totalOrders : 0.0;

        // ====================================================================
        // 2. Account age (THẬT từ User.createdAt)
        // ====================================================================
        double accountAgeMonths = 0.0;
        if (user.getCreatedAt() != null) {
            accountAgeMonths = ChronoUnit.MONTHS.between(user.getCreatedAt(), LocalDateTime.now());
        }

        // ====================================================================
        // 3. Review-based features (THẬT)
        // ====================================================================
        Double avgRating = bookReviewRepository.findAverageRatingByUser(user);
        if (avgRating == null) avgRating = 0.0;

        // ====================================================================
        // 4. Cart abandonment (THẬT)
        // ====================================================================
        double cartAbandonmentRate = 0.0;
        var cartOpt = cartRepository.findByBuyerId(user.getId());
        if (cartOpt.isPresent()) {
            int cartItemCount = cartOpt.get().getItems().size();
            if (totalOrders > 0) {
                cartAbandonmentRate = Math.min(1.0, (double) cartItemCount / totalOrders);
            } else if (cartItemCount > 0) {
                cartAbandonmentRate = 0.8;
            }
        }

        // ====================================================================
        // 5. Loyalty member (THẬT)
        // ====================================================================
        String loyaltyMember = (totalOrders >= 5 || totalSpent >= 1_000_000) ? "Yes" : "No";

        // ====================================================================
        // 6. Satisfaction score (THẬT từ rating)
        // ====================================================================
        double satisfactionScore = avgRating > 0 ? (avgRating / 5.0) * 100.0 : 0.0;

        // ====================================================================
        // 7. Product review score avg (THẬT)
        // ====================================================================
        double productReviewScoreAvg = avgRating;

        // ====================================================================
        // 8. Customer support tickets (THẬT từ bảng support_tickets)
        // ====================================================================
        // Lưu ý: Cần inject SupportTicketRepository. Tạm thời dùng 0.0 nếu chưa có.
        // Khi có SupportTicketRepository, thay bằng:
        // long ticketCount = supportTicketRepository.countByUserId(user.getId());
        double customerSupportTickets = 0.0;

        // ====================================================================
        // 9. Browsing frequency per week (THẬT từ user_activity_log)
        // ====================================================================
        // Lưu ý: Cần inject UserActivityLogRepository. Tạm thời ước lượng từ orders + reviews.
        // Khi có UserActivityLogRepository, thay bằng:
        // long activityCount = userActivityLogRepository.countByUserIdInLastWeek(user.getId());
        // double browsingFrequencyPerWeek = Math.min(7.0, activityCount / 7.0);
        long reviewCount = bookReviewRepository.countByUser(user);
        double browsingFrequencyPerWeek = Math.min(7.0, (totalOrders + reviewCount) / 4.0);

        // ====================================================================
        // 10. Price sensitivity index (THẬT từ discount behavior)
        // ====================================================================
        // Công thức: dựa trên tỷ lệ đơn có giảm giá và mức giảm trung bình
        // Nếu user hay mua khi giảm giá → priceSensitivityIndex cao (nhạy cảm giá)
        double priceSensitivityIndex = 5.0; // mặc định trung bình
        if (totalOrders > 0) {
            Double totalDiscount = orderRepository.sumDiscountAmountByBuyer(user);
            if (totalDiscount == null) totalDiscount = 0.0;
            double discountRatio = totalSpent > 0 ? totalDiscount / totalSpent : 0.0;
            // discountRatio: 0-1, map sang 1-10
            priceSensitivityIndex = Math.min(10.0, Math.max(1.0, discountRatio * 20.0));
        }

        // ====================================================================
        // Build Customer entity với features đã tính
        // ====================================================================
        Customer customer = Customer.builder()
                .user(user)
                .accountAgeMonths(accountAgeMonths)
                .avgOrderValue(avgOrderValue)
                .totalOrders((double) totalOrders)
                .customerSupportTickets(customerSupportTickets)
                .loyaltyMember(loyaltyMember)
                .browsingFrequencyPerWeek(browsingFrequencyPerWeek)
                .cartAbandonmentRate(cartAbandonmentRate)
                .productReviewScoreAvg(productReviewScoreAvg)
                .satisfactionScore(satisfactionScore)
                .priceSensitivityIndex(priceSensitivityIndex)
                .build();

        log.info("Computed ML features for user {}: totalOrders={}, totalSpent={}, avgOrderValue={}, " +
                        "avgRating={}, loyaltyMember={}, accountAgeMonths={}, priceSensitivityIndex={}",
                user.getId(), totalOrders, totalSpent, avgOrderValue,
                avgRating, loyaltyMember, accountAgeMonths, priceSensitivityIndex);

        return customer;
    }
}
