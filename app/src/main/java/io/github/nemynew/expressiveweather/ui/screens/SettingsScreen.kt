package io.github.nemynew.expressiveweather.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import io.github.nemynew.expressiveweather.data.WeatherBackgroundUpdateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.data.*
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics

private enum class SettingsPaneCategory(val sectionKey: String) {
    APPEARANCE(SettingsDataHolder.SECTION_APPEARANCE),
    GENERAL(SettingsDataHolder.SECTION_GENERAL),
    SOURCES(SettingsDataHolder.SECTION_SOURCES),
    APP_INFO("app_info"),
    DEBUG(SettingsDataHolder.SECTION_DEBUG)
}

/**
 * 完全に一から再設計した本格 Material 3 Expressive (M3E) 仕様の設定画面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    weather: WeatherData?,
    language: AppLanguage,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    weatherUpdateInterval: WeatherUpdateInterval = WeatherUpdateInterval.MINUTES_30,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    isFrostedGlassEnabled: Boolean = true,
    isColorfulShapesEnabled: Boolean = true,
    hapticIntensity: HapticIntensity = HapticIntensity.STANDARD,
    isWeatherHapticsEnabled: Boolean = true,
    isRadarDebugZoomEnabled: Boolean = false,
    isRadarDebugSourceEnabled: Boolean = true,
    isShowAllJapanRadarLayersEnabled: Boolean = false,
    isLiveAlertNotificationEnabled: Boolean = true,
    alertNotificationThreshold: AlertNotificationThreshold = AlertNotificationThreshold.DEFAULT,
    isKikikuruTestEnabled: Boolean = false,
    isNwsTestEnabled: Boolean = false,
    isGdacsTestEnabled: Boolean = false,
    isGdacsAlertsEnabled: Boolean = false,
    isWmoAlertsEnabled: Boolean = true,
    isWmoAlwaysEnabled: Boolean = false,
    isWmoTestEnabled: Boolean = false,
    isMeteoAlarmAlertsEnabled: Boolean = true,
    isMeteoAlarmTestEnabled: Boolean = false,
    precipitationDemoStatus: RainStatus? = null,
    initialSection: String = SettingsDataHolder.SECTION_ALL,
    onToggleWeatherAdaptiveTheme: () -> Unit,
    onSelectThemePreset: (ThemePreset) -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onToggleFrostedGlass: (Boolean) -> Unit = {},
    onToggleColorfulShapes: (Boolean) -> Unit = {},
    onSelectHapticIntensity: (HapticIntensity) -> Unit = {},
    onToggleWeatherHaptics: (Boolean) -> Unit = {},
    onToggleRadarDebugZoom: (Boolean) -> Unit = {},
    onToggleRadarDebugSource: (Boolean) -> Unit = {},
    onToggleShowAllJapanRadarLayers: (Boolean) -> Unit = {},
    onToggleLiveAlertNotification: (Boolean) -> Unit = {},
    onSelectAlertNotificationThreshold: (AlertNotificationThreshold) -> Unit = {},
    onToggleKikikuruTest: (Boolean) -> Unit = {},
    onToggleNwsTest: (Boolean) -> Unit = {},
    onToggleGdacsTest: (Boolean) -> Unit = {},
    onToggleGdacsAlerts: (Boolean) -> Unit = {},
    onToggleWmoAlerts: (Boolean) -> Unit = {},
    onToggleWmoAlways: (Boolean) -> Unit = {},
    onToggleWmoTest: (Boolean) -> Unit = {},
    onToggleMeteoAlarmAlerts: (Boolean) -> Unit = {},
    onToggleMeteoAlarmTest: (Boolean) -> Unit = {},
    onCyclePrecipitationDemo: () -> Unit = {},
    onClearPrecipitationDemo: () -> Unit = {},
    onToggleTemperatureUnit: () -> Unit,
    onSelectWindSpeedUnit: (WindSpeedUnit) -> Unit = {},
    onSelectWeatherUpdateInterval: (WeatherUpdateInterval) -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectPreviewCondition: ((WeatherCondition?, Boolean?) -> Unit)? = null,
    onClearCache: (() -> Unit)? = null,
    onForceRefresh: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expressHaptics = LocalExpressiveHaptics.current
    var currentHapticIntensity by remember(hapticIntensity) { mutableStateOf(hapticIntensity) }
    var currentWeatherHaptics by remember(isWeatherHapticsEnabled) { mutableStateOf(isWeatherHapticsEnabled) }
    var currentPreset by remember(themePreset) { mutableStateOf(themePreset) }
    var currentThemeMode by remember(themeMode) { mutableStateOf(themeMode) }
    var currentRadarDebugZoom by remember(isRadarDebugZoomEnabled) { mutableStateOf(isRadarDebugZoomEnabled) }
    var currentRadarDebugSource by remember(isRadarDebugSourceEnabled) { mutableStateOf(isRadarDebugSourceEnabled) }
    var currentShowAllJapanRadarLayers by remember(isShowAllJapanRadarLayersEnabled) { mutableStateOf(isShowAllJapanRadarLayersEnabled) }
    var currentLiveAlertNotification by remember(isLiveAlertNotificationEnabled) { mutableStateOf(isLiveAlertNotificationEnabled) }
    var currentThreshold by remember(alertNotificationThreshold) { mutableStateOf(alertNotificationThreshold) }
    var currentKikikuruTest by remember(isKikikuruTestEnabled) { mutableStateOf(isKikikuruTestEnabled) }
    var currentNwsTest by remember(isNwsTestEnabled) { mutableStateOf(isNwsTestEnabled) }
    var currentGdacsTest by remember(isGdacsTestEnabled) { mutableStateOf(isGdacsTestEnabled) }
    var currentGdacsAlerts by remember(isGdacsAlertsEnabled) { mutableStateOf(isGdacsAlertsEnabled) }
    var currentWmoAlerts by remember(isWmoAlertsEnabled) { mutableStateOf(isWmoAlertsEnabled) }
    var currentWmoAlways by remember(isWmoAlwaysEnabled) { mutableStateOf(isWmoAlwaysEnabled) }
    var currentWmoTest by remember(isWmoTestEnabled) { mutableStateOf(isWmoTestEnabled) }
    var currentMeteoAlarmAlerts by remember(isMeteoAlarmAlertsEnabled) { mutableStateOf(isMeteoAlarmAlertsEnabled) }
    var currentMeteoAlarmTest by remember(isMeteoAlarmTestEnabled) { mutableStateOf(isMeteoAlarmTestEnabled) }
    val isSystemDark = isSystemInDarkTheme()
    val isDark = ThemeMode.resolveIsDark(
        themeMode = currentThemeMode,
        isDay = weather?.isDay ?: true,
        isSystemDark = isSystemDark
    )
    val condition = weather?.condition ?: WeatherCondition.CLEAR

    var currentAdaptiveTheme by remember(currentPreset) { mutableStateOf(currentPreset == ThemePreset.WEATHER) }
    var currentFrostedGlass by remember(isFrostedGlassEnabled) { mutableStateOf(isFrostedGlassEnabled) }
    var currentColorfulShapes by remember(isColorfulShapesEnabled) { mutableStateOf(isColorfulShapesEnabled) }
    var currentUnit by remember(temperatureUnit) { mutableStateOf(temperatureUnit) }
    var currentWindUnit by remember(windSpeedUnit) { mutableStateOf(windSpeedUnit) }
    var currentWeatherUpdateInterval by remember(weatherUpdateInterval) { mutableStateOf(weatherUpdateInterval) }
    var currentLang by remember(language) { mutableStateOf(language) }

    var showTempUnitDialog by rememberSaveable { mutableStateOf(false) }
    var showWindUnitDialog by rememberSaveable { mutableStateOf(false) }
    var showWeatherUpdateIntervalDialog by rememberSaveable { mutableStateOf(false) }
    var showThresholdDialog by rememberSaveable { mutableStateOf(false) }
    var showDataProviderDialog by rememberSaveable { mutableStateOf(false) }
    var showDataSourcesDialog by rememberSaveable { mutableStateOf(false) }
    var showLicenseDialog by rememberSaveable { mutableStateOf(false) }
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var showSimulationDialog by rememberSaveable { mutableStateOf(false) }
    var showHapticIntensityDialog by rememberSaveable { mutableStateOf(false) }
    var showHapticShowcaseDialog by rememberSaveable { mutableStateOf(false) }
    var showBgLocationDisclosureDialog by rememberSaveable { mutableStateOf(false) }
    val isDirectSubPageLaunch = initialSection == SettingsDataHolder.SECTION_LANGUAGE ||
        initialSection == "language" ||
        initialSection == "app_info"
    var currentSubPage by rememberSaveable(initialSection) {
        mutableStateOf<String?>(
            if (initialSection == SettingsDataHolder.SECTION_LANGUAGE || initialSection == "language") "language"
            else if (initialSection == "app_info") "app_info"
            else null
        )
    }
    val handleSubPageBack: () -> Unit = {
        if (isDirectSubPageLaunch) {
            onBack()
        } else {
            currentSubPage = null
        }
    }
    val prefs = remember(context) {
        context.getSharedPreferences("expressive_weather_prefs", android.content.Context.MODE_PRIVATE)
    }
    var isDebugUnlocked by remember {
        mutableStateOf(prefs.getBoolean("debug_settings_unlocked", false))
    }
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "debug_settings_unlocked") {
                isDebugUnlocked = prefs.getBoolean("debug_settings_unlocked", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // バックグラウンド位置情報とバッテリー最適化状態の動的監視
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasBgLocation by remember { mutableStateOf(false) }
    var isBatteryIgnored by remember { mutableStateOf(false) }

    val refreshPermissionStates = remember(context) {
        {
            val provider = CurrentLocationProvider(context)
            hasBgLocation = provider.hasBackgroundLocationPermission()
            isBatteryIgnored = provider.isBatteryOptimizationIgnored()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refreshPermissionStates()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // アプリ情報（アプリ名、バージョン名、ビルド番号、アプリアイコン）の動的取得
    val packageInfo = remember(context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    val appName = currentLang.strings.appName

    val versionName = remember(packageInfo) {
        packageInfo?.versionName ?: "0.1.0"
    }

    val versionCode = remember(packageInfo) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "1"
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString() ?: "1"
        }
    }

    val appIconBitmap: ImageBitmap? = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(
                width,
                height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    val radarRepo = remember { RadarRepository.getInstance(context) }
    var currentRadarSourceMode by remember { mutableStateOf(radarRepo.getSourceMode()) }
    var showRadarSourceDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveWeatherTheme(
        darkTheme = isDark,
        weatherCondition = if (currentPreset == ThemePreset.WEATHER) condition else null,
        weatherAdaptiveColor = currentPreset == ThemePreset.WEATHER,
        themePreset = currentPreset
    ) {
        CompositionLocalProvider(
            LocalAppLanguage provides currentLang,
            LocalAppStrings provides currentLang.strings
        ) {
            val strings = LocalAppStrings.current
            val colorScheme = MaterialTheme.colorScheme

            // 各カテゴリの中身を共通セクションブロックとして定義し、スマホ1ペインとタブレット2ペインで共有
            val renderAppearanceSection: @Composable () -> Unit = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // --- テーマカラー ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when (currentLang) {
                                    AppLanguage.JAPANESE -> "テーマカラー"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "主题配色"
                                    AppLanguage.TRADITIONAL_CHINESE -> "主題配色"
                                    AppLanguage.ENGLISH -> "Theme Color"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )

                            Android15CardGroup {
                                // 1. 【1番上】デバイスのテーマカラーに追従（デフォルトでオン）
                                Android15SettingsCard(
                                    title = when (currentLang) {
                                        AppLanguage.JAPANESE -> "デバイスのテーマカラーに追従"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "跟随系统动态取色"
                                        AppLanguage.TRADITIONAL_CHINESE -> "跟隨系統動態取色"
                                        AppLanguage.ENGLISH -> "Follow Device Theme Color"
                                    },
                                    subtitle = if (currentPreset == ThemePreset.SYSTEM) {
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "端末の壁紙とMaterial Youの配色に連動中"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "已联动设备壁纸与 Material You 动态调色盘"
                                            AppLanguage.TRADITIONAL_CHINESE -> "已聯動設備桌布與 Material You 動態調色盤"
                                            AppLanguage.ENGLISH -> "Matches device wallpaper via Material You"
                                        }
                                    } else {
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "オフ (カスタム配色または天気連動を使用)"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "已关闭 (使用自定义配色或天气联动)"
                                            AppLanguage.TRADITIONAL_CHINESE -> "已關閉 (使用自訂配色或天氣聯動)"
                                            AppLanguage.ENGLISH -> "Off (Using custom or weather adaptive color)"
                                        }
                                    },
                                    icon = Icons.Rounded.Palette,
                                    iconBackgroundColor = if (currentPreset == ThemePreset.SYSTEM) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                                    iconTintColor = if (currentPreset == ThemePreset.SYSTEM) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    position = ConnectedCardPosition.Top,
                                    trailing = {
                                        Switch(
                                            checked = currentPreset == ThemePreset.SYSTEM,
                                            onCheckedChange = { checked ->
                                                expressHaptics.toggle(checked)
                                                if (checked) {
                                                    currentPreset = ThemePreset.SYSTEM
                                                    currentAdaptiveTheme = false
                                                    onSelectThemePreset(ThemePreset.SYSTEM)
                                                } else {
                                                    currentPreset = ThemePreset.WEATHER
                                                    currentAdaptiveTheme = true
                                                    onSelectThemePreset(ThemePreset.WEATHER)
                                                }
                                            }
                                        )
                                    },
                                    onClick = {
                                        val nextSystem = currentPreset != ThemePreset.SYSTEM
                                        expressHaptics.toggle(nextSystem)
                                        if (nextSystem) {
                                            currentPreset = ThemePreset.SYSTEM
                                            currentAdaptiveTheme = false
                                            onSelectThemePreset(ThemePreset.SYSTEM)
                                        } else {
                                            currentPreset = ThemePreset.WEATHER
                                            currentAdaptiveTheme = true
                                            onSelectThemePreset(ThemePreset.WEATHER)
                                        }
                                    }
                                )

                                // 2. 天気連動トグルカード
                                Android15SettingsCard(
                                    title = when (currentLang) {
                                        AppLanguage.JAPANESE -> "天気連動"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "天气动态联动"
                                        AppLanguage.TRADITIONAL_CHINESE -> "天氣動態聯動"
                                        AppLanguage.ENGLISH -> "Weather Adaptive"
                                    },
                                    subtitle = if (currentPreset == ThemePreset.WEATHER) {
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "天候に合わせて自動で色が変化"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "根据实时天气自动调整色彩"
                                            AppLanguage.TRADITIONAL_CHINESE -> "根據即時天氣自動調整色彩"
                                            AppLanguage.ENGLISH -> "Dynamic colors adapt to weather"
                                        }
                                    } else if (currentPreset == ThemePreset.SYSTEM) {
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "オフ (デバイスのテーマカラーを使用中)"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "已关闭 (正在使用系统主题色彩)"
                                            AppLanguage.TRADITIONAL_CHINESE -> "已關閉 (正在使用系統主題色彩)"
                                            AppLanguage.ENGLISH -> "Off (Device theme color active)"
                                        }
                                    } else {
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "固定カラー: ${currentPreset.displayName(currentLang)}"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "固定配色: ${currentPreset.displayName(currentLang)}"
                                            AppLanguage.TRADITIONAL_CHINESE -> "固定配色: ${currentPreset.displayName(currentLang)}"
                                            AppLanguage.ENGLISH -> "Fixed: ${currentPreset.displayName(currentLang)}"
                                        }
                                    },
                                    icon = Icons.Rounded.WbSunny,
                                    iconBackgroundColor = if (currentPreset == ThemePreset.WEATHER) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                                    iconTintColor = if (currentPreset == ThemePreset.WEATHER) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    position = ConnectedCardPosition.Bottom,
                                    trailing = {
                                        Switch(
                                            checked = currentPreset == ThemePreset.WEATHER,
                                            onCheckedChange = { checked ->
                                                expressHaptics.toggle(checked)
                                                if (checked) {
                                                    currentPreset = ThemePreset.WEATHER
                                                    currentAdaptiveTheme = true
                                                    onSelectThemePreset(ThemePreset.WEATHER)
                                                } else {
                                                    currentPreset = ThemePreset.SKY_OCEAN
                                                    currentAdaptiveTheme = false
                                                    onSelectThemePreset(ThemePreset.SKY_OCEAN)
                                                }
                                            }
                                        )
                                    },
                                    onClick = {
                                        val nextWeather = currentPreset != ThemePreset.WEATHER
                                        expressHaptics.toggle(nextWeather)
                                        if (nextWeather) {
                                            currentPreset = ThemePreset.WEATHER
                                            currentAdaptiveTheme = true
                                            onSelectThemePreset(ThemePreset.WEATHER)
                                        } else {
                                            currentPreset = ThemePreset.SKY_OCEAN
                                            currentAdaptiveTheme = false
                                            onSelectThemePreset(ThemePreset.SKY_OCEAN)
                                        }
                                    }
                                )
                            }

                            // デバイス連動・天気連動のどちらもオフの時のみ、横にスクロール可能なカラー選択画面がぬるっと展開
                            AnimatedVisibility(
                                visible = currentPreset != ThemePreset.SYSTEM && currentPreset != ThemePreset.WEATHER,
                                enter = expandVertically(
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                ) + fadeIn(
                                    animationSpec = tween(durationMillis = 200)
                                ),
                                exit = shrinkVertically(
                                    animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 180)
                                )
                            ) {
                                ThemePresetHorizontalPicker(
                                    currentPreset = currentPreset,
                                    language = currentLang,
                                    onSelectPreset = { preset ->
                                        currentPreset = preset
                                        currentAdaptiveTheme = false
                                        onSelectThemePreset(preset)
                                    }
                                )
                            }
                        }

                        // --- ダークモード ---
                        DarkModeToggleSection(
                            currentThemeMode = currentThemeMode,
                            isDark = isDark,
                            language = currentLang,
                            onSelectThemeMode = { mode ->
                                currentThemeMode = mode
                                onSelectThemeMode(mode)
                            }
                        )

                        // --- 視覚効果 (フロスト背景効果 & カラフルアクセント) ---
                        VisualEffectsSettingsSection(
                            isFrostedGlassEnabled = currentFrostedGlass,
                            isColorfulShapesEnabled = currentColorfulShapes,
                            language = currentLang,
                            onToggleFrostedGlass = { checked ->
                                currentFrostedGlass = checked
                                onToggleFrostedGlass(checked)
                            },
                            onToggleColorfulShapes = { checked ->
                                currentColorfulShapes = checked
                                onToggleColorfulShapes(checked)
                            }
                        )
                }
            }

            // ================= 2. 一般設定と単位 (General Preferences & Units) =================
            val renderGeneralSection: @Composable (
                onOpenLanguage: () -> Unit,
                onOpenAppInfo: () -> Unit
            ) -> Unit = { onOpenLanguage, onOpenAppInfo ->
                Android15CardGroup {
                            // 1. 言語を変更
                            Android15SettingsCard(
                                title = strings.changeLanguageTitle,
                                subtitle = currentLang.displayName,
                                icon = Icons.Rounded.Translate,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Top,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                onClick = onOpenLanguage
                            )

                            // 2. 気温の単位
                            Android15SettingsCard(
                                title = strings.tempUnitTitle,
                                subtitle = if (currentUnit == TemperatureUnit.CELSIUS) {
                                    when (currentLang) {
                                        AppLanguage.JAPANESE -> "摂氏 (°C)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "摄氏度 (°C)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "攝氏度 (°C)"
                                        AppLanguage.ENGLISH -> "Celsius (°C)"
                                    }
                                } else {
                                    when (currentLang) {
                                        AppLanguage.JAPANESE -> "華氏 (°F)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "华氏度 (°F)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "華氏度 (°F)"
                                        AppLanguage.ENGLISH -> "Fahrenheit (°F)"
                                    }
                                },
                                icon = Icons.Rounded.DeviceThermostat,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                onClick = { showTempUnitDialog = true }
                            )

                            // 3. 風速の単位
                            Android15SettingsCard(
                                title = strings.windSpeedUnitTitle,
                                subtitle = when (currentWindUnit) {
                                    WindSpeedUnit.FOLLOW_LANGUAGE -> when (currentLang) {
                                        AppLanguage.JAPANESE -> "言語連動 (Auto)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "跟随语言 (自动)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "跟隨語言 (自動)"
                                        AppLanguage.ENGLISH -> "Auto (Follow Language)"
                                    }
                                    WindSpeedUnit.MS -> "m/s"
                                    WindSpeedUnit.KMH -> "km/h"
                                    WindSpeedUnit.MPH -> "mph"
                                },
                                icon = Icons.Rounded.Air,
                                iconBackgroundColor = colorScheme.tertiaryContainer,
                                iconTintColor = colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Middle,
                                onClick = { showWindUnitDialog = true }
                            )

                            // 4. 天気の自動更新頻度 (Weather Update Frequency)
                            val lastUpdateMs = remember { WeatherBackgroundUpdateManager.getInstance(context).getLastUpdateTime() }
                            val lastUpdateCity = remember { WeatherBackgroundUpdateManager.getInstance(context).getLastUpdateCity() }
                            val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                            val lastSyncStr = if (lastUpdateMs > 0L) {
                                val timeStr = timeFormatter.format(Date(lastUpdateMs))
                                val cityPart = if (!lastUpdateCity.isNullOrBlank()) " ($lastUpdateCity)" else ""
                                when (currentLang) {
                                    AppLanguage.JAPANESE -> " • 前回同期: $timeStr$cityPart"
                                    AppLanguage.SIMPLIFIED_CHINESE -> " • 上次同步: $timeStr$cityPart"
                                    AppLanguage.TRADITIONAL_CHINESE -> " • 上次同步: $timeStr$cityPart"
                                    AppLanguage.ENGLISH -> " • Last sync: $timeStr$cityPart"
                                }
                            } else ""

                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "天気の自動更新頻度"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "天气自动刷新频率"
                                    AppLanguage.TRADITIONAL_CHINESE -> "天氣自動重新整理頻率"
                                    AppLanguage.ENGLISH -> "Weather Update Frequency"
                                },
                                subtitle = "${currentWeatherUpdateInterval.getLabel(currentLang)}$lastSyncStr・" + when (currentLang) {
                                    AppLanguage.JAPANESE -> "現在地の天気をWorkManager & アラームで確実に自動同期"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "通过WorkManager与唤醒闹钟准时自动同步当前位置天气"
                                    AppLanguage.TRADITIONAL_CHINESE -> "透過WorkManager與喚醒鬧鐘準時自動同步目前位置天氣"
                                    AppLanguage.ENGLISH -> "Reliably auto-syncs current location via WorkManager & wake alarms"
                                },
                                icon = Icons.Rounded.Update,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = { showWeatherUpdateIntervalDialog = true }
                            )

                            // 5. 警戒レベル・キキクルLive Updates通知
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "警戒レベル・キキクル通知"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "灾害预警实时动态通知"
                                    AppLanguage.TRADITIONAL_CHINESE -> "災害預警即時動態通知"
                                    AppLanguage.ENGLISH -> "Disaster Alert Live Updates"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> if (currentLiveAlertNotification) "発表時に常駐通知でリアルタイム表示" else "通知は現在オフです"
                                    AppLanguage.SIMPLIFIED_CHINESE -> if (currentLiveAlertNotification) "发布时以常驻通知实时展示" else "通知当前已关闭"
                                    AppLanguage.TRADITIONAL_CHINESE -> if (currentLiveAlertNotification) "發布時以常駐通知即時展示" else "通知目前已關閉"
                                    AppLanguage.ENGLISH -> if (currentLiveAlertNotification) "Ongoing live updates when alerts are active" else "Notifications currently disabled"
                                },
                                icon = Icons.Rounded.NotificationsActive,
                                iconBackgroundColor = colorScheme.errorContainer,
                                iconTintColor = colorScheme.onErrorContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentLiveAlertNotification,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentLiveAlertNotification = checked
                                            onToggleLiveAlertNotification(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentLiveAlertNotification
                                    expressHaptics.toggle(next)
                                    currentLiveAlertNotification = next
                                    onToggleLiveAlertNotification(next)
                                }
                            )

                            // 6. 通知する警戒重要度（設定ダイアログ）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "通知する警戒重要度"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "预警通知重要度"
                                    AppLanguage.TRADITIONAL_CHINESE -> "預警通知重要度"
                                    AppLanguage.ENGLISH -> "Alert Notification Level"
                                },
                                subtitle = when (currentThreshold) {
                                    AlertNotificationThreshold.ADVISORY_AND_ABOVE -> when (currentLang) {
                                        AppLanguage.JAPANESE -> "注意報以上（レベル2以上で通知）"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "注意报以上 (2级以上通知)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "注意報以上 (2級以上通知)"
                                        AppLanguage.ENGLISH -> "Advisory & Above (Level 2+)"
                                    }
                                    AlertNotificationThreshold.WARNING_AND_ABOVE -> when (currentLang) {
                                        AppLanguage.JAPANESE -> "警報以上（レベル3以上で通知）"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "警报以上 (3级以上通知)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "警報以上 (3級以上通知)"
                                        AppLanguage.ENGLISH -> "Warning & Above (Level 3+)"
                                    }
                                    AlertNotificationThreshold.EMERGENCY_ONLY -> when (currentLang) {
                                        AppLanguage.JAPANESE -> "特別警報・危険のみ（レベル4以上で通知）"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "仅特别警报/危险 (4级以上通知)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "僅特別警報/危險 (4級以上通知)"
                                        AppLanguage.ENGLISH -> "Emergency Only (Level 4+)"
                                    }
                                    AlertNotificationThreshold.OFF -> when (currentLang) {
                                        AppLanguage.JAPANESE -> "通知オフ"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭通知"
                                        AppLanguage.TRADITIONAL_CHINESE -> "關閉通知"
                                        AppLanguage.ENGLISH -> "Off"
                                    }
                                },
                                icon = Icons.Rounded.NotificationImportant,
                                iconBackgroundColor = colorScheme.errorContainer.copy(alpha = 0.7f),
                                iconTintColor = colorScheme.onErrorContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = { showThresholdDialog = true }
                            )

                            // 7. 警報・キキクル自動監視（高頻度監視の案内）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "警報・キキクル自動監視"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "预警与危险度自动监测"
                                    AppLanguage.TRADITIONAL_CHINESE -> "預警與危險度自動監測"
                                    AppLanguage.ENGLISH -> "Alert & Kikikuru Monitoring"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "警報・注意報およびキキクルを別系統（10分／警戒時3分）で高頻度監視（現在地のみ）"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "独立高频监测预警与危险度（平时10分钟／警戒时3分钟，仅当前位置）"
                                    AppLanguage.TRADITIONAL_CHINESE -> "獨立高頻監測預警與危險度（平時10分鐘／警戒時3分鐘，僅當前位置）"
                                    AppLanguage.ENGLISH -> "Independent high-frequency monitoring for alerts & Kikikuru (10 min / 3 min on alert, current location only)"
                                },
                                icon = Icons.Rounded.Speed,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = when (currentLang) {
                                                AppLanguage.JAPANESE -> "10分・3分"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "10分・3分"
                                                AppLanguage.TRADITIONAL_CHINESE -> "10分・3分"
                                                AppLanguage.ENGLISH -> "10m / 3m"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            )

                            // 7. 位置情報の常時取得（常に許可）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "位置情報の常時取得（常に許可）"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "后台位置信息（始终允许）"
                                    AppLanguage.TRADITIONAL_CHINESE -> "後台位置資訊（始終允許）"
                                    AppLanguage.ENGLISH -> "Background Location (Always Allow)"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> if (hasBgLocation) "バックグラウンドで移動先の警報をリアルタイム監視中" else "バックグラウンド監視には「常に許可」が必要です。タップして設定"
                                    AppLanguage.SIMPLIFIED_CHINESE -> if (hasBgLocation) "在后台实时监测移动目的地的预警" else "需要「始终允许」位置信息以在后台监测预警。点击设置"
                                    AppLanguage.TRADITIONAL_CHINESE -> if (hasBgLocation) "在後台即時監測移動目的地的預警" else "需要「始終允許」位置資訊以在後台監測預警。點擊設定"
                                    AppLanguage.ENGLISH -> if (hasBgLocation) "Monitoring current location alerts in background" else "Requires 'Allow all the time' for background monitoring. Tap to configure"
                                },
                                icon = Icons.Rounded.LocationOn,
                                iconBackgroundColor = if (hasBgLocation) colorScheme.primaryContainer else colorScheme.tertiaryContainer,
                                iconTintColor = if (hasBgLocation) colorScheme.onPrimaryContainer else colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    if (hasBgLocation) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF388E3C),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = when (currentLang) {
                                                    AppLanguage.JAPANESE -> "常に許可"
                                                    AppLanguage.SIMPLIFIED_CHINESE -> "已始终允许"
                                                    AppLanguage.TRADITIONAL_CHINESE -> "已始終允許"
                                                    AppLanguage.ENGLISH -> "Always"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF388E3C)
                                            )
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = when (currentLang) {
                                                    AppLanguage.JAPANESE -> "設定する"
                                                    AppLanguage.SIMPLIFIED_CHINESE -> "前往设置"
                                                    AppLanguage.TRADITIONAL_CHINESE -> "前往設定"
                                                    AppLanguage.ENGLISH -> "Configure"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (hasBgLocation) {
                                        CurrentLocationProvider.openAppLocationSettings(context)
                                    } else {
                                        showBgLocationDisclosureDialog = true
                                    }
                                }
                            )

                            // 8. バッテリー設定ガイド（バックグラウンド監視）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> if (BuildConfig.ALLOW_BATTERY_OPTIMIZATION_REQUEST_DIALOG) "バッテリー制限の解除" else "バッテリー設定（任意）"
                                    AppLanguage.SIMPLIFIED_CHINESE -> if (BuildConfig.ALLOW_BATTERY_OPTIMIZATION_REQUEST_DIALOG) "解除电池优化限制" else "电池设置（可选）"
                                    AppLanguage.TRADITIONAL_CHINESE -> if (BuildConfig.ALLOW_BATTERY_OPTIMIZATION_REQUEST_DIALOG) "解除電池最佳化限制" else "電池設定（可選）"
                                    AppLanguage.ENGLISH -> if (BuildConfig.ALLOW_BATTERY_OPTIMIZATION_REQUEST_DIALOG) "Battery Optimization Exemption" else "Battery Settings (Optional)"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> if (isBatteryIgnored) "制限なし（バックグラウンド監視が停止されません）" else "Androidのアプリ設定からバックグラウンド動作を確認できます"
                                    AppLanguage.SIMPLIFIED_CHINESE -> if (isBatteryIgnored) "无限制（后台监测不会被系统中断）" else "可在 Android 应用设置中检查后台电池策略"
                                    AppLanguage.TRADITIONAL_CHINESE -> if (isBatteryIgnored) "無限制（後台監測不會被系統中斷）" else "可在 Android 應用程式設定中檢查後台電池策略"
                                    AppLanguage.ENGLISH -> if (isBatteryIgnored) "Unrestricted (Background monitoring won't be killed)" else "Open Android app settings to review background battery preferences"
                                },
                                icon = Icons.Rounded.BatteryChargingFull,
                                iconBackgroundColor = if (isBatteryIgnored) colorScheme.primaryContainer else colorScheme.errorContainer.copy(alpha = 0.5f),
                                iconTintColor = if (isBatteryIgnored) colorScheme.onPrimaryContainer else colorScheme.onErrorContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    if (isBatteryIgnored) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF388E3C),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = when (currentLang) {
                                                    AppLanguage.JAPANESE -> "制限なし"
                                                    AppLanguage.SIMPLIFIED_CHINESE -> "无限制"
                                                    AppLanguage.TRADITIONAL_CHINESE -> "無限制"
                                                    AppLanguage.ENGLISH -> "Unrestricted"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF388E3C)
                                            )
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = when (currentLang) {
                                                    AppLanguage.JAPANESE -> "解除する"
                                                    AppLanguage.SIMPLIFIED_CHINESE -> "解除限制"
                                                    AppLanguage.TRADITIONAL_CHINESE -> "解除限制"
                                                    AppLanguage.ENGLISH -> "Disable"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.error
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    CurrentLocationProvider.requestIgnoreBatteryOptimizations(context)
                                }
                            )

                            // 9. ハプティクス強度
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "ハプティクス（触覚フィードバック）強度"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "触觉反馈强度"
                                    AppLanguage.TRADITIONAL_CHINESE -> "觸覺回饋強度"
                                    AppLanguage.ENGLISH -> "Haptics Intensity"
                                },
                                subtitle = currentHapticIntensity.getLabel(currentLang),
                                icon = Icons.Rounded.Vibration,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = { showHapticIntensityDialog = true }
                            )

                            // 10. 天候連動ハプティクス
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "天候連動ハプティクス"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "天气特征触觉反馈"
                                    AppLanguage.TRADITIONAL_CHINESE -> "天氣特徵觸覺回饋"
                                    AppLanguage.ENGLISH -> "Weather-Reactive Haptics"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "雨粒・雷鳴・降雪・突風など天候ごとの固有振動を再生"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "切换城市或遭遇雷雨、降雪、强风时触发对应质感振动"
                                    AppLanguage.TRADITIONAL_CHINESE -> "切換城市或遭遇雷雨、降雪、強風時觸發對應質感震動"
                                    AppLanguage.ENGLISH -> "Play raindrop, thunder, snow & wind tactile signatures"
                                },
                                icon = Icons.Rounded.Thunderstorm,
                                iconBackgroundColor = if (currentWeatherHaptics && currentHapticIntensity != HapticIntensity.OFF) {
                                    colorScheme.secondaryContainer
                                } else {
                                    colorScheme.surfaceContainerHighest
                                },
                                iconTintColor = if (currentWeatherHaptics && currentHapticIntensity != HapticIntensity.OFF) {
                                    colorScheme.onSecondaryContainer
                                } else {
                                    colorScheme.onSurfaceVariant
                                },
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentWeatherHaptics,
                                        enabled = currentHapticIntensity != HapticIntensity.OFF,
                                        onCheckedChange = { checked ->
                                            currentWeatherHaptics = checked
                                            expressHaptics.controller?.weatherHapticsEnabled = checked
                                            expressHaptics.toggle(checked)
                                            onToggleWeatherHaptics(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    if (currentHapticIntensity != HapticIntensity.OFF) {
                                        val next = !currentWeatherHaptics
                                        currentWeatherHaptics = next
                                        expressHaptics.controller?.weatherHapticsEnabled = next
                                        expressHaptics.toggle(next)
                                        onToggleWeatherHaptics(next)
                                    }
                                }
                            )

                            // 11. ハプティクス体験・端末最適化診断
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "ハプティクス体験 & 端末診断"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "触感体验与硬件优化诊断"
                                    AppLanguage.TRADITIONAL_CHINESE -> "觸感體驗與硬體最佳化診斷"
                                    AppLanguage.ENGLISH -> "Haptics Studio & Hardware Profile"
                                },
                                subtitle = expressHaptics.controller?.capabilities?.profile?.getLabel(currentLang)
                                    ?: when (currentLang) {
                                        AppLanguage.JAPANESE -> "最適化プロファイルを確認・テスト"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "查看设备优化配置并试震各项触感"
                                        AppLanguage.TRADITIONAL_CHINESE -> "檢視裝置最佳化設定並測試各項觸感"
                                        AppLanguage.ENGLISH -> "Inspect device calibration & test patterns"
                                    },
                                icon = Icons.Rounded.AutoAwesome,
                                iconBackgroundColor = colorScheme.tertiaryContainer,
                                iconTintColor = colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = { showHapticShowcaseDialog = true }
                            )

                            // 12. アプリ情報 (アプリ名、ロゴ、バージョン名)
                            Android15SettingsCard(
                                title = strings.aboutTitle,
                                subtitle = "Expressive Weather • Ver $versionName",
                                leadingContent = {
                                    if (appIconBitmap != null) {
                                        Box(
                                            modifier = Modifier.size(40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = appIconBitmap,
                                                contentDescription = "Expressive Weather",
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Info,
                                                contentDescription = null,
                                                tint = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                },
                                position = ConnectedCardPosition.Bottom,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = onOpenAppInfo
                            )
                        }
            }

            // ================= 3. 情報ソースとデータ管理 (Data & Storage) =================
            val renderSourcesSection: @Composable () -> Unit = {
                Android15CardGroup {
                            // 1. 雨雲レーダー提供元
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "雨雲レーダー提供元"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达图源"
                                    AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達圖資來源"
                                    AppLanguage.ENGLISH -> "Precipitation Radar Source"
                                },
                                subtitle = currentRadarSourceMode.title(currentLang),
                                icon = Icons.Rounded.Radar,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Top,
                                onClick = { showRadarSourceDialog = true }
                            )

                            // 2. 気象データ提供元
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "気象データ提供元"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "气象数据来源"
                                    AppLanguage.TRADITIONAL_CHINESE -> "氣象資料來源"
                                    AppLanguage.ENGLISH -> strings.dataSourcesTitle
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "気象庁 (JMA) / Open-Meteo"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) / Open-Meteo 全球预报"
                                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) / Open-Meteo 全球預報"
                                    AppLanguage.ENGLISH -> "JMA (Japan) & Open-Meteo (Global)"
                                },
                                icon = Icons.Rounded.CloudQueue,
                                iconBackgroundColor = colorScheme.surfaceContainerHighest,
                                iconTintColor = colorScheme.onSurfaceVariant,
                                position = ConnectedCardPosition.Middle,
                                onClick = { showDataProviderDialog = true }
                            )

                            // 3. GDACS global alerts (opt-in; disabled by default)
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "GDACS 全球災害警報"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "GDACS 全球灾害预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "GDACS 全球災害預警"
                                    AppLanguage.ENGLISH -> "GDACS Global Disaster Alerts"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "重大な全球災害警報を取得して表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "获取并显示重大全球灾害预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "擷取並顯示重大全球災害預警"
                                    AppLanguage.ENGLISH -> "Fetch and display major global disaster alerts"
                                },
                                icon = Icons.Rounded.Public,
                                iconBackgroundColor = colorScheme.tertiaryContainer,
                                iconTintColor = colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentGdacsAlerts,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentGdacsAlerts = checked
                                            onToggleGdacsAlerts(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentGdacsAlerts
                                    expressHaptics.toggle(next)
                                    currentGdacsAlerts = next
                                    onToggleGdacsAlerts(next)
                                }
                            )

                            // 4. MeteoAlarm 欧州公式気象警報（欧州専用）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "MeteoAlarm 欧州公式気象警報"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "MeteoAlarm 欧洲官方气象预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "MeteoAlarm 歐洲官方氣象警報"
                                    AppLanguage.ENGLISH -> "MeteoAlarm Official European Alerts"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "EUMETNET加盟の欧州39カ国の国立気象機関による公式警報（最優先取得）"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "获取EUMETNET欧洲39国国家气象机构的官方预警（最优先）"
                                    AppLanguage.TRADITIONAL_CHINESE -> "擷取EUMETNET歐洲39國國家氣象機構的官方警報（最優先）"
                                    AppLanguage.ENGLISH -> "Official weather warnings from 39 European national met services (EUMETNET - highest priority)"
                                },
                                icon = Icons.Rounded.Warning,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentMeteoAlarmAlerts,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentMeteoAlarmAlerts = checked
                                            onToggleMeteoAlarmAlerts(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentMeteoAlarmAlerts
                                    expressHaptics.toggle(next)
                                    currentMeteoAlarmAlerts = next
                                    onToggleMeteoAlarmAlerts(next)
                                }
                            )

                            // 5. WMO公式気象警報（海外専用）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "WMO公式気象警報（海外専用）"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "WMO官方气象预警（海外专用）"
                                    AppLanguage.TRADITIONAL_CHINESE -> "WMO官方氣象預警（海外專用）"
                                    AppLanguage.ENGLISH -> "WMO Official Alerts (International)"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "世界190カ国以上の国家気象警報を取得（日本・米国・MeteoAlarm対象国除く）"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "获取全球190多个国家官方预警（日本/美国/MeteoAlarm覆盖国除外）"
                                    AppLanguage.TRADITIONAL_CHINESE -> "擷取全球190多個國家官方預警（日本/美國/MeteoAlarm覆蓋國除外）"
                                    AppLanguage.ENGLISH -> "Official weather alerts across 190+ countries (excl. JP, US & MeteoAlarm)"
                                },
                                icon = Icons.Rounded.Public,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentWmoAlerts,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentWmoAlerts = checked
                                            onToggleWmoAlerts(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentWmoAlerts
                                    expressHaptics.toggle(next)
                                    currentWmoAlerts = next
                                    onToggleWmoAlerts(next)
                                }
                            )

                            // 5. 常にWMOを有効化（日本・米国・MeteoAlarm対象国でも有効化）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "日本・米国・MeteoAlarm対象国でも常にWMOを有効化"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "日本/美国/MeteoAlarm覆盖国也始终启用WMO预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "日本/美國/MeteoAlarm覆蓋國也始終啟用WMO預警"
                                    AppLanguage.ENGLISH -> "Always Enable WMO (including JP, US & MeteoAlarm)"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "直接取得（気象庁/NWS/MeteoAlarm）に加えWMO警報も並行取得"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在直接获取（气象厅/NWS/MeteoAlarm）的基础上同步获取WMO预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在直接擷取（氣象廳/NWS/MeteoAlarm）的基礎上同步擷取WMO預警"
                                    AppLanguage.ENGLISH -> "Fetch WMO alerts in addition to JMA / NWS / MeteoAlarm direct data"
                                },
                                icon = Icons.Rounded.Sync,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentWmoAlways,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentWmoAlways = checked
                                            onToggleWmoAlways(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentWmoAlways
                                    expressHaptics.toggle(next)
                                    currentWmoAlways = next
                                    onToggleWmoAlways(next)
                                }
                            )

                            // 6. 天候データを今すぐ更新
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "天候データを今すぐ更新"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "立即刷新天气数据"
                                    AppLanguage.TRADITIONAL_CHINESE -> "立即重新整理天氣資料"
                                    AppLanguage.ENGLISH -> "Refresh Weather Data"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "最新の気象情報をネットワークから再取得"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "从网络重新获取最新的气象信息"
                                    AppLanguage.TRADITIONAL_CHINESE -> "從網路重新獲取最新的氣象資訊"
                                    AppLanguage.ENGLISH -> "Fetch latest weather data from network"
                                },
                                icon = Icons.Rounded.Refresh,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                onClick = {
                                    onForceRefresh?.invoke()
                                    Toast.makeText(
                                        context,
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "気象データを更新中..."
                                            AppLanguage.SIMPLIFIED_CHINESE -> "正在刷新天气数据..."
                                            AppLanguage.TRADITIONAL_CHINESE -> "正在重新整理天氣資料..."
                                            AppLanguage.ENGLISH -> "Refreshing weather data..."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            // 4. キャッシュの消去
                            Android15SettingsCard(
                                title = strings.clearCacheTitle,
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "一時保存されたキャッシュデータをクリア"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "清除本地临时缓存的天气与位置数据"
                                    AppLanguage.TRADITIONAL_CHINESE -> "清除本機暫存的天氣與位置資料"
                                    AppLanguage.ENGLISH -> "Clear cached weather and location data"
                                },
                                icon = Icons.Rounded.DeleteSweep,
                                iconBackgroundColor = colorScheme.errorContainer.copy(alpha = 0.7f),
                                iconTintColor = colorScheme.onErrorContainer,
                                position = ConnectedCardPosition.Bottom,
                                onClick = { showClearCacheDialog = true }
                            )
                }
            }

            // ================= 4. デバッグ・シミュレーション (Debug & Simulation) =================
            val renderDebugSection: @Composable () -> Unit = {
                Android15CardGroup {
                            // 0. デバッグ設定の有効化状態（オフにすると非表示化）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "デバッグ設定を有効化"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "启用调试设置"
                                    AppLanguage.TRADITIONAL_CHINESE -> "啟用偵錯設定"
                                    AppLanguage.ENGLISH -> "Debug Settings Enabled"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "オフにすると非表示に戻り、再有効化にはバージョン名の連続7回タップが必要です"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭后将重新隐藏，再次启用需连续点击版本号7次"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉後將重新隱藏，再次啟用需連續點擊版本號7次"
                                    AppLanguage.ENGLISH -> "Turn off to hide debug settings until 7 rapid taps"
                                },
                                icon = Icons.Rounded.BugReport,
                                iconBackgroundColor = colorScheme.tertiaryContainer,
                                iconTintColor = colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Top,
                                trailing = {
                                    Switch(
                                        checked = isDebugUnlocked,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            if (!checked) {
                                                isDebugUnlocked = false
                                                onClearPrecipitationDemo()
                                                prefs.edit().putBoolean("debug_settings_unlocked", false).apply()
                                                Toast.makeText(
                                                    context,
                                                    when (currentLang) {
                                                        AppLanguage.JAPANESE -> "デバッグ設定を非表示にしました"
                                                        AppLanguage.SIMPLIFIED_CHINESE -> "已隐藏调试设置"
                                                        AppLanguage.TRADITIONAL_CHINESE -> "已隱藏偵錯設定"
                                                        AppLanguage.ENGLISH -> "Debug settings hidden"
                                                    },
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                },
                                onClick = {
                                    isDebugUnlocked = false
                                    onClearPrecipitationDemo()
                                    prefs.edit().putBoolean("debug_settings_unlocked", false).apply()
                                    Toast.makeText(
                                        context,
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "デバッグ設定を非表示にしました"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "已隐藏调试设置"
                                            AppLanguage.TRADITIONAL_CHINESE -> "已隱藏偵錯設定"
                                            AppLanguage.ENGLISH -> "Debug settings hidden"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            // 1. 天候シミュレーション
                            Android15SettingsCard(
                                title = strings.debugSimulationTitle,
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "雨・雪・雷雨などの天候エフェクトを手動テスト"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "手动预览雨、雪、雷暴等天气动画效果"
                                    AppLanguage.TRADITIONAL_CHINESE -> "手動預覽雨、雪、雷雨等天氣動畫效果"
                                    AppLanguage.ENGLISH -> "Manually preview rain, snow, storm effects"
                                },
                                icon = Icons.Rounded.AutoFixHigh,
                                iconBackgroundColor = colorScheme.tertiaryContainer,
                                iconTintColor = colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Middle,
                                onClick = { showSimulationDialog = true }
                            )

                            // 2. 初期セットアップ画面を再表示 (デバッグ設定)
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "初期セットアップ画面を再表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "重新显示初始设置界面"
                                    AppLanguage.TRADITIONAL_CHINESE -> "重新顯示初始設定介面"
                                    AppLanguage.ENGLISH -> "Reopen Setup Screen"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "初回起動時の「ようこそ」画面と登録フローを体験"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "体验初次启动时的欢迎界面与城市设置"
                                    AppLanguage.TRADITIONAL_CHINESE -> "體驗初次啟動時的歡迎介面與城市設定"
                                    AppLanguage.ENGLISH -> "Experience welcome onboarding and location setup"
                                },
                                icon = Icons.Rounded.Celebration,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    val intent = android.content.Intent(context, SetupActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )

                            // 2. 雨雲レーダー ズーム倍率表示 (デバッグ設定・デフォルト: オフ)
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "レーダー ズーム倍率表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "雷达缩放比例显示"
                                    AppLanguage.TRADITIONAL_CHINESE -> "雷達縮放倍率顯示"
                                    AppLanguage.ENGLISH -> "Radar Zoom Level Indicator"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "雨雲レーダー上に現在のズーム倍率バッジを表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在降水雷达地图上显示当前缩放级别角标"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在降雨雷達地圖上顯示當前縮放倍率角標"
                                    AppLanguage.ENGLISH -> "Show real-time zoom level badge on radar map"
                                },
                                icon = Icons.Rounded.ZoomIn,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentRadarDebugZoom,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentRadarDebugZoom = checked
                                            onToggleRadarDebugZoom(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentRadarDebugZoom
                                    expressHaptics.toggle(next)
                                    currentRadarDebugZoom = next
                                    onToggleRadarDebugZoom(next)
                                }
                            )

                            // 3. 雨雲レーダー 出典名表示 (デバッグ設定・デフォルト: オフ)
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "レーダー 出典クレジット表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "雷达图资来源角标"
                                    AppLanguage.TRADITIONAL_CHINESE -> "雷達圖資來源標籤"
                                    AppLanguage.ENGLISH -> "Radar Source Credit Badge"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "メイン画面の雨雲レーダー右下に出典名を表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在主屏雷达卡片右下方显示图资提供方信息"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在主螢幕雷達卡片右下方顯示圖資提供者資訊"
                                    AppLanguage.ENGLISH -> "Show source provider (JMA / NEXRAD / RainViewer) badge on main card"
                                },
                                icon = Icons.Rounded.Layers,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentRadarDebugSource,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentRadarDebugSource = checked
                                            onToggleRadarDebugSource(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentRadarDebugSource
                                    expressHaptics.toggle(next)
                                    currentRadarDebugSource = next
                                    onToggleRadarDebugSource(next)
                                }
                            )

                            // 4. すべての日本向けマップを表示 (デバッグ設定・デフォルト: オフ)
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "すべての日本向けマップを表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "显示所有日本气象地图"
                                    AppLanguage.TRADITIONAL_CHINESE -> "顯示所有日本氣象地圖"
                                    AppLanguage.ENGLISH -> "Show All Japan Radar Layers"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "雨雲・雷・キキクル・雪以外の追加レイヤー（ひまわり・天気・今後の雨など）をレーダーに表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在雷达中显示雨云・雷电・危险度・降雪以外的附加图层（向日葵卫星・天气预报等）"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在雷達中顯示雨雲・雷電・危險度・降雪以外的附加圖層（向日葵衛星・天氣預報等）"
                                    AppLanguage.ENGLISH -> "Display additional layers (Himawari satellite, weather dist, expected rain, etc.) on radar"
                                },
                                icon = Icons.Rounded.Layers,
                                iconBackgroundColor = colorScheme.tertiaryContainer,
                                iconTintColor = colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentShowAllJapanRadarLayers,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            currentShowAllJapanRadarLayers = checked
                                            onToggleShowAllJapanRadarLayers(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentShowAllJapanRadarLayers
                                    expressHaptics.toggle(next)
                                    currentShowAllJapanRadarLayers = next
                                    onToggleShowAllJapanRadarLayers(next)
                                }
                            )

                            // 5. キキクル・各種災害警報・雨雲カウントダウンテスト（リリースビルドでの誤発報防止のため DEBUG 限定）
                            if (BuildConfig.DEBUG) {
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "キキクルをテストする"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "测试危险度分布"
                                    AppLanguage.TRADITIONAL_CHINESE -> "測試危險度分佈"
                                    AppLanguage.ENGLISH -> "Test Kikikuru"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "メイン画面にキキクル警戒レベルカードをテスト表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在主界面测试显示危险度分布预警卡片"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在主畫面測試顯示危險度分佈預警卡片"
                                    AppLanguage.ENGLISH -> "Display Kikikuru alert card on main screen for testing"
                                },
                                icon = Icons.Rounded.Warning,
                                iconBackgroundColor = colorScheme.errorContainer,
                                iconTintColor = colorScheme.onErrorContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentKikikuruTest,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    val next = !currentKikikuruTest
                                    currentKikikuruTest = next
                                    SettingsDataHolder.isKikikuruTestEnabled = next
                                    onToggleKikikuruTest(next)
                                    if (next) {
                                        KikikuruNotificationManager.sendTestNotification(context, AlertLevel.LEVEL_2)
                                        Toast.makeText(
                                            context,
                                            when (currentLang) {
                                                AppLanguage.JAPANESE -> "キキクルテストをONにしました（メイン画面にカードを表示中）"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "已开启危险度分布测试（主界面显示预警卡片）"
                                                AppLanguage.TRADITIONAL_CHINESE -> "已開啟危險度分佈測試（主畫面顯示預警卡片）"
                                                AppLanguage.ENGLISH -> "Kikikuru test enabled (showing card on main screen)"
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        KikikuruNotificationManager.cancelAll(context)
                                        Toast.makeText(
                                            context,
                                            when (currentLang) {
                                                AppLanguage.JAPANESE -> "キキクルテストをOFFにしました"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "已关闭危险度分布测试"
                                                AppLanguage.TRADITIONAL_CHINESE -> "已關閉危險度分佈測試"
                                                AppLanguage.ENGLISH -> "Kikikuru test disabled"
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )

                            // 5. NWS (米国気象局) 警報テスト
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "NWS 米国気象警報テスト"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "测试美国NWS气象预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "測試美國NWS氣象預警"
                                    AppLanguage.ENGLISH -> "Test NWS US Weather Alert"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "メイン画面に竜巻警報・鉄砲水注意報カードをテスト表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在主屏测试显示龙卷风警报与山洪预警卡片"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在主螢幕測試顯示龍捲風警報與暴洪預警卡片"
                                    AppLanguage.ENGLISH -> "Display Tornado Warning & Flash Flood Watch on main screen"
                                },
                                icon = Icons.Rounded.CrisisAlert,
                                iconBackgroundColor = colorScheme.errorContainer,
                                iconTintColor = colorScheme.onErrorContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentNwsTest,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    val next = !currentNwsTest
                                    currentNwsTest = next
                                    SettingsDataHolder.isNwsTestEnabled = next
                                    onToggleNwsTest(next)
                                    Toast.makeText(
                                        context,
                                        if (next) "NWSテスト警報をONにしました" else "NWSテスト警報をOFFにしました",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            // 6. GDACS (全球自然災害警報) テスト
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "GDACS 全球自然災害警報テスト"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "测试GDACS全球灾害预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "測試GDACS全球災害預警"
                                    AppLanguage.ENGLISH -> "Test GDACS Global Disaster Alert"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "メイン画面にスーパータイフーン・大地震カードをテスト表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在主屏测试显示超级台风与重大地震警报卡片"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在主螢幕測試顯示超級颱風與重大地震警報卡片"
                                    AppLanguage.ENGLISH -> "Display Super Typhoon & Major Earthquake on main screen"
                                },
                                icon = Icons.Rounded.Public,
                                iconBackgroundColor = colorScheme.tertiaryContainer,
                                iconTintColor = colorScheme.onTertiaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentGdacsTest,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    val next = !currentGdacsTest
                                    currentGdacsTest = next
                                    SettingsDataHolder.isGdacsTestEnabled = next
                                    onToggleGdacsTest(next)
                                    Toast.makeText(
                                        context,
                                        if (next) "GDACSテスト警報をONにしました" else "GDACSテスト警報をOFFにしました",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            // 7. WMO (世界気象警報) テスト
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "WMO 世界気象警報テスト"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "测试WMO世界气象警报"
                                    AppLanguage.TRADITIONAL_CHINESE -> "測試WMO世界氣象警報"
                                    AppLanguage.ENGLISH -> "Test WMO Weather Alerts"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "特別警戒(赤)・警戒(橙)・注意(黄)のWMO警報カードをテスト表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在主屏测试显示特别警戒(红)/警戒(橙)/注意(黄)WMO预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在主螢幕測試顯示特別警戒(紅)/警戒(橙)/注意(黃)WMO預警"
                                    AppLanguage.ENGLISH -> "Display sample WMO alerts with Red, Orange, Yellow severity"
                                },
                                icon = Icons.Rounded.Public,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentWmoTest,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    val next = !currentWmoTest
                                    currentWmoTest = next
                                    SettingsDataHolder.isWmoTestEnabled = next
                                    onToggleWmoTest(next)
                                    Toast.makeText(
                                        context,
                                        if (next) "WMOテスト警報をONにしました" else "WMOテスト警報をOFFにしました",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            // 8. MeteoAlarm (欧州気象警報) テスト
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "MeteoAlarm 欧州気象警報テスト"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "测试MeteoAlarm欧洲气象预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "測試MeteoAlarm歐洲氣象警報"
                                    AppLanguage.ENGLISH -> "Test MeteoAlarm European Alerts"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "特別警戒(雪崩)・警戒(暴風)・注意(大雨)の欧州警報カードをテスト表示"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "在主屏测试显示特别警戒(雪崩)/警戒(大风)/注意(暴雨)欧洲预警"
                                    AppLanguage.TRADITIONAL_CHINESE -> "在主螢幕測試顯示特別警戒(雪崩)/警戒(大風)/注意(暴雨)歐洲警報"
                                    AppLanguage.ENGLISH -> "Display sample European alerts (Avalanche, Wind, Rain)"
                                },
                                icon = Icons.Rounded.Warning,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = currentMeteoAlarmTest,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    val next = !currentMeteoAlarmTest
                                    currentMeteoAlarmTest = next
                                    SettingsDataHolder.isMeteoAlarmTestEnabled = next
                                    onToggleMeteoAlarmTest(next)
                                    Toast.makeText(
                                        context,
                                        if (next) "MeteoAlarmテスト警報をONにしました" else "MeteoAlarmテスト警報をOFFにしました",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            // 9. 雨雲接近・降雨LiveUpdatesテスト
                            val demoStateLabel = when (precipitationDemoStatus) {
                                RainStatus.RAIN_EXPECTED_TONIGHT -> when (currentLang) {
                                    AppLanguage.JAPANESE -> "ON: 今夜の雨 (21時頃・80%)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "开启: 今夜有雨 (21:00・80%)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "開啟: 今夜有雨 (21:00・80%)"
                                    AppLanguage.ENGLISH -> "ON: Tonight's rain (21:00, 80%)"
                                }
                                RainStatus.RAIN_APPROACHING -> when (currentLang) {
                                    AppLanguage.JAPANESE -> "ON: 接近中 (あと20分)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "开启: 接近中 (20分钟后)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "開啟: 接近中 (20分鐘後)"
                                    AppLanguage.ENGLISH -> "ON: Approaching (20 min)"
                                }
                                RainStatus.RAINING -> when (currentLang) {
                                    AppLanguage.JAPANESE -> "ON: 降雨中 (あと35分)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "开启: 降雨中 (剩35分钟)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "開啟: 降雨中 (剩35分鐘)"
                                    AppLanguage.ENGLISH -> "ON: Raining (35 min)"
                                }
                                else -> when (currentLang) {
                                    AppLanguage.JAPANESE -> "OFF (実データ表示)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭 (实际数据)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉 (實際數據)"
                                    AppLanguage.ENGLISH -> "OFF (Actual data)"
                                }
                            }
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "雨雲LiveUpdates・カウントダウンテスト"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "降雨实时动态与倒计时测试"
                                    AppLanguage.TRADITIONAL_CHINESE -> "降雨即時動態與倒數計時測試"
                                    AppLanguage.ENGLISH -> "Test Rain Live Updates Countdown"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "現在: $demoStateLabel / タップで切替 (今夜の雨 → 接近中 → 降雨中 → 実データ)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "当前: $demoStateLabel / 点击切换 (今夜有雨 → 接近中 → 降雨中 → 实际数据)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "目前: $demoStateLabel / 點擊切換 (今夜有雨 → 接近中 → 降雨中 → 實際數據)"
                                    AppLanguage.ENGLISH -> "Current: $demoStateLabel / Tap to cycle"
                                },
                                icon = Icons.Rounded.WaterDrop,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Middle,
                                trailing = {
                                    Switch(
                                        checked = precipitationDemoStatus != null,
                                        onCheckedChange = { checked ->
                                            expressHaptics.toggle(checked)
                                            if (checked) {
                                                onCyclePrecipitationDemo()
                                            } else {
                                                onClearPrecipitationDemo()
                                                Toast.makeText(
                                                    context,
                                                    when (currentLang) {
                                                        AppLanguage.JAPANESE -> "雨雲予測テストをOFFにしました（実データ表示）"
                                                        AppLanguage.SIMPLIFIED_CHINESE -> "已关闭降雨预测测试（显示实际数据）"
                                                        AppLanguage.TRADITIONAL_CHINESE -> "已關閉降雨預測測試（顯示實際數據）"
                                                        AppLanguage.ENGLISH -> "Rain nowcast test OFF (showing actual data)"
                                                    },
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                },
                                onClick = {
                                    val nextStatus = when (precipitationDemoStatus) {
                                        null -> RainStatus.RAIN_EXPECTED_TONIGHT
                                        RainStatus.RAIN_EXPECTED_TONIGHT -> RainStatus.RAIN_APPROACHING
                                        RainStatus.RAIN_APPROACHING -> RainStatus.RAINING
                                        RainStatus.RAINING -> null
                                        else -> null
                                    }
                                    onCyclePrecipitationDemo()
                                    val toastMsg = when (nextStatus) {
                                        RainStatus.RAIN_EXPECTED_TONIGHT -> when (currentLang) {
                                            AppLanguage.JAPANESE -> "テスト表示: 今夜の雨 (21時頃・80%)"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "测试预览: 今夜有雨 (21:00・80%)"
                                            AppLanguage.TRADITIONAL_CHINESE -> "測試預覽: 今夜有雨 (21:00・80%)"
                                            AppLanguage.ENGLISH -> "Test preview: Tonight's rain (21:00, 80%)"
                                        }
                                        RainStatus.RAIN_APPROACHING -> when (currentLang) {
                                            AppLanguage.JAPANESE -> "テスト表示: 雨雲接近中 (あと20分)"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "测试预览: 降雨接近中 (20分钟后)"
                                            AppLanguage.TRADITIONAL_CHINESE -> "測試預覽: 降雨接近中 (20分鐘後)"
                                            AppLanguage.ENGLISH -> "Test preview: Rain approaching (20 min)"
                                        }
                                        RainStatus.RAINING -> when (currentLang) {
                                            AppLanguage.JAPANESE -> "テスト表示: 降雨中 (あと35分)"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "测试预览: 降雨中 (剩35分钟)"
                                            AppLanguage.TRADITIONAL_CHINESE -> "測試預覽: 降雨中 (剩35分鐘)"
                                            AppLanguage.ENGLISH -> "Test preview: Raining (35 min)"
                                        }
                                        else -> when (currentLang) {
                                            AppLanguage.JAPANESE -> "雨雲予測テストをOFFにしました（実データ表示）"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "已关闭降雨预测测试（显示实际数据）"
                                            AppLanguage.TRADITIONAL_CHINESE -> "已關閉降雨預測測試（顯示實際數據）"
                                            AppLanguage.ENGLISH -> "Rain nowcast test OFF (showing actual data)"
                                        }
                                    }
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            )
                            } // end if (BuildConfig.DEBUG)

                            // 6. 気象警報・キキクル警戒レベルカードUI プレビュー
                            var showCardsPreview by remember { mutableStateOf(true) }
                            Android15SettingsCard(
                                title = "気象警報・警戒レベルカードUIプレビュー",
                                subtitle = if (showCardsPreview) "タップして折りたたむ" else "タップして全カードを表示",
                                icon = Icons.Rounded.NotificationsActive,
                                iconBackgroundColor = colorScheme.secondaryContainer,
                                iconTintColor = colorScheme.onSecondaryContainer,
                                position = ConnectedCardPosition.Bottom,
                                onClick = { showCardsPreview = !showCardsPreview }
                            )

                            AnimatedVisibility(
                                visible = showCardsPreview,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val previewTitle = when (currentLang) {
                                        AppLanguage.ENGLISH -> "◆ Kikikuru Alert Level Cards (12SidedShape)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "◆ 气象厅警戒等级卡片 (12SidedShape)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "◆ 氣象廳警戒等級卡片 (12SidedShape)"
                                        AppLanguage.JAPANESE -> "◆ キキクル警戒レベルカード (12SidedShape)"
                                    }
                                    Text(
                                        text = previewTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    io.github.nemynew.expressiveweather.ui.components.KikikuruAlertCard(
                                        alertLevel = AlertLevel.LEVEL_5,
                                        language = currentLang
                                    )
                                    io.github.nemynew.expressiveweather.ui.components.KikikuruAlertCard(
                                        alertLevel = AlertLevel.LEVEL_4,
                                        language = currentLang
                                    )
                                    io.github.nemynew.expressiveweather.ui.components.KikikuruAlertCard(
                                        alertLevel = AlertLevel.LEVEL_3,
                                        language = currentLang
                                    )
                                    io.github.nemynew.expressiveweather.ui.components.KikikuruAlertCard(
                                        alertLevel = AlertLevel.LEVEL_2,
                                        language = currentLang
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "◆ 気象警報・注意報カード",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    io.github.nemynew.expressiveweather.ui.components.WeatherWarningCard(
                                        type = io.github.nemynew.expressiveweather.ui.components.WeatherWarningType.WARNING,
                                        title = "大雨警報・洪水警報",
                                        description = "大雨および河川の増水・氾濫に警戒してください。"
                                    )
                                    io.github.nemynew.expressiveweather.ui.components.WeatherWarningCard(
                                        type = io.github.nemynew.expressiveweather.ui.components.WeatherWarningType.SPECIAL_WARNING,
                                        title = "Severe Weather Warning",
                                        description = "Flash flooding and dangerous conditions are expected."
                                    )
                                    io.github.nemynew.expressiveweather.ui.components.WeatherWarningCard(
                                        type = io.github.nemynew.expressiveweather.ui.components.WeatherWarningType.ADVISORY,
                                        title = "雷注意報・強風注意報",
                                        description = "急な強い雨や落雷、突風に注意してください。"
                                    )
                                }
                            }
                }
            }

            var selectedCategoryName by rememberSaveable(initialSection) {
                mutableStateOf(
                    when (initialSection) {
                        SettingsDataHolder.SECTION_GENERAL,
                        SettingsDataHolder.SECTION_LANGUAGE,
                        "language" -> SettingsPaneCategory.GENERAL.name
                        SettingsDataHolder.SECTION_SOURCES -> SettingsPaneCategory.SOURCES.name
                        "app_info" -> SettingsPaneCategory.APP_INFO.name
                        SettingsDataHolder.SECTION_DEBUG -> SettingsPaneCategory.DEBUG.name
                        else -> SettingsPaneCategory.APPEARANCE.name
                    }
                )
            }
            val selectedCategory = SettingsPaneCategory.entries
                .firstOrNull { it.name == selectedCategoryName }
                ?.takeIf { it != SettingsPaneCategory.DEBUG || isDebugUnlocked }
                ?: SettingsPaneCategory.APPEARANCE

            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val isTwoPane = maxWidth >= 600.dp

                BackHandler(
                    enabled = if (isTwoPane) currentSubPage == "language" else currentSubPage != null
                ) {
                    handleSubPageBack()
                }

                if (isTwoPane) {
                    val visibleCategories = remember(isDebugUnlocked) {
                        buildList {
                            add(SettingsPaneCategory.APPEARANCE)
                            add(SettingsPaneCategory.GENERAL)
                            add(SettingsPaneCategory.SOURCES)
                            add(SettingsPaneCategory.APP_INFO)
                            if (isDebugUnlocked) add(SettingsPaneCategory.DEBUG)
                        }
                    }
                    val leftPaneWidth = (maxWidth * 0.34f).coerceIn(240.dp, 340.dp)
                    val leftScrollState = rememberScrollState()
                    val rightScrollState = rememberScrollState()

                    LaunchedEffect(selectedCategory.name) {
                        rightScrollState.scrollTo(0)
                    }

                    val categoryTitle: (SettingsPaneCategory) -> String = { category ->
                        when (category) {
                            SettingsPaneCategory.APPEARANCE -> strings.appearanceTitle
                            SettingsPaneCategory.GENERAL -> strings.generalSettingsTitle
                            SettingsPaneCategory.SOURCES -> strings.dataSourcesTitle
                            SettingsPaneCategory.APP_INFO -> strings.aboutTitle
                            SettingsPaneCategory.DEBUG -> strings.debugSettingsTitle
                        }
                    }
                    val categorySubtitle: (SettingsPaneCategory) -> String = { category ->
                        when (category) {
                            SettingsPaneCategory.APPEARANCE -> when (currentLang) {
                                AppLanguage.JAPANESE -> "テーマ・ガラス表現・図形"
                                AppLanguage.SIMPLIFIED_CHINESE -> "主题配色与视觉效果"
                                AppLanguage.TRADITIONAL_CHINESE -> "主題配色與視覺效果"
                                AppLanguage.ENGLISH -> "Theme, Glass & Shapes"
                            }
                            SettingsPaneCategory.GENERAL -> "${currentLang.displayName} • ${if (currentUnit == TemperatureUnit.CELSIUS) "°C" else "°F"}"
                            SettingsPaneCategory.SOURCES -> when (currentLang) {
                                AppLanguage.JAPANESE -> "レーダー・世界の警報・キャッシュ"
                                AppLanguage.SIMPLIFIED_CHINESE -> "雷达・全球警报・缓存管理"
                                AppLanguage.TRADITIONAL_CHINESE -> "雷達・全球警報・快取管理"
                                AppLanguage.ENGLISH -> "Radar, Global Alerts & Cache"
                            }
                            SettingsPaneCategory.APP_INFO -> "Ver $versionName"
                            SettingsPaneCategory.DEBUG -> when (currentLang) {
                                AppLanguage.JAPANESE -> "シミュレーション・UI確認"
                                AppLanguage.SIMPLIFIED_CHINESE -> "模拟测试与界面预览"
                                AppLanguage.TRADITIONAL_CHINESE -> "模擬測試與介面預覽"
                                AppLanguage.ENGLISH -> "Simulation & Previews"
                            }
                        }
                    }
                    val categoryIcon: (SettingsPaneCategory) -> androidx.compose.ui.graphics.vector.ImageVector = { category ->
                        when (category) {
                            SettingsPaneCategory.APPEARANCE -> Icons.Rounded.Palette
                            SettingsPaneCategory.GENERAL -> Icons.Rounded.Tune
                            SettingsPaneCategory.SOURCES -> Icons.Rounded.Storage
                            SettingsPaneCategory.APP_INFO -> Icons.Rounded.Info
                            SettingsPaneCategory.DEBUG -> Icons.Rounded.BugReport
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.surfaceContainer)
                    ) {
                        // ================= 左ペイン（親カテゴリ一覧） =================
                        Column(
                            modifier = Modifier
                                .width(leftPaneWidth)
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.pressScaleEffect()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = strings.close,
                                        tint = colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.settingsTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(leftScrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                visibleCategories.forEach { category ->
                                    val isSelected = category == selectedCategory
                                    Surface(
                                        onClick = {
                                            expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                                            selectedCategoryName = category.name
                                            currentSubPage = null
                                        },
                                        shape = RoundedCornerShape(22.dp),
                                        color = if (isSelected) colorScheme.primaryContainer else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) colorScheme.primary
                                                        else colorScheme.surfaceContainerHigh
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = categoryIcon(category),
                                                    contentDescription = null,
                                                    tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = categoryTitle(category),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = categorySubtitle(category),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ================= 右ペイン（選択カテゴリの詳細設定） =================
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(top = 12.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (selectedCategory == SettingsPaneCategory.GENERAL && currentSubPage == "language") {
                                    LanguageSelectionScreen(
                                        currentLanguage = currentLang,
                                        onSelectLanguage = { newLang ->
                                            currentLang = newLang
                                            SettingsDataHolder.language = newLang
                                            onSelectLanguage(newLang)
                                        },
                                        onBack = { currentSubPage = null },
                                        useWindowInsets = false
                                    )
                                } else if (selectedCategory == SettingsPaneCategory.APP_INFO) {
                                    AppInfoScreen(
                                        versionName = versionName,
                                        versionCode = versionCode,
                                        appIconBitmap = appIconBitmap,
                                        language = currentLang,
                                        isDebugUnlocked = isDebugUnlocked,
                                        onUnlockDebug = {
                                            isDebugUnlocked = true
                                            prefs.edit().putBoolean("debug_settings_unlocked", true).apply()
                                        },
                                        onLockDebug = {
                                            isDebugUnlocked = false
                                            prefs.edit().putBoolean("debug_settings_unlocked", false).apply()
                                        },
                                        onBack = { selectedCategoryName = SettingsPaneCategory.GENERAL.name },
                                        embeddedInPane = true
                                    )
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 18.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = categoryIcon(selectedCategory),
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = categoryTitle(selectedCategory),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .verticalScroll(rightScrollState)
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            when (selectedCategory) {
                                                SettingsPaneCategory.APPEARANCE -> renderAppearanceSection()
                                                SettingsPaneCategory.GENERAL -> renderGeneralSection(
                                                    { currentSubPage = "language" },
                                                    { selectedCategoryName = SettingsPaneCategory.APP_INFO.name }
                                                )
                                                SettingsPaneCategory.SOURCES -> renderSourcesSection()
                                                SettingsPaneCategory.DEBUG -> if (isDebugUnlocked) renderDebugSection()
                                                SettingsPaneCategory.APP_INFO -> Unit
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ================= スマホ / 折りたたみ閉時（従来の1ペイン表示） =================
                    if (currentSubPage == "app_info") {
                        AppInfoScreen(
                            versionName = versionName,
                            versionCode = versionCode,
                            appIconBitmap = appIconBitmap,
                            language = currentLang,
                            isDebugUnlocked = isDebugUnlocked,
                            onUnlockDebug = {
                                isDebugUnlocked = true
                                prefs.edit().putBoolean("debug_settings_unlocked", true).apply()
                            },
                            onLockDebug = {
                                isDebugUnlocked = false
                                prefs.edit().putBoolean("debug_settings_unlocked", false).apply()
                            },
                            onBack = handleSubPageBack
                        )
                    } else if (currentSubPage == "language") {
                        LanguageSelectionScreen(
                            currentLanguage = currentLang,
                            onSelectLanguage = { newLang ->
                                currentLang = newLang
                                SettingsDataHolder.language = newLang
                                onSelectLanguage(newLang)
                            },
                            onBack = handleSubPageBack
                        )
                    } else {
                        val screenTitle = when {
                            initialSection == SettingsDataHolder.SECTION_APPEARANCE -> strings.appearanceTitle
                            initialSection == SettingsDataHolder.SECTION_GENERAL -> strings.generalSettingsTitle
                            initialSection == SettingsDataHolder.SECTION_SOURCES -> strings.dataSourcesTitle
                            initialSection == SettingsDataHolder.SECTION_DEBUG && isDebugUnlocked -> strings.debugSettingsTitle
                            else -> strings.settingsTitle
                        }

                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            topBar = {
                                LargeTopAppBar(
                                    title = {
                                        Text(
                                            text = screenTitle,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = onBack,
                                            modifier = Modifier.pressScaleEffect()
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = strings.close,
                                                tint = colorScheme.onSurface
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = colorScheme.surface,
                                        scrolledContainerColor = colorScheme.surfaceContainer
                                    ),
                                    scrollBehavior = scrollBehavior
                                )
                            },
                            containerColor = colorScheme.surface
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding())
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (initialSection == SettingsDataHolder.SECTION_APPEARANCE || initialSection == SettingsDataHolder.SECTION_ALL) {
                                    renderAppearanceSection()
                                }
                                if (initialSection == SettingsDataHolder.SECTION_GENERAL || initialSection == SettingsDataHolder.SECTION_ALL) {
                                    renderGeneralSection(
                                        {
                                            val intent = Intent(context, LanguageSelectionActivity::class.java)
                                            context.startActivity(intent)
                                        },
                                        { currentSubPage = "app_info" }
                                    )
                                }
                                if (initialSection == SettingsDataHolder.SECTION_SOURCES || initialSection == SettingsDataHolder.SECTION_ALL) {
                                    renderSourcesSection()
                                }
                                if (isDebugUnlocked && (initialSection == SettingsDataHolder.SECTION_DEBUG || initialSection == SettingsDataHolder.SECTION_ALL)) {
                                    renderDebugSection()
                                }
                            }
                        }
                    }
                }
            }

            // ================= ダイアログ群 =================
            // 1. 温度単位ダイアログ
            if (showTempUnitDialog) {
                val celsiusText = when (currentLang) {
                    AppLanguage.JAPANESE -> "摂氏 (°C)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "摄氏度 (°C)"
                    AppLanguage.TRADITIONAL_CHINESE -> "攝氏度 (°C)"
                    AppLanguage.ENGLISH -> "Celsius (°C)"
                }
                val fahrenheitText = when (currentLang) {
                    AppLanguage.JAPANESE -> "華氏 (°F)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "华氏度 (°F)"
                    AppLanguage.TRADITIONAL_CHINESE -> "華氏度 (°F)"
                    AppLanguage.ENGLISH -> "Fahrenheit (°F)"
                }
                SingleChoiceDialog(
                    title = strings.tempUnitTitle,
                    options = listOf(
                        TemperatureUnit.CELSIUS to celsiusText,
                        TemperatureUnit.FAHRENHEIT to fahrenheitText
                    ),
                    selectedOption = currentUnit,
                    onOptionSelected = {
                        val previousUnit = currentUnit
                        currentUnit = it
                        if (it != previousUnit) {
                            onToggleTemperatureUnit()
                        }
                    },
                    onDismiss = { showTempUnitDialog = false },
                    cancelText = strings.cancel
                )
            }

            // 3. 風速単位ダイアログ
            if (showWindUnitDialog) {
                val followText = when (currentLang) {
                    AppLanguage.JAPANESE -> "言語に合わせる (推奨)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "跟随语言 (推荐)"
                    AppLanguage.TRADITIONAL_CHINESE -> "跟隨語言 (推薦)"
                    AppLanguage.ENGLISH -> "Match Language (Recommended)"
                }
                val msText = when (currentLang) {
                    AppLanguage.JAPANESE -> "メートル毎秒 (m/s)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "米/秒 (m/s)"
                    AppLanguage.TRADITIONAL_CHINESE -> "公尺/秒 (m/s)"
                    AppLanguage.ENGLISH -> "Meters per second (m/s)"
                }
                val kmhText = when (currentLang) {
                    AppLanguage.JAPANESE -> "キロメートル毎時 (km/h)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "公里/小时 (km/h)"
                    AppLanguage.TRADITIONAL_CHINESE -> "公里/小時 (km/h)"
                    AppLanguage.ENGLISH -> "Kilometers per hour (km/h)"
                }
                val mphText = when (currentLang) {
                    AppLanguage.JAPANESE -> "マイル毎時 (mph)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "英里/小时 (mph)"
                    AppLanguage.TRADITIONAL_CHINESE -> "英里/小時 (mph)"
                    AppLanguage.ENGLISH -> "Miles per hour (mph)"
                }
                SingleChoiceDialog(
                    title = strings.windSpeedUnitTitle,
                    options = listOf(
                        WindSpeedUnit.FOLLOW_LANGUAGE to followText,
                        WindSpeedUnit.MS to msText,
                        WindSpeedUnit.KMH to kmhText,
                        WindSpeedUnit.MPH to mphText
                    ),
                    selectedOption = currentWindUnit,
                    onOptionSelected = {
                        currentWindUnit = it
                        onSelectWindSpeedUnit(it)
                    },
                    onDismiss = { showWindUnitDialog = false },
                    cancelText = strings.cancel
                )
            }

            // 3b. ハプティクス強度ダイアログ
            if (showHapticIntensityDialog) {
                SingleChoiceDialog(
                    title = when (currentLang) {
                        AppLanguage.JAPANESE -> "ハプティクス強度"
                        AppLanguage.SIMPLIFIED_CHINESE -> "触觉反馈强度"
                        AppLanguage.TRADITIONAL_CHINESE -> "觸覺回饋強度"
                        AppLanguage.ENGLISH -> "Haptics Intensity"
                    },
                    options = HapticIntensity.entries.map { it to it.getLabel(currentLang) },
                    selectedOption = currentHapticIntensity,
                    onOptionSelected = { selected ->
                        currentHapticIntensity = selected
                        expressHaptics.controller?.intensity = selected
                        onSelectHapticIntensity(selected)
                        if (selected != HapticIntensity.OFF) {
                            expressHaptics.play(ExpressiveHapticPattern.SWITCH_ON, bypassThrottle = true)
                        }
                    },
                    onDismiss = { showHapticIntensityDialog = false },
                    cancelText = strings.cancel
                )
            }

            // 3c. ハプティクス体験・端末診断ダイアログ
            if (showHapticShowcaseDialog) {
                HapticShowcaseDialog(
                    language = currentLang,
                    currentIntensity = currentHapticIntensity,
                    capabilities = expressHaptics.controller?.capabilities,
                    onDismiss = { showHapticShowcaseDialog = false }
                )
            }

            // 3. 天気の自動更新頻度 選択ダイアログ
            if (showWeatherUpdateIntervalDialog) {
                SingleChoiceDialog(
                    title = when (currentLang) {
                        AppLanguage.JAPANESE -> "天気の自動更新頻度"
                        AppLanguage.SIMPLIFIED_CHINESE -> "天气自动刷新频率"
                        AppLanguage.TRADITIONAL_CHINESE -> "天氣自動重新整理頻率"
                        AppLanguage.ENGLISH -> "Weather Update Frequency"
                    },
                    options = WeatherUpdateInterval.entries.map { interval ->
                        interval to interval.getLabel(currentLang)
                    },
                    selectedOption = currentWeatherUpdateInterval,
                    onOptionSelected = {
                        currentWeatherUpdateInterval = it
                        onSelectWeatherUpdateInterval(it)
                        val msg = when (currentLang) {
                            AppLanguage.JAPANESE -> "天気の更新頻度を「${it.getLabel(currentLang)}」に設定しました"
                            AppLanguage.SIMPLIFIED_CHINESE -> "已将天气刷新频率设为「${it.getLabel(currentLang)}」"
                            AppLanguage.TRADITIONAL_CHINESE -> "已將天氣重新整理頻率設為「${it.getLabel(currentLang)}」"
                            AppLanguage.ENGLISH -> "Weather update frequency set to \"${it.getLabel(currentLang)}\""
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showWeatherUpdateIntervalDialog = false },
                    cancelText = strings.cancel
                )
            }

            // 警戒通知の重要度 選択ダイアログ
            if (showThresholdDialog) {
                val advisoryText = when (currentLang) {
                    AppLanguage.JAPANESE -> "注意報以上（レベル2以上）"
                    AppLanguage.SIMPLIFIED_CHINESE -> "注意报以上 (2级以上)"
                    AppLanguage.TRADITIONAL_CHINESE -> "注意報以上 (2級以上)"
                    AppLanguage.ENGLISH -> "Advisory & Above (Level 2+)"
                }
                val warningText = when (currentLang) {
                    AppLanguage.JAPANESE -> "警報以上（レベル3以上）"
                    AppLanguage.SIMPLIFIED_CHINESE -> "警报以上 (3级以上)"
                    AppLanguage.TRADITIONAL_CHINESE -> "警報以上 (3級以上)"
                    AppLanguage.ENGLISH -> "Warning & Above (Level 3+)"
                }
                val emergencyText = when (currentLang) {
                    AppLanguage.JAPANESE -> "特別警報・危険のみ（レベル4以上）"
                    AppLanguage.SIMPLIFIED_CHINESE -> "仅特别警报/危险 (4级以上)"
                    AppLanguage.TRADITIONAL_CHINESE -> "僅特別警報/危險 (4級以上)"
                    AppLanguage.ENGLISH -> "Emergency Only (Level 4+)"
                }
                val offText = when (currentLang) {
                    AppLanguage.JAPANESE -> "通知オフ"
                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭通知"
                    AppLanguage.TRADITIONAL_CHINESE -> "關閉通知"
                    AppLanguage.ENGLISH -> "Off"
                }

                SingleChoiceDialog(
                    title = when (currentLang) {
                        AppLanguage.JAPANESE -> "通知する警戒重要度"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预警通知重要度"
                        AppLanguage.TRADITIONAL_CHINESE -> "預警通知重要度"
                        AppLanguage.ENGLISH -> "Alert Notification Level"
                    },
                    options = listOf(
                        AlertNotificationThreshold.ADVISORY_AND_ABOVE to advisoryText,
                        AlertNotificationThreshold.WARNING_AND_ABOVE to warningText,
                        AlertNotificationThreshold.EMERGENCY_ONLY to emergencyText,
                        AlertNotificationThreshold.OFF to offText
                    ),
                    selectedOption = currentThreshold,
                    onOptionSelected = {
                        currentThreshold = it
                        onSelectAlertNotificationThreshold(it)
                        if (it == AlertNotificationThreshold.OFF) {
                            currentLiveAlertNotification = false
                            onToggleLiveAlertNotification(false)
                        } else {
                            if (!currentLiveAlertNotification) {
                                currentLiveAlertNotification = true
                                onToggleLiveAlertNotification(true)
                            }
                        }
                    },
                    onDismiss = { showThresholdDialog = false },
                    cancelText = strings.cancel
                )
            }

            // 4. データプロバイダーダイアログ
            if (showDataProviderDialog) {
                DataSourcesCreditsDialog(
                    language = currentLang,
                    onDismiss = { showDataProviderDialog = false }
                )
            }

            if (showDataSourcesDialog) {
                DataSourcesCreditsDialog(
                    language = currentLang,
                    onDismiss = { showDataSourcesDialog = false }
                )
            }

            if (showLicenseDialog) {
                OpenSourceLicenseDialog(
                    language = currentLang,
                    onDismiss = { showLicenseDialog = false }
                )
            }

            if (showBgLocationDisclosureDialog) {
                BackgroundLocationDisclosureDialog(
                    language = currentLang,
                    onConfirmProceed = {
                        showBgLocationDisclosureDialog = false
                        CurrentLocationProvider.openAppLocationSettings(context)
                    },
                    onDismiss = { showBgLocationDisclosureDialog = false }
                )
            }

            // 雨雲レーダー提供元 選択ダイアログ
            if (showRadarSourceDialog) {
                val radarSourceOptions = listOf(
                    RadarSourceMode.AUTO to RadarSourceMode.AUTO.title(currentLang),
                    RadarSourceMode.JMA to RadarSourceMode.JMA.title(currentLang),
                    RadarSourceMode.US_NEXRAD_HRRR to RadarSourceMode.US_NEXRAD_HRRR.title(currentLang),
                    RadarSourceMode.RAIN_VIEWER to RadarSourceMode.RAIN_VIEWER.title(currentLang)
                )
                SingleChoiceDialog(
                    title = when (currentLang) {
                        AppLanguage.JAPANESE -> "雨雲レーダー提供元"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达图源"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達圖資來源"
                        AppLanguage.ENGLISH -> "Precipitation Radar Source"
                    },
                    options = radarSourceOptions,
                    selectedOption = currentRadarSourceMode,
                    onOptionSelected = { mode ->
                        currentRadarSourceMode = mode
                        radarRepo.setSourceMode(mode)
                        showRadarSourceDialog = false
                    },
                    onDismiss = { showRadarSourceDialog = false },
                    cancelText = strings.cancel
                )
            }

            // 5. キャッシュ消去確認ダイアログ
            if (showClearCacheDialog) {
                ClearCacheConfirmationDialog(
                    title = strings.clearCacheTitle,
                    message = strings.clearCacheConfirmMessage,
                    confirmText = when (currentLang) {
                        AppLanguage.JAPANESE -> "消去"
                        AppLanguage.SIMPLIFIED_CHINESE -> "清除"
                        AppLanguage.TRADITIONAL_CHINESE -> "清除"
                        AppLanguage.ENGLISH -> "Clear"
                    },
                    cancelText = strings.cancel,
                    onConfirm = {
                        showClearCacheDialog = false
                        onClearCache?.invoke()
                        Toast.makeText(context, strings.clearCacheSuccess, Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showClearCacheDialog = false }
                )
            }

            // 6. 天候シミュレーションダイアログ
            if (showSimulationDialog) {
                WeatherSimulationDialog(
                    language = currentLang,
                    onSelect = { cond, isD ->
                        onSelectPreviewCondition?.invoke(cond, isD)
                        showSimulationDialog = false
                    },
                    onDismiss = { showSimulationDialog = false },
                    cancelText = strings.cancel,
                    resetText = strings.debugResetPreview
                )
            }
        }
    }
}

@Composable
private fun ThemePresetHorizontalPicker(
    currentPreset: ThemePreset,
    language: AppLanguage,
    onSelectPreset: (ThemePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fixedPresets = listOf(
                ThemePreset.SKY_OCEAN,
                ThemePreset.EMERALD_FOREST,
                ThemePreset.SUNSET_AMBER,
                ThemePreset.LAVENDER_TWILIGHT,
                ThemePreset.SAKURA_BLOSSOM,
                ThemePreset.MIDNIGHT_SLATE
            )
            fixedPresets.forEach { preset ->
                val isSelected = currentPreset == preset
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(preset.previewPrimary)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = colorScheme.onSurface,
                                    shape = CircleShape
                                )
                            } else Modifier
                        )
                        .clickable { onSelectPreset(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = preset.displayName(language),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkModeToggleSection(
    currentThemeMode: ThemeMode,
    isDark: Boolean,
    language: AppLanguage,
    onSelectThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val expressHaptics = LocalExpressiveHaptics.current
        Text(
            text = when (language) {
                AppLanguage.JAPANESE -> "ダークモード"
                AppLanguage.SIMPLIFIED_CHINESE -> "深色模式"
                AppLanguage.TRADITIONAL_CHINESE -> "深色模式"
                AppLanguage.ENGLISH -> "Dark Theme"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Android15CardGroup {
            // 1. 天候に追従
            Android15SettingsCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "天候に追従"
                    AppLanguage.SIMPLIFIED_CHINESE -> "跟随天气 (昼夜)"
                    AppLanguage.TRADITIONAL_CHINESE -> "隨天氣變換 (晝夜)"
                    AppLanguage.ENGLISH -> "Follow Weather"
                },
                subtitle = when (language) {
                    AppLanguage.JAPANESE -> "日の出・日の入りの時間帯に連動"
                    AppLanguage.SIMPLIFIED_CHINESE -> "根据日出日落时间自动切换"
                    AppLanguage.TRADITIONAL_CHINESE -> "根據日出日落時間自動切換"
                    AppLanguage.ENGLISH -> "Adapts to sunrise and sunset"
                },
                icon = Icons.Rounded.WbSunny,
                iconBackgroundColor = colorScheme.primaryContainer,
                iconTintColor = colorScheme.onPrimaryContainer,
                position = ConnectedCardPosition.Top,
                trailing = {
                    Switch(
                        checked = currentThemeMode == ThemeMode.WEATHER,
                        onCheckedChange = { checked ->
                            expressHaptics.toggle(checked)
                            val newMode = if (checked) ThemeMode.WEATHER else (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
                            onSelectThemeMode(newMode)
                        }
                    )
                },
                onClick = {
                    val toWeather = currentThemeMode != ThemeMode.WEATHER
                    expressHaptics.toggle(toWeather)
                    val newMode = if (toWeather) ThemeMode.WEATHER else (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
                    onSelectThemeMode(newMode)
                }
            )

            // 2. デバイスに追従
            Android15SettingsCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "デバイスに追従"
                    AppLanguage.SIMPLIFIED_CHINESE -> "跟随系统设置"
                    AppLanguage.TRADITIONAL_CHINESE -> "跟隨系統設定"
                    AppLanguage.ENGLISH -> "Follow System"
                },
                subtitle = when (language) {
                    AppLanguage.JAPANESE -> "端末のダークモード設定に連動"
                    AppLanguage.SIMPLIFIED_CHINESE -> "与设备的深色模式开关保持同步"
                    AppLanguage.TRADITIONAL_CHINESE -> "與設備的深色模式開關保持同步"
                    AppLanguage.ENGLISH -> "Matches device system theme"
                },
                icon = Icons.Rounded.Smartphone,
                iconBackgroundColor = colorScheme.secondaryContainer,
                iconTintColor = colorScheme.onSecondaryContainer,
                position = ConnectedCardPosition.Middle,
                trailing = {
                    Switch(
                        checked = currentThemeMode == ThemeMode.SYSTEM,
                        onCheckedChange = { checked ->
                            expressHaptics.toggle(checked)
                            val newMode = if (checked) ThemeMode.SYSTEM else (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
                            onSelectThemeMode(newMode)
                        }
                    )
                },
                onClick = {
                    val toSystem = currentThemeMode != ThemeMode.SYSTEM
                    expressHaptics.toggle(toSystem)
                    val newMode = if (toSystem) ThemeMode.SYSTEM else (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
                    onSelectThemeMode(newMode)
                }
            )

            // 3. 常にダークモード（オフで常にライトモード）
            Android15SettingsCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "常にダークモード"
                    AppLanguage.SIMPLIFIED_CHINESE -> "始终深色模式"
                    AppLanguage.TRADITIONAL_CHINESE -> "始終深色模式"
                    AppLanguage.ENGLISH -> "Always Dark Mode"
                },
                subtitle = when (language) {
                    AppLanguage.JAPANESE -> "オフで常にライトモード"
                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭时始终为浅色模式"
                    AppLanguage.TRADITIONAL_CHINESE -> "關閉時始終為淺色模式"
                    AppLanguage.ENGLISH -> "Off for always light mode"
                },
                icon = Icons.Rounded.DarkMode,
                iconBackgroundColor = colorScheme.tertiaryContainer,
                iconTintColor = colorScheme.onTertiaryContainer,
                position = ConnectedCardPosition.Bottom,
                trailing = {
                    Switch(
                        checked = currentThemeMode == ThemeMode.DARK,
                        onCheckedChange = { checked ->
                            expressHaptics.toggle(checked)
                            val newMode = if (checked) ThemeMode.DARK else ThemeMode.LIGHT
                            onSelectThemeMode(newMode)
                        }
                    )
                },
                onClick = {
                    val toDark = currentThemeMode != ThemeMode.DARK
                    expressHaptics.toggle(toDark)
                    val newMode = if (toDark) ThemeMode.DARK else ThemeMode.LIGHT
                    onSelectThemeMode(newMode)
                }
            )
        }
    }
}


@Composable
private fun VisualEffectsSettingsSection(
    isFrostedGlassEnabled: Boolean,
    isColorfulShapesEnabled: Boolean,
    language: AppLanguage,
    onToggleFrostedGlass: (Boolean) -> Unit,
    onToggleColorfulShapes: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val expressHaptics = LocalExpressiveHaptics.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = when (language) {
                AppLanguage.JAPANESE -> "視覚効果"
                AppLanguage.SIMPLIFIED_CHINESE -> "视觉特效"
                AppLanguage.TRADITIONAL_CHINESE -> "視覺特效"
                AppLanguage.ENGLISH -> "Visual Effects"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Android15CardGroup {
            // フロスト背景効果
            Android15SettingsCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "フロスト背景効果"
                    AppLanguage.SIMPLIFIED_CHINESE -> "磨砂玻璃效果"
                    AppLanguage.TRADITIONAL_CHINESE -> "毛玻璃效果"
                    AppLanguage.ENGLISH -> "Frosted Glass Effect"
                },
                subtitle = when (language) {
                    AppLanguage.JAPANESE -> "カードやタイルの背景に半透明すりガラスぼかしを適用"
                    AppLanguage.SIMPLIFIED_CHINESE -> "在卡片与功能磁贴背景应用半透明毛玻璃模糊"
                    AppLanguage.TRADITIONAL_CHINESE -> "在卡片與功能圖塊背景套用半透明毛玻璃模糊"
                    AppLanguage.ENGLISH -> "Translucent optical blur on cards"
                },
                icon = Icons.Rounded.BlurOn,
                iconBackgroundColor = colorScheme.primaryContainer,
                iconTintColor = colorScheme.onPrimaryContainer,
                position = ConnectedCardPosition.Top,
                trailing = {
                    Switch(
                        checked = isFrostedGlassEnabled,
                        onCheckedChange = { checked ->
                            expressHaptics.toggle(checked)
                            onToggleFrostedGlass(checked)
                        }
                    )
                },
                onClick = {
                    val next = !isFrostedGlassEnabled
                    expressHaptics.toggle(next)
                    onToggleFrostedGlass(next)
                }
            )

            // カラフルアクセント
            Android15SettingsCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "カラフルアクセント"
                    AppLanguage.SIMPLIFIED_CHINESE -> "多彩表现力点缀"
                    AppLanguage.TRADITIONAL_CHINESE -> "多彩表現力點綴"
                    AppLanguage.ENGLISH -> "Colorful Accents"
                },
                subtitle = when (language) {
                    AppLanguage.JAPANESE -> "タイルのシェイプ形状や週間天気の気温ラインを鮮やかに彩る"
                    AppLanguage.SIMPLIFIED_CHINESE -> "丰富几何形态磁贴与一周气温曲线的鲜明色彩"
                    AppLanguage.TRADITIONAL_CHINESE -> "豐富幾何形態圖塊與一週氣溫曲線的鮮明色彩"
                    AppLanguage.ENGLISH -> "Vibrant MaterialShapes in tiles and colorful weekly temp lines"
                },
                icon = Icons.Rounded.AutoAwesome,
                iconBackgroundColor = colorScheme.secondaryContainer,
                iconTintColor = colorScheme.onSecondaryContainer,
                position = ConnectedCardPosition.Bottom,
                trailing = {
                    Switch(
                        checked = isColorfulShapesEnabled,
                        onCheckedChange = { checked ->
                            expressHaptics.toggle(checked)
                            onToggleColorfulShapes(checked)
                        }
                    )
                },
                onClick = {
                    val next = !isColorfulShapesEnabled
                    expressHaptics.toggle(next)
                    onToggleColorfulShapes(next)
                }
            )
        }
    }
}

/**
 * 完全に別画面として表示される「アプリ情報」ページ
 * 余計なもの（パッケージ名やデザイン仕様など）は一切入れず、
 * アプリ名 (Expressive Weather)、ロゴ、バージョン名のみを表示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    versionName: String,
    versionCode: String = "",
    appIconBitmap: ImageBitmap?,
    language: AppLanguage,
    isDebugUnlocked: Boolean = false,
    onUnlockDebug: () -> Unit = {},
    onLockDebug: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    embeddedInPane: Boolean = false
) {
    val context = LocalContext.current
    val expressHaptics = LocalExpressiveHaptics.current
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var statusToast by remember { mutableStateOf<Toast?>(null) }
    var showDataSourcesDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val strings = language.strings

    DisposableEffect(Unit) {
        onDispose {
            statusToast?.cancel()
        }
    }

    val appInfoTitle = when (language) {
        AppLanguage.JAPANESE -> "アプリ情報"
        AppLanguage.SIMPLIFIED_CHINESE -> "应用信息"
        AppLanguage.TRADITIONAL_CHINESE -> "應用資訊"
        AppLanguage.ENGLISH -> "App Info"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = if (embeddedInPane) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (embeddedInPane) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = appInfoTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = appInfoTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface,
                        scrolledContainerColor = colorScheme.surfaceContainer
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        containerColor = colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. ロゴ (アプリアイコン)
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = "Expressive Weather",
                        modifier = Modifier.size(96.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. アプリ名
            Text(
                text = "Expressive Weather",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 3. バージョン名 & 4. ビルド番号 のカードグループ
            Android15CardGroup {
                Android15SettingsCard(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "バージョン名"
                        AppLanguage.SIMPLIFIED_CHINESE -> "版本名称"
                        AppLanguage.TRADITIONAL_CHINESE -> "版本名稱"
                        AppLanguage.ENGLISH -> "Version Name"
                    },
                    subtitle = versionName,
                    icon = Icons.Rounded.Sell,
                    iconBackgroundColor = colorScheme.primaryContainer,
                    iconTintColor = colorScheme.onPrimaryContainer,
                    position = ConnectedCardPosition.Top,
                    trailing = {
                        ExpressiveValueBadge(versionName)
                    },
                    onClick = {
                        if (isDebugUnlocked) {
                            statusToast?.cancel()
                            statusToast = Toast.makeText(
                                context,
                                when (language) {
                                    AppLanguage.JAPANESE -> "デバッグ設定は既に有効です"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "调试设置已处于启用状态"
                                    AppLanguage.TRADITIONAL_CHINESE -> "偵錯設定已處於啟用狀態"
                                    AppLanguage.ENGLISH -> "Debug settings are already enabled"
                                },
                                Toast.LENGTH_SHORT
                            ).apply { show() }
                        } else {
                            val now = System.currentTimeMillis()
                            // 連続タップ判定: 前回のタップから 500ms 以内なら連続、それを超えたら1回目にリセット
                            if (now - lastTapTimestamp > 500L) {
                                tapCount = 1
                            } else {
                                tapCount++
                            }
                            lastTapTimestamp = now

                            expressHaptics.play(ExpressiveHapticPattern.SCRUB_TICK, bypassThrottle = true)

                            if (tapCount >= 7) {
                                onUnlockDebug()
                                statusToast?.cancel()
                                statusToast = Toast.makeText(
                                    context,
                                    when (language) {
                                        AppLanguage.JAPANESE -> "デバッグ設定が有効になりました！"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "已启用调试设置！"
                                        AppLanguage.TRADITIONAL_CHINESE -> "已啟用偵錯設定！"
                                        AppLanguage.ENGLISH -> "Debug settings are now enabled!"
                                    },
                                    Toast.LENGTH_SHORT
                                ).apply { show() }
                                expressHaptics.play(ExpressiveHapticPattern.CELEBRATE_UNLOCK, bypassThrottle = true)
                                tapCount = 0
                                lastTapTimestamp = 0L
                            } else if (tapCount >= 3) {
                                val remaining = 7 - tapCount
                                statusToast?.cancel()
                                statusToast = Toast.makeText(
                                    context,
                                    when (language) {
                                        AppLanguage.JAPANESE -> "デバッグ設定が有効になるまであと ${remaining} 回タップしてください"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "还需连续点击 ${remaining} 次即可开启调试设置"
                                        AppLanguage.TRADITIONAL_CHINESE -> "還需連續點擊 ${remaining} 次即可開啟偵錯設定"
                                        AppLanguage.ENGLISH -> "Tap $remaining more times in rapid succession"
                                    },
                                    Toast.LENGTH_SHORT
                                ).apply { show() }
                            }
                        }
                    }
                )


                Android15SettingsCard(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "データソースと帰属表記 (クレジット)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "数据来源与致谢 (版权声明)"
                        AppLanguage.TRADITIONAL_CHINESE -> "資料來源與致謝 (版權聲明)"
                        AppLanguage.ENGLISH -> "Data Sources & Credits"
                    },
                    subtitle = when (language) {
                        AppLanguage.JAPANESE -> "気象庁、Open-Meteo、NOAA、MeteoAlarm、国土地理院 ほか"
                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅、Open-Meteo、NOAA、MeteoAlarm 等"
                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳、Open-Meteo、NOAA、MeteoAlarm 等"
                        AppLanguage.ENGLISH -> "JMA, Open-Meteo, NOAA, MeteoAlarm, GSI, etc."
                    },
                    icon = Icons.Rounded.Public,
                    iconBackgroundColor = colorScheme.tertiaryContainer,
                    iconTintColor = colorScheme.onTertiaryContainer,
                    position = ConnectedCardPosition.Middle,
                    onClick = { showDataSourcesDialog = true }
                )

                Android15SettingsCard(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "プライバシーポリシー"
                        AppLanguage.SIMPLIFIED_CHINESE -> "隐私政策"
                        AppLanguage.TRADITIONAL_CHINESE -> "隱私權政策"
                        AppLanguage.ENGLISH -> "Privacy Policy"
                    },
                    subtitle = when (language) {
                        AppLanguage.JAPANESE -> "位置情報・データ送信先・端末内保存の取り扱い"
                        AppLanguage.SIMPLIFIED_CHINESE -> "位置信息、数据接口与本地存储说明"
                        AppLanguage.TRADITIONAL_CHINESE -> "位置資訊、資料介面與本機儲存說明"
                        AppLanguage.ENGLISH -> "How location, API requests, and on-device data are handled"
                    },
                    icon = Icons.Rounded.PrivacyTip,
                    iconBackgroundColor = colorScheme.secondaryContainer,
                    iconTintColor = colorScheme.onSecondaryContainer,
                    position = ConnectedCardPosition.Middle,
                    onClick = { showPrivacyPolicyDialog = true }
                )

                Android15SettingsCard(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "オープンソースライセンス"
                        AppLanguage.SIMPLIFIED_CHINESE -> "开源许可证"
                        AppLanguage.TRADITIONAL_CHINESE -> "開源許可證"
                        AppLanguage.ENGLISH -> "Open Source License"
                    },
                    subtitle = "Apache License 2.0 & Third-Party Notices",
                    icon = Icons.Rounded.Policy,
                    iconBackgroundColor = colorScheme.surfaceVariant,
                    iconTintColor = colorScheme.onSurfaceVariant,
                    position = ConnectedCardPosition.Bottom,
                    onClick = { showLicenseDialog = true }
                )
            }

            if (BuildConfig.ENABLE_EXTERNAL_DONATION_LINKS) {
                Spacer(modifier = Modifier.height(16.dp))

                // 開発者支援 (Buy Me a Coffee) カードグループ（GitHub 配信版のみ表示）
                Android15CardGroup {
                    Android15SettingsCard(
                        title = when (language) {
                            AppLanguage.JAPANESE -> "開発者を支援 (Buy Me a Coffee)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "支持开发者 (Buy Me a Coffee)"
                            AppLanguage.TRADITIONAL_CHINESE -> "支持開發者 (Buy Me a Coffee)"
                            AppLanguage.ENGLISH -> "Support Developer (Buy Me a Coffee)"
                        },
                        subtitle = when (language) {
                            AppLanguage.JAPANESE -> "buymeacoffee.com/nemynew を開く"
                            AppLanguage.SIMPLIFIED_CHINESE -> "前往 buymeacoffee.com/nemynew 请作者喝杯咖啡"
                            AppLanguage.TRADITIONAL_CHINESE -> "前往 buymeacoffee.com/nemynew 請作者喝杯咖啡"
                            AppLanguage.ENGLISH -> "Open buymeacoffee.com/nemynew"
                        },
                        icon = Icons.Rounded.LocalCafe,
                        iconBackgroundColor = colorScheme.secondaryContainer,
                        iconTintColor = colorScheme.onSecondaryContainer,
                        position = ConnectedCardPosition.Single,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://buymeacoffee.com/nemynew")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                statusToast?.cancel()
                                statusToast = Toast.makeText(
                                    context,
                                    when (language) {
                                        AppLanguage.JAPANESE -> "ブラウザを開けませんでした"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "无法打开浏览器"
                                        AppLanguage.TRADITIONAL_CHINESE -> "無法開啟瀏覽器"
                                        AppLanguage.ENGLISH -> "Could not open browser"
                                    },
                                    Toast.LENGTH_SHORT
                                ).apply { show() }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 気象庁および公的機関の非公式アプリ免責表示カード（常時表示）
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "※本アプリは気象庁公式のアプリではありません。気象庁やNOAA等の公的機関が提供するオープンデータを活用して開発された非公式サードパーティ製アプリです。災害・緊急時は気象庁や自治体の公式発表をご確認ください。"
                            AppLanguage.SIMPLIFIED_CHINESE -> "※本应用非日本气象厅等官方机构的官方应用。系利用气象厅及NOAA等公共开放数据开发的第三方应用。灾害及紧急情况下请以官方权威发布为准。"
                            AppLanguage.TRADITIONAL_CHINESE -> "※本應用程式非日本氣象廳等官方機構的官方應用程式。係利用氣象廳及NOAA等公共開放資料開發的第三方應用程式。災害及緊急情況下請以官方權威發布為準。"
                            AppLanguage.ENGLISH -> "Note: This is an unofficial third-party application and is not affiliated with the Japan Meteorological Agency (JMA), NOAA, or any government entity. In emergencies, please consult official announcements."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // デバッグ設定が有効な場合、無効化（非表示に戻す）カードを表示
            if (isDebugUnlocked) {
                Spacer(modifier = Modifier.height(16.dp))
                Android15CardGroup {
                    Android15SettingsCard(
                        title = when (language) {
                            AppLanguage.JAPANESE -> "デバッグ設定を無効化する"
                            AppLanguage.SIMPLIFIED_CHINESE -> "禁用调试设置"
                            AppLanguage.TRADITIONAL_CHINESE -> "停用偵錯設定"
                            AppLanguage.ENGLISH -> "Disable Debug Settings"
                        },
                        subtitle = when (language) {
                            AppLanguage.JAPANESE -> "タップするとデバッグ設定が非表示に戻ります"
                            AppLanguage.SIMPLIFIED_CHINESE -> "点击后调试设置将重新隐藏"
                            AppLanguage.TRADITIONAL_CHINESE -> "點擊後偵錯設定將重新隱藏"
                            AppLanguage.ENGLISH -> "Hides debug settings until enabled again"
                        },
                        icon = Icons.Rounded.BugReport,
                        iconBackgroundColor = colorScheme.errorContainer.copy(alpha = 0.5f),
                        iconTintColor = colorScheme.onErrorContainer,
                        position = ConnectedCardPosition.Single,
                        onClick = {
                            onLockDebug()
                            statusToast?.cancel()
                            statusToast = Toast.makeText(
                                context,
                                when (language) {
                                    AppLanguage.JAPANESE -> "デバッグ設定を無効化しました"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "已禁用调试设置"
                                    AppLanguage.TRADITIONAL_CHINESE -> "已停用偵錯設定"
                                    AppLanguage.ENGLISH -> "Debug settings disabled"
                                },
                                Toast.LENGTH_SHORT
                            ).apply { show() }
                        }
                    )
                }
            }
        }
    }

    if (showDataSourcesDialog) {
        DataSourcesCreditsDialog(
            language = language,
            onDismiss = { showDataSourcesDialog = false }
        )
    }

    if (showLicenseDialog) {
        OpenSourceLicenseDialog(
            language = language,
            onDismiss = { showLicenseDialog = false }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            language = language,
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }
}

