package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedLabel
import io.github.nemynew.expressiveweather.data.localizedName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import io.github.nemynew.expressiveweather.theme.GoogleSansFlexHeroTemp
import io.github.nemynew.expressiveweather.theme.WeatherAtmosphereTheme

/**
 * タブレットおよび大画面デバイス専用の横並びワイド天候ヘッダー
 *
 * デザイン仕様（画像1・画像2準拠）:
 * 1. 左ブロック: 都市名（太字大） + 天気名称（雨など）
 * 2. 中央ブロック: 体感気温 + 最高・最低気温
 * 3. 右ブロック: 巨大気温数字 (Hero Font) + 天候グラフィックアイコン
 */
@Composable
fun TabletWeatherHeader(
    weather: WeatherData,
    temperatureUnit: TemperatureUnit,
    atmosphere: WeatherAtmosphereTheme,
    isSaved: Boolean = false,
    isDefault: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current

    val cityName = weather.city.localizedName(language)
    val conditionName = weather.condition.getLocalizedLabel(language)
    val currentTemp = temperatureUnit.convert(weather.currentTempC)
    val feelsLikeConverted = temperatureUnit.convert(weather.feelsLikeC)
    val maxTempConverted = temperatureUnit.convert(weather.maxTempC)
    val minTempConverted = temperatureUnit.convert(weather.minTempC)

    val shadow = Shadow(
        color = Color.Black.copy(alpha = 0.45f),
        offset = Offset(0f, 2.5f),
        blurRadius = 6f
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 左側: 都市名 + 天気
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1.1f, fill = false)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = cityName,
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = shadow
                    )
                )

                if (onToggleSave != null && !isDefault) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSaved) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleSave)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkAdd,
                                contentDescription = if (isSaved) strings.removeSaved else strings.addLocation,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = conditionName,
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.92f),
                    shadow = shadow
                )
            )
        }

        // 2. 中央: 体感気温 + 最高・最低気温
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = strings.feelsLike(feelsLikeConverted),
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                    shadow = shadow
                )
            )
            Text(
                text = "${maxTempConverted}° / ${minTempConverted}°",
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.88f),
                    shadow = shadow
                )
            )
        }

        // 3. 右側: 巨大気温数字 + 天気アイコン
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1.1f, fill = false)
        ) {
            Text(
                text = "${currentTemp}°",
                style = TextStyle(
                    fontFamily = GoogleSansFlexHeroTemp,
                    fontSize = 86.sp,
                    color = Color.White,
                    shadow = shadow
                ),
                lineHeight = 86.sp
            )
            Icon(
                painter = painterResource(
                    WeatherIconHelper.getConditionDrawable(weather.condition, weather.isDay)
                ),
                contentDescription = conditionName,
                tint = WeatherIconHelper.getConditionIconTint(weather.condition),
                modifier = Modifier.size(72.dp)
            )
        }
    }
}
