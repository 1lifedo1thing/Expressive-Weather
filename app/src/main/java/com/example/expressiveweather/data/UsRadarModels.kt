package com.example.expressiveweather.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * アメリカ向け雨雲レーダーのフレーム種別
 */
enum class UsRadarFrameType(
    val displayName: String,
    val shortName: String,
    val description: String,
    val updateIntervalMinutes: Int
) {
    /** 実況レーダーマップ: MRMS / NEXRAD Base Reflectivity 地上反射強度 (過去2時間〜現在、約2分〜5分間隔) */
    MRMS_BASE_REFLECTIVITY(
        displayName = "NEXRAD / MRMS Base Reflectivity",
        shortName = "実況 (NEXRAD/MRMS)",
        description = "実況レーダーマップ（NEXRAD地上反射強度・約2〜5分更新）",
        updateIntervalMinutes = 2
    ),

    /** 直近0分から60分先：NEXRAD外挿予測 (現在〜60分先、5分間隔高精度ナウキャスト) */
    NEXRAD_NOWCAST_EXTRAPOLATION(
        displayName = "NEXRAD Extrapolated Nowcast",
        shortName = "外挿予測 (0〜60分)",
        description = "直近0分から60分先：NEXRAD外挿予測（5分更新）",
        updateIntervalMinutes = 5
    ),

    /** 予報レーダーマップ: HRRR 1km AGL Simulated Reflectivity 地上1km模擬反射強度 (1時間〜18時間先) */
    HRRR_SIMULATED_REFLECTIVITY(
        displayName = "HRRR 1km AGL Simulated Reflectivity",
        shortName = "予報 (HRRRシミュレーション)",
        description = "予報レーダーマップ（HRRR 1km AGLシミュレーション・直近15分/以降1時間更新）",
        updateIntervalMinutes = 15
    )
}

/**
 * 全米雨雲レーダーの表示レイヤーフィルター
 */
enum class UsRadarLayerFilter(
    val id: String,
    val titleJa: String,
    val titleEn: String,
    val descriptionJa: String,
    val descriptionEn: String
) {
    ALL(
        "all",
        "統合タイムライン (実況＋外挿＋予報)",
        "Unified Timeline (Obs + Nowcast + Forecast)",
        "過去2時間実況から18時間予報までシームレス連続再生",
        "Seamless playback from 2h ago to 18h ahead"
    ),
    OBSERVATION_ONLY(
        "obs",
        "実況レーダーマップ (NEXRAD / MRMS)",
        "Observation Radar (NEXRAD / MRMS)",
        "地上反射強度の過去2時間高精度実況トラッキング",
        "High-resolution observation tracking for past 2 hours"
    ),
    EXTRAPOLATION_ONLY(
        "extrap",
        "直近0分から60分先：NEXRAD外挿予測",
        "Nowcast Extrapolation (0-60 min)",
        "レーダーエコー移動ベクトルによる1時間短時間予測",
        "Motion-vector extrapolated nowcast for next 60 minutes"
    ),
    HRRR_FORECAST_ONLY(
        "hrrr",
        "予報レーダーマップ (HRRRシミュレーション)",
        "Forecast Radar (HRRR Simulation)",
        "数値気象予測モデルによる18時間先までの広域シミュレーション",
        "Numerical weather prediction simulation up to 18h ahead"
    );

    fun title(language: AppLanguage): String = if (language == AppLanguage.JAPANESE) titleJa else titleEn
    fun description(language: AppLanguage): String = if (language == AppLanguage.JAPANESE) descriptionJa else descriptionEn
}

/**
 * アメリカ気象レイヤーのカテゴリ定義（ナウキャストと同様の分類体系）
 */
