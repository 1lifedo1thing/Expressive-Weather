package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.getLocalizedLabel

@Composable
internal fun BackgroundPreviewSettings(
    condition: WeatherCondition?,
    isDay: Boolean,
    onConditionChange: (WeatherCondition?) -> Unit,
    onDayChange: (Boolean) -> Unit
) {
    val language = LocalAppLanguage.current
    val ja = language == AppLanguage.JAPANESE
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text(if (ja) "デバッグ設定 · 背景テスト" else "Debug · Background test",
            style = MaterialTheme.typography.titleMedium)
        Text(if (ja) "背景のみを切り替えます。気温・予報は取得データのままです。" else
            "Only the background changes. Temperatures and forecasts keep their fetched values.",
            style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeatherCondition.entries.forEach { weather ->
                FilterChip(
                    selected = condition == weather,
                    onClick = { onConditionChange(weather) },
                    label = { Text(weather.getLocalizedLabel(language)) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isDay, onClick = { onDayChange(true) },
                label = { Text(if (ja) "昼" else "Day") })
            FilterChip(selected = !isDay, onClick = { onDayChange(false) },
                label = { Text(if (ja) "夜" else "Night") })
        }
        TextButton(onClick = { onConditionChange(null) }, enabled = condition != null) {
            Text(if (ja) "実際の天候に戻す" else "Use actual weather")
        }
        Text(if (ja) "シートを閉じると背景を確認できます。" else "Dismiss this sheet to inspect the background.",
            style = MaterialTheme.typography.bodySmall)
        Text(
            if (ja) "✨ ジャイロ物理連動: 有効（端末を左右・前後に傾けると、雨・雪の降下角度や雲・光芒の3Dパララックスが滑らかに反応します）"
            else "✨ Gyro motion physics: Active (tilt device to see 3D parallax clouds, slanted rain and snow drift)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
internal fun BackgroundPreviewPill(
    condition: WeatherCondition,
    isDay: Boolean,
    onSettings: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val ja = language == AppLanguage.JAPANESE
    Surface(modifier = modifier, shape = RoundedCornerShape(50),
        color = Color(0xEE172B3E), contentColor = Color.White, shadowElevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            TextButton(onClick = onSettings, modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    if (ja) "プレビュー中 · ${condition.label} · ${if (isDay) "昼" else "夜"}" else
                        "Preview · ${condition.englishLabel} · ${if (isDay) "Day" else "Night"}",
                    color = Color.White, style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Rounded.Close, contentDescription = if (ja) "プレビューを終了" else "Stop preview")
            }
        }
    }
}
