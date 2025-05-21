package com.google.codelabs.maps.placesdemo

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class AutocompleteActivity : AppCompatActivity() {
    private lateinit var responseView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autocomplete)

        // Set up view objects
        responseView = findViewById(R.id.autocomplete_response_content)
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        val placeFields: List<Place.Field> =
            listOf(Place.Field.NAME, Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        autocompleteFragment.setPlaceFields(placeFields)

        // Listen to place selection events
        lifecycleScope.launchWhenCreated {
            autocompleteFragment.placeSelectionEvents().collect { event ->
                when (event) {
                    is PlaceSelectionSuccess -> {
                        val place = event.place
                        responseView.text = prettyPrintAutocompleteWidget(place, false)
                    }

                    is PlaceSelectionError -> Toast.makeText(
                        this@AutocompleteActivity,
                        "Failed to get place '${event.status.statusMessage}'",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

/**
 * Extension function to convert Place selection events to a Flow
 */
private fun AutocompleteSupportFragment.placeSelectionEvents() = callbackFlow {
    setOnPlaceSelectedListener(object : PlaceSelectionListener {
        override fun onPlaceSelected(place: Place) {
            trySend(PlaceSelectionSuccess(place))
        }

        override fun onError(status: Status) {
            trySend(PlaceSelectionError(status))
        }
    })

    awaitClose {
        // Clean up if needed
    }
}


/**
 * Wrapper classes for Place selection events
 */
sealed class PlaceSelectionEvent
data class PlaceSelectionSuccess(val place: Place) : PlaceSelectionEvent()
data class PlaceSelectionError(val status: Status) : PlaceSelectionEvent()