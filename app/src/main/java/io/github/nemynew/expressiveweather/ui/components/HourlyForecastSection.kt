package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.HourlyForecast
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.theme.WeatherAtmosphereTheme

/**
 * 時間ごと天気予報セクション (HourlyForecastSection):
 * - 枠線（border）を完全排除。
 * - 外枠および各時間カプセルの配色を端末のデバイステーマカラー (MaterialTheme.colorScheme) に完全対応。
 */
@Composable
fun HourlyForecastSection(
    hourlyList: List<HourlyForecast>,
    selectedHour: Int?,
    temperatureUnit: TemperatureUnit,
    atmosphere: WeatherAtmosphereTheme,
    onHourSelected: (HourlyForecast) -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
    condition: io.github.nemynew.expressiveweather.data.WeatherCondition? = null,
    isFrostedGlassEnabled: Boolean = true,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, atmosphere.isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, atmosphere.isDark)
    val isSnowing = condition == io.github.nemynew.expressiveweather.data.WeatherCondition.SNOW

    val cardShape = RoundedCornerShape(28.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .frostedGlassEffect(hazeState, isFrostedGlassEnabled, shape = cardShape)
            .snowTileAccumulation(
                isSnowing = isSnowing,
                isDark = atmosphere.isDark,
                shape = SnowTileShape.ROUNDED_RECT,
                cornerRadiusDp = 28.dp,
                seed = 101
            )
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCardClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScrollFade(scrollState, fadeWidth = 24.dp)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                hourlyList.forEachIndexed { index, item ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    val isSelected = selectedHour == item.hour
                    HourlyCard(
                        item = item,
                        isSelected = isSelected,
                        temperatureUnit = temperatureUnit,
                        onClick = { onHourSelected(item) }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
private fun HourlyCard(
    item: HourlyForecast,
    isSelected: Boolean,
    temperatureUnit: TemperatureUnit,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HourlyScale"
    )

    // デバイステーマカラー追従: 選択時は primaryContainer、非選択時は surfaceContainerHighest (完全不透明)
    val selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
    val normalContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) selectedContainerColor else normalContainerColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "HourlyColor"
    )

    val strings = io.github.nemynew.expressiveweather.data.LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = null, // 枠線完全削除
        modifier = Modifier
            .width(56.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 時刻ラベル (例: 現在, 10:00)
            val timeColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val displayTime = if (item.timeLabel == "現在" || item.timeLabel.equals("Now", ignoreCase = true)) {
                strings.now
            } else {
                item.timeLabel
            }
            Text(
                text = displayTime,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = timeColor,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 天気アイコン (昼夜判定連動 - Microsoft Fluent UI System Icons)
            Icon(
                painter = androidx.compose.ui.res.painterResource(
                    WeatherIconHelper.getConditionDrawable(item.condition, item.isDay)
                ),
                contentDescription = item.condition.label,
                tint = WeatherIconHelper.getConditionIconTint(item.condition),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 気温 (統一フォントサイズ 15sp)
            val tempColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = "${temperatureUnit.convert(item.tempC)}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tempColor,
                fontSize = 15.sp
            )

            // 降水確率 (常に表示：0%の場合も「0%」と表示)
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier.height(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(io.github.nemynew.expressiveweather.R.drawable.ic_fluent_drop_24),
                        contentDescription = null,
                        tint = if (item.precipProb > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(1.5.dp))
                    Text(
                        text = "${item.precipProb}%",
                        fontSize = 11.sp,
                        fontWeight = if (item.precipProb > 0) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (item.precipProb > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
