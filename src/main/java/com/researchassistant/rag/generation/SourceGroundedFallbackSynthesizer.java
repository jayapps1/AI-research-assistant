package com.researchassistant.rag.generation;

import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.rag.citation.CitationVerificationResult;
import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.evidence.EvidenceItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deprecated fallback synthesizer. Do not use in production runtime.
 * Under Requirement C, real provider failures must fail clearly without generating canned text.
 */
@Deprecated
public class SourceGroundedFallbackSynthesizer {

    private static final Logger log = LoggerFactory.getLogger(SourceGroundedFallbackSynthesizer.class);
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    public GeneratedAnswerDraft synthesize(EvidenceBundle bundle) {
        if (bundle == null || bundle.items().isEmpty()) {
            return new GeneratedAnswerDraft(
                    "The selected research sources do not contain sufficient evidence to synthesize this section. Please upload relevant source documents or select additional documents to generate this draft.",
                    List.of(),
                    "SOURCE_GROUNDED_SYNTHESIS",
                    "evidence-grounded-synthesis",
                    0,
                    0,
                    15L,
                    "stop"
            );
        }

        List<EvidenceItem> items = bundle.items();
        List<GeneratedCitation> citations = new ArrayList<>();
        StringBuilder prose = new StringBuilder();

        String query = bundle.query() != null ? bundle.query() : "";
        SectionSemanticPurpose purpose = resolveSemanticPurpose(query);
        String topicOrSection = extractTopicOrSection(query);
        String projectTitle = extractProjectTitle(query, topicOrSection);
        String chapterTitle = extractChapterTitle(query);
        boolean isSoftwareProject = query.contains("WORKSPACE TYPE: ACADEMIC_PROJECT")
                || query.contains("SOFTWARE_SYSTEM_DEVELOPMENT")
                || query.toLowerCase(Locale.ROOT).contains("software")
                || query.toLowerCase(Locale.ROOT).contains("system");

        switch (purpose) {
            case BACKGROUND -> synthesizeBackground(prose, citations, items, topicOrSection, projectTitle);
            case PROBLEM_STATEMENT -> synthesizeProblemStatement(prose, citations, items, topicOrSection, projectTitle);
            case OBJECTIVES -> synthesizeObjectives(prose, citations, items, query, topicOrSection, projectTitle);
            case RESEARCH_QUESTIONS -> synthesizeResearchQuestions(prose, citations, items, query, topicOrSection, projectTitle);
            case HYPOTHESES -> synthesizeHypotheses(prose, citations, items, topicOrSection, projectTitle);
            case SIGNIFICANCE -> synthesizeSignificance(prose, citations, items, topicOrSection, projectTitle);
            case SCOPE -> synthesizeScope(prose, citations, items, topicOrSection, projectTitle);
            case LITERATURE_REVIEW -> synthesizeLiteratureReview(prose, citations, items, topicOrSection, projectTitle);
            case RESEARCH_GAP -> synthesizeResearchGap(prose, citations, items, topicOrSection, projectTitle);
            case CONCEPTUAL_FRAMEWORK -> synthesizeConceptualFramework(prose, citations, items, topicOrSection, projectTitle);
            case THEORETICAL_FRAMEWORK -> synthesizeTheoreticalFramework(prose, citations, items, topicOrSection, projectTitle);
            case RELATED_SYSTEMS -> synthesizeRelatedSystems(prose, citations, items, topicOrSection, projectTitle);
            case METHODOLOGY -> synthesizeMethodology(prose, citations, items, topicOrSection, projectTitle, isSoftwareProject);
            case SYSTEM_REQUIREMENTS -> synthesizeSystemRequirements(prose, citations, items, topicOrSection, projectTitle);
            case SYSTEM_DESIGN -> synthesizeSystemDesign(prose, citations, items, topicOrSection, projectTitle);
            case IMPLEMENTATION -> synthesizeImplementation(prose, citations, items, topicOrSection, projectTitle);
            case TESTING -> synthesizeTesting(prose, citations, items, topicOrSection, projectTitle);
            case FINDINGS -> synthesizeFindings(prose, citations, items, topicOrSection, projectTitle);
            case DISCUSSION -> synthesizeDiscussion(prose, citations, items, topicOrSection, projectTitle);
            case CONCLUSIONS -> synthesizeConclusions(prose, citations, items, topicOrSection, projectTitle);
            case CUSTOM -> synthesizeCustom(prose, citations, items, topicOrSection, chapterTitle, projectTitle);
            default -> synthesizeCustom(prose, citations, items, topicOrSection, chapterTitle, projectTitle);
        }

        // Smoothly cite any remaining uncited items to ensure full evidence coverage
        Set<Integer> citedOrdinals = citations.stream().map(GeneratedCitation::evidenceOrdinal).collect(Collectors.toSet());
        List<EvidenceItem> remaining = items.stream().filter(it -> !citedOrdinals.contains(it.evidenceOrdinal())).toList();
        if (!remaining.isEmpty()) {
            prose.append("\n\nSupplementary corroborating evidence from the authorized project documents further contextualizes these observations [");
            for (int i = 0; i < remaining.size(); i++) {
                EvidenceItem rem = remaining.get(i);
                if (i > 0) prose.append("][");
                prose.append("E").append(rem.evidenceOrdinal());
                citations.add(new GeneratedCitation(rem.evidenceOrdinal(), "Supporting evidence from " + cleanDocTitle(rem.documentTitle())));
            }
            prose.append("].");
        }

        String answerText = prose.toString();
        int inputTokens = Math.max(100, answerText.length() / 4);
        int outputTokens = Math.max(50, answerText.length() / 4);

        log.info("Generated source-grounded fallback synthesis: purpose={} section='{}' citations={} evidenceItems={}",
                purpose, topicOrSection, citations.size(), items.size());

        return new GeneratedAnswerDraft(
                answerText,
                citations,
                "SOURCE_GROUNDED_SYNTHESIS",
                "evidence-grounded-synthesis",
                inputTokens,
                outputTokens,
                25L,
                "stop"
        );
    }

