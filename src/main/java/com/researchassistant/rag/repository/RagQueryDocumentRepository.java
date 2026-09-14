package com.researchassistant.rag.repository;

import com.researchassistant.rag.entity.RagQueryDocument;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RagQueryDocumentRepository
        extends JpaRepository<RagQueryDocument, RagQueryDocument.IdKey> {
}
