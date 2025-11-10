package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.CreateTicketRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.FeedbackRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.NotificationDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import java.util.List;

public interface CustomerTicketServiceInterface {

    /**
     * Creates a new ticket for the customer.
     * @param dto The DTO containing ticket details.
     * @param username The username of the customer creating the ticket.
     * @return A DTO of the newly created ticket.
     */
    TicketDetailDto createTicket(CreateTicketRequestDto dto, String username);

    /**
     * Submits feedback for a 'FIXED' ticket.
     * This will either move the ticket to 'RESOLVED' or 'REOPENED' based on the rating.
     * @param ticketId The ID of the ticket.
     * @param dto The feedback DTO (rating and comment).
     * @param username The username of the customer submitting the feedback.
     * @return A DTO of the updated ticket.
     */
    TicketDetailDto addFeedback(Long ticketId, FeedbackRequestDto dto, String username);

    /**
     * Gets a summary of all active (non-resolved) tickets for the customer.
     * @param username The username of the customer.
     * @return A list of active ticket summaries.
     */
    List<TicketSummaryDto> getCustomerActiveTickets(String username);

    /**
     * Gets a list of tickets for the customer that are in the 'FIXED' state, awaiting feedback.
     * @param username The username of the customer.
     * @return A list of ticket summaries.
     */
    List<TicketSummaryDto> getCustomerFeedbackTickets(String username);

    /**
     * Gets a list of all public notifications for tickets created for the customer.
     * @param username The username of the customer.
     * @return A list of notification DTOs.
     */
    List<NotificationDto> getNotifications(String username);
}