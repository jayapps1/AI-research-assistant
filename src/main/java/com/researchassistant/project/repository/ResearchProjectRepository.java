package com.researchassistant.project.repository;

import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ResearchProjectRepository
        extends JpaRepository<ResearchProject, UUID> {

    Page<ResearchProject> findAllByWorkspaceId(
            UUID workspaceId,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where p.workspace.id = :workspaceId
              and exists (
                  select m.id
                  from ProjectMembership m
                  where m.project = p
                    and m.user.id = :userId
                    and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
              )
            """)
    Page<ResearchProject> findAuthorizedMemberProjects(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * Locks a project row for future document-number allocation.
     *
     * <p>Document creation should reserve {@code nextDocumentNumber}
     * only through this row lock, increment the field, create the
     * document with the reserved value, and commit. It must not infer
     * numbers from document counts or maximum document values.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ResearchProject p where p.id = :projectId")
    Optional<ResearchProject> findByIdForDocumentNumberAllocation(
            @Param("projectId") UUID projectId
    );
}
