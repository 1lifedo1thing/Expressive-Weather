package com.example.expressiveweather.data

/**
 * 気象庁・内閣府ガイドライン「キキクルの色に応じた住民等の行動の例」公式データ定義
 *
 * 大雨キキクル、土砂キキクル、浸水キキクル、洪水キキクルそれぞれの
 * 危険度・警戒レベルに応じた想定周囲状況、住民行動例、避難情報を4言語で完全提供。
 */
object KikikuruGuidelineContent {

    /**
     * 内閣府ガイドラインで発令の目安とされる避難情報
     */
    fun getEvacuationInfo(level: AlertLevel, language: AppLanguage): String? {
        return when (level) {
            AlertLevel.LEVEL_5 -> when (language) {
                AppLanguage.ENGLISH -> "Emergency Safety Assurance"
                AppLanguage.SIMPLIFIED_CHINESE -> "紧急安全确保 相当"
                AppLanguage.TRADITIONAL_CHINESE -> "緊急安全確保 相當"
                AppLanguage.JAPANESE -> "緊急安全確保 相当"
            }
            AlertLevel.LEVEL_4 -> when (language) {
                AppLanguage.ENGLISH -> "Evacuation Order"
                AppLanguage.SIMPLIFIED_CHINESE -> "避难指示 相当"
                AppLanguage.TRADITIONAL_CHINESE -> "避難指示 相當"
                AppLanguage.JAPANESE -> "避難指示 相当"
            }
            AlertLevel.LEVEL_3 -> when (language) {
                AppLanguage.ENGLISH -> "Evacuate Elderly, etc."
                AppLanguage.SIMPLIFIED_CHINESE -> "老年人等避难 相当"
                AppLanguage.TRADITIONAL_CHINESE -> "老年人等避難 相當"
                AppLanguage.JAPANESE -> "高齢者等避難 相当"
            }
            else -> null
        }
    }

