# Cache Operations

The application uses Spring Cache with PostgreSQL as the source of truth.
Redis is optional and must never contain the only copy of research data.

## Providers

Configure `app.cache.provider` with `CACHE_PROVIDER`.

- `simple`: local in-memory cache for development and tests.
- `redis`: Redis-backed cache for production or shared deployments.
- `none`: disables application caching through a no-op cache manager.

Local development defaults to `simple`; no Redis server is required for
`mvnw test` or for starting the application locally.

## Redis Configuration

Redis is configured through environment variables:

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_SSL_ENABLED`
- `REDIS_DATABASE`
- `REDIS_KEY_PREFIX`

Do not commit Redis passwords. Cache values use JSON serialization, not Java
native serialization.

## Namespaces and TTLs

The default key prefix is `research-assistant:v1:`.

Cache groups:

- `project:metadata`: default `PT5M`
- `document:metadata`: default `PT5M`
- `literature:summary`: default `PT5M`
- `research-design:validation`: default `PT1M`

Tenant-aware keys include project/document/user identifiers where responses
can vary by caller.

## Invalidation

`CacheInvalidationService` centralizes eviction.

- Project created, updated, activated, completed, archived, or membership
  changed: evict project metadata.
- Document uploaded, versioned, archived, restored, or reprocessed: evict
  document metadata and project metadata.
- Literature and research-design cache groups have explicit invalidation
  methods for future write services.

TTL is a fallback, not the correctness mechanism.

## Never Cached

Do not cache passwords, password hashes, JWTs, refresh tokens, password reset
tokens, TOTP secrets, TOTP codes, recovery codes, encryption keys,
`ParticipantIdentity`, participant PII, consent details, raw participant
responses, sensitive dataset rows, or raw research document content.

Authorization decisions remain database-backed. Removed or suspended users must
lose access promptly.

## Failure Behavior

Cache failures are logged and counted with `research.cache.operation.failures`.
Core business operations continue against PostgreSQL when cache operations fail.

After restore, Redis should be flushed or allowed to warm naturally. Redis cache
contents are not restored as authoritative application state.
