package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.DatasetValidationIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DatasetValidationIssueRepository extends JpaRepository<DatasetValidationIssue, UUID> {
    Page<DatasetValidationIssue> findAllByDatasetId(UUID datasetId, Pageable pageable);
    void deleteAllByDatasetId(UUID datasetId);
}
