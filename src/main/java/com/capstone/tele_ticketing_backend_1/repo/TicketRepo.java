package com.capstone.tele_ticketing_backend_1.repo;

import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.Ticket;
import com.capstone.tele_ticketing_backend_1.entities.TicketStatus;
import com.capstone.tele_ticketing_backend_1.projections.TicketVolumeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepo extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findAllByCreatedForAndStatusIn(AppUser user, List<TicketStatus> statuses);

    // Dr. X's Note: This finds all tickets FOR a user with the specific status 'FIXED'.
    // This will serve the "my feedback tickets" endpoint.
    List<Ticket> findAllByCreatedForAndStatus(AppUser user, TicketStatus status);

    // In: com/capstone/tele_ticketing_backend_1/repo/TicketRepo.java

// ... existing methods ...

    // Dr. X's Note: This query finds all tickets CREATED BY a specific user (our agent)
// that have one of the given statuses.
    List<Ticket> findAllByCreatedByAndStatusIn(AppUser user, List<TicketStatus> statuses);

    List<Ticket> findAllByStatusIn(List<TicketStatus> statuses);
    List<Ticket> findAllByAssignedToContainsAndStatusIn(AppUser user, List<TicketStatus> statuses);

    long countByCreatedForAndStatus(AppUser user, TicketStatus status);

    // Efficiently counts tickets for a user with any of the given statuses.
    long countByCreatedForAndStatusIn(AppUser user, List<TicketStatus> statuses);

    long countByCreatedByAndStatus(AppUser user, TicketStatus status);

    // Efficiently counts tickets CREATED BY a user with any of the given statuses.
    long countByCreatedByAndStatusIn(AppUser user, List<TicketStatus> statuses);

    // Finds all tickets CREATED BY a user with a specific status.
    List<Ticket> findAllByCreatedByAndStatus(AppUser user, TicketStatus status);

    @Query("SELECT DISTINCT t FROM Ticket t JOIN t.assignedTo a WHERE a.team.id = :teamId AND t.status IN :statuses")
    List<Ticket> findTicketsByTeamAndStatus(@Param("teamId") Long teamId, @Param("statuses") List<TicketStatus> statuses);


    @Query("SELECT DISTINCT t FROM Ticket t JOIN t.assignedTo a WHERE a.team.id = :teamId AND t.status NOT IN ('RESOLVED', 'FIXED') AND t.slaBreachAt BETWEEN :now AND :slaRiskThreshold")
    List<Ticket> findSlaRiskTicketsByTeam(@Param("teamId") Long teamId, @Param("now") LocalDateTime now, @Param("slaRiskThreshold") LocalDateTime slaRiskThreshold);

    @Query("SELECT FUNCTION('DATE', t.createdAt) as date, COUNT(t) as count " +
            "FROM Ticket t " +
            "WHERE t.createdAt >= :startDate " +
            "GROUP BY FUNCTION('DATE', t.createdAt) " +
            "ORDER BY date ASC")
    List<TicketVolumeProjection> getTicketVolumeByDay(@Param("startDate") LocalDateTime startDate);

    // Dr. X's Fix: We extract the epoch from each timestamp first, then subtract.
    @Query("SELECT AVG((EXTRACT(EPOCH FROM t.resolvedAt) - EXTRACT(EPOCH FROM t.createdAt)) / 3600.0) " +
            "FROM Ticket t " +
            "WHERE t.resolvedAt IS NOT NULL AND t.createdAt >= :startDate")
    Double getAverageResolutionTimeInHours(@Param("startDate") LocalDateTime startDate);

    Optional<Ticket> findByTicketUid(String ticketUid);
}