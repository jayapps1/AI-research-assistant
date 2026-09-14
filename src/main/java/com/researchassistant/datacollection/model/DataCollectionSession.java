package com.researchassistant.datacollection.model;

import com.researchassistant.identity.entity.User;
import com.researchassistant.instruments.entity.ResearchInstrument;
import com.researchassistant.methodology.entity.DataCollectionMethod;
import com.researchassistant.participant.model.Participant;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "data_collection_sessions", indexes = @Index(name = "idx_sessions_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class DataCollectionSession {
    public enum Type { QUESTIONNAIRE, INTERVIEW, FOCUS_GROUP, OBSERVATION, RECORD_REVIEW, SECONDARY_DATA, OTHER }
    public enum Status { SCHEDULED, IN_PROGRESS, COMPLETED, PARTIAL, CANCELLED, INVALIDATED }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false)
    private ResearchInstrument instrument;
    @Column(name = "instrument_revision_number", nullable = false)
    private int instrumentRevisionNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "data_collection_method_id")
    private DataCollectionMethod method;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "participant_id")
    private Participant participant;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Type type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Status status = Status.SCHEDULED;
    @Column(name = "session_code", nullable = false, length = 40)
    private String sessionCode;
    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;
    @Column(name = "started_at")
    private OffsetDateTime startedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @Column(name = "location_or_setting", columnDefinition = "TEXT")
    private String locationOrSetting;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "collected_by")
    private User collectedBy;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @ManyToMany
    @JoinTable(name = "data_collection_session_participants", joinColumns = @JoinColumn(name = "session_id"), inverseJoinColumns = @JoinColumn(name = "participant_id"))
    private Set<Participant> participants = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = Status.SCHEDULED; if (instrumentRevisionNumber < 1 && instrument != null) instrumentRevisionNumber = instrument.getRevisionNumber(); }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
