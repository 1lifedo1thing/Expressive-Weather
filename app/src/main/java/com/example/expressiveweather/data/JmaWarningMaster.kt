package com.example.expressiveweather.data

/**
 * 警報・注意報の区分
 */
enum class WarningCategory(val priority: Int, val displayNameJa: String) {
    SPECIAL_WARNING(1, "特別警報"),
    WARNING(2, "警報"),
    ADVISORY(3, "注意報")
}

/**
 * 警報・注意報の公式詳細解説データ
 */
data class JmaWarningDetailInfo(
    val code: String,
    val name: String,
    val category: WarningCategory,
    val equivalentLevel: String,
    val phenomenon: String,
    val actionGuide: String,
    val cautionPoints: List<String>
) {
    fun getLocalizedName(language: AppLanguage): String {
        return JmaWarningMaster.getLocalizedWarningName(name, language)
    }

    fun getLocalizedPhenomenon(language: AppLanguage): String {
        if (language == AppLanguage.JAPANESE) return phenomenon
        return JmaWarningMaster.getLocalizedPhenomenon(name, category, language, phenomenon)
    }

    fun getLocalizedActionGuide(language: AppLanguage): String {
        if (language == AppLanguage.JAPANESE) return actionGuide
        return JmaWarningMaster.getLocalizedActionGuide(name, category, language, actionGuide)
    }

    fun getLocalizedCautionPoints(language: AppLanguage): List<String> {
        if (language == AppLanguage.JAPANESE) return cautionPoints
        return JmaWarningMaster.getLocalizedCautionPoints(name, category, language, cautionPoints)
    }
}

/**
 * 気象庁 警報・注意報（全26種類）公式解説マスター辞書
 */
object JmaWarningMaster {

