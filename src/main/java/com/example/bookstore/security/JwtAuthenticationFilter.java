package com.example.bookstore.security;

import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!isProtectedPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = resolveUserId(request);
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authentication");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
            return;
        }

        UserRole requiredRole = requiredRoleFor(uri);
        if (requiredRole != null && user.getRole() != requiredRole) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role is not allowed for this endpoint");
            return;
        }

        if (!matchesPathIdentity(uri, userId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot access another user's resource");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setAttribute("CURRENT_USER_ID", userId);
        request.setAttribute("CURRENT_USER_ROLE", user.getRole().name());
        filterChain.doFilter(request, response);
    }

    private Long resolveUserId(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long tokenUserId = jwtTokenProvider.extractUserId(token);
            if (tokenUserId != null) {
                return tokenUserId;
            }
        }

        // Backward-compatible fallback while clients are migrating to Bearer JWT.
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isProtectedPath(String uri) {
        return uri.startsWith("/api/carts/") || uri.startsWith("/api/orders/");
    }

    private UserRole requiredRoleFor(String uri) {
        if (uri.startsWith("/api/carts/")) {
            return UserRole.BUYER;
        }

        if (uri.startsWith("/api/orders/me") || uri.startsWith("/api/orders/buyer/")) {
            return UserRole.BUYER;
        }

        if (uri.startsWith("/api/orders/seller/") || uri.startsWith("/api/orders/sub-orders/")) {
            return UserRole.SELLER;
        }

        return null;
    }

    private boolean matchesPathIdentity(String uri, Long userId) {
        if (uri.startsWith("/api/carts/buyer/")) {
            Long pathId = extractNumericPathSegment(uri, "/api/carts/buyer/");
            return pathId == null || pathId.equals(userId);
        }

        if (uri.startsWith("/api/orders/buyer/")) {
            Long pathId = extractNumericPathSegment(uri, "/api/orders/buyer/");
            return pathId == null || pathId.equals(userId);
        }

        if (uri.startsWith("/api/orders/seller/")) {
            Long pathId = extractNumericPathSegment(uri, "/api/orders/seller/");
            return pathId == null || pathId.equals(userId);
        }

        return true;
    }

    private Long extractNumericPathSegment(String uri, String prefix) {
        if (!uri.startsWith(prefix)) {
            return null;
        }

        String rest = uri.substring(prefix.length());
        String firstSegment = rest.contains("/") ? rest.substring(0, rest.indexOf('/')) : rest;
        try {
            return Long.parseLong(firstSegment);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
