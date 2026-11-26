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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text(
            when (language) {
                AppLanguage.JAPANESE -> "デバッグ設定 · 背景テスト"
                AppLanguage.KOREAN -> "디버그 설정 · 배경 테스트"
                AppLanguage.SIMPLIFIED_CHINESE -> "调试设置 · 背景测试"
                AppLanguage.TRADITIONAL_CHINESE -> "除錯設定 · 背景測試"
                AppLanguage.SPANISH -> "Depuración · Prueba de fondo"
                AppLanguage.FRENCH -> "Débogage · Test d'arrière-plan"
                AppLanguage.PORTUGUESE -> "Depuração · Teste de fundo"
                AppLanguage.RUSSIAN -> "Отладка · Тест фона"
                AppLanguage.ENGLISH -> "Debug · Background test"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            when (language) {
                AppLanguage.JAPANESE -> "背景のみを切り替えます。気温・予報は取得データのままです。"
                AppLanguage.KOREAN -> "배경만 전환됩니다. 기온과 예보는 불러온 데이터 그대로 유지됩니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "仅切换背景效果，气温和预报保持实际获取的数据。"
                AppLanguage.TRADITIONAL_CHINESE -> "僅切換背景效果，氣溫與預報維持實際擷取的資料。"
                AppLanguage.SPANISH -> "Solo cambia el fondo. Las temperaturas y el pronóstico conservan los datos reales."
                AppLanguage.FRENCH -> "Seul l'arrière-plan change. Les températures et prévisions conservent leurs valeurs réelles."
                AppLanguage.PORTUGUESE -> "Apenas o fundo muda. As temperaturas e previsões mantêm os dados obtidos."
                AppLanguage.RUSSIAN -> "Меняется только фон. Температура и прогноз остаются реальными."
                AppLanguage.ENGLISH -> "Only the background changes. Temperatures and forecasts keep their fetched values."
            },
            style = MaterialTheme.typography.bodySmall
        )
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
            FilterChip(
                selected = isDay,
                onClick = { onDayChange(true) },
                label = {
                    Text(
                        when (language) {
                            AppLanguage.JAPANESE -> "昼"
                            AppLanguage.KOREAN -> "낮"
                            AppLanguage.SIMPLIFIED_CHINESE -> "白天"
                            AppLanguage.TRADITIONAL_CHINESE -> "白天"
                            AppLanguage.SPANISH -> "Día"
                            AppLanguage.FRENCH -> "Jour"
                            AppLanguage.PORTUGUESE -> "Dia"
                            AppLanguage.RUSSIAN -> "День"
                            AppLanguage.ENGLISH -> "Day"
                        }
                    )
                }
            )
            FilterChip(
                selected = !isDay,
                onClick = { onDayChange(false) },
                label = {
                    Text(
                        when (language) {
                            AppLanguage.JAPANESE -> "夜"
                            AppLanguage.KOREAN -> "밤"
                            AppLanguage.SIMPLIFIED_CHINESE -> "夜间"
                            AppLanguage.TRADITIONAL_CHINESE -> "夜間"
                            AppLanguage.SPANISH -> "Noche"
                            AppLanguage.FRENCH -> "Nuit"
                            AppLanguage.PORTUGUESE -> "Noite"
                            AppLanguage.RUSSIAN -> "Ночь"
                            AppLanguage.ENGLISH -> "Night"
                        }
                    )
                }
            )
        }
        TextButton(onClick = { onConditionChange(null) }, enabled = condition != null) {
            Text(
                when (language) {
                    AppLanguage.JAPANESE -> "実際の天候に戻す"
                    AppLanguage.KOREAN -> "실제 날씨로 되돌리기"
                    AppLanguage.SIMPLIFIED_CHINESE -> "恢复实际天气"
                    AppLanguage.TRADITIONAL_CHINESE -> "恢復實際天氣"
                    AppLanguage.SPANISH -> "Usar clima real"
                    AppLanguage.FRENCH -> "Utiliser la météo réelle"
                    AppLanguage.PORTUGUESE -> "Usar clima real"
                    AppLanguage.RUSSIAN -> "Вернуть реальную погоду"
                    AppLanguage.ENGLISH -> "Use actual weather"
                }
            )
        }
        Text(
            when (language) {
                AppLanguage.JAPANESE -> "シートを閉じると背景を確認できます。"
                AppLanguage.KOREAN -> "시트를 닫으면 배경을 확인할 수 있습니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "关闭此面板即可查看背景效果。"
                AppLanguage.TRADITIONAL_CHINESE -> "關閉此面板即可檢視背景效果。"
                AppLanguage.SPANISH -> "Cierra este panel para ver el fondo."
                AppLanguage.FRENCH -> "Fermez ce panneau pour observer l'arrière-plan."
                AppLanguage.PORTUGUESE -> "Feche este painel para inspecionar o fundo."
                AppLanguage.RUSSIAN -> "Закройте эту панель, чтобы увидеть фон."
                AppLanguage.ENGLISH -> "Dismiss this sheet to inspect the background."
            },
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            when (language) {
                AppLanguage.JAPANESE -> "✨ ジャイロ物理連動: 有効（端末を左右・前後に傾けると、雨・雪の降下角度や雲・光芒の3Dパララックスが滑らかに反応します）"
                AppLanguage.KOREAN -> "✨ 자이로 물리 연동: 활성화됨 (기기를 좌우·앞뒤로 기울이면 비·눈의 낙하 각도와 구름·빛줄기의 3D 시차가 부드럽게 반응합니다)"
                AppLanguage.SIMPLIFIED_CHINESE -> "✨ 陀螺仪物理联动：已启用（倾斜设备即可感受3D视差云层以及雨雪飘落角度变化）"
                AppLanguage.TRADITIONAL_CHINESE -> "✨ 陀螺儀物理聯動：已啟用（傾斜裝置即可感受3D視差雲層與雨雪飄落角度變化）"
                AppLanguage.SPANISH -> "✨ Física giroscópica: Activa (inclina el dispositivo para ver nubes con paralaje 3D y lluvia o nieve inclinada)"
                AppLanguage.FRENCH -> "✨ Physique gyroscopique : Active (inclinez l'appareil pour le parallaxe 3D des nuages et l'inclinaison de la pluie ou de la neige)"
                AppLanguage.PORTUGUESE -> "✨ Física giroscópica: Ativa (incline o dispositivo para ver nuvens com paralaxe 3D e chuva ou neve inclinada)"
                AppLanguage.RUSSIAN -> "✨ Физика гироскопа: активна (наклоните устройство для 3D-параллакса облаков и наклона дождя или снега)"
                AppLanguage.ENGLISH -> "✨ Gyro motion physics: Active (tilt device to see 3D parallax clouds, slanted rain and snow drift)"
            },
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
    val dayNightLabel = when (language) {
        AppLanguage.JAPANESE -> if (isDay) "昼" else "夜"
        AppLanguage.KOREAN -> if (isDay) "낮" else "밤"
        AppLanguage.SIMPLIFIED_CHINESE -> if (isDay) "白天" else "夜间"
        AppLanguage.TRADITIONAL_CHINESE -> if (isDay) "白天" else "夜間"
        AppLanguage.SPANISH -> if (isDay) "Día" else "Noche"
        AppLanguage.FRENCH -> if (isDay) "Jour" else "Nuit"
        AppLanguage.PORTUGUESE -> if (isDay) "Dia" else "Noite"
        AppLanguage.RUSSIAN -> if (isDay) "День" else "Ночь"
        AppLanguage.ENGLISH -> if (isDay) "Day" else "Night"
    }
    val previewPrefix = when (language) {
        AppLanguage.JAPANESE -> "プレビュー中"
        AppLanguage.KOREAN -> "미리보기"
        AppLanguage.SIMPLIFIED_CHINESE -> "预览中"
        AppLanguage.TRADITIONAL_CHINESE -> "預覽中"
        AppLanguage.SPANISH -> "Vista previa"
        AppLanguage.FRENCH -> "Aperçu"
        AppLanguage.PORTUGUESE -> "Prévia"
        AppLanguage.RUSSIAN -> "Предпросмотр"
        AppLanguage.ENGLISH -> "Preview"
    }
    val stopDescription = when (language) {
        AppLanguage.JAPANESE -> "プレビューを終了"
        AppLanguage.KOREAN -> "미리보기 종료"
        AppLanguage.SIMPLIFIED_CHINESE -> "退出预览"
        AppLanguage.TRADITIONAL_CHINESE -> "結束預覽"
        AppLanguage.SPANISH -> "Detener vista previa"
        AppLanguage.FRENCH -> "Arrêter l'aperçu"
        AppLanguage.PORTUGUESE -> "Parar prévia"
        AppLanguage.RUSSIAN -> "Остановить предпросмотр"
        AppLanguage.ENGLISH -> "Stop preview"
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50),
        color = Color(0xEE172B3E), contentColor = Color.White, shadowElevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            TextButton(onClick = onSettings, modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    "$previewPrefix · ${condition.getLocalizedLabel(language)} · $dayNightLabel",
                    color = Color.White, style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Rounded.Close, contentDescription = stopDescription)
            }
        }
    }
}
