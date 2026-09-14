package com.researchassistant.framework.service;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.framework.dto.*;
import com.researchassistant.framework.entity.*;
import com.researchassistant.framework.repository.*;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.service.MethodologyConsistencyService;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.exception.RagCapabilityUnavailableException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConceptualFrameworkService {
    private final ProjectAuthorizationService authorizationService;
    private final ConceptualFrameworkRepository frameworkRepository;
    private final ConceptualVariableRepository variableRepository;
    private final ConceptualRelationshipRepository relationshipRepository;
    private final MethodologyConsistencyService consistencyService;

    public ConceptualFrameworkService(ProjectAuthorizationService authorizationService, ConceptualFrameworkRepository frameworkRepository, ConceptualVariableRepository variableRepository, ConceptualRelationshipRepository relationshipRepository, MethodologyConsistencyService consistencyService) {
        this.authorizationService = authorizationService;
        this.frameworkRepository = frameworkRepository;
        this.variableRepository = variableRepository;
        this.relationshipRepository = relationshipRepository;
        this.consistencyService = consistencyService;
    }

    @Transactional
    public FrameworkResponse create(UUID projectId, User user, CreateConceptualFrameworkRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, user);
        ConceptualFramework framework = new ConceptualFramework();
        framework.setProject(context.project());
        framework.setTitle(request.title().trim());
        framework.setDescription(request.description());
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
        ConceptualFramework framework = framework(id);
        authorizationService.requireProjectViewer(framework.getProject().getId(), user);
        return response(framework);
    }

    @Transactional
    public FrameworkResponse update(UUID id, User user, UpdateConceptualFrameworkRequest request) {
        ConceptualFramework framework = framework(id);
        authorizationService.requireProjectEditor(framework.getProject().getId(), user);
        if (request.title() != null && !request.title().isBlank()) framework.setTitle(request.title().trim());
        if (request.description() != null) framework.setDescription(request.description());
        if (request.origin() != null) framework.setOrigin(request.origin());
        framework.setUpdatedBy(user);
        return response(framework);
    }

    @Transactional
    public FrameworkResponse activate(UUID id, User user) {
        ConceptualFramework framework = framework(id);
        authorizationService.requireProjectEditor(framework.getProject().getId(), user);
        frameworkRepository.findByProjectIdAndStatus(framework.getProject().getId(), ConceptualFrameworkStatus.ACTIVE)
                .filter(active -> !active.getId().equals(framework.getId()))
                .ifPresent(active -> active.setStatus(ConceptualFrameworkStatus.SUPERSEDED));
        framework.setStatus(ConceptualFrameworkStatus.ACTIVE);
        return response(framework);
    }

    @Transactional
    public ConceptualVariableResponse addVariable(UUID frameworkId, User user, CreateConceptualVariableRequest request) {
        ConceptualFramework framework = framework(frameworkId);
        authorizationService.requireProjectEditor(framework.getProject().getId(), user);
        ConceptualVariable variable = new ConceptualVariable();
        variable.setFramework(framework);
        variable.setName(request.name().trim());
        variable.setDescription(request.description());
        variable.setType(request.type() == null ? ConceptualVariableType.OTHER : request.type());
        variable.setOperationalDefinition(request.operationalDefinition());
        variable.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        return variableResponse(variableRepository.save(variable));
    }

    @Transactional
    public ConceptualVariableResponse updateVariable(UUID variableId, User user, CreateConceptualVariableRequest request) {
        ConceptualVariable variable = variable(variableId);
        authorizationService.requireProjectEditor(variable.getFramework().getProject().getId(), user);
        if (request.name() != null && !request.name().isBlank()) variable.setName(request.name().trim());
        if (request.description() != null) variable.setDescription(request.description());
        if (request.type() != null) variable.setType(request.type());
        if (request.operationalDefinition() != null) variable.setOperationalDefinition(request.operationalDefinition());
        if (request.displayOrder() != null) variable.setDisplayOrder(request.displayOrder());
        return variableResponse(variable);
    }

    @Transactional
    public ConceptualRelationshipResponse addRelationship(UUID frameworkId, User user, CreateConceptualRelationshipRequest request) {
        ConceptualFramework framework = framework(frameworkId);
        authorizationService.requireProjectEditor(framework.getProject().getId(), user);
        ConceptualVariable source = variable(request.sourceVariableId());
        ConceptualVariable target = variable(request.targetVariableId());
        consistencyService.requireVariableInFramework(source, framework);
        consistencyService.requireVariableInFramework(target, framework);
        ConceptualRelationship relationship = new ConceptualRelationship();
        relationship.setFramework(framework);
        relationship.setSourceVariable(source);
        relationship.setTargetVariable(target);
        relationship.setType(request.type() == null ? ConceptualRelationshipType.OTHER : request.type());
        relationship.setLabel(request.label());
        relationship.setRationale(request.rationale());
        return relationshipResponse(relationshipRepository.save(relationship));
    }

    @Transactional
    public ConceptualRelationshipResponse updateRelationship(UUID relationshipId, User user, CreateConceptualRelationshipRequest request) {
        ConceptualRelationship relationship = relationship(relationshipId);
        authorizationService.requireProjectEditor(relationship.getFramework().getProject().getId(), user);
        if (request.type() != null) relationship.setType(request.type());
        if (request.label() != null) relationship.setLabel(request.label());
        if (request.rationale() != null) relationship.setRationale(request.rationale());
        return relationshipResponse(relationship);
    }

    public Object generate(UUID projectId, User user) {
        authorizationService.requireProjectEditor(projectId, user);
        throw new RagCapabilityUnavailableException("Conceptual framework generation is not enabled.");
    }

    private ConceptualFramework framework(UUID id) {
        return frameworkRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conceptual framework not found."));
    }
    private ConceptualVariable variable(UUID id) {
        return variableRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conceptual variable not found."));
    }
    private ConceptualRelationship relationship(UUID id) {
        return relationshipRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conceptual relationship not found."));
    }
    private ContentOrigin origin(ContentOrigin origin) { return origin == null ? ContentOrigin.USER : origin; }
    private FrameworkResponse response(ConceptualFramework f) { return new FrameworkResponse(f.getId(), f.getProject().getId(), f.getTitle(), f.getDescription(), f.getStatus().name(), f.getOrigin(), f.getRevisionNumber(), f.getCreatedAt(), f.getUpdatedAt()); }
    private ConceptualVariableResponse variableResponse(ConceptualVariable v) { return new ConceptualVariableResponse(v.getId(), v.getFramework().getId(), v.getName(), v.getDescription(), v.getType(), v.getOperationalDefinition(), v.getDisplayOrder()); }
    private ConceptualRelationshipResponse relationshipResponse(ConceptualRelationship r) { return new ConceptualRelationshipResponse(r.getId(), r.getFramework().getId(), r.getSourceVariable().getId(), r.getTargetVariable().getId(), r.getType(), r.getLabel(), r.getRationale()); }
}
