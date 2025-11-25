package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.*;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.*;
import com.capstone.tele_ticketing_backend_1.repo.FeedbackRepo;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentTicketServiceTest {

    @Mock private TicketRepo ticketRepo;
    @Mock private UserRepo userRepo;
    @Mock private TicketService ticketService; // External service dependency
    @Mock private ActivityLogService activityLogService; // External service dependency
    @Mock private TicketActivityRepo activityRepo;
    @Mock private FeedbackRepo feedbackRepo;

    @InjectMocks
    private AgentTicketService agentTicketService;

    private AppUser mockAgent;
    private AppUser mockCustomer;
    private Ticket mockTicket;
    private final String AGENT_USERNAME = "agent007";
    private final String CUSTOMER_USERNAME = "customer001";

    @BeforeEach
    void setUp() {
        mockAgent = new AppUser();
        mockAgent.setId(1L);
        mockAgent.setUsername(AGENT_USERNAME);

        mockCustomer = new AppUser();
        mockCustomer.setId(2L);
        mockCustomer.setUsername(CUSTOMER_USERNAME);

        mockTicket = new Ticket();
        mockTicket.setId(100L);
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setTitle("Internet Issue");
        mockTicket.setCreatedBy(mockAgent);
        mockTicket.setCreatedFor(mockCustomer);
        mockTicket.setStatus(TicketStatus.CREATED);
        mockTicket.setCreatedAt(LocalDateTime.now());
    }

    // --- createTicketForCustomer Tests ---

    @Test
    void testCreateTicketForCustomer_Success() {
        // Arrange
        AgentCreateTicketRequestDto dto = new AgentCreateTicketRequestDto();
        dto.setCustomerUsername(CUSTOMER_USERNAME);
        dto.setTitle("New Issue");
        dto.setDescription("Description");
        dto.setCategory(TicketCategory.NETWORK_CONNECTIVITY);
        dto.setAttachments(List.of("http://cloud.com/img.png"));

        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(userRepo.findByUsername(CUSTOMER_USERNAME)).thenReturn(Optional.of(mockCustomer));

        // Mock the save to return the ticket object
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);

        // Mock the mapper
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        TicketDetailDto result = agentTicketService.createTicketForCustomer(dto, AGENT_USERNAME);

        // Assert
        assertNotNull(result);

        // Verify Activity Log was called
        verify(activityLogService).createLog(any(Ticket.class), eq(mockAgent), eq(ActivityType.CREATION), anyString(), eq(false));

        // Verify Attachment logic (Capture the argument passed to save)
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepo).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();

        assertFalse(savedTicket.getAttachments().isEmpty());
        assertEquals("http://cloud.com/img.png", savedTicket.getAttachments().iterator().next().getFilePath());
    }

    @Test
    void testCreateTicketForCustomer_AgentNotFound() {
        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                agentTicketService.createTicketForCustomer(new AgentCreateTicketRequestDto(), AGENT_USERNAME)
        );
    }

    @Test
    void testCreateTicketForCustomer_CustomerNotFound() {
        AgentCreateTicketRequestDto dto = new AgentCreateTicketRequestDto();
        dto.setCustomerUsername("unknown_customer");

        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(userRepo.findByUsername("unknown_customer")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                agentTicketService.createTicketForCustomer(dto, AGENT_USERNAME)
        );
    }

    // --- getAgentCreatedTickets Tests ---

    @Test
    void testGetAgentCreatedTickets_Success() {
        // Arrange
        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(ticketRepo.findAllByCreatedByAndStatusIn(eq(mockAgent), anyList()))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = agentTicketService.getAgentCreatedTickets(AGENT_USERNAME);

        // Assert
        assertEquals(1, result.size());
        assertEquals("TKT-100", result.get(0).getTicketUid());
        verify(ticketRepo).findAllByCreatedByAndStatusIn(eq(mockAgent), anyList());
    }

    // --- getAgentActiveTickets Tests ---

    @Test
    void testGetAgentActiveTickets_Success() {
        // Arrange
        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(ticketRepo.findAllByCreatedByAndStatusIn(eq(mockAgent), anyList()))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = agentTicketService.getAgentActiveTickets(AGENT_USERNAME);

        // Assert
        assertEquals(1, result.size());
        verify(ticketRepo).findAllByCreatedByAndStatusIn(eq(mockAgent), anyList());
    }

    // --- getNotifications Tests ---

    @Test
    void testGetNotifications_Success() {
        // Arrange
        TicketActivity activity = new TicketActivity();
        activity.setId(50L);
        activity.setTicket(mockTicket);
        activity.setDescription("Log desc");
        activity.setActivityType(ActivityType.COMMENT);
        activity.setCreatedAt(LocalDateTime.now());

        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(activityRepo.findAllByTicket_CreatedByAndInternalOnlyFalseOrderByCreatedAtDesc(mockAgent))
                .thenReturn(List.of(activity));

        // Act
        List<NotificationDto> result = agentTicketService.getNotifications(AGENT_USERNAME);

        // Assert
        assertEquals(1, result.size());
        assertEquals("TKT-100", result.get(0).getTicketUid());
        assertEquals("COMMENT", result.get(0).getActivityType());
    }

    // --- addFeedbackForCustomer Tests (Complex Logic) ---

    @Test
    void testAddFeedback_PositiveRating_Resolved() {
        // Arrange
        Long ticketId = 100L;
        FeedbackRequestDto dto = new FeedbackRequestDto();
        dto.setRating(5); // Positive
        dto.setComment("Great job!");

        mockTicket.setStatus(TicketStatus.FIXED); // Must be FIXED

        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        agentTicketService.addFeedbackForCustomer(ticketId, dto, AGENT_USERNAME);

        // Assert
        assertEquals(TicketStatus.RESOLVED, mockTicket.getStatus());
        assertNotNull(mockTicket.getResolvedAt());
        assertNotNull(mockTicket.getFeedback());
        assertEquals(5, mockTicket.getFeedback().getRating());

        // Verify Log
        verify(activityLogService).createLog(eq(mockTicket), eq(mockAgent), eq(ActivityType.RESOLUTION), contains("positive feedback"), eq(false));
    }

    @Test
    void testAddFeedback_NegativeRating_Reopened() {
        // Arrange
        Long ticketId = 100L;
        FeedbackRequestDto dto = new FeedbackRequestDto();
        dto.setRating(2); // Negative (<=2)
        dto.setComment("Not fixed yet");

        mockTicket.setStatus(TicketStatus.FIXED); // Must be FIXED

        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        agentTicketService.addFeedbackForCustomer(ticketId, dto, AGENT_USERNAME);

        // Assert
        assertEquals(TicketStatus.REOPENED, mockTicket.getStatus());
        assertNotNull(mockTicket.getFeedback());
        assertEquals(2, mockTicket.getFeedback().getRating());

        // Verify Log
        verify(activityLogService).createLog(eq(mockTicket), eq(mockAgent), eq(ActivityType.REOPENED), contains("negative feedback"), eq(false));
    }

    @Test
    void testAddFeedback_Unauthorized_AgentDidNotCreate() {
        // Arrange
        Long ticketId = 100L;
        AppUser otherAgent = new AppUser();
        otherAgent.setId(99L); // Different ID
        mockTicket.setCreatedBy(otherAgent);

        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));

        // Act & Assert
        AuthorizationException ex = assertThrows(AuthorizationException.class, () ->
                agentTicketService.addFeedbackForCustomer(ticketId, new FeedbackRequestDto(), AGENT_USERNAME)
        );
        assertEquals("You are not authorized to add feedback to this ticket.", ex.getMessage());
    }

    @Test
    void testAddFeedback_InvalidStatus_NotFixed() {
        // Arrange
        Long ticketId = 100L;
        mockTicket.setStatus(TicketStatus.IN_PROGRESS); // Wrong status

        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));

        // Act & Assert
        InvalidTicketStatusException ex = assertThrows(InvalidTicketStatusException.class, () ->
                agentTicketService.addFeedbackForCustomer(ticketId, new FeedbackRequestDto(), AGENT_USERNAME)
        );
        assertTrue(ex.getMessage().contains("FIXED"));
    }

    @Test
    void testAddFeedback_TicketNotFound() {
        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        when(ticketRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
                agentTicketService.addFeedbackForCustomer(999L, new FeedbackRequestDto(), AGENT_USERNAME)
        );
    }

    // --- getAgentFeedbackPendingTickets Tests ---

    @Test
    void testGetAgentFeedbackPendingTickets_Success() {
        // Arrange
        when(userRepo.findByUsername(AGENT_USERNAME)).thenReturn(Optional.of(mockAgent));
        // Should query for FIXED status
        when(ticketRepo.findAllByCreatedByAndStatus(mockAgent, TicketStatus.FIXED))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = agentTicketService.getAgentFeedbackPendingTickets(AGENT_USERNAME);

        // Assert
        assertEquals(1, result.size());
        verify(ticketRepo).findAllByCreatedByAndStatus(mockAgent, TicketStatus.FIXED);
    }
}