package io.github.nemynew.expressiveweather.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

@Serializable
enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun convert(tempC: Int): Int {
        return when (this) {
            CELSIUS -> tempC
            FAHRENHEIT -> ((tempC * 9.0 / 5.0) + 32.0).roundToInt()
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
    val labelKo: String,
    val labelZhHans: String,
    val labelZhHant: String,
    val labelEs: String,
    val labelFr: String,
    val labelPt: String,
    val labelRu: String
) {
    MINUTES_15(15, "15分ごと (おすすめ)", "Every 15 minutes (Recommended)", "15분마다 (권장)", "每15分钟 (推荐)", "每15分鐘 (推薦)", "Cada 15 minutos (Recomendado)", "Toutes les 15 minutes (Recommandé)", "A cada 15 minutos (Recomendado)", "Каждые 15 минут (Рекомендуется)"),
    MINUTES_30(30, "30分ごと", "Every 30 minutes", "30분마다", "每30分钟", "每30分鐘", "Cada 30 minutos", "Toutes les 30 minutes", "A cada 30 minutos", "Каждые 30 минут"),
    HOUR_1(60, "1時間ごと", "Every 1 hour", "1시간마다", "每1小时", "每1小時", "Cada hora", "Toutes les heures", "A cada 1 hora", "Каждый час"),
    HOURS_2(120, "2時間ごと", "Every 2 hours", "2시간마다", "每2小时", "每2小時", "Cada 2 horas", "Toutes les 2 heures", "A cada 2 horas", "Каждые 2 часа"),
    HOURS_3(180, "3時間ごと", "Every 3 hours", "3시간마다", "每3小时", "每3小時", "Cada 3 horas", "Toutes les 3 heures", "A cada 3 horas", "Каждые 3 часа"),
    HOURS_6(360, "6時間ごと", "Every 6 hours", "6시간마다", "每6小时", "每6小時", "Cada 6 horas", "Toutes les 6 heures", "A cada 6 horas", "Каждые 6 часов"),
    OFF(0, "オフ (自動更新なし)", "Off (Manual only)", "끄기 (수동 새로고침)", "关闭 (仅手动刷新)", "關閉 (僅手動重新整理)", "Desactivado (Solo manual)", "Désactivé (Manuel uniquement)", "Desativado (Apenas manual)", "Выкл. (Только вручную)");

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.KOREAN -> labelKo
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.SPANISH -> labelEs
        AppLanguage.FRENCH -> labelFr
        AppLanguage.PORTUGUESE -> labelPt
        AppLanguage.RUSSIAN -> labelRu
    }

    companion object {
        val DEFAULT: WeatherUpdateInterval = MINUTES_15

        fun fromMinutes(minutes: Int): WeatherUpdateInterval {
            return entries.find { it.minutes == minutes } ?: DEFAULT
        }
    }
}

/**
 * アプリ全体のハプティクス（触覚フィードバック）強度設定
 */
@Serializable
enum class HapticIntensity(
    val id: String,
    val scale: Float,
    val labelJa: String,
    val labelEn: String,
    val labelKo: String,
    val labelZhHans: String,
    val labelZhHant: String,
    val labelEs: String,
    val labelFr: String,
    val labelPt: String,
    val labelRu: String
) {
    OFF("off", 0f, "オフ", "Off", "끄기", "关闭", "關閉", "Desactivada", "Désactivé", "Desativada", "Выкл."),
    GENTLE("gentle", 0.55f, "控えめ", "Gentle", "약하게", "轻柔", "輕柔", "Suave", "Léger", "Suave", "Мягкий"),
    STANDARD("standard", 1.0f, "標準 (Expressive)", "Standard (Expressive)", "표준 (Expressive)", "标准 (Expressive)", "標準 (Expressive)", "Estándar (Expressive)", "Standard (Expressive)", "Padrão (Expressive)", "Стандартный (Expressive)"),
    EXPRESSIVE("expressive", 1.35f, "ダイナミック (強)", "Dynamic (Strong)", "강하게 (Dynamic)", "强劲 (Dynamic)", "強勁 (Dynamic)", "Dinámica (Fuerte)", "Dynamique (Fort)", "Dinâmica (Forte)", "Динамичный (Сильный)");

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.KOREAN -> labelKo
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.SPANISH -> labelEs
        AppLanguage.FRENCH -> labelFr
        AppLanguage.PORTUGUESE -> labelPt
        AppLanguage.RUSSIAN -> labelRu
    }

    companion object {
        fun fromId(id: String?): HapticIntensity {
            return entries.find { it.id == id } ?: STANDARD
        }
    }
}

