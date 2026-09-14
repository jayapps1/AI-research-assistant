package com.researchassistant.dataset.dto;

import com.researchassistant.dataset.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DatasetDtos {
    private DatasetDtos() {}
    public record CreateDatasetRequest(@NotBlank String name, String description, ResearchDataset.SourceType sourceType) {}
    public record CreateVariableRequest(@NotBlank String variableName, @NotBlank String label,
            @NotNull DatasetVariable.VariableType type, DatasetVariable.MeasurementLevel measurementLevel,
            boolean nullable, String unit, String missingValueCode, Integer displayOrder) {}
    public record DatasetResponse(UUID id, UUID projectId, String name, ResearchDataset.SourceType sourceType, ResearchDataset.Status status) {
        public static DatasetResponse from(ResearchDataset d) {
            return new DatasetResponse(d.getId(), d.getProject().getId(), d.getName(), d.getSourceType(), d.getStatus());
        }
    }
    public record VariableResponse(UUID id, String variableName, String label, DatasetVariable.VariableType type,
            DatasetVariable.MeasurementLevel measurementLevel, boolean nullable, int displayOrder) {
        public static VariableResponse from(DatasetVariable v) {
            return new VariableResponse(v.getId(), v.getVariableName(), v.getLabel(), v.getType(), v.getMeasurementLevel(), v.isNullable(), v.getDisplayOrder());
        }
    }
    public record ImportStartResponse(UUID importJobId, UUID datasetId, DatasetImportJob.Status status) {}
    public record ColumnProfile(String sourceColumn, String inferredVariableName, DatasetVariable.VariableType inferredType,
            DatasetVariable.MeasurementLevel suggestedMeasurementLevel, List<String> sampleValues, long missingCount, long distinctCount, List<String> warnings) {}
    public record ImportPreviewResponse(UUID importJobId, List<ColumnProfile> columns, List<Map<String, String>> sampleRows, List<String> warnings) {}
    public record ConfirmMappingRequest(List<ColumnMappingRequest> mappings) {}
    public record ColumnMappingRequest(@NotBlank String sourceColumn, UUID variableId, String proposedVariableName,
            @NotNull DatasetVariable.VariableType targetType, DatasetVariable.MeasurementLevel measurementLevel, boolean ignored) {}
    public record ValidationIssueResponse(UUID id, Long rowNumber, UUID variableId, DatasetValidationIssue.Severity severity,
            String code, String message, String rejectedValueSnapshot) {
        public static ValidationIssueResponse from(DatasetValidationIssue i) {
            return new ValidationIssueResponse(i.getId(), i.getRowNumber(), i.getVariableId(), i.getSeverity(), i.getCode(), i.getMessage(), i.getRejectedValueSnapshot());
        }
    }
    public record DatasetSummaryResponse(UUID datasetId, long recordCount, long variableCount,
            Map<String, Long> missingValuesByVariable, Map<String, Map<String, Long>> categoryCounts,
            Map<String, NumericRange> numericRanges) {}
    public record NumericRange(String min, String max) {}
}
