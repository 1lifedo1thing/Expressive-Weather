package com.example.expressiveweather.data

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
                AppLanguage.SIMPLIFIED_CHINESE -> "明天傍晚起需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天傍晚起需要帶傘"
                AppLanguage.JAPANESE -> text
            }
            "明日の夜は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow night"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天夜间需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天夜間需要帶傘"
                AppLanguage.JAPANESE -> text
            }
            "明日の午後は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow afternoon"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天下午需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天下午需要帶傘"
                AppLanguage.JAPANESE -> text
            }
            "明日の午前中は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow morning"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天上午需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天上午需要帶傘"
                AppLanguage.JAPANESE -> text
            }
            "明日未明は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow before dawn"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天凌晨需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天凌晨需要帶傘"
                AppLanguage.JAPANESE -> text
            }
            "明日は傘が必要です" -> when (language) {
                AppLanguage.ENGLISH -> "Umbrella needed tomorrow"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天需要带伞"
                AppLanguage.TRADITIONAL_CHINESE -> "明天需要帶傘"
                AppLanguage.JAPANESE -> text
            }
            "今夜は雨が降るでしょう" -> when (language) {
                AppLanguage.ENGLISH -> "Rain expected tonight"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计今晚有雨"
                AppLanguage.TRADITIONAL_CHINESE -> "預計今晚有雨"
                AppLanguage.JAPANESE -> text
            }
            "まもなく雨が降り始めます" -> when (language) {
                AppLanguage.ENGLISH -> "Rain starting shortly"
                AppLanguage.SIMPLIFIED_CHINESE -> "即将开始下雨"
                AppLanguage.TRADITIONAL_CHINESE -> "即將開始下雨"
                AppLanguage.JAPANESE -> text
            }
            "まもなく激しい雨が降ります" -> when (language) {
                AppLanguage.ENGLISH -> "Heavy rain starting shortly"
                AppLanguage.SIMPLIFIED_CHINESE -> "即将下大雨"
                AppLanguage.TRADITIONAL_CHINESE -> "即將下大雨"
                AppLanguage.JAPANESE -> text
            }
            "まもなく雨が止む見込みです" -> when (language) {
                AppLanguage.ENGLISH -> "Rain stopping shortly"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨很快就会停"
                AppLanguage.TRADITIONAL_CHINESE -> "雨很快就會停"
                AppLanguage.JAPANESE -> text
            }
            "雨は当面の間降り続く見込みです" -> when (language) {
                AppLanguage.ENGLISH -> "Rain will continue for the foreseeable future"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计降雨还将持续一段时间"
                AppLanguage.TRADITIONAL_CHINESE -> "預計降雨還將持續一段時間"
                AppLanguage.JAPANESE -> text
            }
            "激しい雨が続いています" -> when (language) {
                AppLanguage.ENGLISH -> "Heavy rain continues"
                AppLanguage.SIMPLIFIED_CHINESE -> "持续强降雨"
                AppLanguage.TRADITIONAL_CHINESE -> "持續強降雨"
                AppLanguage.JAPANESE -> text
            }
            "雨が降り続いています" -> when (language) {
                AppLanguage.ENGLISH -> "Rain continues"
                AppLanguage.SIMPLIFIED_CHINESE -> "降雨持续中"
                AppLanguage.TRADITIONAL_CHINESE -> "降雨持續中"
                AppLanguage.JAPANESE -> text
            }
            "雨雲が接近しています" -> when (language) {
                AppLanguage.ENGLISH -> "Rain clouds approaching"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨云正在靠近"
                AppLanguage.TRADITIONAL_CHINESE -> "雨雲正在靠近"
                AppLanguage.JAPANESE -> text
            }
            "今日いっぱいは雨の心配はありません" -> when (language) {
                AppLanguage.ENGLISH -> "No rain expected today"
                AppLanguage.SIMPLIFIED_CHINESE -> "今天无需担心下雨"
                AppLanguage.TRADITIONAL_CHINESE -> "今天無須擔心下雨"
                AppLanguage.JAPANESE -> text
            }
            "当面、雨の心配はありません" -> when (language) {
                AppLanguage.ENGLISH -> "No rain expected for now"
                AppLanguage.SIMPLIFIED_CHINESE -> "近期无需担心下雨"
                AppLanguage.TRADITIONAL_CHINESE -> "近期無須擔心下雨"
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
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右将下大雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右將下大雨"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainStartMatch1 = Regex("""^(.+)頃から雨が降り始めます$""").find(text)
                if (rainStartMatch1 != null) {
                    val rawTime = rainStartMatch1.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain starting around $time"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始下雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始下雨"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainStartMatch2 = Regex("""^(.+)頃から雨が降り始める見込みです$""").find(text)
                if (rainStartMatch2 != null) {
                    val rawTime = rainStartMatch2.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain expected around $time"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始下雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始下雨"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainStartMatch3 = Regex("""^(.+)頃から雨が降り出す見込みです$""").find(text)
                if (rainStartMatch3 != null) {
                    val rawTime = rainStartMatch3.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain expected around $time"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始下雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始下雨"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val rainIntensifyMatch = Regex("""^(.+)頃から雨がさらに強まる見込みです$""").find(text)
                if (rainIntensifyMatch != null) {
                    val rawTime = rainIntensifyMatch.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain intensifying around $time"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨势将加剧"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨勢將加劇"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val peakMatch = Regex("""^(.+)頃に雨のピークを迎えます$""").find(text)
                if (peakMatch != null) {
                    val rawTime = peakMatch.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain peak expected around $time"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右迎来降雨高峰"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右迎來降雨高峰"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val stopMatch1 = Regex("""^(.+)頃に雨が止む見込みです$""").find(text)
                if (stopMatch1 != null) {
                    val rawTime = stopMatch1.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain stopping around $time"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨停"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨停"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val stopMatch2 = Regex("""^(.+)頃に雨が止む予想です$""").find(text)
                if (stopMatch2 != null) {
                    val rawTime = stopMatch2.groupValues[1]
                    val time = translateTimeLabel(rawTime, language)
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain stopping around $time"
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨停"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨停"
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
                AppLanguage.SIMPLIFIED_CHINESE -> "气象厅预报：今晚局部地区有雨，晚归请注意携带雨伞。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣象廳預報：今晚局部地區有雨，晚歸請注意攜帶雨傘。"
                AppLanguage.JAPANESE -> text
            }
            text == "今後6時間以上、まとまった雨が降り続く予想です。お足元や視界の悪化にご注意ください。" -> when (language) {
                AppLanguage.ENGLISH -> "Steady rain expected to continue for the next 6+ hours. Take care on wet roads."
                AppLanguage.SIMPLIFIED_CHINESE -> "预计未来6小时以上持续有雨，外出请注意路面湿滑。"
                AppLanguage.TRADITIONAL_CHINESE -> "預計未來6小時以上持續有雨，外出請注意路面濕滑。"
                AppLanguage.JAPANESE -> text
            }
            text == "当面雨は降らない見込みです。お出かけやお洗濯にも安心です。" -> when (language) {
                AppLanguage.ENGLISH -> "Clear conditions ahead. Great weather for going out or laundry."
                AppLanguage.SIMPLIFIED_CHINESE -> "预计近期无雨，适合外出与晾晒。"
                AppLanguage.TRADITIONAL_CHINESE -> "預計近期無雨，適合外出與晾曬。"
                AppLanguage.JAPANESE -> text
            }
            else -> {
                // Tier 4 Forecast with % (JMA / NOAA / Generic)
                val popMatch = Regex("""^(気象庁予報|NOAA/NWS予報|気象予報)：降水確率(\d+)%の雨が予想されています。明日の外出時は雨具をご準備ください。$""").find(text)
                if (popMatch != null) {
                    val source = popMatch.groupValues[1]
                    val pop = popMatch.groupValues[2]
                    val (sourceEn, sourceZhCn, sourceZhTw) = when (source) {
                        "気象庁予報" -> Triple("JMA Forecast", "气象厅预报", "氣象廳預報")
                        "NOAA/NWS予報" -> Triple("NOAA/NWS Forecast", "NOAA/NWS预报", "NOAA/NWS預報")
                        else -> Triple("Weather Forecast", "天气预报", "天氣預報")
                    }
                    return when (language) {
                        AppLanguage.ENGLISH -> "$sourceEn: ${pop}% chance of rain expected. Prepare rain gear for tomorrow."
                        AppLanguage.SIMPLIFIED_CHINESE -> "$sourceZhCn：预计降水概率为 ${pop}%，明天出门请备好雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "$sourceZhTw：預計降水機率為 ${pop}%，明天出門請備好雨具。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Tier 3 Pop tonight
                val tonightPopMatch = Regex("""^今夜にかけてまとまった雨雲が広がる予想です（降水確率(\d+)%）。夜間のお帰りの際は傘をお持ちください。$""").find(text)
                if (tonightPopMatch != null) {
                    val pop = tonightPopMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain clouds expected tonight (${pop}% chance). Don't forget an umbrella for tonight."
                        AppLanguage.SIMPLIFIED_CHINESE -> "今晚可能有降雨（降水概率 ${pop}%），晚归请备好雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "今晚可能有降雨（降水機率 ${pop}%），晚歸請備好雨具。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Heavy rain approaching
                val heavyApproachingMatch = Regex("""^強い雨雲が迫っています（予想雨量 約([\d.]+)mm/h）。安全な屋内へ避難してください。$""").find(text)
                if (heavyApproachingMatch != null) {
                    val mm = heavyApproachingMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Intense rain clouds approaching (est. ~$mm mm/h). Seek shelter indoors."
                        AppLanguage.SIMPLIFIED_CHINESE -> "强降雨云正在靠近（预计雨量约 $mm mm/h），请及时避雨。"
                        AppLanguage.TRADITIONAL_CHINESE -> "強降雨雲正在靠近（預計雨量約 $mm mm/h），請及時避雨。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Rain starts in X min (short)
                val rainStartSoonMatch = Regex("""^あと約(\d+)分で雨が降り始める見込みです。お出かけの際は傘をご用意ください。$""").find(text)
                if (rainStartSoonMatch != null) {
                    val min = rainStartSoonMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain is expected in about $min min. Have an umbrella ready."
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计约 $min 分钟后开始下雨，出门请备好雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計約 $min 分鐘後開始下雨，出門請備好雨具。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Rain starts in X min (folding umbrella)
                val rainStartFoldMatch = Regex("""^あと約(\d+)分で雨が降り始める見込みです。折りたたみ傘があると安心です。$""").find(text)
                if (rainStartFoldMatch != null) {
                    val min = rainStartFoldMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain is expected in about $min min. An umbrella is recommended."
                        AppLanguage.SIMPLIFIED_CHINESE -> "距降雨约 $min 分钟，出门建议携带雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "距降雨約 $min 分鐘，出門建議攜帶雨具。"
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
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右开始降雨（预计约 $mm mm/h），外出请带伞。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右開始降雨（預計約 $mm mm/h），外出請帶傘。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Currently heavy rain
                val currentlyHeavyMatch = Regex("""^現在、激しい雨が降っています（現在雨量 約([\d.]+)mm/h）。不要不急の外出を控えてください。$""").find(text)
                if (currentlyHeavyMatch != null) {
                    val mm = currentlyHeavyMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Intense rain currently falling (~$mm mm/h). Avoid unnecessary outings."
                        AppLanguage.SIMPLIFIED_CHINESE -> "目前正下大雨（当前雨量约 $mm mm/h），请尽量避免不必要的外出。"
                        AppLanguage.TRADITIONAL_CHINESE -> "目前正下大雨（當前雨量約 $mm mm/h），請盡量避免不必要的外出。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Intensifying to peak
                val intensifyMatch = Regex("""^現在より雨脚が強まり、激しい雨（約([\d.]+)mm/h）となる恐れがあります。視界悪化にご注意ください。$""").find(text)
                if (intensifyMatch != null) {
                    val mm = intensifyMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain will become heavier (est. ~$mm mm/h). Watch out for poor visibility."
                        AppLanguage.SIMPLIFIED_CHINESE -> "雨势将进一步加大（约 $mm mm/h），请注意低能见度与安全。"
                        AppLanguage.TRADITIONAL_CHINESE -> "雨勢將進一步加大（約 $mm mm/h），請注意低能見度與安全。"
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
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计约 $min 分钟后达到降雨高峰（预计雨量约 $mm mm/h）。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計約 $min 分鐘後達到降雨高峰（預計雨量約 $mm mm/h）。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                // Stops in X min
                val stopInMinMatch = Regex("""^雨が止むまであと約(\d+)分です。次第に曇り空へと回復する見込みです。$""").find(text)
                if (stopInMinMatch != null) {
                    val min = stopInMinMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "About $min min until rain stops. Conditions will gradually improve."
                        AppLanguage.SIMPLIFIED_CHINESE -> "距离雨停约 $min 分钟，天气将逐渐好转。"
                        AppLanguage.TRADITIONAL_CHINESE -> "距離雨停約 $min 分鐘，天氣將逐漸好轉。"
                        AppLanguage.JAPANESE -> text
                    }
                }

                val stopInMinMatch2 = Regex("""^あと約(\d+)分で雨が止む見込みです。傘の置き忘れにご注意ください。$""").find(text)
                if (stopInMinMatch2 != null) {
                    val min = stopInMinMatch2.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "Rain is expected to stop in about $min min. Don't leave your umbrella behind."
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计约 $min 分钟后雨停，请注意别落下雨伞。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計約 $min 分鐘後雨停，請注意別落下雨傘。"
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
                        AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 左右雨停，外出请注意不要遗忘雨具。"
                        AppLanguage.TRADITIONAL_CHINESE -> "預計 $time 左右雨停，外出請注意不要遺忘雨具。"
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
                AppLanguage.SIMPLIFIED_CHINESE -> "明天凌晨"
                AppLanguage.TRADITIONAL_CHINESE -> "明天凌晨"
                AppLanguage.JAPANESE -> label
            }
            "明日の午前中" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow morning"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天上午"
                AppLanguage.TRADITIONAL_CHINESE -> "明天上午"
                AppLanguage.JAPANESE -> label
            }
            "明日の午後" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow afternoon"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天下午"
                AppLanguage.TRADITIONAL_CHINESE -> "明天下午"
                AppLanguage.JAPANESE -> label
            }
            "明日の夕方以降" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow evening"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天傍晚"
                AppLanguage.TRADITIONAL_CHINESE -> "明天傍晚"
                AppLanguage.JAPANESE -> label
            }
            "明日の夜" -> when (language) {
                AppLanguage.ENGLISH -> "Tomorrow night"
                AppLanguage.SIMPLIFIED_CHINESE -> "明天夜间"
                AppLanguage.TRADITIONAL_CHINESE -> "明天夜間"
                AppLanguage.JAPANESE -> label
            }
            "当面の間" -> when (language) {
                AppLanguage.ENGLISH -> "Foreseeable"
                AppLanguage.SIMPLIFIED_CHINESE -> "持续中"
                AppLanguage.TRADITIONAL_CHINESE -> "持續中"
                AppLanguage.JAPANESE -> label
            }
            "今夜遅く" -> when (language) {
                AppLanguage.ENGLISH -> "Late Tonight"
                AppLanguage.SIMPLIFIED_CHINESE -> "今晚较晚"
                AppLanguage.TRADITIONAL_CHINESE -> "今晚較晚"
                AppLanguage.JAPANESE -> label
            }
            else -> {
                val hourMatch = Regex("""^(\d{1,2})時頃?$""").find(label)
                if (hourMatch != null) {
                    val h = hourMatch.groupValues[1]
                    return when (language) {
                        AppLanguage.ENGLISH -> "$h:00"
                        AppLanguage.SIMPLIFIED_CHINESE -> "${h}点"
                        AppLanguage.TRADITIONAL_CHINESE -> "${h}點"
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
