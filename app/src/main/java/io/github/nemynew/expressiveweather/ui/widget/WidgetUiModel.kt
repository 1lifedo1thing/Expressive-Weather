package io.github.nemynew.expressiveweather.ui.widget

import android.graphics.Color
import io.github.nemynew.expressiveweather.data.AlertLevel
import io.github.nemynew.expressiveweather.data.AlertSeverity
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.GlobalAlertItem
import io.github.nemynew.expressiveweather.data.RainStatus
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.data.getLocalizedLabel
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.data.localizedName

data class WidgetHourlyItem(
    val timeLabel: String,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val tempText: String,
    val precipProbText: String,
    val precipProb: Int
)

data class WidgetDailyItem(
    val dayLabel: String,
    val condition: WeatherCondition,
    val precipProbText: String,
    val precipProb: Int,
    val minTempText: String,
    val maxTempText: String,
    val minTempVal: Int,
    val maxTempVal: Int
)

data class WidgetAlertBanner(
    val headline: String,
    val subText: String,
    val badgeColorArgb: Int,
    val isDisasterAlert: Boolean,
    val minutelyPrecipPoints: List<Double> = emptyList()
)

data class WidgetUiModel(
    val targetCity: City?,
    val isCurrentLocation: Boolean,
    val cityName: String,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val currentTempText: String,
    val feelsLikeText: String,
    val highLowText: String,
    val compactSubtitle: String,
    val conditionLabel: String,
    val humidityWindText: String,
    val updatedTimeText: String,
    val alertBanner: WidgetAlertBanner?,
    val hourlyItems: List<WidgetHourlyItem>,
    val dailyItems: List<WidgetDailyItem>,
    val weekMinTemp: Int,
    val weekMaxTemp: Int,
    val hasData: Boolean,
    val isRefreshing: Boolean = false
)

/**
 * WeatherData + ユーザー設定からウィジェット表示用の純粋モデルを構築
 */
object WidgetUiModelBuilder {

    fun resolveTargetCity(
        config: WidgetConfig,
        currentLocationCity: City?,
        fallbackCity: City
    ): City {
        return if (config.useCurrentLocation || config.pinnedCity == null) {
            currentLocationCity ?: fallbackCity
        } else {
            config.pinnedCity
        }
    }

