package io.github.nemynew.expressiveweather.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * アプリ全体の API 通信（JSON / XML / CAP）を統合管理する OkHttp クライアント。
 * - HTTP/2 コネクションプーリングによる低レイテンシ
 * - コルーチンキャンセル時の即時ソケット中断 (Call.cancel())
 * - レスポンスボディおよびストリームの確実なクローズ
 */
object AppHttpClient {

    private const val DEFAULT_USER_AGENT =
        "ExpressiveWeather/1.0 (Android; +https://github.com/nemy-new/Expressive-Weather)"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(16, 3, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000L
    ): String {
        val callClient = if (timeoutMs == 15_000L) {
            client
        } else {
            client.newBuilder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs + 2_000L, TimeUnit.MILLISECONDS)
                .build()
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/json, application/xml, text/xml, */*")

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        val call = callClient.newCall(requestBuilder.build())
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use { res ->
                            if (!res.isSuccessful) {
                                if (cont.isActive) {
                                    cont.resumeWithException(IOException("HTTP ${res.code} for $url"))
                                }
                                return
                            }
                            val bodyText = res.body?.string() ?: ""
                            if (cont.isActive) cont.resume(bodyText)
                        }
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }
            })
        }
    }

    suspend fun getTextOrNull(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000L
    ): String? {
        return try {
            getText(url = url, headers = headers, timeoutMs = timeoutMs)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getJsonOrNull(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000L
    ): JSONObject? {
        val text = getTextOrNull(url = url, headers = headers, timeoutMs = timeoutMs) ?: return null
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getBytesOrNull(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 8_000L
    ): ByteArray? {
        return try {
            val callClient = if (timeoutMs == 15_000L) {
                client
            } else {
                client.newBuilder()
                    .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .callTimeout(timeoutMs + 2_000L, TimeUnit.MILLISECONDS)
                    .build()
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "image/png, image/*, */*")

            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val call = callClient.newCall(requestBuilder.build())
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use { res ->
                                if (!res.isSuccessful) {
                                    if (cont.isActive) cont.resume(null)
                                    return
                                }
                                val bytes = res.body?.bytes()
                                if (cont.isActive) cont.resume(bytes)
                            }
                        } catch (_: Exception) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                })
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * org.json.JSONObject.optString() が JSON `null` に対して文字列 "null" を返す仕様を回避する安全拡張
 */
fun JSONObject.optNonNullString(key: String, fallback: String = ""): String {
    if (isNull(key)) return fallback
    val value = optString(key, fallback)
    return if (value == "null") fallback else value
}

fun JSONObject.optNullableString(key: String): String? {
    if (isNull(key)) return null
    val value = optString(key, "").trim()
    return if (value.isBlank() || value == "null") null else value
}

fun JSONObject.optFiniteDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val value = optDouble(key, Double.NaN)
    return if (value.isNaN() || value.isInfinite()) null else value
}
