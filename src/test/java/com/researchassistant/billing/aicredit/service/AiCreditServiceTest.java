package com.researchassistant.billing.aicredit.service;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.billing.aicredit.entity.*;
import com.researchassistant.billing.aicredit.exception.AiCreditsExhaustedException;
import com.researchassistant.billing.aicredit.repository.AiCreditLedgerEntryRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditPackRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditWalletRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.subscription.EntitlementService;
import com.researchassistant.usage.UsageLedgerEntryRepository;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiCreditServiceTest {

    @Mock
    private AiCreditWalletRepository walletRepository;
    @Mock
    private AiCreditLedgerEntryRepository ledgerRepository;
    @Mock
    private AiCreditPackRepository packRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private EntitlementService entitlementService;
    @Mock
    private UsageLedgerEntryRepository usageLedgerRepository;
    @Mock
    private AuditEventService auditEventService;

    private AiCreditService creditService;
    private UUID workspaceId;
    private AiCreditWallet wallet;

    @BeforeEach
    void setUp() {
        creditService = new AiCreditService(
                walletRepository,
                ledgerRepository,
                packRepository,
                workspaceRepository,
                entitlementService,
                usageLedgerRepository,
                auditEventService
        );
        workspaceId = UUID.randomUUID();
        com.researchassistant.workspace.entity.Workspace ws = new com.researchassistant.workspace.entity.Workspace();
        ws.setId(workspaceId);
        wallet = new AiCreditWallet();
        wallet.setId(UUID.randomUUID());
        wallet.setWorkspace(ws);
        wallet.setPurchasedBalance(new BigDecimal("100.00"));
        wallet.setPromotionalBalance(new BigDecimal("20.00"));
        wallet.setReservedBalance(BigDecimal.ZERO);
    }

    @Test
    void reserveCreditsThrows402ExceptionWhenInsufficientBalance() {
        when(walletRepository.findByWorkspaceIdForUpdate(workspaceId)).thenReturn(Optional.of(wallet));

        com.researchassistant.subscription.WorkspaceSubscription sub = new com.researchassistant.subscription.WorkspaceSubscription();
        sub.setBillingInterval(com.researchassistant.subscription.BillingInterval.MONTHLY);
        sub.setCurrentPeriodStart(java.time.OffsetDateTime.now().minusDays(5));
        sub.setCurrentPeriodEnd(java.time.OffsetDateTime.now().plusDays(25));
        when(entitlementService.effectiveSubscription(workspaceId)).thenReturn(sub);
        when(entitlementService.getEntitlement(eq(workspaceId), eq(com.researchassistant.subscription.PlanFeature.AI_GENERATION_CREDITS_MONTHLY)))
                .thenReturn(new com.researchassistant.subscription.Entitlement(com.researchassistant.subscription.PlanFeature.AI_GENERATION_CREDITS_MONTHLY, false, null, null));
        when(entitlementService.getEntitlement(eq(workspaceId), eq(com.researchassistant.subscription.PlanFeature.AI_GENERATION)))
                .thenReturn(new com.researchassistant.subscription.Entitlement(com.researchassistant.subscription.PlanFeature.AI_GENERATION, false, null, null));

        // Available: 100 + 20 = 120 credits. Requesting 150 credits.
        assertThatThrownBy(() -> creditService.reserveCredits(workspaceId, new BigDecimal("150.00")))
                .isInstanceOf(AiCreditsExhaustedException.class)
                .satisfies(ex -> {
                    AiCreditsExhaustedException aee = (AiCreditsExhaustedException) ex;
                    assertThat(aee.getTotalAvailable()).isEqualByComparingTo(new BigDecimal("120.00"));
                });
    }

    @Test
    void reserveCreditsSucceedsAndIncrementsReservedCredits() {
        when(walletRepository.findByWorkspaceIdForUpdate(workspaceId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(AiCreditWallet.class))).thenAnswer(i -> i.getArgument(0));

        com.researchassistant.subscription.WorkspaceSubscription sub = new com.researchassistant.subscription.WorkspaceSubscription();
        sub.setBillingInterval(com.researchassistant.subscription.BillingInterval.MONTHLY);
        sub.setCurrentPeriodStart(java.time.OffsetDateTime.now().minusDays(5));
        sub.setCurrentPeriodEnd(java.time.OffsetDateTime.now().plusDays(25));
        when(entitlementService.effectiveSubscription(workspaceId)).thenReturn(sub);
        when(entitlementService.getEntitlement(eq(workspaceId), eq(com.researchassistant.subscription.PlanFeature.AI_GENERATION_CREDITS_MONTHLY)))
                .thenReturn(new com.researchassistant.subscription.Entitlement(com.researchassistant.subscription.PlanFeature.AI_GENERATION_CREDITS_MONTHLY, false, null, null));
        when(entitlementService.getEntitlement(eq(workspaceId), eq(com.researchassistant.subscription.PlanFeature.AI_GENERATION)))
                .thenReturn(new com.researchassistant.subscription.Entitlement(com.researchassistant.subscription.PlanFeature.AI_GENERATION, false, null, null));

        AiCreditReservation reservation = creditService.reserveCredits(workspaceId, new BigDecimal("10.00"));

        assertThat(reservation).isNotNull();
        assertThat(reservation.workspaceId()).isEqualTo(workspaceId);
        assertThat(reservation.estimatedCredits()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void adminGrantCreditsUpdatesWalletAndRecordsLedger() {
        when(walletRepository.findByWorkspaceIdForUpdate(workspaceId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(AiCreditWallet.class))).thenAnswer(i -> i.getArgument(0));

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setFirstName("Admin");
        admin.setLastName("User");

        creditService.adminGrant(workspaceId, new BigDecimal("50.00"), AiCreditBucket.PROMOTIONAL, "Beta tester grant", admin);

        assertThat(wallet.getPromotionalBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        verify(ledgerRepository).save(any(AiCreditLedgerEntry.class));
    }
}
