package io.github.nemynew.expressiveweather.ui.main

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
import androidx.lifecycle.repeatOnLifecycle
import io.github.nemynew.expressiveweather.ui.components.BackgroundPreviewPill
import io.github.nemynew.expressiveweather.ui.components.BackgroundPreviewSettings
import io.github.nemynew.expressiveweather.ui.screens.SettingsActivity
import io.github.nemynew.expressiveweather.ui.screens.SettingsDataHolder
import io.github.nemynew.expressiveweather.ui.radar.RadarCardSection
import io.github.nemynew.expressiveweather.ui.radar.RadarTileHttpClient
import io.github.nemynew.expressiveweather.theme.ThemeMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import io.github.nemynew.expressiveweather.theme.ThemePreset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.AlertSource
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import io.github.nemynew.expressiveweather.data.getLocalizedLabel
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.data.PrecipitationAnalyzer
import io.github.nemynew.expressiveweather.data.RadarDataSource
import io.github.nemynew.expressiveweather.data.RadarProviderRegistry
import io.github.nemynew.expressiveweather.data.RadarSourceMode
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.getAtmosphereTheme
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.nemynew.expressiveweather.data.CurrentLocationProvider
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.local.WeatherDatabase
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import io.github.nemynew.expressiveweather.ui.components.BentoCardType
import io.github.nemynew.expressiveweather.ui.components.ExpressiveFloatingToolbar
import io.github.nemynew.expressiveweather.ui.components.ToolbarMode
import io.github.nemynew.expressiveweather.ui.components.HourlyForecastSection
import io.github.nemynew.expressiveweather.ui.components.LocalSnowScrollOffsetProvider
import io.github.nemynew.expressiveweather.ui.components.MorphingWeatherHeader
import io.github.nemynew.expressiveweather.ui.components.SnowForegroundOverlay
import io.github.nemynew.expressiveweather.ui.components.WeatherAtmosphereCanvas
import io.github.nemynew.expressiveweather.ui.util.LocalDeviceTiltState
import io.github.nemynew.expressiveweather.ui.util.rememberDeviceTiltState
import io.github.nemynew.expressiveweather.ui.components.WeatherBentoGrid
import io.github.nemynew.expressiveweather.ui.components.WeatherCacheStatusBadge
import io.github.nemynew.expressiveweather.ui.components.WeatherDashboardSkeleton
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.data.BugReportSnapshot
import io.github.nemynew.expressiveweather.data.RadarRepository
import io.github.nemynew.expressiveweather.ui.components.BugReportCardSection
import io.github.nemynew.expressiveweather.ui.components.WeatherFooterStatusSection
import io.github.nemynew.expressiveweather.ui.components.TabletNavigationRail
import io.github.nemynew.expressiveweather.ui.components.TabletNavigationDrawer
import io.github.nemynew.expressiveweather.ui.components.TabletWeatherHeader
import androidx.compose.ui.zIndex
import io.github.nemynew.expressiveweather.ui.components.WeeklyForecastSection
import io.github.nemynew.expressiveweather.ui.components.KikikuruWarningBanner
import io.github.nemynew.expressiveweather.ui.components.MunicipalityDisasterBanner
import io.github.nemynew.expressiveweather.ui.components.PrecipitationNowcastCard

