package com.example.bookstore.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Đảm bảo có annotation này
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // 1. QUAN TRỌNG: Stateless (Không lưu session)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. Xử lý lỗi để không redirect về /login
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Thay vì redirect, ta trả về lỗi 401 Unauthorized
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Token is missing or invalid");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                                // Public routes
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()

                        // Allow safe read-only access to panel endpoints and admin pages
                        .requestMatchers(HttpMethod.GET, "/api/panel/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin", "/admin/**").permitAll()

                        // Protected routes dựa trên Role (write operations)
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/panel/**").hasRole("ADMIN")

                        // Yêu cầu đăng nhập cho Carts và Orders
                        .requestMatchers("/api/carts/**", "/api/orders/**").authenticated()

                        // Mọi request khác cho phép đi qua (hoặc để .authenticated() nếu muốn bảo mật tuyệt đối)
                        .anyRequest().permitAll()
                )
                // 3. Đưa Filter của bạn vào đúng vị trí
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}