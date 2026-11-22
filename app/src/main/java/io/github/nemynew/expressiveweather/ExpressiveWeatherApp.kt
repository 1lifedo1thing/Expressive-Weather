package io.github.nemynew.expressiveweather

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import io.github.nemynew.expressiveweather.data.JmaMunicipalityResolver
import io.github.nemynew.expressiveweather.data.WeatherBackgroundUpdateManager
import io.github.nemynew.expressiveweather.ui.widget.WeatherWidgetUpdater
import io.github.nemynew.expressiveweather.ui.widget.WidgetRefreshWorker

/**
 * ExpressiveWeather アプリケーションクラス
 * - プロセス起動時（フォアグラウンド起動、WorkManager、AlarmManager、Bootレシーバー等）に確実に初期化
 */
class ExpressiveWeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ExpressiveWeatherApp onCreate: Initializing application services...")

        // 全国の市区町村・気象庁コード辞書を初期化
        JmaMunicipalityResolver.init(applicationContext)

        // 通常天気のバックグラウンド更新システムを起動・スケジュール
        WeatherBackgroundUpdateManager.init(applicationContext)
        if (WeatherWidgetUpdater.getActiveWidgets(applicationContext).isNotEmpty()) {
            WidgetRefreshWorker.ensurePeriodicSchedule(applicationContext)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        WeatherWidgetUpdater.updateAllFromCacheAsync(applicationContext)
    }

    companion object {
        private const val TAG = "ExpressiveWeatherApp"
    }
}
