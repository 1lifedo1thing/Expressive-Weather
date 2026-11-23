package io.github.nemynew.expressiveweather.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.nemynew.expressiveweather.ui.util.LocalDeviceTiltState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 画面スクロール位置（px）を Draw / Frame フェーズのみで購読するための CompositionLocal。
 * Recomposition を発生させずに、全カードの積雪へスクロール移動の慣性を伝播します。
 */
val LocalSnowScrollOffsetProvider = compositionLocalOf<(() -> Float)?> { null }

/**
 * 雪が積もるタイルの形状種別
 */
enum class SnowTileShape {
    ROUNDED_RECT, // 角丸長方形 / 正方形 (HourlyForecast, Bentoカード, WeeklyForecastなど)
    CIRCLE        // 正円 (WindCard, HumidityCard, PressureCardなど)
}

/**
 * 複数周波数の非調和ノイズと傾き（または合成水平バイアス）による吹き溜まりシフトを合成した
 * 有機的な積雪プロファイル係数（crestWeight to lipWeight）を返します。
 */
internal fun computeOrganicSnowProfile(
    frac: Float,
    seed: Int,
    tiltX: Float
): Pair<Float, Float> {
    val f = frac.coerceIn(0f, 1f)
    // 端を滑らかにゼロへ溶け込ませるエルミート・テーパリング
    val s = sin(f * PI.toFloat()).coerceIn(0f, 1f)
    val edgeTaper = s * s * (3f - 2f * s)

    // 右傾き（tiltX < 0）時は下り坂方向(+X)へ重心が偏る
    val downhill = (-tiltX).coerceIn(-1.2f, 1.2f)
    val driftBias = (1f + downhill * 0.50f * (f - 0.5f) * 2f).coerceIn(0.32f, 1.68f)

    val s1 = seed * 0.73f
    val s2 = seed * 1.37f
    val crestWave = 0.72f +
        0.22f * sin(f * PI.toFloat() * 1.85f + s1 + downhill * 0.65f) +
        0.13f * cos(f * PI.toFloat() * 3.70f + s2) +
        0.07f * sin(f * PI.toFloat() * 6.40f + s1 * 1.4f)

    val lipWave = 0.74f +
        0.20f * cos(f * PI.toFloat() * 2.10f + s2 + downhill * 0.45f) +
        0.14f * sin(f * PI.toFloat() * 4.35f + s1) +
        0.08f * cos(f * PI.toFloat() * 7.20f + s2 * 0.9f)

    val crestWeight = (crestWave.coerceIn(0.35f, 1.25f) * edgeTaper * driftBias)
    val lipWeight = (lipWave.coerceIn(0.35f, 1.25f) * edgeTaper * driftBias)
    return Pair(crestWeight, lipWeight)
}

/**
 * 円形タイルにおける積雪冠の傾き追従中心角（ラジアン）を算出します。
 */
internal fun computeCircleApexAngleRad(tiltX: Float): Float {
    val safeTiltX = if (tiltX.isFinite()) tiltX.coerceIn(-1.2f, 1.2f) else 0f
    return (-safeTiltX * 0.45f).coerceIn(-0.54f, 0.54f)
}

/**
 * 端末傾きとタイル移動慣性（inertiaX）を統合した水平シフト量（+Xが右方向）を算出します。
 */
internal fun computeCombinedHorizontalBias(tiltX: Float, inertiaX: Float): Float {
    val safeTx = if (tiltX.isFinite()) tiltX else 0f
    val safeIx = if (inertiaX.isFinite()) inertiaX else 0f
    return (-safeTx * 0.85f + safeIx).coerceIn(-1.35f, 1.35f)
}

/**
 * タイルの移動速度（px/frame相当の変位）から雪の慣性ズレ量を1ステップ更新します。
 * 右へ動かすと雪は左（-）へ遅れて傾き、停止するとダンピング付きで戻ります。
 */