@Serializable
enum class WindSpeedUnit(val id: String) {
    FOLLOW_LANGUAGE("follow_language"),
    MS("ms"),
    KMH("kmh"),
    MPH("mph");

    private fun usesMetersPerSecondByDefault(language: AppLanguage): Boolean =
        language == AppLanguage.JAPANESE ||
            language == AppLanguage.KOREAN ||
            language == AppLanguage.SIMPLIFIED_CHINESE ||
            language == AppLanguage.TRADITIONAL_CHINESE ||
            language == AppLanguage.RUSSIAN

    fun getDisplayUnit(language: AppLanguage): String {
        return when (this) {
            FOLLOW_LANGUAGE -> if (usesMetersPerSecondByDefault(language)) "m/s" else "km/h"
            MS -> "m/s"
            KMH -> "km/h"
            MPH -> "mph"
        }
    }

    fun convertFromKmH(speedKmH: Double, language: AppLanguage): Double {
        return when (this) {
            FOLLOW_LANGUAGE -> if (usesMetersPerSecondByDefault(language)) speedKmH / 3.6 else speedKmH
            MS -> speedKmH / 3.6
            KMH -> speedKmH
            MPH -> speedKmH * 0.621371
        }
    }

    fun format(speedKmH: Double, language: AppLanguage): String {
        val converted = convertFromKmH(speedKmH, language)
        val unit = getDisplayUnit(language)
        return when (this) {
            FOLLOW_LANGUAGE -> if (usesMetersPerSecondByDefault(language)) String.format(java.util.Locale.US, "%.1f %s", converted, unit) else String.format(java.util.Locale.US, "%.0f %s", converted, unit)
            MS -> String.format(java.util.Locale.US, "%.1f %s", converted, unit)
            KMH -> String.format(java.util.Locale.US, "%.0f %s", converted, unit)
            MPH -> String.format(java.util.Locale.US, "%.0f %s", converted, unit)
        }
    }

    fun formatCompact(speedKmH: Double, language: AppLanguage): String {
        val converted = convertFromKmH(speedKmH, language)
        val unit = getDisplayUnit(language)
        return when (this) {
            FOLLOW_LANGUAGE -> if (usesMetersPerSecondByDefault(language)) String.format(java.util.Locale.US, "%.1f%s", converted, unit) else String.format(java.util.Locale.US, "%.0f%s", converted, unit)
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
            FOLLOW_LANGUAGE -> if (usesMetersPerSecondByDefault(language)) String.format(java.util.Locale.US, "%.1f", converted) else String.format(java.util.Locale.US, "%.0f", converted)
            MS -> String.format(java.util.Locale.US, "%.1f", converted)
            KMH -> String.format(java.util.Locale.US, "%.0f", converted)
            MPH -> String.format(java.util.Locale.US, "%.0f", converted)
        }
    }

