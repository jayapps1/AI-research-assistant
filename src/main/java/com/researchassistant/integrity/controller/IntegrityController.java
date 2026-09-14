package com.researchassistant.integrity.controller;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.integrity.dto.IntegrityDtos.*;
import com.researchassistant.integrity.service.IntegrityReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class IntegrityController {
    private final IntegrityReviewService service;
    private final AuthenticatedUserResolver userResolver;

    public IntegrityController(IntegrityReviewService service, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/similarity-checks")
    public SimilarityResponse runSimilarity(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody SimilarityRequest request) {
        return service.runSimilarity(projectId, user(authentication), request);
    }

    @GetMapping("/similarity-checks/{checkId}")
    public SimilarityResponse getSimilarity(Authentication authentication, @PathVariable UUID checkId) {
        return service.getSimilarity(checkId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/writing-review")
    public WritingReviewResponse writingReview(Authentication authentication, @PathVariable UUID reportId) {
        return service.writingReview(reportId, user(authentication));
    }

    @GetMapping("/writing-reviews/{reviewId}")
    public WritingReviewResponse getWritingReview(Authentication authentication, @PathVariable UUID reviewId) {
        return service.getWritingReview(reviewId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/integrity-review")
    public IntegrityReviewResponse integrityReview(Authentication authentication, @PathVariable UUID reportId) {
        return service.integrityReview(reportId, user(authentication));
    }

    @GetMapping("/integrity-reviews/{reviewId}")
    public IntegrityReviewResponse getIntegrityReview(Authentication authentication, @PathVariable UUID reviewId) {
        return service.getIntegrityReview(reviewId, user(authentication));
    }

    @GetMapping("/projects/{projectId}/ai-usage-summary")
    public AiUsageSummary aiUsage(Authentication authentication, @PathVariable UUID projectId) {
        return service.aiUsage(projectId, user(authentication));
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
