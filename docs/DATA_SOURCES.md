<div align="center">
  <p>
    <a href="../README.md">← README</a> ·
    <a href="FEATURES.md">Features</a> ·
    <a href="BUILD.md">Build &amp; Setup</a> ·
    <strong>Data Sources</strong> ·
    <a href="ja/DATA_SOURCES.md">🇯🇵 日本語</a>
  </p>
</div>

# Data Sources & Attributions

Expressive Weather automatically selects the most accurate public meteorological and disaster alert providers for each region. For full license texts and detailed attributions, see [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

> **Disclaimer**: Expressive Weather is an **unofficial third-party open-source application** and is not affiliated with or endorsed by JMA, GSI, NOAA/NWS, MeteoAlarm, WMO, GDACS, or any government entity. During severe weather or emergencies, always follow official announcements from local authorities.

---

## Regional Data Routing

| Region | Weather & Forecasts | Radar & Nowcast | Severe Weather Alerts |
| :--- | :--- | :--- | :--- |
| **Japan** | JMA AMeDAS & Official Forecasts + Open-Meteo (AQI / 15m precip) | JMA High-Resolution Nowcast (HRPNS), +15h Forecast, Kikikuru, Himawari, Typhoon Tracks | JMA Warnings / Advisories / Emergency Warnings, Kikikuru (Levels 1–5), GDACS |
| **United States** | Open-Meteo Global / Local NWP (NOAA GFS, HRRR, ECMWF) | NOAA MRMS / NEXRAD (via IEM) & HRRR (+18h) | NOAA / NWS Official Alerts, GDACS |
| **Europe** | Open-Meteo Global / Regional NWP (ECMWF, ICON, GFS) | RainViewer Global Composite | MeteoAlarm (EUMETNET, 39 countries), WMO, GDACS |
| **Rest of World** | Open-Meteo Global NWP | RainViewer Global Composite | WMO Official Warnings, GDACS |

---

## Complete Provider List

- **Forecasts & Air Quality**: [Open-Meteo](https://open-meteo.com/) ([CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), non-commercial free tier)
- **Japan Weather, Radar & Warnings**: [Japan Meteorological Agency (JMA)](https://www.jma.go.jp/) (Unofficial client)
- **Japan Base Maps & Hazard Overlays**: [Geospatial Information Authority of Japan (GSI)](https://maps.gsi.go.jp/development/ichiran.html)
- **US Alerts & Radar**: [NOAA / NWS](https://www.weather.gov/) & [Iowa Environmental Mesonet (IEM)](https://mesonet.agron.iastate.edu/)
- **European Alerts**: [MeteoAlarm (EUMETNET)](https://meteoalarm.org/)
- **International Warnings & Disasters**: [WMO](https://severeweather.wmo.int/) & [GDACS](https://www.gdacs.org/)
- **Global Radar Tiles**: [RainViewer](https://www.rainviewer.com/)
- **City Search / Geocoding**: [Photon](https://photon.komoot.io/) by komoot (Data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, ODbL)
- **Interactive Maps**: Google Maps SDK for Android (Map data © Google)
- **Solar Calculations**: On-device NOAA astronomical equations (fully offline)