    private val masterList = listOf(
        // ================= 特別警報 (6種) =================
        JmaWarningDetailInfo(
            code = "33",
            name = "大雨特別警報",
            category = WarningCategory.SPECIAL_WARNING,
            equivalentLevel = "警戒レベル5相当（命の危険）",
            phenomenon = "数十年に一度の降雨量となる大雨が予想され、土砂崩れや大規模な浸水、河川の氾濫など重大な災害がすでに発生している可能性が極めて高い状態です。",
            actionGuide = "直ちに命を守るための最善の行動をとってください。周囲がすでに冠水している場合や夜間の移動が危険な場合は、頑丈な建物の2階以上や崖と反対側の部屋へ退避（垂直避難）してください。",
            cautionPoints = listOf("土砂崩れ・崖崩れ", "低地の浸水・地下街冠水", "河川の越水・堤防決壊")
        ),
        JmaWarningDetailInfo(
            code = "35",
            name = "暴風特別警報",
            category = WarningCategory.SPECIAL_WARNING,
            equivalentLevel = "警戒レベル5相当（命の危険）",
            phenomenon = "過去に経験がないような猛烈な台風や発達した低気圧により、家屋の倒壊、看板や屋根瓦の飛散、大規模な停電など甚大な被害が発生する恐れがあります。",
            actionGuide = "不要不急の外出は絶対に避け、頑丈な建物の中で窓から離れた場所で待機してください。暴風が吹き荒れる前の移動が困難な場合は直ちに屋内の安全な部屋へ避難してください。",
            cautionPoints = listOf("猛烈な暴風による飛来物", "家屋の破損・倒木", "送電網破損による広域停電")
        ),
        JmaWarningDetailInfo(
            code = "32",
            name = "暴風雪特別警報",
            category = WarningCategory.SPECIAL_WARNING,
            equivalentLevel = "警戒レベル5相当（命の危険）",
            phenomenon = "数十年に一度の猛烈な吹雪・ホワイトアウトが発生し、屋外では方向感覚を失い遭難する危険性や、車内立ち往生による一酸化炭素中毒の危険が極めて高まります。",
            actionGuide = "外出は直ちに中止し、屋内の安全な場所で待機してください。やむを得ず車内にいる場合は排気筒が雪で埋もれないよう定期的に除雪し、一酸化炭素中毒に警戒してください。",
            cautionPoints = listOf("ホワイトアウト（視界ゼロ）", "車内立ち往生・遭難", "猛吹雪による孤立")
        ),
        JmaWarningDetailInfo(
            code = "36",
            name = "大雪特別警報",
            category = WarningCategory.SPECIAL_WARNING,
            equivalentLevel = "警戒レベル5相当（命の危険）",
            phenomenon = "数十年に一度の記録的な大雪により、家屋の倒壊、雪崩、幹線道路や鉄道の長期寸断、集落の孤立など重大な災害が発生する恐れがあります。",
            actionGuide = "不要不急の外出を控え、無理な雪下ろしは避けてください。除雪作業を行う場合は必ず2人以上で命綱やヘルメットを着用し、屋根からの落雪に警戒してください。",
            cautionPoints = listOf("記録的豪雪・家屋倒壊", "大規模な交通麻痺・集落孤立", "急斜面での雪崩")
        ),
        JmaWarningDetailInfo(
            code = "37",
            name = "波浪特別警報",
            category = WarningCategory.SPECIAL_WARNING,
            equivalentLevel = "警戒レベル5相当（命の危険）",
            phenomenon = "数十年に一度の猛烈なしけや高波により、防波堤を越える大波、海岸施設の損壊、沿岸部の越波による大規模な浸水が発生する恐れがあります。",
            actionGuide = "海岸、堤防、港湾部には絶対に近づかないでください。沿岸低地にお住まいの方は速やかに内陸や高台の安全な場所へ退避してください。",
            cautionPoints = listOf("防波堤を越える大波", "沿岸部の越波被害", "海岸施設・船舶の損壊")
        ),
        JmaWarningDetailInfo(
            code = "38",
            name = "高潮特別警報",
            category = WarningCategory.SPECIAL_WARNING,
            equivalentLevel = "警戒レベル5相当（命の危険）",
            phenomenon = "猛烈な台風に伴う気圧低下や強風の吹き寄せにより、過去最高水準の潮位上昇が発生し、堤防を越えた大規模な海水流入で街が水没する重大な危機です。",
            actionGuide = "潮位が上昇する前に、直ちに沿岸部や低地から高台へ立ち退き避難してください。逃げ遅れた場合は鉄筋コンクリート造の堅固な建物の高層階へ緊急退避してください。",
            cautionPoints = listOf("記録的な潮位上昇", "防潮堤の越流・破壊", "沿岸低地・地下への海水流入")
        ),

        // ================= 警報 (7種) =================
        JmaWarningDetailInfo(
            code = "03",
            name = "大雨警報",
            category = WarningCategory.WARNING,
            equivalentLevel = "警戒レベル3〜4相当（避難指示・高齢者等避難）",
            phenomenon = "大雨により重大な土砂災害や低地浸水害が発生する恐れがあります。短時間強雨による道路冠水やアンダーパス水没に厳重な警戒が必要です。",
            actionGuide = "自治体の避難指示やハザードマップを確認し、土砂災害警戒区域や浸水想定区域にいる方は早めの避難行動をとってください。",
            cautionPoints = listOf("急傾斜地の山崩れ・がけ崩れ", "道路冠水・アンダーパス冠水", "側溝や水路の増水")
        ),
        JmaWarningDetailInfo(
            code = "04",
            name = "洪水警報",
            category = WarningCategory.WARNING,
            equivalentLevel = "警戒レベル3〜4相当（河川氾濫警戒）",
            phenomenon = "大雨や融雪によって河川の水位が急激に上昇し、堤防の越水や決壊、低地の浸水など重大な洪水災害が発生する恐れがあります。",
            actionGuide = "増水した河川や用水路には絶対に近づかないでください。氾濫危険水位に達した河川の流域では自治体の避難情報に従い安全な高台へ避難してください。",
            cautionPoints = listOf("河川の増水・越水・氾濫", "堤防の浸食・決壊", "用水路や支流の逆流")
        ),
        JmaWarningDetailInfo(
            code = "05",
            name = "暴風警報",
            category = WarningCategory.WARNING,
            equivalentLevel = "警戒レベル3相当（外出危険）",
            phenomenon = "台風や発達した低気圧により、看板の落下、屋根瓦の飛散、街路樹の倒木、電線の断線など重大な被害を及ぼす非常に強い風が吹き荒れる恐れがあります。",
            actionGuide = "風が強まる前に屋外の飛びやすい物を屋内に片付けるか固定してください。暴風が吹き始めたら不要不急の外出を控え、窓から離れて過ごしてください。",
            cautionPoints = listOf("看板・屋根瓦などの飛来物", "倒木・電線切断による停電", "突風による転倒・車両横転")
        ),
        JmaWarningDetailInfo(
            code = "02",
            name = "暴風雪警報",
            category = WarningCategory.WARNING,
            equivalentLevel = "警戒レベル3相当（猛吹雪警戒）",
            phenomenon = "猛烈な風を伴う雪（猛吹雪）により視界が極端に悪化し、積雪や吹きだまりによって交通網の麻痺や車両の立ち往生が発生する恐れがあります。",
            actionGuide = "不要不急の外出や車の運転は控えてください。運転中にやむを得ず立ち往生した場合は、防寒着を着込みマフラー付近の除雪を行って救助を待ってください。",
            cautionPoints = listOf("見通しの利かない猛吹雪", "吹きだまりによる車両立ち往生", "凍結路面でのスリップ事故")
        ),
        JmaWarningDetailInfo(
            code = "06",
            name = "大雪警報",
            category = WarningCategory.WARNING,
            equivalentLevel = "警戒レベル3相当（豪雪警戒）",
            phenomenon = "短時間または集中的な降雪により、道路や鉄道の広範囲な交通障害、路面凍結、家屋の損壊、雪崩などの重大な被害が発生する恐れがあります。",
            actionGuide = "冬用タイヤやチェーンを必ず装着し、大雪が予想される時間帯の車での外出は避けてください。除雪作業はヘルメットを着用し複数人で行ってください。",
            cautionPoints = listOf("路面積雪・凍結によるスリップ", "鉄道の運転見合わせ・道路通行止め", "湿った雪による倒木・架線切断")
        ),
        JmaWarningDetailInfo(
            code = "07",
            name = "波浪警報",
            category = WarningCategory.WARNING,
            equivalentLevel = "警戒レベル3相当（高波警戒）",
            phenomenon = "強風や台風等により高い波（しけ）が発生し、沿岸施設や海岸道路への波の乗り越え、船舶の座礁や転覆などの重大な被害が発生する恐れがあります。",
            actionGuide = "海岸、防波堤、砂浜、磯場には絶対に近づかないでください。釣りやサーフィン、マリンレジャーは直ちに中止してください。",
            cautionPoints = listOf("沿岸・防波堤への高波越波", "海中への転落・流失事故", "港湾施設や係留船の損壊")
        ),
        JmaWarningDetailInfo(
            code = "08",
            name = "高潮警報",
            category = WarningCategory.WARNING,
            equivalentLevel = "警戒レベル3〜4相当（潮位上昇警戒）",
            phenomenon = "台風や低気圧に伴う吸い上げ効果や強風の吹き寄せにより、満潮時を中心に潮位が異常に上昇し、沿岸低地の浸水や道路冠水が発生する恐れがあります。",
            actionGuide = "海岸付近の低地や河口付近にお住まいの方は、潮位が高くなる前に自治体の避難情報に従い安全な場所へ移動してください。",
            cautionPoints = listOf("満潮時間帯の潮位急上昇", "海岸沿い道路・家屋の浸水", "地下施設への海水流入")
        ),

        // ================= 注意報 (13種) =================
        JmaWarningDetailInfo(
            code = "10",
            name = "大雨注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（気象悪化注意）",
            phenomenon = "大雨により、急な道路冠水や側溝の溢れ、地盤の緩みによる局地的な土砂崩れが発生する恐れがあります。",
            actionGuide = "最新の雨雲レーダーや気象情報を確認し、危険な斜面や増水しやすい水路には近づかないようにしてください。",
            cautionPoints = listOf("側溝や低地の冠水", "雨による視界不良・スリップ", "山道での落石・小崩落")
        ),
        JmaWarningDetailInfo(
            code = "18",
            name = "洪水注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（河川増水注意）",
            phenomenon = "雨や雪解けにより河川や用水路の水位が上昇し、川原や親水公園が水没する恐れがあります。",
            actionGuide = "川辺や親水公園でレジャーを行っている場合は速やかに河川敷から上がってください。用水路付近を通る際は足元に注意してください。",
            cautionPoints = listOf("急激な河川水位の上昇", "川原・中州の水没・孤立", "用水路と道路の境目不明瞭化")
        ),
        JmaWarningDetailInfo(
            code = "14",
            name = "雷注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（急変注意）",
            phenomenon = "積乱雲の発達により、落雷、急な強い雨、竜巻などの激しい突風、降雹（ひょう）が発生する恐れがあります。",
            actionGuide = "雷鳴が聞こえたり空が急に暗くなったりした場合は、木の下を避け、頑丈な建物や車内へ直ちに避難してください。",
            cautionPoints = listOf("落雷による感電被害", "竜巻・ダウンバーストによる突風", "降雹による車のへこみ・負傷")
        ),
        JmaWarningDetailInfo(
            code = "15",
            name = "強風注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（風への注意）",
            phenomenon = "強い風により、自転車の転倒、物干し竿や植木鉢の落下、高所作業での事故が発生する恐れがあります。",
            actionGuide = "ベランダの飛びやすい物を固定または屋内に移動し、車のドアの開閉時や橋の上での横風に注意してください。",
            cautionPoints = listOf("ベランダや庭の飛散物", "二輪車や歩行者の転倒", "高速道路や橋梁での横風煽り")
        ),
        JmaWarningDetailInfo(
            code = "13",
            name = "風雪注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（風雪注意）",
            phenomenon = "雪を伴う強い風により、局地的な視界の悪化や雪だまりが発生する恐れがあります。",
            actionGuide = "防寒対策を万全にし、車の運転時はライトを点灯してスピードを控えめに保ってください。",
            cautionPoints = listOf("一時的な視界悪化", "道路の吹きだまり", "凍結路面のスリップ")
        ),
        JmaWarningDetailInfo(
            code = "12",
            name = "大雪注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（降雪注意）",
            phenomenon = "まとまった降雪により、道路の積雪・凍結、歩行者の転倒、交通機関のダイヤ乱れが発生する恐れがあります。",
            actionGuide = "滑りにくい靴を履き、時間に余裕を持って行動してください。車の運転時はスタッドレスタイヤを装着してください。",
            cautionPoints = listOf("歩行時の転倒・骨折", "ノーマルタイヤ走行による立ち往生", "電線や樹木への着雪")
        ),
        JmaWarningDetailInfo(
            code = "16",
            name = "波浪注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（高波注意）",
            phenomenon = "海上や沿岸で高い波が発生し、釣り人やマリンレジャー客が波にさらわれる恐れがあります。",
            actionGuide = "磯釣りや防波堤への立ち入りは控えてください。海岸付近を散策する際は高波に注意してください。",
            cautionPoints = listOf("磯場や突堤での高波", "突然押し寄せる土用波", "小型船舶の動揺")
        ),
        JmaWarningDetailInfo(
            code = "19",
            name = "高潮注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（潮位注意）",
            phenomenon = "満潮時を中心に海水面が高くなり、沿岸の低地や排水路から海水が逆流して冠水する恐れがあります。",
            actionGuide = "海岸付近の低地での駐車は避け、満潮時刻前後の浸水に注意してください。",
            cautionPoints = listOf("満潮時刻前後の潮位上昇", "排水溝からの海水逆流", "海岸沿い道路の水たまり")
        ),
        JmaWarningDetailInfo(
            code = "20",
            name = "濃霧注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（視界不良注意）",
            phenomenon = "濃い霧が発生して見通しが著しく悪化し、道路交通や航空機、船舶の運航に支障が出る恐れがあります。",
            actionGuide = "車の運転時はフォグランプやヘッドライトを下向きで点灯し、車間距離を十分にとって徐行してください。",
            cautionPoints = listOf("急激な視程（視界）低下", "高速道路での追突事故", "航空便やフェリーの遅延")
        ),
        JmaWarningDetailInfo(
            code = "21",
            name = "乾燥注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（火災注意）",
            phenomenon = "空気が著しく乾燥し、木材や落ち葉などが燃えやすくなっており、火災が発生した場合に急速に燃え広がる危険があります。",
            actionGuide = "たばこのポイ捨てや屋外での火気の使用は絶対に避け、調理器具や暖房器具の消し忘れに十分注意してください。",
            cautionPoints = listOf("たき火や野焼きからの延焼", "暖房器具からの出火", "静電気による引火")
        ),
        JmaWarningDetailInfo(
            code = "22",
            name = "なだれ注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（雪崩注意）",
            phenomenon = "気温の上昇や積雪構造の不安定化により、積雪地帯の山腹や急斜面で雪崩が発生する恐れがあります。",
            actionGuide = "急傾斜地や過去に雪崩が発生した斜面には近づかないでください。スキー場ではコース外に立ち入らないでください。",
            cautionPoints = listOf("急斜面での表層雪崩・全層雪崩", "気温上昇時の雪解け崩落", "雪庇（せっぴ）の崩壊")
        ),
        JmaWarningDetailInfo(
            code = "17",
            name = "融雪注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（雪解け注意）",
            phenomenon = "気温の上昇や降雨により積雪が急速に解け、低地の冠水や屋根からの落雪、土砂崩れが発生する恐れがあります。",
            actionGuide = "軒下を通る際は屋根からの落雪に警戒し、道路冠水や側溝の増水に注意してください。",
            cautionPoints = listOf("屋根からの落雪・雪庇落下", "雪解け水による道路冠水", "地盤緩みによる小規模土砂崩れ")
        ),
        JmaWarningDetailInfo(
            code = "24",
            name = "霜注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（農作物注意）",
            phenomenon = "夜間から早朝にかけて地表付近が冷え込み、霜が降りて農作物や新芽が凍害を受ける恐れがあります。",
            actionGuide = "農家の方は防霜ネットや送風機の稼働、散水氷結法などの防霜対策を講じてください。",
            cautionPoints = listOf("農作物・果樹の凍霜害", "早朝の路面凍結", "家庭菜園の新芽被害")
        ),
        JmaWarningDetailInfo(
            code = "23",
            name = "低温注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（凍結注意）",
            phenomenon = "著しい低温が継続し、水道管の凍結破損や、農作物・家畜への生育障害が発生する恐れがあります。",
            actionGuide = "屋外の水道管や給湯器の保温措置・水抜きを行い、十分な防寒対策をとって低体温症を防いでください。",
            cautionPoints = listOf("水道管の凍結・破裂・断水", "給湯器の配管破損", "高齢者のヒートショック・低体温症")
        ),
        JmaWarningDetailInfo(
            code = "25",
            name = "着氷注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（着氷注意）",
            phenomenon = "気温が氷点下の中で水滴や過冷却水が物体に付着して凍結し、電線や船体、アンテナ等に着氷して障害を引き起こす恐れがあります。",
            actionGuide = "船舶は着氷による重心上昇・転覆に警戒し、電力・通信設備の不具合に備えてください。",
            cautionPoints = listOf("電線・アンテナへの着氷", "船体の着氷による復原力低下", "風力発電機の着氷停止")
        ),
        JmaWarningDetailInfo(
            code = "26",
            name = "着雪注意報",
            category = WarningCategory.ADVISORY,
            equivalentLevel = "警戒レベル2相当（着雪注意）",
            phenomenon = "湿った重い雪が電線や樹木、送電鉄塔などに付着し、その重みで倒木や電線の断線、停電が発生する恐れがあります。",
            actionGuide = "停電に備えて懐中電灯やモバイルバッテリーを準備し、倒木や電線切断の恐れがある場所には近づかないでください。",
            cautionPoints = listOf("湿雪の付着による電線断線・停電", "樹木の倒木による道路遮断", "送電鉄塔の冠雪過負荷")
        )
    )

