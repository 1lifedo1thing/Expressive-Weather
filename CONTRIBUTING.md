# Contributing to Expressive Weather

Thank you for your interest in contributing to **Expressive Weather**!

## Getting Started

1. Fork this repository and clone your fork:
   ```bash
   git clone https://github.com/<your-username>/Expressive-Weather.git
   cd Expressive-Weather
   ```
2. Copy `local.properties.example` to `local.properties` and configure `sdk.dir` (and `MAPS_API_KEY` if you are testing Google Maps radar screens).
   - You can compile and run all unit tests without a Maps API key.
3. Build and test both flavors before opening a pull request:
   ```bash
   ./gradlew testPlayDebugUnitTest testGithubDebugUnitTest assemblePlayDebug assembleGithubDebug
   ```

## Distribution Flavors

- `play`: Google Play Store compliant build (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` excluded, external donation links disabled, WorkManager-based widget updates).
- `github`: GitHub Releases / self-build flavor (full background/alarm options and developer support links).

Please keep Play Store policy constraints in mind when adding permissions, background services, or external links.

## Code & PR Guidelines

- Follow official Kotlin code style (`kotlin.code.style=official`).
- Never commit API keys, keystores (`*.jks`, `*.keystore`), `local.properties`, or `keystore.properties`.
- Never log precise user coordinates (`latitude`, `longitude`) or personal data in release builds.
- Include unit tests for parsers, alert models, and domain logic changes.
- Respect third-party API Terms of Service, rate limits, and attribution requirements (see `THIRD_PARTY_NOTICES.md`).

## Licensing

By submitting a pull request, you agree that your contributions will be licensed under the **Apache License 2.0** (see [LICENSE](LICENSE)).
