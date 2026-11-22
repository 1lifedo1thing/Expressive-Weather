package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ツールバーの表示モード
 */
enum class ToolbarMode {
    NORMAL,        // 通常（変形前）：[ 📍 地名 ^ ] [ 🔍 ]  +  [ ⚙ ]
    SAVED_CITIES,  // 登録地名一覧（上方向に変形拡大）
    SEARCH         // 検索モード（添付画像上の横長入力欄に変形）：[ 📍 ] [ 🔍 aaaa | ]
}

fun City.localizedName(language: AppLanguage): String {
    return if (language == AppLanguage.ENGLISH) {
        nameEn.ifBlank { name }
    } else {
        name
    }
}

/**
 * Material 3 Expressive 洗練された適度なタップモディファイア
 * 以前の過剰な縮小・巨大化・多重バウンス振動を解消し、
 * 指のタップに素直に沈み込み（0.94f〜0.96f）、自然にしなやかに復帰する上質なレスポンスを提供。
 */
@Composable
fun Modifier.m3eBouncyClick(
    scaleDown: Float = 0.94f,
    overshootScale: Float = 1.0f,
    hapticPattern: ExpressiveHapticPattern = ExpressiveHapticPattern.EXPRESSIVE_BOUNCE,
    onClick: () -> Unit
): Modifier {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val expressHaptics = LocalExpressiveHaptics.current
    val currentOnClick by rememberUpdatedState(onClick)

    return this
        .scale(scale.value)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            expressHaptics.play(hapticPattern)
            scope.launch {
                // 指で軽く押された自然な沈み込み
                scale.animateTo(
                    targetValue = scaleDown.coerceIn(0.92f, 0.98f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 1200f
                    )
                )
                // 余計なオーバーシュートやプルプルした振動を残さず、スッと品よく定位置（1f）に復帰
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            currentOnClick()
        }
}

/**
 * Material 3 Expressive Morphing Floating Toolbar
 * - 枠線完全削除（border = null）
 * - テーマカラー完全連動
 * - 添付画像の変形（下: 通常時 ↔ 上: 検索時）を完全再現
 * - 地名タップで「登録した地名のみの一覧」へと上方向に変形拡大
 */
