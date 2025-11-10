package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.entities.Team;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import java.util.List;

public interface TicketServiceInterface {

    /**
     * Retrieves the full details of a single ticket by its database ID.
     * @param ticketId The ID of the ticket.
     * @return A DTO containing the full ticket details.
     */
    TicketDetailDto getTicketById(Long ticketId);

    /**
     * Maps a Ticket entity to a safe, serializable TicketDetailDto.
     * This method is public so it can be reused by other services.
     * @param ticket The Ticket entity to map.
     * @return A DTO containing the full ticket details.
     */
    TicketDetailDto mapTicketToDetailDto(Ticket ticket);

    /**
     * Retrieves a list of all teams.
     * @return A list of Team entities.
     */
    List<Team> getAllTeams();

    /**
     * Retrieves the full details of a single ticket by its public-facing UID.
     * @param ticketUid The UID of the ticket (e.g., "TK-12345").
     * @return A DTO containing the full ticket details.
     */
    TicketDetailDto getTicketByUid(String ticketUid);
}