package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GeocodingServiceTest {

    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        // Since there are no dependencies to mock, we just create the instance
        geocodingService = new GeocodingService();
    }

    @Test
    void testGetCoordinates_Chennai_Success() {
        // Arrange
        String city = "Chennai";
        String state = "TN";
        String postalCode = "600001";

        // Act
        Coordinates result = geocodingService.getCoordinates(city, state, postalCode);

        // Assert
        assertNotNull(result, "Coordinates should not be null for Chennai");
        assertEquals(new BigDecimal("13.0827"), result.latitude());
        assertEquals(new BigDecimal("80.2707"), result.longitude());
    }

    @Test
    void testGetCoordinates_CaseInsensitive() {
        // Arrange - lowercase input
        String city = "chennai";

        // Act
        Coordinates result = geocodingService.getCoordinates(city, "TN", "600001");

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("13.0827"), result.latitude());
    }

    @Test
    void testGetCoordinates_OtherCity_ReturnsNull() {
        // Arrange
        String city = "Mumbai";

        // Act
        Coordinates result = geocodingService.getCoordinates(city, "MH", "400001");

        // Assert
        assertNull(result, "Service should return null for cities other than Chennai (mock logic)");
    }

    @Test
    void testGetCoordinates_NullCity_ReturnsNull() {
        // Act
        Coordinates result = geocodingService.getCoordinates(null, "State", "00000");

        // Assert
        assertNull(result);
    }
}