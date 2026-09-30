package com.example.expressiveweather.data

import com.example.expressiveweather.data.local.SavedCityEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedCityTest {

    @Test
    fun testCityToSavedCityEntityConversion() {
        val city = City(
            id = "osaka",
            name = "大阪",
            nameEn = "Osaka",
            country = "日本",
            latitude = 34.6937,
            longitude = 135.5023,
            isFavorite = true
        )

        val entity = SavedCityEntity.fromCity(city)
        assertEquals("osaka", entity.id)
        assertEquals("大阪", entity.name)
        assertEquals("Osaka", entity.nameEn)
        assertEquals("日本", entity.country)
        assertEquals(34.6937, entity.latitude, 0.0001)
        assertEquals(135.5023, entity.longitude, 0.0001)
        assertTrue(entity.isFavorite)

        val convertedCity = entity.toCity()
        assertEquals(city.id, convertedCity.id)
        assertEquals(city.name, convertedCity.name)
        assertEquals(city.latitude, convertedCity.latitude, 0.0001)
        assertEquals(city.longitude, convertedCity.longitude, 0.0001)
    }

    @Test
    fun testCityWithEmptyIdUsesNormalizedLocationKey() {
        val city = City(
            id = "",
            name = "京都",
            nameEn = "Kyoto",
            country = "日本",
            latitude = 35.0116,
            longitude = 135.7681
        )

        val entity = SavedCityEntity.fromCity(city)
        assertEquals("35.01_135.77", entity.id)
        assertEquals("京都", entity.name)
    }
}
