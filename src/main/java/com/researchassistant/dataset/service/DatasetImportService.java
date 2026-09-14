package com.researchassistant.dataset.service;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.dataset.dto.DatasetDtos.*;
import com.researchassistant.dataset.model.*;
import com.researchassistant.dataset.repository.DatasetImportColumnMappingRepository;
import com.researchassistant.dataset.repository.DatasetImportJobRepository;
import com.researchassistant.dataset.repository.DatasetRecordRepository;
import com.researchassistant.dataset.repository.DatasetValueRepository;
import com.researchassistant.dataset.repository.DatasetVariableRepository;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

@Service
public class DatasetImportService {
    private static final long MAX_IMPORT_BYTES = 25L * 1024L * 1024L;
    private static final int SAMPLE_ROWS = 20;

    private final ResearchDatasetRepository datasetRepository;
    private final DatasetImportJobRepository jobRepository;
    private final DatasetImportColumnMappingRepository mappingRepository;
    private final DatasetVariableRepository variableRepository;
    private final DatasetRecordRepository recordRepository;
    private final DatasetValueRepository valueRepository;
    private final ProjectAuthorizationService authorizationService;
    private final Path importDirectory;

    public DatasetImportService(ResearchDatasetRepository datasetRepository, DatasetImportJobRepository jobRepository,
                                DatasetImportColumnMappingRepository mappingRepository, DatasetVariableRepository variableRepository,
                                DatasetRecordRepository recordRepository, DatasetValueRepository valueRepository,
                                ProjectAuthorizationService authorizationService,
                                @Value("${app.dataset.import.storage-dir:./data/dataset-imports}") String importDirectory) {
        this.datasetRepository = datasetRepository;
        this.jobRepository = jobRepository;
        this.mappingRepository = mappingRepository;
        this.variableRepository = variableRepository;
        this.recordRepository = recordRepository;
        this.valueRepository = valueRepository;
        this.authorizationService = authorizationService;
        this.importDirectory = Path.of(importDirectory);
    }

