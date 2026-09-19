package com.researchassistant.subscription.dto;

import com.researchassistant.subscription.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminPlanDtos {

    private AdminPlanDtos() {}

    public enum LimitMode {
        LIMITED,
        UNLIMITED,
        DISABLED
    }

    public record AdminSubscriptionPlanResponse(
            UUID id,
            String code,
            String name,
            String description,
            SubscriptionPlanStatus status,
            BillingInterval billingInterval,
            BigDecimal price,
            BigDecimal yearlyPrice,
            String currency,
            boolean publiclyAvailable,
            boolean featured,
            int displayOrder,
            long workspacesSubscribed,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record CreateSubscriptionPlanRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            SubscriptionPlanStatus status,
            @NotNull BillingInterval billingInterval,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @DecimalMin("0.00") BigDecimal yearlyPrice,
            @NotBlank String currency,
            Boolean publiclyAvailable,
            Boolean featured,
            Integer displayOrder
    ) {}

    public record UpdateSubscriptionPlanRequest(
            String name,
            String description,
            SubscriptionPlanStatus status,
            BillingInterval billingInterval,
            @DecimalMin("0.00") BigDecimal price,
            @DecimalMin("0.00") BigDecimal yearlyPrice,
            String currency,
            Boolean publiclyAvailable,
            Boolean featured,
            Integer displayOrder
    ) {}

    public record AdminPlanEntitlementDto(
            PlanFeature feature,
            boolean enabled,
            LimitMode limitMode,
            Long limitValue,
            LimitUnit limitUnit,
            String formattedValue
    ) {}

    public record UpdateEntitlementsRequest(
            @NotNull List<AdminPlanEntitlementDto> entitlements
    ) {}
}
