package io.github.nemynew.expressiveweather.ui.radar

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.DbzColorLevel
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.RadarPlaybackState
import io.github.nemynew.expressiveweather.data.RadarTileCacheManager
import io.github.nemynew.expressiveweather.data.RadarTimelineFrame
import io.github.nemynew.expressiveweather.data.US_NEXRAD_DBZ_LEVELS
import io.github.nemynew.expressiveweather.data.UsRadarFrameType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream

// =========================================================================
// 1. 再生・時間操作コントローラー (下部フロート配置コントロールバー)
// =========================================================================

/**
 * 下部にフロート配置するレーダー再生・時間操作コントローラー
 *
 * 要件:
 * 1. 再生・一時停止のトグルボタン
 * 2. 現在選択中の予報時刻表示 (例: 「今日 14:30 実況」「今日 19:00 予測」)
 * 3. タイムラインスライダー: 現在時刻の位置に境界マーカーを置き、左側を実況ゾーン、右側をHRRR予測ゾーンとして視覚的に区別
 * 4. タイル透過度 (0.6〜0.8) の調整機能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarTimelineController(
    frames: List<RadarTimelineFrame>,
    currentIndex: Int,
    playbackState: RadarPlaybackState,
    nowDividerIndex: Int,
    onPlayPauseToggle: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onSeekToIndex: (Int) -> Unit,
    onSeekToNow: () -> Unit,
    onSeekToStart: () -> Unit,
    onSeekToEnd: () -> Unit,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val currentFrame = frames.getOrNull(currentIndex)
    var showAlphaSlider by remember { mutableStateOf(false) }

    // カードのスタイルと背景
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        Color(0xDD1E1F25)
    } else {
        Color(0xEEFFFFFF)
    }
    val contentColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // -------------------------------------------------------------
            // 上段: 時刻表示・実況/予報バッジ・透過度調整
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左側: 現在選択中の予報時刻表示
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val loadingText = when (language) {
                            AppLanguage.ENGLISH -> "Loading data..."
                            AppLanguage.KOREAN -> "데이터 불러오는 중..."
                            AppLanguage.SIMPLIFIED_CHINESE -> "正在加载数据..."
                            AppLanguage.TRADITIONAL_CHINESE -> "正在載入資料..."
                            AppLanguage.JAPANESE -> "データ読み込み中..."
                            AppLanguage.SPANISH -> "Cargando datos..."
                            AppLanguage.FRENCH -> "Chargement des données..."
                            AppLanguage.PORTUGUESE -> "Carregando dados..."
                            AppLanguage.RUSSIAN -> "Загрузка данных..."
                        }
                        Text(
                            text = currentFrame?.displayLabel ?: loadingText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 実況 / 予報 の視覚的バッジ
                        val isForecast = currentFrame?.isForecast == true
                        val badgeColor = if (isForecast) {
                            Color(0xFF7C4DFF) // 予測: パープル
                        } else {
                            Color(0xFF00B0FF) // 実況: スカイブルー
                        }

                        val badgeText = if (isForecast) {
                            when (language) {
                                AppLanguage.ENGLISH -> "HRRR Forecast"
                                AppLanguage.KOREAN -> "HRRR 예측"
                                AppLanguage.SIMPLIFIED_CHINESE -> "HRRR预测"
                                AppLanguage.TRADITIONAL_CHINESE -> "HRRR預測"
                                AppLanguage.JAPANESE -> "HRRR予測"
                                AppLanguage.SPANISH -> "Previsión HRRR"
                                AppLanguage.FRENCH -> "Prévision HRRR"
                                AppLanguage.PORTUGUESE -> "Previsão HRRR"
                                AppLanguage.RUSSIAN -> "Прогноз HRRR"
                            }
                        } else {
                            when (language) {
                                AppLanguage.ENGLISH -> "MRMS Live"
                                AppLanguage.KOREAN -> "MRMS 실황"
                                AppLanguage.SIMPLIFIED_CHINESE -> "MRMS实况"
                                AppLanguage.TRADITIONAL_CHINESE -> "MRMS實況"
                                AppLanguage.JAPANESE -> "MRMS実況"
                                AppLanguage.SPANISH -> "MRMS en vivo"
                                AppLanguage.FRENCH -> "MRMS en direct"
                                AppLanguage.PORTUGUESE -> "MRMS ao vivo"
                                AppLanguage.RUSSIAN -> "MRMS сейчас"
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeColor.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // 補足説明（更新間隔やモデル名）
                    Text(
                        text = currentFrame?.description ?: "NEXRAD MRMS / HRRR 1km AGL",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = contentColor.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 右側: 透過度調整ボタン (0.6〜0.8)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showAlphaSlider = !showAlphaSlider }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val opacityDesc = when (language) {
                            AppLanguage.ENGLISH -> "Adjust Opacity"
                            AppLanguage.KOREAN -> "불투명도 조절"
                            AppLanguage.SIMPLIFIED_CHINESE -> "调整透明度"
                            AppLanguage.TRADITIONAL_CHINESE -> "調整透明度"
                            AppLanguage.JAPANESE -> "不透明度調整"
                            AppLanguage.SPANISH -> "Ajustar opacidad"
                            AppLanguage.FRENCH -> "Régler l'opacité"
                            AppLanguage.PORTUGUESE -> "Ajustar opacidade"
                            AppLanguage.RUSSIAN -> "Настроить прозрачность"
                        }
                        Icon(
                            imageVector = Icons.Rounded.Opacity,
                            contentDescription = opacityDesc,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(playbackState.alpha * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // 透過度スライダーの展開表示
            AnimatedVisibility(
                visible = showAlphaSlider,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val opacityLabel = when (language) {
                        AppLanguage.ENGLISH -> "Radar Opacity"
                        AppLanguage.KOREAN -> "레이더 투명도"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷达透明度"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷達透明度"
                        AppLanguage.JAPANESE -> "レーダー透過度"
                        AppLanguage.SPANISH -> "Opacidad del radar"
                        AppLanguage.FRENCH -> "Opacité du radar"
                        AppLanguage.PORTUGUESE -> "Opacidade do radar"
                        AppLanguage.RUSSIAN -> "Прозрачность радара"
                    }
                    Text(
                        text = opacityLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = contentColor.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.width(90.dp)
                    )
                    val expressHaptics = LocalExpressiveHaptics.current
                    Slider(
                        value = playbackState.alpha,
                        onValueChange = { newAlpha ->
                            val oldStep = kotlin.math.round((playbackState.alpha - 0.60f) / 0.025f).toInt()
                            val newStep = kotlin.math.round((newAlpha - 0.60f) / 0.025f).toInt()
                            if (newStep != oldStep) {
                                expressHaptics.play(ExpressiveHapticPattern.SCRUB_TICK)
                            }
                            onAlphaChange(newAlpha)
                        },
                        valueRange = 0.60f..0.80f,
                        steps = 8,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(playbackState.alpha * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            // -------------------------------------------------------------
            // 中段: タイムラインスライダー (実況ゾーン vs HRRR予測ゾーン)
            // -------------------------------------------------------------
            if (frames.isNotEmpty()) {
                TimelineZoneSlider(
                    framesCount = frames.size,
                    currentIndex = currentIndex,
                    nowDividerIndex = nowDividerIndex,
                    onSeekToIndex = onSeekToIndex
                )
            }

            // -------------------------------------------------------------
            // 下段: 再生コントロール (コマ送り・再生・一時停止・ジャンプ)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 2時間前（先頭）へジャンプ
                val rewindDesc = when (language) {
                    AppLanguage.ENGLISH -> "Jump to 2 hours ago"
                    AppLanguage.KOREAN -> "2시간 전으로 이동"
                    AppLanguage.SIMPLIFIED_CHINESE -> "跳至2小时前"
                    AppLanguage.TRADITIONAL_CHINESE -> "跳至2小時前"
                    AppLanguage.JAPANESE -> "2時間前へジャンプ"
                    AppLanguage.SPANISH -> "Ir a hace 2 horas"
                    AppLanguage.FRENCH -> "Aller à il y a 2 h"
                    AppLanguage.PORTUGUESE -> "Ir para 2 horas atrás"
                    AppLanguage.RUSSIAN -> "Перейти на 2 ч назад"
                }
                IconButton(
                    onClick = onSeekToStart,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = rewindDesc,
                        tint = contentColor.copy(alpha = 0.75f)
                    )
                }

                // コマ戻し (1フレーム戻す)
                val prevDesc = when (language) {
                    AppLanguage.ENGLISH -> "Step Back"
                    AppLanguage.KOREAN -> "이전 프레임"
                    AppLanguage.SIMPLIFIED_CHINESE -> "上一帧"
                    AppLanguage.TRADITIONAL_CHINESE -> "上一格"
                    AppLanguage.JAPANESE -> "コマ戻し"
                    AppLanguage.SPANISH -> "Paso atrás"
                    AppLanguage.FRENCH -> "Image précédente"
                    AppLanguage.PORTUGUESE -> "Voltar um quadro"
                    AppLanguage.RUSSIAN -> "Кадр назад"
                }
                IconButton(
                    onClick = onStepBackward,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = prevDesc,
                        tint = contentColor
                    )
                }

                // 再生 / 一時停止 メインボタン (大きめのFABスタイル)
                val playButtonColor by animateColorAsState(
                    targetValue = if (playbackState.isPlaying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    label = "playButtonColor"
                )

                val playPauseDesc = if (playbackState.isPlaying) {
                    when (language) {
                        AppLanguage.ENGLISH -> "Pause"
                        AppLanguage.KOREAN -> "일시정지"
                        AppLanguage.SIMPLIFIED_CHINESE -> "暂停"
                        AppLanguage.TRADITIONAL_CHINESE -> "暫停"
                        AppLanguage.JAPANESE -> "一時停止"
                        AppLanguage.SPANISH -> "Pausar"
                        AppLanguage.FRENCH -> "Pause"
                        AppLanguage.PORTUGUESE -> "Pausar"
                        AppLanguage.RUSSIAN -> "Пауза"
                    }
                } else {
                    when (language) {
                        AppLanguage.ENGLISH -> "Play"
                        AppLanguage.KOREAN -> "재생"
                        AppLanguage.SIMPLIFIED_CHINESE -> "播放"
                        AppLanguage.TRADITIONAL_CHINESE -> "播放"
                        AppLanguage.JAPANESE -> "再生"
                        AppLanguage.SPANISH -> "Reproducir"
                        AppLanguage.FRENCH -> "Lire"
                        AppLanguage.PORTUGUESE -> "Reproduzir"
                        AppLanguage.RUSSIAN -> "Воспроизвести"
                    }
                }

                FilledIconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = playButtonColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = playPauseDesc,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // コマ送り (1フレーム進める)
                val nextDesc = when (language) {
                    AppLanguage.ENGLISH -> "Step Forward"
                    AppLanguage.KOREAN -> "다음 프레임"
                    AppLanguage.SIMPLIFIED_CHINESE -> "下一帧"
                    AppLanguage.TRADITIONAL_CHINESE -> "下一格"
                    AppLanguage.JAPANESE -> "コマ送り"
                    AppLanguage.SPANISH -> "Paso adelante"
                    AppLanguage.FRENCH -> "Image suivante"
                    AppLanguage.PORTUGUESE -> "Avançar um quadro"
                    AppLanguage.RUSSIAN -> "Кадр вперёд"
                }
                IconButton(
                    onClick = onStepForward,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = nextDesc,
                        tint = contentColor
                    )
                }

                // 現在時刻 (NOW) へワンタップジャンプ
                val nowBtnText = when (language) {
                    AppLanguage.ENGLISH -> "NOW"
                    AppLanguage.KOREAN -> "현재"
                    AppLanguage.SIMPLIFIED_CHINESE -> "现在"
                    AppLanguage.TRADITIONAL_CHINESE -> "現在"
                    AppLanguage.JAPANESE -> "現在"
                    AppLanguage.SPANISH -> "AHORA"
                    AppLanguage.FRENCH -> "DIRECT"
                    AppLanguage.PORTUGUESE -> "AGORA"
                    AppLanguage.RUSSIAN -> "СЕЙЧАС"
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { onSeekToNow() }
                ) {
                    Text(
                        text = nowBtnText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

/**
 * 実況ゾーン（左）とHRRR予測ゾーン（右）を視覚的に区別したタイムラインスライダー
 */
