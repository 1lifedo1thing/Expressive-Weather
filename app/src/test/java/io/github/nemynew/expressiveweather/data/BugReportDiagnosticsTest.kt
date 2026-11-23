package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder

class BugReportDiagnosticsTest {

    @Before
    fun setUp() {
        BugReportLogBuffer.clear()
    }

    @After
    fun tearDown() {
        BugReportLogBuffer.clear()
    }

    private fun sampleSnapshot(
        isPlay: Boolean = true,
        logs: List<String> = emptyList()
    ) = BugReportSnapshot(
        versionName = "0.1.0",
        versionCode = 1,
        isPlayDistribution = isPlay,
        deviceInfo = "Google Pixel 9 Pro, Android 16 (API 36)",
        language = AppLanguage.JAPANESE,
        themeMode = ThemeMode.WEATHER,
        themePreset = ThemePreset.WEATHER,
        temperatureUnit = TemperatureUnit.CELSIUS,
        windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
        radarSourceMode = RadarSourceMode.AUTO,
        isFrostedGlassEnabled = true,
        isFromCache = false,
        isStale = false,
        regionBucket = "Japan (JMA)",
        recentLogs = logs
    )

    @Test
    fun sanitizerRedactsApiKeysTokensAndCoordinates() {
        val fakeGoogleKey = "AIza" + "SyAbCdEfGhIjKlMnOpQrStUvWxYz123456"
        val raw = "Failed GET https://example.com?api_key=secret123&lat=35.6895&lon=139.6917 key=$fakeGoogleKey at 35.6762, 139.6503 Bearer tok_999"
        val sanitized = BugReportSanitizer.sanitize(raw, maxLength = 500)

        assertFalse(sanitized.contains("AIzaSy"))
        assertFalse(sanitized.contains("secret123"))
        assertFalse(sanitized.contains("tok_999"))
        assertFalse(sanitized.contains("35.6895"))
        assertFalse(sanitized.contains("139.6917"))
        assertFalse(sanitized.contains("35.6762"))
        assertFalse(sanitized.contains("139.6503"))
        assertTrue(sanitized.contains("[REDACTED_KEY]"))
        assertTrue(sanitized.contains("[REDACTED_COORD]"))
        assertTrue(sanitized.contains("[REDACTED_COORDS]"))
    }

    @Test
    fun logBufferCapsAtMaxEntriesAndSanitizes() {
        val fakeKey = "AIza" + "SyTestKey12345678901234567890123"
        repeat(BugReportLogBuffer.MAX_ENTRIES + 5) { idx ->
            BugReportLogBuffer.record(
                tag = "Net",
                message = "Error #$idx at 35.6895, 139.6917",
                throwable = IllegalStateException(fakeKey)
            )
        }
        val snapshot = BugReportLogBuffer.snapshot()
        assertEquals(BugReportLogBuffer.MAX_ENTRIES, snapshot.size)
        assertTrue(snapshot.first().contains("Error #5"))
        assertTrue(snapshot.last().contains("Error #24"))
        snapshot.forEach { line ->
            assertFalse(line.contains("35.6895"))
            assertFalse(line.contains("AIzaSy"))
        }
    }

    @Test
    fun gitHubIssueUrlMatchesBugReportTemplateAndFields() {
        val url = BugReportBuilder.buildGitHubIssueUrl(
            category = BugReportCategory.RADAR,
            description = "レーダー画面でズーム時にちらつく",
            expected = "滑らかに拡大してほしい",
            snapshot = sampleSnapshot(isPlay = true),
            includeDeviceInfo = true,
            includeAppState = true,
            includeRecentLogs = false
        )

        assertTrue(url.startsWith("${BugReportBuilder.GITHUB_ISSUE_BASE_URL}?"))
        val decoded = URLDecoder.decode(url, "UTF-8")
        assertTrue(decoded.contains("template=bug_report.yml"))
        assertTrue(decoded.contains("flavor=Google Play (play)"))
        assertTrue(decoded.contains("app_version=0.1.0 (1)"))
        assertTrue(decoded.contains("device=Google Pixel 9 Pro, Android 16 (API 36)"))
        assertTrue(decoded.contains("region=Japan (JMA)"))
        assertTrue(decoded.contains("レーダー画面でズーム時にちらつく"))
        assertTrue(decoded.contains("滑らかに拡大してほしい"))
    }

    @Test
    fun gitHubIssueUrlRespectsOptOutFlagsAndCapsLength() {
        val veryLongDesc = "A".repeat(8000)
        val url = BugReportBuilder.buildGitHubIssueUrl(
            category = BugReportCategory.OTHER,
            description = veryLongDesc,
            expected = "",
            snapshot = sampleSnapshot(isPlay = false),
            includeDeviceInfo = false,
            includeAppState = false,
            includeRecentLogs = false
        )

        assertTrue("URL length should stay bounded, got ${url.length}", url.length <= 5650)
        val decoded = URLDecoder.decode(url, "UTF-8")
        assertTrue(decoded.contains("flavor=GitHub Release (github)"))
        assertFalse(decoded.contains("device="))
        assertFalse(decoded.contains("region="))
        assertTrue(decoded.contains("...(truncated)"))
    }

