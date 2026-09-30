package com.example.expressiveweather.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun convert(tempC: Int): Int {
        return when (this) {
            CELSIUS -> tempC
            FAHRENHEIT -> ((tempC * 9.0 / 5.0) + 32).toInt()
        }
    }
}

/**
 * 通常天気のバックグラウンド自動更新頻度
 */
@Serializable
enum class WeatherUpdateInterval(
    val minutes: Int,
    val labelJa: String,
    val labelEn: String,
    val labelZhHans: String,
    val labelZhHant: String
) {
    MINUTES_15(15, "15分ごと", "Every 15 minutes", "每15分钟", "每15分鐘"),
    MINUTES_30(30, "30分ごと (おすすめ)", "Every 30 minutes (Recommended)", "每30分钟 (推荐)", "每30分鐘 (推薦)"),
    HOUR_1(60, "1時間ごと", "Every 1 hour", "每1小时", "每1小時"),
    HOURS_2(120, "2時間ごと", "Every 2 hours", "每2小时", "每2小時"),
    HOURS_3(180, "3時間ごと", "Every 3 hours", "每3小时", "每3小時"),
    HOURS_6(360, "6時間ごと", "Every 6 hours", "每6小时", "每6小時"),
    OFF(0, "オフ (自動更新なし)", "Off (Manual only)", "关闭 (仅手动刷新)", "關閉 (僅手動重新整理)");

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
    }

    companion object {
        fun fromMinutes(minutes: Int): WeatherUpdateInterval {
            return entries.find { it.minutes == minutes } ?: MINUTES_30
        }
    }
}

@Serializable
enum class WindSpeedUnit(val id: String) {
    FOLLOW_LANGUAGE("follow_language"),
    MS("ms"),
    KMH("kmh"),
    MPH("mph");

    fun getDisplayUnit(language: AppLanguage): String {
        return when (this) {
            FOLLOW_LANGUAGE -> if (language == AppLanguage.JAPANESE || language == AppLanguage.SIMPLIFIED_CHINESE || language == AppLanguage.TRADITIONAL_CHINESE) "m/s" else "km/h"
            MS -> "m/s"
            KMH -> "km/h"
            MPH -> "mph"
        }
    }

    fun convertFromKmH(speedKmH: Double, language: AppLanguage): Double {
        return when (this) {
            FOLLOW_LANGUAGE -> if (language == AppLanguage.JAPANESE || language == AppLanguage.SIMPLIFIED_CHINESE || language == AppLanguage.TRADITIONAL_CHINESE) speedKmH / 3.6 else speedKmH
            MS -> speedKmH / 3.6
            KMH -> speedKmH
            MPH -> speedKmH * 0.621371
        }
    }

    fun format(speedKmH: Double, language: AppLanguage): String {
        val converted = convertFromKmH(speedKmH, language)
        val unit = getDisplayUnit(language)
        return when (this) {
            FOLLOW_LANGUAGE -> if (language == AppLanguage.JAPANESE || language == AppLanguage.SIMPLIFIED_CHINESE || language == AppLanguage.TRADITIONAL_CHINESE) String.format(java.util.Locale.US, "%.1f %s", converted, unit) else String.format(java.util.Locale.US, "%.0f %s", converted, unit)
            MS -> String.format(java.util.Locale.US, "%.1f %s", converted, unit)
            KMH -> String.format(java.util.Locale.US, "%.0f %s", converted, unit)
            MPH -> String.format(java.util.Locale.US, "%.0f %s", converted, unit)
        }
    }

    fun formatCompact(speedKmH: Double, language: AppLanguage): String {
        val converted = convertFromKmH(speedKmH, language)
        val unit = getDisplayUnit(language)
        return when (this) {
            FOLLOW_LANGUAGE -> if (language == AppLanguage.JAPANESE || language == AppLanguage.SIMPLIFIED_CHINESE || language == AppLanguage.TRADITIONAL_CHINESE) String.format(java.util.Locale.US, "%.1f%s", converted, unit) else String.format(java.util.Locale.US, "%.0f%s", converted, unit)
            MS -> String.format(java.util.Locale.US, "%.1f%s", converted, unit)
            KMH -> String.format(java.util.Locale.US, "%.0f%s", converted, unit)
            MPH -> String.format(java.util.Locale.US, "%.0f%s", converted, unit)
        }
    }

