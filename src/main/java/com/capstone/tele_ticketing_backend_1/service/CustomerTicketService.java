// File: com/capstone/tele_ticketing_backend_1/service/CustomerTicketService.java
package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.*;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.*;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerTicketService implements CustomerTicketServiceInterface {

    private final TicketRepo ticketRepo;
    private final UserRepo userRepo;
    private final TicketService ticketService;
    private final ActivityLogService activityLogService;
    private final TicketActivityRepo activityRepo;


    @Transactional
    public TicketDetailDto createTicket(CreateTicketRequestDto dto, String username) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Ticket ticket = new Ticket();
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setCategory(dto.getCategory());
        ticket.setStatus(TicketStatus.CREATED);
        ticket.setCreatedBy(user);
        ticket.setCreatedFor(user);

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

        // Dr. X's Addition: Create a public log entry for the ticket creation.
        activityLogService.createLog(savedTicket, user, ActivityType.CREATION, "Ticket was created.", false);

        return ticketService.mapTicketToDetailDto(savedTicket);
    }

    @Transactional
    public TicketDetailDto addFeedback(Long ticketId, FeedbackRequestDto dto, String username) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // ... security and state checks ...
        if (!ticket.getCreatedFor().getId().equals(user.getId())) {
            throw new AuthorizationException("You are not authorized to add feedback to this ticket.");
        }
        if (ticket.getStatus() != TicketStatus.FIXED) {
            throw new InvalidTicketStatusException("Feedback can only be added to tickets with status FIXED.");
        }

        Feedback feedback = new Feedback();
        feedback.setRating(dto.getRating());
        feedback.setComment(dto.getComment());
        feedback.setUser(user);
        feedback.setTicket(ticket);
        ticket.setFeedback(feedback);

        if (dto.getRating() > 2) {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolvedAt(LocalDateTime.now());
            // Dr. X's Addition: Create a public log for resolution.
            activityLogService.createLog(ticket, user, ActivityType.RESOLUTION, "Customer confirmed the fix. Ticket resolved.", false);
        } else {
            ticket.setStatus(TicketStatus.REOPENED);
            // Dr. X's Addition: Create a public log for reopening.
            activityLogService.createLog(ticket, user, ActivityType.REOPENED, "Customer reopened the ticket. Rating: " + dto.getRating() + " star(s).", false);
        }

        Ticket savedTicket = ticketRepo.save(ticket);
        return ticketService.mapTicketToDetailDto(savedTicket);
    }

    public List<TicketSummaryDto> getCustomerActiveTickets(String username) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        List<TicketStatus> activeStatuses = Arrays.asList(
                TicketStatus.CREATED,
                TicketStatus.ASSIGNED,
                TicketStatus.IN_PROGRESS,
                TicketStatus.NEEDS_TRIAGING,
                TicketStatus.REOPENED
        );

        List<Ticket> tickets = ticketRepo.findAllByCreatedForAndStatusIn(user, activeStatuses);

        // Dr. X's Note: Map entities to DTOs to control what data is exposed to the client.
        return tickets.stream()
                .map(ticket -> new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<TicketSummaryDto> getCustomerFeedbackTickets(String username) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        List<Ticket> tickets = ticketRepo.findAllByCreatedForAndStatus(user, TicketStatus.FIXED);

        return tickets.stream()
                .map(ticket -> new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String username) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        List<TicketActivity> activities = activityRepo.findAllByTicket_CreatedForAndInternalOnlyFalseOrderByCreatedAtDesc(user);

        return activities.stream()
                .map(activity -> new NotificationDto(
                        activity.getId(),
                        activity.getTicket().getTicketUid(),
                        activity.getTicket().getTitle(),
                        activity.getDescription(),
                        activity.getActivityType().name(),
                        activity.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}