package io.github.nemynew.expressiveweather.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import io.github.nemynew.expressiveweather.data.AppLanguage
import java.util.Locale

/**
 * アプリ全体の言語・ロケール設定を管理し、漢字のHan統合（中国語フォント表示問題）
 * を防止して日中英の正規フォントグリフを確実に適用するためのユーティリティ。
 */
object LocaleUtils {

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString("app_language", null)
        return if (savedLangCode != null) {
            AppLanguage.fromCode(savedLangCode)
        } else {
            val systemLocale = Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()
            AppLanguage.fromCode(systemLocale.toLanguageTag())
        }
    }

    fun applyLocaleToContext(baseContext: Context): Context {
        val appLang = getSavedLanguage(baseContext)
        val targetLocale = appLang.locale
        Locale.setDefault(targetLocale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        return baseContext.createConfigurationContext(config)
    }

    fun applyLocaleToActivity(activity: Activity) {
        val appLang = getSavedLanguage(activity)
        val targetLocale = appLang.locale
        Locale.setDefault(targetLocale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = activity.getSystemService(LocaleManager::class.java)
            if (localeManager != null) {
                val desiredLocales = LocaleList.forLanguageTags(appLang.code)
                val currentLocales = localeManager.applicationLocales
                val currentPrimary = if (currentLocales.isEmpty) null else currentLocales.get(0)
                if (currentPrimary?.toLanguageTag() != desiredLocales.get(0)?.toLanguageTag()) {
                    localeManager.applicationLocales = desiredLocales
                }
            }
        }

        val config = activity.resources.configuration
        val currentLocale = config.locales.get(0)
        if (currentLocale != targetLocale) {
            val updatedConfig = Configuration(config)
            updatedConfig.setLocale(targetLocale)
            updatedConfig.setLayoutDirection(targetLocale)
            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(updatedConfig, activity.resources.displayMetrics)
        }
    }
}
