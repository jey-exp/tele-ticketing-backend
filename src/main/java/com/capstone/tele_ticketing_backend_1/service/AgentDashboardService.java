package com.capstone.tele_ticketing_backend_1.service;


import com.capstone.tele_ticketing_backend_1.dto.DashboardActivityDto;
import com.capstone.tele_ticketing_backend_1.dto.DashboardStatsDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.TicketActivityRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentDashboardService implements AgentDashboardServiceInterface {

    private final TicketRepo ticketRepo;
    private final TicketActivityRepo activityRepo;
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(String agentUsername) {
        log.info("Fetching dashboard stats for agent: {}", agentUsername);
        try {
            AppUser agent = userRepo.findByUsername(agentUsername)
                    .orElseThrow(() -> new UserNotFoundException("Agent not found: " + agentUsername));

            List<TicketStatus> activeStatuses = List.of(
                    TicketStatus.CREATED, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
                    TicketStatus.NEEDS_TRIAGING, TicketStatus.REOPENED, TicketStatus.FIXED
            );

            long activeCount = ticketRepo.countByCreatedByAndStatusIn(agent, activeStatuses);
            long resolvedCount = ticketRepo.countByCreatedByAndStatus(agent, TicketStatus.RESOLVED);
            long feedbackCount = ticketRepo.countByCreatedByAndStatus(agent, TicketStatus.FIXED);

            DashboardStatsDto stats = new DashboardStatsDto();
            stats.setActiveTickets(activeCount);
            stats.setResolvedTickets(resolvedCount);
            stats.setFeedbackRequiredTickets(feedbackCount);

            log.info("Successfully fetched stats for agent {}: Active={}, Resolved={}, Feedback={}",
                    agentUsername, activeCount, resolvedCount, feedbackCount);

            return stats;

        } catch (UserNotFoundException e) {
            // Dr. X's Addition: Log a WARNING for expected errors like "not found".
            log.warn("Failed to get stats. {}. (Agent: {})", e.getMessage(), agentUsername);
            throw e; // Re-throw for the controller handler
        } catch (Exception e) {
            // Dr. X's Addition: Log an ERROR for unexpected database/system errors.
            log.error("An unexpected error occurred while fetching stats for agent: {}", agentUsername, e);
            throw e; // Re-throw the exception
        }
    }

    @Transactional(readOnly = true)
    public List<DashboardActivityDto> getRecentActivities(String agentUsername) {
        log.info("Fetching recent activities for agent: {}", agentUsername);
        try {
            AppUser agent = userRepo.findByUsername(agentUsername)
                    .orElseThrow(() -> new UserNotFoundException("Agent not found: " + agentUsername));

            List<DashboardActivityDto> activities = activityRepo.findFirst8ByTicket_CreatedByAndInternalOnlyFalseOrderByCreatedAtDesc(agent)
                    .stream()
                    .map(activity -> new DashboardActivityDto(
                            activity.getId(),
                            activity.getDescription(),
                            activity.getTicket().getTicketUid(),
                            activity.getTicket().getTitle(),
                            activity.getCreatedAt()
                    ))
                    .toList();

            log.info("Found {} recent activities for agent: {}", activities.size(), agentUsername);
            return activities;

        } catch (UserNotFoundException e) {
            log.warn("Failed to get recent activities. {}. (Agent: {})", e.getMessage(), agentUsername);
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching recent activities for agent: {}", agentUsername, e);
            throw e;
        }
    }
}