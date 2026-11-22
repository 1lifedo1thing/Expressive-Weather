package io.github.nemynew.expressiveweather.theme

import io.github.nemynew.expressiveweather.data.AppLanguage

/**
 * アプリのダークモード動作モード設定。
 * - WEATHER: 天候に追従（昼はライト、夜はダーク）
 * - SYSTEM: デバイス設定に追従（Android OS のシステムテーマ）
 * - LIGHT: ユーザー設定・常にライト
 * - DARK: ユーザー設定・常にダーク
 */
enum class ThemeMode(
    val id: String,
    val jaName: String,
    val enName: String,
    val zhHansName: String,
    val zhHantName: String,
    val jaDesc: String,
    val enDesc: String,
    val zhHansDesc: String,
    val zhHantDesc: String
) {
    WEATHER(
        id = "weather",
        jaName = "天候に追従 (昼夜)",
        enName = "Follow Weather (Day/Night)",
        zhHansName = "跟随天气 (昼夜)",
        zhHantName = "隨天氣變換 (晝夜)",
        jaDesc = "現在地の日の出・日の入り時刻（昼夜）に合わせて自動切り替え",
        enDesc = "Automatically switches between day and night based on local sun position",
        zhHansDesc = "根据当前位置的日出日落时间自动切换昼夜模式",
        zhHantDesc = "根據當前位置的日出日落時間自動切換晝夜模式"
    ),
    SYSTEM(
        id = "system",
        jaName = "デバイス設定に追従",
        enName = "Follow System",
        zhHansName = "跟随系统设置",
        zhHantName = "跟隨系統設定",
        jaDesc = "端末のシステムダークモード設定に合わせて自動切り替え",
        enDesc = "Matches device-wide system dark theme setting",
        zhHansDesc = "根据设备的系统深色模式设置自动切换",
        zhHantDesc = "根據設備的系統深色模式設定自動切換"
    ),
    LIGHT(
        id = "light",
        jaName = "常にライト",
        enName = "Always Light",
        zhHansName = "始终浅色",
        zhHantName = "始終淺色",
        jaDesc = "昼夜や端末設定に関わらず、明るいライトテーマを維持",
        enDesc = "Always use light theme regardless of time or device setting",
        zhHansDesc = "始终保持明亮的浅色主题",
        zhHantDesc = "始終保持明亮的淺色主題"
    ),
    DARK(
        id = "dark",
        jaName = "常にダーク",
        enName = "Always Dark",
        zhHansName = "始终深色",
        zhHantName = "始終深色",
        jaDesc = "昼夜や端末設定に関わらず、落ち着いたダークテーマを維持",
        enDesc = "Always use dark theme regardless of time or device setting",
        zhHansDesc = "始终保持沉稳的深色主题",
        zhHantDesc = "始終保持沉穩的深色主題"
    );

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
        fun fromId(id: String?): ThemeMode =
            entries.find { it.id == id } ?: WEATHER

        /**
         * 現在の ThemeMode、天候の昼夜情報、システムのダークモード状態から
         * 最終的なダークモード適用可否（true: ダーク, false: ライト）を判定します。
         */
        fun resolveIsDark(
            themeMode: ThemeMode,
            isDay: Boolean,
            isSystemDark: Boolean
        ): Boolean {
            return when (themeMode) {
                WEATHER -> !isDay
                SYSTEM -> isSystemDark
                LIGHT -> false
                DARK -> true
            }
        }
    }
}
