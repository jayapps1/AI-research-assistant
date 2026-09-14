package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.ReferenceImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReferenceImportJobRepository extends JpaRepository<ReferenceImportJob, UUID> {
}
