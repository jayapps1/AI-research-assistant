package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.DatasetImportJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DatasetImportJobRepository extends JpaRepository<DatasetImportJob, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from DatasetImportJob j where j.id = :id")
    Optional<DatasetImportJob> findByIdForUpdate(@Param("id") UUID id);
}
