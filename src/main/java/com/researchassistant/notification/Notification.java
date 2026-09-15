package com.researchassistant.notification;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {@Index(name = "idx_notifications_recipient_created", columnList = "recipient_id,created_at"),
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id,read_at")})
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recipient_id", nullable = false) private User recipient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "workspace_id") private Workspace workspace;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id") private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 80) private NotificationType type;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, length = 1000) private String message;
    @Column(name = "action_url", length = 1000) private String actionUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NotificationPriority priority = NotificationPriority.NORMAL;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "read_at") private OffsetDateTime readAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(createdAt==null) createdAt=OffsetDateTime.now(); }
}
