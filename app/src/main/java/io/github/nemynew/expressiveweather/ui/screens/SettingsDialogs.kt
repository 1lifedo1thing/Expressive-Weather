package io.github.nemynew.expressiveweather.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.MoonPhaseCalculator
import io.github.nemynew.expressiveweather.data.MoonPhaseSimulator
import io.github.nemynew.expressiveweather.data.MoonPhaseStage
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.MoonPhaseDisc
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.HapticCapabilitiesSnapshot
import io.github.nemynew.expressiveweather.ui.haptics.HapticDeviceProfile
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import kotlin.math.abs

@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    cancelText: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .selectableGroup()
            ) {
                options.forEach { (option, label) ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    onOptionSelected(option)
                                    onDismiss()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun ClearCacheConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

private data class SimulationWeatherItem(
    val nameJa: String,
    val nameKo: String,
    val nameEn: String,
    val nameZhHans: String,
    val nameZhHant: String,
    val nameEs: String,
    val nameFr: String,
    val namePt: String,
    val nameRu: String,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val icon: ImageVector
) {
    fun getName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.KOREAN -> nameKo
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhHans
        AppLanguage.TRADITIONAL_CHINESE -> nameZhHant
        AppLanguage.ENGLISH -> nameEn
        AppLanguage.SPANISH -> nameEs
        AppLanguage.FRENCH -> nameFr
        AppLanguage.PORTUGUESE -> namePt
        AppLanguage.RUSSIAN -> nameRu
    }
}

@Composable
fun WeatherSimulationDialog(
    language: AppLanguage,
    onSelect: (WeatherCondition?, Boolean?) -> Unit,
    onDismiss: () -> Unit,
    cancelText: String,
    resetText: String
) {
    val expressHaptics = LocalExpressiveHaptics.current
    val items = remember {
        listOf(
            SimulationWeatherItem("快晴 (昼)", "맑음 (주간)", "Clear (Day)", "晴朗 (白天)", "晴朗 (白天)", "Despejado (Día)", "Dégagé (Jour)", "Céu limpo (Dia)", "Ясно (День)", WeatherCondition.CLEAR, true, Icons.Rounded.WbSunny),
            SimulationWeatherItem("快晴 (夜)", "맑음 (야간)", "Clear (Night)", "晴朗 (夜间)", "晴朗 (夜間)", "Despejado (Noche)", "Dégagé (Nuit)", "Céu limpo (Noite)", "Ясно (Ночь)", WeatherCondition.CLEAR, false, Icons.Rounded.NightsStay),
            SimulationWeatherItem("晴れ時々曇り", "구름 조금", "Few Clouds", "多云", "多雲", "Pocas nubes", "Peu nuageux", "Poucas nuvens", "Малооблачно", WeatherCondition.FEW_CLOUDS, true, Icons.Rounded.CloudQueue),
            SimulationWeatherItem("曇り", "흐림", "Cloudy", "阴天", "陰天", "Nublado", "Nuageux", "Nublado", "Облачно", WeatherCondition.CLOUDY, true, Icons.Rounded.Cloud),
            SimulationWeatherItem("雨", "비", "Rain", "降雨", "降雨", "Lluvia", "Pluie", "Chuva", "Дождь", WeatherCondition.RAIN, true, Icons.Rounded.WaterDrop),
            SimulationWeatherItem("雷雨", "뇌우", "Thunderstorm", "雷阵雨", "雷陣雨", "Tormenta", "Orage", "Tempestade", "Гроза", WeatherCondition.THUNDERSTORM, false, Icons.Rounded.Thunderstorm),
            SimulationWeatherItem("雪", "눈", "Snow", "降雪", "降雪", "Nieve", "Neige", "Neve", "Снег", WeatherCondition.SNOW, true, Icons.Rounded.AcUnit),
            SimulationWeatherItem("霧", "안개", "Fog", "有雾", "有霧", "Niebla", "Brouillard", "Nevoeiro", "Туман", WeatherCondition.FOG, true, Icons.Rounded.Dehaze)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "天候アニメーションのプレビュー"
                    AppLanguage.KOREAN -> "날씨 애니메이션 미리보기"
                    AppLanguage.SIMPLIFIED_CHINESE -> "天气动态效果预览"
                    AppLanguage.TRADITIONAL_CHINESE -> "天氣動態效果預覽"
                    AppLanguage.ENGLISH -> "Atmosphere Simulation"
                    AppLanguage.SPANISH -> "Simulación atmosférica"
                    AppLanguage.FRENCH -> "Simulation atmosphérique"
                    AppLanguage.PORTUGUESE -> "Simulação atmosférica"
                    AppLanguage.RUSSIAN -> "Симуляция погоды"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expressHaptics.playWeatherSignature(item.condition)
                                onSelect(item.condition, item.isDay)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.getName(language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null, null) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = resetText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

private data class MoonPhasePreset(
    val stage: MoonPhaseStage,
    val phaseFraction: Float
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoonPhaseSimulationDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    closeText: String
) {
    val context = LocalContext.current
    val expressHaptics = LocalExpressiveHaptics.current
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        MoonPhaseSimulator.ensureLoaded(context)
    }

    val simulatedPhase by MoonPhaseSimulator.overridePhaseFraction.collectAsState()
    val simulatedSouthern by MoonPhaseSimulator.overrideSouthernHemisphere.collectAsState()

    val liveMoonInfo = remember(language) {
        MoonPhaseCalculator.calculate(language = language)
    }
    val activeMoonInfo = remember(language, simulatedPhase, liveMoonInfo) {
        if (simulatedPhase != null) {
            MoonPhaseCalculator.calculateForFraction(language, simulatedPhase!!)
        } else {
            liveMoonInfo
        }
    }
    val sliderFraction = simulatedPhase ?: liveMoonInfo.phaseFraction
    val clover8Shape = MaterialShapes.Clover8Leaf.toShape()

    val presets = remember {
        listOf(
            MoonPhasePreset(MoonPhaseStage.NEW_MOON, 0.00f),
            MoonPhasePreset(MoonPhaseStage.WAXING_CRESCENT, 0.125f),
            MoonPhasePreset(MoonPhaseStage.FIRST_QUARTER, 0.25f),
            MoonPhasePreset(MoonPhaseStage.WAXING_GIBBOUS, 0.375f),
            MoonPhasePreset(MoonPhaseStage.FULL_MOON, 0.50f),
            MoonPhasePreset(MoonPhaseStage.WANING_GIBBOUS, 0.625f),
            MoonPhasePreset(MoonPhaseStage.THIRD_QUARTER, 0.75f),
            MoonPhasePreset(MoonPhaseStage.WANING_CRESCENT, 0.875f)
        )
    }

    val titleText = when (language) {
        AppLanguage.JAPANESE -> "月の満ち欠けシミュレーター"
        AppLanguage.KOREAN -> "달의 위상 시뮬레이터"
        AppLanguage.SIMPLIFIED_CHINESE -> "月相模拟器"
        AppLanguage.TRADITIONAL_CHINESE -> "月相模擬器"
        AppLanguage.ENGLISH -> "Moon Phase Simulator"
        AppLanguage.SPANISH -> "Simulador de fases lunares"
        AppLanguage.FRENCH -> "Simulateur de phases lunaires"
        AppLanguage.PORTUGUESE -> "Simulador de fases da lua"
        AppLanguage.RUSSIAN -> "Симулятор фаз Луны"
    }

    val statusText = if (simulatedPhase != null) {
        when (language) {
            AppLanguage.JAPANESE -> "シミュレーション中"
            AppLanguage.KOREAN -> "시뮬레이션 중"
            AppLanguage.SIMPLIFIED_CHINESE -> "模拟中"
            AppLanguage.TRADITIONAL_CHINESE -> "模擬中"
            AppLanguage.ENGLISH -> "Simulating"
            AppLanguage.SPANISH -> "Simulando"
            AppLanguage.FRENCH -> "Simulation active"
            AppLanguage.PORTUGUESE -> "Simulando"
            AppLanguage.RUSSIAN -> "Симуляция"
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "現在の実際の月相"
            AppLanguage.KOREAN -> "현재 실제 달 위상"
            AppLanguage.SIMPLIFIED_CHINESE -> "当前真实月相"
            AppLanguage.TRADITIONAL_CHINESE -> "目前真實月相"
            AppLanguage.ENGLISH -> "Live astronomical phase"
            AppLanguage.SPANISH -> "Fase astronómica real"
            AppLanguage.FRENCH -> "Phase astronomique réelle"
            AppLanguage.PORTUGUESE -> "Fase astronômica real"
            AppLanguage.RUSSIAN -> "Текущая фаза Луны"
        }
    }

    val moonAgeLabel = when (language) {
        AppLanguage.JAPANESE -> "月齢 ${activeMoonInfo.ageDays}"
        AppLanguage.KOREAN -> "월령 ${activeMoonInfo.ageDays}"
        AppLanguage.SIMPLIFIED_CHINESE -> "月龄 ${activeMoonInfo.ageDays}"
        AppLanguage.TRADITIONAL_CHINESE -> "月齡 ${activeMoonInfo.ageDays}"
        AppLanguage.ENGLISH -> "Age ${activeMoonInfo.ageDays}d"
        AppLanguage.SPANISH -> "Edad ${activeMoonInfo.ageDays}d"
        AppLanguage.FRENCH -> "Âge ${activeMoonInfo.ageDays} j"
        AppLanguage.PORTUGUESE -> "Idade ${activeMoonInfo.ageDays}d"
        AppLanguage.RUSSIAN -> "Возраст ${activeMoonInfo.ageDays} д."
    }

    val sliderHeaderLabel = when (language) {
        AppLanguage.JAPANESE -> "月齢スライダー（0.0 〜 29.5日）"
        AppLanguage.KOREAN -> "월령 슬라이더 (0.0 ~ 29.5일)"
        AppLanguage.SIMPLIFIED_CHINESE -> "月龄滑块（0.0 ～ 29.5天）"
        AppLanguage.TRADITIONAL_CHINESE -> "月齡滑桿（0.0 ～ 29.5天）"
        AppLanguage.ENGLISH -> "Lunar cycle slider (0.0 – 29.5d)"
        AppLanguage.SPANISH -> "Ciclo lunar (0,0 – 29,5 d)"
        AppLanguage.FRENCH -> "Cycle lunaire (0,0 – 29,5 j)"
        AppLanguage.PORTUGUESE -> "Ciclo lunar (0,0 – 29,5 d)"
        AppLanguage.RUSSIAN -> "Лунный цикл (0,0 – 29,5 д.)"
    }

    val presetsHeaderLabel = when (language) {
        AppLanguage.JAPANESE -> "主要8位相プリセット"
        AppLanguage.KOREAN -> "주요 8위상 프리셋"
        AppLanguage.SIMPLIFIED_CHINESE -> "8大月相预设"
        AppLanguage.TRADITIONAL_CHINESE -> "8大月相預設"
        AppLanguage.ENGLISH -> "8-phase presets"
        AppLanguage.SPANISH -> "Preajustes de 8 fases"
        AppLanguage.FRENCH -> "Préréglages des 8 phases"
        AppLanguage.PORTUGUESE -> "Predefinições das 8 fases"
        AppLanguage.RUSSIAN -> "8 основных фаз"
    }

    val southernLabel = when (language) {
        AppLanguage.JAPANESE -> "南半球の見え方で表示"
        AppLanguage.KOREAN -> "남반구 시점으로 표시"
        AppLanguage.SIMPLIFIED_CHINESE -> "按南半球视角显示"
        AppLanguage.TRADITIONAL_CHINESE -> "依南半球視角顯示"
        AppLanguage.ENGLISH -> "Southern Hemisphere view"
        AppLanguage.SPANISH -> "Vista del hemisferio sur"
        AppLanguage.FRENCH -> "Vue hémisphère sud"
        AppLanguage.PORTUGUESE -> "Visão do hemisfério sul"
        AppLanguage.RUSSIAN -> "Вид из Южного полушария"
    }

    val resetLabel = when (language) {
        AppLanguage.JAPANESE -> "実際の月相に戻す"
        AppLanguage.KOREAN -> "실제 달 위상으로 복원"
        AppLanguage.SIMPLIFIED_CHINESE -> "恢复真实月相"
        AppLanguage.TRADITIONAL_CHINESE -> "恢復真實月相"
        AppLanguage.ENGLISH -> "Reset to live phase"
        AppLanguage.SPANISH -> "Restaurar fase real"
        AppLanguage.FRENCH -> "Revenir à la phase réelle"
        AppLanguage.PORTUGUESE -> "Restaurar fase real"
        AppLanguage.RUSSIAN -> "Вернуть текущую фазу"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ライブプレビュー（暗め紺色の 8-leaf clover + 高精細月相ディスク）
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(134.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(134.dp)
                                    .clip(clover8Shape)
                                    .background(Color(0xFF5C6BC0).copy(alpha = 0.22f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(clover8Shape)
                                    .background(Color(0xFF3F51B5).copy(alpha = 0.42f))
                            )
                            MoonPhaseDisc(
                                phaseFraction = activeMoonInfo.phaseFraction,
                                isSouthernHemisphere = simulatedSouthern == true,
                                modifier = Modifier.size(86.dp)
                            )
                        }

                        Text(
                            text = activeMoonInfo.phaseName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (simulatedPhase != null) {
                                    colorScheme.primaryContainer
                                } else {
                                    colorScheme.surfaceContainerHigh
                                }
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (simulatedPhase != null) {
                                        colorScheme.onPrimaryContainer
                                    } else {
                                        colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "$moonAgeLabel · ${activeMoonInfo.illumination}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 連続スライダー
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sliderHeaderLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${activeMoonInfo.illumination}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary
                        )
                    }
                    Slider(
                        value = sliderFraction,
                        onValueChange = { value ->
                            MoonPhaseSimulator.setSimulatedPhase(context, value)
                        },
                        valueRange = 0f..0.999f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 主要8位相プリセット（2列×4行・ミニ月プレビュー付き）
                Text(
                    text = presetsHeaderLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preset ->
                                val presetInfo = remember(language, preset.phaseFraction) {
                                    MoonPhaseCalculator.calculateForFraction(language, preset.phaseFraction)
                                }
                                val isSelected = simulatedPhase != null &&
                                    (abs(simulatedPhase!! - preset.phaseFraction) < 0.03f ||
                                        (preset.phaseFraction == 0f && simulatedPhase!! > 0.97f))

                                Surface(
                                    onClick = {
                                        expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                                        MoonPhaseSimulator.setSimulatedPhase(context, preset.phaseFraction)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        colorScheme.primaryContainer
                                    } else {
                                        colorScheme.surfaceContainerLow
                                    },
                                    border = if (isSelected) {
                                        BorderStroke(1.2.dp, colorScheme.primary)
                                    } else {
                                        BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.35f))
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3F51B5).copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MoonPhaseDisc(
                                                phaseFraction = preset.phaseFraction,
                                                isSouthernHemisphere = simulatedSouthern == true,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = presetInfo.phaseName,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) {
                                                    colorScheme.onPrimaryContainer
                                                } else {
                                                    colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${presetInfo.illumination}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) {
                                                    colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                } else {
                                                    colorScheme.onSurfaceVariant
                                                },
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 南半球表示切替（ON で南半球固定、OFF で都市の緯度に自動追従）
                val isSouthernToggleOn = simulatedSouthern == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val next = !isSouthernToggleOn
                            expressHaptics.toggle(next)
                            MoonPhaseSimulator.setSimulatedSouthernHemisphere(context, if (next) true else null)
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = southernLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isSouthernToggleOn,
                        onCheckedChange = { checked ->
                            expressHaptics.toggle(checked)
                            MoonPhaseSimulator.setSimulatedSouthernHemisphere(context, if (checked) true else null)
                        }
                    )
                }

                HorizontalDivider(
                    color = colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // 実際の月相に戻す
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            expressHaptics.play(ExpressiveHapticPattern.EXPRESSIVE_BOUNCE)
                            MoonPhaseSimulator.clear(context)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = resetLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(closeText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}


@Composable
fun ThemePresetDialog(
    currentPreset: ThemePreset,
    language: AppLanguage,
    onPresetSelected: (ThemePreset) -> Unit,
    onDismiss: () -> Unit,
    cancelText: String
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "テーマカラーの選択"
                        AppLanguage.KOREAN -> "테마 색상 선택"
                        AppLanguage.SIMPLIFIED_CHINESE -> "选择主题配色"
                        AppLanguage.TRADITIONAL_CHINESE -> "選擇主題配色"
                        AppLanguage.ENGLISH -> "Select Theme Color"
                        AppLanguage.SPANISH -> "Seleccionar color del tema"
                        AppLanguage.FRENCH -> "Choisir la couleur du thème"
                        AppLanguage.PORTUGUESE -> "Selecionar cor do tema"
                        AppLanguage.RUSSIAN -> "Выбор цвета темы"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "好みのカラーを選択して固定、または天候連動に設定できます"
                        AppLanguage.KOREAN -> "원하는 색상을 고정하거나 날씨에 따라 자동 변경되도록 설정할 수 있습니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "选择喜爱的调色板固定显示，或设置为根据天气自动变换"
                        AppLanguage.TRADITIONAL_CHINESE -> "選擇喜愛的調色盤固定顯示，或設定為隨天氣自動變化"
                        AppLanguage.ENGLISH -> "Choose a color palette to fix or adapt to weather"
                        AppLanguage.SPANISH -> "Elige una paleta fija o que se adapte al clima"
                        AppLanguage.FRENCH -> "Choisissez une palette fixe ou adaptée à la météo"
                        AppLanguage.PORTUGUESE -> "Escolha uma paleta fixa ou adaptada ao clima"
                        AppLanguage.RUSSIAN -> "Выберите фиксированную палитру или адаптацию к погоде"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemePreset.entries.forEach { preset ->
                    val isSelected = preset == currentPreset
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onPresetSelected(preset)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName(language),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = preset.description(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // 単一の代表色スウォッチ（天候連動時は4分割タイル）
                            if (preset == ThemePreset.WEATHER) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                        .padding(2.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEDEDED)))
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF9E9E9E)))
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF262626)))
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF5E5E5E)))
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(preset.previewPrimary)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

