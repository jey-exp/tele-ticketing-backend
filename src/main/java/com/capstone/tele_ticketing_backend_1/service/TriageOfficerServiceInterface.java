package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.AiTriageSuggestionDto;
import com.capstone.tele_ticketing_backend_1.dto.NotificationDto;
import com.capstone.tele_ticketing_backend_1.dto.TriageTicketRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import java.util.List;

public interface TriageOfficerServiceInterface {

    /**
     * Gets a summary of all tickets currently pending triage (CREATED, NEEDS_TRIAGING, REOPENED).
     * @return A list of ticket summaries.
     */
    List<TicketSummaryDto> getPendingTickets();

    /**
     * Allows a Triage Officer to assign, prioritize, and set the severity for a ticket.
     * @param ticketId The ID of the ticket to triage.
     * @param dto The DTO containing the priority, severity, and new assignee IDs.
     * @param triageOfficerUsername The username of the Triage Officer performing the action.
     * @return A DTO of the updated ticket.
     */
    TicketDetailDto triageTicket(Long ticketId, TriageTicketRequestDto dto, String triageOfficerUsername);

    /**
     * Calls an AI model to get triage suggestions for all pending tickets.
     * @return A list of AI triage suggestion DTOs.
     */
    List<AiTriageSuggestionDto> getAiTriageSuggestions();

    /**
     * Gets a feed of all relevant notifications for a Triage Officer.
     * @return A list of notification DTOs.
     */
    List<NotificationDto> getNotifications();
}