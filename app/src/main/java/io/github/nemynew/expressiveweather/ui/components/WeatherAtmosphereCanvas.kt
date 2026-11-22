package io.github.nemynew.expressiveweather.ui.components

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.theme.WeatherAtmosphereTheme
import io.github.nemynew.expressiveweather.ui.util.DeviceTiltState
import io.github.nemynew.expressiveweather.ui.util.LocalDeviceTiltState
import io.github.nemynew.expressiveweather.ui.util.rememberDeviceTiltState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Raindrop(
    var xRatio: Float,
    var yRatio: Float,
    var speed: Float,
    var length: Float,
    var alpha: Float,
    var strokeWidth: Float
)

private data class WaterRipple(
    var xRatio: Float,
    var yRatio: Float,
    var maxRadius: Float,
    var phaseOffset: Float,
    var alpha: Float
)

private data class Snowflake(
    var xRatio: Float,
    var yRatio: Float,
    var speed: Float,
    var radius: Float,
    var wobbleSpeed: Float,
    var wobbleAmplitude: Float,
    var alpha: Float
)

private data class SunDust(
    var xRatio: Float,
    var yRatio: Float,
    var speed: Float,
    var radius: Float,
    var pulseOffset: Float,
    var alpha: Float
)

private data class NightStar(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val twinkleOffset: Float,
    val baseAlpha: Float
)

private data class BeamDef(
    val centerDeg: Float,
    val spreadDeg: Float,
    val alpha: Float,
    val speedMultiplier: Float,
    val phase: Float
)

// 6 overlapping natural volumetric light shafts radiating across the sky
private val VOLUMETRIC_BEAMS = listOf(
    BeamDef(centerDeg = 112f, spreadDeg = 14f, alpha = 0.22f, speedMultiplier = 1.0f, phase = 0f),
    BeamDef(centerDeg = 126f, spreadDeg = 18f, alpha = 0.38f, speedMultiplier = 0.8f, phase = 1.2f), // Main prominent beam matching user photo
    BeamDef(centerDeg = 138f, spreadDeg = 12f, alpha = 0.26f, speedMultiplier = 1.3f, phase = 2.4f),
    BeamDef(centerDeg = 150f, spreadDeg = 20f, alpha = 0.32f, speedMultiplier = 0.9f, phase = 3.8f), // Secondary wide beam
    BeamDef(centerDeg = 164f, spreadDeg = 15f, alpha = 0.18f, speedMultiplier = 1.1f, phase = 5.0f),
    BeamDef(centerDeg = 130f, spreadDeg = 32f, alpha = 0.15f, speedMultiplier = 0.6f, phase = 2.0f)  // Ambient broad scatter
)

