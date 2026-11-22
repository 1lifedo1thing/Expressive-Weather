package io.github.nemynew.expressiveweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.ui.main.MainScreen

/** Debug source set only: never replaces the actual API weather or ships in release. */
class RainPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val condition = WeatherCondition.entries.firstOrNull {
            it.name == intent.getStringExtra("condition")
        } ?: WeatherCondition.RAIN
        val isDay = !intent.getBooleanExtra("night", false)
        setContent {
            MainScreen(backgroundCondition = condition, backgroundIsDay = isDay)
        }
    }
}
