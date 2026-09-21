-- ============================================================
-- AI RESEARCH ASSISTANT
-- V33 - UNIVERSAL REPORT TEMPLATES AND PROJECT SETTINGS
-- ============================================================

ALTER TABLE research_report_templates
    ADD COLUMN IF NOT EXISTS default_citation_style VARCHAR(60) DEFAULT 'APA_7',
    ADD COLUMN IF NOT EXISTS citation_style_locked BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS department VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE research_projects
    ADD COLUMN IF NOT EXISTS report_template_id UUID REFERENCES research_report_templates(id),
    ADD COLUMN IF NOT EXISTS citation_style VARCHAR(60) DEFAULT 'APA_7',
    ADD COLUMN IF NOT EXISTS citation_style_locked BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_research_projects_template ON research_projects(report_template_id);

-- 1. TTU Computer Science Final Project Report
INSERT INTO research_report_templates(
    id, name, type, institution, department, system_template, default_citation_style, citation_style_locked, description, configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000517',
    'TTU Computer Science Final Project Report',
    'FINAL_YEAR_PROJECT',
    'Takoradi Technical University',
    'Computer Science',
    TRUE,
    'APA_7',
    FALSE,
    'Official five-chapter final year project structure for Computer Science students at Takoradi Technical University.',
    '{"chapters":[' ||
      '{"type":"PRELIMINARY","title":"Preliminary Pages","sections":[' ||
        '{"type":"TITLE_PAGE","heading":"Title Page"},' ||
        '{"type":"CUSTOM","heading":"Declaration"},' ||
        '{"type":"CUSTOM","heading":"Certification"},' ||
        '{"type":"CUSTOM","heading":"Dedication"},' ||
        '{"type":"CUSTOM","heading":"Acknowledgements"},' ||
        '{"type":"ABSTRACT","heading":"Abstract"},' ||
        '{"type":"CUSTOM","heading":"Table of Contents"},' ||
        '{"type":"CUSTOM","heading":"List of Tables"},' ||
        '{"type":"CUSTOM","heading":"List of Figures"},' ||
        '{"type":"CUSTOM","heading":"List of Abbreviations"}' ||
      ']},' ||
      '{"type":"INTRODUCTION","title":"Chapter One: Introduction","chapterNumber":1,"sections":[' ||
        '{"type":"BACKGROUND","heading":"Background of the Study"},' ||
        '{"type":"PROBLEM_STATEMENT","heading":"Statement of the Problem"},' ||
        '{"type":"CUSTOM","heading":"Purpose / Aim"},' ||
        '{"type":"OBJECTIVES","heading":"General Objective"},' ||
        '{"type":"OBJECTIVES","heading":"Specific Objectives"},' ||
        '{"type":"RESEARCH_QUESTIONS","heading":"Research Questions"},' ||
        '{"type":"SIGNIFICANCE","heading":"Significance of the Study"},' ||
        '{"type":"CUSTOM","heading":"Limitations"},' ||
        '{"type":"SCOPE","heading":"Delimitations"},' ||
        '{"type":"CUSTOM","heading":"Organization of the Study"}' ||
      ']},' ||
      '{"type":"LITERATURE_REVIEW","title":"Chapter Two: Literature Review","chapterNumber":2,"sections":[' ||
        '{"type":"CUSTOM","heading":"Introduction"},' ||
        '{"type":"CONCEPTUAL_REVIEW","heading":"Conceptual / Related Literature"},' ||
        '{"type":"THEORETICAL_REVIEW","heading":"Theoretical Review"},' ||
        '{"type":"EMPIRICAL_REVIEW","heading":"Related Studies"},' ||
        '{"type":"CUSTOM","heading":"Comparison of Existing Work"},' ||
        '{"type":"CUSTOM","heading":"Weaknesses / Limitations of Existing Work"},' ||
        '{"type":"RESEARCH_GAP","heading":"Research Gap"},' ||
        '{"type":"CUSTOM","heading":"How Current Study Addresses the Gap"},' ||
        '{"type":"CUSTOM","heading":"Chapter Summary"}' ||
      ']},' ||
      '{"type":"METHODOLOGY","title":"Chapter Three: Proposed Method / System / Software","chapterNumber":3,"sections":[' ||
        '{"type":"CUSTOM","heading":"System / Method Description"},' ||
        '{"type":"CUSTOM","heading":"Systems Requirement Specification"},' ||
        '{"type":"CUSTOM","heading":"Functional Requirements"},' ||
        '{"type":"CUSTOM","heading":"Non-Functional Requirements"},' ||
        '{"type":"CUSTOM","heading":"Use Cases"},' ||
        '{"type":"CUSTOM","heading":"Architecture Design"},' ||
        '{"type":"CUSTOM","heading":"Detailed Design"},' ||
        '{"type":"CUSTOM","heading":"Algorithms / Flowcharts"},' ||
        '{"type":"CUSTOM","heading":"Activity / State / DFD"},' ||
        '{"type":"CUSTOM","heading":"Data Design"},' ||
        '{"type":"CUSTOM","heading":"Database Design"},' ||
        '{"type":"CUSTOM","heading":"ER Diagram"},' ||
        '{"type":"CUSTOM","heading":"Interface Design"},' ||
        '{"type":"CUSTOM","heading":"Efficiency / Effectiveness"},' ||
        '{"type":"METHODOLOGY","heading":"Development Methodology"}' ||
      ']},' ||
      '{"type":"RESULTS","title":"Chapter Four: Results and Discussions","chapterNumber":4,"sections":[' ||
        '{"type":"RESULTS","heading":"Results"},' ||
        '{"type":"CUSTOM","heading":"System Outputs / Screenshots"},' ||
        '{"type":"CUSTOM","heading":"Testing Results"},' ||
        '{"type":"FINDINGS","heading":"Analysis Results"},' ||
        '{"type":"DISCUSSION","heading":"Discussion"}' ||
      ']},' ||
      '{"type":"DISCUSSION","title":"Chapter Five: Conclusion and Recommendation","chapterNumber":5,"sections":[' ||
        '{"type":"CUSTOM","heading":"Summary of Key Findings"},' ||
        '{"type":"CONCLUSIONS","heading":"Conclusion"},' ||
        '{"type":"RECOMMENDATIONS","heading":"Recommendations"}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","sections":[' ||
        '{"type":"REFERENCES","heading":"References"}' ||
      ']},' ||
      '{"type":"APPENDICES","title":"Appendices","sections":[' ||
        '{"type":"APPENDIX","heading":"Appendix A"}' ||
      ']}' ||
    ']}'
) ON CONFLICT (id) DO UPDATE SET
    configuration_json = EXCLUDED.configuration_json,
    name = EXCLUDED.name,
    department = EXCLUDED.department,
    institution = EXCLUDED.institution,
    description = EXCLUDED.description;

