package io.github.nemynew.expressiveweather.ui.widget

import android.content.Context
import android.content.SharedPreferences
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherJsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * ウィジェットの外観スタイル
 */
enum class WidgetStyle(val id: String) {
    /** 天候・昼夜連動の Expressive グラデーション */
    WEATHER_EXPRESSIVE("weather_expressive"),
    /** Material You（Android 12+ 壁紙ダイナミックカラー / 端末ライト・ダークモード連動） */
    MATERIAL_YOU("material_you"),
    /** Material You ベースの半透明デザイン（透明度調整可） */
    FROSTED_GLASS("frosted_glass");

    fun getDisplayName(language: AppLanguage): String = when (this) {
        WEATHER_EXPRESSIVE -> when (language) {
            AppLanguage.JAPANESE -> "天候連動グラデーション"
            AppLanguage.ENGLISH -> "Weather Adaptive"
            AppLanguage.SIMPLIFIED_CHINESE -> "天气动态渐变"
            AppLanguage.TRADITIONAL_CHINESE -> "天氣動態漸層"
        }
        MATERIAL_YOU -> when (language) {
            AppLanguage.JAPANESE -> "Material You (壁紙連動)"
            AppLanguage.ENGLISH -> "Material You (Dynamic)"
            AppLanguage.SIMPLIFIED_CHINESE -> "Material You（壁纸取色）"
            AppLanguage.TRADITIONAL_CHINESE -> "Material You（桌布取色）"
        }
        FROSTED_GLASS -> when (language) {
            AppLanguage.JAPANESE -> "Material You 半透明"
            AppLanguage.ENGLISH -> "Translucent Material You"
            AppLanguage.SIMPLIFIED_CHINESE -> "Material You 半透明"
            AppLanguage.TRADITIONAL_CHINESE -> "Material You 半透明"
        }
    }

    fun getDescription(language: AppLanguage): String = when (this) {
        WEATHER_EXPRESSIVE -> when (language) {
            AppLanguage.JAPANESE -> "空模様と昼夜に調和する鮮やかなグラデーション背景"
            AppLanguage.ENGLISH -> "Expressive sky gradients matching real-time weather & daylight"
            AppLanguage.SIMPLIFIED_CHINESE -> "随实时天气与昼夜变化的绚丽渐变背景"
            AppLanguage.TRADITIONAL_CHINESE -> "隨即時天氣與晝夜變化的絢麗漸層背景"
        }
        MATERIAL_YOU -> when (language) {
            AppLanguage.JAPANESE -> "ホーム画面の壁紙カラーとダーク/ライト設定に自動調和"
            AppLanguage.ENGLISH -> "Adapts automatically to wallpaper colors and system theme"
            AppLanguage.SIMPLIFIED_CHINESE -> "自动匹配主屏幕壁纸色调与深浅色模式"
            AppLanguage.TRADITIONAL_CHINESE -> "自動匹配主畫面桌布色調與深淺色模式"
        }
        FROSTED_GLASS -> when (language) {
            AppLanguage.JAPANESE -> "端末の Material You カラーをベースにした半透明デザイン（透明度調整可）"
            AppLanguage.ENGLISH -> "Translucent card tinted by your Material You palette (adjustable opacity)"
            AppLanguage.SIMPLIFIED_CHINESE -> "基于设备 Material You 配色的半透明设计（可调不透明度）"
            AppLanguage.TRADITIONAL_CHINESE -> "基於裝置 Material You 配色的半透明設計（可調不透明度）"
        }
    }

    companion object {
        fun fromId(id: String?): WidgetStyle {
            return entries.firstOrNull { it.id == id } ?: WEATHER_EXPRESSIVE
        }
    }
}

/**
 * ウィジェットのレスポンシブサイズ階層
 */
enum class WidgetSizeTier {
    COMPACT_WIDE, // 2x1 等のコンパクト横長
    WIDE_HOURLY,  // 4x1 等の横長＋時間別予報ボックス
    COMPACT,      // 2x2 コンパクトタイル
    MEDIUM,       // 4x2 時間別予報＋ナウキャスト
    LARGE;        // 4x3〜4x5 時間別＋週間予報

    companion object {
        /**
         * OSから通知されるウィジェット幅・高さ (dp) に応じて最適レイアウトを決定。
         */
        fun fromSizeDp(widthDp: Float, heightDp: Float, defaultTier: WidgetSizeTier): WidgetSizeTier {
            if (widthDp <= 0f || heightDp <= 0f) return defaultTier
            return when {
                heightDp >= 210f -> LARGE
                widthDp >= 210f && (heightDp < 124f || widthDp >= heightDp * 2.85f) -> WIDE_HOURLY
                widthDp >= 210f -> MEDIUM
                heightDp < 100f -> COMPACT_WIDE
                else -> COMPACT
            }
        }
    }
}

/**
 * Material Shape ウィジェット用の公式 MaterialShapes 輪郭形状
 */