    fun convertFromMs(speedMs: Double, language: AppLanguage): Double {
        return convertFromKmH(speedMs * 3.6, language)
    }

    fun formatFromMs(speedMs: Double, language: AppLanguage): String {
        return format(speedMs * 3.6, language)
    }

    fun formatCompactFromMs(speedMs: Double, language: AppLanguage): String {
        return formatCompact(speedMs * 3.6, language)
    }

    fun formatValueFromMs(speedMs: Double, language: AppLanguage): String {
        val converted = convertFromMs(speedMs, language)
        return when (this) {
            FOLLOW_LANGUAGE -> if (language == AppLanguage.JAPANESE || language == AppLanguage.SIMPLIFIED_CHINESE || language == AppLanguage.TRADITIONAL_CHINESE) String.format(java.util.Locale.US, "%.1f", converted) else String.format(java.util.Locale.US, "%.0f", converted)
            MS -> String.format(java.util.Locale.US, "%.1f", converted)
            KMH -> String.format(java.util.Locale.US, "%.0f", converted)
            MPH -> String.format(java.util.Locale.US, "%.0f", converted)
        }
    }

    fun formatValue(speedKmH: Double, language: AppLanguage): String {
        val converted = convertFromKmH(speedKmH, language)
        return when (this) {
            FOLLOW_LANGUAGE -> if (language == AppLanguage.JAPANESE || language == AppLanguage.SIMPLIFIED_CHINESE || language == AppLanguage.TRADITIONAL_CHINESE) String.format(java.util.Locale.US, "%.1f", converted) else String.format(java.util.Locale.US, "%.0f", converted)
            MS -> String.format(java.util.Locale.US, "%.1f", converted)
            KMH -> String.format(java.util.Locale.US, "%.0f", converted)
            MPH -> String.format(java.util.Locale.US, "%.0f", converted)
        }
    }
}

/**
 * 警戒情報・キキクル通知の重要度閾値設定
 * ユーザーが「どの重要度以上の警報が発表されたら通知するか」を細かく設定可能
 */
@Serializable
enum class AlertNotificationThreshold(val minLevel: Int, val id: String) {
    ADVISORY_AND_ABOVE(2, "advisory_and_above"), // 注意報以上 (警戒レベル2以上: 注意報・警報・特別警報すべて)
    WARNING_AND_ABOVE(3, "warning_and_above"),   // 警報以上 (警戒レベル3以上: 警報・特別警報のみ)
    EMERGENCY_ONLY(4, "emergency_only"),        // 特別警報・レベル4以上 (避難指示・土砂災害警戒・氾濫危険など極めて危険な場合のみ)
    OFF(99, "off");                              // 通知オフ

    fun displayName(language: AppLanguage): String = when (this) {
        ADVISORY_AND_ABOVE -> when (language) {
            AppLanguage.JAPANESE -> "注意報以上（レベル2以上）"
            AppLanguage.ENGLISH -> "Advisories & Above (Level 2+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "注意警报以上（2级以上）"
            AppLanguage.TRADITIONAL_CHINESE -> "注意報以上（2級以上）"
        }
        WARNING_AND_ABOVE -> when (language) {
            AppLanguage.JAPANESE -> "警報以上（レベル3以上）"
            AppLanguage.ENGLISH -> "Warnings & Above (Level 3+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "重大警报以上（3级以上）"
            AppLanguage.TRADITIONAL_CHINESE -> "警報以上（3級以上）"
        }
        EMERGENCY_ONLY -> when (language) {
            AppLanguage.JAPANESE -> "特別警報・危険のみ（レベル4以上）"
            AppLanguage.ENGLISH -> "Emergency / Critical Only (Level 4+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "仅特别重大预警（4级以上）"
            AppLanguage.TRADITIONAL_CHINESE -> "僅特別重大預警（4級以上）"
        }
        OFF -> when (language) {
            AppLanguage.JAPANESE -> "通知オフ"
            AppLanguage.ENGLISH -> "Off"
            AppLanguage.SIMPLIFIED_CHINESE -> "关闭通知"
            AppLanguage.TRADITIONAL_CHINESE -> "關閉通知"
        }
    }

