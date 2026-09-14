package com.researchassistant.ai.policy;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.exception.AiPolicyViolationException;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiResearchContentPolicyServiceTest {

    @Test
    void shouldBlockExternalTransmissionWhenPolicyDisabled() {
        AiProperties properties = new AiProperties(
                null, null, null, new AiProperties.Privacy(false), null, null
        );
        AiResearchContentPolicyService service = new AiResearchContentPolicyService(properties);

        assertThat(service.isExternalContentAllowed()).isFalse();
        assertThatThrownBy(service::validateExternalContentTransmission)
                .isInstanceOf(AiPolicyViolationException.class)
                .hasMessageContaining("External transmission of research content is disabled");
    }

    @Test
    void shouldSanitizeParticipantIdentityData() {
        AiProperties properties = new AiProperties(
                null, null, null, new AiProperties.Privacy(true), null, null
        );
        AiResearchContentPolicyService service = new AiResearchContentPolicyService(properties);

        String raw = "Participant user@example.com phone +12345678901 stated that stress level is high.";
        String sanitized = service.sanitizeParticipantData(raw);

        assertThat(sanitized).doesNotContain("user@example.com");
        assertThat(sanitized).doesNotContain("+12345678901");
        assertThat(sanitized).contains("[REDACTED_EMAIL]");
    }
}
