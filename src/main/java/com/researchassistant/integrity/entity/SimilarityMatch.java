package com.researchassistant.integrity.entity;

import com.researchassistant.document.entity.*;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.OffsetDateTime; import java.util.UUID;

@Entity @Table(name="similarity_matches")
@Getter @Setter @NoArgsConstructor
public class SimilarityMatch {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="check_id", nullable=false) private SimilarityCheck check;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id") private Document document;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_version_id") private DocumentVersion documentVersion;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="page_id") private DocumentPage page;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="chunk_id") private DocumentChunk chunk;
    @Column(name="matched_text", nullable=false, columnDefinition="TEXT") private String matchedText;
    @Column(name="source_text_snapshot", nullable=false, columnDefinition="TEXT") private String sourceTextSnapshot;
    @Column(name="similarity_score", nullable=false) private double similarityScore;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=60) private SimilarityMatchType type = SimilarityMatchType.OTHER;
    @Column(name="target_start", nullable=false) private int targetStart;
    @Column(name="target_end", nullable=false) private int targetEnd;
    @Column(name="created_at", nullable=false, updatable=false) private OffsetDateTime createdAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(createdAt==null) createdAt=OffsetDateTime.now(); if(type==null) type=SimilarityMatchType.OTHER; }
}
