-- V29__normalize_existing_phone_numbers.sql
-- Normalizes existing valid Ghana phone numbers to canonical E.164 format (+233XXXXXXXXX).
-- Does not modify invalid or unrecognized phone numbers.

-- 1. Normalize 10-digit local Ghana numbers: e.g. 0542011738 -> +233542011738
UPDATE users
SET phone_number = '+233' || SUBSTRING(REGEXP_REPLACE(phone_number, '[\s\-\(\)\.]+', '', 'g') FROM 2),
    updated_at = NOW()
WHERE phone_number IS NOT NULL
  AND REGEXP_REPLACE(phone_number, '[\s\-\(\)\.]+', '', 'g') ~ '^0[235][0-9]{8}$';

-- 2. Normalize 12-digit Ghana numbers without plus prefix: e.g. 233542011738 -> +233542011738
UPDATE users
SET phone_number = '+' || REGEXP_REPLACE(phone_number, '[\s\-\(\)\.]+', '', 'g'),
    updated_at = NOW()
WHERE phone_number IS NOT NULL
  AND REGEXP_REPLACE(phone_number, '[\s\-\(\)\.]+', '', 'g') ~ '^233[235][0-9]{8}$';

-- 3. Clean up any formatting characters from already prefixed +233 numbers: e.g. +233 54 201 1738 -> +233542011738
UPDATE users
SET phone_number = '+' || REGEXP_REPLACE(phone_number, '[^\d]', '', 'g'),
    updated_at = NOW()
WHERE phone_number IS NOT NULL
  AND phone_number LIKE '+%'
  AND REGEXP_REPLACE(phone_number, '[^\d]', '', 'g') ~ '^233[235][0-9]{8}$';
