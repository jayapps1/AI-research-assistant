package com.researchassistant.document.metadata;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.repository.DocumentPageRepository;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.storage.DocumentStorageService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.reference.service.ProjectReferenceRegistryService;
import com.researchassistant.reference.service.ReferenceNormalizationService;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BibliographicMetadataExtractionServiceTest {

    @Test
    void extractedBibliographicTitleDoesNotEraseDisplayTitle() throws Exception {
        Document document = new Document();
        document.setTitle("Digital Agriculture");
        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setUploadedBy(new User());
        BibliographicMetadata metadata = new BibliographicMetadata(
                "High Confidence Scholarly Title",
                "Ada Lovelace",
                2024,
                "Journal of Agricultural Systems",
                null,
                null,
                null,
                null,
                null,
                "10.1234/example",
                null,
                null,
                null,
                "JOURNAL_ARTICLE",
                null,
                "TEST",
                0.95d
        );

        BibliographicMetadataExtractionService service = new BibliographicMetadataExtractionService(
                mock(DocumentRepository.class),
                mock(DocumentPageRepository.class),
                mock(DocumentStorageService.class),
                mock(ReferenceNormalizationService.class),
                mock(ProjectReferenceRegistryService.class),
                List.of()
        );
        Method apply = BibliographicMetadataExtractionService.class
                .getDeclaredMethod("apply", DocumentVersion.class, BibliographicMetadata.class);
        apply.setAccessible(true);

        apply.invoke(service, version, metadata);

        assertThat(document.getTitle()).isEqualTo("Digital Agriculture");
        assertThat(document.getBibliographicTitle()).isEqualTo("High Confidence Scholarly Title");
    }
}
