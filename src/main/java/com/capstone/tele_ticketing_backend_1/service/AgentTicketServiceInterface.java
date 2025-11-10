package com.capstone.tele_ticketing_backend_1.service;


import com.capstone.tele_ticketing_backend_1.dto.AgentCreateTicketRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.FeedbackRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.NotificationDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import java.util.List;

public interface AgentTicketServiceInterface {

    /**
     * Creates a new ticket on behalf of a customer.
     * @param dto The DTO containing ticket and customer details.
     * @param agentUsername The username of the agent creating the ticket.
     * @return A DTO of the newly created ticket.
     */
    TicketDetailDto createTicketForCustomer(AgentCreateTicketRequestDto dto, String agentUsername);

    /**
     * Gets a summary of all tickets created by the agent (active, resolved, etc.).
     * @param agentUsername The username of the agent.
     * @return A list of ticket summaries.
     */
    List<TicketSummaryDto> getAgentCreatedTickets(String agentUsername);

    /**
     * Gets a summary of all active (non-resolved) tickets created by the agent.
     * @param agentUsername The username of the agent.
     * @return A list of active ticket summaries.
     */
    List<TicketSummaryDto> getAgentActiveTickets(String agentUsername);

    /**
     * Gets a list of all public notifications for tickets created by the agent.
     * @param agentUsername The username of the agent.
     * @return A list of notification DTOs.
     */
    List<NotificationDto> getNotifications(String agentUsername);

    /**
     * Allows an agent to submit feedback on behalf of a customer for a 'FIXED' ticket.
     * @param ticketId The ID of the ticket.
     * @param dto The feedback DTO (rating and comment).
     * @param agentUsername The username of the agent submitting the feedback.
     * @return A DTO of the updated ticket (now RESOLVED or REOPENED).
     */
    TicketDetailDto addFeedbackForCustomer(Long ticketId, FeedbackRequestDto dto, String agentUsername);

    /**
     * Gets a list of tickets created by the agent that are in the 'FIXED' state, awaiting feedback.
     * @param agentUsername The username of the agent.
     * @return A list of ticket summaries.
     */
    List<TicketSummaryDto> getAgentFeedbackPendingTickets(String agentUsername);
}
