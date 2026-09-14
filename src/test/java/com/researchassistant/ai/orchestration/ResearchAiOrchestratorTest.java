package com.researchassistant.ai.orchestration;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.fake.FakeAiGenerationProvider;
import com.researchassistant.ai.policy.AiResearchContentPolicyService;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.ai.usage.AiUsageRecordingService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class ResearchAiOrchestratorTest {

    private ProjectAuthorizationService authorizationService;
    private FakeAiGenerationProvider generationProvider;
    private ResearchContextBuilder contextBuilder;
    private AiResearchContentPolicyService policyService;
    private AiUsageRecordingService usageRecordingService;
    private ResearchAiOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        authorizationService = Mockito.mock(ProjectAuthorizationService.class);
        generationProvider = new FakeAiGenerationProvider();
        contextBuilder = Mockito.mock(ResearchContextBuilder.class);
        policyService = Mockito.mock(AiResearchContentPolicyService.class);
        usageRecordingService = Mockito.mock(AiUsageRecordingService.class);

        orchestrator = new ResearchAiOrchestrator(
                authorizationService,
                generationProvider,
                contextBuilder,
                policyService,
                usageRecordingService
        );
    }

    @Test
    void shouldReturnUnavailableStatusWhenProviderDisabled() {
        generationProvider.setAvailable(false);
        User user = new User();
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        AiTaskRequest request = new AiTaskRequest(
                AiTaskType.RESEARCH_PROBLEM_DRAFT,
                user.getId(),
                workspaceId,
                projectId,
                "Draft problem statement",
                "Draft problem statement prompt",
                null,
                null,
                String.class,
                false
        );

        AiTaskResult<String> result = orchestrator.executeTask(user, request, String.class);

        assertThat(result.status()).isEqualTo(AiRequestStatus.CAPABILITY_UNAVAILABLE);
        verify(usageRecordingService).recordRequest(any(), any(), any());
    }
}