internal fun stepSnowInertia(
    currentInertia: Float,
    deltaMoveDp: Float,
    dt: Float,
    sensitivity: Float = 0.032f
): Float {
    val safeCur = if (currentInertia.isFinite()) currentInertia else 0f
    val safeDelta = if (deltaMoveDp.isFinite()) deltaMoveDp else 0f
    val safeDt = dt.coerceIn(0f, 0.05f)
    val damped = safeCur * exp(-safeDt * 8.5f)
    val pushed = damped - safeDelta * sensitivity
    return pushed.coerceIn(-1.15f, 1.15f)
}

/**
 * タイルの移動速度（dp/s）や急傾き変化に応じて積雪被覆率（snowCover）を更新します。
 * 激しく動かすと雪が払い落とされて薄くなり、静止すると再びうっすら積もり直します。
 */
internal fun stepSnowCover(
    currentCover: Float,
    motionSpeedDpPerSec: Float,
    tiltImpulse: Float,
    dt: Float
): Float {
    val safeCover = (if (currentCover.isFinite()) currentCover else 1f).coerceIn(0.16f, 1f)
    val safeDt = dt.coerceIn(0f, 0.05f)
    val speedTrigger = ((motionSpeedDpPerSec - 90f) / 480f).coerceIn(0f, 1.5f)
    val tiltTrigger = ((tiltImpulse - 0.025f) / 0.12f).coerceIn(0f, 1.2f)
    val shedSignal = maxOf(speedTrigger, tiltTrigger)

    return if (shedSignal > 0.05f) {
        (safeCover - shedSignal * 1.15f * safeDt).coerceAtLeast(0.16f)
    } else {
        (safeCover + 0.18f * safeDt).coerceAtMost(1f)
    }
}

/**
 * 端末傾き角度と氷晶ファセット法線の合致度から控えめなダイヤモンドダスト反射輝度を算出します。
 */
internal fun computeFacetGlintAlpha(
    tiltX: Float,
    tiltY: Float,
    facetNormalX: Float,
    facetNormalY: Float,
    ambientPhase: Float
): Float {
    val safeTx = if (tiltX.isFinite()) tiltX.coerceIn(-1.2f, 1.2f) else 0f
    val safeTy = if (tiltY.isFinite()) tiltY.coerceIn(-1.2f, 1.2f) else 0f
    // 静止時は極小（0.03〜0.10）に抑え、傾きが噛み合った時だけキラッと光る
    val ambient = 0.03f + 0.07f * ((sin(ambientPhase) + 1f) * 0.5f)
    val dx = safeTx - facetNormalX
    val dy = safeTy - facetNormalY
    val distSq = dx * dx + dy * dy
    val specular = exp(-distSq * 7.5f) * 0.44f
    return (ambient + specular).coerceIn(0f, 1f)
}

private data class SparkleFacet(
    val frac: Float,
    val normalX: Float,
    val normalY: Float,
    val phaseOffset: Float,
    val radiusDp: Float
)

private class ShedMote {
    var active = false
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var age = 0f
    var life = 0.6f
    var radiusDp = 1.0f
}

private class SnowTilePhysicsDriver(private val seed: Int) {
    val frameTick = mutableLongStateOf(0L)
    var timeSec = 0f
    var inertiaX = 0f
    var inertiaY = 0f
    var snowCover = 1f
    var lastSpawnTime = -1f
    var motesCursor = 0
    val motes = Array(6) { ShedMote() }

    // 幾何計算時の基準（直近の雪端位置）
    var anchorLeftX = 0f
    var anchorRightX = 0f
    var anchorTopY = 0f
    var anchorWidth = 0f

    fun spawnMote(vxDp: Float, vyDp: Float, bias: Float) {
        if (anchorWidth <= 1f) return
        val m = motes[motesCursor]
        motesCursor = (motesCursor + 1) % motes.size
        val idxRatio = ((motesCursor + (seed % 5)) % 6) / 5f
        val xBias = (0.15f + idxRatio * 0.70f + bias * 0.12f).coerceIn(0.05f, 0.95f)
        m.active = true
        m.x = anchorLeftX + anchorWidth * xBias
        m.y = anchorTopY + 1.5f
        m.vx = vxDp
        m.vy = vyDp
        m.age = 0f
        m.life = 0.48f + (motesCursor % 3) * 0.12f
        m.radiusDp = 0.75f + (motesCursor % 2) * 0.45f
    }
}

