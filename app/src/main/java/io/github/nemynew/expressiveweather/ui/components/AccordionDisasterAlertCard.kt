package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 集約防災警報アコーディオンカード
 *
 * 要件:
 * - 警報・注意報カード本来の背景色（cardBg）と文字色（contentColor）を完全に踏襲
 * - 不要な枠線（border）や入れ子の枠は一切排除
 * - まとめや発表概要などの余計な情報は一切含めず、純粋に警報カード一覧をインライン展開
 */
@Composable
fun AccordionDisasterAlertCard(
    type: WeatherWarningType,
    title: String,
    description: String? = null,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = WeatherAlertColors.forWarningType(type)

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "AccordionArrowRotation"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 集約ヘッダーカード（タップで開閉）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onToggleExpand),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.cardBg,
                contentColor = colors.contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = colors.contentColor,
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = title,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.contentColor,
                            lineHeight = 22.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "折りたたむ" else "展開する",
                        tint = colors.contentColor,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(arrowRotation)
                    )
                }

                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.5.sp,
                        color = colors.contentColor
                    )
                }
            }
        }

        // 2. 展開時の警報カード一覧（余計な枠を持たず直下に並ぶ）
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}
