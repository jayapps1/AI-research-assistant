package com.researchassistant.notification;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user_type", columnNames = {"user_id", "type"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 80) private NotificationType type;
    @Column(name = "in_app_enabled", nullable = false) private boolean inAppEnabled = true;
    @Column(name = "email_enabled", nullable = false) private boolean emailEnabled = true;
    @Column(name = "sms_enabled", nullable = false) private boolean smsEnabled = false;
    @Column(name = "push_enabled", nullable = false) private boolean pushEnabled = true;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); }
}
