package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android 15 / Material 3 Expressive 設定画面のグループ内におけるカード位置
 */
enum class ConnectedCardPosition {
    Single,
    Top,
    Middle,
    Bottom;

    fun toShape(): RoundedCornerShape = when (this) {
        Single -> RoundedCornerShape(28.dp)
        Top -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        Middle -> RoundedCornerShape(4.dp)
        Bottom -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
    }
}

/**
 * Android 15 設定UI仕様のカードグループコンテナ
 * カード間には 3.dp の微小ギャップを配置して接続カード（Connected Cards）を表現
 */
@Composable
fun Android15CardGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        content()
    }
}

/**
 * Android 15 設定UI仕様の個別セッティングカード
 * 円形カラフルアイコン + タイトル + サブタイトル + オプションのトグル/操作UI
 */
@Composable
fun Android15SettingsCard(
    title: String,
    subtitle: String? = null,
    leadingContent: @Composable () -> Unit,
    position: ConnectedCardPosition = ConnectedCardPosition.Single,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = position.toShape()
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent()

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

/**
 * Android 15 設定UI仕様の個別セッティングカード
 * 円形カラフルアイコン + タイトル + サブタイトル + オプションのトグル/操作UI
 */
@Composable
fun Android15SettingsCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTintColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    position: ConnectedCardPosition = ConnectedCardPosition.Single,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Android15SettingsCard(
        title = title,
        subtitle = subtitle,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        position = position,
        trailing = trailing,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun ExpressiveValueBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}
