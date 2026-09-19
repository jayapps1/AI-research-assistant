-- ============================================================
-- AI RESEARCH ASSISTANT
-- V25 - SUBSCRIPTION PLAN FEATURED FLAG AND UPGRADES
-- ============================================================

ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_subscription_plans_featured
    ON subscription_plans(featured);
