package com.capstone.tele_ticketing_backend_1.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.EngineerUpdateDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.ActivityType;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.entities.TicketActivity;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EngineerService implements EngineerServiceInterface {

    private final TicketRepo ticketRepo;
    private final UserRepo userRepo;
    private final TicketService ticketService;

    public List<TicketSummaryDto> getAssignedTickets(String username) {
        AppUser engineer = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        // Define the statuses that are considered "active work" for an engineer.
        List<TicketStatus> activeStatuses = List.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);

        List<Ticket> tickets = ticketRepo.findAllByAssignedToContainsAndStatusIn(engineer, activeStatuses);

        return tickets.stream()
                .map(ticket -> new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketDetailDto updateTicket(Long ticketId, EngineerUpdateDto dto, String username) {
        AppUser engineer = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // Create an activity log for the update text, if provided.
        if (dto.getUpdateText() != null && !dto.getUpdateText().isBlank()) {
            TicketActivity activity = new TicketActivity();
            activity.setTicket(ticket);
            activity.setUser(engineer);
            activity.setDescription(dto.getUpdateText());
            activity.setActivityType(ActivityType.COMMENT);
            ticket.getActivities().add(activity);
        }

        // Update the status and create a corresponding status change log.
        if (dto.getNewStatus() != ticket.getStatus()) {
            TicketActivity statusChangeActivity = new TicketActivity();
            statusChangeActivity.setTicket(ticket);
            statusChangeActivity.setUser(engineer);
            statusChangeActivity.setDescription("Status changed from " + ticket.getStatus() + " to " + dto.getNewStatus());
            statusChangeActivity.setActivityType(ActivityType.STATUS_CHANGE);
            ticket.getActivities().add(statusChangeActivity);
            ticket.setStatus(dto.getNewStatus());
        }

        Ticket savedTicket = ticketRepo.save(ticket);

        // Map the saved entity to our clean DTO before returning to prevent lazy loading errors.
        return ticketService.mapTicketToDetailDto(savedTicket);
    }

    private TicketDetailDto mapTicketToDetailDto(Ticket ticket) {
        TicketDetailDto dto = new TicketDetailDto();
        dto.setId(ticket.getId());
        dto.setTicketUid(ticket.getTicketUid());
        dto.setTitle(ticket.getTitle());
        dto.setStatus(ticket.getStatus());
        dto.setCategory(ticket.getCategory());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setResolvedAt(ticket.getResolvedAt());

        // Trigger the lazy load for the 'createdFor' user while the session is active.
        AppUser createdForUser = ticket.getCreatedFor();
        dto.setCreatedFor(new UserSummaryDto(createdForUser.getId(), createdForUser.getUsername(), createdForUser.getFullName()));

        return dto;
    }
}