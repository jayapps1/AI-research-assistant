package com.researchassistant.project.dto;

import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        ProjectRole role,
        ProjectMembershipStatus status,
        OffsetDateTime joinedAt
) {
}
