package com.researchassistant.publicsite.dto;

public record PublicStatisticResponse(
        String code,
        String label,
        String value,
        String prefix,
        String suffix,
        String iconKey,
        boolean featured
) {
}
