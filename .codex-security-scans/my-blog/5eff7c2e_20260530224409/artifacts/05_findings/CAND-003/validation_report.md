# Validation Report: CAND-003

## Finding

Production-looking credentials and weak JWT secret are committed as defaults.

## Rubric

- [x] Sensitive credential-like values are committed in source.
- [x] The values are runtime defaults, not inert documentation.
- [x] A missing environment variable would activate the default.
- [x] The values protect meaningful external services or identity controls.
- [ ] Credential liveness was verified against the provider.

## Evidence

`backend/src/main/resources/application.yml` contains `MAIL_PASSWORD` default `qongmajewctojecc` at line 21, static JWT secret `replace-with-a-long-random-secret-for-local-development` at line 71, COS secret id default at line 83, and COS secret key default at line 84. The datasource also defaults to `root`/`123456` at lines 10-11 and enables `allowPublicKeyRetrieval=true` at line 9.

Spring property placeholders make these values active defaults when environment overrides are absent. `JwtTokenProvider` consumes `my-blog.jwt.secret` for HS256 signing, and `CosConfig` initializes a COS/S3 client when the secret id property is non-empty, which the committed default makes true.

## Tested

No network validation was attempted for the mail or COS credentials. The report does not claim the credentials are currently valid, only that live-looking secret material and a weak signing default are committed and active by default.

## Uncertainty

Credential liveness and deployed environment override status are unknown. The JWT default risk is fully local to deployment configuration: if deployed unchanged, tokens are forgeable by anyone with repository access.

## Minimal Next Step

Rotate the SMTP and COS credentials, remove secret defaults from source, require environment values for production, and fail startup when `my-blog.jwt.secret` equals a known development placeholder.
