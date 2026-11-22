# Changelog

All notable changes to **Expressive Weather** will be documented in this file.

## [Unreleased]

### Added
- Split Gradle builds into `play` (Google Play Store) and `github` (GitHub Releases / self-build) product flavors under unified Application ID `io.github.nemynew.expressiveweather`.
- Added in-app **Background Location Prominent Disclosure** dialog prior to opening Android location settings.
- Added bilingual **Privacy Policy** (`PRIVACY_POLICY.md`) and in-app Privacy Policy dialog in Settings and Setup screens.
- Added `THIRD_PARTY_NOTICES.md` and in-app third-party OSS/font/icon license notices (Google Sans Flex OFL 1.1, Fluent System Icons MIT, AndroidX, OkHttp, Haze, Google Maps SDK).
- Added missing data source attributions (MeteoAlarm, Iowa Environmental Mesonet, GSI, Google Maps) and enabled radar source credit display by default.
- Added GitHub Actions CI & Release workflows, Issue/PR templates, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, and `keystore.properties.example`.
- Added 5 home-screen widgets (Compact, Medium, Large, Material Shape, Material You Rain Radar) and JMA typhoon tracking overlay.

### Changed
- Relicensed project from GPL-3.0 to **Apache License 2.0** to resolve licensing compatibility with Google Play Services Maps SDK.
- Migrated package name, namespace, and Room schemas from `com.example.expressiveweather` to `io.github.nemynew.expressiveweather`.
- Updated launcher app name from generic `Weather`/`天気` to `Expressive Weather` across all locales.
- Removed `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission and external donation links from the `play` flavor.
- Removed unused `location` foreground service type from `KikikuruLiveAlertService`.
- Replaced Esri ArcGIS Online and OSM standard tile direct fetching in `WidgetRadarMapRenderer` with GSI tiles (Japan) and Material You radar-scope fallback (non-Japan).
- Standardized honest app User-Agent (`+https://github.com/nemy-new/Expressive-Weather`), removed spoofed Referer/Chrome UA headers, reduced radar host concurrency, and scoped startup connection prewarm to the active city region.
- Disabled 10-minute Doze-piercing widget alarms in the `play` flavor in favor of WorkManager periodic scheduling.
- Restricted disaster alert mock tests to `BuildConfig.DEBUG` and stripped verbose/debug/info logs in release builds via ProGuard.

## [0.1.0] - 2026-10-01

- Initial open-source release.
