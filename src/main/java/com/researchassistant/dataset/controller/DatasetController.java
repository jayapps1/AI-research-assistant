package com.researchassistant.dataset.controller;

import com.researchassistant.dataset.dto.DatasetDtos.*;
import com.researchassistant.dataset.service.DatasetImportService;
import com.researchassistant.dataset.service.DatasetService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DatasetController {
    private final DatasetService datasetService;
    private final DatasetImportService importService;
    private final AuthenticatedUserResolver userResolver;

    public DatasetController(DatasetService datasetService, DatasetImportService importService, AuthenticatedUserResolver userResolver) {
        this.datasetService = datasetService;
        this.importService = importService;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/datasets")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetResponse create(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateDatasetRequest request) {
        return datasetService.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/datasets")
    public PageResponse<DatasetResponse> list(Authentication authentication, @PathVariable UUID projectId,
                                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(datasetService.list(projectId, user(authentication), PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))));
    }

    @PostMapping("/datasets/{datasetId}/variables")
    @ResponseStatus(HttpStatus.CREATED)
    public VariableResponse addVariable(Authentication authentication, @PathVariable UUID datasetId, @Valid @RequestBody CreateVariableRequest request) {
        return datasetService.addVariable(datasetId, user(authentication), request);
    }

    @PostMapping("/projects/{projectId}/datasets/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportStartResponse startImport(Authentication authentication, @PathVariable UUID projectId, @RequestParam("file") MultipartFile file) {
        return importService.start(projectId, user(authentication), file);
    }

    @GetMapping("/dataset-imports/{importJobId}/preview")
    public ImportPreviewResponse preview(Authentication authentication, @PathVariable UUID importJobId) {
        return importService.preview(importJobId, user(authentication));
    }

    @PostMapping("/dataset-imports/{importJobId}/confirm")
    public ImportStartResponse confirm(Authentication authentication, @PathVariable UUID importJobId, @Valid @RequestBody ConfirmMappingRequest request) {
        return importService.confirm(importJobId, user(authentication), request);
    }

    @PostMapping("/datasets/{datasetId}/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validate(Authentication authentication, @PathVariable UUID datasetId) {
        datasetService.validate(datasetId, user(authentication));
    }

    @GetMapping("/datasets/{datasetId}/validation-issues")
    public PageResponse<ValidationIssueResponse> issues(Authentication authentication, @PathVariable UUID datasetId,
                                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(datasetService.issues(datasetId, user(authentication), PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))));
    }

    @GetMapping("/datasets/{datasetId}/summary")
    public DatasetSummaryResponse summary(Authentication authentication, @PathVariable UUID datasetId) {
        return datasetService.summary(datasetId, user(authentication));
    }

    private User user(Authentication authentication) { return userResolver.requireActiveUser(authentication); }
}
