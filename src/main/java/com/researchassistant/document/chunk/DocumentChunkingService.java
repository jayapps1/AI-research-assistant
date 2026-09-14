package com.researchassistant.document.chunk;

import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.DocumentPage;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.entity.DocumentVersionStatus;
import com.researchassistant.document.extraction.TextNormalization;
import com.researchassistant.document.repository.DocumentChunkEmbeddingRepository;
import com.researchassistant.document.repository.DocumentChunkRepository;
import com.researchassistant.document.repository.DocumentPageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DocumentChunkingService {

    private final DocumentPageRepository pageRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentChunkEmbeddingRepository embeddingRepository;
    private final DeterministicTextChunker textChunker;

    public DocumentChunkingService(
            DocumentPageRepository pageRepository,
            DocumentChunkRepository chunkRepository,
            DocumentChunkEmbeddingRepository embeddingRepository,
            DeterministicTextChunker textChunker
    ) {
        this.pageRepository = pageRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.textChunker = textChunker;
    }

    @Transactional
    public int chunk(DocumentVersion version, DocumentProcessingJob job) {
        if (job != null) {
            job.setStatus(DocumentProcessingStatus.RUNNING);
            job.setStartedAt(OffsetDateTime.now());
        }
        try {
            embeddingRepository.deleteByChunkDocumentVersionId(version.getId());
            chunkRepository.deleteByDocumentVersionId(version.getId());
            List<DocumentPage> pages =
                    pageRepository.findAllByDocumentVersionIdOrderByPageNumber(
                            version.getId()
                    );
            int chunkNumber = 1;
            for (DocumentPage page : pages) {
                for (ChunkSlice slice : textChunker.chunk(page.getTextContent())) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocumentVersion(version);
                    chunk.setPage(page);
                    chunk.setChunkNumber(chunkNumber++);
                    chunk.setTextContent(slice.text());
                    chunk.setCharacterStart(slice.startInclusive());
                    chunk.setCharacterEnd(slice.endExclusive());
                    chunk.setCharacterCount(slice.text().length());
                    chunk.setEstimatedTokenCount(estimateTokens(slice.text()));
                    chunk.setContentChecksumSha256(TextNormalization.sha256(slice.text()));
                    chunkRepository.save(chunk);
                }
            }
            int generated = chunkNumber - 1;
            if (generated == 0) {
                throw new IllegalStateException("No searchable chunks were generated.");
            }
            version.setStatus(DocumentVersionStatus.READY);
            version.getDocument().setStatus(DocumentStatus.READY);
            if (job != null) {
                job.setStatus(DocumentProcessingStatus.COMPLETED);
                job.setCompletedAt(OffsetDateTime.now());
            }
            return generated;
        } catch (Exception exception) {
            version.setStatus(DocumentVersionStatus.FAILED);
            version.getDocument().setStatus(DocumentStatus.FAILED);
            if (job != null) {
                job.setStatus(DocumentProcessingStatus.FAILED);
                job.setErrorCode("CHUNKING_FAILED");
                job.setErrorMessage(shortMessage(exception));
                job.setFailedAt(OffsetDateTime.now());
            }
            throw exception;
        }
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
