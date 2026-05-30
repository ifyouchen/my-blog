# Validation Report: CAND-001

## Finding

Private message conversation ids are not scoped to participants.

## Rubric

- [x] Attacker controls an object identifier crossing a privilege boundary.
- [x] The service loads the protected object without checking ownership/participation.
- [x] The persistence sink returns or mutates protected data without the caller identity.
- [x] Existing guards do not re-establish the missing object-level authorization.
- [x] Impact reaches private message confidentiality or integrity.

## Evidence

`MessageController.listMessages` exposes `/api/messages/conversations/{id}/messages` and passes the path id to `messageAppService.listMessages`. In `MessageAppService.listMessages`, line 147 loads the conversation by id, but the method never checks that `currentUserId` equals `participantAId` or `participantBId`. The following calls at lines 150-151 pass `currentUserId`, but `MessageMapper.xml` lines 39-44 and 50-53 filter only by `conversation_id` and deletion flags.

The same broken control appears in `sendMessage`: `MessageAppService.java:180` loads any conversation id, then `MessageAppService.java:196` creates a message in that conversation with the attacker's user id. `markAllRead` at `MessageAppService.java:293` calls a mapper update where `MessageMapper.xml:107-112` filters by conversation id and `sender_id != receiverId`, but does not verify that `receiverId` is a participant. `deleteConversation` is also unsafe because `MyBatisConversationRepository.deleteByUser` chooses `b_deleted_at` for any non-A user.

## Tested

No live HTTP reproduction was run. Static validation is sufficient because the missing participant guard and identity-free mapper predicates are directly visible. Existing tests were not found covering this authorization path.

## Uncertainty

Conversation id predictability depends on the id generator, but an attacker can also learn ids from their own conversations and attempt neighboring ids. Even without enumeration scale, any leaked id is directly exploitable.

## Minimal Next Step

Add a participant check helper before every conversation-scoped read/write and update mapper predicates to join `blog_conversation` on `participant_a_id`/`participant_b_id`.
