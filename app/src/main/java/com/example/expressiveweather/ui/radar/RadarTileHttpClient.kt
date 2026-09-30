package com.example.expressiveweather.ui.radar

import android.util.Log
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 雨雲レーダー用の接続プール付き HTTP クライアント。
 * サーバーが提示するプロトコルはOkHttpが交渉し、実際に選ばれたプロトコルと遅い応答をログに記録します。
 * メイン画面からの事前接続と時間軸先読みもサポートします。
 */
object RadarTileHttpClient {
    private const val TAG = "RadarTileHttpClient"

    // 高並列ディスパッチャ（気象庁 CDN / RainViewer への並列度を 32 に引き上げ）
    private val dispatcher = Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 32
    }

    // 接続プール: 長時間（5分間）アイドル接続を維持し、画面遷移時の TLS ハンドシェイク（150〜300ms）を 0ms 化
    private val connectionPool = ConnectionPool(
        maxIdleConnections = 32,
        keepAliveDuration = 5,
        timeUnit = TimeUnit.MINUTES
    )

    val client: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(dispatcher)
        .connectionPool(connectionPool)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // 事前接続済みホストの記録
    private val prewarmedHosts = ConcurrentHashMap<String, Long>()
    private val negotiatedProtocols = ConcurrentHashMap<String, String>()
    private val noTileUntilMillis = ConcurrentHashMap<String, Long>()

    private fun isTemporarilyKnownEmpty(url: String): Boolean {
        val expiry = noTileUntilMillis[url] ?: return false
        if (expiry > System.currentTimeMillis()) return true
        noTileUntilMillis.remove(url, expiry)
        return false
    }

    private fun rememberNoTile(url: String) {
        noTileUntilMillis[url] = System.currentTimeMillis() + 30_000L
        if (noTileUntilMillis.size > 1024) {
            val now = System.currentTimeMillis()
            noTileUntilMillis.forEach { (cachedUrl, expiry) ->
                if (expiry <= now) noTileUntilMillis.remove(cachedUrl, expiry)
            }
            if (noTileUntilMillis.size > 2048) noTileUntilMillis.clear()
        }
    }

    private fun recordNegotiatedProtocol(url: String, protocol: String) {
        val host = try {
            URI(url).host ?: return
        } catch (_: Exception) {
            return
        }
        val previous = negotiatedProtocols.put(host, protocol)
        if (previous != protocol) {
            Log.i(TAG, "Negotiated radar protocol for $host: $protocol")
        }
    }

    internal fun getRefererForUrl(url: String): String {
        return try {
            val host = URI(url).host ?: return "https://www.jma.go.jp/bosai/nowc/"
            when {
                host.contains("jma.go.jp") -> "https://www.jma.go.jp/bosai/nowc/"
                host.contains("rainviewer.com") -> "https://www.rainviewer.com/"
                host.contains("iastate.edu") -> "https://mesonet.agron.iastate.edu/"
                else -> "https://$host/"
            }
        } catch (_: Exception) {
            "https://www.jma.go.jp/bosai/nowc/"
        }
    }

    /** レガシー診断用。描画経路ではサイズだけでタイル内容を判定しない。 */
    fun isZoomNotSupportedTile(bytes: ByteArray?): Boolean {
        if (bytes == null) return false
        return bytes.size in 1350..1390
    }

    /**
     * 気象庁や RainViewer、IEM ホストへの接続をバックグラウンドで事前確立。
     * 画面表示時に既存接続が利用できる可能性を高めます。
     */
    fun prewarm(url: String) {
        val host = try {
            URI(url).host ?: return
        } catch (_: Exception) {
            return
        }
        val now = System.currentTimeMillis()
        val nextPrewarmAt = now + 4 * 60 * 1000L
        while (true) {
            val previousPrewarmAt = prewarmedHosts[host]
            if (previousPrewarmAt != null && previousPrewarmAt > now) return
            val claimed = if (previousPrewarmAt == null) {
                prewarmedHosts.putIfAbsent(host, nextPrewarmAt) == null
            } else {
                prewarmedHosts.replace(host, previousPrewarmAt, nextPrewarmAt)
            }
            if (claimed) break
        }

        val request = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile) ExpressiveWeather/1.0")
            .header("Referer", getRefererForUrl(url))
            .header("Accept", "*/*")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                prewarmedHosts.remove(host, nextPrewarmAt)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                recordNegotiatedProtocol(url, response.protocol.toString())
                response.close()
                Log.d(TAG, "Pre-warmed radar connection to $host")
            }
        })
    }

    /**
     * タイル画像の同期ダウンロード（TileProvider の getTile から呼び出し）。
     * IEMなど配信元ごとの遅延と、実際に交渉されたHTTPプロトコルをログで確認できます。
     */
    fun getTileBytes(urlString: String): ByteArray? {
        if (isTemporarilyKnownEmpty(urlString)) return null
        val request = Request.Builder()
            .url(urlString)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile) ExpressiveWeather/1.0")
            .header("Accept", "image/png,image/*;q=0.8,*/*;q=0.5")
            .header("Referer", getRefererForUrl(urlString))
            .build()

        var downloaded: ByteArray? = null
        for (attempt in 0..1) {
            try {
                val startedAtNanos = System.nanoTime()
                val response = client.newCall(request).execute()
                response.use { res ->
                    recordNegotiatedProtocol(urlString, res.protocol.toString())
                    val elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000L
                    if (elapsedMs >= 750L) {
                        val host = try { URI(urlString).host ?: "unknown" } catch (_: Exception) { "unknown" }
                        Log.d(TAG, "Slow radar tile response: ${elapsedMs}ms, ${res.protocol}, HTTP ${res.code}, host=$host")
                    }
                    if (res.isSuccessful) {
                        val bytes = res.body?.bytes()
                        if (bytes != null && isValidPngBytes(bytes)) {
                            noTileUntilMillis.remove(urlString)
                            downloaded = bytes
                            return@use
                        }
                    } else if (res.code == 404 || res.code == 204) {
                        // タイルが存在しない（雨雲なし・エリア外）
                        rememberNoTile(urlString)
                        return null
                    } else if (res.code in 500..599 || res.code == 429) {
                        // 一時サーバーエラー
                        if (attempt < 1) Thread.sleep(80L)
                    }
                }
                if (downloaded != null) break
            } catch (e: Exception) {
                if (attempt < 1) {
                    try { Thread.sleep(80L) } catch (_: Throwable) {}
                }
            }
        }
        return downloaded
    }

    /**
     * 複数タイルのバックグラウンド非同期一括先読み（プリフェッチ）
     */
    fun prefetchTiles(urls: List<String>, onTileDownloaded: (url: String, bytes: ByteArray) -> Unit) {
        for (url in urls) {
            if (isTemporarilyKnownEmpty(url)) continue
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) ExpressiveWeather/1.0")
                .header("Accept", "image/png,image/*;q=0.8,*/*;q=0.5")
                .header("Referer", getRefererForUrl(url))
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    // Ignore prefetch failure
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { res ->
                        if (res.isSuccessful) {
                            val bytes = res.body?.bytes()
                            if (bytes != null && isValidPngBytes(bytes)) {
                                noTileUntilMillis.remove(url)
                                onTileDownloaded(url, bytes)
                            }
                        } else if (res.code == 404 || res.code == 204) {
                            rememberNoTile(url)
                        }
                    }
                }
            })
        }
    }

    /**
     * PNGシグネチャによる破損・エラーHTMLデータの排除
     */
    fun isValidPngBytes(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.size < 8) return false
        return bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte() &&
                bytes[4] == 0x0D.toByte() &&
                bytes[5] == 0x0A.toByte() &&
                bytes[6] == 0x1A.toByte() &&
                bytes[7] == 0x0A.toByte()
    }
}
