package com.researchassistant.ai.usage;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AiUsageRecordingService {

    private final AiRequestRepository requestRepository;
    private final AiUsageCostRepository costRepository;
    private final AiProviderCostCalculator costCalculator;
    private final WorkspaceRepository workspaceRepository;
    private final ResearchProjectRepository projectRepository;

    public AiUsageRecordingService(
            AiRequestRepository requestRepository,
            AiUsageCostRepository costRepository,
            AiProviderCostCalculator costCalculator,
            WorkspaceRepository workspaceRepository,
            ResearchProjectRepository projectRepository
    ) {
        this.requestRepository = requestRepository;
        this.costRepository = costRepository;
        this.costCalculator = costCalculator;
        this.workspaceRepository = workspaceRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public <T> AiRequest recordRequest(User user, AiTaskRequest taskRequest, AiTaskResult<T> result) {
        Workspace workspace = workspaceRepository.findById(taskRequest.workspaceId()).orElse(null);
        ResearchProject project = taskRequest.projectId() == null
                ? null
                : projectRepository.findById(taskRequest.projectId()).orElse(null);

        AiRequest record = new AiRequest();
        record.setId(result.requestId());
        record.setUser(user);
        record.setWorkspace(workspace);
        record.setProject(project);
        record.setTaskType(taskRequest.taskType());
        record.setProvider(result.provider());
        record.setModel(result.model());
        record.setStatus(result.status());
        record.setInputTokens(result.inputTokens());
        record.setOutputTokens(result.outputTokens());
        record.setTotalTokens(result.totalTokens());
        record.setCachedInputTokens(result.cachedInputTokens());
        record.setLatencyMs(result.latencyMs());
        record.setProviderRequestId(result.providerRequestId());
        record.setFailureCategory(result.failureCategory());
        record.setFailureCode(result.failureCode());
        record.setStartedAt(result.startedAt());
        record.setCompletedAt(result.completedAt());

        record = requestRepository.save(record);

        calculateAndRecordCost(record);
        return record;
    }

    private void calculateAndRecordCost(AiRequest request) {
        if (request.getInputTokens() == null && request.getOutputTokens() == null) {
            return;
        }

        AiProviderCostCalculator.CostBreakdown breakdown = costCalculator.calculateGenerationCost(
                request.getProvider() != null ? request.getProvider().name() : null,
                request.getModel(),
                request.getInputTokens(),
                request.getOutputTokens(),
                request.getCachedInputTokens()
        );

        AiUsageCost cost = new AiUsageCost();
        cost.setRequest(request);
        cost.setCurrency(breakdown.currency());
        cost.setSource(breakdown.source());
        cost.setPricingVersion(breakdown.pricingVersion());
        cost.setInputCost(breakdown.inputCost());
        cost.setCachedInputCost(breakdown.cachedInputCost());
        cost.setOutputCost(breakdown.outputCost());
        cost.setTotalCost(breakdown.totalCost());

        costRepository.save(cost);
    }
}
