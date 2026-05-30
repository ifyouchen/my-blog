# Validation Report: CAND-004

## Finding

JWTs are accepted from URL query parameters across all API routes.

## Rubric

- [x] A bearer credential can be supplied through a URL query string.
- [x] The backend accepts that URL credential for protected routes.
- [x] The frontend emits real session tokens in URLs for SSE.
- [x] Exposure channels exist for URLs.
- [ ] Deployment logs/referrers were inspected for actual leakage.

## Evidence

`JwtAuthenticationInterceptor.java:54` reads `request.getParameter("token")`. `JwtAuthenticationInterceptor.java:61` passes that value to `authenticateIfPresent` for all non-public requests. Lines 74-75 authenticate the query token when present. There is no path restriction to `/api/messages/stream` or `/api/notifications/stream`.

`frontend/src/api/messages.js:94-95` creates an EventSource URL with `token=<jwt>`. `frontend/src/api/notifications.js:50-51` does the same. Because the backend accepts `token` globally, the URL credential pattern is not constrained to the SSE compatibility problem.

## Tested

No live request was sent. Static validation is sufficient for backend acceptance and frontend URL construction. Actual exfiltration depends on logs, proxy behavior, browser history, and outbound referrers.

## Uncertainty

This finding is medium confidence and medium severity because the vulnerable behavior is clear, but proof of token leakage from a deployed environment was not gathered.

## Minimal Next Step

Stop accepting `token` query parameters globally. If EventSource cannot send headers, issue short-lived one-purpose SSE tokens scoped only to stream endpoints, or use cookie authentication with CSRF-aware constraints.
