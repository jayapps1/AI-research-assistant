package com.researchassistant.billing.controller;

import com.researchassistant.admin.SystemAdminAuthorizationService;
import com.researchassistant.billing.BillingPaymentIntent;
import com.researchassistant.billing.BillingPaymentIntentRepository;
import com.researchassistant.billing.PaymentAttempt;
import com.researchassistant.billing.PaymentAttemptRepository;
import com.researchassistant.billing.dto.AdminPaymentDtos.AdminPaymentAttemptDetail;
import com.researchassistant.billing.dto.AdminPaymentDtos.AdminPaymentIntentResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/payments")
public class AdminPaymentController {

    private final AuthenticatedUserResolver userResolver;
    private final SystemAdminAuthorizationService adminAuthorizationService;
    private final BillingPaymentIntentRepository intentRepository;
    private final PaymentAttemptRepository attemptRepository;

    public AdminPaymentController(
            AuthenticatedUserResolver userResolver,
            SystemAdminAuthorizationService adminAuthorizationService,
            BillingPaymentIntentRepository intentRepository,
            PaymentAttemptRepository attemptRepository
    ) {
        this.userResolver = userResolver;
        this.adminAuthorizationService = adminAuthorizationService;
        this.intentRepository = intentRepository;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<AdminPaymentIntentResponse> listPayments(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        Page<BillingPaymentIntent> intents = intentRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AdminPaymentIntentResponse> content = intents.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, intents.getTotalElements());
    }

    private User requireAdmin(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return user;
    }

    private AdminPaymentIntentResponse toResponse(BillingPaymentIntent intent) {
        List<PaymentAttempt> attempts = attemptRepository.findAllByPaymentIntentIdOrderByAttemptNumberAsc(intent.getId());

        List<AdminPaymentAttemptDetail> attemptDetails = attempts.stream()
                .map(a -> new AdminPaymentAttemptDetail(
                        a.getId(),
                        a.getAttemptNumber(),
                        a.getInternalReference(),
                        a.getProviderReference(),
                        a.getStatus() != null ? a.getStatus().name() : null,
                        a.getExpectedAmount(),
                        a.getCurrency(),
                        a.getProviderStatus(),
                        a.getFailureCode(),
                        a.getFailureMessageSafe(),
                        a.getAuthorizationUrl(),
                        a.getCreatedAt(),
                        a.getCompletedAt(),
                        a.getProviderVerifiedAt()
                ))
                .toList();

        PaymentAttempt latest = attempts.isEmpty() ? null : attempts.getLast();

        String userName = intent.getInitiatedBy() != null
                ? String.format("%s %s",
                intent.getInitiatedBy().getFirstName() != null ? intent.getInitiatedBy().getFirstName() : "",
                intent.getInitiatedBy().getLastName() != null ? intent.getInitiatedBy().getLastName() : "").trim()
                : "Unknown";

        if (userName.isBlank() && intent.getInitiatedBy() != null) {
            userName = intent.getInitiatedBy().getEmail();
        }

        String env = latest != null && latest.getEnvironment() != null ? latest.getEnvironment().name() : "TEST";
        OffsetDateTime verifiedAt = latest != null ? latest.getProviderVerifiedAt() : null;

        return new AdminPaymentIntentResponse(
                intent.getId(),
                intent.getWorkspace().getId(),
                intent.getWorkspace().getName(),
                intent.getInitiatedBy().getId(),
                intent.getInitiatedBy().getEmail(),
                userName,
                intent.getPlan().getCode(),
                intent.getPlan().getName(),
                intent.getBillingInterval() != null ? intent.getBillingInterval().name() : null,
                intent.getExpectedAmount(),
                intent.getCurrency(),
                env,
                intent.getStatus() != null ? intent.getStatus().name() : null,
                attempts.size(),
                latest != null && latest.getStatus() != null ? latest.getStatus().name() : "NONE",
                latest != null ? latest.getInternalReference() : null,
                latest != null ? latest.getProviderReference() : null,
                latest != null ? latest.getProviderStatus() : null,
                latest != null ? latest.getFailureCode() : null,
                latest != null ? latest.getFailureMessageSafe() : null,
                intent.getCreatedAt(),
                intent.getSettledAt(),
                verifiedAt,
                attemptDetails
        );
    }
}
