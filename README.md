<div align="center">
  <br />
  <img src="docs/images/logo.png" alt="Expressive Weather Logo" width="110" />
  <h1>Expressive Weather</h1>
  <p><strong>An open-source Android weather, radar, and multi-region severe weather alert app designed with Material 3 Expressive.</strong></p>

  <p>
    <a href="README.ja.md">🇯🇵 日本語 README</a> ·
    <a href="#installation">Installation</a> ·
    <a href="docs/FEATURES.md">Features</a> ·
    <a href="docs/BUILD.md">Build &amp; Setup</a> ·
    <a href="docs/DATA_SOURCES.md">Data Sources</a>
  </p>
  <p>
    <a href="PRIVACY_POLICY.md">Privacy Policy</a> ·
    <a href="THIRD_PARTY_NOTICES.md">Third-Party Notices</a> ·
    <a href="CONTRIBUTING.md">Contributing</a> ·
    <a href="CHANGELOG.md">Changelog</a> ·
    <a href="SECURITY.md">Security</a>
  </p>

  <p>
    <a href="https://m3.material.io/">
      <img alt="Material 3 Expressive" src="https://custom-icon-badges.demolab.com/badge/m3%20expressive-lightblue?style=for-the-badge&logoColor=333&logo=material-you" />
    </a>
    <img src="https://img.shields.io/badge/Android-16%20(API%2036)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 16 Ready" />
    <img src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.3" />
    <a href="LICENSE">
      <img src="https://img.shields.io/badge/License-Apache--2.0-blue?style=for-the-badge" alt="License Apache-2.0" />
    </a>
  </p>
</div>

<br />

<div align="center">
  <img src="docs/images/01-main-london.png" width="18%" alt="Main Screen Light" />
  <img src="docs/images/02-main-rain-tokyo.png" width="18%" alt="Rain Glass Nowcast Dark" />
  <img src="docs/images/03-main-rome.png" width="18%" alt="Sunset Rome" />
  <img src="docs/images/04-radar-us-nexrad.png" width="18%" alt="US NEXRAD HRRR Radar" />
  <img src="docs/images/05-radar-japan-jma.png" width="18%" alt="Japan JMA Radar & Nowcast" />
</div>

<br />

---

## Installation

Requires **Android 7.0 (API 24)** or later.

<p>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22io.github.nemynew.expressiveweather%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fnemy-new%2FExpressive-Weather%22%2C%22author%22%3A%22nemy-new%22%2C%22name%22%3A%22Expressive%20Weather%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%7D%22%7D">
    <img src="docs/images/badge_obtainium.png" alt="Get it on Obtainium" height="48" />
  </a>
</p>

- **Obtainium (Recommended for automatic updates)**: Tap the **Get it on Obtainium** badge above to add Expressive Weather directly to [Obtainium](https://github.com/ImranR98/Obtainium) (pre-releases enabled).
- **Direct APK Download**: Download the latest `app-github-release.apk` from [**GitHub Releases**](https://github.com/nemy-new/Expressive-Weather/releases).

> **Google Play Store**: Distribution on Google Play Store is also being considered for the future.

---

## Shortcuts

Jump directly to the section you need:

| Section | Description |
| :--- | :--- |
| [**Features**](docs/FEATURES.md) | Material 3 Expressive UI, tilt-reactive effects, Android 16 Live Updates, regional radars, alerts, and 5 widgets |
| [**Build & Setup**](docs/BUILD.md) | Prerequisites, `MAPS_API_KEY` setup, debug/release Gradle commands, signing, and unit tests |
| [**Data Sources & Attributions**](docs/DATA_SOURCES.md) | Regional routing (Japan / US / Europe / Global) and complete meteorological data provider credits |
| [**Privacy Policy**](PRIVACY_POLICY.md) | No backend server, no ads/tracking, and optional location usage details (EN / JA) |
| [**Third-Party Notices**](THIRD_PARTY_NOTICES.md) | Open-source libraries, bundled fonts, icons, and dataset license texts |
| [**Contributing**](CONTRIBUTING.md) · [**Security**](SECURITY.md) | Pull request guidelines, coding conventions, and vulnerability reporting |

---

## Overview

**Expressive Weather** combines expressive Material 3 UI, tilt-responsive rain/snow atmosphere effects, high-resolution regional weather radars (Japan JMA, US NEXRAD/MRMS/HRRR, Global RainViewer), 5 home-screen widgets, and official severe weather warnings (JMA Kikikuru, NOAA/NWS, European MeteoAlarm, WMO, GDACS) into a single privacy-respecting Android application.

> **Disclaimer**: Expressive Weather is an **unofficial third-party open-source application** and is not affiliated with or endorsed by JMA, GSI, NOAA/NWS, MeteoAlarm, WMO, GDACS, or any government entity. In emergencies, always follow official announcements from local authorities.

---

## Quick Start

```bash
git clone https://github.com/nemy-new/Expressive-Weather.git
cd Expressive-Weather
cp local.properties.example local.properties   # Set MAPS_API_KEY
./gradlew assemblePlayDebug testPlayDebugUnitTest
```

For full signing and Maps API key restriction instructions, see [**Build & Setup**](docs/BUILD.md).

---

## License

Licensed under the **Apache License, Version 2.0**. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details.
