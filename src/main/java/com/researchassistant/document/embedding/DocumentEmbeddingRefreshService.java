package com.researchassistant.document.embedding;

import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingJobType;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.repository.DocumentChunkRepository;
import com.researchassistant.document.repository.DocumentProcessingJobRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentEmbeddingRefreshService {

    private final DocumentChunkRepository chunkRepository;
    private final DocumentEmbeddingService embeddingService;
    private final DocumentProcessingJobRepository jobRepository;

    public DocumentEmbeddingRefreshService(
            DocumentChunkRepository chunkRepository,
            DocumentEmbeddingService embeddingService,
            DocumentProcessingJobRepository jobRepository
    ) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public int refreshEmbeddingsForVersion(DocumentVersion version) {
        if (!embeddingService.enabledAndAvailable()) {
            return 0;
        }

        DocumentProcessingJob job = new DocumentProcessingJob();
        job.setDocumentVersion(version);
        job.setType(DocumentProcessingJobType.EMBEDDING);
        job.setStatus(DocumentProcessingStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        jobRepository.save(job);

        try {
            int embeddedCount = embeddingService.embed(job);
            return embeddedCount;
        } catch (Exception e) {
            job.setStatus(DocumentProcessingStatus.FAILED);
            job.setErrorCode("REEMBEDDING_FAILED");
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            job.setFailedAt(OffsetDateTime.now());
            jobRepository.save(job);
            throw e;
        }
    }
}
