package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JmaWarningMasterTest {

    @Test
    fun testGetDetailByName() {
        val rainWarning = JmaWarningMaster.getDetailByName("大雨警報")
        assertEquals("03", rainWarning.code)
        assertEquals(WarningCategory.WARNING, rainWarning.category)
        assertTrue(rainWarning.phenomenon.isNotBlank())
        assertTrue(rainWarning.actionGuide.isNotBlank())
        assertTrue(rainWarning.cautionPoints.isNotEmpty())

        val rainSpecial = JmaWarningMaster.getDetailByName("大雨特別警報")
        assertEquals(WarningCategory.SPECIAL_WARNING, rainSpecial.category)

        val thunderAdvisory = JmaWarningMaster.getDetailByName("雷注意報")
        assertEquals(WarningCategory.ADVISORY, thunderAdvisory.category)
    }

    @Test
    fun testSortWarningsByPriority() {
        val items = listOf(
            MunicipalityWarningItem("14", "雷注意報", "継続", isWarning = false),
            MunicipalityWarningItem("04", "洪水警報", "発表", isWarning = true),
            MunicipalityWarningItem("33", "大雨特別警報", "発表", isWarning = true, isSpecial = true),
            MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true)
        )

        val sorted = JmaWarningMaster.sortWarningsByPriority(items)

        // 1位: 大雨特別警報 (Special Warning)
        assertEquals("大雨特別警報", sorted[0].name)
        // 2位: 大雨警報 (Warning, priority code)
        assertEquals("大雨警報", sorted[1].name)
        // 3位: 洪水警報 (Warning)
        assertEquals("洪水警報", sorted[2].name)
        // 4位: 雷注意報 (Advisory)
        assertEquals("雷注意報", sorted[3].name)
    }

    @Test
    fun testFormatAggregatedTitle_singleItem() {
        val items = listOf(
            MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true)
        )
        val title = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.JAPANESE)
        assertEquals("大雨警報", title)
    }

    @Test
    fun testFormatAggregatedTitle_multipleWarningsAndAdvisories() {
        val items = listOf(
            MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true),
            MunicipalityWarningItem("04", "洪水警報", "発表", isWarning = true),
            MunicipalityWarningItem("14", "雷注意報", "継続", isWarning = false)
        )
        val titleJa = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.JAPANESE)
        assertEquals("大雨警報 ほか2件の警報・注意報", titleJa)

        val titleEn = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.ENGLISH)
        assertEquals("Heavy Rain Warning & 2 more", titleEn)

        val titleZhHans = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.SIMPLIFIED_CHINESE)
        assertEquals("暴雨预警 等3项预警・注意报", titleZhHans)

        val titleZhHant = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.TRADITIONAL_CHINESE)
        assertEquals("大雨警報 等3項預警・注意報", titleZhHant)
    }

    @Test
    fun testFormatAggregatedTitle_advisoriesOnly() {
        val items = listOf(
            MunicipalityWarningItem("14", "雷注意報", "発表", isWarning = false),
            MunicipalityWarningItem("10", "大雨注意報", "発表", isWarning = false)
        )
        val titleJa = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.JAPANESE)
        assertEquals("大雨注意報 ほか1件の注意報", titleJa)

        val titleEn = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.ENGLISH)
        assertEquals("Heavy Rain Advisory & 1 more", titleEn)

        val titleZhHans = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.SIMPLIFIED_CHINESE)
        assertEquals("大雨注意报 等2项注意报", titleZhHans)

        val titleZhHant = JmaWarningMaster.formatAggregatedTitle(items, AppLanguage.TRADITIONAL_CHINESE)
        assertEquals("大雨注意報 等2項注意報", titleZhHant)
    }

    @Test
    fun testLocalizedWarningDetails() {
        val detail = JmaWarningMaster.getDetailByName("大雨警報")

        // Japanese
        assertEquals("大雨警報", detail.getLocalizedName(AppLanguage.JAPANESE))
        assertTrue(detail.getLocalizedPhenomenon(AppLanguage.JAPANESE).contains("土砂災害"))
        assertTrue(detail.getLocalizedActionGuide(AppLanguage.JAPANESE).contains("避難"))
        assertTrue(detail.getLocalizedCautionPoints(AppLanguage.JAPANESE).isNotEmpty())

        // English
        assertEquals("Heavy Rain Warning", detail.getLocalizedName(AppLanguage.ENGLISH))
        assertTrue(detail.getLocalizedPhenomenon(AppLanguage.ENGLISH).contains("landslide"))
        assertTrue(detail.getLocalizedActionGuide(AppLanguage.ENGLISH).contains("shelter"))
        assertTrue(detail.getLocalizedCautionPoints(AppLanguage.ENGLISH).isNotEmpty())

        // Simplified Chinese
        assertEquals("暴雨预警", detail.getLocalizedName(AppLanguage.SIMPLIFIED_CHINESE))
        assertTrue(detail.getLocalizedPhenomenon(AppLanguage.SIMPLIFIED_CHINESE).contains("滑坡"))
        assertTrue(detail.getLocalizedActionGuide(AppLanguage.SIMPLIFIED_CHINESE).contains("避难") || detail.getLocalizedActionGuide(AppLanguage.SIMPLIFIED_CHINESE).contains("转移"))
        assertTrue(detail.getLocalizedCautionPoints(AppLanguage.SIMPLIFIED_CHINESE).isNotEmpty())

        // Traditional Chinese
        assertEquals("大雨警報", detail.getLocalizedName(AppLanguage.TRADITIONAL_CHINESE))
        assertTrue(detail.getLocalizedPhenomenon(AppLanguage.TRADITIONAL_CHINESE).contains("土石流"))
        assertTrue(detail.getLocalizedActionGuide(AppLanguage.TRADITIONAL_CHINESE).contains("避難") || detail.getLocalizedActionGuide(AppLanguage.TRADITIONAL_CHINESE).contains("撤離"))
        assertTrue(detail.getLocalizedCautionPoints(AppLanguage.TRADITIONAL_CHINESE).isNotEmpty())
    }
}
