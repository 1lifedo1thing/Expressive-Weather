package io.github.nemynew.expressiveweather.data

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
                AppLanguage.KOREAN -> "긴급 안전 확보 상당"
                AppLanguage.SIMPLIFIED_CHINESE -> "紧急安全确保 相当"
                AppLanguage.TRADITIONAL_CHINESE -> "緊急安全確保 相當"
                AppLanguage.SPANISH -> "Aseguramiento de seguridad de emergencia"
                AppLanguage.FRENCH -> "Mise en sécurité d'urgence"
                AppLanguage.PORTUGUESE -> "Segurança de emergência imediata"
                AppLanguage.JAPANESE -> "緊急安全確保 相当"
            }
            AlertLevel.LEVEL_4 -> when (language) {
                AppLanguage.ENGLISH -> "Evacuation Order"
                AppLanguage.KOREAN -> "대피 지시 상당"
                AppLanguage.SIMPLIFIED_CHINESE -> "避难指示 相当"
                AppLanguage.TRADITIONAL_CHINESE -> "避難指示 相當"
                AppLanguage.SPANISH -> "Orden de evacuación"
                AppLanguage.FRENCH -> "Ordre d'évacuation"
                AppLanguage.PORTUGUESE -> "Ordem de evacuação"
                AppLanguage.JAPANESE -> "避難指示 相当"
            }
            AlertLevel.LEVEL_3 -> when (language) {
                AppLanguage.ENGLISH -> "Evacuate Elderly, etc."
                AppLanguage.KOREAN -> "고령자 등 대피 상당"
                AppLanguage.SIMPLIFIED_CHINESE -> "老年人等避难 相当"
                AppLanguage.TRADITIONAL_CHINESE -> "老年人等避難 相當"
                AppLanguage.SPANISH -> "Evacuación de personas mayores y vulnerables"
                AppLanguage.FRENCH -> "Évacuation des personnes âgées et vulnérables"
                AppLanguage.PORTUGUESE -> "Evacuação de idosos e vulneráveis"
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
                    AppLanguage.KOREAN -> "(외부 대피가 오히려 위험한 경우) 생명이 위협받는 상황입니다. 즉시 실내 안전한 곳에서 신변 안전을 확보하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "（若外出避难反而更危险）面临生命危险，请立即确保人身安全！"
                    AppLanguage.TRADITIONAL_CHINESE -> "（若外出避難反而更危險）面臨生命危險，請立即確保人身安全！"
                    AppLanguage.SPANISH -> "(Si evacuar al exterior es demasiado peligroso) ¡Peligro mortal! Póngase a salvo de inmediato en un lugar seguro bajo techo."
                    AppLanguage.FRENCH -> "(Si l'évacuation extérieure est trop dangereuse) Danger mortel ! Mettez-vous immédiatement en sécurité à l'intérieur."
                    AppLanguage.PORTUGUESE -> "(Se evacuar ao ar livre for muito perigoso) Risco de vida! Garanta sua segurança imediatamente em local abrigado."
                    AppLanguage.JAPANESE -> "(立退き避難がかえって危険な場合) 命の危険が迫っています。直ちに身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Please check your surroundings and move to an upper floor safe from flooding at your own discretion."
                    AppLanguage.KOREAN -> "주변 상황을 확인하고 자체 판단에 따라 실내의 침수 우려가 없는 위층으로 이동하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请确认周围情况，根据自身判断转移到室内未被浸水的高楼层避难。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請確認周圍情況，根據自身判斷轉移到室內未被浸水的高樓層避難。"
                    AppLanguage.SPANISH -> "Compruebe su entorno y trasládese a un piso superior a salvo de inundaciones según su propio criterio."
                    AppLanguage.FRENCH -> "Vérifiez votre environnement et montez à un étage supérieur à l'abri des inondations selon votre jugement."
                    AppLanguage.PORTUGUESE -> "Verifique o entorno e desloque-se para um andar superior seguro contra alagamentos, conforme sua avaliação."
                    AppLanguage.JAPANESE -> "周囲の状況を確認し、各自の判断で、屋内の浸水が及ばない階に移動して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Please take safety action as soon as preparations are ready. The elderly and vulnerable should take prompt safety action."
                    AppLanguage.KOREAN -> "안전 확보 준비가 되는 대로 서둘러 행동하세요. 고령자 및 거동이 불편한 분은 신속히 안전을 확보하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "安全防护准备就绪后请尽早行动。老年人等弱势群体请迅速采取避难行动。"
                    AppLanguage.TRADITIONAL_CHINESE -> "安全防護準備就緒後請儘早行動。老年人等弱勢群體請迅速採取避難行動。"
                    AppLanguage.SPANISH -> "Tome medidas de seguridad tan pronto como esté preparado. Las personas mayores y vulnerables deben actuar de inmediato."
                    AppLanguage.FRENCH -> "Prenez des mesures de sécurité dès que vous êtes prêt. Les personnes âgées et vulnérables doivent agir sans délai."
                    AppLanguage.PORTUGUESE -> "Tome medidas de segurança assim que estiver preparado. Idosos e pessoas vulneráveis devem agir imediatamente."
                    AppLanguage.JAPANESE -> "安全確保行動をとる準備が整い次第、早めの行動をとって下さい。高齢者等は速やかに安全確保行動をとって下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Please watch weather updates and rainfall closely. Move up from basements and keep away from roadway underpasses."
                    AppLanguage.KOREAN -> "향후 기상 정보와 주변 강우 상황에 주의하세요. 반지하·지하실에서는 지상으로 이동하고 지하차도에는 접근하지 마세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请密切留意后续天气及雨势。住宅地下室人员请转移至地上，切勿靠近下沉路段隧道。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請密切留意後續天氣及雨勢。住宅地下室人員請轉移至地上，切勿靠近下沉路段隧道。"
                    AppLanguage.SPANISH -> "Siga de cerca los avisos meteorológicos y la lluvia. Salga de los sótanos y aléjese de pasos subterráneos."
                    AppLanguage.FRENCH -> "Suivez attentivement la météo et les précipitations. Quittez les sous-sols et évitez les passages souterrains."
                    AppLanguage.PORTUGUESE -> "Acompanhe atentamente os boletins e a chuva. Saia de porões e mantenha-se longe de passagens subterrâneas."
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に注意して下さい。各自の判断で、住宅の地下室からは地上に移動し、道路のアンダーパスには近づかないようにして下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Please stay alert to future weather forecasts and local rainfall conditions."
                    AppLanguage.KOREAN -> "향후 기상 예보와 주변 강우 상황에 유의하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请留意后续气象预报及周边雨情变化。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請留意後續氣象預報及周邊雨情變化。"
                    AppLanguage.SPANISH -> "Manténgase atento a las próximas previsiones meteorológicas y a las lluvias locales."
                    AppLanguage.FRENCH -> "Restez attentif aux prochaines prévisions météo et à l'évolution des pluies locales."
                    AppLanguage.PORTUGUESE -> "Fique atento às próximas previsões do tempo e às condições de chuva locais."
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
            }

            // ==========================================
            // 2. 土砂キキクル (Image 2)
            // ==========================================
            DisasterType.LANDSLIDE -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening landslide imminent, or very likely already occurring. Please secure your safety immediately!"
                    AppLanguage.KOREAN -> "생명을 위협하는 산사태가 임박했거나 이미 발생했을 가능성이 매우 높습니다. 즉시 신변 안전을 확보하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "危及生命的滑坡灾害迫在眉睫或已经发生。请立即采取行动确保人身安全！"
                    AppLanguage.TRADITIONAL_CHINESE -> "危及生命的滑坡災害迫在眉睫或已經發生。請立即採取行動確保人身安全！"
                    AppLanguage.SPANISH -> "Deslizamiento mortal inminente o muy probablemente ya en curso. ¡Póngase a salvo de inmediato!"
                    AppLanguage.FRENCH -> "Glissement de terrain mortel imminent ou très probablement déjà en cours. Mettez-vous immédiatement en sécurité !"
                    AppLanguage.PORTUGUESE -> "Deslizamento com risco de vida iminente ou provavelmente já em curso. Garanta sua segurança imediatamente!"
                    AppLanguage.JAPANESE -> "命に危険が及ぶ土砂災害が切迫しています。土砂災害がすでに発生している可能性が高い状況です。直ちに身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening landslide could strike at any moment. Please evacuate outside landslide hazard zones immediately."
                    AppLanguage.KOREAN -> "생명을 위협하는 산사태가 언제 발생해도 이상하지 않은 상황입니다. 산사태 위험 구역 밖으로 즉시 대피하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "危及生命的滑坡灾害随时可能发生。请立即撤离至土砂灾害警戒区域之外。"
                    AppLanguage.TRADITIONAL_CHINESE -> "危及生命的滑坡災害隨時可能發生。請立即撤離至土砂災害警戒區域之外。"
                    AppLanguage.SPANISH -> "Un deslizamiento mortal podría ocurrir en cualquier momento. Evacúe de inmediato fuera de las zonas de riesgo."
                    AppLanguage.FRENCH -> "Un glissement de terrain mortel peut survenir à tout moment. Évacuez immédiatement hors des zones à risque."
                    AppLanguage.PORTUGUESE -> "Um deslizamento com risco de vida pode ocorrer a qualquer momento. Evacue imediatamente para fora das áreas de risco."
                    AppLanguage.JAPANESE -> "命に危険が及ぶ土砂災害がいつ発生してもおかしくない状況です。土砂災害警戒区域等の外へ避難して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Landslide vigilance required. The elderly should evacuate outside hazard zones. Others should pause routine activities, prepare for evacuation, and make self-evacuation decisions."
                    AppLanguage.KOREAN -> "산사태 경계가 필요한 상황입니다. 고령자 등은 산사태 위험 구역 밖으로 대피하고, 그 외 주민도 외출을 자제하며 대피를 준비하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "需对滑坡灾害保持警戒。老年人等请撤离警戒区；其他人请暂停外出、做好避难准备并自主判断避险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "需對滑坡災害保持警戒。老年人等請撤離警戒區；其他人請暫停外出、做好避難準備並自主判斷避險。"
                    AppLanguage.SPANISH -> "Se requiere vigilancia por deslizamientos. Las personas mayores deben evacuar las zonas de riesgo; los demás deben suspender actividades habituales y prepararse para evacuar."
                    AppLanguage.FRENCH -> "Vigilance requise face aux glissements de terrain. Les personnes âgées doivent évacuer les zones à risque ; les autres doivent suspendre leurs activités et se préparer à évacuer."
                    AppLanguage.PORTUGUESE -> "Atenção redobrada para deslizamentos. Idosos devem evacuar as áreas de risco; demais moradores devem suspender atividades rotineiras e preparar a evacuação."
                    AppLanguage.JAPANESE -> "土砂災害への警戒が必要な状況です。高齢者等は土砂災害警戒区域等の外へ避難して下さい。高齢者等以外の方も、普段の行動を見合わせ始めたり、避難の準備をしたり、自ら避難の判断をして下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Landslide caution advised. Please check hazard maps for warning zones and evacuation routes. Monitor rainfall updates closely."
                    AppLanguage.KOREAN -> "산사태 주의가 필요한 상황입니다. 재해 지도로 위험 구역과 대피 경로를 확인하고 향후 기상 정보와 강우 상황을 예의주시하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "需注意防范滑坡灾害。请通过防灾地图确认滑坡警戒区及避难路线，留意天气与降雨变化。"
                    AppLanguage.TRADITIONAL_CHINESE -> "需注意防範滑坡災害。請透過防災地圖確認滑坡警戒區及避難路線，留意天氣與降雨變化。"
                    AppLanguage.SPANISH -> "Se recomienda precaución por deslizamientos. Consulte los mapas de riesgo y rutas de evacuación, y siga de cerca la evolución de la lluvia."
                    AppLanguage.FRENCH -> "Prudence recommandée face aux glissements de terrain. Vérifiez les cartes de risques et les itinéraires d'évacuation, et suivez l'évolution des pluies."
                    AppLanguage.PORTUGUESE -> "Recomenda-se cautela com deslizamentos. Consulte os mapas de risco e rotas de evacuação e acompanhe de perto a chuva."
                    AppLanguage.JAPANESE -> "土砂災害への注意が必要な状況です。ハザードマップ等により土砂災害警戒区域等や避難先、避難経路を確認して下さい。今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Please stay alert to future weather forecasts and rainfall on steep slopes."
                    AppLanguage.KOREAN -> "향후 기상 예보와 급경사지 강우 상황에 유의하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请留意后续天气及斜坡雨水汇集情况。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請留意後續天氣及斜坡雨水匯集情況。"
                    AppLanguage.SPANISH -> "Manténgase atento a las previsiones meteorológicas y a la lluvia en laderas empinadas."
                    AppLanguage.FRENCH -> "Restez attentif aux prévisions météo et aux pluies sur les pentes raides."
                    AppLanguage.PORTUGUESE -> "Fique atento às previsões do tempo e às chuvas em encostas íngremes."
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
            }

            // ==========================================
            // 3. 浸水キキクル (Image 3)
            // ==========================================
            DisasterType.INUNDATION -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "(If outdoor evacuation is too dangerous) Life-threatening danger! Please immediately secure personal safety indoors!"
                    AppLanguage.KOREAN -> "(외부 대피가 오히려 위험한 경우) 생명이 위협받는 상황입니다. 즉시 실내 안전한 곳에서 신변 안전을 확보하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "（若外出避难反而更危险）面临生命危险，请立即确保人身安全！"
                    AppLanguage.TRADITIONAL_CHINESE -> "（若外出避難反而更危險）面臨生命危險，請立即確保人身安全！"
                    AppLanguage.SPANISH -> "(Si evacuar al exterior es demasiado peligroso) ¡Peligro mortal! Póngase a salvo de inmediato en un lugar seguro bajo techo."
                    AppLanguage.FRENCH -> "(Si l'évacuation extérieure est trop dangereuse) Danger mortel ! Mettez-vous immédiatement en sécurité à l'intérieur."
                    AppLanguage.PORTUGUESE -> "(Se evacuar ao ar livre for muito perigoso) Risco de vida! Garanta sua segurança imediatamente em local abrigado."
                    AppLanguage.JAPANESE -> "(立退き避難がかえって危険な場合) 命の危険が迫っています。直ちに身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Please check your surroundings and move to an upper floor safe from flooding at your own discretion."
                    AppLanguage.KOREAN -> "주변 상황을 확인하고 자체 판단에 따라 실내의 침수 우려가 없는 위층으로 이동하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请确认周围情况，根据自身判断转移到室内未被浸水的高楼层避险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請確認周圍情況，根據自身判斷轉移到室內未被浸水的高樓層避險。"
                    AppLanguage.SPANISH -> "Compruebe su entorno y trasládese a un piso superior a salvo de inundaciones según su propio criterio."
                    AppLanguage.FRENCH -> "Vérifiez votre environnement et montez à un étage supérieur à l'abri des inondations selon votre jugement."
                    AppLanguage.PORTUGUESE -> "Verifique o entorno e desloque-se para um andar superior seguro contra alagamentos, conforme sua avaliação."
                    AppLanguage.JAPANESE -> "周囲の状況を確認し、各自の判断で、屋内の浸水が及ばない階に移動して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Please take safety action as soon as preparations are ready. The elderly should take prompt safety action."
                    AppLanguage.KOREAN -> "안전 확보 준비가 되는 대로 서둘러 행동하세요. 고령자 등은 신속히 안전 확보 조치를 취하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "准备就绪后请尽早行动。老年人等弱势群体请迅速采取避险措施。"
                    AppLanguage.TRADITIONAL_CHINESE -> "準備就緒後請儘早行動。老年人等弱勢群體應迅速採取避險措施。"
                    AppLanguage.SPANISH -> "Tome medidas de seguridad tan pronto como esté preparado. Las personas mayores deben actuar con prontitud."
                    AppLanguage.FRENCH -> "Prenez des mesures de sécurité dès que vous êtes prêt. Les personnes âgées doivent agir rapidement."
                    AppLanguage.PORTUGUESE -> "Tome medidas de segurança assim que estiver preparado. Os idosos devem agir rapidamente."
                    AppLanguage.JAPANESE -> "安全確保行動をとる準備が整い次第、早めの行動をとって下さい。高齢者等は速やかに安全確保行動をとって下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Please watch weather updates and rainfall closely. Move up from basements and avoid roadway underpasses."
                    AppLanguage.KOREAN -> "향후 기상 정보와 주변 강우 상황에 주의하세요. 지하실에서는 지상으로 이동하고 지하차도에는 접근하지 마세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请注意周围状况与雨情。住宅地下室人员请转移到地面，切忌靠近下沉隧道。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請注意周圍狀況與雨情。住宅地下室人員請轉移到地面，切忌靠近下沉隧道。"
                    AppLanguage.SPANISH -> "Siga de cerca los avisos meteorológicos y la lluvia. Salga de los sótanos y evite pasos subterráneos."
                    AppLanguage.FRENCH -> "Suivez attentivement la météo et les précipitations. Quittez les sous-sols et évitez les passages souterrains."
                    AppLanguage.PORTUGUESE -> "Acompanhe atentamente os boletins e a chuva. Saia de porões e evite passagens subterrâneas."
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に注意して下さい。各自の判断で、住宅の地下室からは地上に移動し、道路のアンダーパスには近づかないようにして下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Please stay alert to upcoming rain and drainage in low-lying areas."
                    AppLanguage.KOREAN -> "향후 강우 상황과 저지대 배수 상태에 유의하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请留意天气与低洼地势积水动向。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請留意天氣與低窪地勢積水動向。"
                    AppLanguage.SPANISH -> "Manténgase atento a las próximas lluvias y al drenaje en zonas bajas."
                    AppLanguage.FRENCH -> "Restez attentif aux prochaines pluies et à l'évacuation des eaux dans les zones basses."
                    AppLanguage.PORTUGUESE -> "Fique atento às próximas chuvas e à drenagem em áreas baixas."
                    AppLanguage.JAPANESE -> "今後の情報や周囲の状況、雨の降り方に留意して下さい。"
                }
            }

            // ==========================================
            // 4. 洪水キキクル (Image 4)
            // ==========================================
            DisasterType.FLOOD -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening danger! Please immediately secure safety in a sturdy building or higher ground away from floodwaters!"
                    AppLanguage.KOREAN -> "생명이 위협받는 상황입니다. 즉시 침수가 미치지 않는 고지대나 튼튼한 건물 상층부로 긴급 대피하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "生命受到重大威胁！请立即确保安全，迅速紧急撤离至坚固高楼或安全高处！"
                    AppLanguage.TRADITIONAL_CHINESE -> "生命受到重大威脅！請立即確保安全，迅速緊急撤離至堅固高樓或安全高處！"
                    AppLanguage.SPANISH -> "¡Peligro mortal! Póngase a salvo de inmediato en un edificio sólido o en terreno elevado lejos de las aguas."
                    AppLanguage.FRENCH -> "Danger mortel ! Mettez-vous immédiatement à l'abri dans un bâtiment solide ou en hauteur, loin des eaux."
                    AppLanguage.PORTUGUESE -> "Risco de vida! Abrigue-se imediatamente em um prédio resistente ou em terreno elevado longe das águas."
                    AppLanguage.JAPANESE -> "命の危険が迫っています。直ちに身の安全を確保して下さい。浸水が及ばない安全な場所や堅牢な建物の上階へ緊急避難して下さい。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Danger of major river overflow (exceeding 50-year return period). Please evacuate flood zones to safe shelters or high ground immediately."
                    AppLanguage.KOREAN -> "하천 범람 위험이 매우 높습니다(50년 빈도 기준 초과). 범람 위험 구역에서 안전한 대피소나 고지대로 즉시 전원 대피하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流外水泛滥重大浸水灾害风险极高（超50年一遇标准）。请立即撤离漫堤危险区至安全避难所。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流外水氾濫重大浸水災害風險極高（超50年一遇標準）。請立即撤離漫堤危險區至安全避難所。"
                    AppLanguage.SPANISH -> "Peligro de desbordamiento grave del río (supera el período de retorno de 50 años). Evacúe de inmediato las zonas inundables hacia refugios seguros o terrenos altos."
                    AppLanguage.FRENCH -> "Danger de débordement majeur du cours d'eau (seuil cinquantennal dépassé). Évacuez immédiatement les zones inondables vers des abris sûrs ou en hauteur."
                    AppLanguage.PORTUGUESE -> "Perigo de transbordamento grave de rio (acima do período de retorno de 50 anos). Evacue imediatamente as áreas de risco para abrigos seguros ou locais elevados."
                    AppLanguage.JAPANESE -> "河川氾濫の危険が極めて高まっています（50年確率値基準超過）。氾濫危険区域から直ちに安全な避難所や高台へ全員避難して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Serious flood risk (reaching 30-year return period). The elderly should evacuate dangerous river areas promptly. Others should prepare for evacuation."
                    AppLanguage.KOREAN -> "하천 수위가 상승하여 범람 우려가 있습니다(30년 빈도 기준 도달). 고령자 등은 위험 지역에서 신속히 대피하고, 일반 주민도 대피를 준비하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流面临泛滥危险（达30年一遇标准）。老年人等请迅速远离河道避难，其他人请做好避难准备。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流面臨氾濫危險（達30年一遇標準）。老年人等請迅速遠離河道避難，其他人請做好避難準備。"
                    AppLanguage.SPANISH -> "Riesgo grave de inundación (alcanza el período de retorno de 30 años). Las personas mayores deben evacuar las zonas ribereñas peligrosas; los demás deben prepararse para evacuar."
                    AppLanguage.FRENCH -> "Risque sérieux de crue (seuil trentennal atteint). Les personnes âgées doivent évacuer rapidement les zones riveraines dangereuses ; les autres doivent se préparer à évacuer."
                    AppLanguage.PORTUGUESE -> "Risco sério de enchente (atinge o período de retorno de 30 anos). Idosos devem evacuar rapidamente áreas ribeirinhas perigosas; os demais devem preparar a evacuação."
                    AppLanguage.JAPANESE -> "河川の水位が上昇し氾濫のおそれがあります（30年確率値基準到達）。高齢者等は危険な場所から速やかに避難して下さい。一般住民も避難準備を整えて下さい。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "River advisory threshold reached. Please monitor water levels and rain closely. Never approach swollen rivers or drainage canals."
                    AppLanguage.KOREAN -> "하천 수위와 향후 강우 상황에 주의하세요. 수위가 불어난 하천이나 배수로에는 절대 접근하지 마세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "达到河道泛滥注意报标准。请注意河流水位与雨势，绝对不要靠近暴涨的河流或水渠。"
                    AppLanguage.TRADITIONAL_CHINESE -> "達到河道氾濫注意報標準。請注意河流水位與雨勢，絕對不要靠近暴漲的河流或水渠。"
                    AppLanguage.SPANISH -> "Se alcanzó el umbral de aviso fluvial. Vigile de cerca el nivel del agua y la lluvia. Nunca se acerque a ríos crecidos ni canales de drenaje."
                    AppLanguage.FRENCH -> "Seuil de vigilance crue atteint. Surveillez attentivement le niveau de l'eau et les pluies. Ne vous approchez jamais des cours d'eau en crue ou des canaux."
                    AppLanguage.PORTUGUESE -> "Limite de aviso fluvial atingido. Monitore de perto o nível da água e a chuva. Nunca se aproxime de rios cheios ou canais de drenagem."
                    AppLanguage.JAPANESE -> "河川の水位や今後の雨の降り方に注意して下さい。増水した河川や用水路には絶対に近づかないようにして下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal river levels. Please stay alert to sudden water level changes from upstream rainfall."
                    AppLanguage.KOREAN -> "평상시 하천 수위입니다. 상류 강우에 따른 급격한 수위 변화에 유의하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流水位正常。请留意上游降雨引发的水位急涨。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流水位正常。請留意上游降雨引發的水位急漲。"
                    AppLanguage.SPANISH -> "Niveles fluviales normales. Manténgase atento a cambios repentinos del caudal por lluvias río arriba."
                    AppLanguage.FRENCH -> "Niveaux fluviaux normaux. Restez attentif aux montées soudaines liées aux pluies en amont."
                    AppLanguage.PORTUGUESE -> "Níveis fluviais normais. Fique atento a elevações repentinas causadas por chuvas a montante."
                    AppLanguage.JAPANESE -> "最新の河川水位情報や気象情報に留意して下さい。"
                }
            }

            // 暴風・強風など
            DisasterType.STORM_WIND -> when (level) {
                AlertLevel.LEVEL_5, AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Violent storm winds expected. Please stay indoors away from windows and watch for flying debris."
                    AppLanguage.KOREAN -> "매우 강한 폭풍이 예상됩니다. 튼튼한 실내에 머물며 창문에서 떨어져 신변 안전을 확보하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "预计将有猛烈暴风。请留在坚固室内远离窗户，确保人身安全。"
                    AppLanguage.TRADITIONAL_CHINESE -> "預計將有猛烈暴風。請留在堅固室內遠離窗戶，確保人身安全。"
                    AppLanguage.SPANISH -> "Se esperan vientos huracanados. Permanezca bajo techo, alejado de las ventanas y atento a objetos arrastrados por el viento."
                    AppLanguage.FRENCH -> "Vents de tempête violents attendus. Restez à l'intérieur, loin des fenêtres, et méfiez-vous des projectiles."
                    AppLanguage.PORTUGUESE -> "Ventos violentos de tempestade previstos. Permaneça em local fechado, longe de janelas, e atento a objetos arremessados."
                    AppLanguage.JAPANESE -> "猛烈な暴風が予想されます。頑丈な屋内にとどまり、窓から離れて身の安全を確保して下さい。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Gale warning in effect. Please refrain from non-essential outdoor trips and secure loose items outside."
                    AppLanguage.KOREAN -> "강풍·폭풍에 경계가 필요합니다. 불필요한 외출을 삼가고 실외의 날아가기 쉬운 물건을 고정하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "需对强风和暴风保持警戒。请减少不必要的外出，固定好室外易被吹飞的物品。"
                    AppLanguage.TRADITIONAL_CHINESE -> "需對強風和暴風保持警戒。請減少不必要的外出，固定好室外易被吹飛的物品。"
                    AppLanguage.SPANISH -> "Alerta por vendaval en vigor. Evite salidas innecesarias y asegure los objetos sueltos en el exterior."
                    AppLanguage.FRENCH -> "Alerte coup de vent en vigueur. Évitez les déplacements non essentiels et sécurisez les objets à l'extérieur."
                    AppLanguage.PORTUGUESE -> "Alerta de vendaval em vigor. Evite saídas desnecessárias e prenda objetos soltos em áreas externas."
                    AppLanguage.JAPANESE -> "強風・暴風に警戒が必要です。不要不急の外出を控え、飛びやすい屋外の物品を固定して下さい。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Notice strong wind conditions. Please watch for sudden gusts and flying debris."
                    AppLanguage.KOREAN -> "강풍에 주의하고 갑작스러운 돌풍과 낙하물에 유의하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "请注意强风，留意突发阵风及高空坠物。"
                    AppLanguage.TRADITIONAL_CHINESE -> "請注意強風，留意突發陣風及高空墜物。"
                    AppLanguage.SPANISH -> "Precaución por vientos fuertes. Esté atento a ráfagas repentinas y objetos que puedan caer."
                    AppLanguage.FRENCH -> "Prudence face aux vents forts. Attention aux rafales soudaines et aux chutes d'objets."
                    AppLanguage.PORTUGUESE -> "Atenção aos ventos fortes. Fique atento a rajadas repentinas e objetos que possam cair."
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
                    AppLanguage.KOREAN -> "중대한 침수 또는 홍수 재해가 임박했거나 이미 발생했을 가능성이 매우 높은 상황."
                    AppLanguage.SIMPLIFIED_CHINESE -> "重大浸水害或洪水灾害迫近，或者极有可能已经发生。"
                    AppLanguage.TRADITIONAL_CHINESE -> "重大浸水害或洪水災害迫近，或者極有可能已經發生。"
                    AppLanguage.SPANISH -> "Desastre grave por anegamiento o inundación inminente, o muy probablemente ya en curso."
                    AppLanguage.FRENCH -> "Inondation grave ou crue majeure imminente, ou très probablement déjà en cours."
                    AppLanguage.PORTUGUESE -> "Desastre grave por alagamento ou enchente iminente, ou com altíssima probabilidade de já estar ocorrendo."
                    AppLanguage.JAPANESE -> "重大な浸水害又は洪水害が切迫。又は、すでに発生している可能性が極めて高い状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Major disaster could occur at any moment due to flash flooding or river rise. Low-lying houses risk above-floor flooding."
                    AppLanguage.KOREAN -> "단시간 집중호우로 인한 침수나 하천 수위 상승으로 중대한 재해가 언제든 발생할 수 있는 상황. 저지대 주택이 실내 바닥까지 침수될 우려."
                    AppLanguage.SIMPLIFIED_CHINESE -> "短时强降雨可能导致浸水或河流暴涨，重大灾害随时可能发生。低洼地区房屋面临漫过地面的积水风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "短時強降雨可能導致浸水或河流暴漲，重大災害隨時可能發生。低窪地區房屋面臨漫過地面的積水風險。"
                    AppLanguage.SPANISH -> "Un desastre grave podría ocurrir en cualquier momento por inundaciones repentinas o crecidas. Las viviendas en zonas bajas corren riesgo de inundación interior."
                    AppLanguage.FRENCH -> "Une catastrophe majeure peut survenir à tout moment par ruissellement ou montée des eaux. Les habitations en zone basse risquent d'être inondées."
                    AppLanguage.PORTUGUESE -> "Um desastre grave pode ocorrer a qualquer momento por enxurradas ou cheia de rios. Casas em áreas baixas correm risco de invasão pela água."
                    AppLanguage.JAPANESE -> "短時間強雨での浸水または河川増水により重大な災害がいつ発生してもおかしくない。周囲より低い場所にある多くの家屋が、床上まで水に浸かるおそれがある。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Gutters, sewers, or rivers may overflow from short heavy rain, potentially causing major disasters."
                    AppLanguage.KOREAN -> "단시간 집중호우로 배수로·하수구 또는 하천이 범람하여 중대한 재해가 발생할 우려."
                    AppLanguage.SIMPLIFIED_CHINESE -> "短时强降雨可能导致排水沟、下水管道或河流水位溢出，面临重大灾害风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "短時強降雨可能導致排水溝、下水管道或河流水位溢出，面臨重大災害風險。"
                    AppLanguage.SPANISH -> "Las cunetas, alcantarillas o ríos pueden desbordarse por lluvias intensas breves, con riesgo de desastres graves."
                    AppLanguage.FRENCH -> "Les caniveaux, égouts ou cours d'eau peuvent déborder sous l'effet de fortes pluies brèves, pouvant provoquer des dégâts majeurs."
                    AppLanguage.PORTUGUESE -> "Bueiros, galerias ou rios podem transbordar devido a chuvas fortes e rápidas, podendo causar desastres graves."
                    AppLanguage.JAPANESE -> "短時間強雨により、側溝や下水、または河川があふれ、重大な災害が発生するおそれがある。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Gutters, sewers, or rivers may overflow from short heavy rain, causing localized road flooding."
                    AppLanguage.KOREAN -> "단시간 강한 비로 배수로나 하수구가 넘쳐 도로 일부가 침수될 우려."
                    AppLanguage.SIMPLIFIED_CHINESE -> "短时强降雨可能导致排水沟或下水溢水，道路面临淹水风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "短時強降雨可能導致排水溝或下水溢水，道路面臨淹水風險。"
                    AppLanguage.SPANISH -> "Las cunetas, alcantarillas o ríos pueden desbordarse por lluvias intensas breves y causar anegamientos locales en carreteras."
                    AppLanguage.FRENCH -> "Les caniveaux, égouts ou cours d'eau peuvent déborder sous de fortes pluies brèves, provoquant des inondations locales de chaussées."
                    AppLanguage.PORTUGUESE -> "Bueiros, galerias ou rios podem transbordar com chuvas fortes e rápidas, causando alagamentos pontuais nas vias."
                    AppLanguage.JAPANESE -> "短時間強雨により、側溝や下水、または河川があふれ、冠水するおそれがある。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal conditions. During rain, water accumulates in low-lying areas."
                    AppLanguage.KOREAN -> "평상시와 같은 상황. 비가 내릴 때는 주변보다 낮은 곳으로 빗물이 모임."
                    AppLanguage.SIMPLIFIED_CHINESE -> "平时状况。降雨时雨水易向低洼处汇聚。"
                    AppLanguage.TRADITIONAL_CHINESE -> "平時狀況。降雨時雨水易向低窪處彙聚。"
                    AppLanguage.SPANISH -> "Condiciones normales. Cuando llueve, el agua se acumula en las zonas bajas."
                    AppLanguage.FRENCH -> "Conditions normales. En cas de pluie, l'eau s'accumule dans les zones basses."
                    AppLanguage.PORTUGUESE -> "Condições normais. Durante a chuva, a água se acumula em áreas baixas."
                    AppLanguage.JAPANESE -> "普段と同じ状況。雨のときは、雨水が周囲より低い場所に集まる。"
                }
            }

            // ==========================================
            // 2. 土砂キキクル (Image 2)
            // ==========================================
            DisasterType.LANDSLIDE -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Life-threatening landslide imminent, or very likely already occurring. Critical danger."
                    AppLanguage.KOREAN -> "생명을 위협하는 산사태가 임박했거나 이미 발생했을 가능성이 높은 극도로 위험한 상황."
                    AppLanguage.SIMPLIFIED_CHINESE -> "危及生命的土砂滑坡灾害迫在眉睫，极有可能已发生滑坡。极度危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "危及生命的土砂滑坡災害迫在眉睫，極有可能已發生滑坡。極度危險。"
                    AppLanguage.SPANISH -> "Deslizamiento mortal inminente o muy probablemente ya en curso. Peligro crítico."
                    AppLanguage.FRENCH -> "Glissement de terrain mortel imminent ou très probablement déjà en cours. Danger critique."
                    AppLanguage.PORTUGUESE -> "Deslizamento com risco de vida iminente ou provavelmente já ocorrendo. Perigo crítico."
                    AppLanguage.JAPANESE -> "命に危険が及ぶ土砂災害が切迫。土砂災害がすでに発生している可能性が高い極めて危険な状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Slope cracks or rising groundwater indicate a landslide could strike at any time."
                    AppLanguage.KOREAN -> "사면 균열이나 지하수 용출 등 생명을 위협하는 산사태가 언제든 발생할 수 있는 매우 위험한 상황."
                    AppLanguage.SIMPLIFIED_CHINESE -> "斜坡开裂或异常涌水，山体滑坡随时可能发生，情况极其危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "斜坡開裂或異常湧水，山體滑坡隨時可能發生，情況極其危險。"
                    AppLanguage.SPANISH -> "Grietas en laderas o brotes de agua subterránea indican que un deslizamiento puede ocurrir en cualquier momento."
                    AppLanguage.FRENCH -> "Des fissures sur les pentes ou des résurgences d'eau indiquent qu'un glissement de terrain peut survenir à tout moment."
                    AppLanguage.PORTUGUESE -> "Rachaduras em encostas ou afloramento de água indicam que um deslizamento pode ocorrer a qualquer momento."
                    AppLanguage.JAPANESE -> "斜面のひび割れや湧水など、命に危険が及ぶ土砂災害がいつ発生してもおかしくない極めて危険な状況。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Prolonged or heavy rain has loosened ground. Heightened vigilance for landslides required."
                    AppLanguage.KOREAN -> "장시간 비나 강한 비로 지반이 약해져 산사태 경계가 필요한 상황."
                    AppLanguage.SIMPLIFIED_CHINESE -> "持续强降雨导致土壤松动，需高度警惕滑坡灾害。"
                    AppLanguage.TRADITIONAL_CHINESE -> "持續強降雨導致土壤鬆動，需高度警惕滑坡災害。"
                    AppLanguage.SPANISH -> "Las lluvias prolongadas o intensas han reblandecido el terreno. Se requiere vigilancia extrema por deslizamientos."
                    AppLanguage.FRENCH -> "Les pluies prolongées ou intenses ont fragilisé le sol. Une vigilance accrue face aux glissements de terrain est requise."
                    AppLanguage.PORTUGUESE -> "Chuvas prolongadas ou intensas encharcaram o solo. É necessária vigilância redobrada para deslizamentos."
                    AppLanguage.JAPANESE -> "長雨や強雨により地盤が緩み、土砂災害への警戒が必要な状況。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Caution required for landslides due to rainfall. Hazard map verification recommended."
                    AppLanguage.KOREAN -> "강우로 인해 산사태 주의가 필요한 상황. 재해 지도 및 대피 경로 확인 필요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "因降雨需对滑坡保持警惕的状态，建议查阅防灾地图。"
                    AppLanguage.TRADITIONAL_CHINESE -> "因降雨需對滑坡保持警惕的狀態，建議查閱防災地圖。"
                    AppLanguage.SPANISH -> "Se requiere precaución por deslizamientos debido a la lluvia. Se recomienda revisar el mapa de riesgos."
                    AppLanguage.FRENCH -> "Prudence requise face aux glissements de terrain en raison des pluies. Il est recommandé de consulter les cartes de risques."
                    AppLanguage.PORTUGUESE -> "Cautela necessária para deslizamentos devido à chuva. Recomenda-se consultar o mapa de riscos."
                    AppLanguage.JAPANESE -> "降雨に伴い土砂災害への注意が必要な状況。ハザードマップ等の確認が必要。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal conditions. Confirm steep slope hazard maps."
                    AppLanguage.KOREAN -> "평상시와 같은 상황. 급경사지 등 재해 위험 구역을 미리 확인."
                    AppLanguage.SIMPLIFIED_CHINESE -> "通常状况。确认陡坡等隐患区域地图。"
                    AppLanguage.TRADITIONAL_CHINESE -> "通常狀況。確認陡坡等隱患區域地圖。"
                    AppLanguage.SPANISH -> "Condiciones normales. Consulte los mapas de riesgo de laderas empinadas."
                    AppLanguage.FRENCH -> "Conditions normales. Consultez les cartes de risques pour les pentes raides."
                    AppLanguage.PORTUGUESE -> "Condições normais. Verifique os mapas de risco de encostas íngremes."
                    AppLanguage.JAPANESE -> "普段と同じ状況。急傾斜地等のハザードマップを確認。"
                }
            }

            // ==========================================
            // 3. 浸水キキクル (Image 3)
            // ==========================================
            DisasterType.INUNDATION -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Major inundation imminent. Extremely high probability inundation is already taking place."
                    AppLanguage.KOREAN -> "중대한 침수 피해가 임박했으며 이미 침수가 발생했을 가능성이 매우 높은 상황."
                    AppLanguage.SIMPLIFIED_CHINESE -> "重大浸水灾害迫近，浸水极其可能已经发生。"
                    AppLanguage.TRADITIONAL_CHINESE -> "重大浸水災害迫近，浸水極其可能已經發生。"
                    AppLanguage.SPANISH -> "Anegamiento grave inminente. Es sumamente probable que ya esté ocurriendo."
                    AppLanguage.FRENCH -> "Inondation majeure imminente. Il est extrêmement probable qu'elle soit déjà en cours."
                    AppLanguage.PORTUGUESE -> "Alagamento grave iminente. Altíssima probabilidade de já estar ocorrendo."
                    AppLanguage.JAPANESE -> "重大な浸水害が切迫。浸水害がすでに発生している可能性が極めて高い状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Roads fully submerged with obscured gutters and manholes. Public transit disrupted. Low-lying homes risk above-floor flooding."
                    AppLanguage.KOREAN -> "도로가 전면 침수되어 배수로·맨홀 위치를 식별하기 어렵고 대중교통 운행에 차질이 생길 우려. 저지대 주택 실내 침수 위험."
                    AppLanguage.SIMPLIFIED_CHINESE -> "道路大面积积水淹没排水口。公共交通受阻。低洼地带房屋面临漫过地面的严重浸水危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "道路大面積積水淹沒排水口。公共交通受阻。低窪地帶房屋面臨漫過地面的嚴重浸水危險。"
                    AppLanguage.SPANISH -> "Calles completamente anegadas con cunetas y alcantarillas ocultas. Transporte público afectado. Riesgo de inundación interior en viviendas bajas."
                    AppLanguage.FRENCH -> "Routes entièrement submergées masquant caniveaux et bouches d'égout. Transports perturbés. Risque d'inondation des logements en zone basse."
                    AppLanguage.PORTUGUESE -> "Vias totalmente alagadas encobrindo bueiros e tampas. Transporte público afetado. Casas em áreas baixas correm risco de alagamento interno."
                    AppLanguage.JAPANESE -> "道路が一面冠水し、側溝やマンホールの場所が分からなくなるおそれがある。道路冠水等のために鉄道やバスなどの交通機関の運行に影響が出るおそれがある。周囲より低い場所にある多くの家屋が、床上まで水に浸かるおそれがある。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Gutters and drains overflowing; street flooding imminent. Low-lying homes at risk of above-floor water."
                    AppLanguage.KOREAN -> "배수로와 하수구가 넘쳐 도로가 언제든 침수될 수 있는 상황. 저지대 주택 침수 우려."
                    AppLanguage.SIMPLIFIED_CHINESE -> "排水沟与下水漫溢，道路随时可能积水，低洼住宅有进水风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "排水溝與下水漫溢，道路隨時可能積水，低窪住宅有進水風險。"
                    AppLanguage.SPANISH -> "Cunetas y desagües desbordados; anegamiento de calles inminente. Viviendas bajas en riesgo de entrada de agua."
                    AppLanguage.FRENCH -> "Caniveaux et égouts en débordement ; inondation des rues imminente. Risque d'infiltration d'eau dans les habitations basses."
                    AppLanguage.PORTUGUESE -> "Bueiros e galerias transbordando; alagamento de ruas iminente. Casas em áreas baixas com risco de invasão pela água."
                    AppLanguage.JAPANESE -> "側溝や下水が溢れ、道路がいつ冠水してもおかしくない。周囲より低い場所にある家屋が、床上まで水に浸かるおそれがある。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "Low-lying areas risk gutter overflow and road puddles. Water may enter basements or underpasses. Homes risk below-floor flooding."
                    AppLanguage.KOREAN -> "저지대 배수로가 넘쳐 도로에 물이 고이고 지하실이나 지하차도로 빗물이 유입될 우려."
                    AppLanguage.SIMPLIFIED_CHINESE -> "低洼处沟渠可能漫溢道路积水，地下室或立交下沉路段有倒灌进水风险，房屋有地面以下进水危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "低窪處溝渠可能漫溢道路積水，地下室或立交下沉路段有倒灌進水風險，房屋有地面以下進水危險。"
                    AppLanguage.SPANISH -> "Riesgo de desborde de desagües y charcos profundos en zonas bajas. El agua puede entrar en sótanos y pasos subterráneos."
                    AppLanguage.FRENCH -> "Risque de débordement de caniveaux et d'accumulation d'eau en zone basse. L'eau peut pénétrer dans les sous-sols et passages souterrains."
                    AppLanguage.PORTUGUESE -> "Áreas baixas com risco de transbordamento de bueiros e acúmulo nas vias. A água pode invadir porões e passagens subterrâneas."
                    AppLanguage.JAPANESE -> "周囲より低い場所で側溝や下水が溢れ、道路が冠水するおそれがある。住宅の地下室や道路のアンダーパスに水が流れ込むおそれがある。周囲より低い場所にある家屋が、床下まで水に浸かるおそれがある。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal conditions. Rainwater collects in lower areas."
                    AppLanguage.KOREAN -> "평상시와 같은 상황. 비가 오면 낮은 지대로 빗물이 모임."
                    AppLanguage.SIMPLIFIED_CHINESE -> "日常状态。降雨时雨水聚集于低洼处。"
                    AppLanguage.TRADITIONAL_CHINESE -> "日常狀態。降雨時雨水聚集於低窪處。"
                    AppLanguage.SPANISH -> "Condiciones normales. El agua de lluvia se acumula en las zonas bajas."
                    AppLanguage.FRENCH -> "Conditions normales. Les eaux de pluie s'accumulent dans les points bas."
                    AppLanguage.PORTUGUESE -> "Condições normais. A água da chuva se acumula em áreas mais baixas."
                    AppLanguage.JAPANESE -> "普段と同じ状況。雨のときは、雨水が周囲より低い場所に集まる。"
                }
            }

            // ==========================================
            // 4. 洪水キキクル (Image 4)
            // ==========================================
            DisasterType.FLOOD -> when (level) {
                AlertLevel.LEVEL_5 -> when (language) {
                    AppLanguage.ENGLISH -> "Major flood (river breach/overflow) imminent or already occurring. Extremely critical danger."
                    AppLanguage.KOREAN -> "중대한 홍수 재해(하천 범람·제방 붕괴)가 임박했거나 이미 발생한 극도로 위험한 상황."
                    AppLanguage.SIMPLIFIED_CHINESE -> "重大河川外水泛滥迫在眉睫，或决堤溃坝已经发生。极端危险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "重大河川外水氾濫迫在眉睫，或決堤潰壩已經發生。極端危險。"
                    AppLanguage.SPANISH -> "Inundación grave (rotura de dique o desbordamiento) inminente o ya en curso. Peligro extremadamente crítico."
                    AppLanguage.FRENCH -> "Crue majeure (rupture de digue ou débordement) imminente ou déjà en cours. Danger extrêmement critique."
                    AppLanguage.PORTUGUESE -> "Enchente grave (rompimento de dique ou transbordamento) iminente ou já em curso. Perigo extremamente crítico."
                    AppLanguage.JAPANESE -> "重大な洪水災害（外水氾濫）が切迫、またはすでに堤防決壊・越水が発生している極めて危険な状況。"
                }
                AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Extremely high risk of major flooding from river overflow (exceeding 50-year return period). Flood danger level reached."
                    AppLanguage.KOREAN -> "하천 범람으로 인한 중대한 침수 피해 위험이 매우 높은 상황(50년 빈도 기준 초과). 범람 위험 수위 도달 우려."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河川外水泛滥引发重大浸水灾害风险极高（超50年一遇标准）。极可能已达泛滥危险水位。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河川外水氾濫引發重大浸水災害風險極高（超50年一遇標準）。極可能已達氾濫危險水位。"
                    AppLanguage.SPANISH -> "Riesgo sumamente alto de inundación grave por desbordamiento del río (supera el período de retorno de 50 años). Nivel de peligro de crecida alcanzado."
                    AppLanguage.FRENCH -> "Risque extrêmement élevé de crue majeure par débordement (seuil cinquantennal dépassé). Niveau de danger d'inondation atteint."
                    AppLanguage.PORTUGUESE -> "Risco extremamente alto de enchente grave por transbordamento de rio (acima do período de retorno de 50 anos). Nível de perigo atingido."
                    AppLanguage.JAPANESE -> "河川流域で発生した外水氾濫に起因する重大な浸水害が極めて発生しやすい状況（50年確率値基準超過）。氾濫危険水位に達しているおそれ。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Risk of serious flooding from river/local drainage (reaching 30-year return period). Water level exceeds evacuation trigger."
                    AppLanguage.KOREAN -> "하천 수위 상승으로 중대한 침수 피해가 발생할 우려(30년 빈도 기준 도달). 대피 판단 수위 초과."
                    AppLanguage.SIMPLIFIED_CHINESE -> "河流水位上涨可能引发外水/内水泛滥（达30年一遇标准）。已超避难判断水位。"
                    AppLanguage.TRADITIONAL_CHINESE -> "河流水位上漲可能引發外水/內水氾濫（達30年一遇標準）。已超避難判斷水位。"
                    AppLanguage.SPANISH -> "Riesgo de inundación grave por crecida fluvial o drenaje local (período de retorno de 30 años). El nivel del agua supera el umbral de evacuación."
                    AppLanguage.FRENCH -> "Risque de crue grave ou de saturation du drainage (seuil trentennal atteint). Le niveau d'eau dépasse le seuil d'évacuation."
                    AppLanguage.PORTUGUESE -> "Risco de enchente grave por cheia fluvial ou drenagem local (período de retorno de 30 anos). Nível da água acima do patamar de evacuação."
                    AppLanguage.JAPANESE -> "河川流域で外水氾濫・内水氾濫に起因する重大な浸水害が発生するおそれ（30年確率値基準到達）。避難判断水位を超過。"
                }
                AlertLevel.LEVEL_2 -> when (language) {
                    AppLanguage.ENGLISH -> "River advisory threshold reached (70-80% of warning threshold). Rising water level caution required."
                    AppLanguage.KOREAN -> "하천 범람 주의보 기준(경보 기준의 70~80%) 도달. 하천 수위 상승 주의 필요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "达河道泛滥注意报标准（达警报标准70~80%），需注意河水上涨。"
                    AppLanguage.TRADITIONAL_CHINESE -> "達河道氾濫注意報標準（達警報標準70~80%），需注意河水上漲。"
                    AppLanguage.SPANISH -> "Umbral de aviso fluvial alcanzado (70-80 % del umbral de alerta). Precaución ante el aumento del caudal."
                    AppLanguage.FRENCH -> "Seuil de vigilance crue atteint (70 à 80 % du seuil d'alerte). Prudence face à la montée des eaux."
                    AppLanguage.PORTUGUESE -> "Limite de aviso fluvial atingido (70 a 80% do limite de alerta). Atenção à elevação do nível do rio."
                    AppLanguage.JAPANESE -> "河川流域で外水氾濫・内水氾濫に起因する浸水害を見逃さないための注意報基準（警報基準の7〜8割）に到達。河川増水のおそれ。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal river levels. Be alert to upstream rain."
                    AppLanguage.KOREAN -> "평상시 하천 수위. 상류 강우에 따른 수위 상승에 유의."
                    AppLanguage.SIMPLIFIED_CHINESE -> "正常河流水位。留意上游汇水情况。"
                    AppLanguage.TRADITIONAL_CHINESE -> "正常河流水位。留意上游匯水情況。"
                    AppLanguage.SPANISH -> "Niveles fluviales normales. Esté atento a las lluvias río arriba."
                    AppLanguage.FRENCH -> "Niveaux fluviaux normaux. Restez attentif aux pluies en amont."
                    AppLanguage.PORTUGUESE -> "Níveis fluviais normais. Fique atento às chuvas a montante."
                    AppLanguage.JAPANESE -> "普段通りの河川水位。上流からの増水等に留意。"
                }
            }

            DisasterType.STORM_WIND -> when (level) {
                AlertLevel.LEVEL_5, AlertLevel.LEVEL_4 -> when (language) {
                    AppLanguage.ENGLISH -> "Violent storm damage occurring. Roof tiles, trees, and power lines may blow down."
                    AppLanguage.KOREAN -> "매우 강한 폭풍으로 지붕재 비산, 가로수 전도, 전선 단선 등 중대한 피해 우려."
                    AppLanguage.SIMPLIFIED_CHINESE -> "严重暴风灾害发生，飞瓦断树、倒杆断线风险极高。"
                    AppLanguage.TRADITIONAL_CHINESE -> "嚴重暴風災害發生，飛瓦斷樹、倒桿斷線風險極高。"
                    AppLanguage.SPANISH -> "Daños graves por vientos violentos. Riesgo de desprendimiento de tejas, caída de árboles y corte de cables eléctricos."
                    AppLanguage.FRENCH -> "Dégâts importants liés aux vents violents. Risque de chutes de tuiles, d'arbres et de lignes électriques."
                    AppLanguage.PORTUGUESE -> "Danos graves por ventos violentos. Risco de destelhamento, queda de árvores e rompimento de fios elétricos."
                    AppLanguage.JAPANESE -> "猛烈な暴風により、屋根瓦の飛散や樹木の倒壊、電線切断などの重大被害のおそれ。"
                }
                AlertLevel.LEVEL_3 -> when (language) {
                    AppLanguage.ENGLISH -> "Strong gale winds making walking difficult; small branches breaking."
                    AppLanguage.KOREAN -> "바람을 맞고 걷기 어려울 정도의 강풍. 간판이나 시설물 낙하 주의."
                    AppLanguage.SIMPLIFIED_CHINESE -> "强风使行走困难，树枝折断，广告牌等有坠落风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "強風使行走困難，樹枝折斷，廣告牌等有墜落風險。"
                    AppLanguage.SPANISH -> "Fuertes vendavales que dificultan caminar; rotura de ramas y riesgo de caída de carteles."
                    AppLanguage.FRENCH -> "Vents violents rendant la marche difficile ; branches cassées et risque de chute de panneaux."
                    AppLanguage.PORTUGUESE -> "Ventos fortes que dificultam caminhar; galhos quebrando e risco de queda de placas."
                    AppLanguage.JAPANESE -> "風に向かって歩けなくなるほどの強風。看板やトタンの飛散に注意。"
                }
                AlertLevel.LEVEL_2, AlertLevel.LEVEL_1 -> when (language) {
                    AppLanguage.ENGLISH -> "Moderate winds. Umbrella may turn inside out."
                    AppLanguage.KOREAN -> "바람이 강해져 우산을 쓰기 어려워질 수 있습니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "风力增强，伞具易被吹翻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "風力增強，傘具易被吹翻。"
                    AppLanguage.SPANISH -> "Vientos moderados a fuertes. Los paraguas pueden darse la vuelta."
                    AppLanguage.FRENCH -> "Vents modérés à soutenus. Les parapluies peuvent se retourner."
                    AppLanguage.PORTUGUESE -> "Ventos moderados a fortes. Guarda-chuvas podem virar ao avesso."
                    AppLanguage.JAPANESE -> "風が強まり、傘がさしにくくなるおそれがあります。"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Normal wind conditions. Monitor weather updates."
                    AppLanguage.KOREAN -> "평상시 수준의 바람입니다. 최신 기상 정보에 유의하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "风力平稳，关注最新天气变化。"
                    AppLanguage.TRADITIONAL_CHINESE -> "風力平穩，留意最新天氣變化。"
                    AppLanguage.SPANISH -> "Condiciones de viento normales. Siga las actualizaciones meteorológicas."
                    AppLanguage.FRENCH -> "Conditions de vent normales. Suivez les bulletins météorologiques."
                    AppLanguage.PORTUGUESE -> "Condições de vento normais. Acompanhe as atualizações meteorológicas."
                    AppLanguage.JAPANESE -> "普段通りの風況。最新の気象情報に留意してください。"
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
                AppLanguage.KOREAN -> "강수·호우 위험도 지도 확인"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看大雨危险度雷达"
                AppLanguage.TRADITIONAL_CHINESE -> "查看大雨危險度雷達"
                AppLanguage.SPANISH -> "Ver radar de lluvia intensa"
                AppLanguage.FRENCH -> "Voir le radar de fortes pluies"
                AppLanguage.PORTUGUESE -> "Ver radar de chuva intensa"
                AppLanguage.JAPANESE -> "雨雲・大雨キキクルを確認"
            }
            DisasterType.LANDSLIDE -> when (language) {
                AppLanguage.ENGLISH -> "Check Landslide Map"
                AppLanguage.KOREAN -> "산사태 위험도 지도 확인"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看土砂灾害分布图"
                AppLanguage.TRADITIONAL_CHINESE -> "查看土砂災害分佈圖"
                AppLanguage.SPANISH -> "Ver mapa de deslizamientos"
                AppLanguage.FRENCH -> "Voir la carte des glissements"
                AppLanguage.PORTUGUESE -> "Ver mapa de deslizamentos"
                AppLanguage.JAPANESE -> "土砂キキクルマップを確認"
            }
            DisasterType.INUNDATION -> when (language) {
                AppLanguage.ENGLISH -> "Check Inundation Map"
                AppLanguage.KOREAN -> "침수 위험도 지도 확인"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看浸水害分布图"
                AppLanguage.TRADITIONAL_CHINESE -> "查看浸水害分佈圖"
                AppLanguage.SPANISH -> "Ver mapa de anegamiento"
                AppLanguage.FRENCH -> "Voir la carte d'inondation"
                AppLanguage.PORTUGUESE -> "Ver mapa de alagamentos"
                AppLanguage.JAPANESE -> "浸水キキクルマップを確認"
            }
            DisasterType.FLOOD -> when (language) {
                AppLanguage.ENGLISH -> "Check Flood Map"
                AppLanguage.KOREAN -> "홍수 위험도 지도 확인"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看洪水灾害分布图"
                AppLanguage.TRADITIONAL_CHINESE -> "查看洪水災害分佈圖"
                AppLanguage.SPANISH -> "Ver mapa de inundaciones"
                AppLanguage.FRENCH -> "Voir la carte des crues"
                AppLanguage.PORTUGUESE -> "Ver mapa de inundações"
                AppLanguage.JAPANESE -> "洪水キキクルマップを確認"
            }
            DisasterType.STORM_WIND -> when (language) {
                AppLanguage.ENGLISH -> "Check Wind Radar"
                AppLanguage.KOREAN -> "강풍·레이더 지도 확인"
                AppLanguage.SIMPLIFIED_CHINESE -> "查看大风雷达"
                AppLanguage.TRADITIONAL_CHINESE -> "查看大風雷達"
                AppLanguage.SPANISH -> "Ver radar de viento"
                AppLanguage.FRENCH -> "Voir le radar de vent"
                AppLanguage.PORTUGUESE -> "Ver radar de vento"
                AppLanguage.JAPANESE -> "強風・レーダーマップを確認"
            }
        }
    }
}
