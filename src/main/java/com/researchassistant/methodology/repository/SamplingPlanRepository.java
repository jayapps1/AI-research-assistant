package com.researchassistant.methodology.repository;

import com.researchassistant.methodology.entity.SamplingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SamplingPlanRepository extends JpaRepository<SamplingPlan, UUID> {
    List<SamplingPlan> findAllByMethodologyId(UUID methodologyId);
}
