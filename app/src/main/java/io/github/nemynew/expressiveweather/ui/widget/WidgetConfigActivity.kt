package io.github.nemynew.expressiveweather.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.local.WeatherDatabase
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.WeatherIconHelper
import io.github.nemynew.expressiveweather.util.LocaleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * ホーム画面ウィジェットの配置時／再設定用 Activity (Material 3 Expressive)
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        setResult(RESULT_CANCELED)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val db = WeatherDatabase.getInstance(applicationContext)
        val repo = WeatherRepository(
            weatherDao = db.weatherDao(),
            savedCityDao = db.savedCityDao(),
            context = applicationContext
        )

        val providerInfo = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetInfo(appWidgetId)
        val isShapeWidget = (providerInfo?.provider?.className == WeatherWidgetShapeProvider::class.java.name) ||
            (io.github.nemynew.expressiveweather.BuildConfig.DEBUG && intent?.getBooleanExtra("preview_shape", false) == true)
        val isRadarWidget = (providerInfo?.provider?.className == WeatherWidgetRadarProvider::class.java.name) ||
            (io.github.nemynew.expressiveweather.BuildConfig.DEBUG && intent?.getBooleanExtra("preview_radar", false) == true)
        val defaultStyle = if (isShapeWidget || isRadarWidget) WidgetStyle.MATERIAL_YOU else WidgetStyle.WEATHER_EXPRESSIVE
        val initialConfig = WidgetPreferences.getConfig(applicationContext, appWidgetId, defaultStyle)
        val language = repo.getLanguage()
        val tempUnit = repo.getTemperatureUnit()
        val windUnit = repo.getWindSpeedUnit()
        val fallbackCurrent = repo.resolveFallbackCurrentCity()

        setContent {
            ExpressiveWeatherTheme(
                language = language,
                themePreset = ThemePreset.SYSTEM,
                weatherAdaptiveColor = false
            ) {
                WidgetConfigScreen(
                    initialConfig = initialConfig,
                    isShapeWidget = isShapeWidget,
                    isRadarWidget = isRadarWidget,
                    language = language,
                    tempUnit = tempUnit,
                    windUnit = windUnit,
                    fallbackCurrentCity = fallbackCurrent,
                    repository = repo,
                    onCancel = { finish() },
                    onSave = { finalConfig ->
                        WidgetPreferences.saveConfig(applicationContext, finalConfig)
                        WidgetRefreshWorker.ensurePeriodicSchedule(applicationContext)
                        WidgetRefreshWorker.enqueueRefresh(applicationContext, appWidgetId)
                        WeatherWidgetUpdater.updateWidgetAsync(
                            context = applicationContext,
                            appWidgetId = appWidgetId,
                            fetchIfMissing = true
                        )
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WidgetConfigScreen(
    initialConfig: WidgetConfig,
    isShapeWidget: Boolean,
    isRadarWidget: Boolean,
    language: AppLanguage,
    tempUnit: TemperatureUnit,
    windUnit: WindSpeedUnit,
    fallbackCurrentCity: City,
    repository: WeatherRepository,
    onCancel: () -> Unit,
    onSave: (WidgetConfig) -> Unit
) {
    val scope = rememberCoroutineScope()
    var useCurrentLocation by remember { mutableStateOf(initialConfig.useCurrentLocation) }
    var selectedCity by remember { mutableStateOf(initialConfig.pinnedCity) }
    var selectedStyle by remember { mutableStateOf(initialConfig.style) }
    var glassOpacity by remember { mutableFloatStateOf(initialConfig.glassOpacity.coerceIn(0.15f, 0.95f)) }
    var selectedShape by remember { mutableStateOf(initialConfig.shapeType) }

    var savedCities by remember { mutableStateOf<List<City>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<City>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var previewWeather by remember { mutableStateOf<WeatherData?>(null) }

    LaunchedEffect(Unit) {
        savedCities = repository.getAllSavedCities()
    }

    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(180)
        searchResults = repository.searchCities(trimmed, language)
        isSearching = false
    }

    val activeTargetCity = if (useCurrentLocation || selectedCity == null) fallbackCurrentCity else selectedCity!!
    LaunchedEffect(activeTargetCity) {
        previewWeather = repository.getCachedWeatherOrNull(activeTargetCity)
    }

    val draftConfig = WidgetConfig(
        appWidgetId = initialConfig.appWidgetId,
        useCurrentLocation = useCurrentLocation,
        pinnedCity = if (useCurrentLocation) null else selectedCity,
        style = selectedStyle,
        showAlertsAndNowcast = false,
        showRefreshButton = false,
        shapeType = selectedShape,
        showCityOnShape = false,
        glassOpacity = glassOpacity
    )

    val titleText = when (language) {
        AppLanguage.JAPANESE -> "ウィジェット設定"
        AppLanguage.ENGLISH -> "Widget Settings"
        AppLanguage.SIMPLIFIED_CHINESE -> "微件设置"
        AppLanguage.TRADITIONAL_CHINESE -> "小工具設定"
    }
    val locationSectionTitle = when (language) {
        AppLanguage.JAPANESE -> "表示する地域"
        AppLanguage.ENGLISH -> "Location"
        AppLanguage.SIMPLIFIED_CHINESE -> "显示地区"
        AppLanguage.TRADITIONAL_CHINESE -> "顯示地區"
    }
    val currentLocLabel = when (language) {
        AppLanguage.JAPANESE -> "現在地 (GPS自動追従)"
        AppLanguage.ENGLISH -> "Current Location (Auto GPS)"
        AppLanguage.SIMPLIFIED_CHINESE -> "当前位置（自动定位）"
        AppLanguage.TRADITIONAL_CHINESE -> "目前位置（自動定位）"
    }
    val styleSectionTitle = when (language) {
        AppLanguage.JAPANESE -> "外観スタイル"
        AppLanguage.ENGLISH -> "Appearance Style"
        AppLanguage.SIMPLIFIED_CHINESE -> "外观样式"
        AppLanguage.TRADITIONAL_CHINESE -> "外觀樣式"
    }
    val optionsSectionTitle = when (language) {
        AppLanguage.JAPANESE -> "表示オプション"
        AppLanguage.ENGLISH -> "Display Options"
        AppLanguage.SIMPLIFIED_CHINESE -> "显示选项"
        AppLanguage.TRADITIONAL_CHINESE -> "顯示選項"
    }
    val alertToggleLabel = when (language) {
        AppLanguage.JAPANESE -> "雨雲接近・キキクル警報を表示"
        AppLanguage.ENGLISH -> "Show Rain Nowcast & Alerts"
        AppLanguage.SIMPLIFIED_CHINESE -> "显示降雨临近与灾害预警"
        AppLanguage.TRADITIONAL_CHINESE -> "顯示降雨即時預報與災害警報"
    }
    val refreshToggleLabel = when (language) {
        AppLanguage.JAPANESE -> "クイック更新ボタンを表示"
        AppLanguage.ENGLISH -> "Show Quick Refresh Button"
        AppLanguage.SIMPLIFIED_CHINESE -> "显示快速刷新按钮"
        AppLanguage.TRADITIONAL_CHINESE -> "顯示快速重新整理按鈕"
    }
    val searchPlaceholder = when (language) {
        AppLanguage.JAPANESE -> "都市名を検索して固定…"
        AppLanguage.ENGLISH -> "Search a city to pin…"
        AppLanguage.SIMPLIFIED_CHINESE -> "搜索城市并固定…"
        AppLanguage.TRADITIONAL_CHINESE -> "搜尋城市並固定…"
    }
    val applyText = when (language) {
        AppLanguage.JAPANESE -> "ウィジェットを適用"
        AppLanguage.ENGLISH -> "Apply Widget"
        AppLanguage.SIMPLIFIED_CHINESE -> "应用微件"
        AppLanguage.TRADITIONAL_CHINESE -> "套用小工具"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = titleText,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onSave(draftConfig) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = applyText,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Interactive Preview Card
            item {
                when {
                    isShapeWidget -> {
                        WidgetShapeLivePreviewCard(
                            config = draftConfig,
                            weather = previewWeather,
                            fallbackCity = activeTargetCity,
                            language = language,
                            tempUnit = tempUnit,
                            windUnit = windUnit
                        )
                    }
                    isRadarWidget -> {
                        WidgetRadarLivePreviewCard(
                            config = draftConfig,
                            weather = previewWeather,
                            fallbackCity = activeTargetCity,
                            language = language,
                            tempUnit = tempUnit,
                            windUnit = windUnit
                        )
                    }
                    else -> {
                        WidgetLivePreviewCard(
                            config = draftConfig,
                            weather = previewWeather,
                            fallbackCity = activeTargetCity,
                            language = language,
                            tempUnit = tempUnit,
                            windUnit = windUnit
                        )
                    }
                }
            }

            // 1.5 Material Shape Picker (Shape ウィジェット用)
            if (isShapeWidget) {
                item {
                    val shapeSectionTitle = when (language) {
                        AppLanguage.JAPANESE -> "Material Shape の形"
                        AppLanguage.ENGLISH -> "Material Shape"
                        AppLanguage.SIMPLIFIED_CHINESE -> "Material Shape 形状"
                        AppLanguage.TRADITIONAL_CHINESE -> "Material Shape 形狀"
                    }
                    SectionHeader(icon = Icons.Rounded.AutoAwesome, title = shapeSectionTitle)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MaterialWidgetShape.entries.forEach { shape ->
                            FilterChip(
                                selected = selectedShape == shape,
                                onClick = { selectedShape = shape },
                                label = { Text(shape.getDisplayName(language)) }
                            )
                        }
                    }
                }
            }

            // 2. Style Selector
            item {
                SectionHeader(icon = Icons.Rounded.Palette, title = styleSectionTitle)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetStyle.entries.forEach { style ->
                        val isSelected = style == selectedStyle
                        Card(
                            onClick = { selectedStyle = style },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedStyle = style }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = style.getDisplayName(language),
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = style.getDescription(language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = selectedStyle == WidgetStyle.FROSTED_GLASS) {
                    val opacityTitle = when (language) {
                        AppLanguage.JAPANESE -> "半透明の不透明度（透明度調整）"
                        AppLanguage.ENGLISH -> "Translucent Opacity"
                        AppLanguage.SIMPLIFIED_CHINESE -> "半透明不透明度"
                        AppLanguage.TRADITIONAL_CHINESE -> "半透明不透明度"
                    }
                    val opacityHint = when (language) {
                        AppLanguage.JAPANESE -> "Material You カラーの濃さと透明度を調整します（左：透明寄り / 右：濃いめ）"
                        AppLanguage.ENGLISH -> "Adjust Material You tint density & transparency"
                        AppLanguage.SIMPLIFIED_CHINESE -> "调整 Material You 配色浓度与透明度"
                        AppLanguage.TRADITIONAL_CHINESE -> "調整 Material You 配色濃度與透明度"
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = opacityTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(glassOpacity * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = glassOpacity,
                                onValueChange = { glassOpacity = it.coerceIn(0.15f, 0.95f) },
                                valueRange = 0.15f..0.95f
                            )
                            Text(
                                text = opacityHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Location Selector + Search
            item {
                SectionHeader(icon = Icons.Rounded.LocationOn, title = locationSectionTitle)
                Spacer(Modifier.height(8.dp))

                // 現在地オプション
                LocationChoiceRow(
                    title = currentLocLabel,
                    subtitle = fallbackCurrentCity.localizedName(language),
                    selected = useCurrentLocation,
                    isCurrent = true,
                    onClick = {
                        useCurrentLocation = true
                    }
                )

                Spacer(Modifier.height(8.dp))

                // 都市検索バー
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        searchResults.take(5).forEach { city ->
                            val isSelected = !useCurrentLocation && selectedCity?.id == city.id &&
                                selectedCity?.latitude == city.latitude
                            LocationChoiceRow(
                                title = city.localizedName(language),
                                subtitle = city.country,
                                selected = isSelected,
                                isCurrent = false,
                                onClick = {
                                    useCurrentLocation = false
                                    selectedCity = city
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    scope.launch {
                                        previewWeather = repository.getCachedWeatherOrNull(city)
                                    }
                                }
                            )
                        }
                    }
                }

                // 保存済み都市リスト
                val pinnedExtraCity = selectedCity?.takeIf { sel ->
                    !useCurrentLocation && savedCities.none { it.id == sel.id }
                }
                val selectableCities = listOfNotNull(pinnedExtraCity) + savedCities
                if (selectableCities.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectableCities.forEach { city ->
                            val isSelected = !useCurrentLocation && selectedCity?.id == city.id &&
                                selectedCity?.latitude == city.latitude
                            LocationChoiceRow(
                                title = city.localizedName(language),
                                subtitle = city.country,
                                selected = isSelected,
                                isCurrent = false,
                                onClick = {
                                    useCurrentLocation = false
                                    selectedCity = city
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LocationChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrent) Icons.Rounded.MyLocation else Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WidgetShapeLivePreviewCard(
    config: WidgetConfig,
    weather: WeatherData?,
    fallbackCity: City,
    language: AppLanguage,
    tempUnit: TemperatureUnit,
    windUnit: WindSpeedUnit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val myPalette = remember(context, isDark) {
        WidgetCanvasRenderer.resolveMaterialYouPalette(context, isDark)
    }
    val model = remember(config, weather, fallbackCity, language, tempUnit, windUnit) {
        WidgetUiModelBuilder.build(
            config = config,
            weather = weather,
            language = language,
            temperatureUnit = tempUnit,
            windSpeedUnit = windUnit,
            fallbackCity = fallbackCity
        )
    }
    val previewBitmap = remember(config, model) {
        WidgetMaterialShapeRenderer.renderShapeWidgetBitmap(
            context = context,
            config = config,
            model = model,
            sizePx = 384
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (config.style == WidgetStyle.FROSTED_GLASS) {
                    Color(myPalette.wallpaperPrimaryColor).copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .padding(vertical = 18.dp)
                .size(188.dp)
        )
    }
}

private data class PreviewSurfaceColors(
    val myPalette: WidgetMaterialYouPalette,
    val isFrosted: Boolean,
    val isLightSurface: Boolean,
    val fgPrimary: Color,
    val fgSecondary: Color,
    val popColor: Color,
    val trayBg: Color,
    val activeBoxBg: Color,
    val activeFgPrimary: Color,
    val activeFgSecondary: Color
)

private fun resolvePreviewSurfaceColors(
    myPalette: WidgetMaterialYouPalette,
    config: WidgetConfig,
    isDark: Boolean
): PreviewSurfaceColors {
    val glassOpacity = config.glassOpacity.coerceIn(0.15f, 0.95f)
    val isMaterialYou = config.style == WidgetStyle.MATERIAL_YOU
    val isFrosted = config.style == WidgetStyle.FROSTED_GLASS
    val isLightSurface = when (config.style) {
        WidgetStyle.MATERIAL_YOU -> myPalette.isLightSurface
        WidgetStyle.FROSTED_GLASS -> myPalette.isLightSurface && glassOpacity >= 0.45f
        WidgetStyle.WEATHER_EXPRESSIVE -> false
    }
    val fgPrimary = if (isMaterialYou || (isFrosted && isLightSurface)) {
        Color(myPalette.primaryTextColor)
    } else {
        Color.White
    }
    val fgSecondary = if (isMaterialYou || (isFrosted && isLightSurface)) {
        Color(myPalette.secondaryTextColor)
    } else {
        Color.White.copy(alpha = 0.88f)
    }
    val popColor = when {
        isMaterialYou || (isFrosted && isLightSurface) -> Color(myPalette.accentColor)
        isFrosted -> Color(WidgetCanvasRenderer.blendColors(myPalette.accentColor, android.graphics.Color.WHITE, 0.35f))
        else -> Color(0xFF90CAF9)
    }
    val trayBg = when (config.style) {
        WidgetStyle.MATERIAL_YOU -> Color(myPalette.trayColor)
        WidgetStyle.FROSTED_GLASS -> {
            val base = if (isDark) myPalette.trayColor else android.graphics.Color.WHITE
            val alpha = if (isLightSurface) {
                (glassOpacity * 170f).toInt().coerceIn(56, 175)
            } else {
                (glassOpacity * 115f + 34f).toInt().coerceIn(42, 148)
            }
            Color(WidgetCanvasRenderer.withAlpha(base, alpha))
        }
        WidgetStyle.WEATHER_EXPRESSIVE -> Color.White.copy(alpha = 0.16f)
    }
    val activeBoxBg = when (config.style) {
        WidgetStyle.MATERIAL_YOU -> Color(myPalette.primaryContainerColor)
        WidgetStyle.FROSTED_GLASS -> {
            val base = if (isDark) myPalette.primaryContainerColor else android.graphics.Color.WHITE
            val alpha = if (isLightSurface) {
                (glassOpacity * 210f).toInt().coerceIn(85, 220)
            } else {
                (glassOpacity * 145f + 54f).toInt().coerceIn(64, 190)
            }
            Color(WidgetCanvasRenderer.withAlpha(base, alpha))
        }
        WidgetStyle.WEATHER_EXPRESSIVE -> Color.White.copy(alpha = 0.28f)
    }
    val activeFgPrimary = if (isMaterialYou || (isFrosted && isLightSurface)) {
        Color(myPalette.onPrimaryContainerColor)
    } else {
        Color.White
    }
    return PreviewSurfaceColors(
        myPalette = myPalette,
        isFrosted = isFrosted,
        isLightSurface = isLightSurface,
        fgPrimary = fgPrimary,
        fgSecondary = fgSecondary,
        popColor = popColor,
        trayBg = trayBg,
        activeBoxBg = activeBoxBg,
        activeFgPrimary = activeFgPrimary,
        activeFgSecondary = activeFgPrimary.copy(alpha = 0.82f)
    )
}

@Composable
private fun WidgetRadarLivePreviewCard(
    config: WidgetConfig,
    weather: WeatherData?,
    fallbackCity: City,
    language: AppLanguage,
    tempUnit: TemperatureUnit,
    windUnit: WindSpeedUnit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val model = remember(config, weather, fallbackCity, language, tempUnit, windUnit) {
        WidgetUiModelBuilder.build(
            config = config,
            weather = weather,
            language = language,
            temperatureUnit = tempUnit,
            windSpeedUnit = windUnit,
            fallbackCity = fallbackCity
        )
    }
    val myPalette = remember(context, isDark) {
        WidgetCanvasRenderer.resolveMaterialYouPalette(context, isDark)
    }
    val colors = remember(myPalette, config.style, config.glassOpacity, isDark) {
        resolvePreviewSurfaceColors(myPalette, config, isDark)
    }
    val glassOpacity = config.glassOpacity.coerceIn(0.15f, 0.95f)
    val bgBitmap = remember(config.style, glassOpacity, model.condition, model.isDay, isDark, myPalette) {
        WidgetCanvasRenderer.renderBackgroundBitmap(
            context = context,
            sizeTier = WidgetSizeTier.MEDIUM,
            style = config.style,
            condition = model.condition,
            isDay = model.isDay,
            isSystemDark = isDark,
            myPalette = myPalette,
            glassOpacity = glassOpacity
        )
    }

    val effectiveCity = model.targetCity ?: fallbackCity
    var radarBitmap by remember(effectiveCity, isDark, language, myPalette) {
        mutableStateOf(
            WidgetRadarMapRenderer.loadCachedOrFallbackBitmap(
                context = context,
                city = effectiveCity,
                myPalette = myPalette,
                isSystemDark = isDark,
                language = language,
                widthPx = 300,
                heightPx = 240
            )
        )
    }
    var badgeText by remember(effectiveCity, language) {
        mutableStateOf(WidgetRadarMapRenderer.getCachedBadgeText(context, effectiveCity, language))
    }
    LaunchedEffect(effectiveCity, isDark, language, myPalette) {
        val fetched = withContext(Dispatchers.IO) {
            WidgetRadarMapRenderer.fetchAndRenderAndCache(
                context = context,
                city = effectiveCity,
                myPalette = myPalette,
                isSystemDark = isDark,
                language = language,
                forceRefresh = false,
                widthPx = 300,
                heightPx = 240
            )
        }
        radarBitmap = fetched
        badgeText = WidgetRadarMapRenderer.getCachedBadgeText(context, effectiveCity, language)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (colors.isFrosted) {
                    Color(myPalette.wallpaperPrimaryColor).copy(alpha = 0.32f)
                } else {
                    Color.Transparent
                }
            )
    ) {
        Image(
            bitmap = bgBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.32f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Image(
                    bitmap = radarBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(myPalette.primaryContainerColor).copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color(myPalette.onPrimaryContainerColor),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (model.isCurrentLocation) {
                        Icon(
                            Icons.Rounded.MyLocation,
                            contentDescription = null,
                            tint = colors.fgPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = model.cityName,
                        color = colors.fgPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(WeatherIconHelper.getConditionDrawable(model.condition, model.isDay)),
                        contentDescription = null,
                        tint = Color(WidgetCanvasRenderer.getConditionIconTint(model.condition, model.isDay, colors.isLightSurface)),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = model.currentTempText,
                        color = colors.fgPrimary,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Medium,
                        fontSize = 28.sp
                    )
                }
                Text(
                    text = if (model.hasData) "${model.conditionLabel}  ${model.highLowText}" else model.conditionLabel,
                    color = colors.fgSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                val previewHours = model.hourlyItems.take(3)
                if (previewHours.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        previewHours.forEachIndexed { index, hour ->
                            val isNow = index == 0
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isNow) colors.activeBoxBg else colors.trayBg)
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = hour.timeLabel,
                                    color = if (isNow) colors.activeFgSecondary else colors.fgSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                                Icon(
                                    painter = painterResource(WeatherIconHelper.getConditionDrawable(hour.condition, hour.isDay)),
                                    contentDescription = null,
                                    tint = Color(
                                        WidgetCanvasRenderer.getConditionIconTint(
                                            hour.condition,
                                            hour.isDay,
                                            colors.isLightSurface
                                        )
                                    ),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = hour.tempText,
                                    color = if (isNow) colors.activeFgPrimary else colors.fgPrimary,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetLivePreviewCard(
    config: WidgetConfig,
    weather: WeatherData?,
    fallbackCity: City,
    language: AppLanguage,
    tempUnit: TemperatureUnit,
    windUnit: WindSpeedUnit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val model = remember(config, weather, fallbackCity, language, tempUnit, windUnit) {
        WidgetUiModelBuilder.build(
            config = config,
            weather = weather,
            language = language,
            temperatureUnit = tempUnit,
            windSpeedUnit = windUnit,
            fallbackCity = fallbackCity
        )
    }

    val glassOpacity = config.glassOpacity.coerceIn(0.15f, 0.95f)
    val myPalette = remember(context, isDark) {
        WidgetCanvasRenderer.resolveMaterialYouPalette(context, isDark)
    }
    val colors = remember(myPalette, config.style, config.glassOpacity, isDark) {
        resolvePreviewSurfaceColors(myPalette, config, isDark)
    }
    val isFrosted = colors.isFrosted
    val isLightSurface = colors.isLightSurface
    val bgBitmap = remember(config.style, glassOpacity, model.condition, model.isDay, isDark, myPalette) {
        WidgetCanvasRenderer.renderBackgroundBitmap(
            context = context,
            sizeTier = WidgetSizeTier.MEDIUM,
            style = config.style,
            condition = model.condition,
            isDay = model.isDay,
            isSystemDark = isDark,
            myPalette = myPalette,
            glassOpacity = glassOpacity
        )
    }

    val fgPrimary = colors.fgPrimary
    val fgSecondary = colors.fgSecondary
    val popColor = colors.popColor
    val trayBg = colors.trayBg

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isFrosted) {
                    Color(myPalette.wallpaperPrimaryColor).copy(alpha = 0.32f)
                } else {
                    Color.Transparent
                }
            )
    ) {
        Image(
            bitmap = bgBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (model.isCurrentLocation) {
                            Icon(
                                Icons.Rounded.MyLocation,
                                contentDescription = null,
                                tint = fgPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = model.cityName,
                            color = fgPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = if (model.hasData) "${model.conditionLabel}  ${model.highLowText}" else model.conditionLabel,
                        color = fgSecondary,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    painter = painterResource(WeatherIconHelper.getConditionDrawable(model.condition, model.isDay)),
                    contentDescription = null,
                    tint = Color(WidgetCanvasRenderer.getConditionIconTint(model.condition, model.isDay, isLightSurface)),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = model.currentTempText,
                    color = fgPrimary,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium,
                    fontSize = 38.sp
                )
            }

            val previewHours = model.hourlyItems.take(5)
            if (previewHours.isNotEmpty()) {
                val activeBoxBg = colors.activeBoxBg
                val activeFgPrimary = colors.activeFgPrimary
                val activeFgSecondary = colors.activeFgSecondary

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    previewHours.forEachIndexed { index, hour ->
                        val isNow = index == 0
                        val itemBg = if (isNow) activeBoxBg else trayBg
                        val itemPrimary = if (isNow) activeFgPrimary else fgPrimary
                        val itemSecondary = if (isNow) activeFgSecondary else fgSecondary
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(itemBg)
                                .padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hour.timeLabel,
                                color = itemSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(Modifier.height(3.dp))
                            Icon(
                                painter = painterResource(WeatherIconHelper.getConditionDrawable(hour.condition, hour.isDay)),
                                contentDescription = null,
                                tint = Color(WidgetCanvasRenderer.getConditionIconTint(hour.condition, hour.isDay, isLightSurface)),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = hour.precipProbText.ifEmpty { " " },
                                color = popColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = hour.tempText,
                                color = itemPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
