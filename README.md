Google Places API Demo
How the App Works
This application demonstrates the Google Places API in an Android app with three main features:

Place Details: Enter a Place ID to view detailed information about a location
Place Autocomplete: Search for places with real-time suggestions as you type
Current Place: Detect your current location and view nearby places on a map

Development Time
It took approximately 15 hours to complete this project:

0.5 hours: Project setup and API configuration
1 hours: Place Details functionality
0.5 hours: Autocomplete functionality
0.5 hours: Current Place functionality with map integration

Challenges Faced

API Key Configuration: Setting up the secrets-gradle-plugin to securely store API keys
Location Permissions: Implementing proper runtime permission handling
Current Place Accuracy: Understanding why the API sometimes returns nearby locations rather than exact position
Map Integration: Properly handling map state during activity lifecycle events

Resources Used

https://developers.google.com/codelabs/maps-platform/places-101-android-kotlin#11