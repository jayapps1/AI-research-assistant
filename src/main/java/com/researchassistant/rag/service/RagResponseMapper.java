package com.researchassistant.rag.service;

import com.researchassistant.analysis.entity.CitationPresentation;
import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.rag.citation.UserFacingCitationRenderer;
import com.researchassistant.rag.dto.response.CitationResponse;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.dto.response.RagConversationResponse;
import com.researchassistant.rag.dto.response.RagEvidenceResponse;
import com.researchassistant.rag.dto.response.RagQuerySummaryResponse;
import com.researchassistant.rag.dto.response.RetrievalSummaryResponse;
import com.researchassistant.rag.entity.AnswerCitation;
import com.researchassistant.rag.entity.GroundedAnswer;
import com.researchassistant.rag.entity.RagConversation;
import com.researchassistant.rag.entity.RagQuery;
import com.researchassistant.rag.entity.RagQueryEvidence;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagResponseMapper {

    private final UserFacingCitationRenderer citationRenderer;

    public RagResponseMapper(UserFacingCitationRenderer citationRenderer) {
        this.citationRenderer = citationRenderer;
    }

    public RagConversationResponse conversation(RagConversation conversation) {
        return new RagConversationResponse(
                conversation.getId(),
                conversation.getProject().getId(),
                conversation.getCreatedBy().getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public RagQuerySummaryResponse querySummary(RagQuery query) {
        return new RagQuerySummaryResponse(
                query.getId(),
                query.getQuestion(),
                query.getScopeType(),
                query.getStatus(),
                query.getCreatedAt(),
                query.getCompletedAt()
        );
    }

    public GroundedAnswerResponse answer(
            RagQuery query,
            GroundedAnswer answer,
            List<AnswerCitation> citations,
            int documentCount,
            int evidenceCount
    ) {
        CitationStyle style = query.getConversation().getProject().getCitationStyle();
        CitationPresentation presentation = query.getConversation().getProject().getCitationPresentation();
        return new GroundedAnswerResponse(
                query.getId(),
                query.getConversation().getId(),
                answer == null ? null : citationRenderer.renderAnswer(answer.getAnswerText(), style, presentation, citations),
                query.getStatus(),
                citations.stream().map(citation -> citation(citation, style, presentation)).toList(),
                new RetrievalSummaryResponse(
                        query.getScopeType(),
                        documentCount,
                        evidenceCount,
                        query.getRetrievalMode(),
                        query.getAnalyzedSourceCount(),
                        query.getSourcesWithRelevantEvidenceCount(),
                        query.getCandidateChunkCount(),
                        query.getEstimatedInputTokens(),
                        query.getActualInputTokens(),
                        query.getActualOutputTokens(),
                        query.getContextBudgetTokens(),
                        query.getTruncatedEvidenceCount(),
                        query.getGenerationStrategy(),
                        query.getConfiguredModel()
                ),
                answer == null ? query.getCreatedAt() : answer.getCreatedAt()
        );
    }

    public CitationResponse citation(AnswerCitation citation, CitationStyle style, CitationPresentation presentation) {
        RagQueryEvidence evidence = citation.getEvidence();
        UserFacingCitationRenderer.RenderedCitation rendered = citationRenderer.renderCitation(citation, style, presentation);
        return new CitationResponse(
                citation.getCitationOrdinal(),
                evidence.getDocumentId(),
                evidence.getDocumentCode(),
                evidence.getDocumentTitle(),
                evidence.getDocumentVersionId(),
                evidence.getVersionNumber(),
                evidence.getPageNumber(),
                evidence.getChunkNumber(),
                excerpt(evidence.getTextSnapshot(), 1000),
                rendered.text(),
                rendered.metadataComplete(),
                rendered.warning()
        );
    }

    public RagEvidenceResponse evidence(RagQueryEvidence evidence) {
        return new RagEvidenceResponse(
                evidence.getEvidenceOrdinal(),
                evidence.getDocumentId(),
                evidence.getDocumentCode(),
                evidence.getDocumentTitle(),
                evidence.getDocumentVersionId(),
                evidence.getVersionNumber(),
                evidence.getPageNumber(),
                evidence.getChunkNumber(),
                excerpt(evidence.getTextSnapshot(), 1500),
                evidence.getLexicalScore(),
                evidence.getSemanticScore(),
                evidence.getFusedScore(),
                evidence.getRerankScore()
        );
    }

    private String excerpt(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
