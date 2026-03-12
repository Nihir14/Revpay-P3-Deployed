package com.revpay.notification_service.controller;

import com.revpay.notification_service.entity.Notification;
import com.revpay.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void sendNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        ResponseEntity<Void> response = notificationController.sendNotification(1L, "Test Message", "INFO");

        assertEquals(200, response.getStatusCode().value());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getNotifications_Success() {
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .message("Test Message")
                .build();
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class))).thenReturn(page);

        ResponseEntity<Map<String, Object>> response = notificationController.getNotifications(1L, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(page, response.getBody().get("data"));
    }

    @Test
    void markAsRead_Success() {
        Notification notification = Notification.builder()
                .id(1L)
                .isRead(false)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        ResponseEntity<Map<String, Object>> response = notificationController.markAsRead(1L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(notification.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }
}
