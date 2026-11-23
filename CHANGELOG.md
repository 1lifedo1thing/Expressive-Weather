# Changelog

All notable changes to **Expressive Weather** will be documented in this file.

## [Unreleased]

## [0.1.0] - 2026-11-23 (Pre-release)

### Added
- Split Gradle builds into `play` (Google Play Store) and `github` (GitHub Releases / self-build) product flavors under unified Application ID `io.github.nemynew.expressiveweather`.
- Added in-app **Background Location Prominent Disclosure** dialog prior to opening Android location settings.
- Added bilingual **Privacy Policy** (`PRIVACY_POLICY.md`) and in-app Privacy Policy dialog in Settings and Setup screens.
- Added `THIRD_PARTY_NOTICES.md` and in-app third-party OSS/font/icon license notices (Google Sans Flex OFL 1.1, Fluent System Icons MIT, AndroidX, OkHttp, Haze, Google Maps SDK).
- Added missing data source attributions (MeteoAlarm, Iowa Environmental Mesonet, GSI, Google Maps) and enabled radar source credit display by default.
- Added GitHub Actions CI & Release workflows, Issue/PR templates, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, and `keystore.properties.example`.
- Added 5 home-screen widgets (Compact 2×2, Medium 4×2, Large 4×4, Material Shape, Material You Rain Radar) and JMA typhoon tracking overlay.
- Added in-app Feedback card (Bug Report & Feature Request) with privacy-sanitized diagnostics for `< 1.0.0` builds, positioned below all dashboard tiles.
- Added top-right language switcher, popular city quick picks, and inline location/permission recovery UI to the initial setup screen.
- Split repository documentation into dedicated section guides under `docs/` (English and Japanese) linked from top-level README shortcuts.

### Changed
- Relicensed project from GPL-3.0 to **Apache License 2.0** to resolve licensing compatibility with Google Play Services Maps SDK.
- Migrated package name, namespace, and Room schemas from `com.example.expressiveweather` to `io.github.nemynew.expressiveweather`.
- Kept localized short launcher app labels (`Weather` / `天気` / `天气` / `天氣`) across all locales.
- Updated initial setup screen to adapt to light (daytime clear sky) and dark (starry night sky) themes with a dedicated circular app icon and high-contrast morphing Material 3 shapes.
- Placed General Settings first across both phone settings sheet and tablet two-pane settings.
- Removed the "Tap to refresh" label from the bottom data source footer while retaining tap-to-refresh functionality.
- Configured all 5 home-screen widgets to always open the configuration screen upon placement and default to **Material You** styling.
- Restricted the standard 2×2 Compact widget so it never promotes into hourly forecast layouts on wide/tablet grids.
- Removed `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission and external donation links from the `play` flavor.
- Removed unused `location` foreground service type from `KikikuruLiveAlertService`.
- Replaced Esri ArcGIS Online and OSM standard tile direct fetching in `WidgetRadarMapRenderer` with GSI tiles (Japan) and Material You radar-scope fallback (non-Japan).
- Standardized honest app User-Agent (`+https://github.com/nemy-new/Expressive-Weather`), removed spoofed Referer/Chrome UA headers, reduced radar host concurrency, and scoped startup connection prewarm to the active city region.
- Disabled 10-minute Doze-piercing widget alarms in the `play` flavor in favor of WorkManager periodic scheduling.
- Restricted disaster alert mock tests to `BuildConfig.DEBUG` and stripped verbose/debug/info logs in release builds via ProGuard.

### Fixed
- Added Room, WorkManager, App Startup, and ViewModel R8 full-mode keep rules to prevent reflection instantiation crashes in minified release builds.
- Prevented unnecessary `MapView` recreation on configuration updates in `RadarMapView`.
- Prevented unsolicited location permission prompts on `MainScreen` immediately after selecting a city manually in Setup.
