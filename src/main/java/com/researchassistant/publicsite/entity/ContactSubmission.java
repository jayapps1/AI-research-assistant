package com.researchassistant.publicsite.entity;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contact_submissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_contact_submissions_ref", columnNames = "reference_code"),
        indexes = {
                @Index(name = "idx_contact_submissions_status_time", columnList = "status, submitted_at"),
                @Index(name = "idx_contact_submissions_ref", columnList = "reference_code"),
                @Index(name = "idx_contact_submissions_email", columnList = "email")
        })
@Getter
@Setter
@NoArgsConstructor
public class ContactSubmission {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "reference_code", nullable = false, length = 40)
    private String referenceCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactSubmissionStatus status = ContactSubmissionStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactSubmissionSource source = ContactSubmissionSource.PUBLIC_WEBSITE;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "user_agent_summary", length = 255)
    private String userAgentSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "first_read_at")
    private OffsetDateTime firstReadAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ContactResponse> responses = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (submittedAt == null) {
            submittedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
