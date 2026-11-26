package io.github.nemynew.expressiveweather.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.theme.currentAccentPalette

/**
 * Microsoft Fluent UI System Icons による天気状態アイコンヘルパー
 */
object WeatherIconHelper {
    @DrawableRes
    fun getConditionDrawable(condition: WeatherCondition, isDay: Boolean = true): Int {
        return if (isDay) {
            when (condition) {
                WeatherCondition.CLEAR -> R.drawable.ic_fluent_weather_sunny_24
                WeatherCondition.FEW_CLOUDS -> R.drawable.ic_fluent_weather_partly_cloudy_day_24
                WeatherCondition.CLOUDY -> R.drawable.ic_fluent_weather_cloudy_24
                WeatherCondition.RAIN -> R.drawable.ic_fluent_weather_drizzle_24
                WeatherCondition.HEAVY_RAIN -> R.drawable.ic_fluent_weather_rain_24
                WeatherCondition.THUNDERSTORM -> R.drawable.ic_fluent_weather_thunderstorm_24
                WeatherCondition.SNOW -> R.drawable.ic_fluent_weather_snow_24
                WeatherCondition.FOG -> R.drawable.ic_fluent_weather_fog_24
            }
        } else {
            when (condition) {
                WeatherCondition.CLEAR -> R.drawable.ic_fluent_weather_moon_24
                WeatherCondition.FEW_CLOUDS -> R.drawable.ic_fluent_weather_partly_cloudy_night_24
                WeatherCondition.CLOUDY -> R.drawable.ic_fluent_weather_cloudy_24
                WeatherCondition.RAIN -> R.drawable.ic_fluent_weather_drizzle_24
                WeatherCondition.HEAVY_RAIN -> R.drawable.ic_fluent_weather_rain_24
                WeatherCondition.THUNDERSTORM -> R.drawable.ic_fluent_weather_thunderstorm_24
                WeatherCondition.SNOW -> R.drawable.ic_fluent_weather_snow_24
                WeatherCondition.FOG -> R.drawable.ic_fluent_weather_fog_24
            }
        }
    }

    @Composable
    fun getConditionIconTint(condition: WeatherCondition): Color {
        val colorScheme = MaterialTheme.colorScheme
        val palette = currentAccentPalette()
        return when (condition) {
            WeatherCondition.CLEAR -> palette.sun.accent
            WeatherCondition.FEW_CLOUDS -> palette.uv.accent
            WeatherCondition.CLOUDY -> colorScheme.onSurfaceVariant
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> palette.precipitation.accent
            WeatherCondition.THUNDERSTORM -> palette.pressure.accent
            WeatherCondition.SNOW -> palette.visibility.accent
            WeatherCondition.FOG -> colorScheme.onSurfaceVariant
        }
    }
}
