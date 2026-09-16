package com.researchassistant.publicsite.entity;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_responses",
        indexes = @Index(name = "idx_contact_responses_submission", columnList = "submission_id"))
@Getter
@Setter
@NoArgsConstructor
public class ContactResponse {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private ContactSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responded_by", nullable = false)
    private User respondedBy;

    @Column(name = "response_message", nullable = false, columnDefinition = "TEXT")
    private String responseMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactResponseChannel channel = ContactResponseChannel.EMAIL;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactResponseStatus status = ContactResponseStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