    /**
     * 住民等の行動の例
     */
    fun getActionAdvice(disasterType: DisasterType, level: AlertLevel, language: AppLanguage): String {
        return when (disasterType) {
            // ==========================================
            // 1. 大雨キキクル (Image 1)
            // ==========================================
            DisasterType.HEAVY_RAIN -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "(If outdoor evacuation is too dangerous) Life-threatening danger! Please immediately secure personal safety indoors!"
                    AppLanguage.SIMPLIFIED_CHINESE -> "（若外出避难反而更危险）面临生命危险，请立即确保人身安全！"
                    AppLanguage.TRADITIONAL_CHINESE -> "（若外出避難反而更危險）面臨生命危險，請立即確保人身安全！"
                    AppLanguage.JAPANESE -> "(立退き避難がかえって危険な場合) 命の危険が迫っています。直ちに身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Please check your surroundings and move to an upper floor safe from flooding at your own discretion."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请确认周围情况，根据自身判断转移到室内未被浸水的高楼层避难。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請確認周圍情況，根據自身判斷轉移到室內未被浸水的高樓層避難。"
                    AppLanguage.JAPANESE -> "周囲の状況を確認し、各自の判断で、屋内の浸水が及ばない階に移動して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Please take safety action as soon as preparations are ready. The elderly and vulnerable should take prompt safety action."
                    AppLanguage.SIMPLIFIED_CHINESE -> "安全防护准备就绪后请尽早行动。老年人等弱势群体请迅速采取避难行动。"
                    AppLanguage.TRADITIONAL_CHINESE -> "安全防護準備就緒後請儘早行動。老年人等弱勢群體請迅速採取避難行動。"
                    AppLanguage.JAPANESE -> "安全確保行動をとる準備が整い次第、早めの行動をとって下さい。高齢者等は速やかに安全確保行動をとって下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Please watch weather updates and rainfall closely. Move up from basements and keep away from roadway underpasses."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请密切留意后续天气及雨势。住宅地下室人员请转移至地上，切勿靠近下沉路段隧道。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請密切留意後續天氣及雨勢。住宅地下室人員請轉移至地上，切勿靠近下沉路段隧道。"
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に注意して下さい。各自の判断で、住宅の地下室からは地上に移動し、道路のアンダーパスには近づかないようにして下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Please stay alert to future weather forecasts and local rainfall conditions."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请留意后续气象预报及周边雨情变化。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請留意後續氣象預報及周邊雨情變化。"
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
            }

            // ==========================================
            // 2. 土砂キキクル (Image 2)
            // ==========================================
            DisasterType.LANDSLIDE -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening landslide imminent, or very likely already occurring. Please secure your safety immediately!"
                    AppLanguage.SIMPLIFIED_CHINESE -> "危及生命的滑坡灾害迫在眉睫或已经发生。请立即采取行动确保人身安全！"
                    AppLanguage.TRADITIONAL_CHINESE -> "危及生命的滑坡災害迫在眉睫或已經發生。請立即採取行動確保人身安全！"
                    AppLanguage.JAPANESE -> "命に危険が及ぶ土砂災害が切迫しています。土砂災害がすでに発生している可能性が高い状況です。直ちに身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening landslide could strike at any moment. Please evacuate outside landslide hazard zones immediately."
                    AppLanguage.SIMPLIFIED_CHINESE -> "危及生命的滑坡灾害随时可能发生。请立即撤离至土砂灾害警戒区域之外。"
                    AppLanguage.TRADITIONAL_CHINESE -> "危及生命的滑坡災害隨時可能發生。請立即撤離至土砂災害警戒區域之外。"
                    AppLanguage.JAPANESE -> "命に危険が及ぶ土砂災害がいつ発生してもおかしくない状況です。土砂災害警戒区域等の外へ避難して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Landslide vigilance required. The elderly should evacuate outside hazard zones. Others should pause routine activities, prepare for evacuation, and make self-evacuation decisions."
                    AppLanguage.SIMPLIFIED_CHINESE -> "需对滑坡灾害保持警戒。老年人等请撤离警戒区；其他人请暂停外出、做好避难准备并自主判断避险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "需對滑坡災害保持警戒。老年人等請撤離警戒區；其他人請暫停外出、做好避難準備並自主判斷避險。"
                    AppLanguage.JAPANESE -> "土砂災害への警戒が必要な状況です。高齢者等は土砂災害警戒区域等の外へ避難して下さい。高齢者等以外の方も、普段の行動を見合わせ始めたり、避難の準備をしたり、自ら避難の判断をして下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Landslide caution advised. Please check hazard maps for warning zones and evacuation routes. Monitor rainfall updates closely."
                    AppLanguage.SIMPLIFIED_CHINESE -> "需注意防范滑坡灾害。请通过防灾地图确认滑坡警戒区及避难路线，留意天气与降雨变化。"
                    AppLanguage.TRADITIONAL_CHINESE -> "需注意防範滑坡災害。請透過防災地圖確認滑坡警戒區及避難路線，留意天氣與降雨變化。"
                    AppLanguage.JAPANESE -> "土砂災害への注意が必要な状況です。ハザードマップ等により土砂災害警戒区域等や避難先、避難経路を確認して下さい。今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Please stay alert to future weather forecasts and rainfall on steep slopes."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请留意后续天气及斜坡雨水汇集情况。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請留意後續天氣及斜坡雨水匯集情況。"
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
            }

