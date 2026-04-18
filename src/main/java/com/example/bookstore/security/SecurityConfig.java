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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Kích hoạt CORS toàn cầu lấy cấu hình từ Bean bên dưới
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 1. QUAN TRỌNG: Stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. Xử lý lỗi
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Token is missing or invalid");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // Public routes
                        .requestMatchers("/api/auth/register-admin").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/panel/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin", "/admin/**").permitAll()

                        // Đã bổ sung bảo mật cho API Sách của Seller
                        .requestMatchers("/api/books/seller/**").hasRole("SELLER")

                        // Protected routes dựa trên Role
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/panel/**").hasRole("ADMIN")

                        // Yêu cầu đăng nhập cho Carts và Orders
                        .requestMatchers("/api/carts/**", "/api/orders/**").authenticated()

                        .anyRequest().permitAll()
                )
                // 3. Đưa Filter vào đúng vị trí
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BEAN CẤU HÌNH CORS TOÀN CẦU: Khắc phục triệt để lỗi (failed) net::ERR_FAILED
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép Frontend (ví dụ localhost:3000, 5173,...) gọi API thoải mái
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}