package com.researchassistant.document.extraction;

import com.researchassistant.document.entity.DocumentVersion;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class PlainTextExtractor implements TextExtractor {

    @Override
    public boolean supports(DocumentVersion version) {
        return "text/plain".equalsIgnoreCase(version.getMimeType());
    }

    @Override
    public ExtractionResult extract(DocumentVersion version, InputStream inputStream) {
        try {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return new ExtractionResult(
                    "plain-text",
                    null,
                    List.of(new ExtractedPage(
                            1,
                            "Logical page 1",
                            TextNormalization.normalize(text)
                    ))
            );
        } catch (IOException exception) {
            throw new TextExtractionException("Text extraction failed.", exception);
        }
    }
}
