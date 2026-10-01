package com.example.expressiveweather.ui.components

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
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.data.localizedName
import com.example.expressiveweather.theme.ExpressiveWeatherTheme
import com.example.expressiveweather.theme.GoogleSansFlex
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.theme.getAtmosphereTheme

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
    if (weather == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No weather data available")
        }
        return
    }

    val isDay = weather.isDay
    val isSystemDark = isSystemInDarkTheme()
    val isDark = ThemeMode.resolveIsDark(themeMode, isDay, isSystemDark)
    val condition = weather.condition
    val colorScheme = MaterialTheme.colorScheme
    val atmosphere = getAtmosphereTheme(condition, colorScheme, isDay = isDay)
    val strings = language.strings

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
            val themeContainer = themeColorScheme.surfaceContainer
            val themeContainerHigh = themeColorScheme.surfaceContainerHigh
            val themeTint = themeColorScheme.surfaceTint

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
                // 天候の空色グラデーション（テーマカラーの上にブレンドして天候の空気感を維持）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(atmosphere.skyGradient)
                        .graphicsLayer { alpha = if (!isDay) 0.35f else 0.45f }
                )

                // 1. Edge-to-Edge 天候動的パーティクル背景
                val hasCrispParticles = condition.isPrecipitating
                WeatherAtmosphereCanvas(
                    condition = condition,
                    atmosphere = atmosphere,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (hasCrispParticles) Modifier else Modifier.blur(26.dp))
                )

                // 2. テーマカラーに沿ったフロストすりガラス拡散レイヤー
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to themeSurface.copy(alpha = if (isDark) 0.65f else 0.55f),
                                0.5f to themeContainer.copy(alpha = if (isDark) 0.70f else 0.60f),
                                1.0f to themeContainerHigh.copy(alpha = if (isDark) 0.80f else 0.72f)
                            )
                        )
                )

                // 3. テーマカラーのフロストハイライト光彩層（すりガラスの質感とテーマカラーの美しい反射）
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

                // 4. メインコンテンツ（固定トップバー + スクロール可能な詳細データ）
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
                                    0.0f to themeSurface.copy(alpha = if (isDark) 0.88f else 0.80f),
                                    0.65f to themeSurface.copy(alpha = if (isDark) 0.68f else 0.58f),
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
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
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
