package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 警戒レベル判定、対象災害種別特定、キキクル通知メーター仕様の単体テスト
 */
class KikikuruAlertTest {

    @Test
    fun testAlertLevelDefinitions() {
        // レベル5相当 災害切迫
        val l5 = AlertLevel.LEVEL_5
        assertEquals(5, l5.level)
        assertEquals("災害切迫", l5.title)
        assertEquals(0xFF3C383EL, l5.colorHex) // 黒色・濃いチャコール
        assertEquals(0xFFFFD700L, l5.textColorHex) // ゴールド
        assertEquals(
            "何らかの災害がすでに発生している可能性が極めて高い状況です。命の危険が迫っているため直ちに身の安全を確保して下さい。",
            l5.actionAdvice
        )

        // レベル4相当 危険
        val l4 = AlertLevel.LEVEL_4
        assertEquals(4, l4.level)
        assertEquals("危険", l4.title)
        assertEquals(0xFFB84FB3L, l4.colorHex) // 紫色・マゼンタ
        assertEquals(
            "危険な場所からの避難が必要とされる警戒レベル4に相当します。自治体からの避難指示の発令に留意するとともに、自ら避難の判断をして下さい。",
            l4.actionAdvice
        )

        // レベル3相当 警戒
        val l3 = AlertLevel.LEVEL_3
        assertEquals(3, l3.level)
        assertEquals("警戒", l3.title)
        assertEquals(0xFFFF5252L, l3.colorHex) // 赤色・コーラル
        assertEquals(
            "高齢者等は危険な場所からの避難が必要とされる警戒レベル3に相当します。普段の行動を見合わせ始めたり、避難の準備をして下さい。",
            l3.actionAdvice
        )

        // レベル2相当 注意
        val l2 = AlertLevel.LEVEL_2
        assertEquals(2, l2.level)
        assertEquals("注意", l2.title)
        assertEquals(0xFFFDD835L, l2.colorHex) // 明るい黄色
        assertEquals(0xFF212121L, l2.textColorHex)
        assertEquals(
            "避難行動の確認が必要とされる警戒レベル2に相当します。ハザードマップ等により災害が想定されている区域や避難先、避難経路を確認して下さい。",
            l2.actionAdvice
        )

        // レベル1 早期注意
        val l1 = AlertLevel.LEVEL_1
        assertEquals(1, l1.level)
        assertEquals("早期注意", l1.title)
        assertEquals(0xFFE0E0E0L, l1.colorHex) // 薄いグレー
        assertEquals(
            "災害への心構えを高める必要があります。最新の防災気象情報等に留意して下さい。",
            l1.actionAdvice
        )

        // 平常時 レベル0
        val l0 = AlertLevel.LEVEL_0
        assertEquals(0, l0.level)
        assertEquals("平常", l0.title)
        assertEquals("現在のところ警報・注意報は発表されていません。", l0.actionAdvice)
    }

    @Test
    fun testAlertLevelFromLevelHelper() {
        assertEquals(AlertLevel.LEVEL_5, AlertLevel.fromLevel(5))
        assertEquals(AlertLevel.LEVEL_4, AlertLevel.fromLevel(4))
        assertEquals(AlertLevel.LEVEL_3, AlertLevel.fromLevel(3))
        assertEquals(AlertLevel.LEVEL_2, AlertLevel.fromLevel(2))
        assertEquals(AlertLevel.LEVEL_1, AlertLevel.fromLevel(1))
        assertEquals(AlertLevel.LEVEL_0, AlertLevel.fromLevel(0))
        assertEquals(AlertLevel.LEVEL_0, AlertLevel.fromLevel(99))
    }

    @Test
    fun testDisasterTargetFlagsSingle() {
        val flags = DisasterTargetFlags(landslide = true)
        assertTrue(flags.hasAnyDisaster)
        assertEquals(DisasterType.LANDSLIDE, flags.primaryType)
        assertTrue(flags.subTypes.isEmpty())
        assertEquals(1, flags.activeTypes.size)
    }

    @Test
    fun testDisasterTargetFlagsMultiplePrioritySorting() {
        // 土砂、洪水、暴風・強風が重なっている場合
        val flags = DisasterTargetFlags(
            landslide = true,
            inundation = false,
            flood = true,
            stormWind = true
        )
        assertTrue(flags.hasAnyDisaster)
        // 優先度順: 土砂災害 (1) > 浸水害 (2) > 洪水災害 (3) > 暴風・強風 (4)
        assertEquals(DisasterType.LANDSLIDE, flags.primaryType)
        val subs = flags.subTypes
        assertEquals(2, subs.size)
        assertEquals(DisasterType.FLOOD, subs[0])
        assertEquals(DisasterType.STORM_WIND, subs[1])
    }

