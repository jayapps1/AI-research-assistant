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

    long countByProjectId(UUID projectId);
    long countByProjectIdAndStatus(UUID projectId, DocumentStatus status);

    @Query("""
            select d
            from Document d
            where exists (
                select m.id
                from ProjectMembership m
                where m.project = d.project
                  and m.user.id = :userId
                  and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            )
            and (:status is null or d.status = :status)
            order by d.updatedAt desc
            """)
    Page<Document> findAllAuthorizedDocumentsForUser(
            @Param("userId") UUID userId,
            @Param("status") DocumentStatus status,
            Pageable pageable
    );

    @Query("""
            select count(d)
            from Document d
            where exists (
                select m.id
                from ProjectMembership m
                where m.project = d.project
                  and m.user.id = :userId
                  and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            )
            and d.status <> com.researchassistant.document.entity.DocumentStatus.ARCHIVED
            """)
    long countActiveDocumentsForUser(@Param("userId") UUID userId);

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
