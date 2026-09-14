package com.researchassistant.datacollection.repository;

import com.researchassistant.datacollection.model.InstrumentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstrumentResponseRepository extends JpaRepository<InstrumentResponse, UUID> {
    List<InstrumentResponse> findAllBySessionId(UUID sessionId);
}
