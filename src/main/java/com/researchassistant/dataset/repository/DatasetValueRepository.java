package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.DatasetValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DatasetValueRepository extends JpaRepository<DatasetValue, UUID> {
    List<DatasetValue> findAllByRecordDatasetId(UUID datasetId);
    List<DatasetValue> findAllByRecordIdIn(Collection<UUID> recordIds);
    long countByRecordDatasetId(UUID datasetId);
}
