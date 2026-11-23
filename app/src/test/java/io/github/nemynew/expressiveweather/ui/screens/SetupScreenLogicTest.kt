package io.github.nemynew.expressiveweather.ui.screens

import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupScreenLogicTest {

    @Test
    fun rerunSetupDoesNotResetExistingThemeMode() {
        assertTrue(shouldResetThemeModeOnSetupComplete(isRerun = false))
        assertFalse(shouldResetThemeModeOnSetupComplete(isRerun = true))
    }

    @Test
    fun permissionDeniedFeedbackDetectsPermanentDenial() {
        assertEquals(
            SetupLocationFeedback.PERMISSION_DENIED,
            resolveLocationPermissionDeniedFeedback(
                shouldShowRationale = true,
                hasRequestedBefore = false
            )
        )
        assertEquals(
            SetupLocationFeedback.PERMISSION_DENIED,
            resolveLocationPermissionDeniedFeedback(
                shouldShowRationale = true,
                hasRequestedBefore = true
            )
        )
        assertEquals(
            SetupLocationFeedback.PERMISSION_DENIED,
            resolveLocationPermissionDeniedFeedback(
                shouldShowRationale = false,
                hasRequestedBefore = false
            )
        )
        assertEquals(
            SetupLocationFeedback.PERMISSION_PERMANENTLY_DENIED,
            resolveLocationPermissionDeniedFeedback(
                shouldShowRationale = false,
                hasRequestedBefore = true
            )
        )
    }

    @Test
    fun popularSetupCitiesAreValidAndDistinctForAllLanguages() {
        AppLanguage.entries.forEach { language ->
            val cities = getPopularSetupCities(language)
            assertTrue("Popular cities should not be empty for $language", cities.size >= 5)
            assertEquals(
                "Popular city IDs should be distinct for $language",
                cities.size,
                cities.map { it.id }.distinct().size
            )
            cities.forEach { city ->
                assertTrue(city.name.isNotBlank())
                assertTrue(city.nameEn.isNotBlank())
                assertTrue(city.country.isNotBlank())
                assertTrue(city.latitude in -90.0..90.0)
                assertTrue(city.longitude in -180.0..180.0)
            }
        }
    }

    @Test
    fun externalCitySelectionPublishesMonotonicVersionAndCity() {
        val initialVersion = WeatherRepository.externalCitySelection.value?.first ?: 0L
        val osaka = City(
            id = "osaka",
            name = "大阪",
            nameEn = "Osaka",
            country = "日本",
            latitude = 34.6937,
            longitude = 135.5023
        )
        WeatherRepository.notifyExternalCitySelected(osaka)
        val first = WeatherRepository.externalCitySelection.value
        assertNotNull(first)
        assertTrue(first!!.first > initialVersion)
        assertEquals(osaka, first.second)

        val london = City(
            id = "london",
            name = "London",
            nameEn = "London",
            country = "United Kingdom",
            latitude = 51.5072,
            longitude = -0.1276
        )
        WeatherRepository.notifyExternalCitySelected(london)
        val second = WeatherRepository.externalCitySelection.value
        assertNotNull(second)
        assertEquals(first.first + 1L, second!!.first)
        assertEquals(london, second.second)
    }
}
