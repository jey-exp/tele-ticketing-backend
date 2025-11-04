package com.capstone.tele_ticketing_backend_1.ai;

import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import com.capstone.tele_ticketing_backend_1.security.service.UserDetailsImpl;
import com.capstone.tele_ticketing_backend_1.service.CustomerTicketService;
import com.capstone.tele_ticketing_backend_1.service.TicketService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingTools {

    private final CustomerTicketService customerTicketService;
    private final TicketService ticketService;
    private final UserRepo userRepo;

    /**
     * Gets the currently authenticated AppUser from the security context.
     * This is crucial for fetching data specific to the user.
     */
    private AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            log.error("AI Tool: Could not retrieve authenticated user.");
            throw new IllegalStateException("User not authenticated.");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found from security context"));
    }

    @Tool("Fetches a summary of the current user's active tickets (status is CREATED, ASSIGNED, IN_PROGRESS, etc.)")
    public String getMyActiveTickets() {
        log.info("AI Tool: Executing getMyActiveTickets");
        try {
            AppUser currentUser = getCurrentUser();
            List<TicketSummaryDto> tickets = customerTicketService.getCustomerActiveTickets(currentUser.getUsername());

            if (tickets == null || tickets.isEmpty()) {
                return "You currently have no active tickets.";
            }

            return "Here is a summary of your active tickets:\n" +
                    tickets.stream()
                            .map(ticket -> String.format("- Ticket %s: '%s' [Status: %s]",
                                    ticket.getTicketUid(),
                                    ticket.getTitle(),
                                    ticket.getStatus().name()))
                            .collect(Collectors.joining("\n"));

        } catch (IllegalStateException e) {
            return "I cannot fetch your tickets as you don't seem to be logged in.";
        } catch (Exception e) {
            log.error("AI Tool: Error fetching active tickets", e);
            return "Sorry, I encountered an error trying to fetch your active tickets.";
        }
    }

    @Tool("Fetches the detailed status and information for a single ticket, given its unique Ticket ID (e.g., 'TK-123456')")
    public String getTicketDetails(String ticketUid) {
        log.info("AI Tool: Executing getTicketDetails for {}", ticketUid);
        try {
            AppUser currentUser = getCurrentUser(); // Used for auth context
            TicketDetailDto ticket = ticketService.getTicketByUid(ticketUid);

            // Security Check: Ensure the user is allowed to see this ticket.
            if (!ticket.getCreatedFor().getId().equals(currentUser.getId())) {
                return "Sorry, you are not authorized to view details for ticket " + ticketUid;
            }

            return String.format(
                    "Details for ticket %s:\n" +
                            "- Title: %s\n" +
                            "- Status: %s\n" +
                            "- Priority: %s\n" +
                            "- Severity: %s\n" +
                            "- Created: %s\n" +
                            "- Assigned To: %s",
                    ticket.getTicketUid(),
                    ticket.getTitle(),
                    ticket.getStatus(),
                    ticket.getPriority() != null ? ticket.getPriority() : "Not set",
                    ticket.getSeverity() != null ? ticket.getSeverity() : "Not set",
                    ticket.getCreatedAt(),
                    ticket.getAssignedTo().isEmpty() ? "Unassigned" : ticket.getAssignedTo().stream().map(UserSummaryDto::getFullName).collect(Collectors.joining(", "))
            );

        } catch (TicketNotFoundException e) {
            return "Sorry, I could not find any ticket with the ID " + ticketUid;
        } catch (IllegalStateException e) {
            return "I cannot fetch ticket details as you don't seem to be logged in.";
        } catch (Exception e) {
            log.error("AI Tool: Error fetching ticket details", e);
            return "Sorry, I encountered an error trying to fetch details for that ticket.";
        }
    }
}
