package io.github.nemynew.expressiveweather.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ResponseSizeLimitExceededException(message: String) : IOException(message)

/**
 * アプリ全体の API 通信（JSON / XML / CAP）を統合管理する OkHttp クライアント。
 * - HTTP/2 コネクションプーリングによる低レイテンシ
 * - コルーチンキャンセル時の即時ソケット中断 (Call.cancel())
 * - レスポンスボディの上限バイト数検証（OOM / DoS 対策）およびストリームの確実なクローズ
 */
object AppHttpClient {

    private const val DEFAULT_USER_AGENT =
        "ExpressiveWeather/1.0 (Android; +https://github.com/nemy-new/Expressive-Weather)"

    /** API テキスト（JSON / XML / CAP）の最大許可レスポンスサイズ (5 MiB) */
    const val DEFAULT_MAX_TEXT_BYTES: Long = 5L * 1024L * 1024L

    /** バイナリ（タイル PNG 等）の最大許可レスポンスサイズ (2 MiB) */
    const val DEFAULT_MAX_BINARY_BYTES: Long = 2L * 1024L * 1024L

    private const val READ_BUFFER_SIZE = 8 * 1024

    /**
     * ResponseBody を上限バイト数以内で安全に読み込む。
     * Content-Length 事前検査と読み込み中の逐次カウントにより、チャンク転送でも OOM を防止する。
     */
    @Throws(IOException::class)
    fun readBytesBounded(body: ResponseBody?, maxBytes: Long): ByteArray {
        if (body == null) return ByteArray(0)
        require(maxBytes > 0L) { "maxBytes must be positive" }

        val declaredLength = body.contentLength()
        if (declaredLength > maxBytes) {
            throw ResponseSizeLimitExceededException(
                "Response Content-Length ($declaredLength bytes) exceeds limit ($maxBytes bytes)"
            )
        }

        val initialCapacity = if (declaredLength in 1L..65_536L) declaredLength.toInt() else 8_192
        val output = ByteArrayOutputStream(initialCapacity)
        val buffer = ByteArray(READ_BUFFER_SIZE)
        var totalRead = 0L

        body.byteStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                totalRead += read
                if (totalRead > maxBytes) {
                    throw ResponseSizeLimitExceededException(
                        "Response body exceeded limit ($maxBytes bytes)"
                    )
                }
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray()
    }

    @Throws(IOException::class)
    fun readTextBounded(body: ResponseBody?, maxBytes: Long): String {
        if (body == null) return ""
        val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        val bytes = readBytesBounded(body, maxBytes)
        return String(bytes, charset)
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(16, 3, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val shortTimeoutClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private fun clientForTimeout(timeoutMs: Long): OkHttpClient = when (timeoutMs) {
        15_000L -> client
        8_000L -> shortTimeoutClient
        else -> client.newBuilder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(timeoutMs + 2_000L, TimeUnit.MILLISECONDS)
            .build()
    }

    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000L,
        maxBytes: Long = DEFAULT_MAX_TEXT_BYTES
    ): String {
        val callClient = clientForTimeout(timeoutMs)

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
                            val bodyText = readTextBounded(res.body, maxBytes)
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
        timeoutMs: Long = 15_000L,
        maxBytes: Long = DEFAULT_MAX_TEXT_BYTES
    ): String? {
        return try {
            getText(url = url, headers = headers, timeoutMs = timeoutMs, maxBytes = maxBytes)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getJsonOrNull(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000L,
        maxBytes: Long = DEFAULT_MAX_TEXT_BYTES
    ): JSONObject? {
        val text = getTextOrNull(url = url, headers = headers, timeoutMs = timeoutMs, maxBytes = maxBytes) ?: return null
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getBytesOrNull(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 8_000L,
        maxBytes: Long = DEFAULT_MAX_BINARY_BYTES
    ): ByteArray? {
        return try {
            val callClient = clientForTimeout(timeoutMs)

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
                                val bytes = readBytesBounded(res.body, maxBytes)
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
