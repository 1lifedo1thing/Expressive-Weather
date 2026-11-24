package io.github.nemynew.expressiveweather.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 雨雲接近・降雨カウントダウン通知がユーザーによってスワイプ消去または停止操作された際に、
 * 常駐サービスを停止して同一降雨イベントの再通知を抑止するための内部ブロードキャストレシーバー。
 */
class PrecipitationLiveDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DISMISS_RAIN_LIVE) return
        val cityName = intent.getStringExtra(EXTRA_CITY_NAME).orEmpty()
        val statusName = intent.getStringExtra(EXTRA_RAIN_STATUS)
        val status = statusName?.let { runCatching { RainStatus.valueOf(it) }.getOrNull() }
        val targetTimestampMillis = if (intent.hasExtra(EXTRA_TARGET_TIMESTAMP_MS)) {
            intent.getLongExtra(EXTRA_TARGET_TIMESTAMP_MS, -1L).takeIf { it > 0L }
        } else {
            null
        }
        Log.d(TAG, "User dismissed rain live notification (city=$cityName, status=$status)")
        PrecipitationLiveNotificationManager.dismissByUser(
            context = context.applicationContext,
            cityName = cityName,
            status = status,
            targetTimestampMillis = targetTimestampMillis
        )
    }

    companion object {
        private const val TAG = "RainLiveDismissRcvr"
        const val ACTION_DISMISS_RAIN_LIVE = "io.github.nemynew.expressiveweather.action.DISMISS_RAIN_LIVE"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_RAIN_STATUS = "extra_rain_status"
        const val EXTRA_TARGET_TIMESTAMP_MS = "extra_target_timestamp_ms"
    }
}