@Composable
fun WeatherAtmosphereCanvas(
    condition: WeatherCondition,
    atmosphere: WeatherAtmosphereTheme? = null,
    tiltState: DeviceTiltState? = null,
    modifier: Modifier = Modifier
) {
    val activeTilt = tiltState ?: LocalDeviceTiltState.current ?: rememberDeviceTiltState()
    val isDark = atmosphere?.isDark == true
    if (condition.hasRainAtmosphere) {
        RainAtmosphere(condition = condition, isDark = isDark, tiltState = activeTilt, modifier = modifier)
        return
    }
    if (condition == WeatherCondition.SNOW) {
        SnowAtmosphere(isDark = isDark, tiltState = activeTilt, modifier = modifier)
        return
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val secondsState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val motionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            if (motionScale > 0f) {
                var lastFrame = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (lastFrame != 0L) {
                            secondsState.floatValue += ((now - lastFrame) / 1_000_000_000f)
                                .coerceAtMost(0.05f) / motionScale
                        }
                        lastFrame = now
                    }
                }
            }
        }
    }

    // Sun dust / shimmering particles for clear weather
    val sunParticles = remember {
        val rng = Random(20260401)
        List(26) {
            SunDust(
                xRatio = rng.nextFloat(),
                yRatio = rng.nextFloat(),
                speed = 0.06f + rng.nextFloat() * 0.12f,
                radius = 1.5f + rng.nextFloat() * 3.5f,
                pulseOffset = rng.nextFloat() * 6.28f,
                alpha = 0.35f + rng.nextFloat() * 0.5f
            )
        }
    }

    // Twinkling stars for dark mode
    val nightStars = remember {
        val rng = Random(20260402)
        List(45) {
            NightStar(
                xRatio = rng.nextFloat(),
                yRatio = rng.nextFloat() * 0.65f,
                radius = 1.0f + rng.nextFloat() * 1.8f,
                twinkleOffset = rng.nextFloat() * 6.28f,
                baseAlpha = 0.30f + rng.nextFloat() * 0.55f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val elapsedSec = secondsState.floatValue
        val animationProgress = (elapsedSec / 6.0f) - kotlin.math.floor(elapsedSec / 6.0f)
        val sunPulse = 1.0f - 0.06f * cos(elapsedSec * (PI.toFloat() / 3.8f))
        val tiltX = if (activeTilt.tiltX.isFinite()) activeTilt.tiltX else 0f
        val tiltY = if (activeTilt.tiltY.isFinite()) activeTilt.tiltY else 0f
        when (condition) {
            WeatherCondition.CLEAR -> {
                drawClearSkyRealistic(
                    sunPulse = sunPulse,
                    sunParticles = sunParticles,
                    progress = animationProgress,
                    accentColor = atmosphere?.accentColor,
                    isDark = isDark,
                    stars = nightStars,
                    tiltX = tiltX,
                    tiltY = tiltY
                )
            }
            WeatherCondition.FEW_CLOUDS -> {
                drawSunBehindClouds(
                    sunPulse = sunPulse,
                    progress = animationProgress,
                    accentColor = atmosphere?.accentColor,
                    isDark = isDark,
                    stars = nightStars,
                    tiltX = tiltX,
                    tiltY = tiltY
                )
                drawRealisticClouds(
                    progress = animationProgress,
                    isFewClouds = true,
                    isDark = isDark,
                    accentColor = atmosphere?.accentColor,
                    tiltX = tiltX,
                    tiltY = tiltY
                )
            }
            WeatherCondition.CLOUDY -> {
                drawRealisticClouds(
                    progress = animationProgress,
                    isFewClouds = false,
                    isDark = isDark,
                    accentColor = atmosphere?.accentColor,
                    tiltX = tiltX,
                    tiltY = tiltY
                )
            }
            WeatherCondition.FOG -> {
                drawFlowingFog(animationProgress, tiltX = tiltX, tiltY = tiltY)
            }
            else -> {}
        }

        // Frosted Glass Reflection Highlight: subtle specular shimmer across top with tilt shift
        drawGlassReflection(tiltX = tiltX, tiltY = tiltY)
    }
}

// =========================================================================
// RENDERERS
// =========================================================================

/**
 * 晴天: 添付画像（media_1789608489320.jpg）完全再現
 * 右上の白熱太陽、扇状に斜め下へ降り注ぐ柔らかな薄明光線（God rays）、右下の柔らかな積雲、光の微粒子
 */
private fun DrawScope.drawClearSkyRealistic(
    sunPulse: Float,
    sunParticles: List<SunDust>,
    progress: Float,
    accentColor: Color?,
    isDark: Boolean,
    stars: List<NightStar>,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    if (size.width <= 1f || size.height <= 1f) return
    val safeTiltX = if (tiltX.isFinite()) tiltX else 0f
    val safeTiltY = if (tiltY.isFinite()) tiltY else 0f

    if (isDark) {
        // --- DARK MODE (夜間・月光と星屑) ---
        // 1. Twinkling Stars in upper atmosphere (奥の星屑は微小パララックス)
        stars.forEach { star ->
            val twinkle = (star.baseAlpha * (0.5f + 0.5f * sin(progress * 4f * PI.toFloat() + star.twinkleOffset))).coerceIn(0.1f, 1f)
            val sx = star.xRatio * size.width + safeTiltX * 14f
            val sy = star.yRatio * size.height + safeTiltY * 8f
            if (sx.isFinite() && sy.isFinite() && star.radius > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = twinkle),
                    radius = star.radius,
                    center = Offset(sx, sy)
                )
            }
        }

        // 2. Soft Moon in upper right (中遠景パララックス)
        val moonCenter = Offset(size.width * 0.88f + safeTiltX * 26f, size.height * 0.08f + safeTiltY * 16f)
        val moonRadius = (size.width * 0.12f).coerceAtLeast(1f)

        // Moon subtle crepuscular rays (gentle silver-blue beams)
        drawVolumetricBeams(
            origin = moonCenter,
            progress = progress,
            beamColor = Color(0xFF90CAF9),
            baseAlphaMultiplier = 0.28f,
            tiltX = safeTiltX
        )

        // Moon Glow
        val moonGlowRadius = (moonRadius * 3.5f * sunPulse).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF82B1FF).copy(alpha = 0.35f),
                    Color(0xFF448AFF).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = moonCenter,
                radius = moonGlowRadius
            ),
            radius = moonGlowRadius,
            center = moonCenter
        )

        // Moon Core
        drawCircle(
            color = Color(0xFFEEF3FF),
            radius = (moonRadius * 0.7f).coerceAtLeast(1f),
            center = moonCenter
        )

        // Night floating particles
        drawSunDustParticles(sunParticles, progress, Color(0xFFBBDEFB), tiltX = safeTiltX)

        // Night bottom-right cloud silhouette (前景パララックス)
        drawPhotoCloudBottomRight(progress, isDark = true, tiltX = safeTiltX, tiltY = safeTiltY)
    } else {
        // --- LIGHT MODE (添付画像の圧倒的にリアルな青空・太陽・光芒・雲) ---
        val sunCenter = Offset(size.width * 0.88f + safeTiltX * 26f, size.height * 0.06f + safeTiltY * 16f)
        val baseRadius = (size.width * 0.22f).coerceAtLeast(1f)

        // 1. Volumetric God Rays (薄明光線: 画面を傾けると光の帯がダイナミックに角度を変える)
        drawVolumetricBeams(
            origin = sunCenter,
            progress = progress,
            beamColor = Color.White,
            baseAlphaMultiplier = 1.0f,
            tiltX = safeTiltX
        )

        // 2. Multi-layered Atmospheric Sun Halo & Corona
        val haloRadius = (size.width * 1.35f * sunPulse).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color(0xFFFFF9E6).copy(alpha = 0.25f),
                    Color(0xFFE1F5FE).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = sunCenter,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = sunCenter
        )

        // Mid vibrant corona
        val coronaRadius = (baseRadius * 2.2f * sunPulse).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color(0xFFFFFDE7).copy(alpha = 0.50f),
                    Color(0xFFB3E5FC).copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = sunCenter,
                radius = coronaRadius
            ),
            radius = coronaRadius,
            center = sunCenter
        )

        // Intense glowing Sun Core
        drawCircle(
            color = Color.White,
            radius = (baseRadius * 0.55f).coerceAtLeast(1f),
            center = sunCenter
        )

        // 3. Shimmering Sun Dust particles drifting through the rays
        drawSunDustParticles(sunParticles, progress, Color.White, tiltX = safeTiltX)

        // 4. Soft Puffy Cumulus Cloud on bottom-right (手前の積雲は大きなパララックス)
        drawPhotoCloudBottomRight(progress, isDark = false, tiltX = safeTiltX, tiltY = safeTiltY)
    }
}

