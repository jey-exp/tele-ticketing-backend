package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.Coordinates;

public interface GeocodingServiceInterface {

    /**
     * Converts address components into geographic coordinates.
     * @param city The city name.
     * @param state The state name.
     * @param postalCode The postal code.
     * @return A Coordinates object containing latitude and longitude, or null if not found.
     */
    Coordinates getCoordinates(String city, String state, String postalCode);
}