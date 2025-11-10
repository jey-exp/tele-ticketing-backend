package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.TicketFilterDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import java.util.List;

public interface ManagerServiceInterface {

    /**
     * Finds a list of tickets based on a dynamic set of filters.
     * @param filters A DTO containing optional filters for status, team, location, and SLA risk.
     * @return A list of ticket summaries matching the criteria.
     */
    List<TicketSummaryDto> findTicketsByCriteria(TicketFilterDto filters);
}
