package com.researchassistant.dataset.service;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.dataset.dto.DatasetDtos.*;
import com.researchassistant.dataset.model.*;
import com.researchassistant.dataset.repository.DatasetVariableRepository;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DatasetService {
    private final ResearchDatasetRepository datasetRepository;
    private final DatasetVariableRepository variableRepository;
    private final DatasetValidationService validationService;
    private final ProjectAuthorizationService authorizationService;

    public DatasetService(ResearchDatasetRepository datasetRepository, DatasetVariableRepository variableRepository,
                          DatasetValidationService validationService, ProjectAuthorizationService authorizationService) {
        this.datasetRepository = datasetRepository;
        this.variableRepository = variableRepository;
        this.validationService = validationService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public DatasetResponse create(UUID projectId, User user, CreateDatasetRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ResearchDataset dataset = new ResearchDataset();
        dataset.setProject(project);
        dataset.setName(request.name());
        dataset.setDescription(request.description());
        dataset.setSourceType(request.sourceType() == null ? ResearchDataset.SourceType.MANUAL : request.sourceType());
        dataset.setCreatedBy(user);
        return DatasetResponse.from(datasetRepository.save(dataset));
    }

    @Transactional(readOnly = true)
    public Page<DatasetResponse> list(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        return datasetRepository.findAllByProjectId(projectId, pageable).map(DatasetResponse::from);
    }

    @Transactional
    public VariableResponse addVariable(UUID datasetId, User user, CreateVariableRequest request) {
        ResearchDataset dataset = loadForEdit(datasetId, user);
        if (!request.variableName().matches("^[A-Za-z][A-Za-z0-9_]*$")) throw new IllegalArgumentException("Invalid variable name.");
        if (variableRepository.existsByDatasetIdAndVariableName(datasetId, request.variableName())) throw new IllegalArgumentException("Duplicate variable name.");
        DatasetVariable variable = new DatasetVariable();
        variable.setDataset(dataset);
        variable.setVariableName(request.variableName());
        variable.setLabel(request.label());
        variable.setType(request.type());
        variable.setMeasurementLevel(request.measurementLevel() == null ? DatasetVariable.MeasurementLevel.UNKNOWN : request.measurementLevel());
        variable.setNullable(request.nullable());
        variable.setUnit(request.unit());
        variable.setMissingValueCode(request.missingValueCode());
        variable.setDisplayOrder(request.displayOrder() == null ? variableRepository.findAllByDatasetIdOrderByDisplayOrderAsc(datasetId).size() + 1 : request.displayOrder());
        return VariableResponse.from(variableRepository.save(variable));
    }

    @Transactional
    public void validate(UUID datasetId, User user) {
        ResearchDataset dataset = loadForEdit(datasetId, user);
        dataset.setStatus(ResearchDataset.Status.VALIDATING);
        validationService.validate(dataset);
    }

    @Transactional(readOnly = true)
    public Page<ValidationIssueResponse> issues(UUID datasetId, User user, Pageable pageable) {
        ResearchDataset dataset = loadForView(datasetId, user);
        return validationService.issues(dataset.getId(), pageable).map(ValidationIssueResponse::from);
    }

    @Transactional(readOnly = true)
    public DatasetSummaryResponse summary(UUID datasetId, User user) {
        ResearchDataset dataset = loadForView(datasetId, user);
        return validationService.summary(dataset);
    }

    private ResearchDataset loadForEdit(UUID id, User user) {
        ResearchDataset dataset = datasetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dataset not found."));
        authorizationService.requireProjectEditor(dataset.getProject().getId(), user);
        return dataset;
    }

    private ResearchDataset loadForView(UUID id, User user) {
        ResearchDataset dataset = datasetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dataset not found."));
        authorizationService.requireProjectViewer(dataset.getProject().getId(), user);
        return dataset;
    }
}
