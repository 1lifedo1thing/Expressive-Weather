package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 降雨予測 Live Updates およびバックグラウンド現在位置追従ユニットテスト
 */
class PrecipitationLiveLocationFollowTest {

    @Test
    fun testPrecipitationNowcastLiveActiveStatus() {
        val rainingNowcast = PrecipitationNowcast(
            status = RainStatus.RAINING,
            headline = "雨は21:45頃まで続く見込み",
            subText = "雨が止むまであと約35分です",
            targetTimeLabel = "21:45",
            minutesRemaining = 35,
            currentIntensityMm = 4.5,
            isLiveActive = true
        )
        assertTrue(rainingNowcast.isLiveActive)
        assertEquals(RainStatus.RAINING, rainingNowcast.status)
        assertEquals(35, rainingNowcast.minutesRemaining)

        val approachingNowcast = PrecipitationNowcast(
            status = RainStatus.RAIN_APPROACHING,
            headline = "まもなく雨が降り始めます",
            subText = "20:15頃から雨の予想（あと約15分）",
            targetTimeLabel = "20:15",
            minutesRemaining = 15,
            isLiveActive = true
        )
        assertTrue(approachingNowcast.isLiveActive)
        assertEquals(RainStatus.RAIN_APPROACHING, approachingNowcast.status)

        val dryNowcast = PrecipitationNowcast(
            status = RainStatus.DRY,
            headline = "当面、雨の心配はありません",
            subText = "今後数時間は傘の必要はありません",
            isLiveActive = false
        )
        assertFalse(dryNowcast.isLiveActive)
        assertEquals(RainStatus.DRY, dryNowcast.status)
    }

