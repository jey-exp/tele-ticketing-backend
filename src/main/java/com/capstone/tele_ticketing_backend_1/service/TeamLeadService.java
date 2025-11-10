package com.capstone.tele_ticketing_backend_1.service;



import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.ReassignTicketDto;
import com.capstone.tele_ticketing_backend_1.dto.TeamDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TeamMemberUpdateRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.ActivityType;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.Team;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.exceptions.AuthorizationException;
import com.capstone.tele_ticketing_backend_1.exceptions.BadRequestException;
import com.capstone.tele_ticketing_backend_1.exceptions.ResourceNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TeamRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamLeadService implements TeamLeadServiceInterface {

    private final UserRepo userRepo;
    private final TeamRepo teamRepo;
    private final TicketRepo ticketRepo;
    private final TicketService ticketService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<TicketSummaryDto> getActiveTeamTickets(String teamLeadUsername) {
        log.info("Fetching active team tickets for team lead: {}", teamLeadUsername);
        Team team = findTeamByLead(teamLeadUsername);
        List<TicketStatus> activeStatuses = List.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);
        List<Ticket> tickets = ticketRepo.findTicketsByTeamAndStatus(team.getId(), activeStatuses);
        log.debug("Found {} active tickets for team: {}", tickets.size(), team.getName());
        return tickets.stream().map(this::mapTicketToSummaryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryDto> getSlaRiskTeamTickets(String teamLeadUsername) {
        log.info("Fetching SLA risk tickets for team lead: {}", teamLeadUsername);
        Team team = findTeamByLead(teamLeadUsername);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursFromNow = now.plusHours(2);
        List<Ticket> tickets = ticketRepo.findSlaRiskTicketsByTeam(team.getId(), now, twoHoursFromNow);
        log.warn("Found {} SLA risk tickets for team: {}", tickets.size(), team.getName());
        return tickets.stream().map(this::mapTicketToSummaryDto).collect(Collectors.toList());
    }

    @Transactional
    public TicketDetailDto reassignTicket(Long ticketId, ReassignTicketDto dto, String teamLeadUsername) {
        log.info("Team lead {} attempting to reassign ticket {}", teamLeadUsername, ticketId);
        Team team = findTeamByLead(teamLeadUsername);
        AppUser teamLead = team.getTeamLead();
        Ticket ticket = ticketRepo.findById(ticketId).orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        // Security Check 1: Ensure the ticket belongs to the team lead's team.
        boolean ticketBelongsToTeam = ticket.getAssignedTo().stream().anyMatch(user -> user.getTeam() != null && user.getTeam().getId().equals(team.getId()));
        if (!ticketBelongsToTeam) {
            log.warn("Unauthorized reassignment attempt by team lead {} for ticket {}", teamLeadUsername, ticketId);
            throw new AuthorizationException("You can only reassign tickets within your own team.");
        }

        List<AppUser> newAssignees = userRepo.findAllById(dto.getNewAssigneeUserIds());
        // Security Check 2: Ensure all new assignees are part of the team.
        for (AppUser assignee : newAssignees) {
            if (assignee.getTeam() == null || !assignee.getTeam().getId().equals(team.getId())) {
                throw new BadRequestException("User " + assignee.getUsername() + " is not a member of your team.");
            }
        }

        String oldAssignees = ticket.getAssignedTo().stream().map(AppUser::getFullName).collect(Collectors.joining(", "));
        ticket.setAssignedTo(new HashSet<>(newAssignees));
        Ticket savedTicket = ticketRepo.save(ticket);

        String newAssigneesNames = newAssignees.stream().map(AppUser::getFullName).collect(Collectors.joining(", "));
        log.info("Successfully reassigned ticket {} from [{}] to [{}]", ticketId, oldAssignees, newAssigneesNames);
        activityLogService.createLog(savedTicket, teamLead, ActivityType.ASSIGNMENT, "Re-assigned from [" + oldAssignees + "] to [" + newAssigneesNames + "]", true);

        return ticketService.mapTicketToDetailDto(savedTicket);
    }

    @Transactional
    public Team updateTeamMembers(TeamMemberUpdateRequestDto dto, String teamLeadUsername) {
        Team team = findTeamByLead(teamLeadUsername);

        if (dto.getUserIdsToAdd() != null) {
            userRepo.findAllById(dto.getUserIdsToAdd()).forEach(user -> user.setTeam(team));
        }
        if (dto.getUserIdsToRemove() != null) {
            userRepo.findAllById(dto.getUserIdsToRemove()).forEach(user -> {
                // Security Check: Ensure team lead isn't removing a member from another team.
                if (user.getTeam() != null && user.getTeam().getId().equals(team.getId())) {
                    user.setTeam(null);
                }
            });
        }
        return teamRepo.save(team);
    }

    private Team findTeamByLead(String username) {
        AppUser teamLead = userRepo.findByUsername(username).orElseThrow(() -> new UserNotFoundException("Team Lead not found"));
        Team team = teamRepo.findByTeamLead(teamLead).orElseThrow(() -> new BadRequestException("You are not registered as a lead of any team."));
        return team;
    }

    private TicketSummaryDto mapTicketToSummaryDto(Ticket ticket) {
        return new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getTeamMembers(String teamLeadUsername) {
        Team team = findTeamByLead(teamLeadUsername);
        return team.getMembers().stream()
                .map(member -> new UserSummaryDto(member.getId(), member.getUsername(), member.getFullName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamDetailDto getTeamDetails(String teamLeadUsername) {
        AppUser teamLead = userRepo.findByUsername(teamLeadUsername).orElseThrow(() -> new UserNotFoundException("Team Lead not found"));
        // This will throw if no team is found, which we'll handle on the frontend.
        Team team = teamRepo.findByTeamLead(teamLead).orElseThrow(() -> new ResourceNotFoundException("Team not found for the current user."));

        return mapTeamToDetailDto(team);
    }

    @Transactional
    public TeamDetailDto updateTeam(TeamMemberUpdateRequestDto dto, String teamLeadUsername) {
        AppUser teamLead = userRepo.findByUsername(teamLeadUsername)
                .orElseThrow(() -> new UserNotFoundException("Team Lead not found"));

        // Find existing team or create a new one if adding members for the first time.
        Team team = teamRepo.findByTeamLead(teamLead).orElseGet(() -> {
            Team newTeam = new Team();
            newTeam.setTeamLead(teamLead);
            newTeam.setName(teamLead.getFullName() + "'s Team"); // Set name automatically
            return newTeam;
        });

        // Add new members
        if (dto.getUserIdsToAdd() != null) {
            userRepo.findAllById(dto.getUserIdsToAdd()).forEach(user -> user.setTeam(team));
        }

        // Remove members
        if (dto.getUserIdsToRemove() != null) {
            userRepo.findAllById(dto.getUserIdsToRemove()).forEach(user -> {
                if (user.getTeam() != null && user.getTeam().getId().equals(team.getId())) {
                    user.setTeam(null);
                }
            });
        }

        Team savedTeam = teamRepo.save(team);
        return mapTeamToDetailDto(savedTeam);
    }

    private TeamDetailDto mapTeamToDetailDto(Team team) {
        TeamDetailDto dto = new TeamDetailDto();
        dto.setId(team.getId());
        dto.setName(team.getName());

        List<UserSummaryDto> members = team.getMembers().stream()
                .map(member -> new UserSummaryDto(member.getId(), member.getUsername(), member.getFullName()))
                .collect(Collectors.toList());

        // Dr. X's Fix: Always include the team lead in the member list for the frontend.
        members.add(0, new UserSummaryDto(team.getTeamLead().getId(), team.getTeamLead().getUsername(), team.getTeamLead().getFullName()));

        dto.setMembers(members);
        return dto;
    }
}