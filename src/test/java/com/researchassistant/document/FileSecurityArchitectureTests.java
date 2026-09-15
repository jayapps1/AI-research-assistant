package com.researchassistant.document;

import com.researchassistant.document.dto.DocumentVersionResponse;
import com.researchassistant.document.security.DisabledFileSecurityScanner;
import com.researchassistant.document.security.FileScanStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FileSecurityArchitectureTests {
    @Test
    void disabledScannerReportsNotScannedRatherThanClean() {
        assertThat(new DisabledFileSecurityScanner().scan(new byte[]{1}, "text/plain", "a.txt"))
                .isEqualTo(FileScanStatus.NOT_SCANNED);
    }

    @Test
    void documentVersionResponseExposesSafeScanState() {
        assertThat(Arrays.stream(DocumentVersionResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList()).contains("scanStatus", "quarantined")
                .doesNotContain("storageKey");
    }
}