private data class DataSourceCreditEntry(
    val name: String,
    val category: String,
    val usage: String,
    val attribution: String,
    val url: String
)

@Composable
fun DataSourcesCreditsDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val items = remember(language) {
        listOf(
            DataSourceCreditEntry(
                name = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA)"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA)"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA)"
                    AppLanguage.ENGLISH -> "Japan Meteorological Agency (JMA)"
                    AppLanguage.SPANISH -> "Agencia Meteorológica de Japón (JMA)"
                    AppLanguage.FRENCH -> "Agence météorologique du Japon (JMA)"
                    AppLanguage.PORTUGUESE -> "Agência Meteorológica do Japão (JMA)"
                    AppLanguage.RUSSIAN -> "Японское метеорологическое агентство (JMA)"
                },
                category = when (language) {
                    AppLanguage.JAPANESE -> "国内高精度実況・防災"
                    AppLanguage.KOREAN -> "일본 고정밀 실황·방재"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本高分辨率降水・防灾预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本高解析度降水・防災預警"
                    AppLanguage.ENGLISH -> "Japan Radar & Disaster Alerts"
                    AppLanguage.SPANISH -> "Radar y alertas de Japón"
                    AppLanguage.FRENCH -> "Radar et alertes du Japon"
                    AppLanguage.PORTUGUESE -> "Radar e alertas do Japão"
                    AppLanguage.RUSSIAN -> "Радар и оповещения (Япония)"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "高解像度降水ナウキャスト(250m)、降水短時間予報(+15h)、キキクル(危険度分布)、気象衛星ひまわり、特別警報・警報・注意報"
                    AppLanguage.KOREAN -> "고해상도 강수 나우캐스트(250m), 단기 강수 예보(+15h), 키키쿠루(위험도 분포), 히마와리 기상위성, 특별경보·경보·주의보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "高分辨率降水临近预报(250m)、短时降水预报(+15h)、危险度分布(Kikikuru)、向日葵气象卫星、气象警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "高解析度降水臨近預報(250m)、短時降水預報(+15h)、危險度分佈(Kikikuru)、向日葵氣象衛星、氣象警報"
                    AppLanguage.ENGLISH -> "High-Resolution Nowcast (250m), Precipitation Forecast (+15h), Kikikuru, Himawari Satellite, Weather Warnings"
                    AppLanguage.SPANISH -> "Pronóstico inmediato de alta resolución (250 m), precipitación (+15 h), Kikikuru, satélite Himawari y alertas meteorológicas"
                    AppLanguage.FRENCH -> "Prévision immédiate haute résolution (250 m), précipitations (+15 h), Kikikuru, satellite Himawari et alertes météo"
                    AppLanguage.PORTUGUESE -> "Previsão imediata de alta resolução (250 m), precipitação (+15 h), Kikikuru, satélite Himawari e alertas meteorológicos"
                    AppLanguage.RUSSIAN -> "Сверхкраткосрочный прогноз осадков (250 м), прогноз (+15 ч), Kikikuru, спутник Himawari и штормовые предупреждения"
                },
                attribution = when (language) {
                    AppLanguage.JAPANESE -> "出典：気象庁（政府標準利用規約2.0準拠）"
                    AppLanguage.KOREAN -> "출처: 일본 기상청 (정부 표준 이용약관 2.0 준거)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "来源：日本气象厅"
                    AppLanguage.TRADITIONAL_CHINESE -> "來源：日本氣象廳"
                    AppLanguage.ENGLISH -> "Source: Japan Meteorological Agency"
                    AppLanguage.SPANISH -> "Fuente: Agencia Meteorológica de Japón (JMA)"
                    AppLanguage.FRENCH -> "Source : Agence météorologique du Japon (JMA)"
                    AppLanguage.PORTUGUESE -> "Fonte: Agência Meteorológica do Japão (JMA)"
                    AppLanguage.RUSSIAN -> "Источник: Японское метеорологическое агентство (JMA)"
                },
                url = "https://www.jma.go.jp/"
            ),
            DataSourceCreditEntry(
                name = "Open-Meteo",
                category = when (language) {
                    AppLanguage.JAPANESE -> "全世界気象予報・大気質"
                    AppLanguage.KOREAN -> "글로벌 기상 예보·대기질"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球天气预报・空气质量"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球天氣預報・空氣品質"
                    AppLanguage.ENGLISH -> "Global Forecast & Air Quality"
                    AppLanguage.SPANISH -> "Pronóstico global y calidad del aire"
                    AppLanguage.FRENCH -> "Prévisions mondiales et qualité de l'air"
                    AppLanguage.PORTUGUESE -> "Previsão global e qualidade do ar"
                    AppLanguage.RUSSIAN -> "Глобальный прогноз и воздух"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "週間・時間別天気予報、気温、降水量、湿度、風向風速、気圧、紫外線指数(UV)、大気質指数(AQI)"
                    AppLanguage.KOREAN -> "주간·시간별 날씨 예보, 기온, 강수량, 습도, 풍향·풍속, 기압, 자외선 지수(UV), 대기질 지수(AQI)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "逐小时与周天气预报、气温、降水、湿度、风力、气压、紫外线(UV)、空气质量(AQI)"
                    AppLanguage.TRADITIONAL_CHINESE -> "逐小時與週天氣預報、氣溫、降水、濕度、風力、氣壓、紫外線(UV)、空氣品質(AQI)"
                    AppLanguage.ENGLISH -> "Weekly & Hourly Forecast, Temperature, Precipitation, Humidity, Wind, Pressure, UV, AQI"
                    AppLanguage.SPANISH -> "Pronóstico semanal y por horas, temperatura, precipitación, humedad, viento, presión, UV y calidad del aire (AQI)"
                    AppLanguage.FRENCH -> "Prévisions hebdomadaires et horaires, température, précipitations, humidité, vent, pression, UV et qualité de l'air (IQA)"
                    AppLanguage.PORTUGUESE -> "Previsão semanal e horária, temperatura, precipitação, umidade, vento, pressão, UV e qualidade do ar (IQA)"
                    AppLanguage.RUSSIAN -> "Почасовой и недельный прогноз, температура, осадки, влажность, ветер, давление, УФ-индекс и качество воздуха (AQI)"
                },
                attribution = "Weather data by Open-Meteo.com (CC BY 4.0)",
                url = "https://open-meteo.com/"
            ),
            DataSourceCreditEntry(
                name = "RainViewer",
                category = when (language) {
                    AppLanguage.JAPANESE -> "世界複合気象レーダー"
                    AppLanguage.KOREAN -> "글로벌 복합 기상 레이더"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球复合气象雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球複合氣象雷達"
                    AppLanguage.ENGLISH -> "Global Radar Network"
                    AppLanguage.SPANISH -> "Red global de radar"
                    AppLanguage.FRENCH -> "Réseau radar mondial"
                    AppLanguage.PORTUGUESE -> "Rede global de radar"
                    AppLanguage.RUSSIAN -> "Глобальная сеть радаров"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "世界各国の気象レーダー観測網を統合したシームレスな実況レーダータイルおよびタイムライン"
                    AppLanguage.KOREAN -> "세계 각국의 기상 레이더 관측망을 통합한 글로벌 실황 레이더 타일 및 타임라인"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球多国气象雷达整合实况切片与时间轴"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球多國氣象雷達整合實況圖磚與時間軸"
                    AppLanguage.ENGLISH -> "Composite global weather radar observation tiles and timeline"
                    AppLanguage.SPANISH -> "Mosaico global compuesto de radares meteorológicos y línea de tiempo"
                    AppLanguage.FRENCH -> "Tuiles radar météo mondiales composites et chronologie"
                    AppLanguage.PORTUGUESE -> "Mosaico global composto de radares meteorológicos e linha do tempo"
                    AppLanguage.RUSSIAN -> "Сводные глобальные тайлы метеорадаров и шкала времени"
                },
                attribution = "Weather data provided by RainViewer",
                url = "https://www.rainviewer.com/api.html"
            ),
            DataSourceCreditEntry(
                name = "Photon / OpenStreetMap",
                category = when (language) {
                    AppLanguage.JAPANESE -> "都市検索・ジオコーディング"
                    AppLanguage.KOREAN -> "도시 검색·지오코딩"
                    AppLanguage.SIMPLIFIED_CHINESE -> "城市搜索・地理编码"
                    AppLanguage.TRADITIONAL_CHINESE -> "城市搜尋・地理編碼"
                    AppLanguage.ENGLISH -> "Geocoding & Location Search"
                    AppLanguage.SPANISH -> "Geocodificación y búsqueda"
                    AppLanguage.FRENCH -> "Géocodage et recherche"
                    AppLanguage.PORTUGUESE -> "Geocodificação e busca"
                    AppLanguage.RUSSIAN -> "Геокодирование и поиск"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "全世界の都市・地名検索およびGPS座標からの逆ジオコーディング"
                    AppLanguage.KOREAN -> "전 세계 도시·지명 검색 및 GPS 좌표 기반 역지오코딩"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球城市与地点检索及经纬度反向地理编码"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球城市與地點檢索及經緯度反向地理編碼"
                    AppLanguage.ENGLISH -> "Global city search and reverse geocoding from coordinates"
                    AppLanguage.SPANISH -> "Búsqueda global de ciudades y geocodificación inversa desde coordenadas"
                    AppLanguage.FRENCH -> "Recherche mondiale de villes et géocodage inversé depuis les coordonnées"
                    AppLanguage.PORTUGUESE -> "Busca global de cidades e geocodificação reversa a partir de coordenadas"
                    AppLanguage.RUSSIAN -> "Глобальный поиск городов и обратное геокодирование по координатам"
                },
                attribution = "© OpenStreetMap contributors (ODbL) / Photon",
                url = "https://www.openstreetmap.org/copyright"
            ),
            DataSourceCreditEntry(
                name = "NOAA / National Weather Service",
                category = when (language) {
                    AppLanguage.JAPANESE -> "全米レーダー & HRRR予報"
                    AppLanguage.KOREAN -> "미국 레이더 & HRRR 예보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美雷达与 HRRR 预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美雷達與 HRRR 預報"
                    AppLanguage.ENGLISH -> "US Radar & HRRR Forecast"
                    AppLanguage.SPANISH -> "Radar de EE. UU. y modelo HRRR"
                    AppLanguage.FRENCH -> "Radar US et prévisions HRRR"
                    AppLanguage.PORTUGUESE -> "Radar dos EUA e modelo HRRR"
                    AppLanguage.RUSSIAN -> "Радар США и модель HRRR"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "全米NEXRAD地上反射強度実況、MRMSモザイク、HRRR数値予報シミュレーション(+18h)、NWS気象警報"
                    AppLanguage.KOREAN -> "미국 NEXRAD 반사도 실황, MRMS 합성, HRRR 수치 예보 시뮬레이션(+18h), NWS 기상특보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD 地表反射强度、MRMS 复合切片、HRRR 数值预报(+18h)、NWS 警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD 地表反射強度、MRMS 複合圖磚、HRRR 數值預報(+18h)、NWS 警報"
                    AppLanguage.ENGLISH -> "US NEXRAD Reflectivity, MRMS Composite, HRRR Simulation (+18h), NWS Weather Alerts"
                    AppLanguage.SPANISH -> "Reflectividad NEXRAD de EE. UU., compuesto MRMS, simulación HRRR (+18 h) y alertas del NWS"
                    AppLanguage.FRENCH -> "Réflectivité NEXRAD US, composite MRMS, simulation HRRR (+18 h) et alertes NWS"
                    AppLanguage.PORTUGUESE -> "Refletividade NEXRAD dos EUA, composto MRMS, simulação HRRR (+18 h) e alertas do NWS"
                    AppLanguage.RUSSIAN -> "Радарная отражаемость NEXRAD, мозаика MRMS, модель HRRR (+18 ч) и оповещения NWS"
                },
                attribution = "Data provided by NOAA / National Weather Service (Public Domain)",
                url = "https://www.weather.gov/"
            ),
            DataSourceCreditEntry(
                name = "GDACS",
                category = when (language) {
                    AppLanguage.JAPANESE -> "国際自然災害アラート"
                    AppLanguage.KOREAN -> "국제 자연재해 경보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球自然灾害预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球自然災害預警"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                    AppLanguage.SPANISH -> "Alertas globales de desastres"
                    AppLanguage.FRENCH -> "Alertes mondiales de catastrophes"
                    AppLanguage.PORTUGUESE -> "Alertas globais de desastres"
                    AppLanguage.RUSSIAN -> "Глобальные оповещения о ЧС"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "熱帯低気圧（台風・ハリケーン）、地震、津波、火山活動、洪水警報"
                    AppLanguage.KOREAN -> "열대저기압(태풍·허리케인), 지진, 쓰나미, 화산 활동, 홍수 경보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "热带气旋、地震、海啸、火山爆发与洪水警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "熱帶氣旋、地震、海嘯、火山爆發與洪水警報"
                    AppLanguage.ENGLISH -> "Tropical Cyclones, Earthquakes, Tsunamis, Volcanic Eruptions, Floods"
                    AppLanguage.SPANISH -> "Ciclones tropicales, terremotos, tsunamis, erupciones volcánicas e inundaciones"
                    AppLanguage.FRENCH -> "Cyclones tropicaux, séismes, tsunamis, éruptions volcaniques et inondations"
                    AppLanguage.PORTUGUESE -> "Ciclones tropicais, terremotos, tsunamis, erupções vulcânicas e enchentes"
                    AppLanguage.RUSSIAN -> "Тропические циклоны, землетрясения, цунами, извержения вулканов и наводнения"
                },
                attribution = "Disaster alerts provided by GDACS (UN & European Commission)",
                url = "https://www.gdacs.org/"
            ),
            DataSourceCreditEntry(
                name = "WMO",
                category = when (language) {
                    AppLanguage.JAPANESE -> "世界気象機関公式警報"
                    AppLanguage.KOREAN -> "세계기상기구 공식 경보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "世界气象组织官方预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "世界氣象組織官方預警"
                    AppLanguage.ENGLISH -> "Official Weather Warnings"
                    AppLanguage.SPANISH -> "Alertas meteorológicas oficiales"
                    AppLanguage.FRENCH -> "Alertes météo officielles"
                    AppLanguage.PORTUGUESE -> "Alertas meteorológicos oficiais"
                    AppLanguage.RUSSIAN -> "Официальные метеопредупреждения"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "SWIC（Severe Weather Information Centre）を通じた各国気象当局の公式警報"
                    AppLanguage.KOREAN -> "SWIC(악기상 정보 센터)를 통해 제공되는 각국 기상청 공식 경보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "通过 SWIC 汇聚的各国气象部门官方权威警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "透過 SWIC 彙聚的各國氣象部門官方權威警報"
                    AppLanguage.ENGLISH -> "Official severe weather warnings from National Meteorological Services worldwide"
                    AppLanguage.SPANISH -> "Alertas oficiales de clima severo de servicios meteorológicos nacionales de todo el mundo"
                    AppLanguage.FRENCH -> "Alertes officielles de phénomènes violents des services météorologiques nationaux"
                    AppLanguage.PORTUGUESE -> "Alertas oficiais de tempo severo de serviços meteorológicos nacionais no mundo todo"
                    AppLanguage.RUSSIAN -> "Официальные предупреждения об опасных явлениях от национальных метеослужб мира"
                },
                attribution = "Official weather warnings provided by WMO",
                url = "https://severeweather.wmo.int/"
            ),
            DataSourceCreditEntry(
                name = "MeteoAlarm (EUMETNET)",
                category = when (language) {
                    AppLanguage.JAPANESE -> "欧州公式気象警報"
                    AppLanguage.KOREAN -> "유럽 공식 기상특보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "欧洲官方气象预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "歐洲官方氣象警報"
                    AppLanguage.ENGLISH -> "European Weather Warnings"
                    AppLanguage.SPANISH -> "Alertas meteorológicas de Europa"
                    AppLanguage.FRENCH -> "Alertes météo européennes"
                    AppLanguage.PORTUGUESE -> "Alertas meteorológicos da Europa"
                    AppLanguage.RUSSIAN -> "Метеопредупреждения Европы"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "欧州39カ国・地域の公式気象警報（CAP / Atomフィード）"
                    AppLanguage.KOREAN -> "유럽 39개 국가 및 지역의 공식 기상특보 (CAP / Atom 피드)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "欧洲39个国家与地区的官方气象警报（CAP / Atom）"
                    AppLanguage.TRADITIONAL_CHINESE -> "歐洲39個國家與地區的官方氣象警報（CAP / Atom）"
                    AppLanguage.ENGLISH -> "Official weather warnings across 39 European countries (CAP / Atom)"
                    AppLanguage.SPANISH -> "Alertas meteorológicas oficiales en 39 países europeos (CAP / Atom)"
                    AppLanguage.FRENCH -> "Alertes météo officielles dans 39 pays européens (CAP / Atom)"
                    AppLanguage.PORTUGUESE -> "Alertas meteorológicos oficiais em 39 países europeus (CAP / Atom)"
                    AppLanguage.RUSSIAN -> "Официальные метеопредупреждения в 39 странах Европы (CAP / Atom)"
                },
                attribution = "Alerts provided by MeteoAlarm / EUMETNET",
                url = "https://meteoalarm.org/"
            ),
            DataSourceCreditEntry(
                name = "Iowa Environmental Mesonet (IEM)",
                category = when (language) {
                    AppLanguage.JAPANESE -> "全米レーダータイル配信"
                    AppLanguage.KOREAN -> "미국 레이더 타일 서비스"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美雷达切片服务"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美雷達圖磚服務"
                    AppLanguage.ENGLISH -> "US Radar Tile Service"
                    AppLanguage.SPANISH -> "Servicio de mosaicos radar de EE. UU."
                    AppLanguage.FRENCH -> "Service de tuiles radar US"
                    AppLanguage.PORTUGUESE -> "Serviço de mosaicos de radar dos EUA"
                    AppLanguage.RUSSIAN -> "Тайлы радаров США"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "NEXRAD / MRMS 合成レーダーおよび HRRR 数値予報タイル配信"
                    AppLanguage.KOREAN -> "NEXRAD / MRMS 합성 레이더 및 HRRR 수치 예보 지도 타일 제공"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NEXRAD / MRMS 合成雷达与 HRRR 数值预报切片"
                    AppLanguage.TRADITIONAL_CHINESE -> "NEXRAD / MRMS 合成雷達與 HRRR 數值預報圖磚"
                    AppLanguage.ENGLISH -> "NEXRAD / MRMS composite radar and HRRR model map tiles"
                    AppLanguage.SPANISH -> "Mosaicos de radar compuesto NEXRAD / MRMS y del modelo HRRR"
                    AppLanguage.FRENCH -> "Tuiles cartographiques radar NEXRAD / MRMS et modèle HRRR"
                    AppLanguage.PORTUGUESE -> "Mosaicos de radar composto NEXRAD / MRMS e do modelo HRRR"
                    AppLanguage.RUSSIAN -> "Композитные радарные карты NEXRAD / MRMS и тайлы модели HRRR"
                },
                attribution = "Tiles hosted by Iowa Environmental Mesonet (Iowa State University)",
                url = "https://mesonet.agron.iastate.edu/"
            ),
            DataSourceCreditEntry(
                name = when (language) {
                    AppLanguage.JAPANESE -> "国土地理院 (GSI)"
                    AppLanguage.KOREAN -> "일본 국토지리원 (GSI)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本国土地理院 (GSI)"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本國土地理院 (GSI)"
                    AppLanguage.ENGLISH -> "Geospatial Information Authority of Japan (GSI)"
                    AppLanguage.SPANISH -> "Autoridad de Información Geoespacial de Japón (GSI)"
                    AppLanguage.FRENCH -> "Autorité d'information géospatiale du Japon (GSI)"
                    AppLanguage.PORTUGUESE -> "Autoridade de Informação Geoespacial do Japão (GSI)"
                    AppLanguage.RUSSIAN -> "Управление геопространственной информации Японии (GSI)"
                },
                category = when (language) {
                    AppLanguage.JAPANESE -> "背景地図・ハザードマップ"
                    AppLanguage.KOREAN -> "배경 지도·재해 위험 지도"
                    AppLanguage.SIMPLIFIED_CHINESE -> "底图与灾害地图"
                    AppLanguage.TRADITIONAL_CHINESE -> "底圖與災害地圖"
                    AppLanguage.ENGLISH -> "Base Maps & Hazard Overlays"
                    AppLanguage.SPANISH -> "Mapas base y zonas de riesgo"
                    AppLanguage.FRENCH -> "Fonds de carte et zones à risque"
                    AppLanguage.PORTUGUESE -> "Mapas base e áreas de risco"
                    AppLanguage.RUSSIAN -> "Базовые карты и зоны риска"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "日本域ウィジェット背景地図（淡色・白地図）および浸水想定区域オーバーレイ"
                    AppLanguage.KOREAN -> "일본 지역 위젯 배경 지도 및 침수 예상 구역 오버레이"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本地区小组件底图及洪水风险图层"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本地區小工具底圖及淹水潛勢圖層"
                    AppLanguage.ENGLISH -> "Japan widget base tiles (pale/blank) and flood hazard overlays"
                    AppLanguage.SPANISH -> "Mapas base de widgets para Japón y capas de riesgo de inundación"
                    AppLanguage.FRENCH -> "Fonds de carte de widget pour le Japon et couches de risque d'inondation"
                    AppLanguage.PORTUGUESE -> "Mapas base de widgets para o Japão e camadas de risco de inundação"
                    AppLanguage.RUSSIAN -> "Базовые карты виджетов для Японии и слои зон возможного затопления"
                },
                attribution = when (language) {
                    AppLanguage.JAPANESE -> "出典：国土地理院（地理院タイル）"
                    AppLanguage.KOREAN -> "출처: 일본 국토지리원 (지리원 타일)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "来源：日本国土地理院（地理院地图）"
                    AppLanguage.TRADITIONAL_CHINESE -> "來源：日本國土地理院（地理院圖磚）"
                    AppLanguage.ENGLISH -> "Source: Geospatial Information Authority of Japan (GSI Tiles)"
                    AppLanguage.SPANISH -> "Fuente: Autoridad de Información Geoespacial de Japón (GSI)"
                    AppLanguage.FRENCH -> "Source : Autorité d'information géospatiale du Japon (GSI)"
                    AppLanguage.PORTUGUESE -> "Fonte: Autoridade de Informação Geoespacial do Japão (GSI)"
                    AppLanguage.RUSSIAN -> "Источник: Управление геопространственной информации Японии (GSI)"
                },
                url = "https://maps.gsi.go.jp/development/ichiran.html"
            ),
            DataSourceCreditEntry(
                name = "Google Maps Platform",
                category = when (language) {
                    AppLanguage.JAPANESE -> "インタラクティブ地図"
                    AppLanguage.KOREAN -> "인터랙티브 지도"
                    AppLanguage.SIMPLIFIED_CHINESE -> "交互式地图"
                    AppLanguage.TRADITIONAL_CHINESE -> "互動式地圖"
                    AppLanguage.ENGLISH -> "Interactive Maps"
                    AppLanguage.SPANISH -> "Mapas interactivos"
                    AppLanguage.FRENCH -> "Cartes interactives"
                    AppLanguage.PORTUGUESE -> "Mapas interativos"
                    AppLanguage.RUSSIAN -> "Интерактивные карты"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー・キキクル・台風進路表示用のベースマップ描画"
                    AppLanguage.KOREAN -> "강수 레이더·키키쿠루·태풍 경로 표시용 기본 지도 렌더링"
                    AppLanguage.SIMPLIFIED_CHINESE -> "用于降水雷达、危险度分布及台风路径的交互底图"
                    AppLanguage.TRADITIONAL_CHINESE -> "用於降雨雷達、危險度分佈及颱風路徑的互動底圖"
                    AppLanguage.ENGLISH -> "Interactive base map rendering for weather radar, Kikikuru, and typhoon tracks"
                    AppLanguage.SPANISH -> "Mapa base interactivo para radar meteorológico, Kikikuru y trayectorias de tifones"
                    AppLanguage.FRENCH -> "Fond de carte interactif pour le radar météo, Kikikuru et les trajectoires de typhons"
                    AppLanguage.PORTUGUESE -> "Mapa base interativo para radar meteorológico, Kikikuru e trajetórias de tufões"
                    AppLanguage.RUSSIAN -> "Интерактивная базовая карта для радара осадков, Kikikuru и траекторий тайфунов"
                },
                attribution = "Map data © Google",
                url = "https://www.google.com/help/terms_maps/"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Public,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "データソースと帰属表記"
                    AppLanguage.KOREAN -> "데이터 출처 및 저작권 표기"
                    AppLanguage.SIMPLIFIED_CHINESE -> "数据来源与致谢"
                    AppLanguage.TRADITIONAL_CHINESE -> "資料來源與致謝"
                    AppLanguage.ENGLISH -> "Data Sources & Credits"
                    AppLanguage.SPANISH -> "Fuentes de datos y créditos"
                    AppLanguage.FRENCH -> "Sources de données et crédits"
                    AppLanguage.PORTUGUESE -> "Fontes de dados e créditos"
                    AppLanguage.RUSSIAN -> "Источники данных и авторы"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSecondaryContainer,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.usage,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.attribution,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "公式"
                                            AppLanguage.KOREAN -> "공식 사이트"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "官网"
                                            AppLanguage.TRADITIONAL_CHINESE -> "官網"
                                            AppLanguage.ENGLISH -> "Website"
                                            AppLanguage.SPANISH -> "Sitio web"
                                            AppLanguage.FRENCH -> "Site web"
                                            AppLanguage.PORTUGUESE -> "Site"
                                            AppLanguage.RUSSIAN -> "Сайт"
                                        },
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "※本アプリは各気象機関および公的機関の公式アプリではありません。提供される情報は教育・参考用であり、緊急時は自治体や気象庁等の公式発表をご確認ください。"
                            AppLanguage.KOREAN -> "※ 본 앱은 각 기상 기관 및 공공기관의 공식 앱이 아닙니다. 제공되는 정보는 참고용이며, 비상시에는 지자체나 기상청 등의 공식 발표를 확인해 주세요."
                            AppLanguage.SIMPLIFIED_CHINESE -> "※本应用非各气象部门或官方机构的官方应用。所有信息仅供参考，紧急情况下请务必以官方权威发布为准。"
                            AppLanguage.TRADITIONAL_CHINESE -> "※本應用程式非各氣象部門或官方機構的官方應用程式。所有資訊僅供參考，緊急情況下請務必以官方權威發布為準。"
                            AppLanguage.ENGLISH -> "Note: This is an unofficial application and not affiliated with any official meteorological agency. In emergencies, please consult official government announcements."
                            AppLanguage.SPANISH -> "Nota: Esta es una aplicación no oficial y no está afiliada a ninguna agencia meteorológica oficial. En emergencias, consulta los avisos oficiales de las autoridades."
                            AppLanguage.FRENCH -> "Remarque : Cette application n'est pas officielle et n'est affiliée à aucun service météorologique officiel. En cas d'urgence, consultez les annonces officielles des autorités."
                            AppLanguage.PORTUGUESE -> "Aviso: Este é um aplicativo não oficial e não é afiliado a nenhuma agência meteorológica oficial. Em emergências, consulte os comunicados oficiais das autoridades."
                            AppLanguage.RUSSIAN -> "Примечание: это неофициальное приложение, не связанное с какими-либо метеорологическими ведомствами. В чрезвычайных ситуациях ориентируйтесь на официальные сообщения властей."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.KOREAN -> "닫기"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                        AppLanguage.SPANISH -> "Cerrar"
                        AppLanguage.FRENCH -> "Fermer"
                        AppLanguage.PORTUGUESE -> "Fechar"
                        AppLanguage.RUSSIAN -> "Закрыть"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

