package com.researchassistant.document.extraction;

import com.researchassistant.document.entity.DocumentVersion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfBoxTextExtractor implements TextExtractor {

    @Override
    public boolean supports(DocumentVersion version) {
        return "application/pdf".equalsIgnoreCase(version.getMimeType());
    }

    @Override
    public ExtractionResult extract(DocumentVersion version, InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                List<ExtractedPage> pages = new ArrayList<>();
                for (int page = 1; page <= document.getNumberOfPages(); page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String text = TextNormalization.normalize(
                            stripper.getText(document)
                    );
                    pages.add(new ExtractedPage(page, "Page " + page, text));
                }
                return new ExtractionResult("pdfbox", pdfBoxVersion(), pages);
            }
        } catch (IOException exception) {
            throw new TextExtractionException("PDF extraction failed.", exception);
        }
    }

    private String pdfBoxVersion() {
        Package pdfboxPackage = PDDocument.class.getPackage();
        return pdfboxPackage == null
                ? null
                : pdfboxPackage.getImplementationVersion();
    }
}