@Composable
fun ExpressiveFloatingToolbar(
    currentCity: City,
    defaultCity: City,
    savedCities: List<City>,
    recentSearches: List<City> = emptyList(),
    searchResults: List<City>,
    isSearching: Boolean,
    isDark: Boolean = false,
    toolbarMode: ToolbarMode = ToolbarMode.NORMAL,
    onToolbarModeChange: (ToolbarMode) -> Unit = {},
    onCitySelected: (City, Boolean) -> Unit,
    onSaveCity: (City) -> Unit = {},
    onRemoveSavedCity: (City) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    var activeToolbarMode by remember(toolbarMode) { mutableStateOf(toolbarMode) }
    
    fun setToolbarMode(newMode: ToolbarMode) {
        activeToolbarMode = newMode
        onToolbarModeChange(newMode)
    }

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val gearRotation = remember { Animatable(0f) }

    // --- Material 3 Expressive 洗練された適度な弾性スプリング（Expressive & Controlled Bounce） ---
    val expressiveSpringDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioLowBouncy, // 加減の効いた上品な弾力（余計な波打ち・揺れ戻しを防止）
        stiffness = 500f      // 高速応答（キビキビと素早い展開・縮小）
    )
    val expressiveSpringFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 500f
    )
    val expressiveSpringIntSize = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 500f
    )

    // --- カラーパレット（デバイスカラー / MaterialTheme.colorScheme / Dynamic Color に完全対応） ---
    // ユーザーリクエスト：「外側カプセルもExpressiveColorでもう少し色がついているようなかんじに」
    // 外枠カプセル：白・グレーではなく、テーマの Expressive Color（primaryContainer）をブレンドしてしっかり色づけ
    val capsuleContainerColor = lerp(
        colorScheme.surfaceContainerHigh,
        colorScheme.primaryContainer,
        if (isDark) 0.38f else 0.45f
    )

    val capsuleBorderColor = lerp(
        colorScheme.outlineVariant,
        colorScheme.primary,
        0.25f
    ).copy(alpha = if (isDark) 0.25f else 0.18f)

    val pillContainerColor = colorScheme.primaryContainer
    val pillContentColor = colorScheme.onPrimaryContainer
    val settingsContainerColor = colorScheme.primary
    val fabContentColor = colorScheme.onPrimary

    // 検索モードに入った時に自動でフォーカス
    LaunchedEffect(activeToolbarMode) {
        if (activeToolbarMode == ToolbarMode.SEARCH) {
            delay(120)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            focusManager.clearFocus()
            searchQuery = ""
            onSearchQueryChange("")
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp), // 影が下端でクリップされないよう調整
        contentAlignment = Alignment.BottomCenter
    ) {
        val totalWidth = maxWidth
        val fabSize = 64.dp
        val spacing = 10.dp
        val normalToolbarWidth = (totalWidth - fabSize - spacing).coerceIn(180.dp, 320.dp)
        val expandedToolbarWidth = totalWidth

        val animatedCornerRadius by animateDpAsState(
            targetValue = if (activeToolbarMode == ToolbarMode.SAVED_CITIES) 28.dp else 32.dp,
            animationSpec = expressiveSpringDp,
            label = "ToolbarCornerRadius"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.wrapContentSize()
        ) {
            // --- 検索候補ポップアップ（検索モードかつ文字入力中または検索中の場合に上部に展開） ---
            AnimatedVisibility(
                visible = activeToolbarMode == ToolbarMode.SEARCH && (searchQuery.isNotBlank() || isSearching),
                enter = fadeIn(expressiveSpringFloat) +
                        expandVertically(expandFrom = Alignment.Bottom, animationSpec = expressiveSpringIntSize),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                        shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = capsuleContainerColor,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, capsuleBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .heightIn(max = 280.dp)
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
                                    color = pillContentColor,
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = strings.searchingCities,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurface,
                                    fontFamily = GoogleSansFlex
                                )
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noCitiesFound,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                fontFamily = GoogleSansFlex
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { city -> "${city.id}_${city.latitude}_${city.longitude}" }
                            ) { city ->
                                val isSaved = savedCities.any { it.id == city.id || (it.latitude == city.latitude && it.longitude == city.longitude) }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .m3eBouncyClick {
                                            // 地名タップ時は天気を選択表示するのみ（自動登録はしない）
                                            onCitySelected(city, false)
                                            setToolbarMode(ToolbarMode.NORMAL)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = pillContainerColor,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocationCity,
                                                    contentDescription = null,
                                                    tint = pillContentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = city.localizedName(language),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface,
                                                fontFamily = GoogleSansFlex
                                            )
                                            if (city.country.isNotBlank()) {
                                                Text(
                                                    text = city.country,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontFamily = GoogleSansFlex
                                                )
                                            }
                                        }

                                        // ユーザーリクエスト：横に＋マークを配置してそれで地名登録をする
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSaved) colorScheme.surfaceContainerHighest else pillContainerColor,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .m3eBouncyClick(
                                                    hapticPattern = if (isSaved) ExpressiveHapticPattern.SWITCH_OFF else ExpressiveHapticPattern.SWITCH_ON
                                                ) {
                                                    if (isSaved) {
                                                        onRemoveSavedCity(city)
                                                    } else {
                                                        onSaveCity(city)
                                                    }
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                                                    contentDescription = if (isSaved) strings.removeSaved else strings.addLocation,
                                                    tint = pillContentColor,
                                                    modifier = Modifier.size(20.dp)
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

            // --- メインナビゲーションバー（通常時 / 検索時 / 登録地名一覧時） ---
            Row(
                modifier = Modifier.wrapContentSize(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(if (activeToolbarMode == ToolbarMode.NORMAL) spacing else 0.dp)
            ) {
                // 変形するメインコンテナ（通常時・検索時は完全なピル型 CircleShape、一覧展開時のみ RoundedCornerShape）
                Surface(
                    shape = if (activeToolbarMode == ToolbarMode.SAVED_CITIES) RoundedCornerShape(animatedCornerRadius) else CircleShape,
                    color = capsuleContainerColor,
                    shadowElevation = 3.dp, // 不自然な黒い影と境界クリッピングを防止
                    border = BorderStroke(1.dp, capsuleBorderColor),
                    modifier = Modifier
                        .wrapContentSize()
                        .then(
                            if (activeToolbarMode == ToolbarMode.SAVED_CITIES) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {} // ポップアップ内の余白タップを消費し、背後スクリムへの透過を防止
                                )
                            } else Modifier
                        )
                ) {
                    AnimatedContent(
                        targetState = (activeToolbarMode == ToolbarMode.SAVED_CITIES),
                        transitionSpec = {
                            if (targetState) {
                                // 展開時（NORMAL -> SAVED_CITIES）：加減の効いた上品な弾性スプリングで展開
                                fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f)) togetherWith
                                fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f)) using
                                SizeTransform(
                                    clip = true,
                                    sizeAnimationSpec = { _, _ ->
                                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f)
                                    }
                                )
                            } else {
                                // 縮小時（SAVED_CITIES -> NORMAL）：横と縦が同一タイミングで一体縮小
                                fadeIn(tween(durationMillis = 180, delayMillis = 40)) togetherWith
                                fadeOut(tween(durationMillis = 90)) using
                                SizeTransform(
                                    clip = true,
                                    sizeAnimationSpec = { _, _ ->
                                        tween(
                                            durationMillis = 220,
                                            easing = FastOutSlowInEasing
                                        )
                                    }
                                )
                            }
                        },
                        contentAlignment = Alignment.BottomCenter,
                        label = "ToolbarMorphContent"
                    ) { isSavedCities ->
                        if (isSavedCities) {
                            // ==================== 【登録地名一覧モード（3段構造・スクロール対応）】 ====================
                            // 1. 最上段：現在地（固定・削除不能）
                            // 2. 登録した地名（個別削除可能）
                            // 3. 最近検索した項目（最大2件）
                            val configuration = LocalConfiguration.current
                            val maxPopupHeight = (configuration.screenHeightDp.dp * 0.72f).coerceIn(360.dp, 600.dp)

                            Column(
                                modifier = Modifier
                                    .width(totalWidth)
                                    .heightIn(max = maxPopupHeight)
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                // 固定ヘッダー行：[ 📍 地名の選択 ] [ ✕ 閉じる ]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NearMe,
                                            contentDescription = null,
                                            tint = pillContentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = strings.selectLocation,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = pillContentColor,
                                            fontFamily = GoogleSansFlex
                                        )
                                    }

                                    // 閉じるボタン
                                    Surface(
                                        shape = CircleShape,
                                        color = pillContainerColor,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .m3eBouncyClick {
                                                setToolbarMode(ToolbarMode.NORMAL)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = strings.close,
                                                tint = pillContentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // スクロール可能コンテンツ領域（現在地 ＋ 登録地名 ＋ 最近の検索）
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {

                            // -------------------------------------------------------------
                            // 1. 最上部固定：現在地（削除不能）
                            // -------------------------------------------------------------
                            val isDefaultSelected = currentCity.id == defaultCity.id ||
                                    (currentCity.latitude == defaultCity.latitude && currentCity.longitude == defaultCity.longitude)

                            val defaultCardBg = if (isDefaultSelected) pillContainerColor else colorScheme.surfaceContainerHighest
                            val defaultCardContent = if (isDefaultSelected) pillContentColor else colorScheme.onSurface

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = defaultCardBg,
                                border = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .m3eBouncyClick {
                                        onCitySelected(defaultCity, false)
                                        setToolbarMode(ToolbarMode.NORMAL)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MyLocation,
                                            contentDescription = "現在地",
                                            tint = defaultCardContent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            val currentLocLabel = if (defaultCity.name == "現在地") {
                                                strings.currentLocation
                                            } else {
                                                strings.currentLocationWithCity(defaultCity.localizedName(language))
                                            }
                                            Text(
                                                text = currentLocLabel,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (isDefaultSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = defaultCardContent,
                                                fontFamily = GoogleSansFlex
                                            )
                                            if (defaultCity.country.isNotBlank()) {
                                                Text(
                                                    text = defaultCity.country,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = defaultCardContent.copy(alpha = 0.75f),
                                                    fontFamily = GoogleSansFlex
                                                )
                                            }
                                        }
                                    }

                                    if (isDefaultSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "選択中",
                                            tint = pillContentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // 2. 登録した地名セクション
                            // -------------------------------------------------------------
                            Text(
                                text = strings.savedLocations,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant,
                                fontFamily = GoogleSansFlex,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )

                            if (savedCities.isEmpty()) {
                                Text(
                                    text = strings.noSavedLocations,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = GoogleSansFlex,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                                )
                            } else {
                                savedCities.forEach { city ->
                                    val isSelected = city.id == currentCity.id ||
                                            (city.latitude == currentCity.latitude && city.longitude == currentCity.longitude)
                                    val itemBg = if (isSelected) pillContainerColor else colorScheme.surfaceContainerHighest
                                    val itemContent = if (isSelected) pillContentColor else colorScheme.onSurface

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = itemBg,
                                        border = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .m3eBouncyClick {
                                                onCitySelected(city, false)
                                                setToolbarMode(ToolbarMode.NORMAL)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocationCity,
                                                    contentDescription = null,
                                                    tint = itemContent,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = city.localizedName(language),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = itemContent,
                                                        fontFamily = GoogleSansFlex
                                                    )
                                                    if (city.country.isNotBlank()) {
                                                        Text(
                                                            text = city.country,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = itemContent.copy(alpha = 0.75f),
                                                            fontFamily = GoogleSansFlex
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = strings.selected,
                                                        tint = itemContent,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                // 登録解除ボタン
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            onRemoveSavedCity(city)
                                                        }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = strings.removeSaved,
                                                        tint = itemContent.copy(alpha = 0.65f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // 3. 最近検索した項目セクション（最大2件）
                            // -------------------------------------------------------------
                            if (recentSearches.isNotEmpty()) {
                                Text(
                                    text = strings.recentSearches,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontFamily = GoogleSansFlex,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )

                                recentSearches.take(2).forEach { city ->
                                    val isSelected = city.id == currentCity.id ||
                                            (city.latitude == currentCity.latitude && city.longitude == currentCity.longitude)
                                    val isSaved = savedCities.any { it.id == city.id || (it.latitude == city.latitude && it.longitude == city.longitude) }
                                    val recentBg = if (isSelected) pillContainerColor else colorScheme.surfaceContainerHighest
                                    val recentContent = if (isSelected) pillContentColor else colorScheme.onSurface

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = recentBg,
                                        border = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .m3eBouncyClick {
                                                // 地名タップ時は天気を選択表示するのみ（自動登録はしない）
                                                onCitySelected(city, false)
                                                setToolbarMode(ToolbarMode.NORMAL)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.History,
                                                    contentDescription = "履歴",
                                                    tint = recentContent.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = city.localizedName(language),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = recentContent,
                                                        fontFamily = GoogleSansFlex
                                                    )
                                                    if (city.country.isNotBlank()) {
                                                        Text(
                                                            text = city.country,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = recentContent.copy(alpha = 0.7f),
                                                            fontFamily = GoogleSansFlex
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = strings.selected,
                                                        tint = pillContentColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // ユーザーリクエスト：横に＋マークを配置して地名登録を行う
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isSaved) colorScheme.surfaceContainerHighest else pillContainerColor,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .m3eBouncyClick(
                                                            hapticPattern = if (isSaved) ExpressiveHapticPattern.SWITCH_OFF else ExpressiveHapticPattern.SWITCH_ON
                                                        ) {
                                                            if (isSaved) {
                                                                onRemoveSavedCity(city)
                                                            } else {
                                                                onSaveCity(city)
                                                            }
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                                                            contentDescription = if (isSaved) strings.removeSaved else strings.addLocation,
                                                            tint = pillContentColor,
                                                            modifier = Modifier.size(18.dp)
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
                } else {
                        // ==================== 【通常時 ↔ 検索時のコンテナ変形（左右モーフィング）】 ====================
                        // 添付画像の下側（通常） ↔ 上側（検索）を流れるような幾何学的モーフィングで実現
                        val isSearch = activeToolbarMode == ToolbarMode.SEARCH
                        val circleSize = 54.dp
                        val innerSpacing = 8.dp

                        val targetBarWidth = if (isSearch) totalWidth else normalToolbarWidth
                        val currentBarWidth by animateDpAsState(
                            targetValue = targetBarWidth,
                            animationSpec = expressiveSpringDp,
                            label = "BarWidth"
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .width(currentBarWidth)
                                .height(64.dp)
                                .padding(5.dp)
                        ) {
                            val totalInnerWidth = maxWidth
                            val expandedItemWidth = (totalInnerWidth - circleSize - innerSpacing).coerceAtLeast(circleSize)

                            val leftWidth by animateDpAsState(
                                targetValue = if (isSearch) circleSize else expandedItemWidth,
                                animationSpec = expressiveSpringDp,
                                label = "LeftMorphWidth"
                            )

                            val rightWidth by animateDpAsState(
                                targetValue = if (isSearch) expandedItemWidth else circleSize,
                                animationSpec = expressiveSpringDp,
                                label = "RightMorphWidth"
                            )

                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(innerSpacing)
                            ) {
                                // 左側パーツ：通常時は [ ✈ 東京 ^ ]、検索時は丸ボタン [ ✈ ]（タップでキャンセル）
                                Surface(
                                    shape = CircleShape,
                                    color = pillContainerColor,
                                    border = null,
                                    modifier = Modifier
                                        .width(leftWidth)
                                        .fillMaxHeight()
                                        .m3eBouncyClick {
                                            if (isSearch) {
                                                setToolbarMode(ToolbarMode.NORMAL)
                                            } else {
                                                setToolbarMode(ToolbarMode.SAVED_CITIES)
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isSearch) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.NearMe,
                                                        contentDescription = strings.savedCities,
                                                        tint = pillContentColor,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Text(
                                                        text = currentCity.localizedName(language),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = pillContentColor,
                                                        fontSize = 16.sp,
                                                        fontFamily = GoogleSansFlex
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                                    contentDescription = strings.savedCities,
                                                    tint = pillContentColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.NearMe,
                                                contentDescription = strings.close,
                                                tint = pillContentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                // 右側パーツ：通常時は丸ボタン [ 🔍 ]、検索時は横長検索バー [ 🔍 aaaa | (✕) ]
                                Surface(
                                    shape = CircleShape,
                                    color = pillContainerColor,
                                    border = null,
                                    modifier = Modifier
                                        .width(rightWidth)
                                        .fillMaxHeight()
                                        .then(
                                            if (!isSearch) {
                                                Modifier.m3eBouncyClick {
                                                    setToolbarMode(ToolbarMode.SEARCH)
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isSearch) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = strings.searchCityPlaceholder,
                                                tint = pillContentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Search,
                                                    contentDescription = null,
                                                    tint = pillContentColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                BasicTextField(
                                                    value = searchQuery,
                                                    onValueChange = {
                                                        searchQuery = it
                                                        onSearchQueryChange(it)
                                                    },
                                                    textStyle = TextStyle(
                                                        fontFamily = GoogleSansFlex,
                                                        color = pillContentColor,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    cursorBrush = SolidColor(pillContentColor),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                    keyboardActions = KeyboardActions(onSearch = {
                                                        focusManager.clearFocus()
                                                    }),
                                                    decorationBox = { innerTextField ->
                                                        if (searchQuery.isEmpty()) {
                                                            Text(
                                                                text = strings.searchCityPlaceholder,
                                                                color = pillContentColor.copy(alpha = 0.65f),
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                fontFamily = GoogleSansFlex
                                                            )
                                                        }
                                                        innerTextField()
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .focusRequester(focusRequester)
                                                )

                                                if (searchQuery.isNotEmpty()) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = strings.close,
                                                        tint = pillContentColor,
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                searchQuery = ""
                                                                onSearchQueryChange("")
                                                            }
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
                }

                // 独立した設定FAB（通常モード時のみ表示、検索時や展開時はスムーズに縮小退場）
                AnimatedVisibility(
                    visible = activeToolbarMode == ToolbarMode.NORMAL,
                    enter = fadeIn(tween(durationMillis = 150, delayMillis = 80)) +
                            scaleIn(
                                initialScale = 0.8f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f)
                            ),
                    exit = fadeOut(tween(durationMillis = 70)) +
                            scaleOut(
                                targetScale = 0.8f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f)
                            )
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = settingsContainerColor,
                        shadowElevation = 3.dp,
                        border = null,
                        modifier = Modifier
                            .size(fabSize)
                            .m3eBouncyClick {
                                scope.launch {
                                    gearRotation.animateTo(
                                        targetValue = gearRotation.value + 90f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 400f)
                                    )
                                }
                                onSettingsClick()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = strings.settingsTitle,
                                tint = fabContentColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(gearRotation.value)
                            )
                        }
                    }
                }
            }
        }
    }
}
