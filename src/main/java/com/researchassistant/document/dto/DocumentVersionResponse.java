package com.researchassistant.document.dto;

import com.researchassistant.document.entity.DocumentVersionStatus;
import com.researchassistant.document.security.FileScanStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID versionId,
        int versionNumber,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        String checksumSha256,
        FileScanStatus scanStatus,
        boolean quarantined,
        DocumentVersionStatus status,
        OffsetDateTime uploadedAt
) {
}
