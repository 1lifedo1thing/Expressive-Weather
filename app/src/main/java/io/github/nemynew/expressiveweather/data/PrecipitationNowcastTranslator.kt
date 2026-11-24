package io.github.nemynew.expressiveweather.data

/**
 * 降水ナウキャスト・自然言語テキストの後方互換および多言語フォールバック翻訳器
 *
 * 過去にキャッシュ保存された WeatherData（localizedHeadlines 等が空マップの古いオブジェクト）や、
 * 動的に切り替えられた言語に対して即座に高品位な自然言語テキストを提供します。
 */
object PrecipitationNowcastTranslator {

    fun translateHeadline(text: String, language: AppLanguage): String? {
        if (language == AppLanguage.JAPANESE) return text
        if (text.isBlank()) return text

        return when (text) {
            "明日の夕方以降は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow evening"
                AppLanguage.KOREAN -> "내일 저녁부터 우산이 필요합니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天傍晚起需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天傍晚起需要帶傘"
                AppLanguage.SPANISH -> "Necesitarás paraguas mañana al atardecer"
                AppLanguage.FRENCH -> "Parapluie nécessaire demain en soirée"
                AppLanguage.PORTUGUESE -> "Você vai precisar de guarda-chuva amanhã à noite"
                AppLanguage.JAPANESE -> text
            }
            "明日の夜は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow night"
                AppLanguage.KOREAN -> "내일 밤에는 우산이 필요합니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天夜间需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天夜間需要帶傘"
                AppLanguage.SPANISH -> "Necesitarás paraguas mañana por la noche"
                AppLanguage.FRENCH -> "Parapluie nécessaire demain soir"
                AppLanguage.PORTUGUESE -> "Você vai precisar de guarda-chuva amanhã à noite"
                AppLanguage.JAPANESE -> text
            }
            "明日の午後は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow afternoon"
                AppLanguage.KOREAN -> "내일 오후에는 우산이 필요합니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天下午需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天下午需要帶傘"
                AppLanguage.SPANISH -> "Necesitarás paraguas mañana por la tarde"
                AppLanguage.FRENCH -> "Parapluie nécessaire demain après-midi"
                AppLanguage.PORTUGUESE -> "Você vai precisar de guarda-chuva amanhã à tarde"
                AppLanguage.JAPANESE -> text
            }
            "明日の午前中は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow morning"
                AppLanguage.KOREAN -> "내일 오전에는 우산이 필요합니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天上午需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天上午需要帶傘"
                AppLanguage.SPANISH -> "Necesitarás paraguas mañana por la mañana"
                AppLanguage.FRENCH -> "Parapluie nécessaire demain matin"
                AppLanguage.PORTUGUESE -> "Você vai precisar de guarda-chuva amanhã de manhã"
                AppLanguage.JAPANESE -> text
            }
            "明日未明は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow before dawn"
                AppLanguage.KOREAN -> "내일 새벽에는 우산이 필요합니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天凌晨需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天凌晨需要帶傘"
                AppLanguage.SPANISH -> "Necesitarás paraguas mañana de madrugada"
                AppLanguage.FRENCH -> "Parapluie nécessaire demain avant l'aube"
                AppLanguage.PORTUGUESE -> "Você vai precisar de guarda-chuva amanhã de madrugada"
                AppLanguage.JAPANESE -> text
            }
            "明日は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow"
                AppLanguage.KOREAN -> "내일은 우산이 필요합니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天需要帶傘"
                AppLanguage.SPANISH -> "Necesitarás paraguas mañana"
                AppLanguage.FRENCH -> "Parapluie nécessaire demain"
                AppLanguage.PORTUGUESE -> "Você vai precisar de guarda-chuva amanhã"
                AppLanguage.JAPANESE -> text
            }
            "今夜は雨が降るでしょう" -> when (language) {
                AppLanguage.ENGLISH -> "Rain expected tonight"
                AppLanguage.KOREAN -> "오늘 밤 비가 올 전망입니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计今晚有雨"
                AppLanguage.TRADITIONAL_CHINESE -> "預計今晚有雨"
                AppLanguage.SPANISH -> "Se espera lluvia esta noche"
                AppLanguage.FRENCH -> "Pluie attendue ce soir"
                AppLanguage.PORTUGUESE -> "Previsão de chuva hoje à noite"
                AppLanguage.JAPANESE -> text
            }
            "まもなく雨が降り始めます" -> when (language) {
                AppLanguage.ENGLISH -> "Rain starting shortly"
                AppLanguage.KOREAN -> "곧 비가 내리기 시작합니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "即将开始下雨"
                AppLanguage.TRADITIONAL_CHINESE -> "即將開始下雨"
                AppLanguage.SPANISH -> "Empezará a llover en breve"
                AppLanguage.FRENCH -> "La pluie va bientôt commencer"
                AppLanguage.PORTUGUESE -> "Vai começar a chover em breve"
                AppLanguage.JAPANESE -> text
            }
            "まもなく激しい雨が降ります" -> when (language) {
                AppLanguage.ENGLISH -> "Heavy rain starting shortly"
                AppLanguage.KOREAN -> "곧 강한 비가 내릴 전망입니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "即将下大雨"
                AppLanguage.TRADITIONAL_CHINESE -> "即將下大雨"
                AppLanguage.SPANISH -> "Lluvia intensa en breve"
                AppLanguage.FRENCH -> "Forte pluie imminente"
                AppLanguage.PORTUGUESE -> "Chuva forte começando em breve"
                AppLanguage.JAPANESE -> text
            }
            "まもなく雨が止む見込みです" -> when (language) {
                AppLanguage.ENGLISH -> "Rain stopping shortly"
                AppLanguage.KOREAN -> "곧 비가 그칠 전망입니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨很快就会停"
                AppLanguage.TRADITIONAL_CHINESE -> "雨很快就會停"
                AppLanguage.SPANISH -> "Dejará de llover en breve"
                AppLanguage.FRENCH -> "La pluie va bientôt cesser"
                AppLanguage.PORTUGUESE -> "A chuva deve parar em breve"
                AppLanguage.JAPANESE -> text
            }
            "雨は当面の間降り続く見込みです" -> when (language) {
                AppLanguage.ENGLISH -> "Rain will continue for the foreseeable future"
                AppLanguage.KOREAN -> "당분간 비가 계속 이어질 전망입니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计降雨还将持续一段时间"
                AppLanguage.TRADITIONAL_CHINESE -> "預計降雨還將持續一段時間"
                AppLanguage.SPANISH -> "La lluvia continuará por el momento"
                AppLanguage.FRENCH -> "La pluie va continuer pour le moment"
                AppLanguage.PORTUGUESE -> "A chuva deve continuar por enquanto"
                AppLanguage.JAPANESE -> text
            }
            "雨は当面の間（6時間以上）降り続く見込みです" -> when (language) {
                AppLanguage.ENGLISH -> "Rain will continue for the next 6+ hours"
                AppLanguage.KOREAN -> "앞으로 6시간 이상 비가 계속될 전망입니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计降雨还将持续6小时以上"
                AppLanguage.TRADITIONAL_CHINESE -> "預計降雨還將持續6小時以上"
                AppLanguage.SPANISH -> "La lluvia continuará durante más de 6 horas"
                AppLanguage.FRENCH -> "La pluie va continuer pendant plus de 6 heures"
                AppLanguage.PORTUGUESE -> "A chuva deve continuar pelas próximas 6+ horas"
                AppLanguage.JAPANESE -> text
            }
            "明日の朝から雨が降る見込みです" -> when (language) {
                AppLanguage.ENGLISH -> "Rain expected tomorrow morning"
                AppLanguage.KOREAN -> "내일 아침부터 비가 올 전망입니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计明天上午有雨"
                AppLanguage.TRADITIONAL_CHINESE -> "預計明天上午有雨"
                AppLanguage.SPANISH -> "Se espera lluvia mañana por la mañana"
                AppLanguage.FRENCH -> "Pluie attendue demain matin"
                AppLanguage.PORTUGUESE -> "Previsão de chuva amanhã de manhã"
                AppLanguage.JAPANESE -> text
            }
            "激しい雨が続いています" -> when (language) {
                AppLanguage.ENGLISH -> "Heavy rain continues"
                AppLanguage.KOREAN -> "강한 비가 계속 내리고 있습니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "持续强降雨"
                AppLanguage.TRADITIONAL_CHINESE -> "持續強降雨"
                AppLanguage.SPANISH -> "Continúa la lluvia intensa"
                AppLanguage.FRENCH -> "Forte pluie en cours"
                AppLanguage.PORTUGUESE -> "Chuva forte continua"
                AppLanguage.JAPANESE -> text
            }
            "雨が降り続いています" -> when (language) {
                AppLanguage.ENGLISH -> "Rain continues"
                AppLanguage.KOREAN -> "비가 계속 내리고 있습니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "降雨持续中"
                AppLanguage.TRADITIONAL_CHINESE -> "降雨持續中"
                AppLanguage.SPANISH -> "Sigue lloviendo"
                AppLanguage.FRENCH -> "La pluie continue"
                AppLanguage.PORTUGUESE -> "Continua chovendo"
                AppLanguage.JAPANESE -> text
            }
            "雨雲が接近しています" -> when (language) {
                AppLanguage.ENGLISH -> "Rain clouds approaching"
                AppLanguage.KOREAN -> "비구름이 다가오고 있습니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨云正在靠近"
                AppLanguage.TRADITIONAL_CHINESE -> "雨雲正在靠近"
                AppLanguage.SPANISH -> "Se acercan nubes de lluvia"
                AppLanguage.FRENCH -> "Nuages de pluie en approche"
                AppLanguage.PORTUGUESE -> "Nuvens de chuva se aproximando"
                AppLanguage.JAPANESE -> text
            }
            "今日いっぱいは雨の心配はありません" -> when (language) {
                AppLanguage.ENGLISH -> "No rain expected today"
                AppLanguage.KOREAN -> "오늘은 비 걱정이 없습니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "今天无需担心下雨"
                AppLanguage.TRADITIONAL_CHINESE -> "今天無須擔心下雨"
                AppLanguage.SPANISH -> "No se espera lluvia hoy"
                AppLanguage.FRENCH -> "Pas de pluie prévue aujourd'hui"
                AppLanguage.PORTUGUESE -> "Sem previsão de chuva hoje"
                AppLanguage.JAPANESE -> text
            }
            "当面、雨の心配はありません" -> when (language) {
                AppLanguage.ENGLISH -> "No rain expected for now"
                AppLanguage.KOREAN -> "당분간 비 걱정이 없습니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "近期无需担心下雨"
                AppLanguage.TRADITIONAL_CHINESE -> "近期無須擔心下雨"
                AppLanguage.SPANISH -> "No se espera lluvia por ahora"
                AppLanguage.FRENCH -> "Pas de pluie prévue pour le moment"
                AppLanguage.PORTUGUESE -> "Sem previsão de chuva por enquanto"
                AppLanguage.JAPANESE -> text
            }
            else -> {
                // Regex patterns
                val heavyStartMatch = Regex("""^(.+)頃から激しい雨が降ります$""").find(text)
                if (heavyStartMatch != null) {
                    val rawTime = heavyStartMatch.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Heavy rain starting around $time"
                        AppLanguage.KOREAN -> "${time}경부터 강한 비가 내릴 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右将下大雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右將下大雨"
                        AppLanguage.SPANISH -> "Lluvia intensa a partir de las $time aprox."
                        AppLanguage.FRENCH -> "Forte pluie prévue vers $time"
                        AppLanguage.PORTUGUESE -> "Chuva forte começando por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainStartMatch1 = Regex("""^(.+)頃から雨が降り始めます$""").find(text)
                if (rainStartMatch1 != null) {
                    val rawTime = rainStartMatch1.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain starting around $time"
                        AppLanguage.KOREAN -> "${time}경부터 비가 내리기 시작합니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始下雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始下雨"
                        AppLanguage.SPANISH -> "Empezará a llover sobre las $time"
                        AppLanguage.FRENCH -> "Début de la pluie vers $time"
                        AppLanguage.PORTUGUESE -> "Chuva começando por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainStartMatch2 = Regex("""^(.+)頃から雨が降り始める見込みです$""").find(text)
                if (rainStartMatch2 != null) {
                    val rawTime = rainStartMatch2.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain expected around $time"
                        AppLanguage.KOREAN -> "${time}경부터 비가 내릴 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始下雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始下雨"
                        AppLanguage.SPANISH -> "Se espera lluvia sobre las $time"
                        AppLanguage.FRENCH -> "Pluie attendue vers $time"
                        AppLanguage.PORTUGUESE -> "Previsão de chuva por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainStartMatch3 = Regex("""^(.+)頃から雨が降り出す見込みです$""").find(text)
                if (rainStartMatch3 != null) {
                    val rawTime = rainStartMatch3.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain expected around $time"
                        AppLanguage.KOREAN -> "${time}경부터 비가 올 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始下雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始下雨"
                        AppLanguage.SPANISH -> "Se espera lluvia sobre las $time"
                        AppLanguage.FRENCH -> "Pluie attendue vers $time"
                        AppLanguage.PORTUGUESE -> "Previsão de chuva por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainIntensifyMatch = Regex("""^(.+)頃から雨がさらに強まる見込みです$""").find(text)
                if (rainIntensifyMatch != null) {
                    val rawTime = rainIntensifyMatch.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain intensifying around $time"
                        AppLanguage.KOREAN -> "${time}경부터 빗줄기가 더 강해질 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨势将加剧"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨勢將加劇"
                        AppLanguage.SPANISH -> "La lluvia se intensificará sobre las $time"
                        AppLanguage.FRENCH -> "Renforcement de la pluie vers $time"
                        AppLanguage.PORTUGUESE -> "Chuva ficando mais forte por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val peakMatch = Regex("""^(.+)頃に雨のピークを迎えます$""").find(text)
                if (peakMatch != null) {
                    val rawTime = peakMatch.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain peak expected around $time"
                        AppLanguage.KOREAN -> "${time}경 비가 가장 강할 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右迎来降雨高峰"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右迎來降雨高峰"
                        AppLanguage.SPANISH -> "Pico de lluvia previsto sobre las $time"
                        AppLanguage.FRENCH -> "Pic de pluie attendu vers $time"
                        AppLanguage.PORTUGUESE -> "Pico de chuva previsto por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val stopMatch1 = Regex("""^(.+)頃に雨が止む見込みです$""").find(text)
                if (stopMatch1 != null) {
                    val rawTime = stopMatch1.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain stopping around $time"
                        AppLanguage.KOREAN -> "${time}경 비가 그칠 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨停"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨停"
                        AppLanguage.SPANISH -> "Dejará de llover sobre las $time"
                        AppLanguage.FRENCH -> "Fin de la pluie vers $time"
                        AppLanguage.PORTUGUESE -> "A chuva deve parar por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val stopMatch2 = Regex("""^(.+)頃に雨が止む予想です$""").find(text)
                if (stopMatch2 != null) {
                    val rawTime = stopMatch2.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain stopping around $time"
                        AppLanguage.KOREAN -> "${time}경 비가 그칠 예상입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨停"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨停"
                        AppLanguage.SPANISH -> "Dejará de llover sobre las $time"
                        AppLanguage.FRENCH -> "Fin de la pluie vers $time"
                        AppLanguage.PORTUGUESE -> "A chuva deve parar por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val continueUntilMatch = Regex("""^(.+)頃まで雨が続く見込みです$""").find(text)
                if (continueUntilMatch != null) {
                    val rawTime = continueUntilMatch.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain continuing until around $time"
                        AppLanguage.KOREAN -> "${time}경까지 비가 이어질 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计降雨持续至 $time 左右"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計降雨持續至 $time 左右"
                        AppLanguage.SPANISH -> "La lluvia continuará hasta las $time aprox."
                        AppLanguage.FRENCH -> "La pluie continue jusqu'à environ $time"
                        AppLanguage.PORTUGUESE -> "A chuva continua até por volta das $time"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val continueHoursMatch = Regex("""^雨は当面の間（今後(\d+)時間）降り続く見込みです$""").find(text)
                if (continueHoursMatch != null) {
                    val hours = continueHoursMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain will continue for the next $hours hours"
                        AppLanguage.KOREAN -> "앞으로 ${hours}시간 동안 비가 계속될 전망입니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计未来 $hours 小时持续有雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計未來 $hours 小時持續有雨"
                        AppLanguage.SPANISH -> "La lluvia continuará durante las próximas $hours horas"
                        AppLanguage.FRENCH -> "La pluie va continuer pendant les $hours prochaines heures"
                        AppLanguage.PORTUGUESE -> "A chuva deve continuar pelas próximas $hours horas"
                        AppLanguage.JAPANESE -> text
                    }
                }

                null
            }
        }
    }

    fun translateSubText(text: String, language: AppLanguage): String? {
        if (language == AppLanguage.JAPANESE) return text
        if (text.isBlank()) return text

        return when {
            text == "気象庁予報：今夜遅くにかけて所により雨が降る見込みです。お帰りの際はご注意ください。" -> when (language) {
                AppLanguage.ENGLISH -> "JMA Forecast: Rain expected in some areas late tonight. Carry an umbrella when heading home."
                AppLanguage.KOREAN -> "기상청 예보: 오늘 밤늦게 곳곳에 비가 내릴 전망입니다. 귀가 시 우산을 챙기세요."
                AppLanguage.SIMPLIFIED_CHINESE -> "气象厅预报：今晚局部地区有雨，晚归请注意携带雨伞。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣象廳預報：今晚局部地區有雨，晚歸請注意攜帶雨傘。"
                AppLanguage.SPANISH -> "Pronóstico JMA: Se esperan lluvias dispersas a última hora de la noche. Lleva paraguas al volver."
                AppLanguage.FRENCH -> "Prévision JMA : Pluies éparses attendues tard ce soir. Prévoyez un parapluie pour le retour."
                AppLanguage.PORTUGUESE -> "Previsão JMA: Chuva isolada prevista para o fim da noite. Leve guarda-chuva ao voltar para casa."
                AppLanguage.JAPANESE -> text
            }
            text == "今後6時間以上、まとまった雨が降り続く予想です。お足元や視界の悪化にご注意ください。" -> when (language) {
                AppLanguage.ENGLISH -> "Steady rain expected to continue for the next 6+ hours. Take care on wet roads."
                AppLanguage.KOREAN -> "앞으로 6시간 이상 비가 계속 내릴 전망입니다. 빗길 미끄럼과 시야 악화에 주의하세요."
                AppLanguage.SIMPLIFIED_CHINESE -> "预计未来6小时以上持续有雨，外出请注意路面湿滑。"
                AppLanguage.TRADITIONAL_CHINESE -> "預計未來6小時以上持續有雨，外出請注意路面濕滑。"
                AppLanguage.SPANISH -> "Se espera lluvia constante durante más de 6 horas. Extrema la precaución en calzadas mojadas."
                AppLanguage.FRENCH -> "Pluie régulière attendue pendant plus de 6 heures. Soyez prudent sur les routes glissantes."
                AppLanguage.PORTUGUESE -> "Chuva constante prevista pelas próximas 6+ horas. Tenha cuidado em vias molhadas."
                AppLanguage.JAPANESE -> text
            }
            text == "当面雨は降らない見込みです。お出かけやお洗濯にも安心です。" -> when (language) {
                AppLanguage.ENGLISH -> "Clear conditions ahead. Great weather for going out or laundry."
                AppLanguage.KOREAN -> "당분간 비 소식이 없습니다. 외출이나 빨래하기 좋은 날씨입니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "预计近期无雨，适合外出与晾晒。"
                AppLanguage.TRADITIONAL_CHINESE -> "預計近期無雨，適合外出與晾曬。"
                AppLanguage.SPANISH -> "Sin lluvias a la vista. Buen tiempo para salir o tender la ropa."
                AppLanguage.FRENCH -> "Temps sec à venir. Idéal pour sortir ou étendre le linge."
                AppLanguage.PORTUGUESE -> "Sem chuva à vista. Ótimo tempo para sair ou estender roupas."
                AppLanguage.JAPANESE -> text
            }
            else -> {
                // Tier 4 Forecast with % (JMA / NOAA / Generic)
                val popMatch = Regex("""^(気象庁予報|NOAA/NWS予報|ECMWF予報|国際数値予報|気象予報)：降水確率(\d+)%の雨が予想されています。明日の外出時は雨具をご準備ください。$""").find(text)
                if (popMatch != null) {
                    val source = popMatch.groupValues[1]
                    val pop = popMatch.groupValues[2]
                    val sourceKo = when (source) {
                        "気象庁予報" -> "기상청 예보"
                        "NOAA/NWS予報" -> "NOAA/NWS 예보"
                        "ECMWF予報" -> "ECMWF 예보"
                        "国際数値予報" -> "글로벌 수치예보"
                        else -> "기상 예보"
                    }
                    val (sourceEn, sourceZhCn, sourceZhTw) = when (source) {
                        "気象庁予報" -> Triple("JMA Forecast", "气象厅预报", "氣象廳預報")
                        "NOAA/NWS予報" -> Triple("NOAA/NWS Forecast", "NOAA/NWS预报", "NOAA/NWS預報")
                        "ECMWF予報" -> Triple("ECMWF Forecast", "ECMWF预报", "ECMWF預報")
                        "国際数値予報" -> Triple("Global Forecast", "全球数值预报", "全球數值預報")
                        else -> Triple("Weather Forecast", "天气预报", "天氣預報")
                    }
                    val (sourceEs, sourceFr, sourcePt) = when (source) {
                        "気象庁予報" -> Triple("Pronóstico JMA", "Prévision JMA", "Previsão JMA")
                        "NOAA/NWS予報" -> Triple("Pronóstico NOAA/NWS", "Prévision NOAA/NWS", "Previsão NOAA/NWS")
                        "ECMWF予報" -> Triple("Pronóstico ECMWF", "Prévision ECMWF", "Previsão ECMWF")
                        "国際数値予報" -> Triple("Pronóstico global", "Prévision mondiale", "Previsão global")
                        else -> Triple("Pronóstico del tiempo", "Prévision météo", "Previsão do tempo")
                    }
                    return when (language) {
                        AppLanguage.ENGLISH -> "$sourceEn: ${pop}% chance of rain expected. Prepare rain gear for tomorrow."
                        AppLanguage.KOREAN -> "$sourceKo: 강수확률 ${pop}%의 비가 예상됩니다. 내일 외출 시 우산을 준비하세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "$sourceZhCn：预计降水概率为 ${pop}%，明天出门请备好雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "$sourceZhTw：預計降水機率為 ${pop}%，明天出門請備好雨具。"
                        AppLanguage.SPANISH -> "$sourceEs: ${pop}% de probabilidad de lluvia. Prepara ropa de lluvia para mañana."
                        AppLanguage.FRENCH -> "$sourceFr : ${pop} % de risque de pluie. Prévoyez de quoi vous protéger demain."
                        AppLanguage.PORTUGUESE -> "$sourcePt: ${pop}% de chance de chuva. Prepare capa ou guarda-chuva para amanhã."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Tier 3 Pop tonight
                val tonightPopMatch = Regex("""^今夜にかけてまとまった雨雲が広がる予想です（降水確率(\d+)%）。夜間のお帰りの際は傘をお持ちください。$""").find(text)
                if (tonightPopMatch != null) {
                    val pop = tonightPopMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain clouds expected tonight (${pop}% chance). Don't forget an umbrella for tonight."
                        AppLanguage.KOREAN -> "오늘 밤 비구름이 확대될 전망입니다(강수확률 ${pop}%). 귀가 시 우산을 챙기세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "今晚可能有降雨（降水概率 ${pop}%），晚归请备好雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "今晚可能有降雨（降水機率 ${pop}%），晚歸請備好雨具。"
                        AppLanguage.SPANISH -> "Se esperan nubes de lluvia esta noche (${pop}% de prob.). No olvides el paraguas."
                        AppLanguage.FRENCH -> "Nuages pluvieux attendus ce soir (${pop} % de risque). N'oubliez pas votre parapluie."
                        AppLanguage.PORTUGUESE -> "Nuvens de chuva previstas para esta noite (${pop}% de chance). Não esqueça o guarda-chuva."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Heavy rain approaching
                val heavyApproachingMatch = Regex("""^強い雨雲が迫っています（予想雨量 約([\d.]+)mm/h）。安全な屋内へ避難してください。$""").find(text)
                if (heavyApproachingMatch != null) {
                    val mm = heavyApproachingMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Intense rain clouds approaching (est. ~$mm mm/h). Seek shelter indoors."
                        AppLanguage.KOREAN -> "강한 비구름이 다가오고 있습니다(예상 강수량 약 $mm mm/h). 안전한 실내로 대피하세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "强降雨云正在靠近（预计雨量约 $mm mm/h），请及时避雨。"
                        AppLanguage.TRADITIONAL_CHINESE -> "強降雨雲正在靠近（預計雨量約 $mm mm/h），請及時避雨。"
                        AppLanguage.SPANISH -> "Se acercan nubes de lluvia intensa (aprox. $mm mm/h). Busca refugio en interiores."
                        AppLanguage.FRENCH -> "Nuages de forte pluie en approche (env. $mm mm/h). Mettez-vous à l'abri."
                        AppLanguage.PORTUGUESE -> "Nuvens de chuva forte se aproximando (aprox. $mm mm/h). Abrigue-se em local seguro."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Rain starts in X min (short)
                val rainStartSoonMatch = Regex("""^あと約(\d+)分で雨が降り始める見込みです。お出かけの際は傘をご用意ください。$""").find(text)
                if (rainStartSoonMatch != null) {
                    val min = rainStartSoonMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain is expected in about $min min. Have an umbrella ready."
                        AppLanguage.KOREAN -> "약 ${min}분 후 비가 내리기 시작할 전망입니다. 외출 시 우산을 준비하세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计约 $min 分钟后开始下雨，出门请备好雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計約 $min 分鐘後開始下雨，出門請備好雨具。"
                        AppLanguage.SPANISH -> "Se espera lluvia en unos $min min. Ten un paraguas a mano."
                        AppLanguage.FRENCH -> "Pluie attendue dans environ $min min. Préparez votre parapluie."
                        AppLanguage.PORTUGUESE -> "Previsão de chuva em cerca de $min min. Deixe o guarda-chuva pronto."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Rain starts in X min (folding umbrella)
                val rainStartFoldMatch = Regex("""^あと約(\d+)分で雨が降り始める見込みです。折りたたみ傘があると安心です。$""").find(text)
                if (rainStartFoldMatch != null) {
                    val min = rainStartFoldMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain is expected in about $min min. An umbrella is recommended."
                        AppLanguage.KOREAN -> "약 ${min}분 후 비가 내리기 시작할 전망입니다. 접이식 우산을 챙기면 좋습니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "距降雨约 $min 分钟，出门建议携带雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "距降雨約 $min 分鐘，出門建議攜帶雨具。"
                        AppLanguage.SPANISH -> "Se espera lluvia en unos $min min. Se recomienda llevar paraguas."
                        AppLanguage.FRENCH -> "Pluie attendue dans environ $min min. Un parapluie est conseillé."
                        AppLanguage.PORTUGUESE -> "Previsão de chuva em cerca de $min min. Recomenda-se levar guarda-chuva."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Rain start 1-6h
                val rainStart1to6hMatch = Regex("""^(.+)頃から雨が降り始める予想です（予想雨量 約([\d.]+)mm/h）。お出かけの際は雨具をお持ちください。$""").find(text)
                if (rainStart1to6hMatch != null) {
                    val rawTime = rainStart1to6hMatch.groupValues[1]
                    val mm = rainStart1to6hMatch.groupValues[2]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain expected around $time (~$mm mm/h). Bring an umbrella if heading out."
                        AppLanguage.KOREAN -> "${time}경부터 비가 내릴 예상입니다(예상 약 $mm mm/h). 외출 시 우산을 챙기세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始降雨（预计约 $mm mm/h），外出请带伞。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始降雨（預計約 $mm mm/h），外出請帶傘。"
                        AppLanguage.SPANISH -> "Se espera lluvia sobre las $time (aprox. $mm mm/h). Lleva paraguas si vas a salir."
                        AppLanguage.FRENCH -> "Pluie attendue vers $time (env. $mm mm/h). Emportez un parapluie si vous sortez."
                        AppLanguage.PORTUGUESE -> "Previsão de chuva por volta das $time (aprox. $mm mm/h). Leve guarda-chuva se for sair."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Currently heavy rain
                val currentlyHeavyMatch = Regex("""^現在、激しい雨が降っています（現在雨量 約([\d.]+)mm/h）。不要不急の外出を控えてください。$""").find(text)
                if (currentlyHeavyMatch != null) {
                    val mm = currentlyHeavyMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Intense rain currently falling (~$mm mm/h). Avoid unnecessary outings."
                        AppLanguage.KOREAN -> "현재 강한 비가 내리고 있습니다(현재 약 $mm mm/h). 불필요한 외출을 삼가세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "目前正下大雨（当前雨量约 $mm mm/h），请尽量避免不必要的外出。"
                        AppLanguage.TRADITIONAL_CHINESE -> "目前正下大雨（當前雨量約 $mm mm/h），請盡量避免不必要的外出。"
                        AppLanguage.SPANISH -> "Está lloviendo intensamente (aprox. $mm mm/h). Evita desplazamientos innecesarios."
                        AppLanguage.FRENCH -> "Forte pluie en cours (env. $mm mm/h). Évitez les déplacements inutiles."
                        AppLanguage.PORTUGUESE -> "Chuva intensa no momento (aprox. $mm mm/h). Evite sair sem necessidade."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Intensifying to peak
                val intensifyMatch = Regex("""^現在より雨脚が強まり、激しい雨（約([\d.]+)mm/h）となる恐れがあります。視界悪化にご注意ください。$""").find(text)
                if (intensifyMatch != null) {
                    val mm = intensifyMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain will become heavier (est. ~$mm mm/h). Watch out for poor visibility."
                        AppLanguage.KOREAN -> "지금보다 빗줄기가 강해져 강한 비(약 $mm mm/h)가 내릴 수 있습니다. 시야 악화에 주의하세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "雨势将进一步加大（约 $mm mm/h），请注意低能见度与安全。"
                        AppLanguage.TRADITIONAL_CHINESE -> "雨勢將進一步加大（約 $mm mm/h），請注意低能見度與安全。"
                        AppLanguage.SPANISH -> "La lluvia se intensificará (aprox. $mm mm/h). Precaución por baja visibilidad."
                        AppLanguage.FRENCH -> "La pluie va s'intensifier (env. $mm mm/h). Attention à la baisse de visibilité."
                        AppLanguage.PORTUGUESE -> "A chuva vai ficar mais forte (aprox. $mm mm/h). Atenção à baixa visibilidade."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Peak in X min
                val peakInMinMatch = Regex("""^あと約(\d+)分で雨のピーク（予想雨量 約([\d.]+)mm/h）に達する見込みです。$""").find(text)
                if (peakInMinMatch != null) {
                    val min = peakInMinMatch.groupValues[1]
                    val mm = peakInMinMatch.groupValues[2]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain peak expected in about $min min (~$mm mm/h)."
                        AppLanguage.KOREAN -> "약 ${min}분 후 비가 가장 강해질 전망입니다(예상 약 $mm mm/h)."
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计约 $min 分钟后达到降雨高峰（预计雨量约 $mm mm/h）。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計約 $min 分鐘後達到降雨高峰（預計雨量約 $mm mm/h）。"
                        AppLanguage.SPANISH -> "Pico de lluvia previsto en unos $min min (aprox. $mm mm/h)."
                        AppLanguage.FRENCH -> "Pic de pluie attendu dans environ $min min (env. $mm mm/h)."
                        AppLanguage.PORTUGUESE -> "Pico de chuva previsto em cerca de $min min (aprox. $mm mm/h)."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Stops in X min
                val stopInMinMatch = Regex("""^雨が止むまであと約(\d+)分です。次第に曇り空へと回復する見込みです。$""").find(text)
                if (stopInMinMatch != null) {
                    val min = stopInMinMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "About $min min until rain stops. Conditions will gradually improve."
                        AppLanguage.KOREAN -> "비가 그치기까지 약 ${min}분 남았습니다. 차차 흐린 날씨로 회복될 전망입니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "距离雨停约 $min 分钟，天气将逐渐好转。"
                        AppLanguage.TRADITIONAL_CHINESE -> "距離雨停約 $min 分鐘，天氣將逐漸好轉。"
                        AppLanguage.SPANISH -> "Quedan unos $min min para que deje de llover. El tiempo mejorará gradualmente."
                        AppLanguage.FRENCH -> "Environ $min min avant la fin de la pluie. Le temps va s'améliorer progressivement."
                        AppLanguage.PORTUGUESE -> "Faltam cerca de $min min para a chuva parar. O tempo deve melhorar gradualmente."
                        AppLanguage.JAPANESE -> text
                    }
                }

                val stopInMinMatch2 = Regex("""^あと約(\d+)分で雨が止む見込みです。傘の置き忘れにご注意ください。$""").find(text)
                if (stopInMinMatch2 != null) {
                    val min = stopInMinMatch2.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain is expected to stop in about $min min. Don't leave your umbrella behind."
                        AppLanguage.KOREAN -> "약 ${min}분 후 비가 그칠 전망입니다. 우산을 두고 내리지 않도록 주의하세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计约 $min 分钟后雨停，请注意别落下雨伞。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計約 $min 分鐘後雨停，請注意別落下雨傘。"
                        AppLanguage.SPANISH -> "Dejará de llover en unos $min min. No olvides tu paraguas."
                        AppLanguage.FRENCH -> "La pluie devrait cesser dans environ $min min. N'oubliez pas votre parapluie."
                        AppLanguage.PORTUGUESE -> "A chuva deve parar em cerca de $min min. Não esqueça seu guarda-chuva."
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Stop around time
                val stopAroundTimeMatch = Regex("""^(.+)頃に雨が止む予想です。お帰りの際は傘のお忘れ物にご注意ください。$""").find(text)
                if (stopAroundTimeMatch != null) {
                    val rawTime = stopAroundTimeMatch.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain expected to stop around $time. Be sure not to forget your umbrella."
                        AppLanguage.KOREAN -> "${time}경 비가 그칠 예상입니다. 우산을 챙기는 것을 잊지 마세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨停，外出请注意不要遗忘雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨停，外出請注意不要遺忘雨具。"
                        AppLanguage.SPANISH -> "Se espera que deje de llover sobre las $time. Recuerda no olvidar tu paraguas."
                        AppLanguage.FRENCH -> "Fin de la pluie prévue vers $time. Pensez à ne pas oublier votre parapluie."
                        AppLanguage.PORTUGUESE -> "A chuva deve parar por volta das $time. Lembre-se de não esquecer o guarda-chuva."
                        AppLanguage.JAPANESE -> text
                    }
                }

                null
            }
        }
    }

    fun translateTimeLabel(label: String, language: AppLanguage): String {
        if (language == AppLanguage.JAPANESE) return label
        return when (label) {
            "明日未明" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow before dawn"
                AppLanguage.KOREAN -> "내일 새벽"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天凌晨"
                AppLanguage.TRADITIONAL_CHINESE -> "明天凌晨"
                AppLanguage.SPANISH -> "Mañana de madrugada"
                AppLanguage.FRENCH -> "Demain avant l'aube"
                AppLanguage.PORTUGUESE -> "Amanhã de madrugada"
                AppLanguage.JAPANESE -> label
            }
            "明日の午前中" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow morning"
                AppLanguage.KOREAN -> "내일 오전"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天上午"
                AppLanguage.TRADITIONAL_CHINESE -> "明天上午"
                AppLanguage.SPANISH -> "Mañana por la mañana"
                AppLanguage.FRENCH -> "Demain matin"
                AppLanguage.PORTUGUESE -> "Amanhã de manhã"
                AppLanguage.JAPANESE -> label
            }
            "明日の午後" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow afternoon"
                AppLanguage.KOREAN -> "내일 오후"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天下午"
                AppLanguage.TRADITIONAL_CHINESE -> "明天下午"
                AppLanguage.SPANISH -> "Mañana por la tarde"
                AppLanguage.FRENCH -> "Demain après-midi"
                AppLanguage.PORTUGUESE -> "Amanhã à tarde"
                AppLanguage.JAPANESE -> label
            }
            "明日の夕方以降" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow evening"
                AppLanguage.KOREAN -> "내일 저녁"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天傍晚"
                AppLanguage.TRADITIONAL_CHINESE -> "明天傍晚"
                AppLanguage.SPANISH -> "Mañana al atardecer"
                AppLanguage.FRENCH -> "Demain en soirée"
                AppLanguage.PORTUGUESE -> "Amanhã ao entardecer"
                AppLanguage.JAPANESE -> label
            }
            "明日の夜" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow night"
                AppLanguage.KOREAN -> "내일 밤"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天夜间"
                AppLanguage.TRADITIONAL_CHINESE -> "明天夜間"
                AppLanguage.SPANISH -> "Mañana por la noche"
                AppLanguage.FRENCH -> "Demain soir"
                AppLanguage.PORTUGUESE -> "Amanhã à noite"
                AppLanguage.JAPANESE -> label
            }
            "当面の間" -> when (language) {
                AppLanguage.ENGLISH -> "Foreseeable"
                AppLanguage.KOREAN -> "당분간"
                AppLanguage.SIMPLIFIED_CHINESE -> "持续中"
                AppLanguage.TRADITIONAL_CHINESE -> "持續中"
                AppLanguage.SPANISH -> "Por el momento"
                AppLanguage.FRENCH -> "Pour le moment"
                AppLanguage.PORTUGUESE -> "Por enquanto"
                AppLanguage.JAPANESE -> label
            }
            "今夜遅く" -> when (language) {
                AppLanguage.ENGLISH -> "Late Tonight"
                AppLanguage.KOREAN -> "오늘 밤늦게"
                AppLanguage.SIMPLIFIED_CHINESE -> "今晚较晚"
                AppLanguage.TRADITIONAL_CHINESE -> "今晚較晚"
                AppLanguage.SPANISH -> "Esta noche tarde"
                AppLanguage.FRENCH -> "Tard ce soir"
                AppLanguage.PORTUGUESE -> "Fim da noite"
                AppLanguage.JAPANESE -> label
            }
            else -> {
                val hourMatch = Regex("""^(\d{1,2})時頃?$""").find(label)
                if (hourMatch != null) {
                    val h = hourMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "$h:00"
                        AppLanguage.KOREAN -> "${h}시"
                        AppLanguage.SIMPLIFIED_CHINESE -> "${h}点"
                        AppLanguage.TRADITIONAL_CHINESE -> "${h}點"
                        AppLanguage.SPANISH -> "$h:00"
                        AppLanguage.FRENCH -> "${h}h00"
                        AppLanguage.PORTUGUESE -> "$h:00"
                        AppLanguage.JAPANESE -> "${h}時"
                    }
                }
                val minMatch = Regex("""^(\d{1,2}:\d{2})頃?$""").find(label)
                if (minMatch != null) {
                    return minMatch.groupValues[1]
                }
                label
            }
        }
    }
}
