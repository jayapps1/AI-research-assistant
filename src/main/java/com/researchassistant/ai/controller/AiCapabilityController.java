package com.researchassistant.ai.controller;

import com.researchassistant.ai.dto.AiCapabilityResponse;
import com.researchassistant.ai.provider.AiCapabilityService;
import com.researchassistant.identity.service.AuthenticatedUserResolver;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiCapabilityController {

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final AiCapabilityService capabilityService;

    public AiCapabilityController(
            AuthenticatedUserResolver authenticatedUserResolver,
            AiCapabilityService capabilityService
    ) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.capabilityService = capabilityService;
    }

    @GetMapping("/capabilities")
    public AiCapabilityResponse capabilities(Authentication authentication) {
        authenticatedUserResolver.requireActiveUser(authentication);
        return capabilityService.capabilities();
    }
}
