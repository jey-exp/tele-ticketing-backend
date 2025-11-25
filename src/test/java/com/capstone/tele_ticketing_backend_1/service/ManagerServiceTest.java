package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.TicketFilterDto;
import com.capstone.tele_ticketing_backend_1.dto.TicketSummaryDto;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private TicketRepo ticketRepo;

    @InjectMocks
    private ManagerService managerService;

    private Ticket mockTicket;

    @BeforeEach
    void setUp() {
        mockTicket = new Ticket();
        mockTicket.setId(1L);
        mockTicket.setTicketUid("TKT-101");
        mockTicket.setTitle("Network Latency");
        mockTicket.setStatus(TicketStatus.CREATED);
        mockTicket.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testFindTicketsByCriteria_NoFilters_ReturnsAll() {
        // Arrange
        TicketFilterDto emptyFilters = new TicketFilterDto();

        // We use any(Specification.class) because we are testing the Service flow,
        // not the JPA query generation logic itself.
        when(ticketRepo.findAll(any(Specification.class))).thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = managerService.findTicketsByCriteria(emptyFilters);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TKT-101", result.get(0).getTicketUid());

        verify(ticketRepo).findAll(any(Specification.class));
    }

    @Test
    void testFindTicketsByCriteria_WithAllFilters_Success() {
        // Arrange
        TicketFilterDto filters = new TicketFilterDto();
        filters.setStatuses(List.of(TicketStatus.CREATED, TicketStatus.IN_PROGRESS));
        filters.setTeamId(5L);
        filters.setCity("New York");
        filters.setSlaAtRisk(true);
        filters.setSlaBreached(false);

        when(ticketRepo.findAll(any(Specification.class))).thenReturn(List.of(mockTicket));

        // Act
        List<TicketSummaryDto> result = managerService.findTicketsByCriteria(filters);

        // Assert
        assertEquals(1, result.size());

        // Verify that findAll was called (implying specifications were constructed without error)
        verify(ticketRepo).findAll(any(Specification.class));
    }

    @Test
    void testFindTicketsByCriteria_NoResults() {
        // Arrange
        TicketFilterDto filters = new TicketFilterDto();
        filters.setCity("NonExistentCity");

        when(ticketRepo.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        // Act
        List<TicketSummaryDto> result = managerService.findTicketsByCriteria(filters);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}