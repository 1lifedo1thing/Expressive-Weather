# Privacy Policy / プライバシーポリシー

**Effective Date / 発効日**: 2026-11-22  
**Application / アプリ名**: Expressive Weather (`io.github.nemynew.expressiveweather`)  
**Repository & Contact / リポジトリ・連絡先**: https://github.com/nemy-new/Expressive-Weather

---

## English

### 1. Overview
Expressive Weather is an open-source weather and severe-weather alert application. The developer **does not operate any proprietary backend server**, **does not require account registration**, **does not collect personal profiles**, and **does not display advertisements or use third-party ad/analytics tracking SDKs**.

### 2. Data Accessed and How It Is Used

#### 2.1 Location Data (Foreground & Optional Background)
- **What is accessed**: Approximate or precise device coordinates (latitude and longitude) only if you grant location permission.
- **Why it is used**:
  - To fetch local weather forecasts, precipitation nowcasts, radar tiles, and official disaster/weather warnings.
  - To resolve human-readable city/municipality names via on-device/OS geocoding (`android.location.Geocoder`).
- **Background Location (Optional)**:
  - If you explicitly choose to enable **"Allow all the time"** in Android settings after reviewing the in-app prominent disclosure, Expressive Weather accesses location in the background even when the app is closed or not in use to:
    1. Automatically monitor severe weather warnings and disaster alerts (such as JMA Kikikuru, NOAA/NWS, MeteoAlarm, WMO) for your current location as you move.
    2. Keep active precipitation countdown Live Updates accurate as you travel.
    3. Keep home-screen weather widgets and cached forecasts up to date for your current area.
  - You can use the app fully without granting background location (or any location permission at all) by manually searching and saving cities. You can revoke location permissions at any time in Android Settings.

#### 2.2 City Search Queries
- When you search for a city, the query text (and optionally your current coordinates for local relevance bias, if location permission is granted) is sent directly from your device to **Photon (komoot / OpenStreetMap)**.

#### 2.3 On-Device Storage
- Saved cities, recent searches, weather/radar caches, and app preferences are stored locally on your device (`SharedPreferences`, Room SQLite database, and app cache directory).
- Sensitive preference and cache files are excluded from Android cloud backup via `backup_rules.xml` and `data_extraction_rules.xml`.
- You can delete all local data at any time by clearing app cache/storage in Android Settings or uninstalling the app.

### 3. Third-Party Services & Data Recipients
To deliver weather data, maps, and warnings, the app sends HTTP requests directly from your device (including your IP address, standard request headers, and requested coordinates or tile indexes) to the following third-party services, each governed by its own privacy policy:

