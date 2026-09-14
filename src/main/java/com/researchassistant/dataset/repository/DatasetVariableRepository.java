package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.DatasetVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetVariableRepository extends JpaRepository<DatasetVariable, UUID> {
    List<DatasetVariable> findAllByDatasetIdOrderByDisplayOrderAsc(UUID datasetId);
    boolean existsByDatasetIdAndVariableName(UUID datasetId, String variableName);
}
