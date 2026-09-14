package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ProjectTaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectTaskAssigneeRepository extends JpaRepository<ProjectTaskAssignee, UUID> {
    List<ProjectTaskAssignee> findAllByTaskId(UUID taskId);
    List<ProjectTaskAssignee> findAllByTaskProjectIdAndUserId(UUID projectId, UUID userId);
    Optional<ProjectTaskAssignee> findByTaskIdAndProjectMembershipId(UUID taskId, UUID membershipId);
    void deleteByTaskIdAndProjectMembershipId(UUID taskId, UUID membershipId);
}