    @Test
    fun plainTextReportRespectsDiagnosticToggles() {
        val snapshot = sampleSnapshot(logs = listOf("[12:00:00Z] WeatherStream: Timeout"))
        val withoutLogs = BugReportBuilder.formatPlainTextReport(
            category = BugReportCategory.WEATHER_DATA,
            description = "Temp looks stale",
            expected = "Should refresh",
            snapshot = snapshot,
            includeDeviceInfo = true,
            includeAppState = false,
            includeRecentLogs = false
        )
        assertTrue(withoutLogs.contains("Google Play (play)"))
        assertFalse(withoutLogs.contains("Region / Provider"))
        assertFalse(withoutLogs.contains("Recent in-app diagnostics"))

        val withLogs = BugReportBuilder.formatPlainTextReport(
            category = BugReportCategory.WEATHER_DATA,
            description = "Temp looks stale",
            expected = "",
            snapshot = snapshot,
            includeDeviceInfo = true,
            includeAppState = true,
            includeRecentLogs = true
        )
        assertTrue(withLogs.contains("Region / Provider: Japan (JMA)"))
        assertTrue(withLogs.contains("Recent in-app diagnostics"))
        assertTrue(withLogs.contains("WeatherStream: Timeout"))
    }

    @Test
    fun allVersionsBelowOnePointZeroArePreRelease() {
        assertTrue(BugReportBuilder.isPreReleaseVersion("0.0.1"))
        assertTrue(BugReportBuilder.isPreReleaseVersion("0.1.0"))
        assertTrue(BugReportBuilder.isPreReleaseVersion("0.9.9"))
        assertTrue(BugReportBuilder.isPreReleaseVersion("0.12.3"))
        assertTrue(BugReportBuilder.isPreReleaseVersion("v0.5.0"))
        assertTrue(BugReportBuilder.isPreReleaseVersion("1.0.0-beta.1"))
        assertTrue(BugReportBuilder.isPreReleaseVersion("1.0.0-rc1"))

        assertFalse(BugReportBuilder.isPreReleaseVersion("1.0.0"))
        assertFalse(BugReportBuilder.isPreReleaseVersion("v1.0.0"))
        assertFalse(BugReportBuilder.isPreReleaseVersion("1.0.1"))
        assertFalse(BugReportBuilder.isPreReleaseVersion("2.0.0"))
    }

    @Test
    fun gitHubFeatureRequestUrlMatchesTemplateAndFields() {
        val url = BugReportBuilder.buildGitHubFeatureRequestUrl(
            category = FeatureRequestCategory.WIDGET,
            proposal = "ウィジェットに降水確率グラフを追加してほしい",
            problem = "ホーム画面で雨のタイミングをすぐ確認したい",
            alternatives = "JMA の時間別データが使えそう",
            snapshot = sampleSnapshot(isPlay = true),
            includeAppInfo = true
        )

        assertTrue(url.startsWith("${BugReportBuilder.GITHUB_ISSUE_BASE_URL}?"))
        val decoded = URLDecoder.decode(url, "UTF-8")
        assertTrue(decoded.contains("template=feature_request.yml"))
        assertTrue(decoded.contains("title=[Widget] ウィジェットに降水確率グラフを追加してほしい"))
        assertTrue(decoded.contains("proposal=ウィジェットに降水確率グラフを追加してほしい"))
        assertTrue(decoded.contains("problem=ホーム画面で雨のタイミングをすぐ確認したい"))
        assertTrue(decoded.contains("alternatives=JMA の時間別データが使えそう"))
        assertTrue(decoded.contains("App Version: 0.1.0 (1)"))
    }

    @Test
    fun featureRequestPlainTextAndUrlRespectOptOutAndLengthCap() {
        val plainNoInfo = BugReportBuilder.formatPlainTextFeatureRequest(
            category = FeatureRequestCategory.UI_DESIGN,
            proposal = "Add compact mode",
            problem = "",
            alternatives = "",
            snapshot = sampleSnapshot(isPlay = false),
            includeAppInfo = false
        )
        assertTrue(plainNoInfo.contains("Add compact mode"))
        assertFalse(plainNoInfo.contains("## App Info"))

        val longUrl = BugReportBuilder.buildGitHubFeatureRequestUrl(
            category = FeatureRequestCategory.OTHER,
            proposal = "P".repeat(8000),
            problem = "",
            alternatives = "",
            snapshot = sampleSnapshot(isPlay = false),
            includeAppInfo = false
        )
        assertTrue("Feature URL should stay bounded, got ${longUrl.length}", longUrl.length <= 5650)
        val decoded = URLDecoder.decode(longUrl, "UTF-8")
        assertTrue(decoded.contains("...(truncated)"))
        assertFalse(decoded.contains("App Version:"))
    }
}