            // ==========================================
            // 3. 浸水キキクル (Image 3)
            // ==========================================
            DisasterType.INUNDATION -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "(If outdoor evacuation is too dangerous) Life-threatening danger! Please immediately secure personal safety indoors!"
                    AppLanguage.SIMPLIFIED_CHINESE -> "（若外出避难反而更危险）面临生命危险，请立即确保人身安全！"
                    AppLanguage.TRADITIONAL_CHINESE -> "（若外出避難反而更危險）面臨生命危險，請立即確保人身安全！"
                    AppLanguage.JAPANESE -> "(立退き避難がかえって危険な場合) 命の危険が迫っています。直ちに身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Please check your surroundings and move to an upper floor safe from flooding at your own discretion."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请确认周围情况，根据自身判断转移到室内未被浸水的高楼层避险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請確認周圍情況，根據自身判斷轉移到室內未被浸水的高樓層避險。"
                    AppLanguage.JAPANESE -> "周囲の状況を確認し、各自の判断で、屋内の浸水が及ばない階に移動して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Please take safety action as soon as preparations are ready. The elderly should take prompt safety action."
                    AppLanguage.SIMPLIFIED_CHINESE -> "准备就绪后请尽早行动。老年人等弱势群体请迅速采取避险措施。"
                    AppLanguage.TRADITIONAL_CHINESE -> "準備就緒後請儘早行動。老年人等弱勢群體應迅速採取避險措施。"
                    AppLanguage.JAPANESE -> "安全確保行動をとる準備が整い次第、早めの行動をとって下さい。高齢者等は速やかに安全確保行動をとって下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Please watch weather updates and rainfall closely. Move up from basements and avoid roadway underpasses."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请注意周围状况与雨情。住宅地下室人员请转移到地面，切忌靠近下沉隧道。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請注意周圍狀況與雨情。住宅地下室人員請轉移到地面，切忌靠近下沉隧道。"
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に注意して下さい。各自の判断で、住宅の地下室からは地上に移動し、道路のアンダーパスには近づかないようにして下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Please stay alert to upcoming rain and drainage in low-lying areas."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请留意天气与低洼地势积水动向。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請留意天氣與低窪地勢積水動向。"
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
            }

            // ==========================================
            // 4. 洪水キキクル (Image 4)
            // ==========================================
            DisasterType.FLOOD -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening danger! Please immediately secure safety in a sturdy building or higher ground away from floodwaters!"
                    AppLanguage.SIMPLIFIED_CHINESE -> "生命受到重大威胁！请立即确保安全，迅速紧急撤离至坚固高楼或安全高处！"
                    AppLanguage.TRADITIONAL_CHINESE -> "生命受到重大威脅！請立即確保安全，迅速緊急撤離至堅固高樓或安全高處！"
                    AppLanguage.JAPANESE -> "命の危険が迫っています。直ちに身の安全を確保して下さい。浸水が及ばない安全な場所や堅牢な建物の上階へ緊急避難して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Danger of major river overflow (exceeding 50-year return period). Please evacuate flood zones to safe shelters or high ground immediately."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流外水泛滥重大浸水灾害风险极高（超50年一遇标准）。请立即撤离漫堤危险区至安全避难所。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流外水氾濫重大浸水災害風險極高（超50年一遇標準）。請立即撤離漫堤危險區至安全避難所。"
                    AppLanguage.JAPANESE -> "河川氾濫の危険が極めて高まっています（50年確率値基準超過）。氾濫危険区域から直ちに安全な避難所や高台へ全員避難して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Serious flood risk (reaching 30-year return period). The elderly should evacuate dangerous river areas promptly. Others should prepare for evacuation."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流面临泛滥危险（达30年一遇标准）。老年人等请迅速远离河道避难，其他人请做好避难准备。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流面臨氾濫危險（達30年一遇標準）。老年人等請迅速遠離河道避難，其他人請做好避難準備。"
                    AppLanguage.JAPANESE -> "河川の水位が上昇し氾濫のおそれがあります（30年確率値基準到達）。高齢者等は危険な場所から速やかに避難して下さい。一般住民も避難準備を整えて下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "River advisory threshold reached. Please monitor water levels and rain closely. Never approach swollen rivers or drainage canals."
                    AppLanguage.SIMPLIFIED_CHINESE -> "达到河道泛滥注意报标准。请注意河流水位与雨势，绝对不要靠近暴涨的河流或水渠。"
                    AppLanguage.TRADITIONAL_CHINESE -> "達到河道氾濫注意報標準。請注意河流水位與雨勢，絕對不要靠近暴漲的河流或水渠。"
                    AppLanguage.JAPANESE -> "河川の水位や今後の雨の降り方に注意して下さい。増水した河川や用水路には絶対に近づかないようにして下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal river levels. Please stay alert to sudden water level changes from upstream rainfall."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流水位正常。请留意上游降雨引发的水位急涨。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流水位正常。請留意上游降雨引發的水位急漲。"
                    AppLanguage.JAPANESE -> "最新の河川水位情報や気象情報に留意して下さい。"
                }
            }

            // 暴風・強風など
            DisasterType.STORM_WIND -> when (level) {
                AlertLevel.LEVEL_5, AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Violent storm winds expected. Please stay indoors away from windows and watch for flying debris."
                    AppLanguage.SIMPLIFIED_CHINESE -> "预计将有猛烈暴风。请留在坚固室内远离窗户，确保人身安全。"
                    AppLanguage.TRADITIONAL_CHINESE -> "預計將有猛烈暴風。請留在堅固室內遠離窗戶，確保人身安全。"
                    AppLanguage.JAPANESE -> "猛烈な暴風が予想されます。頑丈な屋内にとどまり、窓から離れて身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Gale warning in effect. Please refrain from non-essential outdoor trips and secure loose items outside."
                    AppLanguage.SIMPLIFIED_CHINESE -> "需对强风和暴风保持警戒。请减少不必要的外出，固定好室外易被吹飞的物品。"
                    AppLanguage.TRADITIONAL_CHINESE -> "需對強風和暴風保持警戒。請減少不必要的外出，固定好室外易被吹飛的物品。"
                    AppLanguage.JAPANESE -> "強風・暴風に警戒が必要です。不要不急の外出を控え、飛びやすい屋外の物品を固定して下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Notice strong wind conditions. Please watch for sudden gusts and flying debris."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请注意强风，留意突发阵风及高空坠物。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請注意強風，留意突發陣風及高空墜物。"
                    AppLanguage.JAPANESE -> "強風に注意し、突風や飛来物に留意して下さい。"
                }
            }
        }
    }

    /**
     * 想定される周囲の状況例
     */
    fun getSituationExample(disasterType: DisasterType, level: AlertLevel, language: AppLanguage): String {
        return when (disasterType) {
            // ==========================================
            // 1. 大雨キキクル (Image 1)
            // ==========================================
            DisasterType.HEAVY_RAIN -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Severe inundation or flood disaster is imminent, or extremely likely already occurring."
                    AppLanguage.SIMPLIFIED_CHINESE -> "重大浸水害或洪水灾害迫近，或者极有可能已经发生。"
                    AppLanguage.TRADITIONAL_CHINESE -> "重大浸水害或洪水災害迫近，或者極有可能已經發生。"
                    AppLanguage.JAPANESE -> "重大な浸水害又は洪水害が切迫。又は、すでに発生している可能性が極めて高い状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Major disaster could occur at any moment due to flash flooding or river rise. Low-lying houses risk above-floor flooding."
                    AppLanguage.SIMPLIFIED_CHINESE -> "短时强降雨可能导致浸水或河流暴涨，重大灾害随时可能发生。低洼地区房屋面临漫过地面的积水风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "短時強降雨可能導致浸水或河流暴漲，重大災害隨時可能發生。低窪地區房屋面臨漫過地面的積水風險。"
                    AppLanguage.JAPANESE -> "短時間強雨での浸水または河川増水により重大な災害がいつ発生してもおかしくない。周囲より低い場所にある多くの家屋が、床上まで水に浸かるおそれがある。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Gutters, sewers, or rivers may overflow from short heavy rain, potentially causing major disasters."
                    AppLanguage.SIMPLIFIED_CHINESE -> "短时强降雨可能导致排水沟、下水管道或河流水位溢出，面临重大灾害风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "短時強降雨可能導致排水溝、下水管道或河流水位溢出，面臨重大災害風險。"
                    AppLanguage.JAPANESE -> "短時間強雨により、側溝や下水、または河川があふれ、重大な災害が発生するおそれがある。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Gutters, sewers, or rivers may overflow from short heavy rain, causing localized road flooding."
                    AppLanguage.SIMPLIFIED_CHINESE -> "短时强降雨可能导致排水沟或下水溢水，道路面临淹水风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "短時強降雨可能導致排水溝或下水溢水，道路面臨淹水風險。"
                    AppLanguage.JAPANESE -> "短時間強雨により、側溝や下水、または河川があふれ、冠水するおそれがある。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal conditions. During rain, water accumulates in low-lying areas."
                    AppLanguage.SIMPLIFIED_CHINESE -> "平时状况。降雨时雨水易向低洼处汇聚。"
                    AppLanguage.TRADITIONAL_CHINESE -> "平時狀況。降雨時雨水易向低窪處彙聚。"
                    AppLanguage.JAPANESE -> "普段と同じ状況。雨のときは、雨水が周囲より低い場所に集まる。"
                }
            }

            // ==========================================
            // 2. 土砂キキクル (Image 2)
            // ==========================================
            DisasterType.LANDSLIDE -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening landslide imminent, or very likely already occurring. Critical danger."
                    AppLanguage.SIMPLIFIED_CHINESE -> "危及生命的土砂滑坡灾害迫在眉睫，极有可能已发生滑坡。极度危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "危及生命的土砂滑坡災害迫在眉睫，極有可能已發生滑坡。極度危險。"
                    AppLanguage.JAPANESE -> "命に危険が及ぶ土砂災害が切迫。土砂災害がすでに発生している可能性が高い極めて危険な状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Slope cracks or rising groundwater indicate a landslide could strike at any time."
                    AppLanguage.SIMPLIFIED_CHINESE -> "斜坡开裂或异常涌水，山体滑坡随时可能发生，情况极其危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "斜坡開裂或異常湧水，山體滑坡隨時可能發生，情況極其危險。"
                    AppLanguage.JAPANESE -> "斜面のひび割れや湧水など、命に危険が及ぶ土砂災害がいつ発生してもおかしくない極めて危険な状況。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Prolonged or heavy rain has loosened ground. Heightened vigilance for landslides required."
                    AppLanguage.SIMPLIFIED_CHINESE -> "持续强降雨导致土壤松动，需高度警惕滑坡灾害。"
                    AppLanguage.TRADITIONAL_CHINESE -> "持續強降雨導致土壤鬆動，需高度警惕滑坡災害。"
                    AppLanguage.JAPANESE -> "長雨や強雨により地盤が緩み、土砂災害への警戒が必要な状況。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Caution required for landslides due to rainfall. Hazard map verification recommended."
                    AppLanguage.SIMPLIFIED_CHINESE -> "因降雨需对滑坡保持警惕的状态，建议查阅防灾地图。"
                    AppLanguage.TRADITIONAL_CHINESE -> "因降雨需對滑坡保持警惕的狀態，建議查閱防災地圖。"
                    AppLanguage.JAPANESE -> "降雨に伴い土砂災害への注意が必要な状況。ハザードマップ等の確認が必要。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal conditions. Confirm steep slope hazard maps."
                    AppLanguage.SIMPLIFIED_CHINESE -> "通常状况。确认陡坡等隐患区域地图。"
                    AppLanguage.TRADITIONAL_CHINESE -> "通常狀況。確認陡坡等隱患區域地圖。"
                    AppLanguage.JAPANESE -> "普段と同じ状況。急傾斜地等のハザードマップを確認。"
                }
            }

            // ==========================================
            // 3. 浸水キキクル (Image 3)
            // ==========================================
            DisasterType.INUNDATION -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Major inundation imminent. Extremely high probability inundation is already taking place."
                    AppLanguage.SIMPLIFIED_CHINESE -> "重大浸水灾害迫近，浸水极其可能已经发生。"
                    AppLanguage.TRADITIONAL_CHINESE -> "重大浸水災害迫近，浸水極其可能已經發生。"
                    AppLanguage.JAPANESE -> "重大な浸水害が切迫。浸水害がすでに発生している可能性が極めて高い状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Roads fully submerged with obscured gutters and manholes. Public transit disrupted. Low-lying homes risk above-floor flooding."
                    AppLanguage.SIMPLIFIED_CHINESE -> "道路大面积积水淹没排水口。公共交通受阻。低洼地带房屋面临漫过地面的严重浸水危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "道路大面積積水淹沒排水口。公共交通受阻。低窪地帶房屋面臨漫過地面的嚴重浸水危險。"
                    AppLanguage.JAPANESE -> "道路が一面冠水し、側溝やマンホールの場所が分からなくなるおそれがある。道路冠水等のために鉄道やバスなどの交通機関の運行に影響が出るおそれがある。周囲より低い場所にある多くの家屋が、床上まで水に浸かるおそれがある。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Gutters and drains overflowing; street flooding imminent. Low-lying homes at risk of above-floor water."
                    AppLanguage.SIMPLIFIED_CHINESE -> "排水沟与下水漫溢，道路随时可能积水，低洼住宅有进水风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "排水溝與下水漫溢，道路隨時可能積水，低窪住宅有進水風險。"
                    AppLanguage.JAPANESE -> "側溝や下水が溢れ、道路がいつ冠水してもおかしくない。周囲より低い場所にある家屋が、床上まで水に浸かるおそれがある。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Low-lying areas risk gutter overflow and road puddles. Water may enter basements or underpasses. Homes risk below-floor flooding."
                    AppLanguage.SIMPLIFIED_CHINESE -> "低洼处沟渠可能漫溢道路积水，地下室或立交下沉路段有倒灌进水风险，房屋有地面以下进水危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "低窪處溝渠可能漫溢道路積水，地下室或立交下沉路段有倒灌進水風險，房屋有地面以下進水危險。"
                    AppLanguage.JAPANESE -> "周囲より低い場所で側溝や下水が溢れ、道路が冠水するおそれがある。住宅の地下室や道路のアンダーパスに水が流れ込むおそれがある。周囲より低い場所にある家屋が、床下まで水に浸かるおそれがある。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal conditions. Rainwater collects in lower areas."
                    AppLanguage.SIMPLIFIED_CHINESE -> "日常状态。降雨时雨水聚集于低洼处。"
                    AppLanguage.TRADITIONAL_CHINESE -> "日常狀態。降雨時雨水聚集於低窪處。"
                    AppLanguage.JAPANESE -> "普段と同じ状況。雨のときは、雨水が周囲より低い場所に集まる。"
                }
            }

            // ==========================================
            // 4. 洪水キキクル (Image 4)
            // ==========================================
            DisasterType.FLOOD -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Major flood (river breach/overflow) imminent or already occurring. Extremely critical danger."
                    AppLanguage.SIMPLIFIED_CHINESE -> "重大河川外水泛滥迫在眉睫，或决堤溃坝已经发生。极端危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "重大河川外水氾濫迫在眉睫，或決堤潰壩已經發生。極端危險。"
                    AppLanguage.JAPANESE -> "重大な洪水災害（外水氾濫）が切迫、またはすでに堤防決壊・越水が発生している極めて危険な状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Extremely high risk of major flooding from river overflow (exceeding 50-year return period). Flood danger level reached."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河川外水泛滥引发重大浸水灾害风险极高（超50年一遇标准）。极可能已达泛滥危险水位。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河川外水氾濫引發重大浸水災害風險極高（超50年一遇標準）。極可能已達氾濫危險水位。"
                    AppLanguage.JAPANESE -> "河川流域で発生した外水氾濫に起因する重大な浸水害が極めて発生しやすい状況（50年確率値基準超過）。氾濫危険水位に達しているおそれ。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Risk of serious flooding from river/local drainage (reaching 30-year return period). Water level exceeds evacuation trigger."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流水位上涨可能引发外水/内水泛滥（达30年一遇标准）。已超避难判断水位。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流水位上漲可能引發外水/內水氾濫（達30年一遇標準）。已超避難判斷水位。"
                    AppLanguage.JAPANESE -> "河川流域で外水氾濫・内水氾濫に起因する重大な浸水害が発生するおそれ（30年確率値基準到達）。避難判断水位を超過。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "River advisory threshold reached (70-80% of warning threshold). Rising water level caution required."
                    AppLanguage.SIMPLIFIED_CHINESE -> "达河道泛滥注意报标准（达警报标准70~80%），需注意河水上涨。"
                    AppLanguage.TRADITIONAL_CHINESE -> "達河道氾濫注意報標準（達警報標準70~80%），需注意河水上漲。"
                    AppLanguage.JAPANESE -> "河川流域で外水氾濫・内水氾濫に起因する浸水害を見逃さないための注意報基準（警報基準の7〜8割）に到達。河川増水のおそれ。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal river levels. Be alert to upstream rain."
                    AppLanguage.SIMPLIFIED_CHINESE -> "正常河流水位。留意上游汇水情况。"
                    AppLanguage.TRADITIONAL_CHINESE -> "正常河流水位。留意上游匯水情況。"
                    AppLanguage.JAPANESE -> "普段通りの河川水位。上流からの増水等に留意。"
                }
            }

            DisasterType.STORM_WIND -> when (level) {
                AlertLevel.LEVEL_5, AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Violent storm damage occurring. Roof tiles, trees, and power lines may blow down."
                    AppLanguage.SIMPLIFIED_CHINESE -> "严重暴风灾害发生，飞瓦断树、倒杆断线风险极高。"
                    AppLanguage.TRADITIONAL_CHINESE -> "嚴重暴風災害發生，飛瓦斷樹、倒桿斷線風險極高。"
                    AppLanguage.JAPANESE -> "猛烈な暴風により、屋根瓦の飛散や樹木の倒壊、電線切断などの重大被害のおそれ。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Strong gale winds making walking difficult; small branches breaking."
                    AppLanguage.SIMPLIFIED_CHINESE -> "强风使行走困难，树枝折断，广告牌等有坠落风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "強風使行走困難，樹枝折斷，廣告牌等有墜落風險。"
                    AppLanguage.JAPANESE -> "風に向かって歩けなくなるほどの強風。看板やトタンの飛散に注意。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Moderate winds. Umbrella may turn inside out."
                    AppLanguage.SIMPLIFIED_CHINESE -> "风力增强，伞具易被吹翻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "風力增強，傘具易被吹翻。"
                    AppLanguage.JAPANESE -> "風が強まり、傘がさしにくくなるおそれがあります。"
                }
            }
        }
    }

    /**
     * アクションボタン（レーダーマップ遷移）のラベル文言
     */
    fun getButtonLabel(disasterType: DisasterType, language: AppLanguage): String {
        return when (disasterType) {
            DisasterType.HEAVY_RAIN -> when (language) {
                AppLanguage.ENGLISH -> "Check Heavy Rain Radar"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看大雨危险度雷达"
                AppLanguage.TRADITIONAL_CHINESE -> "查看大雨危險度雷達"
                AppLanguage.JAPANESE -> "雨雲・大雨キキクルを確認"
            }
            DisasterType.LANDSLIDE -> when (language) {
                AppLanguage.ENGLISH -> "Check Landslide Map"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看土砂灾害分布图"
                AppLanguage.TRADITIONAL_CHINESE -> "查看土砂災害分佈圖"
                AppLanguage.JAPANESE -> "土砂キキクルマップを確認"
            }
            DisasterType.INUNDATION -> when (language) {
                AppLanguage.ENGLISH -> "Check Inundation Map"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看浸水害分布图"
                AppLanguage.TRADITIONAL_CHINESE -> "查看浸水害分佈圖"
                AppLanguage.JAPANESE -> "浸水キキクルマップを確認"
            }
            DisasterType.FLOOD -> when (language) {
                AppLanguage.ENGLISH -> "Check Flood Map"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看洪水灾害分布图"
                AppLanguage.TRADITIONAL_CHINESE -> "查看洪水災害分佈圖"
                AppLanguage.JAPANESE -> "洪水キキクルマップを確認"
            }
            DisasterType.STORM_WIND -> when (language) {
                AppLanguage.ENGLISH -> "Check Wind Radar"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看大风雷达"
                AppLanguage.TRADITIONAL_CHINESE -> "查看大風雷達"
                AppLanguage.JAPANESE -> "強風・レーダーマップを確認"
            }
        }
    }
}
