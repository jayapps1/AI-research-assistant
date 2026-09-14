package com.researchassistant.dataset.model;

import com.researchassistant.datacollection.model.DataCollectionSession;
import com.researchassistant.participant.model.Participant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_records", indexes = @Index(name = "idx_dataset_records_dataset", columnList = "dataset_id"))
@Getter @Setter @NoArgsConstructor
public class DatasetRecord {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dataset_id", nullable = false)
    private ResearchDataset dataset;
    @Column(name = "row_number", nullable = false)
    private long rowNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "participant_id")
    private Participant participant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_session_id")
    private DataCollectionSession sourceSession;
    @Column(name = "external_record_id", length = 255)
    private String externalRecordId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
