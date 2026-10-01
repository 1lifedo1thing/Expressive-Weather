<div align="center">
  <br />
  <img src="docs/images/logo.png" alt="Expressive Weather Logo" width="110" />
  <h1>Expressive Weather</h1>
  <p><strong>Material 3 Expressive デザインシステムと高精度気象レーダー・防災情報を統合した Android 天気アプリ</strong></p>

  <p>
    <a href="https://m3.material.io/">
      <img alt="Material 3 Expressive" src="https://custom-icon-badges.demolab.com/badge/m3%20expressive-lightblue?style=for-the-badge&logoColor=333&logo=material-you" />
    </a>
    <img src="https://img.shields.io/badge/Android-16%2B%20Ready-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 16 Ready" />
    <img src="https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.1" />
    <a href="LICENSE">
      <img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=for-the-badge" alt="License GPL-3.0" />
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

## 概要

**Expressive Weather** は、Google の **Material 3 Expressive** デザインガイドラインに準拠して設計されたオープンソースの Android 気象アプリケーションです。

単なる数値予報の提示にとどまらず、端末のジャイロセンサーと連動する雨粒・ガラス屈折物理シミュレーション、気象庁の高解像度降水ナウキャストやキキクル（危険度分布）、全米 NEXRAD / MRMS / HRRR 気象レーダー、欧州 MeteoAlarm、世界気象機関（WMO）の公式警報などを包括的に統合しています。

さらに、Android 16+ の **Rich Ongoing Notifications（Live Updates）** および Samsung Galaxy の **Now Bar** に対応し、雨の降り始めや降り止みまでの残り時間をステータスバーやロック画面へ秒単位で掲示しつつ、バックグラウンドでの現在位置移動にも自動追従します。

---

## 主要機能

### 1. UI & インタラクションデザイン

* **ベントグリッド（Bento Grid）レイアウト**:
  * 気温・降水確率・風向風速・UV指数・日の出日の入り・気圧・視程などの気象データを、視覚的階層の明確なモジュール構造で配置。
  * `androidx.graphics.shapes` / Material 3 Expressive Shapes を用いた角丸・ポリゴンシェイプのダイナミックアニメーション。
* **物理シミュレーション & すりガラス効果（Frosted Glass）**:
  * 端末の傾き（加速度・ジャイロセンサー）に応じて画面上の雨粒が流れる屈折描画エンジンを実装。
  * `dev.chrisbanes.haze` によるリアルタイム・バックドロップブラー処理。
* **テーマ & アダプティブカラー**:
  * 天候連動型アダプティブテーマ（快晴、雨、曇り、夜間等に応じて動的に配色を調整）。
  * プリセットカラーパレット（Sky Ocean, Sunset, Lavender, Forest, Monochrome 等）およびライト / ダークモード切替。
* **レスポンシブ画面適応**:
  * スマートフォン縦画面に加え、フォルダブル（折りたたみ端末）の大画面やタブレットの横向き表示（サイドナビゲーションパネル付き2ペインレイアウト）に自動追従。
* **多言語対応**:
  * 日本語、英語、簡体字中国語、繁体字中国語に対応。OS設定に依存せずアプリ内での即時言語切替が可能。

---

### 2. 降雨予測 & Live Updates（降り始め・降り止みカウントダウン）

* **時系列マルチティア統合スキャナー (`PrecipitationAnalyzer`)**:
  * **直近 0〜60分**: 気象庁 高解像度降水ナウキャスト（5分間隔・予想降水量 mm/h）
  * **1時間後〜6時間後**: 降水短時間予報（10分間隔）
  * **6時間後〜15時間後**: 降水短時間予報（1時間間隔）
  * **15時間以降〜翌日**: 府県天気予報（6時間ブロック・降水確率 %）
  * 閾値解析（降り始め: 1.0mm/h, 降り止み: 0.1mm/h, 強雨: 10.0mm/h, 激しい雨: 30.0mm/h）により、「あと何分で雨が降り始めるか」「あと何分で雨が止むか」を高精度に算出。
* **Android 16+ Rich Ongoing Notifications & Galaxy Now Bar 連携**:
  * ステータスバーピルに「雨止むまで あと25分」「雨雲接近 あと15分」などの短縮クリティカルテキスト（`shortCriticalText`）を常時掲示。
  * ロック画面および通知シェードでネイティブクロノメーターによる秒単位カウントダウンを提供。
* **バックグラウンド現在位置追従 (`PrecipitationLiveService`)**:
  * カウントダウン表示中は `foregroundServiceType="dataSync|location"` のフォアグラウンドサービスとして稼働し、OSによるスロットリングを回避。
  * 75m 以上の現在地移動を検知した瞬間、新地点の逆ジオコーディングと最新ナウキャスト再解析を実行。
  * 移動先で雨が止んだ場合は「雨が止みました」通知を表示して自動でサービスを正常終了。

