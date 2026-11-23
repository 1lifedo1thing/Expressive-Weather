<div align="center">
  <p>
    <a href="../../README.ja.md">← 日本語 README</a> ·
    <a href="FEATURES.md">主な機能</a> ·
    <strong>ビルド・セットアップ</strong> ·
    <a href="DATA_SOURCES.md">データソース</a> ·
    <a href="../BUILD.md">🇺🇸 English</a>
  </p>
</div>

# ビルド & セットアップ手順

---

## 前提条件

- **Android Studio**（AGP 9.x / Kotlin 2.3 対応版）
- **JDK 17**
- **Android SDK 36**（Android 16）
- **Google Maps API キー**（Google Cloud Console で **Maps SDK for Android** を有効化したもの。レーダー地図の描画に必要ですが、コンパイルとユニットテスト自体はキーなしでも実行可能です）

---

## 1. リポジトリのクローン

```bash
git clone https://github.com/nemy-new/Expressive-Weather.git
cd Expressive-Weather
```

---

## 2. `MAPS_API_KEY` の設定

[`local.properties.example`](../../local.properties.example) をコピーして `local.properties` を作成し、`MAPS_API_KEY` を設定します。

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

CI/CD 環境では環境変数 `MAPS_API_KEY` として渡すことも可能です。

> **セキュリティの推奨設定**: Google Cloud Console の **「API とサービス → 認証情報」** で、API キーに以下の制限を設定してください。
> - **アプリケーションの制限**: Android アプリ（パッケージ名 `io.github.nemynew.expressiveweather` ＋ 使用するデバッグ鍵／リリース鍵／Play アプリ署名鍵の SHA-1 フィンガープリント）
> - **API の制限**: **Maps SDK for Android** のみ

---

## 3. デバッグ APK のビルド

```bash
# Google Play フレーバー
./gradlew assemblePlayDebug

# GitHub フレーバー
./gradlew assembleGithubDebug
```

---

## 4. 署名付きリリース成果物のビルド

[`keystore.properties.example`](../../keystore.properties.example) を参考に `keystore.properties` を作成します（Git には絶対にコミットしないでください）。

```properties
storeFile=path/to/release-keystore.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

環境変数（`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`）での指定にも対応しています。

```bash
# Google Play 提出用 App Bundle (.aab)
./gradlew bundlePlayRelease

# Google Play 用 APK (.apk)
./gradlew assemblePlayRelease

# GitHub Releases 配布用 APK (.apk)
./gradlew assembleGithubRelease
```

利用可能なビルドオプション（Gradle プロパティまたは環境変数）:
- `VERSION_NAME` / `VERSION_CODE`: バージョン情報の指定
- `REQUIRE_RELEASE_CONFIG=true`: `MAPS_API_KEY` やリリース署名設定が未設定の場合にビルドを即座に失敗させる（CI 向け）

---

## 5. テスト・Lint の実行

```bash
# 両フレーバーのユニットテスト
./gradlew testPlayDebugUnitTest testGithubDebugUnitTest

# Android Lint チェック
./gradlew lintPlayDebug lintVitalPlayRelease lintVitalGithubRelease
```
