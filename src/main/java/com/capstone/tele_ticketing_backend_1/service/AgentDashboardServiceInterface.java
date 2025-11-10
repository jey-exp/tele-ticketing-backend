package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.DashboardActivityDto;
import com.capstone.tele_ticketing_backend_1.dto.DashboardStatsDto;
import java.util.List;

public interface AgentDashboardServiceInterface {

    /**
     * Fetches a list of recent public activities on tickets created by the agent.
     * @param agentUsername The username of the agent.
     * @return A list of activity DTOs.
     */
    List<DashboardActivityDto> getRecentActivities(String agentUsername);
}