package com.example.bookstore.service;

import com.example.bookstore.dto.SellerShopResponse;
import com.example.bookstore.dto.SellerShopUpsertRequest;
import com.example.bookstore.model.SellerShop;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.model.User;
import com.example.bookstore.repository.SellerShopRepository;
import com.example.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerShopService {

    private final SellerShopRepository shopRepository;
    private final UserRepository userRepository;

    /**
     * Lấy thông tin shop của chính Seller đang đăng nhập
     */
    public SellerShopResponse getMyShop(Long sellerId) {
        SellerShop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa tạo cửa hàng nào."));
        return mapToResponse(shop);
    }

    /**
     * Tạo mới Shop
     */
    @Transactional
    public SellerShopResponse createMyShop(Long sellerId, SellerShopUpsertRequest request) {
        // 1. Lấy thông tin User và kiểm tra Role SELLER
        User seller = validateAndGetSeller(sellerId);

        // 2. Kiểm tra xem Seller này đã có shop chưa (Rule: 1 seller - 1 shop)
        if (shopRepository.findBySellerId(sellerId).isPresent()) {
            throw new RuntimeException("Bạn đã có một cửa hàng. Không thể tạo thêm.");
        }

        // 3. Kiểm tra Slug unique
        if (shopRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Đường dẫn (Slug) này đã tồn tại. Vui lòng chọn đường dẫn khác.");
        }

        // 4. Tạo entity và lưu
        SellerShop newShop = SellerShop.builder()
                .seller(seller)
                .slug(request.getSlug())
                .shopName(request.getShopName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .approvalStatus(ApprovalStatus.PENDING) // Mặc định khi mới tạo là PENDING chờ duyệt
                .build();

        SellerShop savedShop = shopRepository.save(newShop);
        return mapToResponse(savedShop);
    }

    /**
     * Cập nhật thông tin Shop
     * Rule: Chỉ sửa shop của chính họ -> được đảm bảo bằng việc tìm shop qua sellerId
     */
    @Transactional
    public SellerShopResponse updateMyShop(Long sellerId, SellerShopUpsertRequest request) {
        // Lấy shop dựa trên sellerId (Đảm bảo tính sở hữu)
        SellerShop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng của bạn."));

        // Kiểm tra Slug có bị trùng với shop khác không (nếu họ thay đổi slug)
        if (!shop.getSlug().equals(request.getSlug()) && shopRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Đường dẫn (Slug) này đã tồn tại. Vui lòng chọn đường dẫn khác.");
        }

        // Cập nhật thông tin
        shop.setSlug(request.getSlug());
        shop.setShopName(request.getShopName());
        shop.setDescription(request.getDescription());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setBannerUrl(request.getBannerUrl());
        shop.setContactEmail(request.getContactEmail());
        shop.setContactPhone(request.getContactPhone());
        shop.setAddress(request.getAddress());
        shop.setCity(request.getCity());
        shop.setProvince(request.getProvince());

        SellerShop updatedShop = shopRepository.save(shop);
        return mapToResponse(updatedShop);
    }

    /**
     * Tạm dừng hoặc kích hoạt lại Shop (Đổi status)
     */
    @Transactional
    public SellerShopResponse changeStatus(Long sellerId, ApprovalStatus status) {
        SellerShop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng của bạn."));
        
        shop.setApprovalStatus(status);
        SellerShop updatedShop = shopRepository.save(shop);
        return mapToResponse(updatedShop);
    }

    /**
     * Xóa shop của chính Seller (hard delete)
     */
    @Transactional
    public void deleteMyShop(Long sellerId) {
        SellerShop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng của bạn."));
        shopRepository.delete(shop);
    }

    /**
     * Lấy thông tin Shop Public qua Slug (Dành cho khách hàng xem)
     */
    public SellerShopResponse getPublicShopBySlug(String slug) {
        SellerShop shop = shopRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cửa hàng."));
        
        // Khách chỉ được xem các shop đã được duyệt
        if (shop.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("Cửa hàng này hiện không hoạt động.");
        }
        
        return mapToResponse(shop);
    }

    // --- CÁC HÀM TIỆN ÍCH (HELPER METHODS) ---

    /**
     * Lấy User và validate Role
     */
    private User validateAndGetSeller(Long sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        
        // Giả sử Entity User của bạn có hàm getRole() hoặc Enum Role
        // Thay đổi phần này cho khớp với cấu trúc Entity User của bạn
        if (user.getRole() != UserRole.SELLER) {
            throw new RuntimeException("Bạn không có quyền đăng ký cửa hàng (Yêu cầu Role SELLER).");
        }
        return user;
    }

    /**
     * Map từ Entity sang Response DTO
     */
    private SellerShopResponse mapToResponse(SellerShop shop) {
        return SellerShopResponse.builder()
                .id(shop.getId())
                .sellerId(shop.getSeller().getId()) // Tránh query toàn bộ object User
                .slug(shop.getSlug())
                .shopName(shop.getShopName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .contactEmail(shop.getContactEmail())
                .contactPhone(shop.getContactPhone())
                .address(shop.getAddress())
                .city(shop.getCity())
                .province(shop.getProvince())
                .approvalStatus(shop.getApprovalStatus())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}