/**
 * 扇状に降り注ぐボリューメトリック薄明光線（God Rays）
 */
private fun DrawScope.drawVolumetricBeams(
    origin: Offset,
    progress: Float,
    beamColor: Color,
    baseAlphaMultiplier: Float,
    tiltX: Float = 0f
) {
    if (size.width <= 1f || size.height <= 1f) return
    val rayLength = size.height * 1.5f
    if (rayLength <= 1f) return
    if (!origin.x.isFinite() || !origin.y.isFinite()) return
    val safeTiltX = if (tiltX.isFinite()) tiltX else 0f
    val tiltSway = safeTiltX * 10f // 端末の傾きで光芒が大きく扇状にスイープ！

    for (beam in VOLUMETRIC_BEAMS) {
        // Natural gentle swaying of the angle + gyro interactive sweep
        val sway = sin(progress * 2f * PI.toFloat() * beam.speedMultiplier + beam.phase) * 2.8f + tiltSway
        val pulse = 0.82f + 0.18f * sin(progress * 3f * PI.toFloat() * beam.speedMultiplier + beam.phase * 1.7f)
        val totalAlpha = (beam.alpha * pulse * baseAlphaMultiplier).coerceIn(0f, 1f)
        if (totalAlpha <= 0.001f) continue

        val angle1 = (beam.centerDeg - beam.spreadDeg * 0.5f + sway) * (PI.toFloat() / 180f)
        val angle2 = (beam.centerDeg + beam.spreadDeg * 0.5f + sway) * (PI.toFloat() / 180f)

        val p1 = Offset(
            x = origin.x + cos(angle1) * rayLength,
            y = origin.y + sin(angle1) * rayLength
        )
        val p2 = Offset(
            x = origin.x + cos(angle2) * rayLength,
            y = origin.y + sin(angle2) * rayLength
        )
        val midFar = Offset((p1.x + p2.x) * 0.5f, (p1.y + p2.y) * 0.5f)

        if (!midFar.x.isFinite() || !midFar.y.isFinite()) continue

        // LinearGradient requires start and end points to be distinct
        val dx = midFar.x - origin.x
        val dy = midFar.y - origin.y
        if (dx * dx + dy * dy < 4f) continue

        val beamPath = Path().apply {
            moveTo(origin.x, origin.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }

        drawPath(
            path = beamPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    beamColor.copy(alpha = totalAlpha),
                    beamColor.copy(alpha = totalAlpha * 0.45f),
                    Color.Transparent
                ),
                start = origin,
                end = midFar
            )
        )
    }
}

/**
 * 添付画像右下のふわりとしたリアルな積雲
 */
private fun DrawScope.drawPhotoCloudBottomRight(
    progress: Float,
    isDark: Boolean,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    if (size.width <= 1f || size.height <= 1f) return
    val safeTiltX = if (tiltX.isFinite()) tiltX else 0f
    val safeTiltY = if (tiltY.isFinite()) tiltY else 0f
    val floatOffset = sin(progress * 2f * PI.toFloat()) * 6f
    val cloudBase = Offset(size.width * 0.96f + safeTiltX * 55f, size.height * 0.72f + floatOffset + safeTiltY * 28f)
    if (!cloudBase.x.isFinite() || !cloudBase.y.isFinite()) return

    val cloudColors = if (isDark) {
        listOf(
            Color(0xFF2C3E55).copy(alpha = 0.75f),
            Color(0xFF1E2C3D).copy(alpha = 0.40f),
            Color.Transparent
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.88f),
            Color(0xFFEAF5FF).copy(alpha = 0.55f),
            Color.Transparent
        )
    }

    // Overlapping cloud puffs forming the natural cumulus edge from the photo
    val puffs = listOf(
        Triple(Offset(-size.width * 0.08f, -size.height * 0.05f), size.width * 0.32f, 0.90f),
        Triple(Offset(-size.width * 0.16f, size.height * 0.02f), size.width * 0.28f, 0.85f),
        Triple(Offset(-size.width * 0.24f, size.height * 0.06f), size.width * 0.22f, 0.75f),
        Triple(Offset(0f, 0f), size.width * 0.35f, 0.95f)
    )

    for ((offset, radius, alphaMul) in puffs) {
        val safeRadius = radius.coerceAtLeast(1f)
        val center = Offset(cloudBase.x + offset.x, cloudBase.y + offset.y)
        if (!center.x.isFinite() || !center.y.isFinite()) continue
        drawCircle(
            brush = Brush.radialGradient(
                colors = cloudColors.map { it.copy(alpha = it.alpha * alphaMul) },
                center = center,
                radius = safeRadius
            ),
            radius = safeRadius,
            center = center
        )
    }
}

/**
 * 陽光の中で漂い煌めく光の微粒子（Sun Dust）
 */
private fun DrawScope.drawSunDustParticles(
    particles: List<SunDust>,
    progress: Float,
    tint: Color,
    tiltX: Float = 0f
) {
    particles.forEach { p ->
        val y = ((p.yRatio - progress * p.speed) % 1f + 1f) % 1f * size.height
        val wobble = sin(progress * 6.28f + p.pulseOffset) * 12f
        val x = ((p.xRatio * size.width + wobble + tiltX * 38f) % size.width + size.width) % size.width
        val pulseAlpha = (p.alpha * (0.6f + 0.4f * sin(progress * 8f + p.pulseOffset))).coerceIn(0.1f, 1f)

        drawCircle(
            color = tint.copy(alpha = pulseAlpha),
            radius = p.radius,
            center = Offset(x, y)
        )
    }
}

