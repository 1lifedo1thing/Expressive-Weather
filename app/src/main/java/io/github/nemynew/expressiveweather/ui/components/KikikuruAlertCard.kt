package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AlertLevel
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.KikikuruDisasterDetail
import io.github.nemynew.expressiveweather.data.LocalAppLanguage

/**
 * 気象庁キキクル警戒レベルカード コンポーザブル
 *
 * 要件:
 * 1. MaterialShapeの12SidedShape（スカロップ形状）を左側に配置し、中央に警戒レベルの数字を太字で大きく表示
 * 2. 右側にタイトル（「警戒レベルX」太字）と説明文（危険度の状況説明テキスト）を縦並びで配置
 * 3. カード全体の背景色と文字色は、カラーパレット定義に従って動的に切り替え
 *
 * @param level 警戒レベル（2〜5）
 * @param title タイトル（デフォルト: 「警戒レベル$level」）
 * @param description 危険度の状況説明テキスト
 * @param modifier Modifier
 * @param badgeShape スカロップ形状（デフォルト: [TwelveSidedScallopShape]）
 * @param buttonText ボタン文言（デフォルト: 「キキクルマップを確認」、nullで非表示）
 * @param onButtonClick ボタンタップ時のコールバック
 * @param onClick カードタップ時のコールバック（任意）
 */
@Composable
fun KikikuruAlertCard(
    level: Int,
    title: String = "警戒レベル$level",
    description: String,
    modifier: Modifier = Modifier,
    badgeShape: Shape = TwelveSidedScallopShape,
    buttonText: String? = "キキクルマップを確認",
    onButtonClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = WeatherAlertColors.forKikikuruLevel(level)

    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBg,
            contentColor = colors.cardText
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 12SidedShape (スカロップ型) バッジ + 警戒レベル数字
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(badgeShape)
                    .background(colors.badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.badgeText,
                    lineHeight = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 2. 右側: タイトル & 状況説明テキスト & アクションボタン
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.cardText,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.5.sp,
                    color = colors.cardText
                )

                // 3. キキクルUI専用アクションボタン（画像準拠のカプセルピルボタン）
                if (!buttonText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = CircleShape,
                        color = colors.buttonBg,
                        contentColor = colors.buttonText,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onButtonClick?.invoke() ?: onClick?.invoke() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = colors.buttonText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = buttonText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.buttonText
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = colors.buttonText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * [KikikuruDisasterDetail] データモデルを受け取る多言語完全対応オーバーロード
 */
@Composable
fun KikikuruAlertCard(
    detail: KikikuruDisasterDetail,
    modifier: Modifier = Modifier,
    cityName: String? = null,
    language: AppLanguage = LocalAppLanguage.current,
    badgeShape: Shape = TwelveSidedScallopShape,
    onButtonClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val level = detail.alertLevel
    val title = detail.getTitle(language) + if (!cityName.isNullOrBlank()) " ($cityName)" else ""
    val description = detail.getActionAdvice(language)
    val buttonText = detail.getButtonText(language)

    KikikuruAlertCard(
        level = level.level,
        title = title,
        description = description,
        modifier = modifier,
        badgeShape = badgeShape,
        buttonText = buttonText,
        onButtonClick = onButtonClick,
        onClick = onClick
    )
}

/**
 * [AlertLevel] データモデルを直接受け取るオーバーロード
 */
@Composable
fun KikikuruAlertCard(
    alertLevel: AlertLevel,
    modifier: Modifier = Modifier,
    cityName: String? = null,
    language: AppLanguage = LocalAppLanguage.current,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val levelNum = alertLevel.level
    val levelTitle = alertLevel.getTitle(language)
    val title = if (cityName.isNullOrBlank()) {
        when (language) {
            AppLanguage.ENGLISH -> "Alert Level $levelNum $levelTitle"
            AppLanguage.KOREAN -> "경계 단계 $levelNum $levelTitle"
            AppLanguage.SIMPLIFIED_CHINESE -> "警戒等级$levelNum $levelTitle"
            AppLanguage.TRADITIONAL_CHINESE -> "警戒等級$levelNum $levelTitle"
            AppLanguage.SPANISH -> "Nivel de alerta $levelNum $levelTitle"
            AppLanguage.FRENCH -> "Niveau d'alerte $levelNum $levelTitle"
            AppLanguage.PORTUGUESE -> "Nível de alerta $levelNum $levelTitle"
            AppLanguage.RUSSIAN -> "Уровень угрозы $levelNum $levelTitle"
            AppLanguage.JAPANESE -> "警戒レベル$levelNum $levelTitle"
        }
    } else {
        when (language) {
            AppLanguage.ENGLISH -> "Alert Level $levelNum $levelTitle ($cityName)"
            AppLanguage.KOREAN -> "경계 단계 $levelNum $levelTitle ($cityName)"
            AppLanguage.SIMPLIFIED_CHINESE -> "警戒等级$levelNum $levelTitle ($cityName)"
            AppLanguage.TRADITIONAL_CHINESE -> "警戒等級$levelNum $levelTitle ($cityName)"
            AppLanguage.SPANISH -> "Nivel de alerta $levelNum $levelTitle ($cityName)"
            AppLanguage.FRENCH -> "Niveau d'alerte $levelNum $levelTitle ($cityName)"
            AppLanguage.PORTUGUESE -> "Nível de alerta $levelNum $levelTitle ($cityName)"
            AppLanguage.RUSSIAN -> "Уровень угрозы $levelNum $levelTitle ($cityName)"
            AppLanguage.JAPANESE -> "警戒レベル$levelNum $levelTitle（$cityName）"
        }
    }

    val resolvedButtonText = buttonText ?: when (language) {
        AppLanguage.ENGLISH -> "Check Kikikuru Map"
        AppLanguage.KOREAN -> "키키쿠루 지도 확인"
        AppLanguage.SIMPLIFIED_CHINESE -> "查看危险度分布图"
        AppLanguage.TRADITIONAL_CHINESE -> "查看危險度分佈圖"
        AppLanguage.SPANISH -> "Ver mapa de riesgo Kikikuru"
        AppLanguage.FRENCH -> "Voir la carte Kikikuru"
        AppLanguage.PORTUGUESE -> "Ver mapa de risco Kikikuru"
        AppLanguage.RUSSIAN -> "Карта рисков Kikikuru"
        AppLanguage.JAPANESE -> "キキクルマップを確認"
    }

    KikikuruAlertCard(
        level = alertLevel.level,
        title = title,
        description = alertLevel.actionAdvice,
        modifier = modifier,
        buttonText = resolvedButtonText,
        onButtonClick = onButtonClick,
        onClick = onClick
    )
}
