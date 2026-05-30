# Dedupe Report

Four raw candidates were discovered. No cross-file dedupe merged independently attackable instances:

- CAND-001 remains one authorization finding because reading, sending, marking-read, and deletion tampering all share the same missing participant check on `conversationId`.
- CAND-002 remains one stored-XSS finding because title and summary are two sinks for the same shared renderer/root control.
- CAND-003 remains one configuration-secret finding because the affected values share one fail-open default configuration surface.
- CAND-004 remains one token-handling finding because frontend SSE sources and backend query-token acceptance form one credential exposure path.
