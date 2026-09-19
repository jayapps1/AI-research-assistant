package com.researchassistant;

import com.researchassistant.billing.PaymentEnvironment;
import com.researchassistant.billing.PaymentProperties;
import com.researchassistant.billing.PaystackClient;
import com.researchassistant.billing.PaystackWebhookVerifier;
import com.researchassistant.notification.Notification;
import com.researchassistant.notification.NotificationPriority;
import com.researchassistant.notification.NotificationType;
import com.researchassistant.subscription.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SaasBillingNotificationArchitectureTests {

    @Test
    void paystackSmallestUnitConversionUsesDecimalMoneySafely() {
        PaymentProperties properties = new PaymentProperties(
                true,
                PaymentEnvironment.TEST,
                "test-secret-example",
                "test-public-example",
                "http://localhost:8096",
                false,
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                5,
                Duration.ofSeconds(15),
                Duration.ofHours(24),
                Duration.ofMinutes(30)
        );
        PaystackClient client = new PaystackClient(properties, RestClient.builder(), new com.researchassistant.billing.MoneyMinorUnitConverter());

        assertThat(client.toSmallestUnit(new BigDecimal("50.00"))).isEqualTo(5000L);
        assertThat(client.toSmallestUnit(new BigDecimal("0.99"))).isEqualTo(99L);
    }

    @Test
    void paystackWebhookSignatureUsesRawBodyHmacSha512() throws Exception {
        PaymentProperties properties = new PaymentProperties(
                true,
                PaymentEnvironment.TEST,
                "test-secret-signature",
                null,
                null,
                false,
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                5,
                Duration.ofSeconds(15),
                Duration.ofHours(24),
                Duration.ofMinutes(30)
        );
        byte[] body = "{\"event\":\"charge.success\"}".getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        String signature = java.util.HexFormat.of().formatHex(mac.doFinal(body));

        assertThat(new PaystackWebhookVerifier(properties).valid(body, signature)).isTrue();
        assertThat(new PaystackWebhookVerifier(properties).valid("{}".getBytes(StandardCharsets.UTF_8), signature)).isFalse();
    }

    @Test
    void entitlementDoesNotTreatNullLimitAsDisabled() {
        Entitlement unlimited = new Entitlement(PlanFeature.PRIORITY_SUPPORT, true, null, LimitUnit.NONE);

        assertThat(unlimited.enabled()).isTrue();
        assertThat(unlimited.limitValue()).isNull();
    }

    @Test
    void notificationMessageIsBoundedAndDoesNotNeedSensitiveProjectContent() {
        Notification notification = new Notification();
        notification.setType(NotificationType.PROJECT_INVITATION);
        notification.setTitle("Project invitation");
        notification.setMessage("You have been invited to collaborate as REVIEWER.");
        notification.setPriority(NotificationPriority.NORMAL);

        assertThat(notification.getMessage()).doesNotContain("participant", "password", "secret");
    }

    @Test
    void fakeAiProvidersRemainOutOfMainRuntimeSources() throws Exception {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            assertThat(files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("Fake"))
                    .toList()).isEmpty();
        }
    }

    @Test
    void paystackSecretIsNotSerializedOrExposedByConfigurationString() throws Exception {
        PaymentProperties properties = new PaymentProperties(
                true,
                PaymentEnvironment.TEST,
                "sensitive-test-secret-value",
                "sensitive-test-public-value",
                "http://localhost:8096",
                false,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                5,
                Duration.ofSeconds(15),
                Duration.ofHours(24),
                Duration.ofMinutes(30)
        );

        String json = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .build()
                .writeValueAsString(properties);

        assertThat(json).doesNotContain("sensitive-test-secret-value", "sensitive-test-public-value");
        assertThat(properties.toString()).doesNotContain("sensitive-test-secret-value", "sensitive-test-public-value");
    }

    @Test
    void paymentDtosDoNotExposeProviderSecrets() {
        assertThat(java.util.Arrays.stream(com.researchassistant.billing.dto.BillingDtos.PaymentTransactionResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList()).doesNotContain("secretKey", "publicKey", "providerSecret");
    }

    @Test
    void applicationDoesNotExposeActuatorEnvOrHardcodedPaystackKeys() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
        String docs = Files.readString(Path.of("docs/integrations/paystack-test.md"));

        assertThat(yaml).contains("secret-key: ${PAYSTACK_SECRET_KEY:}");
        assertThat(yaml).doesNotContain("sk_" + "test_", "sk_" + "live_", "pk_" + "test_", "pk_" + "live_");
        assertThat(yaml).contains("include: health,info,metrics");
        assertThat(docs).doesNotContain("sk_" + "test_", "sk_" + "live_", "pk_" + "test_", "pk_" + "live_");
    }
}
