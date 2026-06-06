package com.example.bookstore.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Kích hoạt CORS toàn cầu lấy cấu hình từ Bean bên dưới
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // --- VŨ KHÍ TẮT POPUP MẶC ĐỊNH ---
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // ---------------------------------

                // 1. QUAN TRỌNG: Stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. Xử lý lỗi
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String acceptHeader = request.getHeader("Accept");
                            // Nếu là request từ browser (HTML), redirect về trang đăng nhập
                            if (acceptHeader != null && acceptHeader.contains("text/html")) {
                                response.sendRedirect("/main/auth");
                            } else {
                                // API request -> trả về 401
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Token is missing or invalid");
                            }
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // Public routes
                        .requestMatchers("/api/auth/register-admin").hasRole("ADMIN")
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                        .requestMatchers("/admin", "/admin/**").permitAll()
                        .requestMatchers("/seller", "/seller/**").permitAll()
                        .requestMatchers("/buyer", "/buyer/**").permitAll()

                        .requestMatchers("/api/admin/seeder/**").hasRole("ADMIN")

                        // Protected routes dựa trên Role
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/books/seller/**").hasRole("SELLER")

                        .requestMatchers("/api/panel/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/admin").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/admin/**").hasRole("ADMIN")

                        // Yêu cầu đăng nhập cho Carts, Orders và Notifications
                        .requestMatchers("/api/carts/**", "/api/orders/**", "/api/notifications/**").authenticated()

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

    // Provide a PasswordEncoder bean required by services that encode/verify passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(CustomPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }
}