package io.github.nemynew.expressiveweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCityDao {

    @Query("SELECT * FROM saved_cities ORDER BY savedAt ASC")
    fun getAllSavedCitiesStream(): Flow<List<SavedCityEntity>>

    @Query("SELECT * FROM saved_cities ORDER BY savedAt ASC")
    suspend fun getAllSavedCitiesOnce(): List<SavedCityEntity>

    @Query("SELECT * FROM saved_cities WHERE id = :id OR (ABS(latitude - :latitude) < 0.01 AND ABS(longitude - :longitude) < 0.01) LIMIT 1")
    suspend fun findMatchingCity(id: String, latitude: Double, longitude: Double): SavedCityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(city: SavedCityEntity): Long

    @Query("DELETE FROM saved_cities WHERE id = :id OR (ABS(latitude - :latitude) < 0.01 AND ABS(longitude - :longitude) < 0.01)")
    suspend fun deleteCity(id: String, latitude: Double, longitude: Double): Int

    @Query("DELETE FROM saved_cities WHERE id = :id")
    suspend fun deleteCityById(id: String): Int
}
