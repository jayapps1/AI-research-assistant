package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.DatasetImportColumnMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetImportColumnMappingRepository extends JpaRepository<DatasetImportColumnMapping, UUID> {
    List<DatasetImportColumnMapping> findAllByJobId(UUID jobId);
    void deleteAllByJobId(UUID jobId);
}
