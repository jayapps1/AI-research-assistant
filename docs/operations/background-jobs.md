# Background Jobs

`background_jobs` is the generic durable job table for platform-wide async work. Workers claim jobs with PostgreSQL row locking and `SKIP LOCKED`, allowing multiple future application instances without duplicate execution.

Use bounded retries with backoff. Transient failures move to `RETRY_SCHEDULED`; exhausted or permanent failures move to `DEAD_LETTER` with safe failure metadata.

`outbox_events` is available for transactional enqueue/event publication without storing secrets or full research content.