@Composable
private fun TimelineZoneSlider(
    framesCount: Int,
    currentIndex: Int,
    nowDividerIndex: Int,
    onSeekToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    if (framesCount <= 1) return

    val maxIndex = framesCount - 1
    val nowFraction = (nowDividerIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
    val currentFraction = (currentIndex.toFloat() / maxIndex).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        // ゾーンラベル（実況ゾーン vs 予測ゾーン）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pastZoneLabel = when (language) {
                AppLanguage.ENGLISH -> "◀ Past 2h (MRMS Live)"
                AppLanguage.KOREAN -> "◀ 지난 2시간 (MRMS 실황)"
                AppLanguage.SIMPLIFIED_CHINESE -> "◀ 过去2小时 (MRMS 实况)"
                AppLanguage.TRADITIONAL_CHINESE -> "◀ 過去2小時 (MRMS 實況)"
                AppLanguage.JAPANESE -> "◀ 過去2時間 (MRMS 実況)"
                AppLanguage.SPANISH -> "◀ Últimas 2h (MRMS en vivo)"
                AppLanguage.FRENCH -> "◀ 2 dern. h (MRMS en direct)"
                AppLanguage.PORTUGUESE -> "◀ Últimas 2h (MRMS ao vivo)"
                AppLanguage.RUSSIAN -> "◀ Прошлые 2 ч (MRMS)"
            }
            Text(
                text = pastZoneLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0091EA)
                )
            )

            val futureZoneLabel = when (language) {
                AppLanguage.ENGLISH -> "Next 18h (HRRR Forecast) ▶"
                AppLanguage.KOREAN -> "앞으로 18시간 (HRRR 예측) ▶"
                AppLanguage.SIMPLIFIED_CHINESE -> "未来18小时 (HRRR 预测) ▶"
                AppLanguage.TRADITIONAL_CHINESE -> "未來18小時 (HRRR 預測) ▶"
                AppLanguage.JAPANESE -> "未来18時間 (HRRR 予測) ▶"
                AppLanguage.SPANISH -> "Próx. 18h (Previsión HRRR) ▶"
                AppLanguage.FRENCH -> "18 proch. h (Prévision HRRR) ▶"
                AppLanguage.PORTUGUESE -> "Próx. 18h (Previsão HRRR) ▶"
                AppLanguage.RUSSIAN -> "След. 18 ч (Прогноз HRRR) ▶"
            }
            Text(
                text = futureZoneLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7C4DFF)
                )
            )
        }

        // カスタムスライダートラック描画領域
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val expressHaptics = LocalExpressiveHaptics.current
            val nowIdx = kotlin.math.round(nowFraction * maxIndex).toInt().coerceIn(0, maxIndex)
            val seekWithHaptic: (Int) -> Unit = { targetIdx ->
                if (targetIdx != currentIndex) {
                    if (targetIdx == 0 || targetIdx == maxIndex || targetIdx == nowIdx) {
                        expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY)
                    } else {
                        expressHaptics.play(ExpressiveHapticPattern.SCRUB_TICK)
                    }
                }
                onSeekToIndex(targetIdx)
            }

            // 1. 背景トラック（実況ゾーンと予測ゾーンの色分け）
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .pointerInput(framesCount) {
                        detectTapGestures { offset ->
                            val clickedFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetIndex = kotlin.math.round(clickedFraction * maxIndex).toInt().coerceIn(0, maxIndex)
                            seekWithHaptic(targetIndex)
                        }
                    }
            ) {
                val trackWidth = size.width
                val trackHeight = size.height
                val splitX = trackWidth * nowFraction

                // 左側: 実況ゾーン (爽やかなシアン〜ブルー系)
                drawRoundRect(
                    color = Color(0xFF00B0FF).copy(alpha = 0.35f),
                    topLeft = Offset(0f, 0f),
                    size = Size(splitX, trackHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )

                // 右側: HRRR予測ゾーン (パープル〜バイオレット系)
                drawRoundRect(
                    color = Color(0xFF7C4DFF).copy(alpha = 0.30f),
                    topLeft = Offset(splitX, 0f),
                    size = Size(trackWidth - splitX, trackHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )

                // 再生進捗バー（現在選択位置までの塗りつぶし）
                val currentX = trackWidth * currentFraction
                val progressBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00B0FF),
                        if (currentFraction <= nowFraction) Color(0xFF0091EA) else Color(0xFF7C4DFF)
                    ),
                    startX = 0f,
                    endX = currentX.coerceAtLeast(1f)
                )
                drawRoundRect(
                    brush = progressBrush,
                    topLeft = Offset(0f, 0f),
                    size = Size(currentX, trackHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )

                // 現在時刻 (NOW) の境界ディバイダー縦線
                drawLine(
                    color = Color.White,
                    start = Offset(splitX, -4f),
                    end = Offset(splitX, trackHeight + 4f),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // 2. 現在時刻境界バッジ ("NOW")
            val markerOffsetDp = (maxWidth * nowFraction) - 16.dp
            Surface(
                modifier = Modifier
                    .offset(x = markerOffsetDp.coerceAtLeast(0.dp), y = (-14).dp)
                    .shadow(3.dp, RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF212121),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Text(
                    text = "NOW",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            // 3. 標準Composeスライダー（ジェスチャードラッグ対応、透明トラックで親の独自描画に重ねる）
            Slider(
                value = currentIndex.toFloat(),
                onValueChange = { newVal ->
                    val targetIdx = kotlin.math.round(newVal).toInt().coerceIn(0, maxIndex)
                    seekWithHaptic(targetIdx)
                },
                valueRange = 0f..maxIndex.toFloat().coerceAtLeast(1f),
                steps = 0,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }

        // スライダー下部の時間スパンラベル (2h前, 現在, 18h後)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val minus2h = when (language) {
                AppLanguage.ENGLISH -> "-2h"
                AppLanguage.KOREAN -> "-2시간"
                AppLanguage.SIMPLIFIED_CHINESE -> "-2小时"
                AppLanguage.TRADITIONAL_CHINESE -> "-2小時"
                AppLanguage.JAPANESE -> "-2時間"
                AppLanguage.SPANISH -> "-2h"
                AppLanguage.FRENCH -> "-2h"
                AppLanguage.PORTUGUESE -> "-2h"
                AppLanguage.RUSSIAN -> "-2 ч"
            }
            Text(
                text = minus2h,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
            val currentLabel = when (language) {
                AppLanguage.ENGLISH -> "Now"
                AppLanguage.KOREAN -> "현재 시각"
                AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                AppLanguage.TRADITIONAL_CHINESE -> "當前"
                AppLanguage.JAPANESE -> "現在時刻"
                AppLanguage.SPANISH -> "Ahora"
                AppLanguage.FRENCH -> "Maintenant"
                AppLanguage.PORTUGUESE -> "Agora"
                AppLanguage.RUSSIAN -> "Сейчас"
            }
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            )
            val plus18h = when (language) {
                AppLanguage.ENGLISH -> "+18h"
                AppLanguage.KOREAN -> "+18시간"
                AppLanguage.SIMPLIFIED_CHINESE -> "+18小时"
                AppLanguage.TRADITIONAL_CHINESE -> "+18小時"
                AppLanguage.JAPANESE -> "+18時間"
                AppLanguage.SPANISH -> "+18h"
                AppLanguage.FRENCH -> "+18h"
                AppLanguage.PORTUGUESE -> "+18h"
                AppLanguage.RUSSIAN -> "+18 ч"
            }
            Text(
                text = plus18h,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// =========================================================================
// 2. dBZカラー凡例バー (薄い青 5dBZ から 紫 65dBZ 以上まで)
// =========================================================================

/**
 * 画面の隅に配置するdBZカラー凡例バー
 *
 * 要件:
 * 1. 画面の隅に雨の強さを示すdBZカラースケール（薄い青 5dBZ から 紫 65dBZ 以上まで）をコンパクトに表示
 * 2. タップで展開し、雨の強さの区分・目安降水量・雷雨雹リスクを閲覧可能
 */
@Composable
fun RadarDbzLegendBar(
    modifier: Modifier = Modifier,
    levels: List<DbzColorLevel> = US_NEXRAD_DBZ_LEVELS
) {
    val language = LocalAppLanguage.current
    var isExpanded by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) Color(0xDD181920) else Color(0xEEFFFFFF)

    Surface(
        modifier = modifier
            .wrapContentWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ヘッダー（タイトル＆インフォアイコン）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val legendTitle = when (language) {
                    AppLanguage.ENGLISH -> "Reflectivity (dBZ)"
                    AppLanguage.KOREAN -> "강수 반사도 (dBZ)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "雨云反射强度 (dBZ)"
                    AppLanguage.TRADITIONAL_CHINESE -> "雨雲反射強度 (dBZ)"
                    AppLanguage.JAPANESE -> "雨雲反射強度 (dBZ)"
                    AppLanguage.SPANISH -> "Reflectividad (dBZ)"
                    AppLanguage.FRENCH -> "Réflectivité (dBZ)"
                    AppLanguage.PORTUGUESE -> "Refletividade (dBZ)"
                    AppLanguage.RUSSIAN -> "Отражаемость (дБZ)"
                }
                Text(
                    text = legendTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
                val legendDetailDesc = when (language) {
                    AppLanguage.ENGLISH -> "Legend Details"
                    AppLanguage.KOREAN -> "범례 상세"
                    AppLanguage.SIMPLIFIED_CHINESE -> "图例详情"
                    AppLanguage.TRADITIONAL_CHINESE -> "圖例詳情"
                    AppLanguage.JAPANESE -> "凡例詳細"
                    AppLanguage.SPANISH -> "Detalles de la leyenda"
                    AppLanguage.FRENCH -> "Détails de la légende"
                    AppLanguage.PORTUGUESE -> "Detalhes da legenda"
                    AppLanguage.RUSSIAN -> "Подробности легенды"
                }
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = legendDetailDesc,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // コンパクトカラーバー（5dBZ 〜 65+dBZ）
            val rawColors = levels.map { it.color }
            val gradientColors = when {
                rawColors.size >= 2 -> rawColors
                rawColors.size == 1 -> listOf(rawColors[0], rawColors[0])
                else -> listOf(Color.Transparent, Color.Transparent)
            }
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(gradientColors))
            )

            // 代表的な数値目盛りラベル (5, 25, 45, 65+)
            Row(
                modifier = Modifier.width(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "5", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                Text(text = "25", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                Text(text = "45", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                Text(text = "65+ dBZ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
            }

            // 展開時の詳細ポップアップ
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    levels.reversed().forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(level.color)
                            )
                            Text(
                                text = "${level.label} dBZ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.width(42.dp)
                            )
                            Text(
                                text = level.intensityName(language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. レーダーマップ画面本体 (ラスタータイルレイヤー重畳描画)
// =========================================================================

/**
 * Compose対応Google Maps SDKを使用したアメリカ向け雨雲レーダーマップ
 *
 * 要件:
 * 1. 半透明のラスタータイルレイヤーとして雨雲レーダーを重畳描画
 * 2. レーダータイルのアルファ値は視認性に応じて0.6〜0.8の間で調整可能
 */
@Composable
fun UsRadarMapView(
    latitude: Double,
    longitude: Double,
    frames: List<RadarTimelineFrame>,
    currentFrameIndex: Int,
    alpha: Float,
    modifier: Modifier = Modifier,
    zoom: Int = 7,
    onMapReady: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    var activeOverlay by remember { mutableStateOf<TileOverlay?>(null) }
    val cacheManager = remember { RadarTileCacheManager.getInstance() }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        var isDestroyed = false
        var isStarted = false
        var isResumed = false
        fun destroySafely() {
            if (isDestroyed) return
            isDestroyed = true
            try {
                if (isResumed) { mapView.onPause(); isResumed = false }
                if (isStarted) { mapView.onStop(); isStarted = false }
                activeOverlay?.remove()
                activeOverlay = null
                googleMapRef = null
                mapView.onDestroy()
            } catch (_: Exception) {}
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!isDestroyed && !isStarted) { mapView.onStart(); isStarted = true }
                Lifecycle.Event.ON_RESUME -> if (!isDestroyed && !isResumed) { mapView.onResume(); isResumed = true }
                Lifecycle.Event.ON_PAUSE -> if (!isDestroyed && isResumed) { mapView.onPause(); isResumed = false }
                Lifecycle.Event.ON_STOP -> if (!isDestroyed && isStarted) { mapView.onStop(); isStarted = false }
                Lifecycle.Event.ON_DESTROY -> destroySafely()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            destroySafely()
        }
    }

    // Google Maps の非同期初期化
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            googleMapRef = map
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isCompassEnabled = true
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom.toFloat()))
            onMapReady()
        }
    }

    // 現在のフレーム変更またはアルファ値変更時に TileOverlay を更新
    LaunchedEffect(currentFrameIndex, alpha, frames.size, googleMapRef) {
        val map = googleMapRef ?: return@LaunchedEffect
        val frame = frames.getOrNull(currentFrameIndex) ?: return@LaunchedEffect

        // 古いオーバーレイを削除
        activeOverlay?.remove()

        // タイルプロバイダーを構築
        val tileProvider = object : TileProvider {
            override fun getTile(x: Int, y: Int, z: Int): Tile? {
                val cacheKey = "${frame.epochSeconds}-$z-$x-$y"
                val cachedBmp = cacheManager.getBitmap(cacheKey)

                if (cachedBmp != null && !cachedBmp.isRecycled) {
                    val stream = ByteArrayOutputStream()
                    cachedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    return Tile(256, 256, stream.toByteArray())
                }

                val tileUrl = frame.tileUrlTemplate
                    .replace("{z}", z.toString())
                    .replace("{x}", x.toString())
                    .replace("{y}", y.toString())
                    .replace("{epoch}", frame.epochSeconds.toString())

                val rawBytes = cacheManager.getRawBytes(tileUrl)
                if (rawBytes != null) {
                    return Tile(256, 256, rawBytes)
                }

                // キャッシュにない場合は TileProvider.NO_TILE を返しつつ非同期でキャッシュロード
                return TileProvider.NO_TILE
            }
        }

        // アルファ値 (0.6f〜0.8f) を Google Maps の transparency (0.2f〜0.4f) にマッピング
        // ※ transparency: 0.0=完全不透明, 1.0=完全透明
        val transparency = (1.0f - alpha).coerceIn(0.2f, 0.4f)

        val options = TileOverlayOptions()
            .tileProvider(tileProvider)
            .transparency(transparency)
            .zIndex(100f)
            .fadeIn(false)

        activeOverlay = map.addTileOverlay(options)
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}

// =========================================================================
// 4. Compose プレビュー関数
// =========================================================================

private fun createMockTimelineFrames(): List<RadarTimelineFrame> {
    val now = System.currentTimeMillis() / 1000L
    val frames = mutableListOf<RadarTimelineFrame>()

    // 過去2時間 (MRMS 実況 5分間隔 = 24フレーム)
    for (i in 24 downTo 0) {
        val t = now - (i * 300L)
        frames.add(
            RadarTimelineFrame(
                epochSeconds = t,
                isForecast = false,
                frameType = UsRadarFrameType.MRMS_BASE_REFLECTIVITY,
                tileUrlTemplate = "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/mrms::{time}/{z}/{x}/{y}.png",
                timeLabel = "14:${String.format("%02d", (60 - i * 5).coerceIn(0, 59))}",
                displayLabel = "今日 14:30 実況",
                description = "MRMS Base Reflectivity (地上反射強度実況・5分更新)",
                intervalMinutes = 5
            )
        )
    }

    // 未来18時間 (HRRR 予測 15分/1時間間隔)
    for (i in 1..24) {
        val t = now + (i * 900L)
        frames.add(
            RadarTimelineFrame(
                epochSeconds = t,
                isForecast = true,
                frameType = UsRadarFrameType.HRRR_SIMULATED_REFLECTIVITY,
                tileUrlTemplate = "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::{time}/{z}/{x}/{y}.png",
                timeLabel = "19:00",
                displayLabel = "今日 19:00 予測",
                description = "HRRR 1km AGL Simulated Reflectivity (15分間隔高精度予測)",
                intervalMinutes = 15
            )
        )
    }

    return frames
}

@Preview(name = "Radar Timeline Controller - Observed (実況選択時)", showBackground = true)
@Composable
fun PreviewRadarTimelineControllerObserved() {
    val mockFrames = remember { createMockTimelineFrames() }
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(16.dp)
        ) {
            RadarTimelineController(
                frames = mockFrames,
                currentIndex = 18,
                playbackState = RadarPlaybackState(isPlaying = false, currentFrameIndex = 18, alpha = 0.72f),
                nowDividerIndex = 24,
                onPlayPauseToggle = {},
                onStepForward = {},
                onStepBackward = {},
                onSeekToIndex = {},
                onSeekToNow = {},
                onSeekToStart = {},
                onSeekToEnd = {},
                onAlphaChange = {}
            )
        }
    }
}

@Preview(name = "Radar Timeline Controller - Forecast (予測選択時)", showBackground = true)
@Composable
fun PreviewRadarTimelineControllerForecast() {
    val mockFrames = remember { createMockTimelineFrames() }
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(16.dp)
        ) {
            RadarTimelineController(
                frames = mockFrames,
                currentIndex = 35,
                playbackState = RadarPlaybackState(isPlaying = true, currentFrameIndex = 35, alpha = 0.75f),
                nowDividerIndex = 24,
                onPlayPauseToggle = {},
                onStepForward = {},
                onStepBackward = {},
                onSeekToIndex = {},
                onSeekToNow = {},
                onSeekToStart = {},
                onSeekToEnd = {},
                onAlphaChange = {}
            )
        }
    }
}

