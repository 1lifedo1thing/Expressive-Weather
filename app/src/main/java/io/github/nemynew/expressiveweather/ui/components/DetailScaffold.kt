package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.getAtmosphereTheme

/**
 * 全詳細画面共通のライブ天候フロスト背景。
 * 現在の空色グラデーション＋天候パーティクル (WeatherAtmosphereCanvas) の上に、
 * テーマカラーに沿ったすりガラス拡散レイヤーを重ねることで、
 * 読みやすさを保ちつつ背後の天気と空の色が自然に透ける質感を実現する。
 */
@Composable
fun DetailAtmosphereBackdrop(
    condition: WeatherCondition,
    isDay: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val themeColorScheme = MaterialTheme.colorScheme
    val themeSurface = themeColorScheme.surface
    val themeContainer = themeColorScheme.surfaceContainer
    val themeContainerHigh = themeColorScheme.surfaceContainerHigh
    val themeTint = themeColorScheme.surfaceTint
    val atmosphere = getAtmosphereTheme(condition, themeColorScheme, isDay = isDay)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to themeSurface,
                    0.45f to themeContainer,
                    1.0f to themeContainerHigh
                )
            )
    ) {
        // 1. 天候の空色グラデーション（テーマカラーの上にブレンドして天候の空気感を維持）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(atmosphere.skyGradient)
                .graphicsLayer { alpha = if (!isDay) 0.56f else 0.66f }
        )

        // 2. Edge-to-Edge 天候動的パーティクル背景（雨・雪は鮮明、晴れ・曇りは柔らかくぼかし）
        val hasCrispParticles = condition.isPrecipitating
        WeatherAtmosphereCanvas(
            condition = condition,
            atmosphere = atmosphere,
            modifier = Modifier
                .fillMaxSize()
                .then(if (hasCrispParticles) Modifier else Modifier.blur(26.dp))
        )

        // 3. テーマカラーに沿ったフロストすりガラス拡散レイヤー（白浮きを抑えて空の気配を透過）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to themeSurface.copy(alpha = if (isDark) 0.48f else 0.32f),
                        0.5f to themeContainer.copy(alpha = if (isDark) 0.54f else 0.40f),
                        1.0f to themeContainerHigh.copy(alpha = if (isDark) 0.64f else 0.50f)
                    )
                )
        )

        // 4. テーマカラーのフロストハイライト光彩層
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeTint.copy(alpha = if (isDark) 0.14f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = 1400f
                    )
                )
        )
    }
}

/**
 * 風詳細画面 (WindDetailScreen) と完全に共通の設計思想を持つプレミアム詳細画面 Scaffold:
 *
 * 1. statusBarsPadding に完全対応した固定フロストトップバー
 * 2. 弾性アニメーション (m3eBouncyClick) つきの丸い戻るボタン
 * 3. BlendMode.DstIn による「上部の滑らかに消えるやつ」(スクロール見切れグラデーションマスク)
 * 4. 天候動的パーティクル背景 (WeatherAtmosphereCanvas) ＋ テーマカラーフロストすりガラス拡散レイヤー
 * 5. navigationBarsPadding に完全対応したフッター余白
 */
@Composable
fun DetailScaffold(
    title: String,
    city: City,
    weather: WeatherData?,
    language: AppLanguage,
    metricType: DetailMetricType? = null,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDay = weather?.isDay ?: true
    val isDark = ThemeMode.resolveIsDark(themeMode, isDay, isSystemDark)
    val condition = weather?.condition
    val strings = language.strings

    if (weather == null) {
        ExpressiveWeatherTheme(
            darkTheme = isDark,
            weatherCondition = condition,
            weatherAdaptiveColor = weatherAdaptiveThemeEnabled,
            themePreset = themePreset,
            language = language
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(44.dp)
                            .m3eBouncyClick(onClick = onBack)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }
        return
    }

    ExpressiveWeatherTheme(
        darkTheme = isDark,
        weatherCondition = condition,
        weatherAdaptiveColor = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        language = language
    ) {
        CompositionLocalProvider(
            LocalAppLanguage provides language,
            LocalAppStrings provides strings
        ) {
            val themeColorScheme = MaterialTheme.colorScheme
            val themeSurface = themeColorScheme.surface

            Box(modifier = modifier.fillMaxSize()) {
                // 1. ライブ天候フロスト背景（空色グラデーション + 動的パーティクル + すりガラス拡散）
                DetailAtmosphereBackdrop(
                    condition = weather.condition,
                    isDay = isDay,
                    isDark = isDark
                )

                // 2. メインコンテンツ（固定トップバー + スクロール可能な詳細データ）
                val scrollState = rememberScrollState()
                val density = LocalDensity.current
                val fadeHeightDp = 52.dp
                val fadeHeightPx = with(density) { fadeHeightDp.toPx() }
                val fadeThresholdPx = with(density) { 36.dp.toPx() }
                val scrollProgress = (scrollState.value.toFloat() / fadeThresholdPx).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- トップバー (statusBarsPadding対応 & テーマカラーフロストグラデーション) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to themeSurface.copy(alpha = if (isDark) 0.68f else 0.56f),
                                    0.65f to themeSurface.copy(alpha = if (isDark) 0.42f else 0.30f),
                                    1.0f to themeSurface.copy(alpha = 0f)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 戻るボタン
                            Surface(
                                shape = CircleShape,
                                color = getDetailCardColor(
                                    isDark = isDark,
                                    colorScheme = themeColorScheme,
                                    alpha = if (isDark) 0.78f else 0.80f
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .m3eBouncyClick(scaleDown = 0.88f, overshootScale = 1.12f) {
                                        onBack()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = strings.close,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // タイトル & 都市名
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = city.localizedName(language),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- スクロール可能コンテンツ（上部見切れグラデーションマスク対応） ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = if (scrollProgress > 0.01f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                            }
                            .drawWithContent {
                                drawContent()
                                // 上部の見切れ部分をスクロール時のみグラデーションでフェードアウト
                                if (scrollProgress > 0.01f) {
                                    val topAlpha = (1f - scrollProgress).coerceIn(0f, 1f)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = topAlpha),
                                                Color.Black
                                            ),
                                            startY = 0f,
                                            endY = fadeHeightPx
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        content()
                        if (metricType != null) {
                            DetailDataSourceCard(
                                metricType = metricType,
                                weather = weather,
                                city = city,
                                language = language
                            )
                        }
                        // 最下部マージン（ジェスチャーバーに被らないように）
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
