# Security Review: my-blog

## Scope

- Scan target: `F:\software\IdeaProject\my-blog`
- Scan mode: repository-wide Codex Security scan, with parent-agent closure after authorized subagents failed due usage limit.
- Commit: `5eff7c2e`
- Scan id: `5eff7c2e_20260530224409`
- Artifacts: `.codex-security-scans/my-blog/5eff7c2e_20260530224409/artifacts`
- Runtime/test status: source-level validation only. Full Spring Boot runtime validation was not run because the app requires local MySQL and Redis setup; the surviving findings have direct code/config evidence.
- Explicit limitations: generated `rank_input.csv` contained dot-cache, docs, SQL seed/backups, tests, and local tool artifacts. Those non-runtime rows are explicitly deferred in the coverage ledger.

### Scan Summary

| Field | Value |
| --- | --- |
| Reportable findings | 4 |
| Severity mix | high: 3, medium: 1 |
| Confidence mix | high: 3, medium: 1 |
| Coverage | Backend auth, private messages, upload/storage, admin checks, MyBatis dynamic SQL, runtime config, frontend auth transport, and frontend rendering reviewed; non-runtime generated/cache/doc/test rows deferred. |
| Validation mode | Static source/config trace with per-candidate ledgers. |
| Final markdown | `.codex-security-scans/my-blog/5eff7c2e_20260530224409/report.md` |
| Final HTML | `.codex-security-scans/my-blog/5eff7c2e_20260530224409/report.html` |

## Threat Model

The scan used `AGENTS.md` as the authoritative repository-specific threat model. Key security-relevant context:

- Backend is Spring Boot with JWT authentication, Redis-backed status checks, MyBatis persistence, upload handling, notifications/messages streams, admin APIs, and a growth/points module.
- Frontend is Vue 3/Vite and proxies `/api` to the backend.
- Assets and privileges that matter include user accounts, JWTs, password reset flows, private messages, notifications, uploaded files, article content, admin-only moderation/configuration APIs, points/revenue/accounting data, MySQL data, Redis state, mail credentials, and COS storage credentials.
- Trust boundaries include anonymous users to public APIs, authenticated users to owner-only objects, authors to readers, users to private-message peers, normal users to admin APIs, frontend-local storage to injected script, and application runtime to external services such as MySQL, Redis, mail, and COS.
- Attacker-controlled inputs include request paths/query/body values, article/comment/message content, uploaded files and names, search/filter parameters, notification/ad/profile URLs, and frontend-rendered stored content.
- Important invariants: backend must validate user status even with valid JWTs; admin APIs require ADMIN role; controllers should not hold business logic; physical deletion is forbidden; public APIs should consistently return result wrappers; rendered/imported HTML must remain sanitized.

## Findings

