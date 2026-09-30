package com.example.expressiveweather.ui.components

/**
 * ベントグリッド内の各天候タイルの種別定義
 * @property id 一意の識別子
 * @property title 日本語タイトル
 * @property isFullWidth 1行全幅（2カラム）か、半幅（1カラム）か
 */
enum class BentoCardType(
    val id: String,
    val title: String,
    val isFullWidth: Boolean = false
) {
    WIND("wind", "風", false),
    UV("uv", "紫外線", false),
    AQI("aqi", "空気質", false),
    HUMIDITY("humidity", "湿度", false),
    SUN_CYCLE("sun_cycle", "太陽", false),
    PRESSURE("pressure", "気圧", false),
    VISIBILITY("visibility", "視程", false),
    PRECIPITATION("precipitation", "降水情報", false),
    COMFORT("comfort", "体感温度", false);

    companion object {
        val DEFAULT_ORDER = listOf(
            WIND, UV,
            AQI, HUMIDITY,
            SUN_CYCLE, PRESSURE,
            VISIBILITY, PRECIPITATION,
            COMFORT
        )
    }

    fun getLocalizedTitle(strings: com.example.expressiveweather.data.AppStrings): String {
        return when (this) {
            WIND -> strings.wind
            UV -> strings.uvIndex
            AQI -> strings.airQuality
            HUMIDITY -> strings.humidity
            SUN_CYCLE -> strings.sun
            PRESSURE -> strings.pressure
            VISIBILITY -> strings.visibility
            PRECIPITATION -> strings.precipitation
            COMFORT -> strings.feelsLikeCard
        }
    }
}