    private val codeMap = masterList.associateBy { it.code }
    private val nameMap = masterList.associateBy { it.name }

    /**
     * 警報名から詳細情報を検索（部分一致対応）
     */
    fun getDetailByName(name: String): JmaWarningDetailInfo {
        nameMap[name]?.let { return it }
        // 部分一致フォールバック
        val found = masterList.firstOrNull { name.contains(it.name) || it.name.contains(name) }
        if (found != null) return found

        // 汎用フォールバック
        val isSpecial = name.contains("特別")
        val isWarning = name.contains("警報")
        val category = if (isSpecial) WarningCategory.SPECIAL_WARNING else if (isWarning) WarningCategory.WARNING else WarningCategory.ADVISORY
        return JmaWarningDetailInfo(
            code = "99",
            name = name,
            category = category,
            equivalentLevel = if (isSpecial) "警戒レベル5相当" else if (isWarning) "警戒レベル3〜4相当" else "警戒レベル2相当",
            phenomenon = "気象庁より${name}が発表されています。天候の急変や周囲の状況変化に十分注意してください。",
            actionGuide = "自治体からの最新情報や気象レーダーを確認し、必要に応じて安全な場所への移動や備えを行ってください。",
            cautionPoints = listOf("気象状況の急変", "自治体からの避難情報確認")
        )
    }