    @Test
    fun testPrecipitationNowcastMultilingualTranslations() {
        val nowcast = PrecipitationNowcast(
            status = RainStatus.RAINING,
            headline = "雨は21:45頃まで続く見込み",
            subText = "雨が止むまであと約35分です",
            targetTimeLabel = "21:45",
            minutesRemaining = 35,
            isLiveActive = true,
            localizedHeadlines = mapOf(
                AppLanguage.JAPANESE.code to "雨は21:45頃まで続く見込み",
                AppLanguage.ENGLISH.code to "Rain continuing until ~21:45",
                AppLanguage.SIMPLIFIED_CHINESE.code to "预计下雨至 21:45 左右",
                AppLanguage.TRADITIONAL_CHINESE.code to "預計下雨至 21:45 左右"
            ),
            localizedSubTexts = mapOf(
                AppLanguage.JAPANESE.code to "雨が止むまであと約35分です",
                AppLanguage.ENGLISH.code to "About 35 min until rain stops",
                AppLanguage.SIMPLIFIED_CHINESE.code to "距离雨停大约还有 35 分钟",
                AppLanguage.TRADITIONAL_CHINESE.code to "距離雨停大約還有 35 分鐘"
            )
        )

        assertEquals("雨は21:45頃まで続く見込み", nowcast.localizedHeadline(AppLanguage.JAPANESE))
        assertEquals("Rain continuing until ~21:45", nowcast.localizedHeadline(AppLanguage.ENGLISH))
        assertEquals("预计下雨至 21:45 左右", nowcast.localizedHeadline(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("預計下雨至 21:45 左右", nowcast.localizedHeadline(AppLanguage.TRADITIONAL_CHINESE))

        assertEquals("雨が止むまであと約35分です", nowcast.localizedSubText(AppLanguage.JAPANESE))
        assertEquals("About 35 min until rain stops", nowcast.localizedSubText(AppLanguage.ENGLISH))
        assertEquals("距离雨停大约还有 35 分钟", nowcast.localizedSubText(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("距離雨停大約還有 35 分鐘", nowcast.localizedSubText(AppLanguage.TRADITIONAL_CHINESE))
    }

    @Test
    fun testLocationShiftDistanceCalculation() {
        // 東京駅 (35.6812, 139.7671) から 有楽町駅 (35.6750, 139.7633) への距離 (約750m)
        val lat1 = 35.6812
        val lon1 = 139.7671
        val lat2 = 35.6750
        val lon2 = 139.7633

        val distanceMeters = computeHaversineDistance(lat1, lon1, lat2, lon2)
        assertTrue("Distance should be around 750m, actual: $distanceMeters", distanceMeters in 700.0..800.0)

        // 75m以上の移動で再評価しきい値をクリアすることを確認
        val threshold = 75.0
        assertTrue(distanceMeters >= threshold)

        // わずかなGPSジッター (10m移動) ではしきい値未満であることを確認
        val jitterLat = 35.68128
        val jitterLon = 139.76718
        val jitterDistance = computeHaversineDistance(lat1, lon1, jitterLat, jitterLon)
        assertTrue("Jitter distance should be < 20m, actual: $jitterDistance", jitterDistance < 20.0)
        assertFalse(jitterDistance >= threshold)
    }

    @Test
    fun testNowcastMinutesCountdownLogic() {
        val now = System.currentTimeMillis()
        val targetMillis = now + 25 * 60 * 1000L // 25分後
        val nowcast = PrecipitationNowcast(
            status = RainStatus.RAINING,
            headline = "雨は続く見込み",
            subText = "止むまであと約25分",
            targetTimestampMillis = targetMillis,
            minutesRemaining = 25,
            isLiveActive = true
        )

        // 1分経過時のシミュレーション
        val simulatedNow = now + 60 * 1000L
        val remainingMin = ((targetMillis - simulatedNow) / 60000L).toInt()
        assertEquals(24, remainingMin)

        val updated = nowcast.copy(minutesRemaining = remainingMin)
        assertEquals(24, updated.minutesRemaining)

        // 終了時刻到達時のシミュレーション
        val finishTime = targetMillis + 1000L
        val finishedRemaining = ((targetMillis - finishTime) / 60000L).toInt()
        assertTrue(finishedRemaining <= 0)
    }

    @Test
    fun testFallbackCurrentCityAvoidsTokyoWhenRealLocationOrSelectionExists() {
        val repo = WeatherRepository()
        assertNull(repo.resolveFallbackCurrentCityOrNull())
        assertEquals("東京", repo.resolveFallbackCurrentCity().name)

        // 手動選択都市（デフォルト以外）がある場合は東京に落ちない
        val osaka = City(
            id = "osaka",
            name = "大阪市",
            nameEn = "Osaka",
            country = "日本",
            latitude = 34.6937,
            longitude = 135.5023
        )
        repo.setLastSelectedCity(osaka)
        assertEquals("大阪市", repo.resolveFallbackCurrentCityOrNull()?.name)
        assertEquals("大阪市", repo.resolveFallbackCurrentCity().name)

        // 実測GPS解決都市が記録されていれば、選択都市より実測現在地を優先する
        val nagoyaGps = City(
            id = "current",
            name = "名古屋市中区",
            nameEn = "Naka Ward, Nagoya",
            country = "日本",
            latitude = 35.1815,
            longitude = 136.9066
        )
        repo.setLastResolvedLocationCity(nagoyaGps)
        assertEquals("名古屋市中区", repo.resolveFallbackCurrentCityOrNull()?.name)
        assertEquals("名古屋市中区", repo.resolveFallbackCurrentCity().name)
    }

    @Test
    fun testMockNowcastApproachingAndRainingLiveFlags() {
        val approaching = PrecipitationAnalyzer.createMock(RainStatus.RAIN_APPROACHING)
        assertTrue(approaching.isLiveActive)
        assertEquals(RainStatus.RAIN_APPROACHING, approaching.status)
        assertNotNull(approaching.targetTimestampMillis)
        assertTrue((approaching.minutesRemaining ?: 0) > 0)

        val raining = PrecipitationAnalyzer.createMock(RainStatus.RAINING)
        assertTrue(raining.isLiveActive)
        assertEquals(RainStatus.RAINING, raining.status)
        assertTrue((raining.minutesRemaining ?: 0) > 0)

        val tonight = PrecipitationAnalyzer.createMock(RainStatus.RAIN_EXPECTED_TONIGHT)
        assertFalse(tonight.isLiveActive)
    }

    @Test
    fun testUserDismissalSuppressesRepeatedRainNotificationsForSameEvent() {
        val now = 1_700_000_000_000L
        val targetStart = now + 20 * 60 * 1000L // 20分後に降り始め
        val approaching = PrecipitationNowcast(
            status = RainStatus.RAIN_APPROACHING,
            headline = "まもなく雨が降り始めます",
            subText = "あと約20分",
            targetTimestampMillis = targetStart,
            minutesRemaining = 20,
            isLiveActive = true
        )
        val approachingNextTick = approaching.copy(minutesRemaining = 19)
        val rainingSameShower = PrecipitationNowcast(
            status = RainStatus.RAINING,
            headline = "雨が降っています",
            subText = "あと約40分で止む見込み",
            targetTimestampMillis = now + 60 * 60 * 1000L,
            minutesRemaining = 40,
            isLiveActive = true
        )

        val dismissal = PrecipitationLiveNotificationManager.createDismissalRecord(
            cityName = "千代田区",
            status = RainStatus.RAIN_APPROACHING,
            targetTimestampMillis = targetStart,
            nowMillis = now
        )

        // 消去直後〜1〜2分後の再更新（同一都市・接近中・同じ雨の降り始め）はすべて抑止される
        assertTrue(
            PrecipitationLiveNotificationManager.shouldSuppressNotification(
                dismissal = dismissal,
                nowcast = approachingNextTick,
                cityName = "千代田区",
                nowMillis = now + 60 * 1000L
            )
        )
        assertTrue(
            PrecipitationLiveNotificationManager.shouldSuppressNotification(
                dismissal = dismissal,
                nowcast = rainingSameShower,
                cityName = "千代田区",
                nowMillis = now + 25 * 60 * 1000L
            )
        )

        // 別都市へ移動した場合は抑止されない
        assertFalse(
            PrecipitationLiveNotificationManager.shouldSuppressNotification(
                dismissal = dismissal,
                nowcast = approachingNextTick,
                cityName = "大阪市",
                nowMillis = now + 60 * 1000L
            )
        )

        // 抑止期間（最低2時間）経過後は新しい雨雲接近で再び通知可能
        assertFalse(
            PrecipitationLiveNotificationManager.shouldSuppressNotification(
                dismissal = dismissal,
                nowcast = approaching,
                cityName = "千代田区",
                nowMillis = dismissal.suppressUntilMillis + 1L
            )
        )
    }

    @Test
    fun testDismissalRecordBoundsAndJsonSerialization() {
        val now = 1_700_000_000_000L
        val untimed = PrecipitationLiveNotificationManager.createDismissalRecord(
            cityName = "東京都",
            status = RainStatus.RAINING,
            targetTimestampMillis = null,
            nowMillis = now
        )
        assertEquals(now + 3 * 60 * 60 * 1000L, untimed.suppressUntilMillis)

        // 異常に遠い未来タイムスタンプでも最大6時間で上限クリップされる
        val farFuture = PrecipitationLiveNotificationManager.createDismissalRecord(
            cityName = "東京都",
            status = RainStatus.RAIN_APPROACHING,
            targetTimestampMillis = now + 48 * 60 * 60 * 1000L,
            nowMillis = now
        )
        assertEquals(now + 6 * 60 * 60 * 1000L, farFuture.suppressUntilMillis)

        val encoded = WeatherJsonSerializer.json.encodeToString(RainNotificationDismissal.serializer(), untimed)
        val decoded = WeatherJsonSerializer.json.decodeFromString(RainNotificationDismissal.serializer(), encoded)
        assertEquals(untimed, decoded)
    }

    @Test
    fun testKikikuruAlertSignatureDeduplicationAndEscalationPolicy() {
        val now = 1_700_000_000_000L
        val warnA = MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true)
        val warnB = MunicipalityWarningItem("04", "洪水警報", "発表", isWarning = true)

        val alertOrder1 = MunicipalityAlertInfo(
            municipalityCode = "1310100",
            municipalityName = "千代田区",
            alertLevel = AlertLevel.LEVEL_3,
            activeWarnings = listOf(warnA, warnB)
        )
        val alertOrder2 = alertOrder1.copy(activeWarnings = listOf(warnB, warnA))
        assertEquals(
            KikikuruNotificationManager.buildAlertSignature(alertOrder1),
            KikikuruNotificationManager.buildAlertSignature(alertOrder2)
        )

        val dismissalLv2 = KikikuruAlertDismissal(
            municipalityCode = "1310100",
            dismissedLevel = 2,
            dismissedSignature = "1310100|lv=2",
            dismissedAtMillis = now,
            suppressUntilMillis = now + 3 * 60 * 60 * 1000L
        )
        val sameLevelAlert = alertOrder1.copy(alertLevel = AlertLevel.LEVEL_2)
        val escalatedAlert = alertOrder1.copy(alertLevel = AlertLevel.LEVEL_3)
        val otherMuniAlert = sameLevelAlert.copy(municipalityCode = "2710000", municipalityName = "大阪市")

        // 同一自治体・同一レベル以下は再通知を抑止
        assertTrue(KikikuruNotificationManager.shouldSuppressAlert(dismissalLv2, sameLevelAlert, now + 60_000L))
        // レベル昇格時は安全のため抑止を解除して必ず再通知する
        assertFalse(KikikuruNotificationManager.shouldSuppressAlert(dismissalLv2, escalatedAlert, now + 60_000L))
        // 別自治体へ移動した場合は抑止しない
        assertFalse(KikikuruNotificationManager.shouldSuppressAlert(dismissalLv2, otherMuniAlert, now + 60_000L))
        // 抑止期限切れ後は再通知可能
        assertFalse(KikikuruNotificationManager.shouldSuppressAlert(dismissalLv2, sameLevelAlert, dismissalLv2.suppressUntilMillis + 1L))
    }

    @Test
    fun testNotificationsExcludeUnregisteredCitiesAndPlaceholderTokyo() = kotlinx.coroutines.runBlocking {
        val osaka = City("osaka", "大阪市", "Osaka", "日本", 34.6937, 135.5023)
        val sapporoUnregistered = City("sapporo", "札幌市", "Sapporo", "日本", 43.0618, 141.3545)
        val gpsNagoya = City("current", "名古屋市", "Nagoya", "日本", 35.1815, 136.9066)

        val repo = WeatherRepository()
        // 未登録都市を閲覧（lastSelectedCity に保存）しただけでは通知対象にならない
        repo.setLastSelectedCity(sapporoUnregistered)
        assertNull(repo.resolveNotificationTargetCityOrNull(hasLocationPermission = false, liveGpsCity = null))
        assertFalse(
            WeatherRepository.isCityEligibleForNotifications(
                city = sapporoUnregistered,
                hasLocationPermission = false,
                resolvedGpsCity = null,
                savedCities = listOf(osaka)
            )
        )
        // 未登録のデフォルト東京も通知対象にならない
        assertFalse(
            WeatherRepository.isCityEligibleForNotifications(
                city = repo.defaultCity,
                hasLocationPermission = false,
                resolvedGpsCity = null,
                savedCities = listOf(osaka)
            )
        )

        // 登録済み都市は通知対象になる
        repo.saveCity(osaka)
        assertTrue(
            WeatherRepository.isCityEligibleForNotifications(
                city = osaka,
                hasLocationPermission = false,
                resolvedGpsCity = null,
                savedCities = listOf(osaka)
            )
        )
        assertEquals("大阪市", repo.resolveNotificationTargetCityOrNull(hasLocationPermission = false)?.name)

        // 位置情報権限あり＋実測GPS都市は通知対象になる
        assertTrue(
            WeatherRepository.isCityEligibleForNotifications(
                city = gpsNagoya,
                hasLocationPermission = true,
                resolvedGpsCity = gpsNagoya,
                savedCities = listOf(osaka)
            )
        )
        assertEquals("名古屋市", repo.resolveNotificationTargetCityOrNull(hasLocationPermission = true, liveGpsCity = gpsNagoya)?.name)
    }

    private fun computeHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // 地球の半径 (メートル)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
