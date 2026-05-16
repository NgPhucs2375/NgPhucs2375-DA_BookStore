package com.example.bookstore.security;

import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.SubOrderRepository;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;

    public CustomPermissionEvaluator(BookRepository bookRepository,
                                     OrderRepository orderRepository,
                                     SubOrderRepository subOrderRepository) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.subOrderRepository = subOrderRepository;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || targetDomainObject == null || permission == null) {
            return false;
        }

        if (targetDomainObject instanceof com.example.bookstore.model.Book book) {
            return hasBookPermission(authentication, book.getId(), permission.toString());
        }

        if (targetDomainObject instanceof com.example.bookstore.model.Order order) {
            return hasOrderPermission(authentication, order.getId(), permission.toString());
        }

        if (targetDomainObject instanceof com.example.bookstore.model.SubOrder subOrder) {
            return hasSubOrderPermission(authentication, subOrder.getId(), permission.toString());
        }

        if (targetDomainObject instanceof User user) {
            return hasUserPermission(authentication, user.getId(), permission.toString());
        }

        if (targetDomainObject instanceof Long targetId) {
            return hasPermission(authentication, targetId, targetDomainObject.getClass().getSimpleName(), permission);
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || targetId == null || targetType == null || permission == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        Long id = toLong(targetId);
        if (id == null) {
            return false;
        }

        String normalizedType = targetType.trim().toLowerCase(Locale.ROOT);
        String normalizedPermission = permission.toString().trim().toLowerCase(Locale.ROOT);

        return switch (normalizedType) {
            case "book", "product" -> hasBookPermission(authentication, id, normalizedPermission);
            case "order" -> hasOrderPermission(authentication, id, normalizedPermission);
            case "suborder", "sub_order" -> hasSubOrderPermission(authentication, id, normalizedPermission);
            case "user", "profile" -> hasUserPermission(authentication, id, normalizedPermission);
            default -> false;
        };
    }

    private boolean hasBookPermission(Authentication authentication, Long bookId, String permission) {
        if (bookId == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return false;
        }

        if ("read".equals(permission) || "view".equals(permission)) {
            ApprovalStatus approvalStatus = bookRepository.findApprovalStatusById(bookId);
            if (approvalStatus == null) {
                return false;
            }

            return approvalStatus == ApprovalStatus.APPROVED
                || bookRepository.existsByIdAndSellerId(bookId, currentSellerId(currentUser, authentication));
        }

        if ("create".equals(permission)) {
            return hasRole(authentication, UserRole.SELLER);
        }

        if ("update".equals(permission) || "edit".equals(permission) || "delete".equals(permission)) {
            Long sellerId = currentSellerId(currentUser, authentication);
            return hasRole(authentication, UserRole.SELLER)
                && sellerId != null
                && bookRepository.existsByIdAndSellerId(bookId, sellerId);
        }

        return false;
    }

    private boolean hasOrderPermission(Authentication authentication, Long orderId, String permission) {
        if (orderId == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return false;
        }

        if ("read".equals(permission) || "view".equals(permission)) {
            return orderRepository.existsByIdAndBuyerId(orderId, currentUserId(currentUser, authentication));
        }

        if ("update".equals(permission) || "edit".equals(permission) || "cancel".equals(permission)) {
            Long currentUserId = currentUserId(currentUser, authentication);
            return hasRole(authentication, UserRole.BUYER)
                && currentUserId != null
                && orderRepository.existsByIdAndBuyerId(orderId, currentUserId);
        }

        return false;
    }

    private boolean hasSubOrderPermission(Authentication authentication, Long subOrderId, String permission) {
        if (subOrderId == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return false;
        }

        if ("read".equals(permission) || "view".equals(permission)) {
            Long currentUserId = currentUserId(currentUser, authentication);
            Long sellerId = currentSellerId(currentUser, authentication);
            return (currentUserId != null && subOrderRepository.existsByIdAndBuyerId(subOrderId, currentUserId))
                || (sellerId != null && subOrderRepository.existsByIdAndSellerId(subOrderId, sellerId));
        }

        if ("update".equals(permission) || "edit".equals(permission) || "status".equals(permission)) {
            Long sellerId = currentSellerId(currentUser, authentication);
            return hasRole(authentication, UserRole.SELLER)
                && sellerId != null
                && subOrderRepository.existsByIdAndSellerId(subOrderId, sellerId);
        }

        return false;
    }

    private boolean hasUserPermission(Authentication authentication, Long userId, String permission) {
        if (userId == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return false;
        }

        Long currentUserId = currentUserId(currentUser, authentication);
        if ("read".equals(permission) || "view".equals(permission) || "update".equals(permission) || "edit".equals(permission)) {
            return Objects.equals(currentUserId, userId);
        }

        return false;
    }

    private User getCurrentUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }

    private Long currentUserId(User currentUser, Authentication authentication) {
        if (currentUser != null) {
            return currentUser.getId();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticatedPrincipal jwtPrincipal) {
            return jwtPrincipal.userId();
        }

        return null;
    }

    private Long currentSellerId(User currentUser, Authentication authentication) {
        if (currentUser != null) {
            return currentUser.getRole() == UserRole.SELLER ? currentUser.getId() : null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticatedPrincipal jwtPrincipal) {
            return jwtPrincipal.sellerId();
        }

        return null;
    }

    private boolean hasRole(Authentication authentication, UserRole role) {
        if (authentication == null || role == null) {
            return false;
        }

        String expectedAuthority = "ROLE_" + role.name();
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet());
        if (authorities.contains(expectedAuthority)) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticatedPrincipal jwtPrincipal) {
            return jwtPrincipal.roles().stream().anyMatch(r -> role.name().equalsIgnoreCase(r));
        }

        if (principal instanceof User user) {
            return user.getRole() == role;
        }

        return false;
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticatedPrincipal jwtPrincipal) {
            return jwtPrincipal.roles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role));
        }
        if (principal instanceof User user) {
            return user.getRole() == UserRole.ADMIN;
        }

        return false;
    }

    private Long toLong(Serializable targetId) {
        if (targetId instanceof Long value) {
            return value;
        }

        if (targetId instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(targetId.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}