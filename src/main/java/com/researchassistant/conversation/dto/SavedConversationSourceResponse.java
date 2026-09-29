package com.researchassistant.conversation.dto;

import java.util.UUID;

public record SavedConversationSourceResponse(
        UUID sourceId,
        UUID referenceId,
        UUID projectReferenceId,
        String citationKey,
        boolean reusedExistingReference,
        boolean reusedExistingProjectReference
) {
}