    /**
     * コードから詳細情報を検索
     */
    fun getDetailByCode(code: String): JmaWarningDetailInfo? {
        return codeMap[code]
    }

    /**
     * 警報リストを重要度順（特別警報 > 警報 > 注意報、同カテゴリ内は優先順）にソート
     */
    fun sortWarningsByPriority(items: List<MunicipalityWarningItem>): List<MunicipalityWarningItem> {
        return items.sortedWith(
            compareBy(
                { if (it.isSpecial) 0 else if (it.isWarning) 1 else 2 },
                { item ->
                    when {
                        item.name.contains("大雨") -> 0
                        item.name.contains("暴風") -> 1
                        item.name.contains("洪水") -> 2
                        item.name.contains("大雪") -> 3
                        item.name.contains("高潮") -> 4
                        item.name.contains("波浪") -> 5
                        item.name.contains("雷") -> 6
                        else -> 10
                    }
                }
            )
        )
    }

    // ==========================================
    // 警報・注意報名の多言語翻訳辞書 (公式JMA対訳)
    // ==========================================
    private val warningTranslationsEn = mapOf(
        "大雨特別警報" to "Emergency Warning for Heavy Rain",
        "暴風特別警報" to "Emergency Warning for Violent Winds",
        "暴風雪特別警報" to "Emergency Warning for Blizzard",
        "大雪特別警報" to "Emergency Warning for Heavy Snow",
        "波浪特別警報" to "Emergency Warning for High Waves",
        "高潮特別警報" to "Emergency Warning for Storm Surge",
        "大雨警報" to "Heavy Rain Warning",
        "洪水警報" to "Flood Warning",
        "暴風警報" to "Gale Warning",
        "暴風雪警報" to "Blizzard Warning",
        "大雪警報" to "Heavy Snow Warning",
        "波浪警報" to "High Waves Warning",
        "高潮警報" to "Storm Surge Warning",
        "大雨注意報" to "Heavy Rain Advisory",
        "洪水注意報" to "Flood Advisory",
        "強風注意報" to "Gale Advisory",
        "風雪注意報" to "Snowstorm Advisory",
        "大雪注意報" to "Heavy Snow Advisory",
        "波浪注意報" to "High Waves Advisory",
        "高潮注意報" to "Storm Surge Advisory",
        "雷注意報" to "Thunderstorm Advisory",
        "融雪注意報" to "Snowmelt Advisory",
        "濃霧注意報" to "Dense Fog Advisory",
        "乾燥注意報" to "Dry Air Advisory",
        "なだれ注意報" to "Avalanche Advisory",
        "低温注意報" to "Cold Weather Advisory",
        "霜注意報" to "Frost Advisory",
        "着氷注意報" to "Icing Advisory",
        "着雪注意報" to "Snow Accretion Advisory"
    )

