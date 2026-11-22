package io.github.nemynew.expressiveweather.data

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    JAPANESE("ja", "日本語"),
    SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
    TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
    ENGLISH("en", "English");

    val strings: AppStrings
        get() = when (this) {
            JAPANESE -> JapaneseStrings
            SIMPLIFIED_CHINESE -> SimplifiedChineseStrings
            TRADITIONAL_CHINESE -> TraditionalChineseStrings
            ENGLISH -> EnglishStrings
        }

    val locale: java.util.Locale
        get() = when (this) {
            JAPANESE -> java.util.Locale.JAPAN
            SIMPLIFIED_CHINESE -> java.util.Locale.SIMPLIFIED_CHINESE
            TRADITIONAL_CHINESE -> java.util.Locale.TRADITIONAL_CHINESE
            ENGLISH -> java.util.Locale.US
        }

    val composeLocale: androidx.compose.ui.text.intl.Locale
        get() = androidx.compose.ui.text.intl.Locale(this.code)

    val composeLocaleList: androidx.compose.ui.text.intl.LocaleList
        get() = androidx.compose.ui.text.intl.LocaleList(this.composeLocale)


    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true) ||
                (code.startsWith("zh-Hans", ignoreCase = true) && it == SIMPLIFIED_CHINESE) ||
                (code.startsWith("zh-Hant", ignoreCase = true) && it == TRADITIONAL_CHINESE) ||
                (code.equals("zh", ignoreCase = true) && it == SIMPLIFIED_CHINESE) ||
                (code.startsWith("zh-CN", ignoreCase = true) && it == SIMPLIFIED_CHINESE) ||
                (code.startsWith("zh-SG", ignoreCase = true) && it == SIMPLIFIED_CHINESE) ||
                (code.startsWith("zh-TW", ignoreCase = true) && it == TRADITIONAL_CHINESE) ||
                (code.startsWith("zh-HK", ignoreCase = true) && it == TRADITIONAL_CHINESE) ||
                (code.startsWith("zh-MO", ignoreCase = true) && it == TRADITIONAL_CHINESE) ||
                (code.startsWith("ja", ignoreCase = true) && it == JAPANESE) ||
                (code.startsWith("en", ignoreCase = true) && it == ENGLISH)
            } ?: JAPANESE
        }
    }
}

data class AppStrings(
    val appName: String,
    // ツールバー・地名選択
    val selectLocation: String,
    val currentLocation: String,
    val currentLocationWithCity: (String) -> String,
    val savedLocations: String,
    val savedCities: String = savedLocations,
    val addLocation: String = "地名を追加",
    val currentlyViewing: String = "現在表示中",
    val noSavedLocations: String,
    val recentSearches: String,
    val searchCityPlaceholder: String,
    val searchingCities: String,
    val noCitiesFound: String,
    val close: String,
    val cancel: String,
    val removeSaved: String,
    val selected: String,

    // ヘッダー・メイン
    val feelsLike: (Int) -> String,
    val high: (Int) -> String,
    val low: (Int) -> String,
    val updated: (String) -> String,
    val justNow: String,
    val minutesAgo: (Int) -> String,
    val hoursAgo: (Int) -> String,
    val daysAgo: (Int) -> String,
    val cachedDataBadge: (String) -> String,
    val weatherDataSource: String,
    val lastUpdatedRelative: (time: String, relative: String) -> String,
    val cachedDataRelative: (time: String, relative: String) -> String,
    val pullToRefreshHint: String,
    val hourlyForecast: String,
    val tapForDetails: String,
    val now: String,
    val sevenDayForecast: String,
    val today: String,
    val tomorrow: String,

    // ベントグリッド タイル名
    val wind: String,
    val uvIndex: String,
    val airQuality: String,
    val humidity: String,
    val sun: String,
    val pressure: String,
    val visibility: String,
    val precipitation: String,
    val dailyPrecipitationSum: String = "降水量",
    val todayTotalPrecip: String = "24時間合計",
    val feelsLikeCard: String,

    // ベントグリッド 詳細
    val windGusts: (Double) -> String,
    val uvDailyMax: (Int) -> String,
    val uvAdvice: String,
    val uvLevelLow: String,
    val uvLevelModerate: String,
    val uvLevelHigh: String,
    val uvLevelVeryHigh: String,
    val uvLevelExtreme: String,

    val aqiLevelGood: String,
    val aqiLevelModerate: String,
    val aqiLevelSensitive: String,
    val aqiLevelUnhealthy: String,
    val aqiLevelHazardous: String,

    val humidityDry: String,
    val humidityComfortable: String,
    val humidityHumid: String,
    val dewPoint: (Int) -> String,

    val sunrise: String,
    val sunset: String,
    val daylightDuration: (String) -> String,

    val pressureLow: String,
    val pressureNormal: String,
    val pressureHigh: String,

    val visibilityClear: String,
    val visibilityGood: String,
    val visibilityMisty: String,
    val visibilityFoggy: String,
    val visibilityExcellent: String,
    val visibilityModerate: String,
    val visibilityPoor: String,

    val maxPrecipChance: (Int) -> String,
    val noPrecipitation: String,
    val rainPrecip: String,
    val snowPrecip: String,

    val tempDiff: (Int) -> String,
    val tempSame: String,
    val feelsHot: String,
    val feelsWarm: String,
    val feelsComfortable: String,
    val feelsCool: String,
    val feelsCold: String,
    val feelsChilly: String,

    // 風詳細画面
    val windDetailsTitle: String,
    val windSpeedAndDirection: String,
    val windDirectionLabel: String,
    val windSpeedLabel: String,
    val windGustsLabel: String,
    val windGustsMultiplier: (Double) -> String,
    val hourlyWindTitle: String,
    val dailyWindTitle: String,
    val beaufortScaleTitle: String,
    val beaufortScaleLevel: (Int) -> String,
    val lifeInsightsTitle: String,
    val laundryTitle: String,
    val umbrellaTitle: String,
    val cyclingTitle: String,
    val outdoorTitle: String,
    val peakWindNotice: (String, Double) -> String,
    val dominantWind: String,
    val maxWind: String,
    val maxGusts: String,

    // 今日の天気詳細画面
    val todayWeatherTitle: String,
    val todayOverview: String,
    val interactive24hChartTitle: String,
    val interactive24hChartSubtitle: String,
    val touchScrubberGuide: String,
    val precipitationAndProbability: String,
    val daypartsTitle: String,
    val morningTitle: String,
    val afternoonTitle: String,
    val eveningTitle: String,
    val overnightTitle: String,
    val lifestyleAdvisorTitle: String,
    val clothingTitle: String,
    val clothingTip: (Int) -> String,
    val umbrellaAdviceTitle: String,
    val umbrellaTip: (Int, Double) -> String,
    val laundryAdviceTitle: String,
    val laundryTip: (Int, Int) -> String,
    val uvAdviceTitle: String,
    val uvTip: (Int) -> String,
    val ventilationAdviceTitle: String,
    val ventilationTip: (Int, Int) -> String,
    val detailedMetricsTitle: String,
    val dewPointLabel: String,
    val sunScheduleTitle: String,
    val sunRemaining: (String) -> String,
    val daylightLabel: String,

    // 設定
    val settingsTitle: String,
    val settingsSubtitle: String,
    val tempUnitTitle: String,
    val tempUnitSubtitle: (String, Boolean) -> String,
    val windSpeedUnitTitle: String,
    val windSpeedUnitSubtitle: (String) -> String,
    val windSpeedMatchLanguage: String,
    val refreshTitle: String,
    val refreshSubtitle: (Boolean) -> String,
    val resetLocationTitle: String,
    val resetLocationSubtitle: String,
    val languageTitle: String,
    val languageSubtitle: String,
    val changeLanguageTitle: String = "言語を変更",
    val currentLanguageSection: String = "現在の言語",
    val availableLanguagesSection: String = "設定可能な言語",
    val weatherThemeTitle: String,
    val weatherThemeSubtitle: (Boolean) -> String,
    val themePresetTitle: String,
    val themePresetSubtitle: (String, Boolean) -> String,
    val themePresetDialogTitle: String,
    val themePresetDialogSubtitle: String,
    val appearanceTitle: String,
    val appearanceSubtitle: String,
    val generalSettingsTitle: String,
    val generalSettingsSubtitle: String,
    val dataSourcesTitle: String,
    val dataSourcesSubtitle: String,
    val dataSourcesSectionTitle: String,
    val dataAndSyncTitle: String,
    val aboutTitle: String,
    val syncNowTitle: String,
    val syncNowSubtitle: String,
    val clearCacheConfirmMessage: String,
    val dataSourcesOpenMeteoDesc: String,
    val dataSourcesFutureNotice: String,
    val sourceActiveBadge: String,
    val sourceComingSoonBadge: String,
    val sourceJmaTitle: String,
    val sourceJmaDesc: String,
    val sourceEcmwfTitle: String,
    val sourceEcmwfDesc: String,
    val sourceNoaaTitle: String,
    val sourceNoaaDesc: String,
    val appVersionTitle: String,
    val appVersionValue: String,
    val debugSettingsTitle: String,
    val debugSettingsSubtitle: String,
    val debugSimulationTitle: String,
    val debugSimulationSubtitle: String,
    val backgroundPreviewTitle: String,
    val backgroundPreviewSubtitle: String,
    val debugResetPreview: String,
    val clearCacheTitle: String,
    val clearCacheSubtitle: String,
    val clearCacheSuccess: String,
    val forceRefreshTitle: String,
    val forceRefreshSubtitle: String,
    val systemDiagnosticsTitle: String,
    val tabAll: String,
    val tabAppearance: String,
    val tabGeneral: String,
    val tabSources: String,
    val tabDebug: String,

    // 読み込み中・エラー
    val loadingWeather: String,
    val loadingLocation: String,
    val errorOccurred: String,
    val retry: String
)

