package com.example.expressiveweather.data

import com.example.expressiveweather.ui.components.WeatherWarningType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeteoAlarmRepositoryTest {

    @Test
    fun isSupported_detectsAll39MeteoAlarmCountriesAndMicrostates() {
        val paris = City(id = "paris", name = "パリ", nameEn = "Paris", country = "France", latitude = 48.8566, longitude = 2.3522)
        val berlin = City(id = "berlin", name = "ベルリン", nameEn = "Berlin", country = "Germany", latitude = 52.5200, longitude = 13.4050)
        val london = City(id = "london", name = "ロンドン", nameEn = "London", country = "United Kingdom", latitude = 51.5074, longitude = -0.1278)
        val rome = City(id = "rome", name = "ローマ", nameEn = "Rome", country = "Italy", latitude = 41.9028, longitude = 12.4964)
        val madrid = City(id = "madrid", name = "マドリード", nameEn = "Madrid", country = "Spain", latitude = 40.4168, longitude = -3.7038)
        val kyiv = City(id = "kyiv", name = "キーウ", nameEn = "Kyiv", country = "Ukraine", latitude = 50.4501, longitude = 30.5234)
        val telAviv = City(id = "telaviv", name = "テルアビブ", nameEn = "Tel Aviv", country = "Israel", latitude = 32.0853, longitude = 34.7818)
        val skopje = City(id = "skopje", name = "スコピエ", nameEn = "Skopje", country = "North Macedonia", latitude = 41.9981, longitude = 21.4254)
        val andorra = City(id = "andorra", name = "アンドラ", nameEn = "Andorra la Vella", country = "Andorra", latitude = 42.5063, longitude = 1.5218)
        val monaco = City(id = "monaco", name = "モナコ", nameEn = "Monaco", country = "Monaco", latitude = 43.7384, longitude = 7.4246)
        val vaduz = City(id = "vaduz", name = "ファドゥーツ", nameEn = "Vaduz", country = "Liechtenstein", latitude = 47.1410, longitude = 9.5209)
        val gibraltar = City(id = "gibraltar", name = "ジブラルタル", nameEn = "Gibraltar", country = "Gibraltar", latitude = 36.1408, longitude = -5.3536)

        assertTrue(MeteoAlarmRepository.isSupported(paris))
        assertTrue(MeteoAlarmRepository.isEuropeanCity(paris))
        assertTrue(MeteoAlarmRepository.isSupported(berlin))
        assertTrue(MeteoAlarmRepository.isSupported(london))
        assertTrue(MeteoAlarmRepository.isSupported(rome))
        assertTrue(MeteoAlarmRepository.isSupported(madrid))
        assertTrue(MeteoAlarmRepository.isSupported(kyiv))
        assertTrue(MeteoAlarmRepository.isSupported(telAviv))
        assertTrue(MeteoAlarmRepository.isSupported(skopje))
        assertTrue(MeteoAlarmRepository.isSupported(andorra))
        assertTrue(MeteoAlarmRepository.isSupported(monaco))
        assertTrue(MeteoAlarmRepository.isSupported(vaduz))
        assertTrue(MeteoAlarmRepository.isSupported(gibraltar))

        // Non-MeteoAlarm cities
        val tokyo = City(id = "tokyo", name = "東京", nameEn = "Tokyo", country = "日本", latitude = 35.6895, longitude = 139.6917)
        val newYork = City(id = "ny", name = "ニューヨーク", nameEn = "New York", country = "United States", latitude = 40.7128, longitude = -74.0060)
        val sydney = City(id = "sydney", name = "シドニー", nameEn = "Sydney", country = "Australia", latitude = -33.8688, longitude = 151.2093)
        val toronto = City(id = "toronto", name = "トロント", nameEn = "Toronto", country = "Canada", latitude = 43.6532, longitude = -79.3832)
        val seoul = City(id = "seoul", name = "ソウル", nameEn = "Seoul", country = "South Korea", latitude = 37.5665, longitude = 126.9780)
        val beijing = City(id = "beijing", name = "北京", nameEn = "Beijing", country = "China", latitude = 39.9042, longitude = 116.4074)

        assertFalse(MeteoAlarmRepository.isSupported(tokyo))
        assertFalse(MeteoAlarmRepository.isSupported(newYork))
        assertFalse(MeteoAlarmRepository.isSupported(sydney))
        assertFalse(MeteoAlarmRepository.isSupported(toronto))
        assertFalse(MeteoAlarmRepository.isSupported(seoul))
        assertFalse(MeteoAlarmRepository.isSupported(beijing))
    }

    @Test
    fun getCountrySlug_mapsCountryNamesAndCodes() {
        fun makeCity(country: String, nameEn: String = "", lat: Double = 0.0, lon: Double = 0.0) =
            City(id = "test", name = "Test", nameEn = nameEn, country = country, latitude = lat, longitude = lon)

        // 39カ国＆主要国
        assertEquals("france", MeteoAlarmRepository.getCountrySlug(makeCity("France")))
        assertEquals("france", MeteoAlarmRepository.getCountrySlug(makeCity("フランス")))
        assertEquals("france", MeteoAlarmRepository.getCountrySlug(makeCity("FR")))
        assertEquals("germany", MeteoAlarmRepository.getCountrySlug(makeCity("Germany")))
        assertEquals("germany", MeteoAlarmRepository.getCountrySlug(makeCity("ドイツ")))
        assertEquals("germany", MeteoAlarmRepository.getCountrySlug(makeCity("DE")))
        assertEquals("united-kingdom", MeteoAlarmRepository.getCountrySlug(makeCity("United Kingdom")))
        assertEquals("united-kingdom", MeteoAlarmRepository.getCountrySlug(makeCity("イギリス")))
        assertEquals("united-kingdom", MeteoAlarmRepository.getCountrySlug(makeCity("GB")))
        assertEquals("italy", MeteoAlarmRepository.getCountrySlug(makeCity("Italy")))
        assertEquals("italy", MeteoAlarmRepository.getCountrySlug(makeCity("イタリア")))
        assertEquals("spain", MeteoAlarmRepository.getCountrySlug(makeCity("Spain")))
        assertEquals("spain", MeteoAlarmRepository.getCountrySlug(makeCity("スペイン")))
        assertEquals("switzerland", MeteoAlarmRepository.getCountrySlug(makeCity("Switzerland")))
        assertEquals("austria", MeteoAlarmRepository.getCountrySlug(makeCity("Austria")))
        assertEquals("netherlands", MeteoAlarmRepository.getCountrySlug(makeCity("Netherlands")))

        // 拡張・検証済みスラグ
        assertEquals("ukraine", MeteoAlarmRepository.getCountrySlug(makeCity("Ukraine")))
        assertEquals("ukraine", MeteoAlarmRepository.getCountrySlug(makeCity("ウクライナ")))
        assertEquals("ukraine", MeteoAlarmRepository.getCountrySlug(makeCity("UA")))
        assertEquals("israel", MeteoAlarmRepository.getCountrySlug(makeCity("Israel")))
        assertEquals("israel", MeteoAlarmRepository.getCountrySlug(makeCity("イスラエル")))
        assertEquals("israel", MeteoAlarmRepository.getCountrySlug(makeCity("IL")))
        assertEquals("republic-of-north-macedonia", MeteoAlarmRepository.getCountrySlug(makeCity("North Macedonia")))
        assertEquals("republic-of-north-macedonia", MeteoAlarmRepository.getCountrySlug(makeCity("北マケドニア")))
        assertEquals("republic-of-north-macedonia", MeteoAlarmRepository.getCountrySlug(makeCity("MK")))
        assertEquals("andorra", MeteoAlarmRepository.getCountrySlug(makeCity("Andorra")))
        assertEquals("andorra", MeteoAlarmRepository.getCountrySlug(makeCity("アンドラ")))

        // マイクロステート / 関連地域
        assertEquals("france", MeteoAlarmRepository.getCountrySlug(makeCity("Monaco")))
        assertEquals("switzerland", MeteoAlarmRepository.getCountrySlug(makeCity("Liechtenstein")))
        assertEquals("italy", MeteoAlarmRepository.getCountrySlug(makeCity("San Marino")))
        assertEquals("italy", MeteoAlarmRepository.getCountrySlug(makeCity("Vatican")))
        assertEquals("united-kingdom", MeteoAlarmRepository.getCountrySlug(makeCity("Gibraltar")))

        // 座標フォールバック (国名未特定でも座標が欧州・イスラエル内にある場合)
        assertEquals("ukraine", MeteoAlarmRepository.getCountrySlug(makeCity("", "", lat = 50.45, lon = 30.52)))
        assertEquals("israel", MeteoAlarmRepository.getCountrySlug(makeCity("", "", lat = 32.08, lon = 34.78)))
    }

    @Test
    fun meteoAlarmGuard_blocksNonEuropeanCities() = runBlocking {
        val tokyo = City(id = "tokyo", name = "東京", nameEn = "Tokyo", country = "日本", latitude = 35.6895, longitude = 139.6917)
        val newYork = City(id = "ny", name = "ニューヨーク", nameEn = "New York", country = "United States", latitude = 40.7128, longitude = -74.0060)

        val alertsTokyo = MeteoAlarmRepository.fetchAlertsForCity(tokyo)
        assertTrue(alertsTokyo.isEmpty())

        val alertsNy = MeteoAlarmRepository.fetchAlertsForCity(newYork)
        assertTrue(alertsNy.isEmpty())
    }

    @Test
    fun sampleAlerts_containThreeDistinctSeverities() {
        val samples = MeteoAlarmRepository.createSampleAlerts()
        assertEquals(3, samples.size)

        // 1. 赤色: 特別警報 (SPECIAL_WARNING / Level 4 Red)
        val redAlert = samples.find { it.type == WeatherWarningType.SPECIAL_WARNING }
        assertNotNull(redAlert)
        assertEquals(AlertSeverity.EXTREME, redAlert?.severity)
        assertEquals(AlertSource.METEOALARM, redAlert?.source)

        // 2. 橙色: 警報 (WARNING / Level 3 Orange)
        val orangeAlert = samples.find { it.type == WeatherWarningType.WARNING }
        assertNotNull(orangeAlert)
        assertEquals(AlertSeverity.SEVERE, orangeAlert?.severity)
        assertEquals(AlertSource.METEOALARM, orangeAlert?.source)

        // 3. 黄色: 注意報 (ADVISORY / Level 2 Yellow)
        val yellowAlert = samples.find { it.type == WeatherWarningType.ADVISORY }
        assertNotNull(yellowAlert)
        assertEquals(AlertSeverity.MODERATE, yellowAlert?.severity)
        assertEquals(AlertSource.METEOALARM, yellowAlert?.source)
    }

    @Test
    fun meteoAlarmEventTranslation_supportsFourLanguages() {
        // Wind
        assertEquals("強風・暴風警報", GlobalAlertItem.translateMeteoAlarmEvent("Wind", AppLanguage.JAPANESE))
        assertEquals("Wind", GlobalAlertItem.translateMeteoAlarmEvent("Wind", AppLanguage.ENGLISH))
        assertEquals("大风强风预警", GlobalAlertItem.translateMeteoAlarmEvent("Wind", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("強風大風警報", GlobalAlertItem.translateMeteoAlarmEvent("Wind", AppLanguage.TRADITIONAL_CHINESE))

        // Snow / Ice
        assertEquals("大雪・路面凍結警報", GlobalAlertItem.translateMeteoAlarmEvent("Snow/Ice", AppLanguage.JAPANESE))
        assertEquals("Snow/Ice", GlobalAlertItem.translateMeteoAlarmEvent("Snow/Ice", AppLanguage.ENGLISH))
        assertEquals("暴雪道路结冰预警", GlobalAlertItem.translateMeteoAlarmEvent("Snow/Ice", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("暴雪道路結冰警報", GlobalAlertItem.translateMeteoAlarmEvent("Snow/Ice", AppLanguage.TRADITIONAL_CHINESE))

        // Thunderstorm
        assertEquals("雷雨・落雷警報", GlobalAlertItem.translateMeteoAlarmEvent("Thunderstorm", AppLanguage.JAPANESE))
        assertEquals("Thunderstorm", GlobalAlertItem.translateMeteoAlarmEvent("Thunderstorm", AppLanguage.ENGLISH))
        assertEquals("强雷暴对流预警", GlobalAlertItem.translateMeteoAlarmEvent("Thunderstorm", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("強雷雨對流警報", GlobalAlertItem.translateMeteoAlarmEvent("Thunderstorm", AppLanguage.TRADITIONAL_CHINESE))

        // Rain / Flooding
        assertEquals("大雨・豪雨警報", GlobalAlertItem.translateMeteoAlarmEvent("Rain", AppLanguage.JAPANESE))
        assertEquals("洪水警報", GlobalAlertItem.translateMeteoAlarmEvent("Flooding", AppLanguage.JAPANESE))

        // High Temperature / Low Temperature
        assertEquals("猛暑・極度高温警報", GlobalAlertItem.translateMeteoAlarmEvent("High Temperature", AppLanguage.JAPANESE))
        assertEquals("低温・極度寒波警報", GlobalAlertItem.translateMeteoAlarmEvent("Low Temperature", AppLanguage.JAPANESE))

        // Coastal Event / Forest Fire / Avalanches
        assertEquals("沿岸高波・波浪警報", GlobalAlertItem.translateMeteoAlarmEvent("Coastal Event", AppLanguage.JAPANESE))
        assertEquals("森林火災警報", GlobalAlertItem.translateMeteoAlarmEvent("Forest Fire", AppLanguage.JAPANESE))
        assertEquals("雪崩警報", GlobalAlertItem.translateMeteoAlarmEvent("Avalanches", AppLanguage.JAPANESE))
    }
}
