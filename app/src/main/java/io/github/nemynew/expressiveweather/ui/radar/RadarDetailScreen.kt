package io.github.nemynew.expressiveweather.ui.radar

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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.Air
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
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.nemynew.expressiveweather.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.JmaLayerCategory
import io.github.nemynew.expressiveweather.data.JmaLayerType
import io.github.nemynew.expressiveweather.data.isAllowedJapanRadarLayer
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.JmaTyphoonIntensity
import io.github.nemynew.expressiveweather.data.JmaTyphoonItem
import io.github.nemynew.expressiveweather.data.JmaTyphoonRepository
import io.github.nemynew.expressiveweather.data.RadarColorScheme
import io.github.nemynew.expressiveweather.data.RadarDataSource
import io.github.nemynew.expressiveweather.data.RadarFrame
import io.github.nemynew.expressiveweather.data.RadarProviderRegistry
import io.github.nemynew.expressiveweather.data.RadarRepository
import io.github.nemynew.expressiveweather.data.RadarSourceMode
import io.github.nemynew.expressiveweather.data.RainViewerResponse
import io.github.nemynew.expressiveweather.data.UsRadarLayerFilter
import io.github.nemynew.expressiveweather.data.UsLayerCategory
import io.github.nemynew.expressiveweather.data.UsRadarLayerType
import io.github.nemynew.expressiveweather.data.US_NEXRAD_DBZ_LEVELS
import io.github.nemynew.expressiveweather.data.RAIN_VIEWER_DBZ_LEVELS
import io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan
import io.github.nemynew.expressiveweather.data.isLocationOrCountryInUnitedStates
import io.github.nemynew.expressiveweather.theme.LocalDarkTheme
import io.github.nemynew.expressiveweather.ui.components.m3eBouncyClick
import io.github.nemynew.expressiveweather.ui.screens.SingleChoiceDialog
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

/**
 * レーダー色分け指標（凡例）の種別
 */
enum class RadarLegendKind {
    US_NEXRAD,
    RAIN_VIEWER,
    RAIN_INTENSITY,
    LIGHTNING,
    KIKIKURU,
    SNOW_DEPTH,
    SNOWFALL_3H,
    SNOWFALL_24H
}

/**
 * 汎用レーダー色分け指標の区分データモデル
 */