-- 2. General Five-Chapter Academic Research Report
INSERT INTO research_report_templates(
    id, name, type, institution, department, system_template, default_citation_style, citation_style_locked, description, configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000518',
    'General Five-Chapter Research Report',
    'RESEARCH_REPORT',
    NULL,
    NULL,
    TRUE,
    'APA_7',
    FALSE,
    'Standard universal 5-chapter research report template suited for academic, survey, and social science investigations.',
    '{"chapters":[' ||
      '{"type":"PRELIMINARY","title":"Preliminary Pages","sections":[' ||
        '{"type":"TITLE_PAGE","heading":"Title Page"},' ||
        '{"type":"CUSTOM","heading":"Declaration"},' ||
        '{"type":"CUSTOM","heading":"Certification / Approval"},' ||
        '{"type":"CUSTOM","heading":"Dedication"},' ||
        '{"type":"CUSTOM","heading":"Acknowledgements"},' ||
        '{"type":"ABSTRACT","heading":"Abstract"},' ||
        '{"type":"CUSTOM","heading":"Table of Contents"},' ||
        '{"type":"CUSTOM","heading":"List of Tables"},' ||
        '{"type":"CUSTOM","heading":"List of Figures"},' ||
        '{"type":"CUSTOM","heading":"List of Abbreviations"}' ||
      ']},' ||
      '{"type":"INTRODUCTION","title":"Chapter One: Introduction","chapterNumber":1,"sections":[' ||
        '{"type":"BACKGROUND","heading":"Background of the Study"},' ||
        '{"type":"PROBLEM_STATEMENT","heading":"Problem Statement"},' ||
        '{"type":"CUSTOM","heading":"Research Aim"},' ||
        '{"type":"OBJECTIVES","heading":"Research Objectives"},' ||
        '{"type":"RESEARCH_QUESTIONS","heading":"Research Questions"},' ||
        '{"type":"HYPOTHESES","heading":"Hypotheses"},' ||
        '{"type":"SIGNIFICANCE","heading":"Significance of the Study"},' ||
        '{"type":"SCOPE","heading":"Scope and Delimitations"},' ||
        '{"type":"CUSTOM","heading":"Limitations"},' ||
        '{"type":"CUSTOM","heading":"Organization of the Study"}' ||
      ']},' ||
      '{"type":"LITERATURE_REVIEW","title":"Chapter Two: Literature Review","chapterNumber":2,"sections":[' ||
        '{"type":"CUSTOM","heading":"Introduction"},' ||
        '{"type":"CONCEPTUAL_REVIEW","heading":"Conceptual Review"},' ||
        '{"type":"THEORETICAL_REVIEW","heading":"Theoretical Review"},' ||
        '{"type":"EMPIRICAL_REVIEW","heading":"Empirical Review"},' ||
        '{"type":"RESEARCH_GAP","heading":"Research Gap"},' ||
        '{"type":"CONCEPTUAL_FRAMEWORK","heading":"Conceptual Framework"},' ||
        '{"type":"CUSTOM","heading":"Chapter Summary"}' ||
      ']},' ||
      '{"type":"METHODOLOGY","title":"Chapter Three: Methodology","chapterNumber":3,"sections":[' ||
        '{"type":"METHODOLOGY","heading":"Research Design"},' ||
        '{"type":"CUSTOM","heading":"Study Area"},' ||
        '{"type":"CUSTOM","heading":"Population"},' ||
        '{"type":"CUSTOM","heading":"Sample Size and Sampling Technique"},' ||
        '{"type":"CUSTOM","heading":"Data Collection Instruments"},' ||
        '{"type":"CUSTOM","heading":"Validity of Instruments"},' ||
        '{"type":"CUSTOM","heading":"Reliability of Instruments"},' ||
        '{"type":"CUSTOM","heading":"Pilot Study"},' ||
        '{"type":"CUSTOM","heading":"Data Collection Procedure"},' ||
        '{"type":"CUSTOM","heading":"Data Analysis Plan"},' ||
        '{"type":"CUSTOM","heading":"Ethical Considerations"}' ||
      ']},' ||
      '{"type":"RESULTS","title":"Chapter Four: Results and Discussion","chapterNumber":4,"sections":[' ||
        '{"type":"CUSTOM","heading":"Response Rate"},' ||
        '{"type":"CUSTOM","heading":"Demographic / Participant Characteristics"},' ||
        '{"type":"RESULTS","heading":"Results and Findings"},' ||
        '{"type":"FINDINGS","heading":"Research Question Analysis"},' ||
        '{"type":"CUSTOM","heading":"Hypothesis Testing"},' ||
        '{"type":"DISCUSSION","heading":"Discussion of Findings"}' ||
      ']},' ||
      '{"type":"DISCUSSION","title":"Chapter Five: Summary, Conclusion and Recommendations","chapterNumber":5,"sections":[' ||
        '{"type":"CUSTOM","heading":"Summary of Findings"},' ||
        '{"type":"CONCLUSIONS","heading":"Conclusions"},' ||
        '{"type":"RECOMMENDATIONS","heading":"Recommendations"},' ||
        '{"type":"CUSTOM","heading":"Suggestions for Future Research"}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","sections":[' ||
        '{"type":"REFERENCES","heading":"References"}' ||
      ']},' ||
      '{"type":"APPENDICES","title":"Appendices","sections":[' ||
        '{"type":"APPENDIX","heading":"Appendix A: Survey Instrument"},' ||
        '{"type":"APPENDIX","heading":"Appendix B: Informed Consent"}' ||
      ']}' ||
    ']}'
) ON CONFLICT (id) DO UPDATE SET
    configuration_json = EXCLUDED.configuration_json,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 3. Quantitative Survey Research Report
