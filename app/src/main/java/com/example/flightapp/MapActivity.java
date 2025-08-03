package com.example.flightapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // When the "Itinerary" button is clicked, finish this activity to go back to SecondActivity
        Button btnItinerary = findViewById(R.id.btnItinerary);
        btnItinerary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // This sends the user back to SecondActivity
            }
        });

    }
}
