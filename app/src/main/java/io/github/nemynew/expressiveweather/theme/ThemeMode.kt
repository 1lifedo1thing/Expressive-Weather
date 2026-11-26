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
    val koName: String,
    val zhHansName: String,
    val zhHantName: String,
    val esName: String,
    val frName: String,
    val ptName: String,
    val ruName: String,
    val jaDesc: String,
    val enDesc: String,
    val koDesc: String,
    val zhHansDesc: String,
    val zhHantDesc: String,
    val esDesc: String,
    val frDesc: String,
    val ptDesc: String,
    val ruDesc: String
) {
    WEATHER(
        id = "weather",
        jaName = "天候に追従 (昼夜)",
        enName = "Follow Weather (Day/Night)",
        koName = "날씨에 따름 (주간/야간)",
        zhHansName = "跟随天气 (昼夜)",
        zhHantName = "隨天氣變換 (晝夜)",
        esName = "Seguir el clima (día/noche)",
        frName = "Suivre la météo (jour/nuit)",
        ptName = "Seguir o clima (dia/noite)",
        ruName = "По погоде (день/ночь)",
        jaDesc = "現在地の日の出・日の入り時刻（昼夜）に合わせて自動切り替え",
        enDesc = "Automatically switches between day and night based on local sun position",
        koDesc = "현재 위치의 일출·일몰 시각에 따라 주간/야간 자동 전환",
        zhHansDesc = "根据当前位置的日出日落时间自动切换昼夜模式",
        zhHantDesc = "根據當前位置的日出日落時間自動切換晝夜模式",
        esDesc = "Cambia automáticamente entre día y noche según la posición local del sol",
        frDesc = "Bascule automatiquement entre le jour et la nuit selon le soleil local",
        ptDesc = "Alterna automaticamente entre dia e noite conforme o nascer e pôr do sol local",
        ruDesc = "Автоматически переключается между днём и ночью по местному положению солнца"
    ),
    SYSTEM(
        id = "system",
        jaName = "デバイス設定に追従",
        enName = "Follow System",
        koName = "시스템 설정 따름",
        zhHansName = "跟随系统设置",
        zhHantName = "跟隨系統設定",
        esName = "Seguir al sistema",
        frName = "Suivre le système",
        ptName = "Seguir o sistema",
        ruName = "Как в системе",
        jaDesc = "端末のシステムダークモード設定に合わせて自動切り替え",
        enDesc = "Matches device-wide system dark theme setting",
        koDesc = "기기의 시스템 다크 모드 설정에 맞춰 자동 전환",
        zhHansDesc = "根据设备的系统深色模式设置自动切换",
        zhHantDesc = "根據設備的系統深色模式設定自動切換",
        esDesc = "Se adapta al ajuste de tema oscuro del dispositivo",
        frDesc = "Suit le réglage du thème sombre du système",
        ptDesc = "Segue a configuração de tema escuro do dispositivo",
        ruDesc = "Соответствует системной настройке тёмной темы устройства"
    ),
    LIGHT(
        id = "light",
        jaName = "常にライト",
        enName = "Always Light",
        koName = "항상 라이트",
        zhHansName = "始终浅色",
        zhHantName = "始終淺色",
        esName = "Siempre claro",
        frName = "Toujours clair",
        ptName = "Sempre claro",
        ruName = "Всегда светлая",
        jaDesc = "昼夜や端末設定に関わらず、明るいライトテーマを維持",
        enDesc = "Always use light theme regardless of time or device setting",
        koDesc = "시간이나 기기 설정과 관계없이 항상 밝은 라이트 테마 유지",
        zhHansDesc = "始终保持明亮的浅色主题",
        zhHantDesc = "始終保持明亮的淺色主題",
        esDesc = "Usa siempre el tema claro sin importar la hora ni el ajuste del dispositivo",
        frDesc = "Utilise toujours le thème clair, quelle que soit l'heure ou le réglage système",
        ptDesc = "Mantém sempre o tema claro, independentemente da hora ou do dispositivo",
        ruDesc = "Всегда использовать светлую тему независимо от времени и настроек"
    ),
    DARK(
        id = "dark",
        jaName = "常にダーク",
        enName = "Always Dark",
        koName = "항상 다크",
        zhHansName = "始终深色",
        zhHantName = "始終深色",
        esName = "Siempre oscuro",
        frName = "Toujours sombre",
        ptName = "Sempre escuro",
        ruName = "Всегда тёмная",
        jaDesc = "昼夜や端末設定に関わらず、落ち着いたダークテーマを維持",
        enDesc = "Always use dark theme regardless of time or device setting",
        koDesc = "시간이나 기기 설정과 관계없이 항상 차분한 다크 테마 유지",
        zhHansDesc = "始终保持沉稳的深色主题",
        zhHantDesc = "始終保持沉穩的深色主題",
        esDesc = "Usa siempre el tema oscuro sin importar la hora ni el ajuste del dispositivo",
        frDesc = "Utilise toujours le thème sombre, quelle que soit l'heure ou le réglage système",
        ptDesc = "Mantém sempre o tema escuro, independentemente da hora ou do dispositivo",
        ruDesc = "Всегда использовать тёмную тему независимо от времени и настроек"
    );

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaName
        AppLanguage.KOREAN -> koName
        AppLanguage.SIMPLIFIED_CHINESE -> zhHansName
        AppLanguage.TRADITIONAL_CHINESE -> zhHantName
        AppLanguage.ENGLISH -> enName
        AppLanguage.SPANISH -> esName
        AppLanguage.FRENCH -> frName
        AppLanguage.PORTUGUESE -> ptName
        AppLanguage.RUSSIAN -> ruName
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaDesc
        AppLanguage.KOREAN -> koDesc
        AppLanguage.SIMPLIFIED_CHINESE -> zhHansDesc
        AppLanguage.TRADITIONAL_CHINESE -> zhHantDesc
        AppLanguage.ENGLISH -> enDesc
        AppLanguage.SPANISH -> esDesc
        AppLanguage.FRENCH -> frDesc
        AppLanguage.PORTUGUESE -> ptDesc
        AppLanguage.RUSSIAN -> ruDesc
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