| # | Title | Severity | Confidence |
| --- | --- | --- | --- |
| 1 | [Private message conversation ids are not scoped to participants](#1-private-message-conversation-ids-are-not-scoped-to-participants) | high | high |
| 2 | [Article feed renders author-controlled titles and summaries as raw HTML](#2-article-feed-renders-author-controlled-titles-and-summaries-as-raw-html) | high | high |
| 3 | [Production-looking credentials and weak JWT secret are committed as defaults](#3-production-looking-credentials-and-weak-jwt-secret-are-committed-as-defaults) | high | high |
| 4 | [JWTs are accepted from URL query parameters across all API routes](#4-jwts-are-accepted-from-url-query-parameters-across-all-api-routes) | medium | medium |

### Confidence Scale

| Label | Meaning |
| --- | --- |
| high | Direct source, configuration, or runtime evidence supports the finding, with no material unresolved reachability or exploitability blocker. |
| medium | Source evidence supports a plausible issue, but runtime behavior, deployment configuration, role reachability, type constraints, or exploit reliability still need proof. |
| low | Weak or incomplete evidence; include only when explicitly preserved as follow-up. |

### [1] Private message conversation ids are not scoped to participants

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The service and mapper code directly show that conversation-scoped reads/writes filter by `conversationId` without a participant check. |
| Category | Authorization bypass / IDOR |
| CWE | CWE-639 Authorization Bypass Through User-Controlled Key; CWE-862 Missing Authorization |
| Affected lines | `backend/src/main/java/com/myblog/interfaces/rest/controller/MessageController.java:385`; `backend/src/main/java/com/myblog/application/service/MessageAppService.java:147`; `backend/src/main/java/com/myblog/application/service/MessageAppService.java:180`; `backend/src/main/java/com/myblog/application/service/MessageAppService.java:196`; `backend/src/main/java/com/myblog/application/service/MessageAppService.java:293`; `backend/src/main/resources/mapper/MessageMapper.xml:39`; `backend/src/main/resources/mapper/MessageMapper.xml:50`; `backend/src/main/resources/mapper/MessageMapper.xml:107` |

#### Summary

Private-message endpoints accept a `conversationId` path parameter, but `MessageAppService` only checks that the conversation exists. It never verifies that the current user is `participantAId` or `participantBId`. The mapper methods then return or mutate messages by `conversation_id` only, so an authenticated user who knows or guesses another conversation id can cross the private-message boundary.

#### Validation

Method: static source trace. The route `/api/messages/conversations/{id}/messages` reaches `MessageAppService.listMessages`; that method loads the conversation by id and calls `countByConversation` and `findByConversation`. In `MessageMapper.xml`, those queries do not use the supplied current user. `sendMessage` and `markAllRead` have the same missing participant check. Remaining uncertainty is only id discovery, not the authorization defect itself.

#### Dataflow

`conversationId` path parameter -> `MessageController.listMessages` -> `MessageAppService.listMessages` -> `conversationRepository.findById` with no participant check -> `MessageMapper.selectByConversation` -> private message content returned. Similar flows allow unauthorized insert via `Message.create` and unauthorized read-state mutation via `markAllRead`.

#### Reachability

Any authenticated user can call the protected message endpoints. The attacker needs a target conversation id; once they have one, the code path does not contain a compensating guard.

#### Severity

High. The impact is direct confidentiality and integrity compromise of private messages. It is below critical because the attacker must authenticate and obtain a valid conversation id, but the object boundary break is clear and affects a core private workflow.

#### Remediation

Add a central `requireParticipant(conversation, currentUserId)` guard and call it before every conversation-scoped read/write/delete/mark-read operation. Update mapper queries and updates to join `blog_conversation` and require the current user to be a non-deleted participant. Add tests for outsider read, send, mark-read, and delete attempts.

### [2] Article feed renders author-controlled titles and summaries as raw HTML

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The source path from author-controlled article fields to `v-html` is direct, and the DOMPurify markdown sanitizer is not invoked for feed title/summary rendering. |
| Category | Stored XSS |
| CWE | CWE-79 Improper Neutralization of Input During Web Page Generation |
| Affected lines | `backend/src/main/java/com/myblog/application/service/ArticleAppService.java:555`; `backend/src/main/java/com/myblog/application/service/ArticleAppService.java:1084`; `frontend/src/components/ArticleFeed.vue:133`; `frontend/src/components/ArticleFeed.vue:135`; `frontend/src/components/ArticleFeed.vue:137`; `frontend/src/components/ArticleFeed.vue:346`; `frontend/src/components/ArticleFeed.vue:347` |

#### Summary

Article titles and summaries are author-controlled. Backend article sanitization only performs sensitive-word checks/masking; it does not HTML-escape these fields. The shared `ArticleFeed` component then renders both values through `v-html`. When no highlight keyword is present, `highlightHtml` returns the raw stored string.

#### Validation

Method: static source trace. `ArticleAppService.createArticle` passes title and summary through `sanitizeArticleContent`, which calls sensitive-word functions but no HTML sanitizer. `ArticleFeed.vue` lines 346-347 insert `highlightHtml(article.title)` and `highlightHtml(article.summary)` with `v-html`. `frontend/src/utils/markdown.js` uses DOMPurify for markdown bodies, but that code is not used by `ArticleFeed`.

#### Dataflow

Article author request body -> `CreateArticleCommand.title/summary` -> `ArticleAppService.sanitizeArticleContent` sensitive-word masking -> article DTO/list API -> `ArticleFeed.highlightHtml` -> `v-html` DOM insertion.

#### Reachability

A normal authenticated author can publish an article. Visitors to home, search, user profile, following, category, tag, topic, or column feeds can render that article in `ArticleFeed`. The app stores session material in localStorage, so injected JavaScript can read bearer tokens and make API calls as the victim.

#### Severity

High. This is stored XSS in a reusable feed component with realistic cross-user reach. Dynamic browser proof would refine payload details, but raw `v-html` insertion of stored author content plus localStorage tokens is enough to support high impact.

#### Remediation

Replace `v-html` with text rendering for title and summary, or rewrite `highlightHtml` to HTML-escape all user text before adding `<mark>` wrappers. Add a regression test with a title/summary containing an image event-handler payload and assert it renders as text.

### [3] Production-looking credentials and weak JWT secret are committed as defaults

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The sensitive values are present in active Spring configuration defaults; liveness of external credentials was not tested, but the committed default behavior is clear. |
| Category | Hardcoded credentials / weak signing default |
| CWE | CWE-798 Use of Hard-coded Credentials; CWE-321 Use of Hard-coded Cryptographic Key |
| Affected lines | `backend/src/main/resources/application.yml:21`; `backend/src/main/resources/application.yml:71`; `backend/src/main/resources/application.yml:83`; `backend/src/main/resources/application.yml:84` |

#### Summary

`application.yml` includes live-looking defaults for SMTP and Tencent COS credentials, plus a static JWT signing secret. These are not only examples: Spring property placeholders activate them whenever environment variables are absent. `CosConfig` is enabled when the COS secret id property is non-empty, which the default makes true.

#### Validation

Method: static configuration trace. The report did not attempt network checks against SMTP or COS. The finding is about committed active defaults and required rotation/removal, not a claim that the provider currently accepts the values.

#### Dataflow

Repository checkout / packaged config -> Spring property resolution -> mail sender, COS S3 client, and `JwtTokenProvider` consume defaults when env overrides are absent.

#### Reachability

Anyone with repository or artifact access can read these defaults. If the values are live or deployed unchanged, attackers can access external services or forge JWTs with the known signing key.

#### Severity

High. Cloud/mail credential exposure and predictable JWT signing material are meaningful compromise risks. Credential liveness or guaranteed production use would raise confidence in operational impact, while confirmed immediate rotation and enforced env-only startup would lower it.

#### Remediation

Rotate the SMTP and COS credentials. Remove secret defaults from source and use blank placeholders or environment-only configuration. Fail startup in non-local profiles when the JWT secret is the development placeholder or too short. Keep a redacted `application.yml.example` for documentation.

### [4] JWTs are accepted from URL query parameters across all API routes

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | medium |
| Confidence rationale | Backend query-token acceptance and frontend SSE URL construction are direct, but actual token leakage depends on deployment logging/referrer behavior not inspected in this scan. |
| Category | Token exposure in URL |
| CWE | CWE-598 Use of GET Request Method With Sensitive Query Strings |
| Affected lines | `backend/src/main/java/com/myblog/infrastructure/security/JwtAuthenticationInterceptor.java:54`; `backend/src/main/java/com/myblog/infrastructure/security/JwtAuthenticationInterceptor.java:61`; `backend/src/main/java/com/myblog/infrastructure/security/JwtAuthenticationInterceptor.java:74`; `backend/src/main/java/com/myblog/infrastructure/security/JwtAuthenticationInterceptor.java:75`; `frontend/src/api/messages.js:94`; `frontend/src/api/notifications.js:51` |

#### Summary

The backend reads a `token` query parameter and authenticates it for every protected `/api/**` request. The frontend uses this to connect EventSource streams, but the backend implementation is global rather than narrowly scoped. Bearer tokens in URLs are commonly captured by browser history, reverse proxies, access logs, error logs, analytics, and referrers.

#### Validation

Method: static source trace. `JwtAuthenticationInterceptor` reads `request.getParameter("token")`, passes it to `authenticateIfPresent`, and calls `authenticateToken(queryToken.trim())`. `messages.js` and `notifications.js` append the JWT to EventSource URLs.

#### Dataflow

localStorage session token -> frontend EventSource URL query string -> backend `request.getParameter("token")` -> normal JWT authentication -> protected request identity.

#### Reachability

Any valid user token can be sent in a URL. The known in-repository frontend does this for SSE streams. The exposure consequence depends on who can observe URLs in the deployed environment.

#### Severity

Medium. The code clearly enables URL-borne bearer tokens, but the scan did not prove that production logs or referrers expose them to an attacker. Evidence of broad log access or third-party referrer leakage would raise severity.

#### Remediation

Do not accept query-string JWTs globally. If SSE cannot use Authorization headers, issue short-lived, one-purpose SSE tokens accepted only on stream endpoints, or use cookie-backed stream auth with appropriate CSRF and SameSite controls.

## Reviewed Surfaces

| Surface | Risk Area | Outcome | Notes |
| --- | --- | --- | --- |
| Private message APIs | Authorization / object access | Reported | Missing participant checks in message history, send, mark-read, and deletion flows became finding 1. |
| Article feed rendering | Stored XSS | Reported | Shared feed uses `v-html` with unsanitized title/summary and became finding 2. |
| Runtime configuration | Secrets / default credentials | Reported | `application.yml` commits mail/COS/default JWT/DB secrets and became finding 3. |
| JWT transport | Token leakage | Reported | Backend accepts query token globally and frontend SSE sends token in URLs, finding 4. |
| Upload/static files | Path traversal / content-type confusion | Rejected | User controls file bytes/name/extension but object key is server-generated; no user-controlled storage path found. |
| Admin APIs | Missing role checks | No issue found | Representative controllers and grep checks show `ensureAdmin`/`requireAdmin` role checks. |
| MyBatis mappers | SQL injection | Rejected | No attacker-controlled `${}` found; internal selected column misuse is represented under message authorization. |
| Markdown renderer | Stored XSS | No issue found | DOMPurify and style filtering are used; the unsafe sink is the separate `ArticleFeed` highlighter. |
| Dot caches, docs, SQL backups, tests | Supply-chain / seed-data integrity | Needs follow-up | Generated rank inventory included non-runtime files; they were deferred with explicit scope rationale. |

## Open Questions And Follow Up

- Run a focused dynamic backend test for `MessageAppService` with two conversations and a third authenticated user to confirm all private-message object operations reject outsiders after a fix.
- Run a frontend e2e proof for `frontend/src/components/ArticleFeed.vue` using malicious title and summary payloads, then lock the regression after escaping/text-rendering is implemented.
- Rotate and verify invalidation of `backend/src/main/resources/application.yml` mail/COS credentials before publishing any fix branch.
- Decide whether SSE should use short-lived stream-only tokens or cookie-backed authentication, then remove global query-token acceptance from `JwtAuthenticationInterceptor`.