private class CachedSnowState {
    val fullSnowPath = Path()
    val crestHighlightPath = Path()
    var lastBias = Float.NaN
    var lastCover = Float.NaN
    var lastInertiaY = Float.NaN
    val sparkleX = FloatArray(5)
    val sparkleY = FloatArray(5)
    var sparkleCount = 0
}

/**
 * タイルやボックスの輪郭にうっすら・上品に乗る薄雪＆霜エフェクト。
 * 普段は存在感を抑えつつ、タイルの移動（ドラッグ・スワップ・スクロール）やデバイス傾きに応じて
 * 慣性ズレ・粉雪のこぼれ・角度反射・再積雪まで物理反応します。
 */
@Composable
fun Modifier.snowTileAccumulation(
    isSnowing: Boolean,
    isDark: Boolean = false,
    shape: SnowTileShape = SnowTileShape.ROUNDED_RECT,
    cornerRadiusDp: Dp = 44.dp,
    snowDepthDp: Dp = 3.4.dp,
    seed: Int = 0,
    tileOffsetProvider: (() -> Offset)? = null
): Modifier {
    if (!isSnowing) return this

    val tiltState = LocalDeviceTiltState.current
    val scrollOffsetProvider = LocalSnowScrollOffsetProvider.current
    val latestTileOffsetProvider by rememberUpdatedState(tileOffsetProvider)
    val latestScrollOffsetProvider by rememberUpdatedState(scrollOffsetProvider)
    val density = LocalDensity.current.density.coerceAtLeast(1f)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    val driver = remember(seed) { SnowTilePhysicsDriver(seed) }

    LaunchedEffect(isSnowing, lifecycle, tiltState, density) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val motionScale = Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            )
            if (motionScale > 0f) {
                var lastFrame = 0L
                var prevTileOffset = latestTileOffsetProvider?.invoke()
                var prevScrollY = latestScrollOffsetProvider?.invoke()
                var prevTiltX = tiltState?.tiltX?.takeIf { it.isFinite() } ?: 0f
                var prevTiltY = tiltState?.tiltY?.takeIf { it.isFinite() } ?: 0f

                while (true) {
                    withFrameNanos { now ->
                        if (lastFrame != 0L) {
                            val dt = (((now - lastFrame) / 1_000_000_000f) / motionScale)
                                .coerceAtMost(0.05f)
                            driver.timeSec += dt

                            val curTile = latestTileOffsetProvider?.invoke()
                            val dTileX = if (curTile != null && prevTileOffset != null) {
                                (curTile.x - prevTileOffset!!.x) / density
                            } else 0f
                            val dTileY = if (curTile != null && prevTileOffset != null) {
                                (curTile.y - prevTileOffset!!.y) / density
                            } else 0f
                            prevTileOffset = curTile

                            val curScrollY = latestScrollOffsetProvider?.invoke()
                            // スクロール値が増える＝コンテンツは画面上方向(-Y)へ移動
                            val dScrollScreenY = if (curScrollY != null && prevScrollY != null) {
                                -(curScrollY - prevScrollY!!) / density
                            } else 0f
                            prevScrollY = curScrollY

                            val totalDxDp = dTileX
                            val totalDyDp = dTileY + dScrollScreenY * 0.55f

                            val rawTx = tiltState?.tiltX ?: 0f
                            val rawTy = tiltState?.tiltY ?: 0f
                            val tx = if (rawTx.isFinite()) rawTx.coerceIn(-1.2f, 1.2f) else 0f
                            val ty = if (rawTy.isFinite()) rawTy.coerceIn(-1.2f, 1.2f) else 0f
                            val tiltImpulse = abs(tx - prevTiltX) + abs(ty - prevTiltY) * 0.7f
                            prevTiltX = tx
                            prevTiltY = ty

                            driver.inertiaX = stepSnowInertia(driver.inertiaX, totalDxDp, dt, sensitivity = 0.036f)
                            driver.inertiaY = stepSnowInertia(driver.inertiaY, totalDyDp, dt, sensitivity = 0.026f)

                            val speedDpPerSec = if (dt > 0.0005f) hypot(totalDxDp, totalDyDp) / dt else 0f
                            driver.snowCover = stepSnowCover(
                                currentCover = driver.snowCover,
                                motionSpeedDpPerSec = speedDpPerSec,
                                tiltImpulse = tiltImpulse,
                                dt = dt
                            )

                            // 移動や急な傾きで薄くパウダーが散る（クールダウン付きで過剰生成を防止）
                            val shouldShed = (speedDpPerSec > 165f || tiltImpulse > 0.045f || abs(tx) > 0.52f)
                            if (shouldShed && (driver.timeSec - driver.lastSpawnTime) >= 0.14f) {
                                driver.lastSpawnTime = driver.timeSec
                                val bias = computeCombinedHorizontalBias(tx, driver.inertiaX)
                                val moteVxDp = (-totalDxDp / dt.coerceAtLeast(0.01f)) * 0.14f + bias * 16f
                                val moteVyDp = (-totalDyDp / dt.coerceAtLeast(0.01f)) * 0.10f + 22f
                                driver.spawnMote(
                                    vxDp = moteVxDp.coerceIn(-45f, 45f),
                                    vyDp = moteVyDp.coerceIn(-12f, 48f),
                                    bias = bias
                                )
                            }

                            // アクティブな粉雪粒を更新
                            for (i in driver.motes.indices) {
                                val m = driver.motes[i]
                                if (!m.active) continue
                                m.age += dt
                                if (m.age >= m.life) {
                                    m.active = false
                                } else {
                                    m.x += m.vx * density * dt
                                    m.y += m.vy * density * dt
                                    m.vy += 32f * dt
                                    m.vx *= exp(-dt * 2.5f)
                                }
                            }

                            driver.frameTick.longValue = driver.frameTick.longValue + 1L
                        }
                        lastFrame = now
                    }
                }
            }
        }
    }

    return this.then(
        Modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val cornerRadiusPx = cornerRadiusDp.toPx()
            val snowDepthPx = snowDepthDp.toPx()
            val state = CachedSnowState()

            val facets = List(5) { idx ->
                val f = 0.18f + idx * 0.16f
                val nx = ((idx - 2f) / 2f) * 0.65f + ((seed % 7) - 3) * 0.04f
                val ny = (((idx * 3 + seed) % 5) - 2) * 0.18f
                SparkleFacet(
                    frac = f,
                    normalX = nx,
                    normalY = ny,
                    phaseOffset = idx * 1.43f + seed * 0.41f,
                    radiusDp = 0.85f + (idx % 2) * 0.30f
                )
            }

            // 存在感を抑えた半透明フロストベール（下端 alpha = 0f でガラスへ溶け込む）
            val topColor = if (isDark) Color(0xFFF4F9FF).copy(alpha = 0.46f) else Color.White.copy(alpha = 0.58f)
            val midColor = if (isDark) Color(0xFFD2E3F3).copy(alpha = 0.22f) else Color(0xFFF2F8FD).copy(alpha = 0.28f)
            val bottomColor = Color.Transparent

            val rectSnowBrush = Brush.verticalGradient(
                colors = listOf(topColor, midColor, bottomColor),
                startY = -snowDepthPx * 0.25f,
                endY = snowDepthPx * 0.85f
            )

            val circleBrushCenterY = (h * 0.5f) - (w * 0.5f)
            val circleSnowBrush = Brush.radialGradient(
                colors = listOf(topColor, midColor, bottomColor),
                center = Offset(w * 0.5f, circleBrushCenterY.coerceAtLeast(-w)),
                radius = (w * 0.52f).coerceAtLeast(1f)
            )

            val baseRimColor = Color.White.copy(alpha = if (isDark) 0.28f else 0.42f)
            val rimStroke = Stroke(width = 0.70.dp.toPx(), cap = StrokeCap.Round)

            val glintBrush = Brush.radialGradient(
                0.0f to Color.White,
                0.45f to Color(0xFFE8F4FF).copy(alpha = 0.65f),
                1.0f to Color.Transparent,
                center = Offset.Zero,
                radius = 1f
            )

            onDrawWithContent {
                drawContent()
                if (w <= 1f || h <= 1f) return@onDrawWithContent

                // Draw フェーズでのみ frameTick を参照（Recomposition を一切起こさない）
                @Suppress("UNUSED_VARIABLE")
                val tick = driver.frameTick.longValue

                val rawTx = tiltState?.tiltX ?: 0f
                val rawTy = tiltState?.tiltY ?: 0f
                val tx = if (rawTx.isFinite()) rawTx.coerceIn(-1.2f, 1.2f) else 0f
                val ty = if (rawTy.isFinite()) rawTy.coerceIn(-1.2f, 1.2f) else 0f

                val combinedBias = computeCombinedHorizontalBias(tx, driver.inertiaX)
                val cover = driver.snowCover
                val iy = driver.inertiaY

                if (state.lastBias.isNaN() ||
                    abs(combinedBias - state.lastBias) > 0.004f ||
                    abs(cover - state.lastCover) > 0.01f ||
                    abs(iy - state.lastInertiaY) > 0.005f
                ) {
                    state.lastBias = combinedBias
                    state.lastCover = cover
                    state.lastInertiaY = iy
                    val effectiveTiltX = -combinedBias
                    when (shape) {
                        SnowTileShape.ROUNDED_RECT -> updateRoundedRectSnowGeometry(
                            state = state,
                            driver = driver,
                            w = w,
                            cornerRadiusPx = cornerRadiusPx,
                            snowDepthPx = snowDepthPx,
                            seed = seed,
                            effectiveTiltX = effectiveTiltX,
                            snowCover = cover,
                            inertiaY = iy,
                            facets = facets
                        )
                        SnowTileShape.CIRCLE -> updateCircleSnowGeometry(
                            state = state,
                            driver = driver,
                            w = w,
                            h = h,
                            snowDepthPx = snowDepthPx,
                            seed = seed,
                            effectiveTiltX = effectiveTiltX,
                            snowCover = cover,
                            inertiaY = iy,
                            facets = facets
                        )
                    }
                }

                val coverAlpha = (0.30f + 0.70f * cover).coerceIn(0.25f, 1f)

                // 1. うっすらガラスに溶け込む半透明の薄雪ベール（底面影なし）
                drawPath(
                    path = state.fullSnowPath,
                    brush = if (shape == SnowTileShape.CIRCLE) circleSnowBrush else rectSnowBrush,
                    alpha = coverAlpha
                )

                // 2. 極細の上稜線リムハイライト
                val rimLitScale = (1f - ty * 0.14f).coerceIn(0.75f, 1.15f)
                drawPath(
                    path = state.crestHighlightPath,
                    color = baseRimColor.copy(alpha = (baseRimColor.alpha * coverAlpha * rimLitScale).coerceIn(0f, 1f)),
                    style = rimStroke
                )

                // 3. 傾き・移動時にだけ微かに光る氷晶反射
                val phase = driver.timeSec * 1.45f
                val maxGlintAlpha = (if (isDark) 0.42f else 0.52f) * coverAlpha
                for (idx in 0 until state.sparkleCount) {
                    val facet = facets[idx]
                    val glintAlpha = computeFacetGlintAlpha(
                        tiltX = tx - driver.inertiaX * 0.5f,
                        tiltY = ty - iy * 0.5f,
                        facetNormalX = facet.normalX,
                        facetNormalY = facet.normalY,
                        ambientPhase = phase + facet.phaseOffset
                    )
                    if (glintAlpha > 0.14f) {
                        drawSoftIceGlint(
                            center = Offset(state.sparkleX[idx], state.sparkleY[idx]),
                            radiusPx = facet.radiusDp.dp.toPx() * (0.7f + glintAlpha * 0.5f),
                            alpha = (glintAlpha * maxGlintAlpha).coerceIn(0f, 1f),
                            glintBrush = glintBrush
                        )
                    }
                }

                // 4. タイル移動や急傾きでこぼれ落ちる微細な粉雪粒
                for (i in driver.motes.indices) {
                    val m = driver.motes[i]
                    if (!m.active) continue
                    val progress = (m.age / m.life).coerceIn(0f, 1f)
                    val mAlpha = (sin(progress * PI.toFloat()) * 0.36f).coerceIn(0f, 0.40f)
                    if (mAlpha > 0.02f) {
                        drawCircle(
                            color = Color.White.copy(alpha = mAlpha),
                            radius = m.radiusDp.dp.toPx(),
                            center = Offset(m.x, m.y)
                        )
                    }
                }
            }
        }
    )
}

