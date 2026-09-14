package com.researchassistant.methodology.repository;

import com.researchassistant.methodology.entity.DataCollectionMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DataCollectionMethodRepository extends JpaRepository<DataCollectionMethod, UUID> {
    List<DataCollectionMethod> findAllByMethodologyIdOrderByDisplayOrderAsc(UUID methodologyId);
    long countByMethodologyProjectId(UUID projectId);
}
