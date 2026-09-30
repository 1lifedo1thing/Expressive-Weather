package com.example.expressiveweather

import android.app.Application
import android.util.Log
import com.example.expressiveweather.data.JmaMunicipalityResolver
import com.example.expressiveweather.data.WeatherBackgroundUpdateManager

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

        // 通常天気および警報のバックグラウンド更新システムを起動・スケジュール
        WeatherBackgroundUpdateManager.init(applicationContext)
    }

    companion object {
        private const val TAG = "ExpressiveWeatherApp"
    }
}
