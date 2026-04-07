package com.example.bookstore.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    // Policy này chỉ cho phép một số thẻ HTML cơ bản định dạng văn bản, CẤM TIỆT <script> hay các event onclick
    private static final PolicyFactory SANITIZER_POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "u", "strong", "em", "p", "br", "ul", "ol", "li")
            .toFactory();

    // Hàm này để gọi ở Controller hoặc Service
    public String sanitizeHtml(String input) {
        if (input == null) return null;
        return SANITIZER_POLICY.sanitize(input);
    }
}