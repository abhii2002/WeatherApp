package com.blissvine.weatherapp.network

import com.blissvine.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String?,
        @Query("appid") appid: String?,

    ): Call<WeatherResponse>

/*
When we call this function, we want to run a call which will use weather response as the response of
this whole call.
 */

}