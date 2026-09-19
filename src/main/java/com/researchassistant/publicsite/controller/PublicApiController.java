package com.researchassistant.publicsite.controller;

import com.researchassistant.publicsite.dto.*;
import com.researchassistant.publicsite.service.ContactSubmissionService;
import com.researchassistant.publicsite.service.PublicContentService;
import com.researchassistant.publicsite.service.PublicPricingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
public class PublicApiController {

    private final PublicContentService contentService;
    private final PublicPricingService pricingService;
    private final ContactSubmissionService contactSubmissionService;
    private final com.researchassistant.publicsite.service.PublicStatisticsService statisticsService;

    public PublicApiController(PublicContentService contentService,
                               PublicPricingService pricingService,
                               ContactSubmissionService contactSubmissionService,
                               com.researchassistant.publicsite.service.PublicStatisticsService statisticsService) {
        this.contentService = contentService;
        this.pricingService = pricingService;
        this.contactSubmissionService = contactSubmissionService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public List<PublicStatisticResponse> getStatistics() {
        return statisticsService.getPublicStatistics();
    }

    @GetMapping("/site")
    public PublicSiteSettingsResponse getSiteSettings() {
        return contentService.getPublicSiteSettings();
    }

    @GetMapping("/pages/navigation")
    public List<PublicPageSummaryResponse> getNavigationPages() {
        return contentService.getPublishedNavigationPages();
    }

    @GetMapping("/pages/{slug}")
    public PublicPageResponse getPageBySlug(@PathVariable String slug) {
        return contentService.getPublishedPageBySlug(slug);
    }

    @GetMapping("/services")
    public List<ServiceOfferingResponse> getServices() {
        return contentService.getActiveServices();
    }

    @GetMapping("/faqs")
    public List<FaqItemResponse> getFaqs() {
        return contentService.getPublishedFaqs();
    }

    @GetMapping("/pricing")
    public List<PublicPricingPlanResponse> getPricing() {
        return pricingService.getPublicPricing();
    }

    @PostMapping("/contact")
    public ContactSubmissionResultResponse submitContact(
            @Valid @RequestBody ContactSubmissionRequest request,
            HttpServletRequest servletRequest) {

        String rawIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        return contactSubmissionService.submitContactForm(request, rawIp, userAgent);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