---

### 3. 気象レーダー & 数値予報シミュレーション

* **日本国内（気象庁 JMA）**:
  * **高解像度降水ナウキャスト (HRPNS)**: 250mメッシュの降水強度実況・短時間予測。
  * **雨雲の動き**: 今後の雨雲の進退（最大+15時間先まで）の時系列シミュレーション。
  * **キキクル（危険度分布）**: 土砂災害・浸水害・洪水害の5段階危険度レイヤーを Google Maps 上に重畳描画。
* **アメリカ合衆国（NOAA / NWS）**:
  * **NEXRAD / MRMS**: 全米高解像度レーダー合成反射強度（dBZ）。
  * **HRRR (High-Resolution Rapid Refresh)**: 3kmメッシュ数値予報モデルによる最大+18時間の降水シミュレーション。
* **グローバル（RainViewer）**:
  * 日本・米国以外の世界各国における広域気象レーダータイル合成。
* **高速描画最適化**:
  * OkHttp による HTTP/2 コネクションプーリング、LRU メモリキャッシュ、および現在のマップ表示領域（Viewport）に絞ったタイルリクエスト制御。

---

### 4. 防災気象情報・多国間警報システム

* **日本（気象庁 JMA）**:
  * 気象庁防災情報 XML / JSON をパースし、市区町村ごとの5段階警戒レベル（レベル1: 早期注意情報 〜 レベル5: 緊急安全確保）を算出。
  * 特別警報・警報・注意報の発表状況、避難指示などの防災行動ラベルをカードおよび通知で掲示。
* **アメリカ（NOAA / NWS）**:
  * National Weather Service の CAP / GeoJSON API と連携し、Tornado Warning, Flash Flood Warning, Severe Thunderstorm Warning 等の重大警報を取得。
* **ヨーロッパ（MeteoAlarm）**:
  * 欧州39カ国（EUMETNET 加盟国・地域および小国）の気象警報に対応。
  * ISO-3166-1 alpha-2 国コードおよび座標セントロイド判定による自動マッチング。
* **国際（WMO & GDACS）**:
  * **WMO Alert**: 世界気象機関公式の Common Alerting Protocol（CAP）警報。日本・米国・欧州以外の地域を自動カバーし、設定により常時併用も可能。
  * **GDACS**: 熱帯低気圧（台風・ハリケーン）、地震、洪水などの世界規模の大規模災害アラート。

---

### 5. 高信頼バックグラウンド自動更新エンジン

Doze モード（省電力ディープスリープ）やプロセスキルを考慮した多重層バックグラウンド更新アーキテクチャを実装しています。

| レイヤー | 使用コンポーネント | 役割・動作特性 |
| :--- | :--- | :--- |
| **Layer 1** | `WorkManager` (`WeatherUpdateWorker`) | Android 標準の永続化ワーカー。端末再起動後や長時間スリープ復帰時にもOSが確実に定期実行（15分〜6時間）。 |
| **Layer 2** | `AlarmManager` (`WeatherUpdateReceiver`) | `setAndAllowWhileIdle` による Wakeup アラーム。設定した同期間隔を維持し Doze モードを貫通して更新。 |
| **Layer 3** | `LocationManager` (`PendingIntent`) | 端末の移動（250m以上）をシステムレベルで検知し、バックグラウンド待機中でも新地点の天気を自動取得。 |
| **Layer 4** | インメモリ / フォアグラウンドループ | アプリ起動時およびフォアグラウンド復帰時の即時同期・高応答性ループ。 |

* **オフラインファースト・データ永続化**:
  * Room Database (`WeatherDao`) により、直近の気象データ、ナウキャスト、防災警報、保存地点を永続化。ネットワーク切断時でも直前のデータを即座に復元表示。

---

## 技術スタック

| カテゴリ | 採用技術・ライブラリ |
| :--- | :--- |
| **言語** | Kotlin 2.1 |
| **UI ツールキット** | Jetpack Compose (Compose BOM 2025.02.00) |
| **デザインシステム** | Material 3 Expressive, Material Shapes |
| **アーキテクチャ** | MVVM, Repository Pattern, StateFlow / SharedFlow |
| **非同期処理** | Kotlin Coroutines, Kotlin Flow |
| **ローカルデータベース** | Room Database 2.6 (KSP) |
| **シリアライズ** | Kotlinx Serialization JSON 1.7 |
| **すりガラス・ブラー** | Haze (`dev.chrisbanes.haze:haze-materials`) |
| **バックグラウンド処理** | WorkManager, AlarmManager, Foreground Service |
| **マップ SDK** | Google Maps SDK for Android (`play-services-maps`) |
| **ネットワーク** | OkHttp 4.12 |
| **ターゲット SDK** | Compile SDK: 36 / Target SDK: 36 / Min SDK: 24 |

