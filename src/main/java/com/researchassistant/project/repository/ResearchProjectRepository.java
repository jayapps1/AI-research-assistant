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

    long countByWorkspaceId(UUID workspaceId);

    long countByWorkspaceIdAndStatus(UUID workspaceId, com.researchassistant.project.entity.ResearchProjectStatus status);

    long countByWorkspaceIdAndStatusNot(UUID workspaceId, com.researchassistant.project.entity.ResearchProjectStatus status);

    long countByStatusNot(com.researchassistant.project.entity.ResearchProjectStatus status);

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

    Page<ResearchProject> findAllByWorkspaceIdAndStatus(
            UUID workspaceId,
            com.researchassistant.project.entity.ResearchProjectStatus status,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where p.workspace.id = :workspaceId
              and p.status = :status
              and exists (
                  select m.id
                  from ProjectMembership m
                  where m.project = p
                    and m.user.id = :userId
                    and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
              )
            """)
    Page<ResearchProject> findAuthorizedMemberProjectsByStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("status") com.researchassistant.project.entity.ResearchProjectStatus status,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where p.workspace.id = :workspaceId
              and (:status is null or p.status = :status)
              and (lower(p.title) like :pattern or lower(p.description) like :pattern)
            """)
    Page<ResearchProject> findAllByWorkspaceIdWithPattern(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") com.researchassistant.project.entity.ResearchProjectStatus status,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where p.workspace.id = :workspaceId
              and (:status is null or p.status = :status)
              and (lower(p.title) like :pattern or lower(p.description) like :pattern)
              and exists (
                  select m.id
                  from ProjectMembership m
                  where m.project = p
                    and m.user.id = :userId
                    and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
              )
            """)
    Page<ResearchProject> findAuthorizedMemberProjectsWithPattern(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("status") com.researchassistant.project.entity.ResearchProjectStatus status,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where exists (
                select m.id
                from ProjectMembership m
                where m.project = p
                  and m.user.id = :userId
                  and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            )
            order by p.updatedAt desc
            """)
    Page<ResearchProject> findAllAuthorizedProjectsForUser(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where exists (
                select m.id
                from ProjectMembership m
                where m.project = p
                  and m.user.id = :userId
                  and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            )
            and p.status = :status
            order by p.updatedAt desc
            """)
    Page<ResearchProject> findAllAuthorizedProjectsForUserByStatus(
            @Param("userId") UUID userId,
            @Param("status") com.researchassistant.project.entity.ResearchProjectStatus status,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where exists (
                select m.id
                from ProjectMembership m
                where m.project = p
                  and m.user.id = :userId
                  and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            )
            and (:status is null or p.status = :status)
            and (lower(p.title) like :pattern or lower(p.description) like :pattern)
            order by p.updatedAt desc
            """)
    Page<ResearchProject> findAllAuthorizedProjectsForUserWithPattern(
            @Param("userId") UUID userId,
            @Param("status") com.researchassistant.project.entity.ResearchProjectStatus status,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    @Query("""
            select p
            from ResearchProject p
            where exists (
                select m.id
                from ProjectMembership m
                where m.project = p
                  and m.user.id = :userId
                  and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            )
            order by p.updatedAt desc
            """)
    java.util.List<ResearchProject> findRecentAuthorizedProjectsForUser(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            select count(p)
            from ResearchProject p
            where exists (
                select m.id
                from ProjectMembership m
                where m.project = p
                  and m.user.id = :userId
                  and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            )
            and p.status <> com.researchassistant.project.entity.ResearchProjectStatus.ARCHIVED
            """)
    long countActiveProjectsForUser(@Param("userId") UUID userId);

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
