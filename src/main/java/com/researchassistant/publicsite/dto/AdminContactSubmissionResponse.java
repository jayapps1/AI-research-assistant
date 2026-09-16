package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.ContactSubmission;
import com.researchassistant.publicsite.entity.ContactSubmissionSource;
import com.researchassistant.publicsite.entity.ContactSubmissionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminContactSubmissionResponse(
        UUID id,
        String referenceCode,
        String name,
        String email,
        String phone,
        String subject,
        String message,
        ContactSubmissionStatus status,
        ContactSubmissionSource source,
        UUID assignedToId,
        String assignedToName,
        OffsetDateTime submittedAt,
        OffsetDateTime firstReadAt,
        OffsetDateTime closedAt,
        OffsetDateTime createdAt,
        List<AdminContactResponseDto> responses
) {
    public static AdminContactSubmissionResponse fromEntity(ContactSubmission submission) {
        String assignedName = null;
        if (submission.getAssignedTo() != null) {
            assignedName = submission.getAssignedTo().getFirstName() != null
                    ? submission.getAssignedTo().getFirstName() + " " + submission.getAssignedTo().getLastName()
                    : submission.getAssignedTo().getEmail();
        }

        List<AdminContactResponseDto> responseDtos = submission.getResponses() == null
                ? List.of()
                : submission.getResponses().stream()
                .map(AdminContactResponseDto::fromEntity)
                .toList();

        return new AdminContactSubmissionResponse(
                submission.getId(),
                submission.getReferenceCode(),
                submission.getName(),
                submission.getEmail(),
                submission.getPhone(),
                submission.getSubject(),
                submission.getMessage(),
                submission.getStatus(),
                submission.getSource(),
                submission.getAssignedTo() != null ? submission.getAssignedTo().getId() : null,
                assignedName,
                submission.getSubmittedAt(),
                submission.getFirstReadAt(),
                submission.getClosedAt(),
                submission.getCreatedAt(),
                responseDtos
        );
    }
}