---

## プロジェクト構成

```
app/src/main/java/com/example/expressiveweather/
├── MainActivity.kt                      # メインエントリーポイント & インテントハンドラ
├── Navigation.kt                        # ナビゲーションルーティング定義
├── data/                                # データアクセス・リポジトリ・バックグラウンド層
│   ├── DataRepository.kt                # 天気データ統合リポジトリ (WeatherRepository)
│   ├── WeatherModels.kt                 # 天気データ・ナウキャスト・警報のデータモデル群
│   ├── PrecipitationAnalyzer.kt         # 降雨ナウキャスト・マルチティア解析エンジン
│   ├── PrecipitationLiveService.kt      # Live Updates 常駐フォアグラウンドサービス (位置追従)
│   ├── PrecipitationLiveNotificationManager.kt # Android 16 Live Updates 通知マネージャー
│   ├── WeatherBackgroundUpdateManager.kt# 3層ハイブリッドバックグラウンド更新マネージャー
│   ├── WeatherUpdateWorker.kt           # WorkManager 定期実行ワーカー
│   ├── WeatherUpdateReceiver.kt         # AlarmManager & 位置変更ブロードキャストレシーバー
│   ├── CurrentLocationProvider.kt       # 高精度位置測位・逆ジオコーディングプロバイダー
│   ├── JmaAlertRepository.kt            # 気象庁防災情報・警報解析
│   ├── KikikuruBackgroundMonitor.kt     # キキクル定期監視 & 警戒レベル判定
│   ├── NwsAlertRepository.kt            # 米国 NWS 警報解析
│   ├── MeteoAlarmRepository.kt          # 欧州 MeteoAlarm 39カ国警報解析
│   ├── WmoAlertRepository.kt            # 世界気象機関 (WMO) 公式警報解析
│   └── local/                           # Room Database (Entities, DAO, Database)
└── ui/                                  # UI コンポーネント & 画面層
    ├── main/                            # メイン画面 (MainScreen, MainScreenViewModel)
    ├── components/                      # ベントグリッド, 雨滴屈折描画, 各種カードコンポーネント
    ├── radar/                           # Google Maps 雨雲レーダー・キキクル表示コンポーネント
    └── screens/                         # 各種詳細画面 (今日・時間別・風・警報・設定・初回セットアップ)
```

---

## ビルド & セットアップ方法

### 前提条件
* Android Studio Ladybug (2024.2.1) 以降推奨
* JDK 17
* Android SDK 36 (Android 16 プレビュー / バニラアイスクリーム)
* Google Maps API キー（Google Cloud Console で「Maps SDK for Android」を有効化したもの）

### 手順

1. **リポジトリのクローン**:
   ```bash
   git clone https://github.com/your-username/expressive-weather.git
   cd expressive-weather
   ```

2. **API キーの設定**:
   プロジェクトのルートディレクトリに `local.properties` を作成（または既存ファイルを編集）し、Google Maps API キーを記述します。
   ```properties
   MAPS_API_KEY=AIzaSyYourActualGoogleMapsApiKeyHere
   ```

3. **ビルド & 実行**:
   ```bash
   # デバッグビルドの生成
   ./gradlew assembleDebug

   # 接続中の端末・エミュレータへインストール
   ./gradlew installDebug
   ```

4. **ユニットテストの実行**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## データソース & 出典

本アプリで利用している各種データおよび API は、以下のオープンデータ・公共機関・商用サービスより提供されています。

* **数値天気予報**: [Open-Meteo](https://open-meteo.com/) (Licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/))
* **日本の気象レーダー & 防災気象情報**: 出典 [気象庁 (JMA)](https://www.jma.go.jp/)（高解像度降水ナウキャスト、雨雲の動き、キキクル危険度分布、気象警報・注意報）
  * ※本アプリは気象庁公式のアプリケーションではありません。
* **米国の気象レーダー & 気象警報**: [National Oceanic and Atmospheric Administration (NOAA)](https://www.noaa.gov/) / [National Weather Service (NWS)](https://www.weather.gov/) (Public Domain)
* **欧州の気象警報**: [MeteoAlarm](https://meteoalarm.org/) (EUMETNET)
* **国際気象警報**: [World Meteorological Organization (WMO)](https://severeweather.wmo.int/)
* **国際災害警報**: [Global Disaster Alert and Coordination System (GDACS)](https://www.gdacs.org/)
* **広域気象レーダータイル**: [RainViewer](https://www.rainviewer.com/)
* **都市検索 & ジオコーディング**: [Photon](https://photon.komoot.io/) by komoot (Data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, [ODbL](https://opendatacommons.org/licenses/odbl/))

---

## ライセンス

本プロジェクトは **GNU General Public License v3.0 (GPL-3.0)** の下で公開されています。詳細については [LICENSE](LICENSE) ファイルをご確認ください。