val JapaneseStrings = AppStrings(
    appName = "Expressive Weather",
    selectLocation = "地名の選択",
    currentLocation = "現在地",
    currentLocationWithCity = { city -> "現在地 ($city)" },
    savedLocations = "登録した地名",
    addLocation = "地名を追加",
    currentlyViewing = "現在表示中",
    noSavedLocations = "登録された地名はありません",
    recentSearches = "最近検索した項目",
    searchCityPlaceholder = "都市名を入力...",
    searchingCities = "都市を検索中...",
    noCitiesFound = "該当する都市が見つかりません",
    close = "閉じる",
    cancel = "キャンセル",
    removeSaved = "登録解除",
    selected = "選択中",

    feelsLike = { temp -> "体感 $temp°" },
    high = { temp -> "最高 $temp°" },
    low = { temp -> "最低 $temp°" },
    updated = { time -> "更新: $time" },
    justNow = "たった今",
    minutesAgo = { mins -> "${mins}分前" },
    hoursAgo = { hours -> "${hours}時間前" },
    daysAgo = { days -> "${days}日前" },
    cachedDataBadge = { relative -> "キャッシュ: $relative" },
    weatherDataSource = "気象データ提供元: Open-Meteo",
    lastUpdatedRelative = { time, relative -> "更新: $time（$relative）" },
    cachedDataRelative = { time, relative -> "キャッシュ: $time（$relative）" },
    pullToRefreshHint = "タップして更新",
    hourlyForecast = "時間ごとの天気",
    tapForDetails = "タップして詳細",
    now = "現在",
    sevenDayForecast = "7日間の週間予報",
    today = "今日",
    tomorrow = "明日",

    wind = "風",
    uvIndex = "紫外線 (UV)",
    airQuality = "空気質",
    humidity = "湿度",
    sun = "太陽",
    pressure = "気圧",
    visibility = "視程",
    precipitation = "降水量",
    dailyPrecipitationSum = "降水量",
    todayTotalPrecip = "24時間合計",
    feelsLikeCard = "体感温度",

    windGusts = { gusts -> String.format(java.util.Locale.US, "突風: %.1f m/s", gusts) },
    uvDailyMax = { max -> "本日最大 UV $max" },
    uvAdvice = "日傘やUVクリーム推奨",
    uvLevelLow = "低い",
    uvLevelModerate = "普通",
    uvLevelHigh = "強い",
    uvLevelVeryHigh = "非常に強い",
    uvLevelExtreme = "極端に強い",

    aqiLevelGood = "良い",
    aqiLevelModerate = "普通",
    aqiLevelSensitive = "敏感な人に影響",
    aqiLevelUnhealthy = "健康に悪い",
    aqiLevelHazardous = "極めて悪い",

    humidityDry = "乾燥",
    humidityComfortable = "快適",
    humidityHumid = "多湿",
    dewPoint = { dp -> "露点 $dp°C" },

    sunrise = "日の出",
    sunset = "日の入り",
    daylightDuration = { duration -> "日照時間 $duration" },

    pressureLow = "低気圧",
    pressureNormal = "標準気圧",
    pressureHigh = "高気圧",

    visibilityClear = "明瞭",
    visibilityGood = "普通",
    visibilityMisty = "靄",
    visibilityFoggy = "霧",
    visibilityExcellent = "見通し良好",
    visibilityModerate = "視界良好",
    visibilityPoor = "視界不良",

    maxPrecipChance = { prob -> "最大確率 $prob%" },
    noPrecipitation = "現在降水なし",
    rainPrecip = "雨",
    snowPrecip = "降雪",

    tempDiff = { diff -> if (diff >= 0) "気温比 +$diff°" else "気温比 $diff°" },
    tempSame = "気温と同じ",
    feelsHot = "暑い",
    feelsWarm = "暖かい",
    feelsComfortable = "快適",
    feelsCool = "涼しい",
    feelsCold = "寒い",
    feelsChilly = "肌寒く感じる",

    windDetailsTitle = "風の詳細",
    windSpeedAndDirection = "風速と風向",
    windDirectionLabel = "風向",
    windSpeedLabel = "平均風速",
    windGustsLabel = "最大瞬間風速 (突風)",
    windGustsMultiplier = { mult -> "平均の %.1f 倍".format(mult) },
    hourlyWindTitle = "24時間の風予報",
    dailyWindTitle = "7日間の週間風予報",
    beaufortScaleTitle = "風力階級",
    beaufortScaleLevel = { lvl -> "風力 $lvl" },
    lifeInsightsTitle = "生活・行動への影響",
    laundryTitle = "洗濯物",
    umbrellaTitle = "傘の使用",
    cyclingTitle = "自転車・二輪",
    outdoorTitle = "換気・アウトドア",
    peakWindNotice = { time, spd -> "$time 頃に最大突風 %.1f m/s を予想".format(spd) },
    dominantWind = "卓越風向",
    maxWind = "最大風速",
    maxGusts = "最大突風",

    // 今日の天気詳細画面
    todayWeatherTitle = "今日の詳細天気",
    todayOverview = "本日の空模様と推移",
    interactive24hChartTitle = "タイムライン",
    interactive24hChartSubtitle = "グラフをなぞって詳細を確認",
    touchScrubberGuide = "タップまたはドラッグで時間帯ごとの詳細値を表示",
    precipitationAndProbability = "降水確率・降水量",
    daypartsTitle = "時間帯ごとのステージ分析",
    morningTitle = "朝（6:00 - 12:00）",
    afternoonTitle = "昼（12:00 - 18:00）",
    eveningTitle = "夕方〜夜（18:00 - 24:00）",
    overnightTitle = "深夜〜早朝（0:00 - 6:00）",
    lifestyleAdvisorTitle = "生活指数",
    clothingTitle = "服装指数",
    clothingTip = { temp ->
        when {
            temp >= 26 -> "半袖で涼しく過ごせます。通気性の良い服装を推奨"
            temp >= 20 -> "長袖シャツやカットソーが最適。羽織るものがあると安心"
            temp >= 15 -> "セーターやカーディガン、薄手の上着がおすすめ"
            temp >= 10 -> "ジャケットやトレンチコートなどの上着が必要です"
            else -> "冬物コートやマフラーでしっかり防寒対策を"
        }
    },
    umbrellaAdviceTitle = "傘指数",
    umbrellaTip = { prob, mm ->
        when {
            prob >= 60 || mm >= 2.0 -> "しっかりした雨具や長傘を持ってお出かけください"
            prob >= 30 || mm > 0.0 -> "折りたたみ傘をバッグに入れておくと安心です"
            else -> "傘の心配は不要です。身軽にお出かけできます"
        }
    },
    laundryAdviceTitle = "お洗濯指数",
    laundryTip = { humidity, rainProb ->
        when {
            rainProb >= 40 -> "雨の心配があるため部屋干しを強く推奨します"
            humidity <= 50 -> "速乾！絶好の洗濯日和。厚手の衣類も気持ちよく乾きます"
            humidity <= 70 -> "外干し可能。午後の急な天候変化にはご注意ください"
            else -> "湿度が高めです。部屋干しまたは乾燥機の併用がおすすめ"
        }
    },
    uvAdviceTitle = "紫外線指数",
    uvTip = { uv ->
        when {
            uv >= 8 -> "極めて強い紫外線。日傘、サングラス、日焼け止めが必須です"
            uv >= 6 -> "紫外線が強めです。日陰を利用し日焼け対策をしっかりと"
            uv >= 3 -> "中程度の日差し。長時間の外出時はUV対策を推奨"
            else -> "紫外線は穏やかです。特別な対策は不要です"
        }
    },
    ventilationAdviceTitle = "換気・快適指数",
    ventilationTip = { humidity, aqi ->
        when {
            aqi > 100 -> "大気環境に注意。短時間の換気にとどめることを推奨します"
            humidity in 40..60 -> "快適な空気感。心地よい外気を取り入れる絶好のタイミング"
            humidity > 70 -> "外気の湿度が高めです。換気後のエアコン除湿が効果的"
            else -> "空気がやや乾燥しています。加湿器の活用を検討してください"
        }
    },
    detailedMetricsTitle = "詳細情報",
    dewPointLabel = "露点温度",
    sunScheduleTitle = "日の出・日の入り",
    sunRemaining = { remain -> "日没まであと $remain" },
    daylightLabel = "昼の長さ",

    settingsTitle = "設定",
    settingsSubtitle = "環境設定とシステム調整",
    tempUnitTitle = "温度単位",
    tempUnitSubtitle = { symbol, isC -> "現在: $symbol (${if (isC) "摂氏" else "華氏"})" },
    windSpeedUnitTitle = "風速の単位",
    windSpeedUnitSubtitle = { unit -> "現在: $unit" },
    windSpeedMatchLanguage = "言語に合わせる",
    refreshTitle = "気象データを更新",
    refreshSubtitle = { isRef -> if (isRef) "最新データを受信中..." else "現在の観測情報と予報を同期" },
    resetLocationTitle = "現在地に戻す",
    resetLocationSubtitle = "GPS現在地またはデフォルト都市に設定",
    languageTitle = "言語設定",
    languageSubtitle = "アプリの表示言語を切り替え",
    changeLanguageTitle = "言語を変更",
    currentLanguageSection = "現在の言語",
    availableLanguagesSection = "設定可能な言語",
    weatherThemeTitle = "天気に連動したテーマカラー",
    weatherThemeSubtitle = { enabled -> if (enabled) "現在の天候に合わせてアプリ全体の色調を自動調整中" else "標準のテーマカラーを使用中" },
    themePresetTitle = "テーマカラー・プリセット",
    themePresetSubtitle = { name, isAdaptive -> if (isAdaptive) "天候に合わせて自動変化中 ($name)" else "$name に固定中" },
    themePresetDialogTitle = "テーマカラーの選択",
    themePresetDialogSubtitle = "好みのカラーを選択して固定、または天候連動に設定できます",
    appearanceTitle = "外観設定",
    appearanceSubtitle = "テーマカラー設定",
    generalSettingsTitle = "一般設定",
    generalSettingsSubtitle = "言語、単位設定（温度・風速）",
    dataSourcesTitle = "情報ソース設定",
    dataSourcesSubtitle = "気象データ提供元の確認と将来のソース切り替え",
    dataSourcesSectionTitle = "気象データ提供元",
    dataAndSyncTitle = "気象データと同期",
    aboutTitle = "アプリ情報",
    syncNowTitle = "気象データを今すぐ更新",
    syncNowSubtitle = "最新の観測値と予報データを即時取得します",
    clearCacheConfirmMessage = "保存されているオフライン気象データを消去します。次回の更新時に最新データを再取得します。",
    dataSourcesOpenMeteoDesc = "気象庁 (JMA) ＆ Open-Meteo\n日本国内の気象情報は気象庁公式データ（全国約1,300地点のアメダス実測値・気象台発表の天気予報・概況解説）を直接取得しています。日本国外の地点は全球対応のOpen-Meteo APIを使用しています。",
    dataSourcesFutureNotice = "国内地点は気象庁公式サーバーから10分毎のアメダス観測確定値および気象台発表の公式予報を直接取得しています。",
    sourceActiveBadge = "使用中",
    sourceComingSoonBadge = "近日対応",
    sourceJmaTitle = "気象庁 (JMA)",
    sourceJmaDesc = "日本国内の局地予報・高解像度降水ナウキャスト・メソ数値予報モデル (MSM)。",
    sourceEcmwfTitle = "ECMWF (欧州中期気象予報センター)",
    sourceEcmwfDesc = "世界最高峰の全球数値予報モデル (IFS)。中期予測精度に優れます。",
    sourceNoaaTitle = "NOAA GFS (米国海洋大気庁)",
    sourceNoaaDesc = "米国気象局運用の全球気象予報システム。",
    appVersionTitle = "バージョン",
    appVersionValue = "1.0 (Expressive Edition)",
    debugSettingsTitle = "デバッグ設定",
    debugSettingsSubtitle = "天候アニメーションシミュレーション、キャッシュ管理",
    debugSimulationTitle = "天候アニメーションシミュレーション",
    debugSimulationSubtitle = "各天候の背景グラデーション、雲、降水、雷などのアニメーションをテスト表示",
    backgroundPreviewTitle = "天候アニメーションシミュレーション",
    backgroundPreviewSubtitle = "各天候の背景グラデーション、雲、降水、雷などのアニメーションをテスト表示",
    debugResetPreview = "実際の天候に戻す",
    clearCacheTitle = "キャッシュを消去",
    clearCacheSubtitle = "保存されたオフライン気象データと一時キャッシュをクリア",
    clearCacheSuccess = "キャッシュを消去しました",
    forceRefreshTitle = "強制再同期",
    forceRefreshSubtitle = "APIサーバーから最新データを直接再取得",
    systemDiagnosticsTitle = "システム診断情報",
    tabAll = "すべて",
    tabAppearance = "外観",
    tabGeneral = "一般",
    tabSources = "情報ソース",
    tabDebug = "デバッグ",

    loadingWeather = "気象データを取得中...",
    loadingLocation = "現在地を測位中...",
    errorOccurred = "エラーが発生しました",
    retry = "再試行"
)

