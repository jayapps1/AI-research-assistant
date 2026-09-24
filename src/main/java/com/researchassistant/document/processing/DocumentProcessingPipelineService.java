package com.researchassistant.document.processing;

import com.researchassistant.document.chunk.DocumentChunkingService;
import com.researchassistant.document.embedding.DocumentEmbeddingService;
import com.researchassistant.document.metadata.BibliographicMetadataExtractionService;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingJobType;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.extraction.DocumentTextExtractionService;
import com.researchassistant.document.repository.DocumentProcessingJobRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class DocumentProcessingPipelineService {

    private final DocumentProcessingJobRepository jobRepository;
    private final DocumentTextExtractionService extractionService;
    private final BibliographicMetadataExtractionService metadataExtractionService;
    private final DocumentChunkingService chunkingService;
    private final DocumentEmbeddingService embeddingService;

    public DocumentProcessingPipelineService(
            DocumentProcessingJobRepository jobRepository,
            DocumentTextExtractionService extractionService,
            BibliographicMetadataExtractionService metadataExtractionService,
            DocumentChunkingService chunkingService,
            DocumentEmbeddingService embeddingService
    ) {
        this.jobRepository = jobRepository;
        this.extractionService = extractionService;
        this.metadataExtractionService = metadataExtractionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public void processVersion(DocumentVersion version) {
        if (version.isQuarantined()) {
            throw new IllegalStateException("Quarantined documents cannot be processed.");
        }
        DocumentProcessingJob extractionJob =
                createJob(version, DocumentProcessingJobType.TEXT_EXTRACTION);
        extractionService.extract(version, extractionJob);
        if (extractionJob.getStatus() != DocumentProcessingStatus.COMPLETED) {
            return;
        }

        DocumentProcessingJob metadataJob =
                createJob(version, DocumentProcessingJobType.REFERENCE_METADATA_EXTRACTION);
        metadataExtractionService.extractAndApply(version, metadataJob);

        DocumentProcessingJob chunkingJob =
                createJob(version, DocumentProcessingJobType.CHUNKING);
        chunkingService.chunk(version, chunkingJob);
        if (chunkingJob.getStatus() != DocumentProcessingStatus.COMPLETED) {
            return;
        }

        DocumentProcessingJob embeddingJob =
                createJob(version, DocumentProcessingJobType.EMBEDDING);
        embeddingService.embed(embeddingJob);
    }

    @Transactional
    public DocumentProcessingJob createJob(
            DocumentVersion version,
            DocumentProcessingJobType type
    ) {
        long previousAttempts = jobRepository.countByDocumentVersionIdAndType(
                version.getId(),
                type
        );
        DocumentProcessingJob job = new DocumentProcessingJob();
        job.setDocumentVersion(version);
        job.setType(type);
        job.setStatus(DocumentProcessingStatus.QUEUED);
        job.setAttemptNumber(Math.toIntExact(previousAttempts + 1));
        job.setQueuedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }
}
