package com.researchassistant.rag.repository;

import com.researchassistant.rag.entity.GroundedAnswer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroundedAnswerRepository
        extends JpaRepository<GroundedAnswer, UUID> {

    Optional<GroundedAnswer> findByQueryId(UUID queryId);
}
