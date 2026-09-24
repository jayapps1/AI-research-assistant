package com.researchassistant.document.metadata;

import java.util.Optional;

public interface BibliographicMetadataProvider {
    String name();
    boolean available();
    Optional<BibliographicMetadata> resolve(BibliographicMetadata seed);
}
