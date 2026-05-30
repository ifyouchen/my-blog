# Validation Summary

Validation mode: focused source trace. Full dynamic HTTP validation was not attempted because the backend requires MySQL and Redis local services plus application startup. For these candidates, source-level evidence is direct enough to validate reportability without a runtime harness.

| Candidate | Disposition | Confidence | Method |
| --- | --- | --- | --- |
| CAND-001 | reportable | high | Static trace from controller path parameter to service and MyBatis queries/updates. |
| CAND-002 | reportable | high | Static trace from article author input through backend masking to frontend `v-html`. |
| CAND-003 | reportable | high | Static configuration review of committed defaults. |
| CAND-004 | reportable | medium | Static trace of query-token acceptance and frontend EventSource URL construction. |

## Closure Table

| Ledger row | Instance key | Root-control file:line | Entrypoint/source | Sink/control | Disposition | Counterevidence or proof gap | Survives |
| --- | --- | --- | --- | --- | --- | --- | --- |
| COV-AUTHZ-MESSAGES | authz:MessageAppService.java:143 | `backend/src/main/java/com/myblog/application/service/MessageAppService.java:147` | `/api/messages/conversations/{id}/messages` | `MessageMapper.xml:39`, `MessageAppService.java:196`, `MessageMapper.xml:107` | reportable | No participant check found in service or mapper. | yes |
| COV-XSS-FEED | xss:ArticleFeed.vue:346 | `frontend/src/components/ArticleFeed.vue:133` | Article title/summary from author-controlled create/update | `ArticleFeed.vue:346-347` | reportable | Markdown sanitizer is not on this path. | yes |
| COV-SECRETS-CONFIG | secrets:application.yml:21 | `backend/src/main/resources/application.yml:21,71,83,84` | Repository/runtime config defaults | Credentials and JWT secret values | reportable | Credential liveness not checked; committed defaults are still sensitive. | yes |
| COV-AUTH-TOKEN-QUERY | auth-token:JwtAuthenticationInterceptor.java:54 | `backend/src/main/java/com/myblog/infrastructure/security/JwtAuthenticationInterceptor.java:54` | `token` query parameter and SSE URLs | `authenticateToken(queryToken.trim())` | reportable | Actual log/referrer exposure depends on deployment, so confidence is medium. | yes |