INSERT INTO research_report_templates(
    id, name, type, institution, department, system_template, default_citation_style, citation_style_locked, description, configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000519',
    'Quantitative Survey Research Report',
    'RESEARCH_REPORT',
    NULL,
    NULL,
    TRUE,
    'APA_7',
    FALSE,
    'Specialized report template designed for questionnaire, survey, and statistical hypothesis-testing projects.',
    '{"chapters":[' ||
      '{"type":"PRELIMINARY","title":"Preliminary Pages","sections":[' ||
        '{"type":"TITLE_PAGE","heading":"Title Page"},' ||
        '{"type":"ABSTRACT","heading":"Abstract"},' ||
        '{"type":"CUSTOM","heading":"Table of Contents"}' ||
      ']},' ||
      '{"type":"INTRODUCTION","title":"Chapter One: Introduction","chapterNumber":1,"sections":[' ||
        '{"type":"BACKGROUND","heading":"Background of the Study"},' ||
        '{"type":"PROBLEM_STATEMENT","heading":"Problem Statement"},' ||
        '{"type":"OBJECTIVES","heading":"Research Objectives"},' ||
        '{"type":"RESEARCH_QUESTIONS","heading":"Research Questions"},' ||
        '{"type":"HYPOTHESES","heading":"Hypotheses"}' ||
      ']},' ||
      '{"type":"LITERATURE_REVIEW","title":"Chapter Two: Literature Review","chapterNumber":2,"sections":[' ||
        '{"type":"CONCEPTUAL_REVIEW","heading":"Conceptual Review"},' ||
        '{"type":"THEORETICAL_REVIEW","heading":"Theoretical Review"},' ||
        '{"type":"EMPIRICAL_REVIEW","heading":"Empirical Review"},' ||
        '{"type":"RESEARCH_GAP","heading":"Research Gap"},' ||
        '{"type":"CONCEPTUAL_FRAMEWORK","heading":"Conceptual Framework"}' ||
      ']},' ||
      '{"type":"METHODOLOGY","title":"Chapter Three: Survey Methodology","chapterNumber":3,"sections":[' ||
        '{"type":"METHODOLOGY","heading":"Survey Research Design"},' ||
        '{"type":"CUSTOM","heading":"Target Population"},' ||
        '{"type":"CUSTOM","heading":"Sample Size & Sampling Technique"},' ||
        '{"type":"CUSTOM","heading":"Questionnaire Design & Measures"},' ||
        '{"type":"CUSTOM","heading":"Validity & Reliability Testing"},' ||
        '{"type":"CUSTOM","heading":"Data Collection Procedures"},' ||
        '{"type":"CUSTOM","heading":"Statistical Analysis Plan"}' ||
      ']},' ||
      '{"type":"RESULTS","title":"Chapter Four: Statistical Results and Discussion","chapterNumber":4,"sections":[' ||
        '{"type":"CUSTOM","heading":"Sample Characteristics & Descriptive Statistics"},' ||
        '{"type":"RESULTS","heading":"Hypothesis Testing Results"},' ||
        '{"type":"FINDINGS","heading":"Key Empirical Findings"},' ||
        '{"type":"DISCUSSION","heading":"Discussion of Results"}' ||
      ']},' ||
      '{"type":"DISCUSSION","title":"Chapter Five: Conclusion and Recommendations","chapterNumber":5,"sections":[' ||
        '{"type":"CUSTOM","heading":"Summary of Results"},' ||
        '{"type":"CONCLUSIONS","heading":"Conclusion"},' ||
        '{"type":"RECOMMENDATIONS","heading":"Practical & Policy Recommendations"}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","sections":[' ||
        '{"type":"REFERENCES","heading":"References"}' ||
      ']},' ||
      '{"type":"APPENDICES","title":"Appendices","sections":[' ||
        '{"type":"APPENDIX","heading":"Appendix: Questionnaire"}' ||
      ']}' ||
    ']}'
) ON CONFLICT (id) DO UPDATE SET
    configuration_json = EXCLUDED.configuration_json,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 4. Qualitative Research Report
