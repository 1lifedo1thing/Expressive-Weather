package com.example.expressiveweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.expressiveweather.theme.ExpressiveWeatherTheme

class MainActivity : ComponentActivity() {
  override fun attachBaseContext(newBase: android.content.Context) {
    super.attachBaseContext(com.example.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
    val appLang = com.example.expressiveweather.util.LocaleUtils.getSavedLanguage(this)

    // 全国の市区町村・気象庁コード辞書を起動時にロード
    com.example.expressiveweather.data.JmaMunicipalityResolver.init(applicationContext)
    // 通常天気および警報のバックグラウンド更新マネージャーを起動
    com.example.expressiveweather.data.WeatherBackgroundUpdateManager.init(applicationContext)
    com.example.expressiveweather.data.KikikuruBackgroundMonitor.getInstance(applicationContext)

    enableEdgeToEdge()
    setContent {
      ExpressiveWeatherTheme(language = appLang) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation()
        }
      }
    }


    handleTestIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    com.example.expressiveweather.data.WeatherBackgroundUpdateManager.getInstance(applicationContext).onAppForegrounded()
    com.example.expressiveweather.data.KikikuruBackgroundMonitor.getInstance(applicationContext).onAppForegrounded()
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleTestIntent(intent)
  }

  private fun handleTestIntent(intent: android.content.Intent?) {
    if (intent?.getBooleanExtra("trigger_test_alert", false) == true) {
      val levelInt = intent.getIntExtra("alert_level", 4)
      val level = com.example.expressiveweather.data.AlertLevel.fromLevel(levelInt)
      com.example.expressiveweather.data.KikikuruNotificationManager.sendTestNotification(this, level)
    }
    if (intent?.getBooleanExtra("open_radar", false) == true) {
      startActivity(android.content.Intent(this, com.example.expressiveweather.ui.screens.RadarDetailActivity::class.java))
    }
    if (intent?.getBooleanExtra("open_settings", false) == true) {
      val section = intent.getStringExtra("settings_section") ?: com.example.expressiveweather.ui.screens.SettingsDataHolder.SECTION_ALL
      com.example.expressiveweather.ui.screens.SettingsDataHolder.initialSection = section
      startActivity(android.content.Intent(this, com.example.expressiveweather.ui.screens.SettingsActivity::class.java))
    }
    if (intent?.getBooleanExtra("open_setup", false) == true) {
      startActivity(android.content.Intent(this, com.example.expressiveweather.ui.screens.SetupActivity::class.java))
    }
    if (intent?.hasExtra("test_rain_status") == true) {
      val statusName = intent.getStringExtra("test_rain_status")
      val status = if (statusName == "CLEAR" || statusName == "null" || statusName == null) null else com.example.expressiveweather.data.RainStatus.valueOf(statusName)
      val mock = if (status != null) com.example.expressiveweather.data.PrecipitationAnalyzer.createMock(status) else null
      com.example.expressiveweather.ui.main.MainScreenViewModel.staticNowcastOverride.value = mock
      if (mock != null && (mock.status == com.example.expressiveweather.data.RainStatus.RAIN_APPROACHING || mock.status == com.example.expressiveweather.data.RainStatus.RAINING)) {
        com.example.expressiveweather.data.PrecipitationLiveNotificationManager.sendTestNotification(this, mock.status)
      } else {
        com.example.expressiveweather.data.PrecipitationLiveNotificationManager.cancel(this)
      }
    }
    if (intent?.hasExtra("test_nws_alert") == true) {
      val enabled = intent.getBooleanExtra("test_nws_alert", false)
      val alerts = if (enabled) com.example.expressiveweather.data.NwsAlertRepository.createSampleAlerts() else null
      com.example.expressiveweather.ui.main.MainScreenViewModel.staticGlobalAlertsOverride.value = alerts
    }
    if (intent?.hasExtra("test_gdacs_alert") == true) {
      val enabled = intent.getBooleanExtra("test_gdacs_alert", false)
      val alerts = if (enabled) com.example.expressiveweather.data.GdacsAlertRepository.createSampleAlerts() else null
      com.example.expressiveweather.ui.main.MainScreenViewModel.staticGlobalAlertsOverride.value = alerts
    }
  }
}