@Composable
fun OpenSourceLicenseDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Policy,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "オープンソースライセンス"
                    AppLanguage.KOREAN -> "오픈소스 라이선스"
                    AppLanguage.SIMPLIFIED_CHINESE -> "开源许可证"
                    AppLanguage.TRADITIONAL_CHINESE -> "開源許可證"
                    AppLanguage.ENGLISH -> "Open Source License"
                    AppLanguage.SPANISH -> "Licencia de código abierto"
                    AppLanguage.FRENCH -> "Licence open source"
                    AppLanguage.PORTUGUESE -> "Licença de código aberto"
                    AppLanguage.RUSSIAN -> "Лицензия открытого ПО"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apache License 2.0",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather は Apache License 2.0 の下で公開されているオープンソースソフトウェアです。本ソフトウェアは「現状有姿（AS IS）」で提供され、明示・黙示を問わず一切の保証を行いません。"
                        AppLanguage.KOREAN -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather는 Apache License 2.0 라이선스로 공개된 오픈소스 소프트웨어입니다. 본 소프트웨어는 어떠한 명시적·묵시적 보증 없이 \"있는 그대로(AS IS)\" 제공됩니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather 基于 Apache License 2.0 开源协议发布。本软件按「原样（AS IS）」提供，不提供任何明示或暗示的担保。"
                        AppLanguage.TRADITIONAL_CHINESE -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather 依據 Apache License 2.0 開源授權條款發布。本軟體依「現況（AS IS）」提供，不提供任何明示或默示擔保。"
                        AppLanguage.ENGLISH -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather is open-source software licensed under the Apache License, Version 2.0. This software is provided \"AS IS\", WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND."
                        AppLanguage.SPANISH -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather es software de código abierto bajo la licencia Apache, versión 2.0. Este software se proporciona \"TAL CUAL\" (AS IS), SIN GARANTÍAS NI CONDICIONES DE NINGÚN TIPO."
                        AppLanguage.FRENCH -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather est un logiciel open source sous licence Apache, version 2.0. Ce logiciel est fourni « EN L'ÉTAT » (AS IS), SANS GARANTIE NI CONDITION D'AUCUNE SORTE."
                        AppLanguage.PORTUGUESE -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather é um software de código aberto licenciado sob a Licença Apache, versão 2.0. Este software é fornecido \"COMO ESTÁ\" (AS IS), SEM GARANTIAS OU CONDIÇÕES DE QUALQUER TIPO."
                        AppLanguage.RUSSIAN -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather — ПО с открытым исходным кодом под лицензией Apache License 2.0. Программа предоставляется «КАК ЕСТЬ» (AS IS), БЕЗ КАКИХ-ЛИБО ГАРАНТИЙ ИЛИ УСЛОВИЙ."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "サードパーティ OSS・アセットライセンス"
                                AppLanguage.KOREAN -> "서드파티 OSS 및 에셋 라이선스"
                                AppLanguage.SIMPLIFIED_CHINESE -> "第三方开源组件与资源许可"
                                AppLanguage.TRADITIONAL_CHINESE -> "第三方開源元件與資源授權"
                                AppLanguage.ENGLISH -> "Third-Party OSS & Asset Licenses"
                                AppLanguage.SPANISH -> "Licencias de terceros (OSS y recursos)"
                                AppLanguage.FRENCH -> "Licences tierces (OSS et ressources)"
                                AppLanguage.PORTUGUESE -> "Licenças de terceiros (OSS e recursos)"
                                AppLanguage.RUSSIAN -> "Сторонние компоненты и ресурсы"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        val thirdPartyLines = listOf(
                            "• Google Sans Flex Font — SIL Open Font License 1.1 (Copyright Google LLC)",
                            "• Fluent System Icons — MIT License (Copyright Microsoft Corp.)",
                            "• AndroidX / Jetpack Compose / Room / WorkManager / Navigation3 — Apache-2.0",
                            "• Kotlin / Coroutines / kotlinx.serialization — Apache-2.0",
                            "• OkHttp — Apache-2.0 (Square, Inc.)",
                            "• Haze — Apache-2.0 (Chris Banes)",
                            "• Google Play Services Maps SDK — Google APIs Terms of Service"
                        )
                        thirdPartyLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nemy-new/Expressive-Weather"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.apache.org/licenses/LICENSE-2.0"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Apache 2.0", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (BuildConfig.ENABLE_EXTERNAL_DONATION_LINKS) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/nemynew"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalCafe,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "Buy Me a Coffee で支援する"
                                AppLanguage.KOREAN -> "Buy Me a Coffee로 후원하기"
                                AppLanguage.SIMPLIFIED_CHINESE -> "在 Buy Me a Coffee 支持作者"
                                AppLanguage.TRADITIONAL_CHINESE -> "在 Buy Me a Coffee 支持作者"
                                AppLanguage.ENGLISH -> "Buy Me a Coffee"
                                AppLanguage.SPANISH -> "Apoyar en Buy Me a Coffee"
                                AppLanguage.FRENCH -> "Soutenir sur Buy Me a Coffee"
                                AppLanguage.PORTUGUESE -> "Apoiar no Buy Me a Coffee"
                                AppLanguage.RUSSIAN -> "Поддержать на Buy Me a Coffee"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.KOREAN -> "닫기"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                        AppLanguage.SPANISH -> "Cerrar"
                        AppLanguage.FRENCH -> "Fermer"
                        AppLanguage.PORTUGUESE -> "Fechar"
                        AppLanguage.RUSSIAN -> "Закрыть"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

