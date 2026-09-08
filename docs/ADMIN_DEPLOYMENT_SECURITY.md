# Administrator API deployment and checks

2026-09-08 additive release. Deploy backend before administrator/frontend clients.

- Normal users use their existing social/member login. An operator designates a specific existing `users._id` by setting the server-owned `role` field to exact `ADMIN`; absent/other values grant no administrator permission. No account has been modified by this implementation. Every member-token request to `/api/admin/**` re-reads the role, so removal takes effect on the next request without logout. Missing users and database failures fail closed. Public APIs continue accepting the same USER token.
- Static administrator login is disabled: `/api/admin/login` always returns 403 and no token. Previously issued ADMIN_ACCESS tokens cannot authenticate any request. `admin.username` and `admin.password` no longer authorize access and are not required by the controller. Only member USER tokens with the current DB ADMIN role can access administrator APIs. No public signup/profile DTO can assign this field.
- The existing JWT signing key must be configured. ADMIN audience/type/role validation is distinct from USER. Login returns `Cache-Control: no-store`; session cookies remain the integrated frontend NextAuth responsibility.
- `app.frontend.url` is the single allowed browser CORS origin. Production no longer additionally permits localhost. The administrator calls Spring server-to-server through its BFF and needs no browser CORS grant.
- Run the production profile, terminate TLS at a trusted ingress, restrict origin access and apply login rate limits at the ingress. The built-in status screen warns because ingress controls cannot be inferred from Spring configuration. Select hostname/provider with the operator before DNS or deployment changes.
- `GET /api/admin/security/status` is read-only, ADMIN-only, no-store and returns safe configuration observations. It does not expose settings, usernames, keys, hashes or addresses and does not certify penetration-test coverage.

## Request-only article import

`POST /api/admin/articles/batch` accepts an ADMIN bearer token and the same <=200 article envelope and processed/created/existing response as `/import`. The dedicated import key authenticates only `/import`, never `/batch`. Both ingestion routes use the same validation and atomic insert-only identity, so retries do not overwrite articles.

Sources are jumpball, rookie, other. The public `/api/articles/other` uses existing pagination. Manual administrator posts remain separate. Source must match original URL publisher. Submitted links are syntax validated, never fetched or DNS resolved. Only public-shaped HTTP(S) hostnames, no credentials, and standard web ports are accepted. Arbitrary URLs are not scraped by Spring.

Normalize known Jumpball/Rookie www and HTTP variants to `https://jumpball.co.kr` / `https://www.rookie.co.kr`; strip fragments and utm_*/fbclid/gclid parameters while retaining identity query order/values (including nclick). Existing stored HTTP/www/tracking variants are recognized with a source-scoped lookup. Paths are case sensitive. Existing rows are not rewritten; this is not a duplicate cleanup migration. Unknown publisher HTTP/www aliases and changed identity query order are deliberately not inferred equivalent. All validation runs before the first write, but database failures can leave partial writes; a retry is insert-only. Unique source+url index protects competing normalized inserts across both ingress routes. Existing deployment replicas must all run this version before claiming cross-ingress normalization consistency.

Batch field validation errors use HTTP 422; existing bean validation retains HTTP 400 for backward compatibility. Invalid JSON is 400, absent/invalid bearer 401, member bearer without a current ADMIN database role 403.

## Validation

Run `./gradlew test bootJar` using Java 17. Tests use fixture values and mocked persistence, not production credentials or Mongo data. Regression coverage includes public other pagination, ADMIN/USER/anonymous/invalid JWT access, import-key isolation, safe/no-store status output, batch limits, full-request validation before writes, legacy aliases, canonical cross-ingress identity and insert-only retries. Deployment TLS/DNS/rate limiting and actual production persistence still require release verification.
