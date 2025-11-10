package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.ActivityLogDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.ActivityType;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import java.util.List;

public interface ActivityLogServiceInterface {

    /**
     * Core method to create a new log entry. This will be called by other services.
     */
    void createLog(Ticket ticket, AppUser user, ActivityType type, String description, boolean isInternal);

    /**
     * Retrieves logs for a ticket based on the requesting user's role and permissions.
     */
    List<ActivityLogDto> getLogsForTicket(Long ticketId, AppUser requestingUser);
}