INSERT INTO research_report_templates(
    id, name, type, institution, department, system_template, default_citation_style, citation_style_locked, description, configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000520',
    'Qualitative Research Report',
    'RESEARCH_REPORT',
    NULL,
    NULL,
    TRUE,
    'APA_7',
    FALSE,
    'Qualitative study template structured for interviews, focus groups, thematic analysis, and phenomenological designs.',
    '{"chapters":[' ||
      '{"type":"PRELIMINARY","title":"Preliminary Pages","sections":[' ||
        '{"type":"TITLE_PAGE","heading":"Title Page"},' ||
        '{"type":"ABSTRACT","heading":"Abstract"},' ||
        '{"type":"CUSTOM","heading":"Table of Contents"}' ||
      ']},' ||
      '{"type":"INTRODUCTION","title":"Chapter One: Introduction","chapterNumber":1,"sections":[' ||
        '{"type":"BACKGROUND","heading":"Background and Context"},' ||
        '{"type":"PROBLEM_STATEMENT","heading":"Problem Statement"},' ||
        '{"type":"CUSTOM","heading":"Purpose of the Study"},' ||
        '{"type":"OBJECTIVES","heading":"Research Questions"}' ||
      ']},' ||
      '{"type":"LITERATURE_REVIEW","title":"Chapter Two: Literature & Theoretical Framing","chapterNumber":2,"sections":[' ||
        '{"type":"THEORETICAL_REVIEW","heading":"Theoretical Perspectives"},' ||
        '{"type":"EMPIRICAL_REVIEW","heading":"Review of Related Qualitative Literature"},' ||
        '{"type":"RESEARCH_GAP","heading":"Thematic & Contextual Gap"}' ||
      ']},' ||
      '{"type":"METHODOLOGY","title":"Chapter Three: Qualitative Methodology","chapterNumber":3,"sections":[' ||
        '{"type":"METHODOLOGY","heading":"Qualitative Paradigm & Design"},' ||
        '{"type":"CUSTOM","heading":"Participant Selection & Purposive Sampling"},' ||
        '{"type":"CUSTOM","heading":"Data Collection: Interviews / Focus Groups"},' ||
        '{"type":"CUSTOM","heading":"Trustworthiness & Rigor (Credibility, Transferability)"},' ||
        '{"type":"CUSTOM","heading":"Thematic Coding & Analysis Procedure"},' ||
        '{"type":"CUSTOM","heading":"Ethical Considerations & Reflexivity"}' ||
      ']},' ||
      '{"type":"RESULTS","title":"Chapter Four: Qualitative Findings and Themes","chapterNumber":4,"sections":[' ||
        '{"type":"FINDINGS","heading":"Overview of Emergent Themes"},' ||
        '{"type":"RESULTS","heading":"Detailed Thematic Analysis with Participant Excerpts"},' ||
        '{"type":"DISCUSSION","heading":"Discussion and Grounding in Literature"}' ||
      ']},' ||
      '{"type":"DISCUSSION","title":"Chapter Five: Conclusion and Implications","chapterNumber":5,"sections":[' ||
        '{"type":"CONCLUSIONS","heading":"Summary of Thematic Insights"},' ||
        '{"type":"RECOMMENDATIONS","heading":"Implications and Recommendations"}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","sections":[' ||
        '{"type":"REFERENCES","heading":"References"}' ||
      ']},' ||
      '{"type":"APPENDICES","title":"Appendices","sections":[' ||
        '{"type":"APPENDIX","heading":"Appendix: Interview Guide"}' ||
      ']}' ||
    ']}'
) ON CONFLICT (id) DO UPDATE SET
    configuration_json = EXCLUDED.configuration_json,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 5. Mixed Methods Research Report
