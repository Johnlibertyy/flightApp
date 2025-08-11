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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
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
            FirebaseApp.initializeApp(this);
            setContentView(R.layout.activity_main);
            initFirebase();
            initUI();
            setupSpinner();
            fetchAirlines();
            initLocation();
            setupListeners();
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onCreate", e);
            Toast.makeText(this, getString(R.string.error_init_app), Toast.LENGTH_LONG).show();
        }
    }

    private void initFirebase() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initUI() {
        flightNumberInput = findViewById(R.id.textInput);
        airlineSpinner = findViewById(R.id.airlineSpinner);
        if (flightNumberInput == null) {
            Toast.makeText(this, getString(R.string.error_flight_input_missing), Toast.LENGTH_LONG).show();
        }
        if (airlineSpinner == null) {
            Toast.makeText(this, getString(R.string.error_airline_spinner_missing), Toast.LENGTH_LONG).show();
        }
    }

    private void setupSpinner() {
        airlineList.add(new Airline(getString(R.string.select_airline_hint), ""));
        airlineAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, airlineList);
        airlineAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        airlineSpinner.setAdapter(airlineAdapter);
    }

    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();
    }

    private void setupListeners() {
        Button findFlightButton = findViewById(R.id.findWayButton);
        if (findFlightButton == null) {
            Toast.makeText(this, getString(R.string.error_find_button_missing), Toast.LENGTH_LONG).show();
            return;
        }
        findFlightButton.setOnClickListener(v -> handleFindFlightClick());
    }

    private void handleFindFlightClick() {
        String flightNumber = flightNumberInput != null ? flightNumberInput.getText().toString().trim() : "";
        Airline selectedAirlineObj = (Airline) airlineSpinner.getSelectedItem();
        String selectedAirline = selectedAirlineObj != null ? selectedAirlineObj.name : "";
        String selectedAirlineCode = selectedAirlineObj != null ? selectedAirlineObj.code : "";
        if (selectedAirline.isEmpty() || selectedAirline.equals(getString(R.string.select_airline_hint))) {
            Toast.makeText(this, getString(R.string.toast_select_airline), Toast.LENGTH_SHORT).show();
            return;
        }
        if (flightNumber.isEmpty()) {
            triggerCrashForMissingFlight();
            return;
        }
        logFlightSearchEvent(flightNumber, selectedAirline);
        logFlightSearchAnalytics(selectedAirlineCode, flightNumber, true);
        getUserLocation();
        Intent intent = new Intent(this, SecondActivity.class);
        intent.putExtra(Constants.EXTRA_FLIGHT_NUMBER, flightNumber);
        intent.putExtra(Constants.EXTRA_AIRLINE_NAME, selectedAirline);
        intent.putExtra(Constants.EXTRA_AIRLINE_CODE, selectedAirlineCode);
        startActivity(intent);
    }

    private void triggerCrashForMissingFlight() {
        Log.e("MainActivity", "User clicked Find Way without flight number - crashing app");
        RuntimeException crashException = new RuntimeException("User clicked Find Way button without entering flight number");
        FirebaseCrashlytics.getInstance().recordException(crashException);
        FirebaseCrashlytics.getInstance().sendUnsentReports();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        throw crashException;
    }

    // 🔽 Get permission or fetch location
    private void requestLocationPermission() {
        // Request both coarse + fine for flexibility
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            getUserLocation();
        }
    }

    // 🔽 Fetch and show last known location
    private void getUserLocation() {
        boolean fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Try last known location first (fast, may be null)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        handleLocationSuccess(location);
                    } else {
                        Log.w("UserLocation", "Last location null, requesting fresh location...");
                        // 2. Try a direct current location request (Android 12+ API works earlier too)
                        fusedLocationClient.getCurrentLocation(fineGranted ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                                .addOnSuccessListener(loc -> {
                                    if (loc != null) {
                                        handleLocationSuccess(loc);
                                    } else {
                                        Log.w("UserLocation", "getCurrentLocation returned null, requesting single update");
                                        requestSingleLocationUpdate(fineGranted);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("UserLocation", "getCurrentLocation failed", e);
                                    requestSingleLocationUpdate(fineGranted);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLocation", "getLastLocation failed", e);
                    requestSingleLocationUpdate(fineGranted);
                });
    }

    private void requestSingleLocationUpdate(boolean highAccuracy) {
        try {
            LocationRequest request = new LocationRequest.Builder(
                    highAccuracy ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2000L)
                    .setWaitForAccurateLocation(highAccuracy)
                    .setMaxUpdates(1)
                    .setMinUpdateIntervalMillis(500)
                    .build();

            LocationCallback callback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    fusedLocationClient.removeLocationUpdates(this);
                    if (locationResult.getLastLocation() != null) {
                        handleLocationSuccess(locationResult.getLastLocation());
                    } else {
                        Toast.makeText(MainActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
                        Log.e("UserLocation", "Single update returned null");
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(request, callback, getMainLooper());
        } catch (SecurityException se) {
            Log.e("UserLocation", "SecurityException requesting single update", se);
        }
    }

    private void handleLocationSuccess(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        Toast.makeText(this, "Your location: " + lat + ", " + lng, Toast.LENGTH_LONG).show();
        Log.d("UserLocation", "Lat: " + lat + ", Lng: " + lng);
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
        
    airlineList.add(new Airline(getString(R.string.loading_airlines), ""));
        airlineAdapter.notifyDataSetChanged();
        
    db.collection(Constants.COL_AIRLINES)
                .get()
                .addOnCompleteListener(task -> {
                    // Remove loading message
            airlineList.remove(airlineList.stream()
                .filter(airline -> airline.name.equals(getString(R.string.loading_airlines)))
                            .findFirst()
                            .orElse(null));
                    
                    if (task.isSuccessful()) {
                        Log.d("MainActivity", "Successfully fetched airlines");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String airlineName = document.getString(Constants.FIELD_AIRLINE_NAME);
                            String airlineCode = document.getString(Constants.FIELD_AIRLINE_CODE);
                            
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

    // New analytics event: flight_search (airline_code, flight_number, success)
    private void logFlightSearchAnalytics(String airlineCode, String flightNumber, boolean success) {
        if (mFirebaseAnalytics == null) return;
        Bundle b = new Bundle();
        b.putString("airline_code", airlineCode == null ? "" : airlineCode);
        b.putString("flight_number", flightNumber == null ? "" : flightNumber);
        b.putString("success", success ? "true" : "false");
        mFirebaseAnalytics.logEvent("flight_search", b);
        Log.d("FirebaseAnalytics", "Logged event 'flight_search' airline_code=" + airlineCode + ", flight_number=" + flightNumber + ", success=" + success);
    }
}
