-- ============================================================
-- AI RESEARCH ASSISTANT
-- V32 - AI COST METERING, CACHED TOKENS, PRICING AND PLAN ENTITLEMENTS
-- ============================================================

-- 1. Add cached token tracking to ai_requests
ALTER TABLE ai_requests
    ADD COLUMN IF NOT EXISTS cached_input_tokens INTEGER;

-- 2. Add cached input cost tracking to ai_usage_costs
ALTER TABLE ai_usage_costs
    ADD COLUMN IF NOT EXISTS cached_input_cost NUMERIC(12, 6);

-- 3. Extend ai_model_pricing with cached token rates, tiers, and context class
ALTER TABLE ai_model_pricing
    ADD COLUMN IF NOT EXISTS cached_input_price_per_million NUMERIC(12, 6),
    ADD COLUMN IF NOT EXISTS service_tier VARCHAR(60),
    ADD COLUMN IF NOT EXISTS context_class VARCHAR(60),
    ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP WITH TIME ZONE;

-- 4. Seed OpenAI Model Pricing (USD rates)
INSERT INTO ai_model_pricing (
    id, provider, model, input_price_per_million, cached_input_price_per_million,
    output_price_per_million, currency, effective_from, active
) VALUES
('b0000000-0000-0000-0000-000000000001', 'OPENAI', 'gpt-5.6-luna', 0.200000, 0.020000, 1.200000, 'USD', CURRENT_TIMESTAMP, TRUE),
('b0000000-0000-0000-0000-000000000002', 'OPENAI', 'text-embedding-3-small', 0.020000, 0.000000, 0.000000, 'USD', CURRENT_TIMESTAMP, TRUE)
ON CONFLICT (id) DO UPDATE SET
    input_price_per_million = EXCLUDED.input_price_per_million,
    cached_input_price_per_million = EXCLUDED.cached_input_price_per_million,
    output_price_per_million = EXCLUDED.output_price_per_million,
    active = EXCLUDED.active;

-- 5. Seed OpenAI Credit Rates (1 Credit = $0.001 USD of calculated usage)
-- gpt-5.6-luna: Input 200 credits/M ($0.20/M), Cached 20 credits/M ($0.02/M), Output 1200 credits/M ($1.20/M)
-- text-embedding-3-small: Input 20 credits/M ($0.02/M)
INSERT INTO ai_credit_rates (
    id, provider, model, credits_per_million_input_tokens, credits_per_million_output_tokens,
    credits_per_million_cached_input_tokens, effective_from, active
) VALUES
('c0000000-0000-0000-0000-000000000001', 'OPENAI', 'gpt-5.6-luna', 200.0000, 1200.0000, 20.0000, CURRENT_TIMESTAMP, TRUE),
('c0000000-0000-0000-0000-000000000002', 'OPENAI', 'text-embedding-3-small', 20.0000, 0.0000, 0.0000, CURRENT_TIMESTAMP, TRUE)
ON CONFLICT (id) DO UPDATE SET
    credits_per_million_input_tokens = EXCLUDED.credits_per_million_input_tokens,
    credits_per_million_output_tokens = EXCLUDED.credits_per_million_output_tokens,
    credits_per_million_cached_input_tokens = EXCLUDED.credits_per_million_cached_input_tokens,
    active = EXCLUDED.active;

-- 6. Seed Subscription Plan Monthly AI Credit Entitlements
-- FREE: 50 credits/mo ($0.05 allowance)
-- STUDENT: 260 credits/mo ($0.26 allowance)
-- PRO: 1,300 credits/mo ($1.30 allowance)
-- INSTITUTION: 5,000 credits/mo ($5.00 allowance)
INSERT INTO plan_entitlements (id, plan_id, feature, enabled, limit_value, limit_unit)
VALUES
('00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000000101', 'AI_GENERATION_CREDITS_MONTHLY', TRUE, 50, 'CREDITS'),
('00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000000102', 'AI_GENERATION_CREDITS_MONTHLY', TRUE, 260, 'CREDITS'),
('00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000000103', 'AI_GENERATION_CREDITS_MONTHLY', TRUE, 1300, 'CREDITS'),
('00000000-0000-0000-0000-000000002004', '00000000-0000-0000-0000-000000000104', 'AI_GENERATION_CREDITS_MONTHLY', TRUE, 5000, 'CREDITS')
ON CONFLICT (plan_id, feature) DO UPDATE SET
    enabled = EXCLUDED.enabled,
    limit_value = EXCLUDED.limit_value,
    limit_unit = EXCLUDED.limit_unit;
