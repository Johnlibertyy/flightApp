package com.example.flightapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp; // Import FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MainActivity extends AppCompatActivity {
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText flightNumberInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase App first
        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        flightNumberInput = findViewById(R.id.textInput);
        Button findFlightButton = findViewById(R.id.findWayButton);

        findFlightButton.setOnClickListener(v -> {
            String flightNumber = flightNumberInput.getText().toString().trim();

            if (flightNumber.isEmpty()) {
                flightNumberInput.setError("Flight number cannot be empty");
                FirebaseCrashlytics.getInstance().log("Attempted to search with empty flight number.");
                FirebaseCrashlytics.getInstance().setCustomKey("empty_input_field", "flightNumberInput");
                throw new RuntimeException("Crash: Flight number was empty on search attempt.");
            }

            logFlightSearchEvent(flightNumber);
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("FLIGHT_NUMBER", flightNumber);
            startActivity(intent);
        });
    }

    private void logFlightSearchEvent(String flightNumber) {
        Bundle bundle = new Bundle();
        bundle.putString("flight_number_searched", flightNumber);
        mFirebaseAnalytics.logEvent("find_flight_button_click", bundle);
        Log.d("FirebaseAnalytics", "Logged event 'find_flight_button_click' for flight: " + flightNumber);
    }
}