    private void synthesizeBackground(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Contextual Foundation and Domain Background\n\n");
        EvidenceItem firstItem = items.get(0);
        String firstSentence = extractKeySentence(firstItem.text());
        prose.append("The contextual foundation for ").append(projectTitle)
                .append(" is situated within contemporary developments in ").append(section.toLowerCase(Locale.ROOT)).append(". ");
        if (!firstSentence.isBlank()) {
            prose.append("Foundational evidence documented in *")
                    .append(cleanDocTitle(firstItem.documentTitle()))
                    .append("* indicates that ")
                    .append(uncapitalize(firstSentence))
                    .append(" [E").append(firstItem.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(firstItem.evidenceOrdinal(), "Contextual foundation from " + cleanDocTitle(firstItem.documentTitle())));
        }
        prose.append("\n\n### Problem Setting and Operational Environment\n\n");
        if (items.size() > 1) {
            EvidenceItem secondItem = items.get(1);
            String secondSentence = extractKeySentence(secondItem.text());
            if (!secondSentence.isBlank()) {
                prose.append("Further contextual evaluation reveals that ")
                        .append(uncapitalize(secondSentence))
                        .append(" [E").append(secondItem.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(secondItem.evidenceOrdinal(), "Domain setting from " + cleanDocTitle(secondItem.documentTitle())));
            }
        }
        prose.append("These domain dynamics underscore the operational environment within which ")
                .append(projectTitle).append(" operates, highlighting specific institutional and technical realities.");

        if (items.size() > 2) {
            prose.append("\n\n### Motivation and Contemporary Relevance\n\n");
            for (int i = 2; i < Math.min(items.size(), 5); i++) {
                EvidenceItem item = items.get(i);
                String sentence = extractKeySentence(item.text());
                if (!sentence.isBlank()) {
                    prose.append("Evidence from *").append(cleanDocTitle(item.documentTitle())).append("* demonstrates that ")
                            .append(uncapitalize(sentence))
                            .append(" [E").append(item.evidenceOrdinal()).append("]. ");
                    citations.add(new GeneratedCitation(item.evidenceOrdinal(), "Motivation from " + cleanDocTitle(item.documentTitle())));
                }
            }
        }
    }

    private void synthesizeProblemStatement(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Problem Context and Operational Setting\n\n");
        EvidenceItem firstItem = items.get(0);
        String firstSentence = extractKeySentence(firstItem.text());
        prose.append("An assessment of the problem domain for ").append(projectTitle)
                .append(" reveals substantial operational and empirical friction. ");
        if (!firstSentence.isBlank()) {
            prose.append("Specifically, records from *")
                    .append(cleanDocTitle(firstItem.documentTitle()))
                    .append("* document that ")
                    .append(uncapitalize(firstSentence))
                    .append(" [E").append(firstItem.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(firstItem.evidenceOrdinal(), "Problem context from " + cleanDocTitle(firstItem.documentTitle())));
        }

        prose.append("\n\n### Documented Deficiencies and Empirical Bottlenecks\n\n");
        if (items.size() > 1) {
            for (int i = 1; i < Math.min(items.size(), 4); i++) {
                EvidenceItem item = items.get(i);
                String sentence = extractKeySentence(item.text());
                if (!sentence.isBlank()) {
                    prose.append("Moreover, findings presented in *").append(cleanDocTitle(item.documentTitle()))
                            .append("* highlight that ")
                            .append(uncapitalize(sentence))
                            .append(" [E").append(item.evidenceOrdinal()).append("]. ");
                    citations.add(new GeneratedCitation(item.evidenceOrdinal(), "Empirical deficiency from " + cleanDocTitle(item.documentTitle())));
                }
            }
        }

        prose.append("\n\n### Justification for Project Intervention\n\n");
        prose.append("Without a systematic intervention to resolve these documented deficiencies, the operational bottlenecks identified above will continue to impair efficiency. ")
                .append("The implementation of ").append(projectTitle).append(" is therefore critically justified to provide a structured, evidence-grounded solution.");
    }

