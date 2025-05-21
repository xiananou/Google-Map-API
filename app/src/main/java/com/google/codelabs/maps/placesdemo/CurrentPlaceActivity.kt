package com.google.codelabs.maps.placesdemo

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class CurrentPlaceActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var placesClient: PlacesClient
    private lateinit var currentButton: Button
    private lateinit var responseView: TextView
    private var map: GoogleMap? = null
    private var lastKnownLocation: LatLng? = null
    private val likelyPlaces = mutableListOf<LikelyPlace>()

    // Default location (Sydney, Australia) used when location permission is not granted
    // or no current location is available
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current)

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
        }

        // Use a different approach for getting API key that won't cause errors
        // with the secrets-gradle-plugin
        val apiKey = BuildConfig.MAPS_API_KEY

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e(TAG, "No api key")
            finish()
            return
        }

        // Retrieve a PlacesClient (previously initialized - see DemoApplication)
        placesClient = Places.createClient(this)
        (supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?)?.getMapAsync(this)

        // Set view objects
        currentButton = findViewById(R.id.current_button)
        responseView = findViewById(R.id.current_response_content)

        // Set listener for initiating Current Place
        currentButton.setOnClickListener {
            currentButton.isEnabled = false
            checkPermissionThenFindCurrentPlace()
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        super.onSaveInstanceState(outState)
    }


    private fun checkPermissionThenFindCurrentPlace() {
        when {
            (ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) -> {
                // You can use the API that requires the permission.
                findCurrentPlace()
            }

            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)
                -> {
                Log.d(TAG, "Showing permission rationale dialog")

            }

            else -> {
                // Ask for both the ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        ACCESS_FINE_LOCATION,
                        ACCESS_COARSE_LOCATION
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    /**
     * Handles the result of the permission request.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            when {
                (grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) -> {
                    // Permission is granted. Continue with finding current place.
                    findCurrentPlace()
                }
                else -> {
                    // Permission denied. Show educational UI or disable the functionality
                    // that depends on this permission.
                    Log.d(TAG, "Permission denied")
                    responseView.text = "Location permission denied. Cannot determine current place."
                    currentButton.isEnabled = true
                }
            }
        }
    }


    private fun findCurrentPlace() {
        // Use fields to define the data types to return.
        val placeFields = listOf(
            Place.Field.NAME,
            Place.Field.ID,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
        )

        // Use the builder to create a FindCurrentPlaceRequest.
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        // Call findCurrentPlace and handle the response.
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should never happen since we checked permissions earlier
            responseView.text = "Location permission is not granted"
            currentButton.isEnabled = true
            return
        }

        placesClient.findCurrentPlace(request)
            .addOnSuccessListener { response ->
                val placeLikelihoods = response.placeLikelihoods
                val stringBuilder = StringBuilder()

                if (placeLikelihoods.isEmpty()) {
                    stringBuilder.append("No current place found")
                    currentButton.isEnabled = true
                } else {
                    stringBuilder.append("Current places:\n")
                    for ((i, placeLikelihood) in placeLikelihoods.withIndex()) {
                        val place = placeLikelihood.place
                        stringBuilder.append("""
                            |${i + 1}. ${place.name}
                            |   Likelihood: ${placeLikelihood.likelihood}
                            |   Address: ${place.address}
                            |   ID: ${place.id}
                            |   Lat/Lng: ${place.latLng}
                            |
                        """.trimMargin())

                        // Only show the first 5 results
                        if (i >= 4) break
                    }

                    likelyPlaces.clear()

                    likelyPlaces.addAll(
                        response.placeLikelihoods.take(M_MAX_ENTRIES).mapNotNull { placeLikelihood ->
                            placeLikelihood.toLikelyPlace()
                        }
                    )

                    openPlacesDialog()
                }
                responseView.text = stringBuilder.toString()
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                responseView.text = "Error: ${exception.message}"
                currentButton.isEnabled = true
            }
    }


    private fun openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        val listener =
            DialogInterface.OnClickListener { _, which -> // The "which" argument contains the position of the selected item.
                val likelyPlace = likelyPlaces[which]
                lastKnownLocation = likelyPlace.latLng

                val snippet = buildString {
                    append(likelyPlace.address)
                    if (likelyPlace.attribution.isNotEmpty()) {
                        append("\n")
                        append(likelyPlace.attribution.joinToString(", "))
                    }
                }

                val place = Place.builder().apply {
                    name = likelyPlace.name
                    latLng = likelyPlace.latLng
                }.build()

                map?.clear()

                setPlaceOnMap(place, snippet)
            }

        // Display the dialog.
        AlertDialog.Builder(this)
            .setTitle(R.string.pick_place)
            .setItems(likelyPlaces.map { it.name }.toTypedArray(), listener)
            .setOnDismissListener {
                currentButton.isEnabled = true
            }
            .show()
    }


    private fun setPlaceOnMap(place: Place?, markerSnippet: String?) {
        val latLng = place?.latLng ?: defaultLocation
        map?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                DEFAULT_ZOOM
            )
        )
        map?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(place?.name)
                .snippet(markerSnippet)
        )
    }


    override fun onMapReady(map: GoogleMap) {
        this.map = map

        // Enable the my-location layer if permission has been granted
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error showing location: ${e.message}")
        }

        lastKnownLocation?.let { location ->
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    location,
                    DEFAULT_ZOOM
                )
            )
        }
    }

    private data class LikelyPlace(
        val name: String,
        val address: String,
        val attribution: List<String>,
        val latLng: LatLng
    )

    private fun PlaceLikelihood.toLikelyPlace(): LikelyPlace? {
        val name = this.place.name
        val address = this.place.address
        val latLng = this.place.latLng
        val attributions = this.place.attributions ?: emptyList()

        return if (name != null && address != null && latLng != null) {
            LikelyPlace(name, address, attributions, latLng)
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "CurrentPlaceActivity"
        private const val PERMISSION_REQUEST_CODE = 9
        private const val DEFAULT_ZOOM = 15f
        private const val M_MAX_ENTRIES = 5
        // Key for storing activity state.
        private const val KEY_LOCATION = "location"
    }
}