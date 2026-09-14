package com.researchassistant.document.extraction;

import com.researchassistant.document.entity.DocumentVersion;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class LegacyWordTextExtractor implements TextExtractor {

    @Override
    public boolean supports(DocumentVersion version) {
        return "application/msword".equalsIgnoreCase(version.getMimeType());
    }

    @Override
    public ExtractionResult extract(DocumentVersion version, InputStream inputStream) {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return new ExtractionResult(
                    "apache-poi-hwpf",
                    poiVersion(),
                    List.of(new ExtractedPage(
                            1,
                            "Logical section 1",
                            TextNormalization.normalize(extractor.getText())
                    ))
            );
        } catch (IOException exception) {
            throw new TextExtractionException("DOC extraction failed.", exception);
        }
    }

    private String poiVersion() {
        Package poiPackage = HWPFDocument.class.getPackage();
        return poiPackage == null ? null : poiPackage.getImplementationVersion();
    }
}
