package com.researchassistant.methodology.repository;

import com.researchassistant.methodology.entity.StudyPopulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StudyPopulationRepository extends JpaRepository<StudyPopulation, UUID> {
    List<StudyPopulation> findAllByMethodologyId(UUID methodologyId);
}