    private val warningTranslationsZhHans = mapOf(
        "大雨特別警報" to "暴雨特别重大预警",
        "暴風特別警報" to "强风暴特别重大预警",
        "暴風雪特別警報" to "暴风雪特别重大预警",
        "大雪特別警報" to "暴雪特别重大预警",
        "波浪特別警報" to "巨浪特别重大预警",
        "高潮特別警報" to "风暴潮特别重大预警",
        "大雨警報" to "暴雨预警",
        "洪水警報" to "洪水预警",
        "暴風警報" to "大风预警",
        "暴風雪警報" to "暴风雪预警",
        "大雪警報" to "暴雪预警",
        "波浪警報" to "巨浪预警",
        "高潮警報" to "风暴潮预警",
        "大雨注意報" to "大雨注意报",
        "洪水注意報" to "洪水注意报",
        "強風注意報" to "大风注意报",
        "風雪注意報" to "风雪注意报",
        "大雪注意報" to "大雪注意报",
        "波浪注意報" to "海浪注意报",
        "高潮注意報" to "风暴潮注意报",
        "雷注意報" to "雷电注意报",
        "融雪注意報" to "融雪注意报",
        "濃霧注意報" to "大雾注意报",
        "乾燥注意報" to "干燥火险注意报",
        "なだれ注意報" to "雪崩注意报",
        "低温注意報" to "低温冰冻注意报",
        "霜注意報" to "霜冻注意报",
        "着氷注意報" to "结冰注意报",
        "着雪注意報" to "积雪注意报"
    )

