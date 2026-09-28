# Manual security audit runs

The administrator starts a run through POST `/api/admin/security/runs` with no body or `{}`. Unknown request fields and reports supplied by clients are rejected. Existing member-token/current DB ADMIN authorization protects create, list and detail. Responses use private/no-store; 429 includes Retry-After 30. Storage failure is 503, not an empty successful history.

The server reads current configuration, database connectivity, administrator count and the article identity index; it never creates indexes, changes roles, imports articles or uploads attack files. Manual-review controls remain unknown. Existing `/security/status` is compatible but now labels legacy/JWT policy descriptions as warnings instead of active-test passes.

Fixed frontend probes use only the configured frontend origin and `/`, `/admin`, `/api/admin/session`, `/robots.txt`, `/sitemap.xml`. Public HTTPS DNS is resolved once; every address must be public before Apache HttpClient's custom DNS resolver pins the validated addresses. Default TLS certificate and hostname verification remain enabled. Redirects, retries, cookies and credential caches are disabled. Localhost HTTP is allowed only when explicitly configured in non-production and pinned to loopback. No self-backend URL is inferred.

HTTP responses are observed without consuming their bodies. Header count/line limits, one-second connect/read limits and a two-second cancellable request deadline bound each request. Header findings concern only the observed response; public availability and redirects do not prove authenticated administrator safety. Invalid targets, private DNS, timeouts and transport errors produce unknown without storing exception text, response bodies, addresses, cookies or secret values.

The check worker has a 15-second response budget. Storage has a separate five-second response budget; frontend POST timeout should be at least 25 seconds. A Mongo driver may not stop an in-flight operation immediately when interrupted: a storage timeout can return 503 even if the server-side write later completes. Refresh history before retrying. Stuck workers are bounded rather than spawning unlimited threads.

Only a manually requested run writes the separate `security_audit_runs` collection. Records contain UUID, timestamps, safe environment labels, server-generated checks and counts. They contain no actor identifier or raw credentials. Queries exclude expired records and return at most 20 runs within 30 days. After a successful save the service removes expired history and history older than the newest 20, restricting pruning by an observed timestamp cutoff to avoid deleting newer concurrent records. No TTL index is created automatically; physical expiry cleanup occurs on the next manual run. Concurrent replicas can transiently exceed the physical cap; query caps remain enforced.

Cooldown and execution exclusion are per JVM (30 seconds), not a distributed lock. Multiple replicas require an ingress/distributed control if a global limit is needed. A process restart resets the in-memory cooldown. The inspection result does not certify penetration testing, malware protection, provider ACLs or rate limits.

Verification uses mocked MongoDB, fixed test values, injected DNS/transport fixtures and local HTTP fixture servers. Local transport tests verify no Authorization/Cookie transmission, no redirect following and cancellation of slow response headers. No real audit POST, database result write or application restart is performed by these tests.

Primary implementation references: Apache HttpClient `PoolingHttpClientConnectionManagerBuilder.setDnsResolver`, default TLS configuration, request cancellation and immediate response closure. Dependency version is managed by the existing Spring Boot BOM.


## Repository and storage metadata checks (2026-09-09)

The existing empty-body run endpoint and schema version 1 remain unchanged. New categories are `repository` and `storage`; `provider-api` is an additional evidence type. Existing histories remain readable.

- GitHub targets are fixed to the two application repositories. An optional server-only `GITHUB_SECURITY_TOKEN` enables bounded Dependabot reads; absent credentials or denied/rate-limited access produce unknown findings. No token, response body, workflow name, log, repository URL or alert description is saved. API version 2022-11-28 remains supported. A successful latest Actions run does not certify deployment or database ingestion.
- The crawler workflow check separates its enabled state and latest scheduled execution from repository-wide Actions. A scheduled execution older than eight days warns; missing or future timestamps are unknown. Workflow revision does not identify the crawler branch checkout. No import receipt is inferred.
- Mongo and GridFS checks read at most 1001 documents per fixed collection with maxTimeMS=1000 and disk use disabled. Reported counts and bytes are samples, not total database/storage size. Missing collections, permissions and incomplete cursors are unknown. Article timestamps are explicitly sample observations; whole-collection freshness remains unknown because no indexed latest query is performed. No image bytes, object identifiers, chunk validation or backups are read.
- R2 checks only HEAD the existing configured bucket using the existing SDK, TLS verification, a two-second call budget and zero retries. Endpoints are restricted to Cloudflare R2 HTTPS hosts. SDK 2.20.0 UrlConnectionHttpClient disables redirects. A successful HEAD does not establish ACL safety, public exposure, object integrity, total size or restoration.
- GitHub requests pin public DNS, do not follow redirects, and cap successful response bodies at 256 KiB; error bodies are not consumed. Provider errors retain no exception details. Run execution has a 25-second budget, storage five seconds, and frontend timeout 40 seconds. Unfinished extension checks persist as unknown. Worker concurrency remains bounded; no workflow dispatch, upload/delete/import, index creation or permission changes occur.

Fixture verification uses mocked provider/DB interfaces and pure bounded-body parsing. It does not execute the real run endpoint, contact providers/databases or test the production R2 transport. R2 redirect behavior was checked against the pinned AWS SDK source, not a live bucket.
