package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import java.util.List;

public interface UserServiceInterface {

    /**
     * Retrieves a list of all users with the CUSTOMER role.
     * @return A list of UserSummaryDto for all customers.
     */
    List<UserSummaryDto> getAllCustomers();

    /**
     * Retrieves a list of all users with an assignable engineer role (Field, L1, NOC).
     * @return A list of UserSummaryDto for all assignable engineers.
     */
    List<UserSummaryDto> getAssignableEngineers();

    /**
     * Retrieves a list of all engineers who are not currently assigned to any team.
     * @return A list of UserSummaryDto for all unassigned engineers.
     */
    List<UserSummaryDto> getUnassignedEngineers();

    /**
     * Retrieves a distinct list of city names for all customers.
     * @return A list of city name strings.
     */
    List<String> getCustomerCities();
}