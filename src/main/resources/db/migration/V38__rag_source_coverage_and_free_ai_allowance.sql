ALTER TABLE rag_queries
    ADD COLUMN IF NOT EXISTS analyzed_source_count INTEGER,
    ADD COLUMN IF NOT EXISTS sources_with_relevant_evidence_count INTEGER;

INSERT INTO plan_entitlements (id, plan_id, feature, enabled, limit_value, limit_unit, metadata_json)
SELECT '00000000-0000-0000-0000-000000001901',
       id,
       'AI_GENERATION_CREDITS_MONTHLY',
       TRUE,
       500,
       'CREDITS',
       '{"defaultAllowancePeriod":"MONTHLY","source":"development-default"}'
FROM subscription_plans
WHERE code = 'FREE'
ON CONFLICT (plan_id, feature)
DO UPDATE SET enabled = TRUE,
              limit_value = 500,
              limit_unit = 'CREDITS',
              metadata_json = COALESCE(plan_entitlements.metadata_json, EXCLUDED.metadata_json),
              updated_at = CURRENT_TIMESTAMP;