data class RadarLegendLevelItem(
    val rangeLabel: String,
    val nameJa: String,
    val color: Color,
    val onColor: Color = Color.White,
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

val LIGHTNING_ACTIVITY_LEVELS = listOf(
    RadarLegendLevelItem(
        rangeLabel = "活動度 4",
        nameJa = "激しい雷",
        color = Color(0xFFB40068),
        onColor = Color.White,
        impactJa = "猛烈な発雷。激しい雷雨・突風・降雹の危険が極めて高い。",
        warningJa = "重大な落雷事故のおそれ。直ちに頑丈な建物や車内など安全な場所へ避難。",
        nameEn = "Violent Lightning",
        impactEn = "Intense lightning activity; severe squalls and hail very likely.",
        warningEn = "Extreme lightning hazard. Immediately seek shelter inside sturdy buildings or vehicles.",
        nameZhCn = "剧烈雷电",
        impactZhCn = "雷电活动极为猛烈，极易伴随短时雷暴大风与冰雹灾害。",
        warningZhCn = "雷击风险极高，请立即前往坚固建筑物或密闭车厢内避险。",
        nameZhTw = "劇烈雷電",
        impactZhTw = "雷電活動極為猛烈，極易伴隨短時雷暴大風與冰雹災害。",
        warningZhTw = "雷擊風險極高，請立即前往堅固建築物或密閉車廂內避險。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "活動度 3",
        nameJa = "雷",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "落雷が多数発生している。発雷の危険性が高い。",
        warningJa = "落雷の危険あり。周囲の天候急変に警戒し、速やかに屋内へ退避。",
        nameEn = "Active Lightning",
        impactEn = "Frequent lightning strikes observed; high risk of lightning hazard.",
        warningEn = "High risk of lightning strikes. Move indoors promptly.",
        nameZhCn = "强雷电",
        impactZhCn = "已监测到多起落雷，雷电发生频率高。",
        warningZhCn = "有雷击危险，请警惕周围天气突变并尽快退避室内。",
        nameZhTw = "強雷電",
        impactZhTw = "已監測到多起落雷，雷電發生頻率高。",
        warningZhTw = "有雷擊危險，請警惕周圍天氣突變並儘快退避室內。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "活動度 2",
        nameJa = "雷の可能性",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "1時間以内に落雷の可能性が高い。雷雲が活発に発達中。",
        warningJa = "屋外活動時は空の様子に注意し、いつでも避難できる態勢を準備。",
        nameEn = "Lightning Likely",
        impactEn = "High probability of lightning within 1 hour; thunderclouds actively developing.",
        warningEn = "Monitor sky conditions during outdoor activities; prepare to shelter.",
        nameZhCn = "有雷电可能",
        impactZhCn = "未来1小时内发生落雷概率高，雷暴云正在持续发展。",
        warningZhCn = "户外活动需密切注意云层变化，做好随时避雷准备。",
        nameZhTw = "有雷電可能",
        impactZhTw = "未來1小時內發生落雷機率高，雷暴雲正在持續發展。",
        warningZhTw = "戶外活動需密切注意雲層變化，做好隨時避雷準備。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "活動度 1",
        nameJa = "雷の兆候",
        color = Color(0xFF218CFF),
        onColor = Color.White,
        impactJa = "1時間以内に発雷の可能性がある（雷雲の兆候）。",
        warningJa = "急な強い雨や突風など、天候の急変に留意。",
        nameEn = "Signs of Lightning",
        impactEn = "Potential for lightning development within 1 hour.",
        warningEn = "Be aware of sudden showers or gusts associated with developing clouds.",
        nameZhCn = "雷电征兆",
        impactZhCn = "未来1小时内可能伴随雷电发生（初生雷云）。",
        warningZhCn = "防范局地突发阵雨、雷阵雨及短时阵风。",
        nameZhTw = "雷電徵兆",
        impactZhTw = "未來1小時內可能伴隨雷電發生（初生雷雲）。",
        warningZhTw = "防範局地突發陣雨、雷陣雨及短時陣風。"
    )
)

val KIKIKURU_RISK_LEVELS = listOf(
    RadarLegendLevelItem(
        rangeLabel = "Lv 5",
        nameJa = "災害切迫",
        color = Color(0xFF3C383E),
        onColor = Color(0xFFFFD700),
        impactJa = "すでに災害が発生または切迫している極めて危険な状況。",
        warningJa = "命の危険。ただちに身の安全を確保してください（命を守る最善の行動）。",
        nameEn = "Emergency (Level 5)",
        impactEn = "Disaster has already occurred or is imminent; extreme life-threatening risk.",
        warningEn = "Take immediate life-saving action; secure personal safety without delay.",
        nameZhCn = "灾害切迫 (5级)",
        impactZhCn = "灾害已经发生或迫在眉睫，面临严重生命威胁。",
        warningZhCn = "生命危在旦夕！请立即采取最有利于保全生命的避险行动。",
        nameZhTw = "災害切迫 (5級)",
        impactZhTw = "災害已經發生或迫在眉睫，面臨嚴重生命威脅。",
        warningZhTw = "生命危在旦夕！請立即採取最有利於保全生命的避險行動。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 4",
        nameJa = "危険",
        color = Color(0xFFB84FB3),
        onColor = Color.White,
        impactJa = "重大な災害が発生する可能性が非常に高く、全員避難が必要。",
        warningJa = "警戒レベル4相当。危険な場所から全員速やかに避難してください。",
        nameEn = "Danger (Level 4)",
        impactEn = "Very high probability of severe disaster; all residents must evacuate.",
        warningEn = "Equivalent to Alert Level 4. Evacuate dangerous zones immediately.",
        nameZhCn = "危险 (4级)",
        impactZhCn = "极可能发生重大灾害，危险区域内所有人员必须紧急撤离。",
        warningZhCn = "相当于警戒级别4。危险地带所有人员请迅速撤离避难。",
        nameZhTw = "危險 (4級)",
        impactZhTw = "極可能發生重大災害，危險區域內所有人員必須緊急撤離。",
        warningZhTw = "相當於警戒級別4。危險地帶所有人員請迅速撤離避難。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 3",
        nameJa = "警戒",
        color = Color(0xFFFF5252),
        onColor = Color.White,
        impactJa = "警報基準を超過。高齢者等の避難行動が必要とされる状況。",
        warningJa = "警戒レベル3相当。高齢者や避難に時間を要する方は避難を開始。",
        nameEn = "Warning (Level 3)",
        impactEn = "Exceeds warning criteria; evacuation of vulnerable individuals required.",
        warningEn = "Equivalent to Alert Level 3. Seniors and vulnerable people should evacuate.",
        nameZhCn = "警戒 (3级)",
        impactZhCn = "超过灾害警报标准，易受灾群体需要提前开始转移撤离。",
        warningZhCn = "相当于警戒级别3。老年人及行动不便者应尽早从危险地带避难。",
        nameZhTw = "警戒 (3級)",
        impactZhTw = "超過災害警報標準，易受災群體需要提前開始轉移撤離。",
        warningZhTw = "相當於警戒級別3。老年人及行動不便者應儘早從危險地帶避難。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 2",
        nameJa = "注意",
        color = Color(0xFFFDD835),
        onColor = Color(0xFF212121),
        impactJa = "注意報基準を超過。今後の気象推移に警戒が必要。",
        warningJa = "警戒レベル2相当。ハザードマップで避難経路や避難先を確認。",
        nameEn = "Advisory (Level 2)",
        impactEn = "Exceeds advisory criteria; preparedness checks recommended.",
        warningEn = "Equivalent to Alert Level 2. Check hazard maps and evacuation routes.",
        nameZhCn = "注意 (2级)",
        impactZhCn = "达到气象注意报标准，需要提高警惕注意后续变化。",
        warningZhCn = "相当于警戒级别2。确认防灾避险路线及应急避难场所。",
        nameZhTw = "注意 (2級)",
        impactZhTw = "達到氣象注意報標準，需要提高警惕注意後續變化。",
        warningZhTw = "相當於警戒級別2。確認防災避險路線及應急避難場所。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 1",
        nameJa = "今後の情報等",
        color = Color(0xFFB0BEC5),
        onColor = Color(0xFF212121),
        impactJa = "現時点で災害発生の危険度は低いが、今後の雨に留意。",
        warningJa = "最新の防災気象情報や今後の推移を確認してください。",
        nameEn = "Caution (Level 1)",
        impactEn = "Currently low danger, but monitor evolving weather conditions.",
        warningEn = "Stay tuned to the latest weather bulletins and updates.",
        nameZhCn = "早期注意 (1级)",
        impactZhCn = "当前危险度处于低位，但需关注未来降水发展。",
        warningZhCn = "请持续留意最新发布的气象灾害防范预报信息。",
        nameZhTw = "早期注意 (1級)",
        impactZhTw = "當前危險度處於低位，但需關注未來降水發展。",
        warningZhTw = "請持續留意最新發布的氣象災害防範預報資訊。"
    )
)

val SNOW_DEPTH_LEVELS = listOf(
    RadarLegendLevelItem(
        rangeLabel = "200cm〜",
        nameJa = "200cm以上",
        color = Color(0xFF78006E),
        onColor = Color.White,
        impactJa = "記録的豪雪。建物の倒壊や集落の孤立、交通完全麻痺の危機。",
        warningJa = "大規模雪崩・屋根の倒壊に厳重警戒。不要不急の外出は厳禁。",
        nameEn = "200cm or more",
        impactEn = "Record catastrophic snowfall; threat of structural collapse and isolation.",
        warningEn = "Extreme avalanche danger. Avoid all unnecessary travel.",
        nameZhCn = "200cm以上",
        impactZhCn = "历史罕见特大暴雪，建筑物压塌风险极高，交通全线瘫痪。",
        warningZhCn = "严密防范大型雪崩与房屋倒塌，切勿盲目外出。",
        nameZhTw = "200cm以上",
        impactZhTw = "歷史罕見特大暴雪，建築物壓塌風險極高，交通全線癱瘓。",
        warningZhTw = "嚴密防範大型雪崩與房屋倒塌，切勿盲目外出。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "100〜200",
        nameJa = "100〜200cm",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "極めて深い積雪。屋根雪下ろしの危険や大規模な通行止めが発生。",
        warningJa = "雪崩や落雪事故、車の立ち往生に厳重警戒。",
        nameEn = "100 to 200cm",
        impactEn = "Very heavy snowpack; widespread road closures and roof-clearing hazards.",
        warningEn = "Alert for avalanches, roof snow shed, and vehicle entrapment.",
        nameZhCn = "100至200cm",
        impactZhCn = "极深积雪，多条主干道路封闭受阻，房顶除雪作业风险大。",
        warningZhCn = "高度警惕雪崩滑雪、屋顶坠雪伤人及车辆严重滞留搁浅。",
        nameZhTw = "100至200cm",
        impactZhTw = "極深積雪，多條主幹道路封閉受阻，房頂除雪作業風險大。",
        warningZhTw = "高度警惕雪崩滑雪、屋頂墜雪傷人及車輛嚴重滯留擱淺。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "50〜100",
        nameJa = "50〜100cm",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "まとまった大雪。車の立ち往生や路面凍結によるスリップが多発。",
        warningJa = "チェーンや冬用タイヤ等の冬装備必須。歩行中の転倒に注意。",
        nameEn = "50 to 100cm",
        impactEn = "Substantial heavy snow; frequent vehicle stalling and severe icy conditions.",
        warningEn = "Winter tires/chains required. Watch for falls on slick surfaces.",
        nameZhCn = "50至100cm",
        impactZhCn = "成片大雪积雪，车辆极易受困打滑，路面积雪严重。",
        warningZhCn = "机动车必须装配雪地胎或防滑链，行人谨防路面湿滑摔倒。",
        nameZhTw = "50至100cm",
        impactZhTw = "成片大雪積雪，車輛極易受困打滑，路面積雪嚴重。",
        warningZhTw = "機動車必須裝配雪地胎或防滑鏈，行人謹防路面濕滑摔倒。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "20〜50",
        nameJa = "20〜50cm",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "足元がすっぽり埋まる積雪。交通ダイヤの乱れや除雪が必要。",
        warningJa = "スリップ事故や歩行困難に注意。時間に余裕をもった行動を。",
        nameEn = "20 to 50cm",
        impactEn = "Deep snow covering feet; transit delays and snow clearing needed.",
        warningEn = "Drive cautiously to prevent skidding. Allow extra commute time.",
        nameZhCn = "20至50cm",
        impactZhCn = "积雪可没过脚踝至小腿，公共交通出现延误，需要进行除雪。",
        warningZhCn = "谨防车辆打滑失控，出行预留充足时间。",
        nameZhTw = "20至50cm",
        impactZhTw = "積雪可沒過腳踝至小腿，公共交通出現延誤，需要進行除雪。",
        warningZhTw = "謹防車輛打滑失控，出行預留充足時間。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "5〜20",
        nameJa = "5〜20cm",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "道路一面に積雪。靴が埋まり、冬用タイヤ未装着車の走行不能。",
        warningJa = "ノーマルタイヤでの走行は極めて危険。凍結路面に注意。",
        nameEn = "5 to 20cm",
        impactEn = "Full ground coverage; non-winter equipped vehicles will get stuck.",
        warningEn = "Do not drive on summer tires. Beware of hidden black ice.",
        nameZhCn = "5至20cm",
        impactZhCn = "路面全面覆雪，未更换雪地胎的车辆极易受阻抛锚。",
        warningZhCn = "严禁使用普通四季胎在雪道行驶，注意桥面冻结。",
        nameZhTw = "5至20cm",
        impactZhTw = "路面全面覆雪，未更換雪地胎的車輛極易受阻拋錨。",
        warningZhTw = "嚴禁使用普通四季胎在雪道行駛，注意橋面凍結。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "1〜5",
        nameJa = "1〜5cm",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "うっすらと白く積もる程度。路面凍結によるスリップに注意。",
        warningJa = "日陰や橋の上の路面凍結（ブラックアイスバーン）に注意。",
        nameEn = "1 to 5cm",
        impactEn = "Light white snow dusting; slick spots and black ice hazard.",
        warningEn = "Watch for black ice on bridges and shaded pavement.",
        nameZhCn = "1至5cm",
        impactZhCn = "薄薄一层白雪覆盖，局部路面结冰湿滑。",
        warningZhCn = "注意高架桥梁、背阴路段暗冰防滑。",
        nameZhTw = "1至5cm",
        impactZhTw = "薄薄一層白雪覆蓋，局部路面結冰濕滑。",
        warningZhTw = "注意高架橋樑、背陰路段暗冰防滑。"
    )
)

val SNOWFALL_3H_LEVELS = listOf(
    RadarLegendLevelItem(
        rangeLabel = "20cm〜",
        nameJa = "20cm以上",
        color = Color(0xFF78006E),
        onColor = Color.White,
        impactJa = "3時間で20cm以上の猛烈な降雪。一気に積雪が急増。",
        warningJa = "急速な交通障害・車両滞留に最大級警戒。",
        nameEn = "20cm or more",
        impactEn = "Intense rapid snowfall exceeding 20cm in 3h; drastic accumulation.",
        warningEn = "Maximum alert for rapid traffic paralysis and vehicle stranding.",
        nameZhCn = "20cm以上",
        impactZhCn = "3小时强降雪超过20cm，积雪瞬间暴涨。",
        warningZhCn = "对交通瘫痪及车辆大面积抛锚受困保持最高级别戒备。",
        nameZhTw = "20cm以上",
        impactZhTw = "3小時強降雪超過20cm，積雪瞬間暴漲。",
        warningZhTw = "對交通癱瘓及車輛大面積拋錨受困保持最高級別戒備。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "10〜20",
        nameJa = "10〜20cm",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "3時間で10〜20cmの非常に激しい降雪。除雪が追いつかない恐れ。",
        warningJa = "急な積雪増とホワイトアウト（視界不良）に厳重警戒。",
        nameEn = "10 to 20cm",
        impactEn = "Extremely heavy snow of 10-20cm in 3h; clearing cannot keep pace.",
        warningEn = "High danger of whiteouts and rapidly worsening roads.",
        nameZhCn = "10至20cm",
        impactZhCn = "3小时降雪达10至20cm，积雪速度快除雪困难。",
        warningZhCn = "严密戒备白化能见度骤降及路况急剧恶化。",
        nameZhTw = "10至20cm",
        impactZhTw = "3小時降雪達10至20cm，積雪速度快除雪困難。",
        warningZhTw = "嚴密戒備白化能見度驟降及路況急劇惡化。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "5〜10",
        nameJa = "5〜10cm",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "まとまった激しい雪。短時間で道路状況が悪化。",
        warningJa = "車の走行は冬用タイヤを装着し速度を落として走行。",
        nameEn = "5 to 10cm",
        impactEn = "Heavy snow band; road conditions deteriorate rapidly.",
        warningEn = "Use winter tires and drive at significantly reduced speed.",
        nameZhCn = "5至10cm",
        impactZhCn = "短时较强降雪，道路状况迅速变差。",
        warningZhCn = "驾车请务必减速慢行，确保冬季轮胎防滑。",
        nameZhTw = "5至10cm",
        impactZhTw = "短時較強降雪，道路狀況迅速變差。",
        warningZhTw = "駕車請務必減速慢行，確保冬季輪胎防滑。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "3〜5",
        nameJa = "3〜5cm",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "しっかりとした降雪。道路にうっすら積雪。",
        warningJa = "路面の凍結やシャーベット状の雪に注意。",
        nameEn = "3 to 5cm",
        impactEn = "Continuous steady snowfall covering pavement.",
        warningEn = "Caution on icy or slushy road surfaces.",
        nameZhCn = "3至5cm",
        impactZhCn = "持续稳定降雪，路面形成明显薄积雪或泥雪混合。",
        warningZhCn = "注意湿滑暗冰及泥雪路面行车安全。",
        nameZhTw = "3至5cm",
        impactZhTw = "持續穩定降雪，路面形成明顯薄積雪或泥雪混合。",
        warningZhTw = "注意濕滑暗冰及泥雪路面行車安全。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "1〜3",
        nameJa = "1〜3cm",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "並の降雪。周囲が白くなり始める。",
        warningJa = "滑りやすい靴を避け、歩行時足元に注意。",
        nameEn = "1 to 3cm",
        impactEn = "Moderate snowfall whitening the scenery.",
        warningEn = "Wear non-slip footwear and watch your step.",
        nameZhCn = "1至3cm",
        impactZhCn = "普通降雪，四周景物开始泛白。",
        warningZhCn = "外出注意防滑，避免穿底面光滑鞋子。",
        nameZhTw = "1至3cm",
        impactZhTw = "普通降雪，四周景物開始泛白。",
        warningZhTw = "外出注意防滑，避免穿底面光滑鞋子。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "0.5〜1",
        nameJa = "0.5〜1cm",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "ちらつく雪・弱い降雪。",
        warningJa = "気温が低い場合は路面凍結に注意。",
        nameEn = "0.5 to 1cm",
        impactEn = "Flurries or light falling snow.",
        warningEn = "Watch for freeze conditions when temperatures are low.",
        nameZhCn = "0.5至1cm",
        impactZhCn = "零星飘雪或微弱降雪。",
        warningZhCn = "气温偏低时注意路面轻微结冰。",
        nameZhTw = "0.5至1cm",
        impactZhTw = "零星飄雪或微弱降雪。",
        warningZhTw = "氣溫偏低時注意路面輕微結冰。"
    )
)

val SNOWFALL_24H_LEVELS = listOf(
    RadarLegendLevelItem(
        rangeLabel = "50cm〜",
        nameJa = "50cm以上",
        color = Color(0xFF78006E),
        onColor = Color.White,
        impactJa = "24時間で50cm以上の豪雪。大雪警報・特別警報級。",
        warningJa = "交通機関の計画運休や倒木・停電に厳重警戒。",
        nameEn = "50cm or more",
        impactEn = "Severe accumulation over 50cm in 24h; heavy snow warning level.",
        warningEn = "Prepare for transit suspensions, power outages, and tree damage.",
        nameZhCn = "50cm以上",
        impactZhCn = "24小时累计降雪达50cm以上，属于特大暴雪警报级别。",
        warningZhCn = "严密戒备公共交通大面积停运、电网断电及倒木灾害。",
        nameZhTw = "50cm以上",
        impactZhTw = "24小時累計降雪達50cm以上，屬於特大暴雪警報級別。",
        warningZhTw = "嚴密戒備公共交通大面積停運、電網斷電及倒木災害。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "30〜50",
        nameJa = "30〜50cm",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "大雪警報基準の大雪。車の立ち往生リスク増大。",
        warningJa = "大雪警報発表のおそれ。不要不急の外出を控えてください。",
        nameEn = "30 to 50cm",
        impactEn = "Heavy snow warning criteria met; high stranding hazard.",
        warningEn = "Refrain from non-essential outings.",
        nameZhCn = "30至50cm",
        impactZhCn = "达到大雪警报标准的大雪，车辆受困风险激增。",
        warningZhCn = "可能发布暴雪警报，请尽量避免非必要出行。",
        nameZhTw = "30至50cm",
        impactZhTw = "達到大雪警報標準的大雪，車輛受困風險激增。",
        warningZhTw = "可能發布暴雪警報，請儘量避免非必要出行。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "20〜30",
        nameJa = "20〜30cm",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "まとまった降雪。生活道路や峠道で通行障害。",
        warningJa = "雪道運転に不慣れな場合は運転を自粛してください。",
        nameEn = "20 to 30cm",
        impactEn = "Substantial snowfall causing traffic snarls on passes and local roads.",
        warningEn = "Avoid driving if unaccustomed to winter conditions.",
        nameZhCn = "20至30cm",
        impactZhCn = "大范围强降雪，山道与一般生活道路出现通行障碍。",
        warningZhCn = "不熟悉雪地驾驶者请切勿冒险自驾出行。",
        nameZhTw = "20至30cm",
        impactZhTw = "大範圍強降雪，山道與一般生活道路出現通行障礙。",
        warningZhTw = "不熟悉雪地駕駛者請切勿冒險自駕出行。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "10〜20",
        nameJa = "10〜20cm",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "大雪注意報基準のまとまった雪。除雪が必要。",
        warningJa = "通勤通学の交通機関の遅延に留意。",
        nameEn = "10 to 20cm",
        impactEn = "Moderate snow meeting advisory levels; snow shoveling required.",
        warningEn = "Expect transit delays during morning/evening commutes.",
        nameZhCn = "10至20cm",
        impactZhCn = "达到大雪注意报标准，需要进行除雪作业。",
        warningZhCn = "请预留通勤出行时间，关注公共交通晚点情况。",
        nameZhTw = "10至20cm",
        impactZhTw = "達到大雪注意報標準，需要進行除雪作業。",
        warningZhTw = "請預留通勤出行時間，關注公共交通晚點情況。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "5〜10",
        nameJa = "5〜10cm",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "雪景色となる降雪。路面の凍結・スリップに注意。",
        warningJa = "滑り止め措置を講じて歩行・走行してください。",
        nameEn = "5 to 10cm",
        impactEn = "Noticeable snowfall landscape; slippery frozen roads.",
        warningEn = "Ensure anti-skid preparations before heading out.",
        nameZhCn = "5至10cm",
        impactZhCn = "地表明显积雪，道路容易结冰打滑。",
        warningZhCn = "出行前请做好防滑防冻准备措施。",
        nameZhTw = "5至10cm",
        impactZhTw = "地表面顯積雪，道路容易結冰打滑。",
        warningZhTw = "出行前請做好防滑防凍準備措施。"
    ),
    RadarLegendLevelItem(
        rangeLabel = "1〜5",
        nameJa = "1〜5cm",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "うっすらとした降雪。日陰の路面凍結に注意。",
        warningJa = "朝晩の冷え込みによる凍結路面に注意。",
        nameEn = "1 to 5cm",
        impactEn = "Light snow cover; frozen patches in shaded zones.",
        warningEn = "Watch for morning and nighttime icy road spots.",
        nameZhCn = "1至5cm",
        impactZhCn = "浅浅薄雪覆盖，背阴路段易结薄冰。",
        warningZhCn = "注意早晚降温时段的道路暗冰湿滑。",
        nameZhTw = "1至5cm",
        impactZhTw = "淺淺薄雪覆蓋，背陰路段易結薄冰。",
        warningZhTw = "注意早晚降溫時段的道路暗冰濕滑。"
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
    showAllJapanLayers: Boolean = false,
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

    // 気象庁地図レイヤーの初期選択状態（ディープリンク指定があれば最優先で初期化、非表示レイヤーはHRPNSにフォールバック）
    val initialLayer = remember(initialLayerId, showAllJapanLayers) {
        if (!initialLayerId.isNullOrBlank()) {
            val layer = JmaLayerType.entries.firstOrNull { it.id.equals(initialLayerId, ignoreCase = true) }
                ?: JmaLayerType.HRPNS
            if (!showAllJapanLayers && !isAllowedJapanRadarLayer(layer)) {
                JmaLayerType.HRPNS
            } else {
                layer
            }
        } else {
            JmaLayerType.HRPNS
        }
    }
    var selectedJmaLayer by remember(initialLayer) { mutableStateOf(initialLayer) }

    LaunchedEffect(showAllJapanLayers) {
        if (!showAllJapanLayers && !isAllowedJapanRadarLayer(selectedJmaLayer)) {
            selectedJmaLayer = JmaLayerType.HRPNS
        }
    }
    val expressHaptics = LocalExpressiveHaptics.current
    var isSpeedDialOpen by remember { mutableStateOf(false) }
    var showFullLayerSheet by remember { mutableStateOf(false) }
    var sheetCategory by remember { mutableStateOf(JmaLayerCategory.NOWCAST) }
    var sheetUsCategory by remember { mutableStateOf(UsLayerCategory.UNIFIED) }
    // 降水量・雨の強さ一覧モーダルシートの表示状態
    var showIntensitySheet by remember { mutableStateOf(false) }
    // 縦型降水強度凡例ウィジェットの展開状態 (Frame 95 ⇔ Frame 96)
    var isLegendExpanded by remember { mutableStateOf(false) }

    // 気象庁台風情報（日本域かつ台風レイヤー選択時、または日本域表示時のみ取得して海外ユーザーの無駄な通信を回避）
    var activeTyphoons by remember { mutableStateOf<List<JmaTyphoonItem>>(emptyList()) }
    var selectedTyphoonForPopup by remember { mutableStateOf<JmaTyphoonItem?>(null) }

    LaunchedEffect(selectedJmaLayer, dataSource) {
        if (selectedJmaLayer != JmaLayerType.TYPHOON) {
            selectedTyphoonForPopup = null
        }
        if (dataSource == RadarDataSource.JMA_NOWCAST && (selectedJmaLayer == JmaLayerType.TYPHOON || activeTyphoons.isEmpty())) {
            activeTyphoons = JmaTyphoonRepository.getActiveTyphoons()
        }
    }

    // 基準観測時刻（フレーム読み込み時に更新し、毎秒のリコンポーズを回避）
    val fallbackObsSec = remember(radarData) { System.currentTimeMillis() / 1000L }

    // 過去＋未来ナウキャストの全フレーム
    val frames = remember(radarData) {
        if (radarData != null) {
            radarData!!.radar.past + radarData!!.radar.nowcast
        } else {
            emptyList()
        }
    }

    // 初回ロード & 設定・座標・国名・レイヤー変更時（単一のキャンセル安全な Effect に統一）
    LaunchedEffect(latitude, longitude, country, sourceMode, selectedJmaLayer, selectedUsLayer) {
        isLoading = true
        try {
            val resolvedSource = RadarProviderRegistry.resolveProvider(sourceMode, latitude, longitude, country)
            dataSource = resolvedSource
            if (resolvedSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer != JmaLayerType.HRPNS) {
                val result = repo.getJmaLayerData(selectedJmaLayer)
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
                } else {
                    radarData = null
                    currentFrameIndex = -1
                    currentTimestamp = 0L
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
                    } else {
                        currentFrameIndex = -1
                        currentTimestamp = 0L
                    }
                } else {
                    radarData = null
                    currentFrameIndex = -1
                    currentTimestamp = 0L
                }
            }
        } finally {
            isLoading = false
        }
    }

    // 通知ディープリンクからの再遷移検知
    LaunchedEffect(initialLayerId, showAllJapanLayers) {
        if (!initialLayerId.isNullOrBlank()) {
            val target = JmaLayerType.entries.firstOrNull { it.id.equals(initialLayerId, ignoreCase = true) }
            if (target != null) {
                val resolved = if (!showAllJapanLayers && !isAllowedJapanRadarLayer(target)) {
                    JmaLayerType.HRPNS
                } else {
                    target
                }
                if (resolved != selectedJmaLayer) {
                    selectedJmaLayer = resolved
                }
            }
        }
    }

    // レイヤー変更ハンドラ（実際のフェッチは LaunchedEffect が単一実行・キャンセル管理）
    fun switchJmaLayer(newLayer: JmaLayerType) {
        val resolved = if (!showAllJapanLayers && !isAllowedJapanRadarLayer(newLayer)) {
            JmaLayerType.HRPNS
        } else {
            newLayer
        }
        if (resolved != JmaLayerType.TYPHOON) {
            selectedTyphoonForPopup = null
        }
        isSpeedDialOpen = false
        showFullLayerSheet = false
        isPlaying = false
        if (resolved != selectedJmaLayer) {
            isLoading = true
            radarData = null
            currentFrameIndex = -1
            currentTimestamp = 0L
            selectedJmaLayer = resolved
        }
    }

    val host = radarData?.host ?: "https://tilecache.rainviewer.com"

    val timeFormatter = remember(language) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    val currentFrame = frames.getOrNull(currentFrameIndex)
    val pastSize = radarData?.radar?.past?.size ?: 0
    val currentObsTime = remember(radarData, fallbackObsSec) {
        val pastList = radarData?.radar?.past ?: emptyList()
        if (pastList.isNotEmpty()) {
            pastList.last().time
        } else {
            fallbackObsSec
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
    var playbackStretchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun triggerPlaybackStretch(index: Int) {
        playbackStretchJob?.cancel()
        playbackStretchJob = coroutineScope.launch {
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

    // スピードダイヤル項目一覧（下から上へのインデックス順：0がボタン直上、通常時は4項目、デバッグ時は全7項目）
    val bottomToTopItems = remember(selectedJmaLayer, language, showAllJapanLayers) {
        if (!showAllJapanLayers) {
            listOf(
                // index 0: ボタン直上（雨雲ナウキャスト）
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
                // index 1: 雷ナウキャスト
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "雷ナウキャスト"
                        AppLanguage.ENGLISH -> "Lightning Nowcast"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷电即时预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷電即時預報"
                    },
                    icon = Icons.Rounded.Thunderstorm,
                    isSelected = selectedJmaLayer == JmaLayerType.THNS,
                    onClick = { switchJmaLayer(JmaLayerType.THNS) }
                ),
                // index 2: キキクル危険度
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
                // index 3: 雪・降雪量情報
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "雪・降雪量情報"
                        AppLanguage.ENGLISH -> "Snow & Snowfall"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降雪・积雪量信息"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雪・積雪量資訊"
                    },
                    icon = Icons.Rounded.AcUnit,
                    isSelected = selectedJmaLayer.category == JmaLayerCategory.SNOW,
                    onClick = {
                        if (selectedJmaLayer.category == JmaLayerCategory.SNOW) {
                            val nextSnow = when (selectedJmaLayer) {
                                JmaLayerType.SNOW_DEPTH -> JmaLayerType.SNOWFALL_3H
                                JmaLayerType.SNOWFALL_3H -> JmaLayerType.SNOWFALL_24H
                                else -> JmaLayerType.SNOW_DEPTH
                            }
                            switchJmaLayer(nextSnow)
                        } else {
                            switchJmaLayer(JmaLayerType.SNOW_DEPTH)
                        }
                    }
                ),
                // index 4: 台風情報 (進路・暴風域)
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "台風情報 (進路・暴風域)"
                        AppLanguage.ENGLISH -> "Typhoon Track & Warning"
                        AppLanguage.SIMPLIFIED_CHINESE -> "台风情报 (路径・暴风区)"
                        AppLanguage.TRADITIONAL_CHINESE -> "颱風情報 (路徑・暴風區)"
                    },
                    icon = Icons.Rounded.Air,
                    iconRes = R.drawable.ic_storm,
                    isSelected = selectedJmaLayer == JmaLayerType.TYPHOON,
                    onClick = { switchJmaLayer(JmaLayerType.TYPHOON) }
                )
            )
        } else {
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
                // index 6: 台風情報
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "台風情報 (進路・暴風域)"
                        AppLanguage.ENGLISH -> "Typhoon Track & Warning"
                        AppLanguage.SIMPLIFIED_CHINESE -> "台风情报 (路径・暴风区)"
                        AppLanguage.TRADITIONAL_CHINESE -> "颱風情報 (路徑・暴風區)"
                    },
                    icon = Icons.Rounded.Air,
                    iconRes = R.drawable.ic_storm,
                    isSelected = selectedJmaLayer == JmaLayerType.TYPHOON,
                    onClick = { switchJmaLayer(JmaLayerType.TYPHOON) }
                ),
                // index 7: 最上段
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "すべてのレイヤー (${JmaLayerType.entries.size}種)"
                        AppLanguage.ENGLISH -> "All Layers (${JmaLayerType.entries.size})"
                        AppLanguage.SIMPLIFIED_CHINESE -> "所有图层 (${JmaLayerType.entries.size}种)"
                        AppLanguage.TRADITIONAL_CHINESE -> "所有圖層 (${JmaLayerType.entries.size}種)"
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
    }

    val usSpeedDialItems = remember(language, selectedUsLayer) {
        listOf(
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "全米 NEXRAD / HRRR"
                    AppLanguage.ENGLISH -> "US NEXRAD / HRRR"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR"
                },
                icon = Icons.Rounded.WaterDrop,
                isSelected = selectedUsLayer == UsRadarLayerType.UNIFIED_ALL,
                onClick = {
                    selectedUsLayer = UsRadarLayerType.UNIFIED_ALL
                    isSpeedDialOpen = false
                }
            ),
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "すべての全米レイヤー (${UsRadarLayerType.entries.size}種)"
                    AppLanguage.ENGLISH -> "All US Layers (${UsRadarLayerType.entries.size})"
                    AppLanguage.SIMPLIFIED_CHINESE -> "所有全美图层 (${UsRadarLayerType.entries.size}种)"
                    AppLanguage.TRADITIONAL_CHINESE -> "所有全美圖層 (${UsRadarLayerType.entries.size}種)"
                },
                icon = Icons.Rounded.Layers,
                isSelected = selectedUsLayer != UsRadarLayerType.UNIFIED_ALL,
                onClick = {
                    isSpeedDialOpen = false
                    sheetUsCategory = selectedUsLayer.category
                    showFullLayerSheet = true
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
            zoom = if (dataSource == RadarDataSource.JMA_NOWCAST && (selectedJmaLayer.category == JmaLayerCategory.HIMAWARI || selectedJmaLayer == JmaLayerType.TYPHOON)) 5 else 10,
            recenterTrigger = recenterTrigger,
            zoomInTrigger = zoomInTrigger,
            zoomOutTrigger = zoomOutTrigger,
            isInteractive = true,
            isPlaying = isPlaying,
            currentFrameIndex = currentFrameIndex,
            colorScheme = selectedColorScheme,
            isDark = isDark,
            showZoomBadge = showZoomBadge,
            typhoons = if (dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer == JmaLayerType.TYPHOON) activeTyphoons else emptyList(),
            selectedTyphoon = selectedTyphoonForPopup,
            onTyphoonClick = { typhoon ->
                selectedTyphoonForPopup = typhoon
            },
            onMapClick = {
                selectedTyphoonForPopup = null
            },
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
                            val nextOpen = !isSpeedDialOpen
                            isSpeedDialOpen = nextOpen
                            expressHaptics.toggle(nextOpen)
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
                                    text = if (dataSource == RadarDataSource.JMA_NOWCAST && (selectedJmaLayer.isStatic || selectedJmaLayer == JmaLayerType.TYPHOON)) {
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
                                if (relativeTimeLabel.isNotBlank() && !(dataSource == RadarDataSource.JMA_NOWCAST && (selectedJmaLayer.isStatic || selectedJmaLayer == JmaLayerType.TYPHOON))) {
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
                                        currentFrame?.isExtrapolation == true -> "NEXRAD"
                                        currentFrame?.isForecast == true -> "HRRR"
                                        else -> "NEXRAD/MRMS"
                                    }
                                }
                                dataSource == RadarDataSource.JMA_NOWCAST && selectedJmaLayer == JmaLayerType.TYPHOON -> {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "台風実況・進路予報"
                                        AppLanguage.ENGLISH -> "Track & Warnings"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "台风实况・路径预报"
                                        AppLanguage.TRADITIONAL_CHINESE -> "颱風實況・路徑預報"
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
                            RadarNowClockText(
                                nowPrefix = nowPrefix,
                                resTag = resTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onPrimary
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
                                        val maxIdx = (frames.size - 1).coerceAtLeast(0)
                                        val idx = value.roundToInt().coerceIn(0, maxIdx)
                                        if (idx != currentFrameIndex && frames.isNotEmpty()) {
                                            val obsCount = frames.count { !it.isForecast && !it.isExtrapolation }
                                            val isNowBoundary = obsCount in 1 until frames.size && idx == obsCount - 1
                                            if (idx == 0 || idx == maxIdx || isNowBoundary) {
                                                expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY)
                                            } else {
                                                expressHaptics.play(ExpressiveHapticPattern.SCRUB_TICK)
                                            }
                                        }
                                        currentFrameIndex = idx
                                        if (idx in frames.indices) {
                                            currentTimestamp = frames[idx].time
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
                } else if (dataSource == RadarDataSource.JMA_NOWCAST && (selectedJmaLayer.isStatic || selectedJmaLayer == JmaLayerType.TYPHOON)) {
                    // 静的マップ / 台風マップ表示中のインジケータ (影なし・枠なし)
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
                                text = if (selectedJmaLayer == JmaLayerType.TYPHOON) {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 台風進路予報 (実況・暴風域・予報円)"
                                        AppLanguage.ENGLISH -> "JMA Typhoon Track & Warnings (Active / Forecast)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "气象厅 台风路径预报 (实况・暴风区・预报圆)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "氣象廳 颱風路徑預報 (實況・暴風區・預報圓)"
                                    }
                                } else {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "公式背景地図レイヤー表示中"
                                        AppLanguage.ENGLISH -> "Base map layer active"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "官方背景地图图层显示中"
                                        AppLanguage.TRADITIONAL_CHINESE -> "官方背景地圖圖層顯示中"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }

                // 【下段】3連ボタン (⏪ / ▶⏸ / ⏩) (影なし・枠なし・押下時に伸びて他が縮む弾性アニメーション)
                if (!(dataSource == RadarDataSource.JMA_NOWCAST && (selectedJmaLayer.isStatic || selectedJmaLayer == JmaLayerType.TYPHOON)) && frames.isNotEmpty()) {
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
                                    .m3eBouncyClick(
                                        scaleDown = 0.88f,
                                        overshootScale = 1.06f,
                                        hapticPattern = ExpressiveHapticPattern.SCRUB_TICK
                                    ) {
                                        triggerPlaybackStretch(0)
                                        isPlaying = false
                                        if (currentFrameIndex > 0) {
                                            currentFrameIndex--
                                            currentTimestamp = frames[currentFrameIndex].time
                                            if (currentFrameIndex == 0) {
                                                expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY, bypassThrottle = true)
                                            }
                                        } else {
                                            expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY, bypassThrottle = true)
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
                                            AppLanguage.TRADITIONAL_CHINESE -> "上一幀"
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
                                    .m3eBouncyClick(
                                        scaleDown = 0.90f,
                                        overshootScale = 1.08f,
                                        hapticPattern = if (isPlaying) ExpressiveHapticPattern.SWITCH_OFF else ExpressiveHapticPattern.SWITCH_ON
                                    ) {
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
                                    .m3eBouncyClick(
                                        scaleDown = 0.88f,
                                        overshootScale = 1.06f,
                                        hapticPattern = ExpressiveHapticPattern.SCRUB_TICK
                                    ) {
                                        triggerPlaybackStretch(2)
                                        isPlaying = false
                                        if (currentFrameIndex < frames.size - 1) {
                                            currentFrameIndex++
                                            currentTimestamp = frames[currentFrameIndex].time
                                            if (currentFrameIndex == frames.size - 1) {
                                                expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY, bypassThrottle = true)
                                            }
                                        } else {
                                            expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY, bypassThrottle = true)
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
                                            AppLanguage.TRADITIONAL_CHINESE -> "下一幀"
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

        // 5. 縦型指標ウィジェット (Frame 95: 折りたたみスリムピル ⇔ Frame 96: 展開カード)
        // 雨雲ナウキャスト、雷ナウキャスト、キキクル危険度、雪降雪量情報（およびUS/RainViewer）に対応
        val currentLegendKind: RadarLegendKind? = when {
            dataSource == RadarDataSource.US_NEXRAD_HRRR -> RadarLegendKind.US_NEXRAD
            dataSource == RadarDataSource.RAIN_VIEWER -> RadarLegendKind.RAIN_VIEWER
            selectedJmaLayer == JmaLayerType.THNS || selectedJmaLayer == JmaLayerType.TRNS -> RadarLegendKind.LIGHTNING
            selectedJmaLayer.category == JmaLayerCategory.RISK -> RadarLegendKind.KIKIKURU
            selectedJmaLayer == JmaLayerType.SNOW_DEPTH -> RadarLegendKind.SNOW_DEPTH
            selectedJmaLayer == JmaLayerType.SNOWFALL_3H -> RadarLegendKind.SNOWFALL_3H
            selectedJmaLayer == JmaLayerType.SNOWFALL_24H -> RadarLegendKind.SNOWFALL_24H
            selectedJmaLayer.id == "hrpns" || selectedJmaLayer.category == JmaLayerCategory.RAINFALL || selectedJmaLayer.category == JmaLayerCategory.NOWCAST -> RadarLegendKind.RAIN_INTENSITY
            else -> null
        }

        if (currentLegendKind != null) {
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
                val isUsRadar = currentLegendKind == RadarLegendKind.US_NEXRAD
                val isRainViewer = currentLegendKind == RadarLegendKind.RAIN_VIEWER

                val legendColors: List<Color> = when (currentLegendKind) {
                    RadarLegendKind.US_NEXRAD -> US_NEXRAD_DBZ_LEVELS.reversed().map { it.color }
                    RadarLegendKind.RAIN_VIEWER -> RAIN_VIEWER_DBZ_LEVELS.reversed().map { it.color }
                    RadarLegendKind.RAIN_INTENSITY -> RAIN_INTENSITY_LEVELS.map { it.color }
                    RadarLegendKind.LIGHTNING -> LIGHTNING_ACTIVITY_LEVELS.map { it.color }
                    RadarLegendKind.KIKIKURU -> KIKIKURU_RISK_LEVELS.map { it.color }
                    RadarLegendKind.SNOW_DEPTH -> SNOW_DEPTH_LEVELS.map { it.color }
                    RadarLegendKind.SNOWFALL_3H -> SNOWFALL_3H_LEVELS.map { it.color }
                    RadarLegendKind.SNOWFALL_24H -> SNOWFALL_24H_LEVELS.map { it.color }
                }

                val scaleLabels: List<String> = when (currentLegendKind) {
                    RadarLegendKind.US_NEXRAD -> listOf("65", "45", "25", "5")
                    RadarLegendKind.RAIN_VIEWER -> listOf("60", "45", "25", "15")
                    RadarLegendKind.RAIN_INTENSITY -> listOf("80", "10", "1", "0.1")
                    RadarLegendKind.LIGHTNING -> listOf("4", "3", "2", "1")
                    RadarLegendKind.KIKIKURU -> listOf("Lv5", "Lv4", "Lv3", "Lv2")
                    RadarLegendKind.SNOW_DEPTH -> listOf("200", "50", "20", "1")
                    RadarLegendKind.SNOWFALL_3H -> listOf("20", "10", "3", "0.5")
                    RadarLegendKind.SNOWFALL_24H -> listOf("50", "20", "10", "1")
                }

                val legendTitle: String = when (currentLegendKind) {
                    RadarLegendKind.US_NEXRAD, RadarLegendKind.RAIN_VIEWER -> when (language) {
                        AppLanguage.JAPANESE -> "雨雲反射強度 (dBZ)"
                        AppLanguage.ENGLISH -> "Radar Reflectivity (dBZ)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷达反射强度 (dBZ)"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷達反射強度 (dBZ)"
                    }
                    RadarLegendKind.RAIN_INTENSITY -> when (language) {
                        AppLanguage.JAPANESE -> "雨の強さ (mm/h)"
                        AppLanguage.ENGLISH -> "Rain Intensity (mm/h)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降雨强度 (mm/h)"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雨強度 (mm/h)"
                    }
                    RadarLegendKind.LIGHTNING -> when (language) {
                        AppLanguage.JAPANESE -> "雷活動度 (活動度1〜4)"
                        AppLanguage.ENGLISH -> "Lightning Activity (1-4)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷电活动度 (1~4)"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷電活動度 (1~4)"
                    }
                    RadarLegendKind.KIKIKURU -> when (selectedJmaLayer) {
                        JmaLayerType.LANDSLIDE -> when (language) {
                            AppLanguage.JAPANESE -> "土砂キキクル (危険度)"
                            AppLanguage.ENGLISH -> "Landslide Kikikuru (Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "泥石流风险 (Kikikuru)"
                            AppLanguage.TRADITIONAL_CHINESE -> "土石流風險 (Kikikuru)"
                        }
                        JmaLayerType.INUNDATION -> when (language) {
                            AppLanguage.JAPANESE -> "浸水キキクル (危険度)"
                            AppLanguage.ENGLISH -> "Inundation Kikikuru (Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "积水淹没风险 (Kikikuru)"
                            AppLanguage.TRADITIONAL_CHINESE -> "積水淹沒風險 (Kikikuru)"
                        }
                        JmaLayerType.FLOOD -> when (language) {
                            AppLanguage.JAPANESE -> "洪水キキクル (危険度)"
                            AppLanguage.ENGLISH -> "Flood Kikikuru (Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "洪水风险 (Kikikuru)"
                            AppLanguage.TRADITIONAL_CHINESE -> "洪水風險 (Kikikuru)"
                        }
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "キキクル (危険度分布)"
                            AppLanguage.ENGLISH -> "Kikikuru (Hazard Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "Kikikuru (灾害风险)"
                            AppLanguage.TRADITIONAL_CHINESE -> "Kikikuru (災害風險)"
                        }
                    }
                    RadarLegendKind.SNOW_DEPTH -> when (language) {
                        AppLanguage.JAPANESE -> "現在の積雪深 (cm)"
                        AppLanguage.ENGLISH -> "Snow Depth (cm)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "当前积雪深度 (cm)"
                        AppLanguage.TRADITIONAL_CHINESE -> "當前積雪深度 (cm)"
                    }
                    RadarLegendKind.SNOWFALL_3H -> when (language) {
                        AppLanguage.JAPANESE -> "3時間降雪量 (cm)"
                        AppLanguage.ENGLISH -> "3-Hour Snowfall (cm)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "3小时降雪量 (cm)"
                        AppLanguage.TRADITIONAL_CHINESE -> "3小時降雪量 (cm)"
                    }
                    RadarLegendKind.SNOWFALL_24H -> when (language) {
                        AppLanguage.JAPANESE -> "24時間降雪量 (cm)"
                        AppLanguage.ENGLISH -> "24-Hour Snowfall (cm)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "24小时降雪量 (cm)"
                        AppLanguage.TRADITIONAL_CHINESE -> "24小時降雪量 (cm)"
                    }
                }

                val expandedItems: List<Pair<String, String>> = when (currentLegendKind) {
                    RadarLegendKind.US_NEXRAD -> US_NEXRAD_DBZ_LEVELS.reversed().map { it.intensityName(language) to "${it.label} dBZ" }
                    RadarLegendKind.RAIN_VIEWER -> RAIN_VIEWER_DBZ_LEVELS.reversed().map { it.intensityName(language) to "${it.label} dBZ" }
                    RadarLegendKind.RAIN_INTENSITY -> RAIN_INTENSITY_LEVELS.map { it.name(language) to it.rangeLabel }
                    RadarLegendKind.LIGHTNING -> LIGHTNING_ACTIVITY_LEVELS.map { it.name(language) to it.rangeLabel }
                    RadarLegendKind.KIKIKURU -> KIKIKURU_RISK_LEVELS.map { it.name(language) to it.rangeLabel }
                    RadarLegendKind.SNOW_DEPTH -> SNOW_DEPTH_LEVELS.map { it.name(language) to it.rangeLabel }
                    RadarLegendKind.SNOWFALL_3H -> SNOWFALL_3H_LEVELS.map { it.name(language) to it.rangeLabel }
                    RadarLegendKind.SNOWFALL_24H -> SNOWFALL_24H_LEVELS.map { it.name(language) to it.rangeLabel }
                }

                val barHeight = (expandedItems.size * 22).dp

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
                            legendColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(color)
                                )
                            }
                        }

                        // 代表数値ラベル (上から下へ等間隔)
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
                                text = legendTitle,
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
                            // 垂直カラーバー
                            Column(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(3.dp))
                            ) {
                                legendColors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(color)
                                    )
                                }
                            }

                            // 区分の名称とラベル
                            Column(
                                modifier = Modifier
                                    .height(barHeight)
                                    .weight(1f),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                expandedItems.forEach { (name, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface,
                                            fontSize = 9.5.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = label,
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

        // 6. 【最前面】スピードダイヤル式レイヤー選択メニュー（背景タップスクリムは #2 に一元化）
        var keepSpeedDialMounted by remember { mutableStateOf(false) }
        LaunchedEffect(isSpeedDialOpen) {
            if (isSpeedDialOpen) {
                keepSpeedDialMounted = true
            } else {
                delay(65L)
                keepSpeedDialMounted = false
            }
        }
        if (keepSpeedDialMounted) {
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
                                        AppLanguage.JAPANESE -> "NOAA / NEXRAD / HRRRシミュレーション (全${UsRadarLayerType.entries.size}種類)"
                                        AppLanguage.ENGLISH -> "NOAA / NEXRAD / HRRR Simulation (${UsRadarLayerType.entries.size} Layers)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "NOAA / NEXRAD / HRRR 模拟 (全${UsRadarLayerType.entries.size}种)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "NOAA / NEXRAD / HRRR 模擬 (全${UsRadarLayerType.entries.size}種)"
                                    }
                                } else {
                                    val count = JmaLayerType.entries.count { showAllJapanLayers || isAllowedJapanRadarLayer(it) }
                                    when (language) {
                                        AppLanguage.JAPANESE -> "気象庁公式配信タイル (全${count}種類)"
                                        AppLanguage.ENGLISH -> "JMA Official Tiles ($count Layers)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅官方切片 (全${count}种)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳官方圖磚 (全${count}種)"
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
                        // 日本 気象庁用カテゴリチップ横スクロール列 (通常時は4カテゴリ、デバッグ時は全9カテゴリ、絵文字なし)
                        val visibleCategories = remember(showAllJapanLayers) {
                            if (showAllJapanLayers) {
                                JmaLayerCategory.entries
                            } else {
                                listOf(JmaLayerCategory.NOWCAST, JmaLayerCategory.RISK, JmaLayerCategory.SNOW, JmaLayerCategory.TYPHOON)
                            }
                        }

                        LaunchedEffect(showAllJapanLayers, visibleCategories) {
                            if (!visibleCategories.contains(sheetCategory)) {
                                sheetCategory = JmaLayerCategory.NOWCAST
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            items(visibleCategories) { cat ->
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
                        val layersInSelectedCategory = remember(sheetCategory, showAllJapanLayers) {
                            val allInCat = JmaLayerType.entries.filter { it.category == sheetCategory }
                            if (showAllJapanLayers) {
                                allInCat
                            } else {
                                allInCat.filter { isAllowedJapanRadarLayer(it) }
                            }
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
                                text = when (currentLegendKind) {
                                    RadarLegendKind.US_NEXRAD -> when (language) {
                                        AppLanguage.JAPANESE -> "全米レーダー dBZ 反射強度基準"
                                        AppLanguage.ENGLISH -> "US Radar dBZ Reflectivity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "全美雷达 dBZ 反射强度标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "全美雷達 dBZ 反射強度標準"
                                    }
                                    RadarLegendKind.RAIN_VIEWER -> when (language) {
                                        AppLanguage.JAPANESE -> "世界レーダー dBZ 反射強度基準"
                                        AppLanguage.ENGLISH -> "Global Radar dBZ Reflectivity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 dBZ 反射强度标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 dBZ 反射強度標準"
                                    }
                                    RadarLegendKind.LIGHTNING -> when (language) {
                                        AppLanguage.JAPANESE -> "雷ナウキャスト 活動度基準・ガイド"
                                        AppLanguage.ENGLISH -> "Lightning Nowcast Activity Scale & Guide"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "雷电即时预报活动度等级指南"
                                        AppLanguage.TRADITIONAL_CHINESE -> "雷電即時預報活動度等級指南"
                                    }
                                    RadarLegendKind.KIKIKURU -> when (language) {
                                        AppLanguage.JAPANESE -> "キキクル（危険度分布）判定基準・ガイド"
                                        AppLanguage.ENGLISH -> "Kikikuru Risk Level Scale & Action Guide"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "Kikikuru 风险分布判定等级指南"
                                        AppLanguage.TRADITIONAL_CHINESE -> "Kikikuru 風險分布判定等級指南"
                                    }
                                    RadarLegendKind.SNOW_DEPTH -> when (language) {
                                        AppLanguage.JAPANESE -> "現在の積雪の深さ 区分・目安"
                                        AppLanguage.ENGLISH -> "Snow Depth Scale & Travel Guide"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前积雪深度等级・出行参考"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前積雪深度等級・出行參考"
                                    }
                                    RadarLegendKind.SNOWFALL_3H -> when (language) {
                                        AppLanguage.JAPANESE -> "3時間降雪量 区分・目安"
                                        AppLanguage.ENGLISH -> "3-Hour Snowfall Intensity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "3小时降雪量等级・出行参考"
                                        AppLanguage.TRADITIONAL_CHINESE -> "3小時降雪量等級・出行參考"
                                    }
                                    RadarLegendKind.SNOWFALL_24H -> when (language) {
                                        AppLanguage.JAPANESE -> "24時間降雪量 区分・目安"
                                        AppLanguage.ENGLISH -> "24-Hour Snowfall Intensity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "24小时降雪量等级・出行参考"
                                        AppLanguage.TRADITIONAL_CHINESE -> "24小時降雪量等級・出行參考"
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
                                text = when (currentLegendKind) {
                                    RadarLegendKind.US_NEXRAD -> when (language) {
                                        AppLanguage.JAPANESE -> "NOAA / NEXRAD / MRMS 標準カラーパレット (dBZ)"
                                        AppLanguage.ENGLISH -> "NOAA / NEXRAD / MRMS Standard Color Palette (dBZ)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "NOAA / NEXRAD / MRMS 标准色盘 (dBZ)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "NOAA / NEXRAD / MRMS 標準色盤 (dBZ)"
                                    }
                                    RadarLegendKind.RAIN_VIEWER -> when (language) {
                                        AppLanguage.JAPANESE -> "RainViewer Universal Blue 標準カラーパレット (dBZ)"
                                        AppLanguage.ENGLISH -> "RainViewer Universal Blue Color Palette (dBZ)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "RainViewer Universal Blue 标准色盘 (dBZ)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "RainViewer Universal Blue 標準色盤 (dBZ)"
                                    }
                                    RadarLegendKind.LIGHTNING -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 雷ナウキャスト（活動度4〜1）基準"
                                        AppLanguage.ENGLISH -> "JMA Lightning Nowcast Activity Levels 1–4"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅雷电即时预报（活动度4〜1）标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳雷電即時預報（活動度4〜1）標準"
                                    }
                                    RadarLegendKind.KIKIKURU -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 警戒レベル・災害切迫度（Lv1〜Lv5）"
                                        AppLanguage.ENGLISH -> "JMA Alert Levels & Evacuation Guidance (Lv1–Lv5)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅警戒级别・灾害危险度（Lv1〜Lv5）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳警戒級別・災害危險度（Lv1〜Lv5）"
                                    }
                                    RadarLegendKind.SNOW_DEPTH -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 解析積雪深・アメダス積雪階級（cm）"
                                        AppLanguage.ENGLISH -> "JMA Analyzed Snow Depth Standards (cm)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅解析积雪深度标准（cm）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳解析積雪深度標準（cm）"
                                    }
                                    RadarLegendKind.SNOWFALL_3H -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 3時間解析降雪量・短時間豪雪警戒（cm）"
                                        AppLanguage.ENGLISH -> "JMA 3-Hour Analyzed Snowfall Standards (cm)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅3小时解析降雪量标准（cm）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳3小時解析降雪量標準（cm）"
                                    }
                                    RadarLegendKind.SNOWFALL_24H -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 24時間解析降雪量・大雪警戒（cm）"
                                        AppLanguage.ENGLISH -> "JMA 24-Hour Analyzed Snowfall Standards (cm)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅24小时解析降雪量标准（cm）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳24小時解析降雪量標準（cm）"
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

                    // 段階のカテゴリ一覧
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (currentLegendKind) {
                            RadarLegendKind.US_NEXRAD -> {
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
                            RadarLegendKind.RAIN_VIEWER -> {
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
                            RadarLegendKind.LIGHTNING,
                            RadarLegendKind.KIKIKURU,
                            RadarLegendKind.SNOW_DEPTH,
                            RadarLegendKind.SNOWFALL_3H,
                            RadarLegendKind.SNOWFALL_24H -> {
                                val items: List<RadarLegendLevelItem> = when (currentLegendKind) {
                                    RadarLegendKind.LIGHTNING -> LIGHTNING_ACTIVITY_LEVELS
                                    RadarLegendKind.KIKIKURU -> KIKIKURU_RISK_LEVELS
                                    RadarLegendKind.SNOW_DEPTH -> SNOW_DEPTH_LEVELS
                                    RadarLegendKind.SNOWFALL_3H -> SNOWFALL_3H_LEVELS
                                    RadarLegendKind.SNOWFALL_24H -> SNOWFALL_24H_LEVELS
                                }
                                items.forEach { level ->
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

        // 7. 台風タップ時ポップアップ（右隣に出現 & 弾性アニメーション & ボックス外タップで消滅）
        if (selectedTyphoonForPopup != null) {
            // 背景全面透明スクリム（ボックス以外の場所をタップしたときに消す）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(90f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedTyphoonForPopup = null
                    }
            )
        }

        AnimatedVisibility(
            visible = selectedTyphoonForPopup != null,
            enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
                    scaleIn(
                        initialScale = 0.70f,
                        transformOrigin = TransformOrigin(0f, 0.5f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 100)) +
                    scaleOut(
                        targetScale = 0.80f,
                        transformOrigin = TransformOrigin(0f, 0.5f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, top = 76.dp, bottom = 100.dp, start = 16.dp)
                .zIndex(95f)
        ) {
            val typhoon = selectedTyphoonForPopup
            if (typhoon != null) {
                TyphoonInfoCard(
                    typhoon = typhoon,
                    language = language,
                    isDark = isDark,
                    onClose = { selectedTyphoonForPopup = null }
                )
            }
        }
    }
}

/**
 * 台風タップ時に弾性アニメーションで右隣に出現する気圧・風速等の詳細情報ポップアップカード
 * アプリのテーマカラー（MaterialTheme.colorScheme）に厳格に準拠
 */
@Composable
private fun TyphoonInfoCard(
    typhoon: JmaTyphoonItem,
    language: AppLanguage,
    isDark: Boolean,
    onClose: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val analysis = typhoon.analysis

    val cardBg = colorScheme.surfaceContainer
    val tileBg = colorScheme.surfaceContainerHighest

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* ボックス内クリックを吸収して外側へ貫通させない */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ヘッダー: Stormアイコン、タイトル、勢力区分、閉じるボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_storm),
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = typhoon.simpleName(language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val classification = analysis.officialClassification(language)
                        if (classification.isNotBlank()) {
                            val intensityColor = when (analysis.intensity) {
                                JmaTyphoonIntensity.VIOLENT -> colorScheme.tertiary
                                JmaTyphoonIntensity.VERY_STRONG -> colorScheme.error
                                JmaTyphoonIntensity.STRONG -> colorScheme.primary
                                JmaTyphoonIntensity.NONE -> colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = classification,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = intensityColor
                            )
                        }
                    }
                }

                // 閉じる丸ボタン
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerHighest)
                        .clickable { onClose() },
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 主要気象諸元グリッド (中心気圧, 進行方向・速さ, 最大風速, 最大瞬間風速)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 中心気圧
                TyphoonMetricTile(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "中心気圧"
                        AppLanguage.ENGLISH -> "Pressure"
                        AppLanguage.SIMPLIFIED_CHINESE -> "中心气压"
                        AppLanguage.TRADITIONAL_CHINESE -> "中心氣壓"
                    },
                    value = analysis.pressureHpa?.let { "$it hPa" } ?: "---",
                    accentColor = colorScheme.primary,
                    bgColor = tileBg,
                    modifier = Modifier.weight(1f)
                )

                // 進行方向・速さ
                TyphoonMetricTile(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "進行方向・速さ"
                        AppLanguage.ENGLISH -> "Movement"
                        AppLanguage.SIMPLIFIED_CHINESE -> "移向・移速"
                        AppLanguage.TRADITIONAL_CHINESE -> "移向・移速"
                    },
                    value = if (analysis.course.isNotBlank()) "${analysis.course} ${analysis.speedText}" else analysis.speedText.ifBlank { "---" },
                    accentColor = colorScheme.onSurface,
                    bgColor = tileBg,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 最大風速 (M3 Tertiary)
                TyphoonMetricTile(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "最大風速"
                        AppLanguage.ENGLISH -> "Max Wind"
                        AppLanguage.SIMPLIFIED_CHINESE -> "最大风速"
                        AppLanguage.TRADITIONAL_CHINESE -> "最大風速"
                    },
                    value = analysis.maxWindMs?.let { "$it m/s" } ?: "---",
                    subtitle = analysis.maxWindMs?.let {
                        val kmh = (it * 3.6).toInt()
                        when (language) {
                            AppLanguage.JAPANESE -> "約 $kmh km/h"
                            AppLanguage.ENGLISH -> "~$kmh km/h"
                            AppLanguage.SIMPLIFIED_CHINESE -> "约 $kmh km/h"
                            AppLanguage.TRADITIONAL_CHINESE -> "約 $kmh km/h"
                        }
                    },
                    accentColor = colorScheme.tertiary,
                    bgColor = tileBg,
                    modifier = Modifier.weight(1f)
                )

                // 最大瞬間風速 (M3 Error)
                TyphoonMetricTile(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "最大瞬間風速"
                        AppLanguage.ENGLISH -> "Peak Gust"
                        AppLanguage.SIMPLIFIED_CHINESE -> "最大阵风"
                        AppLanguage.TRADITIONAL_CHINESE -> "最大陣風"
                    },
                    value = analysis.gustWindMs?.let { "$it m/s" } ?: "---",
                    subtitle = analysis.gustWindMs?.let {
                        val kmh = (it * 3.6).toInt()
                        when (language) {
                            AppLanguage.JAPANESE -> "約 $kmh km/h"
                            AppLanguage.ENGLISH -> "~$kmh km/h"
                            AppLanguage.SIMPLIFIED_CHINESE -> "约 $kmh km/h"
                            AppLanguage.TRADITIONAL_CHINESE -> "約 $kmh km/h"
                        }
                    },
                    accentColor = colorScheme.error,
                    bgColor = tileBg,
                    modifier = Modifier.weight(1f)
                )
            }

            // 強風域・暴風域の範囲情報
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tileBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 暴風域 (25m/s以上: M3 error)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorScheme.error)
                    )
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "暴風域 (25m/s以上): "
                            AppLanguage.ENGLISH -> "Storm (≥25m/s): "
                            AppLanguage.SIMPLIFIED_CHINESE -> "暴风圈 (≥25m/s): "
                            AppLanguage.TRADITIONAL_CHINESE -> "暴風圈 (≥25m/s): "
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analysis.stormRadiusKm?.let {
                            when (language) {
                                AppLanguage.JAPANESE -> "半径 ${it.toInt()} km"
                                AppLanguage.ENGLISH -> "Radius ${it.toInt()} km"
                                AppLanguage.SIMPLIFIED_CHINESE -> "半径 ${it.toInt()} km"
                                AppLanguage.TRADITIONAL_CHINESE -> "半徑 ${it.toInt()} km"
                            }
                        } ?: when (language) {
                            AppLanguage.JAPANESE -> "なし"
                            AppLanguage.ENGLISH -> "None"
                            AppLanguage.SIMPLIFIED_CHINESE -> "无"
                            AppLanguage.TRADITIONAL_CHINESE -> "無"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }

                // 強風域 (15m/s以上: M3 tertiary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorScheme.tertiary)
                    )
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "強風域 (15m/s以上): "
                            AppLanguage.ENGLISH -> "Gale (≥15m/s): "
                            AppLanguage.SIMPLIFIED_CHINESE -> "强风圈 (≥15m/s): "
                            AppLanguage.TRADITIONAL_CHINESE -> "強風圈 (≥15m/s): "
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analysis.galeRadiusKm?.let {
                            when (language) {
                                AppLanguage.JAPANESE -> "半径 ${it.toInt()} km"
                                AppLanguage.ENGLISH -> "Radius ${it.toInt()} km"
                                AppLanguage.SIMPLIFIED_CHINESE -> "半径 ${it.toInt()} km"
                                AppLanguage.TRADITIONAL_CHINESE -> "半徑 ${it.toInt()} km"
                            }
                        } ?: "---",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }

            // フッター情報 (中心位置 & 発表時刻)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (analysis.locationName.isNotBlank()) {
                    Text(
                        text = analysis.locationName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val agencyLabel = when (language) {
                    AppLanguage.JAPANESE -> "気象庁"
                    AppLanguage.ENGLISH -> "JMA"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象厅"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象廳"
                }
                Text(
                    text = "${typhoon.formattedIssueTime} ($agencyLabel)",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun TyphoonMetricTile(
    title: String,
    value: String,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            fontSize = 16.sp
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * スピードダイヤル用アイテムデータ
 */
private data class SpeedDialItemData(
    val title: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

/**
 * ボタンの中から飛び出すエクスプレッション弾性アニメーション付きスピードダイヤルピルUI
 * （非表示時は左下のボタンの位置に待機し、展開時に下から順にポヨンと飛び出す！）
 */
@Composable
private fun RadarNowClockText(
    nowPrefix: String,
    resTag: String?,
    style: androidx.compose.ui.text.TextStyle,
    color: Color
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var minuteEpoch by remember { mutableLongStateOf(System.currentTimeMillis() / 60_000L) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                minuteEpoch = now / 60_000L
                val remainingMs = (60_000L - (now % 60_000L)).coerceAtLeast(250L)
                delay(remainingMs)
            }
        }
    }

    val realTimeStr = remember(minuteEpoch) {
        timeFormatter.format(Date(minuteEpoch * 60_000L))
    }

    Text(
        text = if (resTag != null) "$nowPrefix $realTimeStr • $resTag" else "$nowPrefix $realTimeStr",
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        fontSize = 10.sp,
        lineHeight = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AnimatedSpeedDialItem(
    index: Int,
    isOpen: Boolean,
    item: SpeedDialItemData,
    bgColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    val initialYOffset = (index + 1) * 44f

    val animScale = remember { Animatable(0.85f) }
    val animAlpha = remember { Animatable(0f) }
    val animTransY = remember { Animatable(initialYOffset) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
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
                animTransY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = 1200f
                    )
                )
            }
        } else {
            launch { animAlpha.animateTo(0f, tween(35)) }
            launch { animScale.animateTo(0.85f, tween(45)) }
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
                    transformOrigin = TransformOrigin(0f, 1f)
                    clip = true
                    shape = CircleShape
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
                        if (item.iconRes != null) {
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