    private val warningTranslationsZhHant = mapOf(
        "大雨特別警報" to "特大暴雨特別警報",
        "暴風特別警報" to "超強風暴特別警報",
        "暴風雪特別警報" to "暴風雪特別警報",
        "大雪特別警報" to "特大暴雪特別警報",
        "波浪特別警報" to "巨浪特別警報",
        "高潮特別警報" to "風暴潮特別警報",
        "大雨警報" to "大雨警報",
        "洪水警報" to "洪水警報",
        "暴風警報" to "暴風警報",
        "暴風雪警報" to "暴風雪警報",
        "大雪警報" to "大雪警報",
        "波浪警報" to "巨浪警報",
        "高潮警報" to "風暴潮警報",
        "大雨注意報" to "大雨注意報",
        "洪水注意報" to "洪水注意報",
        "強風注意報" to "強風注意報",
        "風雪注意報" to "風雪注意報",
        "大雪注意報" to "大雪注意報",
        "波浪注意報" to "巨浪注意報",
        "高潮注意報" to "風暴潮注意報",
        "雷注意報" to "雷電注意報",
        "融雪注意報" to "融雪注意報",
        "濃霧注意報" to "濃霧注意報",
        "乾燥注意報" to "乾燥火險注意報",
        "なだれ注意報" to "雪崩注意報",
        "低温注意報" to "低溫注意報",
        "霜注意報" to "霜凍注意報",
        "着氷注意報" to "結冰注意報",
        "着雪注意報" to "積雪注意報"
    )

    /**
     * 警報・注意報名の多言語翻訳取得
     */
    fun getLocalizedWarningName(name: String, language: AppLanguage): String {
        return when (language) {
            AppLanguage.ENGLISH -> warningTranslationsEn[name] ?: name
            AppLanguage.SIMPLIFIED_CHINESE -> warningTranslationsZhHans[name] ?: name
            AppLanguage.TRADITIONAL_CHINESE -> warningTranslationsZhHant[name] ?: name
            AppLanguage.JAPANESE -> name
        }
    }

    /**
     * 現象と被害の多言語翻訳取得
     */
    fun getLocalizedPhenomenon(name: String, category: WarningCategory, language: AppLanguage, fallback: String): String {
        return when (language) {
            AppLanguage.ENGLISH -> when {
                name.contains("特別") -> "Extremely hazardous conditions that occur once in decades. Severe disasters such as major landslides or massive flooding are imminent or already occurring."
                name.contains("大雨") -> "Heavy rainfall may cause severe landslides, river flooding, and urban inundation in low-lying areas."
                name.contains("洪水") -> "Significant rise in river levels and hazardous flood conditions are anticipated."
                name.contains("暴風雪") -> "Severe blizzard conditions, whiteouts, and dangerous drifting snow causing serious traffic paralysis."
                name.contains("暴風") -> "Violent winds capable of damaging buildings, knocking down trees, and causing power outages."
                name.contains("大雪") -> "Heavy snowfall resulting in avalanche risk, road blockages, and traffic disruption."
                name.contains("高潮") -> "Dangerous rise in sea levels causing extensive seawater flooding in coastal areas."
                name.contains("波浪") -> "Extremely high and rough seas posing danger to coastal infrastructure and vessels."
                name.contains("雷") -> "Sudden severe downpours, lightning strikes, hail, and gusty winds are expected."
                name.contains("濃霧") -> "Dense fog drastically reducing visibility and disrupting travel."
                name.contains("乾燥") -> "Very dry atmospheric conditions with heightened risk of spreading fires."
                name.contains("なだれ") -> "Snowpack instability leading to high avalanche risk on steep slopes."
                else -> "The Japan Meteorological Agency has issued $name. Please stay alert to rapidly changing weather."
            }
            AppLanguage.SIMPLIFIED_CHINESE -> when {
                name.contains("特別") -> "数十年一遇的极度危险灾害天气。滑坡泥石流、特大洪水等重大灾害极有可能已经发生，危及生命安全。"
                name.contains("大雨") -> "强降雨可能引发严重山体滑坡、泥石流及低洼地区城市积淹水。"
                name.contains("洪水") -> "河流水位暴涨，面临漫坝、决堤及洪水泛滥风险。"
                name.contains("暴風雪") -> "强暴风雪伴随能见度归零（白瞎）现象，极易引发严重交通瘫痪与受困风险。"
                name.contains("暴風") -> "狂风风暴可能造成房屋损毁、树木倒伏、飞散物伤害以及电网供电中断。"
                name.contains("大雪") -> "强降雪易导致积雪压垮简易棚房、道路严重结冰和雪崩灾害。"
                name.contains("高潮") -> "强风暴潮引发海水倒灌，沿海低洼陆地及地下空间面临严重水浸风险。"
                name.contains("波浪") -> "沿海巨浪滔天，危及海岸堤防、渔船及海上作业安全。"
                name.contains("雷") -> "雷电交加，伴有短时强降水、雷雨大风及局部冰雹。"
                name.contains("濃霧") -> "大雾弥漫，能见度骤降，对道路交通和海上航行产生显著影响。"
                name.contains("乾燥") -> "空气干燥，森林火险及城市火灾气象等级极高。"
                name.contains("なだれ") -> "积雪融化或积雪过厚导致山地斜坡雪崩风险极高。"
                else -> "日本气象厅已发布${name}，请密切留意天气突变并做好防范准备。"
            }
            AppLanguage.TRADITIONAL_CHINESE -> when {
                name.contains("特別") -> "數十年一遇的極度危險災害天氣。土石流、特大洪水等重大災害極有可能已經發生，危及生命安全。"
                name.contains("大雨") -> "強降雨可能引發嚴重土石流、山崩及低窪地區積淹水。"
                name.contains("洪水") -> "河川水位暴漲，面臨溢堤、潰堤及洪水氾濫風險。"
                name.contains("暴風雪") -> "強烈暴風雪伴隨能見度歸零現象，極易引發嚴重交通癱瘓與受困風險。"
                name.contains("暴風") -> "強烈風暴可能造成房舍損毀、路樹倒塌、飛散物危害及停電事故。"
                name.contains("大雪") -> "劇烈降雪易導致積雪壓垮建物、路面嚴重結冰與雪崩災害。"
                name.contains("高潮") -> "風暴潮引發海水倒灌，沿海低窪陸地及地下空間面臨嚴重淹水風險。"
                name.contains("波浪") -> "沿岸巨浪洶湧，危及海岸防波堤、船隻與海上作業安全。"
                name.contains("雷") -> "雷電交加，易伴隨瞬間大雨、雷雨強陣風及局部冰雹。"
                name.contains("濃霧") -> "濃霧瀰漫，能見度驟降，對陸路交通與海上航行產生顯著影響。"
                name.contains("乾燥") -> "空氣乾燥，火災氣象風險極高，請注意用火安全。"
                name.contains("なだれ") -> "積雪過厚或融雪導致山區斜坡雪崩風險極高。"
                else -> "日本氣象廳已發布${name}，請密切留意天氣變化並做好防範。"
            }
            AppLanguage.JAPANESE -> fallback
        }
    }

