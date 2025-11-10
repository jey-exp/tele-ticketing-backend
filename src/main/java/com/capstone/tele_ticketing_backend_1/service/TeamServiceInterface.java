package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.TeamSummaryDto;
import java.util.List;

public interface TeamServiceInterface {

    /**
     * Retrieves a summary of all teams in the system.
     * @return A list of TeamSummaryDto, including basic team info and the team lead.
     */
    List<TeamSummaryDto> getAllTeams();
}