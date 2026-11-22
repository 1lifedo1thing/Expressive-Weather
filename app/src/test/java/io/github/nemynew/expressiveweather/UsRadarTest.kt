package io.github.nemynew.expressiveweather

import io.github.nemynew.expressiveweather.data.RadarDataSource
import io.github.nemynew.expressiveweather.data.RadarProviderRegistry
import io.github.nemynew.expressiveweather.data.RadarSourceMode
import io.github.nemynew.expressiveweather.data.UsRadarFrameType
import io.github.nemynew.expressiveweather.data.UsRadarLayerFilter
import io.github.nemynew.expressiveweather.data.UsRadarRepository
import io.github.nemynew.expressiveweather.data.isLocationInUnitedStates
import io.github.nemynew.expressiveweather.data.isLocationOrCountryInUnitedStates
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsRadarTest {

    @Test
    fun testUsLocationDetection() {
        // 米国本土 (CONUS)
        assertTrue(isLocationInUnitedStates(37.7749, -122.4194)) // San Francisco, CA
        assertTrue(isLocationInUnitedStates(40.7128, -74.0060))  // New York, NY
        assertTrue(isLocationInUnitedStates(25.7617, -80.1918))  // Miami, FL
        assertTrue(isLocationInUnitedStates(47.6062, -122.3321)) // Seattle, WA

        // ハワイ & アラスカ
        assertTrue(isLocationInUnitedStates(21.3069, -157.8583)) // Honolulu, HI
        assertTrue(isLocationInUnitedStates(61.2181, -149.9003)) // Anchorage, AK

        // 日本・海外 (米国外)
        assertFalse(isLocationInUnitedStates(35.6895, 139.6917)) // Tokyo
        assertFalse(isLocationInUnitedStates(48.8566, 2.3522))   // Paris
        assertFalse(isLocationInUnitedStates(51.5074, -0.1278))  // London

        // 国名判定
        assertTrue(isLocationOrCountryInUnitedStates("United States", 37.0, -120.0))
        assertTrue(isLocationOrCountryInUnitedStates("USA", 37.0, -120.0))
        assertTrue(isLocationOrCountryInUnitedStates("アメリカ合衆国", 37.0, -120.0))
        assertFalse(isLocationOrCountryInUnitedStates("日本", 35.6895, 139.6917))

        // カナダ・メキシコ等の国境付近都市が明示的な国名を持つ場合は米国と誤判定しないこと
        assertFalse(isLocationOrCountryInUnitedStates("Canada", 43.6532, -79.3832)) // Toronto
        assertFalse(isLocationOrCountryInUnitedStates("カナダ", 49.2827, -123.1207)) // Vancouver
        assertFalse(isLocationOrCountryInUnitedStates("Mexico", 32.5149, -117.0382)) // Tijuana
    }

    @Test
    fun testProviderResolution() {
        // 日本座標なら JMA
        val tokyoSource = RadarProviderRegistry.resolveProvider(
            mode = RadarSourceMode.AUTO,
            latitude = 35.6895,
            longitude = 139.6917,
            country = "日本"
        )
        assertEquals(RadarDataSource.JMA_NOWCAST, tokyoSource)

        // 米国座標なら US_NEXRAD_HRRR
        val nySource = RadarProviderRegistry.resolveProvider(
            mode = RadarSourceMode.AUTO,
            latitude = 40.7128,
            longitude = -74.0060,
            country = "United States"
        )
        assertEquals(RadarDataSource.US_NEXRAD_HRRR, nySource)

        // その他海外なら RAIN_VIEWER
        val parisSource = RadarProviderRegistry.resolveProvider(
            mode = RadarSourceMode.AUTO,
            latitude = 48.8566,
            longitude = 2.3522,
            country = "France"
        )
        assertEquals(RadarDataSource.RAIN_VIEWER, parisSource)

        // 明示的指定ならモードに従う
        val explicitUs = RadarProviderRegistry.resolveProvider(
            mode = RadarSourceMode.US_NEXRAD_HRRR,
            latitude = 35.6895,
            longitude = 139.6917,
            country = "日本"
        )
        assertEquals(RadarDataSource.US_NEXRAD_HRRR, explicitUs)
    }

    @Test
    fun testUsRadarTimelineGeneration() = runBlocking {
        val repo = UsRadarRepository.getInstance()
        val result = repo.getTimelineFrames(37.7749, -122.4194)

        assertTrue(result.isSuccess)
        val frames = result.getOrThrow()
        assertTrue(frames.isNotEmpty())

        // 過去実況、外挿予測、HRRR予報の3層が含まれていること
        val pastFrames = frames.filter { !it.isForecast && !it.isExtrapolation }
        val extrapolationFrames = frames.filter { it.isExtrapolation }
        val hrrrForecastFrames = frames.filter { it.isForecast && !it.isExtrapolation }

        assertTrue("Past frames should not be empty", pastFrames.isNotEmpty())
        assertTrue("Extrapolation nowcast frames should not be empty", extrapolationFrames.isNotEmpty())
        assertTrue("HRRR Forecast frames should not be empty", hrrrForecastFrames.isNotEmpty())

        // 過去フレームは MRMS
        assertEquals(UsRadarFrameType.MRMS_BASE_REFLECTIVITY, pastFrames.first().frameType)

        // 外挿予測フレームは NEXRAD_NOWCAST_EXTRAPOLATION
        assertEquals(UsRadarFrameType.NEXRAD_NOWCAST_EXTRAPOLATION, extrapolationFrames.first().frameType)

        // 未来フレームは HRRR
        assertEquals(UsRadarFrameType.HRRR_SIMULATED_REFLECTIVITY, hrrrForecastFrames.first().frameType)

        // フィルター動作のテスト
        val obsOnly = repo.getTimelineFrames(37.7749, -122.4194, filter = UsRadarLayerFilter.OBSERVATION_ONLY).getOrThrow()
        assertTrue(obsOnly.all { it.frameType == UsRadarFrameType.MRMS_BASE_REFLECTIVITY })

        val extrapOnly = repo.getTimelineFrames(37.7749, -122.4194, filter = UsRadarLayerFilter.EXTRAPOLATION_ONLY).getOrThrow()
        assertTrue(extrapOnly.all { it.frameType == UsRadarFrameType.NEXRAD_NOWCAST_EXTRAPOLATION })

        val hrrrOnly = repo.getTimelineFrames(37.7749, -122.4194, filter = UsRadarLayerFilter.HRRR_FORECAST_ONLY).getOrThrow()
        assertTrue(hrrrOnly.all { it.frameType == UsRadarFrameType.HRRR_SIMULATED_REFLECTIVITY })

        // 時系列順に昇順ソートされていること
        for (i in 0 until frames.size - 1) {
            assertTrue(
                "Frames must be in chronological ascending order",
                frames[i].epochSeconds <= frames[i + 1].epochSeconds
            )
        }

        // タイルURL生成の検証
        val sampleFrame = frames.first()
        val tileUrl = repo.buildTileUrl(sampleFrame, x = 10, y = 20, zoom = 5)
        assertNotNull(tileUrl)
        assertTrue(tileUrl.contains("/5/10/20.png"))
        assertFalse(tileUrl.contains("{z}"))
        assertFalse(tileUrl.contains("{x}"))
        assertFalse(tileUrl.contains("{y}"))
    }

    @Test
    fun testRadarRepositoryResolutionForUsCities() = runBlocking {
        val radarRepo = io.github.nemynew.expressiveweather.data.RadarRepository.getInstance()
        // ニューヨーク
        val (nyResult, nySource) = radarRepo.getRadarData(40.7128, -74.0060, "アメリカ合衆国")
        assertEquals(RadarDataSource.US_NEXRAD_HRRR, nySource)

        // 国名が null でも米国座標なら NEXRAD/HRRR に解決されるべき
        val (nyNoCountryResult, nyNoCountrySource) = radarRepo.getRadarData(40.7128, -74.0060, null)
        assertEquals(RadarDataSource.US_NEXRAD_HRRR, nyNoCountrySource)

        // 国名が "United States" でも解決されるべき
        val (laResult, laSource) = radarRepo.getRadarData(34.0522, -118.2437, "United States")
        assertEquals(RadarDataSource.US_NEXRAD_HRRR, laSource)

        // 国名が空文字でも、西経米国座標なら NEXRAD/HRRR に解決されること
        val (sfResult, sfSource) = radarRepo.getRadarData(37.7749, -122.4194, "")
        assertEquals(RadarDataSource.US_NEXRAD_HRRR, sfSource)

        // 誤って国名に "JP" が入っていても、西経（アメリカ本土）座標であれば日本誤判定を遮断し NEXRAD/HRRR に解決されること
        val (robustResult, robustSource) = radarRepo.getRadarData(40.7128, -74.0060, "JP")
        assertEquals(RadarDataSource.US_NEXRAD_HRRR, robustSource)
    }

    @Test
    fun testUsRadarLayerCandidates() = runBlocking {
        val repo = UsRadarRepository.getInstance()
        val allLayers = io.github.nemynew.expressiveweather.data.UsRadarLayerType.entries

        // 全12種類定義されていること
        assertEquals(12, allLayers.size)

        // 4カテゴリに適切に分類されていること
        val categories = io.github.nemynew.expressiveweather.data.UsLayerCategory.entries
        assertEquals(4, categories.size)
        assertTrue(allLayers.any { it.category == io.github.nemynew.expressiveweather.data.UsLayerCategory.UNIFIED })
        assertTrue(allLayers.any { it.category == io.github.nemynew.expressiveweather.data.UsLayerCategory.HRRR_FORECAST })
        assertTrue(allLayers.any { it.category == io.github.nemynew.expressiveweather.data.UsLayerCategory.NOWCAST })
        assertTrue(allLayers.any { it.category == io.github.nemynew.expressiveweather.data.UsLayerCategory.OBSERVATION })

        // HRRRシミュレーション予報マップ候補群の検証
        val hrrrLayers = allLayers.filter { it.category == io.github.nemynew.expressiveweather.data.UsLayerCategory.HRRR_FORECAST }
        assertEquals(6, hrrrLayers.size)

        for (layer in hrrrLayers) {
            val result = repo.getTimelineFrames(40.7128, -74.0060, layer = layer)
            assertTrue(result.isSuccess)
            val frames = result.getOrThrow()
            assertTrue("Frames for ${layer.id} should not be empty", frames.isNotEmpty())
            assertTrue("HRRR simulation frames must be forecast", frames.all { it.isForecast })
        }
    }
}