enum class UsLayerCategory(
    val id: String,
    val displayNameJa: String,
    val displayNameEn: String,
    val descriptionJa: String,
    val descriptionEn: String
) {
    UNIFIED(
        "unified",
        "統合タイムライン",
        "Unified Timeline",
        "実況・外挿予測・HRRR予報の連続再生",
        "Seamless playback from past obs to 18h HRRR forecast"
    ),
    HRRR_FORECAST(
        "hrrr_forecast",
        "HRRRシミュレーション予報",
        "HRRR Forecast",
        "NOAA高解像度数値予報モデルによる18時間シミュレーション",
        "NOAA High-Resolution Rapid Refresh 18-hour simulations"
    ),
    NOWCAST(
        "nowcast",
        "直近外挿予測 (0〜60分)",
        "Nowcast Extrapolation",
        "NEXRADレーダーエコー移動ベクトル予測",
        "Motion-vector extrapolated nowcast for next 60 minutes"
    ),
    OBSERVATION(
        "observation",
        "実況レーダーマップ",
        "Observation Radar",
        "全米NEXRAD/MRMS地上反射強度およびエコー実況",
        "Nationwide NEXRAD & MRMS multi-radar observations"
    );

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> displayNameJa
        AppLanguage.ENGLISH -> displayNameEn
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            UNIFIED -> "综合时间线"
            HRRR_FORECAST -> "HRRR模拟预报"
            NOWCAST -> "短临外推预测 (0~60分)"
            OBSERVATION -> "实况雷达地图"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            UNIFIED -> "綜合時間線"
            HRRR_FORECAST -> "HRRR模擬預報"
            NOWCAST -> "短臨外推預測 (0~60分)"
            OBSERVATION -> "實況雷達地圖"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descriptionJa
        AppLanguage.ENGLISH -> descriptionEn
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            UNIFIED -> "实况、短临外推与HRRR预报的连续回放"
            HRRR_FORECAST -> "NOAA高分辨率数值预报模型18小时模拟"
            NOWCAST -> "NEXRAD雷达回波移动矢量预测"
            OBSERVATION -> "全美NEXRAD/MRMS多雷达合成实况"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            UNIFIED -> "實況、短臨外推與HRRR預報的連續播放"
            HRRR_FORECAST -> "NOAA高解析度數值預報模型18小時模擬"
            NOWCAST -> "NEXRAD雷達回波移動向量預測"
            OBSERVATION -> "全美NEXRAD/MRMS多雷達合成實況"
        }
    }
}

/**
 * 全米レーダー・HRRR数値予報シミュレーションのマップ候補（レイヤー一覧）
 */