/**
 * 晴れ時々曇り: 雲の背後に光芒が透ける表現
 */
private fun DrawScope.drawSunBehindClouds(
    sunPulse: Float,
    progress: Float,
    accentColor: Color?,
    isDark: Boolean,
    stars: List<NightStar>,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    if (size.width <= 1f || size.height <= 1f) return
    val safeTiltX = if (tiltX.isFinite()) tiltX else 0f
    val safeTiltY = if (tiltY.isFinite()) tiltY else 0f
    val sunCenter = Offset(size.width * 0.85f + safeTiltX * 24f, size.height * 0.08f + safeTiltY * 16f)
    if (!sunCenter.x.isFinite() || !sunCenter.y.isFinite()) return

    if (isDark) {
        stars.forEach { star ->
            val twinkle = (star.baseAlpha * (0.5f + 0.5f * sin(progress * 4f * PI.toFloat() + star.twinkleOffset))).coerceIn(0.1f, 1f)
            val sx = star.xRatio * size.width + safeTiltX * 14f
            val sy = star.yRatio * size.height + safeTiltY * 8f
            if (sx.isFinite() && sy.isFinite() && star.radius > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = twinkle),
                    radius = star.radius,
                    center = Offset(sx, sy)
                )
            }
        }
        drawVolumetricBeams(sunCenter, progress, Color(0xFF90CAF9), 0.22f, tiltX = safeTiltX)
    } else {
        drawVolumetricBeams(sunCenter, progress, Color.White, 0.75f, tiltX = safeTiltX)
        val sunGlowRadius = (size.width * 0.35f * sunPulse).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color(0xFFFFF9E6).copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = sunCenter,
                radius = sunGlowRadius
            ),
            radius = sunGlowRadius,
            center = sunCenter
        )
    }
}

private enum class PuffLayer {
    BASE,       // 雲底（平らな底部とアンビエントシャドウ）
    CORE,       // 濃密な雲本体コア
    TOP_DOME,   // 上部に盛り上がるモコモコ頭部（シルバーライニング・陽光ハイライト）
    EDGE_WISP   // 外縁のちぎれ綿パフ（有機的なフェザーエッジ）
}

private data class CloudPuffDef(
    val relX: Float,
    val relY: Float,
    val relRadius: Float,
    val layerType: PuffLayer
)

private data class CloudPalette(
    val highlight: Color,
    val core: Color,
    val shadow: Color,
    val baseShadow: Color
)

// 積雲（Cumulus Cloud）を構成する17個の有機的パフ（安定した美しい雲の造形）
private val CUMULUS_PUFFS = listOf(
    // 1. BASE LAYER: 平らな雲底と大気散乱アンビエントシャドウ
    CloudPuffDef(-0.85f,  0.26f, 0.42f, PuffLayer.BASE),
    CloudPuffDef(-0.45f,  0.30f, 0.52f, PuffLayer.BASE),
    CloudPuffDef( 0.00f,  0.32f, 0.58f, PuffLayer.BASE),
    CloudPuffDef( 0.45f,  0.29f, 0.52f, PuffLayer.BASE),
    CloudPuffDef( 0.85f,  0.25f, 0.40f, PuffLayer.BASE),

    // 2. CORE LAYER: 濃密な雲本体
    CloudPuffDef(-0.55f,  0.08f, 0.58f, PuffLayer.CORE),
    CloudPuffDef(-0.15f,  0.02f, 0.68f, PuffLayer.CORE),
    CloudPuffDef( 0.25f,  0.05f, 0.64f, PuffLayer.CORE),
    CloudPuffDef( 0.60f,  0.10f, 0.52f, PuffLayer.CORE),

    // 3. TOP DOME LAYER: 上部に力強く湧き上がるカリフラワードーム
    CloudPuffDef(-0.35f, -0.22f, 0.48f, PuffLayer.TOP_DOME),
    CloudPuffDef( 0.05f, -0.30f, 0.54f, PuffLayer.TOP_DOME),
    CloudPuffDef( 0.40f, -0.18f, 0.44f, PuffLayer.TOP_DOME),
    CloudPuffDef(-0.65f, -0.10f, 0.38f, PuffLayer.TOP_DOME),

    // 4. EDGE WISPS: 雲の輪郭をふんわりぼかす微小パフ
    CloudPuffDef(-1.05f,  0.12f, 0.28f, PuffLayer.EDGE_WISP),
    CloudPuffDef( 1.05f,  0.10f, 0.26f, PuffLayer.EDGE_WISP),
    CloudPuffDef(-0.10f, -0.42f, 0.32f, PuffLayer.EDGE_WISP),
    CloudPuffDef( 0.75f,  0.22f, 0.28f, PuffLayer.EDGE_WISP)
)

// 曇天時に低空を流れるちぎれ雲（Fractus Scud Clouds）
private val FRACTUS_PUFFS = listOf(
    CloudPuffDef(-0.80f,  0.05f, 0.35f, PuffLayer.CORE),
    CloudPuffDef(-0.40f, -0.05f, 0.45f, PuffLayer.CORE),
    CloudPuffDef( 0.05f,  0.02f, 0.48f, PuffLayer.CORE),
    CloudPuffDef( 0.50f, -0.02f, 0.40f, PuffLayer.CORE),
    CloudPuffDef( 0.85f,  0.08f, 0.30f, PuffLayer.CORE),
    CloudPuffDef(-1.05f,  0.10f, 0.22f, PuffLayer.EDGE_WISP),
    CloudPuffDef( 1.05f,  0.05f, 0.20f, PuffLayer.EDGE_WISP)
)

