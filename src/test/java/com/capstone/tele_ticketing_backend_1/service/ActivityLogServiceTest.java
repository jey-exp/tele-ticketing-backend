package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.ActivityLogDto;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.AuthorizationException;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private TicketActivityRepo activityRepo;

    @Mock
    private TicketRepo ticketRepo;

    @InjectMocks
    private ActivityLogService activityLogService;

    private AppUser mockUser;
    private Ticket mockTicket;
    private Role customerRole;
    private Role engineerRole;

    @BeforeEach
    void setUp() {
        // Setup Roles
        customerRole = new Role();
        customerRole.setName(ERole.ROLE_CUSTOMER);

        engineerRole = new Role();
        engineerRole.setName(ERole.ROLE_L1_ENGINEER);

        // Setup User (Default to Customer)
        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setUsername("john_doe");
        mockUser.setFullName("John Doe");
        mockUser.setRoles(Set.of(customerRole));

        // Setup Ticket
        mockTicket = new Ticket();
        mockTicket.setId(100L);
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setCreatedFor(mockUser); // User owns this ticket
        mockTicket.setCreatedBy(mockUser);
    }

    // --- Test createLog ---

    @Test
    void testCreateLog_Success() {
        // Arrange
        String description = "Ticket created";
        ActivityType type = ActivityType.CREATION;
        boolean isInternal = false;

        // Act
        activityLogService.createLog(mockTicket, mockUser, type, description, isInternal);

        // Assert - Verify repository was called with correct data
        ArgumentCaptor<TicketActivity> logCaptor = ArgumentCaptor.forClass(TicketActivity.class);
        verify(activityRepo, times(1)).save(logCaptor.capture());

        TicketActivity capturedLog = logCaptor.getValue();
        assertEquals(mockTicket, capturedLog.getTicket());
        assertEquals(mockUser, capturedLog.getUser());
        assertEquals(type, capturedLog.getActivityType());
        assertEquals(description, capturedLog.getDescription());
        assertEquals(isInternal, capturedLog.isInternalOnly());
    }

    // --- Test getLogsForTicket ---

    @Test
    void testGetLogsForTicket_TicketNotFound() {
        // Arrange
        Long ticketId = 999L;
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TicketNotFoundException.class, () ->
                activityLogService.getLogsForTicket(ticketId, mockUser)
        );

        verify(activityRepo, never()).findByTicketIdOrderByCreatedAtDesc(any());
    }

    @Test
    void testGetLogsForTicket_AsInternalUser_ShouldSeeAllLogs() {
        // Arrange
        // Change user role to Engineer
        mockUser.setRoles(Set.of(engineerRole));

        TicketActivity internalLog = new TicketActivity();
        internalLog.setId(1L);
        internalLog.setInternalOnly(true);
        internalLog.setActivityType(ActivityType.COMMENT);
        internalLog.setUser(mockUser);
        internalLog.setCreatedAt(LocalDateTime.now());

        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));
        // Should call the method that returns ALL logs
        when(activityRepo.findByTicketIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(internalLog));

        // Act
        List<ActivityLogDto> result = activityLogService.getLogsForTicket(100L, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(activityRepo).findByTicketIdOrderByCreatedAtDesc(100L);
        verify(activityRepo, never()).findByTicketIdAndInternalOnlyFalseOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void testGetLogsForTicket_AsCustomerOwner_ShouldSeeOnlyPublicLogs() {
        // Arrange
        // User is ROLE_CUSTOMER by default in setUp()

        TicketActivity publicLog = new TicketActivity();
        publicLog.setId(2L);
        publicLog.setInternalOnly(false); // Public
        publicLog.setActivityType(ActivityType.STATUS_CHANGE);
        publicLog.setUser(mockUser);
        publicLog.setCreatedAt(LocalDateTime.now());

        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));
        // Should call the method that filters InternalOnly = False
        when(activityRepo.findByTicketIdAndInternalOnlyFalseOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(publicLog));

        // Act
        List<ActivityLogDto> result = activityLogService.getLogsForTicket(100L, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(activityRepo).findByTicketIdAndInternalOnlyFalseOrderByCreatedAtDesc(100L);
        verify(activityRepo, never()).findByTicketIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void testGetLogsForTicket_AsCustomerNonOwner_ShouldThrowAuthorizationException() {
        // Arrange
        AppUser anotherUser = new AppUser();
        anotherUser.setId(2L); // Different ID

        // Ticket belongs to anotherUser
        mockTicket.setCreatedFor(anotherUser);
        mockTicket.setCreatedBy(anotherUser);

        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));

        // Act & Assert
        AuthorizationException exception = assertThrows(AuthorizationException.class, () ->
                activityLogService.getLogsForTicket(100L, mockUser)
        );

        assertEquals("You are not authorized to view this ticket's logs.", exception.getMessage());
        verify(activityRepo, never()).findByTicketIdAndInternalOnlyFalseOrderByCreatedAtDesc(anyLong());
    }
}