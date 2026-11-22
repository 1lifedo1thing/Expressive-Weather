package io.github.nemynew.expressiveweather.ui.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.DisplayMetrics
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.WeatherCondition
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Widget 用デバイス連携 Material You カラーセット
 */
data class WidgetMaterialYouPalette(
    val cardTopColor: Int,
    val cardBottomColor: Int,
    val trayColor: Int,
    val pillColor: Int,
    val borderColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val accentColor: Int,
    val shapeContainerColor: Int,
    val shapeTextColor: Int,
    val wallpaperPrimaryColor: Int,
    val wallpaperSecondaryColor: Int,
    val isLightSurface: Boolean,
    val primaryContainerColor: Int = pillColor,
    val onPrimaryContainerColor: Int = primaryTextColor
)

/**
 * Widget 用 Canvas レンダラー
 * - デバイスの壁紙／システムテーマ（Material You）へ動的追従するパレット解決
 * - 滑らかな天候グラデーション・丸角追従ガラスボーダー・トレイ描画
 * - 多色レイヤー表現の Expressive 天気アイコン描画
 * - 60分降水スパークライン & 週間気温レンジバー描画
 */
object WidgetCanvasRenderer {

    /**
     * API 31+ では OS の `dynamicDarkColorScheme` / `dynamicLightColorScheme` から
     * 壁紙・システムカラーパレットに完全追従する実カラーを取得する。
     */
    fun resolveMaterialYouPalette(
        context: Context,
        isSystemDark: Boolean
    ): WidgetMaterialYouPalette {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val scheme = if (isSystemDark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
                val surfaceHigh = scheme.surfaceContainerHigh.toArgb()
                val surfaceBase = scheme.surfaceContainer.toArgb()
                val secondaryContainer = scheme.secondaryContainer.toArgb()
                val primaryContainer = scheme.primaryContainer.toArgb()
                val onSurface = scheme.onSurface.toArgb()
                val onSurfaceVariant = scheme.onSurfaceVariant.toArgb()
                val primary = scheme.primary.toArgb()
                val outlineVariant = scheme.outlineVariant.toArgb()

                val cardTop = if (isSystemDark) {
                    blendColors(surfaceHigh, primaryContainer, 0.14f)
                } else {
                    blendColors(surfaceHigh, primaryContainer, 0.22f)
                }
                val cardBottom = if (isSystemDark) {
                    blendColors(surfaceBase, secondaryContainer, 0.10f)
                } else {
                    blendColors(surfaceBase, secondaryContainer, 0.16f)
                }
                val tray = if (isSystemDark) {
                    blendColors(scheme.surfaceContainerHighest.toArgb(), secondaryContainer, 0.35f)
                } else {
                    blendColors(Color.WHITE, secondaryContainer, 0.42f)
                }
                val pill = if (isSystemDark) {
                    blendColors(secondaryContainer, primaryContainer, 0.25f)
                } else {
                    blendColors(secondaryContainer, primaryContainer, 0.35f)
                }
                val shapeContainer = if (isSystemDark) {
                    blendColors(scheme.surfaceContainerHighest.toArgb(), secondaryContainer, 0.55f)
                } else {
                    blendColors(secondaryContainer, primaryContainer, 0.30f)
                }
                val shapeText = if (isSystemDark) {
                    scheme.onSecondaryContainer.toArgb()
                } else {
                    scheme.onSecondaryContainer.toArgb()
                }

                val wallpaperColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    runCatching {
                        WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    }.getOrNull()
                } else null
                val wpPrimary = wallpaperColors?.primaryColor?.toArgb() ?: primaryContainer
                val wpSecondary = wallpaperColors?.secondaryColor?.toArgb() ?: secondaryContainer

                return WidgetMaterialYouPalette(
                    cardTopColor = cardTop,
                    cardBottomColor = cardBottom,
                    trayColor = tray,
                    pillColor = pill,
                    borderColor = withAlpha(outlineVariant, if (isSystemDark) 90 else 110),
                    primaryTextColor = onSurface,
                    secondaryTextColor = onSurfaceVariant,
                    accentColor = primary,
                    shapeContainerColor = shapeContainer,
                    shapeTextColor = shapeText,
                    wallpaperPrimaryColor = wpPrimary,
                    wallpaperSecondaryColor = wpSecondary,
                    isLightSurface = !isSystemDark,
                    primaryContainerColor = primaryContainer,
                    onPrimaryContainerColor = scheme.onPrimaryContainer.toArgb()
                )
            } catch (_: Throwable) {
            }
        }

        val fallbackSurface = ContextCompat.getColor(context, R.color.widget_my_surface)
        val fallbackTray = ContextCompat.getColor(context, R.color.widget_my_surface_variant)
        val fallbackPrimary = ContextCompat.getColor(context, R.color.widget_my_text_primary)
        val fallbackSecondary = ContextCompat.getColor(context, R.color.widget_my_text_secondary)
        val fallbackAccent = ContextCompat.getColor(context, R.color.widget_my_accent)
        return WidgetMaterialYouPalette(
            cardTopColor = fallbackSurface,
            cardBottomColor = fallbackSurface,
            trayColor = fallbackTray,
            pillColor = fallbackTray,
            borderColor = withAlpha(fallbackSecondary, 70),
            primaryTextColor = fallbackPrimary,
            secondaryTextColor = fallbackSecondary,
            accentColor = fallbackAccent,
            shapeContainerColor = if (isSystemDark) Color.rgb(44, 38, 56) else fallbackSurface,
            shapeTextColor = fallbackPrimary,
            wallpaperPrimaryColor = fallbackAccent,
            wallpaperSecondaryColor = fallbackTray,
            isLightSurface = !isSystemDark
        )
    }

    @Volatile
    private var cachedGoogleSansBase: Typeface? = null

    private fun getGoogleSansTypeface(context: Context): Typeface {
        cachedGoogleSansBase?.let { return it }
        val resolved = try {
            ResourcesCompat.getFont(context, R.font.google_sans_flex)
        } catch (_: Throwable) {
            null
        } ?: Typeface.SANS_SERIF
        cachedGoogleSansBase = resolved
        return resolved
    }

    /**
     * 四角ウィジェット上で Samsung One UI 等のシステムフォント上書きを受けず、
     * 確実に Google Sans Flex（wght + ROND=100）を描画するための軽量テキストビットマップ生成。
     */
    fun renderGoogleSansTextBitmap(
        context: Context,
        text: String,
        textSizeSp: Float,
        colorArgb: Int,
        fontWeight: Int = 500,
        roundness: Int = 100,
        letterSpacing: Float = 0f,
        maxWidthDp: Float = 0f,
        densityDpi: Int = DisplayMetrics.DENSITY_XHIGH,
        softShadow: Boolean = false
    ): Bitmap {
        val safeText = text.ifEmpty { " " }
        val scale = densityDpi / 160f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            textSize = textSizeSp * scale
            typeface = getGoogleSansTypeface(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                fontVariationSettings = "'wght' $fontWeight, 'ROND' $roundness"
            }
            this.letterSpacing = letterSpacing
            if (softShadow) {
                setShadowLayer(3f * scale, 0f, 1f * scale, Color.argb(115, 0, 0, 0))
            }
        }

        val finalText = if (maxWidthDp > 0f) {
            TextUtils.ellipsize(
                safeText,
                paint,
                maxWidthDp * scale,
                TextUtils.TruncateAt.END
            ).toString()
        } else {
            safeText
        }

        val metrics = paint.fontMetrics
        val padH = ceil(2f * scale).toInt()
        val padV = ceil(1.5f * scale).toInt()
        val measuredW = ceil(paint.measureText(finalText)).toInt().coerceAtLeast(2)
        val measuredH = ceil(metrics.descent - metrics.ascent).toInt().coerceAtLeast(2)
        val maxAllowedPx = if (maxWidthDp > 0f) ceil(maxWidthDp * scale).toInt() else measuredW
        val bmpW = (measuredW + padH * 2).coerceAtMost(maxAllowedPx + padH * 2).coerceAtLeast(4)
        val bmpH = (measuredH + padV * 2).coerceAtLeast(4)

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        bitmap.density = densityDpi
        val canvas = Canvas(bitmap)
        val baseline = padV - metrics.ascent
        canvas.drawText(finalText, padH.toFloat(), baseline, paint)
        return bitmap
    }

    /**
     * 四角型ウィジェット全体の背景ビットマップを描画（全スタイル対応・枠線なし）
     */
    fun renderBackgroundBitmap(
        context: Context,
        sizeTier: WidgetSizeTier,
        style: WidgetStyle,
        condition: WeatherCondition,
        isDay: Boolean,
        isSystemDark: Boolean,
        myPalette: WidgetMaterialYouPalette,
        glassOpacity: Float = 0.72f
    ): Bitmap {
        val (width, height) = when (sizeTier) {
            WidgetSizeTier.COMPACT_WIDE -> 160 to 68
            WidgetSizeTier.WIDE_HOURLY -> 220 to 64
            WidgetSizeTier.COMPACT -> 128 to 128
            WidgetSizeTier.MEDIUM -> 160 to 80
            WidgetSizeTier.LARGE -> 144 to 144
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (style) {
            WidgetStyle.WEATHER_EXPRESSIVE -> {
                val colors = resolveSkyGradientColors(condition, isDay)
                paint.shader = LinearGradient(
                    0f, 0f, w * 0.30f, h,
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, paint)

                val glowColor = if (isDay) {
                    when (condition) {
                        WeatherCondition.CLEAR, WeatherCondition.FEW_CLOUDS -> Color.argb(90, 255, 236, 179)
                        WeatherCondition.THUNDERSTORM -> Color.argb(65, 179, 136, 255)
                        else -> Color.argb(40, 255, 255, 255)
                    }
                } else {
                    when (condition) {
                        WeatherCondition.CLEAR, WeatherCondition.FEW_CLOUDS -> Color.argb(72, 130, 177, 255)
                        WeatherCondition.THUNDERSTORM -> Color.argb(75, 149, 117, 205)
                        else -> Color.argb(40, 144, 202, 249)
                    }
                }
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.84f, h * 0.14f, max(w, h) * 0.72f,
                        glowColor,
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, glowPaint)
            }

            WidgetStyle.MATERIAL_YOU -> {
                canvas.drawColor(myPalette.cardTopColor)
            }

            WidgetStyle.FROSTED_GLASS -> {
                val opacity = glassOpacity.coerceIn(0.15f, 0.95f)
                val alphaByte = (opacity * 255f).toInt().coerceIn(32, 245)
                canvas.drawColor(withAlpha(myPalette.cardTopColor, alphaByte))
            }
        }

        return bitmap
    }

    /**
     * 軽量ボックスブラーを複数パス適用して滑らかなガウス近似ぼかしを生成
     */
    private fun fastBoxBlurBitmap(source: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return source
        val w = source.width
        val h = source.height
        val pix = IntArray(w * h)
        source.getPixels(pix, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        val window = radius * 2 + 1

        repeat(2) {
            // Horizontal pass
            for (y in 0 until h) {
                val rowOff = y * w
                var sumA = 0
                var sumR = 0
                var sumG = 0
                var sumB = 0
                for (dx in -radius..radius) {
                    val p = pix[rowOff + dx.coerceIn(0, w - 1)]
                    sumA += (p ushr 24) and 0xFF
                    sumR += (p ushr 16) and 0xFF
                    sumG += (p ushr 8) and 0xFF
                    sumB += p and 0xFF
                }
                for (x in 0 until w) {
                    tmp[rowOff + x] = ((sumA / window) shl 24) or
                        ((sumR / window) shl 16) or
                        ((sumG / window) shl 8) or
                        (sumB / window)
                    val outIdx = (x - radius).coerceIn(0, w - 1)
                    val inIdx = (x + radius + 1).coerceIn(0, w - 1)
                    val pOut = pix[rowOff + outIdx]
                    val pIn = pix[rowOff + inIdx]
                    sumA += ((pIn ushr 24) and 0xFF) - ((pOut ushr 24) and 0xFF)
                    sumR += ((pIn ushr 16) and 0xFF) - ((pOut ushr 16) and 0xFF)
                    sumG += ((pIn ushr 8) and 0xFF) - ((pOut ushr 8) and 0xFF)
                    sumB += (pIn and 0xFF) - (pOut and 0xFF)
                }
            }
            // Vertical pass
            for (x in 0 until w) {
                var sumA = 0
                var sumR = 0
                var sumG = 0
                var sumB = 0
                for (dy in -radius..radius) {
                    val p = tmp[dy.coerceIn(0, h - 1) * w + x]
                    sumA += (p ushr 24) and 0xFF
                    sumR += (p ushr 16) and 0xFF
                    sumG += (p ushr 8) and 0xFF
                    sumB += p and 0xFF
                }
                for (y in 0 until h) {
                    pix[y * w + x] = ((sumA / window) shl 24) or
                        ((sumR / window) shl 16) or
                        ((sumG / window) shl 8) or
                        (sumB / window)
                    val outIdx = (y - radius).coerceIn(0, h - 1)
                    val inIdx = (y + radius + 1).coerceIn(0, h - 1)
                    val pOut = tmp[outIdx * w + x]
                    val pIn = tmp[inIdx * w + x]
                    sumA += ((pIn ushr 24) and 0xFF) - ((pOut ushr 24) and 0xFF)
                    sumR += ((pIn ushr 16) and 0xFF) - ((pOut ushr 16) and 0xFF)
                    sumG += ((pIn ushr 8) and 0xFF) - ((pOut ushr 8) and 0xFF)
                    sumB += (pIn and 0xFF) - (pOut and 0xFF)
                }
            }
        }
        source.setPixels(pix, 0, w, 0, 0, w, h)
        return source
    }

    /**
     * トレイ／カプセル用の丸角カード背景ビットマップ（Material You 動的カラー対応）
     */
    fun renderRoundedSurfaceBitmap(
        widthPx: Int,
        heightPx: Int,
        fillColorArgb: Int,
        borderColorArgb: Int = Color.TRANSPARENT,
        cornerRadiusPx: Float = 28f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColorArgb }
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        if (borderColorArgb != Color.TRANSPARENT) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = borderColorArgb
            }
            val insetRect = RectF(0.75f, 0.75f, widthPx - 0.75f, heightPx - 0.75f)
            canvas.drawRoundRect(insetRect, cornerRadiusPx, cornerRadiusPx, stroke)
        }
        return bitmap
    }

    /**
     * 立体感のある多色レイヤー Material Expressive 天気アイコンをビットマップ生成
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun renderExpressiveWeatherIconBitmap(
        condition: WeatherCondition,
        isDay: Boolean,
        isLightSurface: Boolean,
        sizePx: Int = 96
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = sizePx.toFloat()
        val cx = w * 0.5f
        val cy = w * 0.5f

        val sunTop = Color.rgb(255, 213, 79)
        val sunBottom = Color.rgb(255, 160, 0)
        val cloudLightTop = Color.rgb(255, 255, 255)
        val cloudLightBottom = if (isLightSurface) Color.rgb(212, 224, 238) else Color.rgb(218, 230, 244)
        val cloudBackColor = if (isLightSurface) Color.rgb(166, 184, 206) else Color.rgb(176, 194, 216)
        val stormCloudTop = if (isLightSurface) Color.rgb(144, 164, 184) else Color.rgb(198, 210, 226)
        val stormCloudBottom = if (isLightSurface) Color.rgb(96, 120, 144) else Color.rgb(138, 158, 180)
        val rainDropTop = Color.rgb(129, 212, 250)
        val rainDropBottom = if (isLightSurface) Color.rgb(2, 136, 209) else Color.rgb(41, 182, 246)
        val moonTop = Color.rgb(255, 236, 179)
        val moonBottom = Color.rgb(255, 193, 7)
        val snowColor = if (isLightSurface) Color.rgb(3, 169, 244) else Color.rgb(179, 229, 252)

        when (condition) {
            WeatherCondition.CLEAR -> {
                if (isDay) {
                    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            cx, cy, w * 0.48f,
                            Color.argb(90, 255, 213, 79),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(cx, cy, w * 0.48f, halo)

                    val sunPath = iconPolygonPath(MaterialShapes.Cookie9Sided, cx, cy, w * 0.72f)
                    val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(cx, cy - w * 0.36f, cx, cy + w * 0.36f, sunTop, sunBottom, Shader.TileMode.CLAMP)
                    }
                    canvas.drawPath(sunPath, sunPaint)
                } else {
                    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            cx, cy, w * 0.46f,
                            Color.argb(65, 255, 224, 130),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(cx, cy, w * 0.46f, halo)
                    val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(cx, cy - w * 0.3f, cx, cy + w * 0.3f, moonTop, moonBottom, Shader.TileMode.CLAMP)
                    }
                    drawCrescentMoon(canvas, cx - w * 0.04f, cy + w * 0.03f, w * 0.31f, moonPaint)
                    val starPath = iconPolygonPath(MaterialShapes.Cookie4Sided, cx + w * 0.23f, cy - w * 0.21f, w * 0.17f)
                    canvas.drawPath(starPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = moonTop })
                }
            }

            WeatherCondition.FEW_CLOUDS -> {
                if (isDay) {
                    val sunPath = iconPolygonPath(MaterialShapes.Cookie9Sided, cx - w * 0.14f, cy - w * 0.14f, w * 0.50f)
                    val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(cx - w * 0.14f, cy - w * 0.38f, cx - w * 0.14f, cy + w * 0.12f, sunTop, sunBottom, Shader.TileMode.CLAMP)
                    }
                    canvas.drawPath(sunPath, sunPaint)
                } else {
                    val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(cx - w * 0.16f, cy - w * 0.3f, cx - w * 0.16f, cy + w * 0.1f, moonTop, moonBottom, Shader.TileMode.CLAMP)
                    }
                    drawCrescentMoon(canvas, cx - w * 0.14f, cy - w * 0.13f, w * 0.21f, moonPaint)
                }
                val cloudPath = iconPolygonPath(MaterialShapes.Puffy, cx + w * 0.07f, cy + w * 0.11f, w * 0.68f, scaleY = 0.72f)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy - w * 0.12f, cx, cy + w * 0.35f, cloudLightTop, cloudLightBottom, Shader.TileMode.CLAMP)
                }
                canvas.drawPath(cloudPath, cloudPaint)
            }

            WeatherCondition.CLOUDY -> {
                val backCloud = iconPolygonPath(MaterialShapes.Puffy, cx - w * 0.09f, cy - w * 0.08f, w * 0.64f, scaleY = 0.72f)
                canvas.drawPath(backCloud, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cloudBackColor })
                val frontCloud = iconPolygonPath(MaterialShapes.Puffy, cx + w * 0.06f, cy + w * 0.08f, w * 0.72f, scaleY = 0.74f)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy - w * 0.16f, cx, cy + w * 0.32f, cloudLightTop, cloudLightBottom, Shader.TileMode.CLAMP)
                }
                canvas.drawPath(frontCloud, cloudPaint)
            }

            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> {
                val cloud = iconPolygonPath(MaterialShapes.Puffy, cx, cy - w * 0.10f, w * 0.76f, scaleY = 0.70f)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy - w * 0.36f, cx, cy + w * 0.16f, stormCloudTop, stormCloudBottom, Shader.TileMode.CLAMP)
                }
                canvas.drawPath(cloud, cloudPaint)
                val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy + w * 0.15f, cx, cy + w * 0.42f, rainDropTop, rainDropBottom, Shader.TileMode.CLAMP)
                }
                val offsets = if (condition == WeatherCondition.HEAVY_RAIN) {
                    listOf(-0.20f, -0.01f, 0.18f)
                } else {
                    listOf(-0.12f, 0.12f)
                }
                offsets.forEach { dx ->
                    drawSlantedDrop(canvas, cx + w * dx, cy + w * 0.28f, w * 0.085f, w * 0.21f, dropPaint)
                }
            }

            WeatherCondition.THUNDERSTORM -> {
                val cloud = iconPolygonPath(MaterialShapes.Puffy, cx, cy - w * 0.12f, w * 0.76f, scaleY = 0.68f)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy - w * 0.36f, cx, cy + w * 0.12f, stormCloudTop, stormCloudBottom, Shader.TileMode.CLAMP)
                }
                canvas.drawPath(cloud, cloudPaint)
                val bolt = Path().apply {
                    val bx = cx - w * 0.01f
                    val by = cy + w * 0.04f
                    val s = w * 0.34f
                    moveTo(bx + s * 0.22f, by - s * 0.22f)
                    lineTo(bx - s * 0.30f, by + s * 0.32f)
                    lineTo(bx + s * 0.04f, by + s * 0.32f)
                    lineTo(bx - s * 0.18f, by + s * 0.96f)
                    lineTo(bx + s * 0.42f, by + s * 0.14f)
                    lineTo(bx + s * 0.06f, by + s * 0.14f)
                    close()
                }
                val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy, cx, cy + w * 0.4f, sunTop, sunBottom, Shader.TileMode.CLAMP)
                }
                canvas.drawPath(bolt, boltPaint)
            }

            WeatherCondition.SNOW -> {
                val cloud = iconPolygonPath(MaterialShapes.Puffy, cx, cy - w * 0.11f, w * 0.74f, scaleY = 0.68f)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy - w * 0.36f, cx, cy + w * 0.14f, cloudLightTop, cloudLightBottom, Shader.TileMode.CLAMP)
                }
                canvas.drawPath(cloud, cloudPaint)
                val crystalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = snowColor }
                listOf(-0.18f to 0.27f, 0.02f to 0.34f, 0.20f to 0.25f).forEach { (dx, dy) ->
                    val flake = iconPolygonPath(MaterialShapes.Flower, cx + w * dx, cy + w * dy, w * 0.15f)
                    canvas.drawPath(flake, crystalPaint)
                }
            }

            WeatherCondition.FOG -> {
                val cloud = iconPolygonPath(MaterialShapes.Puffy, cx, cy - w * 0.09f, w * 0.72f, scaleY = 0.66f)
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx, cy - w * 0.34f, cx, cy + w * 0.14f, cloudLightTop, cloudBackColor, Shader.TileMode.CLAMP)
                }
                canvas.drawPath(cloud, cloudPaint)
                val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cloudBackColor }
                val barH = w * 0.085f
                val r = barH / 2f
                listOf(0.21f to 0.66f, 0.34f to 0.50f).forEach { (dy, wRatio) ->
                    val halfW = w * wRatio * 0.5f
                    val top = cy + w * dy - barH / 2f
                    canvas.drawRoundRect(RectF(cx - halfW, top, cx + halfW, top + barH), r, r, mistPaint)
                }
            }
        }
        return bitmap
    }

    /**
     * 60分雨量スパークライン用の軽量ビットマップ（例: 240x40px）
     */
    fun renderRainSparklineBitmap(
        points: List<Double>,
        barColorArgb: Int = Color.argb(235, 129, 212, 250),
        trackColorArgb: Int = Color.argb(55, 255, 255, 255)
    ): Bitmap {
        val width = 240
        val height = 40
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val sampled = if (points.isEmpty()) {
            listOf(0.0)
        } else if (points.size > 16) {
            val step = points.size.toFloat() / 16f
            (0 until 16).map { idx -> points[min((idx * step).toInt(), points.lastIndex)] }
        } else {
            points
        }

        val maxIntensity = max(2.0, sampled.maxOrNull() ?: 2.0)
        val count = sampled.size
        val gap = 4f
        val barWidth = max(4f, (width - gap * (count - 1)) / count.toFloat())
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColorArgb }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColorArgb }
        val rect = RectF()
        val radius = barWidth * 0.45f

        for (i in 0 until count) {
            val left = i * (barWidth + gap)
            val right = left + barWidth
            rect.set(left, 2f, right, height.toFloat())
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            val value = sampled[i].coerceAtLeast(0.0)
            if (value > 0.02) {
                val ratio = (value / maxIntensity).toFloat().coerceIn(0.18f, 1.0f)
                val barTop = height - (height - 4f) * ratio
                rect.set(left, barTop, right, height.toFloat())
                canvas.drawRoundRect(rect, radius, radius, barPaint)
            }
        }
        return bitmap
    }

    /**
     * 週間予報の最低・最高気温レンジバー（1:1 比率の角丸ライン表現・横引き伸ばし防止）
     */
    fun renderTempRangeBarBitmap(
        dayMin: Int,
        dayMax: Int,
        weekMin: Int,
        weekMax: Int,
        trackColorArgb: Int = Color.argb(64, 255, 255, 255),
        pillColorArgb: Int = Color.rgb(228, 234, 240)
    ): Bitmap {
        val width = 560
        val height = 12
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val span = max(1, weekMax - weekMin).toFloat()
        val startFrac = ((dayMin - weekMin) / span).coerceIn(0f, 0.90f)
        val endFrac = ((dayMax - weekMin) / span).coerceIn(startFrac + 0.10f, 1f)

        val cy = height / 2f
        val trackStroke = 4.5f
        val lineStroke = 9f
        val trackCap = trackStroke / 2f
        val lineCap = lineStroke / 2f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColorArgb
            style = Paint.Style.STROKE
            strokeWidth = trackStroke
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(trackCap + 1f, cy, width - trackCap - 1f, cy, trackPaint)

        val usableSpan = width - lineStroke - 2f
        val lineStartX = lineCap + 1f + usableSpan * startFrac
        val lineEndX = max(lineStartX + 4f, lineCap + 1f + usableSpan * endFrac)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pillColorArgb
            style = Paint.Style.STROKE
            strokeWidth = lineStroke
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(lineStartX, cy, lineEndX, cy, linePaint)
        return bitmap
    }

    /**
     * アプリ本体の WeatherIconHelper と完全統一したアイコンカラーを返す
     */
    fun getConditionIconTint(condition: WeatherCondition, isDay: Boolean, useDarkInk: Boolean): Int {
        if (useDarkInk) {
            return when (condition) {
                WeatherCondition.CLEAR -> if (isDay) Color.rgb(255, 160, 0) else Color.rgb(92, 107, 192)
                WeatherCondition.FEW_CLOUDS -> Color.rgb(255, 179, 0)
                WeatherCondition.CLOUDY -> Color.rgb(120, 144, 156)
                WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Color.rgb(2, 136, 209)
                WeatherCondition.THUNDERSTORM -> Color.rgb(126, 87, 194)
                WeatherCondition.SNOW -> Color.rgb(79, 195, 247)
                WeatherCondition.FOG -> Color.rgb(144, 164, 174)
            }
        }
        return when (condition) {
            WeatherCondition.CLEAR -> if (isDay) Color.rgb(255, 213, 79) else Color.rgb(255, 236, 179)
            WeatherCondition.FEW_CLOUDS -> Color.rgb(255, 213, 79)
            WeatherCondition.CLOUDY -> Color.rgb(224, 234, 244)
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Color.rgb(79, 195, 247)
            WeatherCondition.THUNDERSTORM -> Color.rgb(179, 157, 219)
            WeatherCondition.SNOW -> Color.rgb(129, 212, 250)
            WeatherCondition.FOG -> Color.rgb(207, 216, 220)
        }
    }

    fun blendColors(colorA: Int, colorB: Int, ratioB: Float): Int {
        val t = ratioB.coerceIn(0f, 1f)
        val inv = 1f - t
        val a = (Color.alpha(colorA) * inv + Color.alpha(colorB) * t).toInt().coerceIn(0, 255)
        val r = (Color.red(colorA) * inv + Color.red(colorB) * t).toInt().coerceIn(0, 255)
        val g = (Color.green(colorA) * inv + Color.green(colorB) * t).toInt().coerceIn(0, 255)
        val b = (Color.blue(colorA) * inv + Color.blue(colorB) * t).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun iconPolygonPath(
        polygon: RoundedPolygon,
        cx: Float,
        cy: Float,
        size: Float,
        scaleY: Float = 1f
    ): Path {
        return try {
            val h = size * scaleY
            val matrix = Matrix().apply {
                setScale(size, h)
                postTranslate(cx - size / 2f, cy - h / 2f)
            }
            polygon.toPath().apply { transform(matrix) }
        } catch (_: Throwable) {
            Path().apply { addCircle(cx, cy, size * 0.45f, Path.Direction.CW) }
        }
    }

    private fun drawCrescentMoon(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val moonPath = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        val cutout = Path().apply { addCircle(cx + r * 0.48f, cy - r * 0.34f, r * 0.86f, Path.Direction.CW) }
        moonPath.op(cutout, Path.Op.DIFFERENCE)
        canvas.drawPath(moonPath, paint)
    }

    private fun drawSlantedDrop(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, paint: Paint) {
        canvas.save()
        canvas.rotate(18f, cx, cy)
        canvas.drawRoundRect(RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f), w / 2f, w / 2f, paint)
        canvas.restore()
    }

    private fun resolveSkyGradientColors(condition: WeatherCondition, isDay: Boolean): IntArray {
        return if (!isDay) {
            when (condition) {
                WeatherCondition.CLEAR -> intArrayOf(0xFF152E4D.toInt(), 0xFF0C1A2E.toInt(), 0xFF060E1A.toInt())
                WeatherCondition.FEW_CLOUDS -> intArrayOf(0xFF192B44.toInt(), 0xFF0F1D2F.toInt(), 0xFF070F1B.toInt())
                WeatherCondition.CLOUDY -> intArrayOf(0xFF232C37.toInt(), 0xFF161D25.toInt(), 0xFF0D1117.toInt())
                WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> intArrayOf(0xFF172534.toInt(), 0xFF0E1822.toInt(), 0xFF070D14.toInt())
                WeatherCondition.THUNDERSTORM -> intArrayOf(0xFF1E1638.toInt(), 0xFF120D24.toInt(), 0xFF090612.toInt())
                WeatherCondition.SNOW -> intArrayOf(0xFF1A2938.toInt(), 0xFF111C28.toInt(), 0xFF0A111A.toInt())
                WeatherCondition.FOG -> intArrayOf(0xFF1E262E.toInt(), 0xFF131920.toInt(), 0xFF0B0F13.toInt())
            }
        } else {
            when (condition) {
                WeatherCondition.CLEAR -> intArrayOf(0xFF4CA0E8.toInt(), 0xFF2B7FC6.toInt(), 0xFF1863A3.toInt())
                WeatherCondition.FEW_CLOUDS -> intArrayOf(0xFF4D98DC.toInt(), 0xFF2F78B8.toInt(), 0xFF1F619B.toInt())
                WeatherCondition.CLOUDY -> intArrayOf(0xFF5A6F84.toInt(), 0xFF45596D.toInt(), 0xFF344556.toInt())
                WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> intArrayOf(0xFF395470.toInt(), 0xFF29415A.toInt(), 0xFF1C3046.toInt())
                WeatherCondition.THUNDERSTORM -> intArrayOf(0xFF342B59.toInt(), 0xFF251E44.toInt(), 0xFF17122E.toInt())
                WeatherCondition.SNOW -> intArrayOf(0xFF557FA6.toInt(), 0xFF40688E.toInt(), 0xFF2F5375.toInt())
                WeatherCondition.FOG -> intArrayOf(0xFF5F707D.toInt(), 0xFF4B5B68.toInt(), 0xFF394753.toInt())
            }
        }
    }

    private fun resolveGlassAccentColor(condition: WeatherCondition, isDay: Boolean): Int {
        return if (!isDay) {
            Color.argb(65, 100, 149, 237)
        } else {
            when (condition) {
                WeatherCondition.CLEAR, WeatherCondition.FEW_CLOUDS -> Color.argb(72, 66, 165, 245)
                WeatherCondition.THUNDERSTORM -> Color.argb(72, 126, 87, 194)
                WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Color.argb(68, 41, 182, 246)
                else -> Color.argb(56, 176, 190, 197)
            }
        }
    }
}