val EnglishStrings = AppStrings(
    appName = "Expressive Weather",
    selectLocation = "Select Location",
    currentLocation = "Current Location",
    currentLocationWithCity = { city -> "Current Location ($city)" },
    savedLocations = "Saved Locations",
    addLocation = "Add Location",
    currentlyViewing = "Currently Viewing",
    noSavedLocations = "No saved locations",
    recentSearches = "Recent Searches",
    searchCityPlaceholder = "Search cities...",
    searchingCities = "Searching cities...",
    noCitiesFound = "No cities found",
    close = "Close",
    cancel = "Cancel",
    removeSaved = "Remove",
    selected = "Selected",

    feelsLike = { temp -> "Feels like $temp°" },
    high = { temp -> "H: $temp°" },
    low = { temp -> "L: $temp°" },
    updated = { time -> "Updated: $time" },
    justNow = "Just now",
    minutesAgo = { mins -> "${mins}m ago" },
    hoursAgo = { hours -> "${hours}h ago" },
    daysAgo = { days -> "${days}d ago" },
    cachedDataBadge = { relative -> "Cached: $relative" },
    weatherDataSource = "Weather data by Open-Meteo",
    lastUpdatedRelative = { time, relative -> "Updated: $time ($relative)" },
    cachedDataRelative = { time, relative -> "Cached: $time ($relative)" },
    pullToRefreshHint = "Tap to refresh",
    hourlyForecast = "Hourly Forecast",
    tapForDetails = "Tap for details",
    now = "Now",
    sevenDayForecast = "7-Day Forecast",
    today = "Today",
    tomorrow = "Tomorrow",

    wind = "Wind",
    uvIndex = "UV Index",
    airQuality = "Air Quality",
    humidity = "Humidity",
    sun = "Sun",
    pressure = "Pressure",
    visibility = "Visibility",
    precipitation = "Precipitation",
    dailyPrecipitationSum = "Daily Rainfall",
    todayTotalPrecip = "24h Total",
    feelsLikeCard = "Feels Like",

    windGusts = { gusts -> String.format(java.util.Locale.US, "Gusts: %.1f m/s", gusts) },
    uvDailyMax = { max -> "Daily Max UV $max" },
    uvAdvice = "Sunscreen recommended",
    uvLevelLow = "Low",
    uvLevelModerate = "Moderate",
    uvLevelHigh = "High",
    uvLevelVeryHigh = "Very High",
    uvLevelExtreme = "Extreme",

    aqiLevelGood = "Good",
    aqiLevelModerate = "Moderate",
    aqiLevelSensitive = "Sensitive",
    aqiLevelUnhealthy = "Unhealthy",
    aqiLevelHazardous = "Hazardous",

    humidityDry = "Dry",
    humidityComfortable = "Comfortable",
    humidityHumid = "Humid",
    dewPoint = { dp -> "Dew point $dp°C" },

    sunrise = "Sunrise",
    sunset = "Sunset",
    daylightDuration = { duration -> "Daylight $duration" },

    pressureLow = "Low",
    pressureNormal = "Normal",
    pressureHigh = "High",

    visibilityClear = "Clear",
    visibilityGood = "Good",
    visibilityMisty = "Misty",
    visibilityFoggy = "Foggy",
    visibilityExcellent = "Clear View",
    visibilityModerate = "Good Visibility",
    visibilityPoor = "Poor Visibility",

    maxPrecipChance = { prob -> "Max chance $prob%" },
    noPrecipitation = "No precipitation",
    rainPrecip = "Rain",
    snowPrecip = "Snow",

    tempDiff = { diff -> if (diff >= 0) "+$diff° vs temp" else "$diff° vs temp" },
    tempSame = "Same as temp",
    feelsHot = "Hot",
    feelsWarm = "Warm",
    feelsComfortable = "Comfortable",
    feelsCool = "Cool",
    feelsCold = "Cold",
    feelsChilly = "Feels chilly",

    windDetailsTitle = "Wind Details",
    windSpeedAndDirection = "Speed & Direction",
    windDirectionLabel = "Direction",
    windSpeedLabel = "Average Wind",
    windGustsLabel = "Peak Wind Gusts",
    windGustsMultiplier = { mult -> "%.1fx avg speed".format(mult) },
    hourlyWindTitle = "24-Hour Wind Forecast",
    dailyWindTitle = "7-Day Wind Forecast",
    beaufortScaleTitle = "Beaufort Wind Scale",
    beaufortScaleLevel = { lvl -> "Force $lvl" },
    lifeInsightsTitle = "Living & Activity Insights",
    laundryTitle = "Laundry",
    umbrellaTitle = "Umbrella",
    cyclingTitle = "Cycling",
    outdoorTitle = "Outdoor & Windows",
    peakWindNotice = { time, spd -> "Peak gusts of %.1f m/s around $time".format(spd) },
    dominantWind = "Dominant Wind",
    maxWind = "Max Wind",
    maxGusts = "Max Gusts",

    // 今日の天気詳細画面
    todayWeatherTitle = "Today's Weather Details",
    todayOverview = "Today's Conditions & Outlook",
    interactive24hChartTitle = "Timeline",
    interactive24hChartSubtitle = "Scrub the chart to explore details",
    touchScrubberGuide = "Tap or drag along the timeline for hourly values",
    precipitationAndProbability = "Precipitation & Probability",
    daypartsTitle = "Dayparts Breakdown",
    morningTitle = "Morning (6:00 - 12:00)",
    afternoonTitle = "Afternoon (12:00 - 18:00)",
    eveningTitle = "Evening (18:00 - 24:00)",
    overnightTitle = "Overnight (0:00 - 6:00)",
    lifestyleAdvisorTitle = "Life Indices",
    clothingTitle = "Clothing Index",
    clothingTip = { temp ->
        when {
            temp >= 26 -> "Short sleeves recommended. Wear light, breathable clothing"
            temp >= 20 -> "Long-sleeve shirt or light top is ideal. Keep a layer handy"
            temp >= 15 -> "Sweater, cardigan, or a light jacket recommended"
            temp >= 10 -> "A jacket or trench coat is needed for comfort"
            else -> "Heavy coat, scarf, and warm layers strongly advised"
        }
    },
    umbrellaAdviceTitle = "Umbrella Index",
    umbrellaTip = { prob, mm ->
        when {
            prob >= 60 || mm >= 2.0 -> "Sturdy umbrella or rain gear essential today"
            prob >= 30 || mm > 0.0 -> "Keep a compact folding umbrella in your bag"
            else -> "No umbrella needed today. Travel light"
        }
    },
    laundryAdviceTitle = "Laundry Index",
    laundryTip = { humidity, rainProb ->
        when {
            rainProb >= 40 -> "Indoor drying strongly recommended due to rain risk"
            humidity <= 50 -> "Fast dry! Perfect laundry weather for heavy clothes"
            humidity <= 70 -> "Outdoor drying okay. Watch out for sudden afternoon clouds"
            else -> "High humidity. Indoor drying or machine dryer recommended"
        }
    },
    uvAdviceTitle = "UV Index",
    uvTip = { uv ->
        when {
            uv >= 8 -> "Very high UV! Sunscreen, sunglasses, and hat essential"
            uv >= 6 -> "High UV rays. Seek shade during midday and apply sunscreen"
            uv >= 3 -> "Moderate sunlight. UV protection advised for long outdoor stays"
            else -> "Low UV levels. No special precautions required"
        }
    },
    ventilationAdviceTitle = "Comfort & Ventilation",
    ventilationTip = { humidity, aqi ->
        when {
            aqi > 100 -> "Air quality alert. Keep window ventilation brief"
            humidity in 40..60 -> "Crisp and fresh air! Ideal time to air out rooms"
            humidity > 70 -> "Outdoor air is muggy. Dehumidifier/AC recommended after airing"
            else -> "Air is somewhat dry. Consider using a humidifier indoors"
        }
    },
    detailedMetricsTitle = "Details",
    dewPointLabel = "Dew Point",
    sunScheduleTitle = "Sun Schedule",
    sunRemaining = { remain -> "$remain until sunset" },
    daylightLabel = "Daylight",

    settingsTitle = "Settings",
    settingsSubtitle = "Preferences & System Configuration",
    tempUnitTitle = "Temperature Unit",
    tempUnitSubtitle = { symbol, isC -> "Current: $symbol (${if (isC) "Celsius" else "Fahrenheit"})" },
    windSpeedUnitTitle = "Wind Speed Unit",
    windSpeedUnitSubtitle = { unit -> "Current: $unit" },
    windSpeedMatchLanguage = "Match Language",
    refreshTitle = "Refresh Weather Data",
    refreshSubtitle = { isRef -> if (isRef) "Receiving latest data..." else "Sync current observations & forecast" },
    resetLocationTitle = "Back to Current Location",
    resetLocationSubtitle = "Use GPS location or default city",
    languageTitle = "Language",
    languageSubtitle = "Switch display language",
    changeLanguageTitle = "Change language",
    currentLanguageSection = "Current language",
    availableLanguagesSection = "Available languages",
    weatherThemeTitle = "Weather-Adaptive Theme",
    weatherThemeSubtitle = { enabled -> if (enabled) "App theme dynamically matches current weather" else "Using default system theme colors" },
    themePresetTitle = "Theme Color & Preset",
    themePresetSubtitle = { name, isAdaptive -> if (isAdaptive) "Adapting to weather ($name)" else "Fixed to $name" },
    themePresetDialogTitle = "Select Theme Color",
    themePresetDialogSubtitle = "Choose a preset color to fix, or enable weather-adaptive colors",
    appearanceTitle = "Appearance",
    appearanceSubtitle = "Theme color settings",
    generalSettingsTitle = "General Settings",
    generalSettingsSubtitle = "Language and unit preferences (Temp & Wind)",
    dataSourcesTitle = "Data Sources",
    dataSourcesSubtitle = "Weather forecast data providers & future source switching",
    dataSourcesSectionTitle = "Weather Data Provider",
    dataAndSyncTitle = "Weather Data & Sync",
    aboutTitle = "App Info",
    syncNowTitle = "Sync Weather Data",
    syncNowSubtitle = "Fetch latest observations & forecast",
    clearCacheConfirmMessage = "Cached weather data will be deleted. Fresh data will be retrieved upon next refresh.",
    dataSourcesOpenMeteoDesc = "Japan Meteorological Agency (JMA) & Open-Meteo\nDomestic weather is directly retrieved from JMA official data (AMeDAS observation stations nationwide, official regional forecasts & overviews). Overseas locations use the Open-Meteo global API.",
    dataSourcesFutureNotice = "Domestic weather data is automatically retrieved from official JMA endpoints, with global locations seamlessly powered by Open-Meteo.",
    sourceActiveBadge = "Active",
    sourceComingSoonBadge = "Coming Soon",
    sourceJmaTitle = "Japan Meteorological Agency (JMA)",
    sourceJmaDesc = "Japanese regional high-resolution radar and mesoscale numerical forecast model (MSM).",
    sourceEcmwfTitle = "ECMWF Integrated Forecast System",
    sourceEcmwfDesc = "World-leading global medium-range numerical weather prediction model.",
    sourceNoaaTitle = "NOAA GFS (Global Forecast System)",
    sourceNoaaDesc = "Global operational weather model from the US National Weather Service.",
    appVersionTitle = "Version",
    appVersionValue = "1.0 (Expressive Edition)",
    debugSettingsTitle = "Debug Settings",
    debugSettingsSubtitle = "Atmosphere animation preview and cache tools",
    debugSimulationTitle = "Atmosphere Simulation",
    debugSimulationSubtitle = "Test background animations, clouds, rain, thunder, snow, and fog",
    backgroundPreviewTitle = "Atmosphere Simulation",
    backgroundPreviewSubtitle = "Test background animations, clouds, rain, thunder, snow, and fog",
    debugResetPreview = "Reset to actual weather",
    clearCacheTitle = "Clear App Cache",
    clearCacheSubtitle = "Clear cached weather data and local room database",
    clearCacheSuccess = "Cache cleared successfully",
    forceRefreshTitle = "Force Re-sync",
    forceRefreshSubtitle = "Directly fetch newest data from remote server",
    systemDiagnosticsTitle = "System Diagnostics",
    tabAll = "All",
    tabAppearance = "Appearance",
    tabGeneral = "General",
    tabSources = "Sources",
    tabDebug = "Debug",

    loadingWeather = "Fetching weather data...",
    loadingLocation = "Locating your position...",
    errorOccurred = "An error occurred",
    retry = "Retry"
)

