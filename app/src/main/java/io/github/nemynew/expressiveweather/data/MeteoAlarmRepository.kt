package io.github.nemynew.expressiveweather.data

import android.util.Log
import io.github.nemynew.expressiveweather.ui.components.WeatherWarningType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 欧州気象警報ネットワーク (MeteoAlarm / EUMETNET) 公式気象警報取得リポジトリ
 *
 * エンドポイント: https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-{country}
 * - 欧州気象機関ネットワーク (EUMETNET) に加盟する欧州39カ国の国立気象機関
 *   (DWD:ドイツ, Météo-France:フランス, AEMET:スペイン, Met Office:イギリス, ZAMG/GeoSphere:オーストリア等)
 *   による公式CAP気象警報をリアルタイム集約。
 * - 新カラー体系に準拠:
 *   - 黄色 (Moderate / Minor): 注意 (ADVISORY)
 *   - 橙色 (Severe): 警戒 (WARNING)
 *   - 赤色 (Extreme): 特別警戒 (SPECIAL_WARNING)
 * - 11種の公式危険種別 (Awareness Types):
 *   Wind, Snow/Ice, Thunderstorms, Fog, High Temperature, Low Temperature,
 *   Coastal Event, Forest Fire, Avalanches, Rain, Flooding
 */
object MeteoAlarmRepository {
    private const val TAG = "MeteoAlarmRepository"
    private const val BASE_FEED_URL = "https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-"
    private const val USER_AGENT = "ExpressiveWeather/1.0 (Android; +https://github.com/nemy-new/Expressive-Weather)"

    // 国別の生データ15分間メモリキャッシュ
    private val feedCache = ConcurrentHashMap<String, Pair<Long, List<MeteoAlarmRawEntry>>>()
    private const val CACHE_TTL_MS = 15 * 60 * 1000L
    private val fetchMutex = Mutex()

    /**
     * MeteoAlarm Atomフィードの生警報エントリ
     */
    data class MeteoAlarmRawEntry(
        val id: String,
        val title: String,
        val event: String,
        val areaDesc: String,
        val severity: String,
        val onset: String,
        val expires: String,
        val sent: String,
        val identifier: String,
        val capXmlUrl: String?
    )

