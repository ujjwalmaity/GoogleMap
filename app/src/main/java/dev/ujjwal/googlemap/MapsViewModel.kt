package dev.ujjwal.googlemap

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import java.util.*

class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    var location: MutableLiveData<Location> = MutableLiveData()
    var latitude: MutableLiveData<Double> = MutableLiveData()
    var longitude: MutableLiveData<Double> = MutableLiveData()
    var address: MutableLiveData<String> = MutableLiveData()

    companion object {
        private val TAG = MapsViewModel::class.java.simpleName
    }

    fun getLocation() {
        if (!::fusedLocationProviderClient.isInitialized) {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        locationRequest = LocationRequest.create().setInterval(2000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (tempLocation in locationResult.locations) {
                    location.value = tempLocation
                    latitude.value = tempLocation.latitude
                    longitude.value = tempLocation.longitude
                    Log.i(TAG, tempLocation.latitude.toString() + "  " + tempLocation.longitude)
                    val geoCoder = Geocoder(context, Locale.getDefault())
                    try {
                        val listAddress: List<Address>? = geoCoder.getFromLocation(latitude.value!!, longitude.value!!, 1)
                        if (listAddress != null && listAddress.isNotEmpty()) {
                            //Log.i(TAG, listAddress[0].toString());
                            address.value = listAddress[0].getAddressLine(0)
                            Log.i(TAG, "${address.value}")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (::fusedLocationProviderClient.isInitialized)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        if (::fusedLocationProviderClient.isInitialized)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}
