# Production Checklist

- Set strong `JWT_SECRET`, `CREDENTIAL_ENCRYPTION_KEY`, database credentials, object storage configuration, and AI provider credentials when AI is enabled.
- Configure explicit `API_ALLOWED_ORIGINS`; do not use wildcard origins with credentials.
- Keep Paystack in test mode unless a later live-payment phase explicitly changes policy.
- Enable PostgreSQL backups, object storage backups, key recovery procedures, restore drills, RPO, and RTO targets.
- Monitor actuator health, HTTP metrics, Hikari metrics, AI errors/cost, payment attempt failures, background queue depth, notification failures, storage health, and backup freshness.
