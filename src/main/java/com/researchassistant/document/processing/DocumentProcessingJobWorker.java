package com.researchassistant.document.processing;

import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.entity.DocumentVersionStatus;
import com.researchassistant.document.repository.DocumentProcessingJobRepository;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.repository.DocumentVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentProcessingJobWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingJobWorker.class);

    private final DocumentProcessingJobRepository jobRepository;
    private final DocumentProcessingPipelineService pipelineService;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final TransactionTemplate transactionTemplate;

    public DocumentProcessingJobWorker(
            DocumentProcessingJobRepository jobRepository,
            DocumentProcessingPipelineService pipelineService,
            DocumentRepository documentRepository,
            DocumentVersionRepository versionRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.jobRepository = jobRepository;
        this.pipelineService = pipelineService;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${app.document.processing.worker-delay-ms:5000}")
    public void pollAndProcessJobs() {
        int processedCount = 0;
        while (processedCount < 5) {
            try {
                Boolean processed = transactionTemplate.execute(status -> processNextQueuedJob());
                if (processed == null || !processed) {
                    break;
                }
                processedCount++;
            } catch (Exception e) {
                log.error("Error executing processing job iteration: {}", e.getMessage(), e);
                break;
            }
        }
    }

    public boolean processNextQueuedJob() {
        Optional<DocumentProcessingJob> jobOpt = jobRepository.findNextQueuedForUpdateSkipLocked("INGESTION");
        if (jobOpt.isEmpty()) {
            return false;
        }

        DocumentProcessingJob job = jobOpt.get();
        UUID versionId = job.getDocumentVersion() != null ? job.getDocumentVersion().getId() : null;
        if (versionId == null) {
            log.error("Job {} has no document version", job.getId());
            job.setStatus(DocumentProcessingStatus.FAILED);
            job.setErrorCode("NO_VERSION");
            job.setErrorMessage("Job has no document version");
            jobRepository.save(job);
            return true;
        }

        DocumentVersion version = versionRepository.findById(versionId).orElse(null);
        if (version == null) {
            log.error("Document version {} not found for job {}", versionId, job.getId());
            job.setStatus(DocumentProcessingStatus.FAILED);
            job.setErrorCode("VERSION_NOT_FOUND");
            job.setErrorMessage("Document version not found");
            jobRepository.save(job);
            return true;
        }

        String docCode = version.getDocument() != null ? version.getDocument().getDocumentCode() : null;
        log.info("Claimed ingestion job {} for document version {} (document {})",
                job.getId(), version.getId(), docCode);

        job.setStatus(DocumentProcessingStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        jobRepository.saveAndFlush(job);

        try {
            pipelineService.processVersion(version);

            job.setStatus(DocumentProcessingStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
            log.info("Successfully completed ingestion job {} for document {}",
                    job.getId(), docCode);
            return true;
        } catch (Exception e) {
            log.error("Failed ingestion job {} for document {}: {}", job.getId(), docCode, e.getMessage(), e);
            job.setStatus(DocumentProcessingStatus.FAILED);
            job.setErrorCode("INGESTION_FAILED");
            job.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 1900 ? e.getMessage().substring(0, 1900) : e.getMessage());
            job.setFailedAt(OffsetDateTime.now());
            jobRepository.save(job);

            version.setStatus(DocumentVersionStatus.FAILED);
            versionRepository.save(version);
            if (version.getDocument() != null) {
                version.getDocument().setStatus(DocumentStatus.FAILED);
                documentRepository.save(version.getDocument());
            }
            return true;
        }
    }
}
