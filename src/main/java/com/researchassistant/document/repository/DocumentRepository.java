package com.researchassistant.document.repository;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentType;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findAllByProjectIdAndStatusNot(
            UUID projectId,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findAllByProjectIdAndStatus(
            UUID projectId,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findAllByProjectIdAndTypeAndStatusNot(
            UUID projectId,
            DocumentType type,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findAllByProjectIdAndTypeAndStatus(
            UUID projectId,
            DocumentType type,
            DocumentStatus status,
            Pageable pageable
    );

    /**
     * Locks a document row before allocating the next immutable
     * version number. Version numbers must not be derived from row
     * counts because archived or failed versions still consume their
     * historical number.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Document d where d.id = :documentId")
    Optional<Document> findByIdForVersionAllocation(
            @Param("documentId") UUID documentId
    );
}