    fun formatValue(speedKmH: Double, language: AppLanguage): String {
        val converted = convertFromKmH(speedKmH, language)
        return when (this) {
            FOLLOW_LANGUAGE -> if (usesMetersPerSecondByDefault(language)) String.format(java.util.Locale.US, "%.1f", converted) else String.format(java.util.Locale.US, "%.0f", converted)
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
            AppLanguage.KOREAN -> "주의보 이상 (레벨 2 이상)"
            AppLanguage.ENGLISH -> "Advisories & Above (Level 2+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "注意警报以上（2级以上）"
            AppLanguage.TRADITIONAL_CHINESE -> "注意報以上（2級以上）"
            AppLanguage.SPANISH -> "Avisos y superiores (Nivel 2+)"
            AppLanguage.FRENCH -> "Vigilances et plus (Niveau 2+)"
            AppLanguage.PORTUGUESE -> "Avisos e superiores (Nível 2+)"
            AppLanguage.RUSSIAN -> "Предупреждения и выше (Уровень 2+)"
        }
        WARNING_AND_ABOVE -> when (language) {
            AppLanguage.JAPANESE -> "警報以上（レベル3以上）"
            AppLanguage.KOREAN -> "경보 이상 (레벨 3 이상)"
            AppLanguage.ENGLISH -> "Warnings & Above (Level 3+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "重大警报以上（3级以上）"
            AppLanguage.TRADITIONAL_CHINESE -> "警報以上（3級以上）"
            AppLanguage.SPANISH -> "Alertas y superiores (Nivel 3+)"
            AppLanguage.FRENCH -> "Alertes et plus (Niveau 3+)"
            AppLanguage.PORTUGUESE -> "Alertas e superiores (Nível 3+)"
            AppLanguage.RUSSIAN -> "Тревоги и выше (Уровень 3+)"
        }
        EMERGENCY_ONLY -> when (language) {
            AppLanguage.JAPANESE -> "特別警報・危険のみ（レベル4以上）"
            AppLanguage.KOREAN -> "특별경보·위험만 (레벨 4 이상)"
            AppLanguage.ENGLISH -> "Emergency / Critical Only (Level 4+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "仅特别重大预警（4级以上）"
            AppLanguage.TRADITIONAL_CHINESE -> "僅特別重大預警（4級以上）"
            AppLanguage.SPANISH -> "Solo emergencias / crítico (Nivel 4+)"
            AppLanguage.FRENCH -> "Urgence / critique uniquement (Niveau 4+)"
            AppLanguage.PORTUGUESE -> "Apenas emergência / crítico (Nível 4+)"
            AppLanguage.RUSSIAN -> "Только экстренные (Уровень 4+)"
        }
        OFF -> when (language) {
            AppLanguage.JAPANESE -> "通知オフ"
            AppLanguage.KOREAN -> "알림 끄기"
            AppLanguage.ENGLISH -> "Off"
            AppLanguage.SIMPLIFIED_CHINESE -> "关闭通知"
            AppLanguage.TRADITIONAL_CHINESE -> "關閉通知"
            AppLanguage.SPANISH -> "Desactivado"
            AppLanguage.FRENCH -> "Désactivé"
            AppLanguage.PORTUGUESE -> "Desativado"
            AppLanguage.RUSSIAN -> "Выкл"
        }
    }

