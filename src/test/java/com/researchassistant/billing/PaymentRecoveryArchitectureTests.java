package com.researchassistant.billing;

import com.researchassistant.billing.dto.BillingDtos;
import com.researchassistant.subscription.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRecoveryArchitectureTests {
    @Test
    void paymentAttemptStatusesKeepPendingSeparateFromFailure() {
        assertThat(PaymentAttemptStatus.PENDING).isNotEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(PaymentAttemptStatus.values()).contains(
                PaymentAttemptStatus.FAILED,
                PaymentAttemptStatus.ABANDONED,
                PaymentAttemptStatus.CANCELLED,
                PaymentAttemptStatus.EXPIRED,
                PaymentAttemptStatus.VERIFICATION_FAILED
        );
    }

    @Test
    void payAgainResponseDoesNotExposeProviderSecrets() {
        assertThat(Arrays.stream(BillingDtos.PaymentAttemptInitializationResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList()).contains("paymentIntentId", "paymentAttemptId", "attemptNumber", "authorizationUrl", "reference", "status")
                .doesNotContain("secretKey", "publicKey", "accessCode");
    }

    @Test
    void newMigrationCreatesIntentAttemptAndComplimentaryTables() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V21__payment_recovery_complimentary_access_jobs_file_security.sql"));
        assertThat(migration).contains("billing_payment_intents", "payment_attempts", "complimentary_access_grants");
        assertThat(migration).contains("next_attempt_number", "payment_idempotency_records");
    }

    @Test
    void complimentaryAccessIsExplicitAndAuditable() {
        assertThat(ComplimentaryAccessType.values()).contains(ComplimentaryAccessType.DEVELOPER_ACCESS, ComplimentaryAccessType.SPONSORED);
        assertThat(SubscriptionAccessSource.values()).contains(SubscriptionAccessSource.COMPLIMENTARY, SubscriptionAccessSource.PAID);
        assertThat(EntitlementLimitMode.values()).contains(EntitlementLimitMode.UNLIMITED, EntitlementLimitMode.DISABLED);
    }
}