    @Transactional
    public ImportStartResponse start(UUID projectId, User user, MultipartFile file) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        if (file.getSize() > MAX_IMPORT_BYTES) throw new IllegalArgumentException("Import file is too large.");
        DatasetImportJob.Format format = format(file.getOriginalFilename());
        ResearchDataset dataset = new ResearchDataset();
        dataset.setProject(project);
        dataset.setName(stripExtension(file.getOriginalFilename()));
        dataset.setSourceType(format == DatasetImportJob.Format.CSV ? ResearchDataset.SourceType.IMPORTED_CSV : ResearchDataset.SourceType.IMPORTED_XLSX);
        dataset.setStatus(ResearchDataset.Status.DRAFT);
        dataset.setCreatedBy(user);
        ResearchDataset savedDataset = datasetRepository.save(dataset);
        DatasetImportJob job = new DatasetImportJob();
        job.setDataset(savedDataset);
        job.setFormat(format);
        job.setOriginalFilename(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        job.setFileSizeBytes(file.getSize());
        try {
            Files.createDirectories(importDirectory);
            byte[] bytes = file.getBytes();
            job.setChecksumSha256(sha256(bytes));
            String storageKey = savedDataset.getProject().getId() + "/" + UUID.randomUUID() + "." + format.name().toLowerCase();
            Path target = importDirectory.resolve(storageKey).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            job.setStorageKey(storageKey);
            job.setStatus(DatasetImportJob.Status.AWAITING_MAPPING);
        } catch (IOException exception) {
            throw new IllegalStateException("Dataset import storage failed.", exception);
        }
        DatasetImportJob saved = jobRepository.save(job);
        return new ImportStartResponse(saved.getId(), savedDataset.getId(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse preview(UUID jobId, User user) {
        DatasetImportJob job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Import job not found."));
        authorizationService.requireProjectViewer(job.getDataset().getProject().getId(), user);
        TabularData data = read(job);
        return new ImportPreviewResponse(job.getId(), profile(data), data.sampleRows(), List.of());
    }

    @Transactional
    public ImportStartResponse confirm(UUID jobId, User user, ConfirmMappingRequest request) {
        DatasetImportJob job = jobRepository.findByIdForUpdate(jobId).orElseThrow(() -> new ResourceNotFoundException("Import job not found."));
        authorizationService.requireProjectEditor(job.getDataset().getProject().getId(), user);
        if (job.getStatus() != DatasetImportJob.Status.AWAITING_MAPPING && job.getStatus() != DatasetImportJob.Status.COMPLETED_WITH_ERRORS) {
            throw new IllegalStateException("Import job cannot be confirmed from current state.");
        }
        job.setStatus(DatasetImportJob.Status.IMPORTING);
        mappingRepository.deleteAllByJobId(jobId);
        List<DatasetImportColumnMapping> mappings = new ArrayList<>();
        for (ColumnMappingRequest requestMapping : request.mappings()) {
            DatasetImportColumnMapping mapping = new DatasetImportColumnMapping();
            mapping.setJob(job);
            mapping.setSourceColumn(requestMapping.sourceColumn());
            mapping.setProposedVariableName(requestMapping.proposedVariableName());
            mapping.setTargetType(requestMapping.targetType());
            mapping.setMeasurementLevel(requestMapping.measurementLevel() == null ? DatasetVariable.MeasurementLevel.UNKNOWN : requestMapping.measurementLevel());
            mapping.setIgnored(requestMapping.ignored());
            mappings.add(mappingRepository.save(mapping));
        }
        TabularData data = read(job);
        importRows(job, mappings, data);
        job.setTotalRows(data.rows().size());
        job.setImportedRows(data.rows().size());
        job.setRejectedRows(0);
        job.setStatus(DatasetImportJob.Status.COMPLETED);
        job.getDataset().setStatus(ResearchDataset.Status.READY);
        return new ImportStartResponse(job.getId(), job.getDataset().getId(), job.getStatus());
    }

    private void importRows(DatasetImportJob job, List<DatasetImportColumnMapping> mappings, TabularData data) {
        Map<String, DatasetVariable> variableByColumn = new LinkedHashMap<>();
        int order = 1;
        for (DatasetImportColumnMapping mapping : mappings) {
            if (mapping.isIgnored()) continue;
            String name = mapping.getProposedVariableName() == null || mapping.getProposedVariableName().isBlank()
                    ? safeVariableName(mapping.getSourceColumn()) : mapping.getProposedVariableName();
            if (variableRepository.existsByDatasetIdAndVariableName(job.getDataset().getId(), name)) {
                throw new IllegalArgumentException("Duplicate variable name: " + name);
            }
            DatasetVariable variable = new DatasetVariable();
            variable.setDataset(job.getDataset());
            variable.setVariableName(name);
            variable.setLabel(mapping.getSourceColumn());
            variable.setType(mapping.getTargetType());
            variable.setMeasurementLevel(mapping.getMeasurementLevel());
            variable.setNullable(true);
            variable.setDisplayOrder(order++);
            variableByColumn.put(mapping.getSourceColumn(), variableRepository.save(variable));
        }
        long rowNumber = 1;
        for (Map<String, String> row : data.rows()) {
            DatasetRecord record = new DatasetRecord();
            record.setDataset(job.getDataset());
            record.setRowNumber(rowNumber++);
            DatasetRecord savedRecord = recordRepository.save(record);
            for (var entry : variableByColumn.entrySet()) {
                DatasetValue value = toValue(savedRecord, entry.getValue(), row.get(entry.getKey()));
                valueRepository.save(value);
            }
        }
    }

    private DatasetValue toValue(DatasetRecord record, DatasetVariable variable, String raw) {
        DatasetValue value = new DatasetValue();
        value.setRecord(record);
        value.setVariable(variable);
        if (raw == null || raw.isBlank()) {
            value.setMissing(true);
            value.setMissingReason(DatasetValue.MissingReason.NOT_PROVIDED);
            return value;
        }
        try {
            switch (variable.getType()) {
                case INTEGER -> value.setIntegerValue(Long.parseLong(raw.trim()));
                case DECIMAL -> value.setDecimalValue(new BigDecimal(raw.trim()));
                case BOOLEAN -> value.setBooleanValue(Boolean.parseBoolean(raw.trim()));
                case TEXT -> value.setTextValue(raw);
                case CATEGORY, ORDINAL -> value.setCategoryCode(raw.trim());
                default -> value.setStringValue(raw);
            }
        } catch (RuntimeException exception) {
            value.setMissing(true);
            value.setMissingReason(DatasetValue.MissingReason.IMPORT_ERROR);
            value.setTextValue(raw);
        }
        return value;
    }

    private List<ColumnProfile> profile(TabularData data) {
        List<ColumnProfile> columns = new ArrayList<>();
        for (String header : data.headers()) {
            List<String> values = data.rows().stream().map(r -> r.get(header)).filter(v -> v != null && !v.isBlank()).toList();
            long missing = data.rows().size() - values.size();
            long distinct = values.stream().limit(1000).distinct().count();
            columns.add(new ColumnProfile(header, safeVariableName(header), infer(values),
                    distinct <= 20 ? DatasetVariable.MeasurementLevel.NOMINAL : DatasetVariable.MeasurementLevel.UNKNOWN,
                    values.stream().limit(5).toList(), missing, distinct, List.of()));
        }
        return columns;
    }

    private DatasetVariable.VariableType infer(List<String> values) {
        if (values.isEmpty()) return DatasetVariable.VariableType.STRING;
        if (values.stream().allMatch(v -> v.matches("-?\\d+"))) return DatasetVariable.VariableType.INTEGER;
        if (values.stream().allMatch(v -> v.matches("-?\\d+(\\.\\d+)?"))) return DatasetVariable.VariableType.DECIMAL;
        if (values.stream().allMatch(v -> v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false"))) return DatasetVariable.VariableType.BOOLEAN;
        if (values.stream().limit(200).distinct().count() <= 20) return DatasetVariable.VariableType.CATEGORY;
        return DatasetVariable.VariableType.STRING;
    }

    private TabularData read(DatasetImportJob job) {
        Path path = importDirectory.resolve(job.getStorageKey()).normalize();
        try {
            return job.getFormat() == DatasetImportJob.Format.CSV ? readCsv(path) : readXlsx(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Dataset import preview failed.", exception);
        }
    }

    private TabularData readCsv(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
            List<String> headers = parser.getHeaderNames();
            List<Map<String, String>> rows = new ArrayList<>();
            parser.forEach(r -> {
                Map<String, String> row = new LinkedHashMap<>();
                headers.forEach(h -> row.put(h, r.get(h)));
                rows.add(row);
            });
            return new TabularData(headers, rows, rows.stream().limit(SAMPLE_ROWS).toList());
        }
    }

    private TabularData readXlsx(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) headers.add(cell.toString());
            List<Map<String, String>> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) values.put(headers.get(c), formatter.formatCellValue(row.getCell(c)));
                rows.add(values);
            }
            return new TabularData(headers, rows, rows.stream().limit(SAMPLE_ROWS).toList());
        }
    }

    private DatasetImportJob.Format format(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) return DatasetImportJob.Format.CSV;
        if (lower.endsWith(".xlsx")) return DatasetImportJob.Format.XLSX;
        throw new IllegalArgumentException("Unsupported import format.");
    }

    private String stripExtension(String filename) {
        if (filename == null || filename.isBlank()) return "Imported dataset";
        int dot = filename.lastIndexOf('.');
        return dot <= 0 ? filename : filename.substring(0, dot);
    }

    private String safeVariableName(String source) {
        String name = source == null ? "variable" : source.trim().replaceAll("[^A-Za-z0-9_]+", "_");
        if (name.isBlank()) name = "variable";
        if (!Character.isLetter(name.charAt(0))) name = "v_" + name;
        return name;
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) builder.append("%02x".formatted(b));
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Checksum failed.", exception);
        }
    }

    private record TabularData(List<String> headers, List<Map<String, String>> rows, List<Map<String, String>> sampleRows) {}
}
