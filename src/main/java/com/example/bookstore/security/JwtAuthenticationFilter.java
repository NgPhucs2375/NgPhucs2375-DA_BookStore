package com.example.bookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Lấy JWT từ Header Authorization
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        Long userId = null;
        List<String> roles = List.of();
        Long sellerId = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                userId = jwtTokenProvider.extractUserId(token);
                roles = jwtTokenProvider.extractRoles(token);
                sellerId = jwtTokenProvider.extractSellerId(token);
            } catch (Exception e) {
                // Token không hợp lệ hoặc hết hạn, ta không set Authentication
                // Spring Security sẽ tự chặn ở EntryPoint nếu endpoint yêu cầu auth
                logger.error("Could not set user authentication in security context", e);
            }
        }

        // 2. Nếu có userId và chưa được xác thực trong Context hiện tại
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String role : roles) {
                if (role != null && !role.isBlank()) {
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(authority));
                }
            }

            JwtAuthenticatedPrincipal principal = new JwtAuthenticatedPrincipal(userId, roles, sellerId);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 3. Thiết lập thông tin xác thực vào Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 4. Luôn gọi doFilter để chuyển request cho Filter tiếp theo (hoặc Controller)
        filterChain.doFilter(request, response);
    }
}