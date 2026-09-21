package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentVersion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository
        extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findAllByDocumentIdOrderByVersionNumberDesc(
            UUID documentId
    );

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(
            UUID documentId,
            int versionNumber
    );

    @Query("""
            select coalesce(sum(v.fileSizeBytes), 0)
            from DocumentVersion v
            where v.document.project.workspace.id = :workspaceId
            """)
    long sumFileSizeBytesByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("select coalesce(sum(v.fileSizeBytes), 0) from DocumentVersion v")
    long sumTotalFileSizeBytes();
}