/**
 * 光学的な光散乱と陰影を持つボリュメトリックパフ（雲胞子）を描画
 */
private fun DrawScope.drawVolumetricPuff(
    center: Offset,
    radius: Float,
    highlightColor: Color,
    coreColor: Color,
    shadowColor: Color,
    lightOffsetRatio: Offset,
    alpha: Float
) {
    if (radius <= 1f || alpha <= 0.01f || !radius.isFinite()) return
    if (!center.x.isFinite() || !center.y.isFinite()) return
    val gradCenter = Offset(
        center.x + lightOffsetRatio.x * radius,
        center.y + lightOffsetRatio.y * radius
    )
    if (!gradCenter.x.isFinite() || !gradCenter.y.isFinite()) return
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to highlightColor.copy(alpha = (highlightColor.alpha * alpha).coerceIn(0f, 1f)),
            0.35f to highlightColor.copy(alpha = (highlightColor.alpha * alpha * 0.96f).coerceIn(0f, 1f)),
            0.65f to coreColor.copy(alpha = (coreColor.alpha * alpha * 0.88f).coerceIn(0f, 1f)),
            0.88f to shadowColor.copy(alpha = (shadowColor.alpha * alpha * 0.65f).coerceIn(0f, 1f)),
            1.00f to Color.Transparent,
            center = gradCenter,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * 画面外（見えない位置）でのみ反対側の画面外へラップアラウンドする完全シームレスな連続座標計算
 */
private fun calculateSeamlessCloudCenter(
    screenWidth: Float,
    cloudWidth: Float,
    timeSeconds: Float,
    speedPxPerSec: Float,
    initialOffsetRatio: Float
): Float {
    val totalSpan = screenWidth + cloudWidth
    val rawDistance = (timeSeconds * speedPxPerSec) + (initialOffsetRatio * totalSpan)
    val travelPos = ((rawDistance % totalSpan) + totalSpan) % totalSpan
    return travelPos - (cloudWidth * 0.5f)
}

private fun getFewCloudsPalette(isDark: Boolean): CloudPalette {
    return if (isDark) {
        CloudPalette(
            highlight = Color(0xFF94A3B8).copy(alpha = 0.72f), // 月光シルバーハイライト
            core = Color(0xFF475569).copy(alpha = 0.65f),      // クールスレートコア
            shadow = Color(0xFF1E293B).copy(alpha = 0.60f),    // ナイトスカイシャドウ
            baseShadow = Color(0xFF0F172A).copy(alpha = 0.68f) // 底面ディープネイビー
        )
    } else {
        // 晴れ時々曇り用: 青空の抜け感と白文字の視認性を保つソフトなスカイブルートーン
        CloudPalette(
            highlight = Color(0xFFF1F6FB).copy(alpha = 0.68f), // 眩しすぎない柔らかな光沢
            core = Color(0xFFCADCEC).copy(alpha = 0.58f),      // 青空の散乱光を帯びたソフトスカイグレー
            shadow = Color(0xFF7E9BB8).copy(alpha = 0.54f),    // 大気の爽やかなスカイブルー陰影
            baseShadow = Color(0xFF4B6B8A).copy(alpha = 0.58f) // 平坦雲底の引き締まったアンビエントシャドウ
        )
    }
}

private fun getCloudyPalette(isDark: Boolean): CloudPalette {
    return if (isDark) {
        CloudPalette(
            highlight = Color(0xFF64748B).copy(alpha = 0.85f),
            core = Color(0xFF334155).copy(alpha = 0.88f),
            shadow = Color(0xFF1E293B).copy(alpha = 0.85f),
            baseShadow = Color(0xFF0A101D).copy(alpha = 0.90f)
        )
    } else {
        // 曇天用: 空全体を覆う重厚でリッチな積乱層雲・高積雲パレット
        // 白文字（気温や都市名）に対して自然なコントラスト陰影を与えるスレートシルバー＆ディープスレート
        CloudPalette(
            highlight = Color(0xFFE2E8F0).copy(alpha = 0.90f), // 上面の鈍いシルバーライト
            core = Color(0xFFA0B2C6).copy(alpha = 0.86f),      // 濃密なスレートシルバーコア
            shadow = Color(0xFF64788D).copy(alpha = 0.82f),    // 豊かな曇天大気シャドウ
            baseShadow = Color(0xFF435567).copy(alpha = 0.88f) // 重厚な雲底アンビエントシャドウ
        )
    }
}

/**
 * 安定した美しいフォルムを持つ立体積雲クラスタを描画（うねうね変形のない優雅な雲）
 */
private fun DrawScope.drawRealisticCumulusCluster(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    palette: CloudPalette,
    overallAlpha: Float = 1.0f,
    puffs: List<CloudPuffDef> = CUMULUS_PUFFS,
    accentTint: Color? = null,
    isDark: Boolean = false
) {
    val lightOffset = if (isDark) Offset(0.2f, -0.3f) else Offset(0.25f, -0.35f)
    val baseRadius = width * 0.28f

    // レイヤー順（BASE -> CORE -> TOP_DOME -> EDGE_WISP）で描画して豊かな立体感を形成
    puffs.forEach { puff ->
        val puffRadius = baseRadius * puff.relRadius
        val puffCenter = Offset(
            centerX + (puff.relX * width * 0.5f),
            centerY + (puff.relY * height * 0.5f)
        )

        val (puffHighlight, puffCore, puffShadow) = when (puff.layerType) {
            PuffLayer.BASE -> Triple(palette.core, palette.shadow, palette.baseShadow)
            PuffLayer.CORE -> Triple(palette.highlight, palette.core, palette.shadow)
            PuffLayer.TOP_DOME -> Triple(palette.highlight, palette.highlight, palette.core)
            PuffLayer.EDGE_WISP -> Triple(palette.highlight, palette.core, palette.shadow)
        }

        val puffAlpha = when (puff.layerType) {
            PuffLayer.BASE -> 0.88f * overallAlpha
            PuffLayer.CORE -> 0.95f * overallAlpha
            PuffLayer.TOP_DOME -> 1.00f * overallAlpha
            PuffLayer.EDGE_WISP -> 0.70f * overallAlpha
        }

        drawVolumetricPuff(
            center = puffCenter,
            radius = puffRadius,
            highlightColor = puffHighlight,
            coreColor = puffCore,
            shadowColor = puffShadow,
            lightOffsetRatio = lightOffset,
            alpha = puffAlpha
        )
    }
}

/**
 * 曇天時の全域オーバーキャスト・キャノピー（全天を覆う重厚な雲海デッキ）
 */
private fun DrawScope.drawOvercastAtmosphereCanopy(
    timeSeconds: Float,
    isDark: Boolean,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    if (size.width <= 1f || size.height <= 1f) return
    val safeTiltX = if (tiltX.isFinite()) tiltX else 0f
    val safeTiltY = if (tiltY.isFinite()) tiltY else 0f

    val topColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFF5A6E82).copy(alpha = 0.65f)
    val midColor = if (isDark) Color(0xFF334155).copy(alpha = 0.65f) else Color(0xFF889BB0).copy(alpha = 0.48f)

    val endY = (size.height * 0.52f + safeTiltY * 10f).coerceAtLeast(1f)
    // 上部全体を柔らかく覆うグラデーション
    drawRect(
        brush = Brush.verticalGradient(
            0.0f to topColor,
            0.50f to midColor,
            0.95f to Color.Transparent,
            startY = 0f,
            endY = endY
        ),
        topLeft = Offset(0f, safeTiltY * 5f),
        size = Size(size.width, size.height * 0.52f)
    )

    // キャノピー表面の巨大ソフト雲海（静かに水平移動・重厚な雲海として空を覆う）
    val canopyW = (size.width * 1.05f).coerceAtLeast(1f)
    val canopySpan = size.width + canopyW
    for (i in 0..2) {
        val rawCanopyDist = (timeSeconds * (size.width * 0.007f)) + (i * 0.38f * canopySpan)
        val canopyX = (((rawCanopyDist % canopySpan) + canopySpan) % canopySpan) - (canopyW * 0.5f) + safeTiltX * 12f
        val canopyY = size.height * 0.07f + safeTiltY * 6f
        val canopyRadius = (canopyW * 0.70f).coerceAtLeast(1f)
        if (!canopyX.isFinite() || !canopyY.isFinite()) continue
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isDark) Color(0xFF475569).copy(alpha = 0.52f) else Color(0xFFCBD5E1).copy(alpha = 0.55f),
                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.32f) else Color(0xFF64748B).copy(alpha = 0.38f),
                    Color.Transparent
                ),
                center = Offset(canopyX, canopyY),
                radius = canopyRadius
            ),
            radius = canopyRadius,
            center = Offset(canopyX, canopyY)
        )
    }
}

