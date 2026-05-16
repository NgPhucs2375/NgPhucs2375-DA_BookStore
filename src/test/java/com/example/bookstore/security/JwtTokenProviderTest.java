package com.example.bookstore.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtTokenProviderTest {

    @Test
    void tokenShouldCarryUserRolesAndSellerId() {
        JwtTokenProvider provider = new JwtTokenProvider("test-secret", 3600);

        String token = provider.createToken(42L, List.of("SELLER", "BUYER"), 42L);

        assertEquals(42L, provider.extractUserId(token));
        assertEquals(List.of("SELLER", "BUYER"), provider.extractRoles(token));
        assertEquals(42L, provider.extractSellerId(token));
    }

    @Test
    void sellerIdShouldBeNullForBuyerToken() {
        JwtTokenProvider provider = new JwtTokenProvider("test-secret", 3600);

        String token = provider.createToken(99L, List.of("BUYER"), null);

        assertEquals(99L, provider.extractUserId(token));
        assertEquals(List.of("BUYER"), provider.extractRoles(token));
        assertNull(provider.extractSellerId(token));
    }
}