val SimplifiedChineseStrings = AppStrings(
    appName = "Expressive Weather",
    selectLocation = "选择地点",
    currentLocation = "当前位置",
    currentLocationWithCity = { city -> "当前位置 ($city)" },
    savedLocations = "已保存的地点",
    savedCities = "已保存的城市",
    addLocation = "添加地点",
    currentlyViewing = "当前查看",
    noSavedLocations = "暂无已保存的地点",
    recentSearches = "最近搜索",
    searchCityPlaceholder = "输入城市名称...",
    searchingCities = "正在搜索城市...",
    noCitiesFound = "未找到匹配的城市",
    close = "关闭",
    cancel = "取消",
    removeSaved = "删除",
    selected = "已选择",

    feelsLike = { temp -> "体感 $temp°" },
    high = { temp -> "最高: $temp°" },
    low = { temp -> "最低: $temp°" },
    updated = { time -> "更新于: $time" },
    justNow = "刚刚更新",
    minutesAgo = { mins -> "${mins}分钟前" },
    hoursAgo = { hours -> "${hours}小时前" },
    daysAgo = { days -> "${days}天前" },
    cachedDataBadge = { relative -> "缓存: $relative" },
    weatherDataSource = "气象数据来源: Open-Meteo & 日本气象厅",
    lastUpdatedRelative = { time, relative -> "更新: $time ($relative)" },
    cachedDataRelative = { time, relative -> "缓存: $time ($relative)" },
    pullToRefreshHint = "点击刷新天气数据",
    hourlyForecast = "逐小时预报",
    tapForDetails = "点击查看详情",
    now = "现在",
    sevenDayForecast = "7天天气预报",
    today = "今天",
    tomorrow = "明天",

    wind = "风况",
    uvIndex = "紫外线指数",
    airQuality = "空气质量",
    humidity = "相对湿度",
    sun = "日出日落",
    pressure = "气压",
    visibility = "能见度",
    precipitation = "降水量",
    dailyPrecipitationSum = "本日总降水量",
    todayTotalPrecip = "24小时累计",
    feelsLikeCard = "体感温度",

    windGusts = { gusts -> String.format(java.util.Locale.US, "阵风: %.1f m/s", gusts) },
    uvDailyMax = { max -> "今日最高紫外线: $max" },
    uvAdvice = "建议涂抹防晒霜",
    uvLevelLow = "较弱",
    uvLevelModerate = "中等",
    uvLevelHigh = "较强",
    uvLevelVeryHigh = "很强",
    uvLevelExtreme = "极强",

    aqiLevelGood = "优",
    aqiLevelModerate = "良",
    aqiLevelSensitive = "轻度污染",
    aqiLevelUnhealthy = "中度污染",
    aqiLevelHazardous = "严重污染",

    humidityDry = "干燥",
    humidityComfortable = "舒适",
    humidityHumid = "潮湿",
    dewPoint = { dp -> "露点温度 $dp°C" },

    sunrise = "日出",
    sunset = "日落",
    daylightDuration = { duration -> "日照时长 $duration" },

    pressureLow = "低压",
    pressureNormal = "正常",
    pressureHigh = "高压",

    visibilityClear = "极佳",
    visibilityGood = "良好",
    visibilityMisty = "薄雾",
    visibilityFoggy = "有雾",
    visibilityExcellent = "视野开阔",
    visibilityModerate = "能见度良好",
    visibilityPoor = "能见度较差",

    maxPrecipChance = { prob -> "最大降水概率 $prob%" },
    noPrecipitation = "无降水",
    rainPrecip = "雨",
    snowPrecip = "雪",

    tempDiff = { diff -> if (diff >= 0) "较气温高 +$diff°" else "较气温低 $diff°" },
    tempSame = "与气温一致",
    feelsHot = "炎热",
    feelsWarm = "温暖",
    feelsComfortable = "舒适",
    feelsCool = "凉爽",
    feelsCold = "寒冷",
    feelsChilly = "稍有凉意",

    windDetailsTitle = "风况详情",
    windSpeedAndDirection = "风速与风向",
    windDirectionLabel = "风向",
    windSpeedLabel = "平均风速",
    windGustsLabel = "最大阵风",
    windGustsMultiplier = { mult -> "相当于平均风速的 %.1f 倍".format(mult) },
    hourlyWindTitle = "24小时风况预报",
    dailyWindTitle = "7天风力趋势",
    beaufortScaleTitle = "蒲福风级",
    beaufortScaleLevel = { lvl -> "$lvl 级风" },
    lifeInsightsTitle = "生活指数与出行建议",
    laundryTitle = "晾晒指数",
    umbrellaTitle = "雨伞指数",
    cyclingTitle = "骑行指数",
    outdoorTitle = "户外与开窗",
    peakWindNotice = { time, spd -> "$time 前后预计出现最大阵风 %.1f m/s".format(spd) },
    dominantWind = "主导风向",
    maxWind = "最大风速",
    maxGusts = "最大阵风",

    todayWeatherTitle = "今日天气详情",
    todayOverview = "今日概况与天气走势",
    interactive24hChartTitle = "时间轴",
    interactive24hChartSubtitle = "滑动图表查看逐小时详情",
    touchScrubberGuide = "点击或拖动时间轴查看小时数值",
    precipitationAndProbability = "降水与降水概率",
    daypartsTitle = "时段分析",
    morningTitle = "上午 (6:00 - 12:00)",
    afternoonTitle = "下午 (12:00 - 18:00)",
    eveningTitle = "傍晚 (18:00 - 24:00)",
    overnightTitle = "深夜 (0:00 - 6:00)",
    lifestyleAdvisorTitle = "生活指数建议",
    clothingTitle = "穿衣指数",
    clothingTip = { temp ->
        when {
            temp >= 26 -> "短袖、轻薄衣物，注意透气排汗"
            temp >= 20 -> "长袖衬衫、T恤搭配薄开衫"
            temp >= 15 -> "建议穿针织衫、薄夹克或风衣"
            temp >= 10 -> "建议穿大衣、夹克并适度添衣"
            else -> "羽绒服、厚棉衣，做好防寒保暖"
        }
    },
    umbrellaAdviceTitle = "雨伞指数",
    umbrellaTip = { prob, mm ->
        when {
            prob >= 60 || mm >= 2.0 -> "降雨明显，外出请务必携带雨具"
            prob >= 30 || mm > 0.0 -> "有降雨可能，随身携带折叠伞备用"
            else -> "天气晴好，无需带伞"
        }
    },
    laundryAdviceTitle = "晾晒指数",
    laundryTip = { humidity, rainProb ->
        when {
            rainProb >= 40 -> "有降雨风险，建议在室内晾晒"
            humidity <= 50 -> "干燥通风，非常适宜晾晒大件衣物"
            humidity <= 70 -> "适宜室外晾晒，午后注意天气变化"
            else -> "空气湿度较高，建议使用干衣机或室内除湿"
        }
    },
    uvAdviceTitle = "紫外线防护",
    uvTip = { uv ->
        when {
            uv >= 8 -> "紫外线极强！外出请涂防晒霜、戴遮阳帽"
            uv >= 6 -> "紫外线较强，中午前后请尽量在阴凉处活动"
            uv >= 3 -> "紫外线中等，长时间户外活动请涂抹防晒"
            else -> "紫外线较弱，无需特殊防护"
        }
    },
    ventilationAdviceTitle = "开窗通风与舒适度",
    ventilationTip = { humidity, aqi ->
        when {
            aqi > 100 -> "空气质量较差，建议减少开窗时间并开启净化器"
            humidity in 40..60 -> "空气清新舒适，非常适宜开窗通风"
            humidity > 70 -> "室外潮湿闷热，通风后建议开启除湿或空调"
            else -> "室内空气偏干燥，可适当使用加湿器"
        }
    },
    detailedMetricsTitle = "详细气象指标",
    dewPointLabel = "露点",
    sunScheduleTitle = "日出日落时刻",
    sunRemaining = { remain -> "距离日落还有 $remain" },
    daylightLabel = "日照时长",

    settingsTitle = "设置",
    settingsSubtitle = "个性化偏好与系统配置",
    tempUnitTitle = "温度单位",
    tempUnitSubtitle = { symbol, isC -> "当前: $symbol (${if (isC) "摄氏度" else "华氏度"})" },
    windSpeedUnitTitle = "风速单位",
    windSpeedUnitSubtitle = { unit -> "当前: $unit" },
    windSpeedMatchLanguage = "跟随语言设定",
    refreshTitle = "刷新天气数据",
    refreshSubtitle = { isRef -> if (isRef) "正在获取最新数据..." else "立即同步最新实况与预报" },
    resetLocationTitle = "返回当前位置",
    resetLocationSubtitle = "使用 GPS 定位或默认城市",
    languageTitle = "语言 / Language",
    languageSubtitle = "切换应用显示语言",
    changeLanguageTitle = "更改语言",
    currentLanguageSection = "当前语言",
    availableLanguagesSection = "可设置的语言",
    weatherThemeTitle = "天气自适应主题",
    weatherThemeSubtitle = { enabled -> if (enabled) "应用界面主题颜色将随实时天气动态变化" else "使用系统默认配色" },
    themePresetTitle = "主题配色预设",
    themePresetSubtitle = { name, isAdaptive -> if (isAdaptive) "天气自适应 ($name)" else "固定颜色: $name" },
    themePresetDialogTitle = "选择主题配色",
    themePresetDialogSubtitle = "选择固定主题配色，或开启天气自适应颜色",
    appearanceTitle = "外观与主题",
    appearanceSubtitle = "主题颜色与视觉样式",
    generalSettingsTitle = "通用设置",
    generalSettingsSubtitle = "语言与单位偏好（温度、风速）",
    dataSourcesTitle = "数据源",
    dataSourcesSubtitle = "气象预测提供方与多数据源支持",
    dataSourcesSectionTitle = "气象数据提供方",
    dataAndSyncTitle = "数据与同步",
    aboutTitle = "应用信息",
    syncNowTitle = "同步天气数据",
    syncNowSubtitle = "获取最新观测与预报数据",
    clearCacheConfirmMessage = "本地缓存的天气与定位数据将被清除，下次刷新时将从网络重新获取。",
    dataSourcesOpenMeteoDesc = "日本气象厅 (JMA) & Open-Meteo\n日本国内天气直接获取自日本气象厅官方接口（全国 AMeDAS 观测站与区域预报）。海外地点通过 Open-Meteo 全球高精度气象 API 提供。",
    dataSourcesFutureNotice = "国内数据自动对接日本气象厅官方接口，海外地点由 Open-Meteo 提供无缝高精度全球支持。",
    sourceActiveBadge = "使用中",
    sourceComingSoonBadge = "敬请期待",
    sourceJmaTitle = "日本气象厅 (JMA)",
    sourceJmaDesc = "日本国内超高分辨率降水雷达与 MSM 中尺度数值天气预报模型。",
    sourceEcmwfTitle = "欧洲中期天气预报中心 (ECMWF)",
    sourceEcmwfDesc = "全球领先的中期数值天气预报模型。",
    sourceNoaaTitle = "美国国家海洋和大气管理局 (NOAA GFS)",
    sourceNoaaDesc = "由美国国家气象局运营的全球天气预报系统。",
    appVersionTitle = "版本信息",
    appVersionValue = "1.0 (Expressive Edition)",
    debugSettingsTitle = "调试与模拟",
    debugSettingsSubtitle = "天气氛围动效预览与缓存管理工具",
    debugSimulationTitle = "天气氛围模拟",
    debugSimulationSubtitle = "测试背景动效、云层、降雨、雷暴、降雪及雾景",
    backgroundPreviewTitle = "天气氛围模拟",
    backgroundPreviewSubtitle = "测试背景动效、云层、降雨、雷暴、降雪及雾景",
    debugResetPreview = "重置为真实天气",
    clearCacheTitle = "清除应用缓存",
    clearCacheSubtitle = "清除本地缓存的天气数据与数据库",
    clearCacheSuccess = "缓存已成功清除",
    forceRefreshTitle = "强制重新同步",
    forceRefreshSubtitle = "直接从远程服务器获取最新实况",
    systemDiagnosticsTitle = "系统诊断",
    tabAll = "全部",
    tabAppearance = "外观",
    tabGeneral = "通用",
    tabSources = "数据源",
    tabDebug = "调试",

    loadingWeather = "正在获取天气数据...",
    loadingLocation = "正在定位当前位置...",
    errorOccurred = "获取天气信息失败",
    retry = "重试"
)