    /**
     * 推奨される避難・安全行動の多言語翻訳取得
     */
    fun getLocalizedActionGuide(name: String, category: WarningCategory, language: AppLanguage, fallback: String): String {
        return when (language) {
            AppLanguage.ENGLISH -> when {
                name.contains("特別") -> "Take immediate action to save your life. If moving outdoors is perilous, move to the 2nd floor or higher of a sturdy building away from mountain slopes."
                name.contains("大雨") || name.contains("洪水") -> "Check local evacuation orders and flood hazard maps. Move to higher ground or a sturdy shelter early before roads submerge."
                name.contains("暴風") || name.contains("暴風雪") -> "Avoid all non-essential trips. Stay indoors away from windows until the violent winds and blizzard subside."
                name.contains("波浪") || name.contains("高潮") -> "Stay completely away from coastlines, sea walls, and harbor areas. Evacuate immediately if residing in low-lying coastal zones."
                name.contains("雷") -> "Seek shelter inside a sturdy building or automobile immediately if thunder is heard. Avoid trees and open areas."
                else -> "Monitor the latest official municipal alerts and radar updates. Ensure personal safety and prepare necessary supplies."
            }
            AppLanguage.SIMPLIFIED_CHINESE -> when {
                name.contains("特別") -> "请立即采取自救应急避难措施！如果室外积水或夜间撤离危险，请紧急退避至坚固建筑物二层以上、背离山体斜坡的安全房间。"
                name.contains("大雨") || name.contains("洪水") -> "请查阅当地防灾疏散指南与避难地图，低洼易涝及山地滑坡风险区居民请及早转移至安全避难所。"
                name.contains("暴風") || name.contains("暴風雪") -> "切勿进行不必要的外出，请留在坚固室内避险并远离迎风面窗户，关好门窗防范狂风暴雪。"
                name.contains("波浪") || name.contains("高潮") -> "严禁靠近海岸堤防、海滨浴场和港口码头。沿海低洼区居民请立即向高地撤离。"
                name.contains("雷") -> "如闻雷声请迅速进入坚固建筑室内躲避，切勿在孤立大树下躲雨或在开阔场地停留。"
                else -> "请密切留意最新官方气象雷达与地方防灾通报，做好日常防灾防风避险准备。"
            }
            AppLanguage.TRADITIONAL_CHINESE -> when {
                name.contains("特別") -> "請立即採取保命避難措施！若室外積水或夜間撤離危險，請緊急避難至堅固建築物二樓以上、遠離山邊的房間。"
                name.contains("大雨") || name.contains("洪水") -> "請查閱防災疏散指南與避難地圖，低窪易淹水及土石流風險區居民請及早撤離至安全處所。"
                name.contains("暴風") || name.contains("暴風雪") -> "切勿進行不必要的外出，請留在室內堅固處所並遠離窗戶，緊閉門窗防範暴風吹襲。"
                name.contains("波浪") || name.contains("高潮") -> "切勿前往海邊、防波堤或港口觀浪。沿海低窪地區居民請迅速往高處撤離。"
                name.contains("雷") -> "聽聞雷聲請儘速進入堅固室內避雷，切勿在樹下避雨或於空曠處逗留。"
                else -> "請密切留意最新官方氣象雷達與防災通報，做好相關安全防範措施。"
            }
            AppLanguage.JAPANESE -> fallback
        }
    }

