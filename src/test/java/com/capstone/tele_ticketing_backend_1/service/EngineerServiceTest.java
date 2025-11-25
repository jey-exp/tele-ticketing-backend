package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.EngineerUpdateDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngineerServiceTest {

    @Mock
    private TicketRepo ticketRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private TicketService ticketService; // Mock the external mapper service

    @InjectMocks
    private EngineerService engineerService;

    private AppUser mockEngineer;
    private Ticket mockTicket;
    private final String ENGINEER_USERNAME = "eng_steve";

    @BeforeEach
    void setUp() {
        mockEngineer = new AppUser();
        mockEngineer.setId(10L);
        mockEngineer.setUsername(ENGINEER_USERNAME);
        mockEngineer.setFullName("Steve Rogers");

        mockTicket = new Ticket();
        mockTicket.setId(100L);
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setTitle("Server Down");
        mockTicket.setStatus(TicketStatus.ASSIGNED);
        mockTicket.setCreatedAt(LocalDateTime.now());
        // Initialize the collection to avoid NullPointer in service methods
        mockTicket.setActivities(new HashSet<>());
    }

    // --- getAssignedTickets Tests ---

    @Test
    void testGetAssignedTickets_Success() {
        // Arrange
        when(userRepo.findByUsername(ENGINEER_USERNAME)).thenReturn(Optional.of(mockEngineer));

        // Return a list containing the mock ticket
        when(ticketRepo.findAllByAssignedToContainsAndStatusIn(eq(mockEngineer), anyList()))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = engineerService.getAssignedTickets(ENGINEER_USERNAME);

        // Assert
        assertEquals(1, result.size());
        assertEquals("TKT-100", result.get(0).getTicketUid());

        // Verify we asked for the correct Statuses (ASSIGNED, IN_PROGRESS)
        verify(ticketRepo).findAllByAssignedToContainsAndStatusIn(eq(mockEngineer), argThat(list ->
                list.contains(TicketStatus.ASSIGNED) && list.contains(TicketStatus.IN_PROGRESS)
        ));
    }

    @Test
    void testGetAssignedTickets_UserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                engineerService.getAssignedTickets("unknown")
        );
    }

    // --- updateTicket Tests ---

    @Test
    void testUpdateTicket_StatusChangeAndComment_Success() {
        // Arrange
        Long ticketId = 100L;
        EngineerUpdateDto dto = new EngineerUpdateDto();
        dto.setNewStatus(TicketStatus.IN_PROGRESS); // Changing from ASSIGNED
        dto.setUpdateText("Starting investigation"); // Adding comment

        when(userRepo.findByUsername(ENGINEER_USERNAME)).thenReturn(Optional.of(mockEngineer));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        engineerService.updateTicket(ticketId, dto, ENGINEER_USERNAME);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepo).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();

        // 1. Verify Status Updated
        assertEquals(TicketStatus.IN_PROGRESS, savedTicket.getStatus());

        // 2. Verify Activities Added
        Set<TicketActivity> activities = savedTicket.getActivities();
        assertEquals(2, activities.size()); // 1 for comment, 1 for status change

        // Verify specific activity types exist in the set
        boolean hasComment = activities.stream().anyMatch(a -> a.getActivityType() == ActivityType.COMMENT && a.getDescription().equals("Starting investigation"));
        boolean hasStatusChange = activities.stream().anyMatch(a -> a.getActivityType() == ActivityType.STATUS_CHANGE && a.getDescription().contains("Status changed from"));

        assertTrue(hasComment, "Comment activity missing");
        assertTrue(hasStatusChange, "Status change activity missing");
    }

    @Test
    void testUpdateTicket_OnlyComment_NoStatusChange() {
        // Arrange
        Long ticketId = 100L;
        EngineerUpdateDto dto = new EngineerUpdateDto();
        dto.setNewStatus(TicketStatus.ASSIGNED); // Same status as current
        dto.setUpdateText("Just a note");

        when(userRepo.findByUsername(ENGINEER_USERNAME)).thenReturn(Optional.of(mockEngineer));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        engineerService.updateTicket(ticketId, dto, ENGINEER_USERNAME);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepo).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();

        // Status should remain unchanged
        assertEquals(TicketStatus.ASSIGNED, savedTicket.getStatus());

        // Should only have 1 activity (Comment)
        assertEquals(1, savedTicket.getActivities().size());
        assertEquals(ActivityType.COMMENT, savedTicket.getActivities().iterator().next().getActivityType());
    }

    @Test
    void testUpdateTicket_UserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                engineerService.updateTicket(100L, new EngineerUpdateDto(), "unknown")
        );
    }

    @Test
    void testUpdateTicket_TicketNotFound() {
        when(userRepo.findByUsername(ENGINEER_USERNAME)).thenReturn(Optional.of(mockEngineer));
        when(ticketRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
                engineerService.updateTicket(999L, new EngineerUpdateDto(), ENGINEER_USERNAME)
        );
    }
}