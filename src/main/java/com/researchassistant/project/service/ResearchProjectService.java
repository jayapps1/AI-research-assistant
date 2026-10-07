package com.researchassistant.project.service;

import com.researchassistant.analysis.entity.ResearchReportTemplate;
import com.researchassistant.analysis.entity.ResearchReportType;
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
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
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
import org.springframework.data.domain.PageRequest;
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
        AcademicWorkspaceType workspaceType = request.workspaceType() == null
                ? AcademicWorkspaceType.ACADEMIC_RESEARCH
                : request.workspaceType();
        validateWorkspaceMetadata(workspaceType, request.projectType());
        project.setWorkspace(workspace);
        project.setTitle(normalizeRequiredTitle(request.title()));
        project.setDescription(normalizeOptionalText(request.description()));
        project.setWorkspaceType(workspaceType);
        project.setAcademicProjectType(workspaceType == AcademicWorkspaceType.ACADEMIC_PROJECT
                ? request.projectType()
                : null);
        project.setInstitution(normalizeOptionalText(request.institution()));
        project.setDepartment(normalizeOptionalText(request.department()));
        project.setProgramme(normalizeOptionalText(request.programme()));
        project.setAcademicYear(normalizeOptionalText(request.academicYear()));
        project.setSupervisor(normalizeOptionalText(request.supervisor()));
        project.setCourseName(normalizeOptionalText(request.courseName()));
        project.setCourseCode(normalizeOptionalText(request.courseCode()));
        project.setLecturer(normalizeOptionalText(request.lecturer()));
        project.setDeadline(request.deadline());
        project.setResearchAim(normalizeOptionalText(request.researchAim()));
        project.setStudyArea(normalizeOptionalText(request.studyArea()));
        project.setResearchType(normalizeOptionalText(request.researchType()));
        project.setKeywords(normalizeOptionalText(request.keywords()));
        ResearchReportTemplate template = resolveTemplate(
                request.reportTemplateId(),
                workspaceType,
                defaultReportType(workspaceType)
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
        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.ACADEMIC_WORKSPACE_CREATED
        );
        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.ACADEMIC_WORKSPACE_TYPE_SELECTED
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
            AcademicWorkspaceType workspaceType,
            Pageable pageable
    ) {
        WorkspaceMembership workspaceMembership =
                workspaceAuthorizationService.requireActiveMembership(
                        workspaceId,
                        currentUser
                );

        String trimmedQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String pattern = trimmedQuery == null ? null : "%" + trimmedQuery.toLowerCase() + "%";
        boolean isAdmin = authorizationService.isWorkspaceAdmin(workspaceMembership);
        Page<ResearchProject> projects = isAdmin
                ? projectRepository.findAllByWorkspaceIdFiltered(workspaceId, status, workspaceType, pattern, pageable)
                : projectRepository.findAuthorizedMemberProjectsFiltered(workspaceId, currentUser.getId(), status, workspaceType, pattern, pageable);

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
        return listWorkspaceProjects(workspaceId, currentUser, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ResearchProjectResponse> listMyProjects(
            User currentUser,
            String query,
            ResearchProjectStatus status,
            AcademicWorkspaceType workspaceType,
            Pageable pageable
    ) {
        String trimmedQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String pattern = trimmedQuery == null ? null : "%" + trimmedQuery.toLowerCase() + "%";
        Page<ResearchProject> projects = projectRepository.findAllAuthorizedProjectsForUserFiltered(
                currentUser.getId(),
                status,
                workspaceType,
                pattern,
                pageable
        );

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

        if (request.workspaceType() != null && request.workspaceType() != project.getWorkspaceType()) {
            throw new InvalidProjectOperationException(
                    "Workspace type cannot be changed after creation. Create a new workspace or request a validated conversion."
            );
        }
        if (request.projectType() != null && project.getWorkspaceType() != AcademicWorkspaceType.ACADEMIC_PROJECT) {
            throw new InvalidProjectOperationException("Project type applies only to Academic Project workspaces.");
        }
        if (request.title() != null) {
            project.setTitle(normalizeRequiredTitle(request.title()));
        }
        if (request.description() != null) {
            project.setDescription(normalizeOptionalText(request.description()));
        }
        if (request.projectType() != null) {
            project.setAcademicProjectType(request.projectType());
        }
        if (request.institution() != null) {
            project.setInstitution(normalizeOptionalText(request.institution()));
        }
        if (request.department() != null) {
            project.setDepartment(normalizeOptionalText(request.department()));
        }
        if (request.programme() != null) {
            project.setProgramme(normalizeOptionalText(request.programme()));
        }
        if (request.academicYear() != null) {
            project.setAcademicYear(normalizeOptionalText(request.academicYear()));
        }
        if (request.supervisor() != null) {
            project.setSupervisor(normalizeOptionalText(request.supervisor()));
        }
        if (request.courseName() != null) {
            project.setCourseName(normalizeOptionalText(request.courseName()));
        }
        if (request.courseCode() != null) {
            project.setCourseCode(normalizeOptionalText(request.courseCode()));
        }
        if (request.lecturer() != null) {
            project.setLecturer(normalizeOptionalText(request.lecturer()));
        }
        if (request.deadline() != null) {
            project.setDeadline(request.deadline());
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
            ResearchReportTemplate template = resolveTemplate(
                    request.reportTemplateId(),
                    project.getWorkspaceType(),
                    defaultReportType(project.getWorkspaceType())
            );
            project.setReportTemplate(template);
            if (!project.isCitationStyleLocked() && template != null && template.getDefaultCitationStyle() != null) {
                project.setCitationStyle(template.getDefaultCitationStyle());
                project.setCitationStyleLocked(template.isCitationStyleLocked());
            }
            auditService.record(
                    currentUser.getId(),
                    SecurityAuditEventType.WORKSPACE_TEMPLATE_CHANGED
            );
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
        counts.put("conversations", countRows("conversations", projectId));
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

    private ResearchReportTemplate resolveTemplate(
            UUID requestedTemplateId,
            AcademicWorkspaceType workspaceType,
            ResearchReportType reportType
    ) {
        if (requestedTemplateId != null) {
            ResearchReportTemplate template = templateRepository.findById(requestedTemplateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report template not found."));
            if (!templateSupportsWorkspace(template, workspaceType)) {
                throw new InvalidProjectOperationException("Selected template is not compatible with this workspace type.");
            }
            return template;
        }
        return templateRepository.findSystemTemplatesForWorkspaceType(
                        reportType,
                        workspaceType.name(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .or(() -> templateRepository.findFirstByTypeAndSystemTemplateTrueOrderByCreatedAtAsc(reportType))
                .orElse(null);
    }

    private ResearchProjectResponse toProjectResponse(
            ResearchProject project,
            ProjectMembership currentUserMembership
    ) {
        AcademicWorkspaceType workspaceType = project.getWorkspaceType() == null
                ? AcademicWorkspaceType.ACADEMIC_RESEARCH
                : project.getWorkspaceType();
        return new ResearchProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getTitle(),
                project.getDescription(),
                workspaceType,
                workspaceTypeLabel(workspaceType),
                project.getAcademicProjectType(),
                defaultReportType(workspaceType),
                finalDocumentLabel(workspaceType),
                workAreaLabel(workspaceType),
                project.getInstitution(),
                project.getDepartment(),
                project.getProgramme(),
                project.getAcademicYear(),
                project.getSupervisor(),
                project.getCourseName(),
                project.getCourseCode(),
                project.getLecturer(),
                project.getDeadline(),
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

    private void validateWorkspaceMetadata(
            AcademicWorkspaceType workspaceType,
            AcademicProjectType projectType
    ) {
        if (workspaceType != AcademicWorkspaceType.ACADEMIC_PROJECT && projectType != null) {
            throw new InvalidProjectOperationException("Project type applies only to Academic Project workspaces.");
        }
    }

    private boolean templateSupportsWorkspace(
            ResearchReportTemplate template,
            AcademicWorkspaceType workspaceType
    ) {
        String supportedTypes = template.getSupportedWorkspaceTypes();
        return supportedTypes == null
                || supportedTypes.isBlank()
                || supportedTypes.contains(workspaceType.name());
    }

    private ResearchReportType defaultReportType(AcademicWorkspaceType workspaceType) {
        return switch (workspaceType == null ? AcademicWorkspaceType.ACADEMIC_RESEARCH : workspaceType) {
            case ACADEMIC_RESEARCH -> ResearchReportType.RESEARCH_REPORT;
            case ACADEMIC_PROJECT -> ResearchReportType.ACADEMIC_PROJECT_REPORT;
            case COURSEWORK -> ResearchReportType.COURSEWORK;
        };
    }

    private String workspaceTypeLabel(AcademicWorkspaceType workspaceType) {
        return switch (workspaceType == null ? AcademicWorkspaceType.ACADEMIC_RESEARCH : workspaceType) {
            case ACADEMIC_RESEARCH -> "Academic Research";
            case ACADEMIC_PROJECT -> "Academic Project";
            case COURSEWORK -> "Coursework";
        };
    }

    private String finalDocumentLabel(AcademicWorkspaceType workspaceType) {
        return switch (workspaceType == null ? AcademicWorkspaceType.ACADEMIC_RESEARCH : workspaceType) {
            case ACADEMIC_RESEARCH -> "Research Report";
            case ACADEMIC_PROJECT -> "Project Report";
            case COURSEWORK -> "Coursework Document";
        };
    }

    private String workAreaLabel(AcademicWorkspaceType workspaceType) {
        return switch (workspaceType == null ? AcademicWorkspaceType.ACADEMIC_RESEARCH : workspaceType) {
            case ACADEMIC_RESEARCH -> "Study Design";
            case ACADEMIC_PROJECT -> "Project Work";
            case COURSEWORK -> "Notes / Work";
        };
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
