package com.example.expressiveweather.ui.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI

/**
 * 端末の傾き・ジャイロセンサー状態
 *
 * @property tiltX 画面の左右傾き（-1.0f: 左傾き 〜 +1.0f: 右傾き）。水平時は0f。
 * @property tiltY 画面の前後傾き（-1.0f: 手前起こし 〜 +1.0f: 奥倒し）。自然な持ち手時は0f。
 * @property isAvailable 端末がジャイロ/回転ベクトルセンサーをサポートしているか
 */
@Stable
interface DeviceTiltState {
    val tiltX: Float
    val tiltY: Float
    val isAvailable: Boolean
}

internal class MutableDeviceTiltState : DeviceTiltState {
    private val _tiltX = mutableFloatStateOf(0f)
    private val _tiltY = mutableFloatStateOf(0f)
    private val _isAvailable = mutableStateOf(false)

    override val tiltX: Float get() = _tiltX.floatValue.takeIf { it.isFinite() } ?: 0f
    override val tiltY: Float get() = _tiltY.floatValue.takeIf { it.isFinite() } ?: 0f
    override val isAvailable: Boolean get() = _isAvailable.value

    fun update(x: Float, y: Float) {
        _tiltX.floatValue = if (x.isFinite()) x else 0f
        _tiltY.floatValue = if (y.isFinite()) y else 0f
    }

    fun setAvailable(available: Boolean) {
        _isAvailable.value = available
    }
}

val LocalDeviceTiltState = compositionLocalOf<DeviceTiltState?> { null }

/**
 * 端末のジャイロ/回転ベクトルセンサーを購読し、低消費電力・ノイズフィルタ済みの
 * 滑らかな画面座標傾き状態（DeviceTiltState）を提供します。
 *
 * - 画面の回転（Portrait / Landscape）を自動補正し、常に画面の左右・上下と一致
 * - 指数平滑化ローパスフィルタにより、微小な手ブレを抑えてシルキーな動きを実現
 * - アプリのライフサイクル（ON_RESUME / ON_PAUSE）に追従し、非表示時はセンサーを解除
 * - Drawフェーズ（Canvas/onDrawBehind）での参照によりRecompositionを回避し高FPSを維持
 */
@Composable
fun rememberDeviceTiltState(
    enabled: Boolean = true,
    smoothFactor: Float = 0.18f
): DeviceTiltState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tiltState = remember { MutableDeviceTiltState() }

    DisposableEffect(lifecycleOwner, enabled) {
        if (!enabled) {
            tiltState.update(0f, 0f)
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            tiltState.setAvailable(false)
            return@DisposableEffect onDispose { }
        }

        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val targetSensor = gravitySensor ?: accelSensor
        if (targetSensor == null) {
            tiltState.setAvailable(false)
            return@DisposableEffect onDispose { }
        }
        tiltState.setAvailable(true)

        var filteredTiltX = 0f
        var filteredTiltY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                }

                // 画面の向きに応じた重力・加速度ベクトルの軸変換
                val (screenAx, screenAy) = when (displayRotation) {
                    Surface.ROTATION_0 -> Pair(event.values[0], event.values[1])
                    Surface.ROTATION_90 -> Pair(-event.values[1], event.values[0])
                    Surface.ROTATION_180 -> Pair(-event.values[0], -event.values[1])
                    Surface.ROTATION_270 -> Pair(event.values[1], -event.values[0])
                    else -> Pair(event.values[0], event.values[1])
                }
                val screenAz = event.values[2]

                val inPlaneMag = kotlin.math.sqrt(screenAx * screenAx + screenAy * screenAy)
                val rawX = if (inPlaneMag < 0.5f) {
                    0f
                } else {
                    // 画面の左右ロール傾き: 垂直保持時は screenAx == 0 -> rollAngle == 0.0f
                    // 端末を右に傾けると screenAx > 0 -> rollAngle > 0
                    // ±45度（PI/4 rad）で ±1.0f に正規化
                    val rollAngle = kotlin.math.atan2(screenAx, screenAy)
                    (rollAngle / (PI.toFloat() / 4f)).coerceIn(-1.5f, 1.5f)
                }

                // 画面の前後ピッチ傾き: 手に持つ自然な約30度（0.52rad）後傾をニュートラル 0f とする
                val pitchAngle = kotlin.math.atan2(screenAz, screenAy.coerceAtLeast(0.1f))
                val neutralPitch = 0.52f
                val rawY = ((pitchAngle - neutralPitch) / (PI.toFloat() / 4f)).coerceIn(-1.5f, 1.5f)

                if (!rawX.isFinite() || !rawY.isFinite()) return

                if (!filteredTiltX.isFinite()) filteredTiltX = 0f
                if (!filteredTiltY.isFinite()) filteredTiltY = 0f

                // 指数移動平均フィルタでジッターを消去し、有機的な慣性追従を実現
                filteredTiltX += smoothFactor * (rawX - filteredTiltX)
                filteredTiltY += smoothFactor * (rawY - filteredTiltY)

                val safeX = if (filteredTiltX.isFinite()) filteredTiltX else 0f
                val safeY = if (filteredTiltY.isFinite()) filteredTiltY else 0f
                filteredTiltX = safeX
                filteredTiltY = safeY

                tiltState.update(safeX, safeY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        var isRegistered = false
        fun register() {
            if (!isRegistered) {
                sensorManager.registerListener(listener, targetSensor, SensorManager.SENSOR_DELAY_GAME)
                isRegistered = true
            }
        }
        fun unregister() {
            if (isRegistered) {
                sensorManager.unregisterListener(listener)
                isRegistered = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                Lifecycle.Event.ON_DESTROY -> unregister()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            register()
        }

        onDispose {
            unregister()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return tiltState
}
