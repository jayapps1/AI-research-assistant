package com.researchassistant.datacollection.repository;

import com.researchassistant.datacollection.model.InstrumentResponseOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InstrumentResponseOptionRepository extends JpaRepository<InstrumentResponseOption, UUID> {}