enum class UsRadarLayerType(
    val id: String,
    val category: UsLayerCategory,
    val displayNameJa: String,
    val displayNameEn: String,
    val shortNameJa: String,
    val shortNameEn: String,
    val descriptionJa: String,
    val descriptionEn: String,
    val resolution: String,
    val updateIntervalMinutes: Int,
    val associatedFilter: UsRadarLayerFilter
) {
    // 1. 統合タイムライン
    UNIFIED_ALL(
        id = "us_unified",
        category = UsLayerCategory.UNIFIED,
        displayNameJa = "統合タイムライン (実況+外挿+予報)",
        displayNameEn = "Unified Radar (Obs + Nowcast + HRRR)",
        shortNameJa = "統合レーダー",
        shortNameEn = "Unified",
        descriptionJa = "過去2時間実況から0〜60分外挿、18時間HRRR予報までシームレス連続再生",
        descriptionEn = "Seamless playback from 2h observation to 18h HRRR forecast",
        resolution = "1km〜3km AGL",
        updateIntervalMinutes = 5,
        associatedFilter = UsRadarLayerFilter.ALL
    ),

    // 2. HRRR 予報シミュレーション群 (マップ候補)
    HRRR_COMPOSITE_REFL(
        id = "hrrr_refl",
        category = UsLayerCategory.HRRR_FORECAST,
        displayNameJa = "HRRR 1km 模擬反射強度予報",
        displayNameEn = "HRRR 1km Simulated Reflectivity",
        shortNameJa = "HRRR 模擬反射強度",
        shortNameEn = "HRRR Reflectivity",
        descriptionJa = "地上1kmの降水・豪雨・雷雨エコー模擬シミュレーション (18時間先まで)",
        descriptionEn = "1km AGL simulated radar reflectivity forecast up to 18 hours",
        resolution = "1km AGL",
        updateIntervalMinutes = 15,
        associatedFilter = UsRadarLayerFilter.HRRR_FORECAST_ONLY
    ),
    HRRR_PRECIP_TYPE(
        id = "hrrr_ptype",
        category = UsLayerCategory.HRRR_FORECAST,
        displayNameJa = "HRRR 降水種別予報 (雨・雪・みぞれ・雨氷)",
        displayNameEn = "HRRR Precipitation Type (Rain/Snow/Ice)",
        shortNameJa = "HRRR 降水種別",
        shortNameEn = "HRRR Precip Type",
        descriptionJa = "雨（緑）・雪（青）・みぞれ（橙）・着氷性の雨（紫）の降水タイプ判別予報",
        descriptionEn = "Simulated precipitation type: Rain, Snow, Sleet, and Freezing Rain",
        resolution = "3km",
        updateIntervalMinutes = 60,
        associatedFilter = UsRadarLayerFilter.HRRR_FORECAST_ONLY
    ),
    HRRR_TOTAL_PRECIP(
        id = "hrrr_qpf",
        category = UsLayerCategory.HRRR_FORECAST,
        displayNameJa = "HRRR 積算降水量予報 (今後18時間)",
        displayNameEn = "HRRR Total Accumulated Precipitation",
        shortNameJa = "HRRR 積算降水",
        shortNameEn = "HRRR Accumulation",
        descriptionJa = "今後1時間〜18時間で予想される総降水量シミュレーション",
        descriptionEn = "Total accumulated precipitation simulation over the next 18 hours",
        resolution = "3km",
        updateIntervalMinutes = 60,
        associatedFilter = UsRadarLayerFilter.HRRR_FORECAST_ONLY
    ),
    HRRR_WIND_GUSTS(
        id = "hrrr_gust",
        category = UsLayerCategory.HRRR_FORECAST,
        displayNameJa = "HRRR 地上突風・強風予報",
        displayNameEn = "HRRR Surface Wind Gusts",
        shortNameJa = "HRRR 突風予報",
        shortNameEn = "HRRR Gusts",
        descriptionJa = "雷雨に伴うダウンバーストや突風・強風場の予測シミュレーション",
        descriptionEn = "Predicted surface wind gusts and downburst potential field",
        resolution = "3km",
        updateIntervalMinutes = 60,
        associatedFilter = UsRadarLayerFilter.HRRR_FORECAST_ONLY
    ),
    HRRR_TORNADO_POTENTIAL(
        id = "hrrr_tornado",
        category = UsLayerCategory.HRRR_FORECAST,
        displayNameJa = "HRRR シビアストーム・竜巻指数 (STP/UH)",
        displayNameEn = "HRRR Significant Tornado / Updraft Helicity",
        shortNameJa = "HRRR 竜巻指数",
        shortNameEn = "HRRR Tornado/UH",
        descriptionJa = "スーパーセルや竜巻をもたらす回転上昇流ポテンシャルシミュレーション",
        descriptionEn = "Updraft helicity & significant tornado parameter simulation",
        resolution = "3km",
        updateIntervalMinutes = 60,
        associatedFilter = UsRadarLayerFilter.HRRR_FORECAST_ONLY
    ),
    HRRR_CLOUD_COVER(
        id = "hrrr_cloud",
        category = UsLayerCategory.HRRR_FORECAST,
        displayNameJa = "HRRR 雲量・低視程シミュレーション",
        displayNameEn = "HRRR Cloud Cover & Fog Simulation",
        shortNameJa = "HRRR 雲量・霧",
        shortNameEn = "HRRR Cloud/Fog",
        descriptionJa = "全層雲量および濃霧・低視程域の広域シミュレーション",
        descriptionEn = "Total cloud cover and low-visibility / fog field forecast",
        resolution = "3km",
        updateIntervalMinutes = 60,
        associatedFilter = UsRadarLayerFilter.HRRR_FORECAST_ONLY
    ),

    // 3. 直近外挿予測群
    NEXRAD_NOWCAST_EXTRAPOLATION(
        id = "nexrad_extrap",
        category = UsLayerCategory.NOWCAST,
        displayNameJa = "直近0分から60分先：NEXRAD外挿予測",
        displayNameEn = "NEXRAD 0-60 min Extrapolated Nowcast",
        shortNameJa = "NEXRAD 外挿予測",
        shortNameEn = "NEXRAD Nowcast",
        descriptionJa = "レーダーエコーの移動ベクトルによる60分先までの高精度短時間予測",
        descriptionEn = "Motion-vector extrapolated radar nowcast for next 60 minutes",
        resolution = "1km Seamless",
        updateIntervalMinutes = 5,
        associatedFilter = UsRadarLayerFilter.EXTRAPOLATION_ONLY
    ),
    NEXRAD_STORM_MOTION(
        id = "nexrad_motion",
        category = UsLayerCategory.NOWCAST,
        displayNameJa = "NEXRAD ストーム移動ベクトル・追跡",
        displayNameEn = "NEXRAD Storm Motion & Cell Tracking",
        shortNameJa = "ストーム追跡",
        shortNameEn = "Storm Tracking",
        descriptionJa = "発達した雷雨セルの移動方向・予想到達地点トラッキング",
        descriptionEn = "Thunderstorm cell tracking and movement vector forecast",
        resolution = "1km",
        updateIntervalMinutes = 5,
        associatedFilter = UsRadarLayerFilter.EXTRAPOLATION_ONLY
    ),

    // 4. 実況レーダー群
    MRMS_BASE_REFLECTIVITY(
        id = "mrms_refl",
        category = UsLayerCategory.OBSERVATION,
        displayNameJa = "MRMS 地上反射強度実況",
        displayNameEn = "MRMS Seamless Base Reflectivity",
        shortNameJa = "MRMS 地上反射実況",
        shortNameEn = "MRMS Base Refl",
        descriptionJa = "全米NEXRADレーダー合成による過去2時間の地上反射強度実況",
        descriptionEn = "Nationwide NEXRAD multi-sensor ground reflectivity observation",
        resolution = "1km Seamless",
        updateIntervalMinutes = 2,
        associatedFilter = UsRadarLayerFilter.OBSERVATION_ONLY
    ),
    MRMS_ECHO_TOPS(
        id = "mrms_echotops",
        category = UsLayerCategory.OBSERVATION,
        displayNameJa = "MRMS エコー頂高度 (積乱雲の高さ)",
        displayNameEn = "MRMS Echo Tops (18dBZ / 50dBZ)",
        shortNameJa = "エコー頂高度",
        shortNameEn = "Echo Tops",
        descriptionJa = "雨雲の雲頂高度・急激な積乱雲発達と雹・雷の危険度判定",
        descriptionEn = "Cloud top height monitoring rapid convective storm development",
        resolution = "1km",
        updateIntervalMinutes = 2,
        associatedFilter = UsRadarLayerFilter.OBSERVATION_ONLY
    ),
    MRMS_QPE_1H(
        id = "mrms_qpe_1h",
        category = UsLayerCategory.OBSERVATION,
        displayNameJa = "MRMS 1時間積算降水量実況",
        displayNameEn = "MRMS 1-Hour Radar QPE",
        shortNameJa = "MRMS 1時間降水量",
        shortNameEn = "MRMS 1h Precip",
        descriptionJa = "過去1時間のマルチセンサーレーダー推定積算降雨量",
        descriptionEn = "Multi-sensor radar quantitative precipitation estimation (1 hour)",
        resolution = "1km",
        updateIntervalMinutes = 5,
        associatedFilter = UsRadarLayerFilter.OBSERVATION_ONLY
    );

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> displayNameJa
        AppLanguage.ENGLISH -> displayNameEn
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            UNIFIED_ALL -> "综合时间线 (实况+外推+预报)"
            HRRR_COMPOSITE_REFL -> "HRRR 1km 模拟反射率预报"
            HRRR_PRECIP_TYPE -> "HRRR 降水相态预报 (雨/雪/雨夹雪/冻雨)"
            HRRR_TOTAL_PRECIP -> "HRRR 累计降水量预报 (未来18小时)"
            HRRR_WIND_GUSTS -> "HRRR 地面阵风与强风预报"
            HRRR_TORNADO_POTENTIAL -> "HRRR 强风暴与龙卷风指数 (STP/UH)"
            HRRR_CLOUD_COVER -> "HRRR 云量与低能见度模拟"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "近期0至60分钟：NEXRAD外推预测"
            NEXRAD_STORM_MOTION -> "NEXRAD 风暴移动矢量追踪"
            MRMS_BASE_REFLECTIVITY -> "MRMS 地面反射率实况"
            MRMS_ECHO_TOPS -> "MRMS 回波顶高 (积雨云高度)"
            MRMS_QPE_1H -> "MRMS 1小时累计降水实况"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            UNIFIED_ALL -> "綜合時間線 (實況+外推+預報)"
            HRRR_COMPOSITE_REFL -> "HRRR 1km 模擬反射率預報"
            HRRR_PRECIP_TYPE -> "HRRR 降水相態預報 (雨/雪/雨夾雪/凍雨)"
            HRRR_TOTAL_PRECIP -> "HRRR 累計降水量預報 (未來18小時)"
            HRRR_WIND_GUSTS -> "HRRR 地面陣風與強風預報"
            HRRR_TORNADO_POTENTIAL -> "HRRR 強風暴與龍捲風指數 (STP/UH)"
            HRRR_CLOUD_COVER -> "HRRR 雲量與低能見度模擬"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "近期0至60分鐘：NEXRAD外推預測"
            NEXRAD_STORM_MOTION -> "NEXRAD 風暴移動向量追蹤"
            MRMS_BASE_REFLECTIVITY -> "MRMS 地面反射率實況"
            MRMS_ECHO_TOPS -> "MRMS 回波頂高 (積雨雲高度)"
            MRMS_QPE_1H -> "MRMS 1小時累計降水實況"
        }
    }

    fun shortName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> shortNameJa
        AppLanguage.ENGLISH -> shortNameEn
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            UNIFIED_ALL -> "综合时间线"
            HRRR_COMPOSITE_REFL -> "HRRR模拟"
            HRRR_PRECIP_TYPE -> "HRRR降水相态"
            HRRR_TOTAL_PRECIP -> "HRRR降水量"
            HRRR_WIND_GUSTS -> "HRRR阵风"
            HRRR_TORNADO_POTENTIAL -> "HRRR龙卷风"
            HRRR_CLOUD_COVER -> "HRRR云量"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "NEXRAD外推"
            NEXRAD_STORM_MOTION -> "风暴追踪"
            MRMS_BASE_REFLECTIVITY -> "MRMS实况"
            MRMS_ECHO_TOPS -> "MRMS云顶"
            MRMS_QPE_1H -> "MRMS 1h降水"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            UNIFIED_ALL -> "綜合時間線"
            HRRR_COMPOSITE_REFL -> "HRRR模擬"
            HRRR_PRECIP_TYPE -> "HRRR降水相態"
            HRRR_TOTAL_PRECIP -> "HRRR降水量"
            HRRR_WIND_GUSTS -> "HRRR陣風"
            HRRR_TORNADO_POTENTIAL -> "HRRR龍捲風"
            HRRR_CLOUD_COVER -> "HRRR雲量"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "NEXRAD外推"
            NEXRAD_STORM_MOTION -> "風暴追蹤"
            MRMS_BASE_REFLECTIVITY -> "MRMS實況"
            MRMS_ECHO_TOPS -> "MRMS雲頂"
            MRMS_QPE_1H -> "MRMS 1h降水"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descriptionJa
        AppLanguage.ENGLISH -> descriptionEn
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            UNIFIED_ALL -> "过去2小时实况至18小时预报连续播放"
            HRRR_COMPOSITE_REFL -> "高分辨率数值预报模型地面反射率模拟"
            HRRR_PRECIP_TYPE -> "区分雨、雪、湿雪和冻雨的降水相态分布"
            HRRR_TOTAL_PRECIP -> "未来18小时地面积算降水总量模拟"
            HRRR_WIND_GUSTS -> "雷暴下击暴流与阵风强风场模拟"
            HRRR_TORNADO_POTENTIAL -> "超级单体与龙卷风旋转上升气流潜力模拟"
            HRRR_CLOUD_COVER -> "全层云量及浓雾、低能见度区域广域模拟"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "基于雷达回波移动矢量的60分钟高精度短临预测"
            NEXRAD_STORM_MOTION -> "发展中的强雷雨单体移动方向与预计到达追踪"
            MRMS_BASE_REFLECTIVITY -> "多雷达多传感器合成全国无缝基线反射率实况"
            MRMS_ECHO_TOPS -> "雷达回波顶高及积雨云垂直发展高度"
            MRMS_QPE_1H -> "地面自动雨量站校准后的高精度1小时定量降水"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            UNIFIED_ALL -> "過去2小時實況至18小時預報連續播放"
            HRRR_COMPOSITE_REFL -> "高解析度數值預報模型地面反射率模擬"
            HRRR_PRECIP_TYPE -> "區分雨、雪、濕雪和凍雨的降水相態分布"
            HRRR_TOTAL_PRECIP -> "未來18小時地面計算降水總量模擬"
            HRRR_WIND_GUSTS -> "雷暴下擊暴流與陣風強風場模擬"
            HRRR_TORNADO_POTENTIAL -> "超級單體與龍捲風旋轉上升氣流潛力模擬"
            HRRR_CLOUD_COVER -> "全層雲量及濃霧、低能見度區域廣域模擬"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "基於雷達回波移動向量的60分鐘高精度短臨預測"
            NEXRAD_STORM_MOTION -> "發展中的強雷雨單體移動方向與預計到達追蹤"
            MRMS_BASE_REFLECTIVITY -> "多雷達多感測器合成全國無縫基準反射率實況"
            MRMS_ECHO_TOPS -> "雷達回波頂高及積雨雲垂直發展高度"
            MRMS_QPE_1H -> "地面自動雨量站校準後的高精度1小時定量降水"
        }
    }
}