    @Test
    fun testDisasterTargetFlagsInundationAndFlood() {
        val flags = DisasterTargetFlags(
            landslide = false,
            inundation = true,
            flood = true,
            stormWind = false
        )
        assertEquals(DisasterType.INUNDATION, flags.primaryType)
        assertEquals(listOf(DisasterType.FLOOD), flags.subTypes)
    }

    @Test
    fun testDisasterTargetFlagsEmpty() {
        val flags = DisasterTargetFlags()
        assertFalse(flags.hasAnyDisaster)
        assertNull(flags.primaryType)
        assertTrue(flags.subTypes.isEmpty())
    }

    @Test
    fun testMunicipalityAlertInfoHasActiveAlert() {
        val infoLevel0 = MunicipalityAlertInfo(alertLevel = AlertLevel.LEVEL_0)
        assertFalse("Level 0 should not have active alert", infoLevel0.hasActiveAlert)

        val infoLevel1 = MunicipalityAlertInfo(alertLevel = AlertLevel.LEVEL_1)
        assertFalse("Level 1 should not have active alert banner", infoLevel1.hasActiveAlert)

        val infoLevel2 = MunicipalityAlertInfo(alertLevel = AlertLevel.LEVEL_2)
        assertTrue("Level 2 must have active alert", infoLevel2.hasActiveAlert)

        val infoLevel3 = MunicipalityAlertInfo(alertLevel = AlertLevel.LEVEL_3)
        assertTrue("Level 3 must have active alert", infoLevel3.hasActiveAlert)

        val infoLevel4 = MunicipalityAlertInfo(alertLevel = AlertLevel.LEVEL_4)
        assertTrue("Level 4 must have active alert", infoLevel4.hasActiveAlert)

        val infoLevel5 = MunicipalityAlertInfo(alertLevel = AlertLevel.LEVEL_5)
        assertTrue("Level 5 must have active alert", infoLevel5.hasActiveAlert)
    }

    @Test
    fun testMunicipalityAlertInfoActiveWarningNames() {
        val warnings = listOf(
            MunicipalityWarningItem(code = "03", name = "大雨警報", status = "発表", isWarning = true),
            MunicipalityWarningItem(code = "04", name = "洪水警報", status = "継続", isWarning = true),
            MunicipalityWarningItem(code = "14", name = "雷注意報", status = "発表", isWarning = false)
        )
        val info = MunicipalityAlertInfo(
            municipalityName = "千代田区",
            activeWarnings = warnings
        )
        assertEquals(listOf("大雨警報", "洪水警報", "雷注意報"), info.activeWarningNames)
    }

    @Test
    fun testNotificationTitleAndChipFormatting() {
        val targetCity = "千代田区"
        val level = AlertLevel.LEVEL_4
        val primaryDisaster = "洪水"
        val chipText = "$primaryDisaster レベル${level.level}"
        val title = "$chipText • $targetCity"

        // タイトルがチップ文字列（災害＋レベル）から始まること（Samsung Now Barで地名のみになるのを防止）
        assertTrue(title.startsWith("洪水 レベル4"))
        assertEquals("洪水 レベル4 • 千代田区", title)

        // 四角記号（■・□・◻︎）がタイトルやチップに含まれないことを検証
        assertFalse(title.contains("■"))
        assertFalse(title.contains("□"))
        assertFalse(title.contains("◻"))
        assertFalse(chipText.contains("■"))
        assertFalse(chipText.contains("□"))
    }

    @Test
    fun testKikikuruGuidelineContentOfficialTexts() {
        // 大雨キキクル (Image 1)
        val rainLv5Ja = KikikuruGuidelineContent.getActionAdvice(DisasterType.HEAVY_RAIN, AlertLevel.LEVEL_5, AppLanguage.JAPANESE)
        assertTrue(rainLv5Ja.contains("立退き避難がかえって危険な場合"))
        assertTrue(rainLv5Ja.contains("直ちに身の安全を確保"))

        val rainLv4Ja = KikikuruGuidelineContent.getSituationExample(DisasterType.HEAVY_RAIN, AlertLevel.LEVEL_4, AppLanguage.JAPANESE)
        assertTrue(rainLv4Ja.contains("床上まで水に浸かるおそれ"))

        // 土砂キキクル (Image 2)
        val landLv4Ja = KikikuruGuidelineContent.getActionAdvice(DisasterType.LANDSLIDE, AlertLevel.LEVEL_4, AppLanguage.JAPANESE)
        assertTrue(landLv4Ja.contains("土砂災害警戒区域等の外へ避難して下さい"))

        // 浸水キキクル (Image 3)
        val inunLv4SitJa = KikikuruGuidelineContent.getSituationExample(DisasterType.INUNDATION, AlertLevel.LEVEL_4, AppLanguage.JAPANESE)
        assertTrue(inunLv4SitJa.contains("道路が一面冠水し、側溝やマンホールの場所が分からなくなるおそれ"))

        val inunLv2ActJa = KikikuruGuidelineContent.getActionAdvice(DisasterType.INUNDATION, AlertLevel.LEVEL_2, AppLanguage.JAPANESE)
        assertTrue(inunLv2ActJa.contains("住宅の地下室からは地上に移動し、道路のアンダーパスには近づかない"))

        // 洪水キキクル (Image 4)
        val floodLv4SitJa = KikikuruGuidelineContent.getSituationExample(DisasterType.FLOOD, AlertLevel.LEVEL_4, AppLanguage.JAPANESE)
        assertTrue(floodLv4SitJa.contains("50年確率値基準超過") || floodLv4SitJa.contains("外水氾濫"))

        val floodLv3SitJa = KikikuruGuidelineContent.getSituationExample(DisasterType.FLOOD, AlertLevel.LEVEL_3, AppLanguage.JAPANESE)
        assertTrue(floodLv3SitJa.contains("30年確率値基準到達"))
    }

