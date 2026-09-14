package com.researchassistant.document.extraction;

import com.researchassistant.document.entity.DocumentVersion;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class DocxTextExtractor implements TextExtractor {

    private static final String DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public boolean supports(DocumentVersion version) {
        return DOCX.equalsIgnoreCase(version.getMimeType());
    }

    @Override
    public ExtractionResult extract(DocumentVersion version, InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return new ExtractionResult(
                    "apache-poi-xwpf",
                    poiVersion(),
                    List.of(new ExtractedPage(
                            1,
                            "Logical section 1",
                            TextNormalization.normalize(extractor.getText())
                    ))
            );
        } catch (IOException exception) {
            throw new TextExtractionException("DOCX extraction failed.", exception);
        }
    }

    private String poiVersion() {
        Package poiPackage = XWPFDocument.class.getPackage();
        return poiPackage == null ? null : poiPackage.getImplementationVersion();
    }
}
