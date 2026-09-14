package com.researchassistant.datacollection.model;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "instrument_responses", indexes = @Index(name = "idx_instrument_responses_session", columnList = "session_id"))
@Getter @Setter @NoArgsConstructor
public class InstrumentResponse {
    public enum ItemType { QUESTIONNAIRE_ITEM, INTERVIEW_QUESTION, FOCUS_GROUP_QUESTION, OBSERVATION_ITEM }
    public enum ResponseType { TEXT, NUMERIC, BOOLEAN, DATE, OPTION, MULTIPLE_OPTIONS, RAW }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false)
    private DataCollectionSession session;
    @Enumerated(EnumType.STRING) @Column(name = "instrument_item_type", nullable = false, length = 50)
    private ItemType instrumentItemType;
    @Column(name = "instrument_item_id", nullable = false)
    private UUID instrumentItemId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ResponseType type;
    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;
    @Column(name = "numeric_value")
    private BigDecimal numericValue;
    @Column(name = "boolean_value")
    private Boolean booleanValue;
    @Column(name = "date_value")
    private LocalDate dateValue;
    @Column(name = "option_value", length = 255)
    private String optionValue;
    @Column(name = "raw_value", columnDefinition = "TEXT")
    private String rawValue;
    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recorded_by")
    private User recordedBy;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (recordedAt == null) recordedAt = OffsetDateTime.now(); }
}
