# Fix Report

## Scope

Fixed findings CAND-001, CAND-002, and CAND-004 from the repository security scan.

## Changes

- CAND-001: `MessageAppService` now loads conversations through `loadOwnedConversation`, which rejects users who are not participants before list, send, delete, or mark-read actions reach message repositories. `Conversation` now exposes `isParticipant`.
- CAND-002: `ArticleFeed.vue` now HTML-escapes article titles and summaries before applying keyword highlighting and rendering through `v-html`.
- CAND-004: `JwtAuthenticationInterceptor` no longer accepts JWTs from the `token` query parameter. Frontend message and notification SSE subscriptions now use a fetch-based event stream with an `Authorization` header instead of `EventSource` URLs containing tokens.

## Validation

- `mvn -q '-Dtest=MessageAppServiceTest,JwtAuthenticationInterceptorTest' test`: passed.
- `npm run build`: passed.
- Static check: `rg 'EventSource|token=|getParameter' frontend\src\api backend\src\main\java\com\myblog\infrastructure\security -n` returned no matches.

## Regression Coverage

- `MessageAppServiceTest` covers non-participant rejection for listing, sending, deleting, and mark-read paths, plus positive list behavior for a legitimate participant.
- `JwtAuthenticationInterceptorTest` covers rejection of a protected request that supplies only a query token.
- Frontend build validates the new authorized SSE fetch helper and escaped article feed rendering compile successfully.

## Remaining Risk

Runtime browser-level SSE behavior was not exercised against a live backend because the local MySQL/Redis stack was not started for this fix pass. The implemented transport preserves reconnect behavior in application code and sends JWTs only in the `Authorization` header.