INSERT INTO research_report_templates(
    id, name, type, institution, department, system_template, default_citation_style, citation_style_locked, description, configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000521',
    'Mixed Methods Research Report',
    'RESEARCH_REPORT',
    NULL,
    NULL,
    TRUE,
    'APA_7',
    FALSE,
    'Template integrating convergent or sequential quantitative and qualitative research designs.',
    '{"chapters":[' ||
      '{"type":"PRELIMINARY","title":"Preliminary Pages","sections":[' ||
        '{"type":"TITLE_PAGE","heading":"Title Page"},' ||
        '{"type":"ABSTRACT","heading":"Abstract"},' ||
        '{"type":"CUSTOM","heading":"Table of Contents"}' ||
      ']},' ||
      '{"type":"INTRODUCTION","title":"Chapter One: Introduction","chapterNumber":1,"sections":[' ||
        '{"type":"BACKGROUND","heading":"Background of the Study"},' ||
        '{"type":"PROBLEM_STATEMENT","heading":"Problem Statement"},' ||
        '{"type":"OBJECTIVES","heading":"Mixed Methods Purpose & Objectives"},' ||
        '{"type":"RESEARCH_QUESTIONS","heading":"Quantitative and Qualitative Questions"}' ||
      ']},' ||
      '{"type":"LITERATURE_REVIEW","title":"Chapter Two: Literature Review","chapterNumber":2,"sections":[' ||
        '{"type":"CONCEPTUAL_REVIEW","heading":"Conceptual & Theoretical Framework"},' ||
        '{"type":"EMPIRICAL_REVIEW","heading":"Empirical Studies"},' ||
        '{"type":"RESEARCH_GAP","heading":"Research Gap"}' ||
      ']},' ||
      '{"type":"METHODOLOGY","title":"Chapter Three: Mixed Methods Design","chapterNumber":3,"sections":[' ||
        '{"type":"METHODOLOGY","heading":"Mixed Methods Rationale & Design Type"},' ||
        '{"type":"CUSTOM","heading":"Quantitative Strand (Sampling, Instrument, Analysis)"},' ||
        '{"type":"CUSTOM","heading":"Qualitative Strand (Sampling, Guides, Coding)"},' ||
        '{"type":"CUSTOM","heading":"Data Integration & Joint Display Plan"},' ||
        '{"type":"CUSTOM","heading":"Validity, Reliability & Trustworthiness"}' ||
      ']},' ||
      '{"type":"RESULTS","title":"Chapter Four: Integrated Findings & Results","chapterNumber":4,"sections":[' ||
        '{"type":"RESULTS","heading":"Quantitative Strand Results"},' ||
        '{"type":"FINDINGS","heading":"Qualitative Strand Findings"},' ||
        '{"type":"CUSTOM","heading":"Integration & Joint Display Synthesis"},' ||
        '{"type":"DISCUSSION","heading":"Discussion"}' ||
      ']},' ||
      '{"type":"DISCUSSION","title":"Chapter Five: Conclusion and Recommendations","chapterNumber":5,"sections":[' ||
        '{"type":"CONCLUSIONS","heading":"Integrated Conclusions"},' ||
        '{"type":"RECOMMENDATIONS","heading":"Recommendations"}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","sections":[' ||
        '{"type":"REFERENCES","heading":"References"}' ||
      ']},' ||
      '{"type":"APPENDICES","title":"Appendices","sections":[' ||
        '{"type":"APPENDIX","heading":"Appendix: Research Instruments"}' ||
      ']}' ||
    ']}'
) ON CONFLICT (id) DO UPDATE SET
    configuration_json = EXCLUDED.configuration_json,
    name = EXCLUDED.name,
    description = EXCLUDED.description;
