package com.researchassistant.subscription;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.notification.Notification;
import com.researchassistant.notification.NotificationPriority;
import com.researchassistant.notification.NotificationService;
import com.researchassistant.notification.NotificationType;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SubscriptionRenewalReminderServiceTest {

    private WorkspaceSubscriptionRepository subscriptionRepository;
    private SubscriptionRenewalReminderRepository reminderRepository;
    private NotificationService notificationService;
    private SubscriptionProperties properties;
    private WorkspaceMembershipRepository membershipRepository;
    private FreeSubscriptionProvisioningService freeProvisioningService;
    private AuditEventService auditEventService;

    private SubscriptionRenewalReminderService reminderService;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(WorkspaceSubscriptionRepository.class);
        reminderRepository = mock(SubscriptionRenewalReminderRepository.class);
        notificationService = mock(NotificationService.class);
        membershipRepository = mock(WorkspaceMembershipRepository.class);
        freeProvisioningService = mock(FreeSubscriptionProvisioningService.class);
        auditEventService = mock(AuditEventService.class);

        properties = new SubscriptionProperties(true, List.of(7, 3, 1, 0), "0 0 8 * * *");

        reminderService = new SubscriptionRenewalReminderService(
                subscriptionRepository,
                reminderRepository,
                notificationService,
                properties,
                membershipRepository,
                freeProvisioningService,
                auditEventService
        );
    }

    @Test
    void processRenewalReminders_sends7DayReminderForExpiringPlan() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime periodEnd = now.plusDays(7);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@lab.edu");

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        SubscriptionPlan studentPlan = new SubscriptionPlan();
        studentPlan.setCode("STUDENT");
        studentPlan.setName("Student");

        WorkspaceSubscription sub = new WorkspaceSubscription();
        sub.setId(UUID.randomUUID());
        sub.setWorkspace(workspace);
        sub.setPlan(studentPlan);
        sub.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        sub.setBillingInterval(BillingInterval.MONTHLY);
        sub.setCurrentPeriodEnd(periodEnd);

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        when(reminderRepository.existsBySubscriptionIdAndReminderTypeAndCurrentPeriodEnd(eq(sub.getId()), eq("EXPIRING_7_DAYS"), eq(periodEnd)))
                .thenReturn(false);

        WorkspaceMembership membership = new WorkspaceMembership();
        membership.setUser(owner);
        membership.setRole(WorkspaceRole.OWNER);
        when(membershipRepository.findByWorkspaceIdAndRole(workspace.getId(), WorkspaceRole.OWNER))
                .thenReturn(Optional.of(membership));

        Notification mockNotification = new Notification();
        mockNotification.setId(UUID.randomUUID());
        when(notificationService.create(eq(owner), eq(workspace), isNull(), eq(NotificationType.SUBSCRIPTION_EXPIRING),
                anyString(), anyString(), eq("/app/billing"), eq(NotificationPriority.NORMAL)))
                .thenReturn(mockNotification);

        reminderService.processRenewalRemindersAndExpirations();

        // Verify notification was sent
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(eq(owner), eq(workspace), isNull(), eq(NotificationType.SUBSCRIPTION_EXPIRING),
                eq("Subscription renewal reminder"), messageCaptor.capture(), eq("/app/billing"), eq(NotificationPriority.NORMAL));

        assertThat(messageCaptor.getValue()).contains("Your Student plan expires in 7 days");

        // Verify reminder record was saved
        ArgumentCaptor<SubscriptionRenewalReminder> recordCaptor = ArgumentCaptor.forClass(SubscriptionRenewalReminder.class);
        verify(reminderRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getReminderType()).isEqualTo("EXPIRING_7_DAYS");
        assertThat(recordCaptor.getValue().getSubscription()).isEqualTo(sub);
    }

    @Test
    void processRenewalReminders_isIdempotent_doesNotResendExistingReminder() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime periodEnd = now.plusDays(7);

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        SubscriptionPlan proPlan = new SubscriptionPlan();
        proPlan.setCode("PRO");
        proPlan.setName("Professional");

        WorkspaceSubscription sub = new WorkspaceSubscription();
        sub.setId(UUID.randomUUID());
        sub.setWorkspace(workspace);
        sub.setPlan(proPlan);
        sub.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        sub.setBillingInterval(BillingInterval.MONTHLY);
        sub.setCurrentPeriodEnd(periodEnd);

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        // Simulate that 7-day reminder was ALREADY sent
        when(reminderRepository.existsBySubscriptionIdAndReminderTypeAndCurrentPeriodEnd(eq(sub.getId()), eq("EXPIRING_7_DAYS"), eq(periodEnd)))
                .thenReturn(true);

        reminderService.processRenewalRemindersAndExpirations();

        // Verify no notification was sent
        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void processRenewalReminders_freePlan_neverReceivesRenewalReminder() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime periodEnd = now.plusDays(7);

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        SubscriptionPlan freePlan = new SubscriptionPlan();
        freePlan.setCode("FREE");
        freePlan.setName("Free");

        WorkspaceSubscription sub = new WorkspaceSubscription();
        sub.setId(UUID.randomUUID());
        sub.setWorkspace(workspace);
        sub.setPlan(freePlan);
        sub.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        sub.setBillingInterval(BillingInterval.NONE); // FREE has interval NONE
        sub.setCurrentPeriodEnd(periodEnd);

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));

        reminderService.processRenewalRemindersAndExpirations();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void processExpirations_marksExpiredPaidPlanAndFallsBackToFree() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime pastPeriodEnd = now.minusHours(2); // Expired 2 hours ago

        User owner = new User();
        owner.setId(UUID.randomUUID());

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        SubscriptionPlan proPlan = new SubscriptionPlan();
        proPlan.setCode("PRO");
        proPlan.setName("Professional");

        WorkspaceSubscription sub = new WorkspaceSubscription();
        sub.setId(UUID.randomUUID());
        sub.setWorkspace(workspace);
        sub.setPlan(proPlan);
        sub.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        sub.setBillingInterval(BillingInterval.MONTHLY);
        sub.setCurrentPeriodEnd(pastPeriodEnd);

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));

        WorkspaceMembership membership = new WorkspaceMembership();
        membership.setUser(owner);
        membership.setRole(WorkspaceRole.OWNER);
        when(membershipRepository.findByWorkspaceIdAndRole(workspace.getId(), WorkspaceRole.OWNER))
                .thenReturn(Optional.of(membership));

        reminderService.processRenewalRemindersAndExpirations();

        // Verify marked EXPIRED
        assertThat(sub.getStatus()).isEqualTo(WorkspaceSubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).saveAndFlush(sub);

        // Verify FREE subscription ensured
        verify(freeProvisioningService).ensureFreeSubscription(workspace.getId());

        // Verify expiration notification sent
        verify(notificationService).create(eq(owner), eq(workspace), isNull(), eq(NotificationType.SUBSCRIPTION_EXPIRING),
                eq("Subscription expired"), contains("fallen back to the Free plan"), eq("/app/billing"), eq(NotificationPriority.HIGH));
    }
}
