package com.researchassistant.admin;

import com.researchassistant.audit.AuditEvent;
import com.researchassistant.audit.AuditEventRepository;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.repository.DocumentProcessingJobRepository;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.subscription.*;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AuthenticatedUserResolver userResolver;
    private final SystemAdminAuthorizationService adminAuthorizationService;
    private final AdminDashboardService dashboardService;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AuditEventRepository auditEventRepository;
    private final ComplimentaryAccessService complimentaryAccessService;
    private final ResearchProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final DocumentProcessingJobRepository processingJobRepository;
    private final com.researchassistant.analysis.repository.ResearchReportTemplateRepository reportTemplateRepository;
    private final com.researchassistant.ai.usage.AiRequestRepository aiRequestRepository;
    private final EntityManager entityManager;

    public AdminController(AuthenticatedUserResolver userResolver, SystemAdminAuthorizationService adminAuthorizationService,
                           AdminDashboardService dashboardService, UserRepository userRepository, WorkspaceRepository workspaceRepository,
                           AuditEventRepository auditEventRepository, ComplimentaryAccessService complimentaryAccessService,
                           ResearchProjectRepository projectRepository, DocumentRepository documentRepository,
                           DocumentProcessingJobRepository processingJobRepository,
                           com.researchassistant.analysis.repository.ResearchReportTemplateRepository reportTemplateRepository,
                           com.researchassistant.ai.usage.AiRequestRepository aiRequestRepository,
                           EntityManager entityManager) {
        this.userResolver = userResolver;
        this.adminAuthorizationService = adminAuthorizationService;
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.auditEventRepository = auditEventRepository;
        this.complimentaryAccessService = complimentaryAccessService;
        this.projectRepository = projectRepository;
        this.documentRepository = documentRepository;
        this.processingJobRepository = processingJobRepository;
        this.reportTemplateRepository = reportTemplateRepository;
        this.aiRequestRepository = aiRequestRepository;
        this.entityManager = entityManager;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        requireAdmin(authentication);
        return dashboardService.dashboard();
    }

    @GetMapping("/operations/status")
    public Map<String, Object> operations(Authentication authentication) {
        requireAdmin(authentication);
        return Map.of("database", "UP", "paystack", "TEST_MODE_CONFIGURED_WHEN_KEYS_PRESENT",
                "email", "CONFIGURATION_DEPENDENT", "arkesel", "DISABLED_BY_DEFAULT",
                "firebase", "DISABLED_BY_DEFAULT");
    }

    @GetMapping("/users")
    public Page<Map<String, Object>> users(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return userRepository.findAll(pageable).map(u -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("status", u.getStatus() != null ? u.getStatus().name() : "ACTIVE");
            map.put("authenticationMethod", u.getAuthenticationMethod() != null ? u.getAuthenticationMethod().name() : "PASSWORD");
            map.put("emailVerified", u.isEmailVerified());
            map.put("createdAt", u.getCreatedAt());
            return map;
        });
    }

    @GetMapping("/users/{userId}")
    public Map<String, Object> user(@PathVariable UUID userId, Authentication authentication) {
        requireAdmin(authentication);
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new com.researchassistant.common.exception.ResourceNotFoundException("User not found."));
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("email", u.getEmail());
        map.put("firstName", u.getFirstName());
        map.put("lastName", u.getLastName());
        map.put("status", u.getStatus() != null ? u.getStatus().name() : "ACTIVE");
        map.put("authenticationMethod", u.getAuthenticationMethod() != null ? u.getAuthenticationMethod().name() : "PASSWORD");
        map.put("emailVerified", u.isEmailVerified());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }

    @PostMapping("/users/{userId}/suspend")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> suspend(@PathVariable UUID userId, Authentication authentication) {
        User admin = requireAdmin(authentication);
        if (admin.getId().equals(userId)) {
            throw new IllegalArgumentException("Administrators cannot suspend their own account.");
        }
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new com.researchassistant.common.exception.ResourceNotFoundException("User not found."));
        u.setStatus(UserStatus.SUSPENDED);
        userRepository.save(u);
        return Map.of("id", u.getId(), "status", u.getStatus().name());
    }

    @PostMapping("/users/{userId}/reactivate")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> reactivate(@PathVariable UUID userId, Authentication authentication) {
        requireAdmin(authentication);
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new com.researchassistant.common.exception.ResourceNotFoundException("User not found."));
        u.setStatus(UserStatus.ACTIVE);
        userRepository.save(u);
        return Map.of("id", u.getId(), "status", u.getStatus().name());
    }

    @GetMapping("/workspaces")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<Map<String, Object>> workspaces(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return workspaceRepository.findAll(pageable).map(w -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", w.getId());
            map.put("name", w.getName());
            map.put("type", w.getType() != null ? w.getType().name() : "");
            map.put("status", w.getStatus() != null ? w.getStatus().name() : "ACTIVE");
            map.put("ownerId", w.getOwner() != null ? w.getOwner().getId() : null);
            map.put("ownerEmail", w.getOwner() != null ? w.getOwner().getEmail() : null);
            map.put("createdAt", w.getCreatedAt());
            return map;
        });
    }

    @GetMapping("/projects")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<Map<String, Object>> projects(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return projectRepository.findAll(pageable).map(this::toProjectMap);
    }

    @GetMapping("/documents")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<Map<String, Object>> documents(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return documentRepository.findAll(pageable).map(this::toDocumentMap);
    }

    @GetMapping("/processing-jobs")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<Map<String, Object>> processingJobs(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return processingJobRepository.findAll(pageable).map(this::toProcessingJobMap);
    }

    @GetMapping("/report-templates")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<Map<String, Object>> reportTemplates(Authentication authentication) {
        requireAdmin(authentication);
        return reportTemplateRepository.findAll().stream().map(t -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("type", t.getType());
            map.put("institution", t.getInstitution());
            map.put("department", t.getDepartment());
            map.put("systemTemplate", t.isSystemTemplate());
            map.put("defaultCitationStyle", t.getDefaultCitationStyle());
            map.put("citationStyleLocked", t.isCitationStyleLocked());
            map.put("description", t.getDescription());
            map.put("createdAt", t.getCreatedAt());
            return map;
        }).toList();
    }

    @GetMapping("/research-templates")
    public java.util.List<Map<String, Object>> researchTemplates(Authentication authentication) {
        requireAdmin(authentication);
        return java.util.List.of(
                Map.of("researchType", "GENERAL_ACADEMIC_RESEARCH", "components", java.util.List.of("Problem Statement", "Objectives", "Questions", "Frameworks", "Methodology")),
                Map.of("researchType", "SOFTWARE_SYSTEM_PROJECT", "components", java.util.List.of("Requirements", "Use Cases", "Architecture", "Database Design", "Testing")),
                Map.of("researchType", "QUANTITATIVE_SURVEY", "components", java.util.List.of("Population", "Sampling", "Variables", "Questionnaire", "Statistical Analysis")),
                Map.of("researchType", "QUALITATIVE_RESEARCH", "components", java.util.List.of("Participants", "Interview Guide", "Transcripts", "Coding", "Themes")),
                Map.of("researchType", "MIXED_METHODS", "components", java.util.List.of("Quantitative Strand", "Qualitative Strand", "Integration", "Triangulation"))
        );
    }

    @GetMapping("/ai-operations")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<Map<String, Object>> aiOperations(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return aiRequestRepository.findAll(pageable).map(r -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("workspaceId", r.getWorkspace() == null ? null : r.getWorkspace().getId());
            map.put("projectId", r.getProject() == null ? null : r.getProject().getId());
            map.put("userId", r.getUser() == null ? null : r.getUser().getId());
            map.put("taskType", r.getTaskType());
            map.put("provider", r.getProvider());
            map.put("model", r.getModel());
            map.put("status", r.getStatus());
            map.put("inputTokens", r.getInputTokens());
            map.put("outputTokens", r.getOutputTokens());
            map.put("totalTokens", r.getTotalTokens());
            map.put("latencyMs", r.getLatencyMs());
            map.put("failureCategory", r.getFailureCategory());
            map.put("failureCode", r.getFailureCode());
            map.put("createdAt", r.getCreatedAt());
            return map;
        });
    }

    @GetMapping("/ai-usage")
    public Map<String, Object> aiUsage(Authentication authentication) {
        requireAdmin(authentication);
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("totalRequests", count("ai_requests"));
        map.put("completedRequests", countWhere("ai_requests", "status = 'COMPLETED'"));
        map.put("failedRequests", countWhere("ai_requests", "status in ('FAILED','REJECTED')"));
        map.put("inputTokens", sum("ai_requests", "input_tokens"));
        map.put("outputTokens", sum("ai_requests", "output_tokens"));
        map.put("totalTokens", sum("ai_requests", "total_tokens"));
        map.put("costTracked", count("ai_usage_costs"));
        return map;
    }

    @GetMapping("/storage")
    public Map<String, Object> storage(Authentication authentication) {
        requireAdmin(authentication);
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("mode", "LOCAL DEVELOPMENT");
        map.put("documentCount", count("documents"));
        map.put("versionCount", count("document_versions"));
        map.put("storedBytes", sum("document_versions", "file_size_bytes"));
        map.put("workspaceUsage", entityManager.createNativeQuery("""
                select p.workspace_id, coalesce(sum(v.file_size_bytes), 0)
                from documents d
                join research_projects p on p.id = d.project_id
                left join document_versions v on v.document_id = d.id
                group by p.workspace_id
                order by 2 desc
                limit 20
                """).getResultList());
        return map;
    }

    @GetMapping("/references/styles")
    public java.util.List<Map<String, Object>> referenceStyles(Authentication authentication) {
        requireAdmin(authentication);
        return java.util.List.of(
                Map.of("id", "APA_7", "name", "APA 7th Edition", "active", true),
                Map.of("id", "IEEE", "name", "IEEE", "active", true),
                Map.of("id", "HARVARD", "name", "Harvard", "active", true),
                Map.of("id", "CHICAGO_AUTHOR_DATE", "name", "Chicago Author-Date", "active", true),
                Map.of("id", "MLA_9", "name", "MLA 9", "active", true),
                Map.of("id", "VANCOUVER", "name", "Vancouver", "active", true)
        );
    }

    @GetMapping("/system-health")
    public Map<String, Object> systemHealth(Authentication authentication) {
        requireAdmin(authentication);
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("application", "UP");
        map.put("database", databaseHealth());
        map.put("storage", "LOCAL DEVELOPMENT");
        map.put("backgroundJobs", Map.of(
                "queued", countWhere("document_processing_jobs", "status = 'QUEUED'"),
                "running", countWhere("document_processing_jobs", "status = 'RUNNING'"),
                "failed", countWhere("document_processing_jobs", "status = 'FAILED'")
        ));
        map.put("aiProvider", Map.of("configured", count("ai_model_pricing") >= 0, "credentialsExposed", false));
        return map;
    }

    @GetMapping("/settings")
    public Map<String, Object> settings(Authentication authentication) {
        requireAdmin(authentication);
        return Map.of(
                "credentialsExposed", false,
                "publicSiteEnabled", true,
                "billingEnabled", true,
                "supportedCitationStyles", java.util.List.of("APA_7", "IEEE", "HARVARD", "CHICAGO_AUTHOR_DATE", "MLA_9", "VANCOUVER")
        );
    }

    @GetMapping("/workspaces/{workspaceId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> workspace(@PathVariable UUID workspaceId, Authentication authentication) {
        requireAdmin(authentication);
        Workspace w = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new com.researchassistant.common.exception.ResourceNotFoundException("Workspace not found."));
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", w.getId());
        map.put("name", w.getName());
        map.put("type", w.getType() != null ? w.getType().name() : "");
        map.put("status", w.getStatus() != null ? w.getStatus().name() : "ACTIVE");
        map.put("ownerId", w.getOwner() != null ? w.getOwner().getId() : null);
        map.put("ownerEmail", w.getOwner() != null ? w.getOwner().getEmail() : null);
        map.put("createdAt", w.getCreatedAt());
        return map;
    }

    @GetMapping("/audit-events")
    public Page<AuditEvent> audit(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return auditEventRepository.findAllByOrderByOccurredAtDesc(pageable);
    }

    @PostMapping("/complimentary-access")
    public Map<String, Object> grantComplimentaryAccess(@Valid @RequestBody ComplimentaryAccessService.GrantRequest request,
                                                        Authentication authentication) {
        User admin = requireAdmin(authentication);
        return toGrantResponse(complimentaryAccessService.grant(request, admin));
    }

    @GetMapping("/complimentary-access")
    public Page<Map<String, Object>> complimentaryAccess(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return complimentaryAccessService.list(pageable).map(this::toGrantResponse);
    }

    @GetMapping("/complimentary-access/{grantId}")
    public Map<String, Object> complimentaryAccessGrant(@PathVariable UUID grantId, Authentication authentication) {
        requireAdmin(authentication);
        return toGrantResponse(complimentaryAccessService.get(grantId));
    }

    @PostMapping("/complimentary-access/{grantId}/revoke")
    public Map<String, Object> revokeComplimentaryAccess(@PathVariable UUID grantId,
                                                         @RequestBody RevokeGrantRequest request,
                                                         Authentication authentication) {
        User admin = requireAdmin(authentication);
        return toGrantResponse(complimentaryAccessService.revoke(grantId, request == null ? null : request.reason(), admin));
    }

    private User requireAdmin(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return user;
    }

    private Map<String, Object> toGrantResponse(ComplimentaryAccessGrant grant) {
        return Map.of(
                "id", grant.getId(),
                "scope", grant.getScope(),
                "userId", grant.getUser() == null ? "" : grant.getUser().getId(),
                "workspaceId", grant.getWorkspace() == null ? "" : grant.getWorkspace().getId(),
                "planCode", grant.getPlan() == null ? "" : grant.getPlan().getCode(),
                "type", grant.getType(),
                "status", grant.getStatus(),
                "startsAt", grant.getStartsAt(),
                "expiresAt", grant.getExpiresAt() == null ? "" : grant.getExpiresAt()
        );
    }

    private Map<String, Object> toProjectMap(ResearchProject p) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("title", p.getTitle());
        map.put("workspaceId", p.getWorkspace() == null ? null : p.getWorkspace().getId());
        map.put("workspaceName", p.getWorkspace() == null ? null : p.getWorkspace().getName());
        map.put("ownerId", p.getCreatedBy() == null ? null : p.getCreatedBy().getId());
        map.put("ownerEmail", p.getCreatedBy() == null ? null : p.getCreatedBy().getEmail());
        map.put("researchType", p.getResearchType());
        map.put("status", p.getStatus());
        map.put("reportTemplateId", p.getReportTemplate() == null ? null : p.getReportTemplate().getId());
        map.put("citationStyle", p.getCitationStyle());
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toDocumentMap(Document d) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", d.getId());
        map.put("documentCode", d.getDocumentCode());
        map.put("title", d.getTitle());
        map.put("projectId", d.getProject() == null ? null : d.getProject().getId());
        map.put("workspaceId", d.getProject() == null || d.getProject().getWorkspace() == null ? null : d.getProject().getWorkspace().getId());
        map.put("status", d.getStatus());
        map.put("type", d.getType());
        map.put("currentVersion", d.getCurrentVersion() == null ? null : d.getCurrentVersion().getVersionNumber());
        map.put("fileSizeBytes", d.getCurrentVersion() == null ? null : d.getCurrentVersion().getFileSizeBytes());
        map.put("createdAt", d.getCreatedAt());
        map.put("updatedAt", d.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toProcessingJobMap(DocumentProcessingJob j) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", j.getId());
        map.put("documentVersionId", j.getDocumentVersion() == null ? null : j.getDocumentVersion().getId());
        map.put("documentId", j.getDocumentVersion() == null || j.getDocumentVersion().getDocument() == null ? null : j.getDocumentVersion().getDocument().getId());
        map.put("type", j.getType());
        map.put("status", j.getStatus());
        map.put("attemptNumber", j.getAttemptNumber());
        map.put("errorCode", j.getErrorCode());
        map.put("errorMessage", j.getErrorMessage());
        map.put("queuedAt", j.getQueuedAt());
        map.put("startedAt", j.getStartedAt());
        map.put("completedAt", j.getCompletedAt());
        map.put("failedAt", j.getFailedAt());
        return map;
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }

    private long countWhere(String table, String whereClause) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table + " where " + whereClause).getSingleResult()).longValue();
    }

    private long sum(String table, String column) {
        return ((Number) entityManager.createNativeQuery("select coalesce(sum(" + column + "), 0) from " + table).getSingleResult()).longValue();
    }

    private String databaseHealth() {
        try {
            entityManager.createNativeQuery("select 1").getSingleResult();
            return "UP";
        } catch (RuntimeException ex) {
            return "DOWN";
        }
    }

    public record RevokeGrantRequest(String reason) {}
}
