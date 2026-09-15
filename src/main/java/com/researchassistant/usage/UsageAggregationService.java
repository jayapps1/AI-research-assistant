package com.researchassistant.usage;

import com.researchassistant.ai.usage.AiRequestRepository;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.analysis.repository.ReportExportJobRepository;
import com.researchassistant.document.repository.DocumentVersionRepository;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ResearchProjectStatus;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UsageAggregationService {
    private final AiRequestRepository aiRequestRepository;
    private final UsageLedgerEntryRepository ledgerRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ReportExportJobRepository reportExportJobRepository;
    private final ResearchProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;

    public UsageAggregationService(AiRequestRepository aiRequestRepository,
                                   UsageLedgerEntryRepository ledgerRepository,
                                   DocumentVersionRepository documentVersionRepository,
                                   ReportExportJobRepository reportExportJobRepository,
                                   ResearchProjectRepository projectRepository,
                                   ProjectMembershipRepository membershipRepository) {
        this.aiRequestRepository = aiRequestRepository;
        this.ledgerRepository = ledgerRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.reportExportJobRepository = reportExportJobRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
    }

    public long usage(UUID workspaceId, UsageMetricType metric, OffsetDateTime start, OffsetDateTime end, UUID projectId) {
        return switch (metric) {
            case AI_GENERATION_REQUEST -> aiRequestRepository
                    .countByWorkspaceIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            workspaceId, AiRequestStatus.COMPLETED, start, end);
            case AI_TOTAL_TOKEN -> aiRequestRepository
                    .sumTotalTokensByWorkspaceIdBetweenAndStatus(workspaceId, start, end, AiRequestStatus.COMPLETED);
            case STORAGE_BYTES -> documentVersionRepository.sumFileSizeBytesByWorkspaceId(workspaceId)
                    + reportExportJobRepository.sumFileSizeBytesByWorkspaceId(workspaceId);
            case PROJECT_COUNT -> projectRepository.countByWorkspaceId(workspaceId);
            case ACTIVE_PROJECT_COUNT -> projectRepository.countByWorkspaceIdAndStatus(workspaceId, ResearchProjectStatus.ACTIVE);
            case COLLABORATOR_COUNT -> projectId == null ? 0L : membershipRepository.countByProjectIdAndStatus(projectId, ProjectMembershipStatus.ACTIVE);
            case REPORT_EXPORT -> reportExportJobRepository.countByWorkspaceIdBetween(workspaceId, start, end);
            default -> ledgerRepository.sumQuantity(workspaceId, metric, start, end);
        };
    }
}
