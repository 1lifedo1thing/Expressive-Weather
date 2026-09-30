package com.example.expressiveweather.ui.radar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.JmaLayerCategory
import com.example.expressiveweather.data.JmaLayerType
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.RadarColorScheme
import com.example.expressiveweather.data.RadarDataSource
import com.example.expressiveweather.data.RadarFrame
import com.example.expressiveweather.data.RadarProviderRegistry
import com.example.expressiveweather.data.RadarRepository
import com.example.expressiveweather.data.RadarSourceMode
import com.example.expressiveweather.data.RainViewerResponse
import com.example.expressiveweather.data.UsRadarLayerFilter
import com.example.expressiveweather.data.UsLayerCategory
import com.example.expressiveweather.data.UsRadarLayerType
import com.example.expressiveweather.data.US_NEXRAD_DBZ_LEVELS
import com.example.expressiveweather.data.RAIN_VIEWER_DBZ_LEVELS
import com.example.expressiveweather.data.isLocationOrCountryInJapan
import com.example.expressiveweather.data.isLocationOrCountryInUnitedStates
import com.example.expressiveweather.theme.LocalDarkTheme
import com.example.expressiveweather.ui.components.m3eBouncyClick
import com.example.expressiveweather.ui.screens.SingleChoiceDialog
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 気象庁 降水強度・雨の強さの区分データモデル
 */
data class RainIntensityLevel(
    val rangeLabel: String,
    val nameJa: String,
    val color: Color,
    val onColor: Color,
    val impactJa: String,
    val warningJa: String,
    val nameEn: String,
    val impactEn: String,
    val warningEn: String,
    val nameZhCn: String,
    val impactZhCn: String,
    val warningZhCn: String,
    val nameZhTw: String,
    val impactZhTw: String,
    val warningZhTw: String
) {
    fun name(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.ENGLISH -> nameEn
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhCn
        AppLanguage.TRADITIONAL_CHINESE -> nameZhTw
    }
    fun impact(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> impactJa
        AppLanguage.ENGLISH -> impactEn
        AppLanguage.SIMPLIFIED_CHINESE -> impactZhCn
        AppLanguage.TRADITIONAL_CHINESE -> impactZhTw
    }
    fun warning(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> warningJa
        AppLanguage.ENGLISH -> warningEn
        AppLanguage.SIMPLIFIED_CHINESE -> warningZhCn
        AppLanguage.TRADITIONAL_CHINESE -> warningZhTw
    }
}

val RAIN_INTENSITY_LEVELS = listOf(
    RainIntensityLevel(
        rangeLabel = "80mm/h〜",
        nameJa = "猛烈な雨",
        color = Color(0xFFB40068),
        onColor = Color.White,
        impactJa = "息苦しくなるような圧迫感。水しぶきで前方が見えなくなる。",
        warningJa = "重大な災害発生の危険性が極めて高い。河川氾濫・土砂崩れに最大級警戒。",
        nameEn = "Torrential Rain",
        impactEn = "Oppressive sensation making breathing difficult; front view blocked by spray.",
        warningEn = "Extreme disaster hazard. Maximum alert for river flooding and landslides.",
        nameZhCn = "特大暴雨",
        impactZhCn = "压迫感极强呼吸困难，漫天水花导致前方视线完全受阻。",
        warningZhCn = "发生重大灾害的危险性极高。对河流泛滥及山体滑坡泥石流保持最高级别警戒。",
        nameZhTw = "特大暴雨",
        impactZhTw = "壓迫感極強呼吸困難，漫天水花導致前方視線完全受阻。",
        warningZhTw = "發生重大災害的危險性極高。對河流氾濫及土石流保持最高級別警戒。"
    ),
    RainIntensityLevel(
        rangeLabel = "50〜80",
        nameJa = "非常に激しい雨",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "滝のように降る。傘は全く役に立たない。",
        warningJa = "地下街や低地への浸水・土砂崩れ・中小河川の急な増水に警戒。",
        nameEn = "Extremely Heavy Rain",
        impactEn = "Pours like a waterfall; umbrellas are completely useless.",
        warningEn = "Alert for low-lying inundation, basement flooding, landslides, and river surges.",
        nameZhCn = "大暴雨",
        impactZhCn = "暴雨如瀑布倾泻，雨伞完全失去遮挡作用。",
        warningZhCn = "警惕地下商城、低洼地淹水积水、山体滑坡及中小河流突发暴涨。",
        nameZhTw = "大暴雨",
        impactZhTw = "暴雨如瀑布傾瀉，雨傘完全失去遮擋作用。",
        warningZhTw = "警惕地下商城、低窪地淹水積水、土石流及中小河流突發暴漲。"
    ),
    RainIntensityLevel(
        rangeLabel = "30〜50",
        nameJa = "激しい雨",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "バケツをひっくり返したように降る。",
        warningJa = "道路が川のようになり、高速走行の車はハイドロプレーニングを起こす。",
        nameEn = "Heavy Rain",
        impactEn = "Pours as if buckets of water were dumped.",
        warningEn = "Roads become rivers; fast-moving vehicles risk hydroplaning.",
        nameZhCn = "暴雨",
        impactZhCn = "倾盆大雨，犹如打翻水桶般猛烈倾泻。",
        warningZhCn = "道路易成水流河流，高速行驶车辆极易发生水漂打滑失控。",
        nameZhTw = "暴雨",
        impactZhTw = "傾盆大雨，猶如打翻水桶般猛烈傾瀉。",
        warningZhTw = "道路易成水流河流，高速行駛車輛極易發生水漂打滑失控。"
    ),
    RainIntensityLevel(
        rangeLabel = "20〜30",
        nameJa = "強い雨",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "土砂降りの雨。傘をさしていても濡れる。",
        warningJa = "側溝や下水、小さな川から水があふれ出すことがある。",
        nameEn = "Strong Rain",
        impactEn = "Pouring rain; clothes get wet even holding an umbrella.",
        warningEn = "Street gutters, drains, and small creeks may overflow.",
        nameZhCn = "大雨",
        impactZhCn = "大雨倾盆，即使撑伞身体依然容易被淋湿。",
        warningZhCn = "排水沟、城市下水道及小溪沟可能会溢水泛滥。",
        nameZhTw = "大雨",
        impactZhTw = "大雨傾盆，即使撐傘身體依然容易被淋濕。",
        warningZhTw = "排水溝、城市下水道及小溪溝可能會溢水氾濫。"
    ),
    RainIntensityLevel(
        rangeLabel = "10〜20",
        nameJa = "やや強い雨",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "ザーザーと音を立てて降る。足元が濡れる。",
        warningJa = "地面一面に水たまりができる。長く屋外にいると服にしみ込む。",
        nameEn = "Moderate Rain",
        impactEn = "Rustling rainfall with loud sound; shoes and feet get wet.",
        warningEn = "Puddles form across surfaces; moisture penetrates clothes over time.",
        nameZhCn = "中到大雨",
        impactZhCn = "哗哗作响的雨声清晰可闻，地面溅水打湿鞋袜。",
        warningZhCn = "路面到处形成水洼，长时间停留户外衣物会逐渐渗透湿透。",
        nameZhTw = "中到大雨",
        impactZhTw = "嘩嘩作響的雨聲清晰可聞，地面濺水打濕鞋襪。",
        warningZhTw = "路面到處形成水窪，長時間停留戶外衣物會逐漸滲透濕透。"
    ),
    RainIntensityLevel(
        rangeLabel = "5〜10",
        nameJa = "本降り",
        color = Color(0xFF218CFF),
        onColor = Color.White,
        impactJa = "雨の音が周囲にはっきり聞こえるまとまった雨。",
        warningJa = "傘をさして歩く必要がある。水たまりが広くできる。",
        nameEn = "Steady Rain",
        impactEn = "Continuous steady rain with clear sound.",
        warningEn = "Umbrellas required for walking outside; widespread small puddles.",
        nameZhCn = "中雨",
        impactZhCn = "周围能清楚听到雨声的整场连绵降雨。",
        warningZhCn = "外出必须撑伞步行，低洼地面开始形成明显积水。",
        nameZhTw = "中雨",
        impactZhTw = "周圍能清楚聽到雨聲的整場連綿降雨。",
        warningZhTw = "外出必須撐傘步行，低窪地面開始形成明顯積水。"
    ),
    RainIntensityLevel(
        rangeLabel = "1〜5",
        nameJa = "弱い雨",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "しとしとと降る雨。傘をささないと濡れる。",
        warningJa = "外出時は折りたたみ傘などの携帯が推奨される。",
        nameEn = "Light Rain",
        impactEn = "Gentle pattering rain; you will get wet without cover.",
        warningEn = "Carrying a compact folding umbrella is recommended outdoors.",
        nameZhCn = "小雨",
        impactZhCn = "淅淅沥沥的细雨，不撑伞会被打湿。",
        warningZhCn = "出门建议随身携带便携折叠雨伞。",
        nameZhTw = "小雨",
        impactZhTw = "淅淅瀝瀝的細雨，不撐傘會被打濕。",
        warningZhTw = "出門建議隨身攜帶便攜折疊雨傘。"
    ),
    RainIntensityLevel(
        rangeLabel = "0.1〜1",
        nameJa = "小雨・霧雨",
        color = Color(0xFFD4E3F5),
        onColor = Color(0xFF1E293B),
        impactJa = "傘がなくても何とか耐えられる程度の微弱な雨。",
        warningJa = "地面がうっすら湿る程度。長時間の歩行は傘があると安心。",
        nameEn = "Drizzle",
        impactEn = "Faint mist or drizzle barely bearable without an umbrella.",
        warningEn = "Ground is slightly dampened; having an umbrella is helpful for long walks.",
        nameZhCn = "微雨・毛毛雨",
        impactZhCn = "极微弱的霏霏细雨，即便不撑伞也能短时间耐受。",
        warningZhCn = "路面微微湿润，长时间室外行走备伞更加安心。",
        nameZhTw = "微雨・毛毛雨",
        impactZhTw = "極微弱的霏霏細雨，即便不撐傘也能短時間耐受。",
        warningZhTw = "路面微微濕潤，長時間室外行走備傘更加安心。"
    )
)

