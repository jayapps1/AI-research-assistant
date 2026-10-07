package com.researchassistant.document.entity;

/**
 * Semantic classification of document chunks for retrieval hygiene and academic drafting.
 */
public enum ChunkSemanticType {
    FRONT_MATTER,
    BODY,
    TABLE,
    FIGURE_CAPTION,
    REFERENCES,
    FOOTER_HEADER,
    METADATA
}
