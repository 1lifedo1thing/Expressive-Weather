package io.github.nemynew.expressiveweather.data

/**
 * 気象庁（JMA）地図レイヤーのカテゴリ定義
 */
enum class JmaLayerCategory(
    val id: String,
    val displayName: String,
    val emoji: String,
    val description: String
) {
    NOWCAST("nowcast", "雨雲・雷・竜巻", "🌧️", "過去の実績と直近の短時間予測"),
    RAINFALL("rainfall", "今後の雨", "⏱️", "1〜24時間の積算降水量予測"),
    RISK("risk", "キキクル (危険度)", "🚨", "土砂・大雨・浸水・洪水の危険度分布"),
    HIMAWARI("himawari", "気象衛星ひまわり", "🛰️", "リアルタイム静止気象衛星雲画像"),
    SNOW("snow", "雪・降雪量", "❄️", "現在の積雪深と今後の降雪量"),
    WEATHER_DIST("weather_dist", "天気・気温予報", "🌡️", "天気・予想気温分布"),
    MARINE("marine", "海上分布予報", "🌊", "近海・沿岸の波高と海上風速"),
    BASE_MAP("base_map", "背景地図", "🗺️", "地理院地図・気象庁標準・ハザード"),
    TYPHOON("typhoon", "台風情報", "🌀", "台風の進路予想・暴風域・強風域");

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> displayName
        AppLanguage.KOREAN -> when (this) {
            NOWCAST -> "비구름·낙뢰·토네이도"
            RAINFALL -> "예상 강수량"
            RISK -> "키키쿠루 (위험도)"
            HIMAWARI -> "히마와리 기상위성"
            SNOW -> "눈·적설량"
            WEATHER_DIST -> "날씨·기온 분포 예보"
            MARINE -> "해상 분포 예보"
            BASE_MAP -> "배경 지도"
            TYPHOON -> "태풍 정보"
        }
        AppLanguage.ENGLISH -> when (this) {
            NOWCAST -> "Rain, Lightning & Tornado"
            RAINFALL -> "Expected Rainfall"
            RISK -> "Kikikuru (Risk Levels)"
            HIMAWARI -> "Himawari Satellite"
            SNOW -> "Snow & Snowfall"
            WEATHER_DIST -> "Weather & Temperature"
            MARINE -> "Marine Forecast"
            BASE_MAP -> "Base Maps"
            TYPHOON -> "Typhoon Track & Warnings"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            NOWCAST -> "降水、雷电与龙卷风"
            RAINFALL -> "未来降水"
            RISK -> "Kikikuru (灾害风险)"
            HIMAWARI -> "向日葵气象卫星"
            SNOW -> "积雪与降雪量"
            WEATHER_DIST -> "天气与气温预报"
            MARINE -> "海洋分布预报"
            BASE_MAP -> "背景地图"
            TYPHOON -> "台风路径与预警"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            NOWCAST -> "降水、雷電與龍捲風"
            RAINFALL -> "未來降水"
            RISK -> "Kikikuru (災害風險)"
            HIMAWARI -> "向日葵氣象衛星"
            SNOW -> "積雪與降雪量"
            WEATHER_DIST -> "天氣與氣溫預報"
            MARINE -> "海洋分布預報"
            BASE_MAP -> "背景地圖"
            TYPHOON -> "颱風路徑與預警"
        }
        AppLanguage.SPANISH -> when (this) {
            NOWCAST -> "Lluvia, rayos y tornados"
            RAINFALL -> "Lluvia prevista"
            RISK -> "Kikikuru (Riesgo)"
            HIMAWARI -> "Satélite Himawari"
            SNOW -> "Nieve y nevadas"
            WEATHER_DIST -> "Tiempo y temperatura"
            MARINE -> "Pronóstico marítimo"
            BASE_MAP -> "Mapas base"
            TYPHOON -> "Información de tifones"
        }
        AppLanguage.FRENCH -> when (this) {
            NOWCAST -> "Pluie, foudre et tornades"
            RAINFALL -> "Pluie prévue"
            RISK -> "Kikikuru (Risques)"
            HIMAWARI -> "Satellite Himawari"
            SNOW -> "Neige et chutes"
            WEATHER_DIST -> "Météo et températures"
            MARINE -> "Prévisions marines"
            BASE_MAP -> "Fonds de carte"
            TYPHOON -> "Infos typhons"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            NOWCAST -> "Chuva, raios e tornados"
            RAINFALL -> "Chuva prevista"
            RISK -> "Kikikuru (Riscos)"
            HIMAWARI -> "Satélite Himawari"
            SNOW -> "Neve e precipitação"
            WEATHER_DIST -> "Tempo e temperatura"
            MARINE -> "Previsão marítima"
            BASE_MAP -> "Mapas base"
            TYPHOON -> "Informações de tufões"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> description
        AppLanguage.KOREAN -> when (this) {
            NOWCAST -> "지난 실황과 초단기 예측"
            RAINFALL -> "1~24시간 누적 강수량 예측"
            RISK -> "산사태·호우·침수·홍수 위험도 분포"
            HIMAWARI -> "실시간 정지궤도 기상위성 구름 영상"
            SNOW -> "현재 적설 깊이와 예상 적설량"
            WEATHER_DIST -> "지역별 날씨 및 예상 기온 분포"
            MARINE -> "연안·근해 파고 및 해상 풍속 예보"
            BASE_MAP -> "지리원 담색 지도·기상청 표준 지도·재해 위험도"
            TYPHOON -> "일본 기상청 공식 태풍 진로 예보·폭풍반경·강풍반경"
        }
        AppLanguage.ENGLISH -> when (this) {
            NOWCAST -> "Past observations & short-term nowcast"
            RAINFALL -> "1 to 24-hour cumulative precipitation forecast"
            RISK -> "Landslide, heavy rain, inundation & flood risks"
            HIMAWARI -> "Real-time geostationary meteorological satellite imagery"
            SNOW -> "Current snow depth & forecast snowfall accumulation"
            WEATHER_DIST -> "Regional weather & forecast temperature distributions"
            MARINE -> "Coastal wave height & sea surface wind forecasts"
            BASE_MAP -> "GSI pale map, JMA standard basemap & hazard overlays"
            TYPHOON -> "Official JMA typhoon track forecast, gale and storm areas"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            NOWCAST -> "过去实况与近期短临降水预测"
            RAINFALL -> "1至24小时预计累计降水量分布"
            RISK -> "泥石流、大雨、积水淹没与洪水灾害风险"
            HIMAWARI -> "实时静止气象卫星高清云图"
            SNOW -> "当前积雪深度与未来降雪量预测"
            WEATHER_DIST -> "区域天气与预测气温分布"
            MARINE -> "沿岸及近海海浪与风速预测"
            BASE_MAP -> "地理院浅色地图、气象厅标准底图与防灾风险图"
            TYPHOON -> "日本气象厅官方台风路径预测、强风圈与暴风圈"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            NOWCAST -> "過去實況與近期短臨降水預測"
            RAINFALL -> "1至24小時預計累計降水量分布"
            RISK -> "土石流、大雨、積水淹沒與洪水災害風險"
            HIMAWARI -> "即時靜止氣象衛星高畫質雲圖"
            SNOW -> "當前積雪深度與未來降雪量預測"
            WEATHER_DIST -> "區域天氣與預測氣溫分布"
            MARINE -> "沿岸及近海海浪與風速預測"
            BASE_MAP -> "地理院淺色地圖、氣象廳標準底圖與防災風險圖"
            TYPHOON -> "日本氣象廳官方颱風路徑預測、強風圈與暴風圈"
        }
        AppLanguage.SPANISH -> when (this) {
            NOWCAST -> "Observaciones recientes y pronóstico a muy corto plazo"
            RAINFALL -> "Previsión de precipitación acumulada de 1 a 24 horas"
            RISK -> "Riesgo de deslizamientos, lluvias intensas e inundaciones"
            HIMAWARI -> "Imágenes en tiempo real del satélite geoestacionario"
            SNOW -> "Espesor actual de nieve y acumulación prevista"
            WEATHER_DIST -> "Distribución regional del tiempo y temperaturas previstas"
            MARINE -> "Previsión de altura del oleaje y viento en superficie marina"
            BASE_MAP -> "Mapa claro GSI, mapa estándar JMA y capas de riesgo"
            TYPHOON -> "Trayectoria oficial JMA, zonas de vendaval y tormenta"
        }
        AppLanguage.FRENCH -> when (this) {
            NOWCAST -> "Observations passées et prévision immédiate"
            RAINFALL -> "Prévision des cumuls de pluie de 1 à 24 heures"
            RISK -> "Risques de glissements de terrain, fortes pluies et crues"
            HIMAWARI -> "Imagerie en temps réel du satellite géostationnaire"
            SNOW -> "Hauteur de neige actuelle et chutes prévues"
            WEATHER_DIST -> "Répartition régionale du temps et des températures prévues"
            MARINE -> "Prévisions de hauteur des vagues et de vent en mer"
            BASE_MAP -> "Carte claire GSI, fond standard JMA et zones à risques"
            TYPHOON -> "Trajectoire officielle JMA, zones de coup de vent et tempête"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            NOWCAST -> "Observações recentes e previsão imediata"
            RAINFALL -> "Previsão de precipitação acumulada de 1 a 24 horas"
            RISK -> "Riscos de deslizamentos, chuvas fortes e inundações"
            HIMAWARI -> "Imagens em tempo real do satélite geoestacionário"
            SNOW -> "Espessura atual de neve e acúmulo previsto"
            WEATHER_DIST -> "Distribuição regional do tempo e temperaturas previstas"
            MARINE -> "Previsão de altura das ondas e vento marítimo"
            BASE_MAP -> "Mapa claro GSI, mapa padrão JMA e camadas de risco"
            TYPHOON -> "Trajetória oficial JMA, áreas de ventania e tempestade"
        }
    }
}

