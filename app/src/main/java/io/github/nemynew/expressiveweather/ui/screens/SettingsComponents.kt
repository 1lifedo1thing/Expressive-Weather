package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * スムーズなタップ押し込みエフェクト（バウンス・スプリング無効化）。
 */
@Composable
fun Modifier.pressScaleEffect(pressedScale: Float = 0.985f): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "pressScale"
    )
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                isPressed = true
                try {
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                } finally {
                    isPressed = false
                }
            }
        }
        .graphicsLayer {
            val scale = scaleState.value
            scaleX = scale
            scaleY = scale
        }
}
