package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JmaTyphoonRepositoryTest {

    @Test
    fun intensity_parsingIsAccurate() {
        assertEquals(JmaTyphoonIntensity.VIOLENT, JmaTyphoonIntensity.fromJmaString("猛烈な"))
        assertEquals(JmaTyphoonIntensity.VERY_STRONG, JmaTyphoonIntensity.fromJmaString("非常に強い"))
        assertEquals(JmaTyphoonIntensity.STRONG, JmaTyphoonIntensity.fromJmaString("強い"))
        assertEquals(JmaTyphoonIntensity.NONE, JmaTyphoonIntensity.fromJmaString("-"))
        assertEquals(JmaTyphoonIntensity.NONE, JmaTyphoonIntensity.fromJmaString(null))
    }

    @Test
    fun scale_parsingIsAccurate() {
        assertEquals(JmaTyphoonScale.VERY_LARGE, JmaTyphoonScale.fromJmaString("超大型"))
        assertEquals(JmaTyphoonScale.LARGE, JmaTyphoonScale.fromJmaString("大型"))
        assertEquals(JmaTyphoonScale.NONE, JmaTyphoonScale.fromJmaString("-"))
        assertEquals(JmaTyphoonScale.NONE, JmaTyphoonScale.fromJmaString(""))
    }

    @Test
    fun officialClassification_combinesScaleAndIntensityCorrectly() {
        val sample = JmaTyphoonRepository.createSampleTyphoon()
        assertEquals("大型で非常に強い台風", sample.analysis.officialClassification(AppLanguage.JAPANESE))
        assertEquals("Large & Very Strong Typhoon", sample.analysis.officialClassification(AppLanguage.ENGLISH))
        assertEquals("大型强台风", sample.analysis.officialClassification(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("大型中度颱風(強)", sample.analysis.officialClassification(AppLanguage.TRADITIONAL_CHINESE))
    }

    @Test
    fun distanceAndBearing_calculationIsAccurate() {
        // 東京 (35.6895, 139.6917) から 小笠原 (27.09, 142.19)
        val dist = JmaTyphoonRepository.calculateDistanceKm(35.6895, 139.6917, 27.09, 142.19)
        assertTrue("Distance should be around 980-1020km but was $dist", dist in 950.0..1050.0)

        val bearingJa = JmaTyphoonRepository.calculateBearingDirection(35.6895, 139.6917, 27.09, 142.19, AppLanguage.JAPANESE)
        assertEquals("南南東", bearingJa)
    }

    @Test
    fun sampleTyphoon_hasCompleteForecastsAndGeometry() {
        val sample = JmaTyphoonRepository.createSampleTyphoon()
        assertEquals("第27号", sample.formattedNumber)
        assertEquals("チョーイワン", sample.nameJp)
        assertNotNull(sample.analysis)
        assertEquals(945, sample.analysis.pressureHpa)
        assertEquals(45, sample.analysis.maxWindMs)
        assertEquals(60, sample.analysis.gustWindMs)
        assertEquals(3, sample.forecasts.size)
        assertEquals(3, sample.probabilityCircles.size)
        assertTrue(sample.pastTrack.isNotEmpty())
    }
}
