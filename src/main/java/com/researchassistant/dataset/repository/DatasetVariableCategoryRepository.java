package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.DatasetVariableCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetVariableCategoryRepository extends JpaRepository<DatasetVariableCategory, UUID> {
    List<DatasetVariableCategory> findAllByVariableIdOrderByDisplayOrderAsc(UUID variableId);
    boolean existsByVariableIdAndCode(UUID variableId, String code);
}
