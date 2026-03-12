package com.revpay.notification_service.controller;

import com.revpay.notification_service.entity.Notification;
import com.revpay.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    // Called by other services (via Feign) to create a notification
    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(
            @RequestParam("userId") Long userId,
            @RequestParam("message") String message,
            @RequestParam(value = "type", defaultValue = "NOTIFICATION") String type) {
        log.info("Sending notification to user {}: {} [Type: {}]", userId, message, type);
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .isRead(false)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        return ResponseEntity.ok().build();
    }

    // Called by the frontend to get notifications for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        
        Map<String, Object> response = Map.of(
                "success", true,
                "data", notifications,
                "message", "Notifications retrieved"
        );
        return ResponseEntity.ok(response);
    }

    // Mark a notification as read
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("success", true, "message", "Marked as read"));
    }

    // Test notification endpoint
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testNotification() {
        // Create a test notification for user ID 5
        Notification notification = Notification.builder()
                .userId(5L)
                .message("This is a test notification from RevPay!")
                .type("NOTIFICATION")
                .isRead(false)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("success", true, "message", "Test notification sent to user 5"));
    }
}
