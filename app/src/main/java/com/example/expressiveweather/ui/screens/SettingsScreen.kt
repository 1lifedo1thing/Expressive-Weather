package com.example.expressiveweather.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.expressiveweather.data.WeatherBackgroundUpdateManager
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.expressiveweather.data.*
import com.example.expressiveweather.theme.ExpressiveWeatherTheme
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset

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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isFrostedGlassEnabled: Boolean = true,
    isColorfulShapesEnabled: Boolean = true,
    isRadarDebugZoomEnabled: Boolean = false,
    isRadarDebugSourceEnabled: Boolean = false,
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
    initialSection: String = SettingsDataHolder.SECTION_ALL,
    onToggleWeatherAdaptiveTheme: () -> Unit,
    onSelectThemePreset: (ThemePreset) -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onToggleFrostedGlass: (Boolean) -> Unit = {},
    onToggleColorfulShapes: (Boolean) -> Unit = {},
    onToggleRadarDebugZoom: (Boolean) -> Unit = {},
    onToggleRadarDebugSource: (Boolean) -> Unit = {},
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
    var currentPreset by remember { mutableStateOf(themePreset) }
    var currentThemeMode by remember { mutableStateOf(themeMode) }
    var currentRadarDebugZoom by remember(isRadarDebugZoomEnabled) { mutableStateOf(isRadarDebugZoomEnabled) }
    var currentRadarDebugSource by remember(isRadarDebugSourceEnabled) { mutableStateOf(isRadarDebugSourceEnabled) }
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

    var currentAdaptiveTheme by remember { mutableStateOf(currentPreset == ThemePreset.WEATHER) }
    var currentFrostedGlass by remember { mutableStateOf(isFrostedGlassEnabled) }
    var currentColorfulShapes by remember { mutableStateOf(isColorfulShapesEnabled) }
    var currentUnit by remember { mutableStateOf(temperatureUnit) }
    var currentWindUnit by remember { mutableStateOf(windSpeedUnit) }
    var currentWeatherUpdateInterval by remember(weatherUpdateInterval) { mutableStateOf(weatherUpdateInterval) }
    var currentLang by remember(language) { mutableStateOf(language) }

    var showTempUnitDialog by remember { mutableStateOf(false) }
    var showWindUnitDialog by remember { mutableStateOf(false) }
    var showWeatherUpdateIntervalDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showDataProviderDialog by remember { mutableStateOf(false) }
    var showDataSourcesDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showSimulationDialog by remember { mutableStateOf(false) }
    var currentSubPage by remember(initialSection) {
        mutableStateOf<String?>(
            if (initialSection == SettingsDataHolder.SECTION_LANGUAGE || initialSection == "language") "language"
            else if (initialSection == "app_info") "app_info"
            else null
        )
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

            BackHandler(enabled = currentSubPage != null) {
                currentSubPage = null
            }

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
                    onBack = { currentSubPage = null }
                )
                return@CompositionLocalProvider
            } else if (currentSubPage == "language") {
                LanguageSelectionScreen(
                    currentLanguage = currentLang,
                    onSelectLanguage = { newLang ->
                        currentLang = newLang
                        SettingsDataHolder.language = newLang
                        onSelectLanguage(newLang)
                    },
                    onBack = { currentSubPage = null }
                )
                return@CompositionLocalProvider
            }
            val screenTitle = when {
                initialSection == SettingsDataHolder.SECTION_APPEARANCE -> strings.appearanceTitle
                initialSection == SettingsDataHolder.SECTION_GENERAL -> strings.generalSettingsTitle
                initialSection == SettingsDataHolder.SECTION_SOURCES -> strings.dataSourcesTitle
                initialSection == SettingsDataHolder.SECTION_DEBUG && isDebugUnlocked -> strings.debugSettingsTitle
                else -> strings.settingsTitle
            }

            Scaffold(
                modifier = modifier
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
                        colors = TopAppBarDefaults.largeTopAppBarColors(
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
                    // ================= 1. 外観とディスプレイ (Display & Theme) =================
                    if (initialSection == SettingsDataHolder.SECTION_APPEARANCE || initialSection == SettingsDataHolder.SECTION_ALL) {
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
                                        if (currentPreset != ThemePreset.SYSTEM) {
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
                                        if (currentPreset != ThemePreset.WEATHER) {
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

                    // ================= 2. 一般設定と単位 (General Preferences & Units) =================
                    if (initialSection == SettingsDataHolder.SECTION_GENERAL || initialSection == SettingsDataHolder.SECTION_ALL) {
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
                                onClick = {
                                    val intent = Intent(context, LanguageSelectionActivity::class.java)
                                    context.startActivity(intent)
                                }
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

                            // 4.1 バックグラウンド実行（電池の最適化）の設定
                            val isBatteryOptimizedIgnored = remember {
                                WeatherBackgroundUpdateManager.getInstance(context).isBatteryOptimizationIgnored()
                            }
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "バックグラウンド実行（電池の最適化）"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "后台运行（电池优化）"
                                    AppLanguage.TRADITIONAL_CHINESE -> "後台執行（電池最佳化）"
                                    AppLanguage.ENGLISH -> "Background Execution (Battery)"
                                },
                                subtitle = if (isBatteryOptimizedIgnored) {
                                    when (currentLang) {
                                        AppLanguage.JAPANESE -> "✓ 制限なし（設定頻度どおり確実にバックグラウンド更新されます）"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "✓ 无限制（按设定频率可靠在后台自动刷新）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "✓ 無限制（按設定頻率可靠在後台自動重新整理）"
                                        AppLanguage.ENGLISH -> "✓ Unrestricted (Reliably updates in background)"
                                    }
                                } else {
                                    when (currentLang) {
                                        AppLanguage.JAPANESE -> "タップして制限なしに設定（OSスリープ中の更新停止を防止）"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "点击设为无限制（防止系统休眠时暂停更新）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "點擊設為無限制（防止系統休眠時暫停更新）"
                                        AppLanguage.ENGLISH -> "Tap to allow unrestricted background execution"
                                    }
                                },
                                icon = if (isBatteryOptimizedIgnored) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryAlert,
                                iconBackgroundColor = if (isBatteryOptimizedIgnored) colorScheme.secondaryContainer else colorScheme.tertiaryContainer,
                                iconTintColor = if (isBatteryOptimizedIgnored) colorScheme.onSecondaryContainer else colorScheme.onTertiaryContainer,
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
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
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
                                            currentLiveAlertNotification = checked
                                            onToggleLiveAlertNotification(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentLiveAlertNotification
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
                                    CurrentLocationProvider.openAppLocationSettings(context)
                                }
                            )

                            // 8. バッテリー制限の解除（バックグラウンド監視）
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "バッテリー制限の解除"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "解除电池优化限制"
                                    AppLanguage.TRADITIONAL_CHINESE -> "解除電池最佳化限制"
                                    AppLanguage.ENGLISH -> "Battery Optimization Exemption"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> if (isBatteryIgnored) "制限なし（バックグラウンド監視が停止されません）" else "OSによるスリープや制限を防ぎます。タップして解除"
                                    AppLanguage.SIMPLIFIED_CHINESE -> if (isBatteryIgnored) "无限制（后台监测不会被系统中断）" else "防止系统休眠中止后台监测。点击解除"
                                    AppLanguage.TRADITIONAL_CHINESE -> if (isBatteryIgnored) "無限制（後台監測不會被系統中斷）" else "防止系統休眠中止後台監測。點擊解除"
                                    AppLanguage.ENGLISH -> if (isBatteryIgnored) "Unrestricted (Background monitoring won't be killed)" else "Prevents OS sleep from stopping monitoring. Tap to allow"
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

                            // 9. アプリ情報 (アプリ名、ロゴ、バージョン名、ビルド番号)
                            Android15SettingsCard(
                                title = strings.aboutTitle,
                                subtitle = "${strings.appName} • Ver $versionName (Build $versionCode)",
                                leadingContent = {
                                    if (appIconBitmap != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colorScheme.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = appIconBitmap,
                                                contentDescription = strings.appName,
                                                modifier = Modifier.size(38.dp)
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
                                onClick = { currentSubPage = "app_info" }
                            )
                        }
                    }

                    // ================= 3. 情報ソースとデータ管理 (Data & Storage) =================
                    if (initialSection == SettingsDataHolder.SECTION_SOURCES || initialSection == SettingsDataHolder.SECTION_ALL) {
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
                                            currentGdacsAlerts = checked
                                            onToggleGdacsAlerts(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentGdacsAlerts
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
                                            currentMeteoAlarmAlerts = checked
                                            onToggleMeteoAlarmAlerts(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentMeteoAlarmAlerts
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
                                            currentWmoAlerts = checked
                                            onToggleWmoAlerts(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentWmoAlerts
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
                                            currentWmoAlways = checked
                                            onToggleWmoAlways(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentWmoAlways
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
                    if (isDebugUnlocked && (initialSection == SettingsDataHolder.SECTION_DEBUG || initialSection == SettingsDataHolder.SECTION_ALL)) {
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
                                            if (!checked) {
                                                isDebugUnlocked = false
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
                                            currentRadarDebugZoom = checked
                                            onToggleRadarDebugZoom(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentRadarDebugZoom
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
                                            currentRadarDebugSource = checked
                                            onToggleRadarDebugSource(checked)
                                        }
                                    )
                                },
                                onClick = {
                                    val next = !currentRadarDebugSource
                                    currentRadarDebugSource = next
                                    onToggleRadarDebugSource(next)
                                }
                            )

                            // 4. キキクルをテストする (トグルスイッチ・メイン画面表示連動)
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
                            Android15SettingsCard(
                                title = when (currentLang) {
                                    AppLanguage.JAPANESE -> "雨雲LiveUpdates・カウントダウンテスト"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "降雨实时动态与倒计时测试"
                                    AppLanguage.TRADITIONAL_CHINESE -> "降雨即時動態與倒數計時測試"
                                    AppLanguage.ENGLISH -> "Test Rain Live Updates Countdown"
                                },
                                subtitle = when (currentLang) {
                                    AppLanguage.JAPANESE -> "タップで切替 (今夜の雨 → 接近中 → 降雨中 → 実データ)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "点击切换 (今夜有雨 → 接近中 → 降雨中 → 实际数据)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "點擊切換 (今夜有雨 → 接近中 → 降雨中 → 實際數據)"
                                    AppLanguage.ENGLISH -> "Tap to cycle (Tonight's rain → Approaching → Raining → Actual)"
                                },
                                icon = Icons.Rounded.WaterDrop,
                                iconBackgroundColor = colorScheme.primaryContainer,
                                iconTintColor = colorScheme.onPrimaryContainer,
                                position = ConnectedCardPosition.Middle,
                                onClick = {
                                    onCyclePrecipitationDemo()
                                    Toast.makeText(
                                        context,
                                        when (currentLang) {
                                            AppLanguage.JAPANESE -> "雨雲予測テスト状態を切り替えました"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "已切换降雨预测测试状态"
                                            AppLanguage.TRADITIONAL_CHINESE -> "已切換降雨預測測試狀態"
                                            AppLanguage.ENGLISH -> "Cycled precipitation nowcast test state"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

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
                                    com.example.expressiveweather.ui.components.KikikuruAlertCard(
                                        alertLevel = AlertLevel.LEVEL_5,
                                        language = currentLang
                                    )
                                    com.example.expressiveweather.ui.components.KikikuruAlertCard(
                                        alertLevel = AlertLevel.LEVEL_4,
                                        language = currentLang
                                    )
                                    com.example.expressiveweather.ui.components.KikikuruAlertCard(
                                        alertLevel = AlertLevel.LEVEL_3,
                                        language = currentLang
                                    )
                                    com.example.expressiveweather.ui.components.KikikuruAlertCard(
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
                                    com.example.expressiveweather.ui.components.WeatherWarningCard(
                                        type = com.example.expressiveweather.ui.components.WeatherWarningType.DOMESTIC_WARNING,
                                        title = "大雨警報・洪水警報",
                                        description = "大雨および河川の増水・氾濫に警戒してください。"
                                    )
                                    com.example.expressiveweather.ui.components.WeatherWarningCard(
                                        type = com.example.expressiveweather.ui.components.WeatherWarningType.FOREIGN_WARNING,
                                        title = "Severe Weather Warning",
                                        description = "Flash flooding and dangerous conditions are expected."
                                    )
                                    com.example.expressiveweather.ui.components.WeatherWarningCard(
                                        type = com.example.expressiveweather.ui.components.WeatherWarningType.ADVISORY,
                                        title = "雷注意報・強風注意報",
                                        description = "急な強い雨や落雷、突風に注意してください。"
                                    )
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
                        currentUnit = it
                        if (it != temperatureUnit) {
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
                            val newMode = if (checked) ThemeMode.WEATHER else (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
                            onSelectThemeMode(newMode)
                        }
                    )
                },
                onClick = {
                    val newMode = if (currentThemeMode == ThemeMode.WEATHER) (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT) else ThemeMode.WEATHER
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
                            val newMode = if (checked) ThemeMode.SYSTEM else (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
                            onSelectThemeMode(newMode)
                        }
                    )
                },
                onClick = {
                    val newMode = if (currentThemeMode == ThemeMode.SYSTEM) (if (isDark) ThemeMode.DARK else ThemeMode.LIGHT) else ThemeMode.SYSTEM
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
                            val newMode = if (checked) ThemeMode.DARK else ThemeMode.LIGHT
                            onSelectThemeMode(newMode)
                        }
                    )
                },
                onClick = {
                    val newMode = if (currentThemeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
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
                        onCheckedChange = onToggleFrostedGlass
                    )
                },
                onClick = {
                    onToggleFrostedGlass(!isFrostedGlassEnabled)
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
                        onCheckedChange = onToggleColorfulShapes
                    )
                },
                onClick = {
                    onToggleColorfulShapes(!isColorfulShapesEnabled)
                }
            )
        }
    }
}

/**
 * 完全に別画面として表示される「アプリ情報」ページ
 * 余計なもの（パッケージ名やデザイン仕様など）は一切入れず、
 * アプリ名 (ExpressiveWeather)、ロゴ、バージョン名、ビルド番号のみを表示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    versionName: String,
    versionCode: String,
    appIconBitmap: ImageBitmap?,
    language: AppLanguage,
    isDebugUnlocked: Boolean = false,
    onUnlockDebug: () -> Unit = {},
    onLockDebug: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var statusToast by remember { mutableStateOf<Toast?>(null) }
    var showDataSourcesDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val strings = language.strings

    DisposableEffect(Unit) {
        onDispose {
            statusToast?.cancel()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "アプリ情報"
                            AppLanguage.SIMPLIFIED_CHINESE -> "应用信息"
                            AppLanguage.TRADITIONAL_CHINESE -> "應用資訊"
                            AppLanguage.ENGLISH -> "App Info"
                        },
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
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorScheme.surfaceContainerHighest)
                    .border(
                        width = 1.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = strings.appName,
                        modifier = Modifier.size(90.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. アプリ名 (言語設定に連動)
            Text(
                text = strings.appName,
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

                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        AppLanguage.JAPANESE -> "ビルド番号"
                        AppLanguage.SIMPLIFIED_CHINESE -> "内部版本号"
                        AppLanguage.TRADITIONAL_CHINESE -> "內部版本號"
                        AppLanguage.ENGLISH -> "Build Number"
                    },
                    subtitle = versionCode,
                    icon = Icons.Rounded.Numbers,
                    iconBackgroundColor = colorScheme.secondaryContainer,
                    iconTintColor = colorScheme.onSecondaryContainer,
                    position = ConnectedCardPosition.Middle,
                    trailing = {
                        ExpressiveValueBadge("#$versionCode")
                    },
                    onClick = null
                )

                Android15SettingsCard(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "データソースと帰属表記 (クレジット)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "数据来源与致谢 (版权声明)"
                        AppLanguage.TRADITIONAL_CHINESE -> "資料來源與致謝 (版權聲明)"
                        AppLanguage.ENGLISH -> "Data Sources & Credits"
                    },
                    subtitle = when (language) {
                        AppLanguage.JAPANESE -> "気象庁、Open-Meteo、RainViewer、NOAA ほか"
                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅、Open-Meteo、RainViewer、NOAA 等"
                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳、Open-Meteo、RainViewer、NOAA 等"
                        AppLanguage.ENGLISH -> "JMA, Open-Meteo, RainViewer, NOAA, etc."
                    },
                    icon = Icons.Rounded.Public,
                    iconBackgroundColor = colorScheme.tertiaryContainer,
                    iconTintColor = colorScheme.onTertiaryContainer,
                    position = ConnectedCardPosition.Middle,
                    onClick = { showDataSourcesDialog = true }
                )

                Android15SettingsCard(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "オープンソースライセンス"
                        AppLanguage.SIMPLIFIED_CHINESE -> "开源许可证"
                        AppLanguage.TRADITIONAL_CHINESE -> "開源許可證"
                        AppLanguage.ENGLISH -> "Open Source License"
                    },
                    subtitle = "GNU General Public License v3 (GPL-3.0)",
                    icon = Icons.Rounded.Policy,
                    iconBackgroundColor = colorScheme.surfaceVariant,
                    iconTintColor = colorScheme.onSurfaceVariant,
                    position = ConnectedCardPosition.Bottom,
                    onClick = { showLicenseDialog = true }
                )
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
}

