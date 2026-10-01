package com.example.expressiveweather

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import com.example.expressiveweather.data.CurrentLocationProvider
import com.example.expressiveweather.data.WeatherRepository
import com.example.expressiveweather.data.localizedName
import com.example.expressiveweather.ui.main.MainScreen
import com.example.expressiveweather.ui.main.MainScreenViewModel
import com.example.expressiveweather.ui.screens.WindDetailActivity
import com.example.expressiveweather.ui.screens.WindDetailDataHolder
import com.example.expressiveweather.ui.screens.WindDetailScreen
import com.example.expressiveweather.ui.screens.TodayDetailActivity
import com.example.expressiveweather.ui.screens.TodayDetailDataHolder
import com.example.expressiveweather.ui.screens.TodayDetailScreen
import com.example.expressiveweather.ui.screens.HourlyDetailActivity
import com.example.expressiveweather.ui.screens.HourlyDetailDataHolder
import com.example.expressiveweather.ui.screens.HourlyDetailScreen
import com.example.expressiveweather.ui.screens.RadarDetailActivity
import com.example.expressiveweather.ui.radar.RadarDataHolder
import com.example.expressiveweather.ui.screens.BentoDetailActivity
import com.example.expressiveweather.ui.screens.BentoDetailDataHolder

import com.example.expressiveweather.data.local.WeatherDatabase

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val mainViewModel: MainScreenViewModel = viewModel(
    factory = remember(context) {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          @Suppress("UNCHECKED_CAST")
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

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
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

          com.example.expressiveweather.ui.screens.SetupScreen(
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
              WindDetailDataHolder.set(
                weather = uiState.weatherData,
                city = uiState.selectedCity,
                language = uiState.language,
                windSpeedUnit = uiState.windSpeedUnit,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode
              )
              val intent = Intent(context, WindDetailActivity::class.java)
              context.startActivity(intent)
            },
            onNavigateToTodayDetail = { selectedDay, initialHour ->
              TodayDetailDataHolder.set(
                weather = uiState.weatherData,
                city = uiState.selectedCity,
                language = uiState.language,
                temperatureUnit = uiState.temperatureUnit,
                windSpeedUnit = uiState.windSpeedUnit,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode,
                initialSelectedDay = selectedDay,
                initialSelectedHour = initialHour
              )
              val intent = Intent(context, TodayDetailActivity::class.java)
              context.startActivity(intent)
            },
            onNavigateToHourlyDetail = { initialHour ->
              HourlyDetailDataHolder.set(
                weather = uiState.weatherData,
                city = uiState.selectedCity,
                language = uiState.language,
                temperatureUnit = uiState.temperatureUnit,
                windSpeedUnit = uiState.windSpeedUnit,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode,
                initialSelectedHour = initialHour
              )
              val intent = Intent(context, HourlyDetailActivity::class.java)
              context.startActivity(intent)
            },
            onNavigateToRadarDetail = {
              val currentCity = uiState.selectedCity
              RadarDataHolder.set(
                city = currentCity,
                latitude = currentCity?.latitude ?: 35.6895,
                longitude = currentCity?.longitude ?: 139.6917,
                cityName = currentCity?.let { it.localizedName(uiState.language) } ?: "",
                country = currentCity?.country,
                radarResponse = null,
                language = uiState.language,
                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                themePreset = uiState.themePreset,
                themeMode = uiState.themeMode,
                isDay = uiState.weatherData?.isDay ?: true,
                isRadarDebugZoomEnabled = uiState.isRadarDebugZoomEnabled,
                weatherCondition = uiState.weatherData?.condition
              )
              val intent = Intent(context, RadarDetailActivity::class.java)
              context.startActivity(intent)
            },
            onNavigateToBentoDetail = { card ->
              BentoDetailDataHolder.set(
                weather = uiState.weatherData,
                city = uiState.selectedCity,
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
            weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
            themePreset = uiState.themePreset,
            themeMode = uiState.themeMode,
            onBack = { backStack.removeLastOrNull() },
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
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
