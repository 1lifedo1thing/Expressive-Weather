package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedLabel
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.GoogleSansFlexHeroTemp
import io.github.nemynew.expressiveweather.theme.WeatherAtmosphereTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

/**
 * スクロール進捗（0.0f: 展開 〜 1.0f: 折りたたみ）に1ミリ秒の遅延もなく1対1で完全同期し、
 * メイン画面の巨大な気温・天気・地名表示から画面上部コンパクトヘッダーへと
 * シームレスに位置・サイズ・レイアウトを変形（モーフィング）させるコンポーネント。
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MorphingWeatherHeader(
    weather: WeatherData,
    temperatureUnit: TemperatureUnit,
    atmosphere: WeatherAtmosphereTheme,
    isDark: Boolean,
    progress: Float, // 0f = Expanded (巨大Hero), 1f = Collapsed (Compactヘッダー)
    hazeState: HazeState? = null,
    onCityClick: () -> Unit,
    onDetailClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val density = LocalDensity.current

    val cityName = weather.city.localizedName(language)
    val conditionName = weather.condition.getLocalizedLabel(language)
    val currentTemp = temperatureUnit.convert(weather.currentTempC)
    val feelsLikeConverted = temperatureUnit.convert(weather.feelsLikeC)
    val maxTempConverted = temperatureUnit.convert(weather.maxTempC)
    val minTempConverted = temperatureUnit.convert(weather.minTempC)

    val easedProgress = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))

    // ステータスバーの上部パディング
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarTopPx = with(density) { statusBarTopDp.toPx() }

    // ヘッダーの全体の高さ（展開時: 約314dp + statusBar, 折りたたみ時: 約78dp + statusBar）
    val expandedContentHeightDp = 314.dp
    val collapsedContentHeightDp = 78.dp
    val currentContentHeightDp = expandedContentHeightDp - (expandedContentHeightDp - collapsedContentHeightDp) * easedProgress
    val totalHeightDp = statusBarTopDp + currentContentHeightDp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeightDp)
    ) {
        // --- 1. 背景レイヤー（各天候の背景色に100%馴染む動的すりガラス＆グラデーションぼかし） ---
        val frostProgress = ((progress - 0.05f) / 0.65f).coerceIn(0f, 1f)
        // ヘッダーの高さに背景を追従させ、下端に64dpのぼかしフェードを設ける。
        val headerBlurHeightDp = totalHeightDp + 64.dp

        if (frostProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerBlurHeightDp)
                    .graphicsLayer {
                        alpha = frostProgress
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White,
                                0.72f to Color.White,
                                0.88f to Color.White.copy(alpha = 0.48f),
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                // 1. Haze Backdrop Blur すりガラスぼかし（文字より下で通過カードを天候カラーで光学ぼかし）
                if (hazeState != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeEffect(
                                state = hazeState,
                                style = HazeMaterials.ultraThin().copy(
                                    backgroundColor = atmosphere.topSkyColor,
                                    blurRadius = 22.dp,
                                    tints = listOf(
                                        dev.chrisbanes.haze.HazeTint(
                                            color = atmosphere.topSkyColor.copy(alpha = if (isDark) 0.65f else 0.45f)
                                        )
                                    )
                                )
                            )
                    )
                }

                // 2. 天候背景と100%一体化する空色グラデーションレイヤー
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to atmosphere.topSkyColor,
                                0.52f to atmosphere.topSkyColor,
                                0.65f to atmosphere.topSkyColor.copy(alpha = 0.98f),
                                0.78f to atmosphere.topSkyColor.copy(alpha = 0.75f),
                                0.88f to atmosphere.midSkyColor.copy(alpha = 0.35f),
                                0.95f to atmosphere.midSkyColor.copy(alpha = 0.12f),
                                1.00f to Color.Transparent
                            )
                        )
                )
            }
        }

        // テキストシャドウ（背後に明るい雲が流れても文字の輪郭をくっきり際立たせる）
        val textShadow = Shadow(
            color = if (isDark) Color.Black.copy(alpha = 0.65f) else Color(0xFF0F2644).copy(alpha = 0.48f),
            offset = Offset(0f, 2.5f),
            blurRadius = 10f
        )

        // --- 2. 幾何学的・無段階モーフィングレイアウト ---
        Layout(
            content = {
                // [Index 0] 地名テキスト (City Name) - 文字そのものの幅で完全中央揃え
                Text(
                    text = cityName,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCityClick
                    )
                )

                // [Index 1] 矢印アイコン (Arrow Icon) - 地名テキストの左側に自然に寄り添う
                Icon(
                    imageVector = Icons.Rounded.NearMe,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCityClick
                        )
                )

                // [Index 2] 天気名 (Condition Name)
                Text(
                    text = conditionName,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.4.sp,
                    style = TextStyle(shadow = textShadow)
                )

                // [Index 3] 巨大展開気温 (Giant Hero Temp: 128sp)
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "°",
                        fontFamily = GoogleSansFlexHeroTemp,
                        fontSize = 58.sp,
                        lineHeight = 58.sp,
                        color = Color.Transparent,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .alpha(0f)
                    )
                    Text(
                        text = "$currentTemp",
                        fontFamily = GoogleSansFlexHeroTemp,
                        fontSize = 128.sp,
                        lineHeight = 128.sp,
                        color = Color.White,
                        letterSpacing = (-2).sp,
                        style = TextStyle(shadow = textShadow)
                    )
                    Text(
                        text = "°",
                        fontFamily = GoogleSansFlexHeroTemp,
                        fontSize = 58.sp,
                        lineHeight = 58.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.padding(top = 8.dp),
                        style = TextStyle(shadow = textShadow)
                    )
                }

                // [Index 4] 折りたたみコンパクト気温 (Compact Temp: 42sp)
                Text(
                    text = "${currentTemp}°",
                    fontFamily = GoogleSansFlexHeroTemp,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-1).sp,
                    style = TextStyle(shadow = textShadow)
                )

                // [Index 5] 体感温度 ＆ 最高・最低気温 (Sub Temp Info: 25sp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.feelsLike(feelsLikeConverted),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 25.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = 0.3.sp,
                        style = TextStyle(shadow = textShadow)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = strings.high(maxTempConverted),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 25.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            letterSpacing = 0.2.sp,
                            style = TextStyle(shadow = textShadow)
                        )
                        Text(
                            text = strings.low(minTempConverted),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 25.sp,
                            color = Color.White.copy(alpha = 0.90f),
                            letterSpacing = 0.2.sp,
                            style = TextStyle(shadow = textShadow)
                        )
                    }
                }
            },
            modifier = Modifier.matchParentSize()
        ) { measurables, constraints ->
            val placeableCity = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val placeableIcon = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val placeableCondition = measurables[2].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val placeableGiantTemp = measurables[3].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val placeableCompactTemp = measurables[4].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val placeableSubInfo = measurables[5].measure(constraints.copy(minWidth = 0, minHeight = 0))

            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val leftMarginPx = 24.dp.roundToPx()
            val rightMarginPx = 24.dp.roundToPx()
            val iconSpacingPx = 6.dp.roundToPx()

            layout(width, height) {
                // 1. 地名 (City Name) の配置とスケール
                // 展開時: 文字そのものの幅で完全中央揃え（矢印アイコンに影響されない）
                // 折りたたみ時: 左端(24dp), Y = statusBarTop + 6dp, スケール 15/28 ≒ 0.536f
                val cityExpandedX = (width - placeableCity.width) / 2
                val cityCollapsedX = leftMarginPx
                val cityX = (cityExpandedX + (cityCollapsedX - cityExpandedX) * easedProgress).roundToInt()

                val cityExpandedY = (statusBarTopPx + 8.dp.toPx()).roundToInt()
                val cityCollapsedY = (statusBarTopPx + 6.dp.toPx()).roundToInt()
                val cityY = (cityExpandedY + (cityCollapsedY - cityExpandedY) * easedProgress).roundToInt()

                val cityScale = 1.0f - (1.0f - (15f / 28f)) * easedProgress

                placeableCity.placeRelativeWithLayer(cityX, cityY) {
                    scaleX = cityScale
                    scaleY = cityScale
                    transformOrigin = TransformOrigin(0f, 0f)
                }

                // 2. 矢印アイコン (Arrow Icon) の配置
                // 展開時: 地名文字の左側に寄り添う
                // 折りたたみ時: スクロールに伴い自然にフェードアウト
                val iconAlpha = (1f - progress * 2.8f).coerceIn(0f, 1f)
                if (iconAlpha > 0f) {
                    val iconExpandedX = cityExpandedX - placeableIcon.width - iconSpacingPx
                    val iconCollapsedX = leftMarginPx - placeableIcon.width - iconSpacingPx
                    val iconX = (iconExpandedX + (iconCollapsedX - iconExpandedX) * easedProgress).roundToInt()

                    val iconExpandedY = cityExpandedY + (placeableCity.height - placeableIcon.height) / 2
                    val iconCollapsedY = cityCollapsedY + (placeableCity.height - placeableIcon.height) / 2
                    val iconY = (iconExpandedY + (iconCollapsedY - iconExpandedY) * easedProgress).roundToInt()

                    placeableIcon.placeRelativeWithLayer(iconX, iconY) {
                        scaleX = cityScale
                        scaleY = cityScale
                        alpha = iconAlpha
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }

                // 3. 天気名 (Condition Name) の配置とスケール
                // 展開時: 水平中央, Y = statusBarTop + 46dp, 24sp
                // 折りたたみ時: 左端(24dp), Y = statusBarTop + 28.5dp, 26.5sp相当
                val condExpandedX = (width - placeableCondition.width) / 2
                val condCollapsedX = leftMarginPx
                val condX = (condExpandedX + (condCollapsedX - condExpandedX) * easedProgress).roundToInt()

                val condExpandedY = (statusBarTopPx + 46.dp.toPx()).roundToInt()
                val condCollapsedY = (statusBarTopPx + 28.5.dp.toPx()).roundToInt()
                val condY = (condExpandedY + (condCollapsedY - condExpandedY) * easedProgress).roundToInt()

                val condScale = 1.0f + ((26.5f / 24f) - 1.0f) * easedProgress

                placeableCondition.placeRelativeWithLayer(condX, condY) {
                    scaleX = condScale
                    scaleY = condScale
                    transformOrigin = TransformOrigin(0f, 0f)
                }

                // 4. 巨大展開気温 (Giant Hero Temp: 128sp) の配置とモーフィング
                val tempExpandedX = (width - placeableGiantTemp.width) / 2
                val tempTargetCollapsedScale = 42f / 128f
                val scaledTargetRightX = width - (placeableGiantTemp.width * tempTargetCollapsedScale).roundToInt() - rightMarginPx
                val tempX = (tempExpandedX + (scaledTargetRightX - tempExpandedX) * easedProgress).roundToInt()

                val tempExpandedY = (statusBarTopPx + 74.dp.toPx()).roundToInt()
                val tempCollapsedY = (statusBarTopPx + 6.dp.toPx()).roundToInt()
                val tempY = (tempExpandedY + (tempCollapsedY - tempExpandedY) * easedProgress).roundToInt()

                val giantTempScale = 1.0f - (1.0f - tempTargetCollapsedScale) * easedProgress
                val giantTempAlpha = (1.0f - (easedProgress - 0.70f) / 0.25f).coerceIn(0f, 1f)

                if (giantTempAlpha > 0f) {
                    placeableGiantTemp.placeRelativeWithLayer(tempX, tempY) {
                        scaleX = giantTempScale
                        scaleY = giantTempScale
                        alpha = giantTempAlpha
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }

                // 5. コンパクト気温 (Compact Temp: 42sp)
                val compactTempAlpha = ((easedProgress - 0.70f) / 0.25f).coerceIn(0f, 1f)
                if (compactTempAlpha > 0f) {
                    val compactX = width - placeableCompactTemp.width - rightMarginPx
                    val compactY = (statusBarTopPx + 6.dp.toPx()).roundToInt()
                    placeableCompactTemp.placeRelativeWithLayer(compactX, compactY) {
                        alpha = compactTempAlpha
                    }
                }

                // 6. 体感温度 ＆ 最高・最低気温 (Sub Temp Info: 25sp)
                // 巨大気温としっかり離して配置（Y = statusBarTop + 224dp）
                // スクロール初期 (0%〜25%) で自然にフェードアウト
                val subInfoAlpha = (1.0f - (progress / 0.25f)).coerceIn(0f, 1f)
                if (subInfoAlpha > 0f) {
                    val subInfoExpandedY = (statusBarTopPx + 224.dp.toPx()).roundToInt()
                    val subInfoX = (width - placeableSubInfo.width) / 2
                    val subInfoY = (subInfoExpandedY - (progress * 26.dp.toPx())).roundToInt()
                    placeableSubInfo.placeRelativeWithLayer(subInfoX, subInfoY) {
                        alpha = subInfoAlpha
                    }
                }
            }
        }
    }
}
