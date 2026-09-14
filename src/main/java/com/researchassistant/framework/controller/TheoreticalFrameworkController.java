package com.researchassistant.framework.controller;

import com.researchassistant.framework.dto.*;
import com.researchassistant.framework.service.TheoreticalFrameworkService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TheoreticalFrameworkController {
    private final TheoreticalFrameworkService service;
    private final AuthenticatedUserResolver userResolver;

    public TheoreticalFrameworkController(TheoreticalFrameworkService service, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/theoretical-frameworks")
    @ResponseStatus(HttpStatus.CREATED)
    public FrameworkResponse create(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateTheoreticalFrameworkRequest request) {
        return service.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/theoretical-frameworks")
    public List<FrameworkResponse> list(Authentication authentication, @PathVariable UUID projectId) {
        return service.list(projectId, user(authentication));
    }

    @GetMapping("/theoretical-frameworks/{frameworkId}")
    public FrameworkResponse get(Authentication authentication, @PathVariable UUID frameworkId) {
        return service.get(frameworkId, user(authentication));
    }

    @PatchMapping("/theoretical-frameworks/{frameworkId}")
    public FrameworkResponse update(Authentication authentication, @PathVariable UUID frameworkId, @Valid @RequestBody CreateTheoreticalFrameworkRequest request) {
        return service.update(frameworkId, user(authentication), request);
    }

    @PostMapping("/theoretical-frameworks/{frameworkId}/activate")
    public FrameworkResponse activate(Authentication authentication, @PathVariable UUID frameworkId) {
        return service.activate(frameworkId, user(authentication));
    }

    @PostMapping("/theoretical-frameworks/{frameworkId}/theories")
    @ResponseStatus(HttpStatus.CREATED)
    public TheoryResponse addTheory(Authentication authentication, @PathVariable UUID frameworkId, @Valid @RequestBody CreateTheoryRequest request) {
        return service.addTheory(frameworkId, user(authentication), request);
    }

    @PatchMapping("/theoretical-framework-theories/{theoryId}")
    public TheoryResponse updateTheory(Authentication authentication, @PathVariable UUID theoryId, @Valid @RequestBody CreateTheoryRequest request) {
        return service.updateTheory(theoryId, user(authentication), request);
    }

    @PostMapping("/projects/{projectId}/theoretical-frameworks/generate")
    public Object generate(Authentication authentication, @PathVariable UUID projectId) {
        return service.generate(projectId, user(authentication));
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