val TraditionalChineseStrings = AppStrings(
    appName = "Expressive Weather",
    selectLocation = "選擇地點",
    currentLocation = "目前位置",
    currentLocationWithCity = { city -> "目前位置 ($city)" },
    savedLocations = "已儲存的地點",
    savedCities = "已儲存的城市",
    addLocation = "新增地點",
    currentlyViewing = "目前查看",
    noSavedLocations = "暫無已儲存的地點",
    recentSearches = "最近搜尋",
    searchCityPlaceholder = "輸入城市名稱...",
    searchingCities = "正在搜尋城市...",
    noCitiesFound = "未找到相符的城市",
    close = "關閉",
    cancel = "取消",
    removeSaved = "刪除",
    selected = "已選擇",

    feelsLike = { temp -> "體感 $temp°" },
    high = { temp -> "最高: $temp°" },
    low = { temp -> "最低: $temp°" },
    updated = { time -> "更新於: $time" },
    justNow = "剛剛更新",
    minutesAgo = { mins -> "${mins}分鐘前" },
    hoursAgo = { hours -> "${hours}小時前" },
    daysAgo = { days -> "${days}天前" },
    cachedDataBadge = { relative -> "快取: $relative" },
    weatherDataSource = "氣象資料來源: Open-Meteo & 日本氣象廳",
    lastUpdatedRelative = { time, relative -> "更新: $time ($relative)" },
    cachedDataRelative = { time, relative -> "快取: $time ($relative)" },
    pullToRefreshHint = "輕觸更新天氣資訊",
    hourlyForecast = "逐小時預報",
    tapForDetails = "輕觸查看詳情",
    now = "現在",
    sevenDayForecast = "7天天氣預報",
    today = "今天",
    tomorrow = "明天",

    wind = "風況",
    uvIndex = "紫外線指數",
    airQuality = "空氣品質",
    humidity = "相對濕度",
    sun = "日出日落",
    pressure = "氣壓",
    visibility = "能見度",
    precipitation = "降水量",
    dailyPrecipitationSum = "本日總降水量",
    todayTotalPrecip = "24小時累計",
    feelsLikeCard = "體感溫度",

    windGusts = { gusts -> String.format(java.util.Locale.US, "陣風: %.1f m/s", gusts) },
    uvDailyMax = { max -> "今日最高紫外線: $max" },
    uvAdvice = "建議塗抹防曬乳",
    uvLevelLow = "微弱",
    uvLevelModerate = "中等",
    uvLevelHigh = "偏強",
    uvLevelVeryHigh = "強烈",
    uvLevelExtreme = "極強",

    aqiLevelGood = "優",
    aqiLevelModerate = "普通",
    aqiLevelSensitive = "對敏感族群不良",
    aqiLevelUnhealthy = "不良",
    aqiLevelHazardous = "非常不良",

    humidityDry = "乾燥",
    humidityComfortable = "舒適",
    humidityHumid = "潮濕",
    dewPoint = { dp -> "露點溫度 $dp°C" },

    sunrise = "日出",
    sunset = "日落",
    daylightDuration = { duration -> "日照時長 $duration" },

    pressureLow = "低壓",
    pressureNormal = "正常",
    pressureHigh = "高壓",

    visibilityClear = "極佳",
    visibilityGood = "良好",
    visibilityMisty = "薄霧",
    visibilityFoggy = "有霧",
    visibilityExcellent = "視野開闊",
    visibilityModerate = "能見度尚可",
    visibilityPoor = "能見度較差",

    maxPrecipChance = { prob -> "最高降雨機率 $prob%" },
    noPrecipitation = "無降水",
    rainPrecip = "雨",
    snowPrecip = "雪",

    tempDiff = { diff -> if (diff >= 0) "較氣溫高 +$diff°" else "較氣溫低 $diff°" },
    tempSame = "與氣溫相同",
    feelsHot = "炎熱",
    feelsWarm = "溫暖",
    feelsComfortable = "舒適",
    feelsCool = "涼爽",
    feelsCold = "寒冷",
    feelsChilly = "稍有涼意",

    windDetailsTitle = "風況詳情",
    windSpeedAndDirection = "風速與風向",
    windDirectionLabel = "風向",
    windSpeedLabel = "平均風速",
    windGustsLabel = "最大陣風",
    windGustsMultiplier = { mult -> "相當於平均風速的 %.1f 倍".format(mult) },
    hourlyWindTitle = "24小時風況預報",
    dailyWindTitle = "7天風力趨勢",
    beaufortScaleTitle = "蒲福風級",
    beaufortScaleLevel = { lvl -> "$lvl 級風" },
    lifeInsightsTitle = "生活指數與外出建議",
    laundryTitle = "曬衣指數",
    umbrellaTitle = "雨傘指數",
    cyclingTitle = "騎乘指數",
    outdoorTitle = "戶外與開窗",
    peakWindNotice = { time, spd -> "$time 前後預估出現最大陣風 %.1f m/s".format(spd) },
    dominantWind = "主導風向",
    maxWind = "最大風速",
    maxGusts = "最大陣風",

    todayWeatherTitle = "今日天氣詳情",
    todayOverview = "今日概況與天氣走勢",
    interactive24hChartTitle = "時間軸",
    interactive24hChartSubtitle = "滑動圖表探索每小時詳情",
    touchScrubberGuide = "輕觸或拖動時間軸查看小時數值",
    precipitationAndProbability = "降水與降水機率",
    daypartsTitle = "時段分析",
    morningTitle = "上午 (6:00 - 12:00)",
    afternoonTitle = "下午 (12:00 - 18:00)",
    eveningTitle = "傍晚 (18:00 - 24:00)",
    overnightTitle = "深夜 (0:00 - 6:00)",
    lifestyleAdvisorTitle = "生活指數建議",
    clothingTitle = "穿衣指數",
    clothingTip = { temp ->
        when {
            temp >= 26 -> "短袖、透氣清涼衣物"
            temp >= 20 -> "長袖襯衫、薄長袖配薄外套"
            temp >= 15 -> "建議穿著毛衣、薄夾克或風衣"
            temp >= 10 -> "建議搭配大衣、外套並適度保暖"
            else -> "羽絨外套、厚大衣，做好禦寒保暖"
        }
    },
    umbrellaAdviceTitle = "雨傘指數",
    umbrellaTip = { prob, mm ->
        when {
            prob >= 60 || mm >= 2.0 -> "降雨明顯，外出請務必攜帶雨具"
            prob >= 30 || mm > 0.0 -> "有降雨可能，建議攜帶折疊傘備用"
            else -> "天氣晴朗，無需攜帶雨具"
        }
    },
    laundryAdviceTitle = "曬衣指數",
    laundryTip = { humidity, rainProb ->
        when {
            rainProb >= 40 -> "有降雨風險，建議在室內晾乾"
            humidity <= 50 -> "乾燥通風，非常適合曬乾大件衣物"
            humidity <= 70 -> "適合戶外晾曬，午後留意天氣變化"
            else -> "空氣濕度較高，建議使用乾衣機或室內除濕"
        }
    },
    uvAdviceTitle = "紫外線防護",
    uvTip = { uv ->
        when {
            uv >= 8 -> "紫外線極強！外出請塗抹防曬乳、配戴墨鏡與遮陽帽"
            uv >= 6 -> "紫外線偏強，中午前後盡量在陰涼處活動"
            uv >= 3 -> "紫外線中等，長時間戶外活動請做好防曬"
            else -> "紫外線較弱，無需特別防護"
        }
    },
    ventilationAdviceTitle = "開窗通風與舒適度",
    ventilationTip = { humidity, aqi ->
        when {
            aqi > 100 -> "空氣品質不佳，建議減少開窗並開啟空氣清淨機"
            humidity in 40..60 -> "空氣清新舒適，非常適合開窗通風"
            humidity > 70 -> "室外潮濕悶熱，通風後建議開啟除濕或空調"
            else -> "室內空氣偏乾燥，可適度使用加濕器"
        }
    },
    detailedMetricsTitle = "詳細氣象指標",
    dewPointLabel = "露點",
    sunScheduleTitle = "日出日落時刻",
    sunRemaining = { remain -> "距離日落還有 $remain" },
    daylightLabel = "日照時長",

    settingsTitle = "設定",
    settingsSubtitle = "個人化偏好與系統設定",
    tempUnitTitle = "溫度單位",
    tempUnitSubtitle = { symbol, isC -> "目前: $symbol (${if (isC) "攝氏度" else "華氏度"})" },
    windSpeedUnitTitle = "風速單位",
    windSpeedUnitSubtitle = { unit -> "目前: $unit" },
    windSpeedMatchLanguage = "跟隨語言設定",
    refreshTitle = "更新天氣資訊",
    refreshSubtitle = { isRef -> if (isRef) "正在取得最新資訊..." else "立即同步最新實況與預報" },
    resetLocationTitle = "回到目前位置",
    resetLocationSubtitle = "使用 GPS 定位或預設城市",
    languageTitle = "語言 / Language",
    languageSubtitle = "切換應用程式顯示語言",
    changeLanguageTitle = "變更語言",
    currentLanguageSection = "目前語言",
    availableLanguagesSection = "可設定的語言",
    weatherThemeTitle = "天氣自適應主題",
    weatherThemeSubtitle = { enabled -> if (enabled) "應用程式主題顏色將隨即時天氣動態變換" else "使用系統預設配色" },
    themePresetTitle = "主題配色預設",
    themePresetSubtitle = { name, isAdaptive -> if (isAdaptive) "天氣自適應 ($name)" else "固定配色: $name" },
    themePresetDialogTitle = "選擇主題配色",
    themePresetDialogSubtitle = "選擇固定主題配色，或開啟天氣自適應色彩",
    appearanceTitle = "外觀與主題",
    appearanceSubtitle = "主題顏色與視覺風格",
    generalSettingsTitle = "一般設定",
    generalSettingsSubtitle = "語言與單位設定（溫度、風速）",
    dataSourcesTitle = "資料來源",
    dataSourcesSubtitle = "氣象預報提供商與多資料來源支援",
    dataSourcesSectionTitle = "氣象資料提供商",
    dataAndSyncTitle = "資料與同步",
    aboutTitle = "應用資訊",
    syncNowTitle = "同步天氣資訊",
    syncNowSubtitle = "取得最新觀測與預報資料",
    clearCacheConfirmMessage = "本地快取的天氣與定位資料將被清除，下次更新時將從網路重新擷取。",
    dataSourcesOpenMeteoDesc = "日本氣象廳 (JMA) & Open-Meteo\n日本境內天氣直接擷取自日本氣象廳官方介面（全國 AMeDAS 觀測站與區域預報）。海外地點透過 Open-Meteo 全球高精度氣象 API 提供。",
    dataSourcesFutureNotice = "國內資料自動對接日本氣象廳官方介面，海外地點由 Open-Meteo 提供無縫高精度全球支援。",
    sourceActiveBadge = "使用中",
    sourceComingSoonBadge = "敬請期待",
    sourceJmaTitle = "日本氣象廳 (JMA)",
    sourceJmaDesc = "日本國內超高解析度降雨雷達與 MSM 中尺度數值天氣預報模型。",
    sourceEcmwfTitle = "歐洲中期天氣預報中心 (ECMWF)",
    sourceEcmwfDesc = "全球領先的中期數值天氣預報模型。",
    sourceNoaaTitle = "美國國家海洋暨大氣總署 (NOAA GFS)",
    sourceNoaaDesc = "由美國國家氣象局營運的全球天氣預報系統。",
    appVersionTitle = "版本資訊",
    appVersionValue = "1.0 (Expressive Edition)",
    debugSettingsTitle = "偵錯與模擬",
    debugSettingsSubtitle = "天氣氛圍動態預覽與快取管理工具",
    debugSimulationTitle = "天氣氛圍模擬",
    debugSimulationSubtitle = "測試背景動態、雲層、降雨、雷雨、降雪及霧景",
    backgroundPreviewTitle = "天氣氛圍模擬",
    backgroundPreviewSubtitle = "測試背景動態、雲層、降雨、雷雨、降雪及霧景",
    debugResetPreview = "重設為真實天氣",
    clearCacheTitle = "清除應用程式快取",
    clearCacheSubtitle = "清除本地快取的天氣資料與資料庫",
    clearCacheSuccess = "快取已成功清除",
    forceRefreshTitle = "強制重新同步",
    forceRefreshSubtitle = "直接從遠端伺服器取得最新實況",
    systemDiagnosticsTitle = "系統診斷",
    tabAll = "全部",
    tabAppearance = "外觀",
    tabGeneral = "一般",
    tabSources = "資料來源",
    tabDebug = "偵錯",

    loadingWeather = "正在取得天氣資料...",
    loadingLocation = "正在定位目前位置...",
    errorOccurred = "取得天氣資訊失敗",
    retry = "重試"
)

