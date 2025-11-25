package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketSpecificationTest {

    @Mock
    private Root<Ticket> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path path;

    @Mock
    private Join<Object, Object> join;

    @Mock
    private Predicate predicate;

    // --- hasStatusIn Tests ---

    @Test
    void testHasStatusIn_WithStatuses() {
        // Arrange
        List<TicketStatus> statuses = List.of(TicketStatus.CREATED, TicketStatus.IN_PROGRESS);
        Specification<Ticket> spec = TicketSpecification.hasStatusIn(statuses);

        // Mock the chain: root.get("status") -> path
        when(root.get("status")).thenReturn(path);
        // Mock path.in(statuses) -> predicate
        when(path.in(statuses)).thenReturn(predicate);

        // Act
        Predicate result = spec.toPredicate(root, query, cb);

        // Assert
        assertNotNull(result);
        verify(root).get("status");
        verify(path).in(statuses);
    }

    @Test
    void testHasStatusIn_NullList_ReturnsConjunction() {
        // Arrange
        Specification<Ticket> spec = TicketSpecification.hasStatusIn(null);
        when(cb.conjunction()).thenReturn(predicate);

        // Act
        Predicate result = spec.toPredicate(root, query, cb);

        // Assert
        assertEquals(predicate, result);
        verify(cb).conjunction(); // Should return a "do nothing" predicate
        verify(root, never()).get(anyString());
    }

    // --- inTeam Tests ---

    @Test
    void testInTeam_WithId() {
        // Arrange
        Long teamId = 5L;
        Specification<Ticket> spec = TicketSpecification.inTeam(teamId);

        // Mock the complex chain: root.join("assignedTo").get("team").get("id")
        Join<Object, Object> assignedToJoin = mock(Join.class);
        Path<Object> teamPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);

        when(root.join("assignedTo")).thenReturn(assignedToJoin);
        when(assignedToJoin.get("team")).thenReturn(teamPath);
        when(teamPath.get("id")).thenReturn(idPath);

        when(cb.equal(idPath, teamId)).thenReturn(predicate);

        // Act
        Predicate result = spec.toPredicate(root, query, cb);

        // Assert
        assertNotNull(result);
        verify(cb).equal(idPath, teamId);
    }

    @Test
    void testInTeam_NullId() {
        // Arrange
        Specification<Ticket> spec = TicketSpecification.inTeam(null);
        when(cb.conjunction()).thenReturn(predicate);

        // Act
        spec.toPredicate(root, query, cb);

        // Assert
        verify(cb).conjunction();
        verify(root, never()).join(anyString());
    }

    // --- inCity Tests ---

    @Test
    void testInCity_WithCity() {
        // Arrange
        String city = "Chennai";
        Specification<Ticket> spec = TicketSpecification.inCity(city);

        // Mock chain: root.join("createdFor").get("city")
        Join<Object, Object> createdForJoin = mock(Join.class);
        Path<Object> cityPath = mock(Path.class);

        when(root.join("createdFor")).thenReturn(createdForJoin);
        when(createdForJoin.get("city")).thenReturn(cityPath);

        when(cb.equal(cityPath, city)).thenReturn(predicate);

        // Act
        spec.toPredicate(root, query, cb);

        // Assert
        verify(cb).equal(cityPath, city);
    }

    @Test
    void testInCity_NullOrBlank() {
        // Arrange
        Specification<Ticket> spec = TicketSpecification.inCity("");
        when(cb.conjunction()).thenReturn(predicate);

        // Act
        spec.toPredicate(root, query, cb);

        // Assert
        verify(cb).conjunction();
        verify(root, never()).join(anyString());
    }

    // --- isAtSlaRisk Tests ---

    @Test
    void testIsAtSlaRisk() {
        // Arrange
        Specification<Ticket> spec = TicketSpecification.isAtSlaRisk();
        when(root.get("slaBreachAt")).thenReturn(path);
        when(cb.between(any(Expression.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(predicate);

        // Act
        spec.toPredicate(root, query, cb);

        // Assert
        // Verify we are checking the "slaBreachAt" field
        verify(root).get("slaBreachAt");
        // Verify we called between()
        verify(cb).between(eq(path), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // --- isSlaBreached Tests ---

    @Test
    void testIsSlaBreached() {
        // Arrange
        Specification<Ticket> spec = TicketSpecification.isSlaBreached();
        when(root.get("slaBreachAt")).thenReturn(path);
        when(cb.lessThan(any(Expression.class), any(LocalDateTime.class)))
                .thenReturn(predicate);

        // Act
        spec.toPredicate(root, query, cb);

        // Assert
        verify(root).get("slaBreachAt");
        verify(cb).lessThan(eq(path), any(LocalDateTime.class));
    }

    // Helper needed for assertEquals in simple cases
    private void assertEquals(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}