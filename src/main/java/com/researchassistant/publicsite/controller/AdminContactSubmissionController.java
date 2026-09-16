package com.researchassistant.publicsite.controller;

import com.researchassistant.admin.SystemAdminAuthorizationService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.publicsite.dto.*;
import com.researchassistant.publicsite.entity.ContactSubmissionStatus;
import com.researchassistant.publicsite.service.ContactSubmissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/contact-submissions")
public class AdminContactSubmissionController {

    private final AuthenticatedUserResolver userResolver;
    private final SystemAdminAuthorizationService adminAuthorizationService;
    private final ContactSubmissionService contactSubmissionService;

    public AdminContactSubmissionController(AuthenticatedUserResolver userResolver,
                                            SystemAdminAuthorizationService adminAuthorizationService,
                                            ContactSubmissionService contactSubmissionService) {
        this.userResolver = userResolver;
        this.adminAuthorizationService = adminAuthorizationService;
        this.contactSubmissionService = contactSubmissionService;
    }

    private User requireAdmin(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return user;
    }

    @GetMapping
    public Page<AdminContactSubmissionResponse> getSubmissions(
            @RequestParam(required = false) ContactSubmissionStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        requireAdmin(authentication);
        return contactSubmissionService.searchSubmissions(status, search, pageable);
    }

    @GetMapping("/{id}")
    public AdminContactSubmissionResponse getSubmissionDetail(
            @PathVariable UUID id,
            Authentication authentication) {
        requireAdmin(authentication);
        return contactSubmissionService.getSubmissionDetail(id);
    }

    @PatchMapping("/{id}/status")
    public AdminContactSubmissionResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContactStatusRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contactSubmissionService.updateStatus(id, request.status(), admin);
    }

    @PatchMapping("/{id}/assign")
    public AdminContactSubmissionResponse assignSubmission(
            @PathVariable UUID id,
            @RequestBody AssignContactRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contactSubmissionService.assignSubmission(id, request.assignedToUserId(), admin);
    }

    @PostMapping("/{id}/respond")
    public AdminContactResponseDto respondToSubmission(
            @PathVariable UUID id,
            @Valid @RequestBody CreateContactReplyRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contactSubmissionService.respondToSubmission(id, admin, request);
    }
}