val LocalAppStrings = staticCompositionLocalOf { JapaneseStrings }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.JAPANESE }

/**
 * 風向き方位の多言語（日本語・英語・簡体字中国語・繁体字中国語）変換
 */
fun getLocalizedWindDirection(direction: String, language: AppLanguage): String {
    return when (language) {
        AppLanguage.JAPANESE -> direction
        AppLanguage.SIMPLIFIED_CHINESE -> when (direction) {
            "北" -> "北"
            "北北東" -> "东北偏北"
            "北東" -> "东北"
            "東北東" -> "东北偏东"
            "東" -> "东"
            "東南東" -> "东南偏东"
            "南東" -> "东南"
            "南南東" -> "东南偏南"
            "南" -> "南"
            "南南西" -> "西南偏南"
            "南西" -> "西南"
            "西南西" -> "西南偏西"
            "西" -> "西"
            "西北西" -> "西北偏西"
            "北西" -> "西北"
            "北北西" -> "西北偏北"
            else -> direction
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (direction) {
            "北" -> "北"
            "北北東" -> "東北偏北"
            "北東" -> "東北"
            "東北東" -> "東北偏東"
            "東" -> "東"
            "東南東" -> "東南偏東"
            "南東" -> "東南"
            "南南東" -> "東南偏南"
            "南" -> "南"
            "南南西" -> "西南偏南"
            "南西" -> "西南"
            "西南西" -> "西南偏西"
            "西" -> "西"
            "西北西" -> "西北偏西"
            "北西" -> "西北"
            "北北西" -> "西北偏北"
            else -> direction
        }
        AppLanguage.ENGLISH -> when (direction) {
            "北" -> "N"
            "北北東" -> "NNE"
            "北東" -> "NE"
            "東北東" -> "ENE"
            "東" -> "E"
            "東南東" -> "ESE"
            "南東" -> "SE"
            "南南東" -> "SSE"
            "南" -> "S"
            "南南西" -> "SSW"
            "南西" -> "SW"
            "西南西" -> "WSW"
            "西" -> "W"
            "西北西" -> "WNW"
            "北西" -> "NW"
            "北北西" -> "NNW"
            else -> direction
        }
    }
}

/**
 * 角度（0〜360度）から16方位の多言語風向文字列を取得
 */
fun getLocalizedWindDirection(degree: Float, language: AppLanguage): String {
    val directionsJa = listOf(
        "北", "北北東", "北東", "東北東",
        "東", "東南東", "南東", "南南東",
        "南", "南南西", "南西", "西南西",
        "西", "西北西", "北西", "北北西"
    )
    val normalized = (degree % 360f + 360f) % 360f
    val index = (((normalized + 11.25f) % 360f) / 22.5f).toInt().coerceIn(0, 15)
    return getLocalizedWindDirection(directionsJa[index], language)
}

/**
 * 曜日の多言語（日本語・英語・簡体字・繁体字）双方向変換
 */
fun getLocalizedDayLabel(dayLabel: String, language: AppLanguage): String {
    val trimmed = dayLabel.trim().removeSuffix(".")
    val dayKey = when {
        trimmed.equals("Today", ignoreCase = true) || trimmed == "今日" || trimmed == "今天" -> "TODAY"
        trimmed.equals("Tomorrow", ignoreCase = true) || trimmed == "明日" || trimmed == "明天" -> "TOMORROW"
        trimmed.startsWith("Mon", ignoreCase = true) || trimmed.startsWith("月") || trimmed == "周一" || trimmed == "週一" || trimmed == "星期一" || trimmed == "禮拜一" -> "MON"
        trimmed.startsWith("Tue", ignoreCase = true) || trimmed.startsWith("火") || trimmed == "周二" || trimmed == "週二" || trimmed == "星期二" || trimmed == "禮拜二" -> "TUE"
        trimmed.startsWith("Wed", ignoreCase = true) || trimmed.startsWith("水") || trimmed == "周三" || trimmed == "週三" || trimmed == "星期三" || trimmed == "禮拜三" -> "WED"
        trimmed.startsWith("Thu", ignoreCase = true) || trimmed.startsWith("木") || trimmed == "周四" || trimmed == "週四" || trimmed == "星期四" || trimmed == "禮拜四" -> "THU"
        trimmed.startsWith("Fri", ignoreCase = true) || trimmed.startsWith("金") || trimmed == "周五" || trimmed == "週五" || trimmed == "星期五" || trimmed == "禮拜五" -> "FRI"
        trimmed.startsWith("Sat", ignoreCase = true) || trimmed.startsWith("土") || trimmed == "周六" || trimmed == "週六" || trimmed == "星期六" || trimmed == "禮拜六" -> "SAT"
        trimmed.startsWith("Sun", ignoreCase = true) || trimmed.startsWith("日") || trimmed == "周日" || trimmed == "週日" || trimmed == "星期日" || trimmed == "星期天" || trimmed == "周天" || trimmed == "週天" || trimmed == "禮拜天" || trimmed == "禮拜日" -> "SUN"
        else -> null
    }

    if (dayKey == null) return dayLabel

    return when (language) {
        AppLanguage.JAPANESE -> when (dayKey) {
            "TODAY" -> "今日"
            "TOMORROW" -> "明日"
            "MON" -> "月"
            "TUE" -> "火"
            "WED" -> "水"
            "THU" -> "木"
            "FRI" -> "金"
            "SAT" -> "土"
            "SUN" -> "日"
            else -> dayLabel
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (dayKey) {
            "TODAY" -> "今天"
            "TOMORROW" -> "明天"
            "MON" -> "周一"
            "TUE" -> "周二"
            "WED" -> "周三"
            "THU" -> "周四"
            "FRI" -> "周五"
            "SAT" -> "周六"
            "SUN" -> "周日"
            else -> dayLabel
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (dayKey) {
            "TODAY" -> "今天"
            "TOMORROW" -> "明天"
            "MON" -> "週一"
            "TUE" -> "週二"
            "WED" -> "週三"
            "THU" -> "週四"
            "FRI" -> "週五"
            "SAT" -> "週六"
            "SUN" -> "週日"
            else -> dayLabel
        }
        AppLanguage.ENGLISH -> when (dayKey) {
            "TODAY" -> "Today"
            "TOMORROW" -> "Tomorrow"
            "MON" -> "Mon"
            "TUE" -> "Tue"
            "WED" -> "Wed"
            "THU" -> "Thu"
            "FRI" -> "Fri"
            "SAT" -> "Sat"
            "SUN" -> "Sun"
            else -> dayLabel
        }
    }
}

/**
 * 24時間予報などのタイムライン時間ラベル（「現在」「Now」「现在」）の多言語変換
 */
fun getLocalizedTimeLabel(timeLabel: String, language: AppLanguage): String {
    val trimmed = timeLabel.trim()
    if (trimmed == "現在" || trimmed.equals("now", ignoreCase = true) || trimmed == "现在") {
        return when (language) {
            AppLanguage.JAPANESE -> "現在"
            AppLanguage.SIMPLIFIED_CHINESE -> "现在"
            AppLanguage.TRADITIONAL_CHINESE -> "現在"
            AppLanguage.ENGLISH -> "Now"
        }
    }
    return timeLabel
}


fun WeatherCondition.getLocalizedLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.JAPANESE -> label
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            WeatherCondition.CLEAR -> "晴"
            WeatherCondition.FEW_CLOUDS -> "多云"
            WeatherCondition.CLOUDY -> "阴"
            WeatherCondition.RAIN -> "雨"
            WeatherCondition.HEAVY_RAIN -> "大雨"
            WeatherCondition.THUNDERSTORM -> "雷阵雨"
            WeatherCondition.SNOW -> "雪"
            WeatherCondition.FOG -> "雾"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            WeatherCondition.CLEAR -> "晴"
            WeatherCondition.FEW_CLOUDS -> "多雲"
            WeatherCondition.CLOUDY -> "陰"
            WeatherCondition.RAIN -> "雨"
            WeatherCondition.HEAVY_RAIN -> "大雨"
            WeatherCondition.THUNDERSTORM -> "雷陣雨"
            WeatherCondition.SNOW -> "雪"
            WeatherCondition.FOG -> "霧"
        }
        AppLanguage.ENGLISH -> englishLabel
    }
}

