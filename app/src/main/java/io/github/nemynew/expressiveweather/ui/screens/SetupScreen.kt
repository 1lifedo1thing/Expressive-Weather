package io.github.nemynew.expressiveweather.ui.screens

import android.Manifest
import android.content.Context
import android.location.Location
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.CurrentLocationProvider
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.GoogleSansFlexWelcome
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.getAtmosphereTheme
import io.github.nemynew.expressiveweather.ui.components.MorphingSetupVisual
import io.github.nemynew.expressiveweather.ui.components.WeatherAtmosphereCanvas
import io.github.nemynew.expressiveweather.ui.util.rememberDeviceTiltState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * セットアップ画面（Setup / Onboarding Screen）
 *
 * - 背景: メイン画面の晴れの時と同じものに、美しいソフトブラー（ぼかし）を適用
 * - 上部: 「ようこそ」（ステータスバーから少し下げた落ち着きのある優雅な配置。Google Sans Flex・Round高め）
 * - 中心: 不透明な白系MaterialShapeタイル（Pill含む多彩な形状がローディングのように連続変形）＋大きなアプリアイコン
 * - 下部: (現在地を使用 or 地名検索→登録)
 *         検索ボタンをタップするとメイン画面のようにそのまま上にせり上がり、キーボード入力欄に変形。
 * - 地名検索:
 *   1. Photon API (https://photon.komoot.io/api/?q={query}&lang=ja) を使用。
 *      現在地が取得済みの場合は lat, lon を付加して現在地優先バイアスを適用。
 *   2. 入力が 300ミリ秒停止したタイミングで非同期検索を実行（300ms デバウンス）。
 *      空文字の場合は結果リストをクリアし、直近の検索履歴を表示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    repository: WeatherRepository,
    locationProvider: CurrentLocationProvider,
    onSetupComplete: (City) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expressHaptics = LocalExpressiveHaptics.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // 端末言語または保存済み言語設定を取得
    val currentLanguage = remember {
        val prefs = context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString("app_language", null)
        if (savedLangCode != null) {
            AppLanguage.fromCode(savedLangCode)
        } else {
            AppLanguage.fromCode(Locale.getDefault().toLanguageTag())
        }
    }

    // メイン画面の晴れの時と完全に同じ背景アトモスフィア
    val colorScheme = MaterialTheme.colorScheme
    val atmosphere = remember(colorScheme) {
        getAtmosphereTheme(WeatherCondition.CLEAR, colorScheme, isDay = true)
    }
    val tiltState = rememberDeviceTiltState()

    // 端末の現在地（取得できている場合は Photon API のバイアスに使用）
    var deviceLocation by remember { mutableStateOf<Location?>(null) }
    LaunchedEffect(Unit) {
        if (locationProvider.hasLocationPermission()) {
            deviceLocation = locationProvider.getCurrentLocation(forceFresh = false)
        }
    }

    // 現在地取得中状態 & 完了処理中ガード
    var isLocating by remember { mutableStateOf(false) }
    var isCompletingSetup by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

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
                repository.setThemeMode(ThemeMode.WEATHER)
                repository.setSetupCompleted(true)
                expressHaptics.play(ExpressiveHapticPattern.CELEBRATE_UNLOCK)
                onSetupComplete(city)
            } catch (e: CancellationException) {
                isCompletingSetup = false
                throw e
            } catch (e: Exception) {
                isCompletingSetup = false
                throw e
            }
        }
    }

    // 検索モード（メイン画面のナビゲーションバーのように上にせり上がる）
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<City>>(emptyList()) }
    var recentSearches by remember { mutableStateOf<List<City>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // 検索履歴の初期ロードおよび検索モード突入時のリロード
    LaunchedEffect(isSearchActive) {
        recentSearches = repository.getRecentSearches()
        if (isSearchActive) {
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
    LaunchedEffect(searchQuery, deviceLocation) {
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

    // UI文言
    val welcomeText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "ようこそ"
            AppLanguage.SIMPLIFIED_CHINESE -> "欢迎"
            AppLanguage.TRADITIONAL_CHINESE -> "歡迎"
            AppLanguage.ENGLISH -> "Welcome"
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

    val locationPermissionDeniedText = remember(currentLanguage) {
        when (currentLanguage) {
            AppLanguage.JAPANESE -> "位置情報の利用が許可されませんでした。地名検索をご利用ください。"
            AppLanguage.SIMPLIFIED_CHINESE -> "未授予位置信息权限，请使用城市搜索。"
            AppLanguage.TRADITIONAL_CHINESE -> "未授予位置資訊權限，請使用城市搜尋。"
            AppLanguage.ENGLISH -> "Location permission was denied. Please search for a city."
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

    // 現在地権限ランチャー
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (isLocating || isCompletingSetup) return@rememberLauncherForActivityResult
            isLocating = true
            coroutineScope.launch {
                try {
                    val location = locationProvider.getCurrentLocation(forceFresh = true)
                    deviceLocation = location
                    val city = if (location != null) {
                        locationProvider.resolveCity(location)
                    } else {
                        repository.defaultCity
                    }
                    completeSetupWithCity(city)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    expressHaptics.play(ExpressiveHapticPattern.ERROR_REJECT)
                    Toast.makeText(context, e.localizedMessage ?: "Location error", Toast.LENGTH_SHORT).show()
                } finally {
                    isLocating = false
                }
            }
        } else {
            expressHaptics.play(ExpressiveHapticPattern.ERROR_REJECT)
            Toast.makeText(context, locationPermissionDeniedText, Toast.LENGTH_LONG).show()
        }
    }

    // ビジュアルサイズの動的切り替え（検索モード時はキーボード領域を確保するため少しスリムに調整）
    val visualSize by animateDpAsState(
        targetValue = if (isSearchActive) 210.dp else 290.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "VisualSize"
    )
    val iconSize by animateDpAsState(
        targetValue = if (isSearchActive) 86.dp else 120.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "IconSize"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // ================= 背景: メイン画面の晴れ空 ＋ ぼかし =================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(22.dp)
        ) {
            // 1. スカイグラデーション
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(atmosphere.skyGradient)
            )

            // 2. 晴れのアトモスフィアキャンバス（太陽の光彩、フレア、微動）
            WeatherAtmosphereCanvas(
                condition = WeatherCondition.CLEAR,
                atmosphere = atmosphere,
                tiltState = tiltState,
                modifier = Modifier.fillMaxSize()
            )

            // 3. 昼間地域の上質な光拡散フロスト
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.08f))
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
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isSearchActive = false
                        focusManager.clearFocus()
                    }
            )
        }

        // ================= メインコンテンツレイヤー =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ================= 1. 上部: 「ようこそ」表記（もう少し下に配置） =================
            Spacer(modifier = Modifier.height(54.dp))

            AnimatedVisibility(
                visible = !isSearchActive,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = welcomeText,
                        fontFamily = GoogleSansFlexWelcome,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ================= 2. 中心: 不透明白MaterialShape（Pill含む多彩な変形）＋ 大きなアプリアイコン =================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MorphingSetupVisual(
                    visualSize = visualSize,
                    iconSize = iconSize
                )
            }

            // ================= 3. 下部: アクションエリア =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                            .padding(bottom = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = locatingPrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.primary
                        )
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
                                isLocating = true
                                coroutineScope.launch {
                                    try {
                                        val location = locationProvider.getCurrentLocation(forceFresh = true)
                                        deviceLocation = location
                                        val city = if (location != null) {
                                            locationProvider.resolveCity(location)
                                        } else {
                                            repository.defaultCity
                                        }
                                        completeSetupWithCity(city)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.localizedMessage ?: "Error", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLocating = false
                                    }
                                }
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

                // ================= 検索エリア: 上にせり上がる入力欄 ＋ 候補リスト / 直近履歴 =================
                // 検索モード時は直上に候補カードまたは検索履歴カードが展開
                val showPopup = isSearchActive && (searchQuery.isNotBlank() || isSearching || searchResults.isNotEmpty() || recentSearches.isNotEmpty())
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
                            .heightIn(max = 250.dp)
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
                            // 検索結果または直近の検索履歴を表示
                            val isShowingHistory = searchQuery.isBlank() && recentSearches.isNotEmpty()
                            val displayList = if (isShowingHistory) recentSearches else searchResults

                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isShowingHistory) {
                                    item {
                                        Text(
                                            text = recentSearchesText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                items(displayList) { city ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isCompletingSetup) {
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
                                                imageVector = if (isShowingHistory) Icons.Rounded.History else Icons.Rounded.LocationCity,
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
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(enabled = !isSearchActive) {
                            isSearchActive = true
                        }
                ) {
                    if (isSearchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
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
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
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

                            // クリア / 閉じるボタン
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
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close search",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        // 通常時: 縁を無くして文字とアイコンを完全中央揃え
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

                if (!isSearchActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (currentLanguage) {
                            AppLanguage.JAPANESE -> "プライバシーポリシーを確認"
                            AppLanguage.SIMPLIFIED_CHINESE -> "查看隐私政策"
                            AppLanguage.TRADITIONAL_CHINESE -> "查看隱私權政策"
                            AppLanguage.ENGLISH -> "Privacy Policy"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier
                            .clickable { showPrivacyPolicyDialog = true }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
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
