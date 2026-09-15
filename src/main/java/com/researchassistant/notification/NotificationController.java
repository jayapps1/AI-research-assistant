package com.researchassistant.notification;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.notification.dto.NotificationDtos.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class NotificationController {
    private final AuthenticatedUserResolver userResolver;
    private final NotificationService notificationService;
    private final UserDeviceService userDeviceService;

    public NotificationController(AuthenticatedUserResolver userResolver, NotificationService notificationService, UserDeviceService userDeviceService) {
        this.userResolver = userResolver;
        this.notificationService = notificationService;
        this.userDeviceService = userDeviceService;
    }

    @GetMapping("/api/v1/me/notifications")
    public Page<NotificationResponse> notifications(Pageable pageable, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return notificationService.list(user.getId(), pageable).map(this::toResponse);
    }

    @GetMapping("/api/v1/me/notifications/unread-count")
    public UnreadCountResponse unread(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return new UnreadCountResponse(notificationService.unreadCount(user.getId()));
    }

    @PostMapping("/api/v1/me/notifications/{id}/read")
    public NotificationResponse read(@PathVariable UUID id, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return toResponse(notificationService.markRead(id, user.getId()));
    }

    @PostMapping("/api/v1/me/notifications/read-all")
    public void readAll(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        notificationService.markAllRead(user.getId());
    }

    @PostMapping("/api/v1/me/devices")
    public UserDeviceResponse registerDevice(@Valid @RequestBody RegisterDeviceRequest request, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return toDevice(userDeviceService.register(user, request.platform(), request.pushToken(), request.deviceName()));
    }

    @GetMapping("/api/v1/me/devices")
    public List<UserDeviceResponse> devices(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return userDeviceService.list(user.getId()).stream().map(this::toDevice).toList();
    }

    @DeleteMapping("/api/v1/me/devices/{deviceId}")
    public void deleteDevice(@PathVariable UUID deviceId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        userDeviceService.delete(deviceId, user.getId());
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getActionUrl(), n.getPriority(), n.getCreatedAt(), n.getReadAt());
    }

    private UserDeviceResponse toDevice(UserDevice d) {
        return new UserDeviceResponse(d.getId(), d.getPlatform(), d.getDeviceName(), d.isActive(), d.getLastSeenAt());
    }
}
