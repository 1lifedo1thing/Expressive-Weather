package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.getLocalizedWindDirection
import io.github.nemynew.expressiveweather.theme.WeatherAtmosphereTheme
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Expressive ベントグリッド:
 * - 各天候パネルの外枠シェイプは Circle, Square, 6-sided cookie の3種類のみに限定。
 * - 各ボックスの背景および内包全要素は端末のデバイステーマカラー (MaterialTheme.colorScheme) に厳格に追従。
 * - 【新機能】長押し＋ドラッグ＆ドロップで各タイルの順番を自由に入れ替え可能（滑らかな物理スプリング補間）。
 * - 【新機能】短押しタップで各タイルの詳細情報画面を表示。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherBentoGrid(
    weather: WeatherData,
    atmosphere: WeatherAtmosphereTheme,
    cardOrder: List<BentoCardType> = BentoCardType.DEFAULT_ORDER,
    onOrderChange: (List<BentoCardType>) -> Unit = {},
    onCardClick: (BentoCardType) -> Unit = {},
    modifier: Modifier = Modifier,
    condition: io.github.nemynew.expressiveweather.data.WeatherCondition? = null,
    isFrostedGlassEnabled: Boolean = true,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    isColorful: Boolean = true,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    val expressHaptics = LocalExpressiveHaptics.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val effectiveCondition = condition ?: weather.condition
    val isSnowing = effectiveCondition == io.github.nemynew.expressiveweather.data.WeatherCondition.SNOW

    // 現在のタイル順序リスト（リアクティブ状態）
    val currentOrder = remember {
        mutableStateListOf<BentoCardType>().apply {
            addAll(cardOrder.ifEmpty { BentoCardType.DEFAULT_ORDER })
        }
    }
    LaunchedEffect(cardOrder) {
        if (cardOrder.isNotEmpty() && currentOrder.toList() != cardOrder) {
            currentOrder.clear()
            currentOrder.addAll(cardOrder)
        }
    }

    // ドラッグ中の状態管理
    var draggingCard by remember { mutableStateOf<BentoCardType?>(null) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // ドラッグ終了後の吸い付き（セトリング）アニメーション管理
    var settlingCard by remember { mutableStateOf<BentoCardType?>(null) }
    val settleAnimOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val spacing = 16.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val totalWidth = maxWidth
        val columns = when {
            totalWidth >= 760.dp -> 4
            totalWidth >= 520.dp -> 3
            else -> 2
        }
        val tileWidth = (totalWidth - spacing * (columns - 1)) / columns
        val tileHeight = tileWidth // 1:1 正方形比率

        val totalCards = currentOrder.size
        val numRows = (totalCards + columns - 1) / columns
        val totalHeight = (tileHeight * numRows) + (spacing * (numRows - 1).coerceAtLeast(0))

        val spacingPx = with(density) { spacing.toPx() }
        val tileWidthPx = with(density) { tileWidth.toPx() }
        val tileHeightPx = with(density) { tileHeight.toPx() }
        val swapThresholdPx = (tileWidthPx + spacingPx) * 0.5f

        fun getSlotOffset(index: Int): Offset {
            val col = index % columns
            val row = index / columns
            return Offset(
                x = col * (tileWidthPx + spacingPx),
                y = row * (tileHeightPx + spacingPx)
            )
        }

        fun getSlotCenter(index: Int): Offset {
            val topLeft = getSlotOffset(index)
            return Offset(
                x = topLeft.x + tileWidthPx / 2f,
                y = topLeft.y + tileHeightPx / 2f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            BentoCardType.entries.forEach { card ->
                key(card.id) {
                    val isDragging = draggingCard == card
                    val isSettling = settlingCard == card
                    val isElevated = isDragging || isSettling

                    val targetIndex = currentOrder.indexOf(card)
                    val targetOffsetPx = if (targetIndex >= 0) getSlotOffset(targetIndex) else Offset.Zero

                    val animatedOffset by animateOffsetAsState(
                        targetValue = targetOffsetPx,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 450f
                        ),
                        label = "TileOffset_${card.id}"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isElevated) 1.08f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 600f
                        ),
                        label = "TileScale_${card.id}"
                    )

                    val currentRenderOffset = when {
                        isDragging -> dragStartOffset + dragOffset
                        isSettling -> settleAnimOffset.value
                        else -> animatedOffset
                    }

                    val snowShape = when (card) {
                        BentoCardType.UV,
                        BentoCardType.AQI,
                        BentoCardType.SUN_CYCLE -> SnowTileShape.ROUNDED_RECT
                        else -> SnowTileShape.CIRCLE
                    }

                    val latestRenderOffset by rememberUpdatedState(currentRenderOffset)
                    val tileOffsetProvider = remember(card.id) { { latestRenderOffset } }

                    val cardModifier = Modifier
                        .size(tileWidth, tileHeight)
                        .graphicsLayer {
                            translationX = currentRenderOffset.x
                            translationY = currentRenderOffset.y
                            scaleX = scale
                            scaleY = scale
                        }
                        .zIndex(if (isDragging) 100f else if (isSettling) 50f else 1f)
                        .snowTileAccumulation(
                            isSnowing = isSnowing,
                            isDark = atmosphere.isDark,
                            shape = snowShape,
                            cornerRadiusDp = 44.dp,
                            seed = card.ordinal * 17 + 7,
                            tileOffsetProvider = tileOffsetProvider
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isDragging && !isSettling) {
                                expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                                onCardClick(card)
                            }
                        }
                        .semantics {
                            val idx = currentOrder.indexOf(card)
                            val actions = mutableListOf<CustomAccessibilityAction>()
                            if (idx > 0) {
                                actions.add(
                                    CustomAccessibilityAction("Move earlier") {
                                        val cur = currentOrder.indexOf(card)
                                        if (cur > 0) {
                                            expressHaptics.play(ExpressiveHapticPattern.DRAG_SWAP)
                                            currentOrder.removeAt(cur)
                                            currentOrder.add(cur - 1, card)
                                            onOrderChange(currentOrder.toList())
                                            true
                                        } else false
                                    }
                                )
                            }
                            if (idx in 0 until currentOrder.lastIndex) {
                                actions.add(
                                    CustomAccessibilityAction("Move later") {
                                        val cur = currentOrder.indexOf(card)
                                        if (cur in 0 until currentOrder.lastIndex) {
                                            expressHaptics.play(ExpressiveHapticPattern.DRAG_SWAP)
                                            currentOrder.removeAt(cur)
                                            currentOrder.add(cur + 1, card)
                                            onOrderChange(currentOrder.toList())
                                            true
                                        } else false
                                    }
                                )
                            }
                            if (actions.isNotEmpty()) {
                                customActions = actions
                            }
                        }
                        .pointerInput(card, columns, tileWidthPx, tileHeightPx) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    expressHaptics.play(ExpressiveHapticPattern.DRAG_PICKUP, bypassThrottle = true)
                                    val currentIdx = currentOrder.indexOf(card)
                                    dragStartOffset = if (currentIdx >= 0) getSlotOffset(currentIdx) else Offset.Zero
                                    dragOffset = Offset.Zero
                                    draggingCard = card
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount

                                    val currentVisualCenter = dragStartOffset + dragOffset + Offset(
                                        tileWidthPx / 2f,
                                        tileHeightPx / 2f
                                    )

                                    var closestSlot = currentOrder.indexOf(card)
                                    var minDistance = Float.MAX_VALUE
                                    for (i in 0 until currentOrder.size) {
                                        val centerI = getSlotCenter(i)
                                        val dist = (currentVisualCenter - centerI).getDistance()
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestSlot = i
                                        }
                                    }

                                    val fromIdx = currentOrder.indexOf(card)
                                    if (fromIdx != -1 && closestSlot != -1 && fromIdx != closestSlot && minDistance < swapThresholdPx) {
                                        expressHaptics.play(ExpressiveHapticPattern.DRAG_SWAP)
                                        currentOrder.removeAt(fromIdx)
                                        currentOrder.add(closestSlot, card)
                                    }
                                },
                                onDragEnd = {
                                    expressHaptics.play(ExpressiveHapticPattern.DRAG_DROP, bypassThrottle = true)
                                    val currentPos = dragStartOffset + dragOffset
                                    val targetIdx = currentOrder.indexOf(card)
                                    val targetPos = if (targetIdx >= 0) getSlotOffset(targetIdx) else currentPos
                                    val releasedCard = card
                                    draggingCard = null
                                    settlingCard = releasedCard
                                    coroutineScope.launch {
                                        settleAnimOffset.snapTo(currentPos)
                                        settleAnimOffset.animateTo(
                                            targetValue = targetPos,
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = 500f
                                            )
                                        )
                                        if (settlingCard == releasedCard) {
                                            settlingCard = null
                                        }
                                        onOrderChange(currentOrder.toList())
                                    }
                                },
                                onDragCancel = {
                                    expressHaptics.play(ExpressiveHapticPattern.DRAG_DROP)
                                    val currentPos = dragStartOffset + dragOffset
                                    val targetIdx = currentOrder.indexOf(card)
                                    val targetPos = if (targetIdx >= 0) getSlotOffset(targetIdx) else currentPos
                                    val releasedCard = card
                                    draggingCard = null
                                    settlingCard = releasedCard
                                    coroutineScope.launch {
                                        settleAnimOffset.snapTo(currentPos)
                                        settleAnimOffset.animateTo(
                                            targetValue = targetPos,
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = 500f
                                            )
                                        )
                                        if (settlingCard == releasedCard) {
                                            settlingCard = null
                                        }
                                    }
                                }
                            )
                        }

                    BentoCardItem(
                        type = card,
                        weather = weather,
                        tileSize = tileWidth,
                        modifier = cardModifier,
                        isFrostedGlassEnabled = isFrostedGlassEnabled,
                        isDark = atmosphere.isDark,
                        isColorful = isColorful,
                        windSpeedUnit = windSpeedUnit,
                        temperatureUnit = temperatureUnit
                    )
                }
            }
        }
    }
}

