package io.github.nemynew.expressiveweather.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.ui.components.WeatherIconHelper

/**
 * Material 3 Expressive (`MaterialShapes`) ベースのオーガニックウィジェット・レンダラー。
 * 透明背景の上に有機的な Material Shape（ペブル/クッキー/クローバー/サニー等）と
 * Google Sans Flex の気温、MaterialShapes ベースの天候アイコンを高精細描画します。
 */
object WidgetMaterialShapeRenderer {

    @Volatile
    private var cachedTempTypeface: Typeface? = null

    private fun getTempTypeface(context: Context): Typeface {
        cachedTempTypeface?.let { return it }
        val resolved = try {
            ResourcesCompat.getFont(context, R.font.google_sans_flex)
        } catch (_: Throwable) {
            null
        } ?: Typeface.SANS_SERIF
        cachedTempTypeface = resolved
        return resolved
    }

    /**
     * プレビュー／ホーム画面ウィジェット用の透明背景 + Material Shape ビットマップを生成
     */
    fun renderShapeWidgetBitmap(
        context: Context,
        config: WidgetConfig,
        model: WidgetUiModel,
        sizePx: Int = 384
    ): Bitmap {
        val appCtx = context.applicationContext
        val isSystemDark = (appCtx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val palette = resolvePalette(appCtx, config.style, model.condition, model.isDay, isSystemDark, config.glassOpacity)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = sizePx.toFloat()

        // 1. 外枠の Material Shape を描画（Material You はフラット単色、天候連動のみグラデーション）
        val outerPath = buildOuterShapePath(config.shapeType, w)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (config.style == WidgetStyle.WEATHER_EXPRESSIVE && palette.gradientBottomColor != palette.fillColor) {
                shader = LinearGradient(
                    0f, 0f, w * 0.2f, w,
                    palette.fillColor,
                    palette.gradientBottomColor,
                    Shader.TileMode.CLAMP
                )
            } else {
                color = palette.fillColor
            }
        }
        canvas.drawPath(outerPath, bgPaint)

        // 2. 右上に大きく気温テキスト（例: 15°）を描画
        val tempText = model.currentTempText
        val typeface = getTempTypeface(appCtx)
        val lenScale = when {
            tempText.length >= 4 -> 0.70f
            tempText.length == 3 -> 0.88f
            else -> 1.0f
        }
        val baseTempSize = when (config.shapeType) {
            MaterialWidgetShape.CLOVER_4, MaterialWidgetShape.COOKIE_12 -> w * 0.305f
            else -> w * 0.34f
        }
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primaryTextColor
            textSize = baseTempSize * lenScale
            this.typeface = typeface
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                fontVariationSettings = "'wght' 380, 'ROND' 100"
            }
            letterSpacing = -0.04f
            textAlign = Paint.Align.CENTER
        }
        val tempCenterX = when (config.shapeType) {
            MaterialWidgetShape.PILL, MaterialWidgetShape.OVAL -> w * 0.60f
            else -> w * 0.59f
        }
        val tempCenterY = when (config.shapeType) {
            MaterialWidgetShape.PILL, MaterialWidgetShape.OVAL -> w * 0.34f
            else -> w * 0.36f
        }
        val tempBaselineY = tempCenterY - (tempPaint.descent() + tempPaint.ascent()) / 2f
        canvas.drawText(tempText, tempCenterX, tempBaselineY, tempPaint)

        // 3. 左下にアプリ本体と統一された Fluent 天気アイコンを描画
        val iconCx = when (config.shapeType) {
            MaterialWidgetShape.PILL, MaterialWidgetShape.OVAL -> w * 0.35f
            else -> w * 0.36f
        }
        val iconCy = when (config.shapeType) {
            MaterialWidgetShape.PILL, MaterialWidgetShape.OVAL -> w * 0.65f
            else -> w * 0.64f
        }
        val iconSize = w * 0.33f
        drawUnifiedAppWeatherIcon(
            context = appCtx,
            canvas = canvas,
            condition = model.condition,
            isDay = model.isDay,
            cx = iconCx,
            cy = iconCy,
            iconSize = iconSize,
            isLightSurface = palette.isLightSurface
        )

        return bitmap
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun buildOuterShapePath(shapeType: MaterialWidgetShape, size: Float): Path {
        return when (shapeType) {
            MaterialWidgetShape.PILL -> normalizedPolygonToPath(MaterialShapes.Pill, size, insetRatio = 0.03f)
            MaterialWidgetShape.OVAL -> normalizedPolygonToPath(MaterialShapes.Oval, size, insetRatio = 0.03f)
            MaterialWidgetShape.COOKIE_12 -> normalizedPolygonToPath(MaterialShapes.Cookie12Sided, size, insetRatio = 0.04f)
            MaterialWidgetShape.CLOVER_4 -> normalizedPolygonToPath(MaterialShapes.Clover4Leaf, size, insetRatio = 0.04f)
            MaterialWidgetShape.SUNNY -> normalizedPolygonToPath(MaterialShapes.Sunny, size, insetRatio = 0.03f)
            MaterialWidgetShape.PUFFY -> normalizedPolygonToPath(MaterialShapes.Puffy, size, insetRatio = 0.04f)
        }
    }

    /**
     * 公式 MaterialShapes (normalized 座標) を比率を一切歪めずにキャンバス中央へフィット変換
     */
    private fun normalizedPolygonToPath(
        polygon: RoundedPolygon,
        size: Float,
        insetRatio: Float = 0.04f
    ): Path {
        return try {
            val rawPath = polygon.toPath()
            val bounds = RectF()
            rawPath.computeBounds(bounds, true)
            val inset = size * insetRatio
            val targetRect = RectF(inset, inset, size - inset, size - inset)
            val matrix = Matrix()
            if (bounds.width() > 0f && bounds.height() > 0f) {
                matrix.setRectToRect(bounds, targetRect, Matrix.ScaleToFit.CENTER)
            } else {
                val drawSize = size - inset * 2f
                matrix.setScale(drawSize, drawSize)
                matrix.postTranslate(inset, inset)
            }
            rawPath.apply { transform(matrix) }
        } catch (_: Throwable) {
            Path().apply { addCircle(size / 2f, size / 2f, size * 0.45f, Path.Direction.CW) }
        }
    }

    private fun drawUnifiedAppWeatherIcon(
        context: Context,
        canvas: Canvas,
        condition: WeatherCondition,
        isDay: Boolean,
        cx: Float,
        cy: Float,
        iconSize: Float,
        isLightSurface: Boolean
    ) {
        val resId = WeatherIconHelper.getConditionDrawable(condition, isDay)
        val tintColor = WidgetCanvasRenderer.getConditionIconTint(condition, isDay, isLightSurface)
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate()
        if (drawable != null) {
            DrawableCompat.setTint(drawable, tintColor)
            val half = (iconSize / 2f).toInt()
            val cxInt = cx.toInt()
            val cyInt = cy.toInt()
            drawable.setBounds(cxInt - half, cyInt - half, cxInt + half, cyInt + half)
            drawable.draw(canvas)
        }
    }

    private data class ShapePalette(
        val fillColor: Int,
        val gradientBottomColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int,
        val isLightSurface: Boolean
    )

    private fun resolvePalette(
        context: Context,
        style: WidgetStyle,
        condition: WeatherCondition,
        isDay: Boolean,
        isSystemDark: Boolean,
        glassOpacity: Float
    ): ShapePalette {
        return when (style) {
            WidgetStyle.MATERIAL_YOU -> {
                val myPalette = WidgetCanvasRenderer.resolveMaterialYouPalette(context, isSystemDark)
                ShapePalette(
                    fillColor = myPalette.shapeContainerColor,
                    gradientBottomColor = myPalette.shapeContainerColor,
                    primaryTextColor = myPalette.shapeTextColor,
                    secondaryTextColor = WidgetCanvasRenderer.withAlpha(myPalette.shapeTextColor, 200),
                    isLightSurface = myPalette.isLightSurface
                )
            }

            WidgetStyle.WEATHER_EXPRESSIVE -> {
                val (top, bottom) = when {
                    !isDay -> Color.rgb(21, 46, 77) to Color.rgb(10, 22, 42)
                    condition == WeatherCondition.THUNDERSTORM -> Color.rgb(52, 43, 89) to Color.rgb(28, 22, 54)
                    condition == WeatherCondition.RAIN || condition == WeatherCondition.HEAVY_RAIN -> Color.rgb(63, 89, 115) to Color.rgb(34, 55, 76)
                    condition == WeatherCondition.CLOUDY -> Color.rgb(93, 112, 131) to Color.rgb(60, 77, 94)
                    else -> Color.rgb(74, 156, 226) to Color.rgb(33, 112, 178)
                }
                ShapePalette(
                    fillColor = top,
                    gradientBottomColor = bottom,
                    primaryTextColor = Color.WHITE,
                    secondaryTextColor = Color.argb(210, 255, 255, 255),
                    isLightSurface = false
                )
            }

            WidgetStyle.FROSTED_GLASS -> {
                val myPalette = WidgetCanvasRenderer.resolveMaterialYouPalette(context, isSystemDark)
                val alphaByte = (glassOpacity.coerceIn(0.15f, 0.95f) * 255f).toInt()
                val textPrimary = if (myPalette.isLightSurface && glassOpacity >= 0.45f) {
                    myPalette.shapeTextColor
                } else {
                    Color.WHITE
                }
                ShapePalette(
                    fillColor = WidgetCanvasRenderer.withAlpha(myPalette.shapeContainerColor, alphaByte),
                    gradientBottomColor = WidgetCanvasRenderer.withAlpha(myPalette.shapeContainerColor, alphaByte),
                    primaryTextColor = textPrimary,
                    secondaryTextColor = WidgetCanvasRenderer.withAlpha(textPrimary, 205),
                    isLightSurface = myPalette.isLightSurface && glassOpacity >= 0.45f
                )
            }
        }
    }
}
