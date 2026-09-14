package com.researchassistant.document.extraction;

import com.researchassistant.document.entity.DocumentVersion;

import java.io.InputStream;

public interface TextExtractor {

    boolean supports(DocumentVersion version);

    ExtractionResult extract(DocumentVersion version, InputStream inputStream);
}
