package com.example.expressiveweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 天気データのローカルキャッシュ Entity
 *
 * @param locationKey 正規化された座標キー（例: "35.68_139.77"）
 * @param latitude リクエスト時の代表緯度
 * @param longitude リクエスト時の代表経度
 * @param fetchedAt APIからデータを取得した時刻（Unixエポックミリ秒）
 * @param weatherDataJson Kotlinx SerializationでシリアライズされたWeatherData JSON文字列
 */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val locationKey: String,
    val latitude: Double,
    val longitude: Double,
    val fetchedAt: Long,
    val weatherDataJson: String
)
