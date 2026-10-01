package com.example.expressiveweather.data

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