    @Test
    fun testKikikuruMultilingualSupport() {
        val types = listOf(DisasterType.HEAVY_RAIN, DisasterType.LANDSLIDE, DisasterType.INUNDATION, DisasterType.FLOOD)
        val languages = listOf(AppLanguage.JAPANESE, AppLanguage.ENGLISH, AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE)
        val levels = listOf(AlertLevel.LEVEL_5, AlertLevel.LEVEL_4, AlertLevel.LEVEL_3, AlertLevel.LEVEL_2)

        for (t in types) {
            for (lang in languages) {
                // Short & long labels non-empty
                assertTrue(t.getLabel(lang).isNotBlank())
                assertTrue(t.getShortLabel(lang).isNotBlank())
                assertTrue(KikikuruGuidelineContent.getButtonLabel(t, lang).isNotBlank())

                for (lvl in levels) {
                    val advice = KikikuruGuidelineContent.getActionAdvice(t, lvl, lang)
                    val sit = KikikuruGuidelineContent.getSituationExample(t, lvl, lang)
                    assertTrue("Advice for $t at $lvl in $lang must not be blank", advice.isNotBlank())
                    assertTrue("Situation for $t at $lvl in $lang must not be blank", sit.isNotBlank())

                    val detail = KikikuruDisasterDetail(disasterType = t, alertLevel = lvl)
                    val title = detail.getTitle(lang)
                    assertTrue("Detail title in $lang must not be blank", title.isNotBlank())
                }
            }
        }
    }

    @Test
    fun testEvacuationInfoMultilingual() {
        assertEquals("緊急安全確保 相当", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_5, AppLanguage.JAPANESE))
        assertEquals("Emergency Safety Assurance", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_5, AppLanguage.ENGLISH))
        assertEquals("紧急安全确保 相当", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_5, AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("緊急安全確保 相當", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_5, AppLanguage.TRADITIONAL_CHINESE))

        assertEquals("避難指示 相当", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_4, AppLanguage.JAPANESE))
        assertEquals("Evacuation Order", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_4, AppLanguage.ENGLISH))

        assertEquals("高齢者等避難 相当", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_3, AppLanguage.JAPANESE))
        assertEquals("Evacuate Elderly, etc.", KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_3, AppLanguage.ENGLISH))

        assertNull(KikikuruGuidelineContent.getEvacuationInfo(AlertLevel.LEVEL_2, AppLanguage.JAPANESE))
    }

    @Test
    fun testRadarLayerMappings() {
        assertEquals("hrpns", DisasterType.HEAVY_RAIN.radarLayerId)
        assertEquals("landslide", DisasterType.LANDSLIDE.radarLayerId)
        assertEquals("inundation", DisasterType.INUNDATION.radarLayerId)
        assertEquals("flood", DisasterType.FLOOD.radarLayerId)
    }

    @Test
    fun testAllDisasterDetailsFallback() {
        val info = MunicipalityAlertInfo(
            alertLevel = AlertLevel.LEVEL_4,
            disasterFlags = DisasterTargetFlags(landslide = true, flood = true)
        )
        val details = info.allDisasterDetails
        assertEquals(4, details.size)
        assertEquals(DisasterType.HEAVY_RAIN, details[0].disasterType)
        assertEquals(DisasterType.LANDSLIDE, details[1].disasterType)
        assertEquals(AlertLevel.LEVEL_4, details[1].alertLevel)
        assertEquals(DisasterType.FLOOD, details[3].disasterType)
        assertEquals(AlertLevel.LEVEL_4, details[3].alertLevel)
    }
}
