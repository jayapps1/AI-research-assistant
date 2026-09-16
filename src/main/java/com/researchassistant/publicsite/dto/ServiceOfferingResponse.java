package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.ServiceOffering;
import com.researchassistant.publicsite.entity.ServiceOfferingStatus;

import java.util.UUID;

public record ServiceOfferingResponse(
        UUID id,
        String code,
        String name,
        String shortDescription,
        String description,
        String iconKey,
        String featureListJson,
        String ctaLabel,
        String ctaUrl,
        boolean featured,
        ServiceOfferingStatus status,
        int displayOrder
) {
    public static ServiceOfferingResponse fromEntity(ServiceOffering service) {
        return new ServiceOfferingResponse(
                service.getId(),
                service.getCode(),
                service.getName(),
                service.getShortDescription(),
                service.getDescription(),
                service.getIconKey(),
                service.getFeatureListJson(),
                service.getCtaLabel(),
                service.getCtaUrl(),
                service.isFeatured(),
                service.getStatus(),
                service.getDisplayOrder()
        );
    }
}
