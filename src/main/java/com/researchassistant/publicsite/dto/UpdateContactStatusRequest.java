package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.ContactSubmissionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateContactStatusRequest(
        @NotNull(message = "Status is required.")
        ContactSubmissionStatus status
) {}
