# Attack Path Analysis Summary

All four validated candidates are in scope for the web application threat model: user-generated content, private messages, authentication, uploaded/runtime configuration, and admin/user APIs are product surfaces.

| Candidate | Decision | Severity | Rationale |
| --- | --- | --- | --- |
| CAND-001 | reportable | high | Authenticated users can cross object boundaries to read and mutate private conversations by id. |
| CAND-002 | reportable | high | A normal author can store script-capable markup that executes for feed viewers and can read localStorage JWTs. |
| CAND-003 | reportable | high | Committed cloud/mail credentials and weak JWT defaults can compromise external services or token signing if exposed/deployed. |
| CAND-004 | reportable | medium | URL-borne JWTs are exposed through common logging/history/referrer channels, but deployed leakage evidence was not collected. |
