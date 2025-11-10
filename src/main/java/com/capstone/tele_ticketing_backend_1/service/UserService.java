package com.capstone.tele_ticketing_backend_1.service;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.ERole;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserServiceInterface {

    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllCustomers() {
        log.info("Fetching all customers");
        List<AppUser> customers = userRepo.findAllByRoles_Name(ERole.ROLE_CUSTOMER);
        log.debug("Found {} customers", customers.size());
        return mapUsersToSummaryDto(customers);
    }

    // Dr. X's Addition: Move the engineer-fetching logic here.
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAssignableEngineers() {
        log.info("Fetching assignable engineers");
        List<ERole> engineerRoles = List.of(
                ERole.ROLE_FIELD_ENGINEER,
                ERole.ROLE_L1_ENGINEER,
                ERole.ROLE_NOC_ENGINEER
        );
        List<AppUser> engineers = userRepo.findAllByRoles_NameIn(engineerRoles);
        log.debug("Found {} assignable engineers", engineers.size());
        return mapUsersToSummaryDto(engineers);
    }

    // Helper method to avoid code duplication
    private List<UserSummaryDto> mapUsersToSummaryDto(List<AppUser> users) {
        return users.stream()
                .map(user -> new UserSummaryDto(user.getId(), user.getUsername(), user.getFullName()))
                .collect(Collectors.toList());
    }

    public List<UserSummaryDto> getUnassignedEngineers() {
        log.info("Fetching unassigned engineers");
        List<ERole> engineerRoles = List.of(ERole.ROLE_FIELD_ENGINEER, ERole.ROLE_L1_ENGINEER, ERole.ROLE_NOC_ENGINEER);
        List<AppUser> engineers = userRepo.findByTeamIsNullAndRoles_NameIn(engineerRoles);
        log.debug("Found {} unassigned engineers", engineers.size());
        return mapUsersToSummaryDto(engineers);
    }

    @Transactional(readOnly = true)
    public List<String> getCustomerCities() {
        log.info("Fetching distinct customer cities");
        List<String> cities = userRepo.findDistinctCities();
        log.debug("Found {} distinct cities", cities.size());
        return cities;
    }

}