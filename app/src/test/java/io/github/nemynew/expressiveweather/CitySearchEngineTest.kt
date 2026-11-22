package io.github.nemynew.expressiveweather

import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.CitySearchEngine
import io.github.nemynew.expressiveweather.data.localizedName
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitySearchEngineTest {

    @Test
    fun testExpandQueries_Tokyo() {
        val expanded = CitySearchEngine.expandQueries("東京")
        assertTrue("Expanded queries should contain 東京都", expanded.contains("東京都"))
        assertTrue("Expanded queries should contain Tokyo", expanded.contains("Tokyo"))
    }

    @Test
    fun testExpandQueries_Hiragana() {
        val expanded = CitySearchEngine.expandQueries("とうきょう")
        assertTrue("Expanded queries should map to 東京", expanded.contains("東京"))
        assertTrue("Expanded queries should map to 東京都", expanded.contains("東京都"))
        assertTrue("Expanded queries should map to Tokyo", expanded.contains("Tokyo"))
    }

    @Test
    fun testExpandQueries_English() {
        val expanded = CitySearchEngine.expandQueries("Osaka")
        assertTrue("Expanded queries should contain 大阪市", expanded.contains("大阪市"))
        assertTrue("Expanded queries should contain Osaka", expanded.contains("Osaka"))
    }

    @Test
    fun testSearch_Tokyo_Japanese() = runBlocking {
        val results = CitySearchEngine.search("東京", AppLanguage.JAPANESE, allowNetwork = false)
        assertFalse("Results for 東京 should not be empty", results.isEmpty())
        val top = results.first()
        assertEquals("東京都", top.name)
        assertEquals("Tokyo", top.nameEn)
        assertEquals("東京都", top.localizedName(AppLanguage.JAPANESE))
        assertEquals("Tokyo", top.localizedName(AppLanguage.ENGLISH))
    }

    @Test
    fun testSearch_Tokyo_English() = runBlocking {
        val results = CitySearchEngine.search("Tokyo", AppLanguage.ENGLISH, allowNetwork = false)
        assertFalse("Results for Tokyo should not be empty", results.isEmpty())
        val top = results.first()
        assertEquals("Tokyo", top.nameEn)
        assertEquals("Tokyo", top.localizedName(AppLanguage.ENGLISH))
    }

    @Test
    fun testSearch_Osaka_Japanese() = runBlocking {
        val results = CitySearchEngine.search("大阪", AppLanguage.JAPANESE, allowNetwork = false)
        assertFalse("Results for 大阪 should not be empty", results.isEmpty())
        val top = results.first()
        assertEquals("大阪市", top.name)
        assertEquals("Osaka", top.nameEn)
    }

    @Test
    fun testSearch_Kyoto_Japanese() = runBlocking {
        val results = CitySearchEngine.search("京都", AppLanguage.JAPANESE, allowNetwork = false)
        assertFalse("Results for 京都 should not be empty", results.isEmpty())
        val top = results.first()
        assertEquals("京都市", top.name)
        assertEquals("Kyoto", top.nameEn)
    }

    @Test
    fun testSearch_Sapporo_Japanese() = runBlocking {
        val results = CitySearchEngine.search("札幌", AppLanguage.JAPANESE, allowNetwork = false)
        assertFalse("Results for 札幌 should not be empty", results.isEmpty())
        val top = results.first()
        assertEquals("札幌市", top.name)
        assertEquals("Sapporo", top.nameEn)
    }

    @Test
    fun testSearch_Hiragana_Saitama() = runBlocking {
        val results = CitySearchEngine.search("さいたま", AppLanguage.JAPANESE, allowNetwork = false)
        assertFalse("Results for さいたま should not be empty", results.isEmpty())
        val top = results.first()
        assertEquals("さいたま市", top.name)
    }

    @Test
    fun testSearch_WorldCities() = runBlocking {
        val resultsLondon = CitySearchEngine.search("London", AppLanguage.ENGLISH, allowNetwork = false)
        assertFalse("Results for London should not be empty", resultsLondon.isEmpty())
        assertEquals("London", resultsLondon.first().localizedName(AppLanguage.ENGLISH))

        val resultsNY = CitySearchEngine.search("ニューヨーク", AppLanguage.JAPANESE, allowNetwork = false)
        assertFalse("Results for ニューヨーク should not be empty", resultsNY.isEmpty())
        assertEquals("ニューヨーク", resultsNY.first().name)
        assertEquals("New York", resultsNY.first().nameEn)
    }
}