    /**
     * 欧州気象機関ネットワーク (EUMETNET / MeteoAlarm) 公式加盟全39カ国および関連地域マッピング
     * (公式Atomフィード: feeds.meteoalarm.org で実稼働確認済み39スラグ)
     * 日本語名、英語名、公用語名、ISO 3166-1 alpha-2 / alpha-3 コードに対応
     */
    private val COUNTRY_SLUG_MAP = mapOf(
        // 1. Andorra
        "andorra" to "andorra", "andorre" to "andorra", "ad" to "andorra", "and" to "andorra", "アンドラ" to "andorra", "안도라" to "andorra",
        // 2. Austria
        "austria" to "austria", "autriche" to "austria", "áustria" to "austria", "at" to "austria", "aut" to "austria", "österreich" to "austria", "oesterreich" to "austria", "オーストリア" to "austria", "오스트리아" to "austria",
        // 3. Belgium
        "belgium" to "belgium", "bélgica" to "belgium", "belgica" to "belgium", "be" to "belgium", "bel" to "belgium", "belgique" to "belgium", "belgië" to "belgium", "belgie" to "belgium", "ベルギー" to "belgium", "벨기에" to "belgium",
        // 4. Bosnia and Herzegovina
        "bosnia and herzegovina" to "bosnia-herzegovina", "bosnia-herzegovina" to "bosnia-herzegovina", "bosnia" to "bosnia-herzegovina",
        "bosnia y herzegovina" to "bosnia-herzegovina", "bosnie-herzégovine" to "bosnia-herzegovina", "bosnie-herzegovine" to "bosnia-herzegovina", "bosnie" to "bosnia-herzegovina",
        "bósnia e herzegovina" to "bosnia-herzegovina", "bosnia e herzegovina" to "bosnia-herzegovina", "bósnia" to "bosnia-herzegovina",
        "ba" to "bosnia-herzegovina", "bih" to "bosnia-herzegovina", "ボスニア" to "bosnia-herzegovina", "ボスニア・ヘルツェゴビナ" to "bosnia-herzegovina", "보스니아" to "bosnia-herzegovina",
        // 5. Bulgaria
        "bulgaria" to "bulgaria", "bulgarie" to "bulgaria", "bulgária" to "bulgaria", "bg" to "bulgaria", "bgr" to "bulgaria", "ブルガリア" to "bulgaria", "불가리아" to "bulgaria",
        // 6. Croatia
        "croatia" to "croatia", "croacia" to "croatia", "croatie" to "croatia", "croácia" to "croatia", "hr" to "croatia", "hrv" to "croatia", "hrvatska" to "croatia", "クロアチア" to "croatia", "크로아티아" to "croatia",
        // 7. Cyprus
        "cyprus" to "cyprus", "chipre" to "cyprus", "chypre" to "cyprus", "cy" to "cyprus", "cyp" to "cyprus", "キプロス" to "cyprus", "키프로스" to "cyprus",
        // 8. Czechia
        "czechia" to "czechia", "czech republic" to "czechia", "chequia" to "czechia", "república checa" to "czechia", "republica checa" to "czechia",
        "tchéquie" to "czechia", "tchequie" to "czechia", "république tchèque" to "czechia", "republique tcheque" to "czechia",
        "chéquia" to "czechia", "tchéquia" to "czechia", "república tcheca" to "czechia", "republica tcheca" to "czechia",
        "cz" to "czechia", "cze" to "czechia", "česko" to "czechia", "cesko" to "czechia", "チェコ" to "czechia", "チェコ共和国" to "czechia", "체코" to "czechia",
        // 9. Denmark
        "denmark" to "denmark", "dinamarca" to "denmark", "danemark" to "denmark", "dk" to "denmark", "dnk" to "denmark", "danmark" to "denmark", "デンマーク" to "denmark", "덴마크" to "denmark",
        // 10. Estonia
        "estonia" to "estonia", "estonie" to "estonia", "estônia" to "estonia", "estónia" to "estonia", "ee" to "estonia", "est" to "estonia", "eesti" to "estonia", "エストニア" to "estonia", "에스토니아" to "estonia",
        // 11. Finland
        "finland" to "finland", "finlandia" to "finland", "finlande" to "finland", "finlândia" to "finland", "fi" to "finland", "fin" to "finland", "suomi" to "finland", "フィンランド" to "finland", "핀란드" to "finland",
        // 12. France (+ Monaco)
        "france" to "france", "francia" to "france", "frança" to "france", "franca" to "france", "fr" to "france", "fra" to "france", "フランス" to "france", "프랑스" to "france",
        "monaco" to "france", "mónaco" to "france", "mônaco" to "france", "mc" to "france", "mco" to "france", "モナコ" to "france", "모나코" to "france",
        // 13. Germany
        "germany" to "germany", "alemania" to "germany", "allemagne" to "germany", "alemanha" to "germany", "de" to "germany", "deu" to "germany", "deutschland" to "germany", "ドイツ" to "germany", "독일" to "germany",
        // 14. Greece
        "greece" to "greece", "grecia" to "greece", "grèce" to "greece", "grece" to "greece", "grécia" to "greece", "gr" to "greece", "grc" to "greece", "ギリシャ" to "greece", "ギリシア" to "greece", "그리스" to "greece",
        // 15. Hungary
        "hungary" to "hungary", "hungría" to "hungary", "hungria" to "hungary", "hongrie" to "hungary", "hu" to "hungary", "hun" to "hungary", "magyarország" to "hungary", "magyarorszag" to "hungary", "ハンガリー" to "hungary", "헝가리" to "hungary",
        // 16. Iceland
        "iceland" to "iceland", "islandia" to "iceland", "islande" to "iceland", "islândia" to "iceland", "is" to "iceland", "isl" to "iceland", "ísland" to "iceland", "アイスランド" to "iceland", "아이슬란드" to "iceland",
        // 17. Ireland
        "ireland" to "ireland", "irlanda" to "ireland", "irlande" to "ireland", "ie" to "ireland", "irl" to "ireland", "éire" to "ireland", "eire" to "ireland", "アイルランド" to "ireland", "아일랜드" to "ireland",
        // 18. Israel
        "israel" to "israel", "israël" to "israel", "il" to "israel", "isr" to "israel", "イスラエル" to "israel", "이스라엘" to "israel",
        // 19. Italy (+ San Marino, Vatican)
        "italy" to "italy", "italie" to "italy", "itália" to "italy", "it" to "italy", "ita" to "italy", "italia" to "italy", "イタリア" to "italy", "이탈리아" to "italy",
        "san marino" to "italy", "saint-marin" to "italy", "são marinho" to "italy", "sao marinho" to "italy", "sm" to "italy", "smr" to "italy", "サンマリノ" to "italy", "산마리노" to "italy",
        "vatican" to "italy", "vaticano" to "italy", "ciudad del vaticano" to "italy", "cidade do vaticano" to "italy", "va" to "italy", "vat" to "italy", "バチカン" to "italy", "ヴァチカン" to "italy", "바티칸" to "italy",
        // 20. Latvia
        "latvia" to "latvia", "letonia" to "latvia", "lettonie" to "latvia", "letônia" to "latvia", "letónia" to "latvia", "lv" to "latvia", "lva" to "latvia", "latvija" to "latvia", "ラトビア" to "latvia", "라트비아" to "latvia",
        // 21. Lithuania
        "lithuania" to "lithuania", "lituania" to "lithuania", "lituanie" to "lithuania", "lituânia" to "lithuania", "lt" to "lithuania", "ltu" to "lithuania", "lietuva" to "lithuania", "リトアニア" to "lithuania", "리투아니아" to "lithuania",
        // 22. Luxembourg
        "luxembourg" to "luxembourg", "luxemburgo" to "luxembourg", "lu" to "luxembourg", "lux" to "luxembourg", "ルクセンブルク" to "luxembourg", "룩셈부르크" to "luxembourg",
        // 23. Malta
        "malta" to "malta", "malte" to "malta", "mt" to "malta", "mlt" to "malta", "マルタ" to "malta", "몰타" to "malta",
        // 24. Moldova
        "moldova" to "moldova", "moldavia" to "moldova", "moldavie" to "moldova", "moldávia" to "moldova", "md" to "moldova", "mda" to "moldova", "モルドバ" to "moldova", "몰도바" to "moldova",
        // 25. Montenegro
        "montenegro" to "montenegro", "monténégro" to "montenegro", "me" to "montenegro", "mne" to "montenegro", "crna gora" to "montenegro", "モンテネグロ" to "montenegro", "몬테네그로" to "montenegro",
        // 26. Netherlands
        "netherlands" to "netherlands", "nl" to "netherlands", "nld" to "netherlands", "nederland" to "netherlands", "holland" to "netherlands", "the netherlands" to "netherlands",
        "países bajos" to "netherlands", "paises bajos" to "netherlands", "holanda" to "netherlands",
        "pays-bas" to "netherlands", "pays bas" to "netherlands", "hollande" to "netherlands",
        "países baixos" to "netherlands", "paises baixos" to "netherlands",
        "オランダ" to "netherlands", "네덜란드" to "netherlands",
        // 27. Norway
        "norway" to "norway", "noruega" to "norway", "norvège" to "norway", "norvege" to "norway", "no" to "norway", "nor" to "norway", "norge" to "norway", "ノルウェー" to "norway", "노르웨이" to "norway",
        // 28. Poland
        "poland" to "poland", "polonia" to "poland", "pologne" to "poland", "polônia" to "poland", "polónia" to "poland", "pl" to "poland", "pol" to "poland", "polska" to "poland", "ポーランド" to "poland", "폴란드" to "poland",
        // 29. Portugal
        "portugal" to "portugal", "pt" to "portugal", "prt" to "portugal", "ポルトガル" to "portugal", "포르투갈" to "portugal",
        // 30. North Macedonia (公式スラッグ: republic-of-north-macedonia)
        "republic-of-north-macedonia" to "republic-of-north-macedonia",
        "republic of north macedonia" to "republic-of-north-macedonia",
        "north macedonia" to "republic-of-north-macedonia",
        "north-macedonia" to "republic-of-north-macedonia",
        "macedonia" to "republic-of-north-macedonia",
        "macedonia del norte" to "republic-of-north-macedonia",
        "macédoine du nord" to "republic-of-north-macedonia", "macedoine du nord" to "republic-of-north-macedonia", "macédoine" to "republic-of-north-macedonia",
        "macedônia do norte" to "republic-of-north-macedonia", "macedónia do norte" to "republic-of-north-macedonia", "macedônia" to "republic-of-north-macedonia", "macedónia" to "republic-of-north-macedonia",
        "mk" to "republic-of-north-macedonia", "mkd" to "republic-of-north-macedonia",
        "北マケドニア" to "republic-of-north-macedonia", "マケドニア" to "republic-of-north-macedonia", "북마케도니아" to "republic-of-north-macedonia",
        // 31. Romania
        "romania" to "romania", "rumanía" to "romania", "rumania" to "romania", "roumanie" to "romania", "romênia" to "romania", "roménia" to "romania", "romenia" to "romania", "ro" to "romania", "rou" to "romania", "românia" to "romania", "ルーマニア" to "romania", "루마니아" to "romania",
        // 32. Serbia (+ Kosovo)
        "serbia" to "serbia", "serbie" to "serbia", "sérvia" to "serbia", "servia" to "serbia", "rs" to "serbia", "srb" to "serbia", "srbija" to "serbia", "セルビア" to "serbia", "세르비아" to "serbia",
        "kosovo" to "serbia", "xk" to "serbia", "xks" to "serbia", "コソボ" to "serbia", "코소보" to "serbia",
        // 33. Slovakia
        "slovakia" to "slovakia", "eslovaquia" to "slovakia", "slovaquie" to "slovakia", "eslováquia" to "slovakia", "sk" to "slovakia", "svk" to "slovakia", "slovensko" to "slovakia", "スロバキア" to "slovakia", "スロヴァキア" to "slovakia", "슬로바키아" to "slovakia",
        // 34. Slovenia
        "slovenia" to "slovenia", "eslovenia" to "slovenia", "slovénie" to "slovenia", "slovenie" to "slovenia", "eslovênia" to "slovenia", "eslovénia" to "slovenia", "si" to "slovenia", "svn" to "slovenia", "slovenija" to "slovenia", "スロベニア" to "slovenia", "슬로베니아" to "slovenia",
        // 35. Spain
        "spain" to "spain", "espagne" to "spain", "espanha" to "spain", "es" to "spain", "esp" to "spain", "españa" to "spain", "espana" to "spain", "スペイン" to "spain", "스페인" to "spain",
        // 36. Sweden
        "sweden" to "sweden", "suecia" to "sweden", "suède" to "sweden", "suede" to "sweden", "suécia" to "sweden", "se" to "sweden", "swe" to "sweden", "sverige" to "sweden", "スウェーデン" to "sweden", "스웨덴" to "sweden",
        // 37. Switzerland (+ Liechtenstein)
        "switzerland" to "switzerland", "suiza" to "switzerland", "suíça" to "switzerland", "suica" to "switzerland", "ch" to "switzerland", "che" to "switzerland", "schweiz" to "switzerland", "suisse" to "switzerland", "svizzera" to "switzerland", "スイス" to "switzerland", "스위스" to "switzerland",
        "liechtenstein" to "switzerland", "li" to "switzerland", "lie" to "switzerland", "リヒテンシュタイン" to "switzerland", "리히텐슈타인" to "switzerland",
        // 38. Ukraine
        "ukraine" to "ukraine", "ucrania" to "ukraine", "ucrânia" to "ukraine", "ua" to "ukraine", "ukr" to "ukraine", "україна" to "ukraine", "ウクライナ" to "ukraine", "우크라이나" to "ukraine",
        // 39. United Kingdom (+ Gibraltar, Crown dependencies)
        "united kingdom" to "united-kingdom", "united-kingdom" to "united-kingdom", "uk" to "united-kingdom",
        "reino unido" to "united-kingdom", "royaume-uni" to "united-kingdom", "royaume uni" to "united-kingdom",
        "gran bretaña" to "united-kingdom", "gran bretana" to "united-kingdom", "grande-bretagne" to "united-kingdom", "grã-bretanha" to "united-kingdom",
        "inglaterra" to "united-kingdom", "angleterre" to "united-kingdom", "escocia" to "united-kingdom", "écosse" to "united-kingdom", "escócia" to "united-kingdom",
        "gales" to "united-kingdom", "pays de galles" to "united-kingdom", "país de gales" to "united-kingdom",
        "irlanda del norte" to "united-kingdom", "irlande du nord" to "united-kingdom", "irlanda do norte" to "united-kingdom",
        "gb" to "united-kingdom", "gbr" to "united-kingdom", "great britain" to "united-kingdom",
        "england" to "united-kingdom", "scotland" to "united-kingdom", "wales" to "united-kingdom", "northern ireland" to "united-kingdom",
        "イギリス" to "united-kingdom", "英国" to "united-kingdom", "영국" to "united-kingdom",
        "gibraltar" to "united-kingdom", "gi" to "united-kingdom", "gib" to "united-kingdom", "ジブラルタル" to "united-kingdom",
        "isle of man" to "united-kingdom", "im" to "united-kingdom", "imn" to "united-kingdom", "マン島" to "united-kingdom",
        "jersey" to "united-kingdom", "je" to "united-kingdom", "jey" to "united-kingdom",
        "guernsey" to "united-kingdom", "gg" to "united-kingdom", "ggy" to "united-kingdom"
    )

