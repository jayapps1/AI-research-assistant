package com.researchassistant.subscription;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_renewal_reminders",
        uniqueConstraints = @UniqueConstraint(name = "uk_renewal_reminders_sub_type_end", columnNames = {"subscription_id", "reminder_type", "current_period_end"}),
        indexes = {
                @Index(name = "idx_renewal_reminders_sub_end", columnList = "subscription_id,current_period_end")
        })
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionRenewalReminder {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private WorkspaceSubscription subscription;

    @Column(name = "reminder_type", nullable = false, length = 40)
    private String reminderType;

    @Column(name = "current_period_end", nullable = false)
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "scheduled_for", nullable = false)
    private OffsetDateTime scheduledFor;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @Column(name = "notification_id")
    private UUID notificationId;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (scheduledFor == null) {
            scheduledFor = now;
        }
        if (sentAt == null) {
            sentAt = now;
        }
    }
}