/**
 * タイムライン上で統一管理されるレーダーフレーム
 *
 * @param epochSeconds UTCエポック秒
 * @param isForecast true: 予測(HRRR) / false: 実況(MRMS)
 * @param frameType フレーム種別
 * @param tileUrlTemplate WebメルカトルXYZタイルURLテンプレート (例: https://.../{z}/{x}/{y}.png)
 * @param timeLabel 時刻表示 (例: "14:30")
 * @param displayLabel 画面表示用テキスト (例: "今日 14:30 実況", "今日 19:00 予測")
 * @param description 補足説明
 * @param intervalMinutes フレーム更新間隔（分）
 */
@Serializable
data class RadarTimelineFrame(
    val epochSeconds: Long,
    val isForecast: Boolean,
    val frameType: UsRadarFrameType,
    val tileUrlTemplate: String,
    val timeLabel: String,
    val displayLabel: String,
    val description: String = "",
    val intervalMinutes: Int = 5,
    val isExtrapolation: Boolean = (frameType == UsRadarFrameType.NEXRAD_NOWCAST_EXTRAPOLATION)
)

/**
 * レーダーの再生制御状態
 *
 * @param isPlaying 再生中フラグ
 * @param currentFrameIndex 現在選択されているタイムラインフレームのインデックス
 * @param alpha レーダータイルのアルファ値 (0.6f〜0.8f)
 * @param playbackSpeedMs コマ送り再生の間隔ミリ秒
 * @param isLooping 末尾到達時に先頭へ巻き戻すループ再生フラグ
 */
