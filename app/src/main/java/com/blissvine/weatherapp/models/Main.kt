package com.blissvine.weatherapp.models

data class Main(
    val temp : Double,
    val pressure: Double,
    val humidity: Int,
    val tempMin: Double,
    val tempMax : Double,
    val sea_level: Double,
    val grnd_level: Double
): java.io.Serializable