package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.ReassignTicketDto;
import com.capstone.tele_ticketing_backend_1.dto.TeamDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TeamMemberUpdateRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketDetailDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import java.util.List;

public interface TeamLeadServiceInterface {

    /**
     * Gets a summary of all active (ASSIGNED, IN_PROGRESS) tickets assigned to the team lead's team.
     * @param teamLeadUsername The username of the team lead.
     * @return A list of active ticket summaries.
     */
    List<TicketSummaryDto> getActiveTeamTickets(String teamLeadUsername);

    /**
     * Gets a summary of tickets assigned to the team lead's team that are at SLA risk (breaching within 2 hours).
     * @param teamLeadUsername The username of the team lead.
     * @return A list of ticket summaries at SLA risk.
     */
    List<TicketSummaryDto> getSlaRiskTeamTickets(String teamLeadUsername);

    /**
     * Reassigns a ticket from one set of engineers to another within the team lead's team.
     * @param ticketId The ID of the ticket to reassign.
     * @param dto The DTO containing the list of new assignee user IDs.
     * @param teamLeadUsername The username of the team lead performing the action.
     * @return A DTO of the updated ticket.
     */
    TicketDetailDto reassignTicket(Long ticketId, ReassignTicketDto dto, String teamLeadUsername);

    /**
     * Gets a list of all members currently in the team lead's team.
     * @param teamLeadUsername The username of the team lead.
     * @return A list of user summaries for the team members.
     */
    List<UserSummaryDto> getTeamMembers(String teamLeadUsername);

    /**
     * Gets the details of the team lead's team, including its name and member list.
     * @param teamLeadUsername The username of the team lead.
     * @return A DTO containing the team's details.
     */
    TeamDetailDto getTeamDetails(String teamLeadUsername);

    /**
     * Creates a team for the lead if it doesn't exist, and updates the team's membership.
     * @param dto The DTO containing user IDs to add or remove.
     * @param teamLeadUsername The username of the team lead.
     * @return A DTO of the updated team, including the new member list.
     */
    TeamDetailDto updateTeam(TeamMemberUpdateRequestDto dto, String teamLeadUsername);
}
