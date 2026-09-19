package com.researchassistant.identity.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String phoneNumber,
        String locale,
        List<String> systemRoles,
        ProfileImageDto profileImage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public UserProfileResponse {
        systemRoles = systemRoles == null ? List.of() : List.copyOf(systemRoles);
        profileImage = profileImage == null ? ProfileImageDto.empty() : profileImage;
    }
}
