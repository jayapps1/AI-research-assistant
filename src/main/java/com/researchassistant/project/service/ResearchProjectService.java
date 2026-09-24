package com.researchassistant.project.service;

import com.researchassistant.cache.CacheInvalidationService;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.project.dto.AddProjectMemberRequest;
import com.researchassistant.project.dto.ChangeProjectMemberRoleRequest;
import com.researchassistant.project.dto.CreateResearchProjectRequest;
import com.researchassistant.project.dto.ProjectMemberResponse;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.project.dto.UpdateResearchProjectRequest;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.entity.ResearchProjectStatus;
import com.researchassistant.project.exception.InvalidProjectOperationException;
import com.researchassistant.project.exception.ResearchProjectNotFoundException;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.subscription.PlanFeature;
import com.researchassistant.usage.QuotaService;
import com.researchassistant.usage.UsageMetricType;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ResearchProjectService {

    private final ResearchProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectAuthorizationService authorizationService;
    private final SecurityAuditService auditService;
    private final CacheInvalidationService cacheInvalidationService;
    private final QuotaService quotaService;
    private final com.researchassistant.analysis.repository.ResearchReportTemplateRepository templateRepository;
    private final EntityManager entityManager;
    private static final UUID TTU_COMPUTER_SCIENCE_TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000517");
    private static final UUID GENERAL_FIVE_CHAPTER_TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000518");
    private static final UUID QUANTITATIVE_SURVEY_TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000519");
    private static final UUID QUALITATIVE_TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000520");
    private static final UUID MIXED_METHODS_TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000521");

    public ResearchProjectService(
            ResearchProjectRepository projectRepository,
            ProjectMembershipRepository membershipRepository,
            UserRepository userRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectAuthorizationService authorizationService,
            SecurityAuditService auditService,
            CacheInvalidationService cacheInvalidationService,
            QuotaService quotaService,
            com.researchassistant.analysis.repository.ResearchReportTemplateRepository templateRepository,
            EntityManager entityManager
    ) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.quotaService = quotaService;
        this.templateRepository = templateRepository;
        this.entityManager = entityManager;
    }

    public ResearchProjectResponse createProject(
            UUID workspaceId,
            User currentUser,
            CreateResearchProjectRequest request
    ) {
        if (request.workspaceId() != null && !workspaceId.equals(request.workspaceId())) {
            throw new InvalidProjectOperationException(
                    "Request workspace does not match path workspace."
            );
        }

        WorkspaceMembership workspaceMembership =
                workspaceAuthorizationService.requireAdminOrOwner(
                        workspaceId,
                        currentUser
                );

        Workspace workspace = workspaceMembership.getWorkspace();
        quotaService.requireWithinQuota(workspaceId, PlanFeature.PROJECT_CREATION, UsageMetricType.PROJECT_COUNT, 1L);

        ResearchProject project = new ResearchProject();
        project.setWorkspace(workspace);
        project.setTitle(normalizeRequiredTitle(request.title()));
        project.setDescription(normalizeOptionalText(request.description()));
        project.setResearchAim(normalizeOptionalText(request.researchAim()));
        project.setStudyArea(normalizeOptionalText(request.studyArea()));
        project.setResearchType(normalizeOptionalText(request.researchType()));
        project.setKeywords(normalizeOptionalText(request.keywords()));
        com.researchassistant.analysis.entity.ResearchReportTemplate template = resolveTemplate(
                request.reportTemplateId(),
                request.researchType()
        );
        project.setReportTemplate(template);
        if (request.citationStyle() != null) {
            project.setCitationStyle(request.citationStyle());
        } else if (template != null && template.getDefaultCitationStyle() != null) {
            project.setCitationStyle(template.getDefaultCitationStyle());
        }
        if (request.citationStyleLocked() != null) {
            project.setCitationStyleLocked(request.citationStyleLocked());
        } else if (template != null) {
            project.setCitationStyleLocked(template.isCitationStyleLocked());
        }
        if (request.citationPresentation() != null) {
            project.setCitationPresentation(request.citationPresentation());
        }
        if (normalizeOptionalText(request.bibliographySort()) != null) {
            project.setBibliographySort(normalizeOptionalText(request.bibliographySort()));
        }
        if (request.includeDoi() != null) {
            project.setIncludeDoi(request.includeDoi());
        }
        if (request.includeUrl() != null) {
            project.setIncludeUrl(request.includeUrl());
        }
        project.setStatus(ResearchProjectStatus.DRAFT);
        project.setCreatedBy(currentUser);
        project.setNextDocumentNumber(1L);

        ResearchProject savedProject = projectRepository.save(project);

        ProjectMembership membership = new ProjectMembership();
        membership.setProject(savedProject);
        membership.setUser(currentUser);
        membership.setRole(ProjectRole.LEAD);
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        membership.setAddedBy(currentUser);
        membership.setJoinedAt(OffsetDateTime.now());
        membershipRepository.save(membership);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_CREATED
        );
        cacheInvalidationService.evictProjectMetadata(savedProject.getId());

        return toProjectResponse(savedProject, membership);
    }

    @Transactional(readOnly = true)
    public Page<ResearchProjectResponse> listWorkspaceProjects(
            UUID workspaceId,
            User currentUser,
            String query,
            ResearchProjectStatus status,
            Pageable pageable
    ) {
        WorkspaceMembership workspaceMembership =
                workspaceAuthorizationService.requireActiveMembership(
                        workspaceId,
                        currentUser
                );

        String trimmedQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        boolean isAdmin = authorizationService.isWorkspaceAdmin(workspaceMembership);
        Page<ResearchProject> projects;

        if (trimmedQuery != null) {
            String pattern = "%" + trimmedQuery.toLowerCase() + "%";
            projects = isAdmin
                    ? projectRepository.findAllByWorkspaceIdWithPattern(workspaceId, status, pattern, pageable)
                    : projectRepository.findAuthorizedMemberProjectsWithPattern(workspaceId, currentUser.getId(), status, pattern, pageable);
        } else if (status != null) {
            projects = isAdmin
                    ? projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, status, pageable)
                    : projectRepository.findAuthorizedMemberProjectsByStatus(workspaceId, currentUser.getId(), status, pageable);
        } else {
            projects = isAdmin
                    ? projectRepository.findAllByWorkspaceId(workspaceId, pageable)
                    : projectRepository.findAuthorizedMemberProjects(workspaceId, currentUser.getId(), pageable);
        }

        return projects.map(project -> toProjectResponse(
                project,
                membershipRepository
                        .findByProjectIdAndUserIdAndStatus(
                                project.getId(),
                                currentUser.getId(),
                                ProjectMembershipStatus.ACTIVE
                        )
                        .orElse(null)
        ));
    }

    @Transactional(readOnly = true)
    public Page<ResearchProjectResponse> listWorkspaceProjects(
            UUID workspaceId,
            User currentUser,
            Pageable pageable
    ) {
        return listWorkspaceProjects(workspaceId, currentUser, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ResearchProjectResponse> listMyProjects(
            User currentUser,
            String query,
            ResearchProjectStatus status,
            Pageable pageable
    ) {
        String trimmedQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        Page<ResearchProject> projects;

        if (trimmedQuery != null) {
            String pattern = "%" + trimmedQuery.toLowerCase() + "%";
            projects = projectRepository.findAllAuthorizedProjectsForUserWithPattern(
                    currentUser.getId(),
                    status,
                    pattern,
                    pageable
            );
        } else if (status != null) {
            projects = projectRepository.findAllAuthorizedProjectsForUserByStatus(
                    currentUser.getId(),
                    status,
                    pageable
            );
        } else {
            projects = projectRepository.findAllAuthorizedProjectsForUser(
                    currentUser.getId(),
                    pageable
            );
        }

        return projects.map(project -> toProjectResponse(
                project,
                membershipRepository
                        .findByProjectIdAndUserIdAndStatus(
                                project.getId(),
                                currentUser.getId(),
                                ProjectMembershipStatus.ACTIVE
                        )
                        .orElse(null)
        ));
    }

    @Transactional(readOnly = true)
    public ResearchProjectResponse getProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectViewer(
                        projectId,
                        currentUser
                );

        return toProjectResponse(
                context.project(),
                context.projectMembership().orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public ResearchProject getProjectEntity(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Research project not found."));
    }

    public ResearchProjectResponse updateProject(
            UUID projectId,
            User currentUser,
            UpdateResearchProjectRequest request
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();

        if (request.title() != null) {
            project.setTitle(normalizeRequiredTitle(request.title()));
        }
        if (request.description() != null) {
            project.setDescription(normalizeOptionalText(request.description()));
        }
        if (request.researchAim() != null) {
            project.setResearchAim(normalizeOptionalText(request.researchAim()));
        }
        if (request.studyArea() != null) {
            project.setStudyArea(normalizeOptionalText(request.studyArea()));
        }
        if (request.researchType() != null) {
            project.setResearchType(normalizeOptionalText(request.researchType()));
        }
        if (request.keywords() != null) {
            project.setKeywords(normalizeOptionalText(request.keywords()));
        }
        if (request.reportTemplateId() != null) {
            com.researchassistant.analysis.entity.ResearchReportTemplate template = resolveTemplate(request.reportTemplateId(), project.getResearchType());
            project.setReportTemplate(template);
            if (!project.isCitationStyleLocked() && template != null && template.getDefaultCitationStyle() != null) {
                project.setCitationStyle(template.getDefaultCitationStyle());
                project.setCitationStyleLocked(template.isCitationStyleLocked());
            }
        }
        if (request.citationStyle() != null && !project.isCitationStyleLocked()) {
            project.setCitationStyle(request.citationStyle());
        }
        if (request.citationStyleLocked() != null) {
            project.setCitationStyleLocked(request.citationStyleLocked());
        }
        if (request.citationPresentation() != null) {
            project.setCitationPresentation(request.citationPresentation());
        }
        if (request.bibliographySort() != null) {
            project.setBibliographySort(normalizeOptionalText(request.bibliographySort()) == null ? "STYLE_DEFAULT" : normalizeOptionalText(request.bibliographySort()));
        }
        if (request.includeDoi() != null) {
            project.setIncludeDoi(request.includeDoi());
        }
        if (request.includeUrl() != null) {
            project.setIncludeUrl(request.includeUrl());
        }
        if (request.status() != null) {
            changeStatus(project, request.status());
        }

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_UPDATED
        );
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    private void changeStatus(ResearchProject project, ResearchProjectStatus targetStatus) {
        if (project.getStatus() == targetStatus) {
            return;
        }
        if (project.getStatus() == ResearchProjectStatus.TRASHED && targetStatus != ResearchProjectStatus.ACTIVE) {
            throw new InvalidProjectOperationException("Trashed projects must be restored before changing status.");
        }
        if (project.getStatus() == ResearchProjectStatus.ARCHIVED && targetStatus != ResearchProjectStatus.ACTIVE) {
            throw new InvalidProjectOperationException("Archived projects must be restored before changing status.");
        }
        if (targetStatus == ResearchProjectStatus.TRASHED || targetStatus == ResearchProjectStatus.ARCHIVED) {
            throw new InvalidProjectOperationException("Use archive or trash actions for destructive status changes.");
        }
        project.setStatus(targetStatus);
    }

    public ResearchProjectResponse activateProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();
        if (project.getStatus() != ResearchProjectStatus.DRAFT
                && project.getStatus() != ResearchProjectStatus.COMPLETED
                && project.getStatus() != ResearchProjectStatus.ON_HOLD) {
            throw new InvalidProjectOperationException(
                    "Only draft or completed projects can be activated."
            );
        }
        project.setStatus(ResearchProjectStatus.ACTIVE);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_ACTIVATED
        );
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    public ResearchProjectResponse completeProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();
        if (project.getStatus() == ResearchProjectStatus.ARCHIVED) {
            throw new InvalidProjectOperationException(
                    "Archived projects cannot be completed."
            );
        }
        project.setStatus(ResearchProjectStatus.COMPLETED);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_COMPLETED
        );
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    public ResearchProjectResponse holdProject(UUID projectId, User currentUser) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(projectId, currentUser);
        ResearchProject project = context.project();
        if (project.getStatus() == ResearchProjectStatus.ARCHIVED
                || project.getStatus() == ResearchProjectStatus.TRASHED) {
            throw new InvalidProjectOperationException("Archived or trashed projects cannot be put on hold.");
        }
        project.setStatus(ResearchProjectStatus.ON_HOLD);
        auditService.record(currentUser.getId(), SecurityAuditEventType.RESEARCH_PROJECT_UPDATED);
        cacheInvalidationService.evictProjectMetadata(projectId);
        return toProjectResponse(project, context.projectMembership().orElse(null));
    }

    public ResearchProjectResponse archiveProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();
        project.setStatus(ResearchProjectStatus.ARCHIVED);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_ARCHIVED
        );
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    public ResearchProjectResponse trashProject(UUID projectId, User currentUser) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(projectId, currentUser);

        ResearchProject project = context.project();
        project.setStatus(ResearchProjectStatus.TRASHED);

        auditService.record(currentUser.getId(), SecurityAuditEventType.RESEARCH_PROJECT_TRASHED);
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toProjectResponse(project, context.projectMembership().orElse(null));
    }

    public ResearchProjectResponse restoreProject(UUID projectId, User currentUser) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(projectId, currentUser);

        ResearchProject project = context.project();
        if (project.getStatus() == ResearchProjectStatus.TRASHED
                || project.getStatus() == ResearchProjectStatus.ARCHIVED) {
            project.setStatus(ResearchProjectStatus.ACTIVE);
        }

        auditService.record(currentUser.getId(), SecurityAuditEventType.RESEARCH_PROJECT_RESTORED);
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toProjectResponse(project, context.projectMembership().orElse(null));
    }

    public void permanentlyDeleteProject(UUID projectId, User currentUser, String confirmation) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(projectId, currentUser);

        ResearchProject project = context.project();
        if (project.getStatus() != ResearchProjectStatus.TRASHED) {
            throw new InvalidProjectOperationException("Only trashed projects can be permanently deleted.");
        }
        String token = confirmation == null ? "" : confirmation.trim();
        if (!"DELETE".equals(token) && !project.getTitle().equals(token)) {
            throw new InvalidProjectOperationException("Permanent delete confirmation did not match.");
        }

        Map<String, Long> dependencies = projectDependencyCounts(projectId);
        List<String> blocking = dependencies.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        if (!blocking.isEmpty()) {
            throw new InvalidProjectOperationException(
                    "Permanent delete is not safe while project artifacts exist: " + String.join(", ", blocking)
            );
        }

        membershipRepository.deleteAll(membershipRepository.findAllByProjectIdAndStatus(projectId, ProjectMembershipStatus.ACTIVE));
        projectRepository.delete(project);
        auditService.record(currentUser.getId(), SecurityAuditEventType.RESEARCH_PROJECT_PERMANENTLY_DELETED);
        cacheInvalidationService.evictProjectMetadata(projectId);
    }

    private Map<String, Long> projectDependencyCounts(UUID projectId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("documents", countRows("documents", projectId));
        counts.put("rag_conversations", countRows("rag_conversations", projectId));
        counts.put("research_objectives", countRows("research_objectives", projectId));
        counts.put("research_questions", countRows("research_questions", projectId));
        counts.put("research_hypotheses", countRows("research_hypotheses", projectId));
        counts.put("research_problems", countRows("research_problems", projectId));
        counts.put("research_datasets", countRows("research_datasets", projectId));
        counts.put("analysis_runs", countRows("analysis_runs", projectId));
        counts.put("research_reports", countRows("research_reports", projectId));
        counts.put("project_references", countRows("project_references", projectId));
        counts.put("project_tasks", countRows("project_tasks", projectId));
        counts.put("project_activities", countRows("project_activities", projectId));
        return counts;
    }

    private long countRows(String tableName, UUID projectId) {
        Object value = entityManager
                .createNativeQuery("select count(*) from " + tableName + " where project_id = :projectId")
                .setParameter("projectId", projectId)
                .getSingleResult();
        return ((Number) value).longValue();
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(
            UUID projectId,
            User currentUser,
            Pageable pageable
    ) {
        authorizationService.requireProjectViewer(projectId, currentUser);

        return membershipRepository.findAllByProjectIdAndStatus(
                        projectId,
                        ProjectMembershipStatus.ACTIVE,
                        pageable
                )
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    public ProjectMemberResponse addMember(
            UUID projectId,
            User currentUser,
            AddProjectMemberRequest request
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        boolean workspaceAdmin =
                authorizationService.isWorkspaceAdmin(
                        context.workspaceMembership()
                );
        quotaService.requireWithinQuota(context.project().getWorkspace().getId(), PlanFeature.COLLABORATORS_PER_PROJECT,
                UsageMetricType.COLLABORATOR_COUNT, 1L, projectId);

        if (request.role() == ProjectRole.LEAD && !workspaceAdmin) {
            throw new InvalidProjectOperationException(
                    "Only workspace owners or admins may add a project lead."
            );
        }

        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        User targetUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found.")
                );

        workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        context.project().getWorkspace().getId(),
                        targetUser.getId(),
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new InvalidProjectOperationException(
                                "User must belong to the workspace before joining the project."
                        )
                );

        if (membershipRepository.findByProjectIdAndUserId(
                projectId,
                targetUser.getId()
        ).isPresent()) {
            throw new DuplicateResourceException(
                    "User already has a project membership."
            );
        }

        ProjectMembership membership = new ProjectMembership();
        membership.setProject(context.project());
        membership.setUser(targetUser);
        membership.setRole(request.role());
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        membership.setAddedBy(currentUser);
        membership.setJoinedAt(OffsetDateTime.now());

        ProjectMembership savedMembership =
                membershipRepository.save(membership);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.PROJECT_MEMBER_ADDED
        );
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toMemberResponse(savedMembership);
    }

    public ProjectMemberResponse changeMemberRole(
            UUID projectId,
            UUID targetUserId,
            User currentUser,
            ChangeProjectMemberRoleRequest request
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        boolean workspaceAdmin =
                authorizationService.isWorkspaceAdmin(
                        context.workspaceMembership()
                );

        ProjectMembership targetMembership =
                membershipRepository.findByProjectIdAndUserIdAndStatus(
                                projectId,
                                targetUserId,
                                ProjectMembershipStatus.ACTIVE
                        )
                        .orElseThrow(ResearchProjectNotFoundException::new);

        membershipRepository.findActiveByProjectIdForUpdate(projectId);

        if (request.role() == ProjectRole.LEAD && !workspaceAdmin) {
            throw new InvalidProjectOperationException(
                    "Only workspace owners or admins may assign project lead."
            );
        }

        if (targetMembership.getRole() == ProjectRole.LEAD
                && request.role() != ProjectRole.LEAD
                && activeLeadCount(projectId) <= 1) {
            throw new InvalidProjectOperationException(
                    "Project must retain at least one active lead."
            );
        }

        targetMembership.setRole(request.role());

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.PROJECT_MEMBER_ROLE_CHANGED
        );
        cacheInvalidationService.evictProjectMetadata(projectId);

        return toMemberResponse(targetMembership);
    }

    public void removeMember(
            UUID projectId,
            UUID targetUserId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ProjectMembership targetMembership =
                membershipRepository.findByProjectIdAndUserIdAndStatus(
                                projectId,
                                targetUserId,
                                ProjectMembershipStatus.ACTIVE
                        )
                        .orElseThrow(ResearchProjectNotFoundException::new);

        membershipRepository.findActiveByProjectIdForUpdate(projectId);

        if (targetMembership.getRole() == ProjectRole.LEAD
                && activeLeadCount(projectId) <= 1) {
            throw new InvalidProjectOperationException(
                    "Project must retain at least one active lead."
            );
        }

        targetMembership.setStatus(ProjectMembershipStatus.REMOVED);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.PROJECT_MEMBER_REMOVED
        );
        cacheInvalidationService.evictProjectMetadata(projectId);
    }

    private long activeLeadCount(UUID projectId) {
        return membershipRepository.countByProjectIdAndRoleAndStatus(
                projectId,
                ProjectRole.LEAD,
                ProjectMembershipStatus.ACTIVE
        );
    }

    private com.researchassistant.analysis.entity.ResearchReportTemplate resolveTemplate(UUID requestedTemplateId, String researchType) {
        if (requestedTemplateId != null) {
            return templateRepository.findById(requestedTemplateId).orElse(null);
        }
        UUID defaultTemplateId = switch (normalizeOptionalText(researchType) == null ? "" : normalizeOptionalText(researchType)) {
            case "SOFTWARE_SYSTEM_PROJECT" -> TTU_COMPUTER_SCIENCE_TEMPLATE_ID;
            case "QUANTITATIVE_SURVEY", "EXPERIMENTAL_RESEARCH" -> QUANTITATIVE_SURVEY_TEMPLATE_ID;
            case "QUALITATIVE_RESEARCH", "CASE_STUDY" -> QUALITATIVE_TEMPLATE_ID;
            case "MIXED_METHODS" -> MIXED_METHODS_TEMPLATE_ID;
            default -> GENERAL_FIVE_CHAPTER_TEMPLATE_ID;
        };
        return templateRepository.findById(defaultTemplateId)
                .or(() -> templateRepository.findFirstByTypeAndSystemTemplateTrueOrderByCreatedAtAsc(
                        com.researchassistant.analysis.entity.ResearchReportType.RESEARCH_REPORT
                ))
                .orElse(null);
    }

    private ResearchProjectResponse toProjectResponse(
            ResearchProject project,
            ProjectMembership currentUserMembership
    ) {
        return new ResearchProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getTitle(),
                project.getDescription(),
                project.getResearchAim(),
                project.getStudyArea(),
                project.getResearchType(),
                project.getKeywords(),
                project.getReportTemplate() == null ? null : project.getReportTemplate().getId(),
                project.getReportTemplate() == null ? null : project.getReportTemplate().getName(),
                project.getCitationStyle() == null ? com.researchassistant.analysis.entity.CitationStyle.APA_7 : project.getCitationStyle(),
                project.isCitationStyleLocked(),
                project.getCitationPresentation() == null ? com.researchassistant.analysis.entity.CitationPresentation.PARENTHETICAL : project.getCitationPresentation(),
                project.getBibliographySort(),
                project.isIncludeDoi(),
                project.isIncludeUrl(),
                project.getStatus(),
                project.getCreatedBy().getId(),
                currentUserMembership == null
                        ? null
                        : currentUserMembership.getRole(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ProjectMemberResponse toMemberResponse(
            ProjectMembership membership
    ) {
        User user = membership.getUser();
        return new ProjectMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                membership.getRole(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }

    private String normalizeRequiredTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            throw new InvalidProjectOperationException(
                    "Project title is required."
            );
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
