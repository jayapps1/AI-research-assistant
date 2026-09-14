package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.ReferenceImportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReferenceImportItemRepository extends JpaRepository<ReferenceImportItem, UUID> {
    List<ReferenceImportItem> findAllByImportJobIdOrderByItemOrdinalAsc(UUID importJobId);
}
