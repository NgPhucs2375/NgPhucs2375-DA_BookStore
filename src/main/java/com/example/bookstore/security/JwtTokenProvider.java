package com.example.bookstore.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final byte[] secret;
    private final long expirationSeconds;

    public JwtTokenProvider(
        @Value("${app.security.jwt.secret:bookstore-dev-secret-change-me}") String secret,
        @Value("${app.security.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(Long userId, String role) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expirationSeconds;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"userId\":%d,\"role\":\"%s\",\"iat\":%d,\"exp\":%d}", userId, role, now, exp);

        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public Long extractUserId(String token) {
        JsonNode payload = validateAndParse(token);
        if (payload == null || !payload.has("userId")) {
            return null;
        }
        return payload.get("userId").asLong();
    }

    private JsonNode validateAndParse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = OBJECT_MAPPER.readTree(payloadBytes);

            if (!payload.has("exp")) {
                return null;
            }

            long exp = payload.get("exp").asLong();
            long now = Instant.now().getEpochSecond();
            if (exp < now) {
                return null;
            }

            return payload;
        } catch (Exception ex) {
            return null;
        }
    }

    private String sign(String input) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] signature = hmac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign JWT", ex);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
