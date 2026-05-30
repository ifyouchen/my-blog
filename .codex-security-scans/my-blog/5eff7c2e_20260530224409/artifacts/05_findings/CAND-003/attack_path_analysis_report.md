# Attack Path Analysis: CAND-003

## Decision

Reportable, high severity.

## Attack Path

1. A person or process with repository, build artifact, log bundle, or image access reads `backend/src/main/resources/application.yml`.
2. The file exposes default SMTP credentials, COS access key material, and the JWT signing secret.
3. If those values are live or deployed unchanged, the attacker can access external cloud/mail resources or forge application JWTs.

## Counterevidence

The values are expressed as environment-overridable defaults, so a hardened deployment can avoid using them. That does not defeat the finding because the defaults themselves are committed and active when overrides are absent. Credential liveness was not tested.

## Severity

High. COS/mail credentials and JWT signing material are security-sensitive. Confidence in credential liveness is not proven, but committed active defaults are enough to require rotation and source cleanup.
