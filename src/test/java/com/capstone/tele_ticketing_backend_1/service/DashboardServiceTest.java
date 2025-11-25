package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.DashboardActivityDto;
import com.capstone.tele_ticketing_backend_1.dto.DashboardStatsDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.entities.TicketActivity;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TicketRepo ticketRepo;

    @Mock
    private TicketActivityRepo activityRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private DashboardService dashboardService;

    private AppUser mockUser;
    private final String USERNAME = "test_user";

    @BeforeEach
    void setUp() {
        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setUsername(USERNAME);
        mockUser.setFullName("Test User");
    }

    // --- getDashboardStats Tests ---

    @Test
    void testGetDashboardStats_Success() {
        // Arrange
        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));

        // Mock counts
        when(ticketRepo.countByCreatedForAndStatusIn(eq(mockUser), anyList())).thenReturn(10L); // Active
        when(ticketRepo.countByCreatedForAndStatus(mockUser, TicketStatus.RESOLVED)).thenReturn(5L); // Resolved
        when(ticketRepo.countByCreatedForAndStatus(mockUser, TicketStatus.FIXED)).thenReturn(2L); // Feedback

        // Act
        DashboardStatsDto result = dashboardService.getDashboardStats(USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getActiveTickets());
        assertEquals(5L, result.getResolvedTickets());
        assertEquals(2L, result.getFeedbackRequiredTickets());

        // Verify repo interactions
        verify(userRepo).findByUsername(USERNAME);
        verify(ticketRepo).countByCreatedForAndStatusIn(eq(mockUser), anyList());
        verify(ticketRepo, times(2)).countByCreatedForAndStatus(eq(mockUser), any(TicketStatus.class));
    }

    @Test
    void testGetDashboardStats_UserNotFound() {
        // Arrange
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
                dashboardService.getDashboardStats("unknown")
        );
        assertEquals("User not found: unknown", exception.getMessage());

        // Verify no ticket interactions occurred
        verify(ticketRepo, never()).countByCreatedForAndStatusIn(any(), any());
    }

    // --- getRecentActivities Tests ---

    @Test
    void testGetRecentActivities_Success() {
        // Arrange
        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));

        // Create mock Ticket (required for DTO mapping)
        Ticket mockTicket = new Ticket();
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setTitle("Printer issue");

        // Create mock Activity
        TicketActivity activity = new TicketActivity();
        activity.setId(50L);
        activity.setDescription("Ticket created");
        activity.setCreatedAt(LocalDateTime.now());
        activity.setTicket(mockTicket); // Link ticket to activity

        when(activityRepo.findFirst8ByTicket_CreatedForAndInternalOnlyFalseOrderByCreatedAtDesc(mockUser))
                .thenReturn(List.of(activity));

        // Act
        List<DashboardActivityDto> result = dashboardService.getRecentActivities(USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        DashboardActivityDto dto = result.get(0);
        assertEquals(50L, dto.getActivityId());
        assertEquals("Ticket created", dto.getDescription());
        assertEquals("TKT-100", dto.getTicketUid());
        assertEquals("Printer issue", dto.getTicketTitle());
    }

    @Test
    void testGetRecentActivities_EmptyList() {
        // Arrange
        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(activityRepo.findFirst8ByTicket_CreatedForAndInternalOnlyFalseOrderByCreatedAtDesc(mockUser))
                .thenReturn(Collections.emptyList());

        // Act
        List<DashboardActivityDto> result = dashboardService.getRecentActivities(USERNAME);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRecentActivities_UserNotFound() {
        // Arrange
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                dashboardService.getRecentActivities("unknown")
        );

        verify(activityRepo, never()).findFirst8ByTicket_CreatedForAndInternalOnlyFalseOrderByCreatedAtDesc(any());
    }
}