    fun description(language: AppLanguage): String = when (this) {
        ADVISORY_AND_ABOVE -> when (language) {
            AppLanguage.JAPANESE -> "大雨・洪水等の注意報からすべての警戒情報を通知"
            AppLanguage.KOREAN -> "호우·홍수 등 주의보부터 모든 기상 경계 정보를 알림"
            AppLanguage.ENGLISH -> "Notifies all alerts starting from advisory level"
            AppLanguage.SIMPLIFIED_CHINESE -> "从一般注意警报开始通知所有气象预警"
            AppLanguage.TRADITIONAL_CHINESE -> "從一般注意報開始通知所有氣象警報"
            AppLanguage.SPANISH -> "Notifica todas las alertas desde el nivel de aviso"
            AppLanguage.FRENCH -> "Notifie toutes les alertes dès le niveau de vigilance"
            AppLanguage.PORTUGUESE -> "Notifica todos os alertas a partir do nível de aviso"
            AppLanguage.RUSSIAN -> "Уведомлять обо всех оповещениях начиная с уровня внимания"
        }
        WARNING_AND_ABOVE -> when (language) {
            AppLanguage.JAPANESE -> "大雨・洪水・暴風等の警報、高齢者等避難（レベル3）以上を通知"
            AppLanguage.KOREAN -> "호우·홍수·강풍 경보 및 고령자 등 대피(레벨 3) 이상 알림"
            AppLanguage.ENGLISH -> "Notifies severe warnings & evacuation advisories (Level 3+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "通知大雨/暴风警报、疏散准备（3级）及以上"
            AppLanguage.TRADITIONAL_CHINESE -> "通知大雨/暴風警報、疏散準備（3級）及以上"
            AppLanguage.SPANISH -> "Notifica alertas graves y avisos de evacuación (Nivel 3+)"
            AppLanguage.FRENCH -> "Notifie les alertes sévères et avis d'évacuation (Niveau 3+)"
            AppLanguage.PORTUGUESE -> "Notifica alertas severos e avisos de evacuação (Nível 3+)"
            AppLanguage.RUSSIAN -> "Уведомлять о серьёзных тревогах и рекомендациях эвакуации (Уровень 3+)"
        }
        EMERGENCY_ONLY -> when (language) {
            AppLanguage.JAPANESE -> "避難指示（レベル4）や特別警報・氾濫発生（レベル5）のみ通知"
            AppLanguage.KOREAN -> "대피 지시(레벨 4) 및 특별경보·범람 발생(레벨 5)만 알림"
            AppLanguage.ENGLISH -> "Notifies emergency orders & special warnings only (Level 4+)"
            AppLanguage.SIMPLIFIED_CHINESE -> "仅在发布紧急疏散令（4级）或特别警报（5级）时通知"
            AppLanguage.TRADITIONAL_CHINESE -> "僅在發布緊急疏散令（4級）或特別警報（5級）時通知"
            AppLanguage.SPANISH -> "Notifica solo órdenes de emergencia y alertas especiales (Nivel 4+)"
            AppLanguage.FRENCH -> "Notifie uniquement les ordres d'urgence et alertes spéciales (Niveau 4+)"
            AppLanguage.PORTUGUESE -> "Notifica apenas ordens de emergência e alertas especiais (Nível 4+)"
            AppLanguage.RUSSIAN -> "Уведомлять только о приказах об эвакуации и особых тревогах (Уровень 4+)"
        }
        OFF -> when (language) {
            AppLanguage.JAPANESE -> "警戒レベル・キキクル通知を行いません"
            AppLanguage.KOREAN -> "경계 레벨 및 재난 위험도 알림을 받지 않습니다"
            AppLanguage.ENGLISH -> "No disaster alert notifications will be sent"
            AppLanguage.SIMPLIFIED_CHINESE -> "不发送任何灾害预警通知"
            AppLanguage.TRADITIONAL_CHINESE -> "不發送任何災害預警通知"
            AppLanguage.SPANISH -> "No se enviarán notificaciones de alertas de desastre"
            AppLanguage.FRENCH -> "Aucune notification d'alerte catastrophe ne sera envoyée"
            AppLanguage.PORTUGUESE -> "Nenhuma notificação de alerta de desastre será enviada"
            AppLanguage.RUSSIAN -> "Уведомления о стихийных бедствиях не будут отправляться"
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
    val precipitationMm: Double = 0.0,
    val uvIndex: Double? = null
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
    val europeanAqi: Int = aqi,
    val isAvailable: Boolean = true,
    val ozone: Double? = null,
    val nitrogenDioxide: Double? = null
)

@Serializable
data class SunSchedule(
    val sunrise: String,
    val sunset: String,
    val dayProgress: Float,
    val daylightHours: String
)

/**
 * 気象学的風向（風が吹いてくる方位、0°=北風）を、
 * 風が吹いていく方向（一般的な天気アプリの矢印の向き、北風なら南向き180°）へ変換する。
 */
fun windFlowRotationDegrees(windFromDegree: Float): Float {
    if (!windFromDegree.isFinite()) return 180f
    return ((windFromDegree + 180f) % 360f + 360f) % 360f
}

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
    val nameKo: String,
    val nameZhHans: String,
    val nameZhHant: String,
    val nameEs: String,
    val nameFr: String,
    val namePt: String,
    val nameRu: String,
    val descJa: String,
    val descEn: String,
    val descKo: String,
    val descZhHans: String,
    val descZhHant: String,
    val descEs: String,
    val descFr: String,
    val descPt: String,
    val descRu: String
) {
    CALM(0, 0.0, 0.2, "静穏", "Calm", "고요", "无风", "無風", "Calma", "Calme", "Calmaria", "Штиль", "煙は直立して昇る。風の気配はほとんどない。", "Smoke rises vertically. Calm air.", "연기가 수직으로 올라갑니다. 바람이 거의 느껴지지 않습니다.", "烟直上，地面平静无风。", "煙直上，地面平靜無風。", "El humo asciende verticalmente. Aire en calma.", "La fumée s'élève verticalement. Air calme.", "A fumaça sobe verticalmente. Ar calmo.", "Дым поднимается вертикально. Воздух неподвижен."),
    LIGHT_AIR(1, 0.3, 1.5, "至軽風", "Light Air", "실바람", "软风", "軟風", "Ventolina", "Très légère brise", "Aragem", "Тихий", "煙がわずかになびくが、風向計は動かない。", "Smoke drift indicates wind direction, leaves still.", "연기가 약간 날려 풍향을 알 수 있으나 풍향계는 움직이지 않습니다.", "烟能表示风向，风向标不转动。", "煙能表示風向，風向標不轉動。", "El humo indica la dirección del viento, pero las hojas no se mueven.", "La fumée indique la direction du vent, les feuilles restent immobiles.", "A fumaça indica a direção do vento, mas as folhas ficam paradas.", "Направление ветра заметно по сносу дыма, листья неподвижны."),
    LIGHT_BREEZE(2, 1.6, 3.3, "軽風", "Light Breeze", "남실바람", "轻风", "輕風", "Brisa muy débil", "Légère brise", "Brisa leve", "Лёгкий", "顔に風を感じる。木の葉がかすかに揺れる。", "Wind felt on face; leaves rustle; vane moved by wind.", "얼굴에 바람이 느껴지고 나뭇잎이 살짝 흔들립니다.", "人面感觉有风，树叶微响，风向标转动。", "人面感覺有風，樹葉微響，風向標轉動。", "Se siente el viento en la cara; las hojas susurran y la veleta gira.", "On sent le vent sur le visage ; les feuilles bruissent et la girouette tourne.", "Sente-se o vento no rosto; as folhas farfalham e o cata-vento se move.", "Ветер ощущается лицом; шелестят листья, флюгер приходит в движение."),
    GENTLE_BREEZE(3, 3.4, 5.4, "軟風", "Gentle Breeze", "산들바람", "微风", "微風", "Brisa débil", "Petite brise", "Brisa fraca", "Слабый", "木の葉や小枝が絶えず揺れる。旗が広がる。", "Leaves and small twigs constantly in motion; flags extend.", "나뭇잎과 작은 가지가 계속 흔들리고 깃발이 펼쳐집니다.", "树叶及微枝摇动不息，旗帜展开。", "樹葉及微枝搖動不息，旗幟展開。", "Las hojas y ramitas se agitan constantemente; las banderas se despliegan.", "Les feuilles et les rameaux sont constamment agités ; les drapeaux se déploient.", "Folhas e pequenos galhos agitam-se continuamente; bandeiras se estendem.", "Листья и тонкие ветки постоянно колышутся; развеваются флаги."),
    MODERATE_BREEZE(4, 5.5, 7.9, "和風", "Moderate Breeze", "건들바람", "和风", "和風", "Brisa moderada", "Jolie brise", "Brisa moderada", "Умеренный", "砂埃が立ち、紙片が舞う。小枝が動く。", "Dust and loose paper raised; small branches move.", "먼지와 종이 조각이 날리고 작은 나뭇가지가 흔들립니다.", "能吹起地面灰尘和纸张，树小枝摇动。", "能吹起地面灰塵和紙張，樹小枝搖動。", "Se levanta polvo y papeles sueltos; se mueven las ramas pequeñas.", "La poussière et les papiers volent ; les petites branches bougent.", "Levanta poeira e papéis soltos; galhos pequenos se movem.", "Ветер поднимает пыль и бумажки; качаются тонкие ветви деревьев."),
    FRESH_BREEZE(5, 8.0, 10.7, "疾風", "Fresh Breeze", "흔들바람", "清风", "清風", "Brisa fresca", "Bonne brise", "Brisa forte", "Свежий", "葉のある低木が揺れ始める。水面に波頭が立つ。", "Small trees in leaf begin to sway; crested wavelets form.", "잎이 무성한 작은 나무가 흔들리고 수면에 잔물결이 일어납니다.", "有叶的小树摇摆，水面起小波。", "有葉的小樹搖擺，水面起小波。", "Los árboles pequeños con hojas comienzan a balancearse; se forman pequeñas olas.", "Les arbustes feuillus commencent à se balancer ; de petites vagues se forment.", "Árvores pequenas com folhas começam a balançar; formam-se pequenas ondas.", "Качаются тонкие стволы деревьев; на воде образуются волны с гребнями."),
    STRONG_BREEZE(6, 10.8, 13.8, "雄風", "Strong Breeze", "된바람", "强风", "強風", "Brisa fuerte", "Vent frais", "Vento fresco", "Сильный", "大枝が揺れ、電線が鳴る。傘がさしにくくなる。", "Large branches in motion; umbrellas used with difficulty.", "큰 나뭇가지가 흔들리고 전선이 울리며 우산을 쓰기 어렵습니다.", "大树枝摇动，电线呼呼有声，撑伞困难。", "大樹枝搖動，電線呼呼有聲，撐傘困難。", "Se agitan ramas grandes; es difícil usar el paraguas.", "Les grosses branches s'agitent ; l'usage d'un parapluie devient difficile.", "Galhos grandes se agitam; é difícil usar guarda-chuva.", "Качаются толстые ветви деревьев; трудно пользоваться зонтом."),
    HIGH_WIND(7, 13.9, 17.1, "強風", "Near Gale", "센바람", "疾风", "疾風", "Viento fuerte", "Grand frais", "Vento forte", "Крепкий", "樹木全体が揺れ、風に向かって歩きにくい。", "Whole trees in motion; inconvenience felt walking against wind.", "나무 전체가 흔들리고 바람을 맞고 걷기 불편합니다.", "全树摇动，迎风步行感觉不便。", "全樹搖動，迎風步行感覺不便。", "Todos los árboles se mueven; cuesta caminar contra el viento.", "Les arbres entiers s'agitent ; il devient pénible de marcher contre le vent.", "Árvores inteiras balançam; é difícil caminhar contra o vento.", "Качаются стволы деревьев; идти против ветра затруднительно."),
    GALE(8, 17.2, 20.7, "疾強風", "Gale", "큰바람", "大风", "大風", "Temporal", "Coup de vent", "Ventania", "Очень крепкий", "小枝が折れる。風に向かって歩くのが極めて困難。", "Twigs break off trees; generally impedes progress.", "작은 나뭇가지가 꺾이고 바람을 거슬러 걷기 매우 어렵습니다.", "微枝折断，迎风行走极其困难。", "微枝折斷，迎風行走極其困難。", "Se rompen ramitas de los árboles; avanzar contra el viento es muy difícil.", "Les rameaux se cassent ; la marche contre le vent est très difficile.", "Galhos pequenos se quebram; caminhar contra o vento é muito difícil.", "Ветер ломает тонкие ветви; идти против ветра очень трудно."),
    STRONG_GALE(9, 20.8, 24.4, "大強風", "Strong Gale", "큰센바람", "烈风", "烈風", "Temporal fuerte", "Fort coup de vent", "Ventania forte", "Шторм", "家屋に小被害（煙突や瓦が飛ぶ恐れあり）。", "Slight structural damage occurs; chimney pots removed.", "건물에 경미한 피해(기와나 간판 낙하 등)가 발생할 수 있습니다.", "房屋受轻微损坏（烟囱、瓦片飞走）。", "房屋受輕微損壞（煙囪、瓦片飛走）。", "Daños estructurales leves; pueden desprenderse tejas o chimeneas.", "Légers dommages aux bâtiments ; des tuiles ou cheminées peuvent s'arracher.", "Pequenos danos estruturais; telhas e chaminés podem ser arrancadas.", "Небольшие повреждения построек; может срывать черепицу и трубы."),
    STORM(10, 24.5, 28.4, "暴風", "Storm", "노대바람", "狂风", "狂風", "Temporal muy fuerte", "Tempête", "Tempestade", "Сильный шторм", "樹木が根こそぎ倒れる。陸上では滅多にない強風。", "Trees uprooted; considerable structural damage occurs.", "나무가 뿌리째 뽑히고 건물에 상당한 피해가 발생합니다.", "陆上少见，树木连根拔起，房屋受损严重。", "陸上少見，樹木連根拔起，房屋受損嚴重。", "Árboles arrancados de raíz; daños estructurales considerables.", "Arbres déracinés ; dommages importants aux bâtiments.", "Árvores arrancadas pela raiz; danos estruturais consideráveis.", "Деревья вырываются с корнем; значительные разрушения строений."),
    VIOLENT_STORM(11, 28.5, 32.6, "狂風", "Violent Storm", "왕바람", "暴风", "暴風", "Borrasca violenta", "Violente tempête", "Tempestade violenta", "Жестокий шторм", "広範囲にわたる甚大な破壊が発生する危険。", "Widespread damage occurs. Rarely experienced on land.", "광범위한 지역에 심각한 피해가 발생할 위험이 있습니다.", "陆上极罕见，造成大范围重大破坏。", "陸上極罕見，造成大範圍重大破壞。", "Daños generalizados. Muy poco frecuente en tierra firme.", "Dégâts étendus et graves. Très rare à l'intérieur des terres.", "Danos generalizados. Muito raro em terra firme.", "Большие разрушения на обширной территории. На суше бывает редко."),
    HURRICANE(12, 32.7, 999.0, "颶風", "Hurricane", "싹쓸바람", "飓风", "颶風", "Huracán", "Ouragan", "Furacão", "Ураган", "甚大な壊滅的被害。外出は極めて危険。", "Devastation occurs. Extremely dangerous.", "막대한 파괴적 피해가 우려됩니다. 외출은 매우 위험합니다.", "摧毁性破坏，外出极其危险。", "摧毀性破壞，外出極其危險。", "Daños devastadores. Extremadamente peligroso salir al exterior.", "Dégâts dévastateurs. Sortir est extrêmement dangereux.", "Danos devastadores. Extremamente perigoso sair ao ar livre.", "Опустошительные разрушения. Выходить на улицу крайне опасно.");

    fun getName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.KOREAN -> nameKo
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhHans
        AppLanguage.TRADITIONAL_CHINESE -> nameZhHant
        AppLanguage.ENGLISH -> nameEn
        AppLanguage.SPANISH -> nameEs
        AppLanguage.FRENCH -> nameFr
        AppLanguage.PORTUGUESE -> namePt
        AppLanguage.RUSSIAN -> nameRu
    }

