package com.researchassistant.document.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTitleNormalizerTest {

    @Test
    void uploadWithoutExplicitDisplayTitleUsesFilenameStem() {
        assertThat(DocumentTitleNormalizer.displayTitleFromFilename("Digital Agriculture.pdf"))
                .isEqualTo("Digital Agriculture");
    }

    @Test
    void blankDisplayTitleIsRejectedBeforePersistence() {
        assertThatThrownBy(() -> DocumentTitleNormalizer.requireDisplayTitle("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Document title is required.");
    }
}
