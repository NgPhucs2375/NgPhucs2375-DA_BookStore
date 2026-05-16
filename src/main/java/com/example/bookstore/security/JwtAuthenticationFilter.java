package com.example.bookstore.security;

import com.example.bookstore.model.User;
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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Lấy JWT từ Header Authorization
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        Long userId = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                userId = jwtTokenProvider.extractUserId(token);
            } catch (Exception e) {
                // Token không hợp lệ hoặc hết hạn, ta không set Authentication
                // Spring Security sẽ tự chặn ở EntryPoint nếu endpoint yêu cầu auth
                logger.error("Could not set user authentication in security context", e);
            }
        }

        // 2. Nếu có userId 
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                // Luôn set attribute để Controller sử dụng
                request.setAttribute("CURRENT_USER_ID", user.getId());
                request.setAttribute("CURRENT_USER_ROLE", user.getRole().name());

                // Chỉ set Authentication nếu chưa có (Stateless app thường null mỗi request)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    String roleName = "ROLE_" + user.getRole().name();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority(roleName))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // 4. Luôn gọi doFilter để chuyển request cho Filter tiếp theo (hoặc Controller)
        filterChain.doFilter(request, response);
    }
}