    private data class CountryCentroid(val slug: String, val lat: Double, val lon: Double)

    // MeteoAlarm加盟39カ国の地理的代表座標（近傍フォールバック用）
    private val COUNTRY_CENTROIDS = listOf(
        CountryCentroid("andorra", 42.50, 1.52),
        CountryCentroid("austria", 47.51, 14.55),
        CountryCentroid("belgium", 50.85, 4.35),
        CountryCentroid("bosnia-herzegovina", 43.85, 18.36),
        CountryCentroid("bulgaria", 42.73, 25.48),
        CountryCentroid("croatia", 45.10, 15.20),
        CountryCentroid("cyprus", 35.12, 33.42),
        CountryCentroid("czechia", 49.81, 15.47),
        CountryCentroid("denmark", 56.26, 9.50),
        CountryCentroid("estonia", 58.59, 25.01),
        CountryCentroid("finland", 61.92, 25.74),
        CountryCentroid("france", 46.22, 2.21),
        CountryCentroid("germany", 51.16, 10.45),
        CountryCentroid("greece", 39.07, 21.82),
        CountryCentroid("hungary", 47.16, 19.50),
        CountryCentroid("iceland", 64.96, -19.02),
        CountryCentroid("ireland", 53.14, -7.69),
        CountryCentroid("israel", 31.04, 34.85),
        CountryCentroid("italy", 41.87, 12.56),
        CountryCentroid("latvia", 56.87, 24.60),
        CountryCentroid("lithuania", 55.16, 23.88),
        CountryCentroid("luxembourg", 49.81, 6.12),
        CountryCentroid("malta", 35.93, 14.37),
        CountryCentroid("moldova", 47.41, 28.36),
        CountryCentroid("montenegro", 42.70, 19.37),
        CountryCentroid("netherlands", 52.13, 5.29),
        CountryCentroid("norway", 60.47, 8.46),
        CountryCentroid("poland", 51.91, 19.14),
        CountryCentroid("portugal", 39.39, -8.22),
        CountryCentroid("republic-of-north-macedonia", 41.60, 21.74),
        CountryCentroid("romania", 45.94, 24.96),
        CountryCentroid("serbia", 44.01, 21.00),
        CountryCentroid("slovakia", 48.66, 19.69),
        CountryCentroid("slovenia", 46.15, 14.99),
        CountryCentroid("spain", 40.46, -3.74),
        CountryCentroid("sweden", 60.12, 18.64),
        CountryCentroid("switzerland", 46.81, 8.22),
        CountryCentroid("ukraine", 48.37, 31.16),
        CountryCentroid("united-kingdom", 55.37, -3.43)
    )

