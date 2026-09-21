package com.researchassistant.admin;

import com.researchassistant.ai.usage.AiRequestRepository;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.billing.PaymentTransactionRepository;
import com.researchassistant.billing.PaymentTransactionStatus;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.repository.DocumentVersionRepository;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.notification.NotificationDeliveryRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.subscription.WorkspaceSubscriptionRepository;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ResearchProjectRepository projectRepository;
    private final AiRequestRepository aiRequestRepository;
    private final WorkspaceSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ResearchDatasetRepository datasetRepository;
    private final com.researchassistant.ai.usage.AiProviderBudgetService budgetService;
    private final com.researchassistant.ai.usage.AiUsageCostRepository costRepository;
    private final com.researchassistant.ai.usage.AiModelPricingRepository pricingRepository;

    public AdminDashboardService(UserRepository userRepository, WorkspaceRepository workspaceRepository,
                                 ResearchProjectRepository projectRepository, AiRequestRepository aiRequestRepository,
                                 WorkspaceSubscriptionRepository subscriptionRepository,
                                 PaymentTransactionRepository paymentTransactionRepository,
                                 NotificationDeliveryRepository notificationDeliveryRepository,
                                 DocumentRepository documentRepository,
                                 DocumentVersionRepository documentVersionRepository,
                                 ResearchDatasetRepository datasetRepository,
                                 com.researchassistant.ai.usage.AiProviderBudgetService budgetService,
                                 com.researchassistant.ai.usage.AiUsageCostRepository costRepository,
                                 com.researchassistant.ai.usage.AiModelPricingRepository pricingRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.projectRepository = projectRepository;
        this.aiRequestRepository = aiRequestRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.datasetRepository = datasetRepository;
        this.budgetService = budgetService;
        this.costRepository = costRepository;
        this.pricingRepository = pricingRepository;
    }


    public Map<String, Object> dashboard() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime today = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime firstOfMonth = today.withDayOfMonth(1);

        var users = userRepository.findAll();
        long totalUsers = users.size();
        long activeUsers = users.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();

        var aiRequests = aiRequestRepository.findAll();
        long aiRequestsToday = aiRequests.stream().filter(r -> !r.getCreatedAt().isBefore(today)).count();
        long aiRequestsThisMonth = aiRequests.stream().filter(r -> !r.getCreatedAt().isBefore(firstOfMonth)).count();
        long aiTokensTotal = aiRequests.stream().mapToLong(r -> r.getTotalTokens() != null ? r.getTotalTokens() : 0).sum();
        long aiFailures = aiRequests.stream().filter(r -> r.getStatus() == AiRequestStatus.FAILED || r.getStatus() == AiRequestStatus.TIMED_OUT).count();

        var payments = paymentTransactionRepository.findAll();
        long successfulPayments = payments.stream().filter(p -> p.getStatus() == PaymentTransactionStatus.SUCCESS).count();
        long failedPayments = payments.stream().filter(p -> p.getStatus() == PaymentTransactionStatus.FAILED || p.getStatus() == PaymentTransactionStatus.VERIFICATION_FAILED).count();
        long pendingPayments = payments.stream().filter(p -> p.getStatus() == PaymentTransactionStatus.PENDING || p.getStatus() == PaymentTransactionStatus.INITIALIZED).count();

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", activeUsers);
        data.put("workspaces", workspaceRepository.count());
        data.put("researchProjects", projectRepository.count());
        data.put("activeSubscriptions", subscriptionRepository.count());
        data.put("aiRequestsToday", aiRequestsToday);
        data.put("aiRequestsThisMonth", aiRequestsThisMonth);
        data.put("aiTokensTotal", aiTokensTotal);
        data.put("aiFailures", aiFailures);
        data.put("aiBudget", budgetService.getBudgetStatus());
        data.put("aiTotalSpendUsd", costRepository.sumTotalCostByCurrency("USD"));
        data.put("totalStorageBytes", documentVersionRepository.sumTotalFileSizeBytes());
        data.put("aiPricing", pricingRepository.findAll());
        data.put("testPayments", (long) payments.size());
        data.put("successfulPayments", successfulPayments);
        data.put("failedPayments", failedPayments);
        data.put("pendingPayments", pendingPayments);
        data.put("paymentEnvironment", "TEST");
        data.put("documentsCount", documentRepository.count());
        data.put("datasetsCount", datasetRepository.count());
        data.put("notificationDeliveries", notificationDeliveryRepository.count());

        Map<String, String> systemHealth = Map.of(
                "database", "Healthy",
                "aiGeneration", "Healthy",
                "embeddings", "Healthy",
                "paystack", "TEST",
                "storage", "Healthy",
                "email", "Healthy"
        );
        data.put("systemHealth", systemHealth);

        return data;
    }
}
