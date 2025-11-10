package com.capstone.tele_ticketing_backend_1.service;


import com.capstone.tele_ticketing_backend_1.dto.ApproveSignupRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.RoleChangeRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.UserDetailsDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.UserSignupRequest;
import java.util.List;

public interface AdminServiceInterface {

    /**
     * Retrieves all pending user signup requests.
     * @return A list of UserSignupRequest entities.
     */
    List<UserSignupRequest> getPendingSignupRequests();

    /**
     * Approves a signup request, creates a new AppUser, and deletes the request.
     * @param requestId The ID of the signup request to approve.
     * @param dto The DTO containing the final, admin-approved role.
     * @return The newly created AppUser entity.
     */
    AppUser approveSignupRequest(Long requestId, ApproveSignupRequestDto dto);

    /**
     * Rejects and deletes a pending signup request.
     * @param requestId The ID of the signup request to reject.
     */
    void rejectSignupRequest(Long requestId);

    /**
     * Changes the role of an existing internal user.
     * @param userId The ID of the user to modify.
     * @param dto The DTO containing the new role.
     * @return The updated AppUser entity.
     */
    AppUser changeUserRole(Long userId, RoleChangeRequestDto dto);

    /**
     * Retrieves a list of all internal users (non-customers).
     * @return A list of UserDetailsDto.
     */
    List<UserDetailsDto> getAllInternalUsers();
}
