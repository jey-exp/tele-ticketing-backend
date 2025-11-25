package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TeamRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepo ticketRepo;

    @Mock
    private TeamRepo teamRepo;

    @InjectMocks
    private TicketService ticketService;

    private AppUser mockUser;
    private AppUser mockAssignee;
    private Ticket mockTicket;

    @BeforeEach
    void setUp() {
        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setUsername("customer_jane");
        mockUser.setFullName("Jane Doe");

        mockAssignee = new AppUser();
        mockAssignee.setId(2L);
        mockAssignee.setUsername("eng_bob");
        mockAssignee.setFullName("Bob Builder");

        mockTicket = new Ticket();
        mockTicket.setId(100L);
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setTitle("VPN Issue");
        mockTicket.setDescription("Cannot connect");
        mockTicket.setStatus(TicketStatus.CREATED);
        mockTicket.setPriority(TicketPriority.HIGH); // Assuming Enum exists or String
        mockTicket.setCategory(TicketCategory.NETWORK_CONNECTIVITY); // Assuming Enum exists or String
        mockTicket.setCreatedFor(mockUser);
        mockTicket.setCreatedAt(LocalDateTime.now());

        // Initialize Sets to handle stream mapping
        Set<AppUser> assignees = new HashSet<>();
        assignees.add(mockAssignee);
        mockTicket.setAssignedTo(assignees);
    }

    // --- getTicketById Tests ---

    @Test
    void testGetTicketById_Success() {
        // Arrange
        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));

        // Act
        TicketDetailDto result = ticketService.getTicketById(100L);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("TKT-100", result.getTicketUid());
        assertEquals("customer_jane", result.getCreatedFor().getUsername());

        // Verify mapping of assigned users
        assertFalse(result.getAssignedTo().isEmpty());
        assertEquals("eng_bob", result.getAssignedTo().iterator().next().getUsername());

        verify(ticketRepo).findById(100L);
    }

    @Test
    void testGetTicketById_NotFound() {
        when(ticketRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
                ticketService.getTicketById(999L)
        );
    }

    // --- getTicketByUid Tests ---

    @Test
    void testGetTicketByUid_Success() {
        // Arrange
        when(ticketRepo.findByTicketUid("TKT-100")).thenReturn(Optional.of(mockTicket));

        // Act
        TicketDetailDto result = ticketService.getTicketByUid("TKT-100");

        // Assert
        assertNotNull(result);
        assertEquals("TKT-100", result.getTicketUid());
        verify(ticketRepo).findByTicketUid("TKT-100");
    }

    @Test
    void testGetTicketByUid_NotFound() {
        when(ticketRepo.findByTicketUid("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
                ticketService.getTicketByUid("UNKNOWN")
        );
    }

    // --- getAllTeams Tests ---

    @Test
    void testGetAllTeams() {
        // Arrange
        Team team1 = new Team();
        team1.setName("Alpha");
        when(teamRepo.findAll()).thenReturn(List.of(team1));

        // Act
        List<Team> result = ticketService.getAllTeams();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Alpha", result.get(0).getName());
        verify(teamRepo).findAll();
    }

    // --- Mapping Logic Tests (Specific Edge Cases) ---

    @Test
    void testMapTicketToDetailDto_WithAssignedBy_Success() {
        // Arrange
        AppUser assigner = new AppUser();
        assigner.setId(3L);
        assigner.setUsername("admin");
        assigner.setFullName("Admin User");

        mockTicket.setAssignedBy(assigner);

        // Act
        TicketDetailDto dto = ticketService.mapTicketToDetailDto(mockTicket);

        // Assert
        assertNotNull(dto.getAssignedBy());
        assertEquals("admin", dto.getAssignedBy().getUsername());
    }

    @Test
    void testMapTicketToDetailDto_WithoutAssignedBy_Success() {
        // Arrange
        mockTicket.setAssignedBy(null);

        // Act
        TicketDetailDto dto = ticketService.mapTicketToDetailDto(mockTicket);

        // Assert
        assertNull(dto.getAssignedBy());
    }
}