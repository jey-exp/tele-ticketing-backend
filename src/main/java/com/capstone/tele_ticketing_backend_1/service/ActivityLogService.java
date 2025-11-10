package com.capstone.tele_ticketing_backend_1.service;


import com.capstone.tele_ticketing_backend_1.dto.ActivityLogDto;
import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.AuthorizationException;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService implements ActivityLogServiceInterface {

    private final TicketActivityRepo activityRepo;
    private final TicketRepo ticketRepo;

    /**
     * Core method to create a new log entry. This will be called by other services.
     */
    @Transactional
    public void createLog(Ticket ticket, AppUser user, ActivityType type, String description, boolean isInternal) {
        log.debug("Creating activity log for ticket {} by user {}: {} (internal: {})", 
                ticket.getTicketUid(), user.getUsername(), description, isInternal);
        TicketActivity log = new TicketActivity();
        log.setTicket(ticket);
        log.setUser(user);
        log.setActivityType(type);
        log.setDescription(description);
        log.setInternalOnly(isInternal);
        activityRepo.save(log);
    }

    /**
     * Retrieves logs for a ticket based on the requesting user's role and permissions.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogDto> getLogsForTicket(Long ticketId, AppUser requestingUser) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // Determine if the user has internal viewing rights.
        boolean canViewInternalLogs = canUserViewInternalLogs(requestingUser, ticket);

        List<TicketActivity> activities;
        if (canViewInternalLogs) {
            activities = activityRepo.findByTicketIdOrderByCreatedAtDesc(ticketId);
        } else {
            // Customers/Agents can only view their own tickets.
            if (!ticket.getCreatedFor().getId().equals(requestingUser.getId()) &&
                    !ticket.getCreatedBy().getId().equals(requestingUser.getId())) {
                throw new AuthorizationException("You are not authorized to view this ticket's logs.");
            }
            activities = activityRepo.findByTicketIdAndInternalOnlyFalseOrderByCreatedAtDesc(ticketId);
        }

        return activities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private boolean canUserViewInternalLogs(AppUser user, Ticket ticket) {
        // Simple logic: If the user is not a customer, they can see internal logs.
        // A more complex system might check if the engineer is assigned to the ticket.
        return user.getRoles().stream()
                .noneMatch(role -> role.getName().equals(ERole.ROLE_CUSTOMER));
    }

    private ActivityLogDto mapToDto(TicketActivity activity) {
        ActivityLogDto dto = new ActivityLogDto();
        dto.setId(activity.getId());
        dto.setDescription(activity.getDescription());
        dto.setActivityType(activity.getActivityType().name());
        dto.setInternalOnly(activity.isInternalOnly());
        dto.setCreatedAt(activity.getCreatedAt());

        AppUser user = activity.getUser();
        dto.setUser(new UserSummaryDto(user.getId(), user.getUsername(), user.getFullName()));

        return dto;
    }
}