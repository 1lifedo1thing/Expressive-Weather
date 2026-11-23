<div align="center">
  <p>
    <a href="../../README.ja.md">← 日本語 README</a> ·
    <a href="FEATURES.md">主な機能</a> ·
    <a href="BUILD.md">ビルド・セットアップ</a> ·
    <strong>データソース</strong> ·
    <a href="../DATA_SOURCES.md">🇺🇸 English</a>
  </p>
</div>

# データソース & 出典

Expressive Weather は、選択された都市・地域に応じて最適な公的気象機関および広域データソースを自動選択します。詳細なライセンス表記・帰属表示は [THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md) をご覧ください。

> **免責事項**: 本アプリは独立して開発された**非公式のサードパーティ製オープンソースアプリ**であり、気象庁、国土地理院、NOAA/NWS、MeteoAlarm、WMO、GDACS その他いかなる公的機関とも関係ありません。災害・緊急時は必ず気象庁や自治体の公式発表をご確認ください。

---

## 地域別データルーティング

| 対象地域 | 天気実況・予報 | 雨雲レーダー・衛星 | 気象・災害警報 |
| :--- | :--- | :--- | :--- |
| **日本** | 気象庁（JMA）アメダス・府県/週間予報 ＋ Open-Meteo（大気質・15分降水） | 気象庁 高解像度降水ナウキャスト（HRPNS）、降水短時間予報（+15時間）、キキクル、ひまわり、台風進路 | 気象庁 特別警報・警報・注意報、キキクル（警戒レベル 1〜5）、GDACS |
| **米国** | Open-Meteo 全球／局地数値予報（NOAA GFS, HRRR, ECMWF） | NOAA MRMS / NEXRAD（IEM 経由）＆ HRRR（+18時間） | NOAA / NWS 公式警報、GDACS |
| **欧州** | Open-Meteo 全球／域内数値予報（ECMWF, ICON, GFS） | RainViewer 広域合成レーダー | MeteoAlarm（EUMETNET・39 か国）、WMO、GDACS |
| **その他地域** | Open-Meteo 全球数値予報 | RainViewer 広域合成レーダー | WMO 公式警報、GDACS |

---

## データ提供元一覧

- **数値天気予報・大気質**: [Open-Meteo](https://open-meteo.com/)（[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)・無料枠は非商用限定）
- **日本の気象実況・レーダー・防災情報**: 出典 [気象庁 (JMA)](https://www.jma.go.jp/)（非公式クライアント）
- **日本域ウィジェット背景地図・ハザード**: 出典 [国土地理院 (GSI)](https://maps.gsi.go.jp/development/ichiran.html)
- **米国の気象レーダー・気象警報**: [NOAA / NWS](https://www.weather.gov/) および [Iowa Environmental Mesonet (IEM)](https://mesonet.agron.iastate.edu/)
- **欧州の気象警報**: [MeteoAlarm](https://meteoalarm.org/)（EUMETNET）
- **国際気象・災害警報**: [WMO](https://severeweather.wmo.int/) および [GDACS](https://www.gdacs.org/)
- **広域気象レーダータイル**: [RainViewer](https://www.rainviewer.com/)
- **都市検索（ジオコーディング）**: [Photon](https://photon.komoot.io/) by komoot（Data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, ODbL）
- **インタラクティブ地図**: Google Maps SDK for Android（Map data © Google）
- **太陽軌道・日の出/日の入り計算**: NOAA 天文学数式アルゴリズムによる端末内オフライン計算
