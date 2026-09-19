-- ============================================================
-- AI RESEARCH ASSISTANT
-- V26 - SUBSCRIPTION PLAN PRICES AND RENEWAL REMINDERS
-- ============================================================

-- 1. Create subscription_plan_prices table to support multi-interval (monthly, yearly) pricing per plan
CREATE TABLE subscription_plan_prices (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    billing_interval VARCHAR(20) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'GHS',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_plan_prices_interval CHECK (billing_interval IN ('NONE','MONTHLY','YEARLY')),
    CONSTRAINT chk_plan_prices_price CHECK (price >= 0),
    CONSTRAINT uk_plan_prices_plan_interval UNIQUE (plan_id, billing_interval)
);

CREATE INDEX idx_subscription_plan_prices_plan_active
    ON subscription_plan_prices(plan_id, active);

-- 2. Update subscription_plans table to active business monthly pricing:
-- FREE: GHS 0.00
-- STUDENT: GHS 20.00 per month
-- PRO: GHS 100.00 per month
UPDATE subscription_plans
SET price = 0.00, billing_interval = 'NONE', updated_at = CURRENT_TIMESTAMP
WHERE code = 'FREE';

UPDATE subscription_plans
SET price = 20.00, billing_interval = 'MONTHLY', updated_at = CURRENT_TIMESTAMP
WHERE code = 'STUDENT';

UPDATE subscription_plans
SET price = 100.00, billing_interval = 'MONTHLY', updated_at = CURRENT_TIMESTAMP
WHERE code = 'PRO';

-- 3. Seed active prices into subscription_plan_prices
INSERT INTO subscription_plan_prices (id, plan_id, billing_interval, price, currency, active, effective_from)
SELECT
    '00000000-0000-0000-0000-000000000201'::UUID,
    id,
    'NONE',
    0.00,
    'GHS',
    TRUE,
    CURRENT_TIMESTAMP
FROM subscription_plans
WHERE code = 'FREE'
ON CONFLICT (plan_id, billing_interval) DO UPDATE
SET price = EXCLUDED.price, active = TRUE, updated_at = CURRENT_TIMESTAMP;

INSERT INTO subscription_plan_prices (id, plan_id, billing_interval, price, currency, active, effective_from)
SELECT
    '00000000-0000-0000-0000-000000000202'::UUID,
    id,
    'MONTHLY',
    20.00,
    'GHS',
    TRUE,
    CURRENT_TIMESTAMP
FROM subscription_plans
WHERE code = 'STUDENT'
ON CONFLICT (plan_id, billing_interval) DO UPDATE
SET price = EXCLUDED.price, active = TRUE, updated_at = CURRENT_TIMESTAMP;

INSERT INTO subscription_plan_prices (id, plan_id, billing_interval, price, currency, active, effective_from)
SELECT
    '00000000-0000-0000-0000-000000000203'::UUID,
    id,
    'MONTHLY',
    100.00,
    'GHS',
    TRUE,
    CURRENT_TIMESTAMP
FROM subscription_plans
WHERE code = 'PRO'
ON CONFLICT (plan_id, billing_interval) DO UPDATE
SET price = EXCLUDED.price, active = TRUE, updated_at = CURRENT_TIMESTAMP;

-- 4. Create subscription_renewal_reminders table for idempotent delivery tracking
CREATE TABLE subscription_renewal_reminders (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES workspace_subscriptions(id) ON DELETE CASCADE,
    reminder_type VARCHAR(40) NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_for TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_id UUID REFERENCES notifications(id) ON DELETE SET NULL,
    CONSTRAINT uk_renewal_reminders_sub_type_end UNIQUE (subscription_id, reminder_type, current_period_end)
);

CREATE INDEX idx_renewal_reminders_sub_end
    ON subscription_renewal_reminders(subscription_id, current_period_end);
