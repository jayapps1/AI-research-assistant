package com.researchassistant.rag.repository;

import com.researchassistant.rag.entity.AnswerCitation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnswerCitationRepository
        extends JpaRepository<AnswerCitation, UUID> {

    @EntityGraph(attributePaths = {"evidence"})
    List<AnswerCitation> findAllByAnswerIdOrderByCitationOrdinalAsc(UUID answerId);
}