    fun description(language: AppLanguage): String = when (this) {
        ADVISORY_AND_ABOVE -> when (language) {
            AppLanguage.JAPANESE -> "大雨・洪水等の注意報からすべての警戒情報を通知"
            AppLanguage.ENGLISH -> "Notifies all alerts starting from advisory level"
            AppLanguage.SIMPLIFIED_CHINESE -> "从一般注意警报开始通知所有气象预警"
            AppLanguage.TRADITIONAL_CHINESE -> "從一般注意報開始通知所有氣象警報"
        }
        WARNING_AND_ABOVE -> when (language) {
            AppLanguage.JAPANESE -> "大雨・洪水・暴風等の警報、高齢者等避難（レベル3）以上を通知"
            AppLanguage.ENGLISH -> "Notifies severe warnings & evacuation advisories (Level 3+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "通知大雨/暴风警报、疏散准备（3级）及以上"
            AppLanguage.TRADITIONAL_CHINESE -> "通知大雨/暴風警報、疏散準備（3級）及以上"
        }
        EMERGENCY_ONLY -> when (language) {
            AppLanguage.JAPANESE -> "避難指示（レベル4）や特別警報・氾濫発生（レベル5）のみ通知"
            AppLanguage.ENGLISH -> "Notifies emergency orders & special warnings only (Level 4+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "仅在发布紧急疏散令（4级）或特别警报（5级）时通知"
            AppLanguage.TRADITIONAL_CHINESE -> "僅在發布緊急疏散令（4級）或特別警報（5級）時通知"
        }
        OFF -> when (language) {
            AppLanguage.JAPANESE -> "警戒レベル・キキクル通知を行いません"
            AppLanguage.ENGLISH -> "No disaster alert notifications will be sent"
            AppLanguage.SIMPLIFIED_CHINESE -> "不发送任何灾害预警通知"
            AppLanguage.TRADITIONAL_CHINESE -> "不發送任何災害預警通知"
        }
    }

