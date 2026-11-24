package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import io.github.nemynew.expressiveweather.data.local.LocationKeyNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
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
        val aliases: List<String> = emptyList(),
        val countryKo: String? = null
    ) {
        fun toCity(language: AppLanguage): City {
            val localizedCountry = when (language) {
                AppLanguage.ENGLISH -> countryEn
                AppLanguage.KOREAN -> countryKo ?: localizeCountryToKorean(countryJa, countryEn)
                AppLanguage.SPANISH -> localizeCountryToSpanish(countryJa, countryEn)
                AppLanguage.FRENCH -> localizeCountryToFrench(countryJa, countryEn)
                AppLanguage.PORTUGUESE -> localizeCountryToPortuguese(countryJa, countryEn)
                else -> countryJa
            }
            return City(
                id = "master_${LocationKeyNormalizer.normalize(latitude, longitude)}",
                name = nameJa,
                nameEn = nameEn,
                country = localizedCountry,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    private fun localizeCountryToSpanish(countryJa: String, countryEn: String): String {
        if (countryEn == "Japan") return "Japón"
        if (countryEn.startsWith("Japan ")) return "Japón ${countryEn.removePrefix("Japan ")}"
        if (countryEn == "South Korea") return "Corea del Sur"
        if (countryEn.startsWith("South Korea ")) return "Corea del Sur ${countryEn.removePrefix("South Korea ")}"
        return when (countryEn) {
            "United States" -> "Estados Unidos"
            "United States Hawaii" -> "Estados Unidos Hawái"
            "United Kingdom" -> "Reino Unido"
            "France" -> "Francia"
            "Italy" -> "Italia"
            "Germany" -> "Alemania"
            "Spain" -> "España"
            "Mexico" -> "México"
            "Argentina" -> "Argentina"
            "Colombia" -> "Colombia"
            "Peru" -> "Perú"
            "Chile" -> "Chile"
            "Brazil" -> "Brasil"
            "Portugal" -> "Portugal"
            "Taiwan" -> "Taiwán"
            "China" -> "China"
            "Hong Kong" -> "Hong Kong"
            "Singapore" -> "Singapur"
            "Thailand" -> "Tailandia"
            "Australia" -> "Australia"
            "Canada" -> "Canadá"
            else -> countryEn.ifBlank { countryJa }
        }
    }

    private fun localizeCountryToFrench(countryJa: String, countryEn: String): String {
        if (countryEn == "Japan") return "Japon"
        if (countryEn.startsWith("Japan ")) return "Japon ${countryEn.removePrefix("Japan ")}"
        if (countryEn == "South Korea") return "Corée du Sud"
        if (countryEn.startsWith("South Korea ")) return "Corée du Sud ${countryEn.removePrefix("South Korea ")}"
        return when (countryEn) {
            "United States" -> "États-Unis"
            "United States Hawaii" -> "États-Unis Hawaï"
            "United Kingdom" -> "Royaume-Uni"
            "France" -> "France"
            "Italy" -> "Italie"
            "Germany" -> "Allemagne"
            "Spain" -> "Espagne"
            "Mexico" -> "Mexique"
            "Argentina" -> "Argentine"
            "Colombia" -> "Colombie"
            "Peru" -> "Pérou"
            "Chile" -> "Chili"
            "Brazil" -> "Brésil"
            "Portugal" -> "Portugal"
            "Taiwan" -> "Taïwan"
            "China" -> "Chine"
            "Hong Kong" -> "Hong Kong"
            "Singapore" -> "Singapour"
            "Thailand" -> "Thaïlande"
            "Australia" -> "Australie"
            "Canada" -> "Canada"
            else -> countryEn.ifBlank { countryJa }
        }
    }

    private fun localizeCountryToPortuguese(countryJa: String, countryEn: String): String {
        if (countryEn == "Japan") return "Japão"
        if (countryEn.startsWith("Japan ")) return "Japão ${countryEn.removePrefix("Japan ")}"
        if (countryEn == "South Korea") return "Coreia do Sul"
        if (countryEn.startsWith("South Korea ")) return "Coreia do Sul ${countryEn.removePrefix("South Korea ")}"
        return when (countryEn) {
            "United States" -> "Estados Unidos"
            "United States Hawaii" -> "Estados Unidos Havaí"
            "United Kingdom" -> "Reino Unido"
            "France" -> "França"
            "Italy" -> "Itália"
            "Germany" -> "Alemanha"
            "Spain" -> "Espanha"
            "Mexico" -> "México"
            "Argentina" -> "Argentina"
            "Colombia" -> "Colômbia"
            "Peru" -> "Peru"
            "Chile" -> "Chile"
            "Brazil" -> "Brasil"
            "Portugal" -> "Portugal"
            "Taiwan" -> "Taiwan"
            "China" -> "China"
            "Hong Kong" -> "Hong Kong"
            "Singapore" -> "Singapura"
            "Thailand" -> "Tailândia"
            "Australia" -> "Austrália"
            "Canada" -> "Canadá"
            else -> countryEn.ifBlank { countryJa }
        }
    }

    private fun localizeCountryToKorean(countryJa: String, countryEn: String): String {
        val exactMap = mapOf(
            "日本" to "일본",
            "日本 東京都" to "일본 도쿄",
            "日本 大阪府" to "일본 오사카",
            "日本 神奈川県" to "일본 가나가와",
            "日本 愛知県" to "일본 아이치",
            "日本 北海道" to "일본 홋카이도",
            "日本 福岡県" to "일본 후쿠오카",
            "日本 兵庫県" to "일본 효고",
            "日本 京都府" to "일본 교토",
            "日本 埼玉県" to "일본 사이타마",
            "日本 広島県" to "일본 히로시마",
            "日本 宮城県" to "일본 미야기",
            "日本 千葉県" to "일본 지바",
            "日本 静岡県" to "일본 시즈오카",
            "日本 岡山県" to "일본 오카야마",
            "日本 熊本県" to "일본 구마모토",
            "日本 新潟県" to "일본 니가타",
            "日本 石川県" to "일본 이시카와",
            "日本 沖縄県" to "일본 오키나와",
            "日本 鹿児島県" to "일본 가고시마",
            "日本 長崎県" to "일본 나가사키",
            "日本 愛媛県" to "일본 에히메",
            "日本 香川県" to "일본 가가와",
            "日本 富山県" to "일본 도야마",
            "日本 岐阜県" to "일본 기후",
            "日本 大分県" to "일본 오이타",
            "日本 奈良県" to "일본 나라",
            "日本 栃木県" to "일본 도치기",
            "日本 長野県" to "일본 나가노",
            "日本 岩手県" to "일본 이와테",
            "日本 高知県" to "일본 고치",
            "日本 宮崎県" to "일본 미야자키",
            "日本 福井県" to "일본 후쿠이",
            "日本 秋田県" to "일본 아키타",
            "日本 山形県" to "일본 야마가타",
            "日本 茨城県" to "일본 이바라키",
            "日本 群馬県" to "일본 군마",
            "日本 徳島県" to "일본 도쿠시마",
            "日本 鳥取県" to "일본 돗토리",
            "日本 山梨県" to "일본 야마나시",
            "日本 島根県" to "일본 시마네",
            "日本 佐賀県" to "일본 사가",
            "日本 滋賀県" to "일본 시가",
            "日本 山口県" to "일본 야마구치",
            "日本 三重県" to "일본 미에",
            "日本 青森県" to "일본 아오모리",
            "日本 和歌山県" to "일본 와카야마",
            "大韓民国" to "대한민국",
            "アメリカ合衆国" to "미국",
            "アメリカ合衆国 ハワイ" to "미국 하와이",
            "イギリス" to "영국",
            "フランス" to "프랑스",
            "イタリア" to "이탈리아",
            "ドイツ" to "독일",
            "スペイン" to "스페인",
            "メキシコ" to "멕시코",
            "アルゼンチン" to "아르헨티나",
            "コロンビア" to "콜롬비아",
            "ペルー" to "페루",
            "チリ" to "칠레",
            "ブラジル" to "브라질",
            "ポルトガル" to "포르투갈",
            "台湾" to "대만",
            "中国" to "중국",
            "香港" to "홍콩",
            "シンガポール" to "싱가포르",
            "タイ" to "태국",
            "オーストラリア" to "호주",
            "カナダ" to "캐나다"
        )
        return exactMap[countryJa] ?: countryEn.ifBlank { countryJa }
    }

    // 日本全国47都道府県庁所在地 + 全政令指定都市 + 主要都市・観光地 + 韓国主要都市 + 世界主要大都市の包括マスター辞書
    val MASTER_CITIES: List<MasterCity> = listOf(
        // --- 日本：主要大都市・政令市・都道府県庁所在地 ---
        MasterCity("東京都", "Tokyo", "日本", "Japan", 35.6895, 139.6917, listOf("東京", "とうきょう", "トウキョウ", "tokyo", "tokio", "tóquio", "toquio", "도쿄", "동경")),
        MasterCity("大阪市", "Osaka", "日本 大阪府", "Japan Osaka", 34.6937, 135.5023, listOf("大阪", "おおさか", "オオサカ", "osaka", "osaca", "오사카")),
        MasterCity("横浜市", "Yokohama", "日本 神奈川県", "Japan Kanagawa", 35.4437, 139.6380, listOf("横浜", "よこはま", "ヨコハマ", "yokohama", "요코하마")),
        MasterCity("名古屋市", "Nagoya", "日本 愛知県", "Japan Aichi", 35.1815, 136.9066, listOf("名古屋", "なごや", "ナゴヤ", "nagoya", "나고야")),
        MasterCity("札幌市", "Sapporo", "日本 北海道", "Japan Hokkaido", 43.0618, 141.3545, listOf("札幌", "さっぽろ", "サッポロ", "sapporo", "삿포로")),
        MasterCity("福岡市", "Fukuoka", "日本 福岡県", "Japan Fukuoka", 33.5904, 130.4017, listOf("福岡", "ふくおか", "フクオカ", "fukuoka", "후쿠오카")),
        MasterCity("神戸市", "Kobe", "日本 兵庫県", "Japan Hyogo", 34.6901, 135.1955, listOf("神戸", "こうべ", "コウベ", "kobe", "고베")),
        MasterCity("京都市", "Kyoto", "日本 京都府", "Japan Kyoto", 35.0116, 135.7681, listOf("京都", "きょうと", "キョウト", "kyoto", "kioto", "quioto", "교토")),
        MasterCity("さいたま市", "Saitama", "日本 埼玉県", "Japan Saitama", 35.8617, 139.6455, listOf("さいたま", "サイタマ", "埼玉", "saitama", "사이타마")),
        MasterCity("広島市", "Hiroshima", "日本 広島県", "Japan Hiroshima", 34.3853, 132.4553, listOf("広島", "ひろしま", "ヒロシマ", "hiroshima", "히로시마")),
        MasterCity("仙台市", "Sendai", "日本 宮城県", "Japan Miyagi", 38.2682, 140.8694, listOf("仙台", "せんだい", "センダイ", "sendai", "센다이")),
        MasterCity("千葉市", "Chiba", "日本 千葉県", "Japan Chiba", 35.6074, 140.1065, listOf("千葉", "ちば", "チバ", "chiba", "지바", "치바")),
        MasterCity("北九州市", "Kitakyushu", "日本 福岡県", "Japan Fukuoka", 33.8835, 130.8752, listOf("北九州", "きたきゅうしゅう", "kitakyushu", "기타큐슈")),
        MasterCity("川崎市", "Kawasaki", "日本 神奈川県", "Japan Kanagawa", 35.5308, 139.7029, listOf("川崎", "かわさき", "kawasaki", "가와사키")),
        MasterCity("相模原市", "Sagamihara", "日本 神奈川県", "Japan Kanagawa", 35.5713, 139.3732, listOf("相模原", "さがみはら", "sagamihara", "사가미하라")),
        MasterCity("堺市", "Sakai", "日本 大阪府", "Japan Osaka", 34.5733, 135.4830, listOf("堺", "さかい", "sakai", "사카이")),
        MasterCity("静岡市", "Shizuoka", "日本 静岡県", "Japan Shizuoka", 34.9756, 138.3828, listOf("静岡", "しずおか", "shizuoka", "시즈오카")),
        MasterCity("浜松市", "Hamamatsu", "日本 静岡県", "Japan Shizuoka", 34.7108, 137.7261, listOf("浜松", "はままつ", "hamamatsu", "하마마쓰")),
        MasterCity("岡山市", "Okayama", "日本 岡山県", "Japan Okayama", 34.6551, 133.9195, listOf("岡山", "おかやま", "okayama", "오카야마")),
        MasterCity("熊本市", "Kumamoto", "日本 熊本県", "Japan Kumamoto", 32.7898, 130.7417, listOf("熊本", "くまもと", "kumamoto", "구마모토")),
        MasterCity("新潟市", "Niigata", "日本 新潟県", "Japan Niigata", 37.9161, 139.0364, listOf("新潟", "にいがた", "niigata", "니가타")),
        MasterCity("金沢市", "Kanazawa", "日本 石川県", "Japan Ishikawa", 36.5613, 136.6562, listOf("金沢", "かなざわ", "kanazawa", "가나자와")),
        MasterCity("那覇市", "Naha", "日本 沖縄県", "Japan Okinawa", 26.2124, 127.6809, listOf("那覇", "なは", "ナハ", "沖縄", "おきなわ", "naha", "okinawa", "나하", "오키나와")),
        MasterCity("鹿児島市", "Kagoshima", "日本 鹿児島県", "Japan Kagoshima", 31.5966, 130.5571, listOf("鹿児島", "かごしま", "kagoshima", "가고시마")),
        MasterCity("長崎市", "Nagasaki", "日本 長崎県", "Japan Nagasaki", 32.7503, 129.8777, listOf("長崎", "ながさき", "nagasaki", "나가사키")),
        MasterCity("松山市", "Matsuyama", "日本 愛媛県", "Japan Ehime", 33.8392, 132.7656, listOf("松山", "まつやま", "matsuyama", "마쓰야마")),
        MasterCity("高松市", "Takamatsu", "日本 香川県", "Japan Kagawa", 34.3428, 134.0466, listOf("高松", "たかまつ", "takamatsu", "다카마쓰")),
        MasterCity("富山市", "Toyama", "日本 富山県", "Japan Toyama", 36.6953, 137.2113, listOf("富山", "とやま", "toyama", "도야마")),
        MasterCity("岐阜市", "Gifu", "日本 岐阜県", "Japan Gifu", 35.4233, 136.7607, listOf("岐阜", "ぎふ", "gifu", "기후")),
        MasterCity("大分市", "Oita", "日本 大分県", "Japan Oita", 33.2382, 131.6126, listOf("大分", "おおいた", "oita", "오이타")),
        MasterCity("奈良市", "Nara", "日本 奈良県", "Japan Nara", 34.6851, 135.8048, listOf("奈良", "なら", "nara", "나라")),
        MasterCity("宇都宮市", "Utsunomiya", "日本 栃木県", "Japan Tochigi", 36.5551, 139.8828, listOf("宇都宮", "うつのみや", "utsunomiya", "우쓰노미야")),
        MasterCity("長野市", "Nagano", "日本 長野県", "Japan Nagano", 36.6513, 138.1810, listOf("長野", "ながの", "nagano", "나가노")),
        MasterCity("盛岡市", "Morioka", "日本 岩手県", "Japan Iwate", 39.7036, 141.1527, listOf("盛岡", "もりおか", "morioka", "모리오카")),
        MasterCity("高知市", "Kochi", "日本 高知県", "Japan Kochi", 33.5597, 133.5311, listOf("高知", "こうち", "kochi", "고치")),
        MasterCity("宮崎市", "Miyazaki", "日本 宮崎県", "Japan Miyazaki", 31.9077, 131.4202, listOf("宮崎", "みやざき", "miyazaki", "미야자키")),
        MasterCity("福井市", "Fukui", "日本 福井県", "Japan Fukui", 36.0641, 136.2195, listOf("福井", "ふくい", "fukui", "후쿠이")),
        MasterCity("秋田市", "Akita", "日本 秋田県", "Japan Akita", 39.7200, 140.1026, listOf("秋田", "あきた", "akita", "아키타")),
        MasterCity("山形市", "Yamagata", "日本 山形県", "Japan Yamagata", 38.2554, 140.3396, listOf("山形", "やまがた", "yamagata", "야마가타")),
        MasterCity("水戸市", "Mito", "日本 茨城県", "Japan Ibaraki", 36.3659, 140.4712, listOf("水戸", "みと", "mito", "미토")),
        MasterCity("前橋市", "Maebashi", "日本 群馬県", "Japan Gunma", 36.3895, 139.0634, listOf("前橋", "まえばし", "maebashi", "마에바시")),
        MasterCity("徳島市", "Tokushima", "日本 徳島県", "Japan Tokushima", 34.0703, 134.5548, listOf("徳島", "とくしま", "tokushima", "도쿠시마")),
        MasterCity("鳥取市", "Tottori", "日本 鳥取県", "Japan Tottori", 35.5011, 134.2351, listOf("鳥取", "とっとり", "tottori", "돗토리")),
        MasterCity("甲府市", "Kofu", "日本 山梨県", "Japan Yamanashi", 35.6622, 138.5684, listOf("甲府", "こうふ", "kofu", "고후")),
        MasterCity("松江市", "Matsue", "日本 島根県", "Japan Shimane", 35.4681, 133.0484, listOf("松江", "まつえ", "matsue", "마쓰에")),
        MasterCity("佐賀市", "Saga", "日本 佐賀県", "Japan Saga", 33.2635, 130.3009, listOf("佐賀", "さが", "saga", "사가")),
        MasterCity("大津市", "Otsu", "日本 滋賀県", "Japan Shiga", 35.0178, 135.8547, listOf("大津", "おおつ", "otsu", "오쓰")),
        MasterCity("山口市", "Yamaguchi", "日本 山口県", "Japan Yamaguchi", 34.1785, 131.4737, listOf("山口", "やまぐち", "yamaguchi", "야마구치")),
        MasterCity("津市", "Tsu", "日本 三重県", "Japan Mie", 34.7186, 136.5053, listOf("津", "つ", "tsu")),
        MasterCity("青森市", "Aomori", "日本 青森県", "Japan Aomori", 40.8244, 140.7400, listOf("青森", "あおもり", "aomori", "아오모리")),
        MasterCity("和歌山市", "Wakayama", "日本 和歌山県", "Japan Wakayama", 34.2305, 135.1708, listOf("和歌山", "わかやま", "wakayama", "와카야마")),

        // --- 東京主要エリア・区 ---
        MasterCity("新宿区", "Shinjuku", "日本 東京都", "Japan Tokyo", 35.6938, 139.7036, listOf("新宿", "しんじゅく", "shinjuku", "신주쿠", "신주쿠구")),
        MasterCity("渋谷区", "Shibuya", "日本 東京都", "Japan Tokyo", 35.6580, 139.7016, listOf("渋谷", "しぶや", "shibuya", "시부야", "시부야구")),
        MasterCity("港区", "Minato", "日本 東京都", "Japan Tokyo", 35.6581, 139.7515, listOf("六本木", "港区", "みなと", "roppongi", "minato", "미나토구", "롯폰기")),
        MasterCity("中央区", "Chuo", "日本 東京都", "Japan Tokyo", 35.6707, 139.7720, listOf("銀座", "日本橋", "中央区", "ginza", "chuo", "긴자", "주오구")),
        MasterCity("千代田区", "Chiyoda", "日本 東京都", "Japan Tokyo", 35.6940, 139.7536, listOf("千代田", "秋葉原", "神田", "akihabara", "chiyoda", "치요다구", "아키하바라")),
        MasterCity("豊島区", "Toshima", "日本 東京都", "Japan Tokyo", 35.7289, 139.7126, listOf("池袋", "豊島", "いけぶくろ", "ikebukuro", "이케부쿠로")),
        MasterCity("台東区", "Taito", "日本 東京都", "Japan Tokyo", 35.7126, 139.7799, listOf("上野", "浅草", "台東", "ueno", "asakusa", "우에노", "아사쿠사")),
        MasterCity("品川区", "Shinagawa", "日本 東京都", "Japan Tokyo", 35.6092, 139.7303, listOf("品川", "しながわ", "shinagawa", "시나가와")),
        MasterCity("世田谷区", "Setagaya", "日本 東京都", "Japan Tokyo", 35.6466, 139.6533, listOf("世田谷", "せたがや", "setagaya", "세타가야")),

        // --- 人気観光地・特徴的エリア ---
        MasterCity("日光市", "Nikko", "日本 栃木県", "Japan Tochigi", 36.7554, 139.5986, listOf("日光", "にっこう", "nikko", "닛코")),
        MasterCity("箱根町", "Hakone", "日本 神奈川県", "Japan Kanagawa", 35.2323, 139.1069, listOf("箱根", "はこね", "hakone", "하코네")),
        MasterCity("軽井沢町", "Karuizawa", "日本 長野県", "Japan Nagano", 36.3488, 138.6358, listOf("軽井沢", "かるいざわ", "karuizawa", "가루이자와")),
        MasterCity("石垣市", "Ishigaki", "日本 沖縄県", "Japan Okinawa", 24.3448, 124.1572, listOf("石垣", "石垣島", "いしがき", "ishigaki", "이시가키")),
        MasterCity("宮古島市", "Miyakojima", "日本 沖縄県", "Japan Okinawa", 24.8055, 125.2811, listOf("宮古島", "宮古", "みやこじま", "miyakojima", "미야코지마")),
        MasterCity("函館市", "Hakodate", "日本 北海道", "Japan Hokkaido", 41.7687, 140.7288, listOf("函館", "はこだて", "hakodate", "하코다테")),
        MasterCity("旭川市", "Asahikawa", "日本 北海道", "Japan Hokkaido", 43.7706, 142.3649, listOf("旭川", "あさひかわ", "asahikawa", "아사히카와")),
        MasterCity("小樽市", "Otaru", "日本 北海道", "Japan Hokkaido", 43.1907, 140.9947, listOf("小樽", "おたる", "otaru", "오타루")),
        MasterCity("鎌倉市", "Kamakura", "日本 神奈川県", "Japan Kanagawa", 35.3192, 139.5467, listOf("鎌倉", "かまくら", "kamakura", "가마쿠라")),
        MasterCity("姫路市", "Himeji", "日本 兵庫県", "Japan Hyogo", 34.8151, 134.6853, listOf("姫路", "ひめじ", "himeji", "히메지")),
        MasterCity("別府市", "Beppu", "日本 大分県", "Japan Oita", 33.2846, 131.4912, listOf("別府", "べっぷ", "beppu", "벳푸")),

        // --- 大韓民国 主要都市 ---
        MasterCity("ソウル", "Seoul", "大韓民国", "South Korea", 37.5665, 126.9780, listOf("ソウル", "そうる", "seoul", "seúl", "séoul", "seul", "서울", "서울특별시", "서울시"), "대한민국"),
        MasterCity("釜山", "Busan", "大韓民国", "South Korea", 35.1796, 129.0756, listOf("釜山", "プサン", "ぷさん", "busan", "busán", "pousan", "부산", "부산광역시", "부산시"), "대한민국"),
        MasterCity("仁川", "Incheon", "大韓民国", "South Korea", 37.4563, 126.7052, listOf("仁川", "インチョン", "いんちょん", "incheon", "인천", "인천광역시", "인천시"), "대한민국"),
        MasterCity("大邱", "Daegu", "大韓民国", "South Korea", 35.8714, 128.6014, listOf("大邱", "テグ", "てぐ", "daegu", "대구", "대구광역시", "대구시"), "대한민국"),
        MasterCity("大田", "Daejeon", "大韓民国", "South Korea", 36.3504, 127.3845, listOf("大田", "テジョン", "てじょん", "daejeon", "대전", "대전광역시", "대전시"), "대한민국"),
        MasterCity("光州", "Gwangju", "大韓民国", "South Korea", 35.1595, 126.8526, listOf("光州", "クァンジュ", "gwangju", "광주", "광주광역시", "광주시"), "대한민국"),
        MasterCity("蔚山", "Ulsan", "大韓民国", "South Korea", 35.5384, 129.3114, listOf("蔚山", "ウルサン", "うるさん", "ulsan", "울산", "울산광역시", "울산시"), "대한민국"),
        MasterCity("水原", "Suwon", "大韓民国 京畿道", "South Korea Gyeonggi", 37.2636, 127.0286, listOf("水原", "スウォン", "suwon", "수원", "수원시"), "대한민국 경기도"),
        MasterCity("済州", "Jeju", "大韓民国 済州特別自治道", "South Korea Jeju", 33.4996, 126.5312, listOf("済州", "済州島", "チェジュ", "ちぇじゅ", "jeju", "제주", "제주시", "제주도", "제주특별자치도"), "대한민국 제주특별자치도"),
        MasterCity("世宗", "Sejong", "大韓民国", "South Korea", 36.4800, 127.2890, listOf("世宗", "セジョン", "sejong", "세종", "세종특별자치시", "세종시"), "대한민국"),
        MasterCity("城南", "Seongnam", "大韓民国 京畿道", "South Korea Gyeonggi", 37.4200, 127.1267, listOf("城南", "ソンナム", "seongnam", "성남", "성남시", "분당", "판교"), "대한민국 경기도"),
        MasterCity("高陽", "Goyang", "大韓民国 京畿道", "South Korea Gyeonggi", 37.6584, 126.8320, listOf("高陽", "コヤン", "goyang", "고양", "고양시", "일산"), "대한민국 경기도"),
        MasterCity("龍仁", "Yongin", "大韓民国 京畿道", "South Korea Gyeonggi", 37.2411, 127.1776, listOf("龍仁", "ヨンイン", "yongin", "용인", "용인시"), "대한민국 경기도"),
        MasterCity("昌原", "Changwon", "大韓民国 慶尚南道", "South Korea South Gyeongsang", 35.2280, 128.6811, listOf("昌原", "チャンウォン", "changwon", "창원", "창원시"), "대한민국 경상남도"),
        MasterCity("清州", "Cheongju", "大韓民国 忠清北道", "South Korea North Chungcheong", 36.6424, 127.4890, listOf("清州", "チョンジュ", "cheongju", "청주", "청주시"), "대한민국 충청북도"),
        MasterCity("全州", "Jeonju", "大韓民国 全北特別自治道", "South Korea Jeonbuk", 35.8242, 127.1480, listOf("全州", "チョンジュ", "jeonju", "전주", "전주시"), "대한민국 전북특별자치도"),
        MasterCity("天安", "Cheonan", "大韓民国 忠清南道", "South Korea South Chungcheong", 36.8151, 127.1139, listOf("天安", "チョナン", "cheonan", "천안", "천안시"), "대한민국 충청남도"),
        MasterCity("春川", "Chuncheon", "大韓民国 江原特別自治道", "South Korea Gangwon", 37.8813, 127.7298, listOf("春川", "チュンチョン", "chuncheon", "춘천", "춘천시"), "대한민국 강원특별자치도"),
        MasterCity("江陵", "Gangneung", "大韓民国 江原特別自治道", "South Korea Gangwon", 37.7519, 128.8761, listOf("江陵", "カンヌン", "gangneung", "강릉", "강릉시"), "대한민국 강원특별자치도"),
        MasterCity("浦項", "Pohang", "大韓民国 慶尚北道", "South Korea North Gyeongsang", 36.0190, 129.3435, listOf("浦項", "ポハン", "pohang", "포항", "포항시"), "대한민국 경상북도"),
        MasterCity("慶州", "Gyeongju", "大韓民国 慶尚北道", "South Korea North Gyeongsang", 35.8562, 129.2247, listOf("慶州", "キョンジュ", "gyeongju", "경주", "경주시"), "대한민국 경상북도"),
        MasterCity("麗水", "Yeosu", "大韓民国 全羅南道", "South Korea South Jeolla", 34.7604, 127.6622, listOf("麗水", "ヨス", "yeosu", "여수", "여수시"), "대한민국 전라남도"),

        // --- 世界主要大都市 ---
        MasterCity("ニューヨーク", "New York", "アメリカ合衆国", "United States", 40.7128, -74.0060, listOf("ニューヨーク", "ny", "nyc", "new york", "nueva york", "nova york", "nova iorque", "뉴욕")),
        MasterCity("ロサンゼルス", "Los Angeles", "アメリカ合衆国", "United States", 34.0522, -118.2437, listOf("ロサンゼルス", "ロス", "la", "los angeles", "los ángeles", "로스앤젤레스")),
        MasterCity("サンフランシスコ", "San Francisco", "アメリカ合衆国", "United States", 37.7749, -122.4194, listOf("サンフランシスコ", "san francisco", "são francisco", "샌프란시스코")),
        MasterCity("ロンドン", "London", "イギリス", "United Kingdom", 51.5074, -0.1278, listOf("ロンドン", "london", "londres", "런던")),
        MasterCity("パリ", "Paris", "フランス", "France", 48.8566, 2.3522, listOf("パリ", "paris", "parís", "파리")),
        MasterCity("リヨン", "Lyon", "フランス", "France", 45.7640, 4.8357, listOf("リヨン", "lyon", "리옹")),
        MasterCity("マルセイユ", "Marseille", "フランス", "France", 43.2965, 5.3698, listOf("マルセイユ", "marseille", "marsella", "marselha", "마르세유")),
        MasterCity("トゥールーズ", "Toulouse", "フランス", "France", 43.6047, 1.4442, listOf("トゥールーズ", "toulouse", "툴루즈")),
        MasterCity("ニース", "Nice", "フランス", "France", 43.7102, 7.2620, listOf("ニース", "nice", "niza", "니스")),
        MasterCity("ボルドー", "Bordeaux", "フランス", "France", 44.8378, -0.5792, listOf("ボルドー", "bordeaux", "burdeos", "bordéus", "보르도")),
        MasterCity("ローマ", "Rome", "イタリア", "Italy", 41.9028, 12.4964, listOf("ローマ", "rome", "roma", "로마")),
        MasterCity("ベルリン", "Berlin", "ドイツ", "Germany", 52.5200, 13.4050, listOf("ベルリン", "berlin", "berlín", "berlim", "베를린")),
        MasterCity("マドリード", "Madrid", "スペイン", "Spain", 40.4168, -3.7038, listOf("マドリード", "madrid", "madri", "마드리드")),
        MasterCity("バルセロナ", "Barcelona", "スペイン", "Spain", 41.3874, 2.1686, listOf("バルセロナ", "barcelona", "barcelone", "바르셀로나")),
        MasterCity("バレンシア", "Valencia", "スペイン", "Spain", 39.4699, -0.3763, listOf("バレンシア", "valencia", "valència", "valence", "valência", "발렌시아")),
        MasterCity("セビリア", "Seville", "スペイン", "Spain", 37.3891, -5.9845, listOf("セビリア", "セビージャ", "sevilla", "seville", "séville", "sevilha", "세비야")),
        MasterCity("メキシコシティ", "Mexico City", "メキシコ", "Mexico", 19.4326, -99.1332, listOf("メキシコシティ", "ciudad de méxico", "ciudad de mexico", "cdmx", "mexico city", "mexico", "cidade do méxico", "cidade do mexico", "멕시코시티")),
        MasterCity("ブエノスアイレス", "Buenos Aires", "アルゼンチン", "Argentina", -34.6037, -58.3816, listOf("ブエノスアイレス", "buenos aires", "부에노스아이레스")),
        MasterCity("ボゴタ", "Bogota", "コロンビア", "Colombia", 4.7110, -74.0721, listOf("ボゴタ", "bogotá", "bogota", "보고타")),
        MasterCity("リマ", "Lima", "ペルー", "Peru", -12.0464, -77.0428, listOf("リマ", "lima", "리마")),
        MasterCity("サンティアゴ", "Santiago", "チリ", "Chile", -33.4489, -70.6693, listOf("サンティアゴ", "santiago", "santiago de chile", "산티아고")),
        MasterCity("サンパウロ", "Sao Paulo", "ブラジル", "Brazil", -23.5505, -46.6333, listOf("サンパウロ", "são paulo", "sao paulo", "상파울루")),
        MasterCity("リオデジャネイロ", "Rio de Janeiro", "ブラジル", "Brazil", -22.9068, -43.1729, listOf("リオデジャネイロ", "リオ", "rio de janeiro", "rio", "리우데자네이루")),
        MasterCity("ブラジリア", "Brasilia", "ブラジル", "Brazil", -15.7975, -47.8919, listOf("ブラジリア", "brasília", "brasilia", "브라질리아")),
        MasterCity("リスボン", "Lisbon", "ポルトガル", "Portugal", 38.7223, -9.1393, listOf("リスボン", "lisboa", "lisbon", "lisbonne", "리스본")),
        MasterCity("ポルト", "Porto", "ポルトガル", "Portugal", 41.1579, -8.6291, listOf("ポルト", "porto", "oporto", "포르투")),
        MasterCity("台北", "Taipei", "台湾", "Taiwan", 25.0330, 121.5654, listOf("台北", "タイペイ", "たいぺい", "taipei", "taipéi", "타이베이", "타이페이")),
        MasterCity("北京", "Beijing", "中国", "China", 39.9042, 116.4074, listOf("北京", "ペキン", "beijing", "pekín", "pékin", "pequim", "베이징", "북경")),
        MasterCity("上海", "Shanghai", "中国", "China", 31.2304, 121.4737, listOf("上海", "シャンハイ", "shanghai", "shanghái", "xangai", "상하이", "상해")),
        MasterCity("香港", "Hong Kong", "香港", "Hong Kong", 22.3193, 114.1694, listOf("香港", "ほんこん", "ホンコン", "hong kong", "홍콩")),
        MasterCity("シンガポール", "Singapore", "シンガポール", "Singapore", 1.3521, 103.8198, listOf("シンガポール", "singapore", "singapur", "singapour", "singapura", "싱가포르")),
        MasterCity("バンコク", "Bangkok", "タイ", "Thailand", 13.7563, 100.5018, listOf("バンコク", "bangkok", "방콕")),
        MasterCity("シドニー", "Sydney", "オーストラリア", "Australia", -33.8688, 151.2093, listOf("シドニー", "sydney", "sídney", "sidney", "시드니")),
        MasterCity("ホノルル", "Honolulu", "アメリカ合衆国 ハワイ", "United States Hawaii", 21.3069, -157.8583, listOf("ホノルル", "ハワイ", "honolulu", "hawaii", "hawái", "havaí", "호놀룰루", "하와이")),
        MasterCity("トロント", "Toronto", "カナダ", "Canada", 43.6532, -79.3832, listOf("トロント", "toronto", "토론토")),
        MasterCity("モントリオール", "Montreal", "カナダ", "Canada", 45.5017, -73.5673, listOf("モントリオール", "montréal", "montreal", "몬트리올")),
        MasterCity("バンクーバー", "Vancouver", "カナダ", "Canada", 49.2827, -123.1207, listOf("バンクーバー", "vancouver", "밴쿠버"))
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
        context: Context? = null,
        allowNetwork: Boolean = true
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
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Geocoder 失敗時は Open-Meteo にフォールバック
            }
        }

        // -------------------------------------------------------------
        // 層 3: Open-Meteo Geocoding API（多言語・クエリ拡張並行フェッチ）
        // -------------------------------------------------------------
        if (allowNetwork) {
            try {
                val openMeteoResults = searchOpenMeteo(trimmed, expandedQueries, language)
                collectedCities.addAll(openMeteoResults)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // API 通信エラー時もローカル・Geocoder 結果を維持
            }
        }

        // -------------------------------------------------------------
        // マージ・重複排除・スコアリング
        // -------------------------------------------------------------
        rankAndDeduplicate(trimmed, collectedCities, language)
    }

    /**
     * オフライン主要都市辞書からのインクリメンタル検索
     */
    internal fun searchLocalMaster(query: String, language: AppLanguage): List<City> {
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
        val primaryLocale = when (language) {
            AppLanguage.KOREAN -> Locale.KOREA
            AppLanguage.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.SPANISH -> Locale("es", "ES")
            AppLanguage.FRENCH -> Locale.FRANCE
            AppLanguage.PORTUGUESE -> Locale("pt", "BR")
            AppLanguage.JAPANESE -> Locale.JAPAN
        }
        val primaryGeocoder = Geocoder(context, primaryLocale)
        val enGeocoder = Geocoder(context, Locale.US)

        val list = mutableListOf<City>()

        for (q in queriesToTry) {
            val primaryAddresses = getAddressesFromGeocoder(primaryGeocoder, q, 5)
            if (primaryAddresses.isNotEmpty()) {
                val resolved = primaryAddresses.map { addr ->
                    async {
                        val lat = addr.latitude
                        val lon = addr.longitude

                        val enName = try {
                            val enAddrs = getAddressesFromLocation(enGeocoder, lat, lon, 1)
                            enAddrs.firstOrNull()?.let { resolveName(it, language = AppLanguage.ENGLISH) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            null
                        }

                        val localName = resolveName(addr, language = language)
                        val defaultCountry = when (language) {
                            AppLanguage.KOREAN -> "대한민국"
                            AppLanguage.ENGLISH -> "Unknown"
                            AppLanguage.SIMPLIFIED_CHINESE -> "未知地区"
                            AppLanguage.TRADITIONAL_CHINESE -> "未知地區"
                            AppLanguage.SPANISH -> "Desconocido"
                            AppLanguage.FRENCH -> "Inconnu"
                            AppLanguage.PORTUGUESE -> "Desconhecido"
                            AppLanguage.JAPANESE -> "日本"
                        }
                        val localCountry = addr.countryName ?: (addr.adminArea ?: defaultCountry)
                        val enCountry = when (addr.countryCode) {
                            "JP" -> "Japan"
                            "KR" -> "South Korea"
                            else -> addr.countryName ?: ""
                        }
                        val finalNameEn = if (!enName.isNullOrBlank()) enName else localName

                        City(
                            id = "geo_${LocationKeyNormalizer.normalize(lat, lon)}",
                            name = localName,
                            nameEn = finalNameEn,
                            country = if (language == AppLanguage.ENGLISH) enCountry else localCountry,
                            latitude = lat,
                            longitude = lon
                        )
                    }
                }.awaitAll()
                list.addAll(resolved)
                break // 最も関連性の高いクエリで結果が得られたらループ終了
            }
        }
        list
    }

    private suspend fun getAddressesFromGeocoder(geocoder: Geocoder, query: String, maxResults: Int): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(2500L) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(
                        query,
                        maxResults,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(emptyList())
                            }
                        }
                    )
                }
            } ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query, maxResults) ?: emptyList()
        }
    }

    private suspend fun getAddressesFromLocation(geocoder: Geocoder, lat: Double, lon: Double, maxResults: Int): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(
                        lat,
                        lon,
                        maxResults,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(emptyList())
                            }
                        }
                    )
                }
            } ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, maxResults) ?: emptyList()
        }
    }

    private fun resolveName(addr: Address, language: AppLanguage): String {
        val fallback = when (language) {
            AppLanguage.ENGLISH -> "Unknown"
            AppLanguage.KOREAN -> "알 수 없는 지역"
            AppLanguage.SIMPLIFIED_CHINESE -> "未知地区"
            AppLanguage.TRADITIONAL_CHINESE -> "未知地區"
            AppLanguage.SPANISH -> "Ubicación desconocida"
            AppLanguage.FRENCH -> "Lieu inconnu"
            AppLanguage.PORTUGUESE -> "Local desconhecido"
            AppLanguage.JAPANESE -> "不明な地域"
        }
        return addr.locality
            ?: addr.subLocality
            ?: addr.subAdminArea
            ?: addr.adminArea
            ?: addr.featureName
            ?: fallback
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
            AppLanguage.KOREAN -> "ko"
            AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "zh"
            AppLanguage.SPANISH -> "es"
            AppLanguage.FRENCH -> "fr"
            AppLanguage.PORTUGUESE -> "pt"
            AppLanguage.JAPANESE -> "ja"
        }

        val jobs = queriesToTry.map { q ->
            async {
                fetchOpenMeteoSingle(q, apiLang, language)
            }
        }

        jobs.awaitAll().flatten()
    }

    private suspend fun fetchOpenMeteoSingle(query: String, apiLang: String, appLanguage: AppLanguage): List<City> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val urlStr = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=$apiLang&format=json"

        val json = AppHttpClient.getJsonOrNull(urlStr, timeoutMs = 8_000L) ?: return emptyList()
        val results = json.optJSONArray("results") ?: return emptyList()

        val list = mutableListOf<City>()
        for (i in 0 until results.length()) {
            try {
                val obj = results.optJSONObject(i) ?: continue
                val id = obj.optNonNullString("id", "city_$i")
                val name = obj.optNonNullString("name", query).ifBlank { query }
                val country = obj.optNonNullString("country")
                val admin1 = obj.optNonNullString("admin1")
                val admin2 = obj.optNonNullString("admin2")
                val lat = obj.optFiniteDouble("latitude") ?: continue
                val lon = obj.optFiniteDouble("longitude") ?: continue

                val regionParts = listOf(country, admin1, admin2).filter { it.isNotBlank() }.distinct()
                val regionDesc = regionParts.joinToString(" ")

                val matchedMaster = MASTER_CITIES.firstOrNull {
                    LocationKeyNormalizer.normalize(it.latitude, it.longitude) == LocationKeyNormalizer.normalize(lat, lon)
                }
                val (nameJa, nameEn) = if (apiLang == "en" || apiLang == "es" || apiLang == "fr" || apiLang == "pt") {
                    (matchedMaster?.nameJa ?: name) to name
                } else {
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
            } catch (_: Exception) {
                // Skip malformed city item without failing the entire search
            }
        }
        return list
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

                    // 「都/市/区」や韓国語の行政接尾辞を除いた完全一致（例: 「東京」↔「東京都」、「서울」↔「서울시」）
                    jaName.removeSuffix("市").removeSuffix("都").removeSuffix("府").removeSuffix("区").removeSuffix("県")
                        .equals(trimmed, ignoreCase = true) ||
                            primaryName.removeSuffix("특별시").removeSuffix("광역시").removeSuffix("특별자치시")
                                .removeSuffix("특별자치도").removeSuffix("시").removeSuffix("구")
                                .equals(
                                    trimmed.removeSuffix("특별시").removeSuffix("광역시").removeSuffix("특별자치시")
                                        .removeSuffix("특별자치도").removeSuffix("시").removeSuffix("구"),
                                    ignoreCase = true
                                ) -> 900

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
