package com.example.expressiveweather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset

@Composable
fun ExpressiveThemeColorCard(
    currentPreset: ThemePreset,
    themeMode: ThemeMode,
    onSelectPreset: (ThemePreset) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    language: AppLanguage = AppLanguage.JAPANESE,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "テーマカラー"
                        AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "主题配色"
                        AppLanguage.ENGLISH -> "Theme Color"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Icon(Icons.Rounded.Palette, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isWeather = currentPreset == ThemePreset.WEATHER
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isWeather) Modifier.border(2.5.dp, colorScheme.primary, RoundedCornerShape(12.dp))
                            else Modifier.border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        )
                        .clickable(role = Role.RadioButton) { onSelectPreset(ThemePreset.WEATHER) }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
                        Row(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEDEDED)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF9E9E9E)))
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF262626)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF5E5E5E)))
                        }
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(30.dp).background(colorScheme.outlineVariant.copy(alpha = 0.4f)))

                val presets = ThemePreset.entries.filter { it != ThemePreset.WEATHER && it != ThemePreset.SYSTEM }
                presets.forEach { preset ->
                    val isSel = currentPreset == preset
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSel) Modifier.border(2.5.dp, colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable(role = Role.RadioButton) { onSelectPreset(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(preset.previewPrimary))
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 1.dp)

            // ダークモード設定セクション (天候追従 / デバイス設定追従 / 常にライト / 常にダーク)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "ダークモード設定"
                            AppLanguage.SIMPLIFIED_CHINESE -> "深色模式设置"
                            AppLanguage.TRADITIONAL_CHINESE -> "深色模式設定"
                            AppLanguage.ENGLISH -> "Dark Theme Mode"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primaryContainer
                    ) {
                        Text(
                            text = themeMode.displayName(language),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = themeMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.RadioButton) { onSelectThemeMode(mode) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHigh,
                            border = if (!isSelected) BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = mode.displayName(language),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = themeMode.description(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
