package io.github.nemynew.expressiveweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.nemynew.expressiveweather.data.City

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
        fun canonicalId(city: City): String {
            return if (city.id.isNotBlank() && city.id != "current") {
                city.id
            } else {
                LocationKeyNormalizer.normalize(city.latitude, city.longitude)
            }
        }

        fun fromCity(city: City): SavedCityEntity {
            val key = canonicalId(city)
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
