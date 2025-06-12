package com.example.flightapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp; // Import FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText flightNumberInput;
    private Spinner airlineSpinner; // Added Spinner
    private FirebaseFirestore db; // Added Firestore instance
    private ArrayAdapter<String> airlineAdapter;
    private List<String> airlineList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase App first
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        flightNumberInput = findViewById(R.id.textInput);
        airlineSpinner = findViewById(R.id.airlineSpinner);
        Button findFlightButton = findViewById(R.id.findWayButton);

        // Initialize ArrayAdapter for the Spinner
        airlineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, airlineList);
        airlineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        airlineSpinner.setAdapter(airlineAdapter);

        fetchAirlines();

        findFlightButton.setOnClickListener(v -> {
            String flightNumber = flightNumberInput.getText().toString().trim();
            String selectedAirline = airlineSpinner.getSelectedItem() != null ? airlineSpinner.getSelectedItem().toString() : "";

            if (selectedAirline.isEmpty()) {
                // Optionally, show an error or prompt to select an airline
                FirebaseCrashlytics.getInstance().log("Airline not selected.");
                // You might want to set an error on the spinner or show a Toast
                // For now, we'll just log it and proceed or you can throw an error
                // throw new RuntimeException("Crash: Airline was not selected.");
                // For a non-crashing behavior, you might want to show a Toast:
                // Toast.makeText(MainActivity.this, "Please select an airline", Toast.LENGTH_SHORT).show();
                // return; // if you want to prevent proceeding without an airline
            }

            if (flightNumber.isEmpty()) {
                flightNumberInput.setError("Flight number cannot be empty");
                FirebaseCrashlytics.getInstance().log("Attempted to search with empty flight number.");
                FirebaseCrashlytics.getInstance().setCustomKey("empty_input_field", "flightNumberInput");
                throw new RuntimeException("Crash: Flight number was empty on search attempt.");
            }

            logFlightSearchEvent(flightNumber, selectedAirline);
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("FLIGHT_NUMBER", flightNumber);
            intent.putExtra("AIRLINE_NAME", selectedAirline); // Pass selected airline
            startActivity(intent);
        });
    }

    private void fetchAirlines() {
        db.collection("airlines") // Assuming your collection is named "airlines"
                .orderBy("name") // Assuming each document has a "name" field for the airline
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
                        if (!airlineList.isEmpty()) {
                            // Optionally, set a default selection or a prompt
                            // airlineSpinner.setSelection(0); // Selects the first item
                        } else {
                            // Handle case where no airlines are fetched
                            Log.w("Firestore", "No airlines found in the database.");
                            // You could add a placeholder or disable the spinner
                        }
                    } else {
                        Log.e("Firestore", "Error getting airlines: ", task.getException());
                        // Handle the error, e.g., show a Toast to the user
                        // Toast.makeText(MainActivity.this, "Error fetching airlines", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logFlightSearchEvent(String flightNumber, String airlineName) {
        Bundle bundle = new Bundle();
        bundle.putString("flight_number_searched", flightNumber);
        bundle.putString("airline_name_selected", airlineName); // Log selected airline
        mFirebaseAnalytics.logEvent("find_flight_button_click", bundle);
        Log.d("FirebaseAnalytics", "Logged event 'find_flight_button_click' for flight: " + flightNumber + " with airline: " + airlineName);
    }
}