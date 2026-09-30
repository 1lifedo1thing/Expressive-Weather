<div align="center">
  <br />
  <img src="docs/images/logo.png" alt="Expressive Weather Logo" width="110" />
  <h1>Expressive Weather</h1>

  <a href="https://m3.material.io/">
    <img alt="Material 3 Expressive" src="https://custom-icon-badges.demolab.com/badge/m3%20expressive-lightblue?style=for-the-badge&logoColor=333&logo=material-you" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=for-the-badge" alt="License GPL-3.0" />
  </a>
</div>

<br />

<div align="center">
  <img src="docs/images/01-main-london.png" width="180" alt="Main Screen Light" />
  <img src="docs/images/02-main-rain-tokyo.png" width="180" alt="Rain Glass Nowcast Dark" />
  <img src="docs/images/04-radar-us-nexrad.png" width="180" alt="US NEXRAD HRRR Radar" />
  <img src="docs/images/05-radar-japan-jma.png" width="180" alt="Japan JMA Radar & Nowcast" />
</div>

<br />

### 特徴
- 端末の傾き（ジャイロ）とリアルタイムに連動する雨粒・ガラスの屈折アニメーション
- 気象庁（高解像度降水ナウキャスト、キキクル危険度分布、雨雲の動き +15h）
- 全米 NEXRAD / MRMS 地上反射強度実況 & HRRR 数値予報シミュレーション (+18h)
- RainViewer グローバル気象レーダー
- 雨の降り始め・止み時を通知するバックグラウンド降水ナウキャスト通知

### ビルド方法
1. `local.properties` に Google Maps API キーを記述：
   ```properties
   MAPS_API_KEY=your_google_maps_api_key
   ```
2. ビルド & インストール：
   ```bash
   ./gradlew installDebug
   ```

### データソース
- **天気予報**: [Open-Meteo](https://open-meteo.com/) (CC BY 4.0)
- **雨雲・防災**: 出典 気象庁（※本アプリは気象庁公式のアプリではありません）
- **気象レーダー**: [RainViewer](https://www.rainviewer.com/), NOAA / NWS (MRMS, HRRR)
- **都市検索**: [Photon](https://photon.komoot.io/) (© [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors)
- **国際警報**: GDACS, WMO

### ライセンス
[GNU General Public License v3.0 (GPL-3.0)](LICENSE)
