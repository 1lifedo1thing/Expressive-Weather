package io.github.nemynew.expressiveweather.ui.haptics

import android.os.Build
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.HapticIntensity
import io.github.nemynew.expressiveweather.data.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressiveHapticsTest {

    @Test
    fun hapticIntensityFromIdResolvesAndFallsBackSafely() {
        assertEquals(HapticIntensity.OFF, HapticIntensity.fromId("off"))
        assertEquals(HapticIntensity.GENTLE, HapticIntensity.fromId("gentle"))
        assertEquals(HapticIntensity.STANDARD, HapticIntensity.fromId("standard"))
        assertEquals(HapticIntensity.EXPRESSIVE, HapticIntensity.fromId("expressive"))

        assertEquals(HapticIntensity.STANDARD, HapticIntensity.fromId(null))
        assertEquals(HapticIntensity.STANDARD, HapticIntensity.fromId("unknown_preset"))
        assertEquals("標準 (Expressive)", HapticIntensity.STANDARD.getLabel(AppLanguage.JAPANESE))
        assertEquals("Dynamic (Strong)", HapticIntensity.EXPRESSIVE.getLabel(AppLanguage.ENGLISH))
    }

    @Test
    fun detectDeviceProfileMapsHardwareTiersAccurately() {
        assertEquals(
            HapticDeviceProfile.NONE,
            ExpressiveHapticProfiles.detectDeviceProfile(
                manufacturer = "Google",
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                hasVibrator = false,
                hasAmplitudeControl = true,
                supportedPrimitivesCount = 8
            )
        )

        assertEquals(
            HapticDeviceProfile.SAMSUNG_HD_LRA,
            ExpressiveHapticProfiles.detectDeviceProfile(
                manufacturer = "samsung",
                sdkInt = Build.VERSION_CODES.Q,
                hasVibrator = true,
                hasAmplitudeControl = true,
                supportedPrimitivesCount = 2
            )
        )

        assertEquals(
            HapticDeviceProfile.HIGH_DEF_LRA,
            ExpressiveHapticProfiles.detectDeviceProfile(
                manufacturer = "Google",
                sdkInt = Build.VERSION_CODES.R,
                hasVibrator = true,
                hasAmplitudeControl = true,
                supportedPrimitivesCount = 5
            )
        )

        assertEquals(
            HapticDeviceProfile.AMPLITUDE_WAVEFORM,
            ExpressiveHapticProfiles.detectDeviceProfile(
                manufacturer = "Sony",
                sdkInt = Build.VERSION_CODES.O,
                hasVibrator = true,
                hasAmplitudeControl = true,
                supportedPrimitivesCount = 0
            )
        )

        assertEquals(
            HapticDeviceProfile.LEGACY_TIMED_ERM,
            ExpressiveHapticProfiles.detectDeviceProfile(
                manufacturer = "Generic",
                sdkInt = Build.VERSION_CODES.N_MR1,
                hasVibrator = true,
                hasAmplitudeControl = false,
                supportedPrimitivesCount = 0
            )
        )
    }

    @Test
    fun scaleAmplitudeEnforcesSamsungCeilingAndOffZero() {
        assertEquals(
            0,
            ExpressiveHapticProfiles.scaleAmplitude(
                baseAmplitude = 200,
                intensity = HapticIntensity.OFF,
                profile = HapticDeviceProfile.HIGH_DEF_LRA
            )
        )
        assertEquals(
            0,
            ExpressiveHapticProfiles.scaleAmplitude(
                baseAmplitude = 0,
                intensity = HapticIntensity.EXPRESSIVE,
                profile = HapticDeviceProfile.SAMSUNG_HD_LRA
            )
        )

        val samsungMax = ExpressiveHapticProfiles.scaleAmplitude(
            baseAmplitude = 250,
            intensity = HapticIntensity.EXPRESSIVE,
            profile = HapticDeviceProfile.SAMSUNG_HD_LRA
        )
        assertEquals(215, samsungMax)

        val pixelMax = ExpressiveHapticProfiles.scaleAmplitude(
            baseAmplitude = 250,
            intensity = HapticIntensity.EXPRESSIVE,
            profile = HapticDeviceProfile.HIGH_DEF_LRA
        )
        assertEquals(255, pixelMax)

        val minimumPositive = ExpressiveHapticProfiles.scaleAmplitude(
            baseAmplitude = 10,
            intensity = HapticIntensity.GENTLE,
            profile = HapticDeviceProfile.AMPLITUDE_WAVEFORM
        )
        assertEquals(14, minimumPositive)
    }

    @Test
    fun allPatternsProduceValidWaveformsAndLegacyCadences() {
        val profiles = listOf(
            HapticDeviceProfile.HIGH_DEF_LRA,
            HapticDeviceProfile.SAMSUNG_HD_LRA,
            HapticDeviceProfile.AMPLITUDE_WAVEFORM,
            HapticDeviceProfile.LEGACY_TIMED_ERM
        )

        for (pattern in ExpressiveHapticPattern.entries) {
            for (profile in profiles) {
                for (intensity in HapticIntensity.entries) {
                    val spec = ExpressiveHapticProfiles.buildWaveformSpec(pattern, intensity, profile)
                    assertTrue(
                        "Empty timings for $pattern / $profile",
                        spec.timingsMs.isNotEmpty()
                    )
                    assertEquals(spec.timingsMs.size, spec.amplitudes.size)
                    assertTrue(spec.timingsMs.all { it > 0L })
                    assertTrue(spec.amplitudes.all { it in 0..255 })
                    if (profile == HapticDeviceProfile.SAMSUNG_HD_LRA) {
                        assertTrue(
                            "Samsung cap exceeded for $pattern / $intensity",
                            spec.amplitudes.all { it <= 215 }
                        )
                    }
                    if (intensity == HapticIntensity.OFF) {
                        assertTrue(spec.amplitudes.all { it == 0 })
                    }

                    val legacy = ExpressiveHapticProfiles.toLegacyOnOffPattern(spec)
                    assertEquals(spec.timingsMs.size + 1, legacy.size)
                    assertEquals(0L, legacy[0])
                    assertTrue(legacy.drop(1).all { it >= 1L })
                }
            }
        }
    }

    @Test
    fun weatherConditionMappingCoversStormRainSnowWindAndClear() {
        assertEquals(
            ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE,
            ExpressiveHapticPattern.forWeatherCondition(WeatherCondition.THUNDERSTORM)
        )
        assertEquals(
            ExpressiveHapticPattern.WEATHER_RAINDROP_PATTER,
            ExpressiveHapticPattern.forWeatherCondition(WeatherCondition.RAIN)
        )
        assertEquals(
            ExpressiveHapticPattern.WEATHER_RAINDROP_PATTER,
            ExpressiveHapticPattern.forWeatherCondition(WeatherCondition.HEAVY_RAIN)
        )
        assertEquals(
            ExpressiveHapticPattern.WEATHER_SNOW_CRISP,
            ExpressiveHapticPattern.forWeatherCondition(WeatherCondition.SNOW)
        )
        assertNull(ExpressiveHapticPattern.forWeatherCondition(WeatherCondition.FOG))
        assertNull(ExpressiveHapticPattern.forWeatherCondition(WeatherCondition.CLEAR))
        assertNotNull(
            ExpressiveHapticProfiles.buildWaveformSpec(
                ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE,
                HapticIntensity.STANDARD,
                HapticDeviceProfile.SAMSUNG_HD_LRA
            )
        )
    }
}
