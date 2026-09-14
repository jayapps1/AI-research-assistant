package com.researchassistant.methodology.repository;

import com.researchassistant.methodology.entity.SampleSizeCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SampleSizeCalculationRepository extends JpaRepository<SampleSizeCalculation, UUID> {
    List<SampleSizeCalculation> findAllBySamplingPlanIdOrderByCreatedAtDesc(UUID samplingPlanId);
}
