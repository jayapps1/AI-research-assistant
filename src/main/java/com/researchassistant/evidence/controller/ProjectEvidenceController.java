package com.researchassistant.evidence.controller;

import com.researchassistant.evidence.dto.ProjectEvidenceDtos.*;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.service.ProjectEvidenceListService;
import com.researchassistant.evidence.service.ProjectEvidenceService;
import com.researchassistant.evidence.service.ProjectImageAnalysisService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class ProjectEvidenceController {

    private final ProjectEvidenceService evidenceService;
    private final ProjectImageAnalysisService imageAnalysisService;
    private final ProjectEvidenceListService evidenceListService;
    private final AuthenticatedUserResolver userResolver;

    public ProjectEvidenceController(
            ProjectEvidenceService evidenceService,
            ProjectImageAnalysisService imageAnalysisService,
            ProjectEvidenceListService evidenceListService,
            AuthenticatedUserResolver userResolver
    ) {
        this.evidenceService = evidenceService;
        this.imageAnalysisService = imageAnalysisService;
        this.evidenceListService = evidenceListService;
        this.userResolver = userResolver;
    }

    @PostMapping("/evidence/upload")
    public ResponseEntity<ProjectEvidenceResponse> uploadEvidence(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "evidenceType", required = false, defaultValue = "SCREENSHOT") EvidenceType evidenceType,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "sectionId", required = false) UUID sectionId,
            @RequestParam(value = "altText", required = false) String altText,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        ProjectEvidenceResponse response = evidenceService.uploadEvidence(
                projectId, file, evidenceType, caption, description, sectionId, altText, user
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/evidence/structured")
    public ResponseEntity<ProjectEvidenceResponse> createStructuredEvidence(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateStructuredEvidenceRequest request,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        ProjectEvidenceResponse response = evidenceService.createStructuredEvidence(projectId, request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence")
    public ResponseEntity<List<ProjectEvidenceResponse>> listEvidence(
            @PathVariable UUID projectId,
            @RequestParam(value = "sectionId", required = false) UUID sectionId,
            @RequestParam(value = "type", required = false) EvidenceType type,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return ResponseEntity.ok(evidenceService.listProjectEvidence(projectId, sectionId, type, user));
    }

    @GetMapping("/evidence/{evidenceId}")
    public ResponseEntity<ProjectEvidenceResponse> getEvidence(
            @PathVariable UUID projectId,
            @PathVariable UUID evidenceId,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return ResponseEntity.ok(evidenceService.getEvidence(evidenceId, user));
    }

    @PutMapping("/evidence/{evidenceId}")
    public ResponseEntity<ProjectEvidenceResponse> updateEvidence(
            @PathVariable UUID projectId,
            @PathVariable UUID evidenceId,
            @RequestBody UpdateProjectEvidenceRequest request,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return ResponseEntity.ok(evidenceService.updateEvidence(evidenceId, request, user));
    }

    @DeleteMapping("/evidence/{evidenceId}")
    public ResponseEntity<Void> deleteEvidence(
            @PathVariable UUID projectId,
            @PathVariable UUID evidenceId,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        evidenceService.deleteEvidence(evidenceId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evidence/reorder")
    public ResponseEntity<List<ProjectEvidenceResponse>> reorderEvidence(
            @PathVariable UUID projectId,
            @Valid @RequestBody ReorderEvidenceRequest request,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return ResponseEntity.ok(evidenceService.reorderEvidence(projectId, request.evidenceIds(), user));
    }

    @PostMapping("/evidence/{evidenceId}/analyze")
    public ResponseEntity<ProjectImageAnalysisResult> explainFigure(
            @PathVariable UUID projectId,
            @PathVariable UUID evidenceId,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return ResponseEntity.ok(imageAnalysisService.analyzeEvidence(evidenceId, user));
    }

    @GetMapping("/reports/{reportId}/list-of-figures")
    public ResponseEntity<List<ListOfFiguresItem>> getListOfFigures(
            @PathVariable UUID projectId,
            @PathVariable UUID reportId
    ) {
        return ResponseEntity.ok(evidenceListService.generateListOfFigures(reportId));
    }

    @GetMapping("/reports/{reportId}/list-of-tables")
    public ResponseEntity<List<ListOfTablesItem>> getListOfTables(
            @PathVariable UUID projectId,
            @PathVariable UUID reportId
    ) {
        return ResponseEntity.ok(evidenceListService.generateListOfTables(reportId));
    }
}
