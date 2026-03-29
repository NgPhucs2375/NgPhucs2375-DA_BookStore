package com.example.bookstore.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String username;
    private String shopName;
    private String shopAddress;
}
