package com.researchassistant.datacollection.controller;

import com.researchassistant.datacollection.dto.DataCollectionDtos.*;
import com.researchassistant.datacollection.service.DataCollectionSessionService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DataCollectionSessionController {
    private final DataCollectionSessionService service;
    private final AuthenticatedUserResolver userResolver;

    public DataCollectionSessionController(DataCollectionSessionService service, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/data-collection-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateSessionRequest request) {
        return service.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/data-collection-sessions")
    public PageResponse<SessionResponse> list(Authentication authentication, @PathVariable UUID projectId,
                                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.list(projectId, user(authentication), PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))));
    }

    @GetMapping("/data-collection-sessions/{sessionId}")
    public SessionResponse get(Authentication authentication, @PathVariable UUID sessionId) {
        return service.get(sessionId, user(authentication));
    }

    @PostMapping("/data-collection-sessions/{sessionId}/start")
    public SessionResponse start(Authentication authentication, @PathVariable UUID sessionId) {
        return service.start(sessionId, user(authentication));
    }

    @PostMapping("/data-collection-sessions/{sessionId}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public void recordResponse(Authentication authentication, @PathVariable UUID sessionId, @Valid @RequestBody RecordResponseRequest request) {
        service.recordResponse(sessionId, user(authentication), request);
    }

    @PostMapping("/data-collection-sessions/{sessionId}/complete")
    public SessionResponse complete(Authentication authentication, @PathVariable UUID sessionId) {
        return service.complete(sessionId, user(authentication));
    }

    @PostMapping("/data-collection-sessions/{sessionId}/invalidate")
    public SessionResponse invalidate(Authentication authentication, @PathVariable UUID sessionId) {
        return service.invalidate(sessionId, user(authentication));
    }

    private User user(Authentication authentication) { return userResolver.requireActiveUser(authentication); }
}
