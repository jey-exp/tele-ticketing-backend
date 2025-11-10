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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService implements DashboardServiceInterface {

    private final TicketRepo ticketRepo;
    private final TicketActivityRepo activityRepo;
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(String username) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        List<TicketStatus> activeStatuses = List.of(
                TicketStatus.CREATED, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
                TicketStatus.NEEDS_TRIAGING, TicketStatus.REOPENED, TicketStatus.FIXED
        );

        long activeCount = ticketRepo.countByCreatedForAndStatusIn(user, activeStatuses);
        long resolvedCount = ticketRepo.countByCreatedForAndStatus(user, TicketStatus.RESOLVED);
        long feedbackCount = ticketRepo.countByCreatedForAndStatus(user, TicketStatus.FIXED);

        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setActiveTickets(activeCount);
        stats.setResolvedTickets(resolvedCount);
        stats.setFeedbackRequiredTickets(feedbackCount);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<DashboardActivityDto> getRecentActivities(String username) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        return activityRepo.findFirst8ByTicket_CreatedForAndInternalOnlyFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(activity -> new DashboardActivityDto(
                        activity.getId(),
                        activity.getDescription(),
                        activity.getTicket().getTicketUid(),
                        activity.getTicket().getTitle(),
                        activity.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
