<div align="center">
  <p>
    <a href="../README.md">← README</a> ·
    <strong>Features</strong> ·
    <a href="BUILD.md">Build &amp; Setup</a> ·
    <a href="DATA_SOURCES.md">Data Sources</a> ·
    <a href="ja/FEATURES.md">🇯🇵 日本語</a>
  </p>
</div>

# Features

**Expressive Weather** is designed around Material 3 Expressive motion and visual clarity, combining multi-region meteorological data, high-resolution radars, and severe weather warnings in one Android application.

---

## 1. Material 3 Expressive UI & Adaptive Atmosphere

- **Dynamic Shapes & Morphing Visuals**: Expressive Material 3 shapes, frosted-glass cards, and animated transitions across the dashboard, Bento detail views, and initial setup screen.
- **Tilt-Responsive Weather Effects**: Real-time raindrop, glass condensation, and snow accumulation rendering driven by device tilt sensors.
- **Day/Night & Theme Modes**: Supports Weather-adaptive themes, System dynamic colors, and Light/Dark modes across every screen—including clear daytime sky and starry night sky atmospheres during onboarding.
- **Phone, Foldable & Tablet Layouts**: Responsive single-pane phone UI and two-pane tablet/foldable navigation rail and settings layouts.
- **Multi-Language Support**: Full localization in **English**, **Japanese**, **Simplified Chinese**, and **Traditional Chinese**, with a quick language switcher available from initial setup and settings.

---

## 2. Precipitation Countdown & Android 16 Live Updates

- **Minute-Level Rain Analysis**: Combines high-resolution precipitation nowcasts and short-range forecasts (`PrecipitationAnalyzer`) to calculate minutes until rain begins or clears.
- **Android 16 Rich Ongoing Notifications**: Supports Live Updates (`shortCriticalText` and live countdown status) in the status bar and lock screen, with optional automatic location tracking.

---

## 3. Multi-Region Weather Radar & Typhoon Tracking

- **Japan (JMA)**:
  - 250m High-Resolution Precipitation Nowcast (HRPNS)
  - +15-hour short-range precipitation forecast
  - Kikikuru disaster risk overlays (landslide, inundation, flood)
  - Himawari meteorological satellite imagery & JMA typhoon track / forecast cone overlays
- **United States (NOAA / IEM)**:
  - NEXRAD / MRMS base & composite reflectivity
  - Extrapolation nowcast and HRRR (+18-hour) numerical weather forecast layers
- **Global (RainViewer)**:
  - Seamless global composite precipitation radar tiles with timeline playback
- **Interactive Maps**: Native Google Maps SDK for Android with automatic light/dark custom styling, multi-layer caching, and smooth frame playback.

---

## 4. Multi-Region Disaster & Weather Alerts

- **Japan**: JMA Advisories, Warnings, Emergency Warnings, and Kikikuru Alert Levels (1–5).
- **United States**: NOAA / National Weather Service (NWS) active CAP & GeoJSON alerts.
- **Europe**: MeteoAlarm official alerts across 39 European countries.
- **Global**: World Meteorological Organization (WMO) warnings and UN/EC GDACS major disaster alerts.

---

## 5. Five Home-Screen Widgets

| Widget | Size | Highlights |
| :--- | :---: | :--- |
| **Compact** | 2×2 | Current condition, temperature, and daily high/low |
| **Medium Hourly** | 4×2 | Current weather + 5-hour forecast tiles at a glance |
| **Large Dashboard** | 4×4 | Current conditions, hourly row, and 7-day outlook |
| **Material Shape** | 2×2 | Organic Material 3 Expressive shape with bold temperature & icon |
| **Material You Rain Radar** | 4×2 | Live precipitation radar preview paired with current & upcoming weather |

---

## 6. Privacy-First Architecture

- No proprietary backend server, no account registration, no ads, and no third-party analytics SDKs.
- Works completely with manually searched cities—location permissions (foreground or background) are strictly optional, with prominent in-app disclosure before requesting background location.
- See [PRIVACY_POLICY.md](../PRIVACY_POLICY.md) for full details.
