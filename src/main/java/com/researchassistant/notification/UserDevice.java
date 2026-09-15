package com.researchassistant.notification;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_devices",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_devices_push_token", columnNames = "push_token"),
        indexes = @Index(name = "idx_user_devices_user_active", columnList = "user_id,active"))
@Getter
@Setter
@NoArgsConstructor
public class UserDevice {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DevicePlatform platform;
    @Column(name = "push_token", nullable = false, length = 1000) private String pushToken;
    @Column(name = "device_name", length = 255) private String deviceName;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "last_seen_at") private OffsetDateTime lastSeenAt;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); OffsetDateTime now=OffsetDateTime.now(); if(createdAt==null) createdAt=now; updatedAt=now; if(lastSeenAt==null) lastSeenAt=now; }
    @PreUpdate void onUpdate(){ updatedAt=OffsetDateTime.now(); }
}
