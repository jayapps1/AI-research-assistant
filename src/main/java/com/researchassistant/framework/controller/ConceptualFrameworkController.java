package com.researchassistant.framework.controller;

import com.researchassistant.framework.dto.*;
import com.researchassistant.framework.service.ConceptualFrameworkService;
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
public class ConceptualFrameworkController {
    private final ConceptualFrameworkService service;
    private final AuthenticatedUserResolver userResolver;

    public ConceptualFrameworkController(ConceptualFrameworkService service, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/conceptual-frameworks")
    @ResponseStatus(HttpStatus.CREATED)
    public FrameworkResponse create(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateConceptualFrameworkRequest request) {
        return service.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/conceptual-frameworks")
    public List<FrameworkResponse> list(Authentication authentication, @PathVariable UUID projectId) {
        return service.list(projectId, user(authentication));
    }

    @GetMapping("/conceptual-frameworks/{frameworkId}")
    public FrameworkResponse get(Authentication authentication, @PathVariable UUID frameworkId) {
        return service.get(frameworkId, user(authentication));
    }

    @PatchMapping("/conceptual-frameworks/{frameworkId}")
    public FrameworkResponse update(Authentication authentication, @PathVariable UUID frameworkId, @Valid @RequestBody UpdateConceptualFrameworkRequest request) {
        return service.update(frameworkId, user(authentication), request);
    }

    @PostMapping("/conceptual-frameworks/{frameworkId}/activate")
    public FrameworkResponse activate(Authentication authentication, @PathVariable UUID frameworkId) {
        return service.activate(frameworkId, user(authentication));
    }

    @PostMapping("/conceptual-frameworks/{frameworkId}/variables")
    @ResponseStatus(HttpStatus.CREATED)
    public ConceptualVariableResponse addVariable(Authentication authentication, @PathVariable UUID frameworkId, @Valid @RequestBody CreateConceptualVariableRequest request) {
        return service.addVariable(frameworkId, user(authentication), request);
    }

    @PatchMapping("/conceptual-variables/{variableId}")
    public ConceptualVariableResponse updateVariable(Authentication authentication, @PathVariable UUID variableId, @Valid @RequestBody CreateConceptualVariableRequest request) {
        return service.updateVariable(variableId, user(authentication), request);
    }

    @PostMapping("/conceptual-frameworks/{frameworkId}/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    public ConceptualRelationshipResponse addRelationship(Authentication authentication, @PathVariable UUID frameworkId, @Valid @RequestBody CreateConceptualRelationshipRequest request) {
        return service.addRelationship(frameworkId, user(authentication), request);
    }

    @PatchMapping("/conceptual-relationships/{relationshipId}")
    public ConceptualRelationshipResponse updateRelationship(Authentication authentication, @PathVariable UUID relationshipId, @Valid @RequestBody CreateConceptualRelationshipRequest request) {
        return service.updateRelationship(relationshipId, user(authentication), request);
    }

    @PostMapping("/projects/{projectId}/conceptual-frameworks/generate")
    public Object generate(Authentication authentication, @PathVariable UUID projectId) {
        return service.generate(projectId, user(authentication));
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
