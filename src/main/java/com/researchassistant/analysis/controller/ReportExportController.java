package com.researchassistant.analysis.controller;

import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.service.ReportExportService;
import com.researchassistant.document.storage.DocumentStorageObject;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportExportController {
    private final ReportExportService exportService;
    private final AuthenticatedUserResolver userResolver;

    public ReportExportController(ReportExportService exportService, AuthenticatedUserResolver userResolver) {
        this.exportService = exportService;
        this.userResolver = userResolver;
    }

    public record CreateReportExportRequest(@NotNull ReportExportFormat format, Boolean draft) {}
    public record ReportExportResponse(UUID id, UUID reportId, ReportExportFormat format, ReportExportStatus status,
                                       String filename, String mimeType, Long fileSizeBytes, String checksumSha256,
                                       int reportRevisionNumber) {
        static ReportExportResponse from(ReportExportJob job) {
            return new ReportExportResponse(job.getId(), job.getReport().getId(), job.getFormat(), job.getStatus(),
                    job.getFilename(), job.getMimeType(), job.getFileSizeBytes(), job.getChecksumSha256(),
                    job.getReportRevisionNumber());
        }
    }

    @PostMapping("/reports/{reportId}/exports")
    public ReportExportResponse create(Authentication authentication, @PathVariable UUID reportId, @RequestBody CreateReportExportRequest request) {
        return ReportExportResponse.from(exportService.createExport(reportId, request.format(), Boolean.TRUE.equals(request.draft()), user(authentication)));
    }

    @GetMapping("/report-exports/{exportId}")
    public ReportExportResponse get(Authentication authentication, @PathVariable UUID exportId) {
        return ReportExportResponse.from(exportService.get(exportId, user(authentication)));
    }

    @GetMapping("/report-exports/{exportId}/download")
    public ResponseEntity<byte[]> download(Authentication authentication, @PathVariable UUID exportId) throws IOException {
        ReportExportJob job = exportService.get(exportId, user(authentication));
        DocumentStorageObject object = exportService.download(exportId, user(authentication));
        try (java.io.InputStream input = object.inputStream()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(job.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(job.getFilename().replaceAll("[\\r\\n\"]", "_"))
                            .build().toString())
                    .body(input.readAllBytes());
        }
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
