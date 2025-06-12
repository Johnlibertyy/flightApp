package com.example.flightapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText; // Important: Import EditText

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    // 1. Declare the FirebaseAnalytics instance here
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText flightNumberInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge is fine, but for clarity in this example, I'm removing it.
        setContentView(R.layout.activity_main);

        // 2. Initialize FirebaseAnalytics and Views in onCreate
        // It's best practice to initialize this in onCreate, not at declaration.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        flightNumberInput = findViewById(R.id.textInput); // Assuming R.id.textInput is an EditText
        Button findFlightButton = findViewById(R.id.findWayButton);

        findFlightButton.setOnClickListener(v -> {
            // 3. Correctly get the text from the EditText
            String flightNumber = flightNumberInput.getText().toString().trim();

            // It's good practice to ensure the input isn't empty before proceeding
            if (flightNumber.isEmpty()) {
                flightNumberInput.setError("Flight number cannot be empty");
                return;
            }

            // 4. Log the custom event to Firebase Analytics
            logFlightSearchEvent(flightNumber);

            // Proceed with your existing logic
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("FLIGHT_NUMBER", flightNumber);
            startActivity(intent);
        });
    }

    /**
     * Logs a custom event to Firebase Analytics to track a flight search.
     * @param flightNumber The flight number entered by the user.
     */
    private void logFlightSearchEvent(String flightNumber) {
        // Create a Bundle to hold the event parameters
        Bundle bundle = new Bundle();
        // Use a descriptive custom parameter name
        bundle.putString("flight_number_searched", flightNumber);

        // Log the custom event with a descriptive name
        mFirebaseAnalytics.logEvent("find_flight_button_click", bundle);
        Log.d("FirebaseAnalytics", "Logged event 'find_flight_button_click' for flight: " + flightNumber);
    }
}