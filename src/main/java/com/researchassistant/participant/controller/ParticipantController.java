package com.researchassistant.participant.controller;

import com.researchassistant.ethics.dto.EthicsDtos.ParticipantConsentResponse;
import com.researchassistant.ethics.service.EthicsService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.participant.dto.ParticipantDtos.*;
import com.researchassistant.participant.service.ParticipantService;
import com.researchassistant.project.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ParticipantController {
    private final ParticipantService participantService;
    private final EthicsService ethicsService;
    private final AuthenticatedUserResolver userResolver;

    public ParticipantController(ParticipantService participantService, EthicsService ethicsService, AuthenticatedUserResolver userResolver) {
        this.participantService = participantService;
        this.ethicsService = ethicsService;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse create(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateParticipantRequest request) {
        return participantService.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/participants")
    public PageResponse<ParticipantResponse> list(Authentication authentication, @PathVariable UUID projectId,
                                                   @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(participantService.list(projectId, user(authentication), PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))));
    }

    @GetMapping("/participants/{participantId}")
    public ParticipantResponse get(Authentication authentication, @PathVariable UUID participantId) {
        return participantService.get(participantId, user(authentication));
    }

    @PatchMapping("/participants/{participantId}")
    public ParticipantResponse update(Authentication authentication, @PathVariable UUID participantId, @Valid @RequestBody UpdateParticipantRequest request) {
        return participantService.update(participantId, user(authentication), request);
    }

    @PostMapping("/participants/{participantId}/withdraw")
    public ParticipantResponse withdraw(Authentication authentication, @PathVariable UUID participantId) {
        ethicsService.recordWithdrawal(participantId, user(authentication));
        return participantService.withdraw(participantId, user(authentication));
    }

    @PostMapping("/participants/{participantId}/eligibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eligibility(Authentication authentication, @PathVariable UUID participantId, @Valid @RequestBody EligibilityRequest request) {
        participantService.assessEligibility(participantId, user(authentication), request);
    }

    @GetMapping("/participants/{participantId}/consents")
    public List<ParticipantConsentResponse> consents(Authentication authentication, @PathVariable UUID participantId) {
        return ethicsService.participantConsents(participantId, user(authentication));
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