    /**
     * 都市の国名・都市名・座標から MeteoAlarm のフィードスラッグを取得
     */
    fun getCountrySlug(city: City): String? {
        // 日本や米国は対象外
        if (isLocationOrCountryInJapan(city.country, city.latitude, city.longitude)) return null
        if (NwsAlertRepository.isUnitedStates(city)) return null

        val country = city.country.lowercase(Locale.ROOT).trim()
        val nameEn = city.nameEn.lowercase(Locale.ROOT).trim()
        val name = city.name.lowercase(Locale.ROOT).trim()

        // 1. 国名辞書の完全一致
        COUNTRY_SLUG_MAP[country]?.let { return it }

        // 2. 部分一致判定 (4文字以上の正式国名キーワードのみ対象。2〜3文字の短縮コード・略称は完全一致のみ)
        if (country.length >= 4) {
            for ((key, slug) in COUNTRY_SLUG_MAP) {
                if (key.length >= 4 && (country.contains(key) || (key.length >= 6 && key.contains(country)))) {
                    return slug
                }
            }
        }

        // 3. 都市名（英語名／日本語名）による著名都市フォールバック
        when {
            nameEn.contains("berlin") || nameEn.contains("munich") || nameEn.contains("frankfurt") || nameEn.contains("hamburg") || name.contains("ベルリン") -> return "germany"
            nameEn.contains("paris") || nameEn.contains("lyon") || nameEn.contains("marseille") || nameEn.contains("nice") || name.contains("パリ") -> return "france"
            nameEn.contains("madrid") || nameEn.contains("barcelona") || nameEn.contains("seville") || nameEn.contains("valencia") || name.contains("マドリード") -> return "spain"
            nameEn.contains("rome") || nameEn.contains("milan") || nameEn.contains("naples") || nameEn.contains("florence") || name.contains("ローマ") -> return "italy"
            nameEn.contains("london") || nameEn.contains("manchester") || nameEn.contains("birmingham") || nameEn.contains("edinburgh") || name.contains("ロンドン") -> return "united-kingdom"
            nameEn.contains("vienna") || nameEn.contains("salzburg") || nameEn.contains("innsbruck") || name.contains("ウィーン") -> return "austria"
            nameEn.contains("zurich") || nameEn.contains("geneva") || nameEn.contains("bern") || name.contains("チューリッヒ") -> return "switzerland"
            nameEn.contains("amsterdam") || nameEn.contains("rotterdam") || nameEn.contains("the hague") || name.contains("アムステルダム") -> return "netherlands"
            nameEn.contains("brussels") || nameEn.contains("antwerp") || name.contains("ブリュッセル") -> return "belgium"
            nameEn.contains("stockholm") || nameEn.contains("gothenburg") || name.contains("ストックホルム") -> return "sweden"
            nameEn.contains("oslo") || nameEn.contains("bergen") || name.contains("オスロ") -> return "norway"
            nameEn.contains("copenhagen") || name.contains("コペンハーゲン") -> return "denmark"
            nameEn.contains("helsinki") || name.contains("ヘルシンキ") -> return "finland"
            nameEn.contains("warsaw") || nameEn.contains("krakow") || name.contains("ワルシャワ") -> return "poland"
            nameEn.contains("lisbon") || nameEn.contains("porto") || name.contains("リスボン") -> return "portugal"
            nameEn.contains("dublin") || name.contains("ダブリン") -> return "ireland"
            nameEn.contains("athens") || name.contains("アテネ") -> return "greece"
            nameEn.contains("prague") || name.contains("プラハ") -> return "czechia"
            nameEn.contains("budapest") || name.contains("ブダペスト") -> return "hungary"
            nameEn.contains("bucharest") || name.contains("ブカレスト") -> return "romania"
            nameEn.contains("kyiv") || nameEn.contains("kiev") || nameEn.contains("lviv") || nameEn.contains("odesa") || name.contains("キーウ") || name.contains("キエフ") -> return "ukraine"
            nameEn.contains("tel aviv") || nameEn.contains("jerusalem") || nameEn.contains("haifa") || name.contains("テルアビブ") || name.contains("エルサレム") -> return "israel"
            nameEn.contains("skopje") || name.contains("スコピエ") -> return "republic-of-north-macedonia"
            nameEn.contains("andorra") -> return "andorra"
            nameEn.contains("chisinau") -> return "moldova"
            nameEn.contains("podgorica") -> return "montenegro"
            nameEn.contains("sarajevo") -> return "bosnia-herzegovina"
            nameEn.contains("belgrade") -> return "serbia"
            nameEn.contains("sofia") -> return "bulgaria"
            nameEn.contains("zagreb") -> return "croatia"
            nameEn.contains("ljubljana") -> return "slovenia"
            nameEn.contains("bratislava") -> return "slovakia"
            nameEn.contains("tallinn") -> return "estonia"
            nameEn.contains("riga") -> return "latvia"
            nameEn.contains("vilnius") -> return "lithuania"
            nameEn.contains("valletta") -> return "malta"
            nameEn.contains("nicosia") -> return "cyprus"
            nameEn.contains("reykjavik") -> return "iceland"
        }

        // 4. 座標近傍判定フォールバック (ヨーロッパ・イスラエルの対象領域内)
        // ただし明示的に非加盟国名が指定されている場合は乗っ取らず WMO へ委譲する
        val nonMemberCountries = listOf(
            "russia", "rusia", "russie", "rússia", "ロシア", "俄罗斯", "俄羅斯",
            "turkey", "türkiye", "turquía", "turquia", "turquie", "トルコ", "土耳其",
            "belarus", "bielorrusia", "biélorussie", "bielorrússia", "bielorrussia", "ベラルーシ", "白俄罗斯", "白俄羅斯",
            "georgia", "géorgie", "geórgia", "ジョージア", "格鲁吉亚", "喬治亞",
            "armenia", "arménie", "armênia", "arménia", "アルメニア",
            "azerbaijan", "azerbaiyán", "azerbaïdjan", "azerbaijão", "アゼルバイジャン",
            "egypt", "egipto", "égypte", "egito", "エジプト",
            "morocco", "marruecos", "maroc", "marrocos", "モロッコ",
            "algeria", "argelia", "algérie", "argélia", "アルジェリア",
            "tunisia", "túnez", "tunisie", "tunísia", "チュニジア",
            "libya", "libia", "libye", "líbia", "リビア",
            "syria", "siria", "syrie", "síria", "シリア",
            "lebanon", "líbano", "liban", "libano", "レバノン",
            "jordan", "jordania", "jordanie", "jordânia", "ヨルダン",
            "iraq", "irak", "iraque", "イラク",
            "saudi", "arabia saudita", "arabia saudí", "arabie saoudite", "arábia saudita", "サウジアラビア",
            "iran", "irán", "irã", "イラン",
            "kazakhstan", "kazajistán", "kazajstán", "cazaquistão", "カザフスタン"
        )
        if (country.isNotBlank() && nonMemberCountries.any { country.contains(it) }) {
            return null
        }

        val lat = city.latitude
        val lon = city.longitude
        val inEurope = lat in 34.0..72.0 && lon in -25.0..45.0
        val inIsrael = lat in 29.0..33.5 && lon in 34.0..36.5

        if (inEurope || inIsrael) {
            var closestSlug: String? = null
            var minDistanceSq = Double.MAX_VALUE
            for (c in COUNTRY_CENTROIDS) {
                val dLat = lat - c.lat
                val dLon = (lon - c.lon) * kotlin.math.cos(Math.toRadians(lat))
                val distSq = dLat * dLat + dLon * dLon
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq
                    closestSlug = c.slug
                }
            }
            // 半径約500km以内 (distSq <= 25.0) の最近傍加盟国にマッピング
            if (minDistanceSq <= 25.0) {
                return closestSlug
            }
        }