/**
 * 超リアルなボリュメトリック雲システム
 * 晴れ時々曇りは爽やかな透明感、曇りは圧倒的な重層積雲デッキで各天候の臨場感を完璧に表現
 */
private fun DrawScope.drawRealisticClouds(
    progress: Float,
    isFewClouds: Boolean,
    isDark: Boolean,
    accentColor: Color?,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    // 連続的な単調増加実時間（秒）: ガクつきのない完全シームレスな時間
    val timeSeconds = (SystemClock.uptimeMillis() % 1_000_000_000L) / 1000f

    if (isFewClouds) {
        // --- 晴れ時々曇り (FEW_CLOUDS) ---
        val fewCloudsPalette = getFewCloudsPalette(isDark)

        // 1. 遠景の積雲 (高空・ヘッダー文字上部を静かに通過 - 遠景パララックス)
        val w1 = size.width * 0.56f
        val x1 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = w1,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.007f,
            initialOffsetRatio = 0.10f
        )
        drawRealisticCumulusCluster(
            centerX = x1 + tiltX * 18f,
            centerY = size.height * 0.06f + tiltY * 10f,
            width = w1,
            height = size.height * 0.09f,
            palette = fewCloudsPalette,
            overallAlpha = 0.42f,
            accentTint = accentColor,
            isDark = isDark
        )

        // 2. 中景の積雲 (メイン・気温表示の脇から下へ流れる - 中景パララックス)
        val w2 = size.width * 0.80f
        val x2 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = w2,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.012f,
            initialOffsetRatio = 0.55f
        )
        drawRealisticCumulusCluster(
            centerX = x2 + tiltX * 42f,
            centerY = size.height * 0.20f + tiltY * 22f,
            width = w2,
            height = size.height * 0.14f,
            palette = fewCloudsPalette,
            overallAlpha = 0.48f,
            accentTint = accentColor,
            isDark = isDark
        )

        // 3. 前景の積雲 (近景・気温表示の下側、ウィジェット手前奥に雄大に浮かぶ - 近景ダイナミックパララックス)
        val w3 = size.width * 0.98f
        val x3 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = w3,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.018f,
            initialOffsetRatio = 0.88f
        )
        drawRealisticCumulusCluster(
            centerX = x3 + tiltX * 72f,
            centerY = size.height * 0.30f + tiltY * 36f,
            width = w3,
            height = size.height * 0.16f,
            palette = fewCloudsPalette,
            overallAlpha = 0.55f,
            accentTint = accentColor,
            isDark = isDark
        )
    } else {
        // --- 曇り (CLOUDY) ---
        // 空一面が雲に覆われる圧倒的な重厚雲感（多層積雲デッキ ＋ ちぎれ雲）
        val cloudyPalette = getCloudyPalette(isDark)

        // Layer 1: 最上層の広域オーバーキャスト・キャノピー（全天を覆う重厚な雲海）
        drawOvercastAtmosphereCanopy(timeSeconds, isDark, tiltX, tiltY)

        // Layer 2: 中上層の雄大なローリング積雲デッキ (深度比 1)
        val dw1 = size.width * 1.25f
        val dx1 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = dw1,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.007f,
            initialOffsetRatio = 0.15f
        )
        drawRealisticCumulusCluster(
            centerX = dx1 + tiltX * 25f,
            centerY = size.height * 0.09f + tiltY * 14f,
            width = dw1,
            height = size.height * 0.18f,
            palette = cloudyPalette,
            overallAlpha = 0.85f,
            accentTint = accentColor,
            isDark = isDark
        )

        // Layer 3: 中層の重厚な主積雲デッキ (気温エリアを優雅に横切る濃密な雲塊 - 深度比 2)
        val dw2 = size.width * 1.35f
        val dx2 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = dw2,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.011f,
            initialOffsetRatio = 0.60f
        )
        drawRealisticCumulusCluster(
            centerX = dx2 + tiltX * 48f,
            centerY = size.height * 0.20f + tiltY * 24f,
            width = dw2,
            height = size.height * 0.22f,
            palette = cloudyPalette,
            overallAlpha = 0.90f,
            accentTint = accentColor,
            isDark = isDark
        )

        // Layer 4: 前景・中低層の重厚な低空積雲（ウィジェット手前奥のダイナミックな雲 - 深度比 3)
        val dw3 = size.width * 1.15f
        val dx3 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = dw3,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.016f,
            initialOffsetRatio = 0.85f
        )
        drawRealisticCumulusCluster(
            centerX = dx3 + tiltX * 72f,
            centerY = size.height * 0.32f + tiltY * 36f,
            width = dw3,
            height = size.height * 0.18f,
            palette = cloudyPalette,
            overallAlpha = 0.86f,
            accentTint = accentColor,
            isDark = isDark
        )

        // Layer 5: 前景・下層を流れるちぎれ雲（Fractus Scud Clouds 1 - 深度比 4)
        val sw1 = size.width * 0.72f
        val sx1 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = sw1,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.024f,
            initialOffsetRatio = 0.25f
        )
        drawRealisticCumulusCluster(
            centerX = sx1 + tiltX * 98f,
            centerY = size.height * 0.28f + tiltY * 48f,
            width = sw1,
            height = size.height * 0.11f,
            palette = cloudyPalette,
            overallAlpha = 0.78f,
            puffs = FRACTUS_PUFFS,
            accentTint = accentColor,
            isDark = isDark
        )

        // Layer 6: 最下層を速く流れるちぎれ雲（Fractus Scud Clouds 2 - 最前面・深度比 5)
        val sw2 = size.width * 0.85f
        val sx2 = calculateSeamlessCloudCenter(
            screenWidth = size.width,
            cloudWidth = sw2,
            timeSeconds = timeSeconds,
            speedPxPerSec = size.width * 0.032f,
            initialOffsetRatio = 0.72f
        )
        drawRealisticCumulusCluster(
            centerX = sx2 + tiltX * 128f,
            centerY = size.height * 0.38f + tiltY * 62f,
            width = sw2,
            height = size.height * 0.13f,
            palette = cloudyPalette,
            overallAlpha = 0.75f,
            puffs = FRACTUS_PUFFS,
            accentTint = accentColor,
            isDark = isDark
        )
    }
}

