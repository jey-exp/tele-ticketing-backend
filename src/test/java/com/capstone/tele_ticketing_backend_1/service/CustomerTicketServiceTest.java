package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.*;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.*;
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
class CustomerTicketServiceTest {

    @Mock private TicketRepo ticketRepo;
    @Mock private UserRepo userRepo;
    @Mock private TicketService ticketService;       // Mocking the helper service
    @Mock private ActivityLogService activityLogService; // Mocking the logging service
    @Mock private TicketActivityRepo activityRepo;

    @InjectMocks
    private CustomerTicketService customerTicketService;

    private AppUser mockUser;
    private Ticket mockTicket;
    private final String USERNAME = "john_doe";

    @BeforeEach
    void setUp() {
        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setUsername(USERNAME);
        mockUser.setFullName("John Doe");

        mockTicket = new Ticket();
        mockTicket.setId(100L);
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setTitle("Internet Down");
        mockTicket.setCreatedFor(mockUser);
        mockTicket.setCreatedBy(mockUser);
        mockTicket.setStatus(TicketStatus.CREATED);
        mockTicket.setCreatedAt(LocalDateTime.now());
    }

    // --- createTicket Tests ---

    @Test
    void testCreateTicket_Success() {
        // Arrange
        CreateTicketRequestDto dto = new CreateTicketRequestDto();
        dto.setTitle("No Internet");
        dto.setDescription("Router is blinking red");
        dto.setCategory(TicketCategory.NETWORK_CONNECTIVITY);
        dto.setAttachments(List.of("http://cloud-storage.com/image.png"));

        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]); // Return what is saved
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        TicketDetailDto result = customerTicketService.createTicket(dto, USERNAME);

        // Assert
        assertNotNull(result);

        // 1. Verify Activity Log
        verify(activityLogService).createLog(any(Ticket.class), eq(mockUser), eq(ActivityType.CREATION), anyString(), eq(false));

        // 2. Verify Attachments
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepo).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();

        assertFalse(savedTicket.getAttachments().isEmpty());
        assertEquals("http://cloud-storage.com/image.png", savedTicket.getAttachments().iterator().next().getFilePath());
    }

    @Test
    void testCreateTicket_UserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                customerTicketService.createTicket(new CreateTicketRequestDto(), "unknown")
        );
    }

    // --- addFeedback Tests ---

    @Test
    void testAddFeedback_PositiveRating_Resolved() {
        // Arrange
        Long ticketId = 100L;
        FeedbackRequestDto dto = new FeedbackRequestDto();
        dto.setRating(5);
        dto.setComment("Fixed perfectly!");

        mockTicket.setStatus(TicketStatus.FIXED); // Must be FIXED to accept feedback

        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        customerTicketService.addFeedback(ticketId, dto, USERNAME);

        // Assert
        assertEquals(TicketStatus.RESOLVED, mockTicket.getStatus());
        assertNotNull(mockTicket.getResolvedAt());
        assertNotNull(mockTicket.getFeedback());
        assertEquals(5, mockTicket.getFeedback().getRating());

        // Verify Log
        verify(activityLogService).createLog(any(Ticket.class), eq(mockUser), eq(ActivityType.RESOLUTION), contains("resolved"), eq(false));
    }

    @Test
    void testAddFeedback_NegativeRating_Reopened() {
        // Arrange
        Long ticketId = 100L;
        FeedbackRequestDto dto = new FeedbackRequestDto();
        dto.setRating(2); // Low rating
        dto.setComment("Still not working");

        mockTicket.setStatus(TicketStatus.FIXED);

        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        customerTicketService.addFeedback(ticketId, dto, USERNAME);

        // Assert
        assertEquals(TicketStatus.REOPENED, mockTicket.getStatus());

        // Verify Log
        verify(activityLogService).createLog(any(Ticket.class), eq(mockUser), eq(ActivityType.REOPENED), contains("reopened"), eq(false));
    }

    @Test
    void testAddFeedback_Unauthorized() {
        // Arrange
        Long ticketId = 100L;
        AppUser otherUser = new AppUser();
        otherUser.setId(99L); // Different ID
        mockTicket.setCreatedFor(otherUser); // Ticket belongs to someone else

        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));

        // Act & Assert
        AuthorizationException ex = assertThrows(AuthorizationException.class, () ->
                customerTicketService.addFeedback(ticketId, new FeedbackRequestDto(), USERNAME)
        );
        assertEquals("You are not authorized to add feedback to this ticket.", ex.getMessage());
    }

    @Test
    void testAddFeedback_InvalidStatus() {
        // Arrange
        Long ticketId = 100L;
        mockTicket.setStatus(TicketStatus.IN_PROGRESS); // Not FIXED

        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));

        // Act & Assert
        InvalidTicketStatusException ex = assertThrows(InvalidTicketStatusException.class, () ->
                customerTicketService.addFeedback(ticketId, new FeedbackRequestDto(), USERNAME)
        );
        assertTrue(ex.getMessage().contains("FIXED"));
    }

    // --- getCustomerActiveTickets Tests ---

    @Test
    void testGetCustomerActiveTickets() {
        // Arrange
        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(ticketRepo.findAllByCreatedForAndStatusIn(eq(mockUser), anyList()))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = customerTicketService.getCustomerActiveTickets(USERNAME);

        // Assert
        assertEquals(1, result.size());
        assertEquals("TKT-100", result.get(0).getTicketUid());
        verify(ticketRepo).findAllByCreatedForAndStatusIn(eq(mockUser), anyList());
    }

    // --- getCustomerFeedbackTickets Tests ---

    @Test
    void testGetCustomerFeedbackTickets() {
        // Arrange
        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(ticketRepo.findAllByCreatedForAndStatus(mockUser, TicketStatus.FIXED))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = customerTicketService.getCustomerFeedbackTickets(USERNAME);

        // Assert
        assertEquals(1, result.size());
        verify(ticketRepo).findAllByCreatedForAndStatus(mockUser, TicketStatus.FIXED);
    }

    // --- getNotifications Tests ---

    @Test
    void testGetNotifications() {
        // Arrange
        TicketActivity activity = new TicketActivity();
        activity.setId(50L);
        activity.setTicket(mockTicket);
        activity.setDescription("Updates");
        activity.setActivityType(ActivityType.STATUS_CHANGE);
        activity.setCreatedAt(LocalDateTime.now());

        when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
        when(activityRepo.findAllByTicket_CreatedForAndInternalOnlyFalseOrderByCreatedAtDesc(mockUser))
                .thenReturn(List.of(activity));

        // Act
        List<NotificationDto> result = customerTicketService.getNotifications(USERNAME);

        // Assert
        assertEquals(1, result.size());
        assertEquals("TKT-100", result.get(0).getTicketUid());
        assertEquals("STATUS_CHANGE", result.get(0).getActivityType());
    }
}