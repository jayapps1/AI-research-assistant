package com.researchassistant.document.extraction;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentPage;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentTextExtraction;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.entity.DocumentVersionStatus;
import com.researchassistant.document.entity.TextExtractionStatus;
import com.researchassistant.document.repository.DocumentChunkEmbeddingRepository;
import com.researchassistant.document.repository.DocumentChunkRepository;
import com.researchassistant.document.repository.DocumentPageRepository;
import com.researchassistant.document.repository.DocumentTextExtractionRepository;
import com.researchassistant.document.storage.DocumentStorageObject;
import com.researchassistant.document.storage.DocumentStorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DocumentTextExtractionService {

    private static final int MIN_USABLE_TEXT_CHARACTERS = 20;

    private final List<TextExtractor> extractors;
    private final DocumentStorageService storageService;
    private final DocumentTextExtractionRepository extractionRepository;
    private final DocumentPageRepository pageRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentChunkEmbeddingRepository embeddingRepository;

    public DocumentTextExtractionService(
            List<TextExtractor> extractors,
            DocumentStorageService storageService,
            DocumentTextExtractionRepository extractionRepository,
            DocumentPageRepository pageRepository,
            DocumentChunkRepository chunkRepository,
            DocumentChunkEmbeddingRepository embeddingRepository
    ) {
        this.extractors = extractors;
        this.storageService = storageService;
        this.extractionRepository = extractionRepository;
        this.pageRepository = pageRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
    }

    @Transactional
    public DocumentTextExtraction extract(DocumentVersion version) {
        return extract(version, null);
    }

    @Transactional
    public DocumentTextExtraction extract(
            DocumentVersion version,
            DocumentProcessingJob job
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        if (job != null) {
            job.setStatus(DocumentProcessingStatus.RUNNING);
            job.setStartedAt(now);
        }
        version.setStatus(DocumentVersionStatus.PROCESSING);
        version.getDocument().setStatus(DocumentStatus.PROCESSING);

        embeddingRepository.deleteByChunkDocumentVersionId(version.getId());
        chunkRepository.deleteByDocumentVersionId(version.getId());
        pageRepository.deleteByDocumentVersionId(version.getId());
        chunkRepository.flush();
        pageRepository.flush();

        DocumentTextExtraction extraction = extractionRepository
                .findByDocumentVersionId(version.getId())
                .orElseGet(() -> {
                    DocumentTextExtraction e = new DocumentTextExtraction();
                    e.setDocumentVersion(version);
                    return e;
                });
        extraction.setStatus(TextExtractionStatus.RUNNING);
        extraction.setExtractor("unknown");
        extraction.setStartedAt(now);
        extraction = extractionRepository.saveAndFlush(extraction);

        DocumentStorageObject object = storageService.open(version.getStorageKey());
        try (var inputStream = object.inputStream()) {
            TextExtractor extractor = extractors.stream()
                    .filter(candidate -> candidate.supports(version))
                    .findFirst()
                    .orElseThrow(() -> new TextExtractionException(
                            "No extractor is configured for " + version.getMimeType()
                    ));
            ExtractionResult result = extractor.extract(version, inputStream);

            long totalCharacters = 0L;
            StringBuilder checksumSource = new StringBuilder();
            for (ExtractedPage extractedPage : result.pages()) {
                String normalized = TextNormalization.normalize(extractedPage.text());
                if (normalized.isBlank()) {
                    continue;
                }
                DocumentPage page = new DocumentPage();
                page.setDocumentVersion(version);
                page.setPageNumber(extractedPage.pageNumber());
                page.setSourceLabel(extractedPage.sourceLabel());
                page.setTextContent(normalized);
                page.setCharacterCount(normalized.length());
                page.setContentChecksumSha256(TextNormalization.sha256(normalized));
                pageRepository.save(page);
                totalCharacters += normalized.length();
                checksumSource.append(extractedPage.pageNumber())
                        .append(':')
                        .append(normalized)
                        .append('\n');
            }

            extraction.setExtractor(result.extractor());
            extraction.setExtractorVersion(result.extractorVersion());
            extraction.setPageCount(result.pages().size());
            extraction.setCharacterCount(totalCharacters);
            extraction.setContentChecksumSha256(
                    checksumSource.isEmpty()
                            ? null
                            : TextNormalization.sha256(checksumSource.toString())
            );
            extraction.setCompletedAt(OffsetDateTime.now());

            if (!result.pages().isEmpty()
                    && totalCharacters < MIN_USABLE_TEXT_CHARACTERS) {
                markOcrRequired(version, job, extraction);
            } else {
                extraction.setStatus(TextExtractionStatus.COMPLETED);
                extraction.setOcrRequired(false);
                if (job != null) {
                    job.setStatus(DocumentProcessingStatus.COMPLETED);
                    job.setCompletedAt(OffsetDateTime.now());
                }
            }
            return extraction;
        } catch (Exception exception) {
            markFailed(version, job, extraction, exception);
            return extraction;
        }
    }

    private void markOcrRequired(
            DocumentVersion version,
            DocumentProcessingJob job,
            DocumentTextExtraction extraction
    ) {
        extraction.setStatus(TextExtractionStatus.OCR_REQUIRED);
        extraction.setOcrRequired(true);
        extraction.setFailureCode("OCR_REQUIRED");
        extraction.setFailureMessage("No usable embedded text was extracted.");
        version.setStatus(DocumentVersionStatus.FAILED);
        Document document = version.getDocument();
        if (document.getCurrentVersion() != null
                && document.getCurrentVersion().getId().equals(version.getId())) {
            document.setStatus(DocumentStatus.FAILED);
        }
        if (job != null) {
            job.setStatus(DocumentProcessingStatus.FAILED);
            job.setErrorCode("OCR_REQUIRED");
            job.setErrorMessage("OCR is required before this document can be searched.");
            job.setFailedAt(OffsetDateTime.now());
        }
    }

    private void markFailed(
            DocumentVersion version,
            DocumentProcessingJob job,
            DocumentTextExtraction extraction,
            Exception exception
    ) {
        extraction.setStatus(TextExtractionStatus.FAILED);
        extraction.setFailureCode("TEXT_EXTRACTION_FAILED");
        extraction.setFailureMessage(shortMessage(exception));
        extraction.setCompletedAt(OffsetDateTime.now());
        version.setStatus(DocumentVersionStatus.FAILED);
        Document document = version.getDocument();
        if (document.getCurrentVersion() != null
                && document.getCurrentVersion().getId().equals(version.getId())) {
            document.setStatus(DocumentStatus.FAILED);
        }
        if (job != null) {
            job.setStatus(DocumentProcessingStatus.FAILED);
            job.setErrorCode("TEXT_EXTRACTION_FAILED");
            job.setErrorMessage(shortMessage(exception));
            job.setFailedAt(OffsetDateTime.now());
        }
    }

    private String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
