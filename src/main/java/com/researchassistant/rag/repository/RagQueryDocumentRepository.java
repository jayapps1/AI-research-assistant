package com.researchassistant.rag.repository;

import com.researchassistant.rag.entity.RagQueryDocument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RagQueryDocumentRepository
        extends JpaRepository<RagQueryDocument, RagQueryDocument.IdKey> {

    List<RagQueryDocument> findAllByQueryId(UUID queryId);
}