/**
 * リアルな雨筋 ＋ タイル底面に着弾して広がる同心円状水滴波紋
 */
private fun DrawScope.drawRealisticRain(
    drops: List<Raindrop>,
    ripples: List<WaterRipple>,
    progress: Float,
    isHeavy: Boolean,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    val dynamicSlant = (-0.22f + tiltX * 0.65f).coerceIn(-1.25f, 1.25f)
    val rainColor = if (isHeavy) Color(0xA0B0BEC5) else Color(0x9081D4FA)
    val speedMult = (1f - tiltY * 0.20f).coerceIn(0.6f, 1.6f)
    val lengthMult = (1f - tiltY * 0.20f).coerceIn(0.6f, 1.4f)

    // 1. Rain streaks
    drops.forEach { drop ->
        val y = ((drop.yRatio + progress * drop.speed * speedMult * (if (isHeavy) 3.5f else 2.8f)) % 1f) * size.height
        val x = ((drop.xRatio * size.width + y * dynamicSlant) % size.width + size.width) % size.width
        val len = drop.length * lengthMult
        val endX = x + dynamicSlant * len
        val endY = y + len

        drawLine(
            color = rainColor.copy(alpha = drop.alpha),
            start = Offset(x, y),
            end = Offset(endX, endY),
            strokeWidth = drop.strokeWidth
        )
    }

    // 2. Floor ripples (雨粒の着弾波紋)
    ripples.forEach { ripple ->
        val cycle = ((progress * 2.5f + ripple.phaseOffset) % 1f)
        val currentRadius = ripple.maxRadius * cycle
        val currentAlpha = (ripple.alpha * (1f - cycle)).coerceIn(0f, 1f)

        drawOval(
            color = Color.White.copy(alpha = currentAlpha),
            topLeft = Offset(ripple.xRatio * size.width - currentRadius + tiltX * 10f, ripple.yRatio * size.height - currentRadius * 0.4f + tiltY * 6f),
            size = Size(currentRadius * 2f, currentRadius * 0.8f),
            style = Stroke(width = 1.5f)
        )
    }
}

