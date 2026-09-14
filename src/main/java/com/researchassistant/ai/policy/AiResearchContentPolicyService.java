package com.researchassistant.ai.policy;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.exception.AiPolicyViolationException;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AiResearchContentPolicyService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{10,15}");

    private final AiProperties properties;

    public AiResearchContentPolicyService(AiProperties properties) {
        this.properties = properties;
    }

    public boolean isExternalContentAllowed() {
        return properties.privacy().externalResearchContentEnabled();
    }

    public void validateExternalContentTransmission() {
        if (!isExternalContentAllowed()) {
            throw new AiPolicyViolationException(
                    "External transmission of research content is disabled by privacy policy."
            );
        }
    }

    public String sanitizeParticipantData(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String sanitized = EMAIL_PATTERN.matcher(input).replaceAll("[REDACTED_EMAIL]");
        return PHONE_PATTERN.matcher(sanitized).replaceAll("[REDACTED_PHONE]");
    }
}
