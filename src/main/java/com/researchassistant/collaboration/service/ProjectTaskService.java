package com.researchassistant.collaboration.service;

import com.researchassistant.collaboration.dto.CollaborationDtos.*;
import com.researchassistant.collaboration.entity.*;
import com.researchassistant.collaboration.exception.ArtifactVersionConflictException;
import com.researchassistant.collaboration.repository.*;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.exception.InvalidProjectOperationException;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectTaskService {
    private final ProjectTaskRepository taskRepository;
    private final ProjectTaskAssigneeRepository assigneeRepository;
    private final ProjectTaskArtifactLinkRepository linkRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final ProjectAuthorizationService authorizationService;
    private final CollaborationArtifactResolver artifactResolver;
    private final ProjectActivityService activityService;
    private final SecurityAuditService auditService;

    public ProjectTaskService(ProjectTaskRepository taskRepository, ProjectTaskAssigneeRepository assigneeRepository, ProjectTaskArtifactLinkRepository linkRepository, ProjectMembershipRepository membershipRepository, ProjectAuthorizationService authorizationService, CollaborationArtifactResolver artifactResolver, ProjectActivityService activityService, SecurityAuditService auditService) {
        this.taskRepository = taskRepository;
        this.assigneeRepository = assigneeRepository;
        this.linkRepository = linkRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
        this.artifactResolver = artifactResolver;
        this.activityService = activityService;
        this.auditService = auditService;
    }

    public ProjectTaskResponse create(UUID projectId, User actor, CreateProjectTaskRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, actor);
        ProjectTask task = new ProjectTask();
        task.setProject(context.project());
        task.setTitle(required(request.title(), "Task title is required."));
        task.setDescription(blankToNull(request.description()));
        task.setPriority(request.priority() == null ? ProjectTaskPriority.MEDIUM : request.priority());
        task.setDueDate(request.dueDate());
        task.setDueAt(request.dueAt());
        task.setCreatedBy(actor);
        task.setAssignedBy(actor);
        task = taskRepository.save(task);
        addInitialAssignees(task, actor, request.assigneeMemberIds());
        addArtifactLinks(task, request.artifactLinks());
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_TASK_CREATED);
        activityService.record(context.project(), actor, ProjectActivityType.TASK_CREATED, null, task.getId(), "Project task created.");
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<ProjectTaskResponse> list(UUID projectId, User actor, ProjectTaskStatus status, ProjectTaskPriority priority, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, actor);
        Page<ProjectTask> page = status != null
                ? taskRepository.findAllByProjectIdAndStatus(projectId, status, pageable)
                : priority != null
                ? taskRepository.findAllByProjectIdAndPriority(projectId, priority, pageable)
                : taskRepository.findAllByProjectId(projectId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProjectTaskResponse> mine(UUID projectId, User actor) {
        authorizationService.requireProjectViewer(projectId, actor);
        return assigneeRepository.findAllByTaskProjectIdAndUserId(projectId, actor.getId()).stream()
                .map(ProjectTaskAssignee::getTask)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectTaskResponse get(UUID taskId, User actor) {
        ProjectTask task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found."));
        authorizationService.requireProjectViewer(task.getProject().getId(), actor);
        return toResponse(task);
    }

    public ProjectTaskResponse update(UUID taskId, User actor, UpdateProjectTaskRequest request) {
        ProjectTask task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found."));
        authorizationService.requireProjectEditor(task.getProject().getId(), actor);
        if (request.expectedVersion() != null && !request.expectedVersion().equals(task.getVersion())) {
            throw new ArtifactVersionConflictException(task.getId(), request.expectedVersion(), task.getVersion());
        }
        if (request.title() != null) task.setTitle(required(request.title(), "Task title is required."));
        if (request.description() != null) task.setDescription(blankToNull(request.description()));
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.dueDate() != null) task.setDueDate(request.dueDate());
        if (request.dueAt() != null) task.setDueAt(request.dueAt());
        activityService.record(task.getProject(), actor, ProjectActivityType.RESEARCH_ARTIFACT_UPDATED, null, task.getId(), "Project task updated.");
        return toResponse(task);
    }

    public ProjectTaskResponse addAssignee(UUID taskId, User actor, AddTaskAssigneeRequest request) {
        ProjectTask task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found."));
        authorizationService.requireProjectEditor(task.getProject().getId(), actor);
        addAssignee(task, actor, request.memberId());
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_TASK_ASSIGNED);
        activityService.record(task.getProject(), actor, ProjectActivityType.TASK_ASSIGNED, null, task.getId(), "Project task assigned.");
        return toResponse(task);
    }

    public void removeAssignee(UUID taskId, UUID memberId, User actor) {
        ProjectTask task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found."));
        authorizationService.requireProjectEditor(task.getProject().getId(), actor);
        ProjectMembership membership = activeMember(task.getProject().getId(), memberId);
        assigneeRepository.deleteByTaskIdAndProjectMembershipId(taskId, membership.getId());
    }

    public ProjectTaskResponse transition(UUID taskId, User actor, ProjectTaskStatus status) {
        ProjectTask task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found."));
        authorizationService.requireProjectEditor(task.getProject().getId(), actor);
        task.setStatus(status);
        if (status == ProjectTaskStatus.COMPLETED) {
            task.setCompletedAt(OffsetDateTime.now());
            auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_TASK_COMPLETED);
            activityService.record(task.getProject(), actor, ProjectActivityType.TASK_COMPLETED, null, task.getId(), "Project task completed.");
        } else if (status == ProjectTaskStatus.IN_PROGRESS) {
            activityService.record(task.getProject(), actor, ProjectActivityType.TASK_STARTED, null, task.getId(), "Project task started.");
        } else if (status == ProjectTaskStatus.IN_REVIEW) {
            activityService.record(task.getProject(), actor, ProjectActivityType.TASK_SUBMITTED_FOR_REVIEW, null, task.getId(), "Project task submitted for review.");
        } else if (status == ProjectTaskStatus.CANCELLED) {
            activityService.record(task.getProject(), actor, ProjectActivityType.TASK_CANCELLED, null, task.getId(), "Project task cancelled.");
        }
        return toResponse(task);
    }

    private void addInitialAssignees(ProjectTask task, User actor, List<UUID> memberIds) {
        if (memberIds == null) return;
        for (UUID memberId : memberIds) addAssignee(task, actor, memberId);
    }

    private void addAssignee(ProjectTask task, User actor, UUID memberId) {
        ProjectMembership membership = activeMember(task.getProject().getId(), memberId);
        if (membership.getRole() == ProjectRole.VIEWER) {
            throw new InvalidProjectOperationException("Viewer members cannot be assigned project work.");
        }
        if (assigneeRepository.findByTaskIdAndProjectMembershipId(task.getId(), membership.getId()).isPresent()) return;
        ProjectTaskAssignee assignee = new ProjectTaskAssignee();
        assignee.setTask(task);
        assignee.setProjectMembership(membership);
        assignee.setUser(membership.getUser());
        assignee.setAssignedBy(actor);
        assigneeRepository.save(assignee);
    }

    private void addArtifactLinks(ProjectTask task, List<ArtifactLinkRequest> links) {
        if (links == null) return;
        for (ArtifactLinkRequest request : links) {
            artifactResolver.requireArtifact(task.getProject().getId(), request.artifactType(), request.artifactId());
            ProjectTaskArtifactLink link = new ProjectTaskArtifactLink();
            link.setTask(task);
            link.setArtifactType(request.artifactType());
            link.setArtifactId(request.artifactId());
            linkRepository.save(link);
        }
    }

    private ProjectMembership activeMember(UUID projectId, UUID memberId) {
        return membershipRepository.findByIdAndProjectIdAndStatus(memberId, projectId, ProjectMembershipStatus.ACTIVE)
                .orElseThrow(() -> new InvalidProjectOperationException("Assignee must be an active project member."));
    }

    private ProjectTaskResponse toResponse(ProjectTask task) {
        List<UUID> assignees = assigneeRepository.findAllByTaskId(task.getId()).stream().map(a -> a.getProjectMembership().getId()).toList();
        List<ArtifactLinkResponse> links = linkRepository.findAllByTaskId(task.getId()).stream().map(l -> new ArtifactLinkResponse(l.getId(), l.getArtifactType(), l.getArtifactId())).toList();
        return new ProjectTaskResponse(task.getId(), task.getProject().getId(), task.getTitle(), task.getDescription(), task.getStatus(), task.getPriority(), task.getCreatedBy().getId(), task.getAssignedBy() == null ? null : task.getAssignedBy().getId(), task.getDueDate(), task.getDueAt(), task.getCompletedAt(), task.getVersion(), overdue(task), assignees, links, task.getCreatedAt(), task.getUpdatedAt());
    }

    private boolean overdue(ProjectTask task) {
        if (task.getStatus() == ProjectTaskStatus.COMPLETED || task.getStatus() == ProjectTaskStatus.CANCELLED) return false;
        OffsetDateTime now = OffsetDateTime.now();
        return task.getDueAt() != null ? task.getDueAt().isBefore(now) : task.getDueDate() != null && task.getDueDate().isBefore(now.toLocalDate());
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new InvalidProjectOperationException(message);
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
