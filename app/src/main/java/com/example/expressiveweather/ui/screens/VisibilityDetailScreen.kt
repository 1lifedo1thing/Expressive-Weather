package com.example.expressiveweather.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.theme.GoogleSansFlex
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.ui.components.DetailMetricType
import com.example.expressiveweather.ui.components.DetailScaffold

/**
 * 視程（Visibility）詳細画面 - Material 3 Expressive 準拠、多言語対応、枠線フリー
 */
@Composable
fun VisibilityDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = language.strings
    val visibilityKm = weather?.visibilityKm ?: 10.0

    val (visLabel, visColor, visDesc) = getVisibilityInfo(visibilityKm, language)

    DetailScaffold(
        title = strings.visibility,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.VISIBILITY,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        // ================= 1. ヒーローセクション: 同心円スコープ & 視程距離 =================
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = visColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Horizontal Visibility"
                            AppLanguage.SIMPLIFIED_CHINESE -> "水平能见度"
                            AppLanguage.TRADITIONAL_CHINESE -> "水平能見度"
                            else -> "水平視程 (見通し距離)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // レーダースコープ風の同心円キャンバス
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxR = size.width / 2 - 6.dp.toPx()

                        // 同心円グリッド (2km, 5km, 10km)
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.2f),
                            radius = maxR,
                            center = center,
                            style = Stroke(1.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.25f),
                            radius = maxR * 0.65f,
                            center = center,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        )
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.3f),
                            radius = maxR * 0.35f,
                            center = center,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                        )

                        // 十字ターゲットライン
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 視界領域の塗り
                        val reachRatio = (visibilityKm / 10.0).coerceIn(0.1, 1.0).toFloat()
                        drawCircle(
                            color = visColor.copy(alpha = 0.18f),
                            radius = maxR * reachRatio,
                            center = center
                        )
                        drawCircle(
                            color = visColor,
                            radius = maxR * reachRatio,
                            center = center,
                            style = Stroke(2.dp.toPx())
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (visibilityKm >= 10.0) "10+" else String.format("%.1f", visibilityKm),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "km",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // M3 Expressive: 枠線なしトナルサーフェス
                Surface(
                    shape = CircleShape,
                    color = visColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = visLabel,
                        color = visColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = visDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 国際航空気象（ICAO）視程段階ガイド =================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = when (language) {
                        AppLanguage.ENGLISH -> "International Visibility Standards (ICAO)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "国际航空气象 (ICAO) 能见度分级"
                        AppLanguage.TRADITIONAL_CHINESE -> "國際航空氣象 (ICAO) 能見度分級"
                        else -> "国際航空気象（ICAO）の視程階級"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(14.dp))

                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH -> "≥ 10 km"
                        AppLanguage.SIMPLIFIED_CHINESE -> "10 公里以上"
                        AppLanguage.TRADITIONAL_CHINESE -> "10 公里以上"
                        else -> "10 km 以上"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Excellent (Clear)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "极佳 (通透清晰)"
                        AppLanguage.TRADITIONAL_CHINESE -> "極佳 (通透清晰)"
                        else -> "極めて良好 (クリア)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Distant landmarks, horizons, and terrain outlines are crystal clear."
                        AppLanguage.SIMPLIFIED_CHINESE -> "远景地平线与地标轮廓分明，视界极为开阔清晰。"
                        AppLanguage.TRADITIONAL_CHINESE -> "遠景地平線與地標輪廓分明，視野極為開闊清晰。"
                        else -> "遠方の山々や建造物まで輪郭がくっきりと見渡せます。"
                    },
                    isActive = visibilityKm >= 10.0
                )
                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH -> "4 – 10 km"
                        AppLanguage.SIMPLIFIED_CHINESE -> "4 ～ 10 公里"
                        AppLanguage.TRADITIONAL_CHINESE -> "4 ～ 10 公里"
                        else -> "4 〜 10 km"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Good (Normal)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "良好 (正常)"
                        AppLanguage.TRADITIONAL_CHINESE -> "良好 (正常)"
                        else -> "良好 (通常)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Standard visibility; road travel and daily outdoor navigation are fully uninhibited."
                        AppLanguage.SIMPLIFIED_CHINESE -> "标准常规视界，对日常出行、通勤与道路驾驶毫无妨碍。"
                        AppLanguage.TRADITIONAL_CHINESE -> "標準常規視野，對日常出行、通勤與道路駕駛毫無妨礙。"
                        else -> "日常的な移動や運転に支障のない標準的な視界です。"
                    },
                    isActive = visibilityKm in 4.0..9.99
                )
                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH -> "1 – 4 km"
                        AppLanguage.SIMPLIFIED_CHINESE -> "1 ～ 4 公里"
                        AppLanguage.TRADITIONAL_CHINESE -> "1 ～ 4 公里"
                        else -> "1 〜 4 km"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Moderate (Haze / Rain)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "轻度受限 (微雾/降雨)"
                        AppLanguage.TRADITIONAL_CHINESE -> "輕度受限 (微霧/降雨)"
                        else -> "やや不良 (もや・降水)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Distant scenery partially veiled by light mist, airborne particles, or rainfall."
                        AppLanguage.SIMPLIFIED_CHINESE -> "受轻雾、微尘或降雨影响，远距离视界稍显灰白朦胧。"
                        AppLanguage.TRADITIONAL_CHINESE -> "受輕霧、微粒或降雨影響，遠距離視野稍顯灰白朦朧。"
                        else -> "雨やもや、微小粒子等により遠景がかすんで見えます。"
                    },
                    isActive = visibilityKm in 1.0..3.99
                )
                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH -> "< 1 km"
                        AppLanguage.SIMPLIFIED_CHINESE -> "1 公里以下"
                        AppLanguage.TRADITIONAL_CHINESE -> "1 公里以下"
                        else -> "1 km 未満"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Poor (Fog / Heavy Rain)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "视界不良 (大雾预警)"
                        AppLanguage.TRADITIONAL_CHINESE -> "視界不良 (大霧預警)"
                        else -> "視界不良 (霧・濃霧)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Severely restricted forward view. Reduce speed and use low-beam headlights."
                        AppLanguage.SIMPLIFIED_CHINESE -> "前方可视距离受限。驾车外出务必减速慢行并开启近光灯或雾灯。"
                        AppLanguage.TRADITIONAL_CHINESE -> "前方可視距離受限。行車外出務必減速慢行並開啟近光燈或霧燈。"
                        else -> "前方見通しが悪化。車の運転時はライト点灯と減速が必要です。"
                    },
                    isActive = visibilityKm < 1.0
                )
            }
        }

        // ================= 3. 移動・活動への影響 =================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Landscape,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Travel & Outdoor Activities"
                            AppLanguage.SIMPLIFIED_CHINESE -> "出行与户外活动建议"
                            AppLanguage.TRADITIONAL_CHINESE -> "出行與戶外活動建議"
                            else -> "移動・活動への影響"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                VisAdviceRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Driving Safety"
                        AppLanguage.SIMPLIFIED_CHINESE -> "车辆驾驶与交通"
                        AppLanguage.TRADITIONAL_CHINESE -> "車輛駕駛與交通"
                        else -> "自動車の運転"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> if (visibilityKm >= 5.0)
                            "Road visibility is clear; drive with confidence under standard conditions."
                        else
                            "Reduced view: maintain generous following distance and illuminate headlights."
                        AppLanguage.SIMPLIFIED_CHINESE -> if (visibilityKm >= 5.0)
                            "视线良好开阔，适合安心驾驶行驶。"
                        else
                            "视程偏低，请加大车距并保持开启近光车灯，谨慎行驶。"
                        AppLanguage.TRADITIONAL_CHINESE -> if (visibilityKm >= 5.0)
                            "視野開闊，適合安心駕駛行駛。"
                        else
                            "視程偏低，請拉大車距並保持開啟近光車燈，謹慎行駛。"
                        else -> if (visibilityKm >= 5.0)
                            "視界は良好です。通常通り安心して走行いただけます。"
                        else
                            "車間距離を十分にとり、早めのヘッドライト点灯を心がけてください。"
                    }
                )
                VisAdviceRow(
                    icon = Icons.Rounded.RemoveRedEye,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Scenic & Horizon Views"
                        AppLanguage.SIMPLIFIED_CHINESE -> "景观与高台远眺"
                        AppLanguage.TRADITIONAL_CHINESE -> "景觀與高台遠眺"
                        else -> "展望・風景の眺望"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> if (visibilityKm >= 10.0)
                            "Superb conditions for panoramic viewpoints, towers, and mountain lookouts."
                        else
                            "Distant horizons may be obscured or faded by atmospheric moisture."
                        AppLanguage.SIMPLIFIED_CHINESE -> if (visibilityKm >= 10.0)
                            "绝佳的观景远眺天气，高台与观景台视野辽阔舒展。"
                        else
                            "远景可能因水汽或微尘而呈现灰白朦胧。"
                        AppLanguage.TRADITIONAL_CHINESE -> if (visibilityKm >= 10.0)
                            "絕佳的觀景遠眺天氣，高處與觀景台視野遼闊舒展。"
                        else
                            "遠景可能因水氣或懸浮微粒而呈現灰白朦朧。"
                        else -> if (visibilityKm >= 10.0)
                            "絶好の見晴らし日和です。展望台や高台からのパノラマビューを楽しめます。"
                        else
                            "遠くの景色が白っぽくかすむ可能性があります。"
                    }
                )
            }
        }
    }
}

