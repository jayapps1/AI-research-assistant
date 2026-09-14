package com.researchassistant.document.repository;

import com.researchassistant.document.entity.DocumentPage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentPageRepository extends JpaRepository<DocumentPage, UUID> {

    List<DocumentPage> findAllByDocumentVersionIdOrderByPageNumber(UUID versionId);

    void deleteByDocumentVersionId(UUID versionId);
}
