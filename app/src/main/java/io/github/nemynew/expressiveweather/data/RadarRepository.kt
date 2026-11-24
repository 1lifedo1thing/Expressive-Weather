package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 雨雲レーダーの情報ソース設定モード
 */
enum class RadarSourceMode(
    val id: String,
    val titleJa: String,
    val titleEn: String,
    val titleKo: String,
    val titleZhHans: String,
    val titleZhHant: String,
    val titleEs: String,
    val titleFr: String,
    val titlePt: String,
    val descJa: String,
    val descEn: String,
    val descKo: String,
    val descZhHans: String,
    val descZhHant: String,
    val descEs: String,
    val descFr: String,
    val descPt: String
) {
    AUTO(
        "AUTO",
        "自動判定（日本：気象庁、米国：NEXRAD/HRRR、その他：RainViewer）",
        "Auto (JMA / US NEXRAD / RainViewer)",
        "자동 선택 (일본: JMA, 미국: NEXRAD/HRRR, 기타: RainViewer)",
        "自动判断（日本：气象厅，美国：NEXRAD/HRRR，其他：RainViewer）",
        "自動判斷（日本：氣象廳，美國：NEXRAD/HRRR，其他：RainViewer）",
        "Automático (JMA / NEXRAD EE. UU. / RainViewer)",
        "Auto (JMA / NEXRAD É.-U. / RainViewer)",
        "Automático (JMA / NEXRAD EUA / RainViewer)",
        "日本は気象庁、アメリカはNEXRAD/HRRR、その他はRainViewerを自動切替",
        "Uses JMA in Japan, NEXRAD/HRRR in US, RainViewer elsewhere",
        "일본은 기상청(JMA), 미국은 NEXRAD/HRRR, 그 외 지역은 RainViewer 자동 전환",
        "日本境内使用气象厅，美国境内使用NEXRAD/HRRR，其他地区使用RainViewer",
        "日本境內使用氣象廳，美國境內使用NEXRAD/HRRR，其他地區使用RainViewer",
        "Usa JMA en Japón, NEXRAD/HRRR en EE. UU. y RainViewer en el resto",
        "Utilise JMA au Japon, NEXRAD/HRRR aux É.-U. et RainViewer ailleurs",
        "Usa JMA no Japão, NEXRAD/HRRR nos EUA e RainViewer nas demais regiões"
    ),
    JMA(
        "JMA",
        "気象庁 高解像度降水ナウキャスト",
        "Japan Meteorological Agency (JMA)",
        "일본 기상청 고해상도 강수 나우캐스트",
        "日本气象厅 高分辨率降水短时预报",
        "日本氣象廳 高解析度降雨即時預報",
        "Agencia Meteorológica de Japón (JMA)",
        "Agence météorologique du Japon (JMA)",
        "Agência Meteorológica do Japão (JMA)",
        "高精度降水ナウキャスト",
        "High-resolution precipitation nowcast",
        "고정밀 강수 초단기 예보",
        "高精度降水短时预报",
        "高解析度降雨即時預報",
        "Predicción inmediata de precipitaciones en alta resolución",
        "Prévision immédiate des précipitations haute résolution",
        "Previsão imediata de precipitação em alta resolução"
    ),
    US_NEXRAD_HRRR(
        "US_NEXRAD_HRRR",
        "全米 NEXRAD / HRRR (実況・外挿予測・予報)",
        "US NEXRAD / HRRR (Obs, Nowcast, Forecast)",
        "미국 NEXRAD / HRRR (실황·초단기 예측·예보)",
        "全美 NEXRAD / HRRR (实况・外推预报・模拟预报)",
        "全美 NEXRAD / HRRR (實況・外推預報・模擬預報)",
        "EE. UU. NEXRAD / HRRR (Obs., Ahora, Pronóstico)",
        "É.-U. NEXRAD / HRRR (Obs., Immédiat, Prévision)",
        "EUA NEXRAD / HRRR (Obs., Agora, Previsão)",
        "NEXRAD/MRMS実況＋直近0-60分外挿予測＋HRRR予報シミュレーション",
        "NEXRAD/MRMS Obs + 0-60m Extrapolation + HRRR Simulation Forecast",
        "NEXRAD/MRMS 실황 + 0~60분 외삽 예측 + HRRR 수치 예보",
        "NEXRAD/MRMS实况＋0-60分外推预报＋HRRR模拟预报",
        "NEXRAD/MRMS實況＋0-60分外推預報＋HRRR模擬預報",
        "Observación NEXRAD/MRMS + extrapolación 0-60 min + simulación HRRR",
        "Observations NEXRAD/MRMS + extrapolation 0-60 min + simulation HRRR",
        "Observação NEXRAD/MRMS + extrapolação 0-60 min + simulação HRRR"
    ),
    RAIN_VIEWER(
        "RAIN_VIEWER",
        "世界レーダー (RainViewer)",
        "Global Radar (RainViewer)",
        "글로벌 레이더 (RainViewer)",
        "全球雷达 (RainViewer)",
        "全球雷達 (RainViewer)",
        "Radar global (RainViewer)",
        "Radar mondial (RainViewer)",
        "Radar global (RainViewer)",
        "世界中をカバーする広域降水レーダー",
        "Global radar coverage",
        "전 세계를 커버하는 광역 강수 레이더",
        "覆盖全球的广域气象雷达",
        "涵蓋全球的廣域氣象雷達",
        "Cobertura de radar de precipitaciones a nivel mundial",
        "Couverture radar des précipitations dans le monde entier",
        "Cobertura global de radar de precipitação"
    );

    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> titleJa
        AppLanguage.KOREAN -> titleKo
        AppLanguage.SIMPLIFIED_CHINESE -> titleZhHans
        AppLanguage.TRADITIONAL_CHINESE -> titleZhHant
        AppLanguage.SPANISH -> titleEs
        AppLanguage.FRENCH -> titleFr
        AppLanguage.PORTUGUESE -> titlePt
        AppLanguage.ENGLISH -> titleEn
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> descJa
        AppLanguage.KOREAN -> descKo
        AppLanguage.SIMPLIFIED_CHINESE -> descZhHans
        AppLanguage.TRADITIONAL_CHINESE -> descZhHant
        AppLanguage.SPANISH -> descEs
        AppLanguage.FRENCH -> descFr
        AppLanguage.PORTUGUESE -> descPt
        AppLanguage.ENGLISH -> descEn
    }

    companion object {
        fun fromId(id: String?): RadarSourceMode = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/**
 * 統合レーダーデータリポジトリ
 * ユーザー設定（AUTO / JMA / RAIN_VIEWER）および位置情報に応じて
 * 気象庁「高解像度降水ナウキャスト」または「RainViewer」を選択してデータを提供します。
 */
class RadarRepository(
    private val jmaRepo: JmaNowcastRepository = JmaNowcastRepository.getInstance(),
    private val rainViewerRepo: RainViewerRepository = RainViewerRepository.getInstance()
) {

    companion object {
        private const val TAG = "RadarRepository"
        private const val PREF_KEY_RADAR_SOURCE = "radar_source_mode"

        @Volatile
        private var INSTANCE: RadarRepository? = null

        fun getInstance(context: Context? = null): RadarRepository {
            val inst = INSTANCE ?: synchronized(this) {
                INSTANCE ?: RadarRepository().also { repo ->
                    INSTANCE = repo
                }
            }
            if (context != null && inst.appContext == null) {
                inst.initContext(context)
            }
            return inst
        }
    }

    @Volatile
    private var appContext: Context? = null
    private val _sourceModeFlow = MutableStateFlow(RadarSourceMode.AUTO)
    val sourceModeFlow: StateFlow<RadarSourceMode> = _sourceModeFlow.asStateFlow()

    fun initContext(context: Context) {
        if (this.appContext == null) {
            synchronized(this) {
                if (this.appContext == null) {
                    val appCtx = context.applicationContext
                    this.appContext = appCtx
                    val prefs = appCtx.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
                    val saved = prefs.getString(PREF_KEY_RADAR_SOURCE, null)
                    _sourceModeFlow.value = RadarSourceMode.fromId(saved)
                }
            }
        }
    }

    fun getSourceMode(): RadarSourceMode {
        val ctx = appContext ?: return _sourceModeFlow.value
        val prefs = ctx.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY_RADAR_SOURCE, null)
        val mode = RadarSourceMode.fromId(saved)
        _sourceModeFlow.value = mode
        return mode
    }

    fun setSourceMode(mode: RadarSourceMode) {
        _sourceModeFlow.value = mode
        appContext?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(PREF_KEY_RADAR_SOURCE, mode.id)
            ?.apply()
    }

    /**
     * 座標および設定に応じたレーダーデータを取得
     * @param latitude 緯度
     * @param longitude 経度
     * @param country 国名（オプション：都市データから取得可能な場合）
     * @param forceRefresh キャッシュを無視して強制取得
     */
    suspend fun getRadarData(
        latitude: Double,
        longitude: Double,
        country: String? = null,
        forceRefresh: Boolean = false,
        usFilter: UsRadarLayerFilter = UsRadarLayerFilter.ALL,
        usLayer: UsRadarLayerType = UsRadarLayerType.UNIFIED_ALL
    ): Pair<Result<RainViewerResponse>, RadarDataSource> {
        val mode = getSourceMode()
        val targetSource = RadarProviderRegistry.resolveProvider(mode, latitude, longitude, country)
        val usRepo = UsRadarRepository.getInstance()

        return when (targetSource) {
            RadarDataSource.US_NEXRAD_HRRR -> {
                Log.d(TAG, "Location (country=$country) routed to US_NEXRAD_HRRR (layer=${usLayer.id}).")
                val targetLayer = if (usLayer != UsRadarLayerType.UNIFIED_ALL) {
                    usLayer
                } else when (usFilter) {
                    UsRadarLayerFilter.ALL -> UsRadarLayerType.UNIFIED_ALL
                    UsRadarLayerFilter.OBSERVATION_ONLY -> UsRadarLayerType.MRMS_BASE_REFLECTIVITY
                    UsRadarLayerFilter.EXTRAPOLATION_ONLY -> UsRadarLayerType.NEXRAD_NOWCAST_EXTRAPOLATION
                    UsRadarLayerFilter.HRRR_FORECAST_ONLY -> UsRadarLayerType.HRRR_COMPOSITE_REFL
                }
                val framesResult = usRepo.getTimelineFrames(latitude, longitude, targetLayer, forceRefresh)
                val convertedResult = framesResult.map { frames -> usRepo.toRainViewerResponse(frames) }
                Pair(convertedResult, RadarDataSource.US_NEXRAD_HRRR)
            }
            RadarDataSource.JMA_NOWCAST -> {
                Log.d(TAG, "Location (country=$country) routed to JMA_NOWCAST.")
                val result = jmaRepo.getRadarData(forceRefresh)
                Pair(result, RadarDataSource.JMA_NOWCAST)
            }
            RadarDataSource.RAIN_VIEWER -> {
                Log.d(TAG, "Location (country=$country) routed to RAIN_VIEWER.")
                val result = rainViewerRepo.getRadarData(forceRefresh)
                Pair(result, RadarDataSource.RAIN_VIEWER)
            }
        }
    }

    /**
     * 気象庁の特定レイヤー（24種）のデータを取得
     */
    suspend fun getJmaLayerData(
        layerType: JmaLayerType,
        forceRefresh: Boolean = false
    ): Result<RainViewerResponse> {
        return jmaRepo.getLayerData(layerType, forceRefresh)
    }
}

