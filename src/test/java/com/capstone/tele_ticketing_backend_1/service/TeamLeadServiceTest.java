package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.*;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.*;
import com.capstone.tele_ticketing_backend_1.repo.TeamRepo;
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
class TeamLeadServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private TeamRepo teamRepo;
    @Mock private TicketRepo ticketRepo;
    @Mock private TicketService ticketService;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks
    private TeamLeadService teamLeadService;

    private AppUser mockTeamLead;
    private AppUser mockMember;
    private Team mockTeam;
    private Ticket mockTicket;
    private final String LEAD_USERNAME = "lead_steve";

    @BeforeEach
    void setUp() {
        mockTeamLead = new AppUser();
        mockTeamLead.setId(10L);
        mockTeamLead.setUsername(LEAD_USERNAME);
        mockTeamLead.setFullName("Steve Rogers");

        mockMember = new AppUser();
        mockMember.setId(20L);
        mockMember.setUsername("member_tony");
        mockMember.setFullName("Tony Stark");

        mockTeam = new Team();
        mockTeam.setId(1L);
        mockTeam.setName("Avengers");
        mockTeam.setTeamLead(mockTeamLead);

        // Setup team relationships
        mockMember.setTeam(mockTeam);
        mockTeam.setMembers(new HashSet<>(Collections.singletonList(mockMember)));

        mockTicket = new Ticket();
        mockTicket.setId(100L);
        mockTicket.setTicketUid("TKT-100");
        mockTicket.setAssignedTo(new HashSet<>(Collections.singletonList(mockMember))); // Assigned to team member
    }

    // --- getActiveTeamTickets Tests ---

    @Test
    void testGetActiveTeamTickets_Success() {
        // Arrange
        when(userRepo.findByUsername(LEAD_USERNAME)).thenReturn(Optional.of(mockTeamLead));
        when(teamRepo.findByTeamLead(mockTeamLead)).thenReturn(Optional.of(mockTeam));

        when(ticketRepo.findTicketsByTeamAndStatus(eq(1L), anyList()))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = teamLeadService.getActiveTeamTickets(LEAD_USERNAME);

        // Assert
        assertEquals(1, result.size());
        assertEquals("TKT-100", result.get(0).getTicketUid());
        verify(ticketRepo).findTicketsByTeamAndStatus(eq(1L), anyList());
    }

    @Test
    void testGetActiveTeamTickets_NoTeamFound() {
        when(userRepo.findByUsername(LEAD_USERNAME)).thenReturn(Optional.of(mockTeamLead));
        when(teamRepo.findByTeamLead(mockTeamLead)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                teamLeadService.getActiveTeamTickets(LEAD_USERNAME)
        );
    }

    // --- getSlaRiskTeamTickets Tests ---

    @Test
    void testGetSlaRiskTeamTickets_Success() {
        // Arrange
        when(userRepo.findByUsername(LEAD_USERNAME)).thenReturn(Optional.of(mockTeamLead));
        when(teamRepo.findByTeamLead(mockTeamLead)).thenReturn(Optional.of(mockTeam));

        when(ticketRepo.findSlaRiskTicketsByTeam(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = teamLeadService.getSlaRiskTeamTickets(LEAD_USERNAME);

        // Assert
        assertEquals(1, result.size());
        verify(ticketRepo).findSlaRiskTicketsByTeam(eq(1L), any(), any());
    }

    // --- reassignTicket Tests ---

    @Test
    void testReassignTicket_Success() {
        // Arrange
        Long ticketId = 100L;
        ReassignTicketDto dto = new ReassignTicketDto();
        dto.setNewAssigneeUserIds(Set.of(20L)); // Reassigning to the member (same ID is fine for logic test)

        when(userRepo.findByUsername(LEAD_USERNAME)).thenReturn(Optional.of(mockTeamLead));
        when(teamRepo.findByTeamLead(mockTeamLead)).thenReturn(Optional.of(mockTeam));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(userRepo.findAllById(anyList())).thenReturn(List.of(mockMember));

        when(ticketRepo.save(any(Ticket.class))).thenAnswer(i -> i.getArguments()[0]);
        when(ticketService.mapTicketToDetailDto(any(Ticket.class))).thenReturn(new TicketDetailDto());

        // Act
        teamLeadService.reassignTicket(ticketId, dto, LEAD_USERNAME);

        // Assert
        // Verify Activity Log created
        verify(activityLogService).createLog(eq(mockTicket), eq(mockTeamLead), eq(ActivityType.ASSIGNMENT), contains("Re-assigned"), eq(true));
        verify(ticketRepo).save(mockTicket);
    }

    @Test
    void testReassignTicket_Unauthorized_TicketNotInTeam() {
        // Arrange
        AppUser externalUser = new AppUser();
        externalUser.setId(99L);
        // User has no team, or a different team
        externalUser.setTeam(new Team());
        externalUser.getTeam().setId(99L);

        mockTicket.setAssignedTo(new HashSet<>(Collections.singletonList(externalUser)));

        when(userRepo.findByUsername(LEAD_USERNAME)).thenReturn(Optional.of(mockTeamLead));
        when(teamRepo.findByTeamLead(mockTeamLead)).thenReturn(Optional.of(mockTeam));
        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));

        // Act & Assert
        AuthorizationException ex = assertThrows(AuthorizationException.class, () ->
                teamLeadService.reassignTicket(100L, new ReassignTicketDto(), LEAD_USERNAME)
        );
        assertEquals("You can only reassign tickets within your own team.", ex.getMessage());
    }

    @Test
    void testReassignTicket_BadRequest_NewAssigneeNotInTeam() {
        // Arrange
        AppUser externalUser = new AppUser();
        externalUser.setId(99L);
        externalUser.setUsername("external_guy");
        // No team set for this user

        when(userRepo.findByUsername(LEAD_USERNAME)).thenReturn(Optional.of(mockTeamLead));
        when(teamRepo.findByTeamLead(mockTeamLead)).thenReturn(Optional.of(mockTeam));
        when(ticketRepo.findById(100L)).thenReturn(Optional.of(mockTicket));
        when(userRepo.findAllById(anyList())).thenReturn(List.of(externalUser));

        ReassignTicketDto dto = new ReassignTicketDto();
        dto.setNewAssigneeUserIds(Set.of(99L));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                teamLeadService.reassignTicket(100L, dto, LEAD_USERNAME)
        );
        assertTrue(ex.getMessage().contains("is not a member of your team"));
    }

    // --- updateTeam Tests (Using the robust method 'updateTeam') ---

    @Test
    void testUpdateTeam_AddAndRemoveMembers() {
        // Arrange
        TeamMemberUpdateRequestDto dto = new TeamMemberUpdateRequestDto();
        dto.setUserIdsToAdd(Set.of(30L));
        dto.setUserIdsToRemove(Set.of(20L));

        AppUser newUser = new AppUser();
        newUser.setId(30L); // New user to add

        when(userRepo.findByUsername(LEAD_USERNAME)).thenReturn(Optional.of(mockTeamLead));
        when(teamRepo.findByTeamLead(mockTeamLead)).thenReturn(Optional.of(mockTeam));

        when(userRepo.findAllById(dto.getUserIdsToAdd())).thenReturn(List.of(newUser));
        when(userRepo.findAllById(dto.getUserIdsToRemove())).thenReturn(List.of(mockMember));

        when(teamRepo.save(any(Team.class))).thenReturn(mockTeam);

        // Act
        TeamDetailDto result = teamLeadService.updateTeam(dto, LEAD_USERNAME);

        // Assert
        // Verify 'newUser' had setTeam called
        assertEquals(mockTeam, newUser.getTeam());

        // Verify 'mockMember' (removed user) had setTeam(null) called
        assertNull(mockMember.getTeam());

        verify(teamRepo).save(mockTeam);
    }
}