/**
 * Google Play バックグラウンド位置情報ポリシー準拠の顕著な開示（Prominent Disclosure）ダイアログ
 */
@Composable
fun BackgroundLocationDisclosureDialog(
    language: AppLanguage,
    onConfirmProceed: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "バックグラウンド位置情報の利用について"
                    AppLanguage.KOREAN -> "백그라운드 위치 정보 사용 안내"
                    AppLanguage.SIMPLIFIED_CHINESE -> "关于后台位置信息的使用说明"
                    AppLanguage.TRADITIONAL_CHINESE -> "關於後台位置資訊的使用說明"
                    AppLanguage.ENGLISH -> "Background Location Disclosure"
                    AppLanguage.SPANISH -> "Uso de ubicación en segundo plano"
                    AppLanguage.FRENCH -> "Localisation en arrière-plan"
                    AppLanguage.PORTUGUESE -> "Uso de localização em segundo plano"
                    AppLanguage.RUSSIAN -> "Доступ к геолокации в фоне"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "Expressive Weather は、アプリを閉じている間や使用していないときにも位置情報データを収集し、以下の機能を有効にします："
                        AppLanguage.KOREAN -> "Expressive Weather는 앱이 닫혀 있거나 사용 중이지 않을 때도 위치 데이터를 수집하여 다음 기능을 제공합니다:"
                        AppLanguage.SIMPLIFIED_CHINESE -> "Expressive Weather 会在应用关闭或未使用时收集位置数据，以支持以下功能："
                        AppLanguage.TRADITIONAL_CHINESE -> "Expressive Weather 會在應用程式關閉或未使用時收集位置資料，以支援以下功能："
                        AppLanguage.ENGLISH -> "Expressive Weather collects location data to enable the following features even when the app is closed or not in use:"
                        AppLanguage.SPANISH -> "Expressive Weather recopila datos de ubicación para habilitar las siguientes funciones incluso cuando la app está cerrada o no está en uso:"
                        AppLanguage.FRENCH -> "Expressive Weather collecte des données de localisation pour activer les fonctionnalités suivantes même lorsque l'application est fermée ou inutilisée :"
                        AppLanguage.PORTUGUESE -> "O Expressive Weather coleta dados de localização para ativar os recursos a seguir mesmo quando o app está fechado ou fora de uso:"
                        AppLanguage.RUSSIAN -> "Expressive Weather собирает данные о местоположении для работы следующих функций, даже когда приложение закрыто или не используется:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val bullets = when (language) {
                            AppLanguage.JAPANESE -> listOf(
                                "• 移動先に応じた気象警報・特別警報・キキクル（危険度分布）の自動監視と通知",
                                "• 雨雲の接近・降り止みリアルタイムカウントダウン通知の現在地追従",
                                "• ホーム画面ウィジェットおよび天気予報のバックグラウンド自動更新"
                            )
                            AppLanguage.KOREAN -> listOf(
                                "• 이동 위치에 맞춘 기상특보·재난 경보·키키쿠루(위험도 분포) 자동 감지 및 알림",
                                "• 현재 위치 기반 강수 접근·종료 실시간 카운트다운 알림",
                                "• 홈 화면 위젯 및 날씨 예보의 백그라운드 자동 업데이트"
                            )
                            AppLanguage.SIMPLIFIED_CHINESE -> listOf(
                                "• 根据当前位置变化自动监测并推送灾害预警与危险度分布",
                                "• 降雨临近与雨停倒计时实时通知的位置跟随",
                                "• 主屏幕小组件与天气预报的后台自动刷新"
                            )
                            AppLanguage.TRADITIONAL_CHINESE -> listOf(
                                "• 依據目前位置變化自動監測並推播災害警報與危險度分佈",
                                "• 降雨接近與雨停倒數即時通知的位置追蹤",
                                "• 主畫面小工具與天氣預報的後台自動更新"
                            )
                            AppLanguage.ENGLISH -> listOf(
                                "• Automatic severe weather & disaster alert monitoring as you travel",
                                "• Live precipitation approach/stop countdown notifications following your location",
                                "• Background weather and home-screen widget updates for your current area"
                            )
                            AppLanguage.SPANISH -> listOf(
                                "• Monitoreo automático de alertas meteorológicas y desastres mientras te desplazas",
                                "• Notificaciones en tiempo real de inicio y fin de lluvia según tu ubicación",
                                "• Actualización automática en segundo plano del clima y los widgets de la pantalla de inicio"
                            )
                            AppLanguage.FRENCH -> listOf(
                                "• Surveillance automatique des alertes météo et catastrophes lors de vos déplacements",
                                "• Notifications en direct du compte à rebours d'arrivée et de fin de pluie selon votre position",
                                "• Mise à jour automatique en arrière-plan de la météo et des widgets de l'écran d'accueil"
                            )
                            AppLanguage.PORTUGUESE -> listOf(
                                "• Monitoramento automático de alertas meteorológicos e desastres conforme você se desloca",
                                "• Notificações em tempo real de contagem regressiva de início e fim de chuva na sua localização",
                                "• Atualização automática em segundo plano do clima e dos widgets da tela inicial"
                            )
                            AppLanguage.RUSSIAN -> listOf(
                                "• Автоматический мониторинг штормовых предупреждений и ЧС в пути",
                                "• Уведомления в реальном времени о приближении и окончании осадков по вашему местоположению",
                                "• Фоновое обновление погоды и виджетов на главном экране для вашего текущего района"
                            )
                        }
                        bullets.forEach { bullet ->
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "位置座標は天気・警報データ取得のためのみに各気象機関・天気APIへ送信され、広告配信や第三者への販売には一切使用されません。許可は任意であり、Androidの設定からいつでも変更・撤回できます。"
                        AppLanguage.KOREAN -> "위치 좌표는 날씨와 기상특보 데이터를 불러오기 위해서만 각 기상 기관 및 날씨 API로 전송되며, 광고나 제3자 판매에는 절대 사용되지 않습니다. 이 권한은 선택 사항이며 Android 설정에서 언제든지 변경하거나 철회할 수 있습니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "位置坐标仅用于向气象数据服务请求天气与预警信息，绝不用于广告或出售给第三方。该权限为可选项，您可随时在系统设置中更改或撤销。"
                        AppLanguage.TRADITIONAL_CHINESE -> "位置座標僅用於向氣象資料服務請求天氣與警報資訊，絕不用於廣告或出售給第三方。此權限為可選項，您可隨時在系統設定中變更或撤銷。"
                        AppLanguage.ENGLISH -> "Your coordinates are sent solely to weather and alert providers to fetch local forecasts and warnings. They are never sold or used for advertising. This permission is optional and can be revoked at any time in Android Settings."
                        AppLanguage.SPANISH -> "Tus coordenadas se envían únicamente a proveedores de clima y alertas para obtener pronósticos y avisos locales. Nunca se venden ni se usan para publicidad. Este permiso es opcional y puede revocarse en cualquier momento en los ajustes de Android."
                        AppLanguage.FRENCH -> "Vos coordonnées sont envoyées uniquement aux fournisseurs météo et d'alertes pour récupérer les prévisions et avis locaux. Elles ne sont jamais vendues ni utilisées à des fins publicitaires. Cette autorisation est facultative et révocable à tout moment dans les paramètres Android."
                        AppLanguage.PORTUGUESE -> "Suas coordenadas são enviadas apenas aos provedores de clima e alertas para obter previsões e avisos locais. Elas nunca são vendidas nem usadas para publicidade. Essa permissão é opcional e pode ser revogada a qualquer momento nas configurações do Android."
                        AppLanguage.RUSSIAN -> "Ваши координаты передаются только поставщикам погодных данных и оповещений для получения локальных прогнозов и предупреждений. Они никогда не продаются и не используются для рекламы. Это разрешение необязательно и может быть отозвано в любой момент в настройках Android."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onConfirmProceed) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "同意して設定を開く"
                        AppLanguage.KOREAN -> "동의하고 설정 열기"
                        AppLanguage.SIMPLIFIED_CHINESE -> "同意并前往设置"
                        AppLanguage.TRADITIONAL_CHINESE -> "同意並前往設定"
                        AppLanguage.ENGLISH -> "Continue to Settings"
                        AppLanguage.SPANISH -> "Continuar a Ajustes"
                        AppLanguage.FRENCH -> "Continuer vers les paramètres"
                        AppLanguage.PORTUGUESE -> "Continuar para Configurações"
                        AppLanguage.RUSSIAN -> "Перейти в настройки"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "キャンセル"
                        AppLanguage.KOREAN -> "취소"
                        AppLanguage.SIMPLIFIED_CHINESE -> "取消"
                        AppLanguage.TRADITIONAL_CHINESE -> "取消"
                        AppLanguage.ENGLISH -> "Not Now"
                        AppLanguage.SPANISH -> "Ahora no"
                        AppLanguage.FRENCH -> "Plus tard"
                        AppLanguage.PORTUGUESE -> "Agora não"
                        AppLanguage.RUSSIAN -> "Не сейчас"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

