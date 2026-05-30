# Attack Path Analysis: CAND-001

## Decision

Reportable, high severity.

## Attack Path

1. An authenticated user sends requests to `/api/messages/conversations/{id}/messages`, `/api/messages/conversations/{id}/messages` POST, `/api/messages/conversations/{id}/read`, or conversation deletion routes with a conversation id that belongs to two other users.
2. The controller delegates to `MessageAppService`.
3. `MessageAppService` loads the `Conversation` by id but does not check that `currentUserId` is a participant.
4. MyBatis queries/updates operate by `conversation_id` only, returning private messages or modifying message/conversation state.

## Counterevidence

`listConversations` is correctly scoped by user, but that only controls the list endpoint. Direct object endpoints accept any path id. There is no repository evidence of a global object-authorization interceptor.

## Severity

High. The issue crosses a user-to-user confidentiality and integrity boundary in a core private-message feature. It requires authentication and a valid/guessable conversation id, which keeps it below critical, but the impact includes private message disclosure and unauthorized conversation writes.
