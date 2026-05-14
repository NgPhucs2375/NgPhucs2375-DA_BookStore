package com.example.bookstore.dto;

import com.example.bookstore.model.enums.NotificationPriority;
import com.example.bookstore.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {
    private Long userId; // null = broadcast to all
    private NotificationType type;
    private String title;
    private String message;
    private String payloadJson;
    private NotificationPriority priority;
}
