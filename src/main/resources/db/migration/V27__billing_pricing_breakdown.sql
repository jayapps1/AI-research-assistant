-- Migration V27: Add price breakdown snapshot columns to billing_payment_intents

ALTER TABLE billing_payment_intents
    ADD COLUMN IF NOT EXISTS base_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS processing_fee_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS ai_generation_fee_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS total_amount NUMERIC(12, 2);

-- Backfill existing rows
UPDATE billing_payment_intents
SET
    base_amount = COALESCE(base_amount, expected_amount),
    processing_fee_amount = COALESCE(processing_fee_amount, 0.00),
    ai_generation_fee_amount = COALESCE(ai_generation_fee_amount, 0.00),
    total_amount = COALESCE(total_amount, expected_amount)
WHERE total_amount IS NULL;