    fun getDescription(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descJa
        AppLanguage.KOREAN -> descKo
        AppLanguage.SIMPLIFIED_CHINESE -> descZhHans
        AppLanguage.TRADITIONAL_CHINESE -> descZhHant
        AppLanguage.ENGLISH -> descEn
        AppLanguage.SPANISH -> descEs
        AppLanguage.FRENCH -> descFr
        AppLanguage.PORTUGUESE -> descPt
        AppLanguage.RUSSIAN -> descRu
    }

    companion object {
        fun fromSpeedMs(speedMs: Double): BeaufortScale {
            val speed = if (speedMs.isNaN() || speedMs.isInfinite()) 0.0 else speedMs.coerceAtLeast(0.0)
            for (i in 0 until entries.size - 1) {
                if (speed < entries[i + 1].minSpeedMs) {
                    return entries[i]
                }
            }
            return HURRICANE
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
    val globalAlerts: List<GlobalAlertItem> = emptyList(),
    val utcOffsetSeconds: Int? = null
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
    val isEurope: Boolean = false,
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
            return PrecipitationNowcastTranslator.translateTimeLabel(targetTimeLabel, language)
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

    private fun Double.finiteOr(fallback: Double = 0.0): Double = if (isFinite()) this else fallback
    private fun Float.finiteOr(fallback: Float = 0f): Float = if (isFinite()) this else fallback

    private fun sanitize(data: WeatherData): WeatherData = data.copy(
        city = data.city.copy(
            latitude = data.city.latitude.finiteOr(0.0),
            longitude = data.city.longitude.finiteOr(0.0)
        ),
        windSpeedKmH = data.windSpeedKmH.finiteOr(0.0),
        windDegree = data.windDegree.finiteOr(0f),
        visibilityKm = data.visibilityKm.finiteOr(10.0),
        windGustsKmH = data.windGustsKmH.finiteOr(0.0),
        precipitationMm = data.precipitationMm.finiteOr(0.0),
        precipitationSumMm = data.precipitationSumMm.finiteOr(0.0),
        airQuality = data.airQuality.copy(
            pm25 = data.airQuality.pm25.finiteOr(0.0),
            pm10 = data.airQuality.pm10.finiteOr(0.0),
            ozone = data.airQuality.ozone?.takeIf { it.isFinite() },
            nitrogenDioxide = data.airQuality.nitrogenDioxide?.takeIf { it.isFinite() }
        ),
        sunSchedule = data.sunSchedule.copy(
            dayProgress = data.sunSchedule.dayProgress.finiteOr(0.5f).coerceIn(0f, 1f)
        ),
        hourlyForecast = data.hourlyForecast.map {
            it.copy(
                windSpeedKmH = it.windSpeedKmH.finiteOr(0.0),
                precipitationMm = it.precipitationMm.finiteOr(0.0),
                uvIndex = it.uvIndex?.takeIf { uv -> uv.isFinite() }?.coerceAtLeast(0.0)
            )
        },
        dailyForecast = data.dailyForecast.map { d ->
            d.copy(
                uvMax = d.uvMax.finiteOr(0.0).coerceAtLeast(0.0),
                precipitationSumMm = d.precipitationSumMm.finiteOr(0.0),
                windSpeedMaxKmH = d.windSpeedMaxKmH.finiteOr(0.0),
                hourlyForecast = d.hourlyForecast.map {
                    it.copy(
                        windSpeedKmH = it.windSpeedKmH.finiteOr(0.0),
                        precipitationMm = it.precipitationMm.finiteOr(0.0),
                        uvIndex = it.uvIndex?.takeIf { uv -> uv.isFinite() }?.coerceAtLeast(0.0)
                    )
                }
            )
        },
        windDetails = data.windDetails.copy(
            windSpeedKmH = data.windDetails.windSpeedKmH.finiteOr(0.0),
            windSpeedMs = data.windDetails.windSpeedMs.finiteOr(0.0),
            windGustsKmH = data.windDetails.windGustsKmH.finiteOr(0.0),
            windGustsMs = data.windDetails.windGustsMs.finiteOr(0.0),
            windDegree = data.windDetails.windDegree.finiteOr(0f),
            peakWindSpeedKmH = data.windDetails.peakWindSpeedKmH.finiteOr(0.0),
            hourlyWind = data.windDetails.hourlyWind.map {
                it.copy(
                    windSpeedKmH = it.windSpeedKmH.finiteOr(0.0),
                    windSpeedMs = it.windSpeedMs.finiteOr(0.0),
                    windGustsKmH = it.windGustsKmH.finiteOr(0.0),
                    windGustsMs = it.windGustsMs.finiteOr(0.0),
                    windDegree = it.windDegree.finiteOr(0f)
                )
            },
            dailyWind = data.windDetails.dailyWind.map {
                it.copy(
                    windSpeedMaxKmH = it.windSpeedMaxKmH.finiteOr(0.0),
                    windSpeedMaxMs = it.windSpeedMaxMs.finiteOr(0.0),
                    windGustsMaxKmH = it.windGustsMaxKmH.finiteOr(0.0),
                    windGustsMaxMs = it.windGustsMaxMs.finiteOr(0.0),
                    dominantWindDegree = it.dominantWindDegree.finiteOr(0f)
                )
            }
        ),
        precipitationNowcast = data.precipitationNowcast?.let { p ->
            p.copy(
                currentIntensityMm = p.currentIntensityMm.finiteOr(0.0),
                peakIntensityMm = p.peakIntensityMm.finiteOr(0.0),
                minutelyPoints = p.minutelyPoints.map {
                    it.copy(precipMm = it.precipMm.finiteOr(0.0))
                }
            )
        }
    )

    fun serialize(data: WeatherData): String {
        return json.encodeToString(sanitize(data))
    }

    fun deserialize(jsonString: String): WeatherData? {
        return try {
            json.decodeFromString<WeatherData>(jsonString)
        } catch (e: Exception) {
            android.util.Log.w("WeatherJsonSerializer", "Failed to deserialize cached WeatherData: ${e.message}")
            null
        }
    }
}

