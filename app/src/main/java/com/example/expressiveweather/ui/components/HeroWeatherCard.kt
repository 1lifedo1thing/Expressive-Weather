package com.example.expressiveweather.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.TemperatureUnit
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.data.getLocalizedLabel
import com.example.expressiveweather.data.localizedName
import com.example.expressiveweather.theme.GoogleSansFlex
import com.example.expressiveweather.theme.GoogleSansFlexHeroTemp

@Composable
fun HeroWeatherCard(
    weather: WeatherData,
    temperatureUnit: TemperatureUnit,
    onCityClick: () -> Unit,
    onDetailClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDetailClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 0. 現在表示している地名（タップで地名選択シートを開く）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(bottom = 2.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCityClick
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.NearMe,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = weather.city.localizedName(language),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White,
                letterSpacing = 0.2.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 1. 気温の上に具体的な天気名の表示（文字で）
        Text(
            text = weather.condition.getLocalizedLabel(language),
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            color = Color.White.copy(alpha = 0.95f),
            letterSpacing = 0.4.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. 気温の数字のみを中心（°を考慮せず、数字のみを数学的に完璧な画面水平中心に配置）＆ サイズ拡大 (128sp)
        AnimatedContent(
            targetState = temperatureUnit.convert(weather.currentTempC),
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
            },
            label = "TempAnimated"
        ) { temp ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // 左側の不可視ダミー: 右側の°と全く同じフォント・サイズ・パディングを持つことで、数字のみを中心軸に一致させる
                Text(
                    text = "°",
                    fontFamily = GoogleSansFlexHeroTemp,
                    fontSize = 58.sp,
                    lineHeight = 58.sp,
                    color = Color.Transparent,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .alpha(0f)
                )

                // 中央の数字本体: ユーザー指定 Weight 563, Round 100, 拡大サイズ 128sp
                Text(
                    text = "$temp",
                    fontFamily = GoogleSansFlexHeroTemp,
                    fontSize = 128.sp,
                    lineHeight = 128.sp,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )

                // 右側の表示用 ° 記号
                Text(
                    text = "°",
                    fontFamily = GoogleSansFlexHeroTemp,
                    fontSize = 58.sp,
                    lineHeight = 58.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. 気温の下には体感温度を表示
        val feelsLikeConverted = temperatureUnit.convert(weather.feelsLikeC)
        Text(
            text = strings.feelsLike(feelsLikeConverted),
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 23.sp,
            color = Color.White.copy(alpha = 0.95f),
            letterSpacing = 0.4.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. 今日の最高気温と最低気温を表示（体感温度と同じフォントサイズ・ウェイト）
        val maxTempConverted = temperatureUnit.convert(weather.maxTempC)
        val minTempConverted = temperatureUnit.convert(weather.minTempC)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.high(maxTempConverted),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 23.sp,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 0.4.sp
            )
            Text(
                text = strings.low(minTempConverted),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 23.sp,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 0.4.sp
            )
        }
    }
}
