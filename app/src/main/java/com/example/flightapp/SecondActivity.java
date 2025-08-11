package com.example.flightapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SecondActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge mode (if needed)
        EdgeToEdge.enable(this);
        setContentView(R.layout.second_activity);

        // Setup Back button click
        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // Go back to MainActivity
            }
        });

        // Setup Itinerary button click (shows a Toast message)
        Button btnItinerary = findViewById(R.id.btnItinerary);
        btnItinerary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(SecondActivity.this, "Itinerary button clicked", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Map button click: Launch MapActivity which displays an image.
        Button btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SecondActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });

        // Get flight data from Intent
    String flightNumber = getIntent().getStringExtra(Constants.EXTRA_FLIGHT_NUMBER);
    String airlineName = getIntent().getStringExtra(Constants.EXTRA_AIRLINE_NAME);
    String airlineCode = getIntent().getStringExtra(Constants.EXTRA_AIRLINE_CODE);
        
        // Create flight display string with user's actual data from database
        String flightInfo = "Flight Not Found";
        if (flightNumber != null && !flightNumber.isEmpty()) {
            if (airlineCode != null && !airlineCode.isEmpty()) {
                // Use airline code from database
                flightInfo = airlineCode + flightNumber;
            } else {
                // Fallback to just flight number if no code available
                flightInfo = flightNumber;
            }
        }

        RecyclerView timelineRecycler = findViewById(R.id.timelineRecycler);
        timelineRecycler.setLayoutManager(new LinearLayoutManager(this));

        List<TimelineItem> timelineItems = new ArrayList<>();
        timelineItems.add(new TimelineItem("Airport arrival", "Terminal 2", "08:55 am recommended", true));
        
        // Update check-in desk based on selected airline
        String checkInDesk = (airlineName != null && !airlineName.isEmpty()) 
            ? airlineName + " Desk Level 04" 
            : "Check-in Desk Level 04";
        timelineItems.add(new TimelineItem("Check-in & baggage drop", checkInDesk, "10:25 am end of baggage drop", true));
        
        timelineItems.add(new TimelineItem("Security check", "No specific time", "", false));
        timelineItems.add(new TimelineItem("Gate K6", "", "10:45 am close", false));
        timelineItems.add(new TimelineItem("Plane", flightInfo, "10:55 am departure", false));

        TimelineAdapter adapter = new TimelineAdapter(timelineItems);
        timelineRecycler.setAdapter(adapter);


        timelineRecycler.addItemDecoration(new TimelineItemDecoration(this));
    }
}
