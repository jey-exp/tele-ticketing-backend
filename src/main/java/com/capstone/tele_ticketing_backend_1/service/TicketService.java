package com.capstone.tele_ticketing_backend_1.service;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.Team;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TeamRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService  implements TicketServiceInterface{

    private final TicketRepo ticketRepo;
    private final TeamRepo teamRepo;

    @Transactional(readOnly = true)
    public TicketDetailDto getTicketById(Long ticketId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));
        return mapTicketToDetailDto(ticket);
    }


    public TicketDetailDto mapTicketToDetailDto(Ticket ticket) {
        TicketDetailDto dto = new TicketDetailDto();
        dto.setId(ticket.getId());
        dto.setTicketUid(ticket.getTicketUid());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setStatus(ticket.getStatus());
        dto.setCategory(ticket.getCategory());
        dto.setPriority(ticket.getPriority());
        dto.setSeverity(ticket.getSeverity());

        dto.setCreatedFor(new UserSummaryDto(
                ticket.getCreatedFor().getId(),
                ticket.getCreatedFor().getUsername(),
                ticket.getCreatedFor().getFullName()
        ));

        dto.setAssignedTo(ticket.getAssignedTo().stream()
                .map(user -> new UserSummaryDto(user.getId(), user.getUsername(), user.getFullName()))
                .collect(Collectors.toSet()));

        if (ticket.getAssignedBy() != null) {
            dto.setAssignedBy(new UserSummaryDto(
                    ticket.getAssignedBy().getId(),
                    ticket.getAssignedBy().getUsername(),
                    ticket.getAssignedBy().getFullName()
            ));
        }

        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setResolvedAt(ticket.getResolvedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepo.findAll();
    }

    @Transactional(readOnly = true)
    public TicketDetailDto getTicketByUid(String ticketUid) {
        Ticket ticket = ticketRepo.findByTicketUid(ticketUid)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with ID: " + ticketUid));
        return mapTicketToDetailDto(ticket); // Use our existing central mapper
    }
}