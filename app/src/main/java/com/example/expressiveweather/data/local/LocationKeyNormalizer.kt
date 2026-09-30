package com.example.expressiveweather.data.local

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * 座標の正規化ユーティリティ
 *
 * 緯度と経度を受け取り、小数点以下2桁に四捨五入（Half-Up）した上で、
 * 一意のキー文字列（例: 緯度35.6812, 経度139.7671 -> "35.68_139.77"）を生成します。
 * 約1.1km四方のメッシュ単位で同一キーに集約することで、
 * GPSの微小なブレによる過剰なAPI通信を抑止し、キャッシュヒット率を最大化します。
 */
object LocationKeyNormalizer {

    /**
     * 緯度・経度から一意の正規化キー文字列を生成
     */
    fun normalize(latitude: Double, longitude: Double): String {
        val roundedLat = roundToTwoDecimals(latitude)
        val roundedLon = roundToTwoDecimals(longitude)
        return String.format(Locale.US, "%.2f_%.2f", roundedLat, roundedLon)
    }

    /**
     * 代表緯度・経度（小数点第2位丸め）をPairで返却
     */
    fun getNormalizedCoordinates(latitude: Double, longitude: Double): Pair<Double, Double> {
        return Pair(roundToTwoDecimals(latitude), roundToTwoDecimals(longitude))
    }

    private fun roundToTwoDecimals(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
