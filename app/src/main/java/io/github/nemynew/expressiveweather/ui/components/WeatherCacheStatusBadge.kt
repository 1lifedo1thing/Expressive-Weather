package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.ui.util.rememberRelativeTime

/**
 * キャッシュデータまたは古いデータ（15分以上前）表示時に上部に表示されるステータスピルバッジ。
 * タップまたはPull-to-Refreshで最新データを再取得できます。
 */
@Composable
fun WeatherCacheStatusBadge(
    fetchedAtMillis: Long,
    isFromCache: Boolean,
    isStale: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val relativeTime = rememberRelativeTime(fetchedAtMillis, strings)

    // 15分（900秒）以上経過しているか、明示的にキャッシュ・staleの場合に表示
    val now = System.currentTimeMillis()
    val isOldData = (now - fetchedAtMillis) > 15 * 60 * 1000L
    val isVisible = isFromCache || isStale || isOldData

    val infiniteTransition = rememberInfiniteTransition(label = "badgeRefreshRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "badgeRefreshRotation"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isStale) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (isStale) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRefresh
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isFromCache) Icons.Rounded.CloudQueue else Icons.Rounded.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.cachedDataBadge(relativeTime),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "再読み込み",
                    modifier = Modifier
                        .size(14.dp)
                        .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                )
            }
        }
    }
}
