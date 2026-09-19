package com.researchassistant.publicsite.service;

import com.researchassistant.cache.AppCacheNames;
import com.researchassistant.publicsite.dto.PublicPricingPlanResponse;
import com.researchassistant.subscription.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PublicPricingService {

    private final SubscriptionPlanRepository planRepository;
    private final PlanEntitlementRepository entitlementRepository;
    private final SubscriptionPlanPriceRepository planPriceRepository;

    public PublicPricingService(SubscriptionPlanRepository planRepository,
                                PlanEntitlementRepository entitlementRepository,
                                SubscriptionPlanPriceRepository planPriceRepository) {
        this.planRepository = planRepository;
        this.entitlementRepository = entitlementRepository;
        this.planPriceRepository = planPriceRepository;
    }

    @Cacheable(AppCacheNames.PUBLIC_PRICING)
    public List<PublicPricingPlanResponse> getPublicPricing() {
        List<SubscriptionPlan> activePublicPlans =
                planRepository.findByStatusAndPubliclyAvailableTrueOrderByDisplayOrderAsc(SubscriptionPlanStatus.ACTIVE);

        return activePublicPlans.stream()
                .map(this::toPublicPricingPlanResponse)
                .toList();
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_PRICING, allEntries = true)
    public void evictPricingCache() {
        // Cache evicted via annotation
    }

    private PublicPricingPlanResponse toPublicPricingPlanResponse(SubscriptionPlan plan) {
        List<PlanEntitlement> entitlements = entitlementRepository.findAllByPlanId(plan.getId());
        List<String> featureSummaries = new ArrayList<>();

        for (PlanEntitlement e : entitlements) {
            if (!e.isEnabled()) continue;
            String featureText = formatEntitlementSummary(e);
            if (featureText != null && !featureSummaries.contains(featureText)) {
                featureSummaries.add(featureText);
            }
        }

        // Add standard highlights based on plan code if entitlements are sparse
        if (featureSummaries.isEmpty()) {
            featureSummaries = defaultFeatureHighlights(plan.getCode());
        }

        List<SubscriptionPlanPrice> prices = planPriceRepository.findAllByPlanIdAndActiveTrue(plan.getId());
        BigDecimal monthlyPrice = prices.stream()
                .filter(p -> p.getBillingInterval() == BillingInterval.MONTHLY)
                .findFirst()
                .map(SubscriptionPlanPrice::getPrice)
                .orElse(plan.getPrice());

        BigDecimal annualPrice = prices.stream()
                .filter(p -> p.getBillingInterval() == BillingInterval.YEARLY)
                .findFirst()
                .map(SubscriptionPlanPrice::getPrice)
                .orElse(null);

        boolean isFree = monthlyPrice.compareTo(BigDecimal.ZERO) == 0;
        boolean featured = "PRO".equalsIgnoreCase(plan.getCode());

        String ctaLabel = isFree ? "Get Started Free" : "Choose " + plan.getName();
        String ctaUrl = "/auth/register?selectedPlan=" + plan.getCode();

        return new PublicPricingPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                monthlyPrice,
                monthlyPrice,
                annualPrice,
                plan.getCurrency(),
                plan.getBillingInterval(),
                featured,
                plan.getDisplayOrder(),
                featureSummaries,
                ctaLabel,
                ctaUrl
        );
    }

    private String formatEntitlementSummary(PlanEntitlement entitlement) {
        PlanFeature feature = entitlement.getFeature();
        Long limit = entitlement.getLimitValue();
        LimitUnit unit = entitlement.getLimitUnit();

        return switch (feature) {
            case AI_GENERATION -> limit != null && limit > 0
                    ? String.format("%,d AI Generation Requests / month", limit)
                    : "AI Generation Support";
            case AI_TOKENS -> limit != null && limit > 0
                    ? String.format("%,d AI Analysis Tokens / month", limit)
                    : "Grounded AI Tokens Included";
            case PROJECT_CREATION -> limit != null && limit > 0
                    ? String.format("Up to %d Active Research Projects", limit)
                    : "Research Projects";
            case COLLABORATORS_PER_PROJECT -> limit != null && limit > 0
                    ? String.format("Up to %d Collaborators per Project", limit)
                    : "Team Collaboration";
            case DOCUMENT_UPLOAD -> limit != null && limit > 0
                    ? String.format("Up to %d Uploaded Documents", limit)
                    : "Document Uploads";
            case STORAGE -> limit != null && limit > 0
                    ? String.format("%.0f GB Document & Evidence Storage", limit / (1024.0 * 1024.0 * 1024.0))
                    : "Secure Cloud Storage";
            case REPORT_EXPORT_DOCX -> "Microsoft Word (.docx) Export";
            case REPORT_EXPORT_PDF -> "Standard PDF Report Export";
            default -> null;
        };
    }

    private List<String> defaultFeatureHighlights(String planCode) {
        return switch (planCode.toUpperCase()) {
            case "FREE" -> List.of(
                    "Source-grounded AI search",
                    "Up to 5 Research Projects",
                    "Basic Literature Review tools",
                    "DOCX Report Export",
                    "Standard Academic Integrity Check"
            );
            case "STUDENT" -> List.of(
                    "Everything in Free",
                    "Up to 20 Research Projects",
                    "10 Collaborators per Project",
                    "Full Literature & Methodology Workbench",
                    "Quantitative & Thematic Analysis",
                    "DOCX & PDF Report Exports"
            );
            case "PRO" -> List.of(
                    "Everything in Student",
                    "Unlimited Research Projects",
                    "Expanded AI Generation Quotas",
                    "Institutional Citation Management",
                    "Advanced Synthesis & Verification",
                    "Priority Processing & Support"
            );
            default -> List.of(
                    "Full platform feature access",
                    "Collaborative research workbench",
                    "Grounded citation verification"
            );
        };
    }
}
