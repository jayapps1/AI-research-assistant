package com.researchassistant.admin;

import com.researchassistant.ai.usage.AiRequestRepository;
import com.researchassistant.billing.PaymentTransactionRepository;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.notification.NotificationDeliveryRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.subscription.WorkspaceSubscriptionRepository;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
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

    public AdminDashboardService(UserRepository userRepository, WorkspaceRepository workspaceRepository,
                                 ResearchProjectRepository projectRepository, AiRequestRepository aiRequestRepository,
                                 WorkspaceSubscriptionRepository subscriptionRepository,
                                 PaymentTransactionRepository paymentTransactionRepository,
                                 NotificationDeliveryRepository notificationDeliveryRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.projectRepository = projectRepository;
        this.aiRequestRepository = aiRequestRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
    }

    public Map<String, Object> dashboard() {
        OffsetDateTime today = OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        return Map.of(
                "totalUsers", userRepository.count(),
                "workspaces", workspaceRepository.count(),
                "researchProjects", projectRepository.count(),
                "aiRequestsToday", aiRequestRepository.findAll().stream().filter(r -> !r.getCreatedAt().isBefore(today)).count(),
                "activeSubscriptions", subscriptionRepository.count(),
                "testPayments", paymentTransactionRepository.count(),
                "notificationDeliveries", notificationDeliveryRepository.count()
        );
    }
}
