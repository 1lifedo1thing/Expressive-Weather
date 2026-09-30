package com.example.expressiveweather.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.data.AlertLevel
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.DisasterType
import com.example.expressiveweather.data.KikikuruDisasterDetail
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.MunicipalityAlertInfo

/**
 * ホーム画面最上部に固定表示される、警戒レベルおよびキキクル警戒情報バナー
 *
 * デバッグモードカード（KikikuruAlertCard）のデザインに完全準拠:
 * 1. 左側: 12SidedShape (スカロップ形状) バッジ内に警戒レベル数字を太字表示
 * 2. 右側: タイトル＋避難情報バッジ、気象庁JSON見出し文（headline）＋行動アドバイス
 * 3. 下部: カプセルピル型キキクルマップ確認ボタン
 * 4. 上部: 大雨・土砂・浸水・洪水セレクターチップ
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KikikuruWarningBanner(
    alertInfo: MunicipalityAlertInfo?,
    onNavigateToRadar: (targetLayerId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val info = alertInfo ?: return
    val language = LocalAppLanguage.current
    val overallLevel = info.alertLevel

    if (overallLevel.level < 2) {
        // レベル1以下（平常時〜早期注意）: 控えめな1行テキストカード
        if (overallLevel == AlertLevel.LEVEL_1) {
            val primaryDetail = info.targetDisasterPrimary?.let { info.getDetailFor(it) } ?: info.getDetailFor(DisasterType.HEAVY_RAIN)
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = Color(0xFFE1F5FE).copy(alpha = 0.92f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color(0xFF0288D1),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "【${overallLevel.getTitle(language)}】${info.municipalityName}: ${primaryDetail.getActionAdvice(language)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF01579B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    // 4大キキクル (大雨・土砂・浸水・洪水) の初期選択
    // 最も警戒レベルが高い災害種別、または primaryType を初期選択
    val initialType = remember(info.municipalityCode, overallLevel) {
        info.targetDisasterPrimary ?: info.allDisasterDetails.maxByOrNull { it.alertLevel.level }?.disasterType ?: DisasterType.HEAVY_RAIN
    }
    var selectedType by remember(info.municipalityCode, overallLevel) {
        mutableStateOf(initialType)
    }

    val selectedDetail = info.getDetailFor(selectedType)
    val currentLevel = selectedDetail.alertLevel

    // レベルに応じたカードカラー設定 (KikikuruAlertCardと同一)
    val alertColors = WeatherAlertColors.forKikikuruLevel(currentLevel)
    val cardBgColor = alertColors.cardBg
    val contentColor = alertColors.cardText

    // レベル5（切迫）またはレベル4（危険）の場合はボーダー脈動アニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderModifier = if (currentLevel == AlertLevel.LEVEL_5) {
        Modifier.border(2.5.dp, Color(0xFFFFD700).copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
    } else if (currentLevel == AlertLevel.LEVEL_4) {
        Modifier.border(2.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // 1. キキクル種別セレクタータブ [大雨] [土砂] [浸水] [洪水]
            // ==========================================
            val kikiTypes = listOf(
                DisasterType.HEAVY_RAIN,
                DisasterType.LANDSLIDE,
                DisasterType.INUNDATION,
                DisasterType.FLOOD
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                kikiTypes.forEach { dt ->
                    val dtDetail = info.getDetailFor(dt)
                    val isSelected = dt == selectedType
                    val dtLevel = dtDetail.alertLevel
                    val dtLevelColors = WeatherAlertColors.forKikikuruLevel(dtLevel)

                    val chipBg = if (isSelected) {
                        alertColors.buttonBg
                    } else {
                        Color.Black.copy(alpha = 0.08f)
                    }
                    val chipContentColor = if (isSelected) {
                        alertColors.buttonText
                    } else {
                        contentColor
                    }

                    Surface(
                        shape = CircleShape,
                        color = chipBg,
                        contentColor = chipContentColor,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedType = dt }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            // レベルミニバッジ (円の中央に数字が完全に収まるよう配置)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(dtLevelColors.badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dtLevel.level}",
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = dtLevelColors.badgeText,
                                    textAlign = TextAlign.Center,
                                    style = LocalTextStyle.current.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Center,
                                            trim = LineHeightStyle.Trim.Both
                                        )
                                    )
                                )
                            }

                            Text(
                                text = dt.getShortLabel(language),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. メインの警報カード本体 (KikikuruAlertCard のデザインに完全準拠)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 12SidedShape (スカロップ型) バッジ + 警戒レベル数字 (56.dp)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(TwelveSidedScallopShape)
                        .background(alertColors.badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentLevel.level}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = alertColors.badgeText,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 右側: タイトル & 見出し文＋行動アドバイス & アクションボタン
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // タイトル行 + 避難情報バッジ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedDetail.getTitle(language),
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        val evacInfo = selectedDetail.getEvacuationInfo(language)
                        if (evacInfo != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = alertColors.badgeBg,
                                contentColor = alertColors.badgeText
                            ) {
                                Text(
                                    text = "【$evacInfo】",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 本文: 気象庁JSON見出し文 (headlineText) + 住民等の行動アドバイス (actionAdvice)
                    val headline = selectedDetail.headlineText ?: info.headlineText
                    val actionAdvice = selectedDetail.getActionAdvice(language)
                    val fullDescription = if (!headline.isNullOrBlank()) {
                        "$headline\n\n$actionAdvice"
                    } else {
                        actionAdvice
                    }

                    Text(
                        text = fullDescription,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.5.sp,
                        color = contentColor
                    )

                    // 発表警報・注意報チップ一覧 (あれば小さく表示)
                    val warningsToShow = if (selectedDetail.activeWarnings.isNotEmpty()) {
                        selectedDetail.activeWarnings
                    } else {
                        info.activeWarnings
                    }
                    if (warningsToShow.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            warningsToShow.forEach { warning ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.12f),
                                    contentColor = contentColor
                                ) {
                                    Text(
                                        text = "${warning.name} (${warning.status})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. キキクルUI専用アクションボタン（画像準拠のカプセルピルボタン）
                    val buttonText = selectedDetail.getButtonText(language)
                    val targetLayer = selectedDetail.disasterType.radarLayerId

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = CircleShape,
                        color = alertColors.buttonBg,
                        contentColor = alertColors.buttonText,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onNavigateToRadar(targetLayer) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = alertColors.buttonText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = buttonText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = alertColors.buttonText
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = alertColors.buttonText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
