package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ProjectTask;
import com.researchassistant.collaboration.entity.ProjectTaskPriority;
import com.researchassistant.collaboration.entity.ProjectTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID> {
    Page<ProjectTask> findAllByProjectId(UUID projectId, Pageable pageable);
    Page<ProjectTask> findAllByProjectIdAndStatus(UUID projectId, ProjectTaskStatus status, Pageable pageable);
    Page<ProjectTask> findAllByProjectIdAndPriority(UUID projectId, ProjectTaskPriority priority, Pageable pageable);
    Optional<ProjectTask> findByIdAndProjectId(UUID id, UUID projectId);
    long countByProjectId(UUID projectId);
    long countByProjectIdAndStatus(UUID projectId, ProjectTaskStatus status);

    @org.springframework.data.jpa.repository.Query("""
            select t
            from ProjectTask t
            where (
                t.createdBy.id = :userId
                or exists (
                    select a.id
                    from ProjectTaskAssignee a
                    where a.task = t
                      and a.user.id = :userId
                )
            )
            and (:status is null or t.status = :status)
            and (:priority is null or t.priority = :priority)
            and (:projectId is null or t.project.id = :projectId)
            order by t.updatedAt desc
            """)
    Page<ProjectTask> findAllMyTasks(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("status") ProjectTaskStatus status,
            @org.springframework.data.repository.query.Param("priority") ProjectTaskPriority priority,
            @org.springframework.data.repository.query.Param("projectId") UUID projectId,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("""
            select count(t)
            from ProjectTask t
            where (
                t.createdBy.id = :userId
                or exists (
                    select a.id
                    from ProjectTaskAssignee a
                    where a.task = t
                      and a.user.id = :userId
                )
            )
            and t.status in (com.researchassistant.collaboration.entity.ProjectTaskStatus.TODO, com.researchassistant.collaboration.entity.ProjectTaskStatus.IN_PROGRESS, com.researchassistant.collaboration.entity.ProjectTaskStatus.IN_REVIEW)
            """)
    long countOpenTasksForUser(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
