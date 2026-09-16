package com.researchassistant.publicsite.dto;

import com.researchassistant.subscription.BillingInterval;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicPricingPlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        BillingInterval billingInterval,
        boolean featured,
        int displayOrder,
        List<String> features,
        String ctaLabel,
        String ctaUrl
) {}