    fun build(
        config: WidgetConfig,
        weather: WeatherData?,
        language: AppLanguage,
        temperatureUnit: TemperatureUnit,
        windSpeedUnit: WindSpeedUnit,
        fallbackCity: City,
        isRefreshing: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): WidgetUiModel {
        val targetCity = resolveTargetCity(config, fallbackCity, fallbackCity)
        val displayCity = weather?.city ?: targetCity
        val cityName = displayCity.localizedName(language)

        if (weather == null) {
            val loadingText = when (language) {
                AppLanguage.JAPANESE -> if (isRefreshing) "更新中…" else "タップして天気を取得"
                AppLanguage.KOREAN -> if (isRefreshing) "업데이트 중…" else "탭하여 날씨 불러오기"
                AppLanguage.ENGLISH -> if (isRefreshing) "Updating…" else "Tap to load weather"
                AppLanguage.SIMPLIFIED_CHINESE -> if (isRefreshing) "正在更新…" else "点击获取天气"
                AppLanguage.TRADITIONAL_CHINESE -> if (isRefreshing) "更新中…" else "點擊取得天氣"
                AppLanguage.SPANISH -> if (isRefreshing) "Actualizando…" else "Toca para cargar el clima"
                AppLanguage.FRENCH -> if (isRefreshing) "Mise à jour…" else "Appuyer pour charger la météo"
                AppLanguage.PORTUGUESE -> if (isRefreshing) "Atualizando…" else "Toque para carregar o clima"
            }
            return WidgetUiModel(
                targetCity = targetCity,
                isCurrentLocation = config.useCurrentLocation,
                cityName = cityName,
                condition = WeatherCondition.CLEAR,
                isDay = true,
                currentTempText = "--°",
                feelsLikeText = "",
                highLowText = "--° / --°",
                compactSubtitle = loadingText,
                conditionLabel = loadingText,
                humidityWindText = "",
                updatedTimeText = if (isRefreshing) "..." else "",
                alertBanner = null,
                hourlyItems = emptyList(),
                dailyItems = emptyList(),
                weekMinTemp = 0,
                weekMaxTemp = 30,
                hasData = false,
                isRefreshing = isRefreshing
            )
        }

        val currentTemp = temperatureUnit.convert(weather.currentTempC)
        val feelsLikeTemp = temperatureUnit.convert(weather.feelsLikeC)
        val maxTemp = temperatureUnit.convert(weather.maxTempC)
        val minTemp = temperatureUnit.convert(weather.minTempC)
        val conditionLabel = weather.condition.getLocalizedLabel(language)

        val feelsPrefix = when (language) {
            AppLanguage.JAPANESE -> "体感"
            AppLanguage.KOREAN -> "체감"
            AppLanguage.ENGLISH -> "Feels"
            AppLanguage.SIMPLIFIED_CHINESE -> "体感"
            AppLanguage.TRADITIONAL_CHINESE -> "體感"
            AppLanguage.SPANISH -> "Sensación"
            AppLanguage.FRENCH -> "Ressenti"
            AppLanguage.PORTUGUESE -> "Sensação"
        }
        val feelsLikeText = "$feelsPrefix ${feelsLikeTemp}°"
        val highLowText = "↑${maxTemp}° ↓${minTemp}°"

        val humidityLabel = when (language) {
            AppLanguage.JAPANESE -> "湿度"
            AppLanguage.KOREAN -> "습도"
            AppLanguage.ENGLISH -> "Humidity"
            AppLanguage.SIMPLIFIED_CHINESE -> "湿度"
            AppLanguage.TRADITIONAL_CHINESE -> "濕度"
            AppLanguage.SPANISH -> "Humedad"
            AppLanguage.FRENCH -> "Humidité"
            AppLanguage.PORTUGUESE -> "Umidade"
        }
        val windLabel = when (language) {
            AppLanguage.JAPANESE -> "風"
            AppLanguage.KOREAN -> "바람"
            AppLanguage.ENGLISH -> "Wind"
            AppLanguage.SIMPLIFIED_CHINESE -> "风速"
            AppLanguage.TRADITIONAL_CHINESE -> "風速"
            AppLanguage.SPANISH -> "Viento"
            AppLanguage.FRENCH -> "Vent"
            AppLanguage.PORTUGUESE -> "Vento"
        }
        val windFormatted = windSpeedUnit.formatCompact(weather.windSpeedKmH, language)
        val dotSep = when (language) {
            AppLanguage.ENGLISH,
            AppLanguage.SPANISH,
            AppLanguage.FRENCH,
            AppLanguage.PORTUGUESE -> " • "
            AppLanguage.KOREAN -> " · "
            AppLanguage.JAPANESE,
            AppLanguage.SIMPLIFIED_CHINESE,
            AppLanguage.TRADITIONAL_CHINESE -> " ・ "
        }
        val humidityWindText = "$humidityLabel ${weather.humidity}%$dotSep$windLabel $windFormatted"

        val updatingSuffix = when (language) {
            AppLanguage.JAPANESE -> "更新中…"
            AppLanguage.KOREAN -> "업데이트 중…"
            AppLanguage.ENGLISH -> "Updating…"
            AppLanguage.SIMPLIFIED_CHINESE -> "更新中…"
            AppLanguage.TRADITIONAL_CHINESE -> "更新中…"
            AppLanguage.SPANISH -> "Actualizando…"
            AppLanguage.FRENCH -> "Mise à jour…"
            AppLanguage.PORTUGUESE -> "Atualizando…"
        }
        val updatedTimeText = if (isRefreshing) {
            updatingSuffix
        } else {
            weather.lastUpdatedTime.ifBlank { "" }
        }

        val alertBanner: WidgetAlertBanner? = null

        val compactSubtitle = "$conditionLabel$dotSep$highLowText"

        val hourlyItems = weather.hourlyForecast.take(6).map { item ->
            val t = temperatureUnit.convert(item.tempC)
            WidgetHourlyItem(
                timeLabel = getLocalizedTimeLabel(item.timeLabel, language),
                condition = item.condition,
                isDay = item.isDay,
                tempText = "${t}°",
                precipProbText = if (item.precipProb >= 10) "${item.precipProb}%" else "",
                precipProb = item.precipProb
            )
        }

        val dailyConverted = resolveDailyItems(
            dailyList = weather.dailyForecast,
            language = language,
            temperatureUnit = temperatureUnit,
            nowMillis = nowMillis,
            utcOffsetSeconds = weather.utcOffsetSeconds
        )
        val weekMinTemp = dailyConverted.minOfOrNull { it.minTempVal } ?: minTemp
        val weekMaxTemp = dailyConverted.maxOfOrNull { it.maxTempVal } ?: maxTemp

        return WidgetUiModel(
            targetCity = targetCity,
            isCurrentLocation = config.useCurrentLocation,
            cityName = cityName,
            condition = weather.condition,
            isDay = weather.isDay,
            currentTempText = "${currentTemp}°",
            feelsLikeText = feelsLikeText,
            highLowText = highLowText,
            compactSubtitle = compactSubtitle,
            conditionLabel = conditionLabel,
            humidityWindText = humidityWindText,
            updatedTimeText = updatedTimeText,
            alertBanner = alertBanner,
            hourlyItems = hourlyItems,
            dailyItems = dailyConverted,
            weekMinTemp = weekMinTemp,
            weekMaxTemp = maxOf(weekMaxTemp, weekMinTemp + 1),
            hasData = true,
            isRefreshing = isRefreshing
        )
    }