private fun initialRadarFrameIndex(data: RainViewerResponse?, frames: List<RadarFrame>): Int {
    if (frames.isEmpty()) return -1
    val pastCount = data?.radar?.past?.size ?: 0
    return when {
        pastCount > 0 -> pastCount - 1
        data?.radar?.nowcast?.isNotEmpty() == true -> 0
        else -> frames.lastIndex
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarDetailScreen(
    latitude: Double,
    longitude: Double,
    cityName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    country: String? = null,
    showZoomBadge: Boolean = false,
    initialLayerId: String? = null
) {
    val language = LocalAppLanguage.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember(context) { RadarRepository.getInstance(context) }
    val sourceMode by repo.sourceModeFlow.collectAsState()

    var radarData by remember { mutableStateOf<RainViewerResponse?>(null) }
    var dataSource by remember(sourceMode, latitude, longitude, country) {
        mutableStateOf(RadarProviderRegistry.resolveProvider(sourceMode, latitude, longitude, country))
    }
    var selectedUsLayer by remember { mutableStateOf(UsRadarLayerType.UNIFIED_ALL) }
    var selectedUsFilter by remember { mutableStateOf(UsRadarLayerFilter.ALL) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentFrameIndex by remember { mutableIntStateOf(-1) }
    var currentTimestamp by remember { mutableLongStateOf(0L) }
    var selectedColorScheme by remember { mutableStateOf(RadarColorScheme.UNIVERSAL_BLUE) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var zoomInTrigger by remember { mutableIntStateOf(0) }
    var zoomOutTrigger by remember { mutableIntStateOf(0) }

    // 気象庁地図レイヤーの初期選択状態（ディープリンク指定があれば最優先で初期化）
    val initialLayer = remember(initialLayerId) {
        if (!initialLayerId.isNullOrBlank()) {
            JmaLayerType.entries.firstOrNull { it.id.equals(initialLayerId, ignoreCase = true) }
                ?: JmaLayerType.HRPNS
        } else {
            JmaLayerType.HRPNS
        }
    }
    var selectedJmaLayer by remember(initialLayer) { mutableStateOf(initialLayer) }
    var isSpeedDialOpen by remember { mutableStateOf(false) }
    var showFullLayerSheet by remember { mutableStateOf(false) }
    var sheetCategory by remember { mutableStateOf(JmaLayerCategory.NOWCAST) }
    var sheetUsCategory by remember { mutableStateOf(UsLayerCategory.UNIFIED) }
    // 降水量・雨の強さ一覧モーダルシートの表示状態
    var showIntensitySheet by remember { mutableStateOf(false) }
    // 縦型降水強度凡例ウィジェットの展開状態 (Frame 95 ⇔ Frame 96)
    var isLegendExpanded by remember { mutableStateOf(false) }

    // リアルタイム現在時刻（毎秒更新）
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val realTimeStr = remember(currentTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTimeMillis))
    }

    // 過去＋未来ナウキャストの全フレーム
    val frames = remember(radarData) {
        if (radarData != null) {
            radarData!!.radar.past + radarData!!.radar.nowcast
        } else {
            emptyList()
        }
    }

    // 初回ロード & 設定・座標・国名・レイヤー変更時
    LaunchedEffect(latitude, longitude, country, sourceMode, selectedJmaLayer, selectedUsLayer) {
        isLoading = true
        if (dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer != JmaLayerType.HRPNS) {
            val result = repo.getJmaLayerData(selectedJmaLayer)
            dataSource = RadarDataSource.JMA_NOWCAST
            if (result.isSuccess) {
                val data = result.getOrNull()
                radarData = data
                val f = if (data != null) data.radar.past + data.radar.nowcast else emptyList()
                if (f.isNotEmpty()) {
                    val initialIndex = initialRadarFrameIndex(data, f)
                    currentFrameIndex = initialIndex
                    currentTimestamp = f[initialIndex].time
                } else {
                    currentFrameIndex = -1
                    currentTimestamp = 0L
                }
            }
        } else {
            val (result, source) = repo.getRadarData(latitude, longitude, country, usLayer = selectedUsLayer)
            dataSource = source
            if (result.isSuccess) {
                val data = result.getOrNull()
                radarData = data
                val f = if (data != null) data.radar.past + data.radar.nowcast else emptyList()
                if (f.isNotEmpty()) {
                    val initialIndex = initialRadarFrameIndex(data, f)
                    currentFrameIndex = initialIndex
                    currentTimestamp = f[initialIndex].time
                }
            }
        }
        isLoading = false
    }

    // 通知ディープリンクからの再遷移検知
    LaunchedEffect(initialLayerId) {
        if (!initialLayerId.isNullOrBlank()) {
            val target = JmaLayerType.entries.firstOrNull { it.id.equals(initialLayerId, ignoreCase = true) }
            if (target != null && target != selectedJmaLayer) {
                selectedJmaLayer = target
            }
        }
    }

    // レイヤー変更ハンドラ
    fun switchJmaLayer(newLayer: JmaLayerType) {
        selectedJmaLayer = newLayer
        isSpeedDialOpen = false
        showFullLayerSheet = false
        isPlaying = false
        isLoading = true
        radarData = null
        currentFrameIndex = -1
        currentTimestamp = 0L
        coroutineScope.launch {
            val repo = RadarRepository.getInstance(context)
            val result = repo.getJmaLayerData(newLayer)
            dataSource = RadarDataSource.JMA_NOWCAST
            if (result.isSuccess) {
                val data = result.getOrNull()
                radarData = data
                val frames = if (data != null) data.radar.past + data.radar.nowcast else emptyList()
                if (frames.isNotEmpty()) {
                    val initialIndex = initialRadarFrameIndex(data, frames)
                    currentFrameIndex = initialIndex
                    currentTimestamp = frames[initialIndex].time
                } else {
                    currentFrameIndex = -1
                    currentTimestamp = 0L
                }
            } else {
                radarData = null
                currentFrameIndex = -1
                currentTimestamp = 0L
            }
            isLoading = false
        }
    }

    val host = radarData?.host ?: "https://tilecache.rainviewer.com"

    val timeFormatter = remember(language) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    val currentFrame = frames.getOrNull(currentFrameIndex)
    val pastSize = radarData?.radar?.past?.size ?: 0
    val currentObsTime = remember(radarData, currentTimeMillis) {
        val pastList = radarData?.radar?.past ?: emptyList()
        if (pastList.isNotEmpty()) {
            pastList.last().time
        } else {
            currentTimeMillis / 1000L
        }
    }

    val relativeTimeLabel = remember(currentTimestamp, currentObsTime, currentFrameIndex, language) {
        if (currentTimestamp <= 0L) ""
        else {
            val diffSec = currentTimestamp - currentObsTime
            val diffMin = Math.round(diffSec / 60.0).toInt()
            when {
                diffMin in -4..4 -> when (language) {
                    AppLanguage.JAPANESE -> "実況"
                    AppLanguage.ENGLISH -> "Now"
                    AppLanguage.SIMPLIFIED_CHINESE -> "实况"
                    AppLanguage.TRADITIONAL_CHINESE -> "實況"
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
                            AppLanguage.SIMPLIFIED_CHINESE -> if (mins == 0) "+${hours}小时后" else "+${hours}小时${mins}分后"
                            AppLanguage.TRADITIONAL_CHINESE -> if (mins == 0) "+${hours}小時後" else "+${hours}小時${mins}分後"
                        }
                    } else {
                        when (language) {
                            AppLanguage.JAPANESE -> "+${diffMin}分後"
                            AppLanguage.ENGLISH -> "+${diffMin}m"
                            AppLanguage.SIMPLIFIED_CHINESE -> "+${diffMin}分钟后"
                            AppLanguage.TRADITIONAL_CHINESE -> "+${diffMin}分鐘後"
                        }
                    }
                }
            }
        }
    }

    // --- Material 3 Expressive Color パレット (影なし・枠なし・派手な発色) ---
    val colorScheme = MaterialTheme.colorScheme
    // ユーザー要望：全画面マップUIの時にダークモードライトモードがマップ、UIともにちゃんと追従するようにする
    val isDark = LocalDarkTheme.current

    // ユーザー要望「１番薄い色を背景にして」: Material 3 surfaceContainer (完全不透明ソリッド)
    val expressiveCardContainerColor = colorScheme.surfaceContainer

    // シークバーの背景カプセル (surfaceContainerHighest で上品に分離)
    val sliderCapsuleBgColor = colorScheme.surfaceContainerHighest

    // ユーザー要望「マップ選択の左右ボタン、時計の背景、シークバーを最も濃いいろに」: colorScheme.primary
    val speedDialItemBgColor = colorScheme.primary

    // 再生ボタン群の押下・弾性伸縮フォーカス (0 = ⏪, 1 = ▶/⏸, 2 = ⏩, null = 通常)
    var activePlaybackPressedIndex by remember { mutableStateOf<Int?>(null) }

    fun triggerPlaybackStretch(index: Int) {
        coroutineScope.launch {
            activePlaybackPressedIndex = index
            delay(200L) // キビキビとした応答時間
            activePlaybackPressedIndex = null
        }
    }

    // 押下されたボタンが自然で心地よく伸び（1.35f〜1.45f）、他がわずかに縮む（0.88f〜0.90f）上品な弾性アニメーション
    val prevPlaybackWeight by animateFloatAsState(
        targetValue = when (activePlaybackPressedIndex) {
            0 -> 1.35f
            1 -> 0.90f
            2 -> 0.90f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = 500f
        ),
        label = "PrevPlaybackWeight"
    )

    val playPausePlaybackWeight by animateFloatAsState(
        targetValue = when (activePlaybackPressedIndex) {
            0 -> 0.88f
            1 -> 1.45f
            2 -> 0.88f
            else -> if (isPlaying) 1.25f else 1.15f
        },
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = 500f
        ),
        label = "PlayPausePlaybackWeight"
    )

    val nextPlaybackWeight by animateFloatAsState(
        targetValue = when (activePlaybackPressedIndex) {
            0 -> 0.90f
            1 -> 0.90f
            2 -> 1.35f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = 500f
        ),
        label = "NextPlaybackWeight"
    )

    val playbackContainerWidth = 260.dp

    // トリガーボタンの回転角アニメーション（超高速弾性バネでキビキビ回転）
    val triggerRotation by animateFloatAsState(
        targetValue = if (isSpeedDialOpen) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 1200f
        ),
        label = "triggerRotation"
    )

    // スピードダイヤル項目一覧（下から上へのインデックス順：0がボタン直上、6が最上段）
    val bottomToTopItems = remember(selectedJmaLayer, language) {
        listOf(
            // index 0: ボタン直上（最重要・最も押す雨雲）
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "雨雲ナウキャスト"
                    AppLanguage.ENGLISH -> "Precipitation Nowcast"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降雨即时预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨即時預報"
                },
                icon = Icons.Rounded.WaterDrop,
                isSelected = selectedJmaLayer == JmaLayerType.HRPNS,
                onClick = { switchJmaLayer(JmaLayerType.HRPNS) }
            ),
            // index 1
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "雷・竜巻ナウキャスト"
                    AppLanguage.ENGLISH -> "Thunder / Tornado"
                    AppLanguage.SIMPLIFIED_CHINESE -> "雷电・龙卷风即时预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "雷電・龍捲風即時預報"
                },
                icon = Icons.Rounded.Thunderstorm,
                isSelected = selectedJmaLayer == JmaLayerType.THNS || selectedJmaLayer == JmaLayerType.TRNS,
                onClick = { switchJmaLayer(JmaLayerType.THNS) }
            ),
            // index 2
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "キキクル危険度"
                    AppLanguage.ENGLISH -> "Kikikuru Hazard Risk"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Kikikuru危险度"
                    AppLanguage.TRADITIONAL_CHINESE -> "Kikikuru危險度"
                },
                icon = Icons.Rounded.Warning,
                isSelected = selectedJmaLayer.category == JmaLayerCategory.RISK,
                onClick = {
                    if (selectedJmaLayer.category == JmaLayerCategory.RISK) {
                        val nextRisk = when (selectedJmaLayer) {
                            JmaLayerType.LANDSLIDE -> JmaLayerType.HEAVY_RAIN
                            JmaLayerType.HEAVY_RAIN -> JmaLayerType.INUNDATION
                            JmaLayerType.INUNDATION -> JmaLayerType.FLOOD
                            else -> JmaLayerType.LANDSLIDE
                        }
                        switchJmaLayer(nextRisk)
                    } else {
                        switchJmaLayer(JmaLayerType.LANDSLIDE)
                    }
                }
            ),
            // index 3
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "気象衛星ひまわり"
                    AppLanguage.ENGLISH -> "Himawari Satellite"
                    AppLanguage.SIMPLIFIED_CHINESE -> "向日葵气象卫星"
                    AppLanguage.TRADITIONAL_CHINESE -> "向日葵氣象衛星"
                },
                icon = Icons.Rounded.Public,
                isSelected = selectedJmaLayer.category == JmaLayerCategory.HIMAWARI,
                onClick = { switchJmaLayer(JmaLayerType.HIMAWARI_TRUECOLOR) }
            ),
            // index 4
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "天気・予想気温"
                    AppLanguage.ENGLISH -> "Weather & Temp Fcst"
                    AppLanguage.SIMPLIFIED_CHINESE -> "天气・预测气温"
                    AppLanguage.TRADITIONAL_CHINESE -> "天氣・預測氣溫"
                },
                icon = Icons.Rounded.Thermostat,
                isSelected = selectedJmaLayer.category == JmaLayerCategory.WEATHER_DIST,
                onClick = { switchJmaLayer(JmaLayerType.WEATHER_DIST) }
            ),
            // index 5
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "雪・降雪量情報"
                    AppLanguage.ENGLISH -> "Snow & Snowfall"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降雪・积雪量信息"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雪・積雪量資訊"
                },
                icon = Icons.Rounded.AcUnit,
                isSelected = selectedJmaLayer.category == JmaLayerCategory.SNOW,
                onClick = { switchJmaLayer(JmaLayerType.SNOW_DEPTH) }
            ),
            // index 6: 最上段
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "すべてのレイヤー (24種)"
                    AppLanguage.ENGLISH -> "All Layers (24)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "所有图层 (24种)"
                    AppLanguage.TRADITIONAL_CHINESE -> "所有圖層 (24種)"
                },
                icon = Icons.Rounded.Layers,
                isSelected = false,
                onClick = {
                    isSpeedDialOpen = false
                    sheetCategory = selectedJmaLayer.category
                    showFullLayerSheet = true
                }
            )
        )
    }

    val usSpeedDialItems = remember(language) {
        listOf(
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "全米 NEXRAD / HRRR"
                    AppLanguage.ENGLISH -> "US NEXRAD / HRRR"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR"
                },
                icon = Icons.Rounded.Layers,
                isSelected = true,
                onClick = {
                    selectedUsLayer = UsRadarLayerType.UNIFIED_ALL
                    isSpeedDialOpen = false
                }
            ),
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "レーダー提供元切替"
                    AppLanguage.ENGLISH -> "Switch Radar Source"
                    AppLanguage.SIMPLIFIED_CHINESE -> "切换雷达源"
                    AppLanguage.TRADITIONAL_CHINESE -> "切換雷達來源"
                },
                icon = Icons.Rounded.Map,
                isSelected = false,
                onClick = {
                    isSpeedDialOpen = false
                    showSourceDialog = true
                }
            )
        )
    }

    val rainViewerSpeedDialItems = remember(language) {
        listOf(
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "世界レーダー (RainViewer)"
                    AppLanguage.ENGLISH -> "Global Radar (RainViewer)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 (RainViewer)"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 (RainViewer)"
                },
                icon = Icons.Rounded.Public,
                isSelected = true,
                onClick = { isSpeedDialOpen = false }
            ),
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "レーダー提供元切替"
                    AppLanguage.ENGLISH -> "Switch Radar Source"
                    AppLanguage.SIMPLIFIED_CHINESE -> "切换雷达源"
                    AppLanguage.TRADITIONAL_CHINESE -> "切換雷達來源"
                },
                icon = Icons.Rounded.Map,
                isSelected = false,
                onClick = {
                    isSpeedDialOpen = false
                    showSourceDialog = true
                }
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // 1. 全画面地図
        RadarMapView(
            latitude = latitude,
            longitude = longitude,
            radarFrames = frames,
            host = host,
            zoom = if (dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer.category == JmaLayerCategory.HIMAWARI) 5 else 10,
            recenterTrigger = recenterTrigger,
            zoomInTrigger = zoomInTrigger,
            zoomOutTrigger = zoomOutTrigger,
            isInteractive = true,
            isPlaying = isPlaying,
            currentFrameIndex = currentFrameIndex,
            colorScheme = selectedColorScheme,
            isDark = isDark,
            showZoomBadge = showZoomBadge,
            onFrameChanged = { index, timestamp ->
                currentFrameIndex = index
                currentTimestamp = timestamp
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. スピードダイヤルまたは降水強度凡例展開時の背景タップで閉じるスクリム
        if (isSpeedDialOpen || isLegendExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isSpeedDialOpen = false
                        isLegendExpanded = false
                    }
            )
        }

        // 3-A. 左上フローティングピル [ ← {提供元 / レイヤー名} • {地名} ] (影なし・枠なし)
        Surface(
            shape = CircleShape,
            color = expressiveCardContainerColor,
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .m3eBouncyClick(scaleDown = 0.94f) { onBackClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = when (language) {
                        AppLanguage.JAPANESE -> "戻る"
                        AppLanguage.ENGLISH -> "Back"
                        AppLanguage.SIMPLIFIED_CHINESE -> "返回"
                        AppLanguage.TRADITIONAL_CHINESE -> "返回"
                    },
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (dataSource) {
                        RadarDataSource.JMA_NOWCAST -> {
                            if (selectedJmaLayer == JmaLayerType.HRPNS) {
                                when (language) {
                                    AppLanguage.JAPANESE -> "気象庁 雨雲・短時間予報"
                                    AppLanguage.ENGLISH -> "JMA Radar & Short-range Fcst"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 降雨短时预报"
                                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 降雨短時預報"
                                }
                            } else {
                                val jmaPrefix = when (language) {
                                    AppLanguage.JAPANESE -> "気象庁"
                                    AppLanguage.ENGLISH -> "JMA"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "气象厅"
                                    AppLanguage.TRADITIONAL_CHINESE -> "氣象廳"
                                }
                                "$jmaPrefix ${selectedJmaLayer.shortName(language)}"
                            }
                        }
                        RadarDataSource.US_NEXRAD_HRRR -> {
                            if (selectedUsLayer == UsRadarLayerType.UNIFIED_ALL) {
                                when (language) {
                                    AppLanguage.JAPANESE -> "全米 NEXRAD / HRRR (実況・外挿・予報)"
                                    AppLanguage.ENGLISH -> "US NEXRAD / HRRR (Obs, Extrap, Fcst)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR (实况・外推・预报)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR (實況・外推・預報)"
                                }
                            } else {
                                selectedUsLayer.displayName(language)
                            }
                        }
                        RadarDataSource.RAIN_VIEWER -> {
                            when (language) {
                                AppLanguage.JAPANESE -> "世界レーダー (RainViewer)"
                                AppLanguage.ENGLISH -> "Global Radar (RainViewer)"
                                AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 (RainViewer)"
                                AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 (RainViewer)"
                            }
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                if (cityName.isNotBlank()) {
                    Text(
                        text = " • $cityName",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. ボトムフローティングコントロール群 (影なし・枠なし・派手な Expressive Color)
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = expressiveCardContainerColor,
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 【上段】
                // 左端：レイヤー選択ボタン (56dp)
                // 中央：時計ピル (枠の高さ56dpで完全に一致、下に現在時刻も記載)
                // 右端：現在地へ戻るボタン (直径56dpで時計枠の高さと完全に一致)
                val topControlSize = 56.dp
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // 【左端】地図レイヤー選択ボタン（直径56dp）
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = 0.dp,
                        onClick = {
                            isSpeedDialOpen = !isSpeedDialOpen
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(topControlSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (dataSource == RadarDataSource.US_NEXRAD_HRRR) Icons.Rounded.Layers else Icons.Rounded.Add,
                                contentDescription = when (language) {
                                    AppLanguage.JAPANESE -> "地図レイヤー選択"
                                    AppLanguage.ENGLISH -> "Select Map Layer"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "选择地图图层"
                                    AppLanguage.TRADITIONAL_CHINESE -> "選擇地圖圖層"
                                },
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        rotationZ = triggerRotation
                                    }
                            )
                        }
                    }

                    // 【中央】ピル型時刻表示（枠の高さを56dpでボタンと完全に一致！下に現在時刻も記載）
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .height(topControlSize)
                            .m3eBouncyClick(scaleDown = 0.96f) { /* アクション */ }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // フレーム時刻（メイン表示）＋ 相対時間バッジ
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer.isStatic) {
                                        selectedJmaLayer.shortName(language)
                                    } else if (currentTimestamp > 0) {
                                        timeFormatter.format(Date(currentTimestamp * 1000L))
                                    } else {
                                        "--:--"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.onPrimary,
                                    fontSize = 18.sp,
                                    lineHeight = 20.sp
                                )
                                if (relativeTimeLabel.isNotBlank() && !(dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer.isStatic)) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.onPrimary,
                                        shadowElevation = 0.dp
                                    ) {
                                        Text(
                                            text = relativeTimeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = colorScheme.primary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            // その下に現在時刻 ＆ 空間解像度・予報モデル情報
                            val resTag = when {
                                dataSource == RadarDataSource.RAIN_VIEWER -> null
                                dataSource == RadarDataSource.US_NEXRAD_HRRR -> {
                                    when {
                                        currentFrame?.isForecast == true -> "HRRR"
                                        currentFrame?.isExtrapolation == true -> "NEXRAD"
                                        else -> "NEXRAD/MRMS"
                                    }
                                }
                                currentFrame?.isExtrapolation == true -> {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "外挿予測 (0〜60分)"
                                        AppLanguage.ENGLISH -> "Nowcast (0-60m)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "外推预测 (0-60分)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "外推預測 (0-60分)"
                                    }
                                }
                                currentFrame?.isForecast == true -> {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "予報モデル"
                                        AppLanguage.ENGLISH -> "Forecast Model"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "预报模型"
                                        AppLanguage.TRADITIONAL_CHINESE -> "預報模型"
                                    }
                                }
                                else -> currentFrame?.resolutionInfo?.takeIf { it.isNotBlank() }
                            }
                            val nowPrefix = when (language) {
                                AppLanguage.JAPANESE -> "現在"
                                AppLanguage.ENGLISH -> "Now"
                                AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                AppLanguage.TRADITIONAL_CHINESE -> "當前"
                            }
                            Text(
                                text = if (resTag != null) "$nowPrefix $realTimeStr • $resTag" else "$nowPrefix $realTimeStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 【右端】現在地へ戻るボタン（直径56dpで時計枠の高さと完全に一致）
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(topControlSize)
                            .m3eBouncyClick(scaleDown = 0.88f) {
                                recenterTrigger++
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = when (language) {
                                    AppLanguage.JAPANESE -> "現在地へ戻る"
                                    AppLanguage.ENGLISH -> "Current Location"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "回到当前位置"
                                    AppLanguage.TRADITIONAL_CHINESE -> "回到當前位置"
                                },
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // 【中段】横長ピル型タイムラインスライダー (影なし・枠なし)
                if (frames.size > 1 && !(dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer.isStatic)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = sliderCapsuleBgColor,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // USレーダー時は実況・外挿予測・予報の3層セグメントトラックを背景に描画
                                if (dataSource == RadarDataSource.US_NEXRAD_HRRR && frames.size > 1) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .padding(horizontal = 10.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    ) {
                                        val total = frames.size.toFloat()
                                        var currentX = 0f
                                        val w = size.width
                                        val h = size.height

                                        val obsCount = frames.count { !it.isForecast && !it.isExtrapolation }
                                        val extCount = frames.count { it.isExtrapolation }
                                        val fcstCount = frames.count { it.isForecast && !it.isExtrapolation }

                                        if (obsCount > 0) {
                                            val obsWidth = (obsCount / total) * w
                                            drawRect(
                                                color = Color(0xFF2196F3).copy(alpha = 0.5f),
                                                topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                                                size = androidx.compose.ui.geometry.Size(obsWidth, h)
                                            )
                                            currentX += obsWidth
                                        }
                                        if (extCount > 0) {
                                            val extWidth = (extCount / total) * w
                                            drawRect(
                                                color = Color(0xFF00E676).copy(alpha = 0.75f),
                                                topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                                                size = androidx.compose.ui.geometry.Size(extWidth, h)
                                            )
                                            currentX += extWidth
                                        }
                                        if (fcstCount > 0) {
                                            val fcstWidth = (fcstCount / total) * w
                                            drawRect(
                                                color = Color(0xFFB388FF).copy(alpha = 0.65f),
                                                topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                                                size = androidx.compose.ui.geometry.Size(fcstWidth, h)
                                            )
                                        }
                                    }
                                }

                                Slider(
                                    value = currentFrameIndex.toFloat().coerceAtLeast(0f),
                                    onValueChange = { value ->
                                        isPlaying = false
                                        currentFrameIndex = value.toInt()
                                        if (currentFrameIndex in frames.indices) {
                                            currentTimestamp = frames[currentFrameIndex].time
                                        }
                                    },
                                    valueRange = 0f..(frames.size - 1).toFloat().coerceAtLeast(1f),
                                    steps = if (frames.size > 2) frames.size - 2 else 0,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colorScheme.primary,
                                        activeTrackColor = if (dataSource == RadarDataSource.US_NEXRAD_HRRR) Color.Transparent else colorScheme.primary,
                                        inactiveTrackColor = if (dataSource == RadarDataSource.US_NEXRAD_HRRR) Color.Transparent else colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // スライダー下：時間レンジ表示
                        if (dataSource == RadarDataSource.US_NEXRAD_HRRR && frames.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "実況 -2h"
                                        AppLanguage.ENGLISH -> "Obs -2h"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "实况 -2h"
                                        AppLanguage.TRADITIONAL_CHINESE -> "實況 -2h"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在 0m"
                                        AppLanguage.ENGLISH -> "Now 0m"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前 0m"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前 0m"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "外挿 +60m"
                                        AppLanguage.ENGLISH -> "Nowcast +60m"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "外推 +60m"
                                        AppLanguage.TRADITIONAL_CHINESE -> "外推 +60m"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "HRRR +18h",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB388FF),
                                    fontSize = 10.sp
                                )
                            }
                        } else if (dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer == JmaLayerType.HRPNS && pastSize > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "2時間前"
                                        AppLanguage.ENGLISH -> "2h ago"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "2小时前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "2小時前"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在"
                                        AppLanguage.ENGLISH -> "Now"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "+15時間後"
                                        AppLanguage.ENGLISH -> "+15h"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "+15小时后"
                                        AppLanguage.TRADITIONAL_CHINESE -> "+15小時後"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        } else if (dataSource == RadarDataSource.RAIN_VIEWER && frames.size > 1) {
                            // RainViewer: 全世界レーダー（過去2時間の実況を中心とし、長期予報は提供されない）
                            val nowcastCount = radarData?.radar?.nowcast?.size ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "2時間前"
                                        AppLanguage.ENGLISH -> "2h ago"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "2小时前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "2小時前"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                if (nowcastCount > 0) {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "現在"
                                            AppLanguage.ENGLISH -> "Now"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                            AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                    val nowcastMin = nowcastCount * 10
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "+${nowcastMin}分予測"
                                            AppLanguage.ENGLISH -> "+${nowcastMin}m"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "+${nowcastMin}分预测"
                                            AppLanguage.TRADITIONAL_CHINESE -> "+${nowcastMin}分預測"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "1時間前"
                                            AppLanguage.ENGLISH -> "1h ago"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "1小时前"
                                            AppLanguage.TRADITIONAL_CHINESE -> "1小時前"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Normal,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "現在 (最新実況)"
                                            AppLanguage.ENGLISH -> "Now (Live)"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "当前 (最新实况)"
                                            AppLanguage.TRADITIONAL_CHINESE -> "當前 (最新實況)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else if (dataSource == RadarDataSource.JMA_NOWCAST && (selectedJmaLayer == JmaLayerType.THNS || selectedJmaLayer == JmaLayerType.TRNS) && pastSize > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "1時間前"
                                        AppLanguage.ENGLISH -> "1h ago"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "1小时前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "1小時前"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在"
                                        AppLanguage.ENGLISH -> "Now"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "+60分後"
                                        AppLanguage.ENGLISH -> "+60m"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "+60分钟后"
                                        AppLanguage.TRADITIONAL_CHINESE -> "+60分鐘後"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        } else if (frames.size > 1) {
                            val nowcastCount = radarData?.radar?.nowcast?.size ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "過去"
                                        AppLanguage.ENGLISH -> "Past"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "过去"
                                        AppLanguage.TRADITIONAL_CHINESE -> "過去"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在"
                                        AppLanguage.ENGLISH -> "Now"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                if (nowcastCount > 0) {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "予測"
                                            AppLanguage.ENGLISH -> "Forecast"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "预测"
                                            AppLanguage.TRADITIONAL_CHINESE -> "預測"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer.isStatic) {
                    // 静的マップ表示中のインジケータ (影なし・枠なし)
                    Surface(
                        shape = CircleShape,
                        color = sliderCapsuleBgColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (language) {
                                    AppLanguage.JAPANESE -> "公式背景地図レイヤー表示中"
                                    AppLanguage.ENGLISH -> "Base map layer active"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "官方背景地图图层显示中"
                                    AppLanguage.TRADITIONAL_CHINESE -> "官方背景地圖圖層顯示中"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }

                // 【下段】3連ボタン (⏪ / ▶⏸ / ⏩) (影なし・枠なし・押下時に伸びて他が縮む弾性アニメーション)
                if (!(dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer.isStatic) && frames.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .width(playbackContainerWidth)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 左ボタン：前のフレームへ [ ⏪ ]（左端が半円）
                            Surface(
                                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                                color = colorScheme.primaryContainer,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .weight(prevPlaybackWeight)
                                    .fillMaxHeight()
                                    .m3eBouncyClick(scaleDown = 0.88f, overshootScale = 1.06f) {
                                        triggerPlaybackStretch(0)
                                        isPlaying = false
                                        if (currentFrameIndex > 0) {
                                            currentFrameIndex--
                                            currentTimestamp = frames[currentFrameIndex].time
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = when (language) {
                                            AppLanguage.JAPANESE -> "前のフレーム"
                                            AppLanguage.ENGLISH -> "Previous Frame"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "上一帧"
                                            AppLanguage.TRADITIONAL_CHINESE -> "上一訊號"
                                        },
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // 中央ボタン：再生 / 一時停止 [ ▶ / ⏸ ]（角丸四角形）
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.onPrimary,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .weight(playPausePlaybackWeight)
                                    .fillMaxHeight()
                                    .m3eBouncyClick(scaleDown = 0.90f, overshootScale = 1.08f) {
                                        triggerPlaybackStretch(1)
                                        isPlaying = !isPlaying
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
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
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // 右ボタン：次のフレームへ [ ⏩ ]（右端が半円）
                            Surface(
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                                color = colorScheme.primaryContainer,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .weight(nextPlaybackWeight)
                                    .fillMaxHeight()
                                    .m3eBouncyClick(scaleDown = 0.88f, overshootScale = 1.06f) {
                                        triggerPlaybackStretch(2)
                                        isPlaying = false
                                        if (currentFrameIndex < frames.size - 1) {
                                            currentFrameIndex++
                                            currentTimestamp = frames[currentFrameIndex].time
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = when (language) {
                                            AppLanguage.JAPANESE -> "次のフレーム"
                                            AppLanguage.ENGLISH -> "Next Frame"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "下一帧"
                                            AppLanguage.TRADITIONAL_CHINESE -> "下一訊號"
                                        },
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. 縦型降水強度ウィジェット (Frame 95: 折りたたみスリムピル ⇔ Frame 96: 展開カード)
        val showRainIntensityWidget = dataSource == RadarDataSource.RAIN_VIEWER ||
            selectedJmaLayer.id == "hrpns" ||
            selectedJmaLayer.category == JmaLayerCategory.RAINFALL ||
            (selectedJmaLayer.category == JmaLayerCategory.NOWCAST && selectedJmaLayer != JmaLayerType.THNS && selectedJmaLayer != JmaLayerType.TRNS)

        if (showRainIntensityWidget) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = expressiveCardContainerColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 260.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioLowBouncy
                        )
                    )
                    .m3eBouncyClick(scaleDown = if (!isLegendExpanded) 0.94f else 1.0f) {
                        if (!isLegendExpanded) {
                            isLegendExpanded = true
                        }
                    }
            ) {
                val isUsRadar = dataSource == RadarDataSource.US_NEXRAD_HRRR
                val isRainViewer = dataSource == RadarDataSource.RAIN_VIEWER

                if (!isLegendExpanded) {
                    // ==========================================
                    // Frame 95: 折りたたみスリムピル (幅46dp・高さ180dp)
                    // ==========================================
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // カラーバー (上から下へ)
                        Column(
                            modifier = Modifier
                                .width(6.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(3.dp))
                        ) {
                            when {
                                isUsRadar -> {
                                    US_NEXRAD_DBZ_LEVELS.reversed().forEach { level ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .background(level.color)
                                        )
                                    }
                                }
                                isRainViewer -> {
                                    RAIN_VIEWER_DBZ_LEVELS.reversed().forEach { level ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .background(level.color)
                                        )
                                    }
                                }
                                else -> {
                                    RAIN_INTENSITY_LEVELS.forEach { level ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .background(level.color)
                                        )
                                    }
                                }
                            }
                        }

                        // 代表数値ラベル (上から下へ4点等間隔)
                        val scaleLabels = when {
                            isUsRadar -> listOf("65", "45", "25", "5")
                            isRainViewer -> listOf("60", "45", "25", "15")
                            else -> listOf("80", "10", "1", "0.1")
                        }
                        Column(
                            modifier = Modifier.height(160.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            scaleLabels.forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // 展開時カード (タップで開く詳細インラインガイド)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .width(if (isUsRadar || isRainViewer) 210.dp else 200.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ヘッダー行: タイトル ＆ 閉じるボタン
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUsRadar || isRainViewer) {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "雨雲反射強度 (dBZ)"
                                        AppLanguage.ENGLISH -> "Radar Reflectivity (dBZ)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "雷达反射强度 (dBZ)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "雷達反射強度 (dBZ)"
                                    }
                                } else {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "雨の強さ (mm/h)"
                                        AppLanguage.ENGLISH -> "Rain Intensity (mm/h)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "降雨强度 (mm/h)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "降雨強度 (mm/h)"
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable { isLegendExpanded = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = when (language) {
                                        AppLanguage.JAPANESE -> "閉じる"
                                        AppLanguage.ENGLISH -> "Close"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                    },
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // カラーバー ＆ 区分ラベル行（高さ同期）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when {
                                isUsRadar -> {
                                    // 垂直カラーバー (高さ154dp = 7行 × 22dp)
                                    Column(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(154.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    ) {
                                        US_NEXRAD_DBZ_LEVELS.reversed().forEach { level ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(level.color)
                                            )
                                        }
                                    }

                                    // 7区分の名称とdBZラベル
                                    Column(
                                        modifier = Modifier
                                            .height(154.dp)
                                            .weight(1f),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        US_NEXRAD_DBZ_LEVELS.reversed().forEach { level ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = level.intensityName(language),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface,
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${level.label} dBZ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontSize = 8.5.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                                isRainViewer -> {
                                    // 垂直カラーバー (高さ154dp = 7行 × 22dp)
                                    Column(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(154.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    ) {
                                        RAIN_VIEWER_DBZ_LEVELS.reversed().forEach { level ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(level.color)
                                            )
                                        }
                                    }

                                    // 7区分の名称とdBZラベル
                                    Column(
                                        modifier = Modifier
                                            .height(154.dp)
                                            .weight(1f),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        RAIN_VIEWER_DBZ_LEVELS.reversed().forEach { level ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = level.intensityName(language),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface,
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${level.label} dBZ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontSize = 8.5.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // 垂直カラーバー (高さ176dp = 8行 × 22dp)
                                    Column(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(176.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    ) {
                                        RAIN_INTENSITY_LEVELS.forEach { level ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(level.color)
                                            )
                                        }
                                    }

                                    // 8区分の名称とmm/hラベル
                                    Column(
                                        modifier = Modifier
                                            .height(176.dp)
                                            .weight(1f),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        RAIN_INTENSITY_LEVELS.forEach { level ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = level.name(language),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface,
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = level.rangeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontSize = 8.5.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // フッター: 詳細ガイドシートを開くリンク
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isLegendExpanded = false
                                    showIntensitySheet = true
                                },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = when (language) {
                                    AppLanguage.JAPANESE -> "詳細ガイド ❯"
                                    AppLanguage.ENGLISH -> "Detailed Guide ❯"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "详细指南 ❯"
                                    AppLanguage.TRADITIONAL_CHINESE -> "詳細指南 ❯"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. 【最前面】スピードダイヤル式レイヤー選択メニュー（ボトムコントロールの手前に描画して隠れを完全防止）
        // 左端ボタンの中心から上に向かってバネ弾性で飛び出す「ボタンから出てくる」アニメーション！
        if (isSpeedDialOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isSpeedDialOpen = false
                    }
            )
            val activeSpeedDialItems = when (dataSource) {
                RadarDataSource.JMA_NOWCAST -> bottomToTopItems
                RadarDataSource.US_NEXRAD_HRRR -> usSpeedDialItems
                RadarDataSource.RAIN_VIEWER -> rainViewerSpeedDialItems
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 280.dp) // ボトムカードのすぐ上に配置
                    .navigationBarsPadding()
            ) {
                // 上から下（画面の上からボタンへ向かって並ぶ）
                val topToBottomList = remember(activeSpeedDialItems) { activeSpeedDialItems.reversed() }
                topToBottomList.forEachIndexed { revIndex, item ->
                    val fromBottomIndex = topToBottomList.size - 1 - revIndex
                    AnimatedSpeedDialItem(
                        index = fromBottomIndex,
                        isOpen = isSpeedDialOpen,
                        item = item,
                        bgColor = speedDialItemBgColor
                    )
                }
            }
        }

        // 6. Material 3 Expressive 全レイヤー詳細選択ボトムシート (絵文字全廃・影なし枠なし)
        if (showFullLayerSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showFullLayerSheet = false },
                sheetState = sheetState,
                containerColor = expressiveCardContainerColor,
                contentColor = colorScheme.onSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    // シートヘッダー
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (dataSource == RadarDataSource.US_NEXRAD_HRRR) {
                                        when (language) {
                                            AppLanguage.JAPANESE -> "全米気象・予報マップ候補"
                                            AppLanguage.ENGLISH -> "US Weather & Forecast Maps"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "全美气象与预报图层"
                                            AppLanguage.TRADITIONAL_CHINESE -> "全美氣象與預報圖層"
                                        }
                                    } else {
                                        when (language) {
                                            AppLanguage.JAPANESE -> "気象地図レイヤー選択"
                                            AppLanguage.ENGLISH -> "Weather Map Layers"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "气象地图图层选择"
                                            AppLanguage.TRADITIONAL_CHINESE -> "氣象地圖圖層選擇"
                                        }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (dataSource == RadarDataSource.US_NEXRAD_HRRR) {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "NOAA / NEXRAD / HRRRシミュレーション (全12種類)"
                                        AppLanguage.ENGLISH -> "NOAA / NEXRAD / HRRR Simulation (12 Layers)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "NOAA / NEXRAD / HRRR 模拟 (全12种)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "NOAA / NEXRAD / HRRR 模擬 (全12種)"
                                    }
                                } else {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "気象庁公式配信タイル (全24種類)"
                                        AppLanguage.ENGLISH -> "JMA Official Tiles (24 Layers)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅官方切片 (全24种)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳官方圖磚 (全24種)"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showFullLayerSheet = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = when (language) {
                                    AppLanguage.JAPANESE -> "閉じる"
                                    AppLanguage.ENGLISH -> "Close"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                },
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (dataSource == RadarDataSource.US_NEXRAD_HRRR) {
                        // US用カテゴリチップ横スクロール列 (4カテゴリ)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            items(UsLayerCategory.entries) { cat ->
                                val isSelected = sheetUsCategory == cat
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHighest,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.m3eBouncyClick(scaleDown = 0.92f) {
                                        sheetUsCategory = cat
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = cat.displayName(language),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // US選択中カテゴリのレイヤー一覧 (カードリスト)
                        val usLayersInSelectedCategory = remember(sheetUsCategory) {
                            UsRadarLayerType.entries.filter { it.category == sheetUsCategory }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp)
                                .height(360.dp)
                        ) {
                            items(usLayersInSelectedCategory) { layer ->
                                val isCurrent = selectedUsLayer == layer
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCurrent) {
                                        colorScheme.primaryContainer
                                    } else {
                                        colorScheme.surfaceContainerHighest
                                    },
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .m3eBouncyClick(scaleDown = 0.96f) {
                                            selectedUsLayer = layer
                                            showFullLayerSheet = false
                                        }
                                 ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = layer.displayName(language),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) colorScheme.primary else colorScheme.onSurface
                                                )
                                                // 解像度バッジ
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isCurrent) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceContainerHigh
                                                ) {
                                                    Text(
                                                        text = layer.resolution,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isCurrent) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                // 更新間隔バッジ
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isCurrent) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceContainerHigh
                                                ) {
                                                    val updateText = when (language) {
                                                        AppLanguage.JAPANESE -> "${layer.updateIntervalMinutes}分更新"
                                                        AppLanguage.ENGLISH -> "${layer.updateIntervalMinutes}m update"
                                                        AppLanguage.SIMPLIFIED_CHINESE -> "${layer.updateIntervalMinutes}分更新"
                                                        AppLanguage.TRADITIONAL_CHINESE -> "${layer.updateIntervalMinutes}分更新"
                                                    }
                                                    Text(
                                                        text = updateText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isCurrent) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = layer.description(language),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = colorScheme.primary,
                                                shadowElevation = 0.dp,
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = colorScheme.onPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 日本 気象庁用カテゴリチップ横スクロール列 (8カテゴリ、絵文字なし)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            items(JmaLayerCategory.entries) { cat ->
                                val isSelected = sheetCategory == cat
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHighest,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.m3eBouncyClick(scaleDown = 0.92f) {
                                        sheetCategory = cat
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = cat.displayName(language),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // 選択中カテゴリのレイヤー一覧 (カードリスト、絵文字なし、影なし枠なし)
                        val layersInSelectedCategory = remember(sheetCategory) {
                            JmaLayerType.entries.filter { it.category == sheetCategory }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp)
                                .height(340.dp)
                        ) {
                            items(layersInSelectedCategory) { layer ->
                                val isCurrent = selectedJmaLayer == layer
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCurrent) {
                                        colorScheme.primaryContainer
                                    } else {
                                        colorScheme.surfaceContainerHighest
                                    },
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .m3eBouncyClick(scaleDown = 0.96f) {
                                            switchJmaLayer(layer)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = layer.displayName(language),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) colorScheme.primary else colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = layer.description(language),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = colorScheme.primary,
                                                shadowElevation = 0.dp,
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = colorScheme.onPrimary,
                                                        modifier = Modifier.size(16.dp)
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
        }

        // 7. Material 3 Expressive 雨の強さと降水量・予報一覧モーダルシート (100%ソリッド)
        if (showIntensitySheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showIntensitySheet = false },
                sheetState = sheetState,
                containerColor = expressiveCardContainerColor,
                contentColor = colorScheme.onSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    // シートヘッダー
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = when (dataSource) {
                                    RadarDataSource.US_NEXRAD_HRRR -> when (language) {
                                        AppLanguage.JAPANESE -> "全米レーダー dBZ 反射強度基準"
                                        AppLanguage.ENGLISH -> "US Radar dBZ Reflectivity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "全美雷达 dBZ 反射强度标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "全美雷達 dBZ 反射強度標準"
                                    }
                                    RadarDataSource.RAIN_VIEWER -> when (language) {
                                        AppLanguage.JAPANESE -> "世界レーダー dBZ 反射強度基準"
                                        AppLanguage.ENGLISH -> "Global Radar dBZ Reflectivity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 dBZ 反射强度标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 dBZ 反射強度標準"
                                    }
                                    else -> when (language) {
                                        AppLanguage.JAPANESE -> "雨の強さと降水量の目安"
                                        AppLanguage.ENGLISH -> "Rain Intensity & Precipitation Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "降雨强度与降水量等级"
                                        AppLanguage.TRADITIONAL_CHINESE -> "降雨強度與降水量等級"
                                    }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (dataSource) {
                                    RadarDataSource.US_NEXRAD_HRRR -> when (language) {
                                        AppLanguage.JAPANESE -> "NOAA / NEXRAD / MRMS 標準カラーパレット (dBZ)"
                                        AppLanguage.ENGLISH -> "NOAA / NEXRAD / MRMS Standard Color Palette (dBZ)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "NOAA / NEXRAD / MRMS 标准色盘 (dBZ)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "NOAA / NEXRAD / MRMS 標準色盤 (dBZ)"
                                    }
                                    RadarDataSource.RAIN_VIEWER -> when (language) {
                                        AppLanguage.JAPANESE -> "RainViewer Universal Blue 標準カラーパレット (dBZ)"
                                        AppLanguage.ENGLISH -> "RainViewer Universal Blue Color Palette (dBZ)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "RainViewer Universal Blue 标准色盘 (dBZ)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "RainViewer Universal Blue 標準色盤 (dBZ)"
                                    }
                                    else -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 降水ナウキャスト色基準・予報一覧"
                                        AppLanguage.ENGLISH -> "JMA Precipitation Nowcast Color Scale & Forecast"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 降水即时预报颜色标准・预报列表"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 降水即時預報顏色標準・預報列表"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showIntensitySheet = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = when (language) {
                                    AppLanguage.JAPANESE -> "閉じる"
                                    AppLanguage.ENGLISH -> "Close"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                },
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 段階の降水量カテゴリ一覧
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (dataSource) {
                            RadarDataSource.US_NEXRAD_HRRR -> {
                                US_NEXRAD_DBZ_LEVELS.reversed().forEach { level ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colorScheme.surfaceContainerHigh,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 左側：色スウォッチバッジ
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = level.color,
                                                shadowElevation = 0.dp,
                                                modifier = Modifier
                                                    .width(72.dp)
                                                    .height(44.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${level.label} dBZ",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (level.dbzValue in listOf(25, 35)) Color(0xFF1C1B1F) else Color.White,
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // 右側：名前・体感説明
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "${level.intensityName(language)} (${level.dbzValue} dBZ)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = level.description(language),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            RadarDataSource.RAIN_VIEWER -> {
                                RAIN_VIEWER_DBZ_LEVELS.reversed().forEach { level ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colorScheme.surfaceContainerHigh,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 左側：色スウォッチバッジ
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = level.color,
                                                shadowElevation = 0.dp,
                                                modifier = Modifier
                                                    .width(72.dp)
                                                    .height(44.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${level.label} dBZ",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (level.dbzValue in listOf(15, 35)) Color(0xFF1C1B1F) else Color.White,
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // 右側：名前・体感説明
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "${level.intensityName(language)} (${level.dbzValue} dBZ)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = level.description(language),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                RAIN_INTENSITY_LEVELS.forEach { level ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colorScheme.surfaceContainerHigh,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 左側：色スウォッチバッジ
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = level.color,
                                                shadowElevation = 0.dp,
                                                modifier = Modifier
                                                    .width(72.dp)
                                                    .height(44.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = level.rangeLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = level.onColor,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // 右側：名前・体感説明・注意事項
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = level.name(language),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = level.impact(language),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = level.warning(language),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 閉じるボタン
                        Button(
                            onClick = { showIntensitySheet = false },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = when (language) {
                                    AppLanguage.JAPANESE -> "閉じる"
                                    AppLanguage.ENGLISH -> "Close"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // 8. ローディングインジケータ
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // レンダリングソース選択ダイアログ (SingleChoiceDialog)
        if (showSourceDialog) {
            SingleChoiceDialog(
                title = when (language) {
                    AppLanguage.JAPANESE -> "レーダー提供元の選択"
                    AppLanguage.ENGLISH -> "Select Radar Source"
                    AppLanguage.SIMPLIFIED_CHINESE -> "选择雷达数据源"
                    AppLanguage.TRADITIONAL_CHINESE -> "選擇雷達資料來源"
                },
                options = listOf(
                    RadarSourceMode.AUTO to when (language) {
                        AppLanguage.JAPANESE -> "自動判別 (位置・国名に連動)"
                        AppLanguage.ENGLISH -> "Auto (Based on location)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "自动判别 (根据位置)"
                        AppLanguage.TRADITIONAL_CHINESE -> "自動判別 (根據位置)"
                    },
                    RadarSourceMode.US_NEXRAD_HRRR to when (language) {
                        AppLanguage.JAPANESE -> "全米 NEXRAD / HRRR (実況・外挿予測・予報)"
                        AppLanguage.ENGLISH -> "US NEXRAD / HRRR (Obs, Nowcast, Forecast)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR (实况・外推・预报)"
                        AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR (實況・外推・預報)"
                    },
                    RadarSourceMode.JMA to when (language) {
                        AppLanguage.JAPANESE -> "気象庁 高解像度降水ナウキャスト"
                        AppLanguage.ENGLISH -> "JMA High-Resolution Precipitation Nowcast"
                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 高分辨率降雨即时预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 高解析度降雨即時預報"
                    },
                    RadarSourceMode.RAIN_VIEWER to when (language) {
                        AppLanguage.JAPANESE -> "世界レーダー (RainViewer)"
                        AppLanguage.ENGLISH -> "Global Radar (RainViewer)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 (RainViewer)"
                        AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 (RainViewer)"
                    }
                ),
                selectedOption = sourceMode,
                onOptionSelected = { newMode ->
                    repo.setSourceMode(newMode)
                    showSourceDialog = false
                },
                onDismiss = { showSourceDialog = false },
                cancelText = when (language) {
                    AppLanguage.JAPANESE -> "キャンセル"
                    AppLanguage.ENGLISH -> "Cancel"
                    AppLanguage.SIMPLIFIED_CHINESE -> "取消"
                    AppLanguage.TRADITIONAL_CHINESE -> "取消"
                }
            )
        }
    }
}

/**
 * スピードダイヤル用アイテムデータ
 */
private data class SpeedDialItemData(
    val title: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

/**
 * ボタンの中から飛び出すエクスプレッション弾性アニメーション付きスピードダイヤルピルUI
 * （非表示時は左下のボタンの位置に待機し、展開時に下から順にポヨンと飛び出す！）
 */
@Composable
private fun AnimatedSpeedDialItem(
    index: Int,
    isOpen: Boolean,
    item: SpeedDialItemData,
    bgColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    val initialYOffset = (index + 1) * 44f

    val animReveal = remember { Animatable(0f) }
    val animScale = remember { Animatable(0.85f) }
    val animAlpha = remember { Animatable(0f) }
    val animTransY = remember { Animatable(initialYOffset) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            // ユーザー要望：マップ選択の時のメニューのアニメーション速度をもっと早くして
            // 待機ディレイを短縮し（10ms）、超高速な弾性バネでキビキビと瞬時に展開
            delay(index * 10L)
            launch {
                animAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 40)
                )
            }
            launch {
                animScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = 1200f
                    )
                )
            }
            launch {
                // 左端から右へ高速にニュルッと広がるリビールアニメーション（高剛性バネ）
                animReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = 1100f
                    )
                )
            }
            launch {
                // 下から上への移動（高剛性バネで瞬時に上昇）
                animTransY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = 1200f
                    )
                )
            }
        } else {
            // 閉じるときも高速に収束
            launch { animAlpha.animateTo(0f, tween(35)) }
            launch { animScale.animateTo(0.85f, tween(45)) }
            launch { animReveal.animateTo(0f, tween(45)) }
            launch { animTransY.animateTo(initialYOffset, tween(45)) }
        }
    }

    if (animAlpha.value > 0.01f) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = animScale.value
                    scaleY = animScale.value
                    translationY = animTransY.value
                    alpha = animAlpha.value
                    clip = true
                    shape = object : Shape {
                        override fun createOutline(
                            size: Size,
                            layoutDirection: LayoutDirection,
                            density: Density
                        ): Outline {
                            val progress = animReveal.value.coerceIn(0f, 1.05f)
                            val currentWidth = (size.width * progress).coerceAtLeast(0f)
                            val maxRadius = size.height / 2f
                            val radius = maxRadius.coerceAtMost(currentWidth / 2f)
                            val corner = CornerRadius(radius, radius)
                            return Outline.Rounded(
                                RoundRect(
                                    left = 0f,
                                    top = 0f,
                                    right = currentWidth,
                                    bottom = size.height,
                                    cornerRadius = corner
                                )
                            )
                        }
                    }
                }
        ) {
            Surface(
                shape = CircleShape,
                color = bgColor, // ボタンの色（colorScheme.primary）と完全に一致！
                shadowElevation = 0.dp, // 影なし！
                modifier = Modifier.m3eBouncyClick(scaleDown = 0.90f, overshootScale = 1.08f) { item.onClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = if (item.isSelected) 14.dp else 18.dp,
                        vertical = 10.dp
                    )
                ) {
                    if (item.isSelected) {
                        // 選択中アイテム：白サークル＋プライマリ色チェックマーク
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.onPrimary,
                            shadowElevation = 0.dp,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (item.isSelected) FontWeight.Black else FontWeight.Bold,
                        color = colorScheme.onPrimary,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
