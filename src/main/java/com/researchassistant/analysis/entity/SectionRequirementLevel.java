package com.researchassistant.analysis.entity;

/**
 * Defines the necessity of a section according to an institution / department template:
 * - REQUIRED: Mandatory section. Produces validation warnings if missing.
 * - RECOMMENDED: Highly suggested but can be omitted if not applicable to the study.
 * - OPTIONAL: Discretionary section that can be included or excluded freely.
 */
public enum SectionRequirementLevel {
    REQUIRED,
    RECOMMENDED,
    OPTIONAL
}
