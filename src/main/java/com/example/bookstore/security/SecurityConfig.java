package com.example.bookstore.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Thêm import này cho HttpMethod.GET
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. Các public route cũ của bạn
                .requestMatchers("/api/auth/**").permitAll()
                
                // 2. THÊM MỚI: Cấu hình cho tính năng Shop
                .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll() // Khách được phép xem chi tiết shop
                .requestMatchers("/api/seller/**").hasRole("SELLER")          // Chỉ Chủ shop (SELLER) mới được tạo/sửa shop của họ
                
                // 3. Các private route cũ của bạn
                .requestMatchers("/api/carts/**", "/api/orders/**").authenticated()
                
                // 4. Các route còn lại
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}