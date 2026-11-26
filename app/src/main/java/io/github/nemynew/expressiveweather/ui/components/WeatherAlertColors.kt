package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.nemynew.expressiveweather.data.AlertLevel

/**
 * キキクル警戒レベルカード用カラー定義
 *
 * @param badgeBg 12SidedShapeバッジの背景色
 * @param badgeText バッジ内テキスト色
 * @param cardBg カードコンテナ背景色
 * @param cardText カード内タイトル・説明文のテキスト色
 */
@Immutable
data class KikikuruCardColors(
    val badgeBg: Color,
    val badgeText: Color,
    val cardBg: Color,
    val cardText: Color,
    val buttonBg: Color = cardText,
    val buttonText: Color = badgeBg
)

/**
 * 警報・注意報カード用カラー定義
 *
 * @param cardBg カードコンテナ背景色
 * @param contentColor カード内アイコン・タイトル・説明文のテキスト色
 */
@Immutable
data class WeatherWarningCardColors(
    val cardBg: Color,
    val contentColor: Color
)

/**
 * 警報・注意報カードの表示種別
 *
 * - [SPECIAL_WARNING]: 特別警戒（赤色: 背景 #FFB4AB、文字 #690005）
 * - [WARNING]: 警戒（橙色: 背景 #FFDBC9、文字 #341100）
 * - [ADVISORY]: 注意（黄色: 背景 #FFCA00、文字 #624E00）
 */
enum class WeatherWarningType {
    /** 特別警戒 (赤色: 背景 #FFB4AB / 文字 #690005) */
    SPECIAL_WARNING,
    /** 警戒 (橙色: 背景 #FFDBC9 / 文字 #341100) */
    WARNING,
    /** 注意 (黄色: 背景 #FFCA00 / 文字 #624E00) */
    ADVISORY,

    // 後方互換用エイリアス
    @Deprecated("Use SPECIAL_WARNING instead", ReplaceWith("SPECIAL_WARNING"))
    FOREIGN_WARNING,
    @Deprecated("Use WARNING instead", ReplaceWith("WARNING"))
    DOMESTIC_WARNING
}

/**
 * 気象警報・キキクル警戒レベルカード デザインシステム カラーパレット
 */
object WeatherAlertColors {

    // ==========================================
    // キキクル警戒レベル用カラー
    // ==========================================

    /**
     * 警戒レベル5 災害切迫
     * - バッジ背景色: #0C000C
     * - バッジ内文字色: #FFFFFF
     * - カード背景色: #616161
     * - カード内文字色: #FFFFFF
     */
    val KikikuruLevel5 = KikikuruCardColors(
        badgeBg = Color(0xFF0C000C),
        badgeText = Color(0xFFFFFFFF),
        cardBg = Color(0xFF616161),
        cardText = Color(0xFFFFFFFF),
        buttonBg = Color(0xFF0C000C),
        buttonText = Color(0xFFFFFFFF)
    )

    /**
     * 警戒レベル4 危険
     * - バッジ背景色: #AA00AA
     * - バッジ内文字色: #FFFFFF
     * - カード背景色: #F7D8FF
     * - カード内文字色: #36003F
     */
    val KikikuruLevel4 = KikikuruCardColors(
        badgeBg = Color(0xFFAA00AA),
        badgeText = Color(0xFFFFFFFF),
        cardBg = Color(0xFFF7D8FF),
        cardText = Color(0xFF36003F),
        buttonBg = Color(0xFF36003F),
        buttonText = Color(0xFFF7D8FF)
    )

    /**
     * 警戒レベル3 警戒
     * - バッジ背景色: #FF2800
     * - バッジ内文字色: #FFFFFF
     * - カード背景色: #FFB4AB
     * - カード内文字色: #690005
     */
    val KikikuruLevel3 = KikikuruCardColors(
        badgeBg = Color(0xFFFF2800),
        badgeText = Color(0xFFFFFFFF),
        cardBg = Color(0xFFFFB4AB),
        cardText = Color(0xFF690005),
        buttonBg = Color(0xFF690005),
        buttonText = Color(0xFFFFB4AB)
    )