/**
 * 雷雨: 枝分かれするプロシージャル稲妻 ＋ 環境フラッシュ ＋ 豪雨
 */
private fun DrawScope.drawRealisticThunderstorm(
    drops: List<Raindrop>,
    ripples: List<WaterRipple>,
    progress: Float,
    lightningClock: Float,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    // 1. Rain & Ripples
    drawRealisticRain(drops, ripples, progress, isHeavy = true, tiltX = tiltX, tiltY = tiltY)

    // 2. Lightning bolt and flash trigger
    if (lightningClock in 0.80f..0.92f) {
        val flashNorm = (lightningClock - 0.80f) / 0.12f
        val flashAlpha = (sin(flashNorm * PI.toFloat() * 3f).coerceIn(0f, 1f) * 0.35f)

        // Ambient screen flash
        drawRect(
            color = Color(0xFFD1C4E9).copy(alpha = flashAlpha),
            size = size
        )

        // Forked Lightning Path with tilt parallax
        if (lightningClock in 0.82f..0.88f) {
            drawLightningBolt(startX = size.width * 0.65f + tiltX * 22f, startY = tiltY * 12f, depth = 0)
        }
    }
}

/**
 * 再帰的にジグザグと枝分かれする稲妻を描画
 */
private fun DrawScope.drawLightningBolt(startX: Float, startY: Float, depth: Int) {
    val boltPath = Path()
    boltPath.moveTo(startX, startY)

    var currentX = startX
    var currentY = startY
    val targetY = size.height * 0.65f
    val random = Random(42 + depth)

    while (currentY < targetY) {
        val segLength = 25f + random.nextFloat() * 35f
        currentY += segLength
        currentX += (random.nextFloat() - 0.5f) * 45f
        boltPath.lineTo(currentX, currentY)
    }

    // Outer glow
    drawPath(
        path = boltPath,
        color = Color(0xFFB388FF).copy(alpha = 0.55f),
        style = Stroke(width = 6f)
    )
    // Core white bolt
    drawPath(
        path = boltPath,
        color = Color.White,
        style = Stroke(width = 2.5f)
    )
}

/**
 * サイン波物理によるふわりと舞い落ちる3層の雪片
 */
private fun DrawScope.drawPhysicsSnow(
    snowflakes: List<Snowflake>,
    progress: Float,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    val speedMult = (1f - tiltY * 0.25f).coerceIn(0.5f, 1.8f)
    snowflakes.forEach { flake ->
        val y = ((flake.yRatio + progress * flake.speed * 1.6f * speedMult) % 1f) * size.height
        val wobble = sin(progress * PI.toFloat() * flake.wobbleSpeed + flake.xRatio * 10f) * flake.wobbleAmplitude
        val x = ((flake.xRatio * size.width + wobble + tiltX * 60f) % size.width + size.width) % size.width

        // Outer soft snowflake aura
        drawCircle(
            color = Color.White.copy(alpha = flake.alpha * 0.5f),
            radius = flake.radius * 1.5f,
            center = Offset(x, y)
        )
        // Solid crystal core
        drawCircle(
            color = Color.White.copy(alpha = flake.alpha),
            radius = flake.radius,
            center = Offset(x, y)
        )
    }
}

/**
 * 流れる多層の霧ヴェール
 */
private fun DrawScope.drawFlowingFog(
    progress: Float,
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    if (size.width <= 1f || size.height <= 1f) return
    val safeTiltX = if (tiltX.isFinite()) tiltX else 0f
    val safeTiltY = if (tiltY.isFinite()) tiltY else 0f

    for (i in 0..2) {
        val yBase = size.height * (0.25f + i * 0.22f)
        val waveOffset = sin(progress * 2f * PI.toFloat() + i * 1.5f) * 18f
        val fogAlpha = 0.15f + i * 0.08f
        val xShift = safeTiltX * (i + 1) * 32f
        val yShift = safeTiltY * (i + 1) * 16f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x35FFFFFF).copy(alpha = fogAlpha),
                    Color(0x20FFFFFF).copy(alpha = fogAlpha * 0.7f),
                    Color.Transparent
                ),
                startY = yBase + waveOffset - 40f + yShift,
                endY = yBase + waveOffset + 90f + yShift
            ),
            topLeft = Offset(xShift, yBase + waveOffset - 40f + yShift),
            size = Size(size.width, 130f)
        )
    }
}

/**
 * グラスモーフィズム特有の斜め反射光沢（上部エッジの洗練された光）
 */
private fun DrawScope.drawGlassReflection(
    tiltX: Float = 0f,
    tiltY: Float = 0f
) {
    if (size.width <= 1f || size.height <= 1f) return
    val safeTiltX = if (tiltX.isFinite()) tiltX else 0f
    val safeTiltY = if (tiltY.isFinite()) tiltY else 0f
    val reflectionHeight = (size.height * 0.45f).coerceAtLeast(1f)
    val shiftX = safeTiltX * size.width * 0.16f
    val shiftY = safeTiltY * reflectionHeight * 0.20f
    val reflectionPath = Path().apply {
        moveTo(shiftX, shiftY)
        lineTo(size.width + shiftX, shiftY)
        lineTo(size.width + shiftX, reflectionHeight * 0.3f + shiftY)
        lineTo(shiftX, reflectionHeight + shiftY)
        close()
    }

    drawPath(
        path = reflectionPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (0.18f + kotlin.math.abs(safeTiltX) * 0.08f).coerceAtMost(0.35f)),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ),
            startY = shiftY,
            endY = reflectionHeight + shiftY
        )
    )
}