        return null
    }

    /**
     * 指定された都市が MeteoAlarm のサポート範囲（欧州全域39カ国＋関連地域）内にあるかを判定
     */
    fun isSupported(city: City): Boolean {
        // 日本や米国は直接取得方式（気象庁/NWS）が最優先
        if (isLocationOrCountryInJapan(city.country, city.latitude, city.longitude)) return false
        if (NwsAlertRepository.isUnitedStates(city)) return false

        return getCountrySlug(city) != null
    }

    /**
     * 後方互換用エイリアス
     */
    fun isEuropeanCity(city: City): Boolean = isSupported(city)

    /**
     * 対象都市の MeteoAlarm 公式気象警報を取得
     */
    suspend fun fetchAlertsForCity(
        city: City,
        forceFresh: Boolean = false
    ): List<GlobalAlertItem> = withContext(Dispatchers.IO) {
        val slug = getCountrySlug(city) ?: return@withContext emptyList()
        val allEntries = getCountryFeedEntries(slug, forceFresh)
        if (allEntries.isEmpty()) return@withContext emptyList()

        val matchingItems = mutableListOf<GlobalAlertItem>()

        for (entry in allEntries) {
            // 解除・キャンセル報はスキップ
            if (isCancellation(entry)) continue

            if (isEventAffectingCity(entry, city, slug)) {
                val (type, severity) = determineTypeAndSeverity(entry)
                val eventName = resolveEventName(entry)
                val headline = entry.title.ifBlank { "$eventName Warning" }
                val desc = formatDescription(entry, eventName)

                val stableId = entry.id.ifBlank { entry.identifier.ifBlank { "${slug}-${matchingItems.size}" } }
                matchingItems.add(
                    GlobalAlertItem(
                        id = "meteoalarm-$stableId",
                        source = AlertSource.METEOALARM,
                        type = type,
                        severity = severity,
                        eventName = eventName,
                        headline = headline,
                        description = desc,
                        instruction = "MeteoAlarm (EUMETNET) 公式気象警報。最新の警報情報および現地自治体の指示に従ってください。",
                        effectiveTime = entry.onset.ifBlank { entry.sent },
                        expiresTime = entry.expires.ifBlank { null },
                        areaDesc = entry.areaDesc.take(120),
                        detailsUrl = "https://meteoalarm.org",
                        eventTypeCode = null
                    )
                )
            }
        }

        // 特別警戒 (赤色) > 警戒 (橙色) > 注意 (黄色) の順でソート
        matchingItems.sortedWith(
            compareBy(
                {
                    when (it.type) {
                        WeatherWarningType.SPECIAL_WARNING -> 0
                        WeatherWarningType.WARNING -> 1
                        WeatherWarningType.ADVISORY -> 2
                        else -> 3
                    }
                },
                { it.severity.ordinal }
            )
        )
    }

    /**
     * キャンセル・解除報判定
     */
    private fun isCancellation(entry: MeteoAlarmRawEntry): Boolean {
        val text = "${entry.title} ${entry.event}".lowercase(Locale.ROOT)
        return text.contains("cancel") ||
                text.contains("aufhebung") ||
                text.contains("annulation") ||
                text.contains("cancelación") ||
                text.contains("cancelamento") ||
                text.contains("iptali") ||
                text.contains("anulowanie")
    }

    /**
     * 重大度とカード色（赤・橙・黄）の判定
     * MeteoAlarm公式仕様:
     * - Red: Extreme (特別警戒)
     * - Orange: Severe (警戒)
     * - Yellow: Moderate / Minor (注意)
     */
    internal fun determineTypeAndSeverity(entry: MeteoAlarmRawEntry): Pair<WeatherWarningType, AlertSeverity> {
        val s = entry.severity.trim().lowercase(Locale.ROOT)
        // CAP 標準 severity メタデータを最優先判定
        when (s) {
            "extreme" -> return Pair(WeatherWarningType.SPECIAL_WARNING, AlertSeverity.EXTREME)
            "severe" -> return Pair(WeatherWarningType.WARNING, AlertSeverity.SEVERE)
            "moderate" -> return Pair(WeatherWarningType.ADVISORY, AlertSeverity.MODERATE)
            "minor" -> return Pair(WeatherWarningType.ADVISORY, AlertSeverity.MINOR)
        }

        // CAP severity が欠落している場合のみタイトルの独立単語（\b）でフォールバック判定
        val t = entry.title.lowercase(Locale.ROOT)
        if (Regex("\\b(red|extreme)\\b").containsMatchIn(t)) {
            return Pair(WeatherWarningType.SPECIAL_WARNING, AlertSeverity.EXTREME)
        }
        if (Regex("\\b(orange|severe)\\b").containsMatchIn(t)) {
            return Pair(WeatherWarningType.WARNING, AlertSeverity.SEVERE)
        }
        if (Regex("\\b(yellow|moderate)\\b").containsMatchIn(t)) {
            return Pair(WeatherWarningType.ADVISORY, AlertSeverity.MODERATE)
        }

        return Pair(WeatherWarningType.ADVISORY, AlertSeverity.MINOR)
    }

    /**
     * イベント名の解決
     */
    private fun resolveEventName(entry: MeteoAlarmRawEntry): String {
        if (entry.event.isNotBlank()) {
            return entry.event.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        // titleから抽出 (例: "Yellow Coastalevent Warning issued for Germany - Flensburg bis Fehmarn")
        val title = entry.title
        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.contains("coastalevent") || lower.contains("coastal") -> "Coastal Event"
            lower.contains("wind") || lower.contains("gale") -> "Wind"
            lower.contains("snow") || lower.contains("ice") -> "Snow-Ice"
            lower.contains("thunderstorm") -> "Thunderstorms"
            lower.contains("rain") -> "Rain"
            lower.contains("flood") -> "Flooding"
            lower.contains("high-temperature") || lower.contains("high temperature") || lower.contains("heat") -> "High Temperature"
            lower.contains("low-temperature") || lower.contains("low temperature") || lower.contains("cold") -> "Low Temperature"
            lower.contains("fog") -> "Fog"
            lower.contains("forest fire") || lower.contains("forest-fire") -> "Forest Fire"
            lower.contains("avalanche") -> "Avalanches"
            else -> "MeteoAlarm Alert"
        }
    }

    /**
     * 詳細解説文の生成
     */
    private fun formatDescription(entry: MeteoAlarmRawEntry, eventName: String): String {
        val sb = StringBuilder()
        sb.append(entry.title.trim())
        if (entry.areaDesc.isNotBlank() && !entry.title.contains(entry.areaDesc)) {
            sb.append("\n対象地域: ").append(entry.areaDesc.trim())
        }
        if (entry.onset.isNotBlank()) {
            sb.append("\n発効: ").append(entry.onset.take(16).replace("T", " "))
        }
        if (entry.expires.isNotBlank()) {
            sb.append(" 〜 期限: ").append(entry.expires.take(16).replace("T", " "))
        }
        return sb.toString()
    }

    /**
     * 都市と警報の照合
     */
    private fun isEventAffectingCity(entry: MeteoAlarmRawEntry, city: City, countrySlug: String): Boolean {
        val cityName = city.name.lowercase(Locale.ROOT).trim()
        val cityNameEn = city.nameEn.lowercase(Locale.ROOT).trim()
        val areaLower = entry.areaDesc.lowercase(Locale.ROOT)
        val titleLower = entry.title.lowercase(Locale.ROOT)

        // 1. 都市名（日本語名または英語名）が areaDesc または title に含まれる場合 -> 即一致
        val minJaOrCjkLen = if (cityName.any { it.code > 127 }) 2 else 3
        val minEnLen = if (cityNameEn.any { it.code > 127 }) 2 else 3
        if (cityName.length >= minJaOrCjkLen && (areaLower.contains(cityName) || titleLower.contains(cityName))) {
            return true
        }
        if (cityNameEn.length >= minEnLen && (areaLower.contains(cityNameEn) || titleLower.contains(cityNameEn))) {
            return true
        }

        // 2. 広域警報（全国または全州・全管区）
        if (areaLower.isBlank() || areaLower == countrySlug || areaLower.contains("entire") || areaLower.contains("nationwide")) {
            return true
        }

        // 3. フィード内のエントリ数が少ない場合（小国や全国共通フィード）
        // titleに国名が含まれていて特定地域指定が全体に及ぶ場合
        if (titleLower.contains(countrySlug) && areaLower.isBlank()) {
            return true
        }

        return false
    }

    /**
     * 国別フィードの取得＆Atom XMLパース（15分キャッシュ）
     */
    private suspend fun getCountryFeedEntries(
        countrySlug: String,
        forceFresh: Boolean = false
    ): List<MeteoAlarmRawEntry> = fetchMutex.withLock {
        val now = System.currentTimeMillis()
        if (!forceFresh) {
            val cached = feedCache[countrySlug]
            if (cached != null && (now - cached.first) < CACHE_TTL_MS) {
                return cached.second
            }
        }

        try {
            val urlString = "$BASE_FEED_URL$countrySlug"
            val xmlText = AppHttpClient.getTextOrNull(
                url = urlString,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Accept" to "application/atom+xml, application/xml"
                ),
                timeoutMs = 8_000L
            ) ?: return feedCache[countrySlug]?.second ?: emptyList()

            val entries = parseAtomXml(xmlText)
            feedCache[countrySlug] = Pair(now, entries)
            Log.d(TAG, "Fetched ${entries.size} MeteoAlarm entries for $countrySlug")
            entries
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch MeteoAlarm feed for $countrySlug: ${e.message}")
            feedCache[countrySlug]?.second ?: emptyList()
        }
    }

    private fun readTextSafe(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text ?: "")
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return sb.toString().trim()
    }

    /**
     * Atom XMLのパース (XmlPullParser)
     */
    private fun parseAtomXml(xml: String): List<MeteoAlarmRawEntry> {
        val entries = mutableListOf<MeteoAlarmRawEntry>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        try {
            factory.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        } catch (_: Exception) {}
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var inEntry = false

        var id = ""
        var title = ""
        var event = ""
        val areaDescList = mutableListOf<String>()
        var severity = ""
        var onset = ""
        var expires = ""
        var sent = ""
        var identifier = ""
        var capXmlUrl: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name.equals("entry", ignoreCase = true)) {
                        inEntry = true
                        id = ""
                        title = ""
                        event = ""
                        areaDescList.clear()
                        severity = ""
                        onset = ""
                        expires = ""
                        sent = ""
                        identifier = ""
                        capXmlUrl = null
                    } else if (inEntry) {
                        when (name.lowercase(Locale.ROOT)) {
                            "id" -> id = readTextSafe(parser)
                            "title" -> title = readTextSafe(parser)
                            "event" -> event = readTextSafe(parser)
                            "areadesc" -> {
                                val desc = readTextSafe(parser)
                                if (desc.isNotBlank() && !areaDescList.contains(desc)) {
                                    areaDescList.add(desc)
                                }
                            }
                            "severity" -> severity = readTextSafe(parser)
                            "onset" -> onset = readTextSafe(parser)
                            "expires" -> expires = readTextSafe(parser)
                            "sent" -> sent = readTextSafe(parser)
                            "identifier" -> identifier = readTextSafe(parser)
                            "link" -> {
                                val type = parser.getAttributeValue(null, "type")
                                val href = parser.getAttributeValue(null, "href")
                                if (type == "application/cap+xml" && !href.isNullOrBlank()) {
                                    capXmlUrl = href
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("entry", ignoreCase = true) && inEntry) {
                        entries.add(
                            MeteoAlarmRawEntry(
                                id = id.ifBlank { "entry-${entries.size}" },
                                title = title,
                                event = event,
                                areaDesc = areaDescList.joinToString(", "),
                                severity = severity,
                                onset = onset,
                                expires = expires,
                                sent = sent,
                                identifier = identifier,
                                capXmlUrl = capXmlUrl
                            )
                        )
                        inEntry = false
                    }
                }
            }
            eventType = parser.next()
        }

        return entries
    }

    /**
     * テスト用のヨーロッパ模擬警報リストを作成
     * (赤色: 特別警戒 / 橙色: 警戒 / 黄色: 注意)
     */
    fun createSampleAlerts(): List<GlobalAlertItem> {
        return listOf(
            GlobalAlertItem(
                id = "meteoalarm-demo-avalanche",
                source = AlertSource.METEOALARM,
                type = WeatherWarningType.SPECIAL_WARNING,
                severity = AlertSeverity.EXTREME,
                eventName = "Avalanches",
                headline = "Red Avalanche Warning issued by GeoSphere Austria / MeteoAlarm",
                description = "極めて高い雪崩の危険性 (Level 5)。急激な積雪と強風により自然発生的な大規模雪崩が多発する危険性があります。山岳部への立ち入りは厳禁です。",
                instruction = "山岳地域および標高の高い斜面への立ち入りは厳禁です。自治体の指示に従い、危険地域からの退避を行ってください。",
                effectiveTime = "2026-10-01T06:00:00+02:00",
                expiresTime = "2026-10-02T18:00:00+02:00",
                areaDesc = "Tyrol / Alpine Region",
                detailsUrl = "https://meteoalarm.org"
            ),
            GlobalAlertItem(
                id = "meteoalarm-demo-wind",
                source = AlertSource.METEOALARM,
                type = WeatherWarningType.WARNING,
                severity = AlertSeverity.SEVERE,
                eventName = "Wind",
                headline = "Orange Wind Warning issued by Deutscher Wetterdienst (DWD) / MeteoAlarm",
                description = "最大瞬間風速 100〜110 km/h (風力11) の暴風が予想されます。倒木、看板の落下、交通機関の乱れに厳重に警戒してください。",
                instruction = "不要不急の外出を控え、ベランダ等の飛散しやすい屋外の物品を屋内に固定・移動してください。",
                effectiveTime = "2026-10-01T12:00:00+02:00",
                expiresTime = "2026-10-01T23:00:00+02:00",
                areaDesc = "Berlin & Brandenburg",
                detailsUrl = "https://meteoalarm.org"
            ),
            GlobalAlertItem(
                id = "meteoalarm-demo-rain",
                source = AlertSource.METEOALARM,
                type = WeatherWarningType.ADVISORY,
                severity = AlertSeverity.MODERATE,
                eventName = "Rain",
                headline = "Yellow Heavy Rain Warning issued by Météo-France / MeteoAlarm",
                description = "大雨による局地的な冠水および河川増水の注意報。24時間で40〜60mmの降水量が予想されます。",
                instruction = "低地や地下街の冠水、アンダーパスの通行に注意してください。",
                effectiveTime = "2026-10-01T08:00:00+02:00",
                expiresTime = "2026-10-01T20:00:00+02:00",
                areaDesc = "Île-de-France / Paris",
                detailsUrl = "https://meteoalarm.org"
            )
        )
    }
}
