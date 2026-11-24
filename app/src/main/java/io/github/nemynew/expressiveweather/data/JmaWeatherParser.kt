package io.github.nemynew.expressiveweather.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 気象庁（JMA）の予報 JSON・アメダス観測 JSON・概況 JSON を統合し、
 * アプリの [WeatherData] に高精度変換するパーサー
 */
object JmaWeatherParser {

    /**
     * 標準日周気温変化の正規化係数 (0.0 = 未明05:00最低, 1.0 = 午後14:00最高)
     */
    internal fun computeDiurnalFraction(hourOfDay: Int): Double {
        val h = ((hourOfDay % 24) + 24) % 24
        return if (h in 5..14) {
            (1.0 - cos(Math.PI * (h - 5.0) / 9.0)) / 2.0
        } else {
            val hoursSincePeak = if (h >= 14) h - 14 else h + 10
            (1.0 + cos(Math.PI * hoursSincePeak / 15.0)) / 2.0
        }
    }

    fun parse(
        city: City,
        forecastArray: JSONArray,
        amedasJson: JSONObject?,
        overviewJson: JSONObject?,
        warningJson: JSONObject?,
        supplementaryAqiJson: JSONObject? = null,
        supplementaryHighResJson: JSONObject? = null,
        jmaMinutelySeries: List<Pair<Long, Double>>? = null,
        kikikuruRisk: JmaPointKikikuruRisk? = null
    ): WeatherData {
        // =========================================================
        // 1. アメダス観測データの抽出 (リアルタイム実測値)
        // =========================================================
        var currentTemp = 20
        var humidity = 65
        var pressure = 1013
        var windSpeedMs = 2.5
        var windDegree = 90.0f
        var windDirectionText = "東"
        var windGustsKmH = 12.0
        var precip1h = 0.0
        var precip24h = 0.0
        var amedasObservedPrecip10m: Double? = null
        var amedasObservedPrecip1h: Double? = null
        var observedMax: Int? = null
        var observedMin: Int? = null

        if (amedasJson != null && amedasJson.length() > 0) {
            val sortedKeys = amedasJson.keys().asSequence().toList().sorted()
            if (sortedKeys.isNotEmpty()) {
                val latestKey = sortedKeys.last()
                val latestObj = amedasJson.optJSONObject(latestKey)
                if (latestObj != null) {
                    val tVal = latestObj.optJSONArray("temp")?.optDouble(0)
                    if (tVal != null && !tVal.isNaN()) currentTemp = tVal.roundToInt()

                    val hVal = latestObj.optJSONArray("humidity")?.optInt(0)
                    if (hVal != null && hVal > 0) humidity = hVal

                    val pNormal = latestObj.optJSONArray("normalPressure")?.optDouble(0)
                    val pStation = latestObj.optJSONArray("pressure")?.optDouble(0)
                    if (pNormal != null && !pNormal.isNaN()) {
                        pressure = pNormal.roundToInt()
                    } else if (pStation != null && !pStation.isNaN()) {
                        pressure = pStation.roundToInt()
                    }

                    val wVal = latestObj.optJSONArray("wind")?.optDouble(0)
                    if (wVal != null && !wVal.isNaN()) windSpeedMs = wVal

                    val wDir = latestObj.optJSONArray("windDirection")?.optInt(0) ?: 0
                    windDirectionText = getWindDirection16Text(wDir)
                    windDegree = wDir * 22.5f

                    val gVal = latestObj.optJSONArray("gust")?.optDouble(0)
                    if (gVal != null && !gVal.isNaN()) {
                        windGustsKmH = gVal * 3.6
                    } else {
                        windGustsKmH = windSpeedMs * 3.6 * 1.35
                    }

                    val p10Raw = latestObj.optJSONArray("precipitation10m")?.optDouble(0, Double.NaN)?.takeIf { !it.isNaN() }
                    val p1Raw = latestObj.optJSONArray("precipitation1h")?.optDouble(0, Double.NaN)?.takeIf { !it.isNaN() }
                    amedasObservedPrecip10m = p10Raw
                    amedasObservedPrecip1h = p1Raw
                    precip1h = p1Raw ?: ((p10Raw ?: 0.0) * 6.0)

                    val p24 = latestObj.optJSONArray("precipitation24h")?.optDouble(0)
                    if (p24 != null && !p24.isNaN()) precip24h = p24

                    val mx = latestObj.optJSONArray("maxTemp")?.optDouble(0)
                    if (mx != null && !mx.isNaN()) observedMax = mx.roundToInt()

                    val mn = latestObj.optJSONArray("minTemp")?.optDouble(0)
                    if (mn != null && !mn.isNaN()) observedMin = mn.roundToInt()
                }
            }
        }

        val windSpeedKmH = windSpeedMs * 3.6

        // =========================================================
        // 2. 気象庁公式予報 (3日予報 & 週間予報) の解析
        // =========================================================
        val f0 = if (forecastArray.length() > 0) forecastArray.optJSONObject(0) else null
        val f1 = if (forecastArray.length() > 1) forecastArray.optJSONObject(1) else null

        // 3日予報 (今日・明日・明後日)
        var todayWeatherCodeStr = "100"
        var todayWeatherText = "晴れ"
        var todayWindText = ""
        var todayPops = listOf(0, 0, 0, 0)
        var tomorrowWeatherCodeStr = "101"
        var tomorrowWeatherText = "晴れ時々曇り"
        var expectedTodayMax: Int? = null
        var expectedTodayMin: Int? = null
        var expectedTomorrowMax: Int? = null
        var expectedTomorrowMin: Int? = null
        val popSlots = mutableListOf<JmaPopSlot>()

        val japanTimeZone = TimeZone.getTimeZone("Asia/Tokyo")
        val calNow = Calendar.getInstance(japanTimeZone)
        val todayJstStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = japanTimeZone }.format(calNow.time)
        val tomorrowCal = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowJstStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = japanTimeZone }.format(tomorrowCal.time)

        var chosenAreaIndex = 0
        if (f0 != null) {
            val tsList = f0.optJSONArray("timeSeries")
            if (tsList != null && tsList.length() > 0) {
                // ts[0]: 天気・風
                val ts0 = tsList.optJSONObject(0)
                val areas0 = ts0?.optJSONArray("areas")
                if (areas0 != null && areas0.length() > 1) {
                    for (i in 0 until areas0.length()) {
                        val aName = areas0.getJSONObject(i).optJSONObject("area")?.optString("name", "") ?: ""
                        if (aName.isNotEmpty()) {
                            if (city.name.contains(aName) || city.country.contains(aName)) {
                                chosenAreaIndex = i
                                break
                            }
                            if (aName == "南部" && (city.name.contains("さいたま") || city.name.contains("埼玉") || city.name.contains("川口") || city.name.contains("所沢") || city.name.contains("越谷"))) {
                                chosenAreaIndex = i
                                break
                            }
                            if (aName == "東京地方" && (city.name.contains("東京") || city.name.contains("千代田") || city.name.contains("新宿") || city.name.contains("渋谷"))) {
                                chosenAreaIndex = i
                                break
                            }
                        }
                    }
                }
                if (areas0 != null && areas0.length() > 0) {
                    val area0 = areas0.getJSONObject(chosenAreaIndex)
                    val wCodes = area0.optJSONArray("weatherCodes")
                    val weathers = area0.optJSONArray("weathers")
                    val winds = area0.optJSONArray("winds")

                    if (wCodes != null && wCodes.length() > 0) {
                        todayWeatherCodeStr = wCodes.optString(0, "100")
                        if (wCodes.length() > 1) tomorrowWeatherCodeStr = wCodes.optString(1, "101")
                    }
                    if (weathers != null && weathers.length() > 0) {
                        todayWeatherText = cleanJmaWeatherText(weathers.optString(0, "晴れ"))
                        if (weathers.length() > 1) tomorrowWeatherText = cleanJmaWeatherText(weathers.optString(1, "晴れ時々曇り"))
                    }
                    if (winds != null && winds.length() > 0) {
                        todayWindText = winds.optString(0, "")
                    }
                }

                // ts[1]: 降水確率 (6時間区切り & timeDefines)
                val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                if (tsList.length() > 1) {
                    val ts1 = tsList.optJSONObject(1)
                    val timeDefines1 = ts1?.optJSONArray("timeDefines")
                    val areas1 = ts1?.optJSONArray("areas")
                    val area1 = if (areas1 != null && areas1.length() > 0) {
                        areas1.optJSONObject(chosenAreaIndex.coerceIn(0, areas1.length() - 1)) ?: areas1.getJSONObject(0)
                    } else null
                    val pList = area1?.optJSONArray("pops")
                    if (pList != null) {
                        val parsedPops = mutableListOf<Int>()
                        for (i in 0 until pList.length()) {
                            val v = pList.optString(i, "0").toIntOrNull() ?: 0
                            parsedPops.add(v)
                        }
                        if (parsedPops.isNotEmpty()) todayPops = parsedPops
                    }
                    if (timeDefines1 != null && pList != null) {
                        for (i in 0 until minOf(timeDefines1.length(), pList.length())) {
                            val tStr = timeDefines1.optString(i, "")
                            val pVal = pList.optString(i, "0").toIntOrNull() ?: 0
                            val tMillis = try {
                                isoFmt.parse(tStr)?.time
                            } catch (_: Exception) {
                                null
                            }
                            if (tMillis != null) {
                                popSlots.add(JmaPopSlot(tMillis, pVal))
                            }
                        }
                    }
                }

                // ts[2]: 気温 (timeDefines の日付・時刻に基づいて今日/明日の最高・最低を精密抽出)
                if (tsList.length() > 2) {
                    val ts2 = tsList.optJSONObject(2)
                    val timeDefines2 = ts2?.optJSONArray("timeDefines")
                    val areas2 = ts2?.optJSONArray("areas")
                    if (areas2 != null && areas2.length() > 0) {
                        var chosenAreaIndex2 = 0
                        for (i in 0 until areas2.length()) {
                            val aName = areas2.getJSONObject(i).optJSONObject("area")?.optString("name", "") ?: ""
                            if (aName.isNotEmpty() && (city.name.contains(aName) || city.country.contains(aName))) {
                                chosenAreaIndex2 = i
                                break
                            }
                        }
                        val area2 = areas2.getJSONObject(chosenAreaIndex2)
                        val temps = area2.optJSONArray("temps")
                        if (temps != null) {
                            if (timeDefines2 != null && timeDefines2.length() == temps.length()) {
                                val todayValues = mutableListOf<Int>()
                                val tomorrowEntries = mutableListOf<Pair<Int, Int>>() // hour to temp
                                for (k in 0 until temps.length()) {
                                    val tDef = timeDefines2.optString(k, "")
                                    val tVal = temps.optString(k, "").toIntOrNull() ?: continue
                                    val datePart = tDef.take(10)
                                    val hourPart = tDef.substringAfter("T", "").take(2).toIntOrNull() ?: 9
                                    when (datePart) {
                                        todayJstStr -> todayValues.add(tVal)
                                        tomorrowJstStr -> tomorrowEntries.add(hourPart to tVal)
                                    }
                                }
                                if (todayValues.isNotEmpty()) {
                                    expectedTodayMax = todayValues.maxOrNull()
                                    if (todayValues.distinct().size >= 2) {
                                        expectedTodayMin = todayValues.minOrNull()
                                    }
                                }
                                if (tomorrowEntries.isNotEmpty()) {
                                    expectedTomorrowMin = tomorrowEntries.firstOrNull { it.first < 9 }?.second
                                        ?: if (tomorrowEntries.size >= 2) tomorrowEntries.minOf { it.second } else null
                                    expectedTomorrowMax = tomorrowEntries.firstOrNull { it.first >= 9 }?.second
                                        ?: tomorrowEntries.maxOf { it.second }
                                }
                            } else {
                                if (temps.length() >= 4) {
                                    val t0 = temps.optString(0, "").toIntOrNull()
                                    val t1 = temps.optString(1, "").toIntOrNull()
                                    if (t0 != null && t1 != null) {
                                        if (t0 == t1) {
                                            expectedTodayMax = t1
                                        } else {
                                            expectedTodayMin = minOf(t0, t1)
                                            expectedTodayMax = maxOf(t0, t1)
                                        }
                                    }
                                    expectedTomorrowMin = temps.optString(2, "").toIntOrNull()
                                    expectedTomorrowMax = temps.optString(3, "").toIntOrNull()
                                } else if (temps.length() == 2) {
                                    // 17時発表では [翌日最低, 翌日最高] の2要素
                                    expectedTomorrowMin = temps.optString(0, "").toIntOrNull()
                                    expectedTomorrowMax = temps.optString(1, "").toIntOrNull()
                                }
                            }
                        }
                    }
                }
            }
        }

        // 最高・最低気温の算出 (実測値と予報値のベストマッチ)
        val minTempC = observedMin ?: expectedTodayMin ?: (currentTemp - 4)
        val maxTempC = maxOf(observedMax ?: (currentTemp + 1), expectedTodayMax ?: (currentTemp + 4))

        val supplementaryHourly = supplementaryHighResJson?.optJSONObject("hourly")
        val supplementaryTimes = supplementaryHourly?.optJSONArray("time")
        val supplementaryHumidities = supplementaryHourly?.optJSONArray("relative_humidity_2m")
        val supplementaryHumidityByHour = buildMap<String, Int> {
            if (supplementaryTimes != null && supplementaryHumidities != null) {
                for (index in 0 until minOf(supplementaryTimes.length(), supplementaryHumidities.length())) {
                    val value = supplementaryHumidities.optInt(index, -1)
                    if (value in 0..100) put(supplementaryTimes.optString(index).take(13), value)
                }
            }
        }
        val supplementaryHumidityByDate = supplementaryHumidityByHour.entries
            .groupBy { it.key.substringBefore('T') }
            .mapValues { (_, entries) -> entries.map { it.value }.average().roundToInt() }
        val humidityHourFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).apply { timeZone = japanTimeZone }
        val humidityDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = japanTimeZone }
        fun hourlyHumidityAt(date: Date): Int? =
            supplementaryHumidityByHour[humidityHourFormatter.format(date)]
        fun dailyHumidityAverageAt(date: Date): Int? =
            supplementaryHumidityByDate[humidityDateFormatter.format(date)]

        // =========================================================
        // 3. 日出・日没・日長計算 (NOAA Solar Equation)
        // =========================================================
        val (sunriseTime, sunsetTime, daylightSec, dayProgress) = calculateSunSchedule(
            city.latitude, city.longitude, calNow
        )
        val daylightHoursStr = formatDaylightDuration(daylightSec)

        // 昼夜判定（計算された天文学的日の出・日の入り時刻に基づく厳密判定）
        val currentMinOfDay = calNow.get(Calendar.HOUR_OF_DAY) * 60 + calNow.get(Calendar.MINUTE)
        val sunriseMin = parseTimeToMinutes(sunriseTime)
        val sunsetMin = parseTimeToMinutes(sunsetTime)
        val isDay = currentMinOfDay in sunriseMin until sunsetMin

        val todayMaxPop = if (popSlots.isNotEmpty()) popSlots.take(4).maxOf { it.pop } else (todayPops.maxOrNull() ?: 0)
        val currentPop = getPopForTimestamp(calNow.timeInMillis, popSlots, todayMaxPop)

        // 実況降水判定:
        // - AMeDAS は直近10分値 (precipitation10m) を優先し、既に止んだ過去の1時間累計で雨残りを起こさない
        // - 高解像度降水ナウキャスト (hrpns) の現在時刻付近タイル値も現在天気判定へ統合
        val liveAmedasPrecipMmH: Double? = when {
            amedasObservedPrecip10m != null -> amedasObservedPrecip10m * 6.0
            amedasObservedPrecip1h != null -> amedasObservedPrecip1h
            else -> null
        }
        val nowMillis = calNow.timeInMillis
        val nowcastCurrentMm: Double? = jmaMinutelySeries
            ?.filter { kotlin.math.abs(it.first - nowMillis) <= 15 * 60 * 1000L }
            ?.minByOrNull { kotlin.math.abs(it.first - nowMillis) }
            ?.second
        val currentLivePrecipMm: Double = maxOf(
            liveAmedasPrecipMmH?.takeIf { it > 0.05 } ?: 0.0,
            nowcastCurrentMm?.takeIf { it >= PrecipitationAnalyzer.THRESHOLD_RAIN_START } ?: 0.0
        )

        // 現在の天気状態 (実況値・ナウキャスト最優先 + 気象台予報のハイブリッド判定)
        val condition = resolveCondition(
            codeStr = todayWeatherCodeStr,
            weatherText = todayWeatherText,
            hourOfDay = calNow.get(Calendar.HOUR_OF_DAY),
            pop = currentPop,
            isDay = isDay,
            isCurrent = true,
            observedPrecipMm = liveAmedasPrecipMmH ?: 0.0,
            nowcastPrecipMm = nowcastCurrentMm,
            hasObservedPrecipData = liveAmedasPrecipMmH != null
        )

        // =========================================================
        // 4. 24時間時系列予報 (HourlyForecast) の生成
        // =========================================================
        val hourlyForecast = mutableListOf<HourlyForecast>()
        val hourlyWindList = mutableListOf<HourlyWindItem>()
        val todayStartDay = calNow.get(Calendar.DAY_OF_YEAR)
        val nowHour = calNow.get(Calendar.HOUR_OF_DAY)
        val nowHourFrac = computeDiurnalFraction(nowHour)
        val step0RefLow = if (nowHour >= 14) (expectedTomorrowMin ?: minTempC) else minTempC
        val step0CurveTemp = step0RefLow + (maxTempC - step0RefLow) * nowHourFrac
        val currentTempOffset = currentTemp - step0CurveTemp

        for (step in 0 until 24) {
            val hCal = (calNow.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, step) }
            val hHour = hCal.get(Calendar.HOUR_OF_DAY)
            val hMinOfDay = hHour * 60
            val hIsDay = if (step == 0) isDay else (hMinOfDay in sunriseMin until sunsetMin)
            val hLabel = if (step == 0) "現在" else String.format(Locale.JAPANESE, "%d:00", hHour)

            // 日周変化モデルに基づく滑らかな気温曲線 (未明5時最低、午後14時最高)
            // 日付跨ぎ (14時〜翌5時の夜間冷却区間) で refLow/refHigh を連続接続し、0時の不連続段差を防止
            val tempFraction = computeDiurnalFraction(hHour)
            val isSameDay = hCal.get(Calendar.DAY_OF_YEAR) == todayStartDay
            val refMin = if (isSameDay) minTempC else (expectedTomorrowMin ?: minTempC)
            val refMax = if (isSameDay) maxTempC else (expectedTomorrowMax ?: maxTempC)
            val refLow = if (hHour >= 14 && isSameDay) (expectedTomorrowMin ?: minTempC) else refMin
            val refHigh = if (hHour < 5 && !isSameDay) maxTempC else refMax
            val baseCurveTemp = refLow + (refHigh - refLow) * tempFraction
            val hTemp = (baseCurveTemp + currentTempOffset * exp(-step / 3.5)).roundToInt()

            // 6時間降水確率ブロックとの同期
            val hPop = if (step == 0) currentPop else getPopForTimestamp(hCal.timeInMillis, popSlots, if (isSameDay) todayMaxPop else 20)

            // 湿度 (気温と逆相関)
            val hHum = hourlyHumidityAt(hCal.time) ?: (100 - (tempFraction * 40)).roundToInt().coerceIn(30, 95)
            // 風速
            val hWind = (windSpeedKmH * (0.8 + tempFraction * 0.4)).coerceAtLeast(1.0)
            val hGust = hWind * 1.35

            // 降水量 (mm) - THRESHOLD_RAIN_START (1.0 mm/h) と整合する階層マッピング
            // step=1 は hrpns (5分刻み)、step>=2 は rasrf (1時間毎) に対応するため許容時間差を調整
            val matchWindowMs = if (step <= 1) 15 * 60 * 1000L else 30 * 60 * 1000L
            val nowcastHourPrecip = jmaMinutelySeries
                ?.filter { kotlin.math.abs(it.first - hCal.timeInMillis) <= matchWindowMs }
                ?.minByOrNull { kotlin.math.abs(it.first - hCal.timeInMillis) }
                ?.second

            // 天気状態の精密判定
            val codeForHour = if (isSameDay) todayWeatherCodeStr else tomorrowWeatherCodeStr
            val textForHour = if (isSameDay) todayWeatherText else tomorrowWeatherText

            val hCondition = if (step == 0) {
                condition
            } else {
                resolveCondition(
                    codeStr = codeForHour,
                    weatherText = textForHour,
                    hourOfDay = hHour,
                    pop = hPop,
                    isDay = hIsDay,
                    isCurrent = false,
                    observedPrecipMm = 0.0,
                    nowcastPrecipMm = nowcastHourPrecip,
                    hasObservedPrecipData = false
                )
            }

            val hPrecipMm = if (step == 0) {
                currentLivePrecipMm
            } else if (nowcastHourPrecip != null) {
                if (nowcastHourPrecip >= PrecipitationAnalyzer.THRESHOLD_RAIN_START) nowcastHourPrecip else 0.0
            } else {
                if (hCondition.isPrecipitating) {
                    if (hPop >= 70) 2.5 else if (hPop >= 50) 1.2 else 0.4
                } else 0.0
            }

            val hDewPoint = hTemp - ((100 - hHum) / 5)

            hourlyForecast.add(
                HourlyForecast(
                    timeLabel = hLabel,
                    hour = hHour,
                    tempC = if (step == 0) currentTemp else hTemp,
                    condition = hCondition,
                    precipProb = hPop,
                    humidity = hHum,
                    windSpeedKmH = hWind,
                    dewPointC = hDewPoint,
                    isDay = hIsDay,
                    precipitationMm = hPrecipMm
                )
            )

            hourlyWindList.add(
                HourlyWindItem(
                    timeLabel = hLabel,
                    hour = hHour,
                    windSpeedKmH = hWind,
                    windGustsKmH = hGust,
                    windDegree = windDegree
                )
            )
        }

        // =========================================================
        // 5. 7日間週間予報 (DailyForecast) の生成
        // =========================================================
        val dailyForecast = mutableListOf<DailyForecast>()
        val dailyWindList = mutableListOf<DailyWindItem>()
        val dayOfWeekFormat = SimpleDateFormat("E", Locale.JAPANESE).apply { timeZone = japanTimeZone }
        val monthDayFormat = SimpleDateFormat("M/d", Locale.JAPANESE).apply { timeZone = japanTimeZone }

        // Day 0: 今日
        val d0Date = calNow.time
        val d0HasRain = hasRainInCode(todayWeatherCodeStr, todayWeatherText) && todayMaxPop >= 50
        val d0Condition = if (d0HasRain) WeatherCondition.RAIN else condition
        dailyForecast.add(
            DailyForecast(
                dayLabel = "今日",
                dateLabel = monthDayFormat.format(d0Date),
                condition = d0Condition,
                minTempC = minTempC,
                maxTempC = maxTempC,
                precipProb = todayMaxPop,
                uvMax = 4.0,
                precipitationSumMm = precip24h,
                windSpeedMaxKmH = windGustsKmH,
                hourlyForecast = hourlyForecast.take(24),
                sunrise = sunriseTime,
                sunset = sunsetTime,
                daylightHours = daylightHoursStr,
                humidityAvg = dailyHumidityAverageAt(d0Date),
                humidityAvgSource = if (dailyHumidityAverageAt(d0Date) != null) "Open-Meteo" else null,
                pressureHpa = pressure
            )
        )
        dailyWindList.add(
            DailyWindItem(
                dayLabel = "今日",
                dateLabel = monthDayFormat.format(d0Date),
                windSpeedMaxKmH = windSpeedKmH,
                windGustsMaxKmH = windGustsKmH,
                dominantWindDegree = windDegree
            )
        )

        // 週間予報テーブル (f1) から翌日以降 (Day 1〜6) をパース
        var f1ParsedDays = 0
        if (f1 != null) {
            val tsList1 = f1.optJSONArray("timeSeries")
            if (tsList1 != null && tsList1.length() >= 2) {
                val tsW = tsList1.optJSONObject(0)
                val tsT = tsList1.optJSONObject(1)

                val timeDefines = tsW?.optJSONArray("timeDefines")
                val areasW = tsW?.optJSONArray("areas")
                val areasT = tsT?.optJSONArray("areas")
                val areaW = areasW?.optJSONObject(chosenAreaIndex.coerceIn(0, (areasW.length() - 1).coerceAtLeast(0))) ?: areasW?.optJSONObject(0)
                val areaT = areasT?.optJSONObject(chosenAreaIndex.coerceIn(0, (areasT.length() - 1).coerceAtLeast(0))) ?: areasT?.optJSONObject(0)

                val wCodes = areaW?.optJSONArray("weatherCodes")
                val pops = areaW?.optJSONArray("pops")
                val tMins = areaT?.optJSONArray("tempsMin")
                val tMaxs = areaT?.optJSONArray("tempsMax")
                val weeklyIsoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

                if (timeDefines != null) {
                    for (i in 0 until timeDefines.length()) {
                        val tIso = timeDefines.optString(i, "")
                        val parsedTime = try { weeklyIsoFmt.parse(tIso) } catch (_: Exception) { null }
                        val dCal = (calNow.clone() as Calendar).apply {
                            if (parsedTime != null) {
                                time = parsedTime
                            } else {
                                add(Calendar.DAY_OF_YEAR, i + 1)
                            }
                        }
                        val dDateStrKey = humidityDateFormatter.format(dCal.time)
                        if (dDateStrKey == todayJstStr) continue
                        val isTomorrow = dDateStrKey == tomorrowJstStr
                        val dLabel = if (isTomorrow) "明日" else dayOfWeekFormat.format(dCal.time)
                        val dDateStr = monthDayFormat.format(dCal.time)

                        val dCode = wCodes?.optString(i, if (isTomorrow) tomorrowWeatherCodeStr else "101") ?: "101"
                        val dCond = mapJmaWeatherCode(dCode, true)
                        val dPop = pops?.optString(i, "20")?.toIntOrNull() ?: 20

                        val dMinRaw = tMins?.optString(i, "")?.toIntOrNull()
                        val dMaxRaw = tMaxs?.optString(i, "")?.toIntOrNull()

                        val dMin = dMinRaw ?: (if (isTomorrow) expectedTomorrowMin ?: (minTempC) else minTempC)
                        val dMax = dMaxRaw ?: (if (isTomorrow) expectedTomorrowMax ?: (maxTempC) else maxTempC)

                        val (dSunrise, dSunset, dDaylight, _) = calculateSunSchedule(
                            city.latitude, city.longitude, dCal
                        )

                        dailyForecast.add(
                            DailyForecast(
                                dayLabel = dLabel,
                                dateLabel = dDateStr,
                                condition = dCond,
                                minTempC = dMin,
                                maxTempC = dMax,
                                precipProb = dPop,
                                uvMax = 4.0,
                                precipitationSumMm = 0.0,
                                windSpeedMaxKmH = windSpeedKmH,
                                hourlyForecast = emptyList(),
                                sunrise = dSunrise,
                                sunset = dSunset,
                                daylightHours = formatDaylightDuration(dDaylight),
                                humidityAvg = dailyHumidityAverageAt(dCal.time),
                                humidityAvgSource = if (dailyHumidityAverageAt(dCal.time) != null) "Open-Meteo" else null,
                                pressureHpa = 1013
                            )
                        )

                        dailyWindList.add(
                            DailyWindItem(
                                dayLabel = dLabel,
                                dateLabel = dDateStr,
                                windSpeedMaxKmH = windSpeedKmH,
                                windGustsMaxKmH = windGustsKmH,
                                dominantWindDegree = windDegree
                            )
                        )
                        f1ParsedDays++
                        if (dailyForecast.size >= 7) break
                    }
                }
            }
        }

        // もし f1 が未取得または短い場合のフォールバック（7日分を補完）
        while (dailyForecast.size < 7) {
            val offset = dailyForecast.size
            val dCal = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dLabel = dayOfWeekFormat.format(dCal.time)
            val dDateStr = monthDayFormat.format(dCal.time)
            dailyForecast.add(
                DailyForecast(
                    dayLabel = dLabel,
                    dateLabel = dDateStr,
                    condition = condition,
                    minTempC = minTempC,
                    maxTempC = maxTempC,
                    precipProb = 20,
                    uvMax = 4.0,
                    precipitationSumMm = 0.0,
                    windSpeedMaxKmH = windSpeedKmH,
                    hourlyForecast = emptyList(),
                    sunrise = sunriseTime,
                    sunset = sunsetTime,
                    daylightHours = daylightHoursStr,
                    humidityAvg = dailyHumidityAverageAt(dCal.time),
                    humidityAvgSource = if (dailyHumidityAverageAt(dCal.time) != null) "Open-Meteo" else null,
                    pressureHpa = 1013
                )
            )
        }

        // =========================================================
        // 6. 概況テキスト・警報・空気質の統合
        // =========================================================
        val overviewText = overviewJson?.optNullableString("text")
        val warningHeadline = warningJson?.optNullableString("headlineText")

        val municipality = JmaMunicipalityResolver.resolveFromLocation(
            city.latitude,
            city.longitude,
            city.name,
            ""
        )
        val baseAlertInfo = if (warningJson != null) {
            JmaAlertRepository.evaluateAlert(warningJson, municipality)
        } else {
            MunicipalityAlertInfo(
                municipalityCode = municipality.code,
                municipalityName = municipality.name,
                prefectureCode = municipality.officeCode,
                prefectureName = municipality.prefName,
                alertLevel = AlertLevel.LEVEL_0
            )
        }
        val alertInfo = JmaAlertRepository.mergeWithKikikuruTileRisk(baseAlertInfo, kikikuruRisk)
        val warningsList = if (alertInfo.activeWarnings.isNotEmpty()) {
            alertInfo.activeWarnings
                .filter { it.status != "解除" && !it.status.contains("解除") && !it.name.contains("解除") }
                .map { item ->
                    if (item.status.isNotBlank() && item.status != "発表" && item.status != "継続") "${item.name} (${item.status})" else item.name
                }
        } else {
            emptyList()
        }

        val airQuality = if (supplementaryAqiJson != null) {
            parseAirQuality(supplementaryAqiJson)
        } else {
            AirQuality(
                aqi = 0,
                levelLabel = "--",
                pm25 = 0.0,
                pm10 = 0.0,
                advice = "",
                usAqi = 0,
                europeanAqi = 0,
                isAvailable = false
            )
        }

        val feelsLike = calculateApparentTemperature(currentTemp, humidity, windSpeedMs)
        val currentTimeFmt = SimpleDateFormat("HH:mm", Locale.JAPANESE).apply { timeZone = japanTimeZone }.format(Date())

        val windDetails = WindDetails(
            windSpeedKmH = windSpeedKmH,
            windGustsKmH = windGustsKmH,
            windDegree = windDegree,
            windDirectionText = windDirectionText,
            beaufortScale = BeaufortScale.fromSpeedMs(windSpeedMs),
            hourlyWind = hourlyWindList,
            dailyWind = dailyWindList,
            peakWindTime = "現在",
            peakWindSpeedKmH = windGustsKmH
        )

        // 日本国内のナウキャスト解析: Open-Meteo等の外部降水情報は遮断し、
        // 気象庁の公式データ（アメダス10分値・高解像度降水ナウキャストタイル・予報・降水確率スロット）のみで高精度解析を実施
        val precipitationNowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = currentLivePrecipMm,
            currentCondition = condition,
            minutelyTimes = null,
            minutelyPrecips = null,
            minutelyCodes = null,
            hourlyForecast = hourlyForecast,
            jmaOverview = overviewText,
            popSlots = popSlots,
            country = city.country,
            latitude = city.latitude,
            longitude = city.longitude,
            conditionName = todayWeatherText,
            jmaMinutelySeries = jmaMinutelySeries
        )

        return WeatherData(
            city = city,
            currentTempC = currentTemp,
            feelsLikeC = feelsLike,
            minTempC = minTempC,
            maxTempC = maxTempC,
            condition = condition,
            conditionSummary = condition.tagline,
            conditionName = todayWeatherText,
            humidity = humidity,
            uvIndex = 4,
            uvLabel = "普通",
            windSpeedKmH = windSpeedKmH,
            windDirection = windDirectionText,
            windDegree = windDegree,
            pressureHpa = pressure,
            visibilityKm = 20.0,
            airQuality = airQuality,
            sunSchedule = SunSchedule(
                sunrise = sunriseTime,
                sunset = sunsetTime,
                dayProgress = dayProgress,
                daylightHours = daylightHoursStr
            ),
            hourlyForecast = hourlyForecast,
            dailyForecast = dailyForecast,
            lastUpdatedTime = currentTimeFmt,
            isDay = isDay,
            dewPointC = currentTemp - ((100 - humidity) / 5),
            windGustsKmH = windGustsKmH,
            precipitationMm = currentLivePrecipMm,
            precipitationSumMm = precip24h,
            precipitationProbabilityMax = todayMaxPop,
            windDetails = windDetails,
            dataSourceName = "気象データ提供元: 気象庁 (JMA)",
            jmaOverviewText = overviewText,
            jmaWarnings = warningsList,
            alertInfo = alertInfo,
            precipitationNowcast = precipitationNowcast,
            utcOffsetSeconds = 9 * 3600
        )
    }

    typealias JmaPopSlot = PopSlot

    private fun getPopForTimestamp(targetMillis: Long, slots: List<JmaPopSlot>, fallback: Int): Int {
        if (slots.isEmpty()) return fallback
        val matched = slots.lastOrNull { targetMillis >= it.startMillis }
        return matched?.pop ?: slots.first().pop
    }

    fun hasRainInCode(codeStr: String, text: String): Boolean {
        val code = codeStr.toIntOrNull() ?: 100
        return code in 300..399 ||
                code in listOf(102, 103, 112, 113, 114, 120, 121, 122, 126, 127, 128, 202, 203, 212, 213, 214, 220, 221, 222, 224, 225, 226) ||
                text.contains("雨")
    }

    /**
     * 実況値（AMeDAS 10分降水量・高解像度降水ナウキャスト）と気象台予報コード・時間帯・降水確率を統合した、
     * 現実の空模様と一致する精密天気状態判定エンジン
     */
    fun resolveCondition(
        codeStr: String,
        weatherText: String,
        hourOfDay: Int,
        pop: Int,
        isDay: Boolean,
        isCurrent: Boolean,
        observedPrecipMm: Double = 0.0,
        nowcastPrecipMm: Double? = null,
        hasObservedPrecipData: Boolean = false
    ): WeatherCondition {
        val code = codeStr.toIntOrNull() ?: 100
        val text = weatherText
        val compactText = text.replace(" ", "").replace("\u3000", "")
        val isSnowCode = code in 400..499 || code in listOf(
            104, 105, 115, 116, 204, 205, 215, 216,
            303, 304, 309, 314, 315, 316, 317, 326, 327, 340
        )
        val isThunderCode = code in listOf(108, 119, 123, 125, 140, 208, 219, 230, 240, 250, 350)

        // 1. 実況・短時間ナウキャスト降水量の最優先判定
        val activeObservedPrecipMm = if (isCurrent && observedPrecipMm > 0.05) observedPrecipMm else 0.0
        val activeNowcastPrecipMm = if (nowcastPrecipMm != null && nowcastPrecipMm >= PrecipitationAnalyzer.THRESHOLD_RAIN_START) {
            nowcastPrecipMm
        } else {
            0.0
        }
        val activePrecipMm = maxOf(activeObservedPrecipMm, activeNowcastPrecipMm)
        if (activePrecipMm > 0.05) {
            return when {
                isSnowCode -> WeatherCondition.SNOW
                isThunderCode && activePrecipMm >= 5.0 -> WeatherCondition.THUNDERSTORM
                activePrecipMm >= 10.0 -> WeatherCondition.HEAVY_RAIN
                else -> WeatherCondition.RAIN
            }
        }

        // 2. 実況観測（AMeDAS / 高解像度降水ナウキャスト）または短時間降水予報で
        //    降水なしが確定している場合は、府県予報コードの「雨」で誤って雨表示にしない
        val isConfirmedDry = (isCurrent && (hasObservedPrecipData || nowcastPrecipMm != null)) ||
            (!isCurrent && nowcastPrecipMm != null)
        if (isConfirmedDry) {
            return resolveDryBaseCondition(code, compactText, hourOfDay, pop, isDay)
        }

        // 3. 実況・短時間降水データが利用できない時間帯（遠未来やオフライン時）の予報コード判定
        // 雷雨判定
        if (isThunderCode || compactText.contains("雷")) {
            if (pop >= 50 || hourOfDay >= 15) return WeatherCondition.THUNDERSTORM
        }

        // 雪判定 (400系)
        if (code in 400..499 || compactText.contains("雪")) {
            if (code in listOf(411, 420, 422) && hourOfDay >= 12 && pop < 30) {
                return if (isDay) WeatherCondition.CLEAR else WeatherCondition.FEW_CLOUDS
            }
            return WeatherCondition.SNOW
        }

        // 霧判定
        if (code in listOf(130, 131, 209) || compactText.contains("霧")) {
            if (hourOfDay in 4..8) return WeatherCondition.FOG
        }

        // 300系 (雨ベース)
        if (code in 300..399) {
            // 雨後晴れ / 雨後曇り (311, 313, 320..325)
            if (code in listOf(311, 313, 320, 321, 322, 323, 324, 325) ||
                compactText.contains("雨後") ||
                compactText.contains("雨のち") ||
                compactText.contains("夕方からくもり") ||
                compactText.contains("夕方から曇") ||
                compactText.contains("夜くもり") ||
                compactText.contains("夜曇")
            ) {
                val rainEndsAfterNoon = code == 322 ||
                    compactText.contains("昼頃から晴") ||
                    compactText.contains("昼頃から曇") ||
                    compactText.contains("昼頃からくもり")
                val rainEndsEvening = code == 323 ||
                    compactText.contains("夕方から晴") ||
                    compactText.contains("夕方からくもり") ||
                    compactText.contains("夕方から曇")
                val clearThreshold = when {
                    compactText.contains("夜くもり") || compactText.contains("夜曇") -> 18
                    rainEndsEvening -> 17
                    rainEndsAfterNoon -> 12
                    code in listOf(320, 321) || compactText.contains("朝の内") -> 9
                    else -> 12
                }
                if (hourOfDay >= clearThreshold && pop < 40) {
                    return if (code in listOf(311, 320, 322, 324) || compactText.contains("晴")) {
                        if (isDay) WeatherCondition.CLEAR else WeatherCondition.FEW_CLOUDS
                    } else {
                        WeatherCondition.CLOUDY
                    }
                }
            }
            if (code in listOf(306, 308, 329)) return WeatherCondition.HEAVY_RAIN
            return WeatherCondition.RAIN
        }

        // 100系・200系の複合天気（晴後雨、曇時々晴夜遅く雨、くもり昼過ぎから夕方雨など）
        val hasRain = hasRainInCode(codeStr, text)
        if (hasRain) {
            val isRainTime: Boolean = when {
                code in listOf(121, 221) || compactText.contains("朝の内") || compactText.contains("朝まで") -> hourOfDay < 9
                compactText.contains("昼過ぎから夕方") || compactText.contains("昼頃から夕方") -> hourOfDay in 12..18
                code in listOf(126, 224) || compactText.contains("昼頃から") -> hourOfDay >= 12
                compactText.contains("昼過ぎから") || compactText.contains("午後から") -> hourOfDay >= 13
                compactText.contains("夕方一時") || compactText.contains("夕方時々") -> hourOfDay in 15..19
                code in listOf(127, 225) || compactText.contains("夕方から") -> hourOfDay >= 16
                compactText.contains("夜遅く") -> hourOfDay >= 21
                compactText.contains("夜のはじめ頃") -> hourOfDay in 18..21
                code in listOf(128, 226) ||
                    compactText.contains("夜は雨") ||
                    compactText.contains("夜から雨") ||
                    compactText.contains("夜一時雨") ||
                    compactText.contains("夜時々雨") ||
                    compactText.contains("夜雨") -> hourOfDay >= 18
                code in listOf(112, 113, 114, 212, 213, 214) ||
                    compactText.contains("後雨") ||
                    compactText.contains("のち雨") -> hourOfDay >= 15
                else -> pop >= 60
            }

            if (isRainTime && pop >= 40) {
                return WeatherCondition.RAIN
            }
        }

        return resolveDryBaseCondition(code, compactText, hourOfDay, pop, isDay)
    }

    /**
     * 非降水時の基底天気（晴れ・薄曇り・曇り・朝霧）を判定
     */
    private fun resolveDryBaseCondition(
        code: Int,
        compactText: String,
        hourOfDay: Int,
        pop: Int,
        isDay: Boolean
    ): WeatherCondition {
        if ((code in listOf(130, 131, 209) || compactText.contains("霧")) && hourOfDay in 4..8) {
            return WeatherCondition.FOG
        }

        // 雨・雪主体の予報日でも、現時点で非降水なら「曇り」（晴れ移行後のみ晴れ）
        if (code in 300..399) {
            val clearThreshold = when {
                code == 323 || compactText.contains("夕方から晴") -> 17
                code == 322 || compactText.contains("昼頃から晴") -> 12
                code in listOf(320, 321) || compactText.contains("朝の内") -> 9
                else -> 12
            }
            if ((code in listOf(311, 320, 322, 324) || compactText.contains("晴")) &&
                hourOfDay >= clearThreshold &&
                pop < 40
            ) {
                return if (isDay) WeatherCondition.CLEAR else WeatherCondition.FEW_CLOUDS
            }
            return WeatherCondition.CLOUDY
        }

        if (code in 400..499) {
            if (code in listOf(411, 420, 422) && hourOfDay >= 12 && pop < 30) {
                return if (isDay) WeatherCondition.CLEAR else WeatherCondition.FEW_CLOUDS
            }
            return WeatherCondition.CLOUDY
        }

        val isSunnyBase = (code in 100..199) || (code in listOf(201, 210, 211, 223)) || compactText.contains("晴")
        val hasCloudy = (code in 200..299) || (code in listOf(101, 110, 111)) || compactText.contains("くもり") || compactText.contains("曇")

        return when {
            isSunnyBase && !hasCloudy -> WeatherCondition.CLEAR
            isSunnyBase && hasCloudy -> WeatherCondition.FEW_CLOUDS
            else -> WeatherCondition.CLOUDY
        }
    }

    /**
     * 気象庁 3桁天気コードを WeatherCondition に高精度マッピング
     */
    fun mapJmaWeatherCode(codeStr: String, isDay: Boolean): WeatherCondition {
        val code = codeStr.toIntOrNull() ?: 100
        return when (code) {
            100 -> WeatherCondition.CLEAR
            101, 110, 111 -> WeatherCondition.FEW_CLOUDS
            102, 103, 112, 113, 120, 121, 122, 126, 127, 328 -> WeatherCondition.RAIN
            104, 105, 115, 116 -> WeatherCondition.SNOW
            108, 119, 123, 125, 140, 350 -> WeatherCondition.THUNDERSTORM
            130, 131, 209 -> WeatherCondition.FOG
            200 -> WeatherCondition.CLOUDY
            201, 210, 211, 223 -> WeatherCondition.FEW_CLOUDS
            202, 203, 212, 213, 220, 221, 222, 224, 225, 228, 229, 231 -> WeatherCondition.RAIN
            204, 205, 215, 216 -> WeatherCondition.SNOW
            208, 219, 230, 240, 250 -> WeatherCondition.THUNDERSTORM
            300, 301, 302, 311, 313, 320, 321, 322, 323, 324, 325 -> WeatherCondition.RAIN
            306, 308, 329 -> WeatherCondition.HEAVY_RAIN
            303, 304, 309, 314, 315, 316, 317, 326, 327, 340, 361, 371 -> WeatherCondition.SNOW
            400, 401, 402, 403, 409, 411, 413, 414, 420, 421, 422, 423, 424, 425, 426, 427 -> WeatherCondition.SNOW
            405, 406, 407, 450 -> WeatherCondition.SNOW
            else -> when (code / 100) {
                1 -> WeatherCondition.CLEAR
                2 -> WeatherCondition.CLOUDY
                3 -> WeatherCondition.RAIN
                4 -> WeatherCondition.SNOW
                else -> WeatherCondition.CLEAR
            }
        }
    }

    private fun cleanJmaWeatherText(raw: String): String {
        return raw.replace("\u3000", " ").trim()
    }

    private fun getWindDirection16Text(code: Int): String {
        return when (code) {
            1 -> "北北東"
            2 -> "北東"
            3 -> "東北東"
            4 -> "東"
            5 -> "東南東"
            6 -> "南東"
            7 -> "南南東"
            8 -> "南"
            9 -> "南南西"
            10 -> "南西"
            11 -> "西南西"
            12 -> "西"
            13 -> "西北西"
            14 -> "北西"
            15 -> "北北西"
            16 -> "北"
            else -> "静穏"
        }
    }

    private fun calculateApparentTemperature(tempC: Int, humidity: Int, windSpeedMs: Double): Int {
        val e = (humidity / 100.0) * 6.105 * Math.exp((17.27 * tempC) / (237.7 + tempC))
        val at = tempC + 0.33 * e - 0.70 * windSpeedMs - 4.0
        return at.roundToInt()
    }

    private fun calculateSunSchedule(
        latitude: Double,
        longitude: Double,
        cal: Calendar
    ): Quad<String, String, Long, Float> {
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)
        val eqtime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        val safeLat = if (latitude.isFinite()) latitude.coerceIn(-89.9, 89.9) else 35.6895
        val safeLon = if (longitude.isFinite()) longitude.coerceIn(-180.0, 180.0) else 139.6917
        val latRad = Math.toRadians(safeLat)
        val denom = cos(latRad) * cos(decl)
        val rawCosHa = if (kotlin.math.abs(denom) < 1e-9) 0.0 else (cos(Math.toRadians(90.833)) - sin(latRad) * sin(decl)) / denom
        val clampedCosHa = if (rawCosHa.isFinite()) rawCosHa.coerceIn(-1.0, 1.0) else 0.0
        val haDeg = Math.toDegrees(acos(clampedCosHa))

        val sunriseMinUtc = 720 - 4 * safeLon - eqtime - 4 * haDeg
        val sunsetMinUtc = 720 - 4 * safeLon - eqtime + 4 * haDeg

        // JST is UTC + 9 (540 minutes)
        val sunriseMinJst = (if (sunriseMinUtc.isFinite()) sunriseMinUtc + 540 else 360.0).roundToInt().mod(1440)
        val sunsetMinJst = (if (sunsetMinUtc.isFinite()) sunsetMinUtc + 540 else 1080.0).roundToInt().mod(1440)

        val sunriseStr = String.format(Locale.JAPANESE, "%02d:%02d", sunriseMinJst / 60, sunriseMinJst % 60)
        val sunsetStr = String.format(Locale.JAPANESE, "%02d:%02d", sunsetMinJst / 60, sunsetMinJst % 60)

        val daylightSpanMin = (sunsetMinJst - sunriseMinJst).coerceAtLeast(0)
        val daylightSec = (daylightSpanMin * 60).toLong()

        val currentMinOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val progress = when {
            daylightSpanMin <= 0 -> 0.5f
            currentMinOfDay < sunriseMinJst -> 0.0f
            currentMinOfDay > sunsetMinJst -> 1.0f
            else -> ((currentMinOfDay - sunriseMinJst).toFloat() / daylightSpanMin.toFloat()).coerceIn(0.0f, 1.0f)
        }

        return Quad(sunriseStr, sunsetStr, daylightSec, progress)
    }

    private fun formatDaylightDuration(seconds: Long): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return String.format(Locale.JAPANESE, "%d時間%d分", hours, mins)
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: 6
            val m = parts[1].toIntOrNull() ?: 0
            return h * 60 + m
        }
        return 360
    }

    private fun parseAirQuality(aqiJson: JSONObject): AirQuality {
        val current = aqiJson.optJSONObject("current") ?: return AirQuality(
            aqi = 0,
            levelLabel = "--",
            pm25 = 0.0,
            pm10 = 0.0,
            advice = "",
            usAqi = 0,
            europeanAqi = 0,
            isAvailable = false
        )
        val pm25 = current.optFiniteDouble("pm2_5") ?: 0.0
        val pm10 = current.optFiniteDouble("pm10") ?: 0.0
        val usAqi = current.optInt("us_aqi", 0)
        val europeanAqi = current.optInt("european_aqi", 0)
        val ozone = current.optFiniteDouble("ozone")
        val no2 = current.optFiniteDouble("nitrogen_dioxide")

        val levelLabel = when {
            usAqi <= 50 -> "良好"
            usAqi <= 100 -> "普通"
            usAqi <= 150 -> "敏感な人に影響"
            usAqi <= 200 -> "健康に影響あり"
            else -> "極めて危険"
        }

        val advice = when {
            usAqi <= 50 -> "清浄な空気です。深呼吸や屋外活動に最適です"
            usAqi <= 100 -> "標準的な大気質です。通常の屋外活動には問題ありません"
            usAqi <= 150 -> "呼吸器系の弱い方は長時間の激しい運動をお控えください"
            usAqi <= 200 -> "健康への悪影響が懸念されます。長時間の外出を避けてください"
            else -> "極めて危険な大気汚染です。屋内にとどまり窓を閉めてください"
        }

        return AirQuality(
            aqi = usAqi,
            levelLabel = levelLabel,
            pm25 = pm25,
            pm10 = pm10,
            advice = advice,
            usAqi = usAqi,
            europeanAqi = europeanAqi,
            isAvailable = true,
            ozone = ozone,
            nitrogenDioxide = no2
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
