# Disaster Recovery

## Outage Classes

- PostgreSQL failure
- Application host loss
- Document/object-storage loss
- Redis loss
- Accidental deletion
- Corrupted migration or data
- Secret/encryption-key loss
- Future region/provider failure

Redis loss is a performance event, not data loss. PostgreSQL and
object/document storage remain authoritative.

## Recovery Order

1. Recover required secret/key access.
2. Restore or provision PostgreSQL.
3. Restore object/document storage.
4. Verify Flyway/schema state.
5. Start the application.
6. Flush or rebuild Redis caches.
7. Run integrity checks.
8. Enable user traffic.
9. Verify monitoring.
10. Record incident and recovery result.

## Secret Loss Warning

If encryption key material is permanently lost, encrypted data may not be
recoverable even when PostgreSQL backups are intact. Key recovery is therefore
part of disaster recovery, not a separate administrative convenience.

## Incident Notes

For accidental deletion or corruption, identify the recovery point before
restoring. Do not combine a random database backup with unrelated document
storage unless the mismatch has been reviewed and accepted.

For corrupted migrations, stop rollout, preserve the database state, inspect
Flyway history, and restore only after selecting a verified recovery point.

For future full-region failure, provision infrastructure in the recovery
region, recover secrets from KMS/secret manager, restore PostgreSQL and object
storage, validate, then route traffic.

## Monitoring And Alerts

Alertable signals prepared by the application include:

- Backup overdue or missing.
- Backup verification failed.
- Backup age.
- Last verified backup age.
- PostgreSQL health through standard datasource health.
- Redis health when Redis is enabled.
- Document storage health.
- Backup/restore success and failure counters.

SMS, WhatsApp, Kubernetes, and production cloud infrastructure are deliberately
out of scope for this phase.
