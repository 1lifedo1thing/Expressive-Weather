package com.example.expressiveweather.theme

import androidx.compose.ui.graphics.Color
import com.example.expressiveweather.data.AppLanguage

/**
 * アプリのテーマカラー・プリセット定義。
 * 天候連動（リアルタイム自動調和）、システム動的カラー（Material You）、
 * および厳選された 6 種の独自プリセット（固定カラー）を提供。
 */
enum class ThemePreset(
    val id: String,
    val jaName: String,
    val enName: String,
    val zhHansName: String,
    val zhHantName: String,
    val jaDesc: String,
    val enDesc: String,
    val zhHansDesc: String,
    val zhHantDesc: String,
    val previewPrimary: Color,
    val previewContainer: Color,
    val previewTertiary: Color
) {
    WEATHER(
        id = "weather",
        jaName = "天候連動 (自動)",
        enName = "Weather Adaptive",
        zhHansName = "天气动态联动 (自动)",
        zhHantName = "天氣動態聯動 (自動)",
        jaDesc = "天候や時間帯に応じてアプリ全体の色調がリアルタイム変化",
        enDesc = "Dynamically adjusts to real-time weather & time of day",
        zhHansDesc = "根据实时天气与时段自动调整界面色调",
        zhHantDesc = "根據即時天氣與時段自動調整介面色調",
        previewPrimary = Color(0xFF006492),
        previewContainer = Color(0xFFC9E6FF),
        previewTertiary = Color(0xFFBA5500)
    ),
    SYSTEM(
        id = "system",
        jaName = "システム動的カラー",
        enName = "Dynamic System (Material You)",
        zhHansName = "系统动态取色 (Material You)",
        zhHantName = "系統動態取色 (Material You)",
        jaDesc = "端末の壁紙と Material You パレットに連動",
        enDesc = "Matches device wallpaper palette via Material You",
        zhHansDesc = "取色自设备壁纸与 Material You 动态调色盘",
        zhHantDesc = "取色自設備桌布與 Material You 動態調色盤",
        previewPrimary = Color(0xFF0B57D0),
        previewContainer = Color(0xFFD3E3FD),
        previewTertiary = Color(0xFF725572)
    ),
    SKY_OCEAN(
        id = "sky_ocean",
        jaName = "スカイ・オーシャン",
        enName = "Sky Ocean",
        zhHansName = "晴空碧蓝",
        zhHantName = "晴空碧藍",
        jaDesc = "澄み渡る青空と清涼なアクアブルーに固定",
        enDesc = "Crisp azure sky and ocean aqua tones",
        zhHansDesc = "清澈蔚蓝天空与海洋水蓝色",
        zhHantDesc = "清澈蔚藍天空與海洋水藍色",
        previewPrimary = Color(0xFF006492),
        previewContainer = Color(0xFFC9E6FF),
        previewTertiary = Color(0xFF006874)
    ),
    EMERALD_FOREST(
        id = "emerald",
        jaName = "エメラルド・フォレスト",
        enName = "Emerald Forest",
        zhHansName = "翡翠森林",
        zhHantName = "翡翠森林",
        jaDesc = "瑞々しい新緑と大自然のミントグリーンに固定",
        enDesc = "Fresh foliage and vibrant emerald green",
        zhHansDesc = "生机盎然的新绿与清新薄荷绿",
        zhHantDesc = "生機盎然的新綠與清新薄荷綠",
        previewPrimary = Color(0xFF006C4C),
        previewContainer = Color(0xFF89F8C6),
        previewTertiary = Color(0xFF3D6473)
    ),
    SUNSET_AMBER(
        id = "sunset",
        jaName = "サンセット・アンバー",
        enName = "Sunset Amber",
        zhHansName = "暮色琥珀",
        zhHantName = "暮色琥珀",
        jaDesc = "夕暮れの温もりとゴールデンアンバーに固定",
        enDesc = "Warm twilight glow and golden amber",
        zhHansDesc = "日落余晖的温暖金琥珀色",
        zhHantDesc = "日落餘暉的溫暖金琥珀色",
        previewPrimary = Color(0xFF8E4F00),
        previewContainer = Color(0xFFFFDCC2),
        previewTertiary = Color(0xFF5B6334)
    ),
    LAVENDER_TWILIGHT(
        id = "lavender",
        jaName = "ラベンダー・トワイライト",
        enName = "Lavender Twilight",
        zhHansName = "薰衣草暮光",
        zhHantName = "薰衣草暮光",
        jaDesc = "幻想的な黄昏と深みのあるバイオレットに固定",
        enDesc = "Mystic dusk and elegant violet lavender",
        zhHansDesc = "梦幻暮光与深邃优雅紫罗兰",
        zhHantDesc = "夢幻暮光與深邃優雅紫羅蘭",
        previewPrimary = Color(0xFF6750A4),
        previewContainer = Color(0xFFEADDFF),
        previewTertiary = Color(0xFF7D5260)
    ),
    SAKURA_BLOSSOM(
        id = "sakura",
        jaName = "サクラ・ブロッサム",
        enName = "Sakura Blossom",
        zhHansName = "樱花绯粉",
        zhHantName = "櫻花緋粉",
        jaDesc = "華やかで優しいローズ・チェリーピンクに固定",
        enDesc = "Graceful sakura rose and delicate pink",
        zhHansDesc = "温柔优雅的樱花玫瑰与柔粉色",
        zhHantDesc = "溫柔優雅的櫻花玫瑰與柔粉色",
        previewPrimary = Color(0xFF9C4146),
        previewContainer = Color(0xFFFFDAD9),
        previewTertiary = Color(0xFF755A2F)
    ),
    MIDNIGHT_SLATE(
        id = "slate",
        jaName = "ミッドナイト・スレート",
        enName = "Midnight Slate",
        zhHansName = "夜幕岩灰",
        zhHantName = "夜幕岩灰",
        jaDesc = "洗練されたミニマルなスレートグレーに固定",
        enDesc = "Refined minimal cool slate gray",
        zhHansDesc = "洗练简约的极简板岩深灰",
        zhHantDesc = "洗練簡約的極簡板岩深灰",
        previewPrimary = Color(0xFF4F616E),
        previewContainer = Color(0xFFD2E5F5),
        previewTertiary = Color(0xFF655B6F)
    );

    val isFixed: Boolean
        get() = this != WEATHER && this != SYSTEM

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaName
        AppLanguage.SIMPLIFIED_CHINESE -> zhHansName
        AppLanguage.TRADITIONAL_CHINESE -> zhHantName
        AppLanguage.ENGLISH -> enName
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaDesc
        AppLanguage.SIMPLIFIED_CHINESE -> zhHansDesc
        AppLanguage.TRADITIONAL_CHINESE -> zhHantDesc
        AppLanguage.ENGLISH -> enDesc
    }

    companion object {
        fun fromId(id: String?): ThemePreset =
            entries.find { it.id == id } ?: WEATHER
    }
}
