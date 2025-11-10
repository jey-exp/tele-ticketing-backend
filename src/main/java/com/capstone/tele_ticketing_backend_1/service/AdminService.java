package com.capstone.tele_ticketing_backend_1.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.ApproveSignupRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.RoleChangeRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.UserDetailsDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.ERole;
import com.capstone.tele_ticketing_backend_1.entities.Role;
import com.capstone.tele_ticketing_backend_1.entities.UserSignupRequest;
import com.capstone.tele_ticketing_backend_1.exceptions.BadRequestException;
import com.capstone.tele_ticketing_backend_1.exceptions.ResourceNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.RoleNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.RoleRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserSignupRequestRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService implements AdminServiceInterface {

    private final UserSignupRequestRepo signupRequestRepo;
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;

    @Transactional(readOnly = true)
    public List<UserSignupRequest> getPendingSignupRequests() {
        return signupRequestRepo.findAll();
    }

    @Transactional
    public AppUser approveSignupRequest(Long requestId, ApproveSignupRequestDto dto) {
        UserSignupRequest request = signupRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Signup request not found."));

        AppUser newUser = new AppUser();
        newUser.setUsername(request.getUsername());
        newUser.setFullName(request.getFullName());
        newUser.setPassword(request.getPassword());

        Role userRole = findRole(dto.getFinalRole());

        // Dr. X's Fix: Use a mutable Set here as well.
        Set<Role> newRoles = new HashSet<>();
        newRoles.add(userRole);
        newUser.setRoles(newRoles);

        AppUser savedUser = userRepo.save(newUser);
        signupRequestRepo.delete(request);
        return savedUser;
    }

    @Transactional
    public void rejectSignupRequest(Long requestId) {
        if (!signupRequestRepo.existsById(requestId)) {
            throw new ResourceNotFoundException("Signup request not found.");
        }
        signupRequestRepo.deleteById(requestId);
    }

    @Transactional
    public AppUser changeUserRole(Long userId, RoleChangeRequestDto dto) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (user.getRoles().stream().anyMatch(role -> role.getName().equals(ERole.ROLE_CUSTOMER))) {
            throw new BadRequestException("Cannot change the role of a customer account.");
        }

        Role newRole = findRole(dto.getNewRole());

        // Dr. X's Fix: Create a new MUTABLE HashSet for Hibernate to manage.
        Set<Role> newRoles = new HashSet<>();
        newRoles.add(newRole);
        user.setRoles(newRoles); // This is now safe

        return userRepo.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserDetailsDto> getAllInternalUsers() {
        List<AppUser> users = userRepo.findAllByRoles_NameNot(ERole.ROLE_CUSTOMER);
        return users.stream()
                .map(this::mapUserToDetailsDto)
                .collect(Collectors.toList());
    }

    private Role findRole(String roleName) {
        ERole eRole = ERole.valueOf("ROLE_" + roleName.toUpperCase());
        return roleRepo.findByName(eRole)
                .orElseThrow(() -> new RoleNotFoundException("Role " + roleName + " not found."));
    }

    private UserDetailsDto mapUserToDetailsDto(AppUser user) {
        UserDetailsDto dto = new UserDetailsDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());

        user.getRoles().stream()
                .filter(role -> role.getName() != ERole.ROLE_CUSTOMER)
                .findFirst()
                .ifPresent(role -> dto.setRole(role.getName().name()));

        return dto;
    }
}