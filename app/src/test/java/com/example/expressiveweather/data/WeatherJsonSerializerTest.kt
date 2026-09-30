package com.example.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeatherJsonSerializerTest {

    @Test
    fun testSerializationAndDeserializationRoundTrip() {
        val city = City(
            id = "tokyo",
            name = "東京",
            nameEn = "Tokyo",
            country = "日本",
            latitude = 35.6895,
            longitude = 139.6917,
            isFavorite = true
        )

        val weatherData = WeatherData(
            city = city,
            currentTempC = 22,
            feelsLikeC = 21,
            minTempC = 18,
            maxTempC = 25,
            condition = WeatherCondition.CLEAR,
            conditionSummary = "澄み渡る青空",
            humidity = 55,
            uvIndex = 6,
            uvLabel = "普通",
            windSpeedKmH = 15.0,
            windDirection = "南",
            windDegree = 180f,
            pressureHpa = 1013,
            visibilityKm = 10.0,
            airQuality = AirQuality(
                aqi = 42,
                levelLabel = "良好",
                pm25 = 12.5,
                pm10 = 25.0,
                advice = "空気が綺麗です"
            ),
            sunSchedule = SunSchedule(
                sunrise = "05:45",
                sunset = "17:50",
                dayProgress = 0.5f,
                daylightHours = "12h 05m"
            ),
            hourlyForecast = listOf(
                HourlyForecast(
                    timeLabel = "12:00",
                    hour = 12,
                    tempC = 23,
                    condition = WeatherCondition.CLEAR,
                    precipProb = 10,
                    humidity = 50,
                    windSpeedKmH = 14.0
                )
            ),
            dailyForecast = listOf(
                DailyForecast(
                    dayLabel = "今日",
                    dateLabel = "9/17",
                    condition = WeatherCondition.CLEAR,
                    minTempC = 18,
                    maxTempC = 25,
                    precipProb = 10
                )
            ),
            lastUpdatedTime = "12:00"
        )

        val json = WeatherJsonSerializer.serialize(weatherData)
        assertNotNull(json)

        val deserialized = WeatherJsonSerializer.deserialize(json)
        assertNotNull(deserialized)
        assertEquals(weatherData.city.name, deserialized?.city?.name)
        assertEquals(weatherData.currentTempC, deserialized?.currentTempC)
        assertEquals(weatherData.feelsLikeC, deserialized?.feelsLikeC)
        assertEquals(weatherData.condition, deserialized?.condition)
        assertEquals(weatherData.humidity, deserialized?.humidity)
        assertEquals(weatherData.airQuality.aqi, deserialized?.airQuality?.aqi)
        assertEquals(weatherData.sunSchedule.sunrise, deserialized?.sunSchedule?.sunrise)
        assertEquals(weatherData.windSpeedKmH, deserialized?.windSpeedKmH ?: 0.0, 0.001)
        assertEquals(weatherData.hourlyForecast.size, deserialized?.hourlyForecast?.size)
        assertEquals(weatherData.dailyForecast.size, deserialized?.dailyForecast?.size)
        assertEquals(weatherData.fetchedAtMillis, deserialized?.fetchedAtMillis)
    }
}