/**
 * 気象庁サーバーから取得可能な気象地図レイヤー一覧
 */
enum class JmaLayerType(
    val id: String,
    val category: JmaLayerCategory,
    val displayName: String,
    val shortName: String,
    val description: String,
    val targetTimesUrls: List<String>,
    val elementKey: String,
    val elementKeys: List<String> = listOf(elementKey),
    val additionalPathPatterns: List<String> = emptyList(),
    val lv1: String = "jmatile",
    val lv2: String = "nowc",
    val member: String = "none",
    val pathPattern: String = "surf/hrpns",
    val ext: String = "png",
    val minZoom: Int = 3,
    val maxZoom: Int = 10,
    val isStatic: Boolean = false,
    val staticUrl: String? = null
) {
    // ==========================================
    // 1. 雨雲・雷・竜巻（ナウキャスト群）
    // ==========================================
    TYPHOON(
        id = "typhoon",
        category = JmaLayerCategory.TYPHOON,
        displayName = "台風情報 (進路予想・暴風域)",
        shortName = "台風情報",
        description = "台風の中心位置・進行方向・移動速度・強風域・暴風域・進路予報円",
        targetTimesUrls = emptyList(),
        elementKey = "typhoon",
        minZoom = 3,
        maxZoom = 9
    ),
    HRPNS(
        id = "hrpns",
        category = JmaLayerCategory.NOWCAST,
        displayName = "雨雲の動き (ナウキャスト)",
        shortName = "雨雲ナウキャスト",
        description = "過去1時間実績＋今後1時間の高解像度降水予測",
        targetTimesUrls = listOf(
            "https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N1.json",
            "https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N2.json"
        ),
        elementKey = "hrpns",
        lv2 = "nowc",
        pathPattern = "surf/hrpns"
    ),
    THNS(
        id = "thns",
        category = JmaLayerCategory.NOWCAST,
        displayName = "雷活動度 (雷ナウキャスト)",
        shortName = "雷ナウキャスト",
        description = "激しい雷雨の兆候・発雷確率（活動度1〜4）",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N3.json"),
        elementKey = "thns",
        lv2 = "nowc",
        pathPattern = "surf/thns",
        maxZoom = 9
    ),
    TRNS(
        id = "trns",
        category = JmaLayerCategory.NOWCAST,
        displayName = "竜巻発生確度ナウキャスト",
        shortName = "竜巻発生確度",
        description = "竜巻や激しい突風の発生危険度（確度1・2）",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N3.json"),
        elementKey = "trns",
        lv2 = "nowc",
        pathPattern = "surf/trns"
    ),

    // ==========================================
    // 2. 今後の雨（降水量予測）
    // ==========================================
    RASRF_1H(
        id = "rasrf_1h",
        category = JmaLayerCategory.RAINFALL,
        displayName = "今後の雨 (1時間降水量)",
        shortName = "1時間予想雨量",
        description = "今後の1時間予想降水量分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/rasrf/targetTimes.json"),
        elementKey = "rasrf",
        lv2 = "rasrf",
        pathPattern = "surf/rasrf"
    ),
    RASRF_3H(
        id = "rasrf_3h",
        category = JmaLayerCategory.RAINFALL,
        displayName = "今後の雨 (3時間降水量)",
        shortName = "3時間予想雨量",
        description = "今後の3時間予想降水量分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/rasrf/targetTimes.json"),
        elementKey = "rasrf03h",
        lv2 = "rasrf",
        pathPattern = "surf/rasrf03h"
    ),
    RASRF_24H(
        id = "rasrf_24h",
        category = JmaLayerCategory.RAINFALL,
        displayName = "今後の雨 (24時間降水量)",
        shortName = "24時間予想雨量",
        description = "今後の24時間積算予想降水量分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/rasrf/targetTimes.json"),
        elementKey = "rasrf24h",
        lv2 = "rasrf",
        pathPattern = "surf/rasrf24h"
    ),

    // ==========================================
    // 3. キキクル（危険度分布）
    // ==========================================
    LANDSLIDE(
        id = "landslide",
        category = JmaLayerCategory.RISK,
        displayName = "土砂キキクル (土砂災害危険度)",
        shortName = "土砂キキクル",
        description = "大雨による土砂災害の危険度（注意/警戒/危険/極めて危険）",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/risk/targetTimes.json"),
        elementKey = "land",
        lv2 = "risk",
        pathPattern = "surf/land"
    ),
    HEAVY_RAIN(
        id = "heavyrain",
        category = JmaLayerCategory.RISK,
        displayName = "大雨キキクル (浸水・洪水の重ね表示)",
        shortName = "大雨キキクル",
        description = "浸水キキクルと洪水キキクルを重ねて表示する大雨危険度分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/risk/targetTimes.json"),
        elementKey = "inund",
        elementKeys = listOf("inund", "flood"),
        additionalPathPatterns = listOf("surf/flood"),
        lv2 = "risk",
        pathPattern = "surf/inund"
    ),
    INUNDATION(
        id = "inundation",
        category = JmaLayerCategory.RISK,
        displayName = "浸水キキクル (浸水害危険度)",
        shortName = "浸水キキクル",
        description = "短時間強雨による道路・家屋等の浸水害危険度",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/risk/targetTimes.json"),
        elementKey = "inund",
        lv2 = "risk",
        pathPattern = "surf/inund"
    ),
    FLOOD(
        id = "flood",
        category = JmaLayerCategory.RISK,
        displayName = "洪水キキクル (洪水危険度)",
        shortName = "洪水キキクル",
        description = "河川の増水・氾濫の発生危険度分布（指定河川洪水予報・中小河川）",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/risk/targetTimes.json"),
        elementKey = "flood",
        lv2 = "risk",
        pathPattern = "surf/flood"
    ),

    // ==========================================
    // 4. 気象衛星ひまわり（リアルタイム衛星画像）
    // ==========================================
    HIMAWARI_TRUECOLOR(
        id = "himawari_truecolor",
        category = JmaLayerCategory.HIMAWARI,
        displayName = "ひまわり (トゥルーカラー再現画像)",
        shortName = "ひまわりカラー",
        description = "宇宙から肉眼で見た地球に近い高画質衛星雲画像",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/himawari/data/satimg/targetTimes_jp.json"),
        elementKey = "REP/ETC",
        lv1 = "himawari",
        lv2 = "satimg",
        member = "jp",
        pathPattern = "REP/ETC",
        ext = "jpg",
        maxZoom = 6
    ),
    HIMAWARI_INFRARED(
        id = "himawari_infrared",
        category = JmaLayerCategory.HIMAWARI,
        displayName = "ひまわり (赤外画像・夜間対応)",
        shortName = "ひまわり赤外",
        description = "雲の温度と高さを可視化（夜間も24時間観測可能）",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/himawari/data/satimg/targetTimes_jp.json"),
        elementKey = "B13/TBB",
        lv1 = "himawari",
        lv2 = "satimg",
        member = "jp",
        pathPattern = "B13/TBB",
        ext = "jpg",
        maxZoom = 6
    ),
    HIMAWARI_STEAM(
        id = "himawari_steam",
        category = JmaLayerCategory.HIMAWARI,
        displayName = "ひまわり (水蒸気画像)",
        shortName = "ひまわり水蒸気",
        description = "中・上層大気の湿り気とジェット気流の流れを可視化",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/himawari/data/satimg/targetTimes_jp.json"),
        elementKey = "B08/TBB",
        lv1 = "himawari",
        lv2 = "satimg",
        member = "jp",
        pathPattern = "B08/TBB",
        ext = "jpg",
        maxZoom = 6
    ),
    HIMAWARI_CLOUDTOP(
        id = "himawari_cloudtop",
        category = JmaLayerCategory.HIMAWARI,
        displayName = "ひまわり (雲頂強調画像)",
        shortName = "ひまわり雲頂強調",
        description = "局地的大雨をもたらす発達した積乱雲の雲頂を色分け強調",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/himawari/data/satimg/targetTimes_jp.json"),
        elementKey = "SND/ETC",
        lv1 = "himawari",
        lv2 = "satimg",
        member = "jp",
        pathPattern = "SND/ETC",
        ext = "jpg",
        maxZoom = 6
    ),

    // ==========================================
    // 5. 雪・降雪量（雪情報）
    // ==========================================
    SNOW_DEPTH(
        id = "snow_depth",
        category = JmaLayerCategory.SNOW,
        displayName = "積雪の深さ (現在の積雪深)",
        shortName = "積雪の深さ",
        description = "全国のリアルタイム積雪深分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/snow/targetTimes.json"),
        elementKey = "snowd",
        lv2 = "snow",
        pathPattern = "surf/snowd",
        maxZoom = 11
    ),
    SNOWFALL_3H(
        id = "snowfall_3h",
        category = JmaLayerCategory.SNOW,
        displayName = "3時間降雪量",
        shortName = "3時間降雪量",
        description = "直近3時間の積算降雪量分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/snow/targetTimes.json"),
        elementKey = "snowf03h",
        lv2 = "snow",
        pathPattern = "surf/snowf03h",
        maxZoom = 11
    ),
    SNOWFALL_24H(
        id = "snowfall_24h",
        category = JmaLayerCategory.SNOW,
        displayName = "24時間降雪量",
        shortName = "24時間降雪量",
        description = "直近24時間の積算降雪量分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/snow/targetTimes.json"),
        elementKey = "snowf24h",
        lv2 = "snow",
        pathPattern = "surf/snowf24h",
        maxZoom = 11
    ),

    // ==========================================
    // 6. 天気・気温予報分布
    // ==========================================
    WEATHER_DIST(
        id = "weather_dist",
        category = JmaLayerCategory.WEATHER_DIST,
        displayName = "天気分布予報",
        shortName = "天気分布予報",
        description = "晴れ・曇り・雨・雪の地域予報",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/wdist/targetTimes.json"),
        elementKey = "wm",
        lv2 = "wdist",
        pathPattern = "surf/wm"
    ),
    TEMP_DIST(
        id = "temp_dist",
        category = JmaLayerCategory.WEATHER_DIST,
        displayName = "気温分布予報",
        shortName = "気温分布予報",
        description = "時間帯ごとの全国予想気温分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/wdist/targetTimes.json"),
        elementKey = "temp",
        lv2 = "wdist",
        pathPattern = "surf/temp"
    ),
    MAX_TEMP(
        id = "max_temp",
        category = JmaLayerCategory.WEATHER_DIST,
        displayName = "日中の最高気温予報",
        shortName = "最高気温予報",
        description = "本日の日中最高気温予報",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/wdist/targetTimes.json"),
        elementKey = "max_temp",
        lv2 = "wdist",
        pathPattern = "surf/max_temp"
    ),
    MIN_TEMP(
        id = "min_temp",
        category = JmaLayerCategory.WEATHER_DIST,
        displayName = "朝の最低気温予報",
        shortName = "最低気温予報",
        description = "明朝の最低気温予報",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/wdist/targetTimes.json"),
        elementKey = "min_temp",
        lv2 = "wdist",
        pathPattern = "surf/min_temp"
    ),

    // ==========================================
    // 7. 海上分布予報
    // ==========================================
    WAVE_HEIGHT(
        id = "wave_height",
        category = JmaLayerCategory.MARINE,
        displayName = "海上波高分布予報",
        shortName = "波の高さ",
        description = "沿岸・近海の波高予想分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/umimesh/targetTimes.json"),
        elementKey = "wavh",
        lv2 = "umimesh",
        pathPattern = "surf/wavh",
        maxZoom = 8
    ),
    WIND_SPEED(
        id = "wind_speed",
        category = JmaLayerCategory.MARINE,
        displayName = "海上風速分布予報",
        shortName = "海上風速",
        description = "沿岸・近海の風速予想分布",
        targetTimesUrls = listOf("https://www.jma.go.jp/bosai/jmatile/data/umimesh/targetTimes.json"),
        elementKey = "ws",
        lv2 = "umimesh",
        pathPattern = "surf/ws",
        maxZoom = 8
    ),

    // ==========================================
    // 8. 背景地図（ベースマップ切替）
    // ==========================================
    GSI_PALE(
        id = "gsi_pale",
        category = JmaLayerCategory.BASE_MAP,
        displayName = "国土地理院 淡色地図",
        shortName = "地理院淡色地図",
        description = "道路や地名が分かりやすい標準淡色マップ",
        targetTimesUrls = emptyList(),
        elementKey = "gsi_pale",
        isStatic = true,
        staticUrl = "https://www.jma.go.jp/tile/gsi/pale/{z}/{x}/{y}.png",
        maxZoom = 14
    ),
    JMA_BASE(
        id = "jma_base",
        category = JmaLayerCategory.BASE_MAP,
        displayName = "気象庁標準地図",
        shortName = "気象庁標準地図",
        description = "気象データを引き立てる公式標準マップ",
        targetTimesUrls = emptyList(),
        elementKey = "jma_base",
        isStatic = true,
        staticUrl = "https://www.jma.go.jp/tile/jma/base/{z}/{x}/{y}.png",
        maxZoom = 11
    ),
    FLOOD_HAZARD(
        id = "flood_hazard",
        category = JmaLayerCategory.BASE_MAP,
        displayName = "洪水浸水想定区域 (国交省ハザード)",
        shortName = "洪水浸水区域",
        description = "河川氾濫時の想定最大浸水深レイヤー",
        targetTimesUrls = emptyList(),
        elementKey = "flood_hazard",
        isStatic = true,
        staticUrl = "https://disaportaldata.gsi.go.jp/raster/01_flood_l2_shinsuishin/{z}/{x}/{y}.png",
        maxZoom = 12
    );

    /**
     * タイルURLテンプレートを生成する
     */
    fun buildTileUrl(
        basetime: String,
        validtime: String,
        memberOverride: String? = null,
        pathPatternOverride: String? = null
    ): String {
        if (isStatic && staticUrl != null) {
            return staticUrl
        }
        val actualMember = if (!memberOverride.isNullOrBlank()) memberOverride else member
        val actualPathPattern = pathPatternOverride ?: pathPattern
        return "https://www.jma.go.jp/bosai/$lv1/data/$lv2/$basetime/$actualMember/$validtime/$actualPathPattern/{z}/{x}/{y}.$ext"
    }

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> displayName
        AppLanguage.KOREAN -> when (this) {
            HRPNS -> "비구름 이동 (강수 나우캐스트)"
            THNS -> "낙뢰 활동도 (낙뢰 나우캐스트)"
            TRNS -> "토네이도 발생 확률 나우캐스트"
            RASRF_1H -> "예상 강수량 (1시간)"
            RASRF_3H -> "예상 강수량 (3시간)"
            RASRF_24H -> "예상 강수량 (24시간)"
            LANDSLIDE -> "산사태 키키쿠루 (산사태 위험도)"
            HEAVY_RAIN -> "호우 키키쿠루 (침수·홍수 통합)"
            INUNDATION -> "침수 키키쿠루 (침수 위험도)"
            FLOOD -> "홍수 키키쿠루 (하천 홍수 위험도)"
            HIMAWARI_TRUECOLOR -> "히마와리 (천연색 위성영상)"
            HIMAWARI_INFRARED -> "히마와리 (적외 영상·야간 대응)"
            HIMAWARI_STEAM -> "히마와리 (수증기 영상)"
            HIMAWARI_CLOUDTOP -> "히마와리 (운정 강조 영상)"
            SNOW_DEPTH -> "적설 깊이 (현재 적설심)"
            SNOWFALL_3H -> "3시간 신적설량"
            SNOWFALL_24H -> "24시간 신적설량"
            WEATHER_DIST -> "날씨 분포 예보"
            TEMP_DIST -> "기온 분포 예보"
            MAX_TEMP -> "낮 최고기온 예보"
            MIN_TEMP -> "아침 최저기온 예보"
            WAVE_HEIGHT -> "해상 파고 분포 예보"
            WIND_SPEED -> "해상 풍속 분포 예보"
            GSI_PALE -> "국토지리원 담색 지도"
            JMA_BASE -> "일본 기상청 표준 지도"
            FLOOD_HAZARD -> "홍수 침수 예상 구역 (재해 지도)"
            TYPHOON -> "태풍 정보 (진로 예보·폭풍반경)"
        }
        AppLanguage.ENGLISH -> when (this) {
            HRPNS -> "Precipitation Nowcast (Rain Movement)"
            THNS -> "Lightning Activity (Thunder Nowcast)"
            TRNS -> "Tornado Probability Nowcast"
            RASRF_1H -> "Expected Rain (1-Hour Precipitation)"
            RASRF_3H -> "Expected Rain (3-Hour Precipitation)"
            RASRF_24H -> "Expected Rain (24-Hour Precipitation)"
            LANDSLIDE -> "Landslide Kikikuru (Hazard Risk)"
            HEAVY_RAIN -> "Heavy Rain Kikikuru (Flood & Inundation)"
            INUNDATION -> "Inundation Kikikuru (Flooding Risk)"
            FLOOD -> "Flood Kikikuru (River Flood Risk)"
            HIMAWARI_TRUECOLOR -> "Himawari (True Color Reproduction)"
            HIMAWARI_INFRARED -> "Himawari (Infrared / Day & Night)"
            HIMAWARI_STEAM -> "Himawari (Water Vapor)"
            HIMAWARI_CLOUDTOP -> "Himawari (Cloud Top Enhanced)"
            SNOW_DEPTH -> "Snow Depth (Current)"
            SNOWFALL_3H -> "3-Hour Snowfall"
            SNOWFALL_24H -> "24-Hour Snowfall"
            WEATHER_DIST -> "Weather Forecast Distribution"
            TEMP_DIST -> "Temperature Forecast Distribution"
            MAX_TEMP -> "Daytime High Temperature Forecast"
            MIN_TEMP -> "Morning Low Temperature Forecast"
            WAVE_HEIGHT -> "Marine Wave Height Forecast"
            WIND_SPEED -> "Marine Wind Speed Forecast"
            GSI_PALE -> "GSI Pale Map"
            JMA_BASE -> "JMA Standard Map"
            FLOOD_HAZARD -> "Flood Inundation Hazard Map (MLIT)"
            TYPHOON -> "Typhoon Information (Track & Warning Areas)"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            HRPNS -> "雨云动态 (临近预报)"
            THNS -> "雷电活动度 (雷电临近预报)"
            TRNS -> "龙卷风发生概率临近预报"
            RASRF_1H -> "未来降水 (1小时降水量)"
            RASRF_3H -> "未来降水 (3小时降水量)"
            RASRF_24H -> "未来降水 (24小时降水量)"
            LANDSLIDE -> "泥石流 Kikikuru (地质灾害危险度)"
            HEAVY_RAIN -> "大雨 Kikikuru (积水与洪水叠加)"
            INUNDATION -> "浸水 Kikikuru (积水内涝危险度)"
            FLOOD -> "洪水 Kikikuru (河流洪水危险度)"
            HIMAWARI_TRUECOLOR -> "向日葵 (真实色彩再现图)"
            HIMAWARI_INFRARED -> "向日葵 (红外云图・支持夜间)"
            HIMAWARI_STEAM -> "向日葵 (水汽图)"
            HIMAWARI_CLOUDTOP -> "向日葵 (云顶增强图)"
            SNOW_DEPTH -> "积雪深度 (当前实况)"
            SNOWFALL_3H -> "3小时降雪量"
            SNOWFALL_24H -> "24小时降雪量"
            WEATHER_DIST -> "天气分布预报"
            TEMP_DIST -> "气温分布预报"
            MAX_TEMP -> "日间最高气温预报"
            MIN_TEMP -> "晨间最低气温预报"
            WAVE_HEIGHT -> "海上浪高分布预报"
            WIND_SPEED -> "海上风速分布预报"
            GSI_PALE -> "国土地理院 浅色地图"
            JMA_BASE -> "气象厅标准底图"
            FLOOD_HAZARD -> "洪水淹没风险区 (国交省防灾图)"
            TYPHOON -> "台风情报 (路径预测・暴风圈)"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            HRPNS -> "雨雲動態 (臨近預報)"
            THNS -> "雷電活動度 (雷電臨近預報)"
            TRNS -> "龍捲風發生機率臨近預報"
            RASRF_1H -> "未來降水 (1小時降水量)"
            RASRF_3H -> "未來降水 (3小時降水量)"
            RASRF_24H -> "未來降水 (24小時降水量)"
            LANDSLIDE -> "土石流 Kikikuru (地質災害危險度)"
            HEAVY_RAIN -> "大雨 Kikikuru (淹水與洪水疊加)"
            INUNDATION -> "淹水 Kikikuru (積水內澇危險度)"
            FLOOD -> "洪水 Kikikuru (河流洪水危險度)"
            HIMAWARI_TRUECOLOR -> "向日葵 (真實色彩再現圖)"
            HIMAWARI_INFRARED -> "向日葵 (紅外線雲圖・支援夜間)"
            HIMAWARI_STEAM -> "向日葵 (水汽圖)"
            HIMAWARI_CLOUDTOP -> "向日葵 (雲頂增強圖)"
            SNOW_DEPTH -> "積雪深度 (當前實況)"
            SNOWFALL_3H -> "3小時降雪量"
            SNOWFALL_24H -> "24小時降雪量"
            WEATHER_DIST -> "天氣分布預報"
            TEMP_DIST -> "氣溫分布預報"
            MAX_TEMP -> "日間最高氣溫預報"
            MIN_TEMP -> "晨間最低氣溫預報"
            WAVE_HEIGHT -> "海上浪高分布預報"
            WIND_SPEED -> "海上風速分布預報"
            GSI_PALE -> "國土地理院 淺色地圖"
            JMA_BASE -> "氣象廳標準底圖"
            FLOOD_HAZARD -> "洪水淹沒風險區 (國交省防災圖)"
            TYPHOON -> "颱風情報 (路徑預測・暴風圈)"
        }
        AppLanguage.SPANISH -> when (this) {
            HRPNS -> "Radar de lluvia (Pronóstico inmediato)"
            THNS -> "Actividad eléctrica (Rayos)"
            TRNS -> "Probabilidad de tornados"
            RASRF_1H -> "Lluvia prevista (1 hora)"
            RASRF_3H -> "Lluvia prevista (3 horas)"
            RASRF_24H -> "Lluvia prevista (24 horas)"
            LANDSLIDE -> "Kikikuru Deslizamientos (Riesgo)"
            HEAVY_RAIN -> "Kikikuru Lluvia intensa (Inundación y ríos)"
            INUNDATION -> "Kikikuru Inundación urbana"
            FLOOD -> "Kikikuru Crecida fluvial"
            HIMAWARI_TRUECOLOR -> "Himawari (Color real)"
            HIMAWARI_INFRARED -> "Himawari (Infrarrojo / 24 h)"
            HIMAWARI_STEAM -> "Himawari (Vapor de agua)"
            HIMAWARI_CLOUDTOP -> "Himawari (Topes nubosos)"
            SNOW_DEPTH -> "Espesor de nieve (Actual)"
            SNOWFALL_3H -> "Nevada en 3 horas"
            SNOWFALL_24H -> "Nevada en 24 horas"
            WEATHER_DIST -> "Distribución del tiempo"
            TEMP_DIST -> "Distribución de temperatura"
            MAX_TEMP -> "Temperatura máxima diurna"
            MIN_TEMP -> "Temperatura mínima matinal"
            WAVE_HEIGHT -> "Previsión de altura de olas"
            WIND_SPEED -> "Previsión de viento marítimo"
            GSI_PALE -> "Mapa claro GSI"
            JMA_BASE -> "Mapa estándar JMA"
            FLOOD_HAZARD -> "Mapa de zonas inundables (MLIT)"
            TYPHOON -> "Información de tifones (Trayectoria y avisos)"
        }
        AppLanguage.FRENCH -> when (this) {
            HRPNS -> "Prévision pluie (Mouvement des averses)"
            THNS -> "Activité orageuse (Foudre)"
            TRNS -> "Probabilité de tornades"
            RASRF_1H -> "Pluie prévue (1 heure)"
            RASRF_3H -> "Pluie prévue (3 heures)"
            RASRF_24H -> "Pluie prévue (24 heures)"
            LANDSLIDE -> "Kikikuru Glissements de terrain"
            HEAVY_RAIN -> "Kikikuru Fortes pluies (Crues et ruissellement)"
            INUNDATION -> "Kikikuru Inondation urbaine"
            FLOOD -> "Kikikuru Crue fluviale"
            HIMAWARI_TRUECOLOR -> "Himawari (Couleurs réelles)"
            HIMAWARI_INFRARED -> "Himawari (Infrarouge / Jour et nuit)"
            HIMAWARI_STEAM -> "Himawari (Vapeur d'eau)"
            HIMAWARI_CLOUDTOP -> "Himawari (Sommets nuageux)"
            SNOW_DEPTH -> "Hauteur de neige (Actuelle)"
            SNOWFALL_3H -> "Chutes de neige (3 h)"
            SNOWFALL_24H -> "Chutes de neige (24 h)"
            WEATHER_DIST -> "Répartition météo prévue"
            TEMP_DIST -> "Répartition des températures"
            MAX_TEMP -> "Température maximale diurne"
            MIN_TEMP -> "Température minimale matinale"
            WAVE_HEIGHT -> "Prévision de hauteur des vagues"
            WIND_SPEED -> "Prévision du vent en mer"
            GSI_PALE -> "Carte claire GSI"
            JMA_BASE -> "Carte standard JMA"
            FLOOD_HAZARD -> "Zones inondables (MLIT)"
            TYPHOON -> "Infos typhons (Trajectoire et alertes)"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            HRPNS -> "Radar de chuva (Previsão imediata)"
            THNS -> "Atividade elétrica (Raios)"
            TRNS -> "Probabilidade de tornados"
            RASRF_1H -> "Chuva prevista (1 hora)"
            RASRF_3H -> "Chuva prevista (3 horas)"
            RASRF_24H -> "Chuva prevista (24 horas)"
            LANDSLIDE -> "Kikikuru Deslizamentos (Risco)"
            HEAVY_RAIN -> "Kikikuru Chuva forte (Alagamento e rios)"
            INUNDATION -> "Kikikuru Alagamento urbano"
            FLOOD -> "Kikikuru Cheia fluvial"
            HIMAWARI_TRUECOLOR -> "Himawari (Cor real)"
            HIMAWARI_INFRARED -> "Himawari (Infravermelho / 24 h)"
            HIMAWARI_STEAM -> "Himawari (Vapor d'água)"
            HIMAWARI_CLOUDTOP -> "Himawari (Topos de nuvens)"
            SNOW_DEPTH -> "Espessura de neve (Atual)"
            SNOWFALL_3H -> "Queda de neve (3 h)"
            SNOWFALL_24H -> "Queda de neve (24 h)"
            WEATHER_DIST -> "Distribuição do tempo"
            TEMP_DIST -> "Distribuição de temperatura"
            MAX_TEMP -> "Temperatura máxima diurna"
            MIN_TEMP -> "Temperatura mínima matinal"
            WAVE_HEIGHT -> "Previsão de altura das ondas"
            WIND_SPEED -> "Previsão de vento marítimo"
            GSI_PALE -> "Mapa claro GSI"
            JMA_BASE -> "Mapa padrão JMA"
            FLOOD_HAZARD -> "Mapa de áreas alagáveis (MLIT)"
            TYPHOON -> "Informações de tufões (Trajetória e alertas)"
        }
    }

    fun shortName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> shortName
        AppLanguage.KOREAN -> when (this) {
            HRPNS -> "강수 나우캐스트"
            THNS -> "낙뢰 예보"
            TRNS -> "토네이도 확률"
            RASRF_1H -> "1시간 예상 강수"
            RASRF_3H -> "3시간 예상 강수"
            RASRF_24H -> "24시간 예상 강수"
            LANDSLIDE -> "산사태 위험"
            HEAVY_RAIN -> "호우 위험"
            INUNDATION -> "침수 위험"
            FLOOD -> "홍수 위험"
            HIMAWARI_TRUECOLOR -> "히마와리 컬러"
            HIMAWARI_INFRARED -> "히마와리 적외"
            HIMAWARI_STEAM -> "히마와리 수증기"
            HIMAWARI_CLOUDTOP -> "운정 강조"
            SNOW_DEPTH -> "적설 깊이"
            SNOWFALL_3H -> "3시간 적설"
            SNOWFALL_24H -> "24시간 적설"
            WEATHER_DIST -> "날씨 분포"
            TEMP_DIST -> "기온 분포"
            MAX_TEMP -> "최고기온"
            MIN_TEMP -> "최저기온"
            WAVE_HEIGHT -> "파고 예보"
            WIND_SPEED -> "해상 풍속"
            GSI_PALE -> "지리원 담색"
            JMA_BASE -> "JMA 표준"
            FLOOD_HAZARD -> "침수 예상구역"
            TYPHOON -> "태풍 정보"
        }
        AppLanguage.ENGLISH -> when (this) {
            HRPNS -> "Precip Nowcast"
            THNS -> "Lightning"
            TRNS -> "Tornado"
            RASRF_1H -> "1h Rain"
            RASRF_3H -> "3h Rain"
            RASRF_24H -> "24h Rain"
            LANDSLIDE -> "Landslide"
            HEAVY_RAIN -> "Heavy Rain"
            INUNDATION -> "Inundation"
            FLOOD -> "River Flood"
            HIMAWARI_TRUECOLOR -> "Himawari Color"
            HIMAWARI_INFRARED -> "Himawari IR"
            HIMAWARI_STEAM -> "Himawari Vapor"
            HIMAWARI_CLOUDTOP -> "Cloud Top"
            SNOW_DEPTH -> "Snow Depth"
            SNOWFALL_3H -> "3h Snow"
            SNOWFALL_24H -> "24h Snow"
            WEATHER_DIST -> "Weather Dist"
            TEMP_DIST -> "Temp Dist"
            MAX_TEMP -> "High Temp"
            MIN_TEMP -> "Low Temp"
            WAVE_HEIGHT -> "Wave Height"
            WIND_SPEED -> "Marine Wind"
            GSI_PALE -> "GSI Pale"
            JMA_BASE -> "JMA Base"
            FLOOD_HAZARD -> "Flood Hazard"
            TYPHOON -> "Typhoon"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            HRPNS -> "降水临近预报"
            THNS -> "雷电预报"
            TRNS -> "龙卷风概率"
            RASRF_1H -> "1小时预报雨量"
            RASRF_3H -> "3小时预报雨量"
            RASRF_24H -> "24小时预报雨量"
            LANDSLIDE -> "泥石流风险"
            HEAVY_RAIN -> "大雨风险"
            INUNDATION -> "积水风险"
            FLOOD -> "洪水风险"
            HIMAWARI_TRUECOLOR -> "向日葵真彩"
            HIMAWARI_INFRARED -> "向日葵红外"
            HIMAWARI_STEAM -> "向日葵水汽"
            HIMAWARI_CLOUDTOP -> "向日葵云顶"
            SNOW_DEPTH -> "积雪深度"
            SNOWFALL_3H -> "3小时降雪"
            SNOWFALL_24H -> "24小时降雪"
            WEATHER_DIST -> "天气分布"
            TEMP_DIST -> "气温分布"
            MAX_TEMP -> "最高气温"
            MIN_TEMP -> "最低气温"
            WAVE_HEIGHT -> "浪高预报"
            WIND_SPEED -> "海上风速"
            GSI_PALE -> "地理院浅色"
            JMA_BASE -> "气象厅底图"
            FLOOD_HAZARD -> "洪水淹没区"
            TYPHOON -> "台风情报"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            HRPNS -> "降水臨近預報"
            THNS -> "雷電預報"
            TRNS -> "龍捲風機率"
            RASRF_1H -> "1小時預報雨量"
            RASRF_3H -> "3小時預報雨量"
            RASRF_24H -> "24小時預報雨量"
            LANDSLIDE -> "土石流風險"
            HEAVY_RAIN -> "大雨風險"
            INUNDATION -> "淹水風險"
            FLOOD -> "洪水風險"
            HIMAWARI_TRUECOLOR -> "向日葵真彩"
            HIMAWARI_INFRARED -> "向日葵紅外"
            HIMAWARI_STEAM -> "向日葵水汽"
            HIMAWARI_CLOUDTOP -> "向日葵雲頂"
            SNOW_DEPTH -> "積雪深度"
            SNOWFALL_3H -> "3小時降雪"
            SNOWFALL_24H -> "24小時降雪"
            WEATHER_DIST -> "天氣分布"
            TEMP_DIST -> "氣溫分布"
            MAX_TEMP -> "最高氣溫"
            MIN_TEMP -> "最低氣溫"
            WAVE_HEIGHT -> "浪高預報"
            WIND_SPEED -> "海上風速"
            GSI_PALE -> "地理院淺色"
            JMA_BASE -> "氣象廳底圖"
            FLOOD_HAZARD -> "洪水淹沒區"
            TYPHOON -> "颱風情報"
        }
        AppLanguage.SPANISH -> when (this) {
            HRPNS -> "Radar lluvia"
            THNS -> "Rayos"
            TRNS -> "Tornados"
            RASRF_1H -> "Lluvia 1 h"
            RASRF_3H -> "Lluvia 3 h"
            RASRF_24H -> "Lluvia 24 h"
            LANDSLIDE -> "Deslizamiento"
            HEAVY_RAIN -> "Lluvia intensa"
            INUNDATION -> "Inundación"
            FLOOD -> "Crecida"
            HIMAWARI_TRUECOLOR -> "Himawari Color"
            HIMAWARI_INFRARED -> "Himawari IR"
            HIMAWARI_STEAM -> "Himawari Vapor"
            HIMAWARI_CLOUDTOP -> "Topes nubosos"
            SNOW_DEPTH -> "Espesor nieve"
            SNOWFALL_3H -> "Nieve 3 h"
            SNOWFALL_24H -> "Nieve 24 h"
            WEATHER_DIST -> "Tiempo"
            TEMP_DIST -> "Temperatura"
            MAX_TEMP -> "Temp. máx."
            MIN_TEMP -> "Temp. mín."
            WAVE_HEIGHT -> "Oleaje"
            WIND_SPEED -> "Viento mar"
            GSI_PALE -> "GSI Claro"
            JMA_BASE -> "Base JMA"
            FLOOD_HAZARD -> "Riesgo inund."
            TYPHOON -> "Tifón"
        }
        AppLanguage.FRENCH -> when (this) {
            HRPNS -> "Radar pluie"
            THNS -> "Foudre"
            TRNS -> "Tornades"
            RASRF_1H -> "Pluie 1 h"
            RASRF_3H -> "Pluie 3 h"
            RASRF_24H -> "Pluie 24 h"
            LANDSLIDE -> "Glissement"
            HEAVY_RAIN -> "Fortes pluies"
            INUNDATION -> "Inondation"
            FLOOD -> "Crue"
            HIMAWARI_TRUECOLOR -> "Himawari Couleur"
            HIMAWARI_INFRARED -> "Himawari IR"
            HIMAWARI_STEAM -> "Himawari Vapeur"
            HIMAWARI_CLOUDTOP -> "Sommets nuag."
            SNOW_DEPTH -> "Hauteur neige"
            SNOWFALL_3H -> "Neige 3 h"
            SNOWFALL_24H -> "Neige 24 h"
            WEATHER_DIST -> "Météo"
            TEMP_DIST -> "Température"
            MAX_TEMP -> "Temp. max."
            MIN_TEMP -> "Temp. min."
            WAVE_HEIGHT -> "Vagues"
            WIND_SPEED -> "Vent mer"
            GSI_PALE -> "GSI Clair"
            JMA_BASE -> "Base JMA"
            FLOOD_HAZARD -> "Risque crue"
            TYPHOON -> "Typhon"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            HRPNS -> "Radar chuva"
            THNS -> "Raios"
            TRNS -> "Tornados"
            RASRF_1H -> "Chuva 1 h"
            RASRF_3H -> "Chuva 3 h"
            RASRF_24H -> "Chuva 24 h"
            LANDSLIDE -> "Deslizamento"
            HEAVY_RAIN -> "Chuva forte"
            INUNDATION -> "Alagamento"
            FLOOD -> "Cheia"
            HIMAWARI_TRUECOLOR -> "Himawari Cor"
            HIMAWARI_INFRARED -> "Himawari IV"
            HIMAWARI_STEAM -> "Himawari Vapor"
            HIMAWARI_CLOUDTOP -> "Topos nuvens"
            SNOW_DEPTH -> "Espessura neve"
            SNOWFALL_3H -> "Neve 3 h"
            SNOWFALL_24H -> "Neve 24 h"
            WEATHER_DIST -> "Tempo"
            TEMP_DIST -> "Temperatura"
            MAX_TEMP -> "Temp. máx."
            MIN_TEMP -> "Temp. mín."
            WAVE_HEIGHT -> "Ondas"
            WIND_SPEED -> "Vento mar"
            GSI_PALE -> "GSI Claro"
            JMA_BASE -> "Base JMA"
            FLOOD_HAZARD -> "Risco cheia"
            TYPHOON -> "Tufão"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> description
        AppLanguage.KOREAN -> when (this) {
            HRPNS -> "지난 1시간 실황 + 향후 1시간 고해상도 강수 예측"
            THNS -> "강한 뇌우 징후 및 낙뢰 발생 확률 (활동도 1~4)"
            TRNS -> "토네이도 및 강한 돌풍 발생 위험도 (확도 1·2)"
            RASRF_1H -> "향후 1시간 예상 강수량 분포"
            RASRF_3H -> "향후 3시간 예상 강수량 분포"
            RASRF_24H -> "향후 24시간 누적 예상 강수량 분포"
            LANDSLIDE -> "호우로 인한 산사태·토사재해 위험도 (주의/경계/위험/매우 위험)"
            HEAVY_RAIN -> "침수 위험도와 홍수 위험도를 함께 표시하는 호우 위험도 분포"
            INUNDATION -> "단시간 집중호우로 인한 도로·주택 침수 위험도"
            FLOOD -> "하천 수위 상승 및 범람 위험도 분포 (주요 하천·중소 하천)"
            HIMAWARI_TRUECOLOR -> "우주에서 육안으로 본 지구에 가까운 고화질 위성 구름 영상"
            HIMAWARI_INFRARED -> "구름의 온도와 고도를 시각화 (야간 포함 24시간 관측)"
            HIMAWARI_STEAM -> "중·상층 대기 수증기 및 제트기류 흐름 시각화"
            HIMAWARI_CLOUDTOP -> "국지성 집중호우를 유발하는 발달한 적란운 운정을 색상 강조"
            SNOW_DEPTH -> "전국 실시간 적설 깊이 분포"
            SNOWFALL_3H -> "최근 3시간 누적 신적설량 분포"
            SNOWFALL_24H -> "최근 24시간 누적 신적설량 분포"
            WEATHER_DIST -> "맑음·흐림·비·눈 지역별 날씨 분포 예보"
            TEMP_DIST -> "시간대별 전국 예상 기온 분포"
            MAX_TEMP -> "오늘 낮 최고기온 예보"
            MIN_TEMP -> "내일 아침 최저기온 예보"
            WAVE_HEIGHT -> "연안·근해 해역 예상 파고 분포"
            WIND_SPEED -> "연안·근해 해역 예상 풍속 분포"
            GSI_PALE -> "도로와 지명이 잘 보이는 표준 담색 지도"
            JMA_BASE -> "기상 데이터를 돋보이게 하는 공식 표준 배경 지도"
            FLOOD_HAZARD -> "하천 범람 시 예상 최대 침수 깊이 레이어"
            TYPHOON -> "태풍 중심 위치·이동 방향·속도·강풍반경·폭풍반경·진로 예보원"
        }
        AppLanguage.ENGLISH -> when (this) {
            HRPNS -> "Past 1h observation + 1h high-resolution precipitation forecast"
            THNS -> "Signs of severe thunderstorms & lightning probability (Activity 1-4)"
            TRNS -> "Risk of tornadoes and severe gusts (Category 1 & 2)"
            RASRF_1H -> "Forecast precipitation distribution for the next 1 hour"
            RASRF_3H -> "Forecast precipitation distribution for the next 3 hours"
            RASRF_24H -> "Forecast cumulative precipitation distribution for 24 hours"
            LANDSLIDE -> "Landslide risk due to heavy rain (Advisory / Warning / Danger / Extreme)"
            HEAVY_RAIN -> "Heavy rain risk combining inundation and river flood risks"
            INUNDATION -> "Urban inundation risk for streets and properties from intense rain"
            FLOOD -> "River surge and flood danger distribution for rivers"
            HIMAWARI_TRUECOLOR -> "High-definition satellite image close to natural naked-eye vision"
            HIMAWARI_INFRARED -> "Cloud temperature & altitude visualization (24/7 day and night)"
            HIMAWARI_STEAM -> "Visualizes mid-to-upper level atmospheric moisture & jet streams"
            HIMAWARI_CLOUDTOP -> "Color-highlights tops of deep convective clouds causing downpours"
            SNOW_DEPTH -> "Real-time nationwide snow depth distribution"
            SNOWFALL_3H -> "Cumulative snowfall distribution for the past 3 hours"
            SNOWFALL_24H -> "Cumulative snowfall distribution for the past 24 hours"
            WEATHER_DIST -> "Regional forecast distribution: sunny, cloudy, rain, and snow"
            TEMP_DIST -> "Hourly nationwide forecast temperature distribution"
            MAX_TEMP -> "Forecast maximum daytime temperature for today"
            MIN_TEMP -> "Forecast minimum temperature for tomorrow morning"
            WAVE_HEIGHT -> "Expected wave height distribution in coastal and offshore waters"
            WIND_SPEED -> "Expected wind speed distribution in coastal and offshore waters"
            GSI_PALE -> "Standard pale map highlighting roads and place names clearly"
            JMA_BASE -> "Official standard basemap optimized for weather overlays"
            FLOOD_HAZARD -> "Estimated maximum inundation depth layer during river floods"
            TYPHOON -> "JMA official typhoon center position, speed, direction, gale/storm warning areas & forecast circles"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            HRPNS -> "过去1小时实况＋未来1小时高分辨率降水预测"
            THNS -> "强雷雨前兆与落雷概率（活动度1~4）"
            TRNS -> "龙卷风和强突风的发生危险度（确度1与2）"
            RASRF_1H -> "未来1小时预计降水量分布"
            RASRF_3H -> "未来3小时预计降水量分布"
            RASRF_24H -> "未来24小时预计累计降水量分布"
            LANDSLIDE -> "大雨引发泥石流地质灾害危险度（注意/警戒/危险/极度危险）"
            HEAVY_RAIN -> "叠加显示淹没积水与河流洪水的综合大雨危险度"
            INUNDATION -> "短时强降雨导致的道路与居民区内涝积水危险度"
            FLOOD -> "河流暴涨与泛滥危险度分布（指定河流与中小河流）"
            HIMAWARI_TRUECOLOR -> "接近人眼从太空俯瞰地球的高清卫星云图"
            HIMAWARI_INFRARED -> "可视化云层温度与高度（夜间全天候可用）"
            HIMAWARI_STEAM -> "可视化对流层中高层大气湿度与急流动态"
            HIMAWARI_CLOUDTOP -> "色彩高亮显示易引发局地强降雨的强对流积雨云云顶"
            SNOW_DEPTH -> "全国实时积雪深度分布"
            SNOWFALL_3H -> "最近3小时累计降雪量分布"
            SNOWFALL_24H -> "最近24小时累计降雪量分布"
            WEATHER_DIST -> "晴、多云、雨、雪的区域预报"
            TEMP_DIST -> "不同时段全国预计气温分布"
            MAX_TEMP -> "今日白天最高气温预报"
            MIN_TEMP -> "明晨最低气温预报"
            WAVE_HEIGHT -> "沿岸与近海海域预计浪高分布"
            WIND_SPEED -> "沿岸与近海海域预计风速分布"
            GSI_PALE -> "道路与地名清晰易读的标准浅色地图"
            JMA_BASE -> "突出气象数据展示的官方标准底图"
            FLOOD_HAZARD -> "河流泛滥时预计最大淹没水深图层"
            TYPHOON -> "日本气象厅官方台风中心位置、移动速度、方向、强风圈、暴风圈与预测圆"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            HRPNS -> "過去1小時實況＋未來1小時高解析度降水預測"
            THNS -> "強雷雨前兆與落雷機率（活動度1~4）"
            TRNS -> "龍捲風和強突風的發生危險度（確度1與2）"
            RASRF_1H -> "未來1小時預計降水量分布"
            RASRF_3H -> "未來3小時預計降水量分布"
            RASRF_24H -> "未來24小時預計累計降水量分布"
            LANDSLIDE -> "大雨引發土石流地質災害危險度（注意/警戒/危險/極度危險）"
            HEAVY_RAIN -> "疊加顯示淹沒積水與河流洪水的綜合大雨危險度"
            INUNDATION -> "短時強降雨導致的道路與居民區內澇淹水危險度"
            FLOOD -> "河流暴漲與氾濫危險度分布（指定河流與中小河流）"
            HIMAWARI_TRUECOLOR -> "接近人眼從太空俯瞰地球的高畫質衛星雲圖"
            HIMAWARI_INFRARED -> "可視化雲層溫度與高度（夜間全天候可用）"
            HIMAWARI_STEAM -> "可視化對流層中高層大氣濕度與急流動態"
            HIMAWARI_CLOUDTOP -> "色彩高亮顯示易引發局地強降雨的強對流積雨雲雲頂"
            SNOW_DEPTH -> "全國即時積雪深度分布"
            SNOWFALL_3H -> "最近3小時累計降雪量分布"
            SNOWFALL_24H -> "最近24小時累計降雪量分布"
            WEATHER_DIST -> "晴、多雲、雨、雪的區域預報"
            TEMP_DIST -> "不同時段全國預計氣溫分布"
            MAX_TEMP -> "今日白天最高氣溫預報"
            MIN_TEMP -> "明晨最低氣溫預報"
            WAVE_HEIGHT -> "沿岸與近海海域預計浪高分布"
            WIND_SPEED -> "沿岸與近海海域預計風速分布"
            GSI_PALE -> "道路與地名清晰易讀的標準淺色地圖"
            JMA_BASE -> "突出氣象數據展示的官方標準底圖"
            FLOOD_HAZARD -> "河流氾濫時預計最大淹沒水深圖層"
            TYPHOON -> "日本氣象廳官方颱風中心位置、移動速度、方向、強風圈、暴風圈與預測圓"
        }
        AppLanguage.SPANISH -> when (this) {
            HRPNS -> "Última hora observada + previsión de alta resolución a 1 hora"
            THNS -> "Indicios de tormentas severas y probabilidad de rayos (Actividad 1-4)"
            TRNS -> "Riesgo de tornados y rachas intensas (Categorías 1 y 2)"
            RASRF_1H -> "Distribución de precipitación prevista para la próxima hora"
            RASRF_3H -> "Distribución de precipitación prevista para las próximas 3 horas"
            RASRF_24H -> "Distribución de precipitación acumulada prevista en 24 horas"
            LANDSLIDE -> "Riesgo de deslizamientos por lluvia intensa (Aviso / Alerta / Peligro / Extremo)"
            HEAVY_RAIN -> "Riesgo combinado de inundación urbana y crecida fluvial por lluvia intensa"
            INUNDATION -> "Riesgo de inundación urbana en calles y viviendas por lluvia intensa"
            FLOOD -> "Distribución de peligro de crecida y desbordamiento de ríos"
            HIMAWARI_TRUECOLOR -> "Imagen satelital de alta definición similar a la visión natural"
            HIMAWARI_INFRARED -> "Visualización de temperatura y altitud de nubes (día y noche)"
            HIMAWARI_STEAM -> "Visualiza la humedad atmosférica en niveles medios-altos y corrientes en chorro"
            HIMAWARI_CLOUDTOP -> "Resalta con color las cimas de cumulonimbos causantes de aguaceros"
            SNOW_DEPTH -> "Distribución nacional en tiempo real del espesor de nieve"
            SNOWFALL_3H -> "Distribución acumulada de nevadas en las últimas 3 horas"
            SNOWFALL_24H -> "Distribución acumulada de nevadas en las últimas 24 horas"
            WEATHER_DIST -> "Previsión regional del tiempo: sol, nubes, lluvia y nieve"
            TEMP_DIST -> "Distribución horaria nacional de temperaturas previstas"
            MAX_TEMP -> "Previsión de temperatura máxima diurna para hoy"
            MIN_TEMP -> "Previsión de temperatura mínima para mañana por la mañana"
            WAVE_HEIGHT -> "Distribución prevista de altura de olas en aguas costeras y mar adentro"
            WIND_SPEED -> "Distribución prevista de velocidad del viento en zonas marítimas"
            GSI_PALE -> "Mapa claro estándar que facilita la lectura de vías y topónimos"
            JMA_BASE -> "Mapa base oficial optimizado para capas meteorológicas"
            FLOOD_HAZARD -> "Capa de profundidad máxima estimada de inundación fluvial"
            TYPHOON -> "Posición del centro, velocidad, dirección, áreas de viento y círculos de pronóstico JMA"
        }
        AppLanguage.FRENCH -> when (this) {
            HRPNS -> "Observation de la dernière heure + prévision haute résolution à 1 h"
            THNS -> "Signes d'orages violents et probabilité de foudre (Activité 1 à 4)"
            TRNS -> "Risque de tornades et de rafales violentes (Catégories 1 et 2)"
            RASRF_1H -> "Répartition des précipitations prévues pour la prochaine heure"
            RASRF_3H -> "Répartition des précipitations prévues pour les 3 prochaines heures"
            RASRF_24H -> "Répartition des précipitations cumulées prévues sur 24 heures"
            LANDSLIDE -> "Risque de glissements de terrain liés aux fortes pluies (Vigilance à Extrême)"
            HEAVY_RAIN -> "Risque combiné de ruissellement urbain et de crue fluviale"
            INUNDATION -> "Risque d'inondation urbaine des routes et habitations par fortes averses"
            FLOOD -> "Répartition du danger de montée des eaux et de débordement des rivières"
            HIMAWARI_TRUECOLOR -> "Image satellite haute définition proche de la vision naturelle"
            HIMAWARI_INFRARED -> "Visualisation de la température et de l'altitude des nuages (24 h/24)"
            HIMAWARI_STEAM -> "Visualise l'humidité en moyenne et haute atmosphère et les courants-jets"
            HIMAWARI_CLOUDTOP -> "Met en évidence les sommets de cumulonimbus à l'origine d'averses intenses"
            SNOW_DEPTH -> "Répartition nationale en temps réel de la hauteur de neige"
            SNOWFALL_3H -> "Cumul des chutes de neige au cours des 3 dernières heures"
            SNOWFALL_24H -> "Cumul des chutes de neige au cours des 24 dernières heures"
            WEATHER_DIST -> "Prévision régionale : soleil, nuages, pluie et neige"
            TEMP_DIST -> "Répartition horaire nationale des températures prévues"
            MAX_TEMP -> "Température maximale diurne prévue pour aujourd'hui"
            MIN_TEMP -> "Température minimale prévue pour demain matin"
            WAVE_HEIGHT -> "Répartition prévue de la hauteur des vagues près des côtes et au large"
            WIND_SPEED -> "Répartition prévue de la vitesse du vent en mer"
            GSI_PALE -> "Fond de carte clair mettant en valeur routes et noms de lieux"
            JMA_BASE -> "Fond de carte officiel optimisé pour les données météo"
            FLOOD_HAZARD -> "Hauteur maximale estimée de submersion en cas de crue fluviale"
            TYPHOON -> "Position officielle JMA, vitesse, direction, zones de vents et cercles de trajectoire"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            HRPNS -> "Observação da última hora + previsão em alta resolução para 1 hora"
            THNS -> "Sinais de tempestades severas e probabilidade de raios (Atividade 1-4)"
            TRNS -> "Risco de tornados e rajadas intensas (Categorias 1 e 2)"
            RASRF_1H -> "Distribuição de precipitação prevista para a próxima hora"
            RASRF_3H -> "Distribuição de precipitação prevista para as próximas 3 horas"
            RASRF_24H -> "Distribuição de precipitação acumulada prevista em 24 horas"
            LANDSLIDE -> "Risco de deslizamentos por chuva forte (Atenção / Alerta / Perigo / Extremo)"
            HEAVY_RAIN -> "Risco combinado de alagamento urbano e cheia fluvial por chuva forte"
            INUNDATION -> "Risco de alagamento urbano em vias e imóveis devido a chuvas intensas"
            FLOOD -> "Distribuição do perigo de cheia e transbordamento de rios"
            HIMAWARI_TRUECOLOR -> "Imagem de satélite em alta definição próxima à visão natural"
            HIMAWARI_INFRARED -> "Visualização da temperatura e altitude das nuvens (24 horas)"
            HIMAWARI_STEAM -> "Visualiza a umidade nos níveis médios e altos da atmosfera e correntes de jato"
            HIMAWARI_CLOUDTOP -> "Destaca em cores os topos de nuvens cumulonimbus causadoras de temporais"
            SNOW_DEPTH -> "Distribuição nacional em tempo real da espessura de neve"
            SNOWFALL_3H -> "Distribuição acumulada de queda de neve nas últimas 3 horas"
            SNOWFALL_24H -> "Distribuição acumulada de queda de neve nas últimas 24 horas"
            WEATHER_DIST -> "Previsão regional do tempo: sol, nuvens, chuva e neve"
            TEMP_DIST -> "Distribuição horária nacional das temperaturas previstas"
            MAX_TEMP -> "Previsão de temperatura máxima diurna para hoje"
            MIN_TEMP -> "Previsão de temperatura mínima para amanhã de manhã"
            WAVE_HEIGHT -> "Distribuição prevista da altura das ondas em águas costeiras e abertas"
            WIND_SPEED -> "Distribuição prevista da velocidade do vento em áreas marítimas"
            GSI_PALE -> "Mapa claro padrão que destaca estradas e nomes de locais"
            JMA_BASE -> "Mapa base oficial otimizado para camadas meteorológicas"
            FLOOD_HAZARD -> "Camada de profundidade máxima estimada de inundação fluvial"
            TYPHOON -> "Posição oficial JMA do centro, velocidade, direção, áreas de vento e círculos de previsão"
        }
    }
}

/**
 * 日本向けマップにおいて通常表示（デフォルト）が許可されているレイヤーかどうかを判定する。
 * 雨雲ナウキャスト、雷ナウキャスト、キキクル危険度（土砂・大雨・浸水・洪水）、雪降雪量情報（積雪深・3h降雪・24h降雪）のみ許可され、
 * それ以外はデバッグ設定でONにしないと非表示となる。
 */
fun isAllowedJapanRadarLayer(layer: JmaLayerType): Boolean = when (layer) {
    JmaLayerType.HRPNS,
    JmaLayerType.THNS,
    JmaLayerType.TYPHOON,
    JmaLayerType.LANDSLIDE,
    JmaLayerType.HEAVY_RAIN,
    JmaLayerType.INUNDATION,
    JmaLayerType.FLOOD,
    JmaLayerType.SNOW_DEPTH,
    JmaLayerType.SNOWFALL_3H,
    JmaLayerType.SNOWFALL_24H -> true
    else -> false
}
