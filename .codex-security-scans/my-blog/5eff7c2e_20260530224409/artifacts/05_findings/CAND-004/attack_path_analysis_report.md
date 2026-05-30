# Attack Path Analysis: CAND-004

## Decision

Reportable, medium severity.

## Attack Path

1. The frontend opens SSE streams with `token=<jwt>` in the URL.
2. The backend also accepts `token` for any protected `/api/**` route, not just SSE.
3. URLs containing bearer tokens can be stored by browser history, reverse proxies, access logs, error logs, analytics, or referrers from pages that load third-party resources.
4. Anyone who obtains the URL token can authenticate as the user until token expiry.

## Counterevidence

Normal Axios API calls use the Authorization header, and the token is needed for EventSource compatibility. However, the backend global query-token acceptance broadens the pattern beyond the narrow SSE use case, and URL credentials are a recognized leakage hazard.

## Severity

Medium. The bug can expose bearer tokens, but actual exploitation depends on deployment logging/referrer behavior or another party seeing URLs. Scope-limited one-time SSE tokens would materially reduce the impact.