fun WeatherCondition.getLocalizedTagline(language: AppLanguage): String {
    return when (language) {
        AppLanguage.JAPANESE -> tagline
        AppLanguage.SIMPLIFIED_CHINESE -> when (this) {
            WeatherCondition.CLEAR -> "晴空万里，阳光明媚"
            WeatherCondition.FEW_CLOUDS -> "柔和的云层间透出阳光"
            WeatherCondition.CLOUDY -> "厚云密布，气候宜人"
            WeatherCondition.RAIN -> "细雨绵绵，外出请携带雨具"
            WeatherCondition.HEAVY_RAIN -> "降雨明显，出行请注意安全"
            WeatherCondition.THUNDERSTORM -> "对流活跃，注意防范雷击与大风"
            WeatherCondition.SNOW -> "雪花飘落，注意防寒保暖"
            WeatherCondition.FOG -> "能见度较低，注意交通安全"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (this) {
            WeatherCondition.CLEAR -> "晴空萬里，陽光明媚"
            WeatherCondition.FEW_CLOUDS -> "柔和的雲層間透出陽光"
            WeatherCondition.CLOUDY -> "厚雲密佈，氣候宜人"
            WeatherCondition.RAIN -> "細雨綿綿，外出請攜帶雨具"
            WeatherCondition.HEAVY_RAIN -> "降雨劇烈，行路請注意安全"
            WeatherCondition.THUNDERSTORM -> "對流旺盛，注意防範雷擊與強風"
            WeatherCondition.SNOW -> "雪花飄落，注意防寒保暖"
            WeatherCondition.FOG -> "能見度較低，注意交通安全"
        }
        AppLanguage.ENGLISH -> when (this) {
            WeatherCondition.CLEAR -> "Clear skies and pleasant sunshine throughout the day"
            WeatherCondition.FEW_CLOUDS -> "Gentle rays of sunlight filtering through soft clouds"
            WeatherCondition.CLOUDY -> "Overcast skies with calm and mild weather ahead"
            WeatherCondition.RAIN -> "Gentle rain is falling across the area. Don't forget your umbrella"
            WeatherCondition.HEAVY_RAIN -> "Heavy rain is falling. Watch your step and travel safely"
            WeatherCondition.THUNDERSTORM -> "Unstable atmosphere. Watch out for sudden lightning and gusts"
            WeatherCondition.SNOW -> "Snow is falling gently. Bundle up and stay warm"
            WeatherCondition.FOG -> "Misty conditions with reduced visibility. Drive carefully"
        }
    }
}

/**
 * 主要都市の中国語辞書（簡体字・繁体字）
 */
object ChineseCityDictionary {
    private val jaToZhSimp = mapOf(
        "東京都" to "东京",
        "東京" to "东京",
        "大阪市" to "大阪",
        "大阪" to "大阪",
        "横浜市" to "横滨",
        "横浜" to "横滨",
        "名古屋市" to "名古屋",
        "名古屋" to "名古屋",
        "札幌市" to "札幌",
        "札幌" to "札幌",
        "福岡市" to "福冈",
        "福岡" to "福冈",
        "神戸市" to "神户",
        "神戸" to "神户",
        "京都市" to "京都",
        "京都" to "京都",
        "さいたま市" to "埼玉",
        "さいたま" to "埼玉",
        "埼玉" to "埼玉",
        "広島市" to "广岛",
        "広島" to "广岛",
        "仙台市" to "仙台",
        "仙台" to "仙台",
        "千葉市" to "千叶",
        "千葉" to "千叶",
        "北九州市" to "北九州",
        "川崎市" to "川崎",
        "静岡市" to "静冈",
        "静岡" to "静冈",
        "浜松市" to "滨松",
        "那覇市" to "那霸",
        "那覇" to "那霸",
        "沖縄" to "冲绳",
        "金沢市" to "金泽",
        "金沢" to "金泽",
        "長崎市" to "长崎",
        "長崎" to "长崎",
        "熊本市" to "熊本",
        "熊本" to "熊本",
        "鹿児島市" to "鹿儿岛",
        "鹿児島" to "鹿儿岛",
        "千代田区" to "千代田区",
        "新宿区" to "新宿区",
        "渋谷区" to "涩谷区",
        "港区" to "港区",
        "中央区" to "中央区",
        "勝浦市" to "胜浦市",
        "勝浦" to "胜浦"
    )

    private val enToZhSimp = mapOf(
        "Tokyo" to "东京",
        "Osaka" to "大阪",
        "Kyoto" to "京都",
        "Yokohama" to "横滨",
        "Nagoya" to "名古屋",
        "Sapporo" to "札幌",
        "Fukuoka" to "福冈",
        "Kobe" to "神户",
        "Hiroshima" to "广岛",
        "Sendai" to "仙台",
        "Chiba" to "千叶",
        "Naha" to "那霸",
        "Okinawa" to "冲绳",
        "Beijing" to "北京",
        "Shanghai" to "上海",
        "Guangzhou" to "广州",
        "Shenzhen" to "深圳",
        "Hong Kong" to "香港",
        "Taipei" to "台北",
        "Seoul" to "首尔",
        "London" to "伦敦",
        "Paris" to "巴黎",
        "New York" to "纽约",
        "Los Angeles" to "洛杉矶",
        "Singapore" to "新加坡",
        "Sydney" to "悉尼"
    )

    private val jaToZhTrad = mapOf(
        "東京都" to "東京",
        "東京" to "東京",
        "大阪市" to "大阪",
        "大阪" to "大阪",
        "横浜市" to "橫濱",
        "横浜" to "橫濱",
        "名古屋市" to "名古屋",
        "名古屋" to "名古屋",
        "札幌市" to "札幌",
        "札幌" to "札幌",
        "福岡市" to "福岡",
        "福岡" to "福岡",
        "神戸市" to "神戶",
        "神戸" to "神戶",
        "京都市" to "京都",
        "京都" to "京都",
        "さいたま市" to "埼玉",
        "さいたま" to "埼玉",
        "埼玉" to "埼玉",
        "広島市" to "廣島",
        "広島" to "廣島",
        "仙台市" to "仙台",
        "仙台" to "仙台",
        "千葉市" to "千葉",
        "千葉" to "千葉",
        "北九州市" to "北九州",
        "川崎市" to "川崎",
        "静岡市" to "靜岡",
        "静岡" to "靜岡",
        "浜松市" to "濱松",
        "那覇市" to "那霸",
        "那覇" to "那霸",
        "沖縄" to "沖繩",
        "金沢市" to "金澤",
        "金沢" to "金澤",
        "長崎市" to "長崎",
        "長崎" to "長崎",
        "熊本市" to "熊本",
        "熊本" to "熊本",
        "鹿児島市" to "鹿兒島",
        "鹿児島" to "鹿兒島",
        "千代田区" to "千代田區",
        "新宿区" to "新宿區",
        "渋谷区" to "澀谷區",
        "港区" to "港區",
        "中央区" to "中央區",
        "勝浦市" to "勝浦市",
        "勝浦" to "勝浦"
    )

    private val enToZhTrad = mapOf(
        "Tokyo" to "東京",
        "Osaka" to "大阪",
        "Kyoto" to "京都",
        "Yokohama" to "橫濱",
        "Nagoya" to "名古屋",
        "Sapporo" to "札幌",
        "Fukuoka" to "福岡",
        "Kobe" to "神戶",
        "Hiroshima" to "廣島",
        "Sendai" to "仙台",
        "Chiba" to "千葉",
        "Naha" to "那霸",
        "Okinawa" to "沖繩",
        "Beijing" to "北京",
        "Shanghai" to "上海",
        "Guangzhou" to "廣州",
        "Shenzhen" to "深圳",
        "Hong Kong" to "香港",
        "Taipei" to "臺北",
        "Seoul" to "首爾",
        "London" to "倫敦",
        "Paris" to "巴黎",
        "New York" to "紐約",
        "Los Angeles" to "洛杉磯",
        "Singapore" to "新加坡",
        "Sydney" to "雪梨"
    )

    fun translateToSimplified(nameJa: String, nameEn: String): String {
        return jaToZhSimp[nameJa]
            ?: enToZhSimp[nameEn]
            ?: jaToZhSimp[nameJa.removeSuffix("市").removeSuffix("区").removeSuffix("町").removeSuffix("村")]
            ?: if (nameJa.isNotBlank()) nameJa else nameEn
    }

    fun translateToTraditional(nameJa: String, nameEn: String): String {
        return jaToZhTrad[nameJa]
            ?: enToZhTrad[nameEn]
            ?: jaToZhTrad[nameJa.removeSuffix("市").removeSuffix("区").removeSuffix("町").removeSuffix("村")]
            ?: if (nameJa.isNotBlank()) nameJa else nameEn
    }
}

fun City.localizedName(language: AppLanguage): String {
    return when (language) {
        AppLanguage.JAPANESE -> name
        AppLanguage.SIMPLIFIED_CHINESE -> ChineseCityDictionary.translateToSimplified(name, nameEn)
        AppLanguage.TRADITIONAL_CHINESE -> ChineseCityDictionary.translateToTraditional(name, nameEn)
        AppLanguage.ENGLISH -> if (nameEn.isNotBlank()) nameEn else name
    }
}

fun AppLanguage.getDisplayNameIn(language: AppLanguage): String = when (this) {
    AppLanguage.JAPANESE -> when (language) {
        AppLanguage.JAPANESE -> "日本語"
        AppLanguage.ENGLISH -> "Japanese"
        AppLanguage.SIMPLIFIED_CHINESE -> "日语"
        AppLanguage.TRADITIONAL_CHINESE -> "日語"
    }
    AppLanguage.ENGLISH -> when (language) {
        AppLanguage.JAPANESE -> "English"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.SIMPLIFIED_CHINESE -> "英语 (English)"
        AppLanguage.TRADITIONAL_CHINESE -> "英語 (English)"
    }
    AppLanguage.SIMPLIFIED_CHINESE -> when (language) {
        AppLanguage.JAPANESE -> "中国語 (簡体字)"
        AppLanguage.ENGLISH -> "Chinese (Simplified)"
        AppLanguage.SIMPLIFIED_CHINESE -> "简体中文"
        AppLanguage.TRADITIONAL_CHINESE -> "簡體中文"
    }
    AppLanguage.TRADITIONAL_CHINESE -> when (language) {
        AppLanguage.JAPANESE -> "中国語 (繁体字)"
        AppLanguage.ENGLISH -> "Chinese (Traditional)"
        AppLanguage.SIMPLIFIED_CHINESE -> "繁体中文"
        AppLanguage.TRADITIONAL_CHINESE -> "繁體中文"
    }
}

