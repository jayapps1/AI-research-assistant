package com.researchassistant.rag.generation;

import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.evidence.EvidenceItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SourceGroundedFallbackSynthesizerTest {

    private final SourceGroundedFallbackSynthesizer synthesizer = new SourceGroundedFallbackSynthesizer();

    private EvidenceBundle createBundle(String query) {
        EvidenceItem item1 = new EvidenceItem(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DOC-001",
                "Agricultural Technology Trends 2024.pdf",
                UUID.randomUUID(),
                1,
                1,
                2,
                "Adoption of cloud platforms in sub-Saharan agriculture remains restricted by infrastructure gaps and fragmented advisory channels.",
                1.0,
                0.8,
                0.9,
                0.95,
                1
        );
        EvidenceItem item2 = new EvidenceItem(
                UUID.randomUUID(),
                2,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DOC-002",
                "Farm Management Systems Review.pdf",
                UUID.randomUUID(),
                1,
                3,
                4,
                "Empirical evaluations indicate that modular software designs improve operational uptime and user satisfaction across farming cooperatives.",
                0.95,
                0.75,
                0.85,
                0.9,
                2
        );
        return new EvidenceBundle(
                query,
                null,
                List.of(item1, item2),
                2,
                2,
                2,
                2,
                128000,
                100,
                0,
                "HYBRID",
                "text-embedding-3-small",
                "NoOpEvidenceReranker",
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Synthesizer produces distinct, purpose-specific structures with no generic 'Overview and Thematic Context' for non-literature sections")
    void testPurposeSpecificSynthesis() {
        String bgPrompt = """
                SEMANTIC PURPOSE: BACKGROUND
                TARGET SECTION: Background of the Study
                TARGET CHAPTER: Chapter One: Introduction
                WORKSPACE TYPE: ACADEMIC_RESEARCH
                PROJECT TYPE: GENERAL_ACADEMIC_PROJECT
                Project Title: Digital Agriculture Platform
                """;

        String problemPrompt = """
                SEMANTIC PURPOSE: PROBLEM_STATEMENT
                TARGET SECTION: Problem Statement
                TARGET CHAPTER: Chapter One: Introduction
                WORKSPACE TYPE: ACADEMIC_RESEARCH
                PROJECT TYPE: GENERAL_ACADEMIC_PROJECT
                Project Title: Digital Agriculture Platform
                """;

        String litPrompt = """
                SEMANTIC PURPOSE: LITERATURE_REVIEW
                TARGET SECTION: Literature Review
                TARGET CHAPTER: Chapter Two: Literature Review
                WORKSPACE TYPE: ACADEMIC_RESEARCH
                PROJECT TYPE: GENERAL_ACADEMIC_PROJECT
                Project Title: Digital Agriculture Platform
                """;

        String designPrompt = """
                SEMANTIC PURPOSE: SYSTEM_DESIGN
                TARGET SECTION: System Architecture & Design
                TARGET CHAPTER: Chapter Three: System Design
                WORKSPACE TYPE: ACADEMIC_PROJECT
                PROJECT TYPE: SOFTWARE_SYSTEM_DEVELOPMENT
                Project Title: Digital Agriculture Platform
                """;

        String customPrompt = """
                SEMANTIC PURPOSE: CUSTOM
                TARGET SECTION: Deployment Strategy
                TARGET CHAPTER: Chapter Six: Deployment
                WORKSPACE TYPE: ACADEMIC_PROJECT
                PROJECT TYPE: SOFTWARE_SYSTEM_DEVELOPMENT
                Project Title: Digital Agriculture Platform
                """;

        GeneratedAnswerDraft bgDraft = synthesizer.synthesize(createBundle(bgPrompt));
        GeneratedAnswerDraft problemDraft = synthesizer.synthesize(createBundle(problemPrompt));
        GeneratedAnswerDraft litDraft = synthesizer.synthesize(createBundle(litPrompt));
        GeneratedAnswerDraft designDraft = synthesizer.synthesize(createBundle(designPrompt));
        GeneratedAnswerDraft customDraft = synthesizer.synthesize(createBundle(customPrompt));

        // Background assertions:
        assertThat(bgDraft.answerText()).contains("### Contextual Foundation and Domain Background");
        assertThat(bgDraft.answerText()).contains("The contextual foundation for Digital Agriculture Platform");
        assertThat(bgDraft.answerText()).doesNotContain("### Overview and Thematic Context");
        assertThat(bgDraft.answerText()).doesNotContain("synthesis of the grounded empirical literature");
        assertThat(bgDraft.citations()).isNotEmpty();
        assertThat(bgDraft.answerText()).contains("[E1]");

        // Problem Statement assertions:
        assertThat(problemDraft.answerText()).contains("### Problem Context and Operational Setting");
        assertThat(problemDraft.answerText()).contains("An assessment of the problem domain for Digital Agriculture Platform");
        assertThat(problemDraft.answerText()).doesNotContain("### Overview and Thematic Context");
        assertThat(problemDraft.answerText()).doesNotContain("synthesis of the grounded empirical literature");
        assertThat(problemDraft.citations()).isNotEmpty();

        // Literature Review assertions:
        assertThat(litDraft.answerText()).contains("### Thematic Overview and Conceptual Foundations");
        assertThat(litDraft.answerText()).contains("literature review synthesizes findings and theoretical perspectives");
        assertThat(litDraft.answerText()).contains("### Synthesis and Research Gaps");

        // System Design assertions:
        assertThat(designDraft.answerText()).contains("### System Architecture and High-Level Design");
        assertThat(designDraft.answerText()).contains("architectural design for Digital Agriculture Platform");
        assertThat(designDraft.answerText()).doesNotContain("### Overview and Thematic Context");
        assertThat(designDraft.answerText()).doesNotContain("synthesis of the grounded empirical literature");

        // Custom section assertions:
        assertThat(customDraft.answerText()).contains("### Deployment Strategy Analysis");
        assertThat(customDraft.answerText()).contains("This section examines Deployment Strategy");
        assertThat(customDraft.answerText()).doesNotContain("### Overview and Thematic Context");

        // Pairwise uniqueness:
        assertThat(bgDraft.answerText()).isNotEqualTo(problemDraft.answerText());
        assertThat(bgDraft.answerText()).isNotEqualTo(litDraft.answerText());
        assertThat(litDraft.answerText()).isNotEqualTo(designDraft.answerText());
        assertThat(designDraft.answerText()).isNotEqualTo(customDraft.answerText());
    }
}
