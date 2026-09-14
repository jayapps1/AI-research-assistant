package com.researchassistant.datacollection.repository;

import com.researchassistant.datacollection.model.DataCollectionSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataCollectionSessionRepository extends JpaRepository<DataCollectionSession, UUID> {
    Page<DataCollectionSession> findAllByProjectId(UUID projectId, Pageable pageable);
    List<DataCollectionSession> findAllByProjectIdAndStatus(UUID projectId, DataCollectionSession.Status status);
    boolean existsByProjectIdAndSessionCode(UUID projectId, String sessionCode);
}
