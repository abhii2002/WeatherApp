package com.blissvine.weatherapp.models

import java.io.Serializable

/*
These are JSON objects (information in form of objects)
 */
data class WeatherResponse (
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys : Sys,
    val name: String,
    val cod: Int

    ): Serializable