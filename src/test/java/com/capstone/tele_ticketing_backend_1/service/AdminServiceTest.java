package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.ApproveSignupRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.RoleChangeRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.UserDetailsDto;
import com.capstone.tele_ticketing_backend_1.entities.*;
import com.capstone.tele_ticketing_backend_1.exceptions.BadRequestException;
import com.capstone.tele_ticketing_backend_1.exceptions.ResourceNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.RoleNotFoundException;
import com.capstone.tele_ticketing_backend_1.exceptions.UserNotFoundException;
import com.capstone.tele_ticketing_backend_1.repo.RoleRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserSignupRequestRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserSignupRequestRepo signupRequestRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private RoleRepo roleRepo;

    @InjectMocks
    private AdminService adminService;

    // Helper objects
    private Role fieldEngineerRole;
    private Role customerRole;
    private Role managerRole;
    private AppUser mockUser;
    private UserSignupRequest mockSignupRequest;

    @BeforeEach
    void setUp() {
        // Setup Roles
        fieldEngineerRole = new Role();
        fieldEngineerRole.setId(1);
        fieldEngineerRole.setName(ERole.ROLE_FIELD_ENGINEER);

        customerRole = new Role();
        customerRole.setId(2);
        customerRole.setName(ERole.ROLE_CUSTOMER);

        managerRole = new Role();
        managerRole.setId(3);
        managerRole.setName(ERole.ROLE_MANAGER);

        // Setup User
        mockUser = new AppUser();
        mockUser.setId(10L);
        mockUser.setUsername("existingUser");
        mockUser.setFullName("Existing User");
        // Use a mutable set for roles
        Set<Role> roles = new HashSet<>();
        roles.add(fieldEngineerRole);
        mockUser.setRoles(roles);

        // Setup Signup Request
        mockSignupRequest = new UserSignupRequest();
        mockSignupRequest.setId(100L);
        mockSignupRequest.setUsername("newUser");
        mockSignupRequest.setPassword("password123");
        mockSignupRequest.setFullName("New User");
    }

    // --- getPendingSignupRequests ---

    @Test
    void testGetPendingSignupRequests() {
        // Arrange
        when(signupRequestRepo.findAll()).thenReturn(List.of(mockSignupRequest));

        // Act
        List<UserSignupRequest> result = adminService.getPendingSignupRequests();

        // Assert
        assertEquals(1, result.size());
        assertEquals("newUser", result.get(0).getUsername());
        verify(signupRequestRepo, times(1)).findAll();
    }

    // --- approveSignupRequest ---

    @Test
    void testApproveSignupRequest_Success() {
        // Arrange
        Long requestId = 100L;
        ApproveSignupRequestDto dto = new ApproveSignupRequestDto();
        // The service adds "ROLE_" and capitalizes, so we pass "field_engineer"
        dto.setFinalRole("field_engineer");

        when(signupRequestRepo.findById(requestId)).thenReturn(Optional.of(mockSignupRequest));
        when(roleRepo.findByName(ERole.ROLE_FIELD_ENGINEER)).thenReturn(Optional.of(fieldEngineerRole));
        when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setId(50L); // Simulate DB ID generation
            return saved;
        });

        // Act
        AppUser result = adminService.approveSignupRequest(requestId, dto);

        // Assert
        assertNotNull(result);
        assertEquals("newUser", result.getUsername());
        assertTrue(result.getRoles().contains(fieldEngineerRole));

        // Verify delete was called
        verify(signupRequestRepo).delete(mockSignupRequest);
    }

    @Test
    void testApproveSignupRequest_RequestNotFound() {
        // Arrange
        when(signupRequestRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                adminService.approveSignupRequest(999L, new ApproveSignupRequestDto())
        );
    }

    @Test
    void testApproveSignupRequest_RoleNotFound() {
        // Arrange
        Long requestId = 100L;
        ApproveSignupRequestDto dto = new ApproveSignupRequestDto();
        dto.setFinalRole("INVALID_ROLE"); // This will likely throw IllegalArgumentException in Enum.valueOf or RoleNotFoundException

        when(signupRequestRepo.findById(requestId)).thenReturn(Optional.of(mockSignupRequest));

        // If the Enum.valueOf fails, Java throws IllegalArgumentException.
        // If the Repo return empty, service throws RoleNotFoundException.
        // Assuming "INVALID_ROLE" allows Enum conversion or we mock strict enum checking:

        // Let's test a valid Enum string but not found in DB
        dto.setFinalRole("field_engineer");
        when(roleRepo.findByName(ERole.ROLE_FIELD_ENGINEER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RoleNotFoundException.class, () ->
                adminService.approveSignupRequest(requestId, dto)
        );
    }

    // --- rejectSignupRequest ---

    @Test
    void testRejectSignupRequest_Success() {
        // Arrange
        when(signupRequestRepo.existsById(100L)).thenReturn(true);

        // Act
        adminService.rejectSignupRequest(100L);

        // Assert
        verify(signupRequestRepo).deleteById(100L);
    }

    @Test
    void testRejectSignupRequest_NotFound() {
        // Arrange
        when(signupRequestRepo.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                adminService.rejectSignupRequest(999L)
        );
        verify(signupRequestRepo, never()).deleteById(anyLong());
    }

    // --- changeUserRole ---

    @Test
    void testChangeUserRole_Success() {
        // Arrange
        Long userId = 10L;
        RoleChangeRequestDto dto = new RoleChangeRequestDto();
        dto.setNewRole("manager");

        when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
        when(roleRepo.findByName(ERole.ROLE_MANAGER)).thenReturn(Optional.of(managerRole));
        when(userRepo.save(any(AppUser.class))).thenReturn(mockUser);

        // Act
        AppUser result = adminService.changeUserRole(userId, dto);

        // Assert
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains(managerRole));
        verify(userRepo).save(mockUser);
    }

    @Test
    void testChangeUserRole_UserNotFound() {
        // Arrange
        when(userRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                adminService.changeUserRole(999L, new RoleChangeRequestDto())
        );
    }

    @Test
    void testChangeUserRole_CannotChangeCustomer() {
        // Arrange
        AppUser customerUser = new AppUser();
        customerUser.setId(20L);
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        customerUser.setRoles(roles);

        when(userRepo.findById(20L)).thenReturn(Optional.of(customerUser));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                adminService.changeUserRole(20L, new RoleChangeRequestDto())
        );
        assertEquals("Cannot change the role of a customer account.", ex.getMessage());
    }

    // --- getAllInternalUsers ---

    @Test
    void testGetAllInternalUsers() {
        // Arrange
        when(userRepo.findAllByRoles_NameNot(ERole.ROLE_CUSTOMER)).thenReturn(List.of(mockUser));

        // Act
        List<UserDetailsDto> result = adminService.getAllInternalUsers();

        // Assert
        assertEquals(1, result.size());
        assertEquals("existingUser", result.get(0).getUsername());
        // Verify the mapping logic extracted the role name correctly
        assertEquals(ERole.ROLE_FIELD_ENGINEER.name(), result.get(0).getRole());
    }
}