package com.researchassistant.methodology.controller;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.methodology.dto.MethodologyRequests.*;
import com.researchassistant.methodology.dto.MethodologyResponses.*;
import com.researchassistant.methodology.service.MethodologyService;
import com.researchassistant.methodology.service.ResearchDesignValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MethodologyController {
    private final MethodologyService service;
    private final ResearchDesignValidationService validationService;
    private final AuthenticatedUserResolver userResolver;

    public MethodologyController(MethodologyService service, ResearchDesignValidationService validationService, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.validationService = validationService;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/methodologies")
    @ResponseStatus(HttpStatus.CREATED)
    public MethodologyResponse create(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateMethodologyRequest request) {
        return service.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/methodologies")
    public List<MethodologyResponse> list(Authentication authentication, @PathVariable UUID projectId) {
        return service.list(projectId, user(authentication));
    }

    @GetMapping("/projects/{projectId}/methodologies/history")
    public List<MethodologyResponse> history(Authentication authentication, @PathVariable UUID projectId) {
        return service.list(projectId, user(authentication));
    }

    @GetMapping("/methodologies/{methodologyId}")
    public MethodologyResponse get(Authentication authentication, @PathVariable UUID methodologyId) {
        return service.get(methodologyId, user(authentication));
    }

    @PatchMapping("/methodologies/{methodologyId}")
    public MethodologyResponse update(Authentication authentication, @PathVariable UUID methodologyId, @Valid @RequestBody CreateMethodologyRequest request) {
        return service.update(methodologyId, user(authentication), request);
    }

    @PostMapping("/methodologies/{methodologyId}/activate")
    public MethodologyResponse activate(Authentication authentication, @PathVariable UUID methodologyId) {
        return service.activate(methodologyId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/methodologies/generate")
    public Object generateMethodology(Authentication authentication, @PathVariable UUID projectId) {
        return service.generateMethodology(projectId, user(authentication));
    }

    @PostMapping("/methodologies/{methodologyId}/population")
    @ResponseStatus(HttpStatus.CREATED)
    public PopulationResponse createPopulation(Authentication authentication, @PathVariable UUID methodologyId, @Valid @RequestBody CreatePopulationRequest request) {
        return service.createPopulation(methodologyId, user(authentication), request);
    }

    @PatchMapping("/study-populations/{populationId}")
    public PopulationResponse updatePopulation(Authentication authentication, @PathVariable UUID populationId, @Valid @RequestBody CreatePopulationRequest request) {
        return service.updatePopulation(populationId, user(authentication), request);
    }

    @PostMapping("/methodologies/{methodologyId}/sampling-plan")
    @ResponseStatus(HttpStatus.CREATED)
    public SamplingPlanResponse createSamplingPlan(Authentication authentication, @PathVariable UUID methodologyId, @Valid @RequestBody CreateSamplingPlanRequest request) {
        return service.createSamplingPlan(methodologyId, user(authentication), request);
    }

    @PatchMapping("/sampling-plans/{samplingPlanId}")
    public SamplingPlanResponse updateSamplingPlan(Authentication authentication, @PathVariable UUID samplingPlanId, @Valid @RequestBody CreateSamplingPlanRequest request) {
        return service.updateSamplingPlan(samplingPlanId, user(authentication), request);
    }

    @PostMapping("/sampling-plans/{samplingPlanId}/sample-size/calculate")
    public SampleSizeCalculationResponse calculate(Authentication authentication, @PathVariable UUID samplingPlanId, @Valid @RequestBody CalculateSampleSizeRequest request) {
        return service.calculate(samplingPlanId, user(authentication), request);
    }

    @GetMapping("/sampling-plans/{samplingPlanId}/sample-size-calculations")
    public List<SampleSizeCalculationResponse> calculations(Authentication authentication, @PathVariable UUID samplingPlanId) {
        return service.calculations(samplingPlanId, user(authentication));
    }

    @PostMapping("/methodologies/{methodologyId}/data-collection-methods")
    @ResponseStatus(HttpStatus.CREATED)
    public DataCollectionMethodResponse createMethod(Authentication authentication, @PathVariable UUID methodologyId, @Valid @RequestBody CreateDataCollectionMethodRequest request) {
        return service.createDataCollectionMethod(methodologyId, user(authentication), request);
    }

    @GetMapping("/methodologies/{methodologyId}/data-collection-methods")
    public List<DataCollectionMethodResponse> methods(Authentication authentication, @PathVariable UUID methodologyId) {
        return service.dataCollectionMethods(methodologyId, user(authentication));
    }

    @GetMapping("/data-collection-methods/{methodId}")
    public DataCollectionMethodResponse method(Authentication authentication, @PathVariable UUID methodId) {
        return service.dataCollectionMethod(methodId, user(authentication));
    }

    @PatchMapping("/data-collection-methods/{methodId}")
    public DataCollectionMethodResponse updateMethod(Authentication authentication, @PathVariable UUID methodId, @Valid @RequestBody CreateDataCollectionMethodRequest request) {
        return service.updateDataCollectionMethod(methodId, user(authentication), request);
    }

    @PostMapping("/methodologies/{methodologyId}/data-collection-methods/reorder")
    public List<DataCollectionMethodResponse> reorder(Authentication authentication, @PathVariable UUID methodologyId) {
        return service.dataCollectionMethods(methodologyId, user(authentication));
    }

    @PostMapping("/methodologies/{methodologyId}/data-collection-methods/generate")
    public Object generateDataCollection(Authentication authentication, @PathVariable UUID methodologyId) {
        return service.generateDataCollection(methodologyId, user(authentication));
    }

    @GetMapping("/projects/{projectId}/research-design/validate")
    public ResearchDesignValidationResult validate(Authentication authentication, @PathVariable UUID projectId) {
        return validationService.validate(projectId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/research-design/review")
    public ResearchDesignValidationResult review(Authentication authentication, @PathVariable UUID projectId) {
        return validationService.validate(projectId, user(authentication));
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
