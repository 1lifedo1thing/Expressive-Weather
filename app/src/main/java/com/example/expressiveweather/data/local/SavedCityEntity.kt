package com.example.expressiveweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.expressiveweather.data.City

/**
 * ユーザーが登録したお気に入り・保存都市の Room Entity
 */
@Entity(tableName = "saved_cities")
data class SavedCityEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val nameEn: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = true,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toCity(): City = City(
        id = id,
        name = name,
        nameEn = nameEn,
        country = country,
        latitude = latitude,
        longitude = longitude,
        isFavorite = isFavorite
    )

    companion object {
        fun fromCity(city: City): SavedCityEntity {
            val key = if (city.id.isNotBlank()) city.id else LocationKeyNormalizer.normalize(city.latitude, city.longitude)
            return SavedCityEntity(
                id = key,
                name = city.name,
                nameEn = city.nameEn,
                country = city.country,
                latitude = city.latitude,
                longitude = city.longitude,
                isFavorite = true,
                savedAt = System.currentTimeMillis()
            )
        }
    }
}