    /**
     * 警戒レベル2 注意
     * - バッジ背景色: #FAF500
     * - バッジ内文字色: #624E00
     * - カード背景色: #FFCA00
     * - カード内文字色: #624E00
     * - ボタン背景色: #624E00
     * - ボタン内文字色: #FAF500
     */
    val KikikuruLevel2 = KikikuruCardColors(
        badgeBg = Color(0xFFFAF500),
        badgeText = Color(0xFF624E00),
        cardBg = Color(0xFFFFCA00),
        cardText = Color(0xFF624E00),
        buttonBg = Color(0xFF624E00),
        buttonText = Color(0xFFFAF500)
    )

    /**
     * 警戒レベル1 早期注意
     */
    val KikikuruLevel1 = KikikuruCardColors(
        badgeBg = Color(0xFFB0BEC5),
        badgeText = Color(0xFF263238),
        cardBg = Color(0xFFECEFF1),
        cardText = Color(0xFF37474F),
        buttonBg = Color(0xFF37474F),
        buttonText = Color(0xFFECEFF1)
    )

    /**
     * 警戒レベル0 平常時
     */
    val KikikuruLevel0 = KikikuruCardColors(
        badgeBg = Color(0xFF43A047),
        badgeText = Color(0xFFFFFFFF),
        cardBg = Color(0xFFE8F5E9),
        cardText = Color(0xFF1B5E20),
        buttonBg = Color(0xFF1B5E20),
        buttonText = Color(0xFFE8F5E9)
    )

    // ==========================================
    // 警報・注意報カード用カラー（黄色：注意 / 橙色：警戒 / 赤色：特別警戒）
    // ==========================================

    /**
     * 特別警戒 (赤色)
     * - 背景色: #FFB4AB
     * - 文字色: #690005
     */
    val SpecialWarning = WeatherWarningCardColors(
        cardBg = Color(0xFFFFB4AB),
        contentColor = Color(0xFF690005)
    )

    /**
     * 警戒 (橙色)
     * - 背景色: #FFDBC9
     * - 文字色: #341100
     */
    val Warning = WeatherWarningCardColors(
        cardBg = Color(0xFFFFDBC9),
        contentColor = Color(0xFF341100)
    )

    /**
     * 注意 (黄色)
     * - 背景色: #FFCA00
     * - 文字色: #624E00
     */
    val Advisory = WeatherWarningCardColors(
        cardBg = Color(0xFFFFCA00),
        contentColor = Color(0xFF624E00)
    )

    // 後方互換用
    @Deprecated("Use SpecialWarning instead")
    val ForeignWarning = SpecialWarning

    @Deprecated("Use Warning instead")
    val DomesticWarning = Warning

    /**
     * 警戒レベルの数値（0〜5）に応じた [KikikuruCardColors] を返却
     */
    fun forKikikuruLevel(level: Int): KikikuruCardColors = when (level) {
        5 -> KikikuruLevel5
        4 -> KikikuruLevel4
        3 -> KikikuruLevel3
        2 -> KikikuruLevel2
        1 -> KikikuruLevel1
        else -> KikikuruLevel0
    }

    /**
     * [AlertLevel] に応じた [KikikuruCardColors] を返却
     */
    fun forKikikuruLevel(level: AlertLevel): KikikuruCardColors = forKikikuruLevel(level.level)

    /**
     * [WeatherWarningType] に応じた [WeatherWarningCardColors] を返却
     */
    @Suppress("DEPRECATION")
    fun forWarningType(type: WeatherWarningType): WeatherWarningCardColors = when (type) {
        WeatherWarningType.SPECIAL_WARNING, WeatherWarningType.FOREIGN_WARNING -> SpecialWarning
        WeatherWarningType.WARNING, WeatherWarningType.DOMESTIC_WARNING -> Warning
        WeatherWarningType.ADVISORY -> Advisory
    }

    /**
     * Material You ([ColorScheme]) のみで構成された [WeatherWarningCardColors] を返却（詳細画面用）。
     */
    @Suppress("DEPRECATION")
    fun materialYouForWarningType(
        type: WeatherWarningType,
        colorScheme: ColorScheme
    ): WeatherWarningCardColors = when (type) {
        WeatherWarningType.SPECIAL_WARNING, WeatherWarningType.FOREIGN_WARNING -> WeatherWarningCardColors(
            cardBg = colorScheme.errorContainer,
            contentColor = colorScheme.onErrorContainer
        )
        WeatherWarningType.WARNING, WeatherWarningType.DOMESTIC_WARNING -> WeatherWarningCardColors(
            cardBg = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer
        )
        WeatherWarningType.ADVISORY -> WeatherWarningCardColors(
            cardBg = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer
        )
    }
}
