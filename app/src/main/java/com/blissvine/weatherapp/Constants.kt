package com.blissvine.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val APP_ID: String = "16efa8cae1c7dc9a6ccbebe70daa3123"
    const val BASE_URL: String = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric" // if we have Celsius unit or Fahrenheit
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA = "weather_response_data"

    fun isNetworkAvailable(context: Context): Boolean{


         val connectivityManager  = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
             val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when{
                 activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
                else -> false
            }

        }else{
             val networkInfo = connectivityManager.activeNetworkInfo
             return networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    }

}