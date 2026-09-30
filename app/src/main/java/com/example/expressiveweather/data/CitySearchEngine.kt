package com.example.expressiveweather.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.expressiveweather.data.local.LocationKeyNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 多言語・表記ゆれ・あいまい入力に対応した高精度ハイブリッド都市検索エンジン
 */
object CitySearchEngine {

    /**
     * オフライン主要都市バイリンガル辞書エントリ
     */
    data class MasterCity(
        val nameJa: String,
        val nameEn: String,
        val countryJa: String,
        val countryEn: String,
        val latitude: Double,
        val longitude: Double,
        val aliases: List<String> = emptyList()
    ) {
        fun toCity(language: AppLanguage): City {
            return City(
                id = "master_${LocationKeyNormalizer.normalize(latitude, longitude)}",
                name = nameJa,
                nameEn = nameEn,
                country = if (language == AppLanguage.ENGLISH) countryEn else countryJa,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    // 日本全国47都道府県庁所在地 + 全政令指定都市 + 主要都市・観光地 + 世界主要大都市の包括マスター辞書
    val MASTER_CITIES: List<MasterCity> = listOf(
        // --- 日本：主要大都市・政令市・都道府県庁所在地 ---
        MasterCity("東京都", "Tokyo", "日本", "Japan", 35.6895, 139.6917, listOf("東京", "とうきょう", "トウキョウ", "tokyo")),
        MasterCity("大阪市", "Osaka", "日本 大阪府", "Japan Osaka", 34.6937, 135.5023, listOf("大阪", "おおさか", "オオサカ", "osaka")),
        MasterCity("横浜市", "Yokohama", "日本 神奈川県", "Japan Kanagawa", 35.4437, 139.6380, listOf("横浜", "よこはま", "ヨコハマ", "yokohama")),
        MasterCity("名古屋市", "Nagoya", "日本 愛知県", "Japan Aichi", 35.1815, 136.9066, listOf("名古屋", "なごや", "ナゴヤ", "nagoya")),
        MasterCity("札幌市", "Sapporo", "日本 北海道", "Japan Hokkaido", 43.0618, 141.3545, listOf("札幌", "さっぽろ", "サッポロ", "sapporo")),
        MasterCity("福岡市", "Fukuoka", "日本 福岡県", "Japan Fukuoka", 33.5904, 130.4017, listOf("福岡", "ふくおか", "フクオカ", "fukuoka")),
        MasterCity("神戸市", "Kobe", "日本 兵庫県", "Japan Hyogo", 34.6901, 135.1955, listOf("神戸", "こうべ", "コウベ", "kobe")),
        MasterCity("京都市", "Kyoto", "日本 京都府", "Japan Kyoto", 35.0116, 135.7681, listOf("京都", "きょうと", "キョウト", "kyoto")),
        MasterCity("さいたま市", "Saitama", "日本 埼玉県", "Japan Saitama", 35.8617, 139.6455, listOf("さいたま", "サイタマ", "埼玉", "saitama")),
        MasterCity("広島市", "Hiroshima", "日本 広島県", "Japan Hiroshima", 34.3853, 132.4553, listOf("広島", "ひろしま", "ヒロシマ", "hiroshima")),
        MasterCity("仙台市", "Sendai", "日本 宮城県", "Japan Miyagi", 38.2682, 140.8694, listOf("仙台", "せんだい", "センダイ", "sendai")),
        MasterCity("千葉市", "Chiba", "日本 千葉県", "Japan Chiba", 35.6074, 140.1065, listOf("千葉", "ちば", "チバ", "chiba")),
        MasterCity("北九州市", "Kitakyushu", "日本 福岡県", "Japan Fukuoka", 33.8835, 130.8752, listOf("北九州", "きたきゅうしゅう", "kitakyushu")),
        MasterCity("川崎市", "Kawasaki", "日本 神奈川県", "Japan Kanagawa", 35.5308, 139.7029, listOf("川崎", "かわさき", "kawasaki")),
        MasterCity("相模原市", "Sagamihara", "日本 神奈川県", "Japan Kanagawa", 35.5713, 139.3732, listOf("相模原", "さがみはら", "sagamihara")),
        MasterCity("堺市", "Sakai", "日本 大阪府", "Japan Osaka", 34.5733, 135.4830, listOf("堺", "さかい", "sakai")),
        MasterCity("静岡市", "Shizuoka", "日本 静岡県", "Japan Shizuoka", 34.9756, 138.3828, listOf("静岡", "しずおか", "shizuoka")),
        MasterCity("浜松市", "Hamamatsu", "日本 静岡県", "Japan Shizuoka", 34.7108, 137.7261, listOf("浜松", "はままつ", "hamamatsu")),
        MasterCity("岡山市", "Okayama", "日本 岡山県", "Japan Okayama", 34.6551, 133.9195, listOf("岡山", "おかやま", "okayama")),
        MasterCity("熊本市", "Kumamoto", "日本 熊本県", "Japan Kumamoto", 32.7898, 130.7417, listOf("熊本", "くまもと", "kumamoto")),
        MasterCity("新潟市", "Niigata", "日本 新潟県", "Japan Niigata", 37.9161, 139.0364, listOf("新潟", "にいがた", "niigata")),
        MasterCity("金沢市", "Kanazawa", "日本 石川県", "Japan Ishikawa", 36.5613, 136.6562, listOf("金沢", "かなざわ", "kanazawa")),
        MasterCity("那覇市", "Naha", "日本 沖縄県", "Japan Okinawa", 26.2124, 127.6809, listOf("那覇", "なは", "ナハ", "沖縄", "おきなわ", "naha", "okinawa")),
        MasterCity("鹿児島市", "Kagoshima", "日本 鹿児島県", "Japan Kagoshima", 31.5966, 130.5571, listOf("鹿児島", "かごしま", "kagoshima")),
        MasterCity("長崎市", "Nagasaki", "日本 長崎県", "Japan Nagasaki", 32.7503, 129.8777, listOf("長崎", "ながさき", "nagasaki")),
        MasterCity("松山市", "Matsuyama", "日本 愛媛県", "Japan Ehime", 33.8392, 132.7656, listOf("松山", "まつやま", "matsuyama")),
        MasterCity("高松市", "Takamatsu", "日本 香川県", "Japan Kagawa", 34.3428, 134.0466, listOf("高松", "たかまつ", "takamatsu")),
        MasterCity("富山市", "Toyama", "日本 富山県", "Japan Toyama", 36.6953, 137.2113, listOf("富山", "とやま", "toyama")),
        MasterCity("岐阜市", "Gifu", "日本 岐阜県", "Japan Gifu", 35.4233, 136.7607, listOf("岐阜", "ぎふ", "gifu")),
        MasterCity("大分市", "Oita", "日本 大分県", "Japan Oita", 33.2382, 131.6126, listOf("大分", "おおいた", "oita")),
        MasterCity("奈良市", "Nara", "日本 奈良県", "Japan Nara", 34.6851, 135.8048, listOf("奈良", "なら", "nara")),
        MasterCity("宇都宮市", "Utsunomiya", "日本 栃木県", "Japan Tochigi", 36.5551, 139.8828, listOf("宇都宮", "うつのみや", "utsunomiya")),
        MasterCity("長野市", "Nagano", "日本 長野県", "Japan Nagano", 36.6513, 138.1810, listOf("長野", "ながの", "nagano")),
        MasterCity("盛岡市", "Morioka", "日本 岩手県", "Japan Iwate", 39.7036, 141.1527, listOf("盛岡", "もりおか", "morioka")),
        MasterCity("高知市", "Kochi", "日本 高知県", "Japan Kochi", 33.5597, 133.5311, listOf("高知", "こうち", "kochi")),
        MasterCity("宮崎市", "Miyazaki", "日本 宮崎県", "Japan Miyazaki", 31.9077, 131.4202, listOf("宮崎", "みやざき", "miyazaki")),
        MasterCity("福井市", "Fukui", "日本 福井県", "Japan Fukui", 36.0641, 136.2195, listOf("福井", "ふくい", "fukui")),
        MasterCity("秋田市", "Akita", "日本 秋田県", "Japan Akita", 39.7200, 140.1026, listOf("秋田", "あきた", "akita")),
        MasterCity("山形市", "Yamagata", "日本 山形県", "Japan Yamagata", 38.2554, 140.3396, listOf("山形", "やまがた", "yamagata")),
        MasterCity("水戸市", "Mito", "日本 茨城県", "Japan Ibaraki", 36.3659, 140.4712, listOf("水戸", "みと", "mito")),
        MasterCity("前橋市", "Maebashi", "日本 群馬県", "Japan Gunma", 36.3895, 139.0634, listOf("前橋", "まえばし", "maebashi")),
        MasterCity("徳島市", "Tokushima", "日本 徳島県", "Japan Tokushima", 34.0703, 134.5548, listOf("徳島", "とくしま", "tokushima")),
        MasterCity("鳥取市", "Tottori", "日本 鳥取県", "Japan Tottori", 35.5011, 134.2351, listOf("鳥取", "とっとり", "tottori")),
        MasterCity("甲府市", "Kofu", "日本 山梨県", "Japan Yamanashi", 35.6622, 138.5684, listOf("甲府", "こうふ", "kofu")),
        MasterCity("松江市", "Matsue", "日本 島根県", "Japan Shimane", 35.4681, 133.0484, listOf("松江", "まつえ", "matsue")),
        MasterCity("佐賀市", "Saga", "日本 佐賀県", "Japan Saga", 33.2635, 130.3009, listOf("佐賀", "さが", "saga")),
        MasterCity("大津市", "Otsu", "日本 滋賀県", "Japan Shiga", 35.0178, 135.8547, listOf("大津", "おおつ", "otsu")),
        MasterCity("山口市", "Yamaguchi", "日本 山口県", "Japan Yamaguchi", 34.1785, 131.4737, listOf("山口", "やまぐち", "yamaguchi")),
        MasterCity("津市", "Tsu", "日本 三重県", "Japan Mie", 34.7186, 136.5053, listOf("津", "つ", "tsu")),
        MasterCity("青森市", "Aomori", "日本 青森県", "Japan Aomori", 40.8244, 140.7400, listOf("青森", "あおもり", "aomori")),
        MasterCity("和歌山市", "Wakayama", "日本 和歌山県", "Japan Wakayama", 34.2305, 135.1708, listOf("和歌山", "わかやま", "wakayama")),

        // --- 東京主要エリア・区 ---
        MasterCity("新宿区", "Shinjuku", "日本 東京都", "Japan Tokyo", 35.6938, 139.7036, listOf("新宿", "しんじゅく", "shinjuku")),
        MasterCity("渋谷区", "Shibuya", "日本 東京都", "Japan Tokyo", 35.6580, 139.7016, listOf("渋谷", "しぶや", "shibuya")),
        MasterCity("港区", "Minato", "日本 東京都", "Japan Tokyo", 35.6581, 139.7515, listOf("六本木", "港区", "みなと", "roppongi", "minato")),
        MasterCity("中央区", "Chuo", "日本 東京都", "Japan Tokyo", 35.6707, 139.7720, listOf("銀座", "日本橋", "中央区", "ginza", "chuo")),
        MasterCity("千代田区", "Chiyoda", "日本 東京都", "Japan Tokyo", 35.6940, 139.7536, listOf("千代田", "秋葉原", "神田", "akihabara", "chiyoda")),
        MasterCity("豊島区", "Toshima", "日本 東京都", "Japan Tokyo", 35.7289, 139.7126, listOf("池袋", "豊島", "いけぶくろ", "ikebukuro")),
        MasterCity("台東区", "Taito", "日本 東京都", "Japan Tokyo", 35.7126, 139.7799, listOf("上野", "浅草", "台東", "ueno", "asakusa")),
        MasterCity("品川区", "Shinagawa", "日本 東京都", "Japan Tokyo", 35.6092, 139.7303, listOf("品川", "しながわ", "shinagawa")),
        MasterCity("世田谷区", "Setagaya", "日本 東京都", "Japan Tokyo", 35.6466, 139.6533, listOf("世田谷", "せたがや", "setagaya")),

        // --- 人気観光地・特徴的エリア ---
        MasterCity("日光市", "Nikko", "日本 栃木県", "Japan Tochigi", 36.7554, 139.5986, listOf("日光", "にっこう", "nikko")),
        MasterCity("箱根町", "Hakone", "日本 神奈川県", "Japan Kanagawa", 35.2323, 139.1069, listOf("箱根", "はこね", "hakone")),
        MasterCity("軽井沢町", "Karuizawa", "日本 長野県", "Japan Nagano", 36.3488, 138.6358, listOf("軽井沢", "かるいざわ", "karuizawa")),
        MasterCity("石垣市", "Ishigaki", "日本 沖縄県", "Japan Okinawa", 24.3448, 124.1572, listOf("石垣", "石垣島", "いしがき", "ishigaki")),
        MasterCity("宮古島市", "Miyakojima", "日本 沖縄県", "Japan Okinawa", 24.8055, 125.2811, listOf("宮古島", "宮古", "みやこじま", "miyakojima")),
        MasterCity("函館市", "Hakodate", "日本 北海道", "Japan Hokkaido", 41.7687, 140.7288, listOf("函館", "はこだて", "hakodate")),
        MasterCity("旭川市", "Asahikawa", "日本 北海道", "Japan Hokkaido", 43.7706, 142.3649, listOf("旭川", "あさひかわ", "asahikawa")),
        MasterCity("小樽市", "Otaru", "日本 北海道", "Japan Hokkaido", 43.1907, 140.9947, listOf("小樽", "おたる", "otaru")),
        MasterCity("鎌倉市", "Kamakura", "日本 神奈川県", "Japan Kanagawa", 35.3192, 139.5467, listOf("鎌倉", "かまくら", "kamakura")),
        MasterCity("姫路市", "Himeji", "日本 兵庫県", "Japan Hyogo", 34.8151, 134.6853, listOf("姫路", "ひめじ", "himeji")),
        MasterCity("別府市", "Beppu", "日本 大分県", "Japan Oita", 33.2846, 131.4912, listOf("別府", "べっぷ", "beppu")),

        // --- 世界主要大都市 ---
        MasterCity("ニューヨーク", "New York", "アメリカ合衆国", "United States", 40.7128, -74.0060, listOf("ニューヨーク", "ny", "nyc", "new york")),
        MasterCity("ロサンゼルス", "Los Angeles", "アメリカ合衆国", "United States", 34.0522, -118.2437, listOf("ロサンゼルス", "ロス", "la", "los angeles")),
        MasterCity("サンフランシスコ", "San Francisco", "アメリカ合衆国", "United States", 37.7749, -122.4194, listOf("サンフランシスコ", "san francisco")),
        MasterCity("ロンドン", "London", "イギリス", "United Kingdom", 51.5074, -0.1278, listOf("ロンドン", "london")),
        MasterCity("パリ", "Paris", "フランス", "France", 48.8566, 2.3522, listOf("パリ", "paris")),
        MasterCity("ローマ", "Rome", "イタリア", "Italy", 41.9028, 12.4964, listOf("ローマ", "rome")),
        MasterCity("ベルリン", "Berlin", "ドイツ", "Germany", 52.5200, 13.4050, listOf("ベルリン", "berlin")),
        MasterCity("マドリード", "Madrid", "スペイン", "Spain", 40.4168, -3.7038, listOf("マドリード", "madrid")),
        MasterCity("ソウル", "Seoul", "大韓民国", "South Korea", 37.5665, 126.9780, listOf("ソウル", "seoul")),
        MasterCity("台北", "Taipei", "台湾", "Taiwan", 25.0330, 121.5654, listOf("台北", "タイペイ", "たいぺい", "taipei")),
        MasterCity("北京", "Beijing", "中国", "China", 39.9042, 116.4074, listOf("北京", "ペキン", "beijing")),
        MasterCity("上海", "Shanghai", "中国", "China", 31.2304, 121.4737, listOf("上海", "シャンハイ", "shanghai")),
        MasterCity("香港", "Hong Kong", "香港", "Hong Kong", 22.3193, 114.1694, listOf("香港", "ほんこん", "ホンコン", "hong kong")),
        MasterCity("シンガポール", "Singapore", "シンガポール", "Singapore", 1.3521, 103.8198, listOf("シンガポール", "singapore")),
        MasterCity("バンコク", "Bangkok", "タイ", "Thailand", 13.7563, 100.5018, listOf("バンコク", "bangkok")),
        MasterCity("シドニー", "Sydney", "オーストラリア", "Australia", -33.8688, 151.2093, listOf("シドニー", "sydney")),
        MasterCity("ホノルル", "Honolulu", "アメリカ合衆国 ハワイ", "United States Hawaii", 21.3069, -157.8583, listOf("ホノルル", "ハワイ", "honolulu", "hawaii")),
        MasterCity("トロント", "Toronto", "カナダ", "Canada", 43.6532, -79.3832, listOf("トロント", "toronto")),
        MasterCity("バンクーバー", "Vancouver", "カナダ", "Canada", 49.2827, -123.1207, listOf("バンクーバー", "vancouver"))
    )

    /**
     * ひらがな→カタカナ変換
     */
    fun hiraToKata(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            if (c in '\u3041'..'\u3096') {
                sb.append((c.code + 0x60).toChar())
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * ユーザー入力クエリを解析・拡張して、複数の検索クエリ候補を生成する。
     * 例:
     * - "東京" -> ["東京", "東京都", "Tokyo"]
     * - "とうきょう" -> ["とうきょう", "東京", "東京都", "Tokyo"]
     * - "Tokyo" -> ["Tokyo", "東京都", "東京"]
     * - "Osaka" -> ["Osaka", "大阪市", "大阪府", "大阪"]
     */
    fun expandQueries(rawQuery: String): List<String> {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return emptyList()

        val candidates = linkedSetOf<String>()
        candidates.add(trimmed)

        val lower = trimmed.lowercase(Locale.ROOT)
        val kata = hiraToKata(trimmed)

        // 1. マスター辞書から完全一致・前方一致するエイリアスを探索
        for (m in MASTER_CITIES) {
            val matches = m.aliases.any { alias ->
                alias.equals(trimmed, ignoreCase = true) ||
                        alias.equals(lower, ignoreCase = true) ||
                        alias.equals(kata, ignoreCase = true) ||
                        (trimmed.length >= 2 && alias.startsWith(trimmed, ignoreCase = true)) ||
                        (trimmed.length >= 2 && alias.startsWith(lower, ignoreCase = true))
            }
            if (matches || m.nameJa.contains(trimmed) || m.nameEn.startsWith(trimmed, ignoreCase = true)) {
                candidates.add(m.nameJa)
                val baseName = m.nameJa.removeSuffix("都").removeSuffix("市").removeSuffix("府").removeSuffix("区").removeSuffix("県")
                if (baseName.isNotBlank()) {
                    candidates.add(baseName)
                }
                candidates.add(m.nameEn)
            }
        }

        // 2. 漢字末尾の接尾辞補完（「都」「市」「府」「区」「県」が付いていない場合）
        val suffixes = listOf("市", "都", "府", "区", "県")
        val hasSuffix = suffixes.any { trimmed.endsWith(it) }
        if (!hasSuffix && trimmed.all { it.code in 0x4E00..0x9FFF }) { // 漢字のみの場合
            for (s in suffixes) {
                candidates.add(trimmed + s)
            }
        }

        return candidates.toList()
    }

    /**
     * メイン検索関数（3層ハイブリッド方式）
     */
    suspend fun search(
        query: String,
        language: AppLanguage,
        context: Context? = null
    ): List<City> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val expandedQueries = expandQueries(trimmed)
        val collectedCities = mutableListOf<City>()

        // -------------------------------------------------------------
        // 層 1: オフライン主要都市辞書からの高速マッチ（即時）
        // -------------------------------------------------------------
        val localMatches = searchLocalMaster(trimmed, language)
        collectedCities.addAll(localMatches)

        // -------------------------------------------------------------
        // 層 2: Android 標準 Geocoder（利用可能な場合）
        // -------------------------------------------------------------
        if (context != null && Geocoder.isPresent()) {
            try {
                val geocoderResults = searchAndroidGeocoder(trimmed, expandedQueries, context, language)
                collectedCities.addAll(geocoderResults)
            } catch (_: Exception) {
                // Geocoder 失敗時は Open-Meteo にフォールバック
            }
        }

        // -------------------------------------------------------------
        // 層 3: Open-Meteo Geocoding API（多言語・クエリ拡張並行フェッチ）
        // -------------------------------------------------------------
        try {
            val openMeteoResults = searchOpenMeteo(trimmed, expandedQueries, language)
            collectedCities.addAll(openMeteoResults)
        } catch (_: Exception) {
            // API 通信エラー時もローカル・Geocoder 結果を維持
        }

        // -------------------------------------------------------------
        // マージ・重複排除・スコアリング
        // -------------------------------------------------------------
        rankAndDeduplicate(trimmed, collectedCities, language)
    }

    /**
     * オフライン主要都市辞書からのインクリメンタル検索
     */
    private fun searchLocalMaster(query: String, language: AppLanguage): List<City> {
        val trimmed = query.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val kata = hiraToKata(trimmed)

        val results = mutableListOf<City>()
        for (m in MASTER_CITIES) {
            val exactMatch = m.nameJa.equals(trimmed, ignoreCase = true) ||
                    m.nameEn.equals(trimmed, ignoreCase = true) ||
                    m.aliases.any { it.equals(trimmed, ignoreCase = true) || it.equals(lower, ignoreCase = true) || it.equals(kata, ignoreCase = true) }

            val prefixMatch = m.nameJa.startsWith(trimmed) ||
                    m.nameEn.startsWith(trimmed, ignoreCase = true) ||
                    m.aliases.any { it.startsWith(trimmed, ignoreCase = true) || it.startsWith(lower, ignoreCase = true) || it.startsWith(kata, ignoreCase = true) }

            if (exactMatch || prefixMatch) {
                results.add(m.toCity(language))
            }
        }
        return results
    }

    /**
     * Android 標準 Geocoder による地名解決
     */
    private suspend fun searchAndroidGeocoder(
        originalQuery: String,
        expandedQueries: List<String>,
        context: Context,
        language: AppLanguage
    ): List<City> = coroutineScope {
        val queriesToTry = (listOf(originalQuery) + expandedQueries).distinct().take(3)
        val jaGeocoder = Geocoder(context, Locale.JAPAN)
        val enGeocoder = Geocoder(context, Locale.US)

        val list = mutableListOf<City>()

        for (q in queriesToTry) {
            val jaAddresses = getAddressesFromGeocoder(jaGeocoder, q, 5)
            if (jaAddresses.isNotEmpty()) {
                for (addr in jaAddresses) {
                    val lat = addr.latitude
                    val lon = addr.longitude

                    // 英語地名も並行して逆ジオコーディング取得
                    val enName = try {
                        val enAddrs = getAddressesFromLocation(enGeocoder, lat, lon, 1)
                        enAddrs.firstOrNull()?.let { resolveName(it, isEn = true) }
                    } catch (_: Exception) { null }

                    val jaName = resolveName(addr, isEn = false)
                    val jaCountry = addr.countryName ?: (addr.adminArea ?: "日本")
                    val enCountry = if (addr.countryCode == "JP") "Japan" else (addr.countryName ?: "")

                    val finalNameEn = if (!enName.isNullOrBlank()) enName else jaName

                    list.add(
                        City(
                            id = "geo_${LocationKeyNormalizer.normalize(lat, lon)}",
                            name = jaName,
                            nameEn = finalNameEn,
                            country = if (language == AppLanguage.ENGLISH) enCountry else jaCountry,
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
                break // 最も関連性の高いクエリで結果が得られたらループ終了
            }
        }
        list
    }

    private suspend fun getAddressesFromGeocoder(geocoder: Geocoder, query: String, maxResults: Int): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(query, maxResults) { list ->
                    if (cont.isActive) cont.resume(list)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query, maxResults) ?: emptyList()
        }
    }

    private suspend fun getAddressesFromLocation(geocoder: Geocoder, lat: Double, lon: Double, maxResults: Int): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lon, maxResults) { list ->
                    if (cont.isActive) cont.resume(list)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, maxResults) ?: emptyList()
        }
    }

    private fun resolveName(addr: Address, isEn: Boolean): String {
        return addr.locality
            ?: addr.subLocality
            ?: addr.subAdminArea
            ?: addr.adminArea
            ?: addr.featureName
            ?: if (isEn) "Unknown" else "不明な地域"
    }

    /**
     * Open-Meteo Geocoding API を用いた検索
     * アプリ言語設定に合わせたリクエストと、日本語/英語名の確実な保持を行う。
     */
    private suspend fun searchOpenMeteo(
        originalQuery: String,
        expandedQueries: List<String>,
        language: AppLanguage
    ): List<City> = coroutineScope {
        // クエリ候補（元クエリ、補完形、ローマ字など）から最大3件を並行リクエスト
        val queriesToTry = (listOf(originalQuery) + expandedQueries).distinct().take(3)
        val apiLang = when (language) {
            AppLanguage.ENGLISH -> "en"
            AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "zh"
            AppLanguage.JAPANESE -> "ja"
        }

        val jobs = queriesToTry.map { q ->
            async {
                fetchOpenMeteoSingle(q, apiLang, language)
            }
        }

        jobs.awaitAll().flatten()
    }

    private fun fetchOpenMeteoSingle(query: String, apiLang: String, appLanguage: AppLanguage): List<City> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val urlStr = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=$apiLang&format=json"

        val json = httpGetJson(urlStr) ?: return emptyList()
        val results = json.optJSONArray("results") ?: return emptyList()

        val list = mutableListOf<City>()
        for (i in 0 until results.length()) {
            val obj = results.getJSONObject(i)
            val id = obj.optString("id", "city_$i")
            val name = obj.optString("name", query)
            val country = obj.optString("country", "")
            val admin1 = obj.optString("admin1", "")
            val admin2 = obj.optString("admin2", "")
            val lat = obj.getDouble("latitude")
            val lon = obj.getDouble("longitude")

            val regionParts = listOf(country, admin1, admin2).filter { it.isNotBlank() }.distinct()
            val regionDesc = regionParts.joinToString(" ")

            // 英語名と日本語名の双方向補完
            val (nameJa, nameEn) = if (apiLang == "en") {
                val matchedMaster = MASTER_CITIES.firstOrNull {
                    LocationKeyNormalizer.normalize(it.latitude, it.longitude) == LocationKeyNormalizer.normalize(lat, lon)
                }
                (matchedMaster?.nameJa ?: name) to name
            } else {
                val matchedMaster = MASTER_CITIES.firstOrNull {
                    LocationKeyNormalizer.normalize(it.latitude, it.longitude) == LocationKeyNormalizer.normalize(lat, lon)
                }
                name to (matchedMaster?.nameEn ?: name)
            }

            list.add(
                City(
                    id = "geo_${id}_$i",
                    name = nameJa,
                    nameEn = nameEn,
                    country = if (regionDesc.isNotEmpty()) regionDesc else country,
                    latitude = lat,
                    longitude = lon
                )
            )
        }
        return list
    }

    private fun httpGetJson(urlString: String): JSONObject? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ExpressiveWeather/1.0 (Android; Mobile)")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.use { it.readText() }
            conn.disconnect()
            JSONObject(response)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 重複排除 & ランキング処理
     */
    private fun rankAndDeduplicate(
        query: String,
        cities: List<City>,
        language: AppLanguage
    ): List<City> {
        val uniqueMap = linkedMapOf<String, City>()
        val trimmed = query.trim()

        for (city in cities) {
            val key = LocationKeyNormalizer.normalize(city.latitude, city.longitude)
            if (!uniqueMap.containsKey(key)) {
                uniqueMap[key] = city
            } else {
                // 既存の都市情報より詳細な情報（name/nameEnの双方保持など）があればマージ
                val existing = uniqueMap[key]!!
                val betterName = if (existing.name.isBlank() || existing.name == existing.nameEn) city.name else existing.name
                val betterNameEn = if (existing.nameEn.isBlank() || existing.nameEn == existing.name) city.nameEn else existing.nameEn
                uniqueMap[key] = existing.copy(name = betterName, nameEn = betterNameEn)
            }
        }

        // スコアリング順でソート
        return uniqueMap.values.sortedWith(
            compareByDescending<City> { city ->
                val primaryName = city.localizedName(language)
                val jaName = city.name
                val enName = city.nameEn

                when {
                    // 完全一致
                    primaryName.equals(trimmed, ignoreCase = true) ||
                            jaName.equals(trimmed, ignoreCase = true) ||
                            enName.equals(trimmed, ignoreCase = true) -> 1000

                    // 「都/市/区」除いた完全一致（例: クエリが「東京」で 都市名が「東京都」）
                    jaName.removeSuffix("市").removeSuffix("都").removeSuffix("府").removeSuffix("区").removeSuffix("県")
                        .equals(trimmed, ignoreCase = true) -> 900

                    // 前方一致
                    primaryName.startsWith(trimmed, ignoreCase = true) ||
                            jaName.startsWith(trimmed, ignoreCase = true) ||
                            enName.startsWith(trimmed, ignoreCase = true) -> 700

                    // 部分一致
                    primaryName.contains(trimmed, ignoreCase = true) ||
                            jaName.contains(trimmed, ignoreCase = true) ||
                            enName.contains(trimmed, ignoreCase = true) -> 500

                    else -> 100
                }
            }
        ).take(15)
    }
}
