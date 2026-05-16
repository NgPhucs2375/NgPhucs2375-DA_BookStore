package com.example.bookstore.controller;

import com.example.bookstore.dto.WishlistActionResponse;
import com.example.bookstore.dto.WishlistItemResponse;
import com.example.bookstore.security.JwtAuthenticatedPrincipal;
import com.example.bookstore.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<WishlistItemResponse> getMyWishlist(HttpServletRequest request) {
        return wishlistService.getWishlist(getCurrentUserId(request));
    }

    @PostMapping("/me/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public WishlistActionResponse toggleWishlist(HttpServletRequest request, @PathVariable Long bookId) {
        return wishlistService.toggleWishlist(getCurrentUserId(request), bookId);
    }

    @DeleteMapping("/me/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public WishlistActionResponse removeFromWishlist(HttpServletRequest request, @PathVariable Long bookId) {
        return wishlistService.removeFromWishlist(getCurrentUserId(request), bookId);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticatedPrincipal jwtPrincipal) {
            return jwtPrincipal.userId();
        }

        Principal principal = request.getUserPrincipal();
        if (principal instanceof org.springframework.security.core.Authentication requestAuthentication
                && requestAuthentication.getPrincipal() instanceof JwtAuthenticatedPrincipal jwtPrincipal) {
            return jwtPrincipal.userId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap");
    }
}
