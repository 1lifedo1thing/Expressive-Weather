<div align="center">
  <p>
    <a href="../README.md">← README</a> ·
    <a href="FEATURES.md">Features</a> ·
    <strong>Build &amp; Setup</strong> ·
    <a href="DATA_SOURCES.md">Data Sources</a> ·
    <a href="ja/BUILD.md">🇯🇵 日本語</a>
  </p>
</div>

# Build & Setup Guide

---

## Prerequisites

- **Android Studio** supporting AGP 9.x and Kotlin 2.3
- **JDK 17**
- **Android SDK 36** (Android 16)
- **Google Maps API Key** with **Maps SDK for Android** enabled in Google Cloud Console *(required only for displaying interactive radar map tiles; unit tests and compilation work without a key)*

---

## 1. Clone the Repository

```bash
git clone https://github.com/nemy-new/Expressive-Weather.git
cd Expressive-Weather
```

---

## 2. Configure `MAPS_API_KEY`

Copy [`local.properties.example`](../local.properties.example) to `local.properties` and set your key:

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

You can also pass `MAPS_API_KEY` via environment variable in CI/CD.

> **Security Tip**: In Google Cloud Console → **APIs & Services → Credentials**, restrict your Android API key to:
> - **Application restriction**: Android apps (`io.github.nemynew.expressiveweather` + your debug/release/Play App Signing SHA-1 certificate fingerprint)
> - **API restriction**: **Maps SDK for Android** only

---

## 3. Build Debug APKs

```bash
# Google Play flavor
./gradlew assemblePlayDebug

# GitHub flavor
./gradlew assembleGithubDebug
```

---

## 4. Build Signed Release Artifacts

Create `keystore.properties` from [`keystore.properties.example`](../keystore.properties.example) (never commit it to Git):

```properties
storeFile=path/to/release-keystore.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

Alternatively, supply `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` as environment variables.

```bash
# Google Play App Bundle (.aab)
./gradlew bundlePlayRelease

# Google Play APK (.apk)
./gradlew assemblePlayRelease

# GitHub Release APK (.apk)
./gradlew assembleGithubRelease
```

Optional Gradle properties / environment variables:
- `VERSION_NAME` / `VERSION_CODE`: Override app version metadata for automated releases.
- `REQUIRE_RELEASE_CONFIG=true`: Fail the build early if `MAPS_API_KEY` or release signing credentials are missing (recommended for CI).

---

## 5. Run Tests & Lint

```bash
# Unit tests for both flavors
./gradlew testPlayDebugUnitTest testGithubDebugUnitTest

# Android Lint checks
./gradlew lintPlayDebug lintVitalPlayRelease lintVitalGithubRelease
```
