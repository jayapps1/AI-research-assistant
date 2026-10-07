package com.researchassistant.document.entity;

/**
 * Distinguishes the three distinct academic file roles:
 * - TEMPLATE_GUIDELINE: Official university / department / lecturer instructions. Used for
 *   required chapters, sections, front matter, formatting, citation style. NEVER used as
 *   scholarly literature evidence in RAG or cited.
 * - EXAMPLE_REPORT: Previous / sample academic reports. Used only as structure/style/tone example.
 *   NOT automatically treated as scholarly evidence.
 * - RESEARCH_SOURCE: Scholarly papers, books, articles, or empirical reports. Eligible for RAG
 *   and citation grounding.
 */
public enum AcademicFileRole {
    RESEARCH_SOURCE,
    TEMPLATE_GUIDELINE,
    EXAMPLE_REPORT
}
