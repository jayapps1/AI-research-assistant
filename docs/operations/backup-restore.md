# Backup And Restore Operations

PostgreSQL is the source of truth for application metadata and research data.
Uploaded documents, datasets, import files, and future exports may live outside
PostgreSQL, so a complete recovery point includes both PostgreSQL and
object/document storage.

## Metadata Model

The application tracks backup metadata only:

- `backup_policies`
- `backup_runs`
- `backup_artifacts`
- `restore_runs`

Backup files are not stored in PostgreSQL.

`BackupManifest` records the backup run id, application version, Flyway schema
version, database backup reference, database checksum when available,
object-storage backup reference, object-storage checksum when available,
object count when practical, policy name, encryption indicator, and
verification status.

Manifests must not include database passwords, JWT secrets, Redis passwords, or
encryption keys.

## Policy Defaults

Configurable defaults:

- Daily retention: `BACKUP_RETENTION_DAILY`, default `7`
- Weekly retention: `BACKUP_RETENTION_WEEKLY`, default `4`
- Monthly retention: `BACKUP_RETENTION_MONTHLY`, default `12`
- RPO target: `BACKUP_TARGET_RPO_MINUTES`, default `1440`
- RTO target: `BACKUP_TARGET_RTO_MINUTES`, default `240`

Daily backups imply an initial RPO target around 24 hours. This is a policy
target, not a guarantee. Actual RTO is not known until restore drills measure
it.

## PostgreSQL Backups

The application does not back up PostgreSQL by reading every table through JPA.
Use infrastructure-grade mechanisms:

- Managed database backups
- `pg_dump` / `pg_restore` for controlled logical backups
- pgBackRest or equivalent
- WAL archiving and Point-In-Time Recovery for production

Local logical backup script:

```powershell
$env:PGPASSWORD = "<set outside command history where possible>"
.\ops\backup\backup-postgres.ps1
```

The script uses `pg_dump -F c`, writes a timestamped `.dump`, and writes a
SHA-256 checksum. Do not hardcode database passwords.

## WAL And PITR

Production should use WAL archiving with tested restore procedures. A complete
PITR runbook must define base backup cadence, WAL archive retention, recovery
target time selection, and validation. The application does not simulate PITR.

## Object Storage Backups

For local document storage:

```powershell
.\ops\backup\backup-object-storage.ps1
```

The script archives only the configured document storage root, writes a file
manifest, and calculates SHA-256 checksums.

For S3/MinIO-compatible production storage, prefer bucket versioning, object
lock where appropriate, lifecycle retention, replication to a separate backup
location/account, and deletion protection.

## Encryption And Secrets

Backups containing research or participant data must be encrypted at rest in
production. Prefer managed storage encryption or proven backup-tool encryption.
Do not put encryption keys inside the same backup archive as encrypted data.

Secrets required for recovery include:

- `JWT_SECRET`
- `CREDENTIAL_ENCRYPTION_KEY`
- Future `RESEARCH_DATA_ENCRYPTION_KEY`
- Database credentials
- Future object-storage credentials

Encrypted database values may be unrecoverable if key material is permanently
lost, even when PostgreSQL backups are intact. Secret recovery must be handled
through a separate KMS or secret-management recovery process.

## Restore

PostgreSQL restore script:

```powershell
$env:PGPASSWORD = "<set securely>"
.\ops\backup\restore-postgres.ps1 -SourceDump .\backup.dump -TargetDatabase research_assist_restore -Checksum "<sha256>" -ConfirmDestructive
```

Object-storage restore script:

```powershell
.\ops\backup\restore-object-storage.ps1 -SourceArchive .\objects.zip -TargetDirectory .\restore-work\documents -Checksum "<sha256>" -DryRun
```

The object restore script validates archive entries for path traversal before
extracting.

Do not restore production backups into development without a sanitization or
deidentification workflow.

## Verification

A backup is not verified just because a file exists. Verification should check:

- Manifest reference exists.
- Backup artifact checksum matches when available.
- DB backup artifact is readable by backup tooling.
- Expected Flyway schema version is available.
- Object-storage manifest is readable.
- Expected object count/checksum coverage is present.
- Restored application validates Flyway state.
- Critical tables exist.
- Sample document storage references resolve.

After restore, flush Redis and let caches rebuild from PostgreSQL.

## Monthly Restore Drill

At least monthly:

1. Create or select a backup.
2. Restore PostgreSQL into an isolated test or staging database.
3. Restore document/object storage into an isolated storage root.
4. Run Flyway validation.
5. Run application smoke tests.
6. Verify document/object references.
7. Record measured restore duration.
8. Mark the backup `VERIFIED` or `VERIFICATION_FAILED`.

This drill provides evidence for actual RTO.

## Consistency Model

Local `pg_dump` and filesystem/object-storage copy are coordinated by manifest
references and timestamps, not by perfect atomicity. Production should prefer
object versioning/snapshots aligned with database backup references.
