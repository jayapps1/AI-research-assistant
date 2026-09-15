# Paystack TEST Mode Integration

This backend only supports Paystack TEST mode in the current phase. Do not use live keys and do not enable live charging.

## Required Environment Variables

```text
PAYSTACK_MODE=test
PAYSTACK_ENABLED=true
PAYSTACK_SECRET_KEY=<paystack-test-secret-key>
PAYSTACK_PUBLIC_KEY=<paystack-test-public-key>
PAYSTACK_CALLBACK_BASE_URL=http://localhost:8096
PAYSTACK_LIVE_ENABLED=false
PAYSTACK_CONNECT_TIMEOUT=PT10S
PAYSTACK_READ_TIMEOUT=PT30S
```

No Paystack key should be committed, logged, or returned from an API response.

## Getting Test Keys

1. Sign in to the Paystack dashboard.
2. Switch the dashboard to TEST mode.
3. Copy the TEST secret key and TEST public key.
4. Set them as environment variables before starting the backend.
5. Keep `PAYSTACK_LIVE_ENABLED=false` during this development phase.

## Initialize a Test Transaction

Authenticate as a workspace owner or workspace admin and call:

```http
POST /api/v1/workspaces/{workspaceId}/billing/initialize
Content-Type: application/json

{
  "planCode": "STUDENT",
  "billingInterval": "MONTHLY"
}
```

The server resolves the amount and currency from `subscription_plans`. The client never submits or controls the amount.

The response contains:

```json
{
  "transactionId": "uuid",
  "reference": "RA-...",
  "authorizationUrl": "https://checkout.paystack.com/...",
  "accessCode": "..."
}
```

Open `authorizationUrl` in a browser and complete checkout with Paystack test card details from Paystack's dashboard documentation.

## Callback and Verification

The callback URL is generated from `PAYSTACK_CALLBACK_BASE_URL`, but browser callback success is not trusted as proof of payment.

The backend activates a subscription only after server-to-server verification:

```http
POST /api/v1/billing/transactions/{transactionId}/verify
```

Verification checks:

- Paystack status is successful.
- Reference matches the internal transaction reference.
- Amount matches the plan price converted to smallest currency units.
- Currency matches the configured plan currency.
- The transaction has not already been applied.

Repeated verification is idempotent and will not activate the same subscription twice.

## Webhook

Configure the Paystack webhook URL:

```text
POST /api/v1/billing/webhooks/paystack
```

The endpoint is public because Paystack does not send the application JWT. Security comes from validating `x-paystack-signature` against the exact raw request body using HMAC-SHA512 and the Paystack secret key.

Currently handled event:

- `charge.success`

Unsupported events are acknowledged and ignored safely.

## Successful Payment Effect

A verified successful TEST payment:

1. Marks `payment_transactions.status` as `SUCCESS`.
2. Stores the transaction environment as `TEST`.
3. Activates a new `workspace_subscriptions` row.
4. Preserves prior subscription rows as history.
5. Makes `EntitlementService` resolve the new plan immediately.
6. Records a safe audit event.

## Test-to-Live Checklist

Live mode is intentionally disabled in this phase. Before enabling production charging later:

- Add explicit production deployment gating.
- Set `PAYSTACK_LIVE_ENABLED=true` only in production.
- Use Paystack live keys from secret management.
- Create separate live provider mappings if recurring Paystack plans are introduced.
- Confirm webhook URL and signature verification in production.
- Re-run manual payment verification with small controlled live transactions.
- Add refund, invoice, tax, and support procedures before real customer operations.
## Payment Recovery

Payment initialization creates a payment intent plus a provider attempt. Retrying a failed, cancelled, abandoned, expired, or verification-failed attempt creates a new Paystack test transaction reference and preserves previous attempts.

Pending Mobile Money attempts remain pending until provider verification returns a final status or the attempt expires under server policy. A paid intent settles only once; any later successful distinct attempt is retained and flagged for administrative review.

Never store Paystack keys in source, tests, migrations, logs, or documentation.
