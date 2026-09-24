package com.researchassistant.reference.controller;

import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.dto.PageResponse;
import com.researchassistant.reference.dto.ReferenceDtos.*;
import com.researchassistant.reference.entity.*;
import com.researchassistant.reference.service.ReferenceLibraryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReferenceController {
    private final ReferenceLibraryService service;
    private final AuthenticatedUserResolver userResolver;

    public ReferenceController(ReferenceLibraryService service, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/references")
    @ResponseStatus(HttpStatus.CREATED)
    public ReferenceResponse create(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateReferenceRequest request) {
        return service.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/references")
    public PageResponse<ReferenceResponse> list(Authentication authentication, @PathVariable UUID projectId,
            @RequestParam(required = false) String author, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) ReferenceType type, @RequestParam(required = false) String title,
            @RequestParam(required = false) String citationKey, @RequestParam(required = false) ReferenceMetadataStatus metadataStatus,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.list(projectId, user(authentication), author, year, type, title, citationKey, metadataStatus, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))));
    }

    @GetMapping("/references/{referenceId}")
    public ReferenceResponse get(Authentication authentication, @PathVariable UUID referenceId) {
        return service.get(referenceId, user(authentication));
    }

    @PatchMapping("/references/{referenceId}")
    public ReferenceResponse update(Authentication authentication, @PathVariable UUID referenceId, @Valid @RequestBody UpdateReferenceRequest request) {
        return service.update(referenceId, user(authentication), request);
    }

    @PostMapping("/references/{referenceId}/archive")
    public ReferenceResponse archive(Authentication authentication, @PathVariable UUID referenceId) {
        return service.archive(referenceId, user(authentication));
    }

    @PostMapping("/references/{referenceId}/citation-enabled")
    public ReferenceResponse setCitationEnabled(Authentication authentication, @PathVariable UUID referenceId, @RequestParam boolean enabled) {
        return service.setAvailableForCitation(referenceId, user(authentication), enabled);
    }

    @PostMapping("/references/{referenceId}/research-enabled")
    public ReferenceResponse setResearchEnabled(Authentication authentication, @PathVariable UUID referenceId, @RequestParam boolean enabled) {
        return service.setAvailableForResearchAi(referenceId, user(authentication), enabled);
    }

    @PostMapping("/references/{referenceId}/usage-scope")
    public ReferenceResponse setUsageScope(Authentication authentication, @PathVariable UUID referenceId, @RequestBody UpdateReferenceUsageScopeRequest request) {
        return service.setUsageScope(referenceId, user(authentication), request.availableForResearchAi(), request.availableForCitation());
    }

    @PostMapping("/projects/{projectId}/references/check-duplicates")
    public DuplicateCheckResponse checkDuplicates(Authentication authentication, @PathVariable UUID projectId, @RequestBody DuplicateCheckRequest request) {
        return service.checkDuplicates(projectId, user(authentication), request);
    }

    @PostMapping("/references/format-citation")
    public FormattedCitation format(Authentication authentication, @Valid @RequestBody FormatCitationRequest request) {
        return service.format(user(authentication), request);
    }

    @PostMapping("/projects/{projectId}/references/import")
    public ImportJobResponse importReferences(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody ImportRequest request) {
        return service.importPreview(projectId, user(authentication), request);
    }

    @GetMapping("/reference-imports/{importJobId}/preview")
    public List<ImportPreviewItem> preview(Authentication authentication, @PathVariable UUID importJobId) {
        return service.preview(importJobId, user(authentication));
    }

    @PostMapping("/reference-imports/{importJobId}/confirm")
    public ImportJobResponse confirm(Authentication authentication, @PathVariable UUID importJobId, @RequestBody ConfirmImportRequest request) {
        return service.confirm(importJobId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/references/export")
    public ResponseEntity<String> export(Authentication authentication, @PathVariable UUID projectId,
            @RequestParam ReferenceImportFormat format, @RequestParam(required = false) List<UUID> referenceIds) {
        ReferenceLibraryService.ExportedReferences export = service.export(projectId, user(authentication), format, referenceIds);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .contentType(MediaType.parseMediaType(export.contentType()))
                .body(export.content());
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
