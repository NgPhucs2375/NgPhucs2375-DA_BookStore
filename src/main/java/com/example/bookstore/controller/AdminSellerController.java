package com.example.bookstore.controller;

import com.example.bookstore.dto.NotificationCreateRequest;
import com.example.bookstore.dto.SellerShopResponse;
import com.example.bookstore.model.SellerShop;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.service.NotificationService;
import com.example.bookstore.service.MailService;
import com.example.bookstore.service.SellerShopService;
import com.example.bookstore.repository.SellerShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/seller-applications")
public class AdminSellerController {

    @Autowired
    private SellerShopRepository shopRepository;

    @Autowired
    private SellerShopService sellerShopService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MailService mailService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        List<SellerShop> filtered = shopRepository.findAll()
            .stream()
            .filter(shop -> keyword.isBlank() || matchesKeyword(shop, keyword))
            .sorted((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            })
            .collect(Collectors.toList());

        int totalItems = filtered.size();
        int totalPages = (int) Math.ceil(totalItems / (double) normalizedSize);
        int fromIndex = Math.min(normalizedPage * normalizedSize, totalItems);
        int toIndex = Math.min(fromIndex + normalizedSize, totalItems);

        List<Map<String, Object>> items = filtered.subList(fromIndex, toIndex).stream()
            .map(this::toAdminRow)
            .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("page", normalizedPage);
        response.put("size", normalizedSize);
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);
        response.put("hasNext", normalizedPage + 1 < totalPages);
        response.put("hasPrev", normalizedPage > 0);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{shopId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long shopId) {
        SellerShop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
        // Promote user and approve via service
        var result = sellerShopService.adminApproveSeller(shop.getSeller().getId());

        // Notify seller
        NotificationCreateRequest nc = new NotificationCreateRequest();
        nc.setType(com.example.bookstore.model.enums.NotificationType.SHOP_APPROVAL_UPDATED);
        nc.setTitle("Yêu cầu mở cửa hàng đã được duyệt");
        nc.setMessage("Cửa hàng của bạn đã được admin duyệt và kích hoạt.");
        nc.setPayloadJson(String.format("{\"shopId\":%d,\"status\":\"APPROVED\"}", shop.getId()));
        var notificationResp = notificationService.createNotification(null, shop.getSeller().getId(), nc);

        boolean mailConfigured = mailService.isConfigured();
        boolean mailSent = mailService.sendSellerApplicationApproved(shop.getSeller(), shop);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("result", result);
        resp.put("notificationId", notificationResp != null ? notificationResp.getId() : null);
        resp.put("mailConfigured", mailConfigured);
        resp.put("mailSent", mailSent);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{shopId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long shopId, @RequestBody(required = false) java.util.Map<String, String> body) {
        SellerShop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
        shop.setApprovalStatus(ApprovalStatus.REJECTED);
        String reason = body == null ? null : body.getOrDefault("reason", null);
        if (reason != null && !reason.isBlank()) {
            shop.setRejectionReason(reason);
        }
        shopRepository.save(shop);
        NotificationCreateRequest nc = new NotificationCreateRequest();
        nc.setType(com.example.bookstore.model.enums.NotificationType.SHOP_APPROVAL_UPDATED);
        nc.setTitle("Yêu cầu mở cửa hàng bị từ chối");
        nc.setMessage("Yêu cầu của bạn bị từ chối" + (reason != null ? (": " + reason) : "."));
        nc.setPayloadJson(String.format("{\"shopId\":%d,\"status\":\"REJECTED\",\"reason\":\"%s\"}", shop.getId(), reason == null ? "" : reason.replaceAll("\"","\\\"")));
        var notificationResp = notificationService.createNotification(null, shop.getSeller().getId(), nc);

        boolean mailConfigured = mailService.isConfigured();
        boolean mailSent = mailService.sendSellerApplicationRejected(shop.getSeller(), shop, reason);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "rejected");
        resp.put("notificationId", notificationResp != null ? notificationResp.getId() : null);
        resp.put("mailConfigured", mailConfigured);
        resp.put("mailSent", mailSent);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{shopId}/resend-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resendEmail(@PathVariable Long shopId, @RequestParam(defaultValue = "rejected") String type) {
        SellerShop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
        boolean configured = mailService.isConfigured();
        boolean sent = false;
        if ("approved".equalsIgnoreCase(type)) {
            sent = mailService.sendSellerApplicationApproved(shop.getSeller(), shop);
        } else {
            sent = mailService.sendSellerApplicationRejected(shop.getSeller(), shop, shop.getRejectionReason());
        }
        return ResponseEntity.ok(Map.of("mailConfigured", configured, "mailSent", sent));
    }

    @PutMapping("/{shopId}/resend-notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resendNotification(@PathVariable Long shopId, @RequestParam(defaultValue = "rejected") String type) {
        SellerShop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
        NotificationCreateRequest nc = new NotificationCreateRequest();
        nc.setType(com.example.bookstore.model.enums.NotificationType.SHOP_APPROVAL_UPDATED);
        if ("approved".equalsIgnoreCase(type)) {
            nc.setTitle("Yêu cầu mở cửa hàng đã được duyệt");
            nc.setMessage("Cửa hàng của bạn đã được admin duyệt và kích hoạt.");
            nc.setPayloadJson(String.format("{\"shopId\":%d,\"status\":\"APPROVED\"}", shop.getId()));
        } else {
            nc.setTitle("Yêu cầu mở cửa hàng bị từ chối");
            nc.setMessage("Yêu cầu của bạn bị từ chối" + (shop.getRejectionReason() != null ? (": " + shop.getRejectionReason()) : "."));
            nc.setPayloadJson(String.format("{\"shopId\":%d,\"status\":\"REJECTED\",\"reason\":\"%s\"}", shop.getId(), shop.getRejectionReason() == null ? "" : shop.getRejectionReason().replaceAll("\"","\\\"")));
        }
        var notificationResp = notificationService.createNotification(null, shop.getSeller().getId(), nc);
        return ResponseEntity.ok(Map.of("notificationId", notificationResp != null ? notificationResp.getId() : null));
    }

    private boolean matchesKeyword(SellerShop shop, String keyword) {
        String shopName = shop.getShopName() == null ? "" : shop.getShopName().toLowerCase(Locale.ROOT);
        String slug = shop.getSlug() == null ? "" : shop.getSlug().toLowerCase(Locale.ROOT);
        User seller = shop.getSeller();
        String username = seller != null && seller.getUsername() != null ? seller.getUsername().toLowerCase(Locale.ROOT) : "";
        String email = seller != null && seller.getEmail() != null ? seller.getEmail().toLowerCase(Locale.ROOT) : "";
        return shopName.contains(keyword) || slug.contains(keyword) || username.contains(keyword) || email.contains(keyword);
    }

    private Map<String, Object> toAdminRow(SellerShop shop) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", shop.getId());
        row.put("sellerId", shop.getSeller() != null ? shop.getSeller().getId() : null);
        row.put("sellerUsername", shop.getSeller() != null ? shop.getSeller().getUsername() : null);
        row.put("sellerEmail", shop.getSeller() != null ? shop.getSeller().getEmail() : null);
        row.put("slug", shop.getSlug());
        row.put("shopName", shop.getShopName());
        row.put("address", shop.getAddress());
        row.put("approvalStatus", shop.getApprovalStatus());
        row.put("rejectionReason", shop.getRejectionReason());
        row.put("createdAt", shop.getCreatedAt());
        row.put("updatedAt", shop.getUpdatedAt());
        return row;
    }
}
