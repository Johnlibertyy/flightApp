package com.example.flightapp;

/**
 * Central place for app-wide constant keys & collection names.
 */
public final class Constants {
    private Constants() {}

    // Intent Extras
    public static final String EXTRA_FLIGHT_NUMBER = "FLIGHT_NUMBER";
    public static final String EXTRA_AIRLINE_NAME = "AIRLINE_NAME";
    public static final String EXTRA_AIRLINE_CODE = "AIRLINE_CODE";

    // Firestore Collections / Fields
    public static final String COL_AIRLINES = "airlines";
    public static final String FIELD_AIRLINE_NAME = "name";
    public static final String FIELD_AIRLINE_CODE = "code";
}
