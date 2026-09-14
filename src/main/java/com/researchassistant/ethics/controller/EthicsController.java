package com.researchassistant.ethics.controller;

import com.researchassistant.ethics.dto.EthicsDtos.*;
import com.researchassistant.ethics.service.EthicsReadinessService;
import com.researchassistant.ethics.service.EthicsService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EthicsController {
    private final EthicsService ethicsService;
    private final EthicsReadinessService readinessService;
    private final AuthenticatedUserResolver userResolver;

    public EthicsController(EthicsService ethicsService, EthicsReadinessService readinessService, AuthenticatedUserResolver userResolver) {
        this.ethicsService = ethicsService;
        this.readinessService = readinessService;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/ethics-protocols")
    @ResponseStatus(HttpStatus.CREATED)
    public EthicsProtocolResponse createProtocol(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateEthicsProtocolRequest request) {
        return ethicsService.createProtocol(projectId, user(authentication), request);
    }

    @PostMapping("/ethics-protocols/{protocolId}/approvals")
    @ResponseStatus(HttpStatus.CREATED)
    public EthicsApprovalResponse recordApproval(Authentication authentication, @PathVariable UUID protocolId, @Valid @RequestBody RecordEthicsApprovalRequest request) {
        return ethicsService.recordApproval(protocolId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/ethics-readiness")
    public EthicsReadinessResponse readiness(@PathVariable UUID projectId) {
        return readinessService.evaluate(projectId);
    }

    @PostMapping("/projects/{projectId}/consent-forms")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentFormResponse createConsentForm(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateConsentFormRequest request) {
        return ethicsService.createConsentForm(projectId, user(authentication), request);
    }

    @PostMapping("/consent-forms/{formId}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentRevisionResponse createRevision(Authentication authentication, @PathVariable UUID formId, @Valid @RequestBody CreateConsentRevisionRequest request) {
        return ethicsService.createRevision(formId, user(authentication), request);
    }

    @PostMapping("/consent-forms/{formId}/activate")
    public ConsentFormResponse activate(Authentication authentication, @PathVariable UUID formId) {
        return ethicsService.activateConsentForm(formId, user(authentication));
    }

    @PostMapping("/participants/{participantId}/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantConsentResponse recordConsent(Authentication authentication, @PathVariable UUID participantId, @Valid @RequestBody RecordParticipantConsentRequest request) {
        return ethicsService.recordConsent(participantId, user(authentication), request);
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
