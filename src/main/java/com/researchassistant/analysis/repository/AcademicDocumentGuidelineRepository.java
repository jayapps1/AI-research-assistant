package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.AcademicDocumentGuideline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicDocumentGuidelineRepository extends JpaRepository<AcademicDocumentGuideline, UUID> {

    List<AcademicDocumentGuideline> findAllByWorkspaceIdOrderByUploadedAtDesc(UUID workspaceId);

    List<AcademicDocumentGuideline> findAllByProjectIdOrderByUploadedAtDesc(UUID projectId);

    Optional<AcademicDocumentGuideline> findFirstByProjectIdAndStatusOrderByVersionDesc(UUID projectId, String status);

    Optional<AcademicDocumentGuideline> findTopByProjectIdOrderByVersionDesc(UUID projectId);
}
