package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.DashboardActivityDto;
import com.capstone.tele_ticketing_backend_1.dto.DashboardStatsDto;
import java.util.List;

public interface DashboardServiceInterface {

    /**
     * Fetches aggregated dashboard statistics for a specific user.
     * @param username The username of the user.
     * @return A DTO containing dashboard stats (active, resolved, feedback).
     */
    DashboardStatsDto getDashboardStats(String username);

    /**
     * Fetches a list of recent public activities for tickets created for the user.
     * @param username The username of the user.
     * @return A list of the 8 most recent activity DTOs.
     */
    List<DashboardActivityDto> getRecentActivities(String username);
}