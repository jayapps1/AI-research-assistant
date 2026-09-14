package com.researchassistant.framework.service;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.framework.dto.*;
import com.researchassistant.framework.entity.*;
import com.researchassistant.framework.repository.*;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.exception.RagCapabilityUnavailableException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TheoreticalFrameworkService {
    private final ProjectAuthorizationService authorizationService;
    private final TheoreticalFrameworkRepository frameworkRepository;
    private final TheoreticalFrameworkTheoryRepository theoryRepository;

    public TheoreticalFrameworkService(ProjectAuthorizationService authorizationService, TheoreticalFrameworkRepository frameworkRepository, TheoreticalFrameworkTheoryRepository theoryRepository) {
        this.authorizationService = authorizationService;
        this.frameworkRepository = frameworkRepository;
        this.theoryRepository = theoryRepository;
    }

    @Transactional
    public FrameworkResponse create(UUID projectId, User user, CreateTheoreticalFrameworkRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, user);
        TheoreticalFramework framework = new TheoreticalFramework();
        framework.setProject(context.project());
        framework.setTitle(request.title().trim());
        framework.setOverview(request.overview());
        framework.setOrigin(origin(request.origin()));
        framework.setCreatedBy(user);
        return response(frameworkRepository.save(framework));
    }

    @Transactional(readOnly = true)
    public List<FrameworkResponse> list(UUID projectId, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        return frameworkRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public FrameworkResponse get(UUID id, User user) {
        TheoreticalFramework framework = framework(id);
        authorizationService.requireProjectViewer(framework.getProject().getId(), user);
        return response(framework);
    }

    @Transactional
    public FrameworkResponse update(UUID id, User user, CreateTheoreticalFrameworkRequest request) {
        TheoreticalFramework framework = framework(id);
        authorizationService.requireProjectEditor(framework.getProject().getId(), user);
        if (request.title() != null && !request.title().isBlank()) framework.setTitle(request.title().trim());
        if (request.overview() != null) framework.setOverview(request.overview());
        if (request.origin() != null) framework.setOrigin(request.origin());
        return response(framework);
    }

    @Transactional
    public FrameworkResponse activate(UUID id, User user) {
        TheoreticalFramework framework = framework(id);
        authorizationService.requireProjectEditor(framework.getProject().getId(), user);
        frameworkRepository.findByProjectIdAndStatus(framework.getProject().getId(), TheoreticalFrameworkStatus.ACTIVE)
                .filter(active -> !active.getId().equals(framework.getId()))
                .ifPresent(active -> active.setStatus(TheoreticalFrameworkStatus.SUPERSEDED));
        framework.setStatus(TheoreticalFrameworkStatus.ACTIVE);
        return response(framework);
    }

    @Transactional
    public TheoryResponse addTheory(UUID frameworkId, User user, CreateTheoryRequest request) {
        TheoreticalFramework framework = framework(frameworkId);
        authorizationService.requireProjectEditor(framework.getProject().getId(), user);
        TheoreticalFrameworkTheory theory = new TheoreticalFrameworkTheory();
        theory.setFramework(framework);
        theory.setTheoryName(request.theoryName().trim());
        theory.setTheorist(request.theorist());
        theory.setOriginalYear(request.originalYear());
        theory.setDescription(request.description());
        theory.setKeyConstructs(request.keyConstructs());
        theory.setRelevanceToStudy(request.relevanceToStudy());
        theory.setLimitations(request.limitations());
        theory.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        theory.setOrigin(origin(request.origin()));
        return theoryResponse(theoryRepository.save(theory));
    }

    @Transactional
    public TheoryResponse updateTheory(UUID theoryId, User user, CreateTheoryRequest request) {
        TheoreticalFrameworkTheory theory = theory(theoryId);
        authorizationService.requireProjectEditor(theory.getFramework().getProject().getId(), user);
        if (request.theoryName() != null && !request.theoryName().isBlank()) theory.setTheoryName(request.theoryName().trim());
        if (request.theorist() != null) theory.setTheorist(request.theorist());
        if (request.originalYear() != null) theory.setOriginalYear(request.originalYear());
        if (request.description() != null) theory.setDescription(request.description());
        if (request.keyConstructs() != null) theory.setKeyConstructs(request.keyConstructs());
        if (request.relevanceToStudy() != null) theory.setRelevanceToStudy(request.relevanceToStudy());
        if (request.limitations() != null) theory.setLimitations(request.limitations());
        if (request.displayOrder() != null) theory.setDisplayOrder(request.displayOrder());
        if (request.origin() != null) theory.setOrigin(request.origin());
        return theoryResponse(theory);
    }

    public Object generate(UUID projectId, User user) {
        authorizationService.requireProjectEditor(projectId, user);
        throw new RagCapabilityUnavailableException("Theoretical framework generation is not enabled.");
    }

    private TheoreticalFramework framework(UUID id) { return frameworkRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theoretical framework not found.")); }
    private TheoreticalFrameworkTheory theory(UUID id) { return theoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theory not found.")); }
    private ContentOrigin origin(ContentOrigin origin) { return origin == null ? ContentOrigin.USER : origin; }
    private FrameworkResponse response(TheoreticalFramework f) { return new FrameworkResponse(f.getId(), f.getProject().getId(), f.getTitle(), f.getOverview(), f.getStatus().name(), f.getOrigin(), f.getRevisionNumber(), f.getCreatedAt(), f.getUpdatedAt()); }
    private TheoryResponse theoryResponse(TheoreticalFrameworkTheory t) { return new TheoryResponse(t.getId(), t.getFramework().getId(), t.getTheoryName(), t.getTheorist(), t.getOriginalYear(), t.getDescription(), t.getKeyConstructs(), t.getRelevanceToStudy(), t.getLimitations(), t.getDisplayOrder(), t.getOrigin()); }
}
