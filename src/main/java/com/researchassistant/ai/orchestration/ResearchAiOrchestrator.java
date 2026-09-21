package com.researchassistant.ai.orchestration;

import com.researchassistant.ai.policy.AiResearchContentPolicyService;
import com.researchassistant.ai.provider.AiGenerationProvider;
import com.researchassistant.ai.provider.DisabledAiGenerationProvider;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.ai.usage.AiUsageRecordingService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.subscription.PlanFeature;
import com.researchassistant.usage.QuotaService;
import com.researchassistant.usage.UsageMetricType;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResearchAiOrchestrator {

    private final ProjectAuthorizationService authorizationService;
    private final AiGenerationProvider generationProvider;
    private final ResearchContextBuilder contextBuilder;
    private final AiResearchContentPolicyService policyService;
    private final AiUsageRecordingService usageRecordingService;
    private final QuotaService quotaService;

    @Autowired
    public ResearchAiOrchestrator(
            ProjectAuthorizationService authorizationService,
            ObjectProvider<AiGenerationProvider> generationProvider,
            ResearchContextBuilder contextBuilder,
            AiResearchContentPolicyService policyService,
            AiUsageRecordingService usageRecordingService,
            QuotaService quotaService
    ) {
        this.authorizationService = authorizationService;
        this.generationProvider = generationProvider.getIfAvailable(DisabledAiGenerationProvider::new);
        this.contextBuilder = contextBuilder;
        this.policyService = policyService;
        this.usageRecordingService = usageRecordingService;
        this.quotaService = quotaService;
    }

    ResearchAiOrchestrator(
            ProjectAuthorizationService authorizationService,
            AiGenerationProvider generationProvider,
            ResearchContextBuilder contextBuilder,
            AiResearchContentPolicyService policyService,
            AiUsageRecordingService usageRecordingService
    ) {
        this(authorizationService, generationProvider, contextBuilder, policyService, usageRecordingService, null);
    }

    ResearchAiOrchestrator(
            ProjectAuthorizationService authorizationService,
            AiGenerationProvider generationProvider,
            ResearchContextBuilder contextBuilder,
            AiResearchContentPolicyService policyService,
            AiUsageRecordingService usageRecordingService,
            QuotaService quotaService
    ) {
        this.authorizationService = authorizationService;
        this.generationProvider = generationProvider == null ? new DisabledAiGenerationProvider() : generationProvider;
        this.contextBuilder = contextBuilder;
        this.policyService = policyService;
        this.usageRecordingService = usageRecordingService;
        this.quotaService = quotaService;
    }

    public <T> AiTaskResult<T> executeTask(
            User user,
            AiTaskRequest request,
            Class<T> responseType
    ) {
        if (request.projectId() != null) {
            authorizationService.requireProjectViewer(request.projectId(), user);
        }

        if (requiresExternalContent(request.taskType())) {
            policyService.validateExternalContentTransmission();
        }
        if (quotaService != null) {
            quotaService.requireWithinQuota(request.workspaceId(), PlanFeature.AI_GENERATION, UsageMetricType.AI_GENERATION_REQUEST, 1L);
        }

        if (!generationProvider.available()) {
            AiTaskResult<T> unavailable = AiTaskResult.unavailable(
                    java.util.UUID.randomUUID(),
                    request.taskType(),
                    "AI generation capability is disabled or unavailable."
            );
            usageRecordingService.recordRequest(user, request, unavailable);
            return unavailable;
        }

        AiTaskResult<T> result = generationProvider.generate(request, responseType);
        usageRecordingService.recordRequest(user, request, result);
        return result;
    }

    private boolean requiresExternalContent(AiTaskType taskType) {
        return switch (taskType) {
            case GROUNDED_QA, LITERATURE_MATRIX_EXTRACTION, LITERATURE_REVIEW_DRAFT, DISCUSSION_DRAFT -> true;
            default -> false;
        };
    }
}
