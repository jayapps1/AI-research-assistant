package com.researchassistant.subscription;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.identity.entity.User;
import com.researchassistant.notification.Notification;
import com.researchassistant.notification.NotificationPriority;
import com.researchassistant.notification.NotificationService;
import com.researchassistant.notification.NotificationType;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SubscriptionRenewalReminderService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRenewalReminderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final WorkspaceSubscriptionRepository subscriptionRepository;
    private final SubscriptionRenewalReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final SubscriptionProperties subscriptionProperties;
    private final WorkspaceMembershipRepository membershipRepository;
    private final FreeSubscriptionProvisioningService freeSubscriptionProvisioningService;
    private final AuditEventService auditEventService;

    public SubscriptionRenewalReminderService(
            WorkspaceSubscriptionRepository subscriptionRepository,
            SubscriptionRenewalReminderRepository reminderRepository,
            NotificationService notificationService,
            SubscriptionProperties subscriptionProperties,
            WorkspaceMembershipRepository membershipRepository,
            FreeSubscriptionProvisioningService freeSubscriptionProvisioningService,
            AuditEventService auditEventService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
        this.subscriptionProperties = subscriptionProperties;
        this.membershipRepository = membershipRepository;
        this.freeSubscriptionProvisioningService = freeSubscriptionProvisioningService;
        this.auditEventService = auditEventService;
    }

    /**
     * Executes the daily check:
     * 1. Expires unrenewed paid subscriptions that have reached period end and falls back to FREE.
     * 2. Sends idempotent renewal reminders for active subscriptions according to configured days (7, 3, 1, 0).
     */
    public void processRenewalRemindersAndExpirations() {
        OffsetDateTime now = OffsetDateTime.now();
        log.info("Running subscription renewal reminders and expiration check at {}", now);

        processExpirations(now);
        if (subscriptionProperties.enabled()) {
            processReminders(now);
        }
    }

    private void processExpirations(OffsetDateTime now) {
        List<WorkspaceSubscription> activeSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == WorkspaceSubscriptionStatus.ACTIVE || s.getStatus() == WorkspaceSubscriptionStatus.PAST_DUE)
                .filter(s -> s.getBillingInterval() != BillingInterval.NONE)
                .filter(s -> s.getCurrentPeriodEnd() != null && !s.getCurrentPeriodEnd().isAfter(now))
                .toList();

        for (WorkspaceSubscription sub : activeSubscriptions) {
            try {
                sub.setStatus(WorkspaceSubscriptionStatus.EXPIRED);
                subscriptionRepository.saveAndFlush(sub);

                auditEventService.record(null, "SYSTEM", sub.getWorkspace(), null, AuditEventType.SUBSCRIPTION_EXPIRED,
                        "WorkspaceSubscription", sub.getId(), "{\"plan\":\"" + sub.getPlan().getCode() + "\"}");

                // Notify workspace owner of expiration
                membershipRepository.findByWorkspaceIdAndRole(sub.getWorkspace().getId(), WorkspaceRole.OWNER)
                        .map(WorkspaceMembership::getUser)
                        .ifPresent(owner -> notificationService.create(
                                owner,
                                sub.getWorkspace(),
                                null,
                                NotificationType.SUBSCRIPTION_EXPIRING,
                                "Subscription expired",
                                "Your " + sub.getPlan().getName() + " plan has expired. Your workspace has fallen back to the Free plan. Renew anytime to restore full limits.",
                                "/app/billing",
                                NotificationPriority.HIGH
                        ));

                // Idempotently ensure standard FREE subscription
                freeSubscriptionProvisioningService.ensureFreeSubscription(sub.getWorkspace().getId());
                log.info("Expired subscription {} for workspace {} and transitioned to FREE.", sub.getId(), sub.getWorkspace().getId());
            } catch (Exception ex) {
                log.error("Failed to process expiration for subscription {}: {}", sub.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void processReminders(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        List<Integer> daysBefore = subscriptionProperties.daysBefore();

        List<WorkspaceSubscription> activeSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == WorkspaceSubscriptionStatus.ACTIVE)
                .filter(s -> s.getBillingInterval() != BillingInterval.NONE) // FREE plans never receive renewal reminders
                .filter(s -> s.getCurrentPeriodEnd() != null && s.getCurrentPeriodEnd().isAfter(now))
                .toList();

        for (WorkspaceSubscription sub : activeSubscriptions) {
            LocalDate expiryDate = sub.getCurrentPeriodEnd().toLocalDate();
            long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);

            if (daysUntilExpiry < 0 || !daysBefore.contains((int) daysUntilExpiry)) {
                continue;
            }

            String reminderType = mapReminderType((int) daysUntilExpiry);
            if (reminderType == null) {
                continue;
            }

            // Check idempotency: do not duplicate reminders for the same subscription, type, and period end
            if (reminderRepository.existsBySubscriptionIdAndReminderTypeAndCurrentPeriodEnd(
                    sub.getId(), reminderType, sub.getCurrentPeriodEnd())) {
                continue;
            }

            // Send reminder
            sendReminder(sub, reminderType, (int) daysUntilExpiry, now);
        }
    }

    private void sendReminder(WorkspaceSubscription sub, String reminderType, int daysUntilExpiry, OffsetDateTime now) {
        User owner = membershipRepository.findByWorkspaceIdAndRole(sub.getWorkspace().getId(), WorkspaceRole.OWNER)
                .map(WorkspaceMembership::getUser)
                .orElse(null);

        if (owner == null) {
            log.warn("Cannot send renewal reminder for workspace {}: no owner found.", sub.getWorkspace().getId());
            return;
        }

        String planName = sub.getPlan().getName();
        String formattedExpiryDate = sub.getCurrentPeriodEnd().format(DATE_FORMATTER);
        String message = buildReminderMessage(sub.getPlan().getCode(), planName, daysUntilExpiry, formattedExpiryDate);

        NotificationPriority priority = daysUntilExpiry <= 1 ? NotificationPriority.HIGH : NotificationPriority.NORMAL;

        Notification notification = notificationService.create(
                owner,
                sub.getWorkspace(),
                null,
                NotificationType.SUBSCRIPTION_EXPIRING,
                "Subscription renewal reminder",
                message,
                "/app/billing",
                priority
        );

        SubscriptionRenewalReminder record = new SubscriptionRenewalReminder();
        record.setSubscription(sub);
        record.setReminderType(reminderType);
        record.setCurrentPeriodEnd(sub.getCurrentPeriodEnd());
        record.setScheduledFor(now);
        record.setSentAt(now);
        record.setNotificationId(notification.getId());

        reminderRepository.save(record);
        log.info("Sent renewal reminder [{}] to user {} for subscription {} (expires in {} days on {}).",
                reminderType, owner.getId(), sub.getId(), daysUntilExpiry, formattedExpiryDate);
    }

    private String buildReminderMessage(String planCode, String planName, int daysUntilExpiry, String formattedDate) {
        String code = planCode == null ? "" : planCode.toUpperCase();
        if ("PRO".equals(code)) {
            if (daysUntilExpiry == 1) {
                return "Your Professional plan expires tomorrow.";
            } else if (daysUntilExpiry == 0) {
                return "Your Professional plan expires today. Renew now to keep Professional features.";
            } else {
                return "Your Professional plan expires in " + daysUntilExpiry + " days. Renew before " + formattedDate + " to keep Professional features.";
            }
        } else if ("STUDENT".equals(code)) {
            if (daysUntilExpiry == 1) {
                return "Your Student plan expires tomorrow. Renew before " + formattedDate + " to keep Student features.";
            } else if (daysUntilExpiry == 0) {
                return "Your Student plan expires today. Renew now to keep Student features.";
            } else {
                return "Your Student plan expires in " + daysUntilExpiry + " days. Renew before " + formattedDate + " to keep Student features.";
            }
        } else {
            if (daysUntilExpiry == 1) {
                return "Your " + planName + " plan expires tomorrow.";
            } else if (daysUntilExpiry == 0) {
                return "Your " + planName + " plan expires today. Renew now to keep " + planName + " features.";
            } else {
                return "Your " + planName + " plan expires in " + daysUntilExpiry + " days. Renew before " + formattedDate + " to keep " + planName + " features.";
            }
        }
    }

    private String mapReminderType(int daysUntilExpiry) {
        return switch (daysUntilExpiry) {
            case 7 -> "EXPIRING_7_DAYS";
            case 3 -> "EXPIRING_3_DAYS";
            case 1 -> "EXPIRING_1_DAY";
            case 0 -> "EXPIRES_TODAY";
            default -> null;
        };
    }
}
