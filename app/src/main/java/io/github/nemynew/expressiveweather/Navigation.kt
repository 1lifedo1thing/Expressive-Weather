package io.github.nemynew.expressiveweather

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.nemynew.expressiveweather.data.CurrentLocationProvider
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.ui.main.MainScreen
import io.github.nemynew.expressiveweather.ui.main.MainScreenViewModel
import io.github.nemynew.expressiveweather.ui.screens.WindDetailActivity
import io.github.nemynew.expressiveweather.ui.screens.WindDetailDataHolder
import io.github.nemynew.expressiveweather.ui.screens.WindDetailScreen
import io.github.nemynew.expressiveweather.ui.screens.TodayDetailActivity
import io.github.nemynew.expressiveweather.ui.screens.TodayDetailDataHolder
import io.github.nemynew.expressiveweather.ui.screens.TodayDetailScreen
import io.github.nemynew.expressiveweather.ui.screens.HourlyDetailActivity
import io.github.nemynew.expressiveweather.ui.screens.HourlyDetailDataHolder
import io.github.nemynew.expressiveweather.ui.screens.HourlyDetailScreen
import io.github.nemynew.expressiveweather.ui.screens.RadarDetailActivity
import io.github.nemynew.expressiveweather.ui.radar.RadarDataHolder
import io.github.nemynew.expressiveweather.ui.screens.BentoDetailActivity
import io.github.nemynew.expressiveweather.ui.screens.BentoDetailDataHolder

import io.github.nemynew.expressiveweather.data.local.WeatherDatabase

