-- V48: Generic production academic template format profiles.
-- These are intentionally institution-neutral and are stored in the
-- template configuration JSON so future admin-managed templates can
-- extend formatting without a separate report/export engine.

INSERT INTO research_report_templates(
    id,
    name,
    type,
    institution,
    department,
    supported_workspace_types,
    system_template,
    default_citation_style,
    citation_style_locked,
    description,
    configuration_json
) VALUES (
    '00000000-0000-0000-0000-000000000521',
    'Generic Academic Research Report',
    'RESEARCH_REPORT',
    NULL,
    NULL,
    'ACADEMIC_RESEARCH',
    TRUE,
    'APA_7',
    FALSE,
    'Generic academic research report structure for research projects, theses, dissertations, and institution-ready extension.',
    '{"formatProfile":{"pageSize":"A4","orientation":"PORTRAIT","margins":{"topInches":1,"rightInches":1,"bottomInches":1,"leftInches":1.25},"defaultFont":"Times New Roman","bodyFontSize":12,"headingFont":"Times New Roman","lineSpacing":1.5,"paragraphSpacingAfterPt":6,"alignment":"JUSTIFY","firstLineIndentInches":0.5,"chapterStart":"NEW_PAGE","pageNumbering":{"frontMatter":"ROMAN_LOWER","mainContent":"ARABIC"},"headingNumbering":"CHAPTER_DECIMAL","citationStyle":"APA_7"},"chapters":[{"type":"PRELIMINARY","title":"Preliminary Pages","required":true,"systemDefined":true,"sections":[{"type":"TITLE_PAGE","heading":"Title Page","required":true,"systemDefined":true},{"type":"ABSTRACT","heading":"Abstract","required":true,"systemDefined":true}]},{"type":"INTRODUCTION","title":"Chapter One: Introduction","chapterNumber":1,"required":true,"systemDefined":true,"sections":[{"type":"BACKGROUND","heading":"Background","required":true,"systemDefined":true},{"type":"PROBLEM_STATEMENT","heading":"Problem Statement","required":true,"systemDefined":true},{"type":"OBJECTIVES","heading":"Aim and Objectives","required":true,"systemDefined":true},{"type":"RESEARCH_QUESTIONS","heading":"Research Questions","required":false,"systemDefined":true}]},{"type":"LITERATURE_REVIEW","title":"Chapter Two: Literature Review","chapterNumber":2,"required":true,"systemDefined":true,"sections":[{"type":"LITERATURE_REVIEW","heading":"Literature Review","required":true,"systemDefined":true},{"type":"RESEARCH_GAP","heading":"Research Gap","required":false,"systemDefined":true}]},{"type":"METHODOLOGY","title":"Chapter Three: Methodology","chapterNumber":3,"required":true,"systemDefined":true,"sections":[{"type":"METHODOLOGY","heading":"Methodology","required":true,"systemDefined":true}]},{"type":"RESULTS","title":"Chapter Four: Results and Discussion","chapterNumber":4,"required":true,"systemDefined":true,"sections":[{"type":"FINDINGS","heading":"Results Interpretation","required":false,"systemDefined":true},{"type":"DISCUSSION","heading":"Discussion","required":false,"systemDefined":true}]},{"type":"CONCLUSION_RECOMMENDATIONS","title":"Chapter Five: Conclusion and Recommendations","chapterNumber":5,"required":true,"systemDefined":true,"sections":[{"type":"CONCLUSIONS","heading":"Conclusion","required":true,"systemDefined":true},{"type":"RECOMMENDATIONS","heading":"Recommendations","required":false,"systemDefined":true}]},{"type":"REFERENCES","title":"References","required":true,"systemDefined":true,"sections":[{"type":"REFERENCES","heading":"References","required":true,"systemDefined":true,"aiEnabled":false}]},{"type":"APPENDICES","title":"Appendices","required":false,"systemDefined":true,"sections":[{"type":"APPENDIX","heading":"Appendix A","required":false,"systemDefined":true}]}]}'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    supported_workspace_types = EXCLUDED.supported_workspace_types,
    description = EXCLUDED.description,
    configuration_json = EXCLUDED.configuration_json;

UPDATE research_report_templates
SET configuration_json = jsonb_set(
        configuration_json::jsonb,
        '{formatProfile}',
        '{"pageSize":"A4","orientation":"PORTRAIT","margins":{"topInches":1,"rightInches":1,"bottomInches":1,"leftInches":1.25},"defaultFont":"Times New Roman","bodyFontSize":12,"headingFont":"Times New Roman","lineSpacing":1.5,"paragraphSpacingAfterPt":6,"alignment":"JUSTIFY","firstLineIndentInches":0.5,"chapterStart":"NEW_PAGE","pageNumbering":{"frontMatter":"ROMAN_LOWER","mainContent":"ARABIC"},"headingNumbering":"CHAPTER_DECIMAL","citationStyle":"APA_7"}'::jsonb,
        true
    )::text
WHERE id IN (
    '00000000-0000-0000-0000-000000000521',
    '00000000-0000-0000-0000-000000000522',
    '00000000-0000-0000-0000-000000000523'
);
