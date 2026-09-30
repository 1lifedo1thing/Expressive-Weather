package com.example.expressiveweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 天気キャッシュ用 DAO
 */
@Dao
interface WeatherDao {

    /**
     * 指定した座標キーのキャッシュを監視する Flow を取得
     * DB 更新時に自動的に新しいエンティティが emit されます。
     */
    @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey LIMIT 1")
    fun getWeatherStream(locationKey: String): Flow<WeatherCacheEntity?>

    /**
     * 指定した座標キーのキャッシュを1度だけ直接取得
     */
    @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey LIMIT 1")
    suspend fun getWeatherOnce(locationKey: String): WeatherCacheEntity?

    /**
     * キャッシュの新規挿入または更新（既存キーがある場合は上書き）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: WeatherCacheEntity): Long

    /**
     * 指定した閾値時刻より古い不要なキャッシュレコードを削除（クリーンアップ用）
     */
    @Query("DELETE FROM weather_cache WHERE fetchedAt < :thresholdTimestamp")
    suspend fun deleteOlderThan(thresholdTimestamp: Long): Int

    /**
     * すべてのキャッシュレコードを削除（デバッグ用）
     */
    @Query("DELETE FROM weather_cache")
    suspend fun deleteAll(): Int
}
