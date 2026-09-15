package com.researchassistant.document.security;

public interface FileSecurityScanner {
    FileScanStatus scan(byte[] content, String mimeType, String originalFilename);
}
