package com.researchassistant.reference;

import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.reference.entity.*;
import com.researchassistant.reference.repository.ReferenceAuthorRepository;
import com.researchassistant.reference.service.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReferenceCoreServiceTests {

    @Test
    void doiNormalizationHandlesCommonPrefixes() {
        ReferenceNormalizationService service = new ReferenceNormalizationService();

        assertThat(service.normalizeDoi("https://doi.org/10.1000/ABC")).isEqualTo("10.1000/abc");
        assertThat(service.normalizeDoi("doi:10.1000/ABC")).isEqualTo("10.1000/abc");
        assertThat(service.normalizeDoi("10.1000/ABC")).isEqualTo("10.1000/abc");
    }

    @Test
    void citationFormatterDoesNotInventMissingMetadata() {
        ReferenceAuthorRepository authors = mock(ReferenceAuthorRepository.class);
        ReferenceEntry entry = new ReferenceEntry();
        entry.setId(UUID.randomUUID());
        entry.setType(ReferenceType.JOURNAL_ARTICLE);
        entry.setTitle("Known title");
        when(authors.findAllByReferenceIdAndRoleOrderByDisplayOrderAsc(entry.getId(), AuthorRole.AUTHOR)).thenReturn(List.of());

        var citation = new CitationFormattingService(authors)
                .format(entry, CitationStyle.APA_7, CitationContext.REFERENCE_LIST, null);

        assertThat(citation.text()).contains("Known title");
        assertThat(citation.warnings()).contains("Missing author.", "Missing publication year.");
        assertThat(citation.metadataComplete()).isFalse();
    }

    @Test
    void risImportAndExportPreserveMajorMetadata() {
        ReferenceInteroperabilityService service = new ReferenceInteroperabilityService(new ReferenceNormalizationService());
        String ris = """
                TY  - JOUR
                AU  - Mensah, Kofi
                PY  - 2025
                TI  - Adoption of AI in Research
                JO  - Journal of Research Systems
                DO  - https://doi.org/10.1000/example
                ER  -
                """;

        var parsed = service.parse(ReferenceImportFormat.RIS, ris);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.getFirst().title()).isEqualTo("Adoption of AI in Research");
        assertThat(parsed.getFirst().authors()).hasSize(1);
        assertThat(parsed.getFirst().doi()).contains("10.1000/example");
    }

    @Test
    void bibtexImportHandlesBracedTitleAndMultipleAuthors() {
        ReferenceInteroperabilityService service = new ReferenceInteroperabilityService(new ReferenceNormalizationService());
        String bib = """
                @article{mensah2025,
                  title = {{AI} Adoption in Research},
                  author = {Mensah, Kofi and Owusu, Ama},
                  journal = {Journal of Research Systems},
                  year = {2025},
                  doi = {10.1000/example}
                }
                """;

        var parsed = service.parse(ReferenceImportFormat.BIBTEX, bib);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.getFirst().title()).contains("AI");
        assertThat(parsed.getFirst().authors()).hasSize(2);
    }

    @Test
    void endnoteXmlRejectsDtdXxePayload() {
        ReferenceInteroperabilityService service = new ReferenceInteroperabilityService(new ReferenceNormalizationService());
        String xxe = """
                <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
                <xml><records><record><titles><title>&xxe;</title></titles></record></records></xml>
                """;

        assertThatThrownBy(() -> service.parse(ReferenceImportFormat.ENDNOTE_XML, xxe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or unsafe");
    }
}
