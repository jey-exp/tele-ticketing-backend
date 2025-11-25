package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.UserSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.ERole;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserService userService;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setUsername("test_user");
        mockUser.setFullName("Test User");
    }

    // --- getAllCustomers Tests ---

    @Test
    void testGetAllCustomers_Success() {
        // Arrange
        when(userRepo.findAllByRoles_Name(ERole.ROLE_CUSTOMER))
                .thenReturn(List.of(mockUser));

        // Act
        List<UserSummaryDto> result = userService.getAllCustomers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test_user", result.get(0).getUsername());

        verify(userRepo).findAllByRoles_Name(ERole.ROLE_CUSTOMER);
    }

    @Test
    void testGetAllCustomers_Empty() {
        when(userRepo.findAllByRoles_Name(ERole.ROLE_CUSTOMER))
                .thenReturn(Collections.emptyList());

        List<UserSummaryDto> result = userService.getAllCustomers();

        assertTrue(result.isEmpty());
    }

    // --- getAssignableEngineers Tests ---

    @Test
    void testGetAssignableEngineers_Success() {
        // Arrange
        when(userRepo.findAllByRoles_NameIn(anyList()))
                .thenReturn(List.of(mockUser));

        // Act
        List<UserSummaryDto> result = userService.getAssignableEngineers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        // Verify that the Service passed the CORRECT list of roles to the Repo
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ERole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepo).findAllByRoles_NameIn(captor.capture());

        List<ERole> capturedRoles = captor.getValue();
        assertTrue(capturedRoles.contains(ERole.ROLE_FIELD_ENGINEER));
        assertTrue(capturedRoles.contains(ERole.ROLE_L1_ENGINEER));
        assertTrue(capturedRoles.contains(ERole.ROLE_NOC_ENGINEER));
    }

    // --- getUnassignedEngineers Tests ---

    @Test
    void testGetUnassignedEngineers_Success() {
        // Arrange
        when(userRepo.findByTeamIsNullAndRoles_NameIn(anyList()))
                .thenReturn(List.of(mockUser));

        // Act
        List<UserSummaryDto> result = userService.getUnassignedEngineers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        // Verify the correct roles were requested
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ERole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepo).findByTeamIsNullAndRoles_NameIn(captor.capture());

        List<ERole> capturedRoles = captor.getValue();
        assertEquals(3, capturedRoles.size()); // Expecting Field, L1, and NOC
    }

    // --- getCustomerCities Tests ---

    @Test
    void testGetCustomerCities_Success() {
        // Arrange
        List<String> cities = List.of("Chennai", "Bangalore");
        when(userRepo.findDistinctCities()).thenReturn(cities);

        // Act
        List<String> result = userService.getCustomerCities();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Chennai", result.get(0));
        verify(userRepo).findDistinctCities();
    }
}