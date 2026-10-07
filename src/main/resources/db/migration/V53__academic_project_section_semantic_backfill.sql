-- V53: Backfill academic project template sections that were introduced as
-- CUSTOM before semantic purpose and generation policy routing existed.
-- The predicates are intentionally narrow so user-created custom sections keep
-- their explicit CUSTOM / CONTEXTUAL_AI behavior.

UPDATE research_report_sections
SET type = 'SYSTEM_REQUIREMENTS',
    semantic_purpose = 'SYSTEM_REQUIREMENTS',
    generation_policy = 'PROJECT_DERIVED_AI',
    ai_enabled = TRUE
WHERE system_defined = TRUE
  AND type = 'CUSTOM'
  AND (semantic_purpose = 'CUSTOM' OR semantic_purpose IS NULL)
  AND lower(heading) = 'requirements or design criteria';

UPDATE research_report_sections
SET type = 'SYSTEM_DESIGN',
    semantic_purpose = 'SYSTEM_DESIGN',
    generation_policy = 'PROJECT_DERIVED_AI',
    ai_enabled = TRUE
WHERE system_defined = TRUE
  AND type = 'CUSTOM'
  AND (semantic_purpose = 'CUSTOM' OR semantic_purpose IS NULL)
  AND lower(heading) = 'system, process, or solution design';

UPDATE research_report_sections
SET type = 'IMPLEMENTATION',
    semantic_purpose = 'IMPLEMENTATION',
    generation_policy = 'PROJECT_DERIVED_AI',
    ai_enabled = TRUE
WHERE system_defined = TRUE
  AND type = 'CUSTOM'
  AND (semantic_purpose = 'CUSTOM' OR semantic_purpose IS NULL)
  AND lower(heading) = 'implementation';

UPDATE research_report_templates
SET configuration_json = replace(
        replace(
            replace(
                configuration_json,
                '{"type":"CUSTOM","heading":"Requirements or Design Criteria"}',
                '{"type":"SYSTEM_REQUIREMENTS","heading":"Requirements or Design Criteria"}'
            ),
            '{"type":"CUSTOM","heading":"System, Process, or Solution Design"}',
            '{"type":"SYSTEM_DESIGN","heading":"System, Process, or Solution Design"}'
        ),
        '{"type":"CUSTOM","heading":"Implementation"}',
        '{"type":"IMPLEMENTATION","heading":"Implementation"}'
    )
WHERE id = '00000000-0000-0000-0000-000000000522';