enum class MaterialWidgetShape(val id: String) {
    /** MaterialShapes.Pill（既定） */
    PILL("pill"),
    /** MaterialShapes.Oval */
    OVAL("oval"),
    /** 12花弁スカロップ・クッキー (MaterialShapes.Cookie12Sided) */
    COOKIE_12("cookie_12"),
    /** 四つ葉クローバー (MaterialShapes.Clover4Leaf) */
    CLOVER_4("clover_4"),
    /** 波型サニー (MaterialShapes.Sunny) */
    SUNNY("sunny"),
    /** ふわふわクラウド (MaterialShapes.Puffy) */
    PUFFY("puffy");

    fun getDisplayName(language: AppLanguage): String = when (this) {
        PILL -> when (language) {
            AppLanguage.JAPANESE -> "Pill (既定)"
            AppLanguage.ENGLISH -> "Pill (Default)"
            AppLanguage.SIMPLIFIED_CHINESE -> "Pill（默认）"
            AppLanguage.TRADITIONAL_CHINESE -> "Pill（預設）"
        }
        OVAL -> when (language) {
            AppLanguage.JAPANESE -> "Oval"
            AppLanguage.ENGLISH -> "Oval"
            AppLanguage.SIMPLIFIED_CHINESE -> "Oval"
            AppLanguage.TRADITIONAL_CHINESE -> "Oval"
        }
        COOKIE_12 -> when (language) {
            AppLanguage.JAPANESE -> "Cookie"
            AppLanguage.ENGLISH -> "Cookie"
            AppLanguage.SIMPLIFIED_CHINESE -> "Cookie"
            AppLanguage.TRADITIONAL_CHINESE -> "Cookie"
        }
        CLOVER_4 -> when (language) {
            AppLanguage.JAPANESE -> "Clover"
            AppLanguage.ENGLISH -> "Clover"
            AppLanguage.SIMPLIFIED_CHINESE -> "Clover"
            AppLanguage.TRADITIONAL_CHINESE -> "Clover"
        }
        SUNNY -> when (language) {
            AppLanguage.JAPANESE -> "Sunny"
            AppLanguage.ENGLISH -> "Sunny"
            AppLanguage.SIMPLIFIED_CHINESE -> "Sunny"
            AppLanguage.TRADITIONAL_CHINESE -> "Sunny"
        }
        PUFFY -> when (language) {
            AppLanguage.JAPANESE -> "Puffy"
            AppLanguage.ENGLISH -> "Puffy"
            AppLanguage.SIMPLIFIED_CHINESE -> "Puffy"
            AppLanguage.TRADITIONAL_CHINESE -> "Puffy"
        }
    }
}

/**
 * ウィジェット ID (`appWidgetId`) ごとの個別設定
 */
@Serializable
data class WidgetConfig(
    val appWidgetId: Int,
    val useCurrentLocation: Boolean = true,
    val pinnedCity: City? = null,
    val style: WidgetStyle = WidgetStyle.WEATHER_EXPRESSIVE,
    val showAlertsAndNowcast: Boolean = false,
    val showRefreshButton: Boolean = false,
    val shapeType: MaterialWidgetShape = MaterialWidgetShape.PILL,
    val showCityOnShape: Boolean = false,
    val glassOpacity: Float = 0.72f
)

/**
 * ウィジェット設定を SharedPreferences に永続化するストア
 */
object WidgetPreferences {
    private const val PREFS_NAME = "expressive_weather_widgets"
    private const val KEY_PREFIX = "widget_config_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(
        context: Context,
        appWidgetId: Int,
        defaultStyle: WidgetStyle = WidgetStyle.WEATHER_EXPRESSIVE
    ): WidgetConfig {
        return getConfigFromPrefs(getPrefs(context), appWidgetId, defaultStyle)
    }

    internal fun getConfigFromPrefs(
        prefs: SharedPreferences,
        appWidgetId: Int,
        defaultStyle: WidgetStyle = WidgetStyle.WEATHER_EXPRESSIVE
    ): WidgetConfig {
        val raw = prefs.getString("$KEY_PREFIX$appWidgetId", null)
            ?: return WidgetConfig(appWidgetId = appWidgetId, style = defaultStyle)
        return try {
            WeatherJsonSerializer.json.decodeFromString<WidgetConfig>(raw).copy(appWidgetId = appWidgetId)
        } catch (_: Exception) {
            WidgetConfig(appWidgetId = appWidgetId, style = defaultStyle)
        }
    }

    fun saveConfig(context: Context, config: WidgetConfig) {
        saveConfigToPrefs(getPrefs(context), config)
    }

    internal fun saveConfigToPrefs(prefs: SharedPreferences, config: WidgetConfig) {
        try {
            val json = WeatherJsonSerializer.json.encodeToString(config)
            prefs.edit().putString("$KEY_PREFIX${config.appWidgetId}", json).apply()
        } catch (_: Exception) {}
    }

    fun deleteConfig(context: Context, appWidgetId: Int) {
        getPrefs(context).edit().remove("$KEY_PREFIX$appWidgetId").apply()
    }

    fun deleteConfigs(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val editor = getPrefs(context).edit()
        appWidgetIds.forEach { editor.remove("$KEY_PREFIX$it") }
        editor.apply()
    }
}
