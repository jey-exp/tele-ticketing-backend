package com.capstone.tele_ticketing_backend_1.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.AgentCreateTicketRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.FeedbackRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.NotificationDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.ActivityType;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.Attachment;
import com.capstone.tele_ticketing_backend_1.entities.Feedback;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.entities.TicketActivity;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.exceptions.AuthorizationException;
import com.capstone.tele_ticketing_backend_1.exceptions.InvalidTicketStatusException;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.FeedbackRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class AgentTicketService implements AgentTicketServiceInterface {

    private final static String noAgent = "Agent not found";

    private final TicketRepo ticketRepo;
    private final UserRepo userRepo;
    private final TicketService ticketService;
    private final ActivityLogService activityLogService;
    private final TicketActivityRepo activityRepo;
    private final FeedbackRepo feedbackRepo;



    @Transactional
    public TicketDetailDto createTicketForCustomer(AgentCreateTicketRequestDto dto, String agentUsername) {
        AppUser agent = userRepo.findByUsername(agentUsername)
                .orElseThrow(() -> new UserNotFoundException(noAgent + agentUsername));

        AppUser customer = userRepo.findByUsername(dto.getCustomerUsername())
                .orElseThrow(() -> new UserNotFoundException("Customer not found: " + dto.getCustomerUsername()));

        Ticket ticket = new Ticket();
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setCategory(dto.getCategory());
        ticket.setStatus(TicketStatus.CREATED);
        ticket.setCreatedBy(agent);
        ticket.setCreatedFor(customer);

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            dto.getAttachments().forEach(attUrl -> {
                Attachment attachment = new Attachment();
                attachment.setFilePath(attUrl);
                attachment.setFileName("attachment_" + System.currentTimeMillis());
                attachment.setTicket(ticket);
                ticket.getAttachments().add(attachment);
            });
        }
        Ticket savedTicket = ticketRepo.save(ticket);

        // Dr. X's Fix: Now, we use the 'savedTicket' object, which is a persisted entity, to create the log.
        activityLogService.createLog(savedTicket, agent, ActivityType.CREATION, "Ticket was created by an agent on behalf of the customer.", false);

        // As per our established pattern, we return a DTO to prevent lazy loading exceptions.
        return ticketService.mapTicketToDetailDto(savedTicket);
    }

    public List<TicketSummaryDto> getAgentCreatedTickets(String agentUsername) {
        AppUser agent = userRepo.findByUsername(agentUsername)
                .orElseThrow(() -> new UserNotFoundException(noAgent + agentUsername));

        List<TicketStatus> activeStatuses = Arrays.asList(
                TicketStatus.CREATED,
                TicketStatus.ASSIGNED,
                TicketStatus.IN_PROGRESS,
                TicketStatus.NEEDS_TRIAGING,
                TicketStatus.REOPENED,
                TicketStatus.FIXED,
                TicketStatus.RESOLVED
        );

        // We use our new repository method here.
        List<Ticket> tickets = ticketRepo.findAllByCreatedByAndStatusIn(agent, activeStatuses);

        return tickets.stream()
                .map(ticket -> new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryDto> getAgentActiveTickets(String agentUsername) {
        AppUser agent = userRepo.findByUsername(agentUsername)
                .orElseThrow(() -> new UserNotFoundException(noAgent + agentUsername));

        List<TicketStatus> activeStatuses = List.of(
                TicketStatus.CREATED, TicketStatus.ASSIGNED,
                TicketStatus.IN_PROGRESS, TicketStatus.NEEDS_TRIAGING, TicketStatus.REOPENED
        );

        List<Ticket> tickets = ticketRepo.findAllByCreatedByAndStatusIn(agent, activeStatuses);

        return tickets.stream()
                .map(ticket -> new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt()))
                .toList();
    }

    // Dr. X's Addition: Method to get all notifications relevant to the agent.
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String agentUsername) {
        AppUser agent = userRepo.findByUsername(agentUsername)
                .orElseThrow(() -> new UserNotFoundException(noAgent + agentUsername));

        List<TicketActivity> activities = activityRepo.findAllByTicket_CreatedByAndInternalOnlyFalseOrderByCreatedAtDesc(agent);

        return activities.stream()
                .map(activity -> new NotificationDto(
                        activity.getId(),
                        activity.getTicket().getTicketUid(),
                        activity.getTicket().getTitle(),
                        activity.getDescription(),
                        activity.getActivityType().name(),
                        activity.getCreatedAt()
                ))
                .toList();
    }
    @Transactional
    public TicketDetailDto addFeedbackForCustomer(Long ticketId, FeedbackRequestDto dto, String agentUsername) {
        AppUser agent = userRepo.findByUsername(agentUsername)
                .orElseThrow(() -> new UserNotFoundException(noAgent + agentUsername));

        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // Dr. X's Note: THIS IS THE NEW, CRITICAL SECURITY CHECK.
        // We verify that the logged-in agent is the one who CREATED the ticket.
        if (!ticket.getCreatedBy().getId().equals(agent.getId())) {
            throw new AuthorizationException("You are not authorized to add feedback to this ticket.");
        }

        // State Check: Ensure the ticket is in the correct status.
        if (ticket.getStatus() != TicketStatus.FIXED) {
            throw new InvalidTicketStatusException("Feedback can only be added to tickets with status FIXED.");
        }

        // The customer for whom the ticket was created.
        AppUser customer = ticket.getCreatedFor();

        Feedback feedback = new Feedback();
        feedback.setRating(dto.getRating());
        feedback.setComment(dto.getComment());
        feedback.setTicket(ticket);

        // Data Integrity: The feedback is FROM the customer, even if submitted by the agent.
        feedback.setUser(customer);

        ticket.setFeedback(feedback);

        // Same business logic as before.
        if (dto.getRating() > 2) {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolvedAt(LocalDateTime.now());
            // Log this action clearly for auditing.
            activityLogService.createLog(ticket, agent, ActivityType.RESOLUTION, "Agent submitted a positive feedback rating (" + dto.getRating() + " stars) on behalf of the customer. Ticket resolved.", false);
        } else {
            ticket.setStatus(TicketStatus.REOPENED);
            activityLogService.createLog(ticket, agent, ActivityType.REOPENED, "Agent submitted a negative feedback rating (" + dto.getRating() + " stars) on behalf of the customer. Ticket reopened.", false);
        }

        Ticket savedTicket = ticketRepo.save(ticket);
        return ticketService.mapTicketToDetailDto(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryDto> getAgentFeedbackPendingTickets(String agentUsername) {
        AppUser agent = userRepo.findByUsername(agentUsername)
                .orElseThrow(() -> new UserNotFoundException(noAgent + agentUsername));

        List<Ticket> tickets = ticketRepo.findAllByCreatedByAndStatus(agent, TicketStatus.FIXED);

        return tickets.stream()
                .map(ticket -> new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt()))
                .toList();
    }
}