    private fun resolveDailyItems(
        dailyList: List<io.github.nemynew.expressiveweather.data.DailyForecast>,
        language: AppLanguage,
        temperatureUnit: TemperatureUnit,
        nowMillis: Long,
        utcOffsetSeconds: Int? = null
    ): List<WidgetDailyItem> {
        if (dailyList.isEmpty()) return emptyList()
        val timeZone = utcOffsetSeconds?.let { java.util.SimpleTimeZone(it * 1000, "CityTZ") }
            ?: java.util.TimeZone.getDefault()
        val todayCal = java.util.Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val tomorrowCal = (todayCal.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val withParsedDates = dailyList.map { day ->
            day to parseMonthDay(day.dateLabel, todayCal)
        }
        val validCurrentOrFuture = withParsedDates.filter { (_, cal) ->
            cal == null || !cal.before(todayCal)
        }
        val firstParsed = withParsedDates.firstOrNull()?.second
        val recentCacheShift = firstParsed != null &&
            ((todayCal.timeInMillis - firstParsed.timeInMillis) in 0L..(7L * 24 * 60 * 60 * 1000L))

        val effectiveList = if (recentCacheShift && validCurrentOrFuture.isNotEmpty()) {
            validCurrentOrFuture.take(7)
        } else {
            withParsedDates.take(7)
        }

        return effectiveList.map { (day, itemCal) ->
            val dMin = temperatureUnit.convert(day.minTempC)
            val dMax = temperatureUnit.convert(day.maxTempC)
            val resolvedLabel = when {
                itemCal != null && recentCacheShift && isSameDay(itemCal, todayCal) ->
                    getLocalizedDayLabel("Today", language)
                itemCal != null && recentCacheShift && isSameDay(itemCal, tomorrowCal) ->
                    getLocalizedDayLabel("Tomorrow", language)
                itemCal != null && recentCacheShift ->
                    dayOfWeekKeyToLocalized(itemCal.get(java.util.Calendar.DAY_OF_WEEK), language)
                else -> getLocalizedDayLabel(day.dayLabel, language)
            }
            WidgetDailyItem(
                dayLabel = resolvedLabel,
                condition = day.condition,
                precipProbText = if (day.precipProb >= 15) "${day.precipProb}%" else "",
                precipProb = day.precipProb,
                minTempText = "${dMin}°",
                maxTempText = "${dMax}°",
                minTempVal = dMin,
                maxTempVal = dMax
            )
        }
    }

    private fun parseMonthDay(dateLabel: String, todayCal: java.util.Calendar): java.util.Calendar? {
        val parts = dateLabel.trim().split("/")
        if (parts.size != 2) return null
        val month = parts[0].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val day = parts[1].toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        val todayMonth = todayCal.get(java.util.Calendar.MONTH) + 1
        var year = todayCal.get(java.util.Calendar.YEAR)
        if (todayMonth == 12 && month <= 2) year += 1
        else if (todayMonth == 1 && month >= 11) year -= 1
        return (todayCal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
            set(java.util.Calendar.DAY_OF_MONTH, day)
        }
    }

    private fun isSameDay(a: java.util.Calendar, b: java.util.Calendar): Boolean {
        return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun dayOfWeekKeyToLocalized(dayOfWeek: Int, language: AppLanguage): String {
        val enKey = when (dayOfWeek) {
            java.util.Calendar.MONDAY -> "Mon"
            java.util.Calendar.TUESDAY -> "Tue"
            java.util.Calendar.WEDNESDAY -> "Wed"
            java.util.Calendar.THURSDAY -> "Thu"
            java.util.Calendar.FRIDAY -> "Fri"
            java.util.Calendar.SATURDAY -> "Sat"
            java.util.Calendar.SUNDAY -> "Sun"
            else -> "Mon"
        }
        return getLocalizedDayLabel(enKey, language)
    }
}
