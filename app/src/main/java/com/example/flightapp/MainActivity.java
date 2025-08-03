package com.example.flightapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText flightNumberInput;
    private Spinner airlineSpinner;
    private FirebaseFirestore db;
    private ArrayAdapter<String> airlineAdapter;
    private final List<String> airlineList = new ArrayList<>();

    // 🔽 Location-related
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        // Firebase and Firestore setup
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();

        // UI elements
        flightNumberInput = findViewById(R.id.textInput);
        airlineSpinner = findViewById(R.id.airlineSpinner);
        Button findFlightButton = findViewById(R.id.findWayButton);

        // Spinner setup
        airlineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, airlineList);
        airlineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        airlineSpinner.setAdapter(airlineAdapter);
        fetchAirlines();

        // 🔽 Location setup
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission(); // request or get location on app start

        // 🔽 On button click, get location and proceed
        findFlightButton.setOnClickListener(v -> {
            String flightNumber = flightNumberInput.getText().toString().trim();
            String selectedAirline = airlineSpinner.getSelectedItem() != null
                    ? airlineSpinner.getSelectedItem().toString()
                    : "";

            if (selectedAirline.isEmpty()) {
                FirebaseCrashlytics.getInstance().log("Airline not selected.");
            }

            if (flightNumber.isEmpty()) {
                flightNumberInput.setError("Flight number cannot be empty");
                FirebaseCrashlytics.getInstance().log("Attempted to search with empty flight number.");
                FirebaseCrashlytics.getInstance().setCustomKey("empty_input_field", "flightNumberInput");
                throw new RuntimeException("Crash: Flight number was empty on search attempt.");
            }

            logFlightSearchEvent(flightNumber, selectedAirline);
            getUserLocation(); // Refresh location on button press
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("FLIGHT_NUMBER", flightNumber);
            intent.putExtra("AIRLINE_NAME", selectedAirline);
            startActivity(intent);
        });
    }

    // 🔽 Get permission or fetch location
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            getUserLocation();
        }
    }

    // 🔽 Fetch and show last known location
    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        Toast.makeText(this, "Your location: " + lat + ", " + lng, Toast.LENGTH_LONG).show();
                        Log.d("UserLocation", "Lat: " + lat + ", Lng: " + lng);
                    } else {
                        Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 🔽 Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchAirlines() {
        db.collection("airlines")
                .orderBy("name")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        airlineList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String airlineName = document.getString("name");
                            if (airlineName != null) {
                                airlineList.add(airlineName);
                            }
                        }
                        airlineAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Firestore", "Error getting airlines: ", task.getException());
                    }
                });
    }

    private void logFlightSearchEvent(String flightNumber, String airlineName) {
        Bundle bundle = new Bundle();
        bundle.putString("flight_number_searched", flightNumber);
        bundle.putString("airline_name_selected", airlineName);
        mFirebaseAnalytics.logEvent("find_flight_button_click", bundle);
        Log.d("FirebaseAnalytics", "Logged event 'find_flight_button_click' for flight: " + flightNumber + " with airline: " + airlineName);
    }
}
