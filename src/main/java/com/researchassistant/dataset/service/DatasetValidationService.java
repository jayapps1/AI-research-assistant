package com.researchassistant.dataset.service;

import com.researchassistant.dataset.dto.DatasetDtos.DatasetSummaryResponse;
import com.researchassistant.dataset.dto.DatasetDtos.NumericRange;
import com.researchassistant.dataset.model.*;
import com.researchassistant.dataset.repository.DatasetRecordRepository;
import com.researchassistant.dataset.repository.DatasetValidationIssueRepository;
import com.researchassistant.dataset.repository.DatasetValueRepository;
import com.researchassistant.dataset.repository.DatasetVariableCategoryRepository;
import com.researchassistant.dataset.repository.DatasetVariableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatasetValidationService {
    private final DatasetVariableRepository variableRepository;
    private final DatasetRecordRepository recordRepository;
    private final DatasetValueRepository valueRepository;
    private final DatasetVariableCategoryRepository categoryRepository;
    private final DatasetValidationIssueRepository issueRepository;

    public DatasetValidationService(DatasetVariableRepository variableRepository, DatasetRecordRepository recordRepository,
                                    DatasetValueRepository valueRepository, DatasetVariableCategoryRepository categoryRepository,
                                    DatasetValidationIssueRepository issueRepository) {
        this.variableRepository = variableRepository;
        this.recordRepository = recordRepository;
        this.valueRepository = valueRepository;
        this.categoryRepository = categoryRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional
    public void validate(ResearchDataset dataset) {
        issueRepository.deleteAllByDatasetId(dataset.getId());
        List<DatasetVariable> variables = variableRepository.findAllByDatasetIdOrderByDisplayOrderAsc(dataset.getId());
        List<DatasetValue> values = valueRepository.findAllByRecordDatasetId(dataset.getId());
        Map<UUID, DatasetVariable> variableById = variables.stream().collect(Collectors.toMap(DatasetVariable::getId, v -> v));
        int errors = 0;
        for (DatasetValue value : values) {
            DatasetVariable variable = variableById.get(value.getVariable().getId());
            if (variable == null) continue;
            String error = validateValue(variable, value);
            if (error != null) {
                errors++;
                issue(dataset, value.getRecord().getRowNumber(), variable.getId(), DatasetValidationIssue.Severity.ERROR, "INVALID_VALUE", error, snapshot(value));
            }
        }
        dataset.setStatus(errors == 0 ? ResearchDataset.Status.READY : ResearchDataset.Status.INVALID);
    }

    @Transactional(readOnly = true)
    public Page<DatasetValidationIssue> issues(UUID datasetId, Pageable pageable) {
        return issueRepository.findAllByDatasetId(datasetId, pageable);
    }

    @Transactional(readOnly = true)
    public DatasetSummaryResponse summary(ResearchDataset dataset) {
        List<DatasetVariable> variables = variableRepository.findAllByDatasetIdOrderByDisplayOrderAsc(dataset.getId());
        List<DatasetValue> values = valueRepository.findAllByRecordDatasetId(dataset.getId());
        Map<UUID, String> names = variables.stream().collect(Collectors.toMap(DatasetVariable::getId, DatasetVariable::getVariableName));
        Map<String, Long> missing = values.stream()
                .filter(DatasetValue::isMissing)
                .collect(Collectors.groupingBy(v -> names.get(v.getVariable().getId()), LinkedHashMap::new, Collectors.counting()));
        Map<String, Map<String, Long>> categoryCounts = values.stream()
                .filter(v -> v.getCategoryCode() != null)
                .collect(Collectors.groupingBy(v -> names.get(v.getVariable().getId()), LinkedHashMap::new,
                        Collectors.groupingBy(DatasetValue::getCategoryCode, LinkedHashMap::new, Collectors.counting())));
        Map<String, NumericRange> ranges = new LinkedHashMap<>();
        for (DatasetVariable variable : variables) {
            if (variable.getType() == DatasetVariable.VariableType.INTEGER || variable.getType() == DatasetVariable.VariableType.DECIMAL) {
                List<BigDecimal> nums = values.stream().filter(v -> v.getVariable().getId().equals(variable.getId()))
                        .map(v -> v.getDecimalValue() != null ? v.getDecimalValue() : v.getIntegerValue() == null ? null : BigDecimal.valueOf(v.getIntegerValue()))
                        .filter(Objects::nonNull).toList();
                if (!nums.isEmpty()) ranges.put(variable.getVariableName(), new NumericRange(Collections.min(nums).toPlainString(), Collections.max(nums).toPlainString()));
            }
        }
        return new DatasetSummaryResponse(dataset.getId(), recordRepository.countByDatasetId(dataset.getId()), variables.size(), missing, categoryCounts, ranges);
    }

    private String validateValue(DatasetVariable variable, DatasetValue value) {
        if (value.isMissing()) return variable.isNullable() ? null : "Required variable is missing.";
        long populated = java.util.stream.Stream.of(
                value.getStringValue(),
                value.getIntegerValue(),
                value.getDecimalValue(),
                value.getBooleanValue(),
                value.getDateValue(),
                value.getDateTimeValue(),
                value.getTextValue(),
                value.getCategoryCode()
        ).filter(Objects::nonNull).count();
        if (populated == 0 && !variable.isNullable()) return "Required variable is empty.";
        if (populated > 1) return "Dataset value contains more than one logical value.";
        if ((variable.getType() == DatasetVariable.VariableType.CATEGORY || variable.getType() == DatasetVariable.VariableType.ORDINAL)
                && value.getCategoryCode() != null
                && !categoryRepository.existsByVariableIdAndCode(variable.getId(), value.getCategoryCode())) {
            return "Category code is not defined for variable.";
        }
        return null;
    }

    private void issue(ResearchDataset dataset, Long row, UUID variableId, DatasetValidationIssue.Severity severity, String code, String message, String snapshot) {
        DatasetValidationIssue issue = new DatasetValidationIssue();
        issue.setDataset(dataset);
        issue.setRowNumber(row);
        issue.setVariableId(variableId);
        issue.setSeverity(severity);
        issue.setCode(code);
        issue.setMessage(message);
        issue.setRejectedValueSnapshot(snapshot);
        issueRepository.save(issue);
    }

    private String snapshot(DatasetValue v) {
        if (v.getStringValue() != null) return v.getStringValue();
        if (v.getIntegerValue() != null) return v.getIntegerValue().toString();
        if (v.getDecimalValue() != null) return v.getDecimalValue().toPlainString();
        if (v.getBooleanValue() != null) return v.getBooleanValue().toString();
        if (v.getDateValue() != null) return v.getDateValue().toString();
        if (v.getDateTimeValue() != null) return v.getDateTimeValue().toString();
        if (v.getTextValue() != null) return v.getTextValue();
        return v.getCategoryCode();
    }
}
