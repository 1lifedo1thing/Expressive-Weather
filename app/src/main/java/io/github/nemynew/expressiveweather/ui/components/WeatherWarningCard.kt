package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 気象警報・注意報カード コンポーザブル
 *
 * 要件:
 * 1. バッジなしのシンプルな横長角丸カード
 * 2. 左上に警告アイコン（三角に感嘆符）と警報・注意報のタイトルを横並びで配置
 * 3. その下部に状況説明テキストを配置
 * 4. 日本国内の警報は紫系、国外の警報は赤系、注意報は黄系を適用できるように引数で種別 [WeatherWarningType] を受け取る設計
 *
 * @param type 警報・注意報の種別 ([WeatherWarningType.DOMESTIC_WARNING], [WeatherWarningType.FOREIGN_WARNING], [WeatherWarningType.ADVISORY])
 * @param title 警報・注意報のタイトル（例: 「大雨警報・洪水警報」「気象警報発表中」など）
 * @param description 状況説明テキスト（例: 「土砂災害や低い土地の浸水に警戒してください」など）
 * @param modifier Modifier
 * @param onClick カードタップ時のコールバック（任意）
 */
@Composable
fun WeatherWarningCard(
    type: WeatherWarningType,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = WeatherAlertColors.forWarningType(type)

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
            contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // 1. 左上: 警告アイコン（三角に感嘆符） + タイトル
            Row(
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

            Spacer(modifier = Modifier.height(6.dp))

            // 2. 下部: 状況説明テキスト
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
