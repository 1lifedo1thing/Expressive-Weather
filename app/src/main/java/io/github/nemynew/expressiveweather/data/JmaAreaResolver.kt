package io.github.nemynew.expressiveweather.data

/**
 * 気象庁予報区オフィスおよびアメダス観測所の解決結果
 */
data class JmaAreaInfo(
    val officeCode: String,
    val officeName: String,
    val stationCode: String,
    val stationName: String
)

data class JmaOffice(
    val code: String,
    val name: String,
    val officeName: String,
    val lat: Double,
    val lon: Double,
    val keywords: List<String>
)

data class AmedasStation(
    val code: String,
    val name: String,
    val lat: Double,
    val lon: Double
)

/**
 * 緯度・経度または地名から最適な気象庁オフィス（予報区）および直近アメダス観測所を特定するリゾルバー
 */
object JmaAreaResolver {

    val OFFICES = listOf(
        // 北海道 (8管区・地方気象台・測候所)
        JmaOffice("011000", "宗谷地方", "稚内地方気象台", 45.415, 141.678, listOf("宗谷", "稚内", "利尻", "礼文")),
        JmaOffice("012000", "上川・留萌地方", "旭川地方気象台", 43.771, 142.365, listOf("上川", "留萌", "旭川", "富良野", "名寄")),
        JmaOffice("013000", "網走・北見・紋別地方", "網走地方気象台", 44.018, 144.272, listOf("網走", "北見", "紋別", "オホーツク")),
        JmaOffice("014030", "十勝地方", "帯広測候所", 42.924, 143.205, listOf("十勝", "帯広")),
        JmaOffice("014100", "釧路・根室地方", "釧路地方気象台", 42.985, 144.385, listOf("釧路", "根室")),
        JmaOffice("015000", "胆振・日高地方", "室蘭地方気象台", 42.315, 140.974, listOf("胆振", "日高", "室蘭", "苫小牧", "浦河")),
        JmaOffice("016000", "石狩・空知・後志地方", "札幌管区気象台", 43.060, 141.329, listOf("北海道", "札幌", "石狩", "空知", "後志", "小樽", "岩見沢")),
        JmaOffice("017000", "渡島・檜山地方", "函館地方気象台", 41.817, 140.753, listOf("渡島", "檜山", "函館", "江差")),

        // 東北
        JmaOffice("020000", "青森県", "青森地方気象台", 40.822, 140.747, listOf("青森", "Aomori", "八戸", "弘前")),
        JmaOffice("030000", "岩手県", "盛岡地方気象台", 39.699, 141.167, listOf("岩手", "盛岡", "Iwate", "Morioka", "花巻")),
        JmaOffice("040000", "宮城県", "仙台管区気象台", 38.262, 140.897, listOf("宮城", "仙台", "Miyagi", "Sendai", "石巻")),
        JmaOffice("050000", "秋田県", "秋田地方気象台", 39.717, 140.102, listOf("秋田", "Akita", "横手", "大館")),
        JmaOffice("060000", "山形県", "山形地方気象台", 38.255, 140.344, listOf("山形", "Yamagata", "鶴岡", "酒田", "米沢")),
        JmaOffice("070000", "福島県", "福島地方気象台", 37.755, 140.468, listOf("福島", "Fukushima", "郡山", "いわき", "会津若松")),

        // 関東・甲信
        JmaOffice("080000", "茨城県", "水戸地方気象台", 36.381, 140.447, listOf("茨城", "水戸", "つくば", "Ibaraki", "Mito", "Tsukuba")),
        JmaOffice("090000", "栃木県", "宇都宮地方気象台", 36.549, 139.869, listOf("栃木", "宇都宮", "日光", "Tochigi", "Utsunomiya", "Nikko")),
        JmaOffice("100000", "群馬県", "前橋地方気象台", 36.405, 139.060, listOf("群馬", "前橋", "高崎", "Gunma", "Maebashi", "Takasaki")),
        JmaOffice("110000", "埼玉県", "熊谷地方気象台", 36.140, 139.382, listOf("埼玉", "さいたま", "熊谷", "川口", "川越", "所沢", "Saitama", "Kumagaya")),
        JmaOffice("120000", "千葉県", "銚子地方気象台", 35.735, 140.857, listOf("千葉", "船橋", "銚子", "成田", "柏", "Chiba", "Narita")),
        JmaOffice("130000", "東京都", "気象庁", 35.692, 139.753, listOf("東京", "Tokyo", "新宿", "渋谷", "千代田", "八王子", "町田", "伊豆", "小笠原")),
        JmaOffice("140000", "神奈川県", "横浜地方気象台", 35.439, 139.653, listOf("神奈川", "横浜", "川崎", "相模原", "横須賀", "小田原", "Kanagawa", "Yokohama")),
        JmaOffice("190000", "山梨県", "甲府地方気象台", 35.667, 138.555, listOf("山梨", "甲府", "富士吉田", "Yamanashi", "Kofu")),
        JmaOffice("200000", "長野県", "長野地方気象台", 36.660, 138.196, listOf("長野", "松本", "上田", "軽井沢", "Nagano", "Matsumoto")),

        // 北陸
        JmaOffice("150000", "新潟県", "新潟地方気象台", 37.894, 139.022, listOf("新潟", "長岡", "上越", "佐渡", "Niigata")),
        JmaOffice("160000", "富山県", "富山地方気象台", 36.708, 137.202, listOf("富山", "高岡", "Toyama")),
        JmaOffice("170000", "石川県", "金沢地方気象台", 36.590, 136.632, listOf("石川", "金沢", "小松", "輪島", "Ishikawa", "Kanazawa")),
        JmaOffice("180000", "福井県", "福井地方気象台", 36.055, 136.222, listOf("福井", "敦賀", "Fukui")),

        // 東海
        JmaOffice("210000", "岐阜県", "岐阜地方気象台", 35.398, 136.760, listOf("岐阜", "大垣", "高山", "多治見", "Gifu")),
        JmaOffice("220000", "静岡県", "静岡地方気象台", 34.975, 138.404, listOf("静岡", "浜松", "沼津", "熱海", "富士", "Shizuoka", "Hamamatsu")),
        JmaOffice("230000", "愛知県", "名古屋地方気象台", 35.167, 136.966, listOf("愛知", "名古屋", "豊橋", "岡崎", "豊田", "Aichi", "Nagoya")),
        JmaOffice("240000", "三重県", "津地方気象台", 34.730, 136.516, listOf("三重", "津", "四日市", "伊勢", "Mie", "Tsu")),

        // 近畿
        JmaOffice("250000", "滋賀県", "彦根地方気象台", 35.275, 136.244, listOf("滋賀", "大津", "彦根", "Shiga", "Otsu")),
        JmaOffice("260000", "京都府", "京都地方気象台", 35.014, 135.733, listOf("京都", "宇治", "舞鶴", "福知山", "Kyoto")),
        JmaOffice("270000", "大阪府", "大阪管区気象台", 34.682, 135.519, listOf("大阪", "堺", "東大阪", "豊中", "枚方", "Osaka")),
        JmaOffice("280000", "兵庫県", "神戸地方気象台", 34.697, 135.213, listOf("兵庫", "神戸", "姫路", "西宮", "尼崎", "明石", "Hyogo", "Kobe")),
        JmaOffice("290000", "奈良県", "奈良地方気象台", 34.693, 135.833, listOf("奈良", "橿原", "Nara")),
        JmaOffice("300000", "和歌山県", "和歌山地方気象台", 34.227, 135.163, listOf("和歌山", "田辺", "白浜", "Wakayama")),

        // 中国
        JmaOffice("310000", "鳥取県", "鳥取地方気象台", 35.513, 134.237, listOf("鳥取", "米子", "Tottori")),
        JmaOffice("320000", "島根県", "松江地方気象台", 35.457, 133.067, listOf("島根", "松江", "出雲", "浜田", "Shimane", "Matsue")),
        JmaOffice("330000", "岡山県", "岡山地方気象台", 34.660, 133.918, listOf("岡山", "倉敷", "Okayama", "Kurashiki")),
        JmaOffice("340000", "広島県", "広島地方気象台", 34.399, 132.463, listOf("広島", "福山", "呉", "尾道", "Hiroshima")),
        JmaOffice("350000", "山口県", "下関地方気象台", 33.957, 130.925, listOf("山口", "下関", "宇部", "Yamaguchi", "Shimonoseki")),

        // 四国
        JmaOffice("360000", "徳島県", "徳島地方気象台", 34.075, 134.573, listOf("徳島", "鳴門", "Tokushima")),
        JmaOffice("370000", "香川県", "高松地方気象台", 34.316, 134.053, listOf("香川", "高松", "丸亀", "Kagawa", "Takamatsu")),
        JmaOffice("380000", "愛媛県", "松山地方気象台", 33.843, 132.782, listOf("愛媛", "松山", "今治", "Ehime", "Matsuyama")),
        JmaOffice("390000", "高知県", "高知地方気象台", 33.567, 133.550, listOf("高知", "南国", "Kochi")),

        // 九州・沖縄
        JmaOffice("400000", "福岡県", "福岡管区気象台", 33.582, 130.376, listOf("福岡", "北九州", "久留米", "Fukuoka", "Kitakyushu")),
        JmaOffice("410000", "佐賀県", "佐賀地方気象台", 33.264, 130.301, listOf("佐賀", "唐津", "鳥栖", "Saga")),
        JmaOffice("420000", "長崎県", "長崎地方気象台", 32.733, 129.869, listOf("長崎", "佐世保", "諫早", "Nagasaki", "Sasebo")),
        JmaOffice("430000", "熊本県", "熊本地方気象台", 32.813, 130.707, listOf("熊本", "八代", "天草", "Kumamoto")),
        JmaOffice("440000", "大分県", "大分地方気象台", 33.235, 131.618, listOf("大分", "別府", "中津", "Oita", "Beppu")),
        JmaOffice("450000", "宮崎県", "宮崎地方気象台", 31.917, 131.417, listOf("宮崎", "都城", "延岡", "Miyazaki")),
        JmaOffice("460100", "鹿児島県（奄美地方除く）", "鹿児島地方気象台", 31.556, 130.548, listOf("鹿児島", "霧島", "鹿屋", "Kagoshima")),
        JmaOffice("460040", "奄美地方", "名瀬測候所", 28.381, 129.497, listOf("奄美", "名瀬", "徳之島", "Amami")),
        JmaOffice("471000", "沖縄本島地方", "沖縄気象台", 26.207, 127.679, listOf("沖縄", "那覇", "名護", "うるま", "浦添", "Okinawa", "Naha")),
        JmaOffice("472000", "大東島地方", "南大東島地方気象台", 25.831, 131.231, listOf("大東島", "南大東", "北大東")),
        JmaOffice("473000", "宮古島地方", "宮古島地方気象台", 24.793, 125.281, listOf("宮古島", "宮古", "平良", "Miyako")),
        JmaOffice("474000", "八重山地方", "石垣島地方気象台", 24.338, 124.164, listOf("石垣島", "八重山", "西表", "与那国", "Ishigaki"))
    )

