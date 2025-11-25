package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.ai.TriageAssistant;
import com.capstone.tele_ticketing_backend_1.dto.*;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.InvalidTicketStatusException;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageOfficerServiceTest {

    @Mock private TicketRepo ticketRepo;
    @Mock private UserRepo userRepo;
    @Mock private TicketService ticketService;
    @Mock private ActivityLogService activityLogService;
    @Mock private TriageAssistant triageAssistant;
    @Mock private TicketActivityRepo activityRepo;

    @InjectMocks
    private TriageOfficerService triageOfficerService;

    private AppUser mockTriageOfficer;
    private AppUser mockEngineer;
    private Ticket mockTicket;
    private final String TRIAGE_OFFICER_USERNAME = "officer_phil";

    @BeforeEach
    void setUp() {
        mockTriageOfficer = new AppUser();
        mockTriageOfficer.setId(10L);
        mockTriageOfficer.setUsername(TRIAGE_OFFICER_USERNAME);

        mockEngineer = new AppUser();
        mockEngineer.setId(20L);
        mockEngineer.setFullName("Eng Tony");

        mockTicket = new Ticket();
        mockTicket.setId(100L);
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setTitle("Urgent Issue");
        mockTicket.setStatus(TicketStatus.NEEDS_TRIAGING);
        mockTicket.setPriority(TicketPriority.LOW);
    }

    // --- getPendingTickets Tests ---

    @Test
    void testGetPendingTickets() {
        // Arrange
        when(ticketRepo.findAllByStatusIn(anyList())).thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = triageOfficerService.getPendingTickets();

        // Assert
        assertEquals(1, result.size());
        assertEquals("TKT-100", result.get(0).getTicketUid());
        verify(ticketRepo).findAllByStatusIn(anyList());
    }

    // --- triageTicket Tests ---

    @Test
    void testTriageTicket_Success_CriticalSeverity() {
        // Arrange
        Long ticketId = 100L;
        TriageTicketRequestDto dto = new TriageTicketRequestDto();
        dto.setPriority(TicketPriority.HIGH);
        dto.setSeverity(TicketSeverity.CRITICAL); // Should set SLA to 20 hours
        dto.setAssignedToUserIds(Set.of(20L));

        when(userRepo.findByUsername(TRIAGE_OFFICER_USERNAME)).thenReturn(Optional.of(mockTriageOfficer));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(userRepo.findAllById(dto.getAssignedToUserIds())).thenReturn(List.of(mockEngineer));

        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        triageOfficerService.triageTicket(ticketId, dto, TRIAGE_OFFICER_USERNAME);

        // Assert
        // Verify SLA Calculation
        assertEquals(20, mockTicket.getSlaDurationHours());
        assertNotNull(mockTicket.getSlaBreachAt());

        // Verify Status and Assignment
        assertEquals(TicketStatus.ASSIGNED, mockTicket.getStatus());
        assertTrue(mockTicket.getAssignedTo().contains(mockEngineer));
        assertEquals(mockTriageOfficer, mockTicket.getAssignedBy());

        // Verify Logs
        verify(activityLogService).createLog(eq(mockTicket), eq(mockTriageOfficer), eq(ActivityType.PRIORITY_CHANGE), contains("Priority changed"), eq(false));
        verify(activityLogService).createLog(eq(mockTicket), eq(mockTriageOfficer), eq(ActivityType.ASSIGNMENT), contains("Assigned to"), eq(true));
    }

    @Test
    void testTriageTicket_InvalidStatus() {
        // Arrange
        mockTicket.setStatus(TicketStatus.IN_PROGRESS); // Not a pending status

        when(userRepo.findByUsername(TRIAGE_OFFICER_USERNAME)).thenReturn(Optional.of(mockTriageOfficer));
        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));

        // Act & Assert
        InvalidTicketStatusException ex = assertThrows(InvalidTicketStatusException.class, () ->
                triageOfficerService.triageTicket(100L, new TriageTicketRequestDto(), TRIAGE_OFFICER_USERNAME)
        );
        assertTrue(ex.getMessage().contains("Ticket is not in a state that can be triaged"));
    }

    @Test
    void testTriageTicket_EngineerNotFound() {
        // Arrange
        TriageTicketRequestDto dto = new TriageTicketRequestDto();
        dto.setAssignedToUserIds(Set.of(99L)); // Missing engineer

        when(userRepo.findByUsername(TRIAGE_OFFICER_USERNAME)).thenReturn(Optional.of(mockTriageOfficer));
        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));
        when(userRepo.findAllById(anyList())).thenReturn(Collections.emptyList()); // Return empty list

        // Act & Assert
        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () ->
                triageOfficerService.triageTicket(100L, dto, TRIAGE_OFFICER_USERNAME)
        );
        assertTrue(ex.getMessage().contains("One or more specified engineers could not be found"));
    }

    // --- getAiTriageSuggestions Tests ---

    @Test
    void testGetAiTriageSuggestions_Success() {
        // Arrange
        TriageSuggestion mockSuggestion = new TriageSuggestion();
        mockSuggestion.setSuggestedPriority("HIGH");
        mockSuggestion.setSuggestedSeverity("CRITICAL");
        mockSuggestion.setSuggestedRole("ROLE_NOC_ENGINEER");

        when(ticketRepo.findAllByStatusIn(anyList())).thenReturn(List.of(mockTicket));
        when(triageAssistant.suggestTriage(anyString())).thenReturn(mockSuggestion);

        // Act
        List<AiTriageSuggestionDto> result = triageOfficerService.getAiTriageSuggestions();

        // Assert
        assertEquals(1, result.size());
        AiTriageSuggestionDto dto = result.get(0);
        assertEquals(TicketPriority.HIGH, dto.getSuggestedPriority());
        assertEquals(TicketSeverity.CRITICAL, dto.getSuggestedSeverity());
        assertEquals(ERole.ROLE_NOC_ENGINEER, dto.getSuggestedRole());
    }

    @Test
    void testGetAiTriageSuggestions_AiFailure_ReturnsSafeDefaults() {
        // Arrange
        when(ticketRepo.findAllByStatusIn(anyList())).thenReturn(List.of(mockTicket));
        // Simulate AI service throwing an error
        when(triageAssistant.suggestTriage(anyString())).thenThrow(new RuntimeException("AI Down"));

        // Act
        List<AiTriageSuggestionDto> result = triageOfficerService.getAiTriageSuggestions();

        // Assert
        assertEquals(1, result.size());
        AiTriageSuggestionDto dto = result.get(0);

        // Should contain error message in title
        assertTrue(dto.getTitle().contains("AI FAILED"));

        // Should have safe default values
        assertEquals(TicketPriority.LOW, dto.getSuggestedPriority());
        assertEquals(TicketSeverity.TRIVIAL, dto.getSuggestedSeverity());
    }

    // --- getNotifications Tests ---

    @Test
    void testGetNotifications() {
        // Arrange
        TicketActivity activity = new TicketActivity();
        activity.setId(5L);
        activity.setTicket(mockTicket);
        activity.setDescription("Reopened by user");
        activity.setActivityType(ActivityType.REOPENED);
        activity.setCreatedAt(LocalDateTime.now());

        when(activityRepo.findAllByActivityTypeInOrderByCreatedAtDesc(anyList()))
                .thenReturn(List.of(activity));

        // Act
        List<NotificationDto> result = triageOfficerService.getNotifications();

        // Assert
        assertEquals(1, result.size());
        assertEquals("REOPENED", result.get(0).getActivityType());
    }
}