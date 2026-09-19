package com.researchassistant.publicsite.service;

import com.researchassistant.analysis.entity.ReportExportStatus;
import com.researchassistant.analysis.repository.ReportExportJobRepository;
import com.researchassistant.cache.AppCacheNames;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.project.entity.ResearchProjectStatus;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.publicsite.dto.*;
import com.researchassistant.publicsite.entity.PublicStatistic;
import com.researchassistant.publicsite.entity.PublicStatisticValueSource;
import com.researchassistant.publicsite.entity.PublicSystemMetric;
import com.researchassistant.publicsite.repository.PublicStatisticRepository;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PublicStatisticsService {

    private final PublicStatisticRepository statisticRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ReportExportJobRepository reportExportJobRepository;

    public PublicStatisticsService(PublicStatisticRepository statisticRepository,
                                   ResearchProjectRepository researchProjectRepository,
                                   UserRepository userRepository,
                                   DocumentRepository documentRepository,
                                   WorkspaceRepository workspaceRepository,
                                   ReportExportJobRepository reportExportJobRepository) {
        this.statisticRepository = statisticRepository;
        this.researchProjectRepository = researchProjectRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.reportExportJobRepository = reportExportJobRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(AppCacheNames.PUBLIC_STATISTICS)
    public List<PublicStatisticResponse> getPublicStatistics() {
        return statisticRepository.findAllByEnabledTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPublicStatisticResponse> getAdminStatistics() {
        return statisticRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPublicStatisticResponse getStatisticById(UUID id) {
        PublicStatistic statistic = statisticRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public statistic not found with id: " + id));
        return toAdminResponse(statistic);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_STATISTICS, allEntries = true)
    public AdminPublicStatisticResponse createStatistic(CreatePublicStatisticRequest request) {
        if (statisticRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("A statistic with code '" + request.code() + "' already exists");
        }

        validateStatisticValues(request.valueSource(), request.manualValue(), request.systemMetric());

        PublicStatistic stat = new PublicStatistic();
        stat.setCode(request.code().trim().toLowerCase(Locale.ROOT));
        stat.setLabel(request.label().trim());
        stat.setDescription(request.description() != null ? request.description().trim() : null);
        stat.setValueSource(request.valueSource());
        stat.setManualValue(request.manualValue() != null ? request.manualValue().trim() : null);
        stat.setSystemMetric(request.systemMetric());
        stat.setPrefix(request.prefix() != null ? request.prefix().trim() : null);
        stat.setSuffix(request.suffix() != null ? request.suffix().trim() : null);
        stat.setIconKey(request.iconKey() != null ? request.iconKey().trim() : null);
        stat.setEnabled(request.enabled() != null ? request.enabled() : true);
        stat.setFeatured(request.featured() != null ? request.featured() : false);
        stat.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : (int) statisticRepository.count());

        stat = statisticRepository.save(stat);
        return toAdminResponse(stat);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_STATISTICS, allEntries = true)
    public AdminPublicStatisticResponse updateStatistic(UUID id, UpdatePublicStatisticRequest request) {
        PublicStatistic stat = statisticRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public statistic not found with id: " + id));

        validateStatisticValues(request.valueSource(), request.manualValue(), request.systemMetric());

        stat.setLabel(request.label().trim());
        stat.setDescription(request.description() != null ? request.description().trim() : null);
        stat.setValueSource(request.valueSource());
        stat.setManualValue(request.manualValue() != null ? request.manualValue().trim() : null);
        stat.setSystemMetric(request.systemMetric());
        stat.setPrefix(request.prefix() != null ? request.prefix().trim() : null);
        stat.setSuffix(request.suffix() != null ? request.suffix().trim() : null);
        stat.setIconKey(request.iconKey() != null ? request.iconKey().trim() : null);
        if (request.enabled() != null) {
            stat.setEnabled(request.enabled());
        }
        if (request.featured() != null) {
            stat.setFeatured(request.featured());
        }
        if (request.displayOrder() != null) {
            stat.setDisplayOrder(request.displayOrder());
        }

        stat = statisticRepository.save(stat);
        return toAdminResponse(stat);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_STATISTICS, allEntries = true)
    public void deleteStatistic(UUID id) {
        if (!statisticRepository.existsById(id)) {
            throw new ResourceNotFoundException("Public statistic not found with id: " + id);
        }
        statisticRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public MetricPreviewResponse previewMetric(PublicSystemMetric metric) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }
        long raw = calculateRawSystemMetric(metric);
        String formatted = NumberFormat.getNumberInstance(Locale.US).format(raw);
        return new MetricPreviewResponse(metric, raw, formatted, Instant.now());
    }

    public long calculateRawSystemMetric(PublicSystemMetric metric) {
        if (metric == null) return 0L;
        return switch (metric) {
            case TOTAL_RESEARCH_PROJECTS ->
                    researchProjectRepository.countByStatusNot(ResearchProjectStatus.ARCHIVED);
            case TOTAL_ACTIVE_USERS ->
                    userRepository.countByStatus(UserStatus.ACTIVE);
            case TOTAL_DOCUMENTS_PROCESSED ->
                    documentRepository.countByStatusNot(DocumentStatus.ARCHIVED);
            case TOTAL_WORKSPACES ->
                    workspaceRepository.countByStatus(WorkspaceStatus.ACTIVE);
            case TOTAL_COMPLETED_REPORT_EXPORTS ->
                    reportExportJobRepository.countByStatus(ReportExportStatus.COMPLETED);
        };
    }

    private void validateStatisticValues(PublicStatisticValueSource source, String manualValue, PublicSystemMetric metric) {
        if (source == PublicStatisticValueSource.SYSTEM_DERIVED) {
            if (metric == null) {
                throw new IllegalArgumentException("System metric must be selected when value source is SYSTEM_DERIVED");
            }
        } else if (source == PublicStatisticValueSource.MANUAL) {
            if (manualValue == null || manualValue.isBlank()) {
                throw new IllegalArgumentException("Manual value must be provided when value source is MANUAL");
            }
        }
    }

    private String resolveValue(PublicStatistic stat) {
        if (stat.getValueSource() == PublicStatisticValueSource.SYSTEM_DERIVED && stat.getSystemMetric() != null) {
            long raw = calculateRawSystemMetric(stat.getSystemMetric());
            return NumberFormat.getNumberInstance(Locale.US).format(raw);
        }
        return stat.getManualValue() != null ? stat.getManualValue() : "";
    }

    private PublicStatisticResponse toPublicResponse(PublicStatistic stat) {
        return new PublicStatisticResponse(
                stat.getCode(),
                stat.getLabel(),
                resolveValue(stat),
                stat.getPrefix(),
                stat.getSuffix(),
                stat.getIconKey(),
                stat.isFeatured()
        );
    }

    private AdminPublicStatisticResponse toAdminResponse(PublicStatistic stat) {
        return new AdminPublicStatisticResponse(
                stat.getId(),
                stat.getCode(),
                stat.getLabel(),
                stat.getDescription(),
                stat.getValueSource(),
                stat.getManualValue(),
                stat.getSystemMetric(),
                resolveValue(stat),
                stat.getPrefix(),
                stat.getSuffix(),
                stat.getIconKey(),
                stat.isEnabled(),
                stat.isFeatured(),
                stat.getDisplayOrder(),
                stat.getCreatedAt(),
                stat.getUpdatedAt()
        );
    }
}
