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
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
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
    val nameKo: String,
    val impactKo: String,
    val warningKo: String,
    val nameEn: String,
    val impactEn: String,
    val warningEn: String,
    val nameZhCn: String,
    val impactZhCn: String,
    val warningZhCn: String,
    val nameZhTw: String,
    val impactZhTw: String,
    val warningZhTw: String,
    val nameEs: String,
    val impactEs: String,
    val warningEs: String,
    val nameFr: String,
    val impactFr: String,
    val warningFr: String,
    val namePt: String,
    val impactPt: String,
    val warningPt: String,
    val nameRu: String,
    val impactRu: String,
    val warningRu: String
) {
    fun name(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.KOREAN -> nameKo
        AppLanguage.ENGLISH -> nameEn
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhCn
        AppLanguage.TRADITIONAL_CHINESE -> nameZhTw
        AppLanguage.SPANISH -> nameEs
        AppLanguage.FRENCH -> nameFr
        AppLanguage.PORTUGUESE -> namePt
        AppLanguage.RUSSIAN -> nameRu
    }
    fun impact(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> impactJa
        AppLanguage.KOREAN -> impactKo
        AppLanguage.ENGLISH -> impactEn
        AppLanguage.SIMPLIFIED_CHINESE -> impactZhCn
        AppLanguage.TRADITIONAL_CHINESE -> impactZhTw
        AppLanguage.SPANISH -> impactEs
        AppLanguage.FRENCH -> impactFr
        AppLanguage.PORTUGUESE -> impactPt
        AppLanguage.RUSSIAN -> impactRu
    }
    fun warning(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> warningJa
        AppLanguage.KOREAN -> warningKo
        AppLanguage.ENGLISH -> warningEn
        AppLanguage.SIMPLIFIED_CHINESE -> warningZhCn
        AppLanguage.TRADITIONAL_CHINESE -> warningZhTw
        AppLanguage.SPANISH -> warningEs
        AppLanguage.FRENCH -> warningFr
        AppLanguage.PORTUGUESE -> warningPt
        AppLanguage.RUSSIAN -> warningRu
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
        nameKo = "극한 호우",
        impactKo = "숨쉬기 어려울 정도의 압박감. 빗물 보라로 앞이 보이지 않음.",
        warningKo = "중대한 재해 발생 위험이 매우 높음. 하천 범람 및 산사태에 최고 수준 경계.",
        nameEn = "Torrential Rain",
        impactEn = "Oppressive sensation making breathing difficult; front view blocked by spray.",
        warningEn = "Extreme disaster hazard. Maximum alert for river flooding and landslides.",
        nameZhCn = "特大暴雨",
        impactZhCn = "压迫感极强呼吸困难，漫天水花导致前方视线完全受阻。",
        warningZhCn = "发生重大灾害的危险性极高。对河流泛滥及山体滑坡泥石流保持最高级别警戒。",
        nameZhTw = "特大暴雨",
        impactZhTw = "壓迫感極強呼吸困難，漫天水花導致前方視線完全受阻。",
        warningZhTw = "發生重大災害的危險性極高。對河流氾濫及土石流保持最高級別警戒。",
        nameEs = "Lluvia torrencial",
        impactEs = "Sensación opresiva que dificulta respirar; la visibilidad queda bloqueada por la lluvia.",
        warningEs = "Riesgo extremo de desastre. Alerta máxima por desbordamiento de ríos y deslizamientos.",
        nameFr = "Pluie torrentielle",
        impactFr = "Sensation oppressante rendant la respiration difficile ; visibilité nulle à cause des projections.",
        warningFr = "Risque extrême de catastrophe. Alerte maximale aux crues et glissements de terrain.",
        namePt = "Chuva torrencial",
        impactPt = "Sensação opressiva que dificulta respirar; visibilidade bloqueada pela cortina de água.",
        warningPt = "Risco extremo de desastre. Alerta máximo para enchentes e deslizamentos de terra.",
        nameRu = "Проливной ливень",
        impactRu = "Тяжёлое ощущение духоты; видимость полностью перекрыта стеной воды.",
        warningRu = "Крайняя опасность бедствия. Максимальная тревога по наводнениям и оползням."
    ),
    RainIntensityLevel(
        rangeLabel = "50〜80",
        nameJa = "非常に激しい雨",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "滝のように降る。傘は全く役に立たない。",
        warningJa = "地下街や低地への浸水・土砂崩れ・中小河川の急な増水に警戒。",
        nameKo = "매우 강한 폭우",
        impactKo = "폭포처럼 쏟아지는 비. 우산이 전혀 소용없음.",
        warningKo = "지하 공간·저지대 침수, 산사태, 중소 하천의 급격한 범람에 경계.",
        nameEn = "Extremely Heavy Rain",
        impactEn = "Pours like a waterfall; umbrellas are completely useless.",
        warningEn = "Alert for low-lying inundation, basement flooding, landslides, and river surges.",
        nameZhCn = "大暴雨",
        impactZhCn = "暴雨如瀑布倾泻，雨伞完全失去遮挡作用。",
        warningZhCn = "警惕地下商城、低洼地淹水积水、山体滑坡及中小河流突发暴涨。",
        nameZhTw = "大暴雨",
        impactZhTw = "暴雨如瀑布傾瀉，雨傘完全失去遮擋作用。",
        warningZhTw = "警惕地下商城、低窪地淹水積水、土石流及中小河流突發暴漲。",
        nameEs = "Lluvia muy intensa",
        impactEs = "Cae como una cascada; el paraguas no sirve de nada.",
        warningEs = "Alerta por inundaciones en zonas bajas y sótanos, deslizamientos y crecidas repentinas.",
        nameFr = "Pluie très violente",
        impactFr = "Tombe comme une cascade ; un parapluie est totalement inutile.",
        warningFr = "Vigilance face aux inondations de zones basses et sous-sols, glissements et crues rapides.",
        namePt = "Chuva muito forte",
        impactPt = "Cai como uma cachoeira; o guarda-chuva não tem utilidade alguma.",
        warningPt = "Alerta para alagamentos em áreas baixas e subsolos, deslizamentos e cheias rápidas.",
        nameRu = "Очень сильный ливень",
        impactRu = "Льёт как водопад; зонт совершенно бесполезен.",
        warningRu = "Опасность подтопления низин и подвалов, оползней и резкого подъёма рек."
    ),
    RainIntensityLevel(
        rangeLabel = "30〜50",
        nameJa = "激しい雨",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "バケツをひっくり返したように降る。",
        warningJa = "道路が川のようになり、高速走行の車はハイドロプレーニングを起こす。",
        nameKo = "매우 강한 비",
        impactKo = "양동이로 물을 붓듯 쏟아지는 비.",
        warningKo = "도로가 물에 잠기며, 고속 주행 차량은 수막현상 위험이 높음.",
        nameEn = "Heavy Rain",
        impactEn = "Pours as if buckets of water were dumped.",
        warningEn = "Roads become rivers; fast-moving vehicles risk hydroplaning.",
        nameZhCn = "暴雨",
        impactZhCn = "倾盆大雨，犹如打翻水桶般猛烈倾泻。",
        warningZhCn = "道路易成水流河流，高速行驶车辆极易发生水漂打滑失控。",
        nameZhTw = "暴雨",
        impactZhTw = "傾盆大雨，猶如打翻水桶般猛烈傾瀉。",
        warningZhTw = "道路易成水流河流，高速行駛車輛極易發生水漂打滑失控。",
        nameEs = "Lluvia intensa",
        impactEs = "Llueve a cántaros, como si volcaran cubos de agua.",
        warningEs = "Las calles parecen ríos; los vehículos a alta velocidad corren riesgo de aquaplaning.",
        nameFr = "Pluie forte",
        impactFr = "Il pleut à seaux, comme des trombes d'eau.",
        warningFr = "Les routes se transforment en ruisseaux ; risque d'aquaplaning à vive allure.",
        namePt = "Chuva forte",
        impactPt = "Chove torrencialmente, como se despejassem baldes de água.",
        warningPt = "As vias ficam alagadas; veículos em alta velocidade correm risco de aquaplanagem.",
        nameRu = "Сильный ливень",
        impactRu = "Льёт как из ведра.",
        warningRu = "Дороги превращаются в потоки воды; на высокой скорости велик риск аквапланирования."
    ),
    RainIntensityLevel(
        rangeLabel = "20〜30",
        nameJa = "強い雨",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "土砂降りの雨。傘をさしていても濡れる。",
        warningJa = "側溝や下水、小さな川から水があふれ出すことがある。",
        nameKo = "강한 비",
        impactKo = "장대비 수준으로, 우산을 써도 옷이 젖음.",
        warningKo = "배수로, 하수구 및 작은 하천이 넘칠 수 있음.",
        nameEn = "Strong Rain",
        impactEn = "Pouring rain; clothes get wet even holding an umbrella.",
        warningEn = "Street gutters, drains, and small creeks may overflow.",
        nameZhCn = "大雨",
        impactZhCn = "大雨倾盆，即使撑伞身体依然容易被淋湿。",
        warningZhCn = "排水沟、城市下水道及小溪沟可能会溢水泛滥。",
        nameZhTw = "大雨",
        impactZhTw = "大雨傾盆，即使撐傘身體依然容易被淋濕。",
        warningZhTw = "排水溝、城市下水道及小溪溝可能會溢水氾濫。",
        nameEs = "Lluvia fuerte",
        impactEs = "Aguacero intenso; la ropa se moja incluso con paraguas.",
        warningEs = "Las alcantarillas, cunetas y arroyos pequeños pueden desbordarse.",
        nameFr = "Forte averse",
        impactFr = "Pluie battante ; les vêtements sont mouillés même avec un parapluie.",
        warningFr = "Les caniveaux, égouts et petits cours d'eau peuvent déborder.",
        namePt = "Chuva intensa",
        impactPt = "Aguaceiro forte; a roupa molha mesmo usando guarda-chuva.",
        warningPt = "Bueiros, sarjetas e pequenos córregos podem transbordar.",
        nameRu = "Сильный дождь",
        impactRu = "Проливной дождь; одежда намокает даже под зонтом.",
        warningRu = "Ливнёвки, канавы и небольшие ручьи могут переполниться."
    ),
    RainIntensityLevel(
        rangeLabel = "10〜20",
        nameJa = "やや強い雨",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "ザーザーと音を立てて降る。足元が濡れる。",
        warningJa = "地面一面に水たまりができる。長く屋外にいると服にしみ込む。",
        nameKo = "다소 강한 비",
        impactKo = "빗소리가 크게 들리며 신발과 발밑이 젖음.",
        warningKo = "노면 곳곳에 물웅덩이가 생기고 오래 야외에 있으면 옷이 젖음.",
        nameEn = "Moderate Rain",
        impactEn = "Rustling rainfall with loud sound; shoes and feet get wet.",
        warningEn = "Puddles form across surfaces; moisture penetrates clothes over time.",
        nameZhCn = "中到大雨",
        impactZhCn = "哗哗作响的雨声清晰可闻，地面溅水打湿鞋袜。",
        warningZhCn = "路面到处形成水洼，长时间停留户外衣物会逐渐渗透湿透。",
        nameZhTw = "中到大雨",
        impactZhTw = "嘩嘩作響的雨聲清晰可聞，地面濺水打濕鞋襪。",
        warningZhTw = "路面到處形成水窪，長時間停留戶外衣物會逐漸滲透濕透。",
        nameEs = "Lluvia moderada",
        impactEs = "Lluvia con sonido constante; el calzado y los pies se mojan fácilmente.",
        warningEs = "Se forman charcos por todo el suelo; la humedad cala si permanece mucho tiempo fuera.",
        nameFr = "Pluie modérée",
        impactFr = "Pluie bruyante et soutenue ; les chaussures et les pieds sont vite mouillés.",
        warningFr = "Des flaques se forment partout ; l'humidité traverse les vêtements à la longue.",
        namePt = "Chuva moderada",
        impactPt = "Chuva com barulho constante; calçados e pés se molham com facilidade.",
        warningPt = "Formam-se poças por todo o chão; a umidade encharca a roupa com o tempo.",
        nameRu = "Умеренно сильный дождь",
        impactRu = "Шумный постоянный дождь; обувь и ноги быстро промокают.",
        warningRu = "Повсюду образуются лужи; при долгом пребывании на улице одежда промокает."
    ),
    RainIntensityLevel(
        rangeLabel = "5〜10",
        nameJa = "本降り",
        color = Color(0xFF218CFF),
        onColor = Color.White,
        impactJa = "雨の音が周囲にはっきり聞こえるまとまった雨。",
        warningJa = "傘をさして歩く必要がある。水たまりが広くできる。",
        nameKo = "보통 비",
        impactKo = "주변에서 빗소리가 뚜렷하게 들리는 꾸준한 비.",
        warningKo = "외출 시 우산이 필요하며 곳곳에 물웅덩이가 생김.",
        nameEn = "Steady Rain",
        impactEn = "Continuous steady rain with clear sound.",
        warningEn = "Umbrellas required for walking outside; widespread small puddles.",
        nameZhCn = "中雨",
        impactZhCn = "周围能清楚听到雨声的整场连绵降雨。",
        warningZhCn = "外出必须撑伞步行，低洼地面开始形成明显积水。",
        nameZhTw = "中雨",
        impactZhTw = "周圍能清楚聽到雨聲的整場連綿降雨。",
        warningZhTw = "外出必須撐傘步行，低窪地面開始形成明顯積水。",
        nameEs = "Lluvia constante",
        impactEs = "Lluvia continua cuyo sonido se oye con claridad alrededor.",
        warningEs = "Se necesita paraguas para caminar afuera; se forman numerosos charcos.",
        nameFr = "Pluie régulière",
        impactFr = "Pluie continue dont le bruit est nettement audible.",
        warningFr = "Parapluie nécessaire pour marcher dehors ; nombreuses petites flaques.",
        namePt = "Chuva contínua",
        impactPt = "Chuva constante com som bem audível ao redor.",
        warningPt = "É necessário guarda-chuva para sair; formam-se várias poças.",
        nameRu = "Умеренный дождь",
        impactRu = "Ровный дождь с отчётливым шумом вокруг.",
        warningRu = "Для прогулки необходим зонт; образуются многочисленные лужи."
    ),
    RainIntensityLevel(
        rangeLabel = "1〜5",
        nameJa = "弱い雨",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "しとしとと降る雨。傘をささないと濡れる。",
        warningJa = "外出時は折りたたみ傘などの携帯が推奨される。",
        nameKo = "약한 비",
        impactKo = "부슬부슬 내리는 비. 우산이 없으면 젖음.",
        warningKo = "외출 시 접이식 우산 등을 챙기는 것이 좋음.",
        nameEn = "Light Rain",
        impactEn = "Gentle pattering rain; you will get wet without cover.",
        warningEn = "Carrying a compact folding umbrella is recommended outdoors.",
        nameZhCn = "小雨",
        impactZhCn = "淅淅沥沥的细雨，不撑伞会被打湿。",
        warningZhCn = "出门建议随身携带便携折叠雨伞。",
        nameZhTw = "小雨",
        impactZhTw = "淅淅瀝瀝的細雨，不撐傘會被打濕。",
        warningZhTw = "出門建議隨身攜帶便攜折疊雨傘。",
        nameEs = "Lluvia ligera",
        impactEs = "Lluvia suave; te mojarás si no llevas protección.",
        warningEs = "Se recomienda llevar un paraguas plegable al salir.",
        nameFr = "Pluie faible",
        impactFr = "Pluie fine et douce ; vous serez mouillé sans protection.",
        warningFr = "Un parapluie pliant est recommandé pour sortir.",
        namePt = "Chuva fraca",
        impactPt = "Chuva leve e suave; você se molhará sem proteção.",
        warningPt = "Recomenda-se levar um guarda-chuva dobrável ao sair.",
        nameRu = "Слабый дождь",
        impactRu = "Тихий моросящий дождь; без зонта можно промокнуть.",
        warningRu = "Рекомендуется взять с собой складной зонт."
    ),
    RainIntensityLevel(
        rangeLabel = "0.1〜1",
        nameJa = "小雨・霧雨",
        color = Color(0xFFD4E3F5),
        onColor = Color(0xFF1E293B),
        impactJa = "傘がなくても何とか耐えられる程度の微弱な雨。",
        warningJa = "地面がうっすら湿る程度。長時間の歩行は傘があると安心。",
        nameKo = "가랑비·이슬비",
        impactKo = "우산 없이도 잠시 견딜 수 있는 아주 약한 비.",
        warningKo = "지면이 살짝 젖는 정도이며 장시간 보행 시 우산이 있으면 안심.",
        nameEn = "Drizzle",
        impactEn = "Faint mist or drizzle barely bearable without an umbrella.",
        warningEn = "Ground is slightly dampened; having an umbrella is helpful for long walks.",
        nameZhCn = "微雨・毛毛雨",
        impactZhCn = "极微弱的霏霏细雨，即便不撑伞也能短时间耐受。",
        warningZhCn = "路面微微湿润，长时间室外行走备伞更加安心。",
        nameZhTw = "微雨・毛毛雨",
        impactZhTw = "極微弱的霏霏細雨，即便不撐傘也能短時間耐受。",
        warningZhTw = "路面微微濕潤，長時間室外行走備傘更加安心。",
        nameEs = "Llovizna",
        impactEs = "Llovizna muy débil, soportable brevemente sin paraguas.",
        warningEs = "El suelo apenas se humedece; conviene llevar paraguas en caminatas largas.",
        nameFr = "Bruine",
        impactFr = "Fine bruine supportable brièvement sans parapluie.",
        warningFr = "Le sol est légèrement humide ; un parapluie est utile pour marcher longtemps.",
        namePt = "Garoa",
        impactPt = "Garoa muito leve, suportável por pouco tempo sem guarda-chuva.",
        warningPt = "O chão fica levemente úmido; um guarda-chuva ajuda em caminhadas longas.",
        nameRu = "Морось",
        impactRu = "Очень слабый дождь, переносимый недолго без зонта.",
        warningRu = "Земля слегка влажная; при долгой ходьбе пригодится зонт."
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
    val nameKo: String,
    val impactKo: String,
    val warningKo: String,
    val nameEn: String,
    val impactEn: String,
    val warningEn: String,
    val nameZhCn: String,
    val impactZhCn: String,
    val warningZhCn: String,
    val nameZhTw: String,
    val impactZhTw: String,
    val warningZhTw: String,
    val nameEs: String,
    val impactEs: String,
    val warningEs: String,
    val nameFr: String,
    val impactFr: String,
    val warningFr: String,
    val namePt: String,
    val impactPt: String,
    val warningPt: String,
    val nameRu: String,
    val impactRu: String,
    val warningRu: String
) {
    fun name(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.KOREAN -> nameKo
        AppLanguage.ENGLISH -> nameEn
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhCn
        AppLanguage.TRADITIONAL_CHINESE -> nameZhTw
        AppLanguage.SPANISH -> nameEs
        AppLanguage.FRENCH -> nameFr
        AppLanguage.PORTUGUESE -> namePt
        AppLanguage.RUSSIAN -> nameRu
    }
    fun impact(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> impactJa
        AppLanguage.KOREAN -> impactKo
        AppLanguage.ENGLISH -> impactEn
        AppLanguage.SIMPLIFIED_CHINESE -> impactZhCn
        AppLanguage.TRADITIONAL_CHINESE -> impactZhTw
        AppLanguage.SPANISH -> impactEs
        AppLanguage.FRENCH -> impactFr
        AppLanguage.PORTUGUESE -> impactPt
        AppLanguage.RUSSIAN -> impactRu
    }
    fun warning(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> warningJa
        AppLanguage.KOREAN -> warningKo
        AppLanguage.ENGLISH -> warningEn
        AppLanguage.SIMPLIFIED_CHINESE -> warningZhCn
        AppLanguage.TRADITIONAL_CHINESE -> warningZhTw
        AppLanguage.SPANISH -> warningEs
        AppLanguage.FRENCH -> warningFr
        AppLanguage.PORTUGUESE -> warningPt
        AppLanguage.RUSSIAN -> warningRu
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
        nameKo = "매우 강한 번개",
        impactKo = "맹렬한 낙뢰 발생. 격렬한 뇌우·돌풍·우박 위험이 매우 높음.",
        warningKo = "중대한 낙뢰 사고 우려. 즉시 튼튼한 건물이나 차량 내부 등 안전한 곳으로 대피.",
        nameEn = "Violent Lightning",
        impactEn = "Intense lightning activity; severe squalls and hail very likely.",
        warningEn = "Extreme lightning hazard. Immediately seek shelter inside sturdy buildings or vehicles.",
        nameZhCn = "剧烈雷电",
        impactZhCn = "雷电活动极为猛烈，极易伴随短时雷暴大风与冰雹灾害。",
        warningZhCn = "雷击风险极高，请立即前往坚固建筑物或密闭车厢内避险。",
        nameZhTw = "劇烈雷電",
        impactZhTw = "雷電活動極為猛烈，極易伴隨短時雷暴大風與冰雹災害。",
        warningZhTw = "雷擊風險極高，請立即前往堅固建築物或密閉車廂內避險。",
        nameEs = "Rayos violentos",
        impactEs = "Actividad eléctrica intensa; muy probable tormenta severa, rachas y granizo.",
        warningEs = "Peligro extremo de rayos. Refúgiese de inmediato en un edificio sólido o vehículo.",
        nameFr = "Foudre violente",
        impactFr = "Activité électrique intense ; rafales violentes et grêle très probables.",
        warningFr = "Danger extrême de foudre. Abritez-vous immédiatement dans un bâtiment solide ou un véhicule.",
        namePt = "Raios intensos",
        impactPt = "Atividade elétrica intensa; alta probabilidade de tempestade severa, rajadas e granizo.",
        warningPt = "Risco extremo de raios. Abrigue-se imediatamente em um prédio resistente ou veículo.",
        nameRu = "Сильнейшая гроза",
        impactRu = "Интенсивные разряды молний; очень вероятны шквалы и град.",
        warningRu = "Крайняя опасность молний. Немедленно укройтесь в прочном здании или автомобиле."
    ),
    RadarLegendLevelItem(
        rangeLabel = "活動度 3",
        nameJa = "雷",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "落雷が多数発生している。発雷の危険性が高い。",
        warningJa = "落雷の危険あり。周囲の天候急変に警戒し、速やかに屋内へ退避。",
        nameKo = "번개",
        impactKo = "낙뢰가 다수 발생 중이며 추가 낙뢰 위험이 높음.",
        warningKo = "낙뢰 위험이 있음. 주변 기상 급변에 경계하고 신속히 실내로 대피.",
        nameEn = "Active Lightning",
        impactEn = "Frequent lightning strikes observed; high risk of lightning hazard.",
        warningEn = "High risk of lightning strikes. Move indoors promptly.",
        nameZhCn = "强雷电",
        impactZhCn = "已监测到多起落雷，雷电发生频率高。",
        warningZhCn = "有雷击危险，请警惕周围天气突变并尽快退避室内。",
        nameZhTw = "強雷電",
        impactZhTw = "已監測到多起落雷，雷電發生頻率高。",
        warningZhTw = "有雷擊危險，請警惕周圍天氣突變並儘快退避室內。",
        nameEs = "Rayos activos",
        impactEs = "Se registran numerosos rayos; alto riesgo de descargas eléctricas.",
        warningEs = "Alto riesgo de impacto de rayo. Busque refugio en interiores cuanto antes.",
        nameFr = "Foudre active",
        impactFr = "Nombreux impacts de foudre observés ; risque élevé d'orage électrique.",
        warningFr = "Risque élevé de foudroiement. Mettez-vous rapidement à l'abri à l'intérieur.",
        namePt = "Raios ativos",
        impactPt = "Vários raios registrados; alto risco de descargas elétricas.",
        warningPt = "Alto risco de queda de raios. Procure abrigo em local fechado rapidamente.",
        nameRu = "Активная гроза",
        impactRu = "Наблюдаются частые удары молний; высокая грозовая опасность.",
        warningRu = "Высокий риск удара молнии. Как можно скорее перейдите в помещение."
    ),
    RadarLegendLevelItem(
        rangeLabel = "活動度 2",
        nameJa = "雷の可能性",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "1時間以内に落雷の可能性が高い。雷雲が活発に発達中。",
        warningJa = "屋外活動時は空の様子に注意し、いつでも避難できる態勢を準備。",
        nameKo = "번개 가능성",
        impactKo = "1시간 이내 낙뢰 가능성이 높으며 뇌운이 활발히 발달 중.",
        warningKo = "야외 활동 시 하늘 상태를 살피고 언제든 대피할 수 있도록 준비.",
        nameEn = "Lightning Likely",
        impactEn = "High probability of lightning within 1 hour; thunderclouds actively developing.",
        warningEn = "Monitor sky conditions during outdoor activities; prepare to shelter.",
        nameZhCn = "有雷电可能",
        impactZhCn = "未来1小时内发生落雷概率高，雷暴云正在持续发展。",
        warningZhCn = "户外活动需密切注意云层变化，做好随时避雷准备。",
        nameZhTw = "有雷電可能",
        impactZhTw = "未來1小時內發生落雷機率高，雷暴雲正在持續發展。",
        warningZhTw = "戶外活動需密切注意雲層變化，做好隨時避雷準備。",
        nameEs = "Rayos probables",
        impactEs = "Alta probabilidad de rayos en menos de 1 hora; nubes de tormenta en desarrollo.",
        warningEs = "Vigile el cielo en actividades al aire libre y prepárese para refugiarse.",
        nameFr = "Foudre probable",
        impactFr = "Forte probabilité de foudre sous 1 heure ; nuages orageux en plein développement.",
        warningFr = "Surveillez le ciel lors d'activités en extérieur et préparez-vous à vous abriter.",
        namePt = "Raios prováveis",
        impactPt = "Alta probabilidade de raios em até 1 hora; nuvens de tempestade em desenvolvimento.",
        warningPt = "Observe o céu em atividades ao ar livre e prepare-se para buscar abrigo.",
        nameRu = "Вероятна гроза",
        impactRu = "Высокая вероятность молний в течение 1 часа; активно развиваются грозовые облака.",
        warningRu = "Следите за небом на открытом воздухе и будьте готовы укрыться."
    ),
    RadarLegendLevelItem(
        rangeLabel = "活動度 1",
        nameJa = "雷の兆候",
        color = Color(0xFF218CFF),
        onColor = Color.White,
        impactJa = "1時間以内に発雷の可能性がある（雷雲の兆候）。",
        warningJa = "急な強い雨や突風など、天候の急変に留意。",
        nameKo = "번개 징후",
        impactKo = "1시간 이내 번개 발생 가능성 있음(뇌운 징후).",
        warningKo = "갑작스러운 강한 비나 돌풍 등 날씨 급변에 유의.",
        nameEn = "Signs of Lightning",
        impactEn = "Potential for lightning development within 1 hour.",
        warningEn = "Be aware of sudden showers or gusts associated with developing clouds.",
        nameZhCn = "雷电征兆",
        impactZhCn = "未来1小时内可能伴随雷电发生（初生雷云）。",
        warningZhCn = "防范局地突发阵雨、雷阵雨及短时阵风。",
        nameZhTw = "雷電徵兆",
        impactZhTw = "未來1小時內可能伴隨雷電發生（初生雷雲）。",
        warningZhTw = "防範局地突發陣雨、雷陣雨及短時陣風。",
        nameEs = "Indicios de rayos",
        impactEs = "Posibilidad de formación de rayos en la próxima hora.",
        warningEs = "Esté atento a chubascos repentinos o rachas de viento.",
        nameFr = "Signes d'orage",
        impactFr = "Risque d'apparition de foudre dans l'heure qui vient.",
        warningFr = "Méfiez-vous des averses soudaines ou rafales liées aux nuages en formation.",
        namePt = "Indícios de raios",
        impactPt = "Possibilidade de formação de raios na próxima hora.",
        warningPt = "Fique atento a pancadas súbitas de chuva ou rajadas de vento.",
        nameRu = "Признаки грозы",
        impactRu = "Возможны разряды молний в течение ближайшего часа.",
        warningRu = "Остерегайтесь внезапных ливней и порывов ветра."
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
        nameKo = "재해 임박 (5단계)",
        impactKo = "이미 재해가 발생했거나 임박한 극도로 위험한 상황.",
        warningKo = "생명의 위험. 즉시 신변 안전을 확보하세요(생명을 지키기 위한 최선의 행동).",
        nameEn = "Emergency (Level 5)",
        impactEn = "Disaster has already occurred or is imminent; extreme life-threatening risk.",
        warningEn = "Take immediate life-saving action; secure personal safety without delay.",
        nameZhCn = "灾害切迫 (5级)",
        impactZhCn = "灾害已经发生或迫在眉睫，面临严重生命威胁。",
        warningZhCn = "生命危在旦夕！请立即采取最有利于保全生命的避险行动。",
        nameZhTw = "災害切迫 (5級)",
        impactZhTw = "災害已經發生或迫在眉睫，面臨嚴重生命威脅。",
        warningZhTw = "生命危在旦夕！請立即採取最有利於保全生命的避險行動。",
        nameEs = "Emergencia (Nivel 5)",
        impactEs = "El desastre ya ocurrió o es inminente; peligro extremo para la vida.",
        warningEs = "Tome medidas inmediatas para salvar su vida y póngase a salvo sin demora.",
        nameFr = "Urgence (Niveau 5)",
        impactFr = "Catastrophe déjà en cours ou imminente ; danger vital extrême.",
        warningFr = "Prenez immédiatement les mesures pour sauver votre vie et vous mettre en sécurité.",
        namePt = "Emergência (Nível 5)",
        impactPt = "Desastre já ocorrido ou iminente; risco extremo à vida.",
        warningPt = "Tome medidas imediatas para preservar sua vida e garanta sua segurança sem demora.",
        nameRu = "Чрезвычайная ситуация (Ур. 5)",
        impactRu = "Бедствие уже произошло или неизбежно; крайняя угроза жизни.",
        warningRu = "Немедленно примите меры для спасения жизни и обеспечьте безопасность."
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 4",
        nameJa = "危険",
        color = Color(0xFFB84FB3),
        onColor = Color.White,
        impactJa = "重大な災害が発生する可能性が非常に高く、全員避難が必要。",
        warningJa = "警戒レベル4相当。危険な場所から全員速やかに避難してください。",
        nameKo = "위험 (4단계)",
        impactKo = "중대한 재해 발생 가능성이 매우 높아 전원 대피가 필요.",
        warningKo = "경계 레벨 4 상당. 위험 지역에서 모두 신속히 대피하세요.",
        nameEn = "Danger (Level 4)",
        impactEn = "Very high probability of severe disaster; all residents must evacuate.",
        warningEn = "Equivalent to Alert Level 4. Evacuate dangerous zones immediately.",
        nameZhCn = "危险 (4级)",
        impactZhCn = "极可能发生重大灾害，危险区域内所有人员必须紧急撤离。",
        warningZhCn = "相当于警戒级别4。危险地带所有人员请迅速撤离避难。",
        nameZhTw = "危險 (4級)",
        impactZhTw = "極可能發生重大災害，危險區域內所有人員必須緊急撤離。",
        warningZhTw = "相當於警戒級別4。危險地帶所有人員請迅速撤離避難。",
        nameEs = "Peligro (Nivel 4)",
        impactEs = "Probabilidad muy alta de desastre grave; todos los residentes deben evacuar.",
        warningEs = "Equivalente al Nivel de Alerta 4. Evacúe las zonas peligrosas de inmediato.",
        nameFr = "Danger (Niveau 4)",
        impactFr = "Probabilité très élevée de catastrophe grave ; évacuation générale requise.",
        warningFr = "Équivalent au niveau d'alerte 4. Évacuez immédiatement les zones à risque.",
        namePt = "Perigo (Nível 4)",
        impactPt = "Probabilidade muito alta de desastre grave; evacuação geral necessária.",
        warningPt = "Equivalente ao Nível de Alerta 4. Evacue imediatamente as áreas de risco.",
        nameRu = "Опасность (Ур. 4)",
        impactRu = "Очень высокая вероятность крупного бедствия; необходима общая эвакуация.",
        warningRu = "Соответствует уровню тревоги 4. Немедленно покиньте опасные зоны."
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 3",
        nameJa = "警戒",
        color = Color(0xFFFF5252),
        onColor = Color.White,
        impactJa = "警報基準を超過。高齢者等の避難行動が必要とされる状況。",
        warningJa = "警戒レベル3相当。高齢者や避難に時間を要する方は避難を開始。",
        nameKo = "경계 (3단계)",
        impactKo = "경보 기준 초과. 고령자 등 대피 취약 계층의 대피가 필요한 상황.",
        warningKo = "경계 레벨 3 상당. 고령자 및 대피에 시간이 걸리는 분은 대피를 시작하세요.",
        nameEn = "Warning (Level 3)",
        impactEn = "Exceeds warning criteria; evacuation of vulnerable individuals required.",
        warningEn = "Equivalent to Alert Level 3. Seniors and vulnerable people should evacuate.",
        nameZhCn = "警戒 (3级)",
        impactZhCn = "超过灾害警报标准，易受灾群体需要提前开始转移撤离。",
        warningZhCn = "相当于警戒级别3。老年人及行动不便者应尽早从危险地带避难。",
        nameZhTw = "警戒 (3級)",
        impactZhTw = "超過災害警報標準，易受災群體需要提前開始轉移撤離。",
        warningZhTw = "相當於警戒級別3。老年人及行動不便者應儘早從危險地帶避難。",
        nameEs = "Alerta (Nivel 3)",
        impactEs = "Supera los criterios de alerta; las personas vulnerables deben evacuar.",
        warningEs = "Equivalente al Nivel de Alerta 3. Personas mayores y vulnerables deben iniciar la evacuación.",
        nameFr = "Alerte (Niveau 3)",
        impactFr = "Seuils d'alerte dépassés ; évacuation requise pour les personnes vulnérables.",
        warningFr = "Équivalent au niveau d'alerte 3. Les personnes âgées et vulnérables doivent évacuer.",
        namePt = "Alerta (Nível 3)",
        impactPt = "Supera os critérios de alerta; pessoas vulneráveis devem evacuar.",
        warningPt = "Equivalente ao Nível de Alerta 3. Idosos e pessoas vulneráveis devem iniciar a evacuação.",
        nameRu = "Тревога (Ур. 3)",
        impactRu = "Превышены критерии предупреждения; требуется эвакуация уязвимых групп.",
        warningRu = "Соответствует уровню тревоги 3. Пожилым и уязвимым следует начать эвакуацию."
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 2",
        nameJa = "注意",
        color = Color(0xFFFDD835),
        onColor = Color(0xFF212121),
        impactJa = "注意報基準を超過。今後の気象推移に警戒が必要。",
        warningJa = "警戒レベル2相当。ハザードマップで避難経路や避難先を確認。",
        nameKo = "주의 (2단계)",
        impactKo = "주의보 기준 초과. 향후 기상 변화에 주의가 필요.",
        warningKo = "경계 레벨 2 상당. 재해 위험 지도로 대피 경로와 대피소를 확인하세요.",
        nameEn = "Advisory (Level 2)",
        impactEn = "Exceeds advisory criteria; preparedness checks recommended.",
        warningEn = "Equivalent to Alert Level 2. Check hazard maps and evacuation routes.",
        nameZhCn = "注意 (2级)",
        impactZhCn = "达到气象注意报标准，需要提高警惕注意后续变化。",
        warningZhCn = "相当于警戒级别2。确认防灾避险路线及应急避难场所。",
        nameZhTw = "注意 (2級)",
        impactZhTw = "達到氣象注意報標準，需要提高警惕注意後續變化。",
        warningZhTw = "相當於警戒級別2。確認防災避險路線及應急避難場所。",
        nameEs = "Aviso (Nivel 2)",
        impactEs = "Supera los criterios de aviso; se recomienda comprobar la preparación.",
        warningEs = "Equivalente al Nivel de Alerta 2. Revise los mapas de riesgo y rutas de evacuación.",
        nameFr = "Vigilance (Niveau 2)",
        impactFr = "Seuils de vigilance dépassés ; vérifiez vos préparatifs.",
        warningFr = "Équivalent au niveau d'alerte 2. Consultez les cartes de risque et itinéraires d'évacuation.",
        namePt = "Atenção (Nível 2)",
        impactPt = "Supera os critérios de aviso; recomenda-se verificar a preparação.",
        warningPt = "Equivalente ao Nível de Alerta 2. Verifique mapas de risco e rotas de evacuação.",
        nameRu = "Внимание (Ур. 2)",
        impactRu = "Превышены критерии оповещения; рекомендуется проверить готовность.",
        warningRu = "Соответствует уровню тревоги 2. Проверьте карты рисков и маршруты эвакуации."
    ),
    RadarLegendLevelItem(
        rangeLabel = "Lv 1",
        nameJa = "今後の情報等",
        color = Color(0xFFB0BEC5),
        onColor = Color(0xFF212121),
        impactJa = "現時点で災害発生の危険度は低いが、今後の雨に留意。",
        warningJa = "最新の防災気象情報や今後の推移を確認してください。",
        nameKo = "향후 기상 정보 유의 (1단계)",
        impactKo = "현재 재해 발생 위험도는 낮으나 향후 강수 변화에 유의.",
        warningKo = "최신 방재 기상 정보와 향후 추이를 확인하세요.",
        nameEn = "Caution (Level 1)",
        impactEn = "Currently low danger, but monitor evolving weather conditions.",
        warningEn = "Stay tuned to the latest weather bulletins and updates.",
        nameZhCn = "早期注意 (1级)",
        impactZhCn = "当前危险度处于低位，但需关注未来降水发展。",
        warningZhCn = "请持续留意最新发布的气象灾害防范预报信息。",
        nameZhTw = "早期注意 (1級)",
        impactZhTw = "當前危險度處於低位，但需關注未來降水發展。",
        warningZhTw = "請持續留意最新發布的氣象災害防範預報資訊。",
        nameEs = "Precaución (Nivel 1)",
        impactEs = "Riesgo actualmente bajo, pero vigile la evolución del tiempo.",
        warningEs = "Manténgase atento a los últimos boletines y avisos meteorológicos.",
        nameFr = "Prudence (Niveau 1)",
        impactFr = "Risque actuellement faible, mais surveillez l'évolution météorologique.",
        warningFr = "Restez attentif aux derniers bulletins et mises à jour météo.",
        namePt = "Precaução (Nível 1)",
        impactPt = "Risco atualmente baixo, mas acompanhe a evolução do tempo.",
        warningPt = "Acompanhe os últimos boletins e atualizações meteorológicas.",
        nameRu = "Предосторожность (Ур. 1)",
        impactRu = "Опасность пока низкая, но следите за развитием погодной ситуации.",
        warningRu = "Следите за свежими метеосводками и обновлениями."
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
        nameKo = "200cm 이상",
        impactKo = "기록적 폭설. 건물 붕괴, 지역 고립, 교통 완전 마비 위험.",
        warningKo = "대규모 눈사태 및 지붕 붕괴에 엄중 경계. 불필요한 외출을 삼가세요.",
        nameEn = "200cm or more",
        impactEn = "Record catastrophic snowfall; threat of structural collapse and isolation.",
        warningEn = "Extreme avalanche danger. Avoid all unnecessary travel.",
        nameZhCn = "200cm以上",
        impactZhCn = "历史罕见特大暴雪，建筑物压塌风险极高，交通全线瘫痪。",
        warningZhCn = "严密防范大型雪崩与房屋倒塌，切勿盲目外出。",
        nameZhTw = "200cm以上",
        impactZhTw = "歷史罕見特大暴雪，建築物壓塌風險極高，交通全線癱瘓。",
        warningZhTw = "嚴密防範大型雪崩與房屋倒塌，切勿盲目外出。",
        nameEs = "200 cm o más",
        impactEs = "Nevada histórica catastrófica; riesgo de colapso estructural y aislamiento.",
        warningEs = "Peligro extremo de avalanchas. Evite cualquier desplazamiento innecesario.",
        nameFr = "200 cm ou plus",
        impactFr = "Enneigement record catastrophique ; risque d'effondrement et d'isolement.",
        warningFr = "Danger extrême d'avalanche. Évitez tout déplacement non essentiel.",
        namePt = "200 cm ou mais",
        impactPt = "Nevasca recorde catastrófica; risco de desabamento e isolamento.",
        warningPt = "Perigo extremo de avalanches. Evite qualquer deslocamento desnecessário.",
        nameRu = "200 см и более",
        impactRu = "Рекордный снегопад; угроза обрушения конструкций и изоляции.",
        warningRu = "Крайняя лавинная опасность. Избегайте любых необязательных поездок."
    ),
    RadarLegendLevelItem(
        rangeLabel = "100〜200",
        nameJa = "100〜200cm",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "極めて深い積雪。屋根雪下ろしの危険や大規模な通行止めが発生。",
        warningJa = "雪崩や落雪事故、車の立ち往生に厳重警戒。",
        nameKo = "100~200cm",
        impactKo = "매우 깊은 적설. 지붕 제설 위험 및 대규모 도로 통제 발생.",
        warningKo = "눈사태, 낙설 사고, 차량 고립에 엄중 경계.",
        nameEn = "100 to 200cm",
        impactEn = "Very heavy snowpack; widespread road closures and roof-clearing hazards.",
        warningEn = "Alert for avalanches, roof snow shed, and vehicle entrapment.",
        nameZhCn = "100至200cm",
        impactZhCn = "极深积雪，多条主干道路封闭受阻，房顶除雪作业风险大。",
        warningZhCn = "高度警惕雪崩滑雪、屋顶坠雪伤人及车辆严重滞留搁浅。",
        nameZhTw = "100至200cm",
        impactZhTw = "極深積雪，多條主幹道路封閉受阻，房頂除雪作業風險大。",
        warningZhTw = "高度警惕雪崩滑雪、屋頂墜雪傷人及車輛嚴重滯留擱淺。",
        nameEs = "100 a 200 cm",
        impactEs = "Manto nivoso muy profundo; cierres generalizados de vías y riesgo al despejar tejados.",
        warningEs = "Alerta por avalanchas, caída de nieve de tejados y vehículos atrapados.",
        nameFr = "100 à 200 cm",
        impactFr = "Manteau neigeux très épais ; fermetures de routes et risques liés au déneigement des toits.",
        warningFr = "Vigilance face aux avalanches, chutes de neige des toits et véhicules bloqués.",
        namePt = "100 a 200 cm",
        impactPt = "Acúmulo muito profundo; interdições de vias e riscos ao remover neve de telhados.",
        warningPt = "Alerta para avalanches, queda de neve de telhados e veículos presos.",
        nameRu = "100–200 см",
        impactRu = "Очень глубокий снежный покров; массовые перекрытия дорог и опасность уборки крыш.",
        warningRu = "Остерегайтесь лавин, схода снега с крыш и снежных заносов."
    ),
    RadarLegendLevelItem(
        rangeLabel = "50〜100",
        nameJa = "50〜100cm",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "まとまった大雪。車の立ち往生や路面凍結によるスリップが多発。",
        warningJa = "チェーンや冬用タイヤ等の冬装備必須。歩行中の転倒に注意。",
        nameKo = "50~100cm",
        impactKo = "많은 대설. 차량 고립 및 도로 결빙으로 인한 미끄럼 사고 다발.",
        warningKo = "스노우 체인·겨울용 타이어 필수. 보행 중 낙상 사고에 주의.",
        nameEn = "50 to 100cm",
        impactEn = "Substantial heavy snow; frequent vehicle stalling and severe icy conditions.",
        warningEn = "Winter tires/chains required. Watch for falls on slick surfaces.",
        nameZhCn = "50至100cm",
        impactZhCn = "成片大雪积雪，车辆极易受困打滑，路面积雪严重。",
        warningZhCn = "机动车必须装配雪地胎或防滑链，行人谨防路面湿滑摔倒。",
        nameZhTw = "50至100cm",
        impactZhTw = "成片大雪積雪，車輛極易受困打滑，路面積雪嚴重。",
        warningZhTw = "機動車必須裝配雪地胎或防滑鏈，行人謹防路面濕滑摔倒。",
        nameEs = "50 a 100 cm",
        impactEs = "Nieve abundante; frecuentes vehículos atascados y fuerte congelación en vías.",
        warningEs = "Neumáticos de invierno o cadenas obligatorios. Cuidado con caídas en suelo resbaladizo.",
        nameFr = "50 à 100 cm",
        impactFr = "Neige abondante ; nombreux véhicules immobilisés et chaussées très verglacées.",
        warningFr = "Pneus hiver ou chaînes obligatoires. Attention aux chutes sur sols glissants.",
        namePt = "50 a 100 cm",
        impactPt = "Neve intensa; veículos presos com frequência e vias muito escorregadias.",
        warningPt = "Pneus de inverno ou correntes obrigatórios. Cuidado com quedas em pisos lisos.",
        nameRu = "50–100 см",
        impactRu = "Сильный снег; частые застревания машин и сильный гололёд.",
        warningRu = "Обязательны зимние шины или цепи. Остерегайтесь падений на скользком покрытии."
    ),
    RadarLegendLevelItem(
        rangeLabel = "20〜50",
        nameJa = "20〜50cm",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "足元がすっぽり埋まる積雪。交通ダイヤの乱れや除雪が必要。",
        warningJa = "スリップ事故や歩行困難に注意。時間に余裕をもった行動を。",
        nameKo = "20~50cm",
        impactKo = "발목까지 푹 빠지는 적설. 대중교통 지연 및 제설 작업 필요.",
        warningKo = "미끄럼 사고 및 보행 불편에 주의하고 여유 있게 이동하세요.",
        nameEn = "20 to 50cm",
        impactEn = "Deep snow covering feet; transit delays and snow clearing needed.",
        warningEn = "Drive cautiously to prevent skidding. Allow extra commute time.",
        nameZhCn = "20至50cm",
        impactZhCn = "积雪可没过脚踝至小腿，公共交通出现延误，需要进行除雪。",
        warningZhCn = "谨防车辆打滑失控，出行预留充足时间。",
        nameZhTw = "20至50cm",
        impactZhTw = "積雪可沒過腳踝至小腿，公共交通出現延誤，需要進行除雪。",
        warningZhTw = "謹防車輛打滑失控，出行預留充足時間。",
        nameEs = "20 a 50 cm",
        impactEs = "Nieve profunda que cubre los pies; retrasos en el transporte y necesidad de despeje.",
        warningEs = "Conduzca con precaución para evitar derrapes y salga con tiempo extra.",
        nameFr = "20 à 50 cm",
        impactFr = "Neige épaisse couvrant les pieds ; retards de transport et déneigement nécessaires.",
        warningFr = "Conduisez prudemment contre le dérapage et prévoyez un temps de trajet supplémentaire.",
        namePt = "20 a 50 cm",
        impactPt = "Neve profunda cobrindo os pés; atrasos no transporte e necessidade de limpeza.",
        warningPt = "Dirija com cautela para evitar derrapagens e saia com antecedência.",
        nameRu = "20–50 см",
        impactRu = "Глубокий снег по щиколотку; задержки транспорта и необходимость расчистки.",
        warningRu = "Ведите машину осторожно и закладывайте дополнительное время в пути."
    ),
    RadarLegendLevelItem(
        rangeLabel = "5〜20",
        nameJa = "5〜20cm",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "道路一面に積雪。靴が埋まり、冬用タイヤ未装着車の走行不能。",
        warningJa = "ノーマルタイヤでの走行は極めて危険。凍結路面に注意。",
        nameKo = "5~20cm",
        impactKo = "도로 전체에 눈이 쌓여 겨울용 타이어 미장착 차량은 주행 곤란.",
        warningKo = "일반 타이어 주행은 매우 위험. 결빙 도로에 주의하세요.",
        nameEn = "5 to 20cm",
        impactEn = "Full ground coverage; non-winter equipped vehicles will get stuck.",
        warningEn = "Do not drive on summer tires. Beware of hidden black ice.",
        nameZhCn = "5至20cm",
        impactZhCn = "路面全面覆雪，未更换雪地胎的车辆极易受阻抛锚。",
        warningZhCn = "严禁使用普通四季胎在雪道行驶，注意桥面冻结。",
        nameZhTw = "5至20cm",
        impactZhTw = "路面全面覆雪，未更換雪地胎的車輛極易受阻拋錨。",
        warningZhTw = "嚴禁使用普通四季胎在雪道行駛，注意橋面凍結。",
        nameEs = "5 a 20 cm",
        impactEs = "Suelo cubierto por completo; los vehículos sin equipo de invierno quedan atascados.",
        warningEs = "No conduzca con neumáticos de verano. Cuidado con el hielo negro.",
        nameFr = "5 à 20 cm",
        impactFr = "Sol entièrement recouvert ; les véhicules non équipés pour l'hiver risquent de rester bloqués.",
        warningFr = "Ne roulez pas en pneus été. Attention au verglas noir.",
        namePt = "5 a 20 cm",
        impactPt = "Solo totalmente coberto; veículos sem pneus de inverno podem ficar presos.",
        warningPt = "Não dirija com pneus comuns. Cuidado com gelo invisível na pista.",
        nameRu = "5–20 см",
        impactRu = "Дороги полностью покрыты снегом; машины на летней резине застревают.",
        warningRu = "Не выезжайте на летних шинах. Остерегайтесь чёрного льда."
    ),
    RadarLegendLevelItem(
        rangeLabel = "1〜5",
        nameJa = "1〜5cm",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "うっすらと白く積もる程度。路面凍結によるスリップに注意。",
        warningJa = "日陰や橋の上の路面凍結（ブラックアイスバーン）に注意。",
        nameKo = "1~5cm",
        impactKo = "하얗게 살짝 쌓이는 정도. 노면 결빙으로 인한 미끄럼 주의.",
        warningKo = "그늘진 곳이나 교량 위 도로 살얼음(블랙 아이스)에 주의.",
        nameEn = "1 to 5cm",
        impactEn = "Light white snow dusting; slick spots and black ice hazard.",
        warningEn = "Watch for black ice on bridges and shaded pavement.",
        nameZhCn = "1至5cm",
        impactZhCn = "薄薄一层白雪覆盖，局部路面结冰湿滑。",
        warningZhCn = "注意高架桥梁、背阴路段暗冰防滑。",
        nameZhTw = "1至5cm",
        impactZhTw = "薄薄一層白雪覆蓋，局部路面結冰濕滑。",
        warningZhTw = "注意高架橋樑、背陰路段暗冰防滑。",
        nameEs = "1 a 5 cm",
        impactEs = "Ligera capa blanca de nieve; zonas resbaladizas y riesgo de hielo negro.",
        warningEs = "Cuidado con el hielo negro en puentes y tramos sombríos.",
        nameFr = "1 à 5 cm",
        impactFr = "Légère couche blanche ; zones glissantes et risque de verglas.",
        warningFr = "Méfiez-vous du verglas sur les ponts et les zones ombragées.",
        namePt = "1 a 5 cm",
        impactPt = "Leve camada de neve; pontos escorregadios e risco de gelo na pista.",
        warningPt = "Atenção ao gelo em pontes e trechos sombreados.",
        nameRu = "1–5 см",
        impactRu = "Тонкий слой снега; скользкие участки и риск гололёда.",
        warningRu = "Остерегайтесь гололёда на мостах и в тенистых участках дорог."
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
        nameKo = "20cm 이상",
        impactKo = "3시간 동안 20cm 이상의 맹렬한 폭설. 적설량이 급격히 증가.",
        warningKo = "급격한 교통 마비 및 차량 고립에 최고 수준 경계.",
        nameEn = "20cm or more",
        impactEn = "Intense rapid snowfall exceeding 20cm in 3h; drastic accumulation.",
        warningEn = "Maximum alert for rapid traffic paralysis and vehicle stranding.",
        nameZhCn = "20cm以上",
        impactZhCn = "3小时强降雪超过20cm，积雪瞬间暴涨。",
        warningZhCn = "对交通瘫痪及车辆大面积抛锚受困保持最高级别戒备。",
        nameZhTw = "20cm以上",
        impactZhTw = "3小時強降雪超過20cm，積雪瞬間暴漲。",
        warningZhTw = "對交通癱瘓及車輛大面積拋錨受困保持最高級別戒備。",
        nameEs = "20 cm o más",
        impactEs = "Nevada intensa superior a 20 cm en 3 h; acumulación drástica.",
        warningEs = "Alerta máxima por parálisis rápida del tráfico y vehículos varados.",
        nameFr = "20 cm ou plus",
        impactFr = "Chutes de neige intenses dépassant 20 cm en 3 h ; accumulation brutale.",
        warningFr = "Alerte maximale face à la paralysie du trafic et aux véhicules bloqués.",
        namePt = "20 cm ou mais",
        impactPt = "Nevasca intensa acima de 20 cm em 3 h; acúmulo muito rápido.",
        warningPt = "Alerta máximo para paralisação do trânsito e veículos retidos.",
        nameRu = "20 см и более",
        impactRu = "Экстремальный снегопад более 20 см за 3 ч; стремительный рост сугробов.",
        warningRu = "Максимальная тревога из-за паралича движения и застрявших машин."
    ),
    RadarLegendLevelItem(
        rangeLabel = "10〜20",
        nameJa = "10〜20cm",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "3時間で10〜20cmの非常に激しい降雪。除雪が追いつかない恐れ。",
        warningJa = "急な積雪増とホワイトアウト（視界不良）に厳重警戒。",
        nameKo = "10~20cm",
        impactKo = "3시간 동안 10~20cm의 매우 강한 폭설. 제설이 따라가기 어려운 수준.",
        warningKo = "급격한 적설 증가와 화이트아웃(시야 불량)에 엄중 경계.",
        nameEn = "10 to 20cm",
        impactEn = "Extremely heavy snow of 10-20cm in 3h; clearing cannot keep pace.",
        warningEn = "High danger of whiteouts and rapidly worsening roads.",
        nameZhCn = "10至20cm",
        impactZhCn = "3小时降雪达10至20cm，积雪速度快除雪困难。",
        warningZhCn = "严密戒备白化能见度骤降及路况急剧恶化。",
        nameZhTw = "10至20cm",
        impactZhTw = "3小時降雪達10至20cm，積雪速度快除雪困難。",
        warningZhTw = "嚴密戒備白化能見度驟降及路況急劇惡化。",
        nameEs = "10 a 20 cm",
        impactEs = "Nieve muy intensa de 10 a 20 cm en 3 h; el despeje no da abasto.",
        warningEs = "Alto peligro de ventisca blanca y rápido deterioro de las vías.",
        nameFr = "10 à 20 cm",
        impactFr = "Neige très forte de 10 à 20 cm en 3 h ; le déneigement peine à suivre.",
        warningFr = "Fort risque de voile blanc et de dégradation rapide des routes.",
        namePt = "10 a 20 cm",
        impactPt = "Neve muito forte de 10 a 20 cm em 3 h; a limpeza não acompanha.",
        warningPt = "Alto risco de visibilidade nula e piora rápida das estradas.",
        nameRu = "10–20 см",
        impactRu = "Очень сильный снегопад 10–20 см за 3 ч; уборка снега не успевает.",
        warningRu = "Высокий риск нулевой видимости и быстрого ухудшения дорожных условий."
    ),
    RadarLegendLevelItem(
        rangeLabel = "5〜10",
        nameJa = "5〜10cm",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "まとまった激しい雪。短時間で道路状況が悪化。",
        warningJa = "車の走行は冬用タイヤを装着し速度を落として走行。",
        nameKo = "5~10cm",
        impactKo = "짧은 시간에 도로 사정이 급격히 악화되는 강한 눈.",
        warningKo = "겨울용 타이어를 장착하고 충분히 감속하여 운행하세요.",
        nameEn = "5 to 10cm",
        impactEn = "Heavy snow band; road conditions deteriorate rapidly.",
        warningEn = "Use winter tires and drive at significantly reduced speed.",
        nameZhCn = "5至10cm",
        impactZhCn = "短时较强降雪，道路状况迅速变差。",
        warningZhCn = "驾车请务必减速慢行，确保冬季轮胎防滑。",
        nameZhTw = "5至10cm",
        impactZhTw = "短時較強降雪，道路狀況迅速變差。",
        warningZhTw = "駕車請務必減速慢行，確保冬季輪胎防滑。",
        nameEs = "5 a 10 cm",
        impactEs = "Nevada fuerte; el estado de las carreteras empeora con rapidez.",
        warningEs = "Use neumáticos de invierno y reduzca considerablemente la velocidad.",
        nameFr = "5 à 10 cm",
        impactFr = "Forte bande neigeuse ; les conditions de route se dégradent vite.",
        warningFr = "Utilisez des pneus hiver et réduisez nettement votre vitesse.",
        namePt = "5 a 10 cm",
        impactPt = "Faixa de neve forte; as condições da via pioram rapidamente.",
        warningPt = "Use pneus de inverno e reduza bastante a velocidade.",
        nameRu = "5–10 см",
        impactRu = "Сильный снегопад; состояние дорог стремительно ухудшается.",
        warningRu = "Используйте зимние шины и значительно снизьте скорость."
    ),
    RadarLegendLevelItem(
        rangeLabel = "3〜5",
        nameJa = "3〜5cm",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "しっかりとした降雪。道路にうっすら積雪。",
        warningJa = "路面の凍結やシャーベット状の雪に注意。",
        nameKo = "3~5cm",
        impactKo = "꾸준히 내리는 눈으로 도로에 눈이 쌓이기 시작.",
        warningKo = "노면 결빙이나 진눈깨비·슬러시 상태의 미끄러운 도로에 주의.",
        nameEn = "3 to 5cm",
        impactEn = "Continuous steady snowfall covering pavement.",
        warningEn = "Caution on icy or slushy road surfaces.",
        nameZhCn = "3至5cm",
        impactZhCn = "持续稳定降雪，路面形成明显薄积雪或泥雪混合。",
        warningZhCn = "注意湿滑暗冰及泥雪路面行车安全。",
        nameZhTw = "3至5cm",
        impactZhTw = "持續穩定降雪，路面形成明顯薄積雪或泥雪混合。",
        warningZhTw = "注意濕滑暗冰及泥雪路面行車安全。",
        nameEs = "3 a 5 cm",
        impactEs = "Nevada continua y constante que cubre la calzada.",
        warningEs = "Precaución en superficies heladas o con aguanieve.",
        nameFr = "3 à 5 cm",
        impactFr = "Chutes de neige régulières recouvrant la chaussée.",
        warningFr = "Prudence sur les routes verglacées ou boueuses.",
        namePt = "3 a 5 cm",
        impactPt = "Neve contínua cobrindo o pavimento.",
        warningPt = "Cuidado com pistas congeladas ou com neve derretida.",
        nameRu = "3–5 см",
        impactRu = "Устойчивый снегопад, покрывающий проезжую часть.",
        warningRu = "Будьте осторожны на обледенелой или заснеженной дороге."
    ),
    RadarLegendLevelItem(
        rangeLabel = "1〜3",
        nameJa = "1〜3cm",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "並の降雪。周囲が白くなり始める。",
        warningJa = "滑りやすい靴を避け、歩行時足元に注意。",
        nameKo = "1~3cm",
        impactKo = "보통 수준의 눈. 주변이 하얗게 덮이기 시작.",
        warningKo = "미끄러운 신발을 피하고 보행 시 발밑에 주의.",
        nameEn = "1 to 3cm",
        impactEn = "Moderate snowfall whitening the scenery.",
        warningEn = "Wear non-slip footwear and watch your step.",
        nameZhCn = "1至3cm",
        impactZhCn = "普通降雪，四周景物开始泛白。",
        warningZhCn = "外出注意防滑，避免穿底面光滑鞋子。",
        nameZhTw = "1至3cm",
        impactZhTw = "普通降雪，四周景物開始泛白。",
        warningZhTw = "外出注意防滑，避免穿底面光滑鞋子。",
        nameEs = "1 a 3 cm",
        impactEs = "Nevada moderada que empieza a blanquear el paisaje.",
        warningEs = "Use calzado antideslizante y tenga cuidado al caminar.",
        nameFr = "1 à 3 cm",
        impactFr = "Neige modérée blanchissant le paysage.",
        warningFr = "Portez des chaussures antidérapantes et surveillez vos pas.",
        namePt = "1 a 3 cm",
        impactPt = "Neve moderada cobrindo a paisagem de branco.",
        warningPt = "Use calçados antiderrapantes e tenha atenção ao caminhar.",
        nameRu = "1–3 см",
        impactRu = "Умеренный снегопад, покрывающий землю белым слоем.",
        warningRu = "Носите нескользящую обувь и будьте осторожны при ходьбе."
    ),
    RadarLegendLevelItem(
        rangeLabel = "0.5〜1",
        nameJa = "0.5〜1cm",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "ちらつく雪・弱い降雪。",
        warningJa = "気温が低い場合は路面凍結に注意。",
        nameKo = "0.5~1cm",
        impactKo = "흩날리는 눈 또는 약한 강설.",
        warningKo = "기온이 낮을 경우 노면 결빙에 주의.",
        nameEn = "0.5 to 1cm",
        impactEn = "Flurries or light falling snow.",
        warningEn = "Watch for freeze conditions when temperatures are low.",
        nameZhCn = "0.5至1cm",
        impactZhCn = "零星飘雪或微弱降雪。",
        warningZhCn = "气温偏低时注意路面轻微结冰。",
        nameZhTw = "0.5至1cm",
        impactZhTw = "零星飄雪或微弱降雪。",
        warningZhTw = "氣溫偏低時注意路面輕微結冰。",
        nameEs = "0,5 a 1 cm",
        impactEs = "Copos dispersos o nevada ligera.",
        warningEs = "Cuidado con heladas si las temperaturas son bajas.",
        nameFr = "0,5 à 1 cm",
        impactFr = "Quelques flocons ou faible neige.",
        warningFr = "Attention au gel lorsque les températures sont basses.",
        namePt = "0,5 a 1 cm",
        impactPt = "Flocos isolados ou neve fraca.",
        warningPt = "Atenção ao congelamento da pista com temperaturas baixas.",
        nameRu = "0,5–1 см",
        impactRu = "Небольшой снег или отдельные снежинки.",
        warningRu = "При низкой температуре возможен гололёд."
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
        nameKo = "50cm 이상",
        impactKo = "24시간 동안 50cm 이상의 폭설. 대설경보·특보급.",
        warningKo = "대중교통 운행 중단, 수목 전도, 정전에 엄중 경계.",
        nameEn = "50cm or more",
        impactEn = "Severe accumulation over 50cm in 24h; heavy snow warning level.",
        warningEn = "Prepare for transit suspensions, power outages, and tree damage.",
        nameZhCn = "50cm以上",
        impactZhCn = "24小时累计降雪达50cm以上，属于特大暴雪警报级别。",
        warningZhCn = "严密戒备公共交通大面积停运、电网断电及倒木灾害。",
        nameZhTw = "50cm以上",
        impactZhTw = "24小時累計降雪達50cm以上，屬於特大暴雪警報級別。",
        warningZhTw = "嚴密戒備公共交通大面積停運、電網斷電及倒木災害。",
        nameEs = "50 cm o más",
        impactEs = "Acumulación severa superior a 50 cm en 24 h; nivel de alerta por nevada intensa.",
        warningEs = "Prepárese para suspensiones de transporte, cortes de luz y caída de árboles.",
        nameFr = "50 cm ou plus",
        impactFr = "Cumul sévère de plus de 50 cm en 24 h ; niveau d'alerte neige forte.",
        warningFr = "Préparez-vous aux arrêts de transport, coupures de courant et chutes d'arbres.",
        namePt = "50 cm ou mais",
        impactPt = "Acúmulo severo acima de 50 cm em 24 h; nível de alerta de nevasca.",
        warningPt = "Prepare-se para suspensões no transporte, quedas de energia e de árvores.",
        nameRu = "50 см и более",
        impactRu = "Сильнейший снегопад свыше 50 см за 24 ч; уровень штормового предупреждения.",
        warningRu = "Возможны отмена транспорта, отключения электричества и падение деревьев."
    ),
    RadarLegendLevelItem(
        rangeLabel = "30〜50",
        nameJa = "30〜50cm",
        color = Color(0xFFFF2800),
        onColor = Color.White,
        impactJa = "大雪警報基準の大雪。車の立ち往生リスク増大。",
        warningJa = "大雪警報発表のおそれ。不要不急の外出を控えてください。",
        nameKo = "30~50cm",
        impactKo = "대설경보 기준의 많은 눈. 차량 고립 위험 증대.",
        warningKo = "대설경보 발효 우려. 불필요한 외출을 자제하세요.",
        nameEn = "30 to 50cm",
        impactEn = "Heavy snow warning criteria met; high stranding hazard.",
        warningEn = "Refrain from non-essential outings.",
        nameZhCn = "30至50cm",
        impactZhCn = "达到大雪警报标准的大雪，车辆受困风险激增。",
        warningZhCn = "可能发布暴雪警报，请尽量避免非必要出行。",
        nameZhTw = "30至50cm",
        impactZhTw = "達到大雪警報標準的大雪，車輛受困風險激增。",
        warningZhTw = "可能發布暴雪警報，請儘量避免非必要出行。",
        nameEs = "30 a 50 cm",
        impactEs = "Criterio de alerta por nevada intensa; alto riesgo de quedar atrapado.",
        warningEs = "Evite salidas que no sean esenciales.",
        nameFr = "30 à 50 cm",
        impactFr = "Seuils d'alerte neige forte atteints ; risque élevé de blocage.",
        warningFr = "Évitez les sorties non indispensables.",
        namePt = "30 a 50 cm",
        impactPt = "Critério de alerta de neve forte; alto risco de veículos presos.",
        warningPt = "Evite saídas não essenciais.",
        nameRu = "30–50 см",
        impactRu = "Снегопад уровня предупреждения; высокий риск застрять в снегу.",
        warningRu = "Воздержитесь от необязательных поездок."
    ),
    RadarLegendLevelItem(
        rangeLabel = "20〜30",
        nameJa = "20〜30cm",
        color = Color(0xFFFF9900),
        onColor = Color.White,
        impactJa = "まとまった降雪。生活道路や峠道で通行障害。",
        warningJa = "雪道運転に不慣れな場合は運転を自粛してください。",
        nameKo = "20~30cm",
        impactKo = "많은 강설로 이면도로 및 고갯길 통행 장애 발생.",
        warningKo = "눈길 운전에 익숙하지 않다면 운전을 자제하세요.",
        nameEn = "20 to 30cm",
        impactEn = "Substantial snowfall causing traffic snarls on passes and local roads.",
        warningEn = "Avoid driving if unaccustomed to winter conditions.",
        nameZhCn = "20至30cm",
        impactZhCn = "大范围强降雪，山道与一般生活道路出现通行障碍。",
        warningZhCn = "不熟悉雪地驾驶者请切勿冒险自驾出行。",
        nameZhTw = "20至30cm",
        impactZhTw = "大範圍強降雪，山道與一般生活道路出現通行障礙。",
        warningZhTw = "不熟悉雪地駕駛者請切勿冒險自駕出行。",
        nameEs = "20 a 30 cm",
        impactEs = "Nevada considerable con atascos en puertos y vías locales.",
        warningEs = "Evite conducir si no tiene experiencia en nieve.",
        nameFr = "20 à 30 cm",
        impactFr = "Chutes de neige importantes perturbant les cols et routes locales.",
        warningFr = "Évitez de conduire si vous n'êtes pas habitué aux conditions hivernales.",
        namePt = "20 a 30 cm",
        impactPt = "Neve considerável causando transtornos em serras e vias locais.",
        warningPt = "Evite dirigir se não tiver prática em pistas com neve.",
        nameRu = "20–30 см",
        impactRu = "Значительный снегопад; затруднения движения на перевалах и местных дорогах.",
        warningRu = "Воздержитесь от вождения без опыта зимней езды."
    ),
    RadarLegendLevelItem(
        rangeLabel = "10〜20",
        nameJa = "10〜20cm",
        color = Color(0xFFFAF500),
        onColor = Color(0xFF1C1B1F),
        impactJa = "大雪注意報基準のまとまった雪。除雪が必要。",
        warningJa = "通勤通学の交通機関の遅延に留意。",
        nameKo = "10~20cm",
        impactKo = "대설주의보 기준의 많은 눈. 제설 작업이 필요.",
        warningKo = "출퇴근·등하교 시 대중교통 지연에 유의하세요.",
        nameEn = "10 to 20cm",
        impactEn = "Moderate snow meeting advisory levels; snow shoveling required.",
        warningEn = "Expect transit delays during morning/evening commutes.",
        nameZhCn = "10至20cm",
        impactZhCn = "达到大雪注意报标准，需要进行除雪作业。",
        warningZhCn = "请预留通勤出行时间，关注公共交通晚点情况。",
        nameZhTw = "10至20cm",
        impactZhTw = "達到大雪注意報標準，需要進行除雪作業。",
        warningZhTw = "請預留通勤出行時間，關注公共交通晚點情況。",
        nameEs = "10 a 20 cm",
        impactEs = "Nieve moderada de nivel de aviso; será necesario retirar nieve.",
        warningEs = "Prevea retrasos en el transporte durante los desplazamientos diarios.",
        nameFr = "10 à 20 cm",
        impactFr = "Neige modérée de niveau vigilance ; déneigement nécessaire.",
        warningFr = "Prévoyez des retards dans les transports aux heures de pointe.",
        namePt = "10 a 20 cm",
        impactPt = "Neve moderada em nível de aviso; será necessário remover neve.",
        warningPt = "Espere atrasos no transporte nos horários de pico.",
        nameRu = "10–20 см",
        impactRu = "Умеренный снегопад; потребуется расчистка снега.",
        warningRu = "Возможны задержки транспорта в часы пик."
    ),
    RadarLegendLevelItem(
        rangeLabel = "5〜10",
        nameJa = "5〜10cm",
        color = Color(0xFF0041FF),
        onColor = Color.White,
        impactJa = "雪景色となる降雪。路面の凍結・スリップに注意。",
        warningJa = "滑り止め措置を講じて歩行・走行してください。",
        nameKo = "5~10cm",
        impactKo = "눈이 수북이 쌓이는 강설. 노면 결빙 및 미끄럼 주의.",
        warningKo = "미끄럼 방지 대비를 철저히 하고 보행·운행하세요.",
        nameEn = "5 to 10cm",
        impactEn = "Noticeable snowfall landscape; slippery frozen roads.",
        warningEn = "Ensure anti-skid preparations before heading out.",
        nameZhCn = "5至10cm",
        impactZhCn = "地表明显积雪，道路容易结冰打滑。",
        warningZhCn = "出行前请做好防滑防冻准备措施。",
        nameZhTw = "5至10cm",
        impactZhTw = "地表面顯積雪，道路容易結冰打滑。",
        warningZhTw = "出行前請做好防滑防凍準備措施。",
        nameEs = "5 a 10 cm",
        impactEs = "Nevada apreciable que cubre el entorno; carreteras heladas y resbaladizas.",
        warningEs = "Tome medidas antideslizantes antes de salir.",
        nameFr = "5 à 10 cm",
        impactFr = "Enneigement marqué ; routes gelées et glissantes.",
        warningFr = "Prévoyez des équipements antidérapants avant de sortir.",
        namePt = "5 a 10 cm",
        impactPt = "Acúmulo visível de neve; vias congeladas e escorregadias.",
        warningPt = "Garanta medidas antiderrapantes antes de sair.",
        nameRu = "5–10 см",
        impactRu = "Заметный снежный покров; скользкие обледенелые дороги.",
        warningRu = "Подготовьте средства против скольжения перед выходом."
    ),
    RadarLegendLevelItem(
        rangeLabel = "1〜5",
        nameJa = "1〜5cm",
        color = Color(0xFFA0D2FF),
        onColor = Color(0xFF003258),
        impactJa = "うっすらとした降雪。日陰の路面凍結に注意。",
        warningJa = "朝晩の冷え込みによる凍結路面に注意。",
        nameKo = "1~5cm",
        impactKo = "약한 강설. 그늘진 구간의 노면 결빙에 주의.",
        warningKo = "아침·저녁 기온 하강에 따른 빙판길에 주의하세요.",
        nameEn = "1 to 5cm",
        impactEn = "Light snow cover; frozen patches in shaded zones.",
        warningEn = "Watch for morning and nighttime icy road spots.",
        nameZhCn = "1至5cm",
        impactZhCn = "浅浅薄雪覆盖，背阴路段易结薄冰。",
        warningZhCn = "注意早晚降温时段的道路暗冰湿滑。",
        nameZhTw = "1至5cm",
        impactZhTw = "淺淺薄雪覆蓋，背陰路段易結薄冰。",
        warningZhTw = "注意早晚降溫時段的道路暗冰濕滑。",
        nameEs = "1 a 5 cm",
        impactEs = "Ligera capa de nieve; placas de hielo en zonas sombrías.",
        warningEs = "Cuidado con el hielo en la calzada por la mañana y por la noche.",
        nameFr = "1 à 5 cm",
        impactFr = "Fine couche de neige ; plaques de verglas à l'ombre.",
        warningFr = "Attention aux zones verglacées le matin et la nuit.",
        namePt = "1 a 5 cm",
        impactPt = "Leve camada de neve; trechos congelados na sombra.",
        warningPt = "Atenção ao gelo na pista de manhã e à noite.",
        nameRu = "1–5 см",
        impactRu = "Лёгкий снежный покров; возможна наледь в тени.",
        warningRu = "Остерегайтесь гололёда утром и ночью."
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
                    AppLanguage.KOREAN -> "실황"
                    AppLanguage.ENGLISH -> "Now"
                    AppLanguage.SIMPLIFIED_CHINESE -> "实况"
                    AppLanguage.TRADITIONAL_CHINESE -> "實況"
                    AppLanguage.SPANISH -> "Ahora"
                    AppLanguage.FRENCH -> "Direct"
                    AppLanguage.PORTUGUESE -> "Agora"
                    AppLanguage.RUSSIAN -> "Сейчас"
                }
                diffMin < 0 -> {
                    val absMin = -diffMin
                    if (absMin >= 60) {
                        val hours = absMin / 60
                        val mins = absMin % 60
                        when (language) {
                            AppLanguage.JAPANESE -> if (mins == 0) "${hours}時間前" else "${hours}時間${mins}分前"
                            AppLanguage.KOREAN -> if (mins == 0) "${hours}시간 전" else "${hours}시간 ${mins}분 전"
                            AppLanguage.ENGLISH -> if (mins == 0) "${hours}h ago" else "${hours}h ${mins}m ago"
                            AppLanguage.SIMPLIFIED_CHINESE -> if (mins == 0) "${hours}小时前" else "${hours}小时${mins}分前"
                            AppLanguage.TRADITIONAL_CHINESE -> if (mins == 0) "${hours}小時前" else "${hours}小時${mins}分前"
                            AppLanguage.SPANISH -> if (mins == 0) "Hace ${hours}h" else "Hace ${hours}h ${mins}m"
                            AppLanguage.FRENCH -> if (mins == 0) "Il y a ${hours}h" else "Il y a ${hours}h ${mins}m"
                            AppLanguage.PORTUGUESE -> if (mins == 0) "Há ${hours}h" else "Há ${hours}h ${mins}m"
                            AppLanguage.RUSSIAN -> if (mins == 0) "${hours} ч назад" else "${hours} ч ${mins} м назад"
                        }
                    } else {
                        when (language) {
                            AppLanguage.JAPANESE -> "${absMin}分前"
                            AppLanguage.KOREAN -> "${absMin}분 전"
                            AppLanguage.ENGLISH -> "${absMin}m ago"
                            AppLanguage.SIMPLIFIED_CHINESE -> "${absMin}分钟前"
                            AppLanguage.TRADITIONAL_CHINESE -> "${absMin}分鐘前"
                            AppLanguage.SPANISH -> "Hace ${absMin}m"
                            AppLanguage.FRENCH -> "Il y a ${absMin}m"
                            AppLanguage.PORTUGUESE -> "Há ${absMin}m"
                            AppLanguage.RUSSIAN -> "${absMin} мин назад"
                        }
                    }
                }
                else -> {
                    if (diffMin >= 60) {
                        val hours = diffMin / 60
                        val mins = diffMin % 60
                        when (language) {
                            AppLanguage.JAPANESE -> if (mins == 0) "+${hours}時間後" else "+${hours}時間${mins}分後"
                            AppLanguage.KOREAN -> if (mins == 0) "+${hours}시간 후" else "+${hours}시간 ${mins}분 후"
                            AppLanguage.ENGLISH -> if (mins == 0) "+${hours}h" else "+${hours}h ${mins}m"
                            AppLanguage.SIMPLIFIED_CHINESE -> if (mins == 0) "+${hours}小时后" else "+${hours}小时${mins}分后"
                            AppLanguage.TRADITIONAL_CHINESE -> if (mins == 0) "+${hours}小時後" else "+${hours}小時${mins}分後"
                            AppLanguage.SPANISH -> if (mins == 0) "+${hours}h" else "+${hours}h ${mins}m"
                            AppLanguage.FRENCH -> if (mins == 0) "+${hours}h" else "+${hours}h ${mins}m"
                            AppLanguage.PORTUGUESE -> if (mins == 0) "+${hours}h" else "+${hours}h ${mins}m"
                            AppLanguage.RUSSIAN -> if (mins == 0) "+${hours} ч" else "+${hours} ч ${mins} м"
                        }
                    } else {
                        when (language) {
                            AppLanguage.JAPANESE -> "+${diffMin}分後"
                            AppLanguage.KOREAN -> "+${diffMin}분 후"
                            AppLanguage.ENGLISH -> "+${diffMin}m"
                            AppLanguage.SIMPLIFIED_CHINESE -> "+${diffMin}分钟后"
                            AppLanguage.TRADITIONAL_CHINESE -> "+${diffMin}分鐘後"
                            AppLanguage.SPANISH -> "+${diffMin}m"
                            AppLanguage.FRENCH -> "+${diffMin}m"
                            AppLanguage.PORTUGUESE -> "+${diffMin}m"
                            AppLanguage.RUSSIAN -> "+${diffMin} м"
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
                        AppLanguage.KOREAN -> "강수 나우캐스트"
                        AppLanguage.ENGLISH -> "Precipitation Nowcast"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降雨即时预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雨即時預報"
                        AppLanguage.SPANISH -> "Nowcast de precipitación"
                        AppLanguage.FRENCH -> "Nowcast des précipitations"
                        AppLanguage.PORTUGUESE -> "Nowcast de precipitação"
                        AppLanguage.RUSSIAN -> "Наукаст осадков"
                    },
                    icon = Icons.Rounded.WaterDrop,
                    isSelected = selectedJmaLayer == JmaLayerType.HRPNS,
                    onClick = { switchJmaLayer(JmaLayerType.HRPNS) }
                ),
                // index 1: 雷ナウキャスト
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "雷ナウキャスト"
                        AppLanguage.KOREAN -> "낙뢰 나우캐스트"
                        AppLanguage.ENGLISH -> "Lightning Nowcast"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷电即时预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷電即時預報"
                        AppLanguage.SPANISH -> "Nowcast de rayos"
                        AppLanguage.FRENCH -> "Nowcast de la foudre"
                        AppLanguage.PORTUGUESE -> "Nowcast de raios"
                        AppLanguage.RUSSIAN -> "Наукаст гроз"
                    },
                    icon = Icons.Rounded.Thunderstorm,
                    isSelected = selectedJmaLayer == JmaLayerType.THNS,
                    onClick = { switchJmaLayer(JmaLayerType.THNS) }
                ),
                // index 2: キキクル危険度
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "キキクル危険度"
                        AppLanguage.KOREAN -> "키키쿠루 위험도"
                        AppLanguage.ENGLISH -> "Kikikuru Hazard Risk"
                        AppLanguage.SIMPLIFIED_CHINESE -> "Kikikuru危险度"
                        AppLanguage.TRADITIONAL_CHINESE -> "Kikikuru危險度"
                        AppLanguage.SPANISH -> "Riesgo Kikikuru"
                        AppLanguage.FRENCH -> "Risque Kikikuru"
                        AppLanguage.PORTUGUESE -> "Risco Kikikuru"
                        AppLanguage.RUSSIAN -> "Риски Kikikuru"
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
                        AppLanguage.KOREAN -> "눈·강설량 정보"
                        AppLanguage.ENGLISH -> "Snow & Snowfall"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降雪・积雪量信息"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雪・積雪量資訊"
                        AppLanguage.SPANISH -> "Nieve y nevadas"
                        AppLanguage.FRENCH -> "Neige et chutes de neige"
                        AppLanguage.PORTUGUESE -> "Neve e precipitação de neve"
                        AppLanguage.RUSSIAN -> "Снег и снегопады"
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
                        AppLanguage.KOREAN -> "태풍 정보 (경로·폭풍역)"
                        AppLanguage.ENGLISH -> "Typhoon Track & Warning"
                        AppLanguage.SIMPLIFIED_CHINESE -> "台风情报 (路径・暴风区)"
                        AppLanguage.TRADITIONAL_CHINESE -> "颱風情報 (路徑・暴風區)"
                        AppLanguage.SPANISH -> "Trayectoria y alertas de tifón"
                        AppLanguage.FRENCH -> "Trajectoire et alertes typhon"
                        AppLanguage.PORTUGUESE -> "Trajetória e alertas de tufão"
                        AppLanguage.RUSSIAN -> "Траектория и оповещения о тайфунах"
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
                        AppLanguage.KOREAN -> "강수 나우캐스트"
                        AppLanguage.ENGLISH -> "Precipitation Nowcast"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降雨即时预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雨即時預報"
                        AppLanguage.SPANISH -> "Nowcast de precipitación"
                        AppLanguage.FRENCH -> "Nowcast des précipitations"
                        AppLanguage.PORTUGUESE -> "Nowcast de precipitação"
                        AppLanguage.RUSSIAN -> "Наукаст осадков"
                    },
                    icon = Icons.Rounded.WaterDrop,
                    isSelected = selectedJmaLayer == JmaLayerType.HRPNS,
                    onClick = { switchJmaLayer(JmaLayerType.HRPNS) }
                ),
                // index 1
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "雷・竜巻ナウキャスト"
                        AppLanguage.KOREAN -> "낙뢰·토네이도 나우캐스트"
                        AppLanguage.ENGLISH -> "Thunder / Tornado"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷电・龙卷风即时预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷電・龍捲風即時預報"
                        AppLanguage.SPANISH -> "Rayos / Tornado"
                        AppLanguage.FRENCH -> "Orage / Tornade"
                        AppLanguage.PORTUGUESE -> "Raios / Tornado"
                        AppLanguage.RUSSIAN -> "Грозы / Торнадо"
                    },
                    icon = Icons.Rounded.Thunderstorm,
                    isSelected = selectedJmaLayer == JmaLayerType.THNS || selectedJmaLayer == JmaLayerType.TRNS,
                    onClick = { switchJmaLayer(JmaLayerType.THNS) }
                ),
                // index 2
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "キキクル危険度"
                        AppLanguage.KOREAN -> "키키쿠루 위험도"
                        AppLanguage.ENGLISH -> "Kikikuru Hazard Risk"
                        AppLanguage.SIMPLIFIED_CHINESE -> "Kikikuru危险度"
                        AppLanguage.TRADITIONAL_CHINESE -> "Kikikuru危險度"
                        AppLanguage.SPANISH -> "Riesgo Kikikuru"
                        AppLanguage.FRENCH -> "Risque Kikikuru"
                        AppLanguage.PORTUGUESE -> "Risco Kikikuru"
                        AppLanguage.RUSSIAN -> "Риски Kikikuru"
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
                        AppLanguage.KOREAN -> "히마와리 기상위성"
                        AppLanguage.ENGLISH -> "Himawari Satellite"
                        AppLanguage.SIMPLIFIED_CHINESE -> "向日葵气象卫星"
                        AppLanguage.TRADITIONAL_CHINESE -> "向日葵氣象衛星"
                        AppLanguage.SPANISH -> "Satélite Himawari"
                        AppLanguage.FRENCH -> "Satellite Himawari"
                        AppLanguage.PORTUGUESE -> "Satélite Himawari"
                        AppLanguage.RUSSIAN -> "Спутник Himawari"
                    },
                    icon = Icons.Rounded.Public,
                    isSelected = selectedJmaLayer.category == JmaLayerCategory.HIMAWARI,
                    onClick = { switchJmaLayer(JmaLayerType.HIMAWARI_TRUECOLOR) }
                ),
                // index 4
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "天気・予想気温"
                        AppLanguage.KOREAN -> "날씨·예상 기온"
                        AppLanguage.ENGLISH -> "Weather & Temp Fcst"
                        AppLanguage.SIMPLIFIED_CHINESE -> "天气・预测气温"
                        AppLanguage.TRADITIONAL_CHINESE -> "天氣・預測氣溫"
                        AppLanguage.SPANISH -> "Tiempo y temp. prevista"
                        AppLanguage.FRENCH -> "Météo et temp. prévues"
                        AppLanguage.PORTUGUESE -> "Clima e temp. prevista"
                        AppLanguage.RUSSIAN -> "Погода и прогноз темп."
                    },
                    icon = Icons.Rounded.Thermostat,
                    isSelected = selectedJmaLayer.category == JmaLayerCategory.WEATHER_DIST,
                    onClick = { switchJmaLayer(JmaLayerType.WEATHER_DIST) }
                ),
                // index 5
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "雪・降雪量情報"
                        AppLanguage.KOREAN -> "눈·강설량 정보"
                        AppLanguage.ENGLISH -> "Snow & Snowfall"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降雪・积雪量信息"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雪・積雪量資訊"
                        AppLanguage.SPANISH -> "Nieve y nevadas"
                        AppLanguage.FRENCH -> "Neige et chutes de neige"
                        AppLanguage.PORTUGUESE -> "Neve e precipitação de neve"
                        AppLanguage.RUSSIAN -> "Снег и снегопады"
                    },
                    icon = Icons.Rounded.AcUnit,
                    isSelected = selectedJmaLayer.category == JmaLayerCategory.SNOW,
                    onClick = { switchJmaLayer(JmaLayerType.SNOW_DEPTH) }
                ),
                // index 6: 台風情報
                SpeedDialItemData(
                    title = when (language) {
                        AppLanguage.JAPANESE -> "台風情報 (進路・暴風域)"
                        AppLanguage.KOREAN -> "태풍 정보 (경로·폭풍역)"
                        AppLanguage.ENGLISH -> "Typhoon Track & Warning"
                        AppLanguage.SIMPLIFIED_CHINESE -> "台风情报 (路径・暴风区)"
                        AppLanguage.TRADITIONAL_CHINESE -> "颱風情報 (路徑・暴風區)"
                        AppLanguage.SPANISH -> "Trayectoria y alertas de tifón"
                        AppLanguage.FRENCH -> "Trajectoire et alertes typhon"
                        AppLanguage.PORTUGUESE -> "Trajetória e alertas de tufão"
                        AppLanguage.RUSSIAN -> "Траектория и оповещения о тайфунах"
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
                        AppLanguage.KOREAN -> "모든 레이어 (${JmaLayerType.entries.size}종)"
                        AppLanguage.ENGLISH -> "All Layers (${JmaLayerType.entries.size})"
                        AppLanguage.SIMPLIFIED_CHINESE -> "所有图层 (${JmaLayerType.entries.size}种)"
                        AppLanguage.TRADITIONAL_CHINESE -> "所有圖層 (${JmaLayerType.entries.size}種)"
                        AppLanguage.SPANISH -> "Todas las capas (${JmaLayerType.entries.size})"
                        AppLanguage.FRENCH -> "Toutes les couches (${JmaLayerType.entries.size})"
                        AppLanguage.PORTUGUESE -> "Todas as camadas (${JmaLayerType.entries.size})"
                        AppLanguage.RUSSIAN -> "Все слои (${JmaLayerType.entries.size})"
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
                    AppLanguage.KOREAN -> "미국 NEXRAD / HRRR"
                    AppLanguage.ENGLISH -> "US NEXRAD / HRRR"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR"
                    AppLanguage.SPANISH -> "EE. UU. NEXRAD / HRRR"
                    AppLanguage.FRENCH -> "É.-U. NEXRAD / HRRR"
                    AppLanguage.PORTUGUESE -> "EUA NEXRAD / HRRR"
                    AppLanguage.RUSSIAN -> "США NEXRAD / HRRR"
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
                    AppLanguage.KOREAN -> "모든 미국 레이어 (${UsRadarLayerType.entries.size}종)"
                    AppLanguage.ENGLISH -> "All US Layers (${UsRadarLayerType.entries.size})"
                    AppLanguage.SIMPLIFIED_CHINESE -> "所有全美图层 (${UsRadarLayerType.entries.size}种)"
                    AppLanguage.TRADITIONAL_CHINESE -> "所有全美圖層 (${UsRadarLayerType.entries.size}種)"
                    AppLanguage.SPANISH -> "Todas las capas de EE. UU. (${UsRadarLayerType.entries.size})"
                    AppLanguage.FRENCH -> "Toutes les couches É.-U. (${UsRadarLayerType.entries.size})"
                    AppLanguage.PORTUGUESE -> "Todas as camadas dos EUA (${UsRadarLayerType.entries.size})"
                    AppLanguage.RUSSIAN -> "Все слои США (${UsRadarLayerType.entries.size})"
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
                    AppLanguage.KOREAN -> "레이더 제공처 전환"
                    AppLanguage.ENGLISH -> "Switch Radar Source"
                    AppLanguage.SIMPLIFIED_CHINESE -> "切换雷达源"
                    AppLanguage.TRADITIONAL_CHINESE -> "切換雷達來源"
                    AppLanguage.SPANISH -> "Cambiar fuente de radar"
                    AppLanguage.FRENCH -> "Changer de source radar"
                    AppLanguage.PORTUGUESE -> "Trocar fonte do radar"
                    AppLanguage.RUSSIAN -> "Сменить источник радара"
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
                    AppLanguage.KOREAN -> "글로벌 레이더 (RainViewer)"
                    AppLanguage.ENGLISH -> "Global Radar (RainViewer)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 (RainViewer)"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 (RainViewer)"
                    AppLanguage.SPANISH -> "Radar global (RainViewer)"
                    AppLanguage.FRENCH -> "Radar mondial (RainViewer)"
                    AppLanguage.PORTUGUESE -> "Radar global (RainViewer)"
                    AppLanguage.RUSSIAN -> "Глобальный радар (RainViewer)"
                },
                icon = Icons.Rounded.Public,
                isSelected = true,
                onClick = { isSpeedDialOpen = false }
            ),
            SpeedDialItemData(
                title = when (language) {
                    AppLanguage.JAPANESE -> "レーダー提供元切替"
                    AppLanguage.KOREAN -> "레이더 제공처 전환"
                    AppLanguage.ENGLISH -> "Switch Radar Source"
                    AppLanguage.SIMPLIFIED_CHINESE -> "切换雷达源"
                    AppLanguage.TRADITIONAL_CHINESE -> "切換雷達來源"
                    AppLanguage.SPANISH -> "Cambiar fuente de radar"
                    AppLanguage.FRENCH -> "Changer de source radar"
                    AppLanguage.PORTUGUESE -> "Trocar fonte do radar"
                    AppLanguage.RUSSIAN -> "Сменить источник радара"
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
                        AppLanguage.KOREAN -> "뒤로"
                        AppLanguage.ENGLISH -> "Back"
                        AppLanguage.SIMPLIFIED_CHINESE -> "返回"
                        AppLanguage.TRADITIONAL_CHINESE -> "返回"
                        AppLanguage.SPANISH -> "Atrás"
                        AppLanguage.FRENCH -> "Retour"
                        AppLanguage.PORTUGUESE -> "Voltar"
                        AppLanguage.RUSSIAN -> "Назад"
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
                                    AppLanguage.KOREAN -> "일본 기상청 강수·단기 예보"
                                    AppLanguage.ENGLISH -> "JMA Radar & Short-range Fcst"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 降雨短时预报"
                                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 降雨短時預報"
                                    AppLanguage.SPANISH -> "JMA Radar y prev. a corto plazo"
                                    AppLanguage.FRENCH -> "JMA Radar et prév. à court terme"
                                    AppLanguage.PORTUGUESE -> "JMA Radar e prev. de curto prazo"
                                    AppLanguage.RUSSIAN -> "Радар и краткосрочный прогноз JMA"
                                }
                            } else {
                                val jmaPrefix = when (language) {
                                    AppLanguage.JAPANESE -> "気象庁"
                                    AppLanguage.KOREAN -> "기상청"
                                    AppLanguage.ENGLISH -> "JMA"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "气象厅"
                                    AppLanguage.TRADITIONAL_CHINESE -> "氣象廳"
                                    AppLanguage.SPANISH -> "JMA"
                                    AppLanguage.FRENCH -> "JMA"
                                    AppLanguage.PORTUGUESE -> "JMA"
                                    AppLanguage.RUSSIAN -> "JMA"
                                }
                                "$jmaPrefix ${selectedJmaLayer.shortName(language)}"
                            }
                        }
                        RadarDataSource.US_NEXRAD_HRRR -> {
                            if (selectedUsLayer == UsRadarLayerType.UNIFIED_ALL) {
                                when (language) {
                                    AppLanguage.JAPANESE -> "全米 NEXRAD / HRRR (実況・外挿・予報)"
                                    AppLanguage.KOREAN -> "미국 NEXRAD / HRRR (실황·외삽·예보)"
                                    AppLanguage.ENGLISH -> "US NEXRAD / HRRR (Obs, Extrap, Fcst)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR (实况・外推・预报)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR (實況・外推・預報)"
                                    AppLanguage.SPANISH -> "EE. UU. NEXRAD / HRRR (Obs., Extrap., Prev.)"
                                    AppLanguage.FRENCH -> "É.-U. NEXRAD / HRRR (Obs., Extrap., Prév.)"
                                    AppLanguage.PORTUGUESE -> "EUA NEXRAD / HRRR (Obs., Extrap., Prev.)"
                                    AppLanguage.RUSSIAN -> "США NEXRAD / HRRR (Набл., Экстрап., Прогноз)"
                                }
                            } else {
                                selectedUsLayer.displayName(language)
                            }
                        }
                        RadarDataSource.RAIN_VIEWER -> {
                            when (language) {
                                AppLanguage.JAPANESE -> "世界レーダー (RainViewer)"
                                AppLanguage.KOREAN -> "글로벌 레이더 (RainViewer)"
                                AppLanguage.ENGLISH -> "Global Radar (RainViewer)"
                                AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 (RainViewer)"
                                AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 (RainViewer)"
                                AppLanguage.SPANISH -> "Radar global (RainViewer)"
                                AppLanguage.FRENCH -> "Radar mondial (RainViewer)"
                                AppLanguage.PORTUGUESE -> "Radar global (RainViewer)"
                                AppLanguage.RUSSIAN -> "Глобальный радар (RainViewer)"
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
                                    AppLanguage.KOREAN -> "지도 레이어 선택"
                                    AppLanguage.ENGLISH -> "Select Map Layer"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "选择地图图层"
                                    AppLanguage.TRADITIONAL_CHINESE -> "選擇地圖圖層"
                                    AppLanguage.SPANISH -> "Seleccionar capa del mapa"
                                    AppLanguage.FRENCH -> "Sélectionner une couche"
                                    AppLanguage.PORTUGUESE -> "Selecionar camada do mapa"
                                    AppLanguage.RUSSIAN -> "Выбрать слой карты"
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
                                        AppLanguage.KOREAN -> "태풍 실황·경로 예보"
                                        AppLanguage.ENGLISH -> "Track & Warnings"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "台风实况・路径预报"
                                        AppLanguage.TRADITIONAL_CHINESE -> "颱風實況・路徑預報"
                                        AppLanguage.SPANISH -> "Trayectoria y alertas"
                                        AppLanguage.FRENCH -> "Trajectoire et alertes"
                                        AppLanguage.PORTUGUESE -> "Trajetória e alertas"
                                        AppLanguage.RUSSIAN -> "Траектория и угрозы"
                                    }
                                }
                                currentFrame?.isExtrapolation == true -> {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "外挿予測 (0〜60分)"
                                        AppLanguage.KOREAN -> "외삽 예측 (0~60분)"
                                        AppLanguage.ENGLISH -> "Nowcast (0-60m)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "外推预测 (0-60分)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "外推預測 (0-60分)"
                                        AppLanguage.SPANISH -> "Nowcast (0-60 min)"
                                        AppLanguage.FRENCH -> "Nowcast (0-60 min)"
                                        AppLanguage.PORTUGUESE -> "Nowcast (0-60 min)"
                                        AppLanguage.RUSSIAN -> "Наукаст (0–60 мин)"
                                    }
                                }
                                currentFrame?.isForecast == true -> {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "予報モデル"
                                        AppLanguage.KOREAN -> "예보 모델"
                                        AppLanguage.ENGLISH -> "Forecast Model"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "预报模型"
                                        AppLanguage.TRADITIONAL_CHINESE -> "預報模型"
                                        AppLanguage.SPANISH -> "Modelo de previsión"
                                        AppLanguage.FRENCH -> "Modèle de prévision"
                                        AppLanguage.PORTUGUESE -> "Modelo de previsão"
                                        AppLanguage.RUSSIAN -> "Прогнозная модель"
                                    }
                                }
                                else -> currentFrame?.resolutionInfo?.takeIf { it.isNotBlank() }
                            }
                            val nowPrefix = when (language) {
                                AppLanguage.JAPANESE -> "現在"
                                AppLanguage.KOREAN -> "현재"
                                AppLanguage.ENGLISH -> "Now"
                                AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                AppLanguage.SPANISH -> "Ahora"
                                AppLanguage.FRENCH -> "Maintenant"
                                AppLanguage.PORTUGUESE -> "Agora"
                                AppLanguage.RUSSIAN -> "Сейчас"
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
                                    AppLanguage.KOREAN -> "현재 위치로 이동"
                                    AppLanguage.ENGLISH -> "Current Location"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "回到当前位置"
                                    AppLanguage.TRADITIONAL_CHINESE -> "回到當前位置"
                                    AppLanguage.SPANISH -> "Ubicación actual"
                                    AppLanguage.FRENCH -> "Position actuelle"
                                    AppLanguage.PORTUGUESE -> "Localização atual"
                                    AppLanguage.RUSSIAN -> "Текущее местоположение"
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
                                    val radarAccentPalette = currentAccentPalette()
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
                                                color = radarAccentPalette.radarObs.copy(alpha = 0.55f),
                                                topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                                                size = androidx.compose.ui.geometry.Size(obsWidth, h)
                                            )
                                            currentX += obsWidth
                                        }
                                        if (extCount > 0) {
                                            val extWidth = (extCount / total) * w
                                            drawRect(
                                                color = radarAccentPalette.radarExtrap.copy(alpha = 0.78f),
                                                topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                                                size = androidx.compose.ui.geometry.Size(extWidth, h)
                                            )
                                            currentX += extWidth
                                        }
                                        if (fcstCount > 0) {
                                            val fcstWidth = (fcstCount / total) * w
                                            drawRect(
                                                color = radarAccentPalette.radarForecast.copy(alpha = 0.65f),
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
                        val timelinePalette = currentAccentPalette()
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
                                        AppLanguage.KOREAN -> "실황 -2시간"
                                        AppLanguage.ENGLISH -> "Obs -2h"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "实况 -2h"
                                        AppLanguage.TRADITIONAL_CHINESE -> "實況 -2h"
                                        AppLanguage.SPANISH -> "Obs. -2h"
                                        AppLanguage.FRENCH -> "Obs. -2h"
                                        AppLanguage.PORTUGUESE -> "Obs. -2h"
                                        AppLanguage.RUSSIAN -> "Набл. -2 ч"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = timelinePalette.radarObs,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在 0m"
                                        AppLanguage.KOREAN -> "현재 0분"
                                        AppLanguage.ENGLISH -> "Now 0m"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前 0m"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前 0m"
                                        AppLanguage.SPANISH -> "Ahora 0m"
                                        AppLanguage.FRENCH -> "Direct 0m"
                                        AppLanguage.PORTUGUESE -> "Agora 0m"
                                        AppLanguage.RUSSIAN -> "Сейчас 0 м"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "外挿 +60m"
                                        AppLanguage.KOREAN -> "외삽 +60분"
                                        AppLanguage.ENGLISH -> "Nowcast +60m"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "外推 +60m"
                                        AppLanguage.TRADITIONAL_CHINESE -> "外推 +60m"
                                        AppLanguage.SPANISH -> "Nowcast +60m"
                                        AppLanguage.FRENCH -> "Nowcast +60m"
                                        AppLanguage.PORTUGUESE -> "Nowcast +60m"
                                        AppLanguage.RUSSIAN -> "Наукаст +60 м"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = timelinePalette.radarExtrap,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "HRRR +18h",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = timelinePalette.radarForecast,
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
                                        AppLanguage.KOREAN -> "2시간 전"
                                        AppLanguage.ENGLISH -> "2h ago"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "2小时前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "2小時前"
                                        AppLanguage.SPANISH -> "Hace 2h"
                                        AppLanguage.FRENCH -> "Il y a 2h"
                                        AppLanguage.PORTUGUESE -> "Há 2h"
                                        AppLanguage.RUSSIAN -> "2 ч назад"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在"
                                        AppLanguage.KOREAN -> "현재"
                                        AppLanguage.ENGLISH -> "Now"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                        AppLanguage.SPANISH -> "Ahora"
                                        AppLanguage.FRENCH -> "Maintenant"
                                        AppLanguage.PORTUGUESE -> "Agora"
                                        AppLanguage.RUSSIAN -> "Сейчас"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "+15時間後"
                                        AppLanguage.KOREAN -> "+15시간 후"
                                        AppLanguage.ENGLISH -> "+15h"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "+15小时后"
                                        AppLanguage.TRADITIONAL_CHINESE -> "+15小時後"
                                        AppLanguage.SPANISH -> "+15h"
                                        AppLanguage.FRENCH -> "+15h"
                                        AppLanguage.PORTUGUESE -> "+15h"
                                        AppLanguage.RUSSIAN -> "+15 ч"
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
                                        AppLanguage.KOREAN -> "2시간 전"
                                        AppLanguage.ENGLISH -> "2h ago"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "2小时前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "2小時前"
                                        AppLanguage.SPANISH -> "Hace 2h"
                                        AppLanguage.FRENCH -> "Il y a 2h"
                                        AppLanguage.PORTUGUESE -> "Há 2h"
                                        AppLanguage.RUSSIAN -> "2 ч назад"
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
                                            AppLanguage.KOREAN -> "현재"
                                            AppLanguage.ENGLISH -> "Now"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                            AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                            AppLanguage.SPANISH -> "Ahora"
                                            AppLanguage.FRENCH -> "Maintenant"
                                            AppLanguage.PORTUGUESE -> "Agora"
                                            AppLanguage.RUSSIAN -> "Сейчас"
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
                                            AppLanguage.KOREAN -> "+${nowcastMin}분 예측"
                                            AppLanguage.ENGLISH -> "+${nowcastMin}m"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "+${nowcastMin}分预测"
                                            AppLanguage.TRADITIONAL_CHINESE -> "+${nowcastMin}分預測"
                                            AppLanguage.SPANISH -> "+${nowcastMin}m"
                                            AppLanguage.FRENCH -> "+${nowcastMin}m"
                                            AppLanguage.PORTUGUESE -> "+${nowcastMin}m"
                                            AppLanguage.RUSSIAN -> "+${nowcastMin} м"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = timelinePalette.radarExtrap,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "1時間前"
                                            AppLanguage.KOREAN -> "1시간 전"
                                            AppLanguage.ENGLISH -> "1h ago"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "1小时前"
                                            AppLanguage.TRADITIONAL_CHINESE -> "1小時前"
                                            AppLanguage.SPANISH -> "Hace 1h"
                                            AppLanguage.FRENCH -> "Il y a 1h"
                                            AppLanguage.PORTUGUESE -> "Há 1h"
                                            AppLanguage.RUSSIAN -> "1 ч назад"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Normal,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "現在 (最新実況)"
                                            AppLanguage.KOREAN -> "현재 (최신 실황)"
                                            AppLanguage.ENGLISH -> "Now (Live)"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "当前 (最新实况)"
                                            AppLanguage.TRADITIONAL_CHINESE -> "當前 (最新實況)"
                                            AppLanguage.SPANISH -> "Ahora (En vivo)"
                                            AppLanguage.FRENCH -> "Maintenant (Direct)"
                                            AppLanguage.PORTUGUESE -> "Agora (Ao vivo)"
                                            AppLanguage.RUSSIAN -> "Сейчас (Live)"
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
                                        AppLanguage.KOREAN -> "1시간 전"
                                        AppLanguage.ENGLISH -> "1h ago"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "1小时前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "1小時前"
                                        AppLanguage.SPANISH -> "Hace 1h"
                                        AppLanguage.FRENCH -> "Il y a 1h"
                                        AppLanguage.PORTUGUESE -> "Há 1h"
                                        AppLanguage.RUSSIAN -> "1 ч назад"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在"
                                        AppLanguage.KOREAN -> "현재"
                                        AppLanguage.ENGLISH -> "Now"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                        AppLanguage.SPANISH -> "Ahora"
                                        AppLanguage.FRENCH -> "Maintenant"
                                        AppLanguage.PORTUGUESE -> "Agora"
                                        AppLanguage.RUSSIAN -> "Сейчас"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "+60分後"
                                        AppLanguage.KOREAN -> "+60분 후"
                                        AppLanguage.ENGLISH -> "+60m"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "+60分钟后"
                                        AppLanguage.TRADITIONAL_CHINESE -> "+60分鐘後"
                                        AppLanguage.SPANISH -> "+60m"
                                        AppLanguage.FRENCH -> "+60m"
                                        AppLanguage.PORTUGUESE -> "+60m"
                                        AppLanguage.RUSSIAN -> "+60 м"
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
                                        AppLanguage.KOREAN -> "과거"
                                        AppLanguage.ENGLISH -> "Past"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "过去"
                                        AppLanguage.TRADITIONAL_CHINESE -> "過去"
                                        AppLanguage.SPANISH -> "Pasado"
                                        AppLanguage.FRENCH -> "Passé"
                                        AppLanguage.PORTUGUESE -> "Passado"
                                        AppLanguage.RUSSIAN -> "Прошлое"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "現在"
                                        AppLanguage.KOREAN -> "현재"
                                        AppLanguage.ENGLISH -> "Now"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前"
                                        AppLanguage.SPANISH -> "Ahora"
                                        AppLanguage.FRENCH -> "Maintenant"
                                        AppLanguage.PORTUGUESE -> "Agora"
                                        AppLanguage.RUSSIAN -> "Сейчас"
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
                                            AppLanguage.KOREAN -> "예측"
                                            AppLanguage.ENGLISH -> "Forecast"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "预测"
                                            AppLanguage.TRADITIONAL_CHINESE -> "預測"
                                            AppLanguage.SPANISH -> "Previsión"
                                            AppLanguage.FRENCH -> "Prévision"
                                            AppLanguage.PORTUGUESE -> "Previsão"
                                            AppLanguage.RUSSIAN -> "Прогноз"
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
                                        AppLanguage.KOREAN -> "일본 기상청 태풍 경로 예보 (실황·폭풍역·예보원)"
                                        AppLanguage.ENGLISH -> "JMA Typhoon Track & Warnings (Active / Forecast)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "气象厅 台风路径预报 (实况・暴风区・预报圆)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "氣象廳 颱風路徑預報 (實況・暴風區・預報圓)"
                                        AppLanguage.SPANISH -> "JMA Trayectoria y alertas de tifón (Actual / Previsión)"
                                        AppLanguage.FRENCH -> "JMA Trajectoire et alertes typhon (Actuel / Prévision)"
                                        AppLanguage.PORTUGUESE -> "JMA Trajetória e alertas de tufão (Atual / Previsão)"
                                        AppLanguage.RUSSIAN -> "Траектория и предупреждения о тайфунах JMA (Сейчас / Прогноз)"
                                    }
                                } else {
                                    when (language) {
                                        AppLanguage.JAPANESE -> "公式背景地図レイヤー表示中"
                                        AppLanguage.KOREAN -> "공식 배경 지도 레이어 표시 중"
                                        AppLanguage.ENGLISH -> "Base map layer active"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "官方背景地图图层显示中"
                                        AppLanguage.TRADITIONAL_CHINESE -> "官方背景地圖圖層顯示中"
                                        AppLanguage.SPANISH -> "Capa de mapa base activa"
                                        AppLanguage.FRENCH -> "Couche de fond de carte active"
                                        AppLanguage.PORTUGUESE -> "Camada de mapa base ativa"
                                        AppLanguage.RUSSIAN -> "Активен базовый слой карты"
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
                                            AppLanguage.KOREAN -> "이전 프레임"
                                            AppLanguage.ENGLISH -> "Previous Frame"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "上一帧"
                                            AppLanguage.TRADITIONAL_CHINESE -> "上一幀"
                                            AppLanguage.SPANISH -> "Fotograma anterior"
                                            AppLanguage.FRENCH -> "Image précédente"
                                            AppLanguage.PORTUGUESE -> "Quadro anterior"
                                            AppLanguage.RUSSIAN -> "Предыдущий кадр"
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
                                                AppLanguage.KOREAN -> "일시정지"
                                                AppLanguage.ENGLISH -> "Pause"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "暂停"
                                                AppLanguage.TRADITIONAL_CHINESE -> "暫停"
                                                AppLanguage.SPANISH -> "Pausar"
                                                AppLanguage.FRENCH -> "Pause"
                                                AppLanguage.PORTUGUESE -> "Pausar"
                                                AppLanguage.RUSSIAN -> "Пауза"
                                            }
                                        } else {
                                            when (language) {
                                                AppLanguage.JAPANESE -> "再生"
                                                AppLanguage.KOREAN -> "재생"
                                                AppLanguage.ENGLISH -> "Play"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "播放"
                                                AppLanguage.TRADITIONAL_CHINESE -> "播放"
                                                AppLanguage.SPANISH -> "Reproducir"
                                                AppLanguage.FRENCH -> "Lire"
                                                AppLanguage.PORTUGUESE -> "Reproduzir"
                                                AppLanguage.RUSSIAN -> "Воспроизвести"
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
                                            AppLanguage.KOREAN -> "다음 프레임"
                                            AppLanguage.ENGLISH -> "Next Frame"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "下一帧"
                                            AppLanguage.TRADITIONAL_CHINESE -> "下一幀"
                                            AppLanguage.SPANISH -> "Siguiente fotograma"
                                            AppLanguage.FRENCH -> "Image suivante"
                                            AppLanguage.PORTUGUESE -> "Próximo quadro"
                                            AppLanguage.RUSSIAN -> "Следующий кадр"
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
                        AppLanguage.KOREAN -> "레이더 반사도 (dBZ)"
                        AppLanguage.ENGLISH -> "Radar Reflectivity (dBZ)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷达反射强度 (dBZ)"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷達反射強度 (dBZ)"
                        AppLanguage.SPANISH -> "Reflectividad del radar (dBZ)"
                        AppLanguage.FRENCH -> "Réflectivité radar (dBZ)"
                        AppLanguage.PORTUGUESE -> "Refletividade do radar (dBZ)"
                        AppLanguage.RUSSIAN -> "Отражаемость радара (dBZ)"
                    }
                    RadarLegendKind.RAIN_INTENSITY -> when (language) {
                        AppLanguage.JAPANESE -> "雨の強さ (mm/h)"
                        AppLanguage.KOREAN -> "강수 강도 (mm/h)"
                        AppLanguage.ENGLISH -> "Rain Intensity (mm/h)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降雨强度 (mm/h)"
                        AppLanguage.TRADITIONAL_CHINESE -> "降雨強度 (mm/h)"
                        AppLanguage.SPANISH -> "Intensidad de lluvia (mm/h)"
                        AppLanguage.FRENCH -> "Intensité des pluies (mm/h)"
                        AppLanguage.PORTUGUESE -> "Intensidade da chuva (mm/h)"
                        AppLanguage.RUSSIAN -> "Интенсивность дождя (мм/ч)"
                    }
                    RadarLegendKind.LIGHTNING -> when (language) {
                        AppLanguage.JAPANESE -> "雷活動度 (活動度1〜4)"
                        AppLanguage.KOREAN -> "낙뢰 활동도 (1~4단계)"
                        AppLanguage.ENGLISH -> "Lightning Activity (1-4)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雷电活动度 (1~4)"
                        AppLanguage.TRADITIONAL_CHINESE -> "雷電活動度 (1~4)"
                        AppLanguage.SPANISH -> "Actividad eléctrica (1-4)"
                        AppLanguage.FRENCH -> "Activité orageuse (1-4)"
                        AppLanguage.PORTUGUESE -> "Atividade de raios (1-4)"
                        AppLanguage.RUSSIAN -> "Грозовая активность (1–4)"
                    }
                    RadarLegendKind.KIKIKURU -> when (selectedJmaLayer) {
                        JmaLayerType.LANDSLIDE -> when (language) {
                            AppLanguage.JAPANESE -> "土砂キキクル (危険度)"
                            AppLanguage.KOREAN -> "산사태 키키쿠루 (위험도)"
                            AppLanguage.ENGLISH -> "Landslide Kikikuru (Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "泥石流风险 (Kikikuru)"
                            AppLanguage.TRADITIONAL_CHINESE -> "土石流風險 (Kikikuru)"
                            AppLanguage.SPANISH -> "Riesgo de deslizamiento (Kikikuru)"
                            AppLanguage.FRENCH -> "Risque de glissement (Kikikuru)"
                            AppLanguage.PORTUGUESE -> "Risco de deslizamento (Kikikuru)"
                            AppLanguage.RUSSIAN -> "Риск оползней (Kikikuru)"
                        }
                        JmaLayerType.INUNDATION -> when (language) {
                            AppLanguage.JAPANESE -> "浸水キキクル (危険度)"
                            AppLanguage.KOREAN -> "침수 키키쿠루 (위험도)"
                            AppLanguage.ENGLISH -> "Inundation Kikikuru (Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "积水淹没风险 (Kikikuru)"
                            AppLanguage.TRADITIONAL_CHINESE -> "積水淹沒風險 (Kikikuru)"
                            AppLanguage.SPANISH -> "Riesgo de anegamiento (Kikikuru)"
                            AppLanguage.FRENCH -> "Risque de submersion (Kikikuru)"
                            AppLanguage.PORTUGUESE -> "Risco de alagamento (Kikikuru)"
                            AppLanguage.RUSSIAN -> "Риск подтоплений (Kikikuru)"
                        }
                        JmaLayerType.FLOOD -> when (language) {
                            AppLanguage.JAPANESE -> "洪水キキクル (危険度)"
                            AppLanguage.KOREAN -> "홍수 키키쿠루 (위험도)"
                            AppLanguage.ENGLISH -> "Flood Kikikuru (Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "洪水风险 (Kikikuru)"
                            AppLanguage.TRADITIONAL_CHINESE -> "洪水風險 (Kikikuru)"
                            AppLanguage.SPANISH -> "Riesgo de inundación (Kikikuru)"
                            AppLanguage.FRENCH -> "Risque de crue (Kikikuru)"
                            AppLanguage.PORTUGUESE -> "Risco de enchente (Kikikuru)"
                            AppLanguage.RUSSIAN -> "Риск наводнений (Kikikuru)"
                        }
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "キキクル (危険度分布)"
                            AppLanguage.KOREAN -> "키키쿠루 (위험도 분포)"
                            AppLanguage.ENGLISH -> "Kikikuru (Hazard Risk)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "Kikikuru (灾害风险)"
                            AppLanguage.TRADITIONAL_CHINESE -> "Kikikuru (災害風險)"
                            AppLanguage.SPANISH -> "Kikikuru (Riesgo de desastre)"
                            AppLanguage.FRENCH -> "Kikikuru (Risque de catastrophe)"
                            AppLanguage.PORTUGUESE -> "Kikikuru (Risco de desastre)"
                            AppLanguage.RUSSIAN -> "Kikikuru (Уровень опасности)"
                        }
                    }
                    RadarLegendKind.SNOW_DEPTH -> when (language) {
                        AppLanguage.JAPANESE -> "現在の積雪深 (cm)"
                        AppLanguage.KOREAN -> "현재 적설량 (cm)"
                        AppLanguage.ENGLISH -> "Snow Depth (cm)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "当前积雪深度 (cm)"
                        AppLanguage.TRADITIONAL_CHINESE -> "當前積雪深度 (cm)"
                        AppLanguage.SPANISH -> "Espesor de nieve (cm)"
                        AppLanguage.FRENCH -> "Hauteur de neige (cm)"
                        AppLanguage.PORTUGUESE -> "Profundidade da neve (cm)"
                        AppLanguage.RUSSIAN -> "Высота снега (см)"
                    }
                    RadarLegendKind.SNOWFALL_3H -> when (language) {
                        AppLanguage.JAPANESE -> "3時間降雪量 (cm)"
                        AppLanguage.KOREAN -> "3시간 강설량 (cm)"
                        AppLanguage.ENGLISH -> "3-Hour Snowfall (cm)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "3小时降雪量 (cm)"
                        AppLanguage.TRADITIONAL_CHINESE -> "3小時降雪量 (cm)"
                        AppLanguage.SPANISH -> "Nevada en 3 horas (cm)"
                        AppLanguage.FRENCH -> "Chutes de neige 3 h (cm)"
                        AppLanguage.PORTUGUESE -> "Neve em 3 horas (cm)"
                        AppLanguage.RUSSIAN -> "Снегопад за 3 ч (см)"
                    }
                    RadarLegendKind.SNOWFALL_24H -> when (language) {
                        AppLanguage.JAPANESE -> "24時間降雪量 (cm)"
                        AppLanguage.KOREAN -> "24시간 강설량 (cm)"
                        AppLanguage.ENGLISH -> "24-Hour Snowfall (cm)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "24小时降雪量 (cm)"
                        AppLanguage.TRADITIONAL_CHINESE -> "24小時降雪量 (cm)"
                        AppLanguage.SPANISH -> "Nevada en 24 horas (cm)"
                        AppLanguage.FRENCH -> "Chutes de neige 24 h (cm)"
                        AppLanguage.PORTUGUESE -> "Neve em 24 horas (cm)"
                        AppLanguage.RUSSIAN -> "Снегопад за 24 ч (см)"
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
                                        AppLanguage.KOREAN -> "닫기"
                                        AppLanguage.ENGLISH -> "Close"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                        AppLanguage.SPANISH -> "Cerrar"
                                        AppLanguage.FRENCH -> "Fermer"
                                        AppLanguage.PORTUGUESE -> "Fechar"
                                        AppLanguage.RUSSIAN -> "Закрыть"
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
                                    AppLanguage.KOREAN -> "상세 가이드 ❯"
                                    AppLanguage.ENGLISH -> "Detailed Guide ❯"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "详细指南 ❯"
                                    AppLanguage.TRADITIONAL_CHINESE -> "詳細指南 ❯"
                                    AppLanguage.SPANISH -> "Guía detallada ❯"
                                    AppLanguage.FRENCH -> "Guide détaillé ❯"
                                    AppLanguage.PORTUGUESE -> "Guia detalhado ❯"
                                    AppLanguage.RUSSIAN -> "Подробнее ❯"
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
                                            AppLanguage.KOREAN -> "미국 기상·예보 지도 레이어"
                                            AppLanguage.ENGLISH -> "US Weather & Forecast Maps"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "全美气象与预报图层"
                                            AppLanguage.TRADITIONAL_CHINESE -> "全美氣象與預報圖層"
                                            AppLanguage.SPANISH -> "Mapas meteorológicos de EE. UU."
                                            AppLanguage.FRENCH -> "Cartes météo et prévisions É.-U."
                                            AppLanguage.PORTUGUESE -> "Mapas meteorológicos dos EUA"
                                            AppLanguage.RUSSIAN -> "Погодные карты США"
                                        }
                                    } else {
                                        when (language) {
                                            AppLanguage.JAPANESE -> "気象地図レイヤー選択"
                                            AppLanguage.KOREAN -> "기상 지도 레이어 선택"
                                            AppLanguage.ENGLISH -> "Weather Map Layers"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "气象地图图层选择"
                                            AppLanguage.TRADITIONAL_CHINESE -> "氣象地圖圖層選擇"
                                            AppLanguage.SPANISH -> "Capas del mapa meteorológico"
                                            AppLanguage.FRENCH -> "Couches de carte météo"
                                            AppLanguage.PORTUGUESE -> "Camadas do mapa meteorológico"
                                            AppLanguage.RUSSIAN -> "Слои погодной карты"
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
                                        AppLanguage.KOREAN -> "NOAA / NEXRAD / HRRR 시뮬레이션 (총 ${UsRadarLayerType.entries.size}종)"
                                        AppLanguage.ENGLISH -> "NOAA / NEXRAD / HRRR Simulation (${UsRadarLayerType.entries.size} Layers)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "NOAA / NEXRAD / HRRR 模拟 (全${UsRadarLayerType.entries.size}种)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "NOAA / NEXRAD / HRRR 模擬 (全${UsRadarLayerType.entries.size}種)"
                                        AppLanguage.SPANISH -> "Simulación NOAA / NEXRAD / HRRR (${UsRadarLayerType.entries.size} capas)"
                                        AppLanguage.FRENCH -> "Simulation NOAA / NEXRAD / HRRR (${UsRadarLayerType.entries.size} couches)"
                                        AppLanguage.PORTUGUESE -> "Simulação NOAA / NEXRAD / HRRR (${UsRadarLayerType.entries.size} camadas)"
                                        AppLanguage.RUSSIAN -> "Модель NOAA / NEXRAD / HRRR (${UsRadarLayerType.entries.size} сл.)"
                                    }
                                } else {
                                    val count = JmaLayerType.entries.count { showAllJapanLayers || isAllowedJapanRadarLayer(it) }
                                    when (language) {
                                        AppLanguage.JAPANESE -> "気象庁公式配信タイル (全${count}種類)"
                                        AppLanguage.KOREAN -> "일본 기상청 공식 타일 (총 ${count}종)"
                                        AppLanguage.ENGLISH -> "JMA Official Tiles ($count Layers)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅官方切片 (全${count}种)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳官方圖磚 (全${count}種)"
                                        AppLanguage.SPANISH -> "Mosaicos oficiales de JMA ($count capas)"
                                        AppLanguage.FRENCH -> "Tuiles officielles JMA ($count couches)"
                                        AppLanguage.PORTUGUESE -> "Blocos oficiais da JMA ($count camadas)"
                                        AppLanguage.RUSSIAN -> "Официальные тайлы JMA ($count сл.)"
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
                                    AppLanguage.KOREAN -> "닫기"
                                    AppLanguage.ENGLISH -> "Close"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                    AppLanguage.SPANISH -> "Cerrar"
                                    AppLanguage.FRENCH -> "Fermer"
                                    AppLanguage.PORTUGUESE -> "Fechar"
                                    AppLanguage.RUSSIAN -> "Закрыть"
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
                                                        AppLanguage.KOREAN -> "${layer.updateIntervalMinutes}분마다 갱신"
                                                        AppLanguage.ENGLISH -> "${layer.updateIntervalMinutes}m update"
                                                        AppLanguage.SIMPLIFIED_CHINESE -> "${layer.updateIntervalMinutes}分更新"
                                                        AppLanguage.TRADITIONAL_CHINESE -> "${layer.updateIntervalMinutes}分更新"
                                                        AppLanguage.SPANISH -> "Act. cada ${layer.updateIntervalMinutes} min"
                                                        AppLanguage.FRENCH -> "Màj ${layer.updateIntervalMinutes} min"
                                                        AppLanguage.PORTUGUESE -> "Atual. a cada ${layer.updateIntervalMinutes} min"
                                                        AppLanguage.RUSSIAN -> "Обн. каждые ${layer.updateIntervalMinutes} мин"
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
                                        AppLanguage.KOREAN -> "미국 레이더 dBZ 반사도 기준"
                                        AppLanguage.ENGLISH -> "US Radar dBZ Reflectivity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "全美雷达 dBZ 反射强度标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "全美雷達 dBZ 反射強度標準"
                                        AppLanguage.SPANISH -> "Escala de reflectividad dBZ (EE. UU.)"
                                        AppLanguage.FRENCH -> "Échelle de réflectivité dBZ (É.-U.)"
                                        AppLanguage.PORTUGUESE -> "Escala de refletividade dBZ (EUA)"
                                        AppLanguage.RUSSIAN -> "Шкала отражаемости dBZ (США)"
                                    }
                                    RadarLegendKind.RAIN_VIEWER -> when (language) {
                                        AppLanguage.JAPANESE -> "世界レーダー dBZ 反射強度基準"
                                        AppLanguage.KOREAN -> "글로벌 레이더 dBZ 반사도 기준"
                                        AppLanguage.ENGLISH -> "Global Radar dBZ Reflectivity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 dBZ 反射强度标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 dBZ 反射強度標準"
                                        AppLanguage.SPANISH -> "Escala de reflectividad dBZ (Global)"
                                        AppLanguage.FRENCH -> "Échelle de réflectivité dBZ (Mondiale)"
                                        AppLanguage.PORTUGUESE -> "Escala de refletividade dBZ (Global)"
                                        AppLanguage.RUSSIAN -> "Шкала отражаемости dBZ (Мир)"
                                    }
                                    RadarLegendKind.LIGHTNING -> when (language) {
                                        AppLanguage.JAPANESE -> "雷ナウキャスト 活動度基準・ガイド"
                                        AppLanguage.KOREAN -> "낙뢰 나우캐스트 활동도 기준·가이드"
                                        AppLanguage.ENGLISH -> "Lightning Nowcast Activity Scale & Guide"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "雷电即时预报活动度等级指南"
                                        AppLanguage.TRADITIONAL_CHINESE -> "雷電即時預報活動度等級指南"
                                        AppLanguage.SPANISH -> "Escala y guía de actividad eléctrica"
                                        AppLanguage.FRENCH -> "Échelle et guide d'activité orageuse"
                                        AppLanguage.PORTUGUESE -> "Escala e guia de atividade de raios"
                                        AppLanguage.RUSSIAN -> "Шкала и справочник грозовой активности"
                                    }
                                    RadarLegendKind.KIKIKURU -> when (language) {
                                        AppLanguage.JAPANESE -> "キキクル（危険度分布）判定基準・ガイド"
                                        AppLanguage.KOREAN -> "키키쿠루(위험도 분포) 판정 기준·가이드"
                                        AppLanguage.ENGLISH -> "Kikikuru Risk Level Scale & Action Guide"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "Kikikuru 风险分布判定等级指南"
                                        AppLanguage.TRADITIONAL_CHINESE -> "Kikikuru 風險分布判定等級指南"
                                        AppLanguage.SPANISH -> "Escala de riesgo Kikikuru y guía de acción"
                                        AppLanguage.FRENCH -> "Échelle de risque Kikikuru et guide d'action"
                                        AppLanguage.PORTUGUESE -> "Escala de risco Kikikuru e guia de ação"
                                        AppLanguage.RUSSIAN -> "Шкала рисков Kikikuru и рекомендации"
                                    }
                                    RadarLegendKind.SNOW_DEPTH -> when (language) {
                                        AppLanguage.JAPANESE -> "現在の積雪の深さ 区分・目安"
                                        AppLanguage.KOREAN -> "현재 적설량 구분·기준"
                                        AppLanguage.ENGLISH -> "Snow Depth Scale & Travel Guide"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "当前积雪深度等级・出行参考"
                                        AppLanguage.TRADITIONAL_CHINESE -> "當前積雪深度等級・出行參考"
                                        AppLanguage.SPANISH -> "Escala de espesor de nieve y guía vial"
                                        AppLanguage.FRENCH -> "Échelle de hauteur de neige et guide"
                                        AppLanguage.PORTUGUESE -> "Escala de profundidade da neve e guia"
                                        AppLanguage.RUSSIAN -> "Шкала высоты снежного покрова"
                                    }
                                    RadarLegendKind.SNOWFALL_3H -> when (language) {
                                        AppLanguage.JAPANESE -> "3時間降雪量 区分・目安"
                                        AppLanguage.KOREAN -> "3시간 강설량 구분·기준"
                                        AppLanguage.ENGLISH -> "3-Hour Snowfall Intensity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "3小时降雪量等级・出行参考"
                                        AppLanguage.TRADITIONAL_CHINESE -> "3小時降雪量等級・出行參考"
                                        AppLanguage.SPANISH -> "Escala de nevada en 3 horas"
                                        AppLanguage.FRENCH -> "Échelle des chutes de neige (3 h)"
                                        AppLanguage.PORTUGUESE -> "Escala de neve em 3 horas"
                                        AppLanguage.RUSSIAN -> "Шкала снегопада за 3 часа"
                                    }
                                    RadarLegendKind.SNOWFALL_24H -> when (language) {
                                        AppLanguage.JAPANESE -> "24時間降雪量 区分・目安"
                                        AppLanguage.KOREAN -> "24시간 강설량 구분·기준"
                                        AppLanguage.ENGLISH -> "24-Hour Snowfall Intensity Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "24小时降雪量等级・出行参考"
                                        AppLanguage.TRADITIONAL_CHINESE -> "24小時降雪量等級・出行參考"
                                        AppLanguage.SPANISH -> "Escala de nevada en 24 horas"
                                        AppLanguage.FRENCH -> "Échelle des chutes de neige (24 h)"
                                        AppLanguage.PORTUGUESE -> "Escala de neve em 24 horas"
                                        AppLanguage.RUSSIAN -> "Шкала снегопада за 24 часа"
                                    }
                                    else -> when (language) {
                                        AppLanguage.JAPANESE -> "雨の強さと降水量の目安"
                                        AppLanguage.KOREAN -> "비의 세기와 강수량 기준"
                                        AppLanguage.ENGLISH -> "Rain Intensity & Precipitation Scale"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "降雨强度与降水量等级"
                                        AppLanguage.TRADITIONAL_CHINESE -> "降雨強度與降水量等級"
                                        AppLanguage.SPANISH -> "Escala de intensidad de lluvia"
                                        AppLanguage.FRENCH -> "Échelle d'intensité des pluies"
                                        AppLanguage.PORTUGUESE -> "Escala de intensidade da chuva"
                                        AppLanguage.RUSSIAN -> "Шкала интенсивности дождя"
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
                                        AppLanguage.KOREAN -> "NOAA / NEXRAD / MRMS 표준 컬러 팔레트 (dBZ)"
                                        AppLanguage.ENGLISH -> "NOAA / NEXRAD / MRMS Standard Color Palette (dBZ)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "NOAA / NEXRAD / MRMS 标准色盘 (dBZ)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "NOAA / NEXRAD / MRMS 標準色盤 (dBZ)"
                                        AppLanguage.SPANISH -> "Paleta estándar NOAA / NEXRAD / MRMS (dBZ)"
                                        AppLanguage.FRENCH -> "Palette standard NOAA / NEXRAD / MRMS (dBZ)"
                                        AppLanguage.PORTUGUESE -> "Paleta padrão NOAA / NEXRAD / MRMS (dBZ)"
                                        AppLanguage.RUSSIAN -> "Стандартная палитра NOAA / NEXRAD / MRMS (dBZ)"
                                    }
                                    RadarLegendKind.RAIN_VIEWER -> when (language) {
                                        AppLanguage.JAPANESE -> "RainViewer Universal Blue 標準カラーパレット (dBZ)"
                                        AppLanguage.KOREAN -> "RainViewer Universal Blue 표준 컬러 팔레트 (dBZ)"
                                        AppLanguage.ENGLISH -> "RainViewer Universal Blue Color Palette (dBZ)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "RainViewer Universal Blue 标准色盘 (dBZ)"
                                        AppLanguage.TRADITIONAL_CHINESE -> "RainViewer Universal Blue 標準色盤 (dBZ)"
                                        AppLanguage.SPANISH -> "Paleta RainViewer Universal Blue (dBZ)"
                                        AppLanguage.FRENCH -> "Palette RainViewer Universal Blue (dBZ)"
                                        AppLanguage.PORTUGUESE -> "Paleta RainViewer Universal Blue (dBZ)"
                                        AppLanguage.RUSSIAN -> "Палитра RainViewer Universal Blue (dBZ)"
                                    }
                                    RadarLegendKind.LIGHTNING -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 雷ナウキャスト（活動度4〜1）基準"
                                        AppLanguage.KOREAN -> "일본 기상청 낙뢰 나우캐스트(활동도 4~1) 기준"
                                        AppLanguage.ENGLISH -> "JMA Lightning Nowcast Activity Levels 1–4"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅雷电即时预报（活动度4〜1）标准"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳雷電即時預報（活動度4〜1）標準"
                                        AppLanguage.SPANISH -> "Niveles de actividad eléctrica JMA (1–4)"
                                        AppLanguage.FRENCH -> "Niveaux d'activité orageuse JMA (1–4)"
                                        AppLanguage.PORTUGUESE -> "Níveis de atividade de raios JMA (1–4)"
                                        AppLanguage.RUSSIAN -> "Уровни грозовой активности JMA (1–4)"
                                    }
                                    RadarLegendKind.KIKIKURU -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 警戒レベル・災害切迫度（Lv1〜Lv5）"
                                        AppLanguage.KOREAN -> "일본 기상청 경계 레벨·재해 위험도(Lv1~Lv5)"
                                        AppLanguage.ENGLISH -> "JMA Alert Levels & Evacuation Guidance (Lv1–Lv5)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅警戒级别・灾害危险度（Lv1〜Lv5）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳警戒級別・災害危險度（Lv1〜Lv5）"
                                        AppLanguage.SPANISH -> "Niveles de alerta y evacuación JMA (Nv1–Nv5)"
                                        AppLanguage.FRENCH -> "Niveaux d'alerte et d'évacuation JMA (Niv. 1–5)"
                                        AppLanguage.PORTUGUESE -> "Níveis de alerta e evacuação JMA (Nv1–Nv5)"
                                        AppLanguage.RUSSIAN -> "Уровни тревоги и эвакуации JMA (Ур. 1–5)"
                                    }
                                    RadarLegendKind.SNOW_DEPTH -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 解析積雪深・アメダス積雪階級（cm）"
                                        AppLanguage.KOREAN -> "일본 기상청 해석 적설량·AMeDAS 적설 등급 (cm)"
                                        AppLanguage.ENGLISH -> "JMA Analyzed Snow Depth Standards (cm)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅解析积雪深度标准（cm）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳解析積雪深度標準（cm）"
                                        AppLanguage.SPANISH -> "Estándares JMA de espesor de nieve (cm)"
                                        AppLanguage.FRENCH -> "Normes JMA de hauteur de neige (cm)"
                                        AppLanguage.PORTUGUESE -> "Padrões JMA de profundidade da neve (cm)"
                                        AppLanguage.RUSSIAN -> "Стандарты высоты снежного покрова JMA (см)"
                                    }
                                    RadarLegendKind.SNOWFALL_3H -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 3時間解析降雪量・短時間豪雪警戒（cm）"
                                        AppLanguage.KOREAN -> "일본 기상청 3시간 해석 강설량·단시간 폭설 경계 (cm)"
                                        AppLanguage.ENGLISH -> "JMA 3-Hour Analyzed Snowfall Standards (cm)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅3小时解析降雪量标准（cm）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳3小時解析降雪量標準（cm）"
                                        AppLanguage.SPANISH -> "Estándares JMA de nevada en 3 h (cm)"
                                        AppLanguage.FRENCH -> "Normes JMA de chutes de neige en 3 h (cm)"
                                        AppLanguage.PORTUGUESE -> "Padrões JMA de neve em 3 h (cm)"
                                        AppLanguage.RUSSIAN -> "Стандарты снегопада JMA за 3 ч (см)"
                                    }
                                    RadarLegendKind.SNOWFALL_24H -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 24時間解析降雪量・大雪警戒（cm）"
                                        AppLanguage.KOREAN -> "일본 기상청 24시간 해석 강설량·대설 경계 (cm)"
                                        AppLanguage.ENGLISH -> "JMA 24-Hour Analyzed Snowfall Standards (cm)"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅24小时解析降雪量标准（cm）"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳24小時解析降雪量標準（cm）"
                                        AppLanguage.SPANISH -> "Estándares JMA de nevada en 24 h (cm)"
                                        AppLanguage.FRENCH -> "Normes JMA de chutes de neige en 24 h (cm)"
                                        AppLanguage.PORTUGUESE -> "Padrões JMA de neve em 24 h (cm)"
                                        AppLanguage.RUSSIAN -> "Стандарты снегопада JMA за 24 ч (см)"
                                    }
                                    else -> when (language) {
                                        AppLanguage.JAPANESE -> "気象庁 降水ナウキャスト色基準・予報一覧"
                                        AppLanguage.KOREAN -> "일본 기상청 강수 나우캐스트 색상 기준·예보 목록"
                                        AppLanguage.ENGLISH -> "JMA Precipitation Nowcast Color Scale & Forecast"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 降水即时预报颜色标准・预报列表"
                                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 降水即時預報顏色標準・預報列表"
                                        AppLanguage.SPANISH -> "Escala de color y previsión JMA Nowcast"
                                        AppLanguage.FRENCH -> "Échelle de couleurs et prévision JMA Nowcast"
                                        AppLanguage.PORTUGUESE -> "Escala de cores e previsão JMA Nowcast"
                                        AppLanguage.RUSSIAN -> "Цветовая шкала и прогноз JMA Nowcast"
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
                                    AppLanguage.KOREAN -> "닫기"
                                    AppLanguage.ENGLISH -> "Close"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                    AppLanguage.SPANISH -> "Cerrar"
                                    AppLanguage.FRENCH -> "Fermer"
                                    AppLanguage.PORTUGUESE -> "Fechar"
                                    AppLanguage.RUSSIAN -> "Закрыть"
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
                                    AppLanguage.KOREAN -> "닫기"
                                    AppLanguage.ENGLISH -> "Close"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                                    AppLanguage.SPANISH -> "Cerrar"
                                    AppLanguage.FRENCH -> "Fermer"
                                    AppLanguage.PORTUGUESE -> "Fechar"
                                    AppLanguage.RUSSIAN -> "Закрыть"
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
                    AppLanguage.KOREAN -> "레이더 제공처 선택"
                    AppLanguage.ENGLISH -> "Select Radar Source"
                    AppLanguage.SIMPLIFIED_CHINESE -> "选择雷达数据源"
                    AppLanguage.TRADITIONAL_CHINESE -> "選擇雷達資料來源"
                    AppLanguage.SPANISH -> "Seleccionar fuente de radar"
                    AppLanguage.FRENCH -> "Sélectionner la source radar"
                    AppLanguage.PORTUGUESE -> "Selecionar fonte do radar"
                    AppLanguage.RUSSIAN -> "Выбрать источник радара"
                },
                options = listOf(
                    RadarSourceMode.AUTO to when (language) {
                        AppLanguage.JAPANESE -> "自動判別 (位置・国名に連動)"
                        AppLanguage.KOREAN -> "자동 선택 (위치·국가 연동)"
                        AppLanguage.ENGLISH -> "Auto (Based on location)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "自动判别 (根据位置)"
                        AppLanguage.TRADITIONAL_CHINESE -> "自動判別 (根據位置)"
                        AppLanguage.SPANISH -> "Automático (Según ubicación)"
                        AppLanguage.FRENCH -> "Automatique (Selon la position)"
                        AppLanguage.PORTUGUESE -> "Automático (Por localização)"
                        AppLanguage.RUSSIAN -> "Авто (По местоположению)"
                    },
                    RadarSourceMode.US_NEXRAD_HRRR to when (language) {
                        AppLanguage.JAPANESE -> "全米 NEXRAD / HRRR (実況・外挿予測・予報)"
                        AppLanguage.KOREAN -> "미국 NEXRAD / HRRR (실황·외삽 예측·예보)"
                        AppLanguage.ENGLISH -> "US NEXRAD / HRRR (Obs, Nowcast, Forecast)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD / HRRR (实况・外推・预报)"
                        AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD / HRRR (實況・外推・預報)"
                        AppLanguage.SPANISH -> "EE. UU. NEXRAD / HRRR (Obs., Nowcast, Prev.)"
                        AppLanguage.FRENCH -> "É.-U. NEXRAD / HRRR (Obs., Nowcast, Prév.)"
                        AppLanguage.PORTUGUESE -> "EUA NEXRAD / HRRR (Obs., Nowcast, Prev.)"
                        AppLanguage.RUSSIAN -> "США NEXRAD / HRRR (Набл., Наукаст, Прогноз)"
                    },
                    RadarSourceMode.JMA to when (language) {
                        AppLanguage.JAPANESE -> "気象庁 高解像度降水ナウキャスト"
                        AppLanguage.KOREAN -> "일본 기상청 고해상도 강수 나우캐스트"
                        AppLanguage.ENGLISH -> "JMA High-Resolution Precipitation Nowcast"
                        AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 高分辨率降雨即时预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 高解析度降雨即時預報"
                        AppLanguage.SPANISH -> "JMA Nowcast de precipitación de alta resolución"
                        AppLanguage.FRENCH -> "JMA Nowcast des précipitations haute résolution"
                        AppLanguage.PORTUGUESE -> "JMA Nowcast de precipitação de alta resolução"
                        AppLanguage.RUSSIAN -> "Высокоточный наукаст осадков JMA"
                    },
                    RadarSourceMode.RAIN_VIEWER to when (language) {
                        AppLanguage.JAPANESE -> "世界レーダー (RainViewer)"
                        AppLanguage.KOREAN -> "글로벌 레이더 (RainViewer)"
                        AppLanguage.ENGLISH -> "Global Radar (RainViewer)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "全球雷达 (RainViewer)"
                        AppLanguage.TRADITIONAL_CHINESE -> "全球雷達 (RainViewer)"
                        AppLanguage.SPANISH -> "Radar global (RainViewer)"
                        AppLanguage.FRENCH -> "Radar mondial (RainViewer)"
                        AppLanguage.PORTUGUESE -> "Radar global (RainViewer)"
                        AppLanguage.RUSSIAN -> "Глобальный радар (RainViewer)"
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
                    AppLanguage.KOREAN -> "취소"
                    AppLanguage.ENGLISH -> "Cancel"
                    AppLanguage.SIMPLIFIED_CHINESE -> "取消"
                    AppLanguage.TRADITIONAL_CHINESE -> "取消"
                    AppLanguage.SPANISH -> "Cancelar"
                    AppLanguage.FRENCH -> "Annuler"
                    AppLanguage.PORTUGUESE -> "Cancelar"
                    AppLanguage.RUSSIAN -> "Отмена"
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
                            AppLanguage.KOREAN -> "닫기"
                            AppLanguage.ENGLISH -> "Close"
                            AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                            AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                            AppLanguage.SPANISH -> "Cerrar"
                            AppLanguage.FRENCH -> "Fermer"
                            AppLanguage.PORTUGUESE -> "Fechar"
                            AppLanguage.RUSSIAN -> "Закрыть"
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
                        AppLanguage.KOREAN -> "중심 기압"
                        AppLanguage.ENGLISH -> "Pressure"
                        AppLanguage.SIMPLIFIED_CHINESE -> "中心气压"
                        AppLanguage.TRADITIONAL_CHINESE -> "中心氣壓"
                        AppLanguage.SPANISH -> "Presión"
                        AppLanguage.FRENCH -> "Pression"
                        AppLanguage.PORTUGUESE -> "Pressão"
                        AppLanguage.RUSSIAN -> "Давление"
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
                        AppLanguage.KOREAN -> "이동 방향·속도"
                        AppLanguage.ENGLISH -> "Movement"
                        AppLanguage.SIMPLIFIED_CHINESE -> "移向・移速"
                        AppLanguage.TRADITIONAL_CHINESE -> "移向・移速"
                        AppLanguage.SPANISH -> "Movimiento"
                        AppLanguage.FRENCH -> "Déplacement"
                        AppLanguage.PORTUGUESE -> "Deslocamento"
                        AppLanguage.RUSSIAN -> "Движение"
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
                        AppLanguage.KOREAN -> "최대 풍속"
                        AppLanguage.ENGLISH -> "Max Wind"
                        AppLanguage.SIMPLIFIED_CHINESE -> "最大风速"
                        AppLanguage.TRADITIONAL_CHINESE -> "最大風速"
                        AppLanguage.SPANISH -> "Viento máx."
                        AppLanguage.FRENCH -> "Vent max."
                        AppLanguage.PORTUGUESE -> "Vento máx."
                        AppLanguage.RUSSIAN -> "Макс. ветер"
                    },
                    value = analysis.maxWindMs?.let { "$it m/s" } ?: "---",
                    subtitle = analysis.maxWindMs?.let {
                        val kmh = (it * 3.6).toInt()
                        when (language) {
                            AppLanguage.JAPANESE -> "約 $kmh km/h"
                            AppLanguage.KOREAN -> "약 $kmh km/h"
                            AppLanguage.ENGLISH -> "~$kmh km/h"
                            AppLanguage.SIMPLIFIED_CHINESE -> "约 $kmh km/h"
                            AppLanguage.TRADITIONAL_CHINESE -> "約 $kmh km/h"
                            AppLanguage.SPANISH -> "~$kmh km/h"
                            AppLanguage.FRENCH -> "~$kmh km/h"
                            AppLanguage.PORTUGUESE -> "~$kmh km/h"
                            AppLanguage.RUSSIAN -> "~$kmh км/ч"
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
                        AppLanguage.KOREAN -> "최대 순간 풍속"
                        AppLanguage.ENGLISH -> "Peak Gust"
                        AppLanguage.SIMPLIFIED_CHINESE -> "最大阵风"
                        AppLanguage.TRADITIONAL_CHINESE -> "最大陣風"
                        AppLanguage.SPANISH -> "Racha máx."
                        AppLanguage.FRENCH -> "Rafale max."
                        AppLanguage.PORTUGUESE -> "Rajada máx."
                        AppLanguage.RUSSIAN -> "Макс. порыв"
                    },
                    value = analysis.gustWindMs?.let { "$it m/s" } ?: "---",
                    subtitle = analysis.gustWindMs?.let {
                        val kmh = (it * 3.6).toInt()
                        when (language) {
                            AppLanguage.JAPANESE -> "約 $kmh km/h"
                            AppLanguage.KOREAN -> "약 $kmh km/h"
                            AppLanguage.ENGLISH -> "~$kmh km/h"
                            AppLanguage.SIMPLIFIED_CHINESE -> "约 $kmh km/h"
                            AppLanguage.TRADITIONAL_CHINESE -> "約 $kmh km/h"
                            AppLanguage.SPANISH -> "~$kmh km/h"
                            AppLanguage.FRENCH -> "~$kmh km/h"
                            AppLanguage.PORTUGUESE -> "~$kmh km/h"
                            AppLanguage.RUSSIAN -> "~$kmh км/ч"
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
                            AppLanguage.KOREAN -> "폭풍역 (25m/s 이상): "
                            AppLanguage.ENGLISH -> "Storm (≥25m/s): "
                            AppLanguage.SIMPLIFIED_CHINESE -> "暴风圈 (≥25m/s): "
                            AppLanguage.TRADITIONAL_CHINESE -> "暴風圈 (≥25m/s): "
                            AppLanguage.SPANISH -> "Tormenta (≥25 m/s): "
                            AppLanguage.FRENCH -> "Tempête (≥25 m/s) : "
                            AppLanguage.PORTUGUESE -> "Tempestade (≥25 m/s): "
                            AppLanguage.RUSSIAN -> "Шторм (≥25 м/с): "
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analysis.stormRadiusKm?.let {
                            when (language) {
                                AppLanguage.JAPANESE -> "半径 ${it.toInt()} km"
                                AppLanguage.KOREAN -> "반경 ${it.toInt()} km"
                                AppLanguage.ENGLISH -> "Radius ${it.toInt()} km"
                                AppLanguage.SIMPLIFIED_CHINESE -> "半径 ${it.toInt()} km"
                                AppLanguage.TRADITIONAL_CHINESE -> "半徑 ${it.toInt()} km"
                                AppLanguage.SPANISH -> "Radio ${it.toInt()} km"
                                AppLanguage.FRENCH -> "Rayon ${it.toInt()} km"
                                AppLanguage.PORTUGUESE -> "Raio ${it.toInt()} km"
                                AppLanguage.RUSSIAN -> "Радиус ${it.toInt()} км"
                            }
                        } ?: when (language) {
                            AppLanguage.JAPANESE -> "なし"
                            AppLanguage.KOREAN -> "없음"
                            AppLanguage.ENGLISH -> "None"
                            AppLanguage.SIMPLIFIED_CHINESE -> "无"
                            AppLanguage.TRADITIONAL_CHINESE -> "無"
                            AppLanguage.SPANISH -> "Ninguno"
                            AppLanguage.FRENCH -> "Aucun"
                            AppLanguage.PORTUGUESE -> "Nenhum"
                            AppLanguage.RUSSIAN -> "Нет"
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
                            AppLanguage.KOREAN -> "강풍역 (15m/s 이상): "
                            AppLanguage.ENGLISH -> "Gale (≥15m/s): "
                            AppLanguage.SIMPLIFIED_CHINESE -> "强风圈 (≥15m/s): "
                            AppLanguage.TRADITIONAL_CHINESE -> "強風圈 (≥15m/s): "
                            AppLanguage.SPANISH -> "Vendaval (≥15 m/s): "
                            AppLanguage.FRENCH -> "Coup de vent (≥15 m/s) : "
                            AppLanguage.PORTUGUESE -> "Ventania (≥15 m/s): "
                            AppLanguage.RUSSIAN -> "Сильный ветер (≥15 м/с): "
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analysis.galeRadiusKm?.let {
                            when (language) {
                                AppLanguage.JAPANESE -> "半径 ${it.toInt()} km"
                                AppLanguage.KOREAN -> "반경 ${it.toInt()} km"
                                AppLanguage.ENGLISH -> "Radius ${it.toInt()} km"
                                AppLanguage.SIMPLIFIED_CHINESE -> "半径 ${it.toInt()} km"
                                AppLanguage.TRADITIONAL_CHINESE -> "半徑 ${it.toInt()} km"
                                AppLanguage.SPANISH -> "Radio ${it.toInt()} km"
                                AppLanguage.FRENCH -> "Rayon ${it.toInt()} km"
                                AppLanguage.PORTUGUESE -> "Raio ${it.toInt()} km"
                                AppLanguage.RUSSIAN -> "Радиус ${it.toInt()} км"
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
                    AppLanguage.KOREAN -> "일본 기상청"
                    AppLanguage.ENGLISH -> "JMA"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象厅"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象廳"
                    AppLanguage.SPANISH -> "JMA"
                    AppLanguage.FRENCH -> "JMA"
                    AppLanguage.PORTUGUESE -> "JMA"
                    AppLanguage.RUSSIAN -> "JMA"
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