@Composable
private fun VisibilityGradeRow(distance: String, label: String, desc: String, isActive: Boolean) {
    // M3 Expressive: 枠線は完全禁止、アクティブ時はトナルサーフェスで表現
    val bg = if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = distance,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun VisAdviceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getVisibilityInfo(km: Double, language: AppLanguage): Triple<String, Color, String> {
    return when {
        km >= 10.0 -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Crystal Clear"
                AppLanguage.SIMPLIFIED_CHINESE -> "视野极佳"
                AppLanguage.TRADITIONAL_CHINESE -> "視野極佳"
                else -> "見通し極めて良好"
            },
            Color(0xFF4CAF50),
            when (language) {
                AppLanguage.ENGLISH -> "Completely unobstructed visibility extending to the farthest horizon."
                AppLanguage.SIMPLIFIED_CHINESE -> "极度通透澄澈的视界，视野毫无遮挡直至地平线。"
                AppLanguage.TRADITIONAL_CHINESE -> "極度通透澄澈的視野，毫無遮擋直至地平線。"
                else -> "遠くの地平線まで遮るもののない、澄み切ったクリアな視界が広がっています。"
            }
        )
        km >= 4.0 -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Good Visibility"
                AppLanguage.SIMPLIFIED_CHINESE -> "能见度良好"
                AppLanguage.TRADITIONAL_CHINESE -> "能見度良好"
                else -> "良好"
            },
            Color(0xFF03A9F4),
            when (language) {
                AppLanguage.ENGLISH -> "Standard clear visibility. Slight haze may appear at great distances but causes no obstruction."
                AppLanguage.SIMPLIFIED_CHINESE -> "常规良好视界。远处虽有轻微泛白，但不影响任何户外活动。"
                AppLanguage.TRADITIONAL_CHINESE -> "常規良好視野。遠處雖有輕微泛白，但不影響任何戶外活動。"
                else -> "一般的な良好な視界です。遠景にわずかな霞が見られますが移動に支障ありません。"
            }
        )
        km >= 1.0 -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Moderate Haze"
                AppLanguage.SIMPLIFIED_CHINESE -> "轻度朦胧"
                AppLanguage.TRADITIONAL_CHINESE -> "輕度朦朧"
                else -> "ややかすむ"
            },
            Color(0xFFFF9800),
            when (language) {
                AppLanguage.ENGLISH -> "Visibility somewhat decreased due to mist, humidity, or precipitation."
                AppLanguage.SIMPLIFIED_CHINESE -> "受轻雾、微粒或降雨影响，中远距离视程有所下降。"
                AppLanguage.TRADITIONAL_CHINESE -> "受輕霧、懸浮微粒或降雨影響，中遠距離視程有所下降。"
                else -> "もやや微小粒子、または降雨により見通しがやや低下しています。"
            }
        )
        else -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Poor Visibility (Fog Alert)"
                AppLanguage.SIMPLIFIED_CHINESE -> "视界不良 (大雾注意)"
                AppLanguage.TRADITIONAL_CHINESE -> "視界不良 (大霧注意)"
                else -> "視界不良 (霧注意)"
            },
            Color(0xFFE53935),
            when (language) {
                AppLanguage.ENGLISH -> "Heavy fog or intense rain limits forward view to under 1 km. Exercise extreme caution."
                AppLanguage.SIMPLIFIED_CHINESE -> "浓雾或暴雨导致1公里外难以辨析，出行需极度谨慎。"
                AppLanguage.TRADITIONAL_CHINESE -> "濃霧或暴雨導致1公里外難以辨認，出行需極度謹慎。"
                else -> "霧や大雨等により1km先が見えない状況です。通行には細心の注意が必要です。"
            }
        )
    }
}

