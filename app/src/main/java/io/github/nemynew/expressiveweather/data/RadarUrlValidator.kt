package io.github.nemynew.expressiveweather.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * レーダータイル URL および外部 API 由来パラメータ（host / path / basetime / validtime / member）の
 * 厳密な検証を行うセキュリティバリデータ。
 *
 * - HTTPS スキーム（ポート 443）のみ許可し、平文 HTTP・非標準ポート・認証情報埋め込みを拒否
 * - 許可された気象・地図タイル配信ホストのみをホワイトリスト許可
 * - パストラバーサル (`..`)・バックスラッシュ・制御文字を拒否
 */
object RadarUrlValidator {

    private val ALLOWED_EXACT_HOSTS = setOf(
        "www.jma.go.jp",
        "cyberjapandata.gsi.go.jp",
        "disaportaldata.gsi.go.jp",
        "mesonet.agron.iastate.edu",
        "tilecache.rainviewer.com",
        "api.rainviewer.com"
    )

    private val ALLOWED_SUFFIX_DOMAINS = setOf(
        "rainviewer.com"
    )

    private val SAFE_HOST_REGEX = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$")
    private val RAINVIEWER_PATH_REGEX = Regex("^/[a-zA-Z0-9/_-]{1,256}$")
    private val JMA_TIME_TOKEN_REGEX = Regex("^\\d{14}$")
    private val JMA_MEMBER_TOKEN_REGEX = Regex("^[a-zA-Z0-9_-]{1,24}$")

    fun isAllowedRadarHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val normalized = host.trim().lowercase()
        if (!SAFE_HOST_REGEX.matches(normalized)) return false
        if (normalized in ALLOWED_EXACT_HOSTS) return true
        return ALLOWED_SUFFIX_DOMAINS.any { domain ->
            normalized == domain || normalized.endsWith(".$domain")
        }
    }

    fun hostMatchesDomain(urlOrHost: String?, domain: String): Boolean {
        if (urlOrHost.isNullOrBlank()) return false
        val normalizedDomain = domain.trim().lowercase()
        val host = urlOrHost.trim().toHttpUrlOrNull()?.host?.lowercase()
            ?: urlOrHost.trim().lowercase()
        if (!SAFE_HOST_REGEX.matches(host)) return false
        return host == normalizedDomain || host.endsWith(".$normalizedDomain")
    }

    fun validateRadarUrl(urlString: String?): HttpUrl? {
        if (urlString.isNullOrBlank()) return null
        if (urlString.length > 2048) return null
        if (hasUnsafeRawCharacters(urlString)) return null

        val parsed = urlString.toHttpUrlOrNull() ?: return null
        if (!validateRadarHttpUrl(parsed)) return null
        return parsed
    }

    fun validateRadarHttpUrl(httpUrl: HttpUrl): Boolean {
        if (httpUrl.scheme != "https") return false
        if (httpUrl.port != 443) return false
        if (httpUrl.username.isNotEmpty() || httpUrl.password.isNotEmpty()) return false
        if (!isAllowedRadarHost(httpUrl.host)) return false
        if (httpUrl.encodedPath.contains("..", ignoreCase = false) ||
            httpUrl.encodedPath.contains("%2e", ignoreCase = true) ||
            httpUrl.encodedPath.contains("%5c", ignoreCase = true)
        ) {
            return false
        }
        if (httpUrl.pathSegments.any { it == "." || it == ".." }) return false
        return true
    }

    fun isAllowedRadarUrl(urlString: String?): Boolean = validateRadarUrl(urlString) != null

    /**
     * RainViewer API (`weather-maps.json`) が返す `host` フィールドの検証。
     * `rainviewer.com` 配下の HTTPS ホスト（クエリ・認証情報・任意パスなし）のみ許可。
     */
    fun isValidRainViewerHost(hostUrl: String?): Boolean {
        if (hostUrl.isNullOrBlank() || hasUnsafeRawCharacters(hostUrl)) return false
        val parsed = hostUrl.trim().toHttpUrlOrNull() ?: return false
        if (parsed.scheme != "https" || parsed.port != 443) return false
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return false
        if (parsed.encodedQuery != null || parsed.fragment != null) return false
        if (parsed.encodedPath != "/" && parsed.encodedPath.isNotEmpty()) return false
        return hostMatchesDomain(parsed.host, "rainviewer.com")
    }

    /**
     * RainViewer API が返すフレームの `path`（例: `/v2/radar/1727318400`）を検証。
     */
    fun isValidRainViewerPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        if (path.contains("..") || path.contains("//") || path.contains('\\')) return false
        return RAINVIEWER_PATH_REGEX.matches(path)
    }

    /**
     * 気象庁 `targetTimes*.json` の `basetime` / `validtime` (14桁 UTC タイムスタンプ) を検証。
     */
    fun isValidJmaTimeToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return JMA_TIME_TOKEN_REGEX.matches(token)
    }

    /**
     * 気象庁 `targetTimes*.json` の `member`（例: `none`, `immed`, `n1`）を検証。
     */
    fun isValidJmaMemberToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return JMA_MEMBER_TOKEN_REGEX.matches(token)
    }

    private fun hasUnsafeRawCharacters(raw: String): Boolean {
        for (ch in raw) {
            if (ch == '\\' || ch.code < 0x20 || ch.code == 0x7F) return true
        }
        return raw.contains("..") ||
            raw.contains("%2e", ignoreCase = true) ||
            raw.contains("%5c", ignoreCase = true)
    }
}