- **Open-Meteo** (`api.open-meteo.com`, `air-quality-api.open-meteo.com`) — Weather & air quality forecasts
- **Japan Meteorological Agency (JMA)** (`www.jma.go.jp`) — Japan weather forecasts, nowcast/radar tiles, Kikikuru risk data, typhoon tracks, and warnings
- **Geospatial Information Authority of Japan (GSI)** (`cyberjapandata.gsi.go.jp`, `disaportaldata.gsi.go.jp`) — Japan base maps and hazard overlays
- **NOAA / National Weather Service (NWS)** (`api.weather.gov`) — US weather warnings
- **Iowa Environmental Mesonet (IEM / Iowa State University)** (`mesonet.agron.iastate.edu`) — US NEXRAD / MRMS / HRRR radar tiles
- **MeteoAlarm (EUMETNET)** (`feeds.meteoalarm.org`) — European weather warnings
- **World Meteorological Organization (WMO)** (`severeweather.wmo.int`) — International official weather warnings
- **GDACS (UN & European Commission)** (`www.gdacs.org`) — Global disaster alerts
- **RainViewer** (`api.rainviewer.com`, `tilecache.rainviewer.com`) — Global composite radar tiles
- **Photon by komoot** (`photon.komoot.io`) — City geocoding search
- **Google Maps SDK for Android (Google LLC)** — Interactive radar map display. Please note that the Google Maps SDK itself may collect device diagnostics, approximate/precise location, and usage information according to the [Google Privacy Policy](https://policies.google.com/privacy).

### 4. Data Sharing and Selling
The developer **never sells, rents, or shares** your personal data for advertising or data-brokerage purposes.

### 5. Children's Privacy
Expressive Weather does not knowingly collect personal information from children under 13 (or the applicable age of digital consent in your jurisdiction).

### 6. Disclaimer (Unofficial Application)
Expressive Weather is an **unofficial, independent third-party open-source application** and is **not affiliated with, endorsed by, or representing** the Japan Meteorological Agency (JMA), NOAA/NWS, EUMETNET/MeteoAlarm, WMO, GDACS, or any government entity. During emergencies, always follow official announcements from your local authorities.

### 7. Changes & Contact
This policy may be updated as features or data sources change. Questions, privacy requests, or security concerns can be submitted via GitHub Issues at:  
https://github.com/nemy-new/Expressive-Weather/issues

---

## 日本語

### 1. 基本方針
Expressive Weather はオープンソースの天気予報・防災情報アプリです。開発者は **独自のデータ収集サーバーを運営しておらず**、**アカウント登録・広告配信・行動トラッキング SDK の組み込みは一切行っていません**。

### 2. 取得する情報と利用目的

#### 2.1 位置情報（フォアグラウンドおよび任意のバックグラウンド）
- **取得内容**: ユーザーが位置情報権限を許可した場合に限り、端末の現在地（緯度・経度）を取得します。
- **利用目的**:
  - 現在地の天気予報、降水ナウキャスト、雨雲レーダー、各種気象警報・防災情報の取得。
  - OS標準のジオコーダー (`android.location.Geocoder`) による現在地の地域名解決。
- **バックグラウンド位置情報（任意）**:
  - アプリ内の事前説明（顕著な開示ダイアログ）を確認のうえ、ユーザー自身が Android の設定で「**常に許可**」を選択した場合に限り、アプリが閉じている間や使用していないときにも位置情報を取得します。これは以下の機能のみに使用されます：
    1. 移動先に応じた気象警報・特別警報・キキクル（危険度分布）等の自動監視と通知。
    2. 雨雲接近・降り止みリアルタイムカウントダウン通知の現在地追従。
    3. ホーム画面ウィジェットおよび天気キャッシュのバックグラウンド自動更新。
  - 位置情報権限（およびバックグラウンド権限）は任意であり、地名検索による手動登録のみでアプリを利用することも可能です。権限はいつでも Android の端末設定から変更・撤回できます。

#### 2.2 都市検索クエリ
- 都市検索時にユーザーが入力したキーワード（および位置情報許可時は検索精度向上のための現在地座標）が、端末から直接 **Photon (komoot / OpenStreetMap)** へ送信されます。

#### 2.3 端末内保存データ
- 保存都市、検索履歴、天気・レーダーのキャッシュ、アプリ設定はユーザーの端末内のみに保存されます（Room DB / SharedPreferences / アプリキャッシュ）。
- これらは Android クラウドバックアップの対象から除外されています。端末のアプリ設定からストレージ消去またはアンインストールを行うことで、いつでも完全に削除できます。

### 3. 外部送信先（サードパーティサービス）
天気予報・地図・防災情報の取得のため、端末から以下の各提供元へ直接通信を行います（通信時には IP アドレス、リクエスト座標またはタイル座標等が各サービスへ送信され、各サービスのプライバシーポリシーが適用されます）：

- **Open-Meteo**（天気予報・大気質）
- **気象庁 (JMA)**（日本の天気予報・降水ナウキャスト・キキクル・台風・警報）
- **国土地理院 (GSI)**（日本域ウィジェット背景地図・ハザードオーバーレイ）
- **NOAA / National Weather Service (NWS)**（米国気象警報）
- **Iowa Environmental Mesonet (IEM / アイオワ州立大学)**（米国 NEXRAD / MRMS / HRRR レーダータイル）
- **MeteoAlarm (EUMETNET)**（欧州気象警報）
- **世界気象機関 (WMO)**（国際公式気象警報）
- **GDACS（国連・欧州委員会）**（国際災害アラート）
- **RainViewer**（広域複合レーダータイル）
- **Photon (komoot / OpenStreetMap)**（都市検索）
- **Google Maps SDK for Android (Google LLC)**（地図表示。SDK 自体が Google の[プライバシーポリシー](https://policies.google.com/privacy)に基づき診断情報・位置情報・利用情報を処理する場合があります）

### 4. 第三者への販売・広告利用の禁止
開発者がユーザーの位置情報や利用データを販売したり、広告目的で第三者へ提供したりすることは一切ありません。

### 5. 非公式アプリである旨の免責
本アプリは独立した非公式のサードパーティ製オープンソースアプリであり、気象庁、国土地理院、NOAA/NWS、MeteoAlarm、WMO、GDACS その他いかなる公的機関とも提携・公式認定関係にありません。災害・緊急時は必ず気象庁や自治体の公式発表をご確認ください。

### 6. お問い合わせ
本ポリシーに関するご質問・ご要望は、以下の GitHub リポジトリの Issues よりご連絡ください：  
https://github.com/nemy-new/Expressive-Weather/issues
