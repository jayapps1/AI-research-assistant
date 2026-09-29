package com.researchassistant.conversation.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "conversations",
        indexes = {
                @Index(name = "idx_conversations_user_last_message", columnList = "user_id,last_message_at"),
                @Index(name = "idx_conversations_user_status_last_message", columnList = "user_id,status,last_message_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ResearchProject project;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private ConversationType type = ConversationType.GENERAL;

    @Column(name = "title", nullable = false, length = 255)
    private String title = "New Search";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (type == null) {
            type = ConversationType.GENERAL;
        }
        if (status == null) {
            status = ConversationStatus.ACTIVE;
        }
        if (title == null || title.isBlank()) {
            title = "New Search";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
