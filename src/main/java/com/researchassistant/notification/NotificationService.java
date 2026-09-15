package com.researchassistant.notification;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final Map<NotificationChannel, NotificationProvider> providers;

    public NotificationService(NotificationRepository notificationRepository, NotificationDeliveryRepository deliveryRepository,
                               NotificationPreferenceRepository preferenceRepository, UserRepository userRepository,
                               List<NotificationProvider> providers) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.providers = providers.stream().collect(Collectors.toMap(NotificationProvider::channel, Function.identity()));
    }

    public Notification create(User recipient, Workspace workspace, ResearchProject project, NotificationType type,
                               String title, String message, String actionUrl, NotificationPriority priority) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setWorkspace(workspace);
        notification.setProject(project);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(safeMessage(message));
        notification.setActionUrl(actionUrl);
        notification.setPriority(priority == null ? NotificationPriority.NORMAL : priority);
        notification = notificationRepository.save(notification);
        createDeliveryIfEnabled(notification, NotificationChannel.IN_APP);
        createDeliveryIfEnabled(notification, NotificationChannel.EMAIL);
        return notification;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
    }

    public Notification markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipient().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (notification.getReadAt() == null) notification.setReadAt(OffsetDateTime.now());
        return notification;
    }

    public void markAllRead(UUID userId) {
        notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .forEach(n -> { if (n.getReadAt() == null) n.setReadAt(OffsetDateTime.now()); });
    }

    private void createDeliveryIfEnabled(Notification notification, NotificationChannel channel) {
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndType(notification.getRecipient().getId(), notification.getType())
                .orElse(null);
        boolean enabled = switch (channel) {
            case IN_APP -> preference == null || preference.isInAppEnabled();
            case EMAIL -> preference == null || preference.isEmailEnabled();
            case SMS -> preference != null && preference.isSmsEnabled();
            case PUSH -> preference == null || preference.isPushEnabled();
        };
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(notification);
        delivery.setChannel(channel);
        if (!enabled || channel == NotificationChannel.IN_APP) {
            delivery.setStatus(channel == NotificationChannel.IN_APP ? NotificationDeliveryStatus.DELIVERED : NotificationDeliveryStatus.SKIPPED);
            deliveryRepository.save(delivery);
            return;
        }
        NotificationProvider provider = providers.get(channel);
        deliveryRepository.save(provider == null ? delivery : provider.send(delivery));
    }

    private String safeMessage(String message) {
        return message == null ? "" : message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
