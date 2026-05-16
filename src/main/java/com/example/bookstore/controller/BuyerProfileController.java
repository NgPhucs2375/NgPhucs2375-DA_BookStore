package com.example.bookstore.controller;

import com.example.bookstore.dto.ChangePasswordDTO;
import com.example.bookstore.dto.UserAddressDTO;
import com.example.bookstore.dto.UserProfileDTO;
import com.example.bookstore.security.JwtAuthenticatedPrincipal;
import com.example.bookstore.service.BuyerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Controller
@RequestMapping("/buyer/profile")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
public class BuyerProfileController {

    private final BuyerProfileService buyerProfileService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtAuthenticatedPrincipal principal) {
            return principal.userId();
        }

        throw new ResponseStatusException(UNAUTHORIZED, "Vui lòng đăng nhập");
    }

    // ==================== UI PAGES ====================

    @GetMapping("/dashboard")
    public String profileDashboard(Model model) {
        Long userId = getCurrentUserId();
        UserProfileDTO profile = buyerProfileService.getUserProfile(userId);
        List<UserAddressDTO> addresses = buyerProfileService.getUserAddresses(userId);
        List<?> securityEvents = buyerProfileService.getSecurityEvents(userId);
        
        model.addAttribute("profile", profile);
        model.addAttribute("addresses", addresses);
        model.addAttribute("securityEvents", securityEvents);
        
        return "buyer/Buyer_Profile_Dashboard";
    }

    @GetMapping("/edit")
    public String editProfile(Model model) {
        Long userId = getCurrentUserId();
        UserProfileDTO profile = buyerProfileService.getUserProfile(userId);
        model.addAttribute("profile", profile);
        return "buyer/Buyer_Profile_Edit";
    }

    @GetMapping("/addresses")
    public String manageAddresses(Model model) {
        Long userId = getCurrentUserId();
        List<UserAddressDTO> addresses = buyerProfileService.getUserAddresses(userId);
        model.addAttribute("addresses", addresses);
        return "buyer/Buyer_Address_Management";
    }

    @GetMapping("/account-settings")
    public String accountSettings(Model model) {
        Long userId = getCurrentUserId();
        UserProfileDTO profile = buyerProfileService.getUserProfile(userId);
        model.addAttribute("profile", profile);
        return "buyer/Buyer_Account_Settings";
    }

    @GetMapping("/security")
    public String securitySettings(Model model) {
        Long userId = getCurrentUserId();
        List<?> securityEvents = buyerProfileService.getSecurityEvents(userId);
        model.addAttribute("securityEvents", securityEvents);
        return "buyer/Buyer_Security_Settings";
    }

    // ==================== API ENDPOINTS ====================

    // Profile Management
    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<UserProfileDTO> getProfile() {
        Long userId = getCurrentUserId();
        UserProfileDTO profile = buyerProfileService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<UserProfileDTO> updateProfile(@RequestBody UserProfileDTO profileDTO) {
        Long userId = getCurrentUserId();
        UserProfileDTO updated = buyerProfileService.updateUserProfile(userId, profileDTO);
        return ResponseEntity.ok(updated);
    }

    // Address Management
    @GetMapping("/api/addresses")
    @ResponseBody
    public ResponseEntity<List<UserAddressDTO>> getAddresses() {
        Long userId = getCurrentUserId();
        List<UserAddressDTO> addresses = buyerProfileService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/api/addresses/create")
    @ResponseBody
    public ResponseEntity<UserAddressDTO> createAddress(@RequestBody UserAddressDTO addressDTO) {
        Long userId = getCurrentUserId();
        UserAddressDTO created = buyerProfileService.createUserAddress(userId, addressDTO);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/api/addresses/{addressId}/update")
    @ResponseBody
    public ResponseEntity<UserAddressDTO> updateAddress(
            @PathVariable Long addressId,
            @RequestBody UserAddressDTO addressDTO) {
        Long userId = getCurrentUserId();
        UserAddressDTO updated = buyerProfileService.updateUserAddress(userId, addressId, addressDTO);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/api/addresses/{addressId}/delete")
    @ResponseBody
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId) {
        Long userId = getCurrentUserId();
        buyerProfileService.deleteUserAddress(userId, addressId);
        return ResponseEntity.ok("Address deleted successfully");
    }

    @PostMapping("/api/addresses/{addressId}/set-default")
    @ResponseBody
    public ResponseEntity<String> setDefaultAddress(@PathVariable Long addressId) {
        Long userId = getCurrentUserId();
        buyerProfileService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok("Default address set successfully");
    }

    // Security Management
    @PostMapping("/api/security/change-password")
    @ResponseBody
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        Long userId = getCurrentUserId();
        try {
            buyerProfileService.changePassword(userId, changePasswordDTO);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/security/events")
    @ResponseBody
    public ResponseEntity<?> getSecurityEvents() {
        Long userId = getCurrentUserId();
        List<?> events = buyerProfileService.getSecurityEvents(userId);
        return ResponseEntity.ok(events);
    }
}