    companion object {
        val DEFAULT = ADVISORY_AND_ABOVE

        fun fromId(id: String?): AlertNotificationThreshold {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}

@Serializable
enum class WeatherCondition(
    val label: String,
    val englishLabel: String,
    val tagline: String,
    val isPrecipitating: Boolean = false
) {
    CLEAR(
        label = "快晴",
        englishLabel = "Clear Sky",
        tagline = "澄み渡る青空と心地よい陽光が広がっています"
    ),
    FEW_CLOUDS(
        label = "晴れ時々曇り",
        englishLabel = "Partly Cloudy",
        tagline = "柔らかな雲間から優しい光が差し込んでいます"
    ),
    CLOUDY(
        label = "曇り",
        englishLabel = "Overcast",
        tagline = "厚い雲が空を覆い、穏やかな一日となりそうです"
    ),
    RAIN(
        label = "雨",
        englishLabel = "Showers",
        tagline = "しっとりとした雨が街を潤しています。傘をお忘れなく",
        isPrecipitating = true
    ),
    HEAVY_RAIN(
        label = "強い雨",
        englishLabel = "Heavy Rain",
        tagline = "激しい降雨が続いています。足元にお気をつけください",
        isPrecipitating = true
    ),
    THUNDERSTORM(
        label = "雷雨",
        englishLabel = "Thunderstorm",
        tagline = "大気の状態が不安定です。急な落雷や突風に注意",
        isPrecipitating = true
    ),
    SNOW(
        label = "雪",
        englishLabel = "Snow Flurries",
        tagline = "白い雪が静かに舞い降りています。暖かくしてお出かけを",
        isPrecipitating = true
    ),
    FOG(
        label = "霧・靄",
        englishLabel = "Misty Fog",
        tagline = "幻想的な霧が漂っています。視界不良にご注意ください"
    )
}

@Serializable
data class City(
    val id: String,
    val name: String,
    val nameEn: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false
)

@Serializable
data class HourlyForecast(
    val timeLabel: String,
    val hour: Int,
    val tempC: Int,
    val condition: WeatherCondition,
    val precipProb: Int,
    val humidity: Int,
    val windSpeedKmH: Double,
    val dewPointC: Int = 0,
    val isDay: Boolean = true,
    val precipitationMm: Double = 0.0
)

@Serializable
data class DailyForecast(
    val dayLabel: String,
    val dateLabel: String,
    val condition: WeatherCondition,
    val minTempC: Int,
    val maxTempC: Int,
    val precipProb: Int,
    val uvMax: Double = 0.0,
    val precipitationSumMm: Double = 0.0,
    val windSpeedMaxKmH: Double = 0.0,
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val sunrise: String = "06:00",
    val sunset: String = "18:00",
    val daylightHours: String = "12h 00m",
    val humidityAvg: Int? = null,
    val humidityAvgSource: String? = null,
    val pressureHpa: Int = 1013
)

@Serializable
data class AirQuality(
    val aqi: Int,
    val levelLabel: String,
    val pm25: Double,
    val pm10: Double,
    val advice: String,
    val usAqi: Int = aqi,
    val europeanAqi: Int = aqi
)

@Serializable
data class SunSchedule(
    val sunrise: String,
    val sunset: String,
    val dayProgress: Float,
    val daylightHours: String
)

@Serializable
data class HourlyWindItem(
    val timeLabel: String,
    val hour: Int,
    val windSpeedKmH: Double,
    val windSpeedMs: Double = windSpeedKmH / 3.6,
    val windGustsKmH: Double,
    val windGustsMs: Double = windGustsKmH / 3.6,
    val windDegree: Float
)

@Serializable
data class DailyWindItem(
    val dayLabel: String,
    val dateLabel: String,
    val windSpeedMaxKmH: Double,
    val windSpeedMaxMs: Double = windSpeedMaxKmH / 3.6,
    val windGustsMaxKmH: Double,
    val windGustsMaxMs: Double = windGustsMaxKmH / 3.6,
    val dominantWindDegree: Float
)

@Serializable
enum class BeaufortScale(
    val level: Int,
    val minSpeedMs: Double,
    val maxSpeedMs: Double,
    val nameJa: String,
    val nameEn: String,
    val nameZhHans: String,
    val nameZhHant: String,
    val descJa: String,
    val descEn: String,
    val descZhHans: String,
    val descZhHant: String
) {
    CALM(0, 0.0, 0.2, "静穏", "Calm", "无风", "無風", "煙は直立して昇る。風の気配はほとんどない。", "Smoke rises vertically. Calm air.", "烟直上，地面平静无风。", "煙直上，地面平靜無風。"),
    LIGHT_AIR(1, 0.3, 1.5, "至軽風", "Light Air", "软风", "軟風", "煙がわずかになびくが、風向計は動かない。", "Smoke drift indicates wind direction, leaves still.", "烟能表示风向，风向标不转动。", "煙能表示風向，風向標不轉動。"),
    LIGHT_BREEZE(2, 1.6, 3.3, "軽風", "Light Breeze", "轻风", "輕風", "顔に風を感じる。木の葉がかすかに揺れる。", "Wind felt on face; leaves rustle; vane moved by wind.", "人面感觉有风，树叶微响，风向标转动。", "人面感覺有風，樹葉微響，風向標轉動。"),
    GENTLE_BREEZE(3, 3.4, 5.4, "軟風", "Gentle Breeze", "微风", "微風", "木の葉や小枝が絶えず揺れる。旗が広がる。", "Leaves and small twigs constantly in motion; flags extend.", "树叶及微枝摇动不息，旗帜展开。", "樹葉及微枝搖動不息，旗幟展開。"),
    MODERATE_BREEZE(4, 5.5, 7.9, "和風", "Moderate Breeze", "和风", "和風", "砂埃が立ち、紙片が舞う。小枝が動く。", "Dust and loose paper raised; small branches move.", "能吹起地面灰尘和纸张，树小枝摇动。", "能吹起地面灰塵和紙張，樹小枝搖動。"),
    FRESH_BREEZE(5, 8.0, 10.7, "疾風", "Fresh Breeze", "清风", "清風", "葉のある低木が揺れ始める。水面に波頭が立つ。", "Small trees in leaf begin to sway; crested wavelets form.", "有叶的小树摇摆，水面起小波。", "有葉的小樹搖擺，水面起小波。"),
    STRONG_BREEZE(6, 10.8, 13.8, "雄風", "Strong Breeze", "强风", "強風", "大枝が揺れ、電線が鳴る。傘がさしにくくなる。", "Large branches in motion; umbrellas used with difficulty.", "大树枝摇动，电线呼呼有声，撑伞困难。", "大樹枝搖動，電線呼呼有聲，撐傘困難。"),
    HIGH_WIND(7, 13.9, 17.1, "強風", "Near Gale", "疾风", "疾風", "樹木全体が揺れ、風に向かって歩きにくい。", "Whole trees in motion; inconvenience felt walking against wind.", "全树摇动，迎风步行感觉不便。", "全樹搖動，迎風步行感覺不便。"),
    GALE(8, 17.2, 20.7, "疾強風", "Gale", "大风", "大風", "小枝が折れる。風に向かって歩くのが極めて困難。", "Twigs break off trees; generally impedes progress.", "微枝折断，迎风行走极其困难。", "微枝折斷，迎風行走極其困難。"),
    STRONG_GALE(9, 20.8, 24.4, "大強風", "Strong Gale", "烈风", "烈風", "家屋に小被害（煙突や瓦が飛ぶ恐れあり）。", "Slight structural damage occurs; chimney pots removed.", "房屋受轻微损坏（烟囱、瓦片飞走）。", "房屋受輕微損壞（煙囪、瓦片飛走）。"),
    STORM(10, 24.5, 28.4, "暴風", "Storm", "狂风", "狂風", "樹木が根こそぎ倒れる。陸上では滅多にない強風。", "Trees uprooted; considerable structural damage occurs.", "陆上少见，树木连根拔起，房屋受损严重。", "陸上少見，樹木連根拔起，房屋受損嚴重。"),
    VIOLENT_STORM(11, 28.5, 32.6, "狂風", "Violent Storm", "暴风", "暴風", "広範囲にわたる甚大な破壊が発生する危険。", "Widespread damage occurs. Rarely experienced on land.", "陆上极罕见，造成大范围重大破坏。", "陸上極罕見，造成大範圍重大破壞。"),
    HURRICANE(12, 32.7, 999.0, "颶風", "Hurricane", "飓风", "颶風", "甚大な壊滅的被害。外出は極めて危険。", "Devastation occurs. Extremely dangerous.", "摧毁性破坏，外出极其危险。", "摧毀性破壞，外出極其危險。");

    fun getName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhHans
        AppLanguage.TRADITIONAL_CHINESE -> nameZhHant
        AppLanguage.ENGLISH -> nameEn
    }

    fun getDescription(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descJa
        AppLanguage.SIMPLIFIED_CHINESE -> descZhHans
        AppLanguage.TRADITIONAL_CHINESE -> descZhHant
        AppLanguage.ENGLISH -> descEn
    }

    companion object {
        fun fromSpeedMs(speedMs: Double): BeaufortScale {
            val speed = speedMs.coerceAtLeast(0.0)
            return entries.firstOrNull { speed >= it.minSpeedMs && speed <= it.maxSpeedMs } ?: HURRICANE
        }
    }
}

@Serializable
data class WindDetails(
    val windSpeedKmH: Double,
    val windSpeedMs: Double = windSpeedKmH / 3.6,
    val windGustsKmH: Double,
    val windGustsMs: Double = windGustsKmH / 3.6,
    val windDegree: Float,
    val windDirectionText: String,
    val beaufortScale: BeaufortScale = BeaufortScale.fromSpeedMs(windSpeedKmH / 3.6),
    val hourlyWind: List<HourlyWindItem> = emptyList(),
    val dailyWind: List<DailyWindItem> = emptyList(),
    val peakWindTime: String = "",
    val peakWindSpeedKmH: Double = windGustsKmH
)

@Serializable
data class WeatherData(
    val city: City,
    val currentTempC: Int,
    val feelsLikeC: Int,
    val minTempC: Int,
    val maxTempC: Int,
    val condition: WeatherCondition,
    val conditionSummary: String,
    val conditionName: String = condition.label,
    val humidity: Int,
    val uvIndex: Int,
    val uvLabel: String,
    val windSpeedKmH: Double,
    val windDirection: String,
    val windDegree: Float,
    val pressureHpa: Int,
    val visibilityKm: Double,
    val airQuality: AirQuality,
    val sunSchedule: SunSchedule,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>,
    val lastUpdatedTime: String,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
    val isDay: Boolean = true,
    val dewPointC: Int = 0,
    val windGustsKmH: Double = windSpeedKmH * 1.3,
    val precipitationMm: Double = 0.0,
    val precipitationSumMm: Double = 0.0,
    val precipitationProbabilityMax: Int = 0,
    val windDetails: WindDetails = WindDetails(
        windSpeedKmH = windSpeedKmH,
        windGustsKmH = windGustsKmH,
        windDegree = windDegree,
        windDirectionText = windDirection
    ),
    val dataSourceName: String = "",
    val jmaOverviewText: String? = null,
    val jmaWarnings: List<String> = emptyList(),
    val alertInfo: MunicipalityAlertInfo? = null,
    val precipitationNowcast: PrecipitationNowcast? = null,
    val globalAlerts: List<GlobalAlertItem> = emptyList()
)

@Serializable
enum class RainStatus {
    RAINING, // 現在降雨中（雨は何時まで降り続くか）
    RAIN_APPROACHING, // 雨雲が接近中（まもなく降り始める）
    RAIN_EXPECTED_TONIGHT, // 今夜は雨が降るでしょう
    RAIN_EXPECTED_LATER, // 明日以降に雨の予報
    DRY // 当面、雨の心配なし
}

@Serializable
data class MinutelyRainPoint(
    val timeLabel: String, // "20:30"
    val timestampMillis: Long,
    val precipMm: Double,
    val probability: Int = 0
)

@Serializable
data class PopSlot(
    val startMillis: Long,
    val pop: Int
)

@Serializable
data class PrecipitationNowcast(
    val status: RainStatus,
    val headline: String,
    val subText: String,
    val targetTimeLabel: String? = null, // "21:45"
    val targetTimestampMillis: Long? = null,
    val minutesRemaining: Int? = null, // 35
    val currentIntensityMm: Double = 0.0,
    val peakIntensityMm: Double = 0.0,
    val maxProbability: Int = 0,
    val minutelyPoints: List<MinutelyRainPoint> = emptyList(),
    val isLiveActive: Boolean = false,
    val isPeak: Boolean = false,
    val localizedHeadlines: Map<String, String> = emptyMap(),
    val localizedSubTexts: Map<String, String> = emptyMap(),
    val localizedTargetTimeLabels: Map<String, String> = emptyMap(),
    val isJapan: Boolean = false,
    val isUS: Boolean = false,
    val dataSourceName: String? = null
) {
    fun localizedHeadline(language: AppLanguage): String {
        val direct = localizedHeadlines[language.code]
        if (!direct.isNullOrBlank()) return direct
        if (language != AppLanguage.JAPANESE) {
            val fallback = PrecipitationNowcastTranslator.translateHeadline(headline, language)
            if (fallback != null) return fallback
        }
        return localizedHeadlines[AppLanguage.JAPANESE.code] ?: headline
    }

    fun localizedSubText(language: AppLanguage): String {
        val direct = localizedSubTexts[language.code]
        if (!direct.isNullOrBlank()) return direct
        if (language != AppLanguage.JAPANESE) {
            val fallback = PrecipitationNowcastTranslator.translateSubText(subText, language)
            if (fallback != null) return fallback
        }
        return localizedSubTexts[AppLanguage.JAPANESE.code] ?: subText
    }

    fun localizedTargetTimeLabel(language: AppLanguage): String? {
        val direct = localizedTargetTimeLabels[language.code]
        if (!direct.isNullOrBlank()) return direct
        if (language != AppLanguage.JAPANESE && targetTimeLabel != null) {
            val fallback = PrecipitationNowcastTranslator.translateTimeLabel(targetTimeLabel, language)
            if (fallback != null) return fallback
        }
        return localizedTargetTimeLabels[AppLanguage.JAPANESE.code] ?: targetTimeLabel
    }
}


/**
 * WeatherData の JSON シリアライズ / デシリアライズ用ヘルパー
 */
object WeatherJsonSerializer {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    fun serialize(data: WeatherData): String {
        return json.encodeToString(data)
    }

    fun deserialize(jsonString: String): WeatherData? {
        return try {
            json.decodeFromString<WeatherData>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}

