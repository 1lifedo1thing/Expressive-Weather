package com.example.expressiveweather.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.WeatherCondition
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.data.localizedName
import com.example.expressiveweather.theme.ThemePreset

@Composable
fun ExpressiveThemeHeroCard(
    weather: WeatherData?,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    language: AppLanguage = AppLanguage.JAPANESE,
    onToggleAdaptiveColor: (Boolean) -> Unit = {},
    onSelectPresetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDay = weather?.isDay ?: true
    val condition = weather?.condition ?: WeatherCondition.CLEAR
    val cityName = weather?.city?.let { it.localizedName(language) } ?: when (language) {
        AppLanguage.JAPANESE -> "現在地"
        AppLanguage.SIMPLIFIED_CHINESE -> "当前位置"
        AppLanguage.TRADITIONAL_CHINESE -> "目前位置"
        AppLanguage.ENGLISH -> "Current Location"
    }
    val tempText = "${weather?.currentTempC ?: 22}°C"
    val isAdaptive = themePreset == ThemePreset.WEATHER

    val animatedCardBg by animateColorAsState(
        targetValue = colorScheme.surfaceContainer,
        label = "heroCardBg"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (condition) {
                                WeatherCondition.CLEAR -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
                                WeatherCondition.CLOUDY, WeatherCondition.FEW_CLOUDS -> Icons.Rounded.Cloud
                                WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Icons.Rounded.WaterDrop
                                WeatherCondition.THUNDERSTORM -> Icons.Rounded.Thunderstorm
                                WeatherCondition.SNOW -> Icons.Rounded.AcUnit
                                WeatherCondition.FOG -> Icons.Rounded.Dehaze
                            },
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "$cityName • $tempText",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                themePreset == ThemePreset.WEATHER -> when (language) {
                                    AppLanguage.JAPANESE -> "天候ダイナミックテーマ適用中"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "天气动态主题生效中"
                                    AppLanguage.TRADITIONAL_CHINESE -> "天氣動態主題生效中"
                                    AppLanguage.ENGLISH -> "Weather-adaptive dynamic theme active"
                                }
                                themePreset == ThemePreset.SYSTEM -> when (language) {
                                    AppLanguage.JAPANESE -> "システム動的カラー適用中"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "系统动态取色生效中"
                                    AppLanguage.TRADITIONAL_CHINESE -> "系統動態取色生效中"
                                    AppLanguage.ENGLISH -> "System dynamic color active"
                                }
                                else -> when (language) {
                                    AppLanguage.JAPANESE -> "${themePreset.displayName(language)} に固定中"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "已固定为 ${themePreset.displayName(language)}"
                                    AppLanguage.TRADITIONAL_CHINESE -> "已固定為 ${themePreset.displayName(language)}"
                                    AppLanguage.ENGLISH -> "Fixed to ${themePreset.displayName(language)}"
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isAdaptive) colorScheme.primary else colorScheme.outlineVariant,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> if (isDay) "昼" else "夜"
                                AppLanguage.SIMPLIFIED_CHINESE -> if (isDay) "白天" else "夜晚"
                                AppLanguage.TRADITIONAL_CHINESE -> if (isDay) "白天" else "夜晚"
                                AppLanguage.ENGLISH -> if (isDay) "Day" else "Night"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dynamic Color Palette Preview Strip
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "カラーパレット (Material 3 Dynamic Palette)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "调色盘预览 (Material 3 Dynamic Palette)"
                        AppLanguage.TRADITIONAL_CHINESE -> "調色盤預覽 (Material 3 Dynamic Palette)"
                        AppLanguage.ENGLISH -> "Color Palette (Material 3 Dynamic Palette)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaletteSwatch(label = "Primary", color = colorScheme.primary, onColor = colorScheme.onPrimary)
                    PaletteSwatch(label = "Container", color = colorScheme.primaryContainer, onColor = colorScheme.onPrimaryContainer)
                    PaletteSwatch(label = "Secondary", color = colorScheme.secondaryContainer, onColor = colorScheme.onSecondaryContainer)
                    PaletteSwatch(label = "Tertiary", color = colorScheme.tertiaryContainer, onColor = colorScheme.onTertiaryContainer)
                    PaletteSwatch(label = "Surface", color = colorScheme.surfaceContainerHigh, onColor = colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    label: String,
    color: Color,
    onColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = onColor.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
