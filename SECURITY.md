# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability, privacy issue, or accidental credential exposure in **Expressive Weather**, please **do not** open a public GitHub issue with exploit details.

Instead, please report it privately using **GitHub Security Advisories**:
- Go to https://github.com/nemy-new/Expressive-Weather/security/advisories/new
- Provide a clear description of the issue, affected version/flavor (`play` or `github`), reproduction steps, and suggested remediation if available.

We aim to acknowledge reports within 7 days and coordinate a fix before public disclosure.

## Scope & Security Practices

- Release signing keys (`*.jks`, `*.keystore`, `keystore.properties`) and `local.properties` must never be committed to Git.
- Debug/test intents and disaster alert mock overrides are restricted to `BuildConfig.DEBUG`.
- Verbose/debug/info logs are stripped in release builds via R8/ProGuard.