    val STATIONS = listOf(
        AmedasStation("11016", "稚内", 45.415, 141.6783),
        AmedasStation("11061", "声問", 45.4033, 141.8017),
        AmedasStation("11076", "浜鬼志別", 45.335, 142.17),
        AmedasStation("11121", "沼川", 45.2483, 141.8517),
        AmedasStation("11176", "豊富", 45.1017, 141.78),
        AmedasStation("11276", "中頓別", 44.965, 142.28),
        AmedasStation("11291", "北見枝幸", 44.94, 142.585),
        AmedasStation("11316", "歌登", 44.8417, 142.48),
        AmedasStation("12041", "音威子府", 44.7267, 142.265),
        AmedasStation("12141", "美深", 44.48, 142.3417),
        AmedasStation("12181", "名寄", 44.37, 142.4567),
        AmedasStation("12231", "下川", 44.295, 142.6367),
        AmedasStation("12301", "和寒", 44.0283, 142.41),
        AmedasStation("12411", "上川", 43.8467, 142.7533),
        AmedasStation("12442", "旭川", 43.7567, 142.3717),
        AmedasStation("12501", "東神楽", 43.67, 142.4467),
        AmedasStation("12551", "美瑛", 43.5883, 142.4933),
        AmedasStation("12626", "富良野", 43.3333, 142.4),
        AmedasStation("12691", "幾寅", 43.1683, 142.5683),
        AmedasStation("12746", "占冠", 42.9783, 142.395),
        AmedasStation("13061", "天塩", 44.8933, 141.7617),
        AmedasStation("13121", "初山別", 44.5233, 141.77),
        AmedasStation("13181", "羽幌", 44.3633, 141.7),
        AmedasStation("13277", "留萌", 43.945, 141.6317),
        AmedasStation("13321", "幌糠", 43.8533, 141.76),
        AmedasStation("14071", "厚田", 43.3967, 141.4367),
        AmedasStation("14101", "新篠津", 43.22, 141.645),
        AmedasStation("14121", "石狩", 43.1933, 141.37),
        AmedasStation("14163", "札幌", 43.06, 141.3283),
        AmedasStation("14206", "恵庭島松", 42.9267, 141.565),
        AmedasStation("14296", "千歳", 42.775, 141.6917),
        AmedasStation("15041", "朱鞠内", 44.2817, 142.1617),
        AmedasStation("15076", "幌加内", 44.01, 142.16),
        AmedasStation("15116", "石狩沼田", 43.815, 141.9267),
        AmedasStation("15156", "秩父別", 43.78, 141.9617),
        AmedasStation("15161", "深川", 43.7183, 142.0733),
        AmedasStation("15241", "滝川", 43.57, 141.9383),
        AmedasStation("15251", "芦別", 43.52, 142.19),
        AmedasStation("15321", "美唄", 43.3633, 141.8267),
        AmedasStation("15356", "岩見沢", 43.2117, 141.785),
        AmedasStation("15442", "夕張", 43.0383, 141.9567),
        AmedasStation("16076", "余市", 43.1817, 140.7583),
        AmedasStation("16091", "小樽", 43.1817, 141.015),
        AmedasStation("16156", "共和", 42.98, 140.6033),
        AmedasStation("16206", "蘭越", 42.81, 140.5417),
        AmedasStation("16217", "倶知安", 42.9, 140.7567),
        AmedasStation("16252", "寿都", 42.795, 140.2233),
        AmedasStation("16286", "喜茂別", 42.7933, 140.9483),
        AmedasStation("16321", "黒松内", 42.6633, 140.31),
        AmedasStation("17036", "雄武", 44.58, 142.9633),
        AmedasStation("17091", "西興部", 44.3317, 142.935),
        AmedasStation("17112", "紋別", 44.345, 143.355),
        AmedasStation("17116", "紋別小向", 44.3033, 143.4033),
        AmedasStation("17196", "滝上", 44.1767, 143.0617),
        AmedasStation("17306", "遠軽", 44.0533, 143.54),
        AmedasStation("17316", "佐呂間", 44.0183, 143.76),
        AmedasStation("17341", "網走", 44.0167, 144.2783),
        AmedasStation("17351", "宇登呂", 44.0517, 144.9817),
        AmedasStation("17482", "白滝", 43.865, 143.1533),
        AmedasStation("17521", "北見", 43.7767, 143.8417),
        AmedasStation("17531", "女満別", 43.88, 144.1633),
        AmedasStation("17561", "斜里", 43.885, 144.7),
        AmedasStation("17596", "留辺蘂", 43.7417, 143.45),
        AmedasStation("17717", "津別", 43.7017, 144.0333),
        AmedasStation("18038", "羅臼", 44.0233, 145.1867),
        AmedasStation("18171", "中標津", 43.5433, 144.9783),
        AmedasStation("18174", "根室中標津", 43.5767, 144.96),
        AmedasStation("18256", "別海", 43.3817, 145.1183),
        AmedasStation("18273", "根室", 43.33, 145.585),
        AmedasStation("18311", "厚床", 43.22, 145.2267),
        AmedasStation("19021", "川湯", 43.6167, 144.4567),
        AmedasStation("19076", "阿寒湖畔", 43.4367, 144.0833),
        AmedasStation("19151", "標茶", 43.2917, 144.5867),
        AmedasStation("19191", "鶴居", 43.2317, 144.325),
        AmedasStation("19261", "中徹別", 43.1983, 144.1417),
        AmedasStation("19347", "鶴丘", 43.04, 144.1917),
        AmedasStation("19376", "太田", 43.09, 144.7783),
        AmedasStation("19416", "白糠", 42.97, 144.0617),
        AmedasStation("19432", "釧路", 42.985, 144.3767),
        AmedasStation("20146", "陸別", 43.4717, 143.7533),
        AmedasStation("20186", "ぬかびら源泉郷", 43.3667, 143.1917),
        AmedasStation("20266", "上士幌", 43.2417, 143.295),
        AmedasStation("20341", "本別", 43.1317, 143.6),
        AmedasStation("20356", "新得", 43.0767, 142.84),
        AmedasStation("20421", "芽室", 42.9, 143.0433),
        AmedasStation("20432", "帯広", 42.9217, 143.2117),
        AmedasStation("20506", "浦幌", 42.8083, 143.6567),
        AmedasStation("20551", "帯広泉", 42.7333, 143.2167),
        AmedasStation("20601", "上札内", 42.6417, 143.0967),
        AmedasStation("20696", "大樹", 42.5, 143.2733),
        AmedasStation("20751", "広尾", 42.2933, 143.3167),
        AmedasStation("21126", "穂別", 42.7617, 142.1433),
        AmedasStation("21161", "大滝", 42.67, 141.0783),
        AmedasStation("21187", "苫小牧", 42.6233, 141.5467),
        AmedasStation("21226", "大岸", 42.59, 140.6433),
        AmedasStation("21261", "白老", 42.5433, 141.3517),
        AmedasStation("21312", "登別", 42.4583, 141.1183),
        AmedasStation("21323", "室蘭", 42.3117, 140.975),
        AmedasStation("22036", "日高", 42.8767, 142.4417),
        AmedasStation("22241", "静内", 42.3433, 142.3617),
        AmedasStation("22306", "中杵臼", 42.2233, 142.9483),
        AmedasStation("22327", "浦河", 42.1617, 142.7767),
        AmedasStation("23031", "長万部", 42.5167, 140.3733),
        AmedasStation("23086", "八雲", 42.2517, 140.2717),
        AmedasStation("23166", "森", 42.0667, 140.5883),
        AmedasStation("23232", "函館", 41.8167, 140.7533),
        AmedasStation("23281", "高松", 41.77, 140.8217),
        AmedasStation("24051", "今金", 42.4283, 140.0083),
        AmedasStation("24141", "熊石", 42.1283, 139.985),
        AmedasStation("24201", "鶉", 41.93, 140.3117),
        AmedasStation("24217", "江差", 41.8667, 140.1233),
        AmedasStation("31001", "大間", 41.5267, 140.9117),
        AmedasStation("31111", "むつ", 41.2833, 141.21),
        AmedasStation("31136", "今別", 41.18, 140.4817),
        AmedasStation("31156", "脇野沢", 41.145, 140.8217),
        AmedasStation("31296", "五所川原", 40.8083, 140.4583),
        AmedasStation("31312", "青森", 40.8217, 140.7683),
        AmedasStation("31332", "野辺地", 40.885, 141.16),
        AmedasStation("31366", "鰺ケ沢", 40.7767, 140.205),
        AmedasStation("31386", "青森大谷", 40.7333, 140.6883),
        AmedasStation("31436", "深浦", 40.645, 139.9317),
        AmedasStation("31461", "弘前", 40.6117, 140.455),
        AmedasStation("31482", "酸ケ湯", 40.6483, 140.8483),
        AmedasStation("31586", "十和田", 40.6067, 141.2333),
        AmedasStation("31602", "八戸", 40.5267, 141.5217),
        AmedasStation("31646", "碇ケ関", 40.4817, 140.62),
        AmedasStation("31721", "三戸", 40.3833, 141.2567),
        AmedasStation("32111", "能代", 40.1983, 140.0317),
        AmedasStation("32126", "鷹巣", 40.2267, 140.3717),
        AmedasStation("32146", "鹿角", 40.215, 140.7867),
        AmedasStation("32296", "五城目", 39.9383, 140.115),
        AmedasStation("32311", "阿仁合", 39.9933, 140.4033),
        AmedasStation("32402", "秋田", 39.7167, 140.0983),
        AmedasStation("32451", "雄和", 39.615, 140.2183),
        AmedasStation("32466", "角館", 39.6033, 140.5567),
        AmedasStation("32496", "大正寺", 39.5267, 140.2333),
        AmedasStation("32571", "本荘", 39.36, 140.055),
        AmedasStation("32596", "横手", 39.32, 140.555),
        AmedasStation("32626", "矢島", 39.235, 140.1367),
        AmedasStation("32691", "湯沢", 39.1867, 140.4633),
        AmedasStation("32771", "湯の岱", 38.96, 140.5283),
        AmedasStation("33071", "二戸", 40.2983, 141.2983),
        AmedasStation("33146", "久慈", 40.1683, 141.7483),
        AmedasStation("33176", "奥中山", 40.06, 141.225),
        AmedasStation("33186", "葛巻", 40.04, 141.4567),
        AmedasStation("33226", "岩手松尾", 39.9517, 141.065),
        AmedasStation("33326", "岩泉", 39.8467, 141.795),
        AmedasStation("33421", "雫石", 39.6967, 140.975),
        AmedasStation("33431", "盛岡", 39.6983, 141.165),
        AmedasStation("33441", "区界", 39.65, 141.3533),
        AmedasStation("33472", "宮古", 39.6467, 141.965),
        AmedasStation("33576", "花巻", 39.4283, 141.135),
        AmedasStation("33631", "湯田", 39.31, 140.7767),
        AmedasStation("33671", "遠野", 39.3383, 141.5433),
        AmedasStation("33716", "北上", 39.2883, 141.11),
        AmedasStation("33877", "大船渡", 39.0633, 141.7133),
        AmedasStation("33911", "一関", 38.9333, 141.125),
        AmedasStation("34012", "駒ノ湯", 38.9133, 140.8283),
        AmedasStation("34096", "川渡", 38.7433, 140.76),
        AmedasStation("34216", "古川", 38.5983, 140.9117),
        AmedasStation("34292", "石巻", 38.4267, 141.2983),
        AmedasStation("34311", "新川", 38.3033, 140.6367),
        AmedasStation("34392", "仙台", 38.2617, 140.8967),
        AmedasStation("34436", "名取", 38.1383, 140.9167),
        AmedasStation("34461", "白石", 38.015, 140.6117),
        AmedasStation("35052", "酒田", 38.9083, 139.8433),
        AmedasStation("35116", "金山", 38.8783, 140.3317),
        AmedasStation("35146", "狩川", 38.8, 139.9733),
        AmedasStation("35162", "新庄", 38.7567, 140.3117),
        AmedasStation("35176", "向町", 38.7583, 140.5167),
        AmedasStation("35216", "肘折", 38.6067, 140.1633),
        AmedasStation("35231", "尾花沢", 38.6083, 140.4117),
        AmedasStation("35361", "大井沢", 38.39, 139.9933),
        AmedasStation("35376", "左沢", 38.37, 140.1917),
        AmedasStation("35426", "山形", 38.255, 140.345),
        AmedasStation("35456", "長井", 38.105, 140.015),
        AmedasStation("35486", "小国", 38.0783, 139.735),
        AmedasStation("35552", "米沢", 37.9117, 140.1433),
        AmedasStation("36056", "茂庭", 37.8917, 140.4367),
        AmedasStation("36127", "福島", 37.7583, 140.47),
        AmedasStation("36251", "西会津", 37.5883, 139.6567),
        AmedasStation("36276", "猪苗代", 37.5517, 140.1083),
        AmedasStation("36342", "金山", 37.4733, 139.5283),
        AmedasStation("36361", "若松", 37.4883, 139.91),
        AmedasStation("36426", "只見", 37.3433, 139.3133),
        AmedasStation("36536", "南郷", 37.265, 139.5367),
        AmedasStation("36562", "湯本", 37.2767, 140.0633),
        AmedasStation("36581", "玉川", 37.2267, 140.4267),
        AmedasStation("36641", "田島", 37.2067, 139.795),
        AmedasStation("36667", "白河", 37.1317, 140.215),
        AmedasStation("36716", "桧枝岐", 37.01, 139.375),
        AmedasStation("40201", "水戸", 36.38, 140.4667),
        AmedasStation("40336", "つくば", 36.0567, 140.125),
        AmedasStation("41011", "那須高原", 37.1233, 140.035),
        AmedasStation("41116", "土呂部", 36.8917, 139.5683),
        AmedasStation("41166", "奥日光", 36.7383, 139.5),
        AmedasStation("41277", "宇都宮", 36.5483, 139.8683),
        AmedasStation("42046", "藤原", 36.8633, 139.0583),
        AmedasStation("42091", "みなかみ", 36.7733, 138.965),
        AmedasStation("42121", "草津", 36.6167, 138.5917),
        AmedasStation("42251", "前橋", 36.405, 139.06),
        AmedasStation("43056", "熊谷", 36.15, 139.38),
        AmedasStation("43156", "秩父", 35.99, 139.0733),
        AmedasStation("43241", "さいたま", 35.875, 139.5867),
        AmedasStation("44132", "東京", 35.6917, 139.75),
        AmedasStation("45148", "銚子", 35.7383, 140.8567),
        AmedasStation("45212", "千葉", 35.6017, 140.1033),
        AmedasStation("46106", "横浜", 35.4383, 139.6517),
        AmedasStation("48031", "野沢温泉", 36.9117, 138.4417),
        AmedasStation("48061", "信濃町", 36.8083, 138.1967),
        AmedasStation("48066", "飯山", 36.875, 138.375),
        AmedasStation("48141", "白馬", 36.6983, 137.8617),
        AmedasStation("48156", "長野", 36.6617, 138.1917),
        AmedasStation("48191", "大町", 36.5233, 137.8317),
        AmedasStation("48216", "菅平", 36.5317, 138.325),
        AmedasStation("48331", "軽井沢", 36.3417, 138.5467),
        AmedasStation("48361", "松本", 36.2467, 137.97),
        AmedasStation("48491", "諏訪", 36.045, 138.1083),
        AmedasStation("48531", "開田高原", 35.9383, 137.6017),
        AmedasStation("48767", "飯田", 35.5233, 137.8217),
        AmedasStation("49142", "甲府", 35.6667, 138.5533),
        AmedasStation("49251", "河口湖", 35.5, 138.76),
        AmedasStation("50331", "静岡", 34.975, 138.4033),
        AmedasStation("51106", "名古屋", 35.1667, 136.965),
        AmedasStation("52041", "河合", 36.305, 137.1),
        AmedasStation("52051", "神岡", 36.3217, 137.3083),
        AmedasStation("52081", "白川", 36.2733, 136.8967),
        AmedasStation("52146", "高山", 36.155, 137.2533),
        AmedasStation("52221", "長滝", 35.9233, 136.8317),
        AmedasStation("52381", "樽見", 35.6383, 136.6017),
        AmedasStation("52571", "関ケ原", 35.3633, 136.4667),
        AmedasStation("52586", "岐阜", 35.4, 136.7617),
        AmedasStation("53133", "津", 34.7333, 136.5183),
        AmedasStation("54157", "相川", 38.0283, 138.24),
        AmedasStation("54191", "下関", 38.0917, 139.5633),
        AmedasStation("54232", "新潟", 37.8933, 139.0183),
        AmedasStation("54236", "松浜", 37.955, 139.1117),
        AmedasStation("54296", "新津", 37.7917, 139.0867),
        AmedasStation("54421", "津川", 37.6717, 139.4467),
        AmedasStation("54501", "長岡", 37.45, 138.8233),
        AmedasStation("54541", "柏崎", 37.3517, 138.5533),
        AmedasStation("54566", "守門", 37.3467, 139.0433),
        AmedasStation("54616", "小出", 37.2267, 138.9633),
        AmedasStation("54651", "高田", 37.1067, 138.2467),
        AmedasStation("54661", "安塚", 37.1067, 138.4567),
        AmedasStation("54676", "十日町", 37.1433, 138.7267),
        AmedasStation("54721", "能生", 37.0833, 138.0233),
        AmedasStation("54816", "関山", 36.9333, 138.2217),
        AmedasStation("54836", "津南", 36.9967, 138.6833),
        AmedasStation("54841", "湯沢", 36.9417, 138.81),
        AmedasStation("55022", "朝日", 36.9367, 137.5633),
        AmedasStation("55041", "氷見", 36.825, 136.9683),
        AmedasStation("55056", "魚津", 36.8217, 137.4283),
        AmedasStation("55091", "伏木", 36.7917, 137.055),
        AmedasStation("55102", "富山", 36.7083, 137.2017),
        AmedasStation("55141", "砺波", 36.61, 136.955),
        AmedasStation("55151", "秋ヶ島", 36.6483, 137.1867),
        AmedasStation("56036", "珠洲", 37.4467, 137.2867),
        AmedasStation("56052", "輪島", 37.39, 136.895),
        AmedasStation("56146", "七尾", 37.03, 136.9917),
        AmedasStation("56227", "金沢", 36.5883, 136.6333),
        AmedasStation("56286", "白山河内", 36.3967, 136.62),
        AmedasStation("56301", "加賀中津原", 36.2617, 136.3967),
        AmedasStation("57066", "福井", 36.055, 136.2217),
        AmedasStation("57121", "大野", 35.9717, 136.4967),
        AmedasStation("57206", "今庄", 35.7667, 136.2),
        AmedasStation("57248", "敦賀", 35.6533, 136.0617),
        AmedasStation("57317", "小浜", 35.4833, 135.7817),
        AmedasStation("60051", "今津", 35.4117, 136.0283),
        AmedasStation("60102", "米原", 35.3833, 136.3433),
        AmedasStation("60131", "彦根", 35.275, 136.2433),
        AmedasStation("60216", "大津", 34.9917, 135.9117),
        AmedasStation("61111", "舞鶴", 35.45, 135.3167),
        AmedasStation("61206", "美山", 35.275, 135.55),
        AmedasStation("61286", "京都", 35.0133, 135.7317),
        AmedasStation("62078", "大阪", 34.6817, 135.5183),
        AmedasStation("63016", "香住", 35.625, 134.6267),
        AmedasStation("63051", "豊岡", 35.535, 134.8217),
        AmedasStation("63071", "兎和野高原", 35.4317, 134.5833),
        AmedasStation("63121", "和田山", 35.3233, 134.8483),
        AmedasStation("63518", "神戸", 34.6967, 135.2117),
        AmedasStation("64036", "奈良", 34.6733, 135.8367),
        AmedasStation("65042", "和歌山", 34.2283, 135.1633),
        AmedasStation("66046", "上長田", 35.2967, 133.725),
        AmedasStation("66091", "千屋", 35.1033, 133.435),
        AmedasStation("66136", "今岡", 35.0983, 134.325),
        AmedasStation("66186", "津山", 35.0633, 134.0083),
        AmedasStation("66408", "岡山", 34.685, 133.925),
        AmedasStation("67016", "高野", 35.0333, 132.9017),
        AmedasStation("67151", "大朝", 34.7683, 132.4633),
        AmedasStation("67437", "広島", 34.3983, 132.4617),
        AmedasStation("68022", "西郷", 36.2033, 133.3333),
        AmedasStation("68121", "斐川", 35.4133, 132.89),
        AmedasStation("68132", "松江", 35.4567, 133.065),
        AmedasStation("68276", "横田", 35.1733, 133.1033),
        AmedasStation("68306", "赤名", 35.0017, 132.7117),
        AmedasStation("68401", "瑞穂", 34.8533, 132.53),
        AmedasStation("68431", "弥栄", 34.7767, 132.1083),
        AmedasStation("69006", "境", 35.5433, 133.235),
        AmedasStation("69076", "米子", 35.4333, 133.3383),
        AmedasStation("69101", "倉吉", 35.4733, 133.8383),
        AmedasStation("69122", "鳥取", 35.4867, 134.2383),
        AmedasStation("69246", "智頭", 35.2633, 134.24),
        AmedasStation("71106", "徳島", 34.0667, 134.5733),
        AmedasStation("72086", "高松", 34.3183, 134.0533),
        AmedasStation("73166", "松山", 33.8433, 132.7767),
        AmedasStation("74182", "高知", 33.5667, 133.5483),
        AmedasStation("81286", "山口", 34.1617, 131.4617),
        AmedasStation("81428", "下関", 33.9483, 130.925),
        AmedasStation("82182", "福岡", 33.5817, 130.375),
        AmedasStation("83216", "大分", 33.235, 131.6183),
        AmedasStation("84496", "長崎", 32.7333, 129.8667),
        AmedasStation("85142", "佐賀", 33.265, 130.305),
        AmedasStation("86141", "熊本", 32.8133, 130.7067),
        AmedasStation("86157", "南阿蘇", 32.8317, 131.0133),
        AmedasStation("87376", "宮崎", 31.9383, 131.4133),
        AmedasStation("88317", "鹿児島", 31.555, 130.5467),
        AmedasStation("91197", "那覇", 26.2067, 127.6867),
        AmedasStation("92011", "南大東", 25.8283, 131.2283),
        AmedasStation("93041", "宮古島", 24.7933, 125.2783),
        AmedasStation("94081", "石垣島", 24.3367, 124.1633),
    )

