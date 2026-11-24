package io.github.nemynew.expressiveweather.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 警戒レベル・キキクル Live Updates 通知がユーザーによってスワイプ消去または停止操作された際に、
 * 常駐サービスを停止し、同一レベル以下の再通知を抑止するための内部ブロードキャストレシーバー。
 */
class KikikuruAlertDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DISMISS_KIKIKURU_ALERT) return
        val municipalityCode = intent.getStringExtra(EXTRA_MUNICIPALITY_CODE).orEmpty()
        val level = intent.getIntExtra(EXTRA_ALERT_LEVEL, 0)
        val signature = intent.getStringExtra(EXTRA_ALERT_SIGNATURE).orEmpty()
        Log.d(TAG, "User dismissed Kikikuru alert notification (muni=$municipalityCode, lv=$level)")
        KikikuruNotificationManager.dismissByUser(
            context = context.applicationContext,
            municipalityCode = municipalityCode,
            dismissedLevel = level,
            signature = signature
        )
    }

    companion object {
        private const val TAG = "KikikuruDismissRcvr"
        const val ACTION_DISMISS_KIKIKURU_ALERT = "io.github.nemynew.expressiveweather.action.DISMISS_KIKIKURU_ALERT"
        const val EXTRA_MUNICIPALITY_CODE = "extra_municipality_code"
        const val EXTRA_ALERT_LEVEL = "extra_alert_level"
        const val EXTRA_ALERT_SIGNATURE = "extra_alert_signature"
    }
}
