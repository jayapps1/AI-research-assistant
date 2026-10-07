package com.researchassistant.template.controller;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import com.researchassistant.template.dto.AcademicTemplateDtos.*;
import com.researchassistant.template.service.AcademicTemplateService;
import com.researchassistant.template.service.ReportDynamicTocService;
import com.researchassistant.template.service.ReportStructureValidationService;
import com.researchassistant.template.service.TemplateRelevanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AcademicTemplateController {

    private final AcademicTemplateService templateService;
    private final ReportDynamicTocService dynamicTocService;
    private final ReportStructureValidationService validationService;
    private final TemplateRelevanceService relevanceService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AcademicTemplateController(
            AcademicTemplateService templateService,
            ReportDynamicTocService dynamicTocService,
            ReportStructureValidationService validationService,
            TemplateRelevanceService relevanceService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.templateService = templateService;
        this.dynamicTocService = dynamicTocService;
        this.validationService = validationService;
        this.relevanceService = relevanceService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping(
            value = "/workspaces/{workspaceId}/templates/guidelines",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicDocumentGuidelineResponse uploadWorkspaceGuideline(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestPart("file") MultipartFile file
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.uploadAndAnalyzeGuideline(workspaceId, null, file, user);
    }

    @PostMapping(
            value = "/workspaces/{workspaceId}/academic-templates/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicDocumentGuidelineResponse uploadWorkspaceGuidelineLegacy(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) UUID projectId,
            @RequestPart("file") MultipartFile file
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.uploadAndAnalyzeGuideline(workspaceId, projectId, file, user);
    }

    @PostMapping(
            value = "/projects/{projectId}/templates/guidelines",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicDocumentGuidelineResponse uploadProjectGuideline(
            Authentication authentication,
            @PathVariable UUID projectId,
            @RequestParam UUID workspaceId,
            @RequestPart("file") MultipartFile file
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.uploadAndAnalyzeGuideline(workspaceId, projectId, file, user);
    }

    @GetMapping("/templates/guidelines/{guidelineId}")
    public AcademicDocumentGuidelineResponse getGuideline(
            Authentication authentication,
            @PathVariable UUID guidelineId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.getGuideline(guidelineId, user);
    }

    @GetMapping("/academic-templates/{guidelineId}")
    public AcademicDocumentGuidelineResponse getGuidelineLegacy(Authentication authentication, @PathVariable UUID guidelineId) {
        return getGuideline(authentication, guidelineId);
    }

    @PutMapping("/templates/guidelines/{guidelineId}")
    public AcademicDocumentGuidelineResponse updateGuideline(
            Authentication authentication,
            @PathVariable UUID guidelineId,
            @RequestBody UpdateAcademicGuidelineRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.updateGuideline(guidelineId, request, user);
    }

    @PutMapping("/academic-templates/{guidelineId}")
    public AcademicDocumentGuidelineResponse updateGuidelineLegacy(
            Authentication authentication,
            @PathVariable UUID guidelineId,
            @RequestBody ExtractedAcademicTemplate request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.updateGuideline(guidelineId, new UpdateAcademicGuidelineRequest(
                request.institution(), request.department(), request.programme(), request.documentType(), request.citationStyle(), request
        ), user);
    }

    @PostMapping("/templates/guidelines/{guidelineId}/approve")
    public AcademicDocumentGuidelineResponse approveGuideline(
            Authentication authentication,
            @PathVariable UUID guidelineId,
            @RequestParam(required = false) UUID projectId,
            @RequestBody ApproveGuidelineRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.approveAndApplyGuideline(guidelineId, projectId, request, user);
    }

    @PostMapping("/academic-templates/{guidelineId}/approve")
    public AcademicDocumentGuidelineResponse approveGuidelineLegacy(
            Authentication authentication,
            @PathVariable UUID guidelineId,
            @RequestBody ApproveGuidelineRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.approveAndApplyGuideline(guidelineId, request.targetProjectId(), request, user);
    }

    @GetMapping("/workspaces/{workspaceId}/templates/guidelines")
    public List<AcademicDocumentGuidelineResponse> listWorkspaceGuidelines(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.listGuidelines(workspaceId, null, user);
    }

    @GetMapping("/workspaces/{workspaceId}/academic-templates")
    public List<AcademicDocumentGuidelineResponse> listWorkspaceGuidelinesLegacy(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.listGuidelines(workspaceId, projectId, user);
    }

    @GetMapping("/projects/{projectId}/templates/guidelines")
    public List<AcademicDocumentGuidelineResponse> listProjectGuidelines(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return templateService.listGuidelines(null, projectId, user);
    }

    @GetMapping("/projects/{projectId}/report/toc")
    public DynamicTableOfContentsResponse getDynamicToc(
            @PathVariable UUID projectId
    ) {
        return dynamicTocService.generateDynamicToc(projectId);
    }

    @GetMapping("/projects/{projectId}/report/validation")
    public DocumentStructureValidationResponse getStructureValidation(
            @PathVariable UUID projectId
    ) {
        return validationService.validateReportStructure(projectId);
    }

    @GetMapping("/report-templates/recommended")
    public List<RecommendedTemplateResponse> getRecommendedTemplates(
            @RequestParam(required = false) AcademicWorkspaceType workspaceType,
            @RequestParam(required = false) AcademicProjectType projectType,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String documentType
    ) {
        return relevanceService.getRecommendedTemplates(workspaceType, projectType, institution, department, documentType);
    }
}
