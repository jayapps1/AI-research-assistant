package com.researchassistant.framework.repository;

import com.researchassistant.framework.entity.TheoreticalFrameworkTheory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TheoreticalFrameworkTheoryRepository extends JpaRepository<TheoreticalFrameworkTheory, UUID> {
    List<TheoreticalFrameworkTheory> findAllByFrameworkIdOrderByDisplayOrderAsc(UUID frameworkId);
}
