package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ProjectInvitation;
import com.researchassistant.collaboration.entity.ProjectInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {
    List<ProjectInvitation> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
    boolean existsByProjectIdAndInvitedEmailNormalizedAndStatus(UUID projectId, String email, ProjectInvitationStatus status);
    Optional<ProjectInvitation> findByIdAndStatus(UUID id, ProjectInvitationStatus status);
}