import io.github.nemynew.expressiveweather.data.RainStatus
import io.github.nemynew.expressiveweather.data.KikikuruBackgroundMonitor
import io.github.nemynew.expressiveweather.data.MunicipalityAlertInfo
import io.github.nemynew.expressiveweather.data.AlertLevel
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.DisasterTargetFlags
import io.github.nemynew.expressiveweather.data.DisasterType
import io.github.nemynew.expressiveweather.data.KikikuruDisasterDetail
import io.github.nemynew.expressiveweather.data.MunicipalityWarningItem
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import android.os.Build
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.GlobalAlertItem
import io.github.nemynew.expressiveweather.ui.screens.AlertDetailActivity
import io.github.nemynew.expressiveweather.ui.screens.AlertDetailDataHolder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    context: Context = LocalContext.current,
    viewModel: MainScreenViewModel = viewModel(
        factory = remember(context) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
    onNavigateToAlertDetail: (AlertSource?) -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundCondition: WeatherCondition? = null,
    backgroundIsDay: Boolean? = null
) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setLiveAlertNotificationEnabled(true)
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notif = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notif) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.refreshCurrentLocation()
        }
        requestNotificationPermissionIfNeeded()
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
            // セットアップ等で手動都市を選んだ直後に不要な位置情報権限ダイアログを出さない
            if (viewModel.uiState.value.selectedCity.id == "current") {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                requestNotificationPermissionIfNeeded()
            }
        } else {
            viewModel.refreshCurrentLocation()
            requestNotificationPermissionIfNeeded()
        }
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
            viewModel.stopContinuousLocationUpdates()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    // 選択中都市のリージョンに応じたレーダー配信元のみ事前接続（不要な他リージョンへの接続を防止）
    LaunchedEffect(uiState.selectedCity.id, uiState.selectedCity.latitude, uiState.selectedCity.longitude) {
        val city = uiState.selectedCity
        val provider = RadarProviderRegistry.resolveProvider(
            mode = RadarSourceMode.AUTO,
            latitude = city.latitude,
            longitude = city.longitude,
            country = city.country
        )
        when (provider) {
            RadarDataSource.JMA_NOWCAST ->
                RadarTileHttpClient.prewarm("https://www.jma.go.jp/bosai/jmatile/")
            RadarDataSource.US_NEXRAD_HRRR ->
                RadarTileHttpClient.prewarm("https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/ridge::USCOMP-N0Q-0/4/3/6.png")
            RadarDataSource.RAIN_VIEWER ->
                RadarTileHttpClient.prewarm("https://tilecache.rainviewer.com")
        }
    }
    var toolbarMode by rememberSaveable { mutableStateOf(ToolbarMode.NORMAL) }
    var isTabletPanelExpanded by rememberSaveable { mutableStateOf(false) }
    // 表示している地域が昼間か夜かに基づいてダークモード/ライトモードを自動判定
    // 昼間の地域なら端末設定にかかわらずライトモード、夜の地域ならダークモードになる
    var previewCondition by rememberSaveable { mutableStateOf(backgroundCondition) }
    var previewIsDay by rememberSaveable { mutableStateOf(backgroundIsDay) }
    val repoPreview by WeatherRepository.previewConditionOverride.collectAsState()
    LaunchedEffect(repoPreview) {
        repoPreview?.let { (cond, isD) ->
            previewCondition = cond
            previewIsDay = isD
        }
    }
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
        accentColorMode = uiState.accentColorMode,
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

                    val hasCrispAtmosphere = condition.isPrecipitating

                    WeatherAtmosphereCanvas(
                        condition = condition,
                        atmosphere = atmosphere,
                        tiltState = tiltState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (hasCrispAtmosphere) Modifier else Modifier.blur(26.dp))
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
                val expressHaptics = LocalExpressiveHaptics.current
                var wasRefreshing by remember { mutableStateOf(false) }
                LaunchedEffect(uiState.isRefreshing, uiState.errorMessage) {
                    if (wasRefreshing && !uiState.isRefreshing) {
                        if (uiState.errorMessage != null) {
                            expressHaptics.play(ExpressiveHapticPattern.ERROR_REJECT)
                        } else {
                            expressHaptics.play(ExpressiveHapticPattern.REFRESH_SUCCESS)
                        }
                    }
                    wasRefreshing = uiState.isRefreshing
                }

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                val activeWeatherSignatureKey = remember(previewCondition, uiState.weatherData?.city?.id, uiState.weatherData?.condition) {
                    previewCondition?.let { "preview:${it.name}" }
                        ?: uiState.weatherData?.let { "${it.city.id}:${it.condition.name}" }
                }
                var lastPlayedSignatureKey by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(activeWeatherSignatureKey, lifecycleOwner) {
                    val key = activeWeatherSignatureKey ?: return@LaunchedEffect
                    lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                        if (lastPlayedSignatureKey != key) {
                            lastPlayedSignatureKey = key
                            expressHaptics.playWeatherSignature(condition)
                        }
                    }
                }

                // 雨天時は表示開始時に短時間のみ雨粒の質感ハプティクスを再生（バッテリー・LRA消耗を防ぐため有限回数で終了）
                LaunchedEffect(activeWeatherSignatureKey, condition, lifecycleOwner) {
                    if (condition == WeatherCondition.RAIN || condition == WeatherCondition.HEAVY_RAIN) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                            val isHeavy = condition == WeatherCondition.HEAVY_RAIN
                            val intervalsMs = if (isHeavy) {
                                longArrayOf(190L, 280L, 150L, 340L, 220L)
                            } else {
                                longArrayOf(340L, 520L, 260L, 480L, 380L)
                            }
                            val strengths = if (isHeavy) {
                                floatArrayOf(0.84f, 0.96f, 0.80f, 0.98f, 0.86f)
                            } else {
                                floatArrayOf(0.76f, 0.88f, 0.72f, 0.92f, 0.78f)
                            }
                            kotlinx.coroutines.delay(550L)
                            for (step in strengths.indices) {
                                expressHaptics.playRaindropPulse(strengths[step])
                                kotlinx.coroutines.delay(intervalsMs[step])
                            }
                        }
                    }
                }

                val onSelectCityAction: (io.github.nemynew.expressiveweather.data.City, Boolean) -> Unit = { city, fromSearch ->
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

                LaunchedEffect(uiState.showCitySheet, isTabletLayout) {
                    if (uiState.showCitySheet) {
                        if (isTabletLayout) {
                            isTabletPanelExpanded = true
                        } else {
                            toolbarMode = ToolbarMode.SAVED_CITIES
                        }
                        viewModel.closeCitySheet()
                    }
                }

                val openSettingsSection: (String) -> Unit = { section ->
                    SettingsDataHolder.weatherData = uiState.weatherData
                    SettingsDataHolder.language = uiState.language
                    SettingsDataHolder.initialSection = section
                    val intent = Intent(context, SettingsActivity::class.java).apply {
                        putExtra("initial_section", section)
                    }
                    context.startActivity(intent)
                }

                val launchSettings: () -> Unit = {
                    openSettingsSection(SettingsDataHolder.SECTION_ALL)
                }

                // キキクルテストモード用: 4大キキクル（大雨・土砂・浸水・洪水）全情報＆多言語対応モックデータ
                val testKikikuruAlertInfo = remember(uiState.language) {
                    val cityName = when (uiState.language) {
                        AppLanguage.ENGLISH -> "Chiyoda (Test)"
                        AppLanguage.KOREAN -> "지요다구 (테스트)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "千代田区 (测试)"
                        AppLanguage.TRADITIONAL_CHINESE -> "千代田區 (測試)"
                        AppLanguage.SPANISH -> "Chiyoda (Prueba)"
                        AppLanguage.FRENCH -> "Chiyoda (Test)"
                        AppLanguage.PORTUGUESE -> "Chiyoda (Teste)"
                        AppLanguage.JAPANESE -> "千代田区 (テスト)"
                    }
                    val mockHeadline = when (uiState.language) {
                        AppLanguage.ENGLISH -> "【Alert Level 4 Equivalent】Extremely dangerous situation. Life-threatening disasters may occur at any moment."
                        AppLanguage.KOREAN -> "【경계 단계 4 상당】생명에 위험이 닥칠 수 있는 산사태·침수 피해가 언제 발생해도 이상하지 않은 매우 위험한 상황입니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "【相当于警戒等级4】情况极其危险，危及生命的重大灾害随时可能发生。"
                        AppLanguage.TRADITIONAL_CHINESE -> "【相當於警戒等級4】情況極其危險，危及生命的重大災害隨時可能發生。"
                        AppLanguage.SPANISH -> "【Equivalente a Nivel de Alerta 4】Situación extremadamente peligrosa. Pueden ocurrir desastres potencialmente mortales en cualquier momento."
                        AppLanguage.FRENCH -> "【Équivalent Niveau d'alerte 4】Situation extrêmement dangereuse. Des catastrophes mettant la vie en danger peuvent survenir à tout moment."
                        AppLanguage.PORTUGUESE -> "【Equivalente ao Nível de Alerta 4】Situação extremamente perigosa. Desastres com risco de vida podem ocorrer a qualquer momento."
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

                val navigateToAlertDetailPage: (AlertSource?, WeatherData?, MunicipalityAlertInfo?, List<GlobalAlertItem>) -> Unit = { source, wData, aInfo, gAlerts ->
                    AlertDetailDataHolder.set(
                        weather = wData,
                        city = uiState.selectedCity,
                        language = uiState.language,
                        weatherAdaptiveThemeEnabled = uiState.weatherAdaptiveThemeEnabled,
                        themePreset = uiState.themePreset,
                        themeMode = uiState.themeMode,
                        alertInfo = aInfo,
                        jmaWarnings = wData?.jmaWarnings.orEmpty(),
                        jmaOverviewText = wData?.jmaOverviewText,
                        globalAlerts = gAlerts,
                        targetSource = source
                    )
                    val intent = Intent(context, AlertDetailActivity::class.java).apply {
                        if (source != null) {
                            putExtra(AlertDetailActivity.EXTRA_SOURCE, source.name)
                        }
                        putExtra(WeatherRepository.EXTRA_CITY_JSON, WeatherRepository.encodeCityJson(uiState.selectedCity))
                    }
                    context.startActivity(intent)
                    onNavigateToAlertDetail(source)
                }

                if (isTabletLayout) {
                    var shouldFocusTabletSearch by remember { mutableStateOf(false) }

                    if (isTabletPanelExpanded) {
                        BackHandler {
                            isTabletPanelExpanded = false
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Collapsed Side Rail (Always 76dp steady)
                            TabletNavigationRail(
                                onExpandClick = {
                                    shouldFocusTabletSearch = false
                                    isTabletPanelExpanded = true
                                },
                                onAddLocationClick = {
                                    shouldFocusTabletSearch = true
                                    isTabletPanelExpanded = true
                                },
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
                                val pullThresholdCrossed by remember {
                                    derivedStateOf { pullToRefreshState.distanceFraction >= 1f }
                                }
                                var wasPullThresholdCrossed by remember { mutableStateOf(false) }
                                LaunchedEffect(pullThresholdCrossed) {
                                    if (pullThresholdCrossed && !wasPullThresholdCrossed && !uiState.isRefreshing) {
                                        expressHaptics.play(ExpressiveHapticPattern.PULL_REFRESH_TRIGGER)
                                    }
                                    wasPullThresholdCrossed = pullThresholdCrossed
                                }

                                PullToRefreshBox(
                                    isRefreshing = uiState.isRefreshing,
                                    onRefresh = {
                                        expressHaptics.play(ExpressiveHapticPattern.PULL_REFRESH_TRIGGER)
                                        viewModel.refreshWeather()
                                    },
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
                                    CompositionLocalProvider(
                                        LocalSnowScrollOffsetProvider provides remember(tabletScrollState) {
                                            { tabletScrollState.value.toFloat() }
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
                                            val isTabletCityDefault = weather.city.id == uiState.defaultCity.id ||
                                                    (weather.city.latitude == uiState.defaultCity.latitude && weather.city.longitude == uiState.defaultCity.longitude) ||
                                                    weather.city.id == "current"
                                            val isTabletCitySaved = uiState.savedCities.any {
                                                it.id == weather.city.id || (it.latitude == weather.city.latitude && it.longitude == weather.city.longitude)
                                            }
                                            TabletWeatherHeader(
                                                weather = weather,
                                                temperatureUnit = uiState.temperatureUnit,
                                                atmosphere = atmosphere,
                                                isSaved = isTabletCitySaved,
                                                isDefault = isTabletCityDefault,
                                                onToggleSave = {
                                                    if (isTabletCitySaved) {
                                                        viewModel.removeSavedCity(weather.city)
                                                    } else {
                                                        viewModel.saveCity(weather.city)
                                                    }
                                                }
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
                                            if (savedMaxAlert != null && savedMaxAlert.alertLevel.level >= 2) {
                                                SavedCitiesAlertBanner(
                                                    savedMaxAlert = savedMaxAlert,
                                                    selectedCity = uiState.selectedCity,
                                                    savedCities = uiState.savedCities,
                                                    language = uiState.language,
                                                    onSelectCity = { viewModel.selectCity(it) }
                                                )
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
                                                onAlertClick = { source ->
                                                    navigateToAlertDetailPage(source, weather, currentAlertInfo, activeGlobalAlertsTablet)
                                                },
                                                onBannerClick = {
                                                    navigateToAlertDetailPage(null, weather, currentAlertInfo, activeGlobalAlertsTablet)
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
                                                    isTestOverride = uiState.precipitationNowcastOverride != null,
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
                                                windSpeedUnit = uiState.windSpeedUnit,
                                                temperatureUnit = uiState.temperatureUnit
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

                                            // Pre-release Bug Report Card (below all tiles)
                                            if (BuildConfig.ENABLE_PRE_RELEASE_BUG_REPORT) {
                                                BugReportCardSection(
                                                    snapshotProvider = {
                                                        BugReportSnapshot.fromCurrentState(
                                                            weather = weather,
                                                            language = uiState.language,
                                                            themeMode = uiState.themeMode,
                                                            themePreset = uiState.themePreset,
                                                            temperatureUnit = uiState.temperatureUnit,
                                                            windSpeedUnit = uiState.windSpeedUnit,
                                                            radarSourceMode = RadarRepository.getInstance(context).getSourceMode(),
                                                            isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                            isFromCache = uiState.isFromCache,
                                                            isStale = uiState.isStale
                                                        )
                                                    },
                                                    condition = condition,
                                                    isDark = isDark,
                                                    isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                                    hazeState = backgroundHazeState
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
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

                    // 2. タブレットドロワー外タップ検知レイヤー（半透明黒ボックスを除去し、クリアな背景を保ったままタップで閉じる）
                    if (isTabletPanelExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(99f)
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
                            recentSearches = uiState.recentSearches,
                            searchResults = uiState.searchResults,
                            isSearching = uiState.isSearching,
                            onCitySelected = { city, fromSearch ->
                                onSelectCityAction(city, fromSearch)
                                isTabletPanelExpanded = false
                            },
                            onSaveCity = { viewModel.saveCity(it) },
                            onRemoveSavedCity = { viewModel.removeSavedCity(it) },
                            onSearchQueryChange = { viewModel.searchCities(it) },
                            onSettingsClick = {
                                isTabletPanelExpanded = false
                                launchSettings()
                            },
                            initialFocusSearch = shouldFocusTabletSearch,
                            onInitialFocusHandled = { shouldFocusTabletSearch = false }
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
                    val phonePullThresholdCrossed by remember {
                        derivedStateOf { pullToRefreshState.distanceFraction >= 1f }
                    }
                    var wasPhonePullThresholdCrossed by remember { mutableStateOf(false) }
                    LaunchedEffect(phonePullThresholdCrossed) {
                        if (phonePullThresholdCrossed && !wasPhonePullThresholdCrossed && !uiState.isRefreshing) {
                            expressHaptics.play(ExpressiveHapticPattern.PULL_REFRESH_TRIGGER)
                        }
                        wasPhonePullThresholdCrossed = phonePullThresholdCrossed
                    }

                    // 巨大な気温と天気が画面上部コンパクトヘッダーへとシームレスに変形するスクロール距離（240dp）
                    val collapseDistancePx = with(density) { 240.dp.toPx() }
                    val scrollProgress by remember {
                        derivedStateOf {
                            (scrollState.value.toFloat() / collapseDistancePx).coerceIn(0f, 1f)
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = {
                            expressHaptics.play(ExpressiveHapticPattern.PULL_REFRESH_TRIGGER)
                            viewModel.refreshWeather()
                        },
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
                        CompositionLocalProvider(
                            LocalSnowScrollOffsetProvider provides remember(scrollState) {
                                { scrollState.value.toFloat() }
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
                            if (savedMaxAlert != null && savedMaxAlert.alertLevel.level >= 2) {
                                SavedCitiesAlertBanner(
                                    savedMaxAlert = savedMaxAlert,
                                    selectedCity = uiState.selectedCity,
                                    savedCities = uiState.savedCities,
                                    language = uiState.language,
                                    onSelectCity = { viewModel.selectCity(it) }
                                )
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
                                onAlertClick = { source ->
                                    navigateToAlertDetailPage(source, weather, currentAlertInfo, activeGlobalAlertsMobile)
                                },
                                onBannerClick = {
                                    navigateToAlertDetailPage(null, weather, currentAlertInfo, activeGlobalAlertsMobile)
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
                                    isTestOverride = uiState.precipitationNowcastOverride != null,
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
                                windSpeedUnit = uiState.windSpeedUnit,
                                temperatureUnit = uiState.temperatureUnit
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

                            // Panel 6: Pre-release Bug Report Card (below all tiles)
                            if (BuildConfig.ENABLE_PRE_RELEASE_BUG_REPORT) {
                                BugReportCardSection(
                                    snapshotProvider = {
                                        BugReportSnapshot.fromCurrentState(
                                            weather = weather,
                                            language = uiState.language,
                                            themeMode = uiState.themeMode,
                                            themePreset = uiState.themePreset,
                                            temperatureUnit = uiState.temperatureUnit,
                                            windSpeedUnit = uiState.windSpeedUnit,
                                            radarSourceMode = RadarRepository.getInstance(context).getSourceMode(),
                                            isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                            isFromCache = uiState.isFromCache,
                                            isStale = uiState.isStale
                                        )
                                    },
                                    condition = condition,
                                    isDark = isDark,
                                    isFrostedGlassEnabled = uiState.isFrostedGlassEnabled,
                                    hazeState = backgroundHazeState
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            }
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
                    // Error State with Retry button (light/dark 両対応のテーマコントラストカード)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                            tonalElevation = 4.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Thunderstorm,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = uiState.errorMessage ?: strings.errorOccurred,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                androidx.compose.material3.Button(
                                    onClick = { viewModel.refreshWeather() },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    )
                                ) {
                                    Text(strings.retry, fontWeight = FontWeight.Bold)
                                }
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

                // 降雪時のみカード前面を極薄く横切る近景粉雪（タッチ透過・立体奥行き用）
                if (condition == WeatherCondition.SNOW) {
                    SnowForegroundOverlay(
                        isDark = atmosphere.isDark,
                        tiltState = tiltState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                    // Floating Bottom Toolbar with Morphing Container (エラー時でも都市切替・設定へ遷移可能に常時表示)
                    run {
                        // 登録地名一覧ポップアップ（SAVED_CITIES）表示中、それ以外の場所をタップしたら閉じるための全画面透明スクリム
                        if (toolbarMode == ToolbarMode.SAVED_CITIES || toolbarMode == ToolbarMode.SEARCH) {
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
                            }
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
                    io.github.nemynew.expressiveweather.ui.components.SettingsBottomSheet(
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
                            openSettingsSection(section)
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

@Composable
private fun SavedCitiesAlertBanner(
    savedMaxAlert: MunicipalityAlertInfo,
    selectedCity: io.github.nemynew.expressiveweather.data.City,
    savedCities: List<io.github.nemynew.expressiveweather.data.City>,
    language: AppLanguage,
    onSelectCity: (io.github.nemynew.expressiveweather.data.City) -> Unit
) {
    val context = LocalContext.current
    val targetCity = remember(savedMaxAlert, savedCities) {
        io.github.nemynew.expressiveweather.data.JmaMunicipalityResolver.init(context)
        savedCities.firstOrNull { city ->
            val resolved = io.github.nemynew.expressiveweather.data.JmaMunicipalityResolver.resolve(
                cityName = city.name,
                adminArea = city.country
            )
            resolved?.code == savedMaxAlert.municipalityCode
        } ?: savedCities.firstOrNull {
            it.name.contains(savedMaxAlert.municipalityName) ||
                savedMaxAlert.municipalityName.contains(it.name)
        }
    }
    val isAlreadySelected = if (targetCity != null) {
        selectedCity.id == targetCity.id ||
            (kotlin.math.abs(selectedCity.latitude - targetCity.latitude) < 0.01 &&
                kotlin.math.abs(selectedCity.longitude - targetCity.longitude) < 0.01)
    } else {
        selectedCity.name.contains(savedMaxAlert.municipalityName)
    }
    if (isAlreadySelected) return

    val displayPlace = targetCity?.localizedName(language) ?: savedMaxAlert.municipalityName
    val bannerText = when (language) {
        AppLanguage.JAPANESE -> "⚠️ 登録地点［$displayPlace］に【レベル${savedMaxAlert.alertLevel.level} ${savedMaxAlert.alertLevel.title}】が発表中 (タップして表示)"
        AppLanguage.KOREAN -> "⚠️ 저장된 위치 [$displayPlace]에 【${savedMaxAlert.alertLevel.level}단계 ${savedMaxAlert.alertLevel.getTitle(language)}】 발령 중 (탭하여 보기)"
        AppLanguage.ENGLISH -> "⚠️ Saved Location [$displayPlace]: [Level ${savedMaxAlert.alertLevel.level}] active (Tap to view)"
        AppLanguage.SIMPLIFIED_CHINESE -> "⚠️ 已保存地点［$displayPlace］正在发布【等级${savedMaxAlert.alertLevel.level} ${savedMaxAlert.alertLevel.title}】(点击查看)"
        AppLanguage.TRADITIONAL_CHINESE -> "⚠️ 已儲存地點［$displayPlace］正在發布【等級${savedMaxAlert.alertLevel.level} ${savedMaxAlert.alertLevel.title}】(點擊查看)"
        AppLanguage.SPANISH -> "⚠️ Ubicación guardada [$displayPlace]: [Nivel ${savedMaxAlert.alertLevel.level}] activo (Toca para ver)"
        AppLanguage.FRENCH -> "⚠️ Lieu enregistré [$displayPlace] : [Niveau ${savedMaxAlert.alertLevel.level}] actif (Appuyer pour voir)"
        AppLanguage.PORTUGUESE -> "⚠️ Local salvo [$displayPlace]: [Nível ${savedMaxAlert.alertLevel.level}] ativo (Toque para ver)"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = targetCity != null) {
                if (targetCity != null) {
                    onSelectCity(targetCity)
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
                text = bannerText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(savedMaxAlert.alertLevel.textColorHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
