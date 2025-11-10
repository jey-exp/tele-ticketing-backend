package com.capstone.tele_ticketing_backend_1.service;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.TeamSummaryDto;
import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.Team;
import com.capstone.tele_ticketing_backend_1.repo.TeamRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService implements TeamServiceInterface {

    private final TeamRepo teamRepo;

    @Transactional(readOnly = true)
    // Dr. X's Fix: Change the return type to a list of DTOs.
    public List<TeamSummaryDto> getAllTeams() {
        log.info("Fetching all teams");
        List<TeamSummaryDto> teams = teamRepo.findAll().stream()
                .map(this::mapTeamToSummaryDto)
                .toList();
        log.debug("Found {} teams", teams.size())
        ;
        return teams;
    }

    // Dr. X's Fix: Create a helper method to perform the mapping.
    private TeamSummaryDto mapTeamToSummaryDto(Team team) {
        TeamSummaryDto dto = new TeamSummaryDto();
        dto.setId(team.getId());
        dto.setName(team.getName());

        // This is the crucial part. We access team.getTeamLead() here,
        // which forces Hibernate to load the lazy AppUser object while the transaction is still active.
        if (team.getTeamLead() != null) {
            dto.setTeamLead(new UserSummaryDto(
                    team.getTeamLead().getId(),
                    team.getTeamLead().getUsername(),
                    team.getTeamLead().getFullName()
            ));
        }
        return dto;
    }
}