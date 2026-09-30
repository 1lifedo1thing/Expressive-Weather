package com.example.expressiveweather.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.expressiveweather.data.CurrentLocationProvider
import com.example.expressiveweather.data.WeatherRepository
import com.example.expressiveweather.data.local.WeatherDatabase
import com.example.expressiveweather.theme.ExpressiveWeatherTheme

/**
 * セットアップ画面（SetupScreen）を独立して起動するための Activity。
 * 設定画面の「初期セットアップを再表示」や、外部Intentからの直接プレビューに使用。
 */
class SetupActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.example.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        val database = WeatherDatabase.getInstance(applicationContext)
        val repository = WeatherRepository(
            weatherDao = database.weatherDao(),
            savedCityDao = database.savedCityDao(),
            context = applicationContext
        )
        val locationProvider = CurrentLocationProvider(applicationContext)
        val language = repository.getLanguage()

        setContent {
            ExpressiveWeatherTheme(language = language) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SetupScreen(
                        repository = repository,
                        locationProvider = locationProvider,
                        onSetupComplete = { city ->
                            setResult(RESULT_OK)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