@Composable
fun MainNavigation(
  widgetLaunchRequest: WidgetLaunchRequest? = null,
  onWidgetLaunchHandled: (Long) -> Unit = {}
) {
  val context = LocalContext.current
  val mainViewModel: MainScreenViewModel = viewModel(
    factory = remember(context) {
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val database = WeatherDatabase.getInstance(context.applicationContext)
          return MainScreenViewModel(
            repository = WeatherRepository(
              weatherDao = database.weatherDao(),
              savedCityDao = database.savedCityDao(),
              context = context.applicationContext
            ),
            locationProvider = CurrentLocationProvider(context.applicationContext)
          ) as T
        }
      }
    }
  )

  val uiState by mainViewModel.uiState.collectAsState()
  val isSetupDone = remember { WeatherRepository.isSetupCompleted(context.applicationContext) }
  val backStack = rememberNavBackStack(if (isSetupDone) Main else Setup)

  LaunchedEffect(widgetLaunchRequest?.eventId) {
    val req = widgetLaunchRequest ?: return@LaunchedEffect
    if (req.selectCurrentLocation) {
      mainViewModel.selectCurrentLocation()
    } else if (req.pinnedCity != null) {
      mainViewModel.selectCity(req.pinnedCity)
    }
    if (backStack.lastOrNull() != Main) {
      backStack.clear()
      backStack.add(Main)
    }
    onWidgetLaunchHandled(req.eventId)
  }

  NavDisplay(
    backStack = backStack,
    onBack = {
      if (backStack.size > 1) {
        backStack.removeLastOrNull()
      }
    },
    entryProvider =
      entryProvider {
        entry<Setup> {
          val database = remember(context) { WeatherDatabase.getInstance(context.applicationContext) }
          val repository = remember(context) {
            WeatherRepository(
              weatherDao = database.weatherDao(),
              savedCityDao = database.savedCityDao(),
              context = context.applicationContext
            )
          }
          val locationProvider = remember(context) {
            CurrentLocationProvider(context.applicationContext)
          }

          io.github.nemynew.expressiveweather.ui.screens.SetupScreen(
            repository = repository,
            locationProvider = locationProvider,
            onSetupComplete = { city ->
              mainViewModel.selectCity(city)
              backStack.clear()
              backStack.add(Main)
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Main> {
          MainScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = mainViewModel,
            onNavigateToWindDetail = {
              val currentCity = uiState.selectedCity
              WindDetailDataHolder.set(
                weather = uiState.weatherData,
                city = currentCity,
                language = uiState.language,
                windSpeedUnit = uiState.windSpeedUnit,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode
              )
              val intent = Intent(context, WindDetailActivity::class.java).apply {
                putExtra(WeatherRepository.EXTRA_CITY_JSON, WeatherRepository.encodeCityJson(currentCity))
              }
              context.startActivity(intent)
            },
            onNavigateToTodayDetail = { selectedDay, initialHour ->
              val currentCity = uiState.selectedCity
              TodayDetailDataHolder.set(
                weather = uiState.weatherData,
                city = currentCity,
                language = uiState.language,
                temperatureUnit = uiState.temperatureUnit,
                windSpeedUnit = uiState.windSpeedUnit,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode,
                initialSelectedDay = selectedDay,
                initialSelectedHour = initialHour
              )
              val intent = Intent(context, TodayDetailActivity::class.java).apply {
                putExtra(WeatherRepository.EXTRA_CITY_JSON, WeatherRepository.encodeCityJson(currentCity))
                putExtra(WeatherRepository.EXTRA_INITIAL_DAY, selectedDay)
                if (initialHour != null) {
                  putExtra(WeatherRepository.EXTRA_INITIAL_HOUR, initialHour)
                }
              }
              context.startActivity(intent)
            },
            onNavigateToHourlyDetail = { initialHour ->
              val currentCity = uiState.selectedCity
              HourlyDetailDataHolder.set(
                weather = uiState.weatherData,
                city = currentCity,
                language = uiState.language,
                temperatureUnit = uiState.temperatureUnit,
                windSpeedUnit = uiState.windSpeedUnit,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode,
                initialSelectedHour = initialHour
              )
              val intent = Intent(context, HourlyDetailActivity::class.java).apply {
                putExtra(WeatherRepository.EXTRA_CITY_JSON, WeatherRepository.encodeCityJson(currentCity))
                if (initialHour != null) {
                  putExtra(WeatherRepository.EXTRA_INITIAL_HOUR, initialHour)
                }
              }
              context.startActivity(intent)
            },
            onNavigateToRadarDetail = {
              val currentCity = uiState.selectedCity
              RadarDataHolder.set(
                city = currentCity,
                latitude = currentCity.latitude,
                longitude = currentCity.longitude,
                cityName = currentCity.localizedName(uiState.language),
                country = currentCity.country,
                radarResponse = null,
                language = uiState.language,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode,
                isDay = uiState.weatherData?.isDay ?: true,
                isRadarDebugZoomEnabled = uiState.isRadarDebugZoomEnabled,
                showAllJapanRadarLayers = uiState.showAllJapanRadarLayers,
                weatherCondition = uiState.weatherData?.condition
              )
              val intent = Intent(context, RadarDetailActivity::class.java).apply {
                putExtra(WeatherRepository.EXTRA_CITY_JSON, WeatherRepository.encodeCityJson(currentCity))
                putExtra("extra_lat", currentCity.latitude)
                putExtra("extra_lon", currentCity.longitude)
                putExtra("city_name", currentCity.localizedName(uiState.language))
              }
              context.startActivity(intent)
            },
            onNavigateToBentoDetail = { card ->
              val currentCity = uiState.selectedCity
              BentoDetailDataHolder.set(
                weather = uiState.weatherData,
                city = currentCity,
                cardType = card,
                language = uiState.language,
                temperatureUnit = uiState.temperatureUnit,
                windSpeedUnit = uiState.windSpeedUnit,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode
              )
              val intent = Intent(context, BentoDetailActivity::class.java).apply {
                putExtra(BentoDetailActivity.EXTRA_CARD_TYPE, card.name)
                putExtra(WeatherRepository.EXTRA_CITY_JSON, WeatherRepository.encodeCityJson(currentCity))
              }
              context.startActivity(intent)
            },
            onNavigateToAlertDetail = { _ -> }
          )
        }
        entry<WindDetail> {
          WindDetailScreen(
            weather = uiState.weatherData,
            city = uiState.selectedCity,
            language = uiState.language,
            windSpeedUnit = uiState.windSpeedUnit,
            weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
            themePreset = uiState.themePreset,
            themeMode = uiState.themeMode,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<TodayDetail> {
          TodayDetailScreen(
            weather = uiState.weatherData,
            city = uiState.selectedCity,
            language = uiState.language,
            temperatureUnit = uiState.temperatureUnit,
            windSpeedUnit = uiState.windSpeedUnit,
            weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
            themePreset = uiState.themePreset,
            themeMode = uiState.themeMode,
            initialSelectedDay = 0,
            initialSelectedHour = null,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
