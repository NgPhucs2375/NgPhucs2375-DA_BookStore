package com.example.bookstore.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mở "đường hầm" mapping link web /images/covers/ thẳng vào ổ cứng thư mục src
        registry.addResourceHandler("/images/covers/**")
                .addResourceLocations("file:src/main/resources/static/images/covers/");
    }
}