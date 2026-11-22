package io.github.nemynew.expressiveweather.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.ui.components.BackgroundPreviewPill
import io.github.nemynew.expressiveweather.ui.components.BackgroundPreviewSettings
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Network-independent interaction tests for the main screen's preview controls. */
class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent {
            var condition by rememberSaveable { mutableStateOf<WeatherCondition?>(null) }
            var isDay by rememberSaveable { mutableStateOf(true) }
            ExpressiveWeatherTheme(darkTheme = !isDay) {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.JAPANESE) {
                    Column {
                        BackgroundPreviewSettings(condition, isDay,
                            onConditionChange = { condition = it }, onDayChange = { isDay = it })
                        condition?.let {
                            BackgroundPreviewPill(it, isDay, onSettings = {},
                                onStop = { condition = null })
                        }
                    }
                }
            }
        }
    }

    @Test
    fun allConditionsCanBePreviewedInDayAndNight() {
        WeatherCondition.entries.forEach { condition ->
            composeTestRule.onNodeWithText(condition.label).performClick().assertIsSelected()
            listOf("昼", "夜").forEach { time ->
                composeTestRule.onNodeWithText(time).performClick().assertIsSelected()
                composeTestRule.onNodeWithText("プレビュー中 · ${condition.label} · $time")
                    .assertExists()
            }
        }
    }

    @Test
    fun pillCloseAndSettingsResetBothStopPreview() {
        composeTestRule.onNodeWithText(WeatherCondition.RAIN.label).performClick()
        composeTestRule.onNodeWithContentDescription("プレビューを終了").performClick()
        composeTestRule.onNodeWithText("プレビュー中", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText(WeatherCondition.SNOW.label).performClick()
        composeTestRule.onNodeWithText("実際の天候に戻す").performClick()
        composeTestRule.onNodeWithText("プレビュー中", substring = true).assertDoesNotExist()
    }
}
