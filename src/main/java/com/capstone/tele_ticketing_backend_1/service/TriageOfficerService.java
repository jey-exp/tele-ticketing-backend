package com.capstone.tele_ticketing_backend_1.service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.capstone.tele_ticketing_backend_1.dto.*;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.ai.TriageAssistant;
import com.capstone.tele_ticketing_backend_1.exceptions.InvalidTicketStatusException;
import com.capstone.tele_ticketing_backend_1.exceptions.TicketNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriageOfficerService implements TriageOfficerServiceInterface {

    private final TicketRepo ticketRepo;
    private final UserRepo userRepo;
    private final TicketService ticketService;
    private final ActivityLogService activityLogService;
    private final TriageAssistant triageAssistant;
    private final TicketActivityRepo activityRepo;

    // Defines which statuses are considered "pending" for a Triage Officer.
    private static final List<TicketStatus> PENDING_STATUSES = List.of(
            TicketStatus.CREATED,
            TicketStatus.NEEDS_TRIAGING,
            TicketStatus.REOPENED
    );

    public List<TicketSummaryDto> getPendingTickets() {
        List<Ticket> tickets = ticketRepo.findAllByStatusIn(PENDING_STATUSES);
        return tickets.stream()
                .map(ticket -> new TicketSummaryDto(ticket.getId(), ticket.getTicketUid(), ticket.getTitle(), ticket.getStatus(), ticket.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketDetailDto triageTicket(Long ticketId, TriageTicketRequestDto dto, String triageOfficerUsername) {
        AppUser triageOfficer = userRepo.findByUsername(triageOfficerUsername)
                .orElseThrow(() -> new UserNotFoundException("Triage officer not found: " + triageOfficerUsername));

        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // State Validation: Ensure the ticket is in a state that can be triaged.
        if (!PENDING_STATUSES.contains(ticket.getStatus())) {
            throw new InvalidTicketStatusException("Ticket is not in a state that can be triaged. Current status: " + ticket.getStatus());
        }

        List<AppUser> engineersToAssign = userRepo.findAllById(dto.getAssignedToUserIds());
        if (engineersToAssign.size() != dto.getAssignedToUserIds().size()) {
            throw new UserNotFoundException("One or more specified engineers could not be found.");
        }

        // Log changes BEFORE applying them for a clear audit trail.
        if (dto.getPriority() != ticket.getPriority()) {
            activityLogService.createLog(ticket, triageOfficer, ActivityType.PRIORITY_CHANGE, "Priority changed from " + ticket.getPriority() + " to " + dto.getPriority(), false); // Public log
        }

        // Apply Updates to the Ticket
        ticket.setSeverity(dto.getSeverity());
        ticket.setPriority(dto.getPriority());
        ticket.setAssignedTo(new HashSet<>(engineersToAssign));
        ticket.setAssignedBy(triageOfficer);
        ticket.setStatus(TicketStatus.ASSIGNED);

        // Calculate and set SLA based on the new severity.
        int slaHours = calculateSlaHours(dto.getSeverity());
        ticket.setSlaDurationHours(slaHours);
        ticket.setSlaBreachAt(LocalDateTime.now().plusHours(slaHours));

        // Create an internal log for the assignment action.
        String assignedEngineersNames = engineersToAssign.stream().map(AppUser::getFullName).collect(Collectors.joining(", "));
        activityLogService.createLog(ticket, triageOfficer, ActivityType.ASSIGNMENT, "Assigned to " + assignedEngineersNames, true); // Internal-only log

        Ticket savedTicket = ticketRepo.save(ticket);

        // Return a clean DTO by calling the central mapper.
        return ticketService.mapTicketToDetailDto(savedTicket);
    }

    private int calculateSlaHours(TicketSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 20;
            case HIGH -> 18;
            case MEDIUM -> 15;
            case LOW -> 10;
            case TRIVIAL -> 2;
            default -> 24; // Default fallback
        };
    }

    @Transactional(readOnly = true)
    public List<AiTriageSuggestionDto> getAiTriageSuggestions() {
        List<Ticket> pendingTickets = ticketRepo.findAllByStatusIn(PENDING_STATUSES);
        List<AiTriageSuggestionDto> suggestions = new ArrayList<>();

        for (Ticket ticket : pendingTickets) {
            String prompt = String.format(
                    "Analyze this ticket:\nTitle: %s\nDescription: %s\nCategory: %s",
                    ticket.getTitle(), ticket.getDescription(), ticket.getCategory()
            );

            try {
                // Call the AI
                TriageSuggestion suggestion = triageAssistant.suggestTriage(prompt);

                // Map the AI's string response to our DTO
                AiTriageSuggestionDto dto = new AiTriageSuggestionDto();
                dto.setTicketId(ticket.getId());
                dto.setTicketUid(ticket.getTicketUid());
                dto.setTitle(ticket.getTitle());

                // Safely map strings to enums, handling potential AI errors
                dto.setSuggestedPriority(safeValueOf(TicketPriority.class, suggestion.getSuggestedPriority(), TicketPriority.LOW));
                dto.setSuggestedSeverity(safeValueOf(TicketSeverity.class, suggestion.getSuggestedSeverity(), TicketSeverity.TRIVIAL));
                dto.setSuggestedRole(safeValueOf(ERole.class, suggestion.getSuggestedRole(), ERole.ROLE_L1_ENGINEER));

                suggestions.add(dto);

            } catch (Exception e) {
                // We can add a "failed" suggestion if we want
                suggestions.add(createFailedSuggestionDto(ticket, e.getMessage()));
            }
        }
        return suggestions;
    }

    // A private helper to safely convert AI string output to enums
    private <T extends Enum<T>> T safeValueOf(Class<T> enumType, String name, T defaultValue) {
        if (name == null) return defaultValue;
        try {
            return T.valueOf(enumType, name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    // A private helper to create a DTO that signals failure
    private AiTriageSuggestionDto createFailedSuggestionDto(Ticket ticket, String error) {
        AiTriageSuggestionDto dto = new AiTriageSuggestionDto();
        dto.setTicketId(ticket.getId());
        dto.setTicketUid(ticket.getTicketUid());
        dto.setTitle(ticket.getTitle() + " (AI FAILED: " + error + ")");
        // We set defaults so the UI doesn't crash
        dto.setSuggestedPriority(TicketPriority.LOW);
        dto.setSuggestedSeverity(TicketSeverity.TRIVIAL);
        dto.setSuggestedRole(ERole.ROLE_L1_ENGINEER);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications() {
        // Define the list of types you want to see
        List<ActivityType> relevantTypes = List.of(
                ActivityType.STATUS_CHANGE,
                ActivityType.PRIORITY_CHANGE,
                ActivityType.ASSIGNMENT,
                ActivityType.REOPENED
        );

        // Call the new repository method
        List<TicketActivity> activities = activityRepo
                .findAllByActivityTypeInOrderByCreatedAtDesc(relevantTypes);

        return activities.stream()
                .map(this::mapActivityToNotificationDto)
                .collect(Collectors.toList());
    }

    private NotificationDto mapActivityToNotificationDto(TicketActivity activity) {
        return new NotificationDto(
                activity.getId(),
                activity.getTicket().getTicketUid(),
                activity.getTicket().getTitle(),
                activity.getDescription(),
                activity.getActivityType().name(),
                activity.getCreatedAt()
        );
    }
}