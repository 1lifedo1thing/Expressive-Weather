package io.github.nemynew.expressiveweather.data

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
    val titleKo: String,
    val descriptionJa: String,
    val descriptionEn: String,
    val descriptionKo: String
) {
    ALL(
        "all",
        "統合タイムライン (実況＋外挿＋予報)",
        "Unified Timeline (Obs + Nowcast + Forecast)",
        "통합 타임라인 (실황 + 외삽 + 예보)",
        "過去2時間実況から18時間予報までシームレス連続再生",
        "Seamless playback from 2h ago to 18h ahead",
        "2시간 전 실황부터 18시간 후 예보까지 연속 재생"
    ),
    OBSERVATION_ONLY(
        "obs",
        "実況レーダーマップ (NEXRAD / MRMS)",
        "Observation Radar (NEXRAD / MRMS)",
        "실황 레이더 지도 (NEXRAD / MRMS)",
        "地上反射強度の過去2時間高精度実況トラッキング",
        "High-resolution observation tracking for past 2 hours",
        "지난 2시간 고정밀 지상 반사도 실황 추적"
    ),
    EXTRAPOLATION_ONLY(
        "extrap",
        "直近0分から60分先：NEXRAD外挿予測",
        "Nowcast Extrapolation (0-60 min)",
        "0~60분 초단기 예측: NEXRAD 외삽 예측",
        "レーダーエコー移動ベクトルによる1時間短時間予測",
        "Motion-vector extrapolated nowcast for next 60 minutes",
        "레이더 에코 이동 벡터 기반 향후 60분 초단기 예측"
    ),
    HRRR_FORECAST_ONLY(
        "hrrr",
        "予報レーダーマップ (HRRRシミュレーション)",
        "Forecast Radar (HRRR Simulation)",
        "예보 레이더 지도 (HRRR 시뮬레이션)",
        "数値気象予測モデルによる18時間先までの広域シミュレーション",
        "Numerical weather prediction simulation up to 18h ahead",
        "수치기상예보 모델 기반 향후 18시간 광역 시뮬레이션"
    );

    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> titleJa
        AppLanguage.KOREAN -> titleKo
        AppLanguage.SPANISH -> when (this) {
            ALL -> "Línea temporal unificada (Obs. + Ahora + Pronóstico)"
            OBSERVATION_ONLY -> "Radar de observación (NEXRAD / MRMS)"
            EXTRAPOLATION_ONLY -> "Extrapolación inmediata (0-60 min)"
            HRRR_FORECAST_ONLY -> "Radar de pronóstico (Simulación HRRR)"
        }
        AppLanguage.FRENCH -> when (this) {
            ALL -> "Chronologie unifiée (Obs. + Immédiat + Prévision)"
            OBSERVATION_ONLY -> "Radar d'observation (NEXRAD / MRMS)"
            EXTRAPOLATION_ONLY -> "Extrapolation immédiate (0-60 min)"
            HRRR_FORECAST_ONLY -> "Radar de prévision (Simulation HRRR)"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            ALL -> "Linha do tempo unificada (Obs. + Agora + Previsão)"
            OBSERVATION_ONLY -> "Radar de observação (NEXRAD / MRMS)"
            EXTRAPOLATION_ONLY -> "Extrapolação imediata (0-60 min)"
            HRRR_FORECAST_ONLY -> "Radar de previsão (Simulação HRRR)"
        }
        else -> titleEn
    }
    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descriptionJa
        AppLanguage.KOREAN -> descriptionKo
        AppLanguage.SPANISH -> when (this) {
            ALL -> "Reproducción continua desde hace 2 h hasta 18 h adelante"
            OBSERVATION_ONLY -> "Seguimiento de observación en alta resolución de las últimas 2 h"
            EXTRAPOLATION_ONLY -> "Predicción inmediata por vectores de movimiento para los próximos 60 min"
            HRRR_FORECAST_ONLY -> "Simulación numérica meteorológica hasta 18 h adelante"
        }
        AppLanguage.FRENCH -> when (this) {
            ALL -> "Lecture continue de 2 h passées à 18 h à venir"
            OBSERVATION_ONLY -> "Suivi d'observation haute résolution des 2 dernières heures"
            EXTRAPOLATION_ONLY -> "Prévision immédiate par vecteurs de déplacement sur 60 min"
            HRRR_FORECAST_ONLY -> "Simulation de prévision numérique jusqu'à 18 h d'échéance"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            ALL -> "Reprodução contínua de 2 h atrás até 18 h à frente"
            OBSERVATION_ONLY -> "Acompanhamento de observação em alta resolução das últimas 2 h"
            EXTRAPOLATION_ONLY -> "Previsão imediata por vetores de deslocamento para os próximos 60 min"
            HRRR_FORECAST_ONLY -> "Simulação numérica meteorológica até 18 h à frente"
        }
        else -> descriptionEn
    }
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
        AppLanguage.KOREAN -> when (this) {
            UNIFIED -> "통합 타임라인"
            HRRR_FORECAST -> "HRRR 시뮬레이션 예보"
            NOWCAST -> "초단기 외삽 예측 (0~60분)"
            OBSERVATION -> "실황 레이더 지도"
        }
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
        AppLanguage.SPANISH -> when (this) {
            UNIFIED -> "Línea temporal unificada"
            HRRR_FORECAST -> "Pronóstico HRRR"
            NOWCAST -> "Extrapolación inmediata (0-60 min)"
            OBSERVATION -> "Radar de observación"
        }
        AppLanguage.FRENCH -> when (this) {
            UNIFIED -> "Chronologie unifiée"
            HRRR_FORECAST -> "Prévision HRRR"
            NOWCAST -> "Prévision immédiate (0-60 min)"
            OBSERVATION -> "Radar d'observation"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            UNIFIED -> "Linha do tempo unificada"
            HRRR_FORECAST -> "Previsão HRRR"
            NOWCAST -> "Extrapolação imediata (0-60 min)"
            OBSERVATION -> "Radar de observação"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descriptionJa
        AppLanguage.KOREAN -> when (this) {
            UNIFIED -> "실황·초단기 외삽 예측·HRRR 예보 연속 재생"
            HRRR_FORECAST -> "NOAA 고해상도 수치예보 모델 18시간 시뮬레이션"
            NOWCAST -> "NEXRAD 레이더 에코 이동 벡터 예측"
            OBSERVATION -> "미국 전역 NEXRAD/MRMS 다중 레이더 실황"
        }
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
        AppLanguage.SPANISH -> when (this) {
            UNIFIED -> "Reproducción continua de observaciones pasadas a pronóstico HRRR de 18 h"
            HRRR_FORECAST -> "Simulaciones de 18 h del modelo NOAA de alta resolución"
            NOWCAST -> "Predicción inmediata por vectores de eco radar NEXRAD (60 min)"
            OBSERVATION -> "Observaciones multirradar NEXRAD y MRMS en todo EE. UU."
        }
        AppLanguage.FRENCH -> when (this) {
            UNIFIED -> "Lecture continue des observations passées aux prévisions HRRR sur 18 h"
            HRRR_FORECAST -> "Simulations sur 18 h du modèle haute résolution NOAA HRRR"
            NOWCAST -> "Prévision immédiate par vecteurs d'échos radar NEXRAD (60 min)"
            OBSERVATION -> "Observations multiradar NEXRAD et MRMS à l'échelle nationale"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            UNIFIED -> "Reprodução contínua de observações passadas até previsão HRRR de 18 h"
            HRRR_FORECAST -> "Simulações de 18 h do modelo NOAA de alta resolução"
            NOWCAST -> "Previsão imediata por vetores de eco radar NEXRAD (60 min)"
            OBSERVATION -> "Observações multirradar NEXRAD e MRMS em todos os EUA"
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
        AppLanguage.KOREAN -> when (this) {
            UNIFIED_ALL -> "통합 타임라인 (실황+외삽+예보)"
            HRRR_COMPOSITE_REFL -> "HRRR 1km 모의 반사도 예보"
            HRRR_PRECIP_TYPE -> "HRRR 강수 형태 예보 (비·눈·진눈깨비·어는비)"
            HRRR_TOTAL_PRECIP -> "HRRR 누적 강수량 예보 (향후 18시간)"
            HRRR_WIND_GUSTS -> "HRRR 지상 돌풍·강풍 예보"
            HRRR_TORNADO_POTENTIAL -> "HRRR 악기상·토네이도 지수 (STP/UH)"
            HRRR_CLOUD_COVER -> "HRRR 운량·저시정 시뮬레이션"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "0~60분 초단기 예측: NEXRAD 외삽 예측"
            NEXRAD_STORM_MOTION -> "NEXRAD 스톰 이동 벡터·추적"
            MRMS_BASE_REFLECTIVITY -> "MRMS 지상 반사도 실황"
            MRMS_ECHO_TOPS -> "MRMS 에코 정고도 (적란운 높이)"
            MRMS_QPE_1H -> "MRMS 1시간 누적 강수량 실황"
        }
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
        AppLanguage.SPANISH -> when (this) {
            UNIFIED_ALL -> "Radar unificado (Obs. + Ahora + HRRR)"
            HRRR_COMPOSITE_REFL -> "Reflectividad simulada HRRR 1 km"
            HRRR_PRECIP_TYPE -> "Tipo de precipitación HRRR (Lluvia/Nieve/Hielo)"
            HRRR_TOTAL_PRECIP -> "Precipitación acumulada HRRR (18 h)"
            HRRR_WIND_GUSTS -> "Rachas de viento en superficie HRRR"
            HRRR_TORNADO_POTENTIAL -> "Índice de tornado y helicidad HRRR (STP/UH)"
            HRRR_CLOUD_COVER -> "Simulación de nubosidad y niebla HRRR"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "Extrapolación NEXRAD (0-60 min)"
            NEXRAD_STORM_MOTION -> "Movimiento y seguimiento de tormentas NEXRAD"
            MRMS_BASE_REFLECTIVITY -> "Reflectividad base continua MRMS"
            MRMS_ECHO_TOPS -> "Topes de eco MRMS (18 dBZ / 50 dBZ)"
            MRMS_QPE_1H -> "Precipitación radar MRMS en 1 hora (QPE)"
        }
        AppLanguage.FRENCH -> when (this) {
            UNIFIED_ALL -> "Radar unifié (Obs. + Immédiat + HRRR)"
            HRRR_COMPOSITE_REFL -> "Réflectivité simulée HRRR 1 km"
            HRRR_PRECIP_TYPE -> "Type de précipitations HRRR (Pluie/Neige/Verglas)"
            HRRR_TOTAL_PRECIP -> "Cumul de précipitations HRRR (18 h)"
            HRRR_WIND_GUSTS -> "Rafales de vent en surface HRRR"
            HRRR_TORNADO_POTENTIAL -> "Potentiel tornadique et hélicité HRRR (STP/UH)"
            HRRR_CLOUD_COVER -> "Simulation de nébulosité et brouillard HRRR"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "Extrapolation immédiate NEXRAD (0-60 min)"
            NEXRAD_STORM_MOTION -> "Déplacement et suivi des orages NEXRAD"
            MRMS_BASE_REFLECTIVITY -> "Réflectivité de base MRMS"
            MRMS_ECHO_TOPS -> "Sommets d'échos MRMS (18 dBZ / 50 dBZ)"
            MRMS_QPE_1H -> "Cumul radar MRMS sur 1 heure (QPE)"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            UNIFIED_ALL -> "Radar unificado (Obs. + Agora + HRRR)"
            HRRR_COMPOSITE_REFL -> "Refletividade simulada HRRR 1 km"
            HRRR_PRECIP_TYPE -> "Tipo de precipitação HRRR (Chuva/Neve/Gelo)"
            HRRR_TOTAL_PRECIP -> "Precipitação acumulada HRRR (18 h)"
            HRRR_WIND_GUSTS -> "Rajadas de vento na superfície HRRR"
            HRRR_TORNADO_POTENTIAL -> "Potencial de tornado e helicidade HRRR (STP/UH)"
            HRRR_CLOUD_COVER -> "Simulação de nebulosidade e nevoeiro HRRR"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "Extrapolação imediata NEXRAD (0-60 min)"
            NEXRAD_STORM_MOTION -> "Deslocamento e rastreio de tempestades NEXRAD"
            MRMS_BASE_REFLECTIVITY -> "Refletividade de base contínua MRMS"
            MRMS_ECHO_TOPS -> "Topos de eco MRMS (18 dBZ / 50 dBZ)"
            MRMS_QPE_1H -> "Precipitação radar MRMS em 1 hora (QPE)"
        }
    }

    fun shortName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> shortNameJa
        AppLanguage.KOREAN -> when (this) {
            UNIFIED_ALL -> "통합 레이더"
            HRRR_COMPOSITE_REFL -> "HRRR 모의 반사도"
            HRRR_PRECIP_TYPE -> "HRRR 강수 형태"
            HRRR_TOTAL_PRECIP -> "HRRR 누적 강수"
            HRRR_WIND_GUSTS -> "HRRR 돌풍"
            HRRR_TORNADO_POTENTIAL -> "HRRR 토네이도"
            HRRR_CLOUD_COVER -> "HRRR 운량·안개"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "NEXRAD 외삽"
            NEXRAD_STORM_MOTION -> "스톰 추적"
            MRMS_BASE_REFLECTIVITY -> "MRMS 실황"
            MRMS_ECHO_TOPS -> "에코 정고도"
            MRMS_QPE_1H -> "MRMS 1h 강수"
        }
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
        AppLanguage.SPANISH -> when (this) {
            UNIFIED_ALL -> "Unificado"
            HRRR_COMPOSITE_REFL -> "Reflectividad HRRR"
            HRRR_PRECIP_TYPE -> "Tipo precip. HRRR"
            HRRR_TOTAL_PRECIP -> "Acumulado HRRR"
            HRRR_WIND_GUSTS -> "Rachas HRRR"
            HRRR_TORNADO_POTENTIAL -> "Tornado/UH HRRR"
            HRRR_CLOUD_COVER -> "Nubes/Niebla HRRR"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "NEXRAD Ahora"
            NEXRAD_STORM_MOTION -> "Rastreo tormentas"
            MRMS_BASE_REFLECTIVITY -> "MRMS Base"
            MRMS_ECHO_TOPS -> "Topes de eco"
            MRMS_QPE_1H -> "MRMS 1 h"
        }
        AppLanguage.FRENCH -> when (this) {
            UNIFIED_ALL -> "Unifié"
            HRRR_COMPOSITE_REFL -> "Réflectivité HRRR"
            HRRR_PRECIP_TYPE -> "Type précip. HRRR"
            HRRR_TOTAL_PRECIP -> "Cumul HRRR"
            HRRR_WIND_GUSTS -> "Rafales HRRR"
            HRRR_TORNADO_POTENTIAL -> "Tornade/UH HRRR"
            HRRR_CLOUD_COVER -> "Nuages/Brouil. HRRR"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "NEXRAD Immédiat"
            NEXRAD_STORM_MOTION -> "Suivi orages"
            MRMS_BASE_REFLECTIVITY -> "MRMS Base"
            MRMS_ECHO_TOPS -> "Sommets d'échos"
            MRMS_QPE_1H -> "MRMS 1 h"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            UNIFIED_ALL -> "Unificado"
            HRRR_COMPOSITE_REFL -> "Refletividade HRRR"
            HRRR_PRECIP_TYPE -> "Tipo precip. HRRR"
            HRRR_TOTAL_PRECIP -> "Acumulado HRRR"
            HRRR_WIND_GUSTS -> "Rajadas HRRR"
            HRRR_TORNADO_POTENTIAL -> "Tornado/UH HRRR"
            HRRR_CLOUD_COVER -> "Nuvens/Nevoeiro HRRR"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "NEXRAD Agora"
            NEXRAD_STORM_MOTION -> "Rastreio tempest."
            MRMS_BASE_REFLECTIVITY -> "MRMS Base"
            MRMS_ECHO_TOPS -> "Topos de eco"
            MRMS_QPE_1H -> "MRMS 1 h"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descriptionJa
        AppLanguage.KOREAN -> when (this) {
            UNIFIED_ALL -> "2시간 전 실황부터 0~60분 외삽, 18시간 HRRR 예보까지 연속 재생"
            HRRR_COMPOSITE_REFL -> "지상 1km 강수·호우·뇌우 에코 모의 시뮬레이션 (최대 18시간)"
            HRRR_PRECIP_TYPE -> "비(초록)·눈(파랑)·진눈깨비(주황)·어는비(보라) 강수 유형 판별"
            HRRR_TOTAL_PRECIP -> "향후 1~18시간 예상 누적 총강수량 시뮬레이션"
            HRRR_WIND_GUSTS -> "뇌우 동반 하강기류 및 돌풍·강풍장 예측 시뮬레이션"
            HRRR_TORNADO_POTENTIAL -> "슈퍼셀 및 토네이도 유발 회전 상승기류 잠재력 시뮬레이션"
            HRRR_CLOUD_COVER -> "전층 운량 및 짙은 안개·저시정 구역 광역 시뮬레이션"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "레이더 에코 이동 벡터 기반 향후 60분 고정밀 초단기 예측"
            NEXRAD_STORM_MOTION -> "발달한 뇌우 셀의 이동 방향 및 예상 도달 지점 추적"
            MRMS_BASE_REFLECTIVITY -> "미국 전역 NEXRAD 다중 레이더 합성 지상 반사도 실황"
            MRMS_ECHO_TOPS -> "비구름 운정 고도 및 급격한 적란운 발달·우박·낙뢰 위험 감시"
            MRMS_QPE_1H -> "최근 1시간 다중 센서 레이더 추정 누적 강수량"
        }
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
        AppLanguage.SPANISH -> when (this) {
            UNIFIED_ALL -> "Reproducción continua de 2 h de observación a 18 h de pronóstico HRRR"
            HRRR_COMPOSITE_REFL -> "Pronóstico de reflectividad radar simulada a 1 km AGL hasta 18 h"
            HRRR_PRECIP_TYPE -> "Tipo de precipitación simulado: lluvia, nieve, aguanieve y lluvia helada"
            HRRR_TOTAL_PRECIP -> "Simulación de precipitación acumulada total en las próximas 18 horas"
            HRRR_WIND_GUSTS -> "Pronóstico de rachas de viento en superficie y microrráfagas"
            HRRR_TORNADO_POTENTIAL -> "Simulación de helicidad ascendente y parámetro de tornado significativo"
            HRRR_CLOUD_COVER -> "Pronóstico de nubosidad total, niebla y baja visibilidad"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "Predicción inmediata extrapolada por vectores para los próximos 60 min"
            NEXRAD_STORM_MOTION -> "Seguimiento de células tormentosas y pronóstico de trayectoria"
            MRMS_BASE_REFLECTIVITY -> "Observación nacional multisensor NEXRAD de reflectividad en superficie"
            MRMS_ECHO_TOPS -> "Altura de topes nubosos para vigilar el rápido desarrollo convectivo"
            MRMS_QPE_1H -> "Estimación cuantitativa de precipitación radar multisensor (1 hora)"
        }
        AppLanguage.FRENCH -> when (this) {
            UNIFIED_ALL -> "Lecture continue de 2 h d'observations à 18 h de prévisions HRRR"
            HRRR_COMPOSITE_REFL -> "Prévision de réflectivité radar simulée à 1 km AGL jusqu'à 18 h"
            HRRR_PRECIP_TYPE -> "Type de précipitations simulé : pluie, neige, grésil et pluie verglaçante"
            HRRR_TOTAL_PRECIP -> "Simulation du cumul total de précipitations sur les 18 prochaines heures"
            HRRR_WIND_GUSTS -> "Prévision des rafales de vent en surface et rafales descendantes"
            HRRR_TORNADO_POTENTIAL -> "Simulation d'hélicité ascendante et d'indice de tornade significative"
            HRRR_CLOUD_COVER -> "Prévision de nébulosité totale, brouillard et faible visibilité"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "Prévision immédiate par vecteurs de déplacement sur 60 minutes"
            NEXRAD_STORM_MOTION -> "Suivi des cellules orageuses et prévision de leur trajectoire"
            MRMS_BASE_REFLECTIVITY -> "Observation nationale multicapteur NEXRAD de la réflectivité au sol"
            MRMS_ECHO_TOPS -> "Hauteur des sommets nuageux pour suivre la convection rapide"
            MRMS_QPE_1H -> "Estimation quantitative radar multicapteur des précipitations (1 heure)"
        }
        AppLanguage.PORTUGUESE -> when (this) {
            UNIFIED_ALL -> "Reprodução contínua de 2 h de observação até 18 h de previsão HRRR"
            HRRR_COMPOSITE_REFL -> "Previsão de refletividade radar simulada a 1 km AGL até 18 h"
            HRRR_PRECIP_TYPE -> "Tipo de precipitação simulado: chuva, neve, granizo fino e chuva congelante"
            HRRR_TOTAL_PRECIP -> "Simulação de precipitação acumulada total nas próximas 18 horas"
            HRRR_WIND_GUSTS -> "Previsão de rajadas de vento na superfície e correntes descendentes"
            HRRR_TORNADO_POTENTIAL -> "Simulação de helicidade ascendente e parâmetro de tornado significativo"
            HRRR_CLOUD_COVER -> "Previsão de nebulosidade total, nevoeiro e baixa visibilidade"
            NEXRAD_NOWCAST_EXTRAPOLATION -> "Previsão imediata extrapolada por vetores para os próximos 60 min"
            NEXRAD_STORM_MOTION -> "Rastreamento de células de tempestade e previsão de trajetória"
            MRMS_BASE_REFLECTIVITY -> "Observação nacional multissensor NEXRAD de refletividade em superfície"
            MRMS_ECHO_TOPS -> "Altura dos topos de nuvens monitorando desenvolvimento convectivo rápido"
            MRMS_QPE_1H -> "Estimativa quantitativa de precipitação por radar multissensor (1 hora)"
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
        AppLanguage.KOREAN -> when (dbzValue) {
            5 -> "매우 약한 비·이슬비"
            15 -> "약한 비"
            25 -> "보통 비"
            35 -> "강한 비"
            45 -> "매우 강한 비"
            55 -> "폭우"
            else -> "극심한 뇌우·우박"
        }
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
        AppLanguage.SPANISH -> when (dbzValue) {
            5 -> "Débil / Llovizna"
            15 -> "Lluvia débil"
            25 -> "Lluvia moderada"
            35 -> "Lluvia fuerte"
            45 -> "Lluvia muy fuerte"
            55 -> "Aguacero intenso"
            else -> "Tormenta severa / Granizo"
        }
        AppLanguage.FRENCH -> when (dbzValue) {
            5 -> "Faible / Bruine"
            15 -> "Pluie faible"
            25 -> "Pluie modérée"
            35 -> "Pluie forte"
            45 -> "Pluie très forte"
            55 -> "Averse torrentielle"
            else -> "Orage violent / Grêle"
        }
        AppLanguage.PORTUGUESE -> when (dbzValue) {
            5 -> "Fraca / Garoa"
            15 -> "Chuva fraca"
            25 -> "Chuva moderada"
            35 -> "Chuva forte"
            45 -> "Chuva muito forte"
            55 -> "Aguaceiro intenso"
            else -> "Tempestade severa / Granizo"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> description
        AppLanguage.KOREAN -> when (dbzValue) {
            5 -> "빗방울이 떨어지는 매우 약한 이슬비 (0.1~0.5 mm/h)"
            15 -> "우산이 필요한 약한 비 (1~2 mm/h)"
            25 -> "꾸준히 내리며 발이 젖는 보통 비 (2~5 mm/h)"
            35 -> "물웅덩이가 생기는 강한 비 (5~15 mm/h)"
            45 -> "우산을 써도 젖는 매우 강한 비·천둥 동반 (15~30 mm/h)"
            55 -> "시야 악화 및 도로 침수 위험이 있는 폭우 (30~60 mm/h)"
            else -> "슈퍼셀·대형 우박·토네이도 위험이 높은 최대급 반사도 (60+ mm/h)"
        }
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
        AppLanguage.SPANISH -> when (dbzValue) {
            5 -> "Llovizna muy débil o gotas aisladas (0,1-0,5 mm/h)"
            15 -> "Lluvia ligera, se recomienda paraguas (1-2 mm/h)"
            25 -> "Lluvia constante que moja el calzado (2-5 mm/h)"
            35 -> "Lluvia fuerte que forma charcos (5-15 mm/h)"
            45 -> "Lluvia intensa y racheada con truenos (15-30 mm/h)"
            55 -> "Aguacero violento, riesgo de inundaciones locales (30-60 mm/h)"
            else -> "Reflectividad máxima; alto riesgo de supercélulas y granizo grande (60+ mm/h)"
        }
        AppLanguage.FRENCH -> when (dbzValue) {
            5 -> "Bruine très légère ou quelques gouttes (0,1-0,5 mm/h)"
            15 -> "Pluie fine nécessitant un parapluie (1-2 mm/h)"
            25 -> "Pluie soutenue, sol bien mouillé (2-5 mm/h)"
            35 -> "Forte pluie formant des flaques (5-15 mm/h)"
            45 -> "Pluie battante accompagnée de tonnerre (15-30 mm/h)"
            55 -> "Averse violente, risque d'inondations locales (30-60 mm/h)"
            else -> "Réflectivité maximale ; fort risque de supercellules et gros grêlons (60+ mm/h)"
        }
        AppLanguage.PORTUGUESE -> when (dbzValue) {
            5 -> "Garoa muito leve ou chuvisco isolado (0,1-0,5 mm/h)"
            15 -> "Chuva leve, recomenda-se guarda-chuva (1-2 mm/h)"
            25 -> "Chuva constante, piso encharcado (2-5 mm/h)"
            35 -> "Chuva forte formando poças (5-15 mm/h)"
            45 -> "Chuva intensa com trovoadas (15-30 mm/h)"
            55 -> "Aguaceiro violento, risco de alagamentos locais (30-60 mm/h)"
            else -> "Refletividade máxima; alto risco de supercélulas e granizo grande (60+ mm/h)"
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
        val mentionsUs = trimmed.contains("アメリカ") ||
            trimmed.contains("米国") ||
            trimmed.contains("美国") ||
            trimmed.contains("美國") ||
            trimmed.contains("미국") ||
            lower.contains("united states") ||
            lower.contains("estados unidos") ||
            lower.contains("états-unis") ||
            lower.contains("etats-unis") ||
            Regex("\\bu\\.?s\\.?a\\.?\\b").containsMatchIn(lower) ||
            trimmed.equals("US", ignoreCase = true) ||
            trimmed.equals("EUA", ignoreCase = true) ||
            trimmed.equals("EE.UU.", ignoreCase = true) ||
            trimmed.equals("EEUU", ignoreCase = true) ||
            lower.contains("puerto rico") ||
            lower.contains("porto rico")
        if (mentionsUs) return true

        val isExplicitNeighborOrOtherCountry = lower.contains("canada") ||
            lower.contains("canadá") ||
            trimmed.contains("カナダ") ||
            trimmed.contains("加拿大") ||
            trimmed.contains("캐나다") ||
            trimmed.equals("CA", ignoreCase = true) ||
            lower.contains("mexico") ||
            lower.contains("méxico") ||
            lower.contains("mexique") ||
            trimmed.contains("メキシコ") ||
            trimmed.contains("墨西哥") ||
            trimmed.contains("멕시코") ||
            trimmed.equals("MX", ignoreCase = true) ||
            lower.contains("cuba") ||
            lower.contains("bahamas") ||
            lower.contains("bermuda") ||
            lower.contains("russia") ||
            lower.contains("rusia") ||
            lower.contains("russie") ||
            lower.contains("rússia") ||
            trimmed.contains("ロシア") ||
            trimmed.contains("俄罗斯") ||
            trimmed.contains("俄羅斯") ||
            trimmed.contains("러시아") ||
            trimmed.equals("RU", ignoreCase = true)
        if (isExplicitNeighborOrOtherCountry) return false
    }
    return isLocationInUnitedStates(latitude, longitude)
}
