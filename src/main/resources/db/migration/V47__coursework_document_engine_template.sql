-- V47: Phase 2 shared academic document engine coursework defaults
--
-- Coursework uses the same report/chapter/section engine, but its starting
-- structure is heading-oriented rather than chapter-oriented.

UPDATE research_report_templates
SET name = 'Generic Coursework Document',
    type = 'COURSEWORK',
    supported_workspace_types = 'COURSEWORK',
    description = 'Generic coursework document for assignments, essays, case studies, term papers, and class exercises.',
    configuration_json =
    '{"chapters":[' ||
      '{"type":"CUSTOM","title":"Coursework Document","required":true,"systemDefined":true,"sections":[' ||
        '{"type":"CUSTOM","heading":"Introduction","required":true,"systemDefined":true},' ||
        '{"type":"CUSTOM","heading":"Main Discussion","required":true,"systemDefined":true},' ||
        '{"type":"CONCLUSIONS","heading":"Conclusion","required":true,"systemDefined":true}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","required":true,"systemDefined":true,"sections":[' ||
        '{"type":"REFERENCES","heading":"References","required":true,"systemDefined":true,"aiEnabled":false}' ||
      ']}' ||
    ']}'
WHERE id = '00000000-0000-0000-0000-000000000523';

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
    '00000000-0000-0000-0000-000000000523',
    'Generic Coursework Document',
    'COURSEWORK',
    NULL,
    NULL,
    'COURSEWORK',
    TRUE,
    'APA_7',
    FALSE,
    'Generic coursework document for assignments, essays, case studies, term papers, and class exercises.',
    '{"chapters":[' ||
      '{"type":"CUSTOM","title":"Coursework Document","required":true,"systemDefined":true,"sections":[' ||
        '{"type":"CUSTOM","heading":"Introduction","required":true,"systemDefined":true},' ||
        '{"type":"CUSTOM","heading":"Main Discussion","required":true,"systemDefined":true},' ||
        '{"type":"CONCLUSIONS","heading":"Conclusion","required":true,"systemDefined":true}' ||
      ']},' ||
      '{"type":"REFERENCES","title":"References","required":true,"systemDefined":true,"sections":[' ||
        '{"type":"REFERENCES","heading":"References","required":true,"systemDefined":true,"aiEnabled":false}' ||
      ']}' ||
    ']}'
) ON CONFLICT (id) DO NOTHING;
