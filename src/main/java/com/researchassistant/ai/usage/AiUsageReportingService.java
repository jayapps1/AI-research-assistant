package com.researchassistant.ai.usage;

import com.researchassistant.ai.usage.dto.AiUsageSummaryResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AiUsageReportingService {

    private final AiRequestRepository requestRepository;

    public AiUsageReportingService(AiRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public AiUsageSummaryResponse getUserUsageSummary(UUID userId) {
        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime startOfMonth = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        long requestsToday = requestRepository.countByUserIdAndCreatedAtAfter(userId, startOfDay);
        long requestsThisMonth = requestRepository.countByUserIdAndCreatedAtAfter(userId, startOfMonth);
        long tokensThisMonth = requestRepository.sumTotalTokensByUserIdSince(userId, startOfMonth);
        long failedRequestsThisMonth = requestRepository.countByUserIdAndStatusAndCreatedAtAfter(
                userId, AiRequestStatus.FAILED, startOfMonth
        );

        return new AiUsageSummaryResponse(
                userId,
                null,
                null,
                requestsToday,
                requestsThisMonth,
                tokensThisMonth,
                failedRequestsThisMonth
        );
    }

    @Transactional(readOnly = true)
    public AiUsageSummaryResponse getWorkspaceUsageSummary(UUID workspaceId) {
        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime startOfMonth = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        long requestsToday = requestRepository.countByWorkspaceIdAndCreatedAtAfter(workspaceId, startOfDay);
        long requestsThisMonth = requestRepository.countByWorkspaceIdAndCreatedAtAfter(workspaceId, startOfMonth);
        long tokensThisMonth = requestRepository.sumTotalTokensByWorkspaceIdSince(workspaceId, startOfMonth);

        return new AiUsageSummaryResponse(
                null,
                workspaceId,
                null,
                requestsToday,
                requestsThisMonth,
                tokensThisMonth,
                0L
        );
    }
}