@Preview(name = "dBZ Legend Bar - Compact", showBackground = true)
@Composable
fun PreviewRadarDbzLegendBarCompact() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF212121))
                .padding(16.dp)
        ) {
            RadarDbzLegendBar()
        }
    }
}

@Preview(name = "Full US Radar Screen Preview", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewUsRadarScreenFull() {
    val mockFrames = remember { createMockTimelineFrames() }
    var currentIndex by remember { mutableIntStateOf(24) }
    var isPlaying by remember { mutableStateOf(false) }
    var alpha by remember { mutableFloatStateOf(0.72f) }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1B222C))
        ) {
            // 背景モック地図
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF263238), Color(0xFF102027))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Map Layer (Google Maps Native)",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 右上: dBZ カラー凡例バー
            RadarDbzLegendBar(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            )

            // 下部: タイムラインコントローラー
            RadarTimelineController(
                frames = mockFrames,
                currentIndex = currentIndex,
                playbackState = RadarPlaybackState(
                    isPlaying = isPlaying,
                    currentFrameIndex = currentIndex,
                    alpha = alpha
                ),
                nowDividerIndex = 24,
                onPlayPauseToggle = { isPlaying = !isPlaying },
                onStepForward = { if (currentIndex < mockFrames.lastIndex) currentIndex++ },
                onStepBackward = { if (currentIndex > 0) currentIndex-- },
                onSeekToIndex = { currentIndex = it },
                onSeekToNow = { currentIndex = 24 },
                onSeekToStart = { currentIndex = 0 },
                onSeekToEnd = { currentIndex = mockFrames.lastIndex },
                onAlphaChange = { alpha = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}
