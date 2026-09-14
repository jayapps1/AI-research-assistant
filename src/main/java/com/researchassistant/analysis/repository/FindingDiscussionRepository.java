package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.FindingDiscussion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FindingDiscussionRepository extends JpaRepository<FindingDiscussion, UUID> {
    List<FindingDiscussion> findAllByFindingIdOrderByDisplayOrderAsc(UUID findingId);
    long countByProjectId(UUID projectId);
    long countByProjectIdAndStatus(UUID projectId, com.researchassistant.analysis.entity.DiscussionStatus status);
}
