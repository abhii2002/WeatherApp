package com.blissvine.weatherapp

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.blissvine.weatherapp.models.WeatherResponse
import com.blissvine.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    // required to get the latitude and longitude of the user
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null
    private lateinit var mSharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // initializing our variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        setupUI()

        if(!isLocationEnabled()){
            Toast.makeText(this, "Your location provider is turned off. Please turn it on.", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{

            Dexter.withActivity(this).withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if(report!!.areAllPermissionsGranted()){
                        requestLocationData()
                    }

                    if(report.isAnyPermissionPermanentlyDenied){
                        Toast.makeText(this@MainActivity, "You have denied location permission. Please enable them as it is mandatory for the app to work.", Toast.LENGTH_SHORT).show()
                    }

                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationDialogForPermissions()
                }

            }).onSameThread().check()

        }

    }

    private fun isLocationEnabled(): Boolean{
        // This provides access to the system location services.
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    /*
    Request Location data
     */

    @Suppress("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallBack, Looper.myLooper()
        )


    }

    private val mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude", "$longitude")
            getLocationWeatherDetails(latitude!!, longitude!!)

        }

    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double ){
        if(Constants.isNetworkAvailable(this@MainActivity)){
            //1. Preparing the url
            val retrofit: Retrofit = Retrofit.Builder().
            baseUrl(Constants.BASE_URL).
            addConverterFactory(GsonConverterFactory.create()).
            build()


            //2. Preparing the service
            /*
            Here we map the service interface in which we declare the end point and the API type
            and we do that along with the request parameters which are required
             */
            val service : WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)

            //3. Making the call
            val listCall : Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object: Callback<WeatherResponse>{
                // when successful http response is given
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response.isSuccessful){

                        hideProgressDialog()

                         val weatherList: WeatherResponse = response.body()!!
                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()
                         setupUI()
                        Log.i("Response Result", "$weatherList" )
                    }else{
                         val rc = response.code()
                        when(rc){
                             400 ->{
                                  Log.e("Error 400", "Bad Connection")
                             }

                            404 ->{
                                Log.e("Error 404", "Not Found")
                            }else ->{
                                 Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }
                // on failure
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorrrr", t.message.toString())
                    hideProgressDialog()
                }

            } )


        }else{
            Toast.makeText(this, "You have no internet connection !!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRationDialogForPermissions(){
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ){ _, _, ->

                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    // we needed to give uri link of our app so that it opens the settings for our particular app
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }

            }.setNegativeButton("Cancel"){ dialog, _ ->
                dialog.dismiss()

            }.show()
      }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
             R.id.action_refresh ->{
                  requestLocationData()
                 true
             }else -> return super.onOptionsItemSelected(item)
        }

    }

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this@MainActivity)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()

    }

    private fun hideProgressDialog(){
         if(mProgressDialog != null){
              mProgressDialog!!.dismiss()
         }
    }


    private fun setupUI(){

        val weatherResponseJsonString = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")

        if(!weatherResponseJsonString.isNullOrEmpty()){
             val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)


            for(i in weatherList.weather.indices){

                Log.i("Weather response", "${weatherList.weather[i].main}")

                tv_main.text = weatherList.weather[i].main
                tv_main_description.text = weatherList.weather[i].description
                tv_temp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

                tv_sunrise.text = unixTime(weatherList.sys.sunrise)
                tv_sunset.text = unixTime(weatherList.sys.sunset)


                tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
                tv_min.text = weatherList.main.tempMin.toString() + " min"
                tv_max.text = weatherList.main.tempMax.toString()  + " max"
                tv_speed.text = weatherList.wind.speed.toString()
                tv_name.text  = weatherList.name
                tv_country.text = weatherList.sys.country


                when(weatherList.weather[i].icon){
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }


            }


        }


    }

    private fun getUnit(value: String): String? {
         var value = "°c"
         if("US" == value || "LR" == value || "MM" == value){
             value = "°F"
         }
        return  value
    }


    private fun unixTime(timex: Long): String?{
        val date  = Date(timex * 1000L)
        val sdf = SimpleDateFormat("hh:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)

    }
}