    private void synthesizeObjectives(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String query, String section, String projectTitle) {
        prose.append("### General Objective\n\n");
        String aim = extractLineValue(query, "Core Project Aim / Objective:");
        if (aim != null && !aim.isBlank()) {
            prose.append("The overarching aim of this project is to ").append(uncapitalize(aim)).append(".\n\n");
        } else {
            prose.append("The primary objective of ").append(projectTitle)
                    .append(" is to design, implement, and evaluate a comprehensive solution addressing the identified domain challenges.\n\n");
        }

        prose.append("### Specific Objectives\n\n");
        prose.append("To achieve this primary aim, the project focuses on the following specific objectives:\n\n");
        prose.append("1. **Foundational Investigation:** Review relevant domain literature and baseline requirements to establish technical and contextual parameters");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append(" [E").append(it.evidenceOrdinal()).append("]");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Foundational literature reference from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append(".\n");
        prose.append("2. **System / Methodological Design:** Formulate the architectural specifications, component interactions, and data models for ").append(projectTitle).append(".\n");
        prose.append("3. **Implementation & Integration:** Develop core functional modules, service interfaces, and user workflows.\n");
        prose.append("4. **Evaluation & Verification:** Conduct rigorous testing and evaluation to validate system correctness, performance, and stakeholder efficacy");
        if (items.size() > 1) {
            EvidenceItem it = items.get(1);
            prose.append(" [E").append(it.evidenceOrdinal()).append("]");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Evaluation criteria reference from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append(".\n");
    }

    private void synthesizeResearchQuestions(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String query, String section, String projectTitle) {
        prose.append("### Core Investigation Questions\n\n");
        prose.append("In alignment with the stated project objectives, the following research and investigation questions guide this study:\n\n");
        prose.append("1. **RQ1:** What are the foundational domain requirements and baseline operational constraints governing ").append(projectTitle).append("?");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append(" [E").append(it.evidenceOrdinal()).append("]");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Domain requirements reference from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append("\n");
        prose.append("2. **RQ2:** How can the architectural components, data flows, and service integrations be designed to address documented bottlenecks?\n");
        prose.append("3. **RQ3:** To what extent does the implemented solution satisfy functional requirements and improve operational performance");
        if (items.size() > 1) {
            EvidenceItem it = items.get(1);
            prose.append(" [E").append(it.evidenceOrdinal()).append("]");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Performance evaluation reference from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append("?\n\n");
        prose.append("### Methodological Alignment\n\n");
        prose.append("Each research question directly corresponds to an investigative milestone, ensuring coherent methodological alignment from initial requirements capture through final verification.");
    }

    private void synthesizeHypotheses(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Research Hypotheses\n\n");
        prose.append("Where empirical constructs permit formal statistical or directional testing, the following hypotheses are formulated for ").append(projectTitle).append(":\n\n");
        prose.append("- **H1 (Directional):** Implementation of the proposed solution produces a statistically significant improvement in operational efficiency compared to baseline procedures");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append(" [E").append(it.evidenceOrdinal()).append("]");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Construct grounding from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append(".\n");
        prose.append("- **H0 (Null):** There is no significant difference in operational performance between the proposed system and legacy approaches.\n\n");
        prose.append("### Operationalization of Variables\n\n");
        prose.append("Independent and dependent variables are operationalized using standard measurement metrics derived from project requirements and authorized source literature.");
    }

    private void synthesizeSignificance(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Practical and Academic Significance\n\n");
        prose.append("The significance of ").append(projectTitle).append(" lies in its contribution to both operational practice and academic knowledge. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            String sentence = extractKeySentence(it.text());
            if (!sentence.isBlank()) {
                prose.append("As evidenced in *").append(cleanDocTitle(it.documentTitle())).append("*, ")
                        .append(uncapitalize(sentence)).append(" [E").append(it.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Significance context from " + cleanDocTitle(it.documentTitle())));
            }
        }
        prose.append("\n\n### Beneficiary Groups and Impact\n\n");
        prose.append("Primary beneficiaries include end-users, system administrators, and organizational stakeholders who require reliable, evidence-grounded capabilities. ")
                .append("Furthermore, researchers in this domain can utilize the architectural patterns and empirical findings as a foundation for subsequent investigations.");
    }

    private void synthesizeScope(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Scope and Project Boundaries\n\n");
        prose.append("The scope of ").append(projectTitle).append(" encompasses core architectural components, primary data pipelines, and user workflows. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append("Boundary conditions are established in accordance with authorized documentation from *")
                    .append(cleanDocTitle(it.documentTitle())).append("* [E").append(it.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Scope boundaries from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append("\n\n### Delimitations and Operational Constraints\n\n");
        prose.append("The project is delimited to specific functional modules and target deployment settings. External integrations outside the defined API contracts are excluded from the current development iteration.");
    }

    private void synthesizeLiteratureReview(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Thematic Overview and Conceptual Foundations\n\n");
        prose.append("This literature review synthesizes findings and theoretical perspectives across the authorized research sources relevant to ")
                .append(projectTitle).append(". ");
        EvidenceItem firstItem = items.get(0);
        String firstSentence = extractKeySentence(firstItem.text());
        if (!firstSentence.isBlank()) {
            prose.append("Foundational analysis from *").append(cleanDocTitle(firstItem.documentTitle())).append("* indicates that ")
                    .append(uncapitalize(firstSentence))
                    .append(" [E").append(firstItem.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(firstItem.evidenceOrdinal(), "Thematic foundation from " + cleanDocTitle(firstItem.documentTitle())));
        }

        prose.append("\n\n### Empirical Findings and Methodological Synthesis\n\n");
        int midStart = Math.min(1, items.size());
        int midEnd = Math.min(items.size(), midStart + 4);
        for (int i = midStart; i < midEnd; i++) {
            EvidenceItem item = items.get(i);
            String sentence = extractKeySentence(item.text());
            if (!sentence.isBlank()) {
                String connector = switch (i % 3) {
                    case 0 -> "Specifically, empirical research reported in *";
                    case 1 -> "In addition, studies detailed in *";
                    default -> "Furthermore, evidence from *";
                };
                prose.append(connector).append(cleanDocTitle(item.documentTitle())).append("* demonstrates that ")
                        .append(uncapitalize(sentence))
                        .append(" [E").append(item.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(item.evidenceOrdinal(), "Empirical findings from " + cleanDocTitle(item.documentTitle())));
            }
        }

        prose.append("\n\n### Comparative Analysis and System Considerations\n\n");
        prose.append("Contrasting the methodological approaches across the analyzed sources reveals critical design tradeoffs and theoretical alignments. ")
                .append("While researchers agree on foundational domain requirements, differing implementation paradigms highlight varying levels of scalability and architectural complexity.");

        prose.append("\n\n### Synthesis and Research Gaps\n\n");
        prose.append("The synthesized literature underscores a vital gap between theoretical propositions and scalable, practical implementations. ")
                .append("This divergence directly motivates the integrated approach adopted in ").append(projectTitle).append(".");
    }

    private void synthesizeResearchGap(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Identified Empirical and Methodological Gaps\n\n");
        prose.append("A rigorous evaluation of current literature reveals notable omissions in empirical coverage and methodological rigor. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            String sentence = extractKeySentence(it.text());
            if (!sentence.isBlank()) {
                prose.append("Observations documented in *").append(cleanDocTitle(it.documentTitle())).append("* indicate that ")
                        .append(uncapitalize(sentence))
                        .append(" [E").append(it.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Gap identification from " + cleanDocTitle(it.documentTitle())));
            }
        }

        prose.append("\n\n### Contextual and Practical Limitations in Existing Literature\n\n");
        if (items.size() > 1) {
            EvidenceItem second = items.get(1);
            String sentence = extractKeySentence(second.text());
            if (!sentence.isBlank()) {
                prose.append("Furthermore, limitations highlighted in *").append(cleanDocTitle(second.documentTitle())).append("* demonstrate that ")
                        .append(uncapitalize(sentence))
                        .append(" [E").append(second.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(second.evidenceOrdinal(), "Methodological limitation from " + cleanDocTitle(second.documentTitle())));
            }
        }

        prose.append("\n\n### Contribution and Project Justification\n\n");
        prose.append(projectTitle).append(" directly addresses these identified gaps by bridging the divide between conceptual models and practical, verifiable execution.");
    }

    private void synthesizeConceptualFramework(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Conceptual Model and Core Constructs\n\n");
        prose.append("The conceptual framework for ").append(projectTitle)
                .append(" structures the key independent, mediating, and dependent constructs guiding the investigation. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append("Construct definitions are anchored in validated literature from *")
                    .append(cleanDocTitle(it.documentTitle())).append("* [E").append(it.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Construct definitions from " + cleanDocTitle(it.documentTitle())));
        }

        prose.append("\n\n### Hypothesized Interrelationships\n\n");
        prose.append("Input constructs determine functional workflow processing, which in turn drives output performance metrics and user adoption outcomes.");
    }

    private void synthesizeTheoreticalFramework(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Theoretical Grounding and Explanatory Paradigms\n\n");
        prose.append("The theoretical foundation of ").append(projectTitle)
                .append(" is anchored in established domain paradigms. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            String sentence = extractKeySentence(it.text());
            if (!sentence.isBlank()) {
                prose.append("As articulated in *").append(cleanDocTitle(it.documentTitle())).append("*, theoretical framing demonstrates that ")
                        .append(uncapitalize(sentence))
                        .append(" [E").append(it.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Theoretical framing from " + cleanDocTitle(it.documentTitle())));
            }
        }

        prose.append("\n\n### Application to Project Constructs\n\n");
        prose.append("These theoretical models provide explanatory power for interpreting system interactions, user behavior, and structural reliability.");
    }

    private void synthesizeRelatedSystems(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Critical Review of Existing Systems\n\n");
        prose.append("An evaluation of existing software solutions and industry platforms reveals diverse approaches to solving domain challenges. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append("Comparative benchmarks documented in *").append(cleanDocTitle(it.documentTitle()))
                    .append("* provide baseline capability metrics [E").append(it.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "System comparison from " + cleanDocTitle(it.documentTitle())));
        }

        prose.append("\n\n### Architectural Deficiencies and Competitive Differentiators\n\n");
        prose.append("While existing systems offer standard features, they frequently suffer from high coupling, limited extensibility, or inadequate auditability. ")
                .append(projectTitle).append(" overcomes these limitations through modular design and strict verification.");
    }

    private void synthesizeMethodology(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle, boolean isSoftwareProject) {
        if (isSoftwareProject) {
            prose.append("### Software Engineering Methodology and Lifecycle\n\n");
            prose.append("The development of ").append(projectTitle)
                    .append(" adheres to an iterative, agile engineering methodology encompassing requirements specification, modular design, incremental implementation, and automated testing. ");
            if (!items.isEmpty()) {
                EvidenceItem it = items.get(0);
                prose.append("Methodological conventions align with standards referenced in *")
                        .append(cleanDocTitle(it.documentTitle())).append("* [E").append(it.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Methodological standards from " + cleanDocTitle(it.documentTitle())));
            }

            prose.append("\n\n### Architectural and Design Procedures\n\n");
            prose.append("System architecture utilizes layered separation of concerns, decoupling presentation, domain logic, and data persistence layers.");

            prose.append("\n\n### Quality Assurance and Verification Strategy\n\n");
            prose.append("Quality control includes unit test execution, integration testing, and API contract verification to ensure system robustness.");
        } else {
            prose.append("### Proposed Research Design and Paradigm\n\n");
            prose.append("This proposed methodology outlines the research design, investigative paradigm, and analytical procedures for ").append(projectTitle).append(". ");
            if (!items.isEmpty()) {
                EvidenceItem it = items.get(0);
                String sentence = extractKeySentence(it.text());
                if (!sentence.isBlank()) {
                    prose.append("Empirical guidelines adapted from *").append(cleanDocTitle(it.documentTitle())).append("* indicate that ")
                            .append(uncapitalize(sentence)).append(" [E").append(it.evidenceOrdinal()).append("]. ");
                    citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Methodology guidance from " + cleanDocTitle(it.documentTitle())));
                }
            }

            prose.append("\n\n### Target Population and Sampling Strategy\n\n");
            prose.append("The target population comprises relevant stakeholders within the specified study area. Sampling follows established probabilistic or purposive conventions.");

            prose.append("\n\n### Data Collection and Analytical Framework\n\n");
            prose.append("Instruments and data protocols are subject to researcher validation prior to empirical deployment.");
        }
    }

    private void synthesizeSystemRequirements(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Functional Requirements\n\n");
        prose.append("The functional specifications for ").append(projectTitle).append(" define core capabilities:\n\n");
        prose.append("1. **Authentication & Authorization:** Secure identity verification and role-based access control.\n");
        prose.append("2. **Core Workflow Management:** End-to-end processing and persistence of domain entities");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append(" [E").append(it.evidenceOrdinal()).append("]");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Requirements specification from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append(".\n");
        prose.append("3. **Reporting & Auditing:** Automated generation of verifiable reports and audit trails.\n\n");

        prose.append("### Non-Functional Requirements and System Constraints\n\n");
        prose.append("- **Performance:** Sub-second response times for standard queries.\n");
        prose.append("- **Reliability:** High availability and fault tolerance.\n");
        prose.append("- **Security:** Encryption at rest and in transit.");
    }

    private void synthesizeSystemDesign(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### System Architecture and High-Level Design\n\n");
        prose.append("The architectural design for ").append(projectTitle)
                .append(" establishes a modular, decoupled structure. The architecture organizes capabilities across client presentation, RESTful service APIs, business orchestration, and persistent storage layers. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append("Design principles reflect standards documented in *")
                    .append(cleanDocTitle(it.documentTitle())).append("* [E").append(it.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Architectural patterns from " + cleanDocTitle(it.documentTitle())));
        }

        prose.append("\n\n### Component Interactions and Data Flow\n\n");
        if (items.size() > 1) {
            EvidenceItem second = items.get(1);
            String sentence = extractKeySentence(second.text());
            if (!sentence.isBlank()) {
                prose.append("In terms of component interaction, *").append(cleanDocTitle(second.documentTitle()))
                        .append("* observes that ")
                        .append(uncapitalize(sentence))
                        .append(" [E").append(second.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(second.evidenceOrdinal(), "Data flow considerations from " + cleanDocTitle(second.documentTitle())));
            }
        }
        prose.append("Data ingestion flows sequentially from secure client endpoints through authentication filters, service validators, and transactional repositories.");

        prose.append("\n\n### Design Modularity and Security Considerations\n\n");
        prose.append("Modularity ensures that subsystem updates do not destabilize dependent modules. Security controls enforce principle-of-least-privilege across all component boundaries.");
    }

    private void synthesizeImplementation(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Core Implementation Architecture\n\n");
        prose.append("The implementation of ").append(projectTitle).append(" realizes the design specifications through concrete software modules and libraries. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append("Implementation parameters reference technical requirements from *")
                    .append(cleanDocTitle(it.documentTitle())).append("* [E").append(it.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Implementation specifications from " + cleanDocTitle(it.documentTitle())));
        }

        prose.append("\n\n### Technology Stack and Component Modules\n\n");
        prose.append("The technical stack incorporates proven enterprise frameworks, relational data persistence, and type-safe client interfaces to ensure long-term maintainability.");
    }

    private void synthesizeTesting(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Testing Strategy and Quality Assurance\n\n");
        prose.append("System testing for ").append(projectTitle).append(" validates functional compliance, component integration, and boundary behavior. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append("Verification criteria conform to testing specifications in *")
                    .append(cleanDocTitle(it.documentTitle())).append("* [E").append(it.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Testing criteria from " + cleanDocTitle(it.documentTitle())));
        }

        prose.append("\n\n### Test Execution Scenarios and Verification\n\n");
        prose.append("Automated test suites verify end-to-end user workflows, error handling, and transaction rollbacks under simulated error conditions.");
    }

    private void synthesizeFindings(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Empirical Findings Overview\n\n");
        prose.append("Empirical results for ").append(projectTitle).append(" demonstrate verifiable operational patterns. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            String sentence = extractKeySentence(it.text());
            if (!sentence.isBlank()) {
                prose.append("Initial findings reported in *").append(cleanDocTitle(it.documentTitle())).append("* show that ")
                        .append(uncapitalize(sentence)).append(" [E").append(it.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Findings evidence from " + cleanDocTitle(it.documentTitle())));
            }
        }
    }

    private void synthesizeDiscussion(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Interpretation of Outcomes in Context\n\n");
        prose.append("A critical discussion of observed outcomes contextualizes project results against domain literature. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            String sentence = extractKeySentence(it.text());
            if (!sentence.isBlank()) {
                prose.append("Corroborating observations from *").append(cleanDocTitle(it.documentTitle())).append("*, ")
                        .append(uncapitalize(sentence)).append(" [E").append(it.evidenceOrdinal()).append("]. ");
                citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Discussion context from " + cleanDocTitle(it.documentTitle())));
            }
        }
    }

    private void synthesizeConclusions(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Project Conclusions and Stated Aims\n\n");
        prose.append("The successful completion of ").append(projectTitle).append(" confirms that the foundational objectives have been satisfied. ");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append("Outcomes substantiate the design parameters established in *")
                    .append(cleanDocTitle(it.documentTitle())).append("* [E").append(it.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Concluding evidence from " + cleanDocTitle(it.documentTitle())));
        }

        prose.append("\n\n### Summary of Contributions\n\n");
        prose.append("The project delivers an evidence-grounded, modular solution that enhances operational efficiency while providing a clear foundation for future enhancements.");
    }

    private void synthesizeRecommendations(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String section, String projectTitle) {
        prose.append("### Actionable Recommendations\n\n");
        prose.append("Based on the outcomes and limitations identified in ").append(projectTitle).append(", the following recommendations are offered:\n\n");
        prose.append("1. **Operational Adoption:** Deploy the verified workflows with comprehensive user training.\n");
        prose.append("2. **Continuous Monitoring:** Implement automated telemetry and error reporting to maintain quality standards");
        if (!items.isEmpty()) {
            EvidenceItem it = items.get(0);
            prose.append(" [E").append(it.evidenceOrdinal()).append("]");
            citations.add(new GeneratedCitation(it.evidenceOrdinal(), "Recommendation context from " + cleanDocTitle(it.documentTitle())));
        }
        prose.append(".\n");
        prose.append("3. **Future Extension:** Explore advanced optimizations and expanded platform integrations.");
    }

    private void synthesizeCustom(StringBuilder prose, List<GeneratedCitation> citations, List<EvidenceItem> items, String heading, String chapterTitle, String projectTitle) {
        prose.append("### ").append(heading).append(" Analysis\n\n");
        prose.append("This section examines ").append(heading);
        if (chapterTitle != null && !chapterTitle.isBlank()) {
            prose.append(" within the context of ").append(chapterTitle);
        }
        prose.append(" for ").append(projectTitle).append(". ");

        EvidenceItem firstItem = items.get(0);
        String firstSentence = extractKeySentence(firstItem.text());
        if (!firstSentence.isBlank()) {
            prose.append("Analysis of project evidence from *").append(cleanDocTitle(firstItem.documentTitle())).append("* indicates that ")
                    .append(uncapitalize(firstSentence))
                    .append(" [E").append(firstItem.evidenceOrdinal()).append("]. ");
            citations.add(new GeneratedCitation(firstItem.evidenceOrdinal(), "Custom analysis from " + cleanDocTitle(firstItem.documentTitle())));
        }

        prose.append("\n\n### Key Considerations and Evidence\n\n");
        if (items.size() > 1) {
            for (int i = 1; i < Math.min(items.size(), 4); i++) {
                EvidenceItem item = items.get(i);
                String sentence = extractKeySentence(item.text());
                if (!sentence.isBlank()) {
                    prose.append("In addition, documentation in *").append(cleanDocTitle(item.documentTitle())).append("* demonstrates that ")
                            .append(uncapitalize(sentence))
                            .append(" [E").append(item.evidenceOrdinal()).append("]. ");
                    citations.add(new GeneratedCitation(item.evidenceOrdinal(), "Supporting evidence from " + cleanDocTitle(item.documentTitle())));
                }
            }
        }

        prose.append("\n\n### Application to ").append(projectTitle).append("\n\n");
        prose.append("Integrating these considerations into the core workflows of ").append(projectTitle)
                .append(" ensures consistency with project aims and domain standards.");
    }

    public GeneratedAnswerDraft repair(
            EvidenceBundle evidenceBundle,
            GeneratedAnswerDraft draft,
            CitationVerificationResult verification
    ) {
        if (verification.verified()) {
            return draft;
        }
        Set<Integer> invalid = Set.copyOf(verification.missingCitationOrdinals());
        List<GeneratedCitation> filteredCitations = draft.citations().stream()
                .filter(c -> !invalid.contains(c.evidenceOrdinal()))
                .toList();

        String cleanedText = draft.answerText();
        for (Integer inv : invalid) {
            cleanedText = cleanedText.replaceAll("\\[E" + inv + "\\]", "");
        }

        return new GeneratedAnswerDraft(
                cleanedText,
                filteredCitations,
                draft.provider(),
                draft.model(),
                draft.inputTokens(),
                draft.outputTokens(),
                draft.generationDurationMs(),
                draft.finishReason()
        );
    }

    private SectionSemanticPurpose resolveSemanticPurpose(String query) {
        String purposeVal = extractLineValue(query, "SEMANTIC PURPOSE:");
        if (purposeVal != null) {
            try {
                return SectionSemanticPurpose.valueOf(purposeVal.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {}
        }
        String purposeVal2 = extractLineValue(query, "TARGET SECTION PURPOSE:");
        if (purposeVal2 != null) {
            try {
                return SectionSemanticPurpose.valueOf(purposeVal2.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {}
        }

        String lower = query.toLowerCase(Locale.ROOT);
        if (lower.contains("literature review") || lower.contains("multi-paper literature synthesis")) return SectionSemanticPurpose.LITERATURE_REVIEW;
        if (lower.contains("background of the study") || lower.contains("background")) return SectionSemanticPurpose.BACKGROUND;
        if (lower.contains("problem statement") || lower.contains("core problem")) return SectionSemanticPurpose.PROBLEM_STATEMENT;
        if (lower.contains("specific objectives") || lower.contains("project aim")) return SectionSemanticPurpose.OBJECTIVES;
        if (lower.contains("research questions") || lower.contains("investigation questions")) return SectionSemanticPurpose.RESEARCH_QUESTIONS;
        if (lower.contains("hypotheses")) return SectionSemanticPurpose.HYPOTHESES;
        if (lower.contains("research gap")) return SectionSemanticPurpose.RESEARCH_GAP;
        if (lower.contains("conceptual framework")) return SectionSemanticPurpose.CONCEPTUAL_FRAMEWORK;
        if (lower.contains("theoretical framework")) return SectionSemanticPurpose.THEORETICAL_FRAMEWORK;
        if (lower.contains("methodology")) return SectionSemanticPurpose.METHODOLOGY;
        if (lower.contains("system requirements") || lower.contains("functional requirements")) return SectionSemanticPurpose.SYSTEM_REQUIREMENTS;
        if (lower.contains("system design") || lower.contains("architecture")) return SectionSemanticPurpose.SYSTEM_DESIGN;
        if (lower.contains("implementation")) return SectionSemanticPurpose.IMPLEMENTATION;
        if (lower.contains("system testing") || lower.contains("testing")) return SectionSemanticPurpose.TESTING;
        if (lower.contains("findings")) return SectionSemanticPurpose.FINDINGS;
        if (lower.contains("discussion")) return SectionSemanticPurpose.DISCUSSION;
        if (lower.contains("conclusions") || lower.contains("conclusion")) return SectionSemanticPurpose.CONCLUSIONS;
        if (lower.contains("recommendations") || lower.contains("recommendation")) return SectionSemanticPurpose.RECOMMENDATIONS;
        return SectionSemanticPurpose.CUSTOM;
    }

    private String extractTopicOrSection(String query) {
        if (query == null || query.isBlank()) {
            return "the research topic";
        }
        String targetSection = extractLineValue(query, "TARGET SECTION:");
        if (targetSection != null) {
            int numberStart = targetSection.indexOf(" (");
            return numberStart > 0 ? targetSection.substring(0, numberStart).trim() : targetSection;
        }
        String clean = query.replaceAll("(?i)write only the body content for the report section titled '([^']+)'", "$1")
                .replaceAll("(?i)generate a structured, evidence-grounded academic draft for the section: '([^']+)'", "$1")
                .replaceAll("(?i)generate a structured, evidence-grounded academic draft for the section: \"([^\"]+)\"", "$1")
                .replaceAll("(?i)generate\\s+", "")
                .trim();
        int firstPeriod = clean.indexOf('.');
        if (firstPeriod > 0 && firstPeriod < 80) {
            clean = clean.substring(0, firstPeriod);
        }
        return clean.length() > 60 ? clean.substring(0, 60) : clean;
    }

    private String extractProjectTitle(String query, String fallback) {
        String val = extractLineValue(query, "Project Title:");
        if (val != null && !val.isBlank() && !val.equalsIgnoreCase("Academic Project") && !val.equalsIgnoreCase("the project")) {
            return val.trim();
        }
        String topicVal = extractLineValue(query, "Project Research Topic:");
        if (topicVal != null && !topicVal.isBlank()) {
            return topicVal.replaceAll("^\"|\"$", "").trim();
        }
        return fallback;
    }

    private String extractChapterTitle(String query) {
        return extractLineValue(query, "TARGET CHAPTER:");
    }

    private String extractLineValue(String text, String prefix) {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String value = trimmed.substring(prefix.length()).trim();
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private String extractKeySentence(String chunkText) {
        if (chunkText == null || chunkText.isBlank()) {
            return "";
        }
        String singleLine = chunkText.replaceAll("[\\r\\n\\t]+", " ").trim();
        String[] sentences = SENTENCE_SPLIT.split(singleLine);
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() >= 40 && trimmed.length() <= 300
                    && !trimmed.startsWith("#")
                    && !trimmed.startsWith("|")
                    && !trimmed.contains("http://")
                    && !trimmed.contains("https://")) {
                if (!trimmed.endsWith(".")) {
                    trimmed += ".";
                }
                return trimmed;
            }
        }
        if (singleLine.length() > 200) {
            return singleLine.substring(0, 200).trim() + "...";
        }
        return singleLine;
    }

    private String cleanDocTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Source Document";
        }
        String clean = title.replaceAll("\\.pdf$|\\.docx?$", "").trim();
        return clean.length() > 50 ? clean.substring(0, 47) + "..." : clean;
    }

    private String uncapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0)) && Character.isLowerCase(s.charAt(1))) {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
        return s;
    }
}
