package com.researchassistant.dataset.service;

import com.researchassistant.datacollection.model.DataCollectionSession;
import com.researchassistant.datacollection.model.InstrumentResponse;
import com.researchassistant.datacollection.repository.DataCollectionSessionRepository;
import com.researchassistant.datacollection.repository.InstrumentResponseRepository;
import com.researchassistant.dataset.model.*;
import com.researchassistant.dataset.repository.DatasetRecordRepository;
import com.researchassistant.dataset.repository.DatasetValueRepository;
import com.researchassistant.dataset.repository.DatasetVariableRepository;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DatasetBuilderService {
    private final DataCollectionSessionRepository sessionRepository;
    private final InstrumentResponseRepository responseRepository;
    private final ResearchDatasetRepository datasetRepository;
    private final DatasetVariableRepository variableRepository;
    private final DatasetRecordRepository recordRepository;
    private final DatasetValueRepository valueRepository;
    private final ProjectAuthorizationService authorizationService;

    public DatasetBuilderService(DataCollectionSessionRepository sessionRepository, InstrumentResponseRepository responseRepository,
                                 ResearchDatasetRepository datasetRepository, DatasetVariableRepository variableRepository,
                                 DatasetRecordRepository recordRepository, DatasetValueRepository valueRepository,
                                 ProjectAuthorizationService authorizationService) {
        this.sessionRepository = sessionRepository;
        this.responseRepository = responseRepository;
        this.datasetRepository = datasetRepository;
        this.variableRepository = variableRepository;
        this.recordRepository = recordRepository;
        this.valueRepository = valueRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ResearchDataset buildFromCompletedSessions(UUID projectId, User user, String name) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ResearchDataset dataset = new ResearchDataset();
        dataset.setProject(project);
        dataset.setName(name == null || name.isBlank() ? "Collected sessions dataset" : name);
        dataset.setSourceType(ResearchDataset.SourceType.COLLECTED_SESSIONS);
        dataset.setStatus(ResearchDataset.Status.VALIDATING);
        dataset.setCreatedBy(user);
        ResearchDataset savedDataset = datasetRepository.save(dataset);
        Map<String, DatasetVariable> variables = new LinkedHashMap<>();
        long rowNumber = 1;
        for (DataCollectionSession session : sessionRepository.findAllByProjectIdAndStatus(projectId, DataCollectionSession.Status.COMPLETED)) {
            DatasetRecord record = new DatasetRecord();
            record.setDataset(savedDataset);
            record.setRowNumber(rowNumber++);
            record.setParticipant(session.getParticipant());
            record.setSourceSession(session);
            DatasetRecord savedRecord = recordRepository.save(record);
            for (InstrumentResponse response : responseRepository.findAllBySessionId(session.getId())) {
                String key = response.getInstrumentItemType().name().toLowerCase() + "_" + response.getInstrumentItemId().toString().replace("-", "_");
                DatasetVariable variable = variables.computeIfAbsent(key, k -> createVariable(savedDataset, k, response, variables.size() + 1));
                DatasetValue value = new DatasetValue();
                value.setRecord(savedRecord);
                value.setVariable(variable);
                value.setStringValue(response.getTextValue());
                value.setDecimalValue(response.getNumericValue());
                value.setBooleanValue(response.getBooleanValue());
                value.setDateValue(response.getDateValue());
                value.setCategoryCode(response.getOptionValue());
                if (response.getTextValue() == null && response.getNumericValue() == null && response.getBooleanValue() == null
                        && response.getDateValue() == null && response.getOptionValue() == null) {
                    value.setMissing(true);
                    value.setMissingReason(DatasetValue.MissingReason.NOT_PROVIDED);
                }
                valueRepository.save(value);
            }
        }
        savedDataset.setStatus(ResearchDataset.Status.READY);
        return savedDataset;
    }

    private DatasetVariable createVariable(ResearchDataset dataset, String name, InstrumentResponse response, int order) {
        DatasetVariable variable = new DatasetVariable();
        variable.setDataset(dataset);
        variable.setVariableName(name);
        variable.setLabel(name);
        variable.setDisplayOrder(order);
        variable.setSourceInstrumentItemId(response.getInstrumentItemId());
        variable.setNullable(true);
        variable.setType(switch (response.getType()) {
            case NUMERIC -> DatasetVariable.VariableType.DECIMAL;
            case BOOLEAN -> DatasetVariable.VariableType.BOOLEAN;
            case DATE -> DatasetVariable.VariableType.DATE;
            case OPTION, MULTIPLE_OPTIONS -> DatasetVariable.VariableType.CATEGORY;
            case TEXT -> DatasetVariable.VariableType.TEXT;
            default -> DatasetVariable.VariableType.STRING;
        });
        variable.setMeasurementLevel(variable.getType() == DatasetVariable.VariableType.CATEGORY
                ? DatasetVariable.MeasurementLevel.NOMINAL : DatasetVariable.MeasurementLevel.UNKNOWN);
        return variableRepository.save(variable);
    }
}
