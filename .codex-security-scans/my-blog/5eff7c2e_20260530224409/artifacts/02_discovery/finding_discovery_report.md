# Finding Discovery Report

Repository-wide discovery used the per-scan threat model copied from `AGENTS.md`, generated `rank_input.csv` with 825 source-like rows, and selected a high-impact runtime deep-review set covering backend auth, messages, uploads, admin checks, config, MyBatis mappers, frontend auth transport, and frontend rendering.

Subagents were authorized but unavailable due usage-limit errors. Parent review closed the selected runtime worklist in `work_ledger.jsonl` and explicitly deferred non-runtime generated/dot-cache/doc/test/SQL-backup rows in `repository_coverage_ledger.md`.

## Candidates

| Candidate | Title | Primary root control | Status |
| --- | --- | --- | --- |
| CAND-001 | Private message conversation ids are not scoped to participants | `backend/src/main/java/com/myblog/application/service/MessageAppService.java:147` | promoted |
| CAND-002 | Article feed renders author-controlled titles and summaries as raw HTML | `frontend/src/components/ArticleFeed.vue:133` and `:346-347` | promoted |
| CAND-003 | Production-looking credentials and weak JWT secret are committed as defaults | `backend/src/main/resources/application.yml:21,71,83,84` | promoted |
| CAND-004 | JWTs are accepted from URL query parameters across all API routes | `backend/src/main/java/com/myblog/infrastructure/security/JwtAuthenticationInterceptor.java:54,75` | promoted |
