package io.github.nemynew.expressiveweather.theme

import io.github.nemynew.expressiveweather.data.AppLanguage

/**
 * アプリ全体のアクセントカラー表示モード（3択）。
 *
 * - [MATERIAL_YOU_COLORFUL]: 現在の Material You テーマの primary 色相へ調和（Harmonize）させた
 *   M3 トーナル多色パレット（ライト: Tone 40/90、ダーク: Tone 80/30）
 * - [MATERIAL_YOU_TONAL]: 単一の MaterialTheme.colorScheme（primary / secondary / tertiary / error）
 *   のみで構成する落ち着いた統一配色
 * - [CLASSIC_VIVID]: 従来の鮮やかなマルチカラー配色
 */
enum class AccentColorMode(val id: String) {
    CLASSIC_VIVID("classic_vivid"),
    MATERIAL_YOU_COLORFUL("material_you_colorful"),
    MATERIAL_YOU_TONAL("material_you_tonal");

    val isColorful: Boolean
        get() = this != MATERIAL_YOU_TONAL

    fun title(language: AppLanguage): String = when (this) {
        MATERIAL_YOU_COLORFUL -> when (language) {
            AppLanguage.JAPANESE -> "Material You カラフル"
            AppLanguage.KOREAN -> "Material You 컬러풀"
            AppLanguage.SIMPLIFIED_CHINESE -> "Material You 多彩"
            AppLanguage.TRADITIONAL_CHINESE -> "Material You 多彩"
            AppLanguage.ENGLISH -> "Material You Colorful"
            AppLanguage.SPANISH -> "Material You colorido"
            AppLanguage.FRENCH -> "Material You coloré"
            AppLanguage.PORTUGUESE -> "Material You colorido"
            AppLanguage.RUSSIAN -> "Material You цветной"
        }
        MATERIAL_YOU_TONAL -> when (language) {
            AppLanguage.JAPANESE -> "Material You テーマ統一"
            AppLanguage.KOREAN -> "Material You 테마 통일"
            AppLanguage.SIMPLIFIED_CHINESE -> "Material You 主题统一"
            AppLanguage.TRADITIONAL_CHINESE -> "Material You 主題統一"
            AppLanguage.ENGLISH -> "Material You Tonal"
            AppLanguage.SPANISH -> "Material You tonal"
            AppLanguage.FRENCH -> "Material You tonal"
            AppLanguage.PORTUGUESE -> "Material You tonal"
            AppLanguage.RUSSIAN -> "Material You тональный"
        }
        CLASSIC_VIVID -> when (language) {
            AppLanguage.JAPANESE -> "クラシック カラフル"
            AppLanguage.KOREAN -> "클래식 컬러풀"
            AppLanguage.SIMPLIFIED_CHINESE -> "经典多彩"
            AppLanguage.TRADITIONAL_CHINESE -> "經典多彩"
            AppLanguage.ENGLISH -> "Classic Colorful"
            AppLanguage.SPANISH -> "Colorido clásico"
            AppLanguage.FRENCH -> "Coloré classique"
            AppLanguage.PORTUGUESE -> "Colorido clássico"
            AppLanguage.RUSSIAN -> "Классический яркий"
        }
    }

    fun subtitle(language: AppLanguage): String = when (this) {
        MATERIAL_YOU_COLORFUL -> when (language) {
            AppLanguage.JAPANESE -> "現在のテーマカラーに調和させた Material You ベースの多色トーン"
            AppLanguage.KOREAN -> "현재 테마 색상에 조화시킨 Material You 기반 다채로운 톤"
            AppLanguage.SIMPLIFIED_CHINESE -> "与当前主题色和谐融合的 Material You 多彩调色盘"
            AppLanguage.TRADITIONAL_CHINESE -> "與目前主題色和諧融合的 Material You 多彩調色盤"
            AppLanguage.ENGLISH -> "Multi-hue Material You tonal accents harmonized with your theme"
            AppLanguage.SPANISH -> "Acentos multicolores de Material You armonizados con tu tema"
            AppLanguage.FRENCH -> "Accents multicolores Material You harmonisés avec votre thème"
            AppLanguage.PORTUGUESE -> "Destaques multicoloridos do Material You harmonizados com seu tema"
            AppLanguage.RUSSIAN -> "Многоцветные тональные акценты Material You в гармонии с вашей темой"
        }
        MATERIAL_YOU_TONAL -> when (language) {
            AppLanguage.JAPANESE -> "テーマのプライマリ系カラーのみで統一したミニマル配色"
            AppLanguage.KOREAN -> "테마의 기본 색상 계열만으로 통일한 미니멀 배색"
            AppLanguage.SIMPLIFIED_CHINESE -> "仅使用当前主题主色系的极简统一配色"
            AppLanguage.TRADITIONAL_CHINESE -> "僅使用目前主題主色系的極簡統一配色"
            AppLanguage.ENGLISH -> "Minimal unified look using only your active theme scheme"
            AppLanguage.SPANISH -> "Estilo minimalista unificado usando solo tu esquema de tema"
            AppLanguage.FRENCH -> "Style minimaliste unifié utilisant uniquement la palette du thème"
            AppLanguage.PORTUGUESE -> "Visual minimalista unificado usando apenas o esquema do tema"
            AppLanguage.RUSSIAN -> "Минималистичный единый стиль только в цветах активной темы"
        }
        CLASSIC_VIVID -> when (language) {
            AppLanguage.JAPANESE -> "従来の鮮やかなマルチカラーでタイルやグラフを彩る"
            AppLanguage.KOREAN -> "기존의 선명한 멀티컬러로 타일과 그래프를 표현"
            AppLanguage.SIMPLIFIED_CHINESE -> "使用传统鲜艳多彩配色点亮磁贴与图表"
            AppLanguage.TRADITIONAL_CHINESE -> "使用傳統鮮豔多彩配色點亮圖塊與圖表"
            AppLanguage.ENGLISH -> "Traditional vivid multi-color accents for tiles and charts"
            AppLanguage.SPANISH -> "Acentos multicolores clásicos y vibrantes para mosaicos y gráficos"
            AppLanguage.FRENCH -> "Accents multicolores classiques et vifs pour tuiles et graphiques"
            AppLanguage.PORTUGUESE -> "Destaques multicoloridos clássicos e vibrantes para blocos e gráficos"
            AppLanguage.RUSSIAN -> "Классические яркие многоцветные акценты для плиток и графиков"
        }
    }

    companion object {
        fun fromId(id: String?): AccentColorMode? =
            entries.find { it.id == id }
    }
}
