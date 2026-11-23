package io.github.nemynew.expressiveweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherJsonSerializer
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.ui.widget.WeatherWidgetUpdater
import io.github.nemynew.expressiveweather.util.LocaleUtils

data class WidgetLaunchRequest(
  val eventId: Long,
  val selectCurrentLocation: Boolean,
  val pinnedCity: City?
)

class MainActivity : ComponentActivity() {
  private val currentLanguageState = mutableStateOf(AppLanguage.JAPANESE)
  private val widgetLaunchState = mutableStateOf<WidgetLaunchRequest?>(null)
  private var nextWidgetEventId: Long = 1L

  override fun attachBaseContext(newBase: android.content.Context) {
    super.attachBaseContext(LocaleUtils.applyLocaleToContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LocaleUtils.applyLocaleToActivity(this)
    val appLang = LocaleUtils.getSavedLanguage(this)
    currentLanguageState.value = appLang

    // 全国の市区町村・気象庁コード辞書を起動時にロード
    io.github.nemynew.expressiveweather.data.JmaMunicipalityResolver.init(applicationContext)
    // 通常天気および警報のバックグラウンド更新マネージャーを起動
    io.github.nemynew.expressiveweather.data.WeatherBackgroundUpdateManager.init(applicationContext)
    io.github.nemynew.expressiveweather.data.KikikuruBackgroundMonitor.getInstance(applicationContext)

    enableEdgeToEdge()
    if (savedInstanceState == null) {
      handleWidgetIntent(intent)
    }
    setContent {
      val activeLanguage by currentLanguageState
      val widgetLaunch by widgetLaunchState
      ExpressiveWeatherTheme(language = activeLanguage) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            widgetLaunchRequest = widgetLaunch,
            onWidgetLaunchHandled = { handledId ->
              if (widgetLaunchState.value?.eventId == handledId) {
                widgetLaunchState.value = null
              }
            }
          )
        }
      }
    }

    if (savedInstanceState == null && BuildConfig.DEBUG) {
      handleTestIntent(intent)
    }
  }

  override fun onResume() {
    super.onResume()
    val savedLang = LocaleUtils.getSavedLanguage(this)
    if (currentLanguageState.value != savedLang) {
      currentLanguageState.value = savedLang
      LocaleUtils.applyLocaleToActivity(this)
    }
    io.github.nemynew.expressiveweather.data.WeatherBackgroundUpdateManager.getInstance(applicationContext).onAppForegrounded()
    io.github.nemynew.expressiveweather.data.KikikuruBackgroundMonitor.getInstance(applicationContext).onAppForegrounded()
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleWidgetIntent(intent)
    if (BuildConfig.DEBUG) {
      handleTestIntent(intent)
    }
  }

  private fun handleWidgetIntent(intent: android.content.Intent?) {
    if (intent == null) return
    val hasWidgetSelectCurrent = intent.hasExtra(WeatherWidgetUpdater.EXTRA_WIDGET_SELECT_CURRENT)
    val cityJson = intent.getStringExtra(WeatherRepository.EXTRA_CITY_JSON)
    if (!hasWidgetSelectCurrent && cityJson.isNullOrBlank()) return

    val selectCurrent = intent.getBooleanExtra(WeatherWidgetUpdater.EXTRA_WIDGET_SELECT_CURRENT, false)
    val pinnedCity = if (!selectCurrent && !cityJson.isNullOrBlank()) {
      try {
        WeatherJsonSerializer.json.decodeFromString<City>(cityJson).takeIf { city ->
          city.latitude.isFinite() &&
            city.longitude.isFinite() &&
            city.latitude in -90.0..90.0 &&
            city.longitude in -180.0..180.0
        }
      } catch (_: Exception) {
        null
      }
    } else null

    if (selectCurrent || pinnedCity != null) {
      widgetLaunchState.value = WidgetLaunchRequest(
        eventId = nextWidgetEventId++,
        selectCurrentLocation = selectCurrent,
        pinnedCity = pinnedCity
      )
    }
  }

  private fun handleTestIntent(intent: android.content.Intent?) {
    if (!BuildConfig.DEBUG || intent == null) return
    if (intent.getBooleanExtra("trigger_test_alert", false)) {
      val levelInt = intent.getIntExtra("alert_level", 4)
      val level = io.github.nemynew.expressiveweather.data.AlertLevel.fromLevel(levelInt)
      io.github.nemynew.expressiveweather.data.KikikuruNotificationManager.sendTestNotification(this, level)
    }
    if (intent.getBooleanExtra("open_radar", false)) {
      startActivity(android.content.Intent(this, io.github.nemynew.expressiveweather.ui.screens.RadarDetailActivity::class.java))
    }
    if (intent.getBooleanExtra("open_settings", false)) {
      val section = intent.getStringExtra("settings_section") ?: io.github.nemynew.expressiveweather.ui.screens.SettingsDataHolder.SECTION_ALL
      val settingsIntent = android.content.Intent(this, io.github.nemynew.expressiveweather.ui.screens.SettingsActivity::class.java).apply {
        putExtra("initial_section", section)
      }
      startActivity(settingsIntent)
    }
    if (intent.getBooleanExtra("open_setup", false)) {
      startActivity(android.content.Intent(this, io.github.nemynew.expressiveweather.ui.screens.SetupActivity::class.java))
    }
    if (intent.hasExtra("test_rain_status")) {
      val statusName = intent.getStringExtra("test_rain_status")
      val status = if (statusName.isNullOrBlank() || statusName == "CLEAR" || statusName == "null") {
        null
      } else {
        io.github.nemynew.expressiveweather.data.RainStatus.entries.firstOrNull { it.name == statusName }
      }
      val mock = if (status != null) io.github.nemynew.expressiveweather.data.PrecipitationAnalyzer.createMock(status) else null
      io.github.nemynew.expressiveweather.ui.main.MainScreenViewModel.staticNowcastOverride.value = mock
      if (mock != null && (mock.status == io.github.nemynew.expressiveweather.data.RainStatus.RAIN_APPROACHING || mock.status == io.github.nemynew.expressiveweather.data.RainStatus.RAINING)) {
        io.github.nemynew.expressiveweather.data.PrecipitationLiveNotificationManager.sendTestNotification(this, mock.status)
      } else {
        io.github.nemynew.expressiveweather.data.PrecipitationLiveNotificationManager.cancel(this)
      }
    }
    if (intent.hasExtra("test_nws_alert")) {
      val enabled = intent.getBooleanExtra("test_nws_alert", false)
      val alerts = if (enabled) io.github.nemynew.expressiveweather.data.NwsAlertRepository.createSampleAlerts() else null
      io.github.nemynew.expressiveweather.ui.main.MainScreenViewModel.staticGlobalAlertsOverride.value = alerts
    }
    if (intent.hasExtra("test_gdacs_alert")) {
      val enabled = intent.getBooleanExtra("test_gdacs_alert", false)
      val alerts = if (enabled) io.github.nemynew.expressiveweather.data.GdacsAlertRepository.createSampleAlerts() else null
      io.github.nemynew.expressiveweather.ui.main.MainScreenViewModel.staticGlobalAlertsOverride.value = alerts
    }
  }
}