data class RadarPlaybackState(
    val isPlaying: Boolean = false,
    val currentFrameIndex: Int = 0,
    val alpha: Float = 0.72f,
    val playbackSpeedMs: Long = 650L,
    val isLooping: Boolean = true
) {
    init {
        // アルファ値を要件の 0.6〜0.8 に安全にクランプ
        require(alpha in 0.5f..1.0f) { "Alpha must be between 0.5 and 1.0" }
    }
}

/**
 * dBZカラー凡例の階調定義
 *
 * @param dbzValue dBZ基準値
 * @param label ラベル (例: "5", "15", "65+")
 * @param intensityName 降水強度名称 (例: "小雨", "豪雨", "猛烈な雷雨・雹")
 * @param color 凡例バー描画色
 * @param description 詳しい解説
 */
data class DbzColorLevel(
    val dbzValue: Int,
    val label: String,
    val intensityName: String,
    val color: Color,
    val description: String
) {
    fun intensityName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> intensityName
        AppLanguage.ENGLISH -> when (dbzValue) {
            5 -> "Faint / Drizzle"
            15 -> "Light Rain"
            25 -> "Moderate Rain"
            35 -> "Heavy Rain"
            45 -> "Very Heavy Rain"
            55 -> "Intense Downpour"
            else -> "Severe Storm / Hail"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (dbzValue) {
            5 -> "微弱/小雨"
            15 -> "小雨"
            25 -> "中雨"
            35 -> "大雨"
            45 -> "暴雨"
            55 -> "大暴雨"
            else -> "特大风暴/冰雹"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (dbzValue) {
            5 -> "微弱/小雨"
            15 -> "小雨"
            25 -> "中雨"
            35 -> "大雨"
            45 -> "暴雨"
            55 -> "大暴雨"
            else -> "特大風暴/冰雹"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> description
        AppLanguage.ENGLISH -> when (dbzValue) {
            5 -> "Extremely light drizzle / sprinkling (0.1-0.5 mm/h)"
            15 -> "Light rain requiring an umbrella (1-2 mm/h)"
            25 -> "Steady rain, feet get wet (2-5 mm/h)"
            35 -> "Pouring rain forming puddles (5-15 mm/h)"
            45 -> "Heavy driving rain with thunder (15-30 mm/h)"
            55 -> "Violent downpour, risk of localized flooding (30-60 mm/h)"
            else -> "Maximum reflectivity; high risk of supercells & large hail (60+ mm/h)"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (dbzValue) {
            5 -> "毛毛雨/极微弱小雨 (0.1~0.5 mm/h)"
            15 -> "需要撑伞的绵绵细雨 (1~2 mm/h)"
            25 -> "适度降雨，地面湿滑 (2~5 mm/h)"
            35 -> "哗哗作响的大雨，形成水洼 (5~15 mm/h)"
            45 -> "撑伞也会淋湿的暴雨伴雷电 (15~30 mm/h)"
            55 -> "倾盆暴雨，能见度骤降及路面积水风险 (30~60 mm/h)"
            else -> "最高级反射强度，极可能伴随超级单体与大冰雹 (60+ mm/h)"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (dbzValue) {
            5 -> "毛毛雨/極微弱小雨 (0.1~0.5 mm/h)"
            15 -> "需要撐傘的綿綿細雨 (1~2 mm/h)"
            25 -> "適度降雨，地面濕滑 (2~5 mm/h)"
            35 -> "嘩嘩作響的大雨，形成水窪 (5~15 mm/h)"
            45 -> "撐傘也會淋濕的暴雨伴雷電 (15~30 mm/h)"
            55 -> "傾盆暴雨，能見度驟降及路面積水風險 (30~60 mm/h)"
            else -> "最高級反射強度，極可能伴隨超級單體與大冰雹 (60+ mm/h)"
        }
    }
}

/**
 * NEXRAD / MRMS / HRRR 標準 dBZ カラーパレット (5dBZ 〜 65+dBZ)
 * 薄い青 (5dBZ) から 紫 (65dBZ以上) までのカラースケール
 */
val US_NEXRAD_DBZ_LEVELS = listOf(
    DbzColorLevel(
        dbzValue = 5,
        label = "5",
        intensityName = "微弱・霧雨",
        color = Color(0xFF64B5F6), // 薄い青
        description = "霧雨・パラつく程度の極めて弱い雨 (0.1〜0.5 mm/h)"
    ),
    DbzColorLevel(
        dbzValue = 15,
        label = "15",
        intensityName = "弱い雨",
        color = Color(0xFF00B0FF), // 水色
        description = "傘が必要となるしとしと降る雨 (1〜2 mm/h)"
    ),
    DbzColorLevel(
        dbzValue = 25,
        label = "25",
        intensityName = "並の雨",
        color = Color(0xFF00E676), // 緑
        description = "本降りの雨、足元が濡れる (2〜5 mm/h)"
    ),
    DbzColorLevel(
        dbzValue = 35,
        label = "35",
        intensityName = "強い雨",
        color = Color(0xFFFFEA00), // 黄色
        description = "水たまりができ、ザーザーと降る雨 (5〜15 mm/h)"
    ),
    DbzColorLevel(
        dbzValue = 45,
        label = "45",
        intensityName = "激しい雨",
        color = Color(0xFFFF9100), // オレンジ
        description = "傘をさしていても濡れる激しい雨・雷を伴う (15〜30 mm/h)"
    ),
    DbzColorLevel(
        dbzValue = 55,
        label = "55",
        intensityName = "非常に激しい雨",
        color = Color(0xFFFF1744), // 赤
        description = "土砂降りの豪雨、視界悪化・道路冠水の危険 (30〜60 mm/h)"
    ),
    DbzColorLevel(
        dbzValue = 65,
        label = "65+",
        intensityName = "猛烈な雷雨・雹",
        color = Color(0xFFD500F9), // 紫・マゼンタ
        description = "スーパーセル・大型雹・竜巻発生の可能性が高い最大級の反射強度 (60+ mm/h)"
    )
)

/**
 * アメリカ合衆国（本土 CONUS, アラスカ, ハワイ, プエルトリコ）の領域内かどうかを判定する
 */
fun isLocationInUnitedStates(latitude: Double, longitude: Double): Boolean {
    // 1. 米国本土 (CONUS: Continental United States)
    // 緯度: 約 24.0°N 〜 49.5°N, 経度: 約 -125.0°W 〜 -66.5°W
    if (latitude in 24.0..49.5 && longitude in -125.0..-66.5) {
        return true
    }

    // 2. アラスカ州 (Alaska)
    // 緯度: 約 51.0°N 〜 71.5°N, 経度: 約 -179.0°W 〜 -130.0°W または東経 172.0°E 〜 180.0°E (アリューシャン列島)
    if (latitude in 51.0..71.5) {
        if (longitude in -179.0..-130.0 || longitude in 172.0..180.0) {
            return true
        }
    }

    // 3. ハワイ州 (Hawaii)
    // 緯度: 約 18.5°N 〜 22.5°N, 経度: 約 -160.5°W 〜 -154.5°W
    if (latitude in 18.5..22.5 && longitude in -160.5..-154.5) {
        return true
    }

    // 4. プエルトリコおよび米領バージン諸島 (Puerto Rico / USVI)
    // 緯度: 約 17.5°N 〜 18.8°N, 経度: 約 -67.5°W 〜 -64.5°W
    if (latitude in 17.5..18.8 && longitude in -67.5..-64.5) {
        return true
    }

    return false
}

/**
 * 国名情報および座標からアメリカ国内かどうかを判定する
 */
fun isLocationOrCountryInUnitedStates(country: String?, latitude: Double, longitude: Double): Boolean {
    if (!country.isNullOrBlank()) {
        val trimmed = country.trim()
        val lower = trimmed.lowercase()
        if (trimmed.contains("アメリカ") ||
            trimmed.contains("米国") ||
            lower.contains("united states") ||
            lower.contains("usa") ||
            trimmed.equals("US", ignoreCase = true) ||
            lower.contains("puerto rico")
        ) {
            return true
        }
        // 他の国名が明示されており座標も米国範囲外なら海外
        if (!isLocationInUnitedStates(latitude, longitude)) {
            return false
        }
    }
    return isLocationInUnitedStates(latitude, longitude)
}
