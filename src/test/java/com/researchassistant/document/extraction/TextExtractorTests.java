package com.researchassistant.document.extraction;

import com.researchassistant.document.entity.DocumentVersion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TextExtractorTests {

    @Test
    void pdfTextIsExtractedPageByPageFromOneBasedPages() throws Exception {
        DocumentVersion version = version("application/pdf");
        ExtractionResult result = new PdfBoxTextExtractor()
                .extract(version, new ByteArrayInputStream(pdfBytes(
                        "First page text",
                        "Second page text"
                )));

        assertThat(result.pages()).hasSize(2);
        assertThat(result.pages().get(0).pageNumber()).isEqualTo(1);
        assertThat(result.pages().get(0).text()).contains("First page text");
        assertThat(result.pages().get(1).pageNumber()).isEqualTo(2);
        assertThat(result.pages().get(1).text()).contains("Second page text");
    }

    @Test
    void emptyPdfHasPagesButNoUsableTextForOcrRequiredPath() throws Exception {
        DocumentVersion version = version("application/pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);

            ExtractionResult result = new PdfBoxTextExtractor()
                    .extract(version, new ByteArrayInputStream(output.toByteArray()));

            assertThat(result.pages()).hasSize(1);
            assertThat(result.pages().getFirst().pageNumber()).isEqualTo(1);
            assertThat(result.pages().getFirst().text()).isBlank();
        }
    }

    @Test
    void txtExtractionUsesLogicalPageOne() {
        DocumentVersion version = version("text/plain");

        ExtractionResult result = new PlainTextExtractor().extract(
                version,
                new ByteArrayInputStream("hello\n\nworld".getBytes(StandardCharsets.UTF_8))
        );

        assertThat(result.pages()).singleElement().satisfies(page -> {
            assertThat(page.pageNumber()).isEqualTo(1);
            assertThat(page.text()).isEqualTo("hello\n\nworld");
        });
    }

    @Test
    void docxExtractionUsesLogicalSectionNotPhysicalPageClaim() throws Exception {
        DocumentVersion version = version(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("DOCX paragraph");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.write(output);

            ExtractionResult result = new DocxTextExtractor()
                    .extract(version, new ByteArrayInputStream(output.toByteArray()));

            assertThat(result.pages()).singleElement().satisfies(page -> {
                assertThat(page.pageNumber()).isEqualTo(1);
                assertThat(page.sourceLabel()).contains("Logical");
                assertThat(page.text()).contains("DOCX paragraph");
            });
        }
    }

    private DocumentVersion version(String mimeType) {
        DocumentVersion version = new DocumentVersion();
        version.setMimeType(mimeType);
        return version;
    }

    private byte[] pdfBytes(String... pageTexts) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream contentStream =
                             new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(font, 12);
                    contentStream.newLineAtOffset(72, 700);
                    contentStream.showText(pageText);
                    contentStream.endText();
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
