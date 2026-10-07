package com.researchassistant.analysis.entity;

public enum SectionSemanticPurpose {
    TITLE_PAGE,
    DECLARATION,
    CERTIFICATION,
    DEDICATION,
    ACKNOWLEDGEMENTS,
    ABSTRACT,
    TABLE_OF_CONTENTS,
    LIST_OF_FIGURES,
    LIST_OF_TABLES,
    BACKGROUND,
    PROBLEM_STATEMENT,
    OBJECTIVES,
    RESEARCH_QUESTIONS,
    HYPOTHESES,
    SIGNIFICANCE,
    SCOPE,
    ORGANIZATION_OF_STUDY,
    LITERATURE_REVIEW,
    RESEARCH_GAP,
    CONCEPTUAL_FRAMEWORK,
    THEORETICAL_FRAMEWORK,
    RELATED_SYSTEMS,
    METHODOLOGY,
    POPULATION_SAMPLING,
    DATA_COLLECTION_METHOD,
    RESEARCH_INSTRUMENT,
    SYSTEM_REQUIREMENTS,
    SYSTEM_DESIGN,
    IMPLEMENTATION,
    TESTING,
    FINDINGS,
    DISCUSSION,
    CONCLUSIONS,
    RECOMMENDATIONS,
    REFERENCES,
    APPENDIX,
    CUSTOM;

    public SectionGenerationPolicy defaultPolicy() {
        return switch (this) {
            case TITLE_PAGE, TABLE_OF_CONTENTS, LIST_OF_FIGURES, LIST_OF_TABLES, REFERENCES ->
                SectionGenerationPolicy.DETERMINISTIC;
            case DECLARATION, CERTIFICATION, DEDICATION, ACKNOWLEDGEMENTS, APPENDIX ->
                SectionGenerationPolicy.USER_AUTHORED_FRONT_MATTER;
            case LITERATURE_REVIEW, RESEARCH_GAP, THEORETICAL_FRAMEWORK ->
                SectionGenerationPolicy.SOURCE_GROUNDED_AI;
            case TESTING, FINDINGS, DISCUSSION ->
                SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED;
            case BACKGROUND, PROBLEM_STATEMENT, OBJECTIVES, RESEARCH_QUESTIONS, HYPOTHESES, SIGNIFICANCE, SCOPE,
                 ORGANIZATION_OF_STUDY,
                 CONCEPTUAL_FRAMEWORK, RELATED_SYSTEMS, METHODOLOGY, POPULATION_SAMPLING, DATA_COLLECTION_METHOD,
                 RESEARCH_INSTRUMENT, SYSTEM_REQUIREMENTS, SYSTEM_DESIGN, IMPLEMENTATION,
                 CONCLUSIONS, RECOMMENDATIONS, ABSTRACT ->
                SectionGenerationPolicy.PROJECT_DERIVED_AI;
            case CUSTOM ->
                SectionGenerationPolicy.CONTEXTUAL_AI;
        };
    }

    public ReportSectionType defaultSectionType() {
        return switch (this) {
            case TITLE_PAGE -> ReportSectionType.TITLE_PAGE;
            case DECLARATION -> ReportSectionType.DECLARATION;
            case CERTIFICATION -> ReportSectionType.CERTIFICATION;
            case DEDICATION -> ReportSectionType.DEDICATION;
            case ACKNOWLEDGEMENTS -> ReportSectionType.ACKNOWLEDGEMENTS;
            case ABSTRACT -> ReportSectionType.ABSTRACT;
            case TABLE_OF_CONTENTS -> ReportSectionType.TABLE_OF_CONTENTS;
            case LIST_OF_FIGURES -> ReportSectionType.LIST_OF_FIGURES;
            case LIST_OF_TABLES -> ReportSectionType.LIST_OF_TABLES;
            case BACKGROUND -> ReportSectionType.BACKGROUND;
            case PROBLEM_STATEMENT -> ReportSectionType.PROBLEM_STATEMENT;
            case OBJECTIVES -> ReportSectionType.OBJECTIVES;
            case RESEARCH_QUESTIONS -> ReportSectionType.RESEARCH_QUESTIONS;
            case HYPOTHESES -> ReportSectionType.HYPOTHESES;
            case SIGNIFICANCE -> ReportSectionType.SIGNIFICANCE;
            case SCOPE, ORGANIZATION_OF_STUDY -> ReportSectionType.SCOPE;
            case LITERATURE_REVIEW -> ReportSectionType.LITERATURE_REVIEW;
            case RESEARCH_GAP -> ReportSectionType.RESEARCH_GAP;
            case CONCEPTUAL_FRAMEWORK -> ReportSectionType.CONCEPTUAL_FRAMEWORK;
            case THEORETICAL_FRAMEWORK -> ReportSectionType.THEORETICAL_REVIEW;
            case RELATED_SYSTEMS -> ReportSectionType.RELATED_SYSTEMS;
            case METHODOLOGY, POPULATION_SAMPLING, DATA_COLLECTION_METHOD, RESEARCH_INSTRUMENT -> ReportSectionType.METHODOLOGY;
            case SYSTEM_REQUIREMENTS -> ReportSectionType.SYSTEM_REQUIREMENTS;
            case SYSTEM_DESIGN -> ReportSectionType.SYSTEM_DESIGN;
            case IMPLEMENTATION -> ReportSectionType.IMPLEMENTATION;
            case TESTING -> ReportSectionType.TESTING;
            case FINDINGS -> ReportSectionType.FINDINGS;
            case DISCUSSION -> ReportSectionType.DISCUSSION;
            case CONCLUSIONS -> ReportSectionType.CONCLUSIONS;
            case RECOMMENDATIONS -> ReportSectionType.RECOMMENDATIONS;
            case REFERENCES -> ReportSectionType.REFERENCES;
            case APPENDIX -> ReportSectionType.APPENDIX;
            case CUSTOM -> ReportSectionType.CUSTOM;
        };
    }
}
