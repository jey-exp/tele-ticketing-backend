package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.EngineerUpdateDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import java.util.List;

public interface EngineerServiceInterface {

    /**
     * Gets a summary of all active (ASSIGNED, IN_PROGRESS) tickets assigned to the engineer.
     * @param username The username of the engineer.
     * @return A list of active ticket summaries.
     */
    List<TicketSummaryDto> getAssignedTickets(String username);

    /**
     * Allows an engineer to update a ticket's status and add an activity log.
     * @param ticketId The ID of the ticket to update.
     * @param dto The DTO containing the new status and/or an update comment.
     * @param username The username of the engineer performing the update.
     * @return A DTO of the updated ticket.
     */
    TicketDetailDto updateTicket(Long ticketId, EngineerUpdateDto dto, String username);
}
