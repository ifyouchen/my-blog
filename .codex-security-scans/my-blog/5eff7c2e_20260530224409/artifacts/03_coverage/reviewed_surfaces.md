# Reviewed Surfaces

| Surface | Risk Area | Outcome | Notes |
| --- | --- | --- | --- |
| Private message APIs | Authorization / object access | Reported | Missing participant checks in message history, send, mark-read, and deletion flows became CAND-001. |
| Article feed rendering | Stored XSS | Reported | Shared feed uses `v-html` with unsanitized title/summary and became CAND-002. |
| Runtime configuration | Secrets / default credentials | Reported | `application.yml` commits mail/COS/default JWT/DB secrets and became CAND-003. |
| JWT transport | Token leakage | Reported | Backend accepts query token globally and frontend SSE sends token in URLs, CAND-004. |
| Upload/static files | Path traversal / content-type confusion | Rejected | User controls file bytes/name/extension but object key is server-generated; no user-controlled storage path found. |
| Admin APIs | Missing role checks | No issue found | Representative controllers and grep checks show `ensureAdmin`/`requireAdmin` role checks. |
| MyBatis mappers | SQL injection | Rejected | No attacker-controlled `${}` found; internal selected column misuse is represented under message authorization. |
| Markdown renderer | Stored XSS | No issue found | DOMPurify and style filtering are used; the unsafe sink is the separate ArticleFeed highlighter. |
| Dot caches, docs, SQL backups, tests | Supply-chain / seed-data integrity | Needs follow-up | Generated rank inventory included non-runtime files; they were deferred with explicit scope rationale. |
