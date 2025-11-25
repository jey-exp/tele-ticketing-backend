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
class AgentDashboardServiceTest {

    @Mock
    private TicketRepo ticketRepo;

    @Mock
    private TicketActivityRepo activityRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private AgentDashboardService agentDashboardService;

    private AppUser mockAgent;
    private String agentUsername = "agent007";

    @BeforeEach
    void setUp() {
        mockAgent = new AppUser();
        mockAgent.setId(1L);
        mockAgent.setUsername(agentUsername);
        mockAgent.setFullName("James Bond");
    }

    // --- getDashboardStats Tests ---

    @Test
    void testGetDashboardStats_Success() {
        // Arrange
        when(userRepo.findByUsername(agentUsername)).thenReturn(Optional.of(mockAgent));

        // Mock counts
        when(ticketRepo.countByCreatedByAndStatusIn(eq(mockAgent), anyList())).thenReturn(10L); // Active
        when(ticketRepo.countByCreatedByAndStatus(mockAgent, TicketStatus.RESOLVED)).thenReturn(5L); // Resolved
        when(ticketRepo.countByCreatedByAndStatus(mockAgent, TicketStatus.FIXED)).thenReturn(2L); // Feedback

        // Act
        DashboardStatsDto result = agentDashboardService.getDashboardStats(agentUsername);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getActiveTickets());
        assertEquals(5L, result.getResolvedTickets());
        assertEquals(2L, result.getFeedbackRequiredTickets());

        // Verify calls
        verify(userRepo).findByUsername(agentUsername);
        verify(ticketRepo).countByCreatedByAndStatusIn(eq(mockAgent), anyList());
        verify(ticketRepo, times(2)).countByCreatedByAndStatus(eq(mockAgent), any(TicketStatus.class));
    }

    @Test
    void testGetDashboardStats_UserNotFound() {
        // Arrange
        when(userRepo.findByUsername(agentUsername)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
                agentDashboardService.getDashboardStats(agentUsername)
        );

        assertEquals("Agent not found: " + agentUsername, exception.getMessage());
        verify(ticketRepo, never()).countByCreatedByAndStatusIn(any(), any());
    }

    // --- getRecentActivities Tests ---

    @Test
    void testGetRecentActivities_Success() {
        // Arrange
        when(userRepo.findByUsername(agentUsername)).thenReturn(Optional.of(mockAgent));

        // Create mock Ticket for the activity
        Ticket mockTicket = new Ticket();
        mockTicket.setTicketUid("TKT-123");
        mockTicket.setTitle("Wifi Issue");

        // Create mock Activity
        TicketActivity activity = new TicketActivity();
        activity.setId(100L);
        activity.setDescription("Ticket Created");
        activity.setCreatedAt(LocalDateTime.now());
        activity.setTicket(mockTicket); // Important for mapping!
        activity.setInternalOnly(false);

        when(activityRepo.findFirst8ByTicket_CreatedByAndInternalOnlyFalseOrderByCreatedAtDesc(mockAgent))
                .thenReturn(List.of(activity));

        // Act
        List<DashboardActivityDto> result = agentDashboardService.getRecentActivities(agentUsername);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        DashboardActivityDto dto = result.get(0);
        assertEquals(100L, dto.getActivityId());
        assertEquals("Ticket Created", dto.getDescription());
        assertEquals("TKT-123", dto.getTicketUid());
        assertEquals("Wifi Issue", dto.getTicketTitle());
    }

    @Test
    void testGetRecentActivities_UserNotFound() {
        // Arrange
        when(userRepo.findByUsername(agentUsername)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
                agentDashboardService.getRecentActivities(agentUsername)
        );

        assertEquals("Agent not found: " + agentUsername, exception.getMessage());
        verify(activityRepo, never()).findFirst8ByTicket_CreatedByAndInternalOnlyFalseOrderByCreatedAtDesc(any());
    }

    @Test
    void testGetRecentActivities_UnexpectedError() {
        // Arrange
        when(userRepo.findByUsername(agentUsername)).thenReturn(Optional.of(mockAgent));
        // Force a RuntimeException from the repo
        when(activityRepo.findFirst8ByTicket_CreatedByAndInternalOnlyFalseOrderByCreatedAtDesc(mockAgent))
                .thenThrow(new RuntimeException("Database down"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                agentDashboardService.getRecentActivities(agentUsername)
        );

        assertEquals("Database down", exception.getMessage());
    }
}