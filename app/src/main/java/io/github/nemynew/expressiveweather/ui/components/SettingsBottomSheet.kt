package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive (M3E) 仕様のポップアップ設定画面（Edge-to-Edge 完全対応 ModalBottomSheet）。
 * タイトル「設定」(28sp) と 4 項目の連結リスト（高さ 72dp、角丸 28dp/8dp）を配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    onSelectCurrentLocation: (() -> Unit)? = null,
    onNavigateToSettings: (String) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    currentUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    currentLanguage: AppLanguage = AppLanguage.JAPANESE,
    weatherAdaptiveThemeEnabled: Boolean = true,
    onToggleUnit: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onToggleWeatherAdaptiveTheme: () -> Unit = {},
    backgroundPreviewContent: @Composable () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("expressive_weather_prefs", android.content.Context.MODE_PRIVATE)
    }
    var isDebugUnlocked by remember {
        mutableStateOf(prefs.getBoolean("debug_settings_unlocked", false))
    }
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "debug_settings_unlocked") {
                isDebugUnlocked = prefs.getBoolean("debug_settings_unlocked", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // 見出し「設定」（28sp、onSurface、行間約1.36倍）
            Text(
                text = if (currentLanguage == AppLanguage.JAPANESE) "設定" else strings.settingsTitle,
                fontSize = 28.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // リスト領域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 上の3つのグループ（一般設定、外観設定、情報ソース）
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val topShape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    )
                    val middleShape = RoundedCornerShape(8.dp)
                    val bottomShape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    )

                    // 項目 1: 「一般設定」（テーマprimaryContainer色、末尾に＞）
                    SettingsPopupListItem(
                        title = if (currentLanguage == AppLanguage.JAPANESE) "一般設定" else strings.generalSettingsTitle,
                        subtitle = when (currentLanguage) {
                            AppLanguage.JAPANESE -> "言語・単位・通知・ハプティクス"
                            AppLanguage.KOREAN -> "언어 · 단위 · 알림 · 햅틱"
                            AppLanguage.SIMPLIFIED_CHINESE -> "语言、单位、通知与触感"
                            AppLanguage.TRADITIONAL_CHINESE -> "語言、單位、通知與觸感"
                            AppLanguage.SPANISH -> "Idioma, unidades, alertas y háptica"
                            AppLanguage.FRENCH -> "Langue, unités, alertes et haptique"
                            AppLanguage.PORTUGUESE -> "Idioma, unidades, alertas e tato"
                            AppLanguage.ENGLISH -> "Language, units, alerts & haptics"
                        },
                        leadingIcon = Icons.Rounded.Tune,
                        iconBackground = colorScheme.primaryContainer,
                        iconTint = colorScheme.onPrimaryContainer,
                        trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        shape = topShape,
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onDismiss()
                                onNavigateToSettings("general")
                            }
                        }
                    )

                    // 項目 2: 「外観設定」（テーマsecondaryContainer色、末尾に＞）
                    SettingsPopupListItem(
                        title = if (currentLanguage == AppLanguage.JAPANESE) "外観設定" else strings.appearanceTitle,
                        subtitle = when (currentLanguage) {
                            AppLanguage.JAPANESE -> "テーマ・ガラス表現・図形"
                            AppLanguage.KOREAN -> "테마 · 유리 효과 · 도형"
                            AppLanguage.SIMPLIFIED_CHINESE -> "主题配色与视觉效果"
                            AppLanguage.TRADITIONAL_CHINESE -> "主題配色與視覺效果"
                            AppLanguage.SPANISH -> "Tema, efecto vidrio y formas"
                            AppLanguage.FRENCH -> "Thème, verre dépoli et formes"
                            AppLanguage.PORTUGUESE -> "Tema, vidro fosco e formas"
                            AppLanguage.ENGLISH -> "Theme, glass & shapes"
                        },
                        leadingIcon = Icons.Rounded.Palette,
                        iconBackground = colorScheme.secondaryContainer,
                        iconTint = colorScheme.onSecondaryContainer,
                        trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        shape = middleShape,
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onDismiss()
                                onNavigateToSettings("appearance")
                            }
                        }
                    )

                    // 項目 3: 「情報ソース」（テーマtertiaryContainer色、末尾に＞）
                    SettingsPopupListItem(
                        title = if (currentLanguage == AppLanguage.JAPANESE) "情報ソース" else strings.dataSourcesTitle,
                        subtitle = when (currentLanguage) {
                            AppLanguage.JAPANESE -> "レーダー・世界の警報・キャッシュ"
                            AppLanguage.KOREAN -> "레이더 · 글로벌 경보 · 캐시"
                            AppLanguage.SIMPLIFIED_CHINESE -> "雷达、全球警报与缓存管理"
                            AppLanguage.TRADITIONAL_CHINESE -> "雷達、全球警報與快取管理"
                            AppLanguage.SPANISH -> "Radar, alertas globales y caché"
                            AppLanguage.FRENCH -> "Radar, alertes mondiales et cache"
                            AppLanguage.PORTUGUESE -> "Radar, alertas globais e cache"
                            AppLanguage.ENGLISH -> "Radar, global alerts & cache"
                        },
                        leadingIcon = Icons.Rounded.Storage,
                        iconBackground = colorScheme.tertiaryContainer,
                        iconTint = colorScheme.onTertiaryContainer,
                        trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        shape = bottomShape,
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onDismiss()
                                onNavigateToSettings("sources")
                            }
                        }
                    )
                }

                // デバッグ設定 (7回連続タップで有効化されている場合のみ表示)
                if (isDebugUnlocked) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 項目 4: 「デバッグ設定」（独立した1項目、角丸28dp、テーマアクセント色、末尾に＞）
                    SettingsPopupListItem(
                        title = if (currentLanguage == AppLanguage.JAPANESE) "デバッグ設定" else strings.debugSettingsTitle,
                        subtitle = null,
                        leadingIcon = Icons.Rounded.BugReport,
                        iconBackground = colorScheme.surfaceContainerHighest,
                        iconTint = colorScheme.primary,
                        trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        shape = RoundedCornerShape(28.dp),
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onDismiss()
                                onNavigateToSettings("debug")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * リスト項目:
 * 高さ 72dp、先頭アイコンは 24dp（テーマに沿った背景とティントの 40dp の円の上）、
 * 主テキストは bodyLarge (onSurface)、サブテキストは bodyMedium (onSurfaceVariant)。
 * 背景は surfaceContainerLow。
 * タップ時は標準 M3 リップルフィードバック（スプリング効果なし）。
 */
@Composable
private fun SettingsPopupListItem(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        shape = shape,
        color = colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 先頭アイコン: 24dp（テーマのロールカラーに基づいた 40dp の円の上）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 主テキスト（bodyLarge） & サブテキスト（bodyMedium、行間サイズ約1.35倍）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface,
                    lineHeight = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 末尾アイコン（chevron_right: ＞）
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
