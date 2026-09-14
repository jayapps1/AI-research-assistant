package com.researchassistant.framework.repository;

import com.researchassistant.framework.entity.TheoreticalFramework;
import com.researchassistant.framework.entity.TheoreticalFrameworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TheoreticalFrameworkRepository extends JpaRepository<TheoreticalFramework, UUID> {
    List<TheoreticalFramework> findAllByProjectIdOrderByRevisionNumberDesc(UUID projectId);
    Optional<TheoreticalFramework> findByProjectIdAndStatus(UUID projectId, TheoreticalFrameworkStatus status);
}
