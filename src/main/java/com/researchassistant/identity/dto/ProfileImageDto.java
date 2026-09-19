package com.researchassistant.identity.dto;

import java.time.OffsetDateTime;

public record ProfileImageDto(
        boolean available,
        String url,
        OffsetDateTime updatedAt,
        Integer width,
        Integer height,
        Long sizeBytes
) {
    public static ProfileImageDto empty() {
        return new ProfileImageDto(false, null, null, null, null, null);
    }
}