private fun updateRoundedRectSnowGeometry(
    state: CachedSnowState,
    driver: SnowTilePhysicsDriver,
    w: Float,
    cornerRadiusPx: Float,
    snowDepthPx: Float,
    seed: Int,
    effectiveTiltX: Float,
    snowCover: Float,
    inertiaY: Float,
    facets: List<SparkleFacet>
) {
    state.fullSnowPath.reset()
    state.crestHighlightPath.reset()
    state.sparkleCount = 0

    val r = cornerRadiusPx.coerceAtMost(w * 0.5f)
    val steps = 56
    val downhill = (-effectiveTiltX).coerceIn(-1.35f, 1.35f)
    val startX = (w * (0.04f + (downhill.coerceAtLeast(0f)) * 0.06f)).coerceIn(0f, w * 0.22f)
    val endX = (w * (0.96f + (downhill.coerceAtMost(0f)) * 0.06f)).coerceIn(w * 0.78f, w)
    val spanX = (endX - startX).coerceAtLeast(1f)

    driver.anchorLeftX = startX
    driver.anchorRightX = endX
    driver.anchorWidth = spanX
    driver.anchorTopY = roundedRectTopY((startX + endX) * 0.5f, w, r)

    val depthScale = (0.45f + 0.55f * snowCover)
    val verticalInertiaScale = (1f - inertiaY * 0.28f).coerceIn(0.65f, 1.35f)

    var nextFacetIdx = 0
    for (i in 0..steps) {
        val frac = i.toFloat() / steps
        val x = startX + frac * spanX
        val tileTopY = roundedRectTopY(x, w, r)
        val (crestWeight, _) = computeOrganicSnowProfile(frac, seed, effectiveTiltX)
        val crestY = tileTopY - snowDepthPx * 0.22f * crestWeight * depthScale * verticalInertiaScale

        if (i == 0) {
            state.fullSnowPath.moveTo(x, crestY)
            state.crestHighlightPath.moveTo(x, crestY)
        } else {
            state.fullSnowPath.lineTo(x, crestY)
            state.crestHighlightPath.lineTo(x, crestY)
        }

        if (nextFacetIdx < facets.size && frac >= facets[nextFacetIdx].frac) {
            state.sparkleX[state.sparkleCount] = x
            state.sparkleY[state.sparkleCount] = crestY + snowDepthPx * 0.16f
            state.sparkleCount++
            nextFacetIdx++
        }
    }

    for (i in steps downTo 0) {
        val frac = i.toFloat() / steps
        val x = startX + frac * spanX
        val tileTopY = roundedRectTopY(x, w, r)
        val (_, lipWeight) = computeOrganicSnowProfile(frac, seed, effectiveTiltX)
        val lipY = tileTopY + snowDepthPx * 0.68f * lipWeight * depthScale * (1f + inertiaY * 0.20f).coerceIn(0.75f, 1.3f)
        state.fullSnowPath.lineTo(x, lipY)
    }
    state.fullSnowPath.close()
}

