package io.github.nemynew.expressiveweather.data

import android.os.Build

/**
 * Google Play 審査ポリシーおよび Android Vitals (Excessive Wakeups / Stuck Wake Locks) に
 * 完全準拠しながら、バックグラウンド取得の頻度と確実性を最大化するためのスケジューリングポリシー。
 *
 * 方針:
 * - 禁止権限 (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`) は一切不要。
 * - Android Vitals の過剰ウェイクアップ基準 (> 10回/時) を確実に下回るよう、
 *   Google Play 版では `RTC_WAKEUP` (`setAndAllowWhileIdle`) の最短間隔を 15 分 (= 最大 4回/時) に制御し、
 *   天気更新とウィジェット更新のウェイクアップを統合（コアレッシング）する。
 * - Android 12+ (API 31+) の単発 WorkManager ジョブには `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST`
 *   付きの Expedited 実行を適用し、通知や FGS 権限なしで App Standby Bucket 遅延を回避する。
 */
internal object BackgroundSchedulePolicy {
    const val PLAY_MIN_ALARM_INTERVAL_MINUTES = 15L
    const val PLAY_WIDGET_ALARM_INTERVAL_MINUTES = 15L
    const val GITHUB_WIDGET_ALARM_INTERVAL_MINUTES = 10L

    const val WORKER_BACKOFF_INITIAL_SECONDS = 30L
    const val WEATHER_WORKER_MAX_ATTEMPTS = 4
    const val WIDGET_WORKER_MAX_ATTEMPTS = 3

    private const val MIN_WORKMANAGER_PERIOD_MINUTES = 15L
    private const val MIN_WORKMANAGER_FLEX_MINUTES = 5L
    private const val MAX_WORKMANAGER_FLEX_MINUTES = 15L

    fun computePeriodicIntervalMinutes(interval: WeatherUpdateInterval): Long {
        return interval.minutes.toLong().coerceAtLeast(MIN_WORKMANAGER_PERIOD_MINUTES)
    }

    fun computePeriodicFlexMinutes(intervalMinutes: Long): Long {
        val safeInterval = intervalMinutes.coerceAtLeast(MIN_WORKMANAGER_PERIOD_MINUTES)
        return (safeInterval / 3L)
            .coerceIn(MIN_WORKMANAGER_FLEX_MINUTES, MAX_WORKMANAGER_FLEX_MINUTES)
            .coerceAtMost(safeInterval)
    }

    /**
     * Play 版では `RTC_WAKEUP` の間隔を最短 15 分 (<= 4回/時) に保ち、Android Vitals 超過を防止する。
     */
    fun computeAlarmDelayMillis(requestedDelayMillis: Long, isPlayDistribution: Boolean): Long {
        val minMillis = if (isPlayDistribution) {
            PLAY_MIN_ALARM_INTERVAL_MINUTES * 60_000L
        } else {
            60_000L
        }
        return requestedDelayMillis.coerceAtLeast(minMillis)
    }

    /**
     * WorkManager と AlarmManager の重複実行による無駄な通信を抑えつつ、
     * OS の非厳密配信（数分の前倒し）で本来必要な定期更新をスキップしない閾値を算出する。
     */
    fun computeMinThrottlingGapMillis(interval: WeatherUpdateInterval): Long {
        if (interval == WeatherUpdateInterval.OFF) return Long.MAX_VALUE
        val intervalMillis = interval.minutes * 60_000L
        return (intervalMillis * 0.45).toLong().coerceIn(3 * 60_000L, 10 * 60_000L)
    }

    /**
     * Play 版でウィジェット専用の Doze 貫通アラームが必要かを判定する。
     * - 天気更新が 15 分間隔のときは天気アラーム側で全ウィジェットも同時更新するため重複アラームを張らない（合計 4回/時）。
     * - 天気更新が 30 分以上または OFF の場合は、ウィジェット専用 15 分アラームを有効化し鮮度を維持する（合計 4〜6回/時 <= 10回/時）。
     */
    fun shouldScheduleDedicatedWidgetAlarm(
        isPlayDistribution: Boolean,
        weatherInterval: WeatherUpdateInterval,
        hasActiveWidgets: Boolean
    ): Boolean {
        if (!hasActiveWidgets) return false
        if (!isPlayDistribution) return true
        return weatherInterval == WeatherUpdateInterval.OFF ||
            weatherInterval.minutes > PLAY_MIN_ALARM_INTERVAL_MINUTES
    }

    fun resolveWidgetAlarmIntervalMinutes(isPlayDistribution: Boolean): Long {
        return if (isPlayDistribution) {
            PLAY_WIDGET_ALARM_INTERVAL_MINUTES
        } else {
            GITHUB_WIDGET_ALARM_INTERVAL_MINUTES
        }
    }

    /**
     * Android 12 (API 31) 以上では Expedited JobScheduler が利用可能（FGS / 通知不要）。
     * Android 11 以下では WorkManager が FGS (`getForegroundInfo`) を要求するため通常実行とする。
     */
    fun shouldUseExpeditedWork(sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
        return sdkInt >= Build.VERSION_CODES.S
    }
}
