<div align="center">
  <br />
  <img src="docs/images/logo.png" alt="Expressive Weather Logo" width="110" />
  <h1>Expressive Weather</h1>
  <p><strong>Material 3 Expressive デザインと高精度気象レーダー・多国間防災情報を統合したオープンソース Android 天気アプリ</strong></p>

  <p>
    <a href="README.md">🇺🇸 English README</a> ·
    <a href="#インストール方法">インストール方法</a> ·
    <a href="docs/ja/FEATURES.md">主な機能</a> ·
    <a href="docs/ja/BUILD.md">ビルド・セットアップ</a> ·
    <a href="docs/ja/DATA_SOURCES.md">データソース</a>
  </p>
  <p>
    <a href="PRIVACY_POLICY.md">プライバシーポリシー</a> ·
    <a href="THIRD_PARTY_NOTICES.md">サードパーティライセンス</a> ·
    <a href="CONTRIBUTING.md">コントリビューション</a> ·
    <a href="CHANGELOG.md">変更履歴</a> ·
    <a href="SECURITY.md">セキュリティ</a>
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

## インストール方法

対応環境: **Android 7.0 (API 24) 以上**

<p>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22io.github.nemynew.expressiveweather%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fnemy-new%2FExpressive-Weather%22%2C%22author%22%3A%22nemy-new%22%2C%22name%22%3A%22Expressive%20Weather%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%7D%22%7D">
    <img src="docs/images/badge_obtainium.png" alt="Get it on Obtainium" height="48" />
  </a>
</p>

- **Obtainium（自動アップデート対応・推奨）**: 上の **GET IT ON Obtainium** バッジをタップすると、[Obtainium](https://github.com/ImranR98/Obtainium) に本アプリをワンタップで追加できます（プレリリース購読設定済み）。
- **APK を直接ダウンロード**: [**GitHub Releases**](https://github.com/nemy-new/Expressive-Weather/releases) から最新の `app-github-release.apk` をダウンロードしてインストールできます。

> **Google Play ストアでの配布について**: 今後は [Google Play ストア](https://play.google.com/store) での配布も検討しています。公開状況や最新リリースは [GitHub Releases](https://github.com/nemy-new/Expressive-Weather/releases) をご確認ください。

---

## ショートカット

目的のセクションへ直接移動できます。

| セクション | 内容 |
| :--- | :--- |
| [**主な機能**](docs/ja/FEATURES.md) | Material 3 Expressive UI、傾き連動エフェクト、Android 16 Live Updates、各国レーダー・警報、5 種のウィジェット |
| [**ビルド & セットアップ**](docs/ja/BUILD.md) | 前提環境、`MAPS_API_KEY` 設定、デバッグ／リリースビルド、署名設定、テスト実行 |
| [**データソース & 出典**](docs/ja/DATA_SOURCES.md) | 地域別データルーティング（日本／米国／欧州／世界）と全提供元の出典一覧 |
| [**プライバシーポリシー**](PRIVACY_POLICY.md) | 独自サーバー・広告なし、位置情報の取り扱い詳細（日英併記） |
| [**サードパーティライセンス**](THIRD_PARTY_NOTICES.md) | 使用ライブラリ、同梱フォント、アイコン、気象データセットのライセンス表記 |
| [**コントリビューション**](CONTRIBUTING.md) · [**セキュリティ**](SECURITY.md) | 開発ガイドライン、PR ルール、脆弱性報告窓口 |

---

## 概要

**Expressive Weather** は、端末の傾きセンサーと連動する雨粒・雪のエフェクト、気象庁の高解像度降水ナウキャスト・キキクル（危険度分布）・台風進路、全米 NEXRAD / MRMS / HRRR 気象レーダー、欧州 MeteoAlarm、世界気象機関（WMO）および GDACS の災害アラート、さらに Android 16 の Live Updates と 5 種類のホーム画面ウィジェットを統合したオープンソース Android アプリです。

> **免責事項**: 本アプリは独立して開発された**非公式のサードパーティ製オープンソースアプリ**であり、気象庁、国土地理院、NOAA/NWS、MeteoAlarm、WMO、GDACS その他いかなる公的機関とも関係ありません。災害・緊急時は必ず気象庁や自治体の公式発表をご確認ください。

---

## クイックスタート

```bash
git clone https://github.com/nemy-new/Expressive-Weather.git
cd Expressive-Weather
cp local.properties.example local.properties   # MAPS_API_KEY を設定
./gradlew assemblePlayDebug testPlayDebugUnitTest
```

リリース署名や Google Maps API キー制限の詳細は [**ビルド & セットアップ**](docs/ja/BUILD.md) をご覧ください。

---

## ライセンス

本プロジェクトは **Apache License 2.0** の下で公開されています。詳細は [LICENSE](LICENSE) および [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) をご確認ください。
