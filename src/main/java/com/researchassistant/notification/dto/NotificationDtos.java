package com.researchassistant.notification.dto;

import com.researchassistant.notification.DevicePlatform;
import com.researchassistant.notification.NotificationPriority;
import com.researchassistant.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}
    public record NotificationResponse(UUID id, NotificationType type, String title, String message, String actionUrl,
                                       NotificationPriority priority, OffsetDateTime createdAt, OffsetDateTime readAt) {}
    public record UnreadCountResponse(long unreadCount) {}
    public record RegisterDeviceRequest(@NotNull DevicePlatform platform, @NotBlank String pushToken, String deviceName) {}
    public record UserDeviceResponse(UUID id, DevicePlatform platform, String deviceName, boolean active, OffsetDateTime lastSeenAt) {}
}
