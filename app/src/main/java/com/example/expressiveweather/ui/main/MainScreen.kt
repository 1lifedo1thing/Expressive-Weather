package com.example.expressiveweather.ui.main

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.expressiveweather.ui.components.BackgroundPreviewPill
import com.example.expressiveweather.ui.components.BackgroundPreviewSettings
import com.example.expressiveweather.ui.screens.SettingsActivity
import com.example.expressiveweather.ui.screens.SettingsDataHolder
import com.example.expressiveweather.ui.radar.RadarCardSection
import com.example.expressiveweather.ui.radar.RadarTileHttpClient
import com.example.expressiveweather.theme.ThemeMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.example.expressiveweather.theme.ThemePreset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.example.expressiveweather.data.WeatherCondition
import com.example.expressiveweather.data.AlertSource
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.getLocalizedLabel
import com.example.expressiveweather.data.localizedName
import com.example.expressiveweather.data.PrecipitationAnalyzer
import com.example.expressiveweather.theme.ExpressiveWeatherTheme
import com.example.expressiveweather.theme.getAtmosphereTheme
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expressiveweather.data.CurrentLocationProvider
import com.example.expressiveweather.data.WeatherRepository
import com.example.expressiveweather.data.local.WeatherDatabase
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.expressiveweather.ui.components.BentoCardType
import com.example.expressiveweather.ui.components.CitySearchSheet
import com.example.expressiveweather.ui.components.CompactWeatherHeader
import com.example.expressiveweather.ui.components.ExpressiveFloatingToolbar
import com.example.expressiveweather.ui.components.ToolbarMode
import com.example.expressiveweather.ui.components.HeroWeatherCard
import com.example.expressiveweather.ui.components.HourlyForecastSection
import com.example.expressiveweather.ui.components.MorphingWeatherHeader
import com.example.expressiveweather.ui.components.WeatherAtmosphereCanvas
import com.example.expressiveweather.ui.util.LocalDeviceTiltState
import com.example.expressiveweather.ui.util.rememberDeviceTiltState
import com.example.expressiveweather.ui.components.WeatherBentoGrid
import com.example.expressiveweather.ui.components.WeatherCacheStatusBadge
import com.example.expressiveweather.ui.components.WeatherDashboardSkeleton
import com.example.expressiveweather.ui.components.WeatherFooterStatusSection
import com.example.expressiveweather.ui.components.TabletNavigationRail
import com.example.expressiveweather.ui.components.TabletNavigationDrawer
import com.example.expressiveweather.ui.components.TabletWeatherHeader
import androidx.compose.ui.zIndex
import com.example.expressiveweather.ui.components.WeeklyForecastSection
import com.example.expressiveweather.ui.components.KikikuruWarningBanner
import com.example.expressiveweather.ui.components.MunicipalityDisasterBanner
import com.example.expressiveweather.ui.components.PrecipitationNowcastCard
import com.example.expressiveweather.data.RainStatus
import com.example.expressiveweather.data.KikikuruBackgroundMonitor
import com.example.expressiveweather.data.MunicipalityAlertInfo
import com.example.expressiveweather.data.AlertLevel
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.DisasterTargetFlags
import com.example.expressiveweather.data.DisasterType
import com.example.expressiveweather.data.KikikuruDisasterDetail
import com.example.expressiveweather.data.MunicipalityWarningItem
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    context: Context = LocalContext.current,
    viewModel: MainScreenViewModel = viewModel(
        factory = remember(context) {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    val database = WeatherDatabase.getInstance(context.applicationContext)
                    return MainScreenViewModel(
                        repository = WeatherRepository(
                            weatherDao = database.weatherDao(),
                            savedCityDao = database.savedCityDao(),
                            context = context.applicationContext
                        ),
                        locationProvider = CurrentLocationProvider(context.applicationContext)
                    ) as T
                }
            }
        }
    ),
    onNavigateToWindDetail: () -> Unit = {},
    onNavigateToTodayDetail: (selectedDay: Int, initialHour: Int?) -> Unit = { _, _ -> },
    onNavigateToHourlyDetail: (initialHour: Int?) -> Unit = {},
    onNavigateToRadarDetail: () -> Unit = {},
    onNavigateToBentoDetail: (card: BentoCardType) -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundCondition: WeatherCondition? = null,
    backgroundIsDay: Boolean? = null
) {
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.refreshCurrentLocation()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setLiveAlertNotificationEnabled(true)
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.refreshCurrentLocation()
        }

        // Android 13+ 通知権限リクエスト
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notif = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notif) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 雨雲レーダー用HTTP接続を起動時に事前確立し、画面表示時に再利用できる可能性を高める
        RadarTileHttpClient.prewarm("https://www.jma.go.jp/bosai/jmatile/")
        RadarTileHttpClient.prewarm("https://tilecache.rainviewer.com")
        RadarTileHttpClient.prewarm("https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/ridge::USCOMP-N0Q-0/4/3/6.png")
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.startContinuousLocationUpdates()
                    viewModel.refreshCurrentLocation(forceFresh = true)
                    KikikuruBackgroundMonitor.getInstance(context).onAppForegrounded()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopContinuousLocationUpdates()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    var toolbarMode by remember { mutableStateOf(ToolbarMode.NORMAL) }
    var isTabletPanelExpanded by rememberSaveable { mutableStateOf(false) }
    // 表示している地域が昼間か夜かに基づいてダークモード/ライトモードを自動判定
    // 昼間の地域なら端末設定にかかわらずライトモード、夜の地域ならダークモードになる
    var previewCondition by rememberSaveable { mutableStateOf(backgroundCondition) }
    var previewIsDay by rememberSaveable { mutableStateOf(backgroundIsDay) }
    val isDay = if (previewCondition != null) {
        previewIsDay ?: uiState.weatherData?.isDay ?: true
    } else {
        uiState.weatherData?.isDay ?: true
    }
    val isSystemDark = isSystemInDarkTheme()
    val isDark = ThemeMode.resolveIsDark(
        themeMode = uiState.themeMode,
        isDay = isDay,
        isSystemDark = isSystemDark
    )
    val condition = previewCondition ?: uiState.weatherData?.condition ?: WeatherCondition.CLEAR

    ExpressiveWeatherTheme(
        darkTheme = isDark,
        weatherCondition = condition,
        weatherAdaptiveColor = uiState.weatherAdaptiveThemeEnabled,
        themePreset = uiState.themePreset,
        language = uiState.language
    ) {
        val tiltState = rememberDeviceTiltState()

        CompositionLocalProvider(
            LocalAppLanguage provides uiState.language,
            LocalAppStrings provides uiState.language.strings,
            LocalDeviceTiltState provides tiltState
        ) {
            val strings = LocalAppStrings.current
            val colorScheme = MaterialTheme.colorScheme
            val atmosphere = getAtmosphereTheme(condition, colorScheme, isDay = isDay)

            val backgroundHazeState = remember { HazeState() }

            BoxWithConstraints(
                modifier = modifier.fillMaxSize()
            ) {
                val isTabletLayout = maxWidth >= 600.dp

                // Haze の入力を背景レイヤーだけに限定し、タイル自身をぼかし元へ混ぜない。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(backgroundHazeState)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(atmosphere.skyGradient)
                    )

                    if (uiState.themePreset != ThemePreset.WEATHER) {
                        // 固定テーマカラー選択時は、テーマカラーを極薄くオーバーレイして色調を調和
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            colorScheme.primary.copy(alpha = 0.08f),
                                            colorScheme.surfaceTint.copy(alpha = 0.04f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    val isRainOrThunder = condition == WeatherCondition.RAIN ||
                        condition == WeatherCondition.HEAVY_RAIN ||
                        condition == WeatherCondition.THUNDERSTORM

                    WeatherAtmosphereCanvas(
                        condition = condition,
                        atmosphere = atmosphere,
                        tiltState = tiltState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isRainOrThunder) Modifier else Modifier.blur(26.dp))
                    )

                    // Frosted glass diffusion & Day/Night dimming layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (!isDay) {
                                    Color.Black.copy(alpha = 0.32f) // 夜間地域のダーク減光
                                } else {
                                    Color.White.copy(alpha = 0.05f) // 昼間地域の上質な光拡散フロスト
                                }
                            )
                    )
                }

                // Main content
                val onSelectCityAction: (com.example.expressiveweather.data.City, Boolean) -> Unit = { city, fromSearch ->
                    if (city.id == uiState.defaultCity.id || city.id == "current") {
                        val fine = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!fine && !coarse) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            viewModel.selectCurrentLocation()
                        }
                    } else {
                        viewModel.selectCity(city, fromSearch)
                    }
                }

                val launchSettings: () -> Unit = {
                    SettingsDataHolder.set(
                        weather = uiState.weatherData,
                        language = uiState.language,
                        temperatureUnit = uiState.temperatureUnit,
                        windSpeedUnit = uiState.windSpeedUnit,
                        weatherUpdateInterval = uiState.weatherUpdateInterval,
                        weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                        themePreset = uiState.themePreset,
                        themeMode = uiState.themeMode,
                        isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                        isColorfulShapesEnabled = uiState.isColorfulShapesEnabled,
                        isRadarDebugZoomEnabled = uiState.isRadarDebugZoomEnabled,
                        isRadarDebugSourceEnabled = uiState.isRadarDebugSourceEnabled,
                        isLiveAlertNotificationEnabled = uiState.isLiveAlertNotificationEnabled,
                        alertNotificationThreshold = uiState.alertNotificationThreshold,
                        isKikikuruTestEnabled = uiState.isKikikuruTestEnabled,
                        isNwsTestEnabled = uiState.isNwsTestEnabled,
                        isGdacsTestEnabled = uiState.isGdacsTestEnabled,
                        isGdacsAlertsEnabled = uiState.isGdacsAlertsEnabled,
                        isWmoAlertsEnabled = uiState.isWmoAlertsEnabled,
                        isWmoAlwaysEnabled = uiState.isWmoAlwaysEnabled,
                        isWmoTestEnabled = uiState.isWmoTestEnabled,
                        initialSection = SettingsDataHolder.SECTION_ALL,
                        onToggleWeatherAdaptiveTheme = { viewModel.toggleWeatherAdaptiveTheme() },
                        onSelectThemePreset = { viewModel.setThemePreset(it) },
                        onSelectThemeMode = { viewModel.setThemeMode(it) },
                        onToggleFrostedGlass = { viewModel.setFrostedGlassEnabled(it) },
                        onToggleColorfulShapes = { viewModel.setColorfulShapesEnabled(it) },
                        onToggleRadarDebugZoom = { viewModel.toggleRadarDebugZoom(it) },
                        onToggleRadarDebugSource = { viewModel.toggleRadarDebugSource(it) },
                        onToggleLiveAlertNotification = { viewModel.toggleLiveAlertNotification(it) },
                        onSelectAlertNotificationThreshold = { viewModel.selectAlertNotificationThreshold(it) },
                        onToggleKikikuruTest = { viewModel.toggleKikikuruTest(it) },
                        onToggleNwsTest = { viewModel.toggleNwsTest(it) },
                        onToggleGdacsTest = { viewModel.toggleGdacsTest(it) },
                        onToggleGdacsAlerts = { viewModel.toggleGdacsAlerts(it) },
                        onToggleWmoAlerts = { viewModel.toggleWmoAlerts(it) },
                        onToggleWmoAlways = { viewModel.toggleWmoAlways(it) },
                        onToggleWmoTest = { viewModel.toggleWmoTest(it) },
                        onCyclePrecipitationDemo = { viewModel.cyclePrecipitationDemoState() },
                        onToggleTemperatureUnit = { viewModel.toggleTemperatureUnit() },
                        onSelectWindSpeedUnit = { viewModel.setWindSpeedUnit(it) },
                        onSelectWeatherUpdateInterval = { viewModel.setWeatherUpdateInterval(it) },
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        onSelectPreviewCondition = { cond, isD ->
                            previewCondition = cond
                            previewIsDay = isD
                        },
                        onClearCache = { viewModel.clearAppCache() },
                        onForceRefresh = { viewModel.refreshWeather() }
                    )
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                }

                // キキクルテストモード用: 4大キキクル（大雨・土砂・浸水・洪水）全情報＆多言語対応モックデータ
                val testKikikuruAlertInfo = remember(uiState.language) {
                    val cityName = when (uiState.language) {
                        AppLanguage.ENGLISH -> "Chiyoda (Test)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "千代田区 (测试)"
                        AppLanguage.TRADITIONAL_CHINESE -> "千代田區 (測試)"
                        AppLanguage.JAPANESE -> "千代田区 (テスト)"
                    }
                    val mockHeadline = when (uiState.language) {
                        AppLanguage.ENGLISH -> "【Alert Level 4 Equivalent】Extremely dangerous situation. Life-threatening disasters may occur at any moment."
                        AppLanguage.SIMPLIFIED_CHINESE -> "【相当于警戒等级4】情况极其危险，危及生命的重大灾害随时可能发生。"
                        AppLanguage.TRADITIONAL_CHINESE -> "【相當於警戒等級4】情況極其危險，危及生命的重大災害隨時可能發生。"
                        AppLanguage.JAPANESE -> "【警戒レベル４相当】これは、命に危険が及ぶ土砂災害や浸水害がいつ発生してもおかしくない極めて危険な状況です。"
                    }
                    MunicipalityAlertInfo(
                        municipalityCode = "1310100",
                        municipalityName = cityName,
                        prefectureCode = "130000",
                        prefectureName = "東京都",
                        alertLevel = AlertLevel.LEVEL_4,
                        disasterFlags = DisasterTargetFlags(
                            heavyRain = true,
                            landslide = true,
                            inundation = true,
                            flood = true
                        ),
                        activeWarnings = listOf(
                            MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true),
                            MunicipalityWarningItem("04", "洪水警報", "発表", isWarning = true),
                            MunicipalityWarningItem("14", "雷注意報", "継続", isWarning = false)
                        ),
                        headlineText = mockHeadline,
                        targetDisasterPrimary = DisasterType.HEAVY_RAIN,
                        disasterDetails = listOf(
                            KikikuruDisasterDetail(
                                disasterType = DisasterType.HEAVY_RAIN,
                                alertLevel = AlertLevel.LEVEL_4,
                                activeWarnings = listOf(MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true)),
                                headlineText = mockHeadline
                            ),
                            KikikuruDisasterDetail(
                                disasterType = DisasterType.LANDSLIDE,
                                alertLevel = AlertLevel.LEVEL_4,
                                activeWarnings = listOf(MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true)),
                                headlineText = mockHeadline
                            ),
                            KikikuruDisasterDetail(
                                disasterType = DisasterType.INUNDATION,
                                alertLevel = AlertLevel.LEVEL_3,
                                activeWarnings = listOf(MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true)),
                                headlineText = mockHeadline
                            ),
                            KikikuruDisasterDetail(
                                disasterType = DisasterType.FLOOD,
                                alertLevel = AlertLevel.LEVEL_2,
                                activeWarnings = listOf(MunicipalityWarningItem("04", "洪水注意報", "発表", isWarning = false)),
                                headlineText = mockHeadline
                            )
                        )
                    )
                }

                if (isTabletLayout) {
                    if (isTabletPanelExpanded) {
                        BackHandler {
                            isTabletPanelExpanded = false
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Collapsed Side Rail (Always 76dp steady)
                            TabletNavigationRail(
                                onExpandClick = { isTabletPanelExpanded = true },
                                onSettingsClick = launchSettings
                            )

                        // Right Content Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (uiState.weatherData != null) {
                                val weather = uiState.weatherData!!
                                val tabletScrollState = rememberScrollState()
                                val pullToRefreshState = rememberPullToRefreshState()

                                PullToRefreshBox(
                                    isRefreshing = uiState.isRefreshing,
                                    onRefresh = { viewModel.refreshWeather() },
                                    state = pullToRefreshState,
                                    modifier = Modifier.fillMaxSize(),
                                    indicator = {
                                        PullToRefreshDefaults.LoadingIndicator(
                                            state = pullToRefreshState,
                                            isRefreshing = uiState.isRefreshing,
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .statusBarsPadding()
                                                .padding(top = 8.dp)
                                        )
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(tabletScrollState)
                                            .statusBarsPadding()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 24.dp)
                                            .padding(bottom = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 960.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // 1. Tablet Wide Weather Header (City, FeelsLike/MinMax, Big Temp + Icon)
                                            TabletWeatherHeader(
                                                weather = weather,
                                                temperatureUnit = uiState.temperatureUnit,
                                                atmosphere = atmosphere
                                            )

                                            // Cache Status Badge
                                            WeatherCacheStatusBadge(
                                                fetchedAtMillis = weather.fetchedAtMillis,
                                                isFromCache = uiState.isFromCache,
                                                isStale = uiState.isStale,
                                                isRefreshing = uiState.isRefreshing,
                                                onRefresh = { viewModel.refreshWeather() },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            )

                                            // Saved Cities Warning Alert
                                            val savedMaxAlert = uiState.savedCitiesMaxAlert
                                            if (savedMaxAlert != null && savedMaxAlert.alertLevel.level >= 2 && !uiState.selectedCity.name.contains(savedMaxAlert.municipalityName)) {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .clickable {
                                                            val targetCity = uiState.savedCities.firstOrNull { it.name.contains(savedMaxAlert.municipalityName) || savedMaxAlert.municipalityName.contains(it.name) }
                                                            if (targetCity != null) {
                                                                viewModel.selectCity(targetCity)
                                                            }
                                                        },
                                                    color = Color(savedMaxAlert.alertLevel.colorHex).copy(alpha = 0.95f),
                                                    tonalElevation = 4.dp
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Warning,
                                                            contentDescription = null,
                                                            tint = Color(savedMaxAlert.alertLevel.textColorHex),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = "⚠️ 登録地点［${savedMaxAlert.municipalityName}］に【レベル${savedMaxAlert.alertLevel.level} ${savedMaxAlert.alertLevel.title}】が発表中 (タップして表示)",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(savedMaxAlert.alertLevel.textColorHex),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }

                                            // Kikikuru Alert Warning Banner
                                            val currentAlertInfo = if (uiState.isKikikuruTestEnabled) testKikikuruAlertInfo else (uiState.currentAlert ?: weather.alertInfo)
                                            KikikuruWarningBanner(
                                                alertInfo = currentAlertInfo,
                                                onNavigateToRadar = { targetLayerId ->
                                                    onNavigateToRadarDetail()
                                                }
                                            )

                                            val activeGlobalAlertsTablet = (uiState.globalAlertsOverride ?: weather.globalAlerts)
                                                .filter { uiState.isGdacsAlertsEnabled || it.source != AlertSource.GDACS }
                                            MunicipalityDisasterBanner(
                                                alertInfo = currentAlertInfo,
                                                jmaWarnings = weather.jmaWarnings,
                                                jmaOverviewText = weather.jmaOverviewText,
                                                globalAlerts = activeGlobalAlertsTablet,
                                                onBannerClick = {
                                                    onNavigateToRadarDetail()
                                                }
                                            )

                                            // 降雨予測・接近＆止むまでのカウントダウンボックス (雨天時は確実に表示)
                                            val rawNowcastTablet = uiState.precipitationNowcastOverride ?: weather.precipitationNowcast
                                            val isRainWeatherTablet = PrecipitationAnalyzer.isRainWeather(weather)
                                            val activeNowcastTablet = if (rawNowcastTablet != null && rawNowcastTablet.status != RainStatus.DRY) {
                                                rawNowcastTablet
                                            } else if (isRainWeatherTablet) {
                                                PrecipitationAnalyzer.createFallbackRainNowcast(weather)
                                            } else {
                                                rawNowcastTablet
                                            }
                                            if (activeNowcastTablet != null && activeNowcastTablet.status != RainStatus.DRY) {
                                                PrecipitationNowcastCard(
                                                    nowcast = activeNowcastTablet,
                                                    isDark = atmosphere.isDark,
                                                    condition = condition,
                                                    hazeState = backgroundHazeState,
                                                    isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                    onCardClick = onNavigateToRadarDetail,
                                                    onRadarClick = onNavigateToRadarDetail,
                                                    onCycleTestMode = { viewModel.cyclePrecipitationDemoState() }
                                                )
                                            }

                                            // Hourly Forecast Section
                                            HourlyForecastSection(
                                                hourlyList = weather.hourlyForecast,
                                                selectedHour = uiState.selectedHour?.hour,
                                                temperatureUnit = uiState.temperatureUnit,
                                                atmosphere = atmosphere,
                                                onHourSelected = {
                                                    viewModel.selectHourlyForecast(it)
                                                    onNavigateToHourlyDetail(it.hour)
                                                },
                                                onCardClick = {
                                                    onNavigateToHourlyDetail(uiState.selectedHour?.hour)
                                                },
                                                condition = condition,
                                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                hazeState = backgroundHazeState
                                            )

                                            // 7-Day Weekly Forecast Section
                                            WeeklyForecastSection(
                                                dailyList = weather.dailyForecast,
                                                temperatureUnit = uiState.temperatureUnit,
                                                atmosphere = atmosphere,
                                                onDayClick = { dayIndex ->
                                                    onNavigateToTodayDetail(dayIndex, null)
                                                },
                                                condition = condition,
                                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                hazeState = backgroundHazeState,
                                                isColorful = uiState.isColorfulShapesEnabled
                                            )

                                            // Radar Card Section
                                            val tabletCity = uiState.selectedCity
                                            RadarCardSection(
                                                latitude = tabletCity.latitude,
                                                longitude = tabletCity.longitude,
                                                cityName = tabletCity.localizedName(uiState.language),
                                                country = tabletCity.country,
                                                atmosphere = atmosphere,
                                                onClick = onNavigateToRadarDetail,
                                                condition = condition,
                                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                hazeState = backgroundHazeState,
                                                isColorful = uiState.isColorfulShapesEnabled,
                                                showSourceBadge = uiState.isRadarDebugSourceEnabled,
                                                showZoomBadge = uiState.isRadarDebugZoomEnabled
                                            )

                                            // Bento Grid
                                            WeatherBentoGrid(
                                                weather = weather,
                                                atmosphere = atmosphere,
                                                cardOrder = uiState.bentoCardOrder,
                                                onOrderChange = { viewModel.updateBentoOrder(it) },
                                                onCardClick = { card ->
                                                    if (card == BentoCardType.WIND) {
                                                        onNavigateToWindDetail()
                                                    } else {
                                                        onNavigateToBentoDetail(card)
                                                    }
                                                },
                                                condition = condition,
                                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                hazeState = backgroundHazeState,
                                                isColorful = uiState.isColorfulShapesEnabled,
                                                windSpeedUnit = uiState.windSpeedUnit
                                            )

                                            // Footer Status
                                            WeatherFooterStatusSection(
                                                weather = weather,
                                                isFromCache = uiState.isFromCache,
                                                isStale = uiState.isStale,
                                                isRefreshing = uiState.isRefreshing,
                                                onRefresh = { viewModel.refreshWeather() },
                                                condition = condition,
                                                isDark = isDark,
                                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                hazeState = backgroundHazeState
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            } else if (uiState.errorMessage != null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Thunderstorm,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = uiState.errorMessage ?: strings.errorOccurred,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        androidx.compose.material3.Button(
                                            onClick = { viewModel.refreshWeather() },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF1E1A18)
                                            )
                                        ) {
                                            Text(strings.retry, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                WeatherDashboardSkeleton(
                                    modifier = Modifier.fillMaxSize(),
                                    isDark = isDark
                                )
                            }
                        }
                    }

                    // 2. オーバーレイ背景スクリム（フェードイン/フェードアウトのみで全画面移動のGPU負荷をゼロ化）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isTabletPanelExpanded,
                        enter = androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 200, easing = androidx.compose.animation.core.LinearEasing)
                        ),
                        exit = androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 160, easing = androidx.compose.animation.core.LinearEasing)
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(99f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.32f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { isTabletPanelExpanded = false }
                                )
                        )
                    }

                    // 3. 展開ナビゲーションドロワー（320dp単体の軽快な Material 3 Expressive スプリングスライド）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isTabletPanelExpanded,
                        enter = androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.82f,
                                stiffness = 420f
                            )
                        ) + androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 150)
                        ),
                        exit = androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 1.0f,
                                stiffness = 600f
                            )
                        ) + androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 120)
                        ),
                        modifier = Modifier
                            .fillMaxHeight()
                            .zIndex(100f)
                    ) {
                        TabletNavigationDrawer(
                            onCollapseClick = { isTabletPanelExpanded = false },
                            currentCity = uiState.selectedCity,
                            defaultCity = uiState.defaultCity,
                            savedCities = uiState.savedCities,
                            searchResults = uiState.searchResults,
                            isSearching = uiState.isSearching,
                            onCitySelected = { city, fromSearch ->
                                onSelectCityAction(city, fromSearch)
                                isTabletPanelExpanded = false
                            },
                            onRemoveSavedCity = { viewModel.removeSavedCity(it) },
                            onSearchQueryChange = { viewModel.searchCities(it) },
                            onSettingsClick = {
                                isTabletPanelExpanded = false
                                launchSettings()
                            }
                        )
                    }
                }
            } else {
                    // Mobile Phone Layout
                    if (uiState.weatherData != null) {
                    val weather = uiState.weatherData!!
                    val scrollState = rememberScrollState()
                    val hazeState = remember { HazeState() }
                    val density = LocalDensity.current
                    val pullToRefreshState = rememberPullToRefreshState()

                    // 巨大な気温と天気が画面上部コンパクトヘッダーへとシームレスに変形するスクロール距離（240dp）
                    val collapseDistancePx = with(density) { 240.dp.toPx() }
                    val scrollProgress by remember {
                        derivedStateOf {
                            (scrollState.value.toFloat() / collapseDistancePx).coerceIn(0f, 1f)
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshWeather() },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                state = pullToRefreshState,
                                isRefreshing = uiState.isRefreshing,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(top = 8.dp)
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 120.dp), // Extra padding for floating toolbar
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 展開時のモーフィングヘッダーの高さ分のスペーサー（ステータスバー余白 + 展開時コンテンツ314dp）
                            Spacer(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .height(314.dp)
                            )

                            // キャッシュ・古いデータ表示時の上部ステータスピルバッジ
                            WeatherCacheStatusBadge(
                                fetchedAtMillis = weather.fetchedAtMillis,
                                isFromCache = uiState.isFromCache,
                                isStale = uiState.isStale,
                                isRefreshing = uiState.isRefreshing,
                                onRefresh = { viewModel.refreshWeather() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            // 複数登録地点（実家・自宅など）で警戒レベル2以上の警報が発表中の場合のヘッダーアラート
                            val savedMaxAlert = uiState.savedCitiesMaxAlert
                            if (savedMaxAlert != null && savedMaxAlert.alertLevel.level >= 2 && !uiState.selectedCity.name.contains(savedMaxAlert.municipalityName)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            val targetCity = uiState.savedCities.firstOrNull { it.name.contains(savedMaxAlert.municipalityName) || savedMaxAlert.municipalityName.contains(it.name) }
                                            if (targetCity != null) {
                                                viewModel.selectCity(targetCity)
                                            }
                                        },
                                    color = Color(savedMaxAlert.alertLevel.colorHex).copy(alpha = 0.95f),
                                    tonalElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = Color(savedMaxAlert.alertLevel.textColorHex),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "⚠️ 登録地点［${savedMaxAlert.municipalityName}］に【レベル${savedMaxAlert.alertLevel.level} ${savedMaxAlert.alertLevel.title}】が発表中 (タップして表示)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(savedMaxAlert.alertLevel.textColorHex),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // 1番上のタイル: 警戒レベル & キキクル警報バナー (Level 2以上で該当カラー塗りつぶしカード)
                            val currentAlertInfo = if (uiState.isKikikuruTestEnabled) testKikikuruAlertInfo else (uiState.currentAlert ?: weather.alertInfo)
                            KikikuruWarningBanner(
                                alertInfo = currentAlertInfo,
                                onNavigateToRadar = { targetLayerId ->
                                    onNavigateToRadarDetail()
                                }
                            )

                            val activeGlobalAlertsMobile = (uiState.globalAlertsOverride ?: weather.globalAlerts)
                                .filter { uiState.isGdacsAlertsEnabled || it.source != AlertSource.GDACS }
                            MunicipalityDisasterBanner(
                                alertInfo = currentAlertInfo,
                                jmaWarnings = weather.jmaWarnings,
                                jmaOverviewText = weather.jmaOverviewText,
                                globalAlerts = activeGlobalAlertsMobile,
                                onBannerClick = {
                                    onNavigateToRadarDetail()
                                }
                            )

                            // 降雨予測・接近＆止むまでのカウントダウンボックス (雨天時は確実に表示)
                            val rawNowcastMobile = uiState.precipitationNowcastOverride ?: weather.precipitationNowcast
                            val isRainWeatherMobile = PrecipitationAnalyzer.isRainWeather(weather)
                            val activeNowcastMobile = if (rawNowcastMobile != null && rawNowcastMobile.status != RainStatus.DRY) {
                                rawNowcastMobile
                            } else if (isRainWeatherMobile) {
                                PrecipitationAnalyzer.createFallbackRainNowcast(weather)
                            } else {
                                rawNowcastMobile
                            }
                            if (activeNowcastMobile != null && activeNowcastMobile.status != RainStatus.DRY) {
                                PrecipitationNowcastCard(
                                    nowcast = activeNowcastMobile,
                                    isDark = atmosphere.isDark,
                                    condition = condition,
                                    hazeState = backgroundHazeState,
                                    isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                    onCardClick = onNavigateToRadarDetail,
                                    onRadarClick = onNavigateToRadarDetail,
                                    onCycleTestMode = { viewModel.cyclePrecipitationDemoState() }
                                )
                            }

                            // Panel 1: Hourly Forecast (Slim Capsule Shape matching mockup panel 1)
                            HourlyForecastSection(
                                hourlyList = weather.hourlyForecast,
                                selectedHour = uiState.selectedHour?.hour,
                                temperatureUnit = uiState.temperatureUnit,
                                atmosphere = atmosphere,
                                onHourSelected = {
                                    viewModel.selectHourlyForecast(it)
                                    onNavigateToHourlyDetail(it.hour)
                                },
                                onCardClick = {
                                    onNavigateToHourlyDetail(uiState.selectedHour?.hour)
                                },
                                condition = condition,
                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                hazeState = backgroundHazeState
                            )

                            // Panel 2: 7-Day Weekly Forecast (Rounded Rectangle matching mockup panel 2)
                            WeeklyForecastSection(
                                dailyList = weather.dailyForecast,
                                temperatureUnit = uiState.temperatureUnit,
                                atmosphere = atmosphere,
                                onDayClick = { dayIndex ->
                                    onNavigateToTodayDetail(dayIndex, null)
                                },
                                condition = condition,
                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                hazeState = backgroundHazeState,
                                isColorful = uiState.isColorfulShapesEnabled
                            )

                            // Precipitation Radar Box (JMA for Japan, RainViewer for Overseas)
                            val currentCity = uiState.selectedCity
                            RadarCardSection(
                                latitude = currentCity.latitude,
                                longitude = currentCity.longitude,
                                cityName = currentCity.localizedName(uiState.language),
                                country = currentCity.country,
                                atmosphere = atmosphere,
                                onClick = onNavigateToRadarDetail,
                                condition = condition,
                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                hazeState = backgroundHazeState,
                                isColorful = uiState.isColorfulShapesEnabled,
                                showSourceBadge = uiState.isRadarDebugSourceEnabled,
                                showZoomBadge = uiState.isRadarDebugZoomEnabled
                            )

                            // Panel 3 & 4: Weather Bento Grid with Material 3 Expressive Shapes & Reorderable Drag
                            WeatherBentoGrid(
                                weather = weather,
                                atmosphere = atmosphere,
                                cardOrder = uiState.bentoCardOrder,
                                onOrderChange = { viewModel.updateBentoOrder(it) },
                                onCardClick = { card ->
                                    if (card == BentoCardType.WIND) {
                                        onNavigateToWindDetail()
                                    } else {
                                        onNavigateToBentoDetail(card)
                                    }
                                },
                                condition = condition,
                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                hazeState = backgroundHazeState,
                                isColorful = uiState.isColorfulShapesEnabled,
                                windSpeedUnit = uiState.windSpeedUnit
                            )

                            // Panel 5: Weather Footer Info & Data Source Card
                            WeatherFooterStatusSection(
                                weather = weather,
                                isFromCache = uiState.isFromCache,
                                isStale = uiState.isStale,
                                isRefreshing = uiState.isRefreshing,
                                onRefresh = { viewModel.refreshWeather() },
                                condition = condition,
                                isDark = isDark,
                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                hazeState = backgroundHazeState
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // スクロール量に完全同期して巨大気温・天気表示からコンパクトヘッダーへとシームレスに変形するモーフィングヘッダー
                        MorphingWeatherHeader(
                            weather = weather,
                            temperatureUnit = uiState.temperatureUnit,
                            atmosphere = atmosphere,
                            isDark = isDark,
                            progress = scrollProgress,
                            hazeState = hazeState,
                            onCityClick = { viewModel.openCitySheet() },
                            onDetailClick = { /* 気温部タップによる詳細遷移はユーザー指示により廃止 */ },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                } else if (uiState.errorMessage != null) {
                    // Error State with Retry button
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Thunderstorm,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.errorMessage ?: strings.errorOccurred,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            androidx.compose.material3.Button(
                                onClick = { viewModel.refreshWeather() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1E1A18)
                                )
                            ) {
                                Text(strings.retry, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Loading State: Skeleton Screen with Shimmer effect mirroring normal weather layout
                    WeatherDashboardSkeleton(
                        modifier = Modifier.fillMaxSize(),
                        isDark = isDark
                    )
                }

                    // Floating Bottom Toolbar with Morphing Container (Attached to saved cities & search)
                    if (uiState.weatherData != null || uiState.errorMessage == null) {
                        // 登録地名一覧ポップアップ（SAVED_CITIES）表示中、それ以外の場所をタップしたら閉じるための全画面透明スクリム
                        if (toolbarMode == ToolbarMode.SAVED_CITIES) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { toolbarMode = ToolbarMode.NORMAL }
                                    )
                            )
                            BackHandler {
                                toolbarMode = ToolbarMode.NORMAL
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .imePadding()
                                .navigationBarsPadding(),
                            contentAlignment = Alignment.Center
                        ) {
                            ExpressiveFloatingToolbar(
                                currentCity = uiState.selectedCity,
                                defaultCity = uiState.defaultCity,
                                savedCities = uiState.savedCities,
                                recentSearches = uiState.recentSearches,
                                searchResults = uiState.searchResults,
                                isSearching = uiState.isSearching,
                                isDark = isDark,
                                toolbarMode = toolbarMode,
                                onToolbarModeChange = { toolbarMode = it },
                                onCitySelected = onSelectCityAction,
                                onRemoveSavedCity = { viewModel.removeSavedCity(it) },
                                onSaveCity = { viewModel.saveCity(it) },
                                onSearchQueryChange = { viewModel.searchCities(it) },
                                onSettingsClick = { viewModel.openSettingsSheet() }
                            )
                        }
                    }
                }

                previewCondition?.let { preview ->
                    BackgroundPreviewPill(
                        condition = preview,
                        isDay = isDay,
                        onSettings = launchSettings,
                        onStop = { previewCondition = null },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = if (uiState.weatherData != null) 88.dp else 16.dp)
                    )
                }

                // Settings Modal Bottom Sheet
                if (uiState.showSettingsSheet) {
                    com.example.expressiveweather.ui.components.SettingsBottomSheet(
                        currentUnit = uiState.temperatureUnit,
                        currentLanguage = uiState.language,
                        weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                        onToggleUnit = { viewModel.toggleTemperatureUnit() },
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        onToggleWeatherAdaptiveTheme = { viewModel.toggleWeatherAdaptiveTheme() },
                        onSelectCurrentLocation = {
                            val fine = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!fine && !coarse) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                viewModel.selectCurrentLocation()
                            }
                        },
                        onNavigateToSettings = { section ->
                            SettingsDataHolder.set(
                                weather = uiState.weatherData,
                                language = uiState.language,
                                temperatureUnit = uiState.temperatureUnit,
                                windSpeedUnit = uiState.windSpeedUnit,
                                weatherUpdateInterval = uiState.weatherUpdateInterval,
                                weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                                themePreset = uiState.themePreset,
                                themeMode = uiState.themeMode,
                                isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                isColorfulShapesEnabled = uiState.isColorfulShapesEnabled,
                                isRadarDebugZoomEnabled = uiState.isRadarDebugZoomEnabled,
                                isRadarDebugSourceEnabled = uiState.isRadarDebugSourceEnabled,
                                isLiveAlertNotificationEnabled = uiState.isLiveAlertNotificationEnabled,
                                alertNotificationThreshold = uiState.alertNotificationThreshold,
                                isKikikuruTestEnabled = uiState.isKikikuruTestEnabled,
                                isNwsTestEnabled = uiState.isNwsTestEnabled,
                                isGdacsTestEnabled = uiState.isGdacsTestEnabled,
                                isGdacsAlertsEnabled = uiState.isGdacsAlertsEnabled,
                                isWmoAlertsEnabled = uiState.isWmoAlertsEnabled,
                                isWmoAlwaysEnabled = uiState.isWmoAlwaysEnabled,
                                isWmoTestEnabled = uiState.isWmoTestEnabled,
                                initialSection = section,
                                onToggleWeatherAdaptiveTheme = { viewModel.toggleWeatherAdaptiveTheme() },
                                onSelectThemePreset = { viewModel.setThemePreset(it) },
                                onSelectThemeMode = { viewModel.setThemeMode(it) },
                                onToggleFrostedGlass = { viewModel.setFrostedGlassEnabled(it) },
                                onToggleColorfulShapes = { viewModel.setColorfulShapesEnabled(it) },
                                onToggleRadarDebugZoom = { viewModel.toggleRadarDebugZoom(it) },
                                onToggleRadarDebugSource = { viewModel.toggleRadarDebugSource(it) },
                                onToggleLiveAlertNotification = { viewModel.toggleLiveAlertNotification(it) },
                                onSelectAlertNotificationThreshold = { viewModel.selectAlertNotificationThreshold(it) },
                                onToggleKikikuruTest = { viewModel.toggleKikikuruTest(it) },
                                onToggleNwsTest = { viewModel.toggleNwsTest(it) },
                                onToggleGdacsTest = { viewModel.toggleGdacsTest(it) },
                                onToggleGdacsAlerts = { viewModel.toggleGdacsAlerts(it) },
                                onToggleWmoAlerts = { viewModel.toggleWmoAlerts(it) },
                                onToggleWmoAlways = { viewModel.toggleWmoAlways(it) },
                                onToggleWmoTest = { viewModel.toggleWmoTest(it) },
                                onCyclePrecipitationDemo = { viewModel.cyclePrecipitationDemoState() },
                                onToggleTemperatureUnit = { viewModel.toggleTemperatureUnit() },
                                onSelectWindSpeedUnit = { viewModel.setWindSpeedUnit(it) },
                                onSelectWeatherUpdateInterval = { viewModel.setWeatherUpdateInterval(it) },
                                onSelectLanguage = { viewModel.setLanguage(it) },
                                onSelectPreviewCondition = { cond, isD ->
                                    previewCondition = cond
                                    previewIsDay = isD
                                },
                                onClearCache = { viewModel.clearAppCache() },
                                onForceRefresh = { viewModel.refreshWeather() }
                            )
                            val intent = Intent(context, SettingsActivity::class.java).apply {
                                putExtra("initial_section", section)
                            }
                            context.startActivity(intent)
                            viewModel.closeSettingsSheet()
                        },
                        onDismiss = { viewModel.closeSettingsSheet() },
                        backgroundPreviewContent = {
                            BackgroundPreviewSettings(
                                condition = previewCondition,
                                isDay = previewIsDay ?: uiState.weatherData?.isDay ?: true,
                                onConditionChange = { previewCondition = it },
                                onDayChange = { previewIsDay = it }
                            )
                        }
                    )
                }
            }
        }
    }
}
