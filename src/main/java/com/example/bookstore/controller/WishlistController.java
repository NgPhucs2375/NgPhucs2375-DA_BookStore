package com.example.bookstore.controller;

import com.example.bookstore.dto.WishlistActionResponse;
import com.example.bookstore.dto.WishlistItemResponse;
import com.example.bookstore.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping("/me")
    public List<WishlistItemResponse> getMyWishlist(HttpServletRequest request) {
        return wishlistService.getWishlist(getCurrentUserId(request));
    }

    @PostMapping("/me/{bookId}")
    public WishlistActionResponse toggleWishlist(HttpServletRequest request, @PathVariable Long bookId) {
        return wishlistService.toggleWishlist(getCurrentUserId(request), bookId);
    }

    @DeleteMapping("/me/{bookId}")
    public WishlistActionResponse removeFromWishlist(HttpServletRequest request, @PathVariable Long bookId) {
        return wishlistService.removeFromWishlist(getCurrentUserId(request), bookId);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object currentUserId = request.getAttribute("CURRENT_USER_ID");
        if (!(currentUserId instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap");
        }
        return userId;
    }
}
