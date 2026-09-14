package com.researchassistant.document.embedding;

import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.DocumentChunkEmbedding;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.EmbeddingStatus;
import com.researchassistant.document.repository.DocumentChunkEmbeddingRepository;
import com.researchassistant.document.repository.DocumentChunkRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentEmbeddingService {

    private final DocumentChunkRepository chunkRepository;
    private final DocumentChunkEmbeddingRepository embeddingRepository;
    private final DocumentEmbeddingProvider provider;
    private final EmbeddingProperties properties;

    public DocumentEmbeddingService(
            DocumentChunkRepository chunkRepository,
            DocumentChunkEmbeddingRepository embeddingRepository,
            DocumentEmbeddingProvider provider,
            EmbeddingProperties properties
    ) {
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.provider = provider;
        this.properties = properties;
    }

    public boolean enabledAndAvailable() {
        return properties.enabled() && provider.available();
    }

    @Transactional
    public int embed(DocumentProcessingJob job) {
        if (!enabledAndAvailable()) {
            if (job != null) {
                job.setStatus(DocumentProcessingStatus.COMPLETED);
                job.setCompletedAt(OffsetDateTime.now());
                job.setErrorCode("EMBEDDINGS_DISABLED");
                job.setErrorMessage("Embeddings are disabled or no provider is available.");
            }
            return 0;
        }
        if (job != null) {
            job.setStatus(DocumentProcessingStatus.RUNNING);
            job.setStartedAt(OffsetDateTime.now());
        }
        try {
            List<DocumentChunk> chunks = chunkRepository
                    .findAllByDocumentVersionIdOrderByChunkNumber(
                            job.getDocumentVersion().getId()
                    );
            int saved = 0;
            int batchSize = Math.max(1, properties.batchSize());
            for (int start = 0; start < chunks.size(); start += batchSize) {
                List<DocumentChunk> batch =
                        chunks.subList(start, Math.min(chunks.size(), start + batchSize));
                List<EmbeddingVector> vectors =
                        provider.embed(batch.stream().map(DocumentChunk::getTextContent).toList());
                List<DocumentChunkEmbedding> embeddings = new ArrayList<>();
                for (int i = 0; i < batch.size(); i++) {
                    DocumentChunk chunk = batch.get(i);
                    EmbeddingVector vector = vectors.get(i);
                    DocumentChunkEmbedding embedding = embeddingRepository
                            .findByChunkIdAndProviderAndModel(
                                    chunk.getId(),
                                    provider.provider(),
                                    provider.model()
                            )
                            .orElseGet(DocumentChunkEmbedding::new);
                    embedding.setChunk(chunk);
                    embedding.setProvider(provider.provider());
                    embedding.setModel(provider.model());
                    embedding.setDimensions(vector.dimensions());
                    embedding.setChunkChecksumSha256(chunk.getContentChecksumSha256());
                    embedding.setStatus(EmbeddingStatus.COMPLETED);
                    embedding.setVectorValues(vector.values());
                    embedding.setEmbeddedAt(OffsetDateTime.now());
                    embeddings.add(embedding);
                }
                embeddingRepository.saveAll(embeddings);
                saved += embeddings.size();
            }
            if (job != null) {
                job.setStatus(DocumentProcessingStatus.COMPLETED);
                job.setCompletedAt(OffsetDateTime.now());
            }
            return saved;
        } catch (Exception exception) {
            if (job != null) {
                job.setStatus(DocumentProcessingStatus.FAILED);
                job.setErrorCode("EMBEDDING_FAILED");
                job.setErrorMessage(shortMessage(exception));
                job.setFailedAt(OffsetDateTime.now());
            }
            throw exception;
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
