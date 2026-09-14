package com.researchassistant.rag.scope;

import java.util.UUID;

public record RetrievalScopeDocument(
        UUID documentId,
        UUID documentVersionId
) {
}
