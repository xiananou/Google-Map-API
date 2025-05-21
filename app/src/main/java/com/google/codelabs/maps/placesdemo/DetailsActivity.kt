package com.google.codelabs.maps.placesdemo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.ktx.api.net.awaitFetchPlace
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class DetailsActivity : AppCompatActivity() {
    private lateinit var placesClient: PlacesClient
    private lateinit var detailsButton: Button
    private lateinit var detailsInput: TextInputEditText
    private lateinit var responseView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        // Set up view objects
        detailsInput = findViewById(R.id.details_input)
        detailsButton = findViewById(R.id.details_button)
        responseView = findViewById(R.id.details_response_content)

        val apiKey = BuildConfig.PLACES_API_KEY

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e(TAG, "No api key")
            finish()
            return
        }

        // Retrieve a PlacesClient (previously initialized - see DemoApplication)
        placesClient = Places.createClient(this)

        // Upon button click, fetch and display the Place Details
        detailsButton.setOnClickListener { button ->
            button.isEnabled = false
            val placeId = detailsInput.text.toString()
            val placeFields = listOf(
                Place.Field.NAME,
                Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
            lifecycleScope.launch {
                try {
                    val response = placesClient.awaitFetchPlace(placeId, placeFields)
                    responseView.text = response.prettyPrint()
                } catch (e: Exception) {
                    e.printStackTrace()
                    responseView.text = e.message
                }
                button.isEnabled = true
            }
        }
    }

    companion object {
        private const val TAG = "DetailsActivity"
    }
}