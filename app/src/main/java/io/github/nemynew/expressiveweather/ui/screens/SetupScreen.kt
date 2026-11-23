package io.github.nemynew.expressiveweather.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.CurrentLocationProvider
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.GoogleSansFlexWelcome
import io.github.nemynew.expressiveweather.theme.LocalDarkTheme
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.getAtmosphereTheme
import io.github.nemynew.expressiveweather.ui.components.MorphingSetupVisual
import io.github.nemynew.expressiveweather.ui.components.WeatherAtmosphereCanvas
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import io.github.nemynew.expressiveweather.ui.util.rememberDeviceTiltState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 初期セットアップ時の位置情報・権限フィードバック種別
 */
internal enum class SetupLocationFeedback {
    NONE,
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    LOCATION_UNAVAILABLE,
    LOCATION_ERROR
}

internal fun shouldResetThemeModeOnSetupComplete(isRerun: Boolean): Boolean = !isRerun

internal fun resolveLocationPermissionDeniedFeedback(
    shouldShowRationale: Boolean,
    hasRequestedBefore: Boolean
): SetupLocationFeedback {
    return if (!shouldShowRationale && hasRequestedBefore) {
        SetupLocationFeedback.PERMISSION_PERMANENTLY_DENIED
    } else {
        SetupLocationFeedback.PERMISSION_DENIED
    }
}

/**
 * 初回検索モード（履歴が空のとき）に提示する主要都市のクイック候補
 */
internal fun getPopularSetupCities(language: AppLanguage): List<City> {
    return when (language) {
        AppLanguage.JAPANESE -> listOf(
            City(id = "tokyo", name = "東京", nameEn = "Tokyo", country = "日本", latitude = 35.6762, longitude = 139.6503),
            City(id = "osaka", name = "大阪", nameEn = "Osaka", country = "日本", latitude = 34.6937, longitude = 135.5023),
            City(id = "nagoya", name = "名古屋", nameEn = "Nagoya", country = "日本", latitude = 35.1815, longitude = 136.9066),
            City(id = "sapporo", name = "札幌", nameEn = "Sapporo", country = "日本", latitude = 43.0618, longitude = 141.3545),
            City(id = "fukuoka", name = "福岡", nameEn = "Fukuoka", country = "日本", latitude = 33.5902, longitude = 130.4017)
        )
        AppLanguage.SIMPLIFIED_CHINESE -> listOf(
            City(id = "beijing", name = "北京", nameEn = "Beijing", country = "中国", latitude = 39.9042, longitude = 116.4074),
            City(id = "shanghai", name = "上海", nameEn = "Shanghai", country = "中国", latitude = 31.2304, longitude = 121.4737),
            City(id = "tokyo", name = "东京", nameEn = "Tokyo", country = "日本", latitude = 35.6762, longitude = 139.6503),
            City(id = "taipei", name = "台北", nameEn = "Taipei", country = "台湾", latitude = 25.0330, longitude = 121.5654),
            City(id = "new_york", name = "纽约", nameEn = "New York", country = "美国", latitude = 40.7128, longitude = -74.0060)
        )
        AppLanguage.TRADITIONAL_CHINESE -> listOf(
            City(id = "taipei", name = "臺北", nameEn = "Taipei", country = "台灣", latitude = 25.0330, longitude = 121.5654),
            City(id = "kaohsiung", name = "高雄", nameEn = "Kaohsiung", country = "台灣", latitude = 22.6273, longitude = 120.3014),
            City(id = "tokyo", name = "東京", nameEn = "Tokyo", country = "日本", latitude = 35.6762, longitude = 139.6503),
            City(id = "osaka", name = "大阪", nameEn = "Osaka", country = "日本", latitude = 34.6937, longitude = 135.5023),
            City(id = "new_york", name = "紐約", nameEn = "New York", country = "美國", latitude = 40.7128, longitude = -74.0060)
        )
        AppLanguage.ENGLISH -> listOf(
            City(id = "new_york", name = "New York", nameEn = "New York", country = "United States", latitude = 40.7128, longitude = -74.0060),
            City(id = "london", name = "London", nameEn = "London", country = "United Kingdom", latitude = 51.5072, longitude = -0.1276),
            City(id = "tokyo", name = "東京", nameEn = "Tokyo", country = "Japan", latitude = 35.6762, longitude = 139.6503),
            City(id = "los_angeles", name = "Los Angeles", nameEn = "Los Angeles", country = "United States", latitude = 34.0522, longitude = -118.2437),
            City(id = "sydney", name = "Sydney", nameEn = "Sydney", country = "Australia", latitude = -33.8688, longitude = 151.2093)
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * セットアップ画面（Setup / Onboarding Screen）
 *
 * - 背景: ライトモード時は昼の晴れ空、ダークモード時は夜の快晴（星空グラデーション）に連動し、ソフトブラー／下位API向けフロストを適用
 * - 上部: 右上にクイック言語切替ピル（再表示時は左上に閉じるボタン）、続いて「ようこそ」＋一言サブタイトル
 * - 中心: Material 3 Expressive モーフィングシェイプ＋アプリアイコン（タブレット・横画面にもレスポンシブ適応）
 * - 下部: 現在地を使用（測位失敗・永久権限拒否時のインライン案内付き） / 地名検索して登録（初回人気都市候補＋Photon API 300ms デバウンス検索）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    repository: WeatherRepository,
    locationProvider: CurrentLocationProvider,
    onSetupComplete: (City) -> Unit,
    modifier: Modifier = Modifier,
    isRerun: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onLanguageChanged: ((AppLanguage) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val expressHaptics = LocalExpressiveHaptics.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = LocalDarkTheme.current

    // 端末言語または保存済み言語設定を取得（右上の言語ピルで即時切替可能）
    var currentLanguage by remember {
        mutableStateOf(repository.getLanguage())
    }
    var showLanguageMenu by remember { mutableStateOf(false) }

    // ステータスバー／ナビゲーションバーのアイコン色をライト・ダーク背景に整合
    SideEffect {
        val window = context.findActivity()?.window
        if (window != null && !view.isInEditMode) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    // ライト時は昼の晴れ空、ダーク時はメイン画面と同じ夜の快晴（星空）
    val colorScheme = MaterialTheme.colorScheme
    val atmosphere = remember(colorScheme, isDark) {
        getAtmosphereTheme(WeatherCondition.CLEAR, colorScheme, isDay = !isDark)
    }
    val tiltState = rememberDeviceTiltState()

    // 端末の現在地（取得できている場合は Photon API のバイアスに使用）
    var deviceLocation by remember { mutableStateOf<Location?>(null) }
    LaunchedEffect(Unit) {
        if (locationProvider.hasLocationPermission()) {
            deviceLocation = locationProvider.getCurrentLocation(forceFresh = false)
        }
    }

    // 現在地取得中状態 & 完了処理中ガード & 位置情報フィードバック状態
    var isLocating by remember { mutableStateOf(false) }
    var isCompletingSetup by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var locationFeedback by remember { mutableStateOf(SetupLocationFeedback.NONE) }
    var locationErrorDetail by remember { mutableStateOf<String?>(null) }
    var hasRequestedLocationPermission by rememberSaveable {
        val prefs = context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        mutableStateOf(prefs.getBoolean("setup_location_permission_requested", false))
    }

    // 検索モード（メイン画面のナビゲーションバーのように上にせり上がる）
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<City>>(emptyList()) }
    var recentSearches by remember { mutableStateOf<List<City>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    val saveFailedText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "都市の保存に失敗しました。もう一度お試しください。"
            AppLanguage.SIMPLIFIED_CHINESE -> "保存城市失败，请重试。"
            AppLanguage.TRADITIONAL_CHINESE -> "儲存城市失敗，請重試。"
            AppLanguage.ENGLISH -> "Failed to save city. Please try again."
        }
    }

    val completeSetupWithCity: suspend (City) -> Unit = { city ->
        if (!isCompletingSetup) {
            isCompletingSetup = true
            try {
                repository.saveCity(city)
                repository.addRecentSearch(city)
                repository.setLastSelectedCity(city)
                if (city.id == "current") {
                    repository.setLastResolvedLocationCity(city)
                }
                if (shouldResetThemeModeOnSetupComplete(isRerun)) {
                    repository.setThemeMode(ThemeMode.WEATHER)
                }
                repository.setSetupCompleted(true)
                expressHaptics.play(ExpressiveHapticPattern.CELEBRATE_UNLOCK)
                onSetupComplete(city)
            } catch (e: CancellationException) {
                isCompletingSetup = false
                throw e
            } catch (e: Exception) {
                isCompletingSetup = false
                expressHaptics.play(ExpressiveHapticPattern.ERROR_REJECT)
                Toast.makeText(context, e.localizedMessage ?: saveFailedText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 現在地を取得してセットアップ完了（測位失敗時は無言フォールバックせずインライン案内）
    val startLocationResolution: () -> Unit = {
        if (!isLocating && !isCompletingSetup) {
            isLocating = true
            locationFeedback = SetupLocationFeedback.NONE
            locationErrorDetail = null
            coroutineScope.launch {
                try {
                    val location = locationProvider.getCurrentLocation(forceFresh = true)
                    deviceLocation = location
                    if (location != null) {
                        val city = locationProvider.resolveCity(location)
                        completeSetupWithCity(city)
                    } else {
                        expressHaptics.play(ExpressiveHapticPattern.ERROR_REJECT)
                        locationFeedback = SetupLocationFeedback.LOCATION_UNAVAILABLE
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    expressHaptics.play(ExpressiveHapticPattern.ERROR_REJECT)
                    locationErrorDetail = e.localizedMessage
                    locationFeedback = SetupLocationFeedback.LOCATION_ERROR
                } finally {
                    isLocating = false
                }
            }
        }
    }

    // 検索履歴の初期ロードおよび検索モード突入時のリロード
    LaunchedEffect(isSearchActive) {
        recentSearches = repository.getRecentSearches()
        if (isSearchActive) {
            locationFeedback = SetupLocationFeedback.NONE
            delay(120)
            try {
                searchFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            searchQuery = ""
            searchResults = emptyList()
            focusManager.clearFocus()
        }
    }

    // 検索モード時のバックハンドラー（戻るキーで検索キャンセル）
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        focusManager.clearFocus()
    }

    // Photon API 地名検索（300ミリ秒デバウンス ＋ 空文字時はクリアして履歴表示）
    LaunchedEffect(searchQuery, deviceLocation, currentLanguage) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            searchResults = emptyList()
            isSearching = false
            recentSearches = repository.getRecentSearches()
            return@LaunchedEffect
        }
        isSearching = true
        delay(300) // 300ミリ秒停止したタイミングで非同期検索を実行
        val lat = deviceLocation?.latitude
        val lon = deviceLocation?.longitude
        searchResults = repository.searchCitiesPhoton(
            query = query,
            language = currentLanguage,
            lat = lat,
            lon = lon
        )
        isSearching = false
    }

    // UI文言（4言語対応）
    val welcomeText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "ようこそ"
            AppLanguage.SIMPLIFIED_CHINESE -> "欢迎"
            AppLanguage.TRADITIONAL_CHINESE -> "歡迎"
            AppLanguage.ENGLISH -> "Welcome"
        }
    }

    val subtitleText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "現在地の天気・雨雲レーダー・警報をひと目で"
            AppLanguage.SIMPLIFIED_CHINESE -> "一目了然查看实时天气、降水雷达与预警"
            AppLanguage.TRADITIONAL_CHINESE -> "一目瞭然掌握即時天氣、降雨雷達與警報"
            AppLanguage.ENGLISH -> "Live weather, rain radar, and alerts at a glance"
        }
    }

    val useLocationText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "現在地を使用"
            AppLanguage.SIMPLIFIED_CHINESE -> "使用当前位置"
            AppLanguage.TRADITIONAL_CHINESE -> "使用目前位置"
            AppLanguage.ENGLISH -> "Use Current Location"
        }
    }

    val searchCityText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "地名を検索して登録"
            AppLanguage.SIMPLIFIED_CHINESE -> "搜索并注册城市"
            AppLanguage.TRADITIONAL_CHINESE -> "搜尋並註冊城市"
            AppLanguage.ENGLISH -> "Search & Register City"
        }
    }

    val searchPlaceholderText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "市区町村名・都市名を入力 (例: 渋谷区, 大阪)"
            AppLanguage.SIMPLIFIED_CHINESE -> "输入城市名称 (例: 北京, 上海, 东京)"
            AppLanguage.TRADITIONAL_CHINESE -> "輸入城市名稱 (例: 臺北, 高雄, 東京)"
            AppLanguage.ENGLISH -> "Enter city name (e.g. Tokyo, London)"
        }
    }

    val recentSearchesText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "最近検索した項目"
            AppLanguage.SIMPLIFIED_CHINESE -> "最近搜索"
            AppLanguage.TRADITIONAL_CHINESE -> "最近搜尋"
            AppLanguage.ENGLISH -> "Recent Searches"
        }
    }

    val popularCitiesText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "おすすめの主要都市"
            AppLanguage.SIMPLIFIED_CHINESE -> "热门城市"
            AppLanguage.TRADITIONAL_CHINESE -> "熱門城市"
            AppLanguage.ENGLISH -> "Popular Cities"
        }
    }

    val locatingPrompt = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "現在地を取得しています..."
            AppLanguage.SIMPLIFIED_CHINESE -> "正在获取当前位置..."
            AppLanguage.TRADITIONAL_CHINESE -> "正在獲取目前位置..."
            AppLanguage.ENGLISH -> "Getting current location..."
        }
    }

    val noResultsText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "該当する都市が見つかりません"
            AppLanguage.SIMPLIFIED_CHINESE -> "未找到匹配的城市"
            AppLanguage.TRADITIONAL_CHINESE -> "未找到匹配的城市"
            AppLanguage.ENGLISH -> "No cities found"
        }
    }

    val searchingText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "検索中..."
            AppLanguage.SIMPLIFIED_CHINESE -> "正在搜索..."
            AppLanguage.TRADITIONAL_CHINESE -> "正在搜尋..."
            AppLanguage.ENGLISH -> "Searching..."
        }
    }

    val locationPrivacyHint = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "位置情報は天気・レーダー取得にのみ使用します"
            AppLanguage.SIMPLIFIED_CHINESE -> "位置信息仅用于获取天气与雷达数据"
            AppLanguage.TRADITIONAL_CHINESE -> "位置資訊僅用於取得天氣與雷達資料"
            AppLanguage.ENGLISH -> "Location is only used to fetch weather and radar"
        }
    }

    val privacyPolicyLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "プライバシーポリシーを確認"
            AppLanguage.SIMPLIFIED_CHINESE -> "查看隐私政策"
            AppLanguage.TRADITIONAL_CHINESE -> "查看隱私權政策"
            AppLanguage.ENGLISH -> "Privacy Policy"
        }
    }

    val closeLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "閉じる"
            AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
            AppLanguage.TRADITIONAL_CHINESE -> "關閉"
            AppLanguage.ENGLISH -> "Close"
        }
    }

    val clearSearchLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "入力をクリア"
            AppLanguage.SIMPLIFIED_CHINESE -> "清除输入"
            AppLanguage.TRADITIONAL_CHINESE -> "清除輸入"
            AppLanguage.ENGLISH -> "Clear search"
        }
    }

    val closeSearchLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "検索を閉じる"
            AppLanguage.SIMPLIFIED_CHINESE -> "关闭搜索"
            AppLanguage.TRADITIONAL_CHINESE -> "關閉搜尋"
            AppLanguage.ENGLISH -> "Close search"
        }
    }

    val languageSelectorLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "言語を切り替え"
            AppLanguage.SIMPLIFIED_CHINESE -> "切换语言"
            AppLanguage.TRADITIONAL_CHINESE -> "切換語言"
            AppLanguage.ENGLISH -> "Change language"
        }
    }

    val retryLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "再試行"
            AppLanguage.SIMPLIFIED_CHINESE -> "重试"
            AppLanguage.TRADITIONAL_CHINESE -> "重試"
            AppLanguage.ENGLISH -> "Retry"
        }
    }

    val useSearchInsteadLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "地名検索を使う"
            AppLanguage.SIMPLIFIED_CHINESE -> "使用城市搜索"
            AppLanguage.TRADITIONAL_CHINESE -> "使用城市搜尋"
            AppLanguage.ENGLISH -> "Search City"
        }
    }

    val openSettingsLabel = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "アプリ設定を開く"
            AppLanguage.SIMPLIFIED_CHINESE -> "打开应用设置"
            AppLanguage.TRADITIONAL_CHINESE -> "開啟應用程式設定"
            AppLanguage.ENGLISH -> "Open App Settings"
        }
    }

    val popularCities = remember(currentLanguage) {
        getPopularSetupCities(currentLanguage)
    }

    // 現在地権限ランチャー
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            startLocationResolution()
        } else {
            val activity = context.findActivity()
            val shouldShowRationale = if (activity != null) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                false
            }
            val wasRequestedBefore = hasRequestedLocationPermission
            if (!hasRequestedLocationPermission) {
                hasRequestedLocationPermission = true
                context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("setup_location_permission_requested", true)
                    .apply()
            }
            expressHaptics.play(ExpressiveHapticPattern.ERROR_REJECT)
            locationFeedback = resolveLocationPermissionDeniedFeedback(
                shouldShowRationale = shouldShowRationale,
                hasRequestedBefore = wasRequestedBefore
            )
        }
    }

    // 背景に応じたコントラスト重視の文字・サーフェス色
    val titleColor = if (isDark) Color.White else colorScheme.onBackground
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.80f) else colorScheme.onBackground.copy(alpha = 0.78f)
    val footnoteColor = if (isDark) Color.White.copy(alpha = 0.72f) else colorScheme.onBackground.copy(alpha = 0.70f)
    val pillContainerColor = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val pillContentColor = if (isDark) Color.White else colorScheme.onSurface
    val pillBorderColor = if (isDark) Color.White.copy(alpha = 0.22f) else colorScheme.outlineVariant.copy(alpha = 0.45f)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // ================= 背景: ライトは昼の晴れ空 / ダークは夜の快晴（星空） =================
        val backgroundModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier
                .fillMaxSize()
                .blur(22.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = backgroundModifier) {
            // 1. スカイグラデーション
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(atmosphere.skyGradient)
            )

            // 2. 快晴のアトモスフィアキャンバス（昼: 太陽の光彩 / 夜: 星空・月光）
            WeatherAtmosphereCanvas(
                condition = WeatherCondition.CLEAR,
                atmosphere = atmosphere,
                tiltState = tiltState,
                modifier = Modifier.fillMaxSize()
            )

            // 3. 光拡散フロスト（API 30以下ではブラー代替としてやや強めのソフトオーバーレイを適用）
            val scrimBrush = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040A12).copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.14f else 0.28f),
                        Color(0xFF040A12).copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.22f else 0.36f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.12f else 0.28f),
                        Color.White.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.08f else 0.20f)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimBrush)
            )
        }

        // 検索モード時のバックドロップ（薄いディミングとタップで検索キャンセル）
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isDark) 0.42f else 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isSearchActive = false
                        focusManager.clearFocus()
                    }
            )
        }

        // ================= メインコンテンツレイヤー（タブレット・折りたたみ・横画面対応） =================
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompactHeight = maxHeight < 600.dp
            val isMediumHeight = maxHeight in 600.dp..739.dp

            val targetVisualSize = when {
                isCompactHeight -> if (isSearchActive) 0.dp else 168.dp
                isMediumHeight -> if (isSearchActive) 136.dp else 224.dp
                else -> if (isSearchActive) 186.dp else 276.dp
            }
            val targetIconSize = when {
                isCompactHeight -> if (isSearchActive) 0.dp else 76.dp
                isMediumHeight -> if (isSearchActive) 62.dp else 96.dp
                else -> if (isSearchActive) 78.dp else 116.dp
            }
            val visualSize by animateDpAsState(
                targetValue = targetVisualSize,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "VisualSize"
            )
            val iconSize by animateDpAsState(
                targetValue = targetIconSize,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "IconSize"
            )
            val searchPopupMaxHeight = (maxHeight * 0.42f).coerceIn(160.dp, 280.dp)

            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ================= 0. 上部ユーティリティバー（再表示時クローズ ＋ 言語切替ピル） =================
                AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn(tween(200)) + expandVertically(),
                    exit = fadeOut(tween(150)) + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRerun && onDismiss != null) {
                            Surface(
                                shape = CircleShape,
                                color = pillContainerColor,
                                border = BorderStroke(1.dp, pillBorderColor)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = closeLabel,
                                        tint = pillContentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }

                        Box {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = pillContainerColor,
                                border = BorderStroke(1.dp, pillBorderColor),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = languageSelectorLabel
                                    ) {
                                        showLanguageMenu = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Language,
                                        contentDescription = languageSelectorLabel,
                                        tint = pillContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = currentLanguage.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pillContentColor
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                AppLanguage.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = lang.displayName,
                                                fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        trailingIcon = if (lang == currentLanguage) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            showLanguageMenu = false
                                            if (lang != currentLanguage) {
                                                currentLanguage = lang
                                                repository.setLanguage(lang)
                                                onLanguageChanged?.invoke(lang)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ================= 1. 上部: 「ようこそ」＋ サブタイトル =================
                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 20.dp))

                AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn(tween(200)) + expandVertically(),
                    exit = fadeOut(tween(150)) + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isCompactHeight) 4.dp else 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = welcomeText,
                            fontFamily = GoogleSansFlexWelcome,
                            fontSize = if (isCompactHeight) 36.sp else 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = subtitleColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ================= 2. 中心: MaterialShape モーフィング ＋ アプリアイコン =================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (visualSize > 0.dp && iconSize > 0.dp) {
                        MorphingSetupVisual(
                            visualSize = visualSize,
                            iconSize = iconSize
                        )
                    }
                }

                // ================= 3. 下部: アクションエリア =================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 測位中プログレス表示（通常時のみ）
                    AnimatedVisibility(
                        visible = isLocating && !isSearchActive,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = if (isDark) Color.White else colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = locatingPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else colorScheme.primary
                            )
                        }
                    }

                    // 位置情報エラー／権限拒否時のインライン案内カード（無言フォールバックや行き詰まりを防止）
                    AnimatedVisibility(
                        visible = locationFeedback != SetupLocationFeedback.NONE && !isSearchActive && !isLocating,
                        enter = fadeIn(tween(200)) + expandVertically(),
                        exit = fadeOut(tween(150)) + shrinkVertically()
                    ) {
                        val feedbackMessage = when (locationFeedback) {
                            SetupLocationFeedback.PERMISSION_DENIED -> when (currentLanguage) {
                                AppLanguage.JAPANESE -> "位置情報の許可がありません。地名検索から都市を登録できます。"
                                AppLanguage.SIMPLIFIED_CHINESE -> "未授予位置权限，您可以通过搜索城市完成设置。"
                                AppLanguage.TRADITIONAL_CHINESE -> "未授予位置權限，您可以透過搜尋城市完成設定。"
                                AppLanguage.ENGLISH -> "Location permission was denied. You can search for a city instead."
                            }
                            SetupLocationFeedback.PERMISSION_PERMANENTLY_DENIED -> when (currentLanguage) {
                                AppLanguage.JAPANESE -> "位置情報が端末設定でオフになっています。アプリ設定から許可するか、地名検索をご利用ください。"
                                AppLanguage.SIMPLIFIED_CHINESE -> "位置权限已在系统设置中关闭。请在应用设置中开启，或使用城市搜索。"
                                AppLanguage.TRADITIONAL_CHINESE -> "位置權限已在系統設定中關閉。請在應用程式設定中開啟，或使用城市搜尋。"
                                AppLanguage.ENGLISH -> "Location access is turned off in system settings. Enable it in App Settings or search for a city."
                            }
                            SetupLocationFeedback.LOCATION_UNAVAILABLE -> when (currentLanguage) {
                                AppLanguage.JAPANESE -> "現在地を取得できませんでした。位置情報サービスを確認して再試行するか、地名検索をご利用ください。"
                                AppLanguage.SIMPLIFIED_CHINESE -> "未能获取当前位置。请检查定位服务后重试，或使用城市搜索。"
                                AppLanguage.TRADITIONAL_CHINESE -> "未能取得目前位置。請檢查定位服務後重試，或使用城市搜尋。"
                                AppLanguage.ENGLISH -> "Couldn't get your current location. Check location services and retry, or search for a city."
                            }
                            SetupLocationFeedback.LOCATION_ERROR -> locationErrorDetail?.takeIf { it.isNotBlank() }
                                ?: when (currentLanguage) {
                                    AppLanguage.JAPANESE -> "現在地の取得中にエラーが発生しました。再試行するか地名検索をご利用ください。"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "获取位置时出错，请重试或使用城市搜索。"
                                    AppLanguage.TRADITIONAL_CHINESE -> "取得位置時發生錯誤，請重試或使用城市搜尋。"
                                    AppLanguage.ENGLISH -> "An error occurred while getting your location. Please retry or search for a city."
                                }
                            SetupLocationFeedback.NONE -> ""
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colorScheme.errorContainer.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = colorScheme.onErrorContainer,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(20.dp)
                                    )
                                    Text(
                                        text = feedbackMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (locationFeedback == SetupLocationFeedback.PERMISSION_PERMANENTLY_DENIED) {
                                        TextButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.fromParts("package", context.packageName, null)
                                                    )
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, openSettingsLabel, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Settings,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = openSettingsLabel, fontWeight = FontWeight.SemiBold)
                                        }
                                    } else if (locationFeedback == SetupLocationFeedback.LOCATION_UNAVAILABLE ||
                                        locationFeedback == SetupLocationFeedback.LOCATION_ERROR
                                    ) {
                                        TextButton(
                                            onClick = { startLocationResolution() },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Text(text = retryLabel, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            locationFeedback = SetupLocationFeedback.NONE
                                            isSearchActive = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Text(text = useSearchInsteadLabel, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // プライマリボタン: 「現在地を使用」（検索モード時は隠れる）
                    AnimatedVisibility(
                        visible = !isSearchActive,
                        enter = fadeIn(tween(200)) + expandVertically(),
                        exit = fadeOut(tween(150)) + shrinkVertically()
                    ) {
                        Button(
                            onClick = {
                                if (isLocating || isCompletingSetup) return@Button
                                if (locationProvider.hasLocationPermission()) {
                                    startLocationResolution()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isLocating && !isCompletingSetup,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = useLocationText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // ================= 検索エリア: 上にせり上がる入力欄 ＋ 候補リスト / 履歴 / 人気都市 =================
                    val showPopup = isSearchActive && (
                        searchQuery.isNotBlank() ||
                            isSearching ||
                            searchResults.isNotEmpty() ||
                            recentSearches.isNotEmpty() ||
                            popularCities.isNotEmpty()
                        )
                    AnimatedVisibility(
                        visible = showPopup,
                        enter = fadeIn(tween(200)) + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut(tween(150)) + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = colorScheme.surfaceContainerHigh,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = searchPopupMaxHeight)
                        ) {
                            if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp,
                                            color = colorScheme.primary
                                        )
                                        Text(
                                            text = searchingText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            } else if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = noResultsText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // 検索結果、直近の検索履歴、または初回向けおすすめ主要都市を表示
                                val isShowingHistory = searchQuery.isBlank() && recentSearches.isNotEmpty()
                                val isShowingPopular = searchQuery.isBlank() && recentSearches.isEmpty()
                                val displayList = when {
                                    isShowingHistory -> recentSearches
                                    isShowingPopular -> popularCities
                                    else -> searchResults
                                }

                                LazyColumn(
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isShowingHistory || isShowingPopular) {
                                        item {
                                            Text(
                                                text = if (isShowingHistory) recentSearchesText else popularCitiesText,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    items(displayList, key = { "${it.id}_${it.latitude}_${it.longitude}" }) { city ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isCompletingSetup, role = Role.Button) {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    coroutineScope.launch {
                                                        completeSetupWithCity(city)
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isShowingHistory -> Icons.Rounded.History
                                                        isShowingPopular -> Icons.Rounded.StarOutline
                                                        else -> Icons.Rounded.LocationCity
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isShowingHistory) colorScheme.onSurfaceVariant else colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = city.localizedName(currentLanguage),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colorScheme.onSurface
                                                    )
                                                    if (city.country.isNotBlank()) {
                                                        Text(
                                                            text = city.country,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 検索バー本体（通常時は「地名を検索して登録」ボタン、タップでせり上がりキーボード入力欄に変形）
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = colorScheme.surfaceContainerHigh,
                        shadowElevation = if (isSearchActive) 4.dp else 0.dp,
                        border = if (isSearchActive) {
                            BorderStroke(1.8.dp, colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .then(
                                if (!isSearchActive) {
                                    Modifier.clickable(
                                        role = Role.Button,
                                        onClickLabel = searchCityText
                                    ) {
                                        isSearchActive = true
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (isSearchActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 18.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = searchPlaceholderText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(colorScheme.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                keyboardController?.hide()
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                    )
                                }

                                // クリア / 閉じるボタン（Material 推奨 48dp タップ領域）
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            searchQuery = ""
                                        } else {
                                            isSearchActive = false
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = if (searchQuery.isNotEmpty()) clearSearchLabel else closeSearchLabel,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            // 通常時: 文字とアイコンを中央揃え
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = searchCityText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // 位置情報利用の補足 ＋ プライバシーポリシー確認ボタン（48dp 最小タップ領域）
                    if (!isSearchActive) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = locationPrivacyHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = footnoteColor,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = { showPrivacyPolicyDialog = true },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = privacyPolicyLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = titleColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            language = currentLanguage,
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }
}