private fun updateCircleSnowGeometry(
    state: CachedSnowState,
    driver: SnowTilePhysicsDriver,
    w: Float,
    h: Float,
    snowDepthPx: Float,
    seed: Int,
    effectiveTiltX: Float,
    snowCover: Float,
    inertiaY: Float,
    facets: List<SparkleFacet>
) {
    state.fullSnowPath.reset()
    state.crestHighlightPath.reset()
    state.sparkleCount = 0

    val cx = w * 0.5f
    val cy = h * 0.5f
    val radius = minOf(w, h) * 0.5f

    val apexAngle = computeCircleApexAngleRad(effectiveTiltX)
    val halfArc = 0.24f * PI.toFloat()
    val steps = 40

    driver.anchorLeftX = cx + radius * sin(apexAngle - halfArc)
    driver.anchorRightX = cx + radius * sin(apexAngle + halfArc)
    driver.anchorWidth = abs(driver.anchorRightX - driver.anchorLeftX).coerceAtLeast(10f)
    driver.anchorTopY = cy - radius * cos(apexAngle)

    val depthScale = (0.45f + 0.55f * snowCover)
    val verticalInertiaScale = (1f - inertiaY * 0.28f).coerceIn(0.65f, 1.35f)

    var nextFacetIdx = 0
    for (i in 0..steps) {
        val frac = i.toFloat() / steps
        val localAngle = -halfArc + frac * (halfArc * 2f)
        val t = apexAngle + localAngle
        val arcRatio = (abs(localAngle) / halfArc).coerceIn(0f, 1f)
        val taper = cos(arcRatio * (PI.toFloat() * 0.5f)).pow(1.45f).coerceAtLeast(0f)
        val (crestWeight, _) = computeOrganicSnowProfile(frac, seed, effectiveTiltX)
        val rOuter = radius + snowDepthPx * 0.20f * crestWeight * depthScale * verticalInertiaScale * (0.45f + 0.55f * taper)

        val x = cx + rOuter * sin(t)
        val y = cy - rOuter * cos(t)

        if (i == 0) {
            state.fullSnowPath.moveTo(x, y)
            state.crestHighlightPath.moveTo(x, y)
        } else {
            state.fullSnowPath.lineTo(x, y)
            state.crestHighlightPath.lineTo(x, y)
        }

        if (nextFacetIdx < facets.size && frac >= facets[nextFacetIdx].frac) {
            val rMid = rOuter - snowDepthPx * 0.16f
            state.sparkleX[state.sparkleCount] = cx + rMid * sin(t)
            state.sparkleY[state.sparkleCount] = cy - rMid * cos(t)
            state.sparkleCount++
            nextFacetIdx++
        }
    }

    for (i in steps downTo 0) {
        val frac = i.toFloat() / steps
        val localAngle = -halfArc + frac * (halfArc * 2f)
        val t = apexAngle + localAngle
        val arcRatio = (abs(localAngle) / halfArc).coerceIn(0f, 1f)
        val taper = cos(arcRatio * (PI.toFloat() * 0.5f)).pow(1.35f).coerceAtLeast(0f)
        val (_, lipWeight) = computeOrganicSnowProfile(frac, seed, effectiveTiltX)
        val rInner = radius - snowDepthPx * 0.66f * lipWeight * depthScale * (0.45f + 0.55f * taper)

        val x = cx + rInner * sin(t)
        val y = cy - rInner * cos(t)
        state.fullSnowPath.lineTo(x, y)
    }
    state.fullSnowPath.close()
}

private fun roundedRectTopY(x: Float, w: Float, r: Float): Float = when {
    x < r -> {
        val dx = r - x
        r - sqrt((r * r - dx * dx).coerceAtLeast(0f))
    }
    x > w - r -> {
        val dx = x - (w - r)
        r - sqrt((r * r - dx * dx).coerceAtLeast(0f))
    }
    else -> 0f
}

private fun DrawScope.drawSoftIceGlint(
    center: Offset,
    radiusPx: Float,
    alpha: Float,
    glintBrush: Brush
) {
    drawCircle(
        brush = glintBrush,
        radius = radiusPx * 2.0f,
        center = center,
        alpha = alpha * 0.65f
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = radiusPx * 0.38f,
        center = center
    )
}
