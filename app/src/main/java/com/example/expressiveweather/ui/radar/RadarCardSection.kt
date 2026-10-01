package com.example.expressiveweather.ui.radar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.R
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.RadarColorScheme
import com.example.expressiveweather.data.RadarDataSource
import com.example.expressiveweather.data.RadarRepository
import com.example.expressiveweather.data.RadarSourceMode
import com.example.expressiveweather.data.RainViewerRepository
import com.example.expressiveweather.data.RainViewerResponse
import com.example.expressiveweather.data.RadarProviderRegistry
import com.example.expressiveweather.data.WeatherCondition
import com.example.expressiveweather.data.isLocationOrCountryInJapan
import com.example.expressiveweather.data.isLocationOrCountryInUnitedStates
import com.example.expressiveweather.theme.LocalDarkTheme
import com.example.expressiveweather.theme.WeatherAtmosphereTheme
import com.example.expressiveweather.ui.components.SnowTileShape
import com.example.expressiveweather.ui.components.frostedGlassEffect
import com.example.expressiveweather.ui.components.getFrostedCardBorder
import com.example.expressiveweather.ui.components.getFrostedCardColor
import com.example.expressiveweather.ui.components.snowTileAccumulation
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RadarCardSection(
    latitude: Double,
    longitude: Double,
    cityName: String,
    atmosphere: WeatherAtmosphereTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    country: String? = null,
    condition: WeatherCondition? = null,
    isFrostedGlassEnabled: Boolean = true,
    hazeState: HazeState? = null,
    isColorful: Boolean = true,
    showSourceBadge: Boolean = false,
    showZoomBadge: Boolean = false
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val isAppDark = LocalDarkTheme.current
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, atmosphere.isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, atmosphere.isDark)
    val isSnowing = condition == WeatherCondition.SNOW

    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(context) { RadarRepository.getInstance(context) }
    val sourceMode by repo.sourceModeFlow.collectAsState()

    var radarData by remember { mutableStateOf<RainViewerResponse?>(null) }
    var dataSource by remember(sourceMode, latitude, longitude, country) {
        mutableStateOf(RadarProviderRegistry.resolveProvider(sourceMode, latitude, longitude, country))
    }
    var isLoading by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentFrameIndex by remember { mutableIntStateOf(-1) }
    var currentTimestamp by remember { mutableLongStateOf(0L) }

    // 過去＋未来ナウキャストの全フレーム
    val allFrames = remember(radarData) {
        if (radarData != null) {
            radarData!!.radar.past + radarData!!.radar.nowcast
        } else {
            emptyList()
        }
    }

    // 統合レーダーデータフェッチ（地名・国名・設定変更時に即座に厳密判定）
    LaunchedEffect(latitude, longitude, country, sourceMode) {
        isLoading = true
        val (result, source) = repo.getRadarData(latitude, longitude, country)
        dataSource = source
        if (result.isSuccess) {
            val data = result.getOrNull()
            radarData = data
            val frames = if (data != null) data.radar.past + data.radar.nowcast else emptyList()
            if (frames.isNotEmpty()) {
                // 初期フレームは「現在（past の末尾、または全体の最新）」
                val initialIndex = if (data?.radar?.past?.isNotEmpty() == true) {
                    data.radar.past.size - 1
                } else {
                    frames.size - 1
                }
                currentFrameIndex = initialIndex
                currentTimestamp = frames[initialIndex].time
            }
        }
        isLoading = false
    }

    val radarTitle = when (language) {
        AppLanguage.JAPANESE -> "雨雲レーダー"
        AppLanguage.ENGLISH -> "Precipitation Radar"
        AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
        AppLanguage.TRADITIONAL_CHINESE -> "降水雷達"
    }

    val timeFormatter = remember(language) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }
    val timeLabel = remember(currentTimestamp, currentFrameIndex, allFrames, language, radarData) {
        if (currentTimestamp > 0 && currentFrameIndex in allFrames.indices) {
            val frame = allFrames[currentFrameIndex]
            val baseTimeStr = timeFormatter.format(Date(currentTimestamp * 1000L))
            val pastList = radarData?.radar?.past ?: emptyList()
            val currentObsTime = if (pastList.isNotEmpty()) pastList.last().time else currentTimestamp
            val diffSec = currentTimestamp - currentObsTime
            val diffMin = Math.round(diffSec / 60.0).toInt()

            val relativeStr = when {
                diffMin in -4..4 -> when (language) {
                    AppLanguage.JAPANESE -> "現在"
                    AppLanguage.ENGLISH -> "Now"
                    AppLanguage.SIMPLIFIED_CHINESE -> "现在"
                    AppLanguage.TRADITIONAL_CHINESE -> "現在"
                }
                diffMin < 0 -> {
                    val absMin = -diffMin
                    if (absMin >= 60) {
                        val hours = absMin / 60
                        val mins = absMin % 60
                        when (language) {
                            AppLanguage.JAPANESE -> if (mins == 0) "${hours}時間前" else "${hours}時間${mins}分前"
                            AppLanguage.ENGLISH -> if (mins == 0) "${hours}h ago" else "${hours}h ${mins}m ago"
                            AppLanguage.SIMPLIFIED_CHINESE -> if (mins == 0) "${hours}小时前" else "${hours}小时${mins}分前"
                            AppLanguage.TRADITIONAL_CHINESE -> if (mins == 0) "${hours}小時前" else "${hours}小時${mins}分前"
                        }
                    } else {
                        when (language) {
                            AppLanguage.JAPANESE -> "${absMin}分前"
                            AppLanguage.ENGLISH -> "${absMin}m ago"
                            AppLanguage.SIMPLIFIED_CHINESE -> "${absMin}分钟前"
                            AppLanguage.TRADITIONAL_CHINESE -> "${absMin}分鐘前"
                        }
                    }
                }
                else -> {
                    if (diffMin >= 60) {
                        val hours = diffMin / 60
                        val mins = diffMin % 60
                        when (language) {
                            AppLanguage.JAPANESE -> if (mins == 0) "+${hours}時間後" else "+${hours}時間${mins}分後"
                            AppLanguage.ENGLISH -> if (mins == 0) "+${hours}h" else "+${hours}h ${mins}m"
                            AppLanguage.SIMPLIFIED_CHINESE -> if (mins == 0) "+${hours}小时" else "+${hours}小时${mins}分"
                            AppLanguage.TRADITIONAL_CHINESE -> if (mins == 0) "+${hours}小時" else "+${hours}小時${mins}分"
                        }
                    } else {
                        when (language) {
                            AppLanguage.JAPANESE -> "+${diffMin}分予測"
                            AppLanguage.ENGLISH -> "+${diffMin}m forecast"
                            AppLanguage.SIMPLIFIED_CHINESE -> "+${diffMin}分预测"
                            AppLanguage.TRADITIONAL_CHINESE -> "+${diffMin}分預測"
                        }
                    }
                }
            }
            "$baseTimeStr ($relativeStr)"
        } else {
            ""
        }
    }

    val cardShape = RoundedCornerShape(28.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .frostedGlassEffect(hazeState, isFrostedGlassEnabled, shape = cardShape)
            .snowTileAccumulation(
                isSnowing = isSnowing,
                isDark = atmosphere.isDark,
                shape = SnowTileShape.ROUNDED_RECT,
                cornerRadiusDp = 28.dp,
                seed = 105
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. ヘッダー: タイトル + 時刻バッジ (アイコン類を排してすっきり整理)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = radarTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    val sourceLabel = when (dataSource) {
                        RadarDataSource.JMA_NOWCAST -> when (language) {
                            AppLanguage.JAPANESE -> "気象庁 高解像度降水ナウキャスト"
                            AppLanguage.ENGLISH -> "JMA High-Resolution Nowcast"
                            AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 高分辨率降水临近预报"
                            AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 高解析度降水臨近預報"
                        }
                        RadarDataSource.US_NEXRAD_HRRR -> when (language) {
                            AppLanguage.JAPANESE -> "全米 NEXRAD / HRRR (実況・外挿予測・予報)"
                            AppLanguage.ENGLISH -> "US NEXRAD / HRRR (Obs, Nowcast, Forecast)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR（实况/外推/预报）"
                            AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR（實況/外推/預報）"
                        }
                        RadarDataSource.RAIN_VIEWER -> when (language) {
                            AppLanguage.JAPANESE -> "世界レーダー (RainViewer)"
                            AppLanguage.ENGLISH -> "Global Radar (RainViewer)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 (RainViewer)"
                            AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 (RainViewer)"
                        }
                    }
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                if (timeLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 地図プレビュー (ボックス)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isAppDark) Color(0xFF121822) else MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                val frames = allFrames
                val host = radarData?.host ?: "https://tilecache.rainviewer.com"

                RadarMapView(
                    latitude = latitude,
                    longitude = longitude,
                    radarFrames = frames,
                    host = host,
                    zoom = 10,
                    isInteractive = false,
                    isPlaying = isPlaying,
                    currentFrameIndex = currentFrameIndex,
                    colorScheme = RadarColorScheme.UNIVERSAL_BLUE,
                    isDark = isAppDark,
                    showZoomBadge = showZoomBadge,
                    onFrameChanged = { index, timestamp ->
                        currentFrameIndex = index
                        currentTimestamp = timestamp
                    },
                    onMapClick = onClick,
                    modifier = Modifier.fillMaxSize()
                )

                // タップで確実に全画面詳細へ遷移するためのオーバーレイ
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onClick() }
                )

                // ローディング表示
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isAppDark) Color(0xFF121822) else MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // 右下のデータソースクレジット (デバッグ設定・デフォルトはオフ)
                if (showSourceBadge) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 8.dp),
                        color = if (isAppDark) Color(0xCC121822) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = when (dataSource) {
                                RadarDataSource.JMA_NOWCAST -> when (language) {
                                    AppLanguage.JAPANESE -> "気象庁 ナウキャスト"
                                    AppLanguage.ENGLISH -> "JMA Nowcast"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "气象厅 临近预报"
                                    AppLanguage.TRADITIONAL_CHINESE -> "氣象廳 臨近預報"
                                }
                                RadarDataSource.US_NEXRAD_HRRR -> when (language) {
                                    AppLanguage.JAPANESE -> "NEXRAD / HRRR (実況・外挿・予報)"
                                    AppLanguage.ENGLISH -> "NEXRAD / HRRR (Obs/Forecast)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "NEXRAD / HRRR (实况/预报)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "NEXRAD / HRRR (實況/預報)"
                                }
                                RadarDataSource.RAIN_VIEWER -> "RainViewer"
                            },
                            color = if (isAppDark) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. フッター操作: ミニ再生トグル & ガイド
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "一時停止"
                                        AppLanguage.ENGLISH -> "Pause"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "暂停"
                                        AppLanguage.TRADITIONAL_CHINESE -> "暫停"
                                    }
                                } else {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "再生"
                                        AppLanguage.ENGLISH -> "Play"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "播放"
                                        AppLanguage.TRADITIONAL_CHINESE -> "播放"
                                    }
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // フレーム進行インジケーター（実況=灰/青、外挿予測=エメラルド、HRRR予報=シアン）
                    val framesCount = allFrames.size
                    if (framesCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayDots = minOf(framesCount, 16)
                            repeat(displayDots) { idx ->
                                val frameIdx = (idx * framesCount) / displayDots
                                val targetF = allFrames.getOrNull(frameIdx)
                                val isForecast = targetF?.isForecast == true
                                val isExtrap = targetF?.resolutionInfo?.contains("外挿") == true
                                val isActive = currentFrameIndex == frameIdx ||
                                    (currentFrameIndex >= frameIdx && idx == displayDots - 1)
                                Box(
                                    modifier = Modifier
                                        .size(if (isActive) 5.dp else 3.5.dp)
                                        .background(
                                            color = when {
                                                isActive -> MaterialTheme.colorScheme.primary
                                                isExtrap -> Color(0xFF00E676).copy(alpha = 0.75f)
                                                isForecast -> Color(0xFF00E5FF).copy(alpha = 0.5f)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            },
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
