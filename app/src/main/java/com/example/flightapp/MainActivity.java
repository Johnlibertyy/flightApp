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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Simple class to hold airline data
    private static class Airline {
        String name;
        String code;
        
        Airline(String name, String code) {
            this.name = name;
            this.code = code;
        }
        
        @Override
        public String toString() {
            return name; // Display name in spinner
        }
    }

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseAuth mAuth;
    private EditText flightNumberInput;
    private Spinner airlineSpinner;
    private FirebaseFirestore db;
    private ArrayAdapter<Airline> airlineAdapter;
    private final List<Airline> airlineList = new ArrayList<>();

    // 🔽 Location-related
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Initialize Firebase App first
            FirebaseApp.initializeApp(this);
            Log.d("MainActivity", "Firebase initialized");
            
            setContentView(R.layout.activity_main);

            // Firebase and Firestore setup
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            Log.d("MainActivity", "Firestore instance created");

            // UI elements
            flightNumberInput = findViewById(R.id.textInput);
            airlineSpinner = findViewById(R.id.airlineSpinner);
            Button findFlightButton = findViewById(R.id.findWayButton);

            // Null check for UI elements with specific error messages
            if (flightNumberInput == null) {
                Log.e("MainActivity", "flightNumberInput (textInput) is null");
                Toast.makeText(this, "Error: Flight number input not found", Toast.LENGTH_LONG).show();
                return;
            }
            if (airlineSpinner == null) {
                Log.e("MainActivity", "airlineSpinner is null");
                Toast.makeText(this, "Error: Airline spinner not found", Toast.LENGTH_LONG).show();
                return;
            }
            if (findFlightButton == null) {
                Log.e("MainActivity", "findFlightButton (findWayButton) is null");
                Toast.makeText(this, "Error: Find flight button not found", Toast.LENGTH_LONG).show();
                return;
            }            // Spinner setup
            airlineList.add(new Airline("Select Airline", "")); // Add default hint
            airlineAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, airlineList);
            airlineAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            airlineSpinner.setAdapter(airlineAdapter);
            Log.d("MainActivity", "Spinner setup complete, about to fetch airlines");
            
            fetchAirlines();

            // 🔽 Location setup (will request permission when user logs in)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            
            // Request location permission after login
            requestLocationPermission();

            // 🔽 On button click, get location and proceed
            findFlightButton.setOnClickListener(v -> {
                String flightNumber = flightNumberInput.getText().toString().trim();
                Airline selectedAirlineObj = (Airline) airlineSpinner.getSelectedItem();
                String selectedAirline = selectedAirlineObj != null ? selectedAirlineObj.name : "";
                String selectedAirlineCode = selectedAirlineObj != null ? selectedAirlineObj.code : "";

                if (selectedAirline.isEmpty() || selectedAirline.equals("Select Airline")) {
                    Toast.makeText(MainActivity.this, "Please select an airline", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (flightNumber.isEmpty()) {
                    // Crash the app and report to Crashlytics
                    Log.e("MainActivity", "User clicked Find Way without flight number - crashing app");
                    
                    // Record the exception to Crashlytics first
                    RuntimeException crashException = new RuntimeException("User clicked Find Way button without entering flight number");
                    FirebaseCrashlytics.getInstance().recordException(crashException);
                    
                    // Force send the crash report immediately
                    FirebaseCrashlytics.getInstance().sendUnsentReports();
                    
                    // Add a small delay to ensure the report is sent before crashing
                    try {
                        Thread.sleep(1000); // 1 second delay
                    } catch (InterruptedException e) {
                        // Ignore interrupt
                    }
                    
                    // Now throw the exception to crash the app
                    throw crashException;
                }

                logFlightSearchEvent(flightNumber, selectedAirline);
                getUserLocation(); // Refresh location on button press
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                intent.putExtra("FLIGHT_NUMBER", flightNumber);
                intent.putExtra("AIRLINE_NAME", selectedAirline);
                intent.putExtra("AIRLINE_CODE", selectedAirlineCode);
                startActivity(intent);
            });
            
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app. Please restart.", Toast.LENGTH_LONG).show();
        }
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
        Log.d("MainActivity", "Starting to fetch airlines from Firestore...");
        
        airlineList.add(new Airline("Loading airlines...", ""));
        airlineAdapter.notifyDataSetChanged();
        
        db.collection("airlines")
                .get()
                .addOnCompleteListener(task -> {
                    // Remove loading message
                    airlineList.remove(airlineList.stream()
                            .filter(airline -> airline.name.equals("Loading airlines..."))
                            .findFirst()
                            .orElse(null));
                    
                    if (task.isSuccessful()) {
                        Log.d("MainActivity", "Successfully fetched airlines");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String airlineName = document.getString("name");
                            String airlineCode = document.getString("code");
                            
                            if (airlineName != null && !airlineName.trim().isEmpty()) {
                                // Use empty string if code is null
                                String code = airlineCode != null ? airlineCode : "";
                                airlineList.add(new Airline(airlineName, code));
                            }
                        }
                        airlineAdapter.notifyDataSetChanged();
                        Log.d("MainActivity", "Added " + (airlineList.size() - 1) + " airlines to spinner");
                    } else {
                        Log.e("MainActivity", "Failed to fetch airlines", task.getException());
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