    /**
     * 座標および地名情報から JmaAreaInfo（officeCode + stationCode）を特定する
     */
    fun resolve(latitude: Double, longitude: Double, cityName: String = "", country: String = ""): JmaAreaInfo {
        // 1. 地名・国名文字列による予報区（オフィス）のマッチング
        // 短縮語 ("津", "Tsu") による誤爆（大津・唐津・Matsuyama等）を防ぐため、
        // 短いキーワードはトークン完全一致とし、最も長く一致するキーワードを優先する
        val query = (cityName + " " + country).trim()
        var matchedOffice: JmaOffice? = null
        var bestMatchLen = 0
        if (query.isNotBlank()) {
            val tokens = query.split(" ", ",", "·", "/", "-", "(", ")").map { it.trim() }.filter { it.isNotEmpty() }
            val strippedTokens = tokens.map { token ->
                if (token.length >= 2) {
                    token.removeSuffix("都道府県").removeSuffix("都").removeSuffix("府").removeSuffix("県")
                        .removeSuffix("市").removeSuffix("区").removeSuffix("町").removeSuffix("村")
                } else {
                    token
                }
            }
            for (office in OFFICES) {
                for (kw in office.keywords) {
                    val isShortAscii = kw.length <= 3 && kw.all { it.code < 128 }
                    val isSingleCharJa = kw.length == 1
                    val matched = when {
                        isSingleCharJa -> {
                            tokens.any { it == kw || it == "${kw}市" } || strippedTokens.any { it == kw }
                        }
                        isShortAscii -> {
                            tokens.any { it.equals(kw, ignoreCase = true) }
                        }
                        kw.length == 2 -> {
                            tokens.any {
                                it.equals(kw, ignoreCase = true) ||
                                    it.startsWith(kw) && (
                                        it == "${kw}市" || it == "${kw}区" || it == "${kw}町" || it == "${kw}村" ||
                                            it == "${kw}県" || it == "${kw}府" || it == "${kw}都" || it == "${kw}地方"
                                        )
                            } || strippedTokens.any { it.equals(kw, ignoreCase = true) }
                        }
                        else -> {
                            query.contains(kw, ignoreCase = true)
                        }
                    }
                    if (matched && kw.length > bestMatchLen) {
                        bestMatchLen = kw.length
                        matchedOffice = office
                    }
                }
            }
        }

        // 2. 文字列で未特定の場合、座標から最近傍の予報区オフィスを特定
        val selectedOffice = matchedOffice ?: OFFICES.minByOrNull { office ->
            val dLat = office.lat - latitude
            val dLon = office.lon - longitude
            dLat * dLat + dLon * dLon
        } ?: OFFICES.first { it.code == "130000" }

        // 3. 最寄りのアメダス観測所を特定（321地点の空間近傍探索）
        val nearestStation = STATIONS.minByOrNull { st ->
            val dLat = st.lat - latitude
            val dLon = st.lon - longitude
            dLat * dLat + dLon * dLon
        } ?: AmedasStation("44132", "東京", 35.6917, 139.75)

        return JmaAreaInfo(
            officeCode = selectedOffice.code,
            officeName = selectedOffice.officeName,
            stationCode = nearestStation.code,
            stationName = nearestStation.name
        )
    }
}