/**
 * プライバシーポリシー要約＆全文リンクダイアログ
 */
@Composable
fun PrivacyPolicyDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val privacyPolicyUrl = "https://github.com/nemy-new/Expressive-Weather/blob/main/PRIVACY_POLICY.md"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.PrivacyTip,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "プライバシーポリシー"
                    AppLanguage.KOREAN -> "개인정보 처리방침"
                    AppLanguage.SIMPLIFIED_CHINESE -> "隐私政策"
                    AppLanguage.TRADITIONAL_CHINESE -> "隱私權政策"
                    AppLanguage.ENGLISH -> "Privacy Policy"
                    AppLanguage.SPANISH -> "Política de privacidad"
                    AppLanguage.FRENCH -> "Politique de confidentialité"
                    AppLanguage.PORTUGUESE -> "Política de privacidade"
                    AppLanguage.RUSSIAN -> "Политика конфиденциальности"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "Expressive Weather は開発者独自の収集サーバーを持たず、アカウント登録や広告トラッキングも行いません。"
                        AppLanguage.KOREAN -> "Expressive Weather는 개발자 자체 수집 서버를 운영하지 않으며, 계정 가입이나 광고 추적도 하지 않습니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "Expressive Weather 不运营开发者自建收集服务器，不要求账号注册，也不进行广告追踪。"
                        AppLanguage.TRADITIONAL_CHINESE -> "Expressive Weather 不營運開發者自建收集伺服器，不要求帳號註冊，也不進行廣告追蹤。"
                        AppLanguage.ENGLISH -> "Expressive Weather operates no developer-owned analytics or tracking servers, requires no account, and displays no ads."
                        AppLanguage.SPANISH -> "Expressive Weather no opera servidores propios de análisis ni seguimiento, no requiere cuenta y no muestra anuncios."
                        AppLanguage.FRENCH -> "Expressive Weather n'exploite aucun serveur d'analyse ou de suivi, ne nécessite aucun compte et n'affiche aucune publicité."
                        AppLanguage.PORTUGUESE -> "O Expressive Weather não opera servidores próprios de análise ou rastreamento, não exige conta e não exibe anúncios."
                        AppLanguage.RUSSIAN -> "Expressive Weather не использует собственные серверы аналитики и отслеживания, не требует учётной записи и не показывает рекламу."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val items = when (language) {
                            AppLanguage.JAPANESE -> listOf(
                                "• 位置情報（任意）：天気予報・降水ナウキャスト・災害警報・ジオコーディングの取得のみに使用。許可した場合のみバックグラウンドでも更新されます。",
                                "• 検索クエリ：都市検索のため Photon (OpenStreetMap) API へ送信されます。",
                                "• 外部通信先：Open-Meteo、気象庁(JMA)、国土地理院(GSI)、NOAA/NWS、IEM、MeteoAlarm、WMO、GDACS、RainViewer、Photon、Google Maps SDK。",
                                "• 端末内保存：保存都市・天気キャッシュ・設定のみを端末内に保存します（アプリ削除またはキャッシュ消去で削除可能）。"
                            )
                            AppLanguage.KOREAN -> listOf(
                                "• 위치 정보(선택): 날씨 예보·강수 나우캐스트·재난 경보·역지오코딩 조회에만 사용됩니다. 직접 허용한 경우에만 백그라운드에서 업데이트됩니다.",
                                "• 검색어: 도시 검색을 위해 Photon(OpenStreetMap) API로 전송됩니다.",
                                "• 외부 통신 대상: Open-Meteo, 일본 기상청(JMA), 일본 국토지리원(GSI), NOAA/NWS, IEM, MeteoAlarm, WMO, GDACS, RainViewer, Photon, Google Maps SDK.",
                                "• 기기 내 저장: 저장된 도시, 날씨 캐시 및 설정만 기기 로컬에 저장됩니다(앱 삭제 또는 캐시 삭제로 제거 가능)."
                            )
                            AppLanguage.SIMPLIFIED_CHINESE -> listOf(
                                "• 位置信息（可选）：仅用于获取天气预报、降水临近预报、灾害预警与地理编码。仅在您授权后于后台更新。",
                                "• 搜索词：用于通过 Photon (OpenStreetMap) 检索城市名称。",
                                "• 第三方接口：Open-Meteo、JMA、GSI、NOAA/NWS、IEM、MeteoAlarm、WMO、GDACS、RainViewer、Photon、Google Maps SDK。",
                                "• 本地存储：仅在设备本地保存收藏城市、天气缓存与偏好设置。"
                            )
                            AppLanguage.TRADITIONAL_CHINESE -> listOf(
                                "• 位置資訊（可選）：僅用於取得天氣預報、降水臨近預報、災害警報與地理編碼。僅在您授權後於後台更新。",
                                "• 搜尋詞：用於透過 Photon (OpenStreetMap) 檢索城市名稱。",
                                "• 第三方介面：Open-Meteo、JMA、GSI、NOAA/NWS、IEM、MeteoAlarm、WMO、GDACS、RainViewer、Photon、Google Maps SDK。",
                                "• 本機儲存：僅在裝置本機儲存收藏城市、天氣快取與偏好設定。"
                            )
                            AppLanguage.ENGLISH -> listOf(
                                "• Location (Optional): Used solely to fetch forecasts, nowcasts, disaster alerts, and reverse geocoding. Background access occurs only if explicitly enabled by you.",
                                "• Search Queries: Sent to Photon (OpenStreetMap) for city lookup.",
                                "• Third-Party Providers: Open-Meteo, JMA, GSI, NOAA/NWS, IEM, MeteoAlarm, WMO, GDACS, RainViewer, Photon, and Google Maps SDK.",
                                "• On-Device Storage: Saved cities, weather caches, and preferences stay locally on your device."
                            )
                            AppLanguage.SPANISH -> listOf(
                                "• Ubicación (opcional): Se usa únicamente para obtener pronósticos, pronósticos inmediatos, alertas de desastres y geocodificación inversa. El acceso en segundo plano solo ocurre si lo habilitas.",
                                "• Búsquedas: Se envían a Photon (OpenStreetMap) para buscar ciudades.",
                                "• Proveedores externos: Open-Meteo, JMA, GSI, NOAA/NWS, IEM, MeteoAlarm, WMO, GDACS, RainViewer, Photon y Google Maps SDK.",
                                "• Almacenamiento local: Las ciudades guardadas, la caché del clima y las preferencias permanecen en tu dispositivo."
                            )
                            AppLanguage.FRENCH -> listOf(
                                "• Localisation (facultative) : Utilisée uniquement pour récupérer les prévisions, la prévision immédiate, les alertes et le géocodage inversé. L'accès en arrière-plan n'a lieu que si vous l'activez.",
                                "• Requêtes de recherche : Envoyées à Photon (OpenStreetMap) pour la recherche de villes.",
                                "• Fournisseurs tiers : Open-Meteo, JMA, GSI, NOAA/NWS, IEM, MeteoAlarm, WMO, GDACS, RainViewer, Photon et Google Maps SDK.",
                                "• Stockage local : Les villes enregistrées, le cache météo et vos préférences restent sur votre appareil."
                            )
                            AppLanguage.PORTUGUESE -> listOf(
                                "• Localização (opcional): Usada apenas para obter previsões, previsões imediatas, alertas de desastres e geocodificação reversa. O acesso em segundo plano ocorre somente se você ativá-lo.",
                                "• Termos de busca: Enviados ao Photon (OpenStreetMap) para localizar cidades.",
                                "• Provedores terceiros: Open-Meteo, JMA, GSI, NOAA/NWS, IEM, MeteoAlarm, WMO, GDACS, RainViewer, Photon e Google Maps SDK.",
                                "• Armazenamento local: Cidades salvas, cache de clima e preferências permanecem apenas no seu dispositivo."
                            )
                            AppLanguage.RUSSIAN -> listOf(
                                "• Геолокация (необязательно): используется только для прогнозов, радара, оповещений о ЧС и обратного геокодирования. Доступ в фоне осуществляется только с вашего явного разрешения.",
                                "• Поисковые запросы: отправляются в Photon (OpenStreetMap) для поиска городов.",
                                "• Сторонние сервисы: Open-Meteo, JMA, GSI, NOAA/NWS, IEM, MeteoAlarm, WMO, GDACS, RainViewer, Photon и Google Maps SDK.",
                                "• Локальное хранение: сохранённые города, кэш погоды и настройки хранятся только на вашем устройстве."
                            )
                        }
                        items.forEach { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (language) {
                            AppLanguage.JAPANESE -> "プライバシーポリシー全文を開く"
                            AppLanguage.KOREAN -> "개인정보 처리방침 전문 보기"
                            AppLanguage.SIMPLIFIED_CHINESE -> "查看完整隐私政策"
                            AppLanguage.TRADITIONAL_CHINESE -> "查看完整隱私權政策"
                            AppLanguage.ENGLISH -> "Read Full Privacy Policy"
                            AppLanguage.SPANISH -> "Leer política de privacidad completa"
                            AppLanguage.FRENCH -> "Lire la politique de confidentialité complète"
                            AppLanguage.PORTUGUESE -> "Ler política de privacidade completa"
                            AppLanguage.RUSSIAN -> "Читать полную политику"
                        },
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.KOREAN -> "닫기"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                        AppLanguage.SPANISH -> "Cerrar"
                        AppLanguage.FRENCH -> "Fermer"
                        AppLanguage.PORTUGUESE -> "Fechar"
                        AppLanguage.RUSSIAN -> "Закрыть"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

private data class HapticPreviewItem(
    val pattern: ExpressiveHapticPattern,
    val labelJa: String,
    val labelKo: String,
    val labelEn: String,
    val labelZhHans: String,
    val labelZhHant: String,
    val labelEs: String,
    val labelFr: String,
    val labelPt: String,
    val labelRu: String,
    val icon: ImageVector
) {
    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.KOREAN -> labelKo
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.SPANISH -> labelEs
        AppLanguage.FRENCH -> labelFr
        AppLanguage.PORTUGUESE -> labelPt
        AppLanguage.RUSSIAN -> labelRu
    }
}

