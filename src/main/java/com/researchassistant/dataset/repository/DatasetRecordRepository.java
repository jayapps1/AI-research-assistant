package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.DatasetRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, UUID> {
    Page<DatasetRecord> findAllByDatasetId(UUID datasetId, Pageable pageable);
    long countByDatasetId(UUID datasetId);
    Optional<DatasetRecord> findFirstByDatasetIdOrderByRowNumberDesc(UUID datasetId);
}
