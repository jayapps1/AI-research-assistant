package com.researchassistant.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries",
        indexes = @Index(name = "idx_notification_deliveries_status_retry", columnList = "status,next_attempt_at"))
@Getter
@Setter
@NoArgsConstructor
public class NotificationDelivery {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "notification_id", nullable = false) private Notification notification;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NotificationChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;
    @Column(length = 80) private String provider;
    @Column(name = "provider_message_id", length = 255) private String providerMessageId;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount = 0;
    @Column(name = "next_attempt_at") private OffsetDateTime nextAttemptAt;
    @Column(name = "sent_at") private OffsetDateTime sentAt;
    @Column(name = "delivered_at") private OffsetDateTime deliveredAt;
    @Column(name = "failure_code", length = 100) private String failureCode;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); OffsetDateTime now=OffsetDateTime.now(); if(createdAt==null) createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate(){ updatedAt=OffsetDateTime.now(); }
}