    /**
     * 警戒ポイントの多言語翻訳取得
     */
    fun getLocalizedCautionPoints(name: String, category: WarningCategory, language: AppLanguage, fallback: List<String>): List<String> {
        return when (language) {
            AppLanguage.ENGLISH -> when {
                name.contains("大雨") -> listOf("Landslides & slope failure", "Road flooding & submerged underpasses", "Rapid overflow of drainage ditches")
                name.contains("洪水") -> listOf("Rapidly rising river levels", "Embankment erosion & collapse", "Inundation of lowlands near rivers")
                name.contains("暴風") -> listOf("Flying debris from extreme winds", "Fallen trees & building damage", "Widespread electrical power outages")
                name.contains("暴風雪") -> listOf("Whiteouts with near-zero visibility", "Vehicles stranded in snow drifts", "Isolation of mountain & rural communities")
                name.contains("大雪") -> listOf("Heavy snow accumulation & roof damage", "Hazardous icy road conditions", "Avalanche on steep snow slopes")
                name.contains("波浪") -> listOf("Rogue waves overtopping sea walls", "Damage to maritime vessels & docks", "Coastal spray & surging waves")
                name.contains("高潮") -> listOf("Extreme sea level rises", "Seawater overtopping sea defenses", "Flooding of underground facilities")
                name.contains("雷") -> listOf("Cloud-to-ground lightning strikes", "Sudden severe microbursts & wind squalls", "Hail damaging crops and property")
                else -> listOf("Rapid weather degradation", "Local government evacuation alerts")
            }
            AppLanguage.SIMPLIFIED_CHINESE -> when {
                name.contains("大雨") -> listOf("山体滑坡与斜坡崩塌", "下穿隧道及道路内涝水浸", "沟渠排水不畅及河水暴涨")
                name.contains("洪水") -> listOf("河流水位急剧暴涨", "堤防决口及漫顶风险", "沿河低洼农田房屋受淹")
                name.contains("暴風") -> listOf("强阵风导致的高空坠物", "树木折断及房屋损毁", "电网故障引发大面积停电")
                name.contains("暴風雪") -> listOf("能见度极低的暴风雪白瞎", "车辆积雪受困与道路阻断", "偏远村落道路积雪受阻孤立")
                name.contains("大雪") -> listOf("暴雪重压导致简易屋顶坍塌", "道路积雪结冰防滑防摔", "山区陡坡雪崩灾害")
                name.contains("波浪") -> listOf("巨浪越过防波堤", "海滨设施及船只受损", "沿海道路浪涌冲刷")
                name.contains("高潮") -> listOf("异常风暴潮位暴涨", "海水漫堤倒灌陆地", "地下空间及低洼商区进水")
                name.contains("雷") -> listOf("雷击危害与人身安全", "局地雷雨大风及突发强降水", "局地冰雹砸伤人车")
                else -> listOf("气象环境剧烈突变", "地方防灾预警动态")
            }
            AppLanguage.TRADITIONAL_CHINESE -> when {
                name.contains("大雨") -> listOf("土石流與山崩邊坡塌方", "地下道及道路積水淹水", "排水溝渠暴漲溢流")
                name.contains("洪水") -> listOf("河川水位急遽暴漲", "堤防受損及溢堤風險", "沿河低窪地帶積水受災")
                name.contains("暴風") -> listOf("強陣風引起之飛落物", "路樹傾倒與建物受損", "電網故障導致大範圍停電")
                name.contains("暴風雪") -> listOf("極度嚴寒與視線不良暴雪", "車輛積雪受困公路中斷", "山區聚落聯外道路中斷")
                name.contains("大雪") -> listOf("劇烈積雪壓垮棚架屋頂", "路面結冰車輛打滑失控", "山坡積雪雪崩危害")
                name.contains("波浪") -> listOf("長浪巨浪越過防波堤", "海岸設施與港內船隻受損", "沿岸越波沖刷危險")
                name.contains("高潮") -> listOf("風暴潮潮位急遽上升", "海水越堤倒灌城鎮", "地下空間與低窪地區積水")
                name.contains("雷") -> listOf("落雷擊傷人身安全風險", "瞬間強風與突發劇烈陣雨", "局部冰雹掉落砸損")
                else -> listOf("天候劇烈惡化變化", "政府最新避難通報動態")
            }
            AppLanguage.JAPANESE -> fallback
        }
    }

    /**
     * 複数警報を「〇〇警報 ほかX件」形式に集約フォーマット（多言語完全対応）
     */
    fun formatAggregatedTitle(
        items: List<MunicipalityWarningItem>,
        language: AppLanguage = AppLanguage.JAPANESE
    ): String {
        if (items.isEmpty()) return ""
        val sorted = sortWarningsByPriority(items)
        val topItem = sorted.first()
        val count = sorted.size
        val topName = getLocalizedWarningName(topItem.name, language)

        if (count == 1) {
            return topName
        }

        val otherCount = count - 1
        val hasWarnings = sorted.any { it.isWarning || it.isSpecial }
        val hasAdvisories = sorted.any { !it.isWarning && !it.isSpecial }

        return when (language) {
            AppLanguage.JAPANESE -> {
                if (hasWarnings && hasAdvisories) {
                    "$topName ほか${otherCount}件の警報・注意報"
                } else if (hasWarnings) {
                    "$topName ほか${otherCount}件の警報"
                } else {
                    "$topName ほか${otherCount}件の注意報"
                }
            }
            AppLanguage.ENGLISH -> {
                "$topName & $otherCount more"
            }
            AppLanguage.SIMPLIFIED_CHINESE -> {
                if (hasWarnings && hasAdvisories) {
                    "$topName 等${count}项预警・注意报"
                } else if (hasWarnings) {
                    "$topName 等${count}项预警"
                } else {
                    "$topName 等${count}项注意报"
                }
            }
            AppLanguage.TRADITIONAL_CHINESE -> {
                if (hasWarnings && hasAdvisories) {
                    "$topName 等${count}項預警・注意報"
                } else if (hasWarnings) {
                    "$topName 等${count}項預警"
                } else {
                    "$topName 等${count}項注意報"
                }
            }
        }
    }
}
