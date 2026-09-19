-- ============================================================
-- AI RESEARCH ASSISTANT
-- V28 - AI CREDIT WALLET, TOP-UP PACKS, LEDGER, AND METERING
-- ============================================================

-- 1. AI Credit Packs (Admin-managed top-up products)
CREATE TABLE ai_credit_packs (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    credit_amount NUMERIC(14, 4) NOT NULL,
    price_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'GHS',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_credit_packs_code UNIQUE (code),
    CONSTRAINT chk_ai_credit_packs_credit_amount CHECK (credit_amount > 0),
    CONSTRAINT chk_ai_credit_packs_price_amount CHECK (price_amount >= 0)
);
CREATE INDEX idx_ai_credit_packs_active_order ON ai_credit_packs(active, display_order);

-- 2. Authoritative Workspace AI Credit Wallets
CREATE TABLE ai_credit_wallets (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    purchased_balance NUMERIC(14, 4) NOT NULL DEFAULT 0.0000,
    promotional_balance NUMERIC(14, 4) NOT NULL DEFAULT 0.0000,
    reserved_balance NUMERIC(14, 4) NOT NULL DEFAULT 0.0000,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_credit_wallets_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_credit_wallets_workspace UNIQUE (workspace_id),
    CONSTRAINT chk_ai_credit_wallets_purchased CHECK (purchased_balance >= 0),
    CONSTRAINT chk_ai_credit_wallets_promotional CHECK (promotional_balance >= 0),
    CONSTRAINT chk_ai_credit_wallets_reserved CHECK (reserved_balance >= 0)
);
CREATE INDEX idx_ai_credit_wallets_workspace ON ai_credit_wallets(workspace_id);

-- 3. AI Credit Purchases (Snapshotted pack purchases)
CREATE TABLE ai_credit_purchases (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    credit_pack_id UUID NOT NULL,
    pack_code VARCHAR(60) NOT NULL,
    pack_name VARCHAR(160) NOT NULL,
    credits_purchased NUMERIC(14, 4) NOT NULL,
    price_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(40) NOT NULL,
    billing_payment_intent_id UUID,
    credited_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_credit_purchases_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_credit_purchases_pack FOREIGN KEY (credit_pack_id) REFERENCES ai_credit_packs(id),
    CONSTRAINT chk_ai_credit_purchases_credits CHECK (credits_purchased > 0),
    CONSTRAINT chk_ai_credit_purchases_price CHECK (price_amount >= 0),
    CONSTRAINT chk_ai_credit_purchases_status CHECK (status IN ('CREATED', 'PAYMENT_PENDING', 'PAID', 'CREDITED', 'FAILED', 'EXPIRED', 'REFUNDED'))
);
CREATE INDEX idx_ai_credit_purchases_workspace_created ON ai_credit_purchases(workspace_id, created_at);
CREATE INDEX idx_ai_credit_purchases_status ON ai_credit_purchases(status);

-- 4. Extend billing_payment_intents for AI Credit Purchases
ALTER TABLE billing_payment_intents
    ADD COLUMN IF NOT EXISTS purchase_type VARCHAR(40) NOT NULL DEFAULT 'SUBSCRIPTION',
    ADD COLUMN IF NOT EXISTS ai_credit_purchase_id UUID REFERENCES ai_credit_purchases(id) ON DELETE SET NULL;

ALTER TABLE billing_payment_intents
    ALTER COLUMN plan_id DROP NOT NULL,
    ALTER COLUMN billing_interval DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_billing_payment_intents_purchase_type') THEN
        ALTER TABLE billing_payment_intents
            ADD CONSTRAINT chk_billing_payment_intents_purchase_type
            CHECK (purchase_type IN ('SUBSCRIPTION', 'AI_CREDIT_PACK'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_billing_payment_intents_purchase_type ON billing_payment_intents(purchase_type);
CREATE INDEX IF NOT EXISTS idx_billing_payment_intents_ai_credit_purchase ON billing_payment_intents(ai_credit_purchase_id);

-- Link back foreign key from ai_credit_purchases to billing_payment_intents
ALTER TABLE ai_credit_purchases
    ADD CONSTRAINT fk_ai_credit_purchases_intent
    FOREIGN KEY (billing_payment_intent_id) REFERENCES billing_payment_intents(id) ON DELETE SET NULL;

-- 5. Immutable AI Credit Ledger Entries
CREATE TABLE ai_credit_ledger_entries (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    bucket VARCHAR(30) NOT NULL,
    type VARCHAR(40) NOT NULL,
    credit_amount NUMERIC(14, 4) NOT NULL,
    balance_before NUMERIC(14, 4) NOT NULL,
    balance_after NUMERIC(14, 4) NOT NULL,
    source_type VARCHAR(60) NOT NULL,
    source_id UUID,
    ai_request_id UUID REFERENCES ai_requests(id) ON DELETE SET NULL,
    payment_intent_id UUID REFERENCES billing_payment_intents(id) ON DELETE SET NULL,
    payment_attempt_id UUID REFERENCES payment_attempts(id) ON DELETE SET NULL,
    purchase_id UUID REFERENCES ai_credit_purchases(id) ON DELETE SET NULL,
    idempotency_key VARCHAR(180),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_credit_ledger_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_credit_ledger_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_ai_credit_ledger_bucket CHECK (bucket IN ('PURCHASED', 'PROMOTIONAL')),
    CONSTRAINT chk_ai_credit_ledger_type CHECK (type IN ('PURCHASE', 'CONSUMPTION', 'RESERVATION', 'RESERVATION_RELEASE', 'REFUND', 'ADMIN_GRANT', 'PROMOTIONAL_GRANT', 'EXPIRY', 'ADJUSTMENT'))
);
CREATE INDEX idx_ai_credit_ledger_workspace_created ON ai_credit_ledger_entries(workspace_id, created_at);
CREATE INDEX idx_ai_credit_ledger_type ON ai_credit_ledger_entries(type);
CREATE INDEX idx_ai_credit_ledger_ai_request ON ai_credit_ledger_entries(ai_request_id);
CREATE INDEX idx_ai_credit_ledger_purchase ON ai_credit_ledger_entries(purchase_id);

-- 6. Token-Aware Credit Rates (Config/Admin driven metering by provider and model)
CREATE TABLE ai_credit_rates (
    id UUID PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(200) NOT NULL,
    credits_per_million_input_tokens NUMERIC(14, 4) NOT NULL,
    credits_per_million_output_tokens NUMERIC(14, 4) NOT NULL,
    credits_per_million_cached_input_tokens NUMERIC(14, 4),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ai_credit_rates_input CHECK (credits_per_million_input_tokens >= 0),
    CONSTRAINT chk_ai_credit_rates_output CHECK (credits_per_million_output_tokens >= 0)
);
CREATE INDEX idx_ai_credit_rates_provider_model_active ON ai_credit_rates(provider, model, active);
