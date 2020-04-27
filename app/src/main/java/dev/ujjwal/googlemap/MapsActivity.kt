package dev.ujjwal.googlemap

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var mMarker: Marker? = null
    private var geofenceMarker: Marker? = null
    private var geofenceCircle: Circle? = null
    private lateinit var geofencingClient: GeofencingClient
    private var mLocationPermissionGranted: Boolean = false

    private lateinit var viewModel: MapsViewModel

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val GEOFENCE_ID = "My Geofence"
        private const val GEOFENCE_RADIUS = 400f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        geofencingClient = LocationServices.getGeofencingClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        getLocationPermission()

        viewModel = ViewModelProviders.of(this).get(MapsViewModel::class.java)
        viewModel.getLocation()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (mLocationPermissionGranted) {
            viewModel.startLocationUpdates()
            mMap?.isMyLocationEnabled = true
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopLocationUpdates()
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (mLocationPermissionGranted) {
            mMap?.isMyLocationEnabled = true
        }

        mMap?.setOnMapClickListener {
            addGeofenceMarker(it)
        }
        mMap?.setOnMarkerClickListener { false }
    }

    private fun observeViewModel() {
        viewModel.location.observe(this, Observer { location ->
            location?.let {
                //updateLocationUI(it)
            }
        })
    }

    private fun updateLocationUI(location: Location) {
        /**Zoom level
         * 1: World
         * 5: Landmass/continent
         * 10: City
         * 15: Streets
         * 20: Buildings
         */
        mMap?.let { map ->
            val myLocation = LatLng(location.latitude, location.longitude)
            //map.clear()
            mMarker?.remove()
            mMarker = map.addMarker(MarkerOptions().position(myLocation).title(viewModel.address.value))
            map.moveCamera(CameraUpdateFactory.newLatLng(myLocation))
            //map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 10.0f))
        }
    }

    private fun addGeofenceMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng).title("Geofence Marker")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        geofenceMarker?.remove()
        geofenceMarker = mMap?.addMarker(markerOptions)
        addGeofenceCircle()
        startGeofence()
    }

    private fun addGeofenceCircle() {
        geofenceCircle?.remove()
        val circleOptions = CircleOptions().center(geofenceMarker?.position)
            .strokeColor(Color.argb(255, 255, 0, 0))
            .fillColor(Color.argb(65, 255, 0, 0))
            .radius(GEOFENCE_RADIUS.toDouble())
        geofenceCircle = mMap?.addCircle(circleOptions)
    }

    private fun startGeofence() {
        geofenceMarker?.let { marker ->

            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(marker.position.latitude, marker.position.longitude, GEOFENCE_RADIUS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(5000)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.removeGeofences(geofencePendingIntent)?.run {
                addOnCompleteListener {
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                        this.addOnSuccessListener {
                            Log.i(TAG, "Add Geofence: ${geofence.requestId}")
                        }
                        this.addOnFailureListener {
                            Log.i(TAG, "${it.message}")
                        }
                    }
                }
            }
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 2067, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