/**
 * タイル種別に応じた Composable の振り分け
 */
@Composable
private fun BentoCardItem(
    type: BentoCardType,
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    when (type) {
        BentoCardType.WIND -> WindCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful, windSpeedUnit = windSpeedUnit)
        BentoCardType.UV -> UvCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful)
        BentoCardType.AQI -> AqiCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful)
        BentoCardType.HUMIDITY -> HumidityCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful)
        BentoCardType.SUN_CYCLE -> SunCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful)
        BentoCardType.PRESSURE -> PressureCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful)
        BentoCardType.VISIBILITY -> VisibilityCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful)
        BentoCardType.PRECIPITATION -> PrecipitationCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful)
        BentoCardType.COMFORT -> ComfortCard(weather = weather, tileSize = tileSize, modifier = modifier, isFrostedGlassEnabled = isFrostedGlassEnabled, isDark = isDark, isColorful = isColorful, temperatureUnit = temperatureUnit)
    }
}

/**
 * 風パネル (WindCard):
 * 外枠: Circle (正円)
 * アイコン: Microsoft Fluent UI System Icons (ic_fluent_weather_squalls_24, ic_fluent_arrow_up_right_24)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WindCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val circleShape = MaterialShapes.Circle.toShape()
    val arrowShape = MaterialShapes.Arrow.toShape()
    val gustsMs = weather.windGustsKmH / 3.6
    val convertedSpeed = windSpeedUnit.convertFromKmH(weather.windSpeedKmH, language)
    val displayUnit = windSpeedUnit.getDisplayUnit(language)
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val arrowColor = if (isColorful) Color(0xFF00BCD4) else MaterialTheme.colorScheme.primary
    val headerIconColor = if (isColorful) Color(0xFF00ACC1) else MaterialTheme.colorScheme.primary

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)
    val outerArrowSize = tileSize * 0.68f
    val innerArrowSize = tileSize * 0.58f

    val animatedWindDegree by androidx.compose.animation.core.animateFloatAsState(
        targetValue = weather.windDegree,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "WindDegreeAnimation"
    )

    Surface(
        shape = circleShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ヘッダー: Fluent UI Weather Squalls アイコン + "風"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (16 * scale).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_weather_squalls_24),
                    contentDescription = strings.wind,
                    tint = headerIconColor,
                    modifier = Modifier.size((20 * scale).dp)
                )
                Spacer(modifier = Modifier.width((6 * scale).dp))
                Text(
                    text = strings.wind,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (15 * scale).sp
                )
            }

            // 風向に回転するArrowシェイプ (ライト/ダーク両対応の高コントラストレイヤー) + 通常向きテキスト
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = (10 * scale).dp, bottom = (10 * scale).dp)
            ) {
                // 風向の向きに滑らかに回転するArrow (外層オーラ + メイン層)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(outerArrowSize)
                        .rotate(animatedWindDegree)
                ) {
                    // 外層オーラ (大きめのArrow)
                    Surface(
                        shape = arrowShape,
                        color = arrowColor.copy(alpha = if (isColorful) 0.24f else 0.18f),
                        modifier = Modifier.size(outerArrowSize)
                    ) {}
                    // メイン層 (縁取り線なしのクリーンな多層塗り)
                    Surface(
                        shape = arrowShape,
                        color = arrowColor.copy(alpha = if (isColorful) 0.50f else 0.36f),
                        modifier = Modifier.size(innerArrowSize)
                    ) {}
                }

                // 風向に関わらず通常の向き（水平）で表示される文字情報 (onSurfaceで高コントラスト)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        val speedText = when (windSpeedUnit) {
                            WindSpeedUnit.MS, WindSpeedUnit.FOLLOW_LANGUAGE -> if (language == AppLanguage.JAPANESE) String.format(Locale.US, "%.1f", convertedSpeed) else String.format(Locale.US, "%.0f", convertedSpeed)
                            else -> String.format(Locale.US, "%.0f", convertedSpeed)
                        }
                        Text(
                            text = speedText,
                            fontSize = (36 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = (36 * scale).sp
                        )
                        Spacer(modifier = Modifier.width((3 * scale).dp))
                        Text(
                            text = displayUnit,
                            fontSize = (15 * scale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = (2 * scale).dp)
                        )
                    }
                    Spacer(modifier = Modifier.height((2 * scale).dp))
                    Text(
                        text = getLocalizedWindDirection(weather.windDirection, language),
                        fontSize = (12 * scale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 突風フッター
            val gustLabel = when (language) {
                AppLanguage.JAPANESE -> "突風"
                AppLanguage.ENGLISH -> "Gusts"
                AppLanguage.SIMPLIFIED_CHINESE -> "阵风"
                AppLanguage.TRADITIONAL_CHINESE -> "陣風"
            }
            val gustFormatted = windSpeedUnit.formatFromMs(gustsMs, language)
            Text(
                text = "$gustLabel: $gustFormatted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (11 * scale).sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (12 * scale).dp)
            )
        }
    }
}

/**
 * 紫外線パネル (UvCard):
 * 外枠: Square (角丸正方形)
 * アイコン: Microsoft Fluent UI System Icons (ic_fluent_weather_sunny_high_24)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UvCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true
) {
    val strings = LocalAppStrings.current
    val squareShape = RoundedCornerShape(44.dp)
    val sunnyShape = MaterialShapes.VerySunny.toShape()
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val sunnyColor = if (isColorful) Color(0xFFFFA000) else MaterialTheme.colorScheme.tertiaryContainer
    val onSunnyColor = if (isColorful) Color(0xFF3E2723) else MaterialTheme.colorScheme.onTertiaryContainer
    val uvIconColor = if (isColorful) Color(0xFFFF8F00) else MaterialTheme.colorScheme.tertiary

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)
    val sunnySize = tileSize * 0.62f

    val infiniteTransition = rememberInfiniteTransition(label = "UvAnimation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "UvRotate"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "UvScale"
    )

    Surface(
        shape = squareShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ヘッダー: Fluent UI Weather Sunny High + "紫外線"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (16 * scale).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_weather_sunny_high_24),
                    contentDescription = strings.uvIndex,
                    tint = uvIconColor,
                    modifier = Modifier.size((20 * scale).dp)
                )
                Spacer(modifier = Modifier.width((6 * scale).dp))
                Text(
                    text = strings.uvIndex,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (15 * scale).sp
                )
            }

            // 中央: 回転・パルスするVerySunnyシェイプ + UV指数 & レベルバッジ
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = (10 * scale).dp, bottom = (10 * scale).dp)
            ) {
                Surface(
                    shape = sunnyShape,
                    color = sunnyColor,
                    modifier = Modifier
                        .size(sunnySize)
                        .graphicsLayer {
                            rotationZ = rotation
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                ) {}

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${weather.uvIndex}",
                        fontSize = (36 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = onSunnyColor,
                        lineHeight = (36 * scale).sp
                    )
                    Spacer(modifier = Modifier.height((3 * scale).dp))
                    val themeColorScheme = MaterialTheme.colorScheme
                    val (uvPillBg, uvPillText) = remember(weather.uvIndex, isColorful, themeColorScheme) {
                        if (isColorful) {
                            when {
                                weather.uvIndex <= 2 -> Color(0xFF388E3C) to Color.White // 低い: 穏やかなグリーン
                                weather.uvIndex <= 5 -> Color(0xFFFFA000) to Color(0xFF2E1C00) // 普通: サニーアンバーゴールド
                                weather.uvIndex <= 7 -> Color(0xFFE65100) to Color.White // 強い: 濃いコーラルオレンジ
                                weather.uvIndex <= 10 -> Color(0xFFC62828) to Color.White // 非常に強い: 深紅
                                else -> Color(0xFF6A1B9A) to Color.White // 極端: 濃密な紫
                            }
                        } else {
                            when {
                                weather.uvIndex <= 2 -> themeColorScheme.surfaceContainerHighest to themeColorScheme.onSurface
                                weather.uvIndex <= 5 -> themeColorScheme.tertiaryContainer to themeColorScheme.onTertiaryContainer
                                weather.uvIndex <= 7 -> themeColorScheme.tertiary to themeColorScheme.onTertiary
                                weather.uvIndex <= 10 -> themeColorScheme.errorContainer to themeColorScheme.onErrorContainer
                                else -> themeColorScheme.error to themeColorScheme.onError
                            }
                        }
                    }
                    val uvLevelLocalized = when {
                        weather.uvIndex <= 2 -> strings.uvLevelLow
                        weather.uvIndex <= 5 -> strings.uvLevelModerate
                        weather.uvIndex <= 7 -> strings.uvLevelHigh
                        weather.uvIndex <= 10 -> strings.uvLevelVeryHigh
                        else -> strings.uvLevelExtreme
                    }
                    Spacer(modifier = Modifier.height((3 * scale).dp))
                    Surface(
                        shape = CircleShape,
                        color = uvPillBg
                    ) {
                        Text(
                            text = uvLevelLocalized,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = uvPillText,
                            fontSize = (11 * scale).sp,
                            modifier = Modifier.padding(horizontal = (10 * scale).dp, vertical = (3 * scale).dp)
                        )
                    }
                }
            }

            // フッター: 日最高UV
            val maxUv = weather.dailyForecast.firstOrNull()?.uvMax?.toInt() ?: weather.uvIndex
            Text(
                text = strings.uvDailyMax(maxUv),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (11 * scale).sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (12 * scale).dp)
            )
        }
    }
}

/**
 * 空気質パネル (AqiCard):
 * 外枠: 6-sided cookie (6面クッキーシェイプ)
 * アイコン: Microsoft Fluent UI System Icons (ic_fluent_weather_duststorm_24)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AqiCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true
) {
    val strings = LocalAppStrings.current
    val squareShape = RoundedCornerShape(44.dp)
    val puffyShape = MaterialShapes.Puffy.toShape()
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val puffyColor = if (isColorful) Color(0xFF00BFA5) else MaterialTheme.colorScheme.secondary
    val aqiIconColor = if (isColorful) Color(0xFF00BFA5) else MaterialTheme.colorScheme.secondary

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)
    val outerPuffySize = tileSize * 0.64f
    val innerPuffySize = tileSize * 0.56f

    val infiniteTransition = rememberInfiniteTransition(label = "AqiAnimation")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AqiFloatY"
    )
    val aqiScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AqiScale"
    )

    Surface(
        shape = squareShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ヘッダー: Fluent UI Weather Duststorm + "空気質"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (16 * scale).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_weather_duststorm_24),
                    contentDescription = strings.airQuality,
                    tint = aqiIconColor,
                    modifier = Modifier.size((20 * scale).dp)
                )
                Spacer(modifier = Modifier.width((6 * scale).dp))
                Text(
                    text = strings.airQuality,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (15 * scale).sp
                )
            }

            // 中央: 浮遊・脈動するPuffy(雲/空気)シェイプ (外層オーラ + メイン層、縁取り線なし) + AQI数値 & レベルバッジ
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = (10 * scale).dp, bottom = (10 * scale).dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(outerPuffySize)
                        .graphicsLayer {
                            translationY = floatY * 2.5f
                            scaleX = aqiScale
                            scaleY = aqiScale
                        }
                ) {
                    // 外層オーラ
                    Surface(
                        shape = puffyShape,
                        color = puffyColor.copy(alpha = if (isColorful) 0.24f else 0.18f),
                        modifier = Modifier.size(outerPuffySize)
                    ) {}
                    // メイン層 (縁取り線なしのクリーンな塗り)
                    Surface(
                        shape = puffyShape,
                        color = puffyColor.copy(alpha = if (isColorful) 0.50f else 0.35f),
                        modifier = Modifier.size(innerPuffySize)
                    ) {}
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${weather.airQuality.aqi}",
                        fontSize = (36 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = (36 * scale).sp
                    )
                    val themeColorScheme = MaterialTheme.colorScheme
                    val (aqiPillBg, aqiPillText) = remember(weather.airQuality.aqi, isColorful, themeColorScheme) {
                        if (isColorful) {
                            when {
                                weather.airQuality.aqi <= 50 -> Color(0xFF00897B) to Color.White // 良い: 澄んだミントティール（青緑系・純度高）
                                weather.airQuality.aqi <= 100 -> Color(0xFF00796B) to Color.White // 普通: 豊かなオーシャンティール（Puffy雲シェイプと完全調和する深みある青緑）
                                weather.airQuality.aqi <= 150 -> Color(0xFF006155) to Color.White // 敏感に影響: 濃密スプルースティール（適度な重みのある青緑）
                                weather.airQuality.aqi <= 200 -> Color(0xFF004D40) to Color.White // 不健康: ディープパインティール（警戒感のある濃青緑）
                                else -> Color(0xFF00332B) to Color.White // 危険: 最も濃密なミッドナイトアビステール
                            }
                        } else {
                            when {
                                weather.airQuality.aqi <= 50 -> themeColorScheme.secondaryContainer to themeColorScheme.onSecondaryContainer
                                weather.airQuality.aqi <= 100 -> themeColorScheme.secondary to themeColorScheme.onSecondary
                                weather.airQuality.aqi <= 150 -> themeColorScheme.primaryContainer to themeColorScheme.onPrimaryContainer
                                weather.airQuality.aqi <= 200 -> themeColorScheme.errorContainer to themeColorScheme.onErrorContainer
                                else -> themeColorScheme.error to themeColorScheme.onError
                            }
                        }
                    }
                    val aqiLevelLocalized = when {
                        weather.airQuality.aqi <= 50 -> strings.aqiLevelGood
                        weather.airQuality.aqi <= 100 -> strings.aqiLevelModerate
                        weather.airQuality.aqi <= 150 -> strings.aqiLevelSensitive
                        weather.airQuality.aqi <= 200 -> strings.aqiLevelUnhealthy
                        else -> strings.aqiLevelHazardous
                    }
                    Spacer(modifier = Modifier.height((3 * scale).dp))
                    Surface(
                        shape = CircleShape,
                        color = aqiPillBg
                    ) {
                        Text(
                            text = aqiLevelLocalized,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = aqiPillText,
                            fontSize = (11 * scale).sp,
                            modifier = Modifier.padding(horizontal = (10 * scale).dp, vertical = (3 * scale).dp)
                        )
                    }
                }
            }

            // フッター: PM2.5
            Text(
                text = "PM2.5: ${weather.airQuality.pm25} μg/m³",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (10.5 * scale).sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (14 * scale).dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

/**
 * 湿度パネル (HumidityCard):
 * 外枠: Circle (正円)
 * アイコン: Microsoft Fluent UI System Icons (ic_fluent_weather_humidity_24)
 * 中央背景: 多層の波打つCookie9Sidedリップルシェイプ（ユーザー指定デザイン）
 * 下部: ピル型バッジ（多湿 / 快適 / 乾燥）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HumidityCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true
) {
    val strings = LocalAppStrings.current
    val circleShape = MaterialShapes.Circle.toShape()
    val waveShape = MaterialShapes.Cookie9Sided.toShape()
    val pillShape = CircleShape
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val rippleColor = if (isColorful) Color(0xFF1E88E5) else MaterialTheme.colorScheme.primary
    val rippleContainerColor = if (isColorful) (if (isDark) Color(0xFF1565C0) else Color(0xFFBBDEFB)) else MaterialTheme.colorScheme.primaryContainer
    val onRippleColor = if (isColorful) (if (isDark) Color(0xFFE3F2FD) else Color(0xFF0D47A1)) else MaterialTheme.colorScheme.onPrimaryContainer
    val humidityIconColor = if (isColorful) Color(0xFF1E88E5) else MaterialTheme.colorScheme.primary

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)
    val outerWaveSize = tileSize * 0.66f
    val midWaveSize = tileSize * 0.57f
    val innerWaveSize = tileSize * 0.48f

    val infiniteTransition = rememberInfiniteTransition(label = "HumidityRipple")
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HumidityOuterScale"
    )
    val midScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HumidityMidScale"
    )
    val innerScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HumidityInnerScale"
    )
    val wobbleAngle by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HumidityWobble"
    )

    Surface(
        shape = circleShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ヘッダー: Fluent UI Weather Humidity + "湿度"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (16 * scale).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_weather_humidity_24),
                    contentDescription = strings.humidity,
                    tint = humidityIconColor,
                    modifier = Modifier.size((19 * scale).dp)
                )
                Spacer(modifier = Modifier.width((5 * scale).dp))
                Text(
                    text = strings.humidity,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (14 * scale).sp
                )
            }

            // 中央: 多層の同心円状Cookie9Sidedリップルシェイプ + 湿度数値
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = (8 * scale).dp, bottom = (26 * scale).dp)
            ) {
                // 外層 (薄いトーン / オーラ)
                Surface(
                    shape = waveShape,
                    color = rippleColor.copy(alpha = if (isColorful) 0.22f else 0.20f),
                    modifier = Modifier
                        .size(outerWaveSize)
                        .graphicsLayer {
                            rotationZ = wobbleAngle * 0.7f
                            scaleX = outerScale
                            scaleY = outerScale
                        }
                ) {}

                // 中間層 (中間トーン)
                Surface(
                    shape = waveShape,
                    color = rippleColor.copy(alpha = if (isColorful) 0.44f else 0.40f),
                    modifier = Modifier
                        .size(midWaveSize)
                        .graphicsLayer {
                            rotationZ = -wobbleAngle * 0.8f
                            scaleX = midScale
                            scaleY = midScale
                        }
                ) {}

                // 内層 (明るいコンテナカラー)
                Surface(
                    shape = waveShape,
                    color = rippleContainerColor,
                    modifier = Modifier
                        .size(innerWaveSize)
                        .graphicsLayer {
                            rotationZ = wobbleAngle
                            scaleX = innerScale
                            scaleY = innerScale
                        }
                ) {}

                // 数値情報 (77 %)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${weather.humidity}",
                        fontSize = (36 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = onRippleColor,
                        lineHeight = (36 * scale).sp
                    )
                    Spacer(modifier = Modifier.width((3 * scale).dp))
                    Text(
                        text = "%",
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Medium,
                        color = onRippleColor.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = (2 * scale).dp)
                    )
                }
            }

            // 下部: ピル型バッジ (多湿 / 快適 / 乾燥) - 露点温度は削除
            val comfortLabel = when {
                weather.humidity in 40..60 -> strings.humidityComfortable
                weather.humidity > 60 -> strings.humidityHumid
                else -> strings.humidityDry
            }
            val themeColorScheme = MaterialTheme.colorScheme
            val (humidityPillBg, humidityPillText) = remember(weather.humidity, isColorful, themeColorScheme) {
                if (isColorful) {
                    when {
                        weather.humidity < 30 -> Color(0xFFE64A19) to Color.White // 超乾燥: 濃いコーラル警告オレンジ
                        weather.humidity < 40 -> Color(0xFFFFA000) to Color(0xFF2E1C00) // 乾燥: 温かなアンバー
                        weather.humidity <= 60 -> Color(0xFF2E7D32) to Color.White // 快適: 爽やかなエメラルドグリーン
                        weather.humidity <= 75 -> Color(0xFF0288D1) to Color.White // 多湿: 瑞々しいオーシャンブルー
                        else -> Color(0xFF1565C0) to Color.White // 超多湿: 濃密なロイヤルディープブルー
                    }
                } else {
                    when {
                        weather.humidity < 30 -> themeColorScheme.errorContainer to themeColorScheme.onErrorContainer
                        weather.humidity < 40 -> themeColorScheme.surfaceContainerHighest to themeColorScheme.onSurface
                        weather.humidity <= 60 -> themeColorScheme.tertiary to themeColorScheme.onTertiary
                        weather.humidity <= 75 -> themeColorScheme.primaryContainer to themeColorScheme.onPrimaryContainer
                        else -> themeColorScheme.primary to themeColorScheme.onPrimary
                    }
                }
            }
            Surface(
                shape = pillShape,
                color = humidityPillBg,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (14 * scale).dp)
            ) {
                Text(
                    text = comfortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = humidityPillText,
                    fontSize = (11.5 * scale).sp,
                    modifier = Modifier.padding(horizontal = (14 * scale).dp, vertical = (4 * scale).dp)
                )
            }
        }
    }
}

/**
 * 太陽パネル (SunCard / 旧: SunCycleCard):
 * 外枠: Square (角丸正方形)
 * アイコン: Material 3 Expressive Shapes (MaterialShapes.Sunny)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SunCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true
) {
    val strings = LocalAppStrings.current
    val squareShape = RoundedCornerShape(44.dp)
    val sunnyShape = MaterialShapes.Sunny.toShape()
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val sunColor = if (isColorful) Color(0xFFFF6D00) else MaterialTheme.colorScheme.primary
    val sunAuraColor = if (isColorful) Color(0xFFFF9E80).copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val sunIconColor = if (isColorful) Color(0xFFFF6D00) else MaterialTheme.colorScheme.primary

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)
    val trackHeight = tileSize * 0.38f
    val markerSize = (22 * scale).dp
    val auraSize = (32 * scale).dp

    val arcTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
    val targetProgress = weather.sunSchedule.dayProgress.coerceIn(0f, 1f)

    // 太陽マーカーのアニメーション (日の出位置 0f から目標位置まで回転しながらエクスプレッシブに移動)
    val animProgress = remember { Animatable(0f) }
    val animRotation = remember { Animatable(0f) }

    LaunchedEffect(weather.sunSchedule.dayProgress, weather.sunSchedule.sunrise, weather.sunSchedule.sunset) {
        animProgress.snapTo(0f)
        animRotation.snapTo(0f)
        launch {
            animProgress.animateTo(
                targetValue = targetProgress,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 180f
                )
            )
        }
        launch {
            // 移動しながら太陽自体が自転 (720度回転)
            animRotation.animateTo(
                targetValue = 720f * targetProgress.coerceAtLeast(0.4f),
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 180f
                )
            )
        }
    }

    Surface(
        shape = squareShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = (16 * scale).dp, bottom = (14 * scale).dp, start = (12 * scale).dp, end = (12 * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ヘッダー: Fluent UI Weather Sunny アイコン + "太陽"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fluent_weather_sunny_24),
                        contentDescription = strings.sun,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size((19 * scale).dp)
                    )
                    Spacer(modifier = Modifier.width((5 * scale).dp))
                    Text(
                        text = strings.sun,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (14 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((4 * scale).dp))

                // 上寄りに美しく配置した半楕円軌道 & MaterialShape太陽マーカー
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight),
                    contentAlignment = Alignment.TopStart
                ) {
                    val density = LocalDensity.current
                    val widthPx = with(density) { maxWidth.toPx() }
                    val heightPx = with(density) { maxHeight.toPx() }

                    val markerSizePx = with(density) { markerSize.toPx() }
                    val auraSizePx = with(density) { auraSize.toPx() }
                    val markerRadiusPx = markerSizePx / 2f

                    // 左右マージン: 15.dp (日の出・日の入時刻の直上に自然に位置)
                    val marginXPx = with(density) { (15 * scale).dp.toPx() }
                    val radiusX = (widthPx / 2f) - marginXPx
                    // 軌道底面 cy: 領域の下端付近 (下端から約4.dp上)
                    val cy = heightPx - with(density) { (4 * scale).dp.toPx() }
                    // 高さ方向の半径: 上端に markerRadiusPx + 3.dp の余白を残す
                    val radiusY = cy - markerRadiusPx - with(density) { (3 * scale).dp.toPx() }
                    val cx = widthPx / 2f

                    // 1. 半楕円アーク軌道の描画
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = (3 * scale).dp.toPx()

                        // 横長の半楕円アーク (startAngle 180° から sweep 180°)
                        drawArc(
                            color = arcTrackColor,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(cx - radiusX, cy - radiusY),
                            size = Size(radiusX * 2f, radiusY * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        // 日の出基準点 (180°)
                        drawCircle(
                            color = arcTrackColor,
                            radius = (2.5f * scale).dp.toPx(),
                            center = Offset(cx - radiusX, cy)
                        )
                        // 日の入基準点 (360°)
                        drawCircle(
                            color = arcTrackColor,
                            radius = (2.5f * scale).dp.toPx(),
                            center = Offset(cx + radiusX, cy)
                        )
                    }

                    // 2. 太陽マーカーの現在位置計算 (半楕円上の座標)
                    val curProgress = animProgress.value
                    val angleRad = (180f + curProgress * 180f) * (PI.toFloat() / 180f)
                    val sunCenterX = cx + radiusX * kotlin.math.cos(angleRad)
                    val sunCenterY = cy + radiusY * sin(angleRad)

                    // 3. ふんわり光るオーラ (Aura)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = sunCenterX - auraSizePx / 2f
                                translationY = sunCenterY - auraSizePx / 2f
                            }
                            .size(auraSize)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                shape = CircleShape
                            )
                    )

                    // 4. MaterialShape (MaterialShapes.Sunny) 太陽マーカー (自転アニメーション付き)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = sunCenterX - markerSizePx / 2f
                                translationY = sunCenterY - markerSizePx / 2f
                                rotationZ = animRotation.value
                            }
                            .size(markerSize)
                    ) {
                        Surface(
                            shape = sunnyShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size((7 * scale).dp)
                                ) {}
                            }
                        }
                    }
                }

                // 軌道と日出没表記の間に十分な間隔を確保
                Spacer(modifier = Modifier.height((10 * scale).dp))

                // 日の出・日の入 時刻 (左右端に配置)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (6 * scale).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = strings.sunrise,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (10.5 * scale).sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = weather.sunSchedule.sunrise,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = (14 * scale).sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.sunset,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (10.5 * scale).sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = weather.sunSchedule.sunset,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = (14 * scale).sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 気圧パネル (PressureCard):
 * 外枠: MaterialShapes.Circle (正円)
 * 外周: 太めの円弧ゲージ (135°〜405°、270°のスイープ、Roundキャップ)
 * 上部: 気圧アイコン (ic_atmospheric_pressure) + "気圧"
 * 中央: 大きな気圧数値 (カンマ区切り、例: 1,019)
 * 下部: "hPa" 単位
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PressureCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true
) {
    val strings = LocalAppStrings.current
    val circleShape = MaterialShapes.Circle.toShape()
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val pressureColor = if (isColorful) Color(0xFF7C4DFF) else MaterialTheme.colorScheme.primary
    val trackColor = pressureColor.copy(alpha = if (isColorful) 0.24f else 0.32f)
    val progressColor = pressureColor
    val pressureIconColor = if (isColorful) Color(0xFF7C4DFF) else MaterialTheme.colorScheme.onSurface

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)

    // 気圧レンジ: 970 hPa 〜 1050 hPa (大気圧変動域 80 hPa)
    val minPressure = 970f
    val maxPressure = 1050f
    val targetProgress = ((weather.pressureHpa - minPressure) / (maxPressure - minPressure)).coerceIn(0.04f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "PressureGaugeProgress"
    )

    // 生き生きとした気圧の微小パルスアニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "PressurePulseAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.996f,
        targetValue = 1.004f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PressurePulseScale"
    )

    Surface(
        shape = circleShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 外周円弧ゲージ（タイルのフチ近くに太めに配置）
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((8 * scale).dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
            ) {
                val strokeWidthPx = (13 * scale).dp.toPx()
                val halfStroke = strokeWidthPx / 2f
                val arcSize = Size(
                    width = size.width - strokeWidthPx,
                    height = size.height - strokeWidthPx
                )
                val topLeft = Offset(halfStroke, halfStroke)
                val startAngle = 135f
                val totalSweep = 270f

                // 背景トラック
                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // アクティブプログレス
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = totalSweep * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // 中央コンテンツ (気圧アイコン+テキスト、数値、hPa)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = (18 * scale).dp)
            ) {
                // 上段: 気圧アイコン + "気圧"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_atmospheric_pressure),
                        contentDescription = strings.pressure,
                        tint = pressureIconColor,
                        modifier = Modifier.size((20 * scale).dp)
                    )
                    Spacer(modifier = Modifier.width((6 * scale).dp))
                    Text(
                        text = strings.pressure,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (15 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((6 * scale).dp))

                // 中段: 気圧数値 (カンマ区切り、例: 1,019)
                val formattedPressure = remember(weather.pressureHpa) {
                    String.format(Locale.US, "%,d", weather.pressureHpa)
                }
                Text(
                    text = formattedPressure,
                    fontSize = (36 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp,
                    lineHeight = (36 * scale).sp
                )

                Spacer(modifier = Modifier.height((4 * scale).dp))

                // 下段: "hPa" 単位
                Text(
                    text = "hPa",
                    fontSize = (15 * scale).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * 視程パネル (VisibilityCard):
 * 外枠: MaterialShapes.Circle (正円)
 * 中央: 澄んだ大気・レンズを表現する多層サークルレイヤー
 * 上部: Fluent UI Eye (ic_fluent_eye_24) + "視程"
 * 中央: 大きな視程数値 (例: 29.4 km)
 * 下部: MaterialShapes.Pill による視界状況ピルバッジ (例: 見通し良好)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VisibilityCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true
) {
    val strings = LocalAppStrings.current
    val circleShape = MaterialShapes.Circle.toShape()
    val cookieShape = MaterialShapes.Cookie12Sided.toShape()
    val pillShape = CircleShape
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val lensColor = if (isColorful) Color(0xFF00B0FF) else MaterialTheme.colorScheme.primary
    val eyeIconColor = if (isColorful) Color(0xFF00B0FF) else MaterialTheme.colorScheme.onSurface

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)

    val infiniteTransition = rememberInfiniteTransition(label = "VisibilityPulse")
    val lensScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LensScale"
    )

    Surface(
        shape = circleShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 中央背景: 澄み切った大気のレイヤードCookie12Sided (タイル最大化レンズ効果)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // 外層 (タイル内に収まる最大サイズのCookie12Sided)
                Surface(
                    shape = cookieShape,
                    color = lensColor.copy(alpha = if (isColorful) 0.22f else 0.14f),
                    modifier = Modifier
                        .fillMaxSize(0.93f)
                        .graphicsLayer {
                            scaleX = lensScale
                            scaleY = lensScale
                        }
                ) {}
                // 内層
                Surface(
                    shape = cookieShape,
                    color = lensColor.copy(alpha = if (isColorful) 0.45f else 0.28f),
                    modifier = Modifier.fillMaxSize(0.76f)
                ) {}
            }

            // 中央コンテンツ (縦並び)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = (16 * scale).dp)
            ) {
                // 上段: Fluent UI Eye + "視程"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fluent_eye_24),
                        contentDescription = strings.visibility,
                        tint = eyeIconColor,
                        modifier = Modifier.size((20 * scale).dp)
                    )
                    Spacer(modifier = Modifier.width((6 * scale).dp))
                    Text(
                        text = strings.visibility,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (15 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((4 * scale).dp))

                // 中央: 視程数値 & 単位
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", weather.visibilityKm),
                        fontSize = (36 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = (36 * scale).sp
                    )
                    Spacer(modifier = Modifier.width((3 * scale).dp))
                    Text(
                        text = "km",
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = (2 * scale).dp)
                    )
                }

                Spacer(modifier = Modifier.height((6 * scale).dp))

                // 下段: ピルバッジ (見通し良好 / 視界良好 / 視界不良)
                val visLabel = if (weather.visibilityKm >= 10.0) strings.visibilityExcellent
                               else if (weather.visibilityKm >= 5.0) strings.visibilityModerate
                               else strings.visibilityPoor
                val themeColorScheme = MaterialTheme.colorScheme
                val (visPillBg, visPillText) = remember(weather.visibilityKm, isColorful, themeColorScheme) {
                    if (isColorful) {
                        when {
                            weather.visibilityKm >= 20.0 -> Color(0xFF0097A7) to Color.White // 20km以上（超快晴）: 澄み切ったディープシアン（クッキー水色の上でも高コントラスト）
                            weather.visibilityKm >= 10.0 -> Color(0xFF00838F) to Color.White // 10km以上（良好）: 豊かなダークシアン
                            weather.visibilityKm >= 5.0 -> Color(0xFF00796B) to Color.White // 5km以上（普通）: ディープティール
                            weather.visibilityKm >= 2.0 -> Color(0xFFE65100) to Color.White // 2km以上（かすみ）: 警告アンバーオレンジ
                            else -> Color(0xFFC62828) to Color.White // 2km未満（濃霧・豪雨）: 危険警告レッド
                        }
                    } else {
                        when {
                            weather.visibilityKm >= 20.0 -> themeColorScheme.primary to themeColorScheme.onPrimary
                            weather.visibilityKm >= 10.0 -> themeColorScheme.primaryContainer to themeColorScheme.onPrimaryContainer
                            weather.visibilityKm >= 5.0 -> themeColorScheme.surfaceContainerHighest to themeColorScheme.onSurface
                            weather.visibilityKm >= 2.0 -> themeColorScheme.errorContainer to themeColorScheme.onErrorContainer
                            else -> themeColorScheme.error to themeColorScheme.onError
                        }
                    }
                }
                Surface(
                    shape = pillShape,
                    color = visPillBg
                ) {
                    Text(
                        text = visLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = visPillText,
                        fontSize = (11 * scale).sp,
                        modifier = Modifier.padding(horizontal = (12 * scale).dp, vertical = (4 * scale).dp)
                    )
                }
            }
        }
    }
}

/**
 * 降水詳細パネル (PrecipitationCard):
 * 外枠: MaterialShapes.Circle (正円)
 * 背景: ダークトーンに、下半分〜中央を満たす波打つ水面（Waveアニメーション）
 * 上部: MaterialShapes.Pill による半透明ピルバッジ（傘アイコン + "降水情報"）
 * 中央: 波の領域に大きく配置された降水量数値（例: 1.3 mm）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrecipitationCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true
) {
    val strings = LocalAppStrings.current
    val circleShape = MaterialShapes.Circle.toShape()
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)

    // 水面と文字のカラーパレット（水の中をしっかり塗りつぶし、縁取り線なし）
    val precipWaveColor = if (isColorful) Color(0xFF0091EA) else MaterialTheme.colorScheme.primary
    val backWaveColor = precipWaveColor.copy(alpha = if (isColorful) 0.30f else 0.22f)
    val mainWaveColor = precipWaveColor.copy(alpha = if (isColorful) 0.52f else 0.38f)
    val waveTextColor = MaterialTheme.colorScheme.onSurface

    // ゆったりと水面が揺らめく波アニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "PrecipWaveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Surface(
        shape = circleShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1. 水面（波・Wave）の描画 (水の中を塗りつぶし、水面の縁取り線は不要)
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height

                // 水位ベースY座標（上から約43%の位置）
                val baseWaterY = height * 0.43f
                val waveAmplitude = (10 * scale).dp.toPx()
                val waveLength = width / 2.7f

                // 奥の波の塗りつぶしパス
                val backWavePath = Path().apply {
                    moveTo(0f, height)
                    val startY = (baseWaterY + 3.dp.toPx()) +
                        sin((0f / waveLength) * 2 * PI.toFloat() + wavePhase + 1.2f) * (waveAmplitude * 0.85f)
                    lineTo(0f, startY)
                    var x = 0f
                    val step = 4f
                    while (x <= width) {
                        val y = (baseWaterY + 3.dp.toPx()) +
                            sin((x / waveLength) * 2 * PI.toFloat() + wavePhase + 1.2f) * (waveAmplitude * 0.85f)
                        lineTo(x, y)
                        x += step
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = backWavePath,
                    color = backWaveColor
                )

                // 手前のメイン波の塗りつぶしパス
                val mainWavePath = Path().apply {
                    moveTo(0f, height)
                    val startY = baseWaterY +
                        sin((0f / waveLength) * 2 * PI.toFloat() + wavePhase) * waveAmplitude
                    lineTo(0f, startY)
                    var x = 0f
                    val step = 4f
                    while (x <= width) {
                        val y = baseWaterY +
                            sin((x / waveLength) * 2 * PI.toFloat() + wavePhase) * waveAmplitude
                        lineTo(x, y)
                        x += step
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = mainWavePath,
                    color = mainWaveColor
                )
            }

            // 2. 上部ヘッダー: 傘アイコン + "降水量"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (14 * scale).dp, start = (12 * scale).dp, end = (12 * scale).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_umbrella_24),
                    contentDescription = strings.precipitation,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((20 * scale).dp)
                )
                Spacer(modifier = Modifier.width((6 * scale).dp))
                Text(
                    text = strings.precipitation,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (15 * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. 中央〜下部: 水面上の降水量数値 & 補足ラベル
            val formattedPrecip = remember(weather.precipitationSumMm) {
                if (weather.precipitationSumMm <= 0.0) {
                    "0.0"
                } else if (weather.precipitationSumMm >= 100.0) {
                    String.format(Locale.US, "%.0f", weather.precipitationSumMm)
                } else {
                    String.format(Locale.US, "%.1f", weather.precipitationSumMm)
                }
            }

            val (numFontSize, unitFontSize) = remember(formattedPrecip.length) {
                when {
                    formattedPrecip.length <= 4 -> 36.sp to 15.sp // 例: "0.0", "12.4"
                    else -> 30.sp to 15.sp                        // 例: "125.0", "1250"
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = (6 * scale).dp, start = (8 * scale).dp, end = (8 * scale).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedPrecip,
                        fontSize = (numFontSize.value * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = waveTextColor,
                        lineHeight = (numFontSize.value * scale).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width((3 * scale).dp))
                    Text(
                        text = "mm",
                        fontSize = (unitFontSize.value * scale).sp,
                        fontWeight = FontWeight.Medium,
                        color = waveTextColor.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = (2 * scale).dp),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height((4 * scale).dp))

                // サブラベル: 24時間合計 / 現在の降雨状況を明記
                val language = LocalAppLanguage.current
                val subPrecipText = remember(weather.precipitationMm, strings.todayTotalPrecip, language) {
                    if (weather.precipitationMm > 0.0) {
                        val mmStr = String.format(Locale.US, "%.1f", weather.precipitationMm)
                        when (language) {
                            AppLanguage.JAPANESE -> "現在 $mmStr mm/h"
                            AppLanguage.ENGLISH -> "Now $mmStr mm/h"
                            AppLanguage.SIMPLIFIED_CHINESE -> "当前 $mmStr mm/h"
                            AppLanguage.TRADITIONAL_CHINESE -> "目前 $mmStr mm/h"
                        }
                    } else {
                        strings.todayTotalPrecip
                    }
                }
                Text(
                    text = subPrecipText,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = waveTextColor.copy(alpha = 0.90f),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 体感・快適度パネル (ComfortCard):
 * 外枠: MaterialShapes.Circle (正円)
 * 中央背景: 温熱感・快適さを表現する柔らかなクローバーシェイプ (Cookie4Sided)
 * 上部: Fluent UI Temperature (ic_fluent_temperature_24) + "体感温度"
 * 中央: 大きな体感温度数値 (例: 23°)
 * 下部: MaterialShapes.Pill による気温比・快適度ピルバッジ (例: 気温比 +1° · 快適)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ComfortCard(
    weather: WeatherData,
    tileSize: androidx.compose.ui.unit.Dp = 180.dp,
    modifier: Modifier = Modifier,
    isFrostedGlassEnabled: Boolean = true,
    isDark: Boolean = false,
    isColorful: Boolean = true,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    val strings = LocalAppStrings.current
    val circleShape = MaterialShapes.Circle.toShape()
    val cloverShape = MaterialShapes.Cookie4Sided.toShape()
    val pillShape = CircleShape
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val cloverColor = if (isColorful) Color(0xFFFF5252) else MaterialTheme.colorScheme.tertiary
    val tempIconColor = if (isColorful) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface

    val scale = (tileSize.value / 180f).coerceIn(0.85f, 1.45f)

    val infiniteTransition = rememberInfiniteTransition(label = "ComfortPulse")
    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ComfortPulseScale"
    )

    Surface(
        shape = circleShape,
        color = cardColor,
        border = cardBorder,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 中央背景: やわらかな温熱クローバーシェイプ (タイル最大化サーモトーンレイヤー)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // 外層 (タイル内に収まる最大サイズのクローバーシェイプ)
                Surface(
                    shape = cloverShape,
                    color = cloverColor.copy(alpha = if (isColorful) 0.22f else 0.15f),
                    modifier = Modifier
                        .fillMaxSize(0.93f)
                        .graphicsLayer {
                            scaleX = pulseScaleState.value
                            scaleY = pulseScaleState.value
                        }
                ) {}
                // 内層
                Surface(
                    shape = cloverShape,
                    color = cloverColor.copy(alpha = if (isColorful) 0.48f else 0.32f),
                    modifier = Modifier.fillMaxSize(0.76f)
                ) {}
            }

            // 中央コンテンツ (縦並び)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = (16 * scale).dp)
            ) {
                // 上段: Fluent UI Temperature + "体感温度"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fluent_temperature_24),
                        contentDescription = null,
                        tint = tempIconColor,
                        modifier = Modifier.size((20 * scale).dp)
                    )
                    Spacer(modifier = Modifier.width((6 * scale).dp))
                    Text(
                        text = strings.feelsLikeCard,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (15 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((4 * scale).dp))

                // 中央: 体感温度数値 (摂氏/華氏設定に連動)
                val convertedFeelsLike = temperatureUnit.convert(weather.feelsLikeC)
                Text(
                    text = "${convertedFeelsLike}°",
                    fontSize = (36 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = (36 * scale).sp
                )

                Spacer(modifier = Modifier.height((6 * scale).dp))

                // 下段: ピルバッジ (気温比、例: 気温比 +1° / +1° vs temp)
                val tempDiff = convertedFeelsLike - temperatureUnit.convert(weather.currentTempC)
                val badgeText = when {
                    tempDiff != 0 -> strings.tempDiff(tempDiff)
                    else -> strings.tempSame
                }

                val themeColorScheme = MaterialTheme.colorScheme
                val (comfortPillBg, comfortPillText) = remember(weather.feelsLikeC, isColorful, themeColorScheme) {
                    if (isColorful) {
                        when {
                            weather.feelsLikeC >= 35 -> Color(0xFFB71C1C) to Color.White // 猛暑 (35°C+): 最高警戒の深紅クリムゾンレッド
                            weather.feelsLikeC >= 29 -> Color(0xFFD32F2F) to Color.White // 暑い (29〜34°C): 情熱的なスカーレットレッド
                            weather.feelsLikeC >= 23 -> Color(0xFFF4511E) to Color.White // 暖かい (23〜28°C): 明るいウォームオレンジレッド
                            weather.feelsLikeC in 16..22 -> Color(0xFFE64A19) to Color.White // 快適 (16〜22°C): 鮮やかで温もりのあるコーラルオレンジ（Clover赤シェイプと最も親和）
                            weather.feelsLikeC in 10..15 -> Color(0xFFC74322) to Color.White // 涼しい (10〜15°C): 深みのあるバーントコーラル
                            weather.feelsLikeC in 5..9 -> Color(0xFFA73E28) to Color.White // 寒い (5〜9°C): 落ち着いた煉瓦レッド
                            else -> Color(0xFF8D3B2B) to Color.White // 極寒 (<=4°C): 重厚なテラコッタレッド
                        }
                    } else {
                        when {
                            weather.feelsLikeC >= 35 -> themeColorScheme.error to themeColorScheme.onError
                            weather.feelsLikeC >= 28 -> themeColorScheme.errorContainer to themeColorScheme.onErrorContainer
                            weather.feelsLikeC >= 23 -> themeColorScheme.secondaryContainer to themeColorScheme.onSecondaryContainer
                            weather.feelsLikeC in 16..22 -> themeColorScheme.tertiary to themeColorScheme.onTertiary // 快適: カードのクローバー(tertiary)と完全調和
                            weather.feelsLikeC in 10..15 -> themeColorScheme.tertiaryContainer to themeColorScheme.onTertiaryContainer
                            weather.feelsLikeC in 5..9 -> themeColorScheme.primaryContainer to themeColorScheme.onPrimaryContainer
                            else -> themeColorScheme.surfaceContainerHighest to themeColorScheme.onSurface
                        }
                    }
                }

                Surface(
                    shape = pillShape,
                    color = comfortPillBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = comfortPillText,
                        fontSize = (11 * scale).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = (12 * scale).dp, vertical = (4 * scale).dp)
                    )
                }
            }
        }
    }
}