/**
 * 端末別ハプティクスプロファイル診断 & 全パターン体験ダイアログ
 */
@Composable
fun HapticShowcaseDialog(
    language: AppLanguage,
    currentIntensity: io.github.nemynew.expressiveweather.data.HapticIntensity,
    capabilities: HapticCapabilitiesSnapshot?,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val expressHaptics = LocalExpressiveHaptics.current

    val uiPatterns = remember {
        listOf(
            HapticPreviewItem(
                ExpressiveHapticPattern.EXPRESSIVE_BOUNCE,
                "弾性タップ", "탄성 탭", "Bouncy Tap", "弹性点击", "彈性點擊",
                "Toque elástico", "Tap rebond", "Toque elástico", "Упругий тап",
                Icons.Rounded.TouchApp
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.SWITCH_ON,
                "スイッチ ON", "스위치 전환", "Switch Snap", "开关切换", "開關切換",
                "Interruptor", "Interrupteur", "Interruptor", "Щелчок",
                Icons.Rounded.ToggleOn
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.DETENT_BOUNDARY,
                "端点デテント", "경계 디텐트", "Detent Edge", "边界刻度", "邊界刻度",
                "Tope de borde", "Cran de bord", "Limite tátil", "Граница",
                Icons.Rounded.LinearScale
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.DRAG_PICKUP,
                "ドラッグ掴み", "드래그 잡기", "Grab & Lift", "抓取抬起", "抓取抬起",
                "Agarrar", "Saisir", "Segurar", "Захват",
                Icons.Rounded.DragIndicator
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.REFRESH_SUCCESS,
                "更新完了", "업데이트 완료", "Sync Done", "同步完成", "同步完成",
                "Actualizado", "Synchronisé", "Atualizado", "Обновлено",
                Icons.Rounded.CheckCircle
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.CELEBRATE_UNLOCK,
                "達成・解除", "완료·잠금 해제", "Celebrate", "解锁庆祝", "解鎖慶祝",
                "Celebración", "Célébration", "Celebração", "Успех",
                Icons.Rounded.AutoAwesome
            )
        )
    }

    val weatherPatterns = remember {
        listOf(
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_RAINDROP_PATTER,
                "雨粒パター", "빗방울 톡톡", "Raindrop Patter", "雨滴轻敲", "雨滴輕敲",
                "Gotas de lluvia", "Gouttes de pluie", "Gotas de chuva", "Капли дождя",
                Icons.Rounded.WaterDrop
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE,
                "雷鳴ランブル", "천둥 울림", "Thunder Rumble", "雷鸣轰隆", "雷鳴轟隆",
                "Trueno", "Tonnerre", "Trovão", "Раскат грома",
                Icons.Rounded.Thunderstorm
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_SNOW_CRISP,
                "粉雪クリスプ", "눈송이 터치", "Crisp Snow", "降雪轻触", "降雪輕觸",
                "Nieve suave", "Neige légère", "Neve suave", "Хруст снега",
                Icons.Rounded.AcUnit
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_WIND_GUST,
                "強風ガスト", "돌풍 진동", "Wind Gust", "阵风拂动", "陣風拂動",
                "Ráfaga", "Rafale", "Rajada", "Порыв ветра",
                Icons.Rounded.Air
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.ALERT_CRITICAL_PULSE,
                "緊急警報パルス", "긴급 경보 펄스", "Critical Alert", "紧急警报脉冲", "緊急警報脈衝",
                "Alerta crítica", "Alerte critique", "Alerta crítico", "Тревога",
                Icons.Rounded.Warning
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Vibration,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "ハプティクス診断 & 体験"
                        AppLanguage.KOREAN -> "햅틱 진단 및 체험"
                        AppLanguage.SIMPLIFIED_CHINESE -> "触感诊断与体验"
                        AppLanguage.TRADITIONAL_CHINESE -> "觸感診斷與體驗"
                        AppLanguage.ENGLISH -> "Haptics Studio & Profile"
                        AppLanguage.SPANISH -> "Estudio y perfil háptico"
                        AppLanguage.FRENCH -> "Studio et profil haptique"
                        AppLanguage.PORTUGUESE -> "Estúdio e perfil tátil"
                        AppLanguage.RUSSIAN -> "Студия и профиль виброотклика"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val profileLabel = capabilities?.profile?.getLabel(language)
                            ?: HapticDeviceProfile.LEGACY_TIMED_ERM.getLabel(language)
                        Text(
                            text = profileLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        if (capabilities != null) {
                            Text(
                                text = "${capabilities.manufacturer} ${capabilities.model} • API ${capabilities.sdkInt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            val yes = when (language) {
                                AppLanguage.JAPANESE -> "対応"
                                AppLanguage.KOREAN -> "지원"
                                AppLanguage.SIMPLIFIED_CHINESE -> "支持"
                                AppLanguage.TRADITIONAL_CHINESE -> "支援"
                                AppLanguage.ENGLISH -> "Yes"
                                AppLanguage.SPANISH -> "Sí"
                                AppLanguage.FRENCH -> "Oui"
                                AppLanguage.PORTUGUESE -> "Sim"
                                AppLanguage.RUSSIAN -> "Да"
                            }
                            val no = when (language) {
                                AppLanguage.JAPANESE -> "非対応"
                                AppLanguage.KOREAN -> "미지원"
                                AppLanguage.SIMPLIFIED_CHINESE -> "不支持"
                                AppLanguage.TRADITIONAL_CHINESE -> "不支援"
                                AppLanguage.ENGLISH -> "No"
                                AppLanguage.SPANISH -> "No"
                                AppLanguage.FRENCH -> "Non"
                                AppLanguage.PORTUGUESE -> "Não"
                                AppLanguage.RUSSIAN -> "Нет"
                            }
                            Text(
                                text = when (language) {
                                    AppLanguage.JAPANESE -> "振幅制御: ${if (capabilities.hasAmplitudeControl) yes else no} / プリミティブ数: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.KOREAN -> "진폭 제어: ${if (capabilities.hasAmplitudeControl) yes else no} / 프리미티브 수: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "振幅控制: ${if (capabilities.hasAmplitudeControl) yes else no} / 原语数: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.TRADITIONAL_CHINESE -> "振幅控制: ${if (capabilities.hasAmplitudeControl) yes else no} / 原語數: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.ENGLISH -> "Amplitude: ${if (capabilities.hasAmplitudeControl) yes else no} • Primitives: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.SPANISH -> "Amplitud: ${if (capabilities.hasAmplitudeControl) yes else no} • Primitivas: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.FRENCH -> "Amplitude : ${if (capabilities.hasAmplitudeControl) yes else no} • Primitives : ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.PORTUGUESE -> "Amplitude: ${if (capabilities.hasAmplitudeControl) yes else no} • Primitivas: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.RUSSIAN -> "Амплитуда: ${if (capabilities.hasAmplitudeControl) yes else no} • Примитивы: ${capabilities.supportedPrimitivesCount}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "現在の強度: ${currentIntensity.getLabel(language)}"
                                AppLanguage.KOREAN -> "현재 강도: ${currentIntensity.getLabel(language)}"
                                AppLanguage.SIMPLIFIED_CHINESE -> "当前强度: ${currentIntensity.getLabel(language)}"
                                AppLanguage.TRADITIONAL_CHINESE -> "目前強度: ${currentIntensity.getLabel(language)}"
                                AppLanguage.ENGLISH -> "Active Intensity: ${currentIntensity.getLabel(language)}"
                                AppLanguage.SPANISH -> "Intensidad activa: ${currentIntensity.getLabel(language)}"
                                AppLanguage.FRENCH -> "Intensité active : ${currentIntensity.getLabel(language)}"
                                AppLanguage.PORTUGUESE -> "Intensidade ativa: ${currentIntensity.getLabel(language)}"
                                AppLanguage.RUSSIAN -> "Текущая сила: ${currentIntensity.getLabel(language)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (currentIntensity == io.github.nemynew.expressiveweather.data.HapticIntensity.OFF) {
                                colorScheme.error
                            } else {
                                colorScheme.onSurface
                            }
                        )
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "UI インタラクション"
                        AppLanguage.KOREAN -> "UI 인터랙션"
                        AppLanguage.SIMPLIFIED_CHINESE -> "UI 交互触感"
                        AppLanguage.TRADITIONAL_CHINESE -> "UI 互動觸感"
                        AppLanguage.ENGLISH -> "UI Interactions"
                        AppLanguage.SPANISH -> "Interacciones de interfaz"
                        AppLanguage.FRENCH -> "Interactions de l'interface"
                        AppLanguage.PORTUGUESE -> "Interações de interface"
                        AppLanguage.RUSSIAN -> "Взаимодействия интерфейса"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )

                uiPatterns.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            FilledTonalButton(
                                onClick = { expressHaptics.play(item.pattern, bypassThrottle = true) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label(language),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "天候・防災シグネチャ"
                        AppLanguage.KOREAN -> "날씨·재난 경보 시그니처"
                        AppLanguage.SIMPLIFIED_CHINESE -> "天气与灾害警报特征"
                        AppLanguage.TRADITIONAL_CHINESE -> "天氣與災害警報特徵"
                        AppLanguage.ENGLISH -> "Weather & Alert Signatures"
                        AppLanguage.SPANISH -> "Firmas de clima y alertas"
                        AppLanguage.FRENCH -> "Signatures météo et alertes"
                        AppLanguage.PORTUGUESE -> "Assinaturas de clima e alertas"
                        AppLanguage.RUSSIAN -> "Погода и оповещения"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )

                weatherPatterns.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            OutlinedButton(
                                onClick = { expressHaptics.play(item.pattern, bypassThrottle = true) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (item.pattern == ExpressiveHapticPattern.ALERT_CRITICAL_PULSE) {
                                        colorScheme.error
                                    } else {
                                        colorScheme.primary
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label(language),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.KOREAN -> "닫기"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                        AppLanguage.SPANISH -> "Cerrar"
                        AppLanguage.FRENCH -> "Fermer"
                        AppLanguage.PORTUGUESE -> "Fechar"
                        AppLanguage.RUSSIAN -> "Закрыть"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}


