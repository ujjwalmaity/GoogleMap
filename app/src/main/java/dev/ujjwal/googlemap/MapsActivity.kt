package dev.ujjwal.googlemap

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var mMarker: Marker? = null
    private var mLocationPermissionGranted: Boolean = false

    private lateinit var viewModel: MapsViewModel

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

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
        if (mLocationPermissionGranted)
            viewModel.startLocationUpdates()
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
    }

    private fun observeViewModel() {
        viewModel.location.observe(this, Observer { location ->
            location?.let {
                updateLocationUI(it)
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
}
