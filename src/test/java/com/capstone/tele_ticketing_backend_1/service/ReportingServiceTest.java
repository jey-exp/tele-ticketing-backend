package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.AverageResolutionTimeDto;
import com.capstone.tele_ticketing_backend_1.dto.SatisfactionScoreDto;
import com.capstone.tele_ticketing_backend_1.dto.TimeSeriesDataPointDto;
import com.capstone.tele_ticketing_backend_1.projections.SatisfactionScoreProjection;
import com.capstone.tele_ticketing_backend_1.projections.TicketVolumeProjection;
import com.capstone.tele_ticketing_backend_1.repo.FeedbackRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private TicketRepo ticketRepo;

    @Mock
    private FeedbackRepo feedbackRepo;

    @InjectMocks
    private ReportingService reportingService;

    // --- Ticket Volume Report Tests ---

    @Test
    void testGetTicketVolumeReport_Success() {
        // Arrange
        // We mock the Projection interface because we can't instantiate it directly
        TicketVolumeProjection projection = mock(TicketVolumeProjection.class);
        LocalDate today = LocalDate.now();

        when(projection.getDate()).thenReturn(today);
        when(projection.getCount()).thenReturn(50L);

        when(ticketRepo.getTicketVolumeByDay(any(LocalDateTime.class)))
                .thenReturn(List.of(projection));

        // Act
        List<TimeSeriesDataPointDto> result = reportingService.getTicketVolumeReport();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).getValue());

        // Verify date formatting logic (MMM dd)
        String expectedDateString = today.format(DateTimeFormatter.ofPattern("MMM dd"));
        assertEquals(expectedDateString, result.get(0).getLabel());

        verify(ticketRepo).getTicketVolumeByDay(any(LocalDateTime.class));
    }

    @Test
    void testGetTicketVolumeReport_Empty() {
        // Arrange
        when(ticketRepo.getTicketVolumeByDay(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        List<TimeSeriesDataPointDto> result = reportingService.getTicketVolumeReport();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- Average Resolution Time Tests ---

    @Test
    void testGetAverageResolutionTimeReport_Success() {
        // Arrange
        Double expectedAvg = 4.5;
        when(ticketRepo.getAverageResolutionTimeInHours(any(LocalDateTime.class)))
                .thenReturn(expectedAvg);

        // Act
        AverageResolutionTimeDto result = reportingService.getAverageResolutionTimeReport();

        // Assert
        assertNotNull(result);
        assertEquals(4.5, result.getAverageResolutionHours());
    }

    @Test
    void testGetAverageResolutionTimeReport_NoData_ReturnsZero() {
        // Arrange
        // Simulate DB returning null (no closed tickets)
        when(ticketRepo.getAverageResolutionTimeInHours(any(LocalDateTime.class)))
                .thenReturn(null);

        // Act
        AverageResolutionTimeDto result = reportingService.getAverageResolutionTimeReport();

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getAverageResolutionHours(), "Should default to 0.0 when repo returns null");
    }

    // --- Satisfaction Score Report Tests ---

    @Test
    void testGetSatisfactionScoreReport_Success() {
        // Arrange
        SatisfactionScoreProjection p1 = mock(SatisfactionScoreProjection.class);
        when(p1.getRating()).thenReturn(5);
        when(p1.getCount()).thenReturn(20L);

        SatisfactionScoreProjection p2 = mock(SatisfactionScoreProjection.class);
        when(p2.getRating()).thenReturn(4);
        when(p2.getCount()).thenReturn(10L);

        when(feedbackRepo.getSatisfactionScoreDistribution())
                .thenReturn(List.of(p1, p2));

        // Act
        List<SatisfactionScoreDto> result = reportingService.getSatisfactionScoreReport();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(5, result.get(0).getRating());
        assertEquals(20L, result.get(0).getCount());

        assertEquals(4, result.get(1).getRating());
        assertEquals(10L, result.get(1).getCount());
    }

    @Test
    void testGetSatisfactionScoreReport_Empty() {
        // Arrange
        when(feedbackRepo.getSatisfactionScoreDistribution())
                .thenReturn(Collections.emptyList());

        // Act
        List<SatisfactionScoreDto> result = reportingService.getSatisfactionScoreReport();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}