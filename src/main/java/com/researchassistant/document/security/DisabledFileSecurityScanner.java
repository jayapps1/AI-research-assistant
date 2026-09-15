package com.researchassistant.document.security;

import org.springframework.stereotype.Component;

@Component
public class DisabledFileSecurityScanner implements FileSecurityScanner {
    @Override
    public FileScanStatus scan(byte[] content, String mimeType, String originalFilename) {
        return FileScanStatus.NOT_SCANNED;
    }
}
