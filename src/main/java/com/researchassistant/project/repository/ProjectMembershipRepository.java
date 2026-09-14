package com.researchassistant.project.repository;

import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMembershipRepository
        extends JpaRepository<ProjectMembership, UUID> {

    Optional<ProjectMembership> findByProjectIdAndUserId(
            UUID projectId,
            UUID userId
    );

    Optional<ProjectMembership> findByProjectIdAndUserIdAndStatus(
            UUID projectId,
            UUID userId,
            ProjectMembershipStatus status
    );

    Optional<ProjectMembership> findByIdAndProjectIdAndStatus(
            UUID id,
            UUID projectId,
            ProjectMembershipStatus status
    );

    List<ProjectMembership> findAllByProjectIdAndStatus(
            UUID projectId,
            ProjectMembershipStatus status
    );

    boolean existsByProjectIdAndUserIdAndStatus(
            UUID projectId,
            UUID userId,
            ProjectMembershipStatus status
    );

    List<ProjectMembership> findAllByProjectIdAndStatus(
            UUID projectId,
            ProjectMembershipStatus status,
            Pageable pageable
    );

    long countByProjectIdAndRoleAndStatus(
            UUID projectId,
            ProjectRole role,
            ProjectMembershipStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ProjectMembership m
            where m.project.id = :projectId
              and m.status = com.researchassistant.project.entity.ProjectMembershipStatus.ACTIVE
            """)
    List<ProjectMembership> findActiveByProjectIdForUpdate(
            @Param("projectId") UUID projectId
    );
}
