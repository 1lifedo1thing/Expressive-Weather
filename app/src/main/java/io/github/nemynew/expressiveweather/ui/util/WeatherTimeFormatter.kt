package io.github.nemynew.expressiveweather.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.nemynew.expressiveweather.data.AppStrings
import kotlinx.coroutines.delay

object WeatherTimeFormatter {
    /**
     * fetchedAtMillis からの経過時間を相対時間（「たった今」「5分前」「2時間前」等）に変換します。
     */
    fun formatRelativeTime(
        fetchedAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        strings: AppStrings
    ): String {
        if (fetchedAtMillis <= 0L) return strings.justNow
        val diffMillis = (nowMillis - fetchedAtMillis).coerceAtLeast(0L)
        val minutes = (diffMillis / (1000 * 60)).toInt()
        val hours = (diffMillis / (1000 * 60 * 60)).toInt()
        val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return when {
            minutes < 1 -> strings.justNow
            minutes < 60 -> strings.minutesAgo(minutes)
            hours < 24 -> strings.hoursAgo(hours)
            else -> strings.daysAgo(days)
        }
    }

    /**
     * 更新時刻と相対時間を組み合わせた文字列を生成します。
     * 例: "更新: 14:30（15分前）" または "キャッシュ: 12:00（2時間前）"
     */
    fun formatLastUpdated(
        lastUpdatedTime: String,
        fetchedAtMillis: Long,
        isFromCache: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        strings: AppStrings
    ): String {
        val relative = formatRelativeTime(fetchedAtMillis, nowMillis, strings)
        return if (isFromCache) {
            strings.cachedDataRelative(lastUpdatedTime, relative)
        } else {
            strings.lastUpdatedRelative(lastUpdatedTime, relative)
        }
    }
}

/**
 * 画面表示中に定期的に（30秒ごと）再計算を行い、
 * 「たった今」→「1分前」などの相対時間表記をリアルタイムに更新する Composable ヘルパー
 */
@Composable
fun rememberRelativeTime(
    fetchedAtMillis: Long,
    strings: AppStrings
): String {
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMillis by remember(fetchedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(fetchedAtMillis, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(30_000L) // 30秒ごとに更新
            }
        }
    }

    return remember(fetchedAtMillis, nowMillis, strings) {
        WeatherTimeFormatter.formatRelativeTime(fetchedAtMillis, nowMillis, strings)
    }
}
