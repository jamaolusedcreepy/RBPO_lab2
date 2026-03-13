package com.support.repository;

import com.support.domain.Ticket;
import com.support.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserId(Long userId);
    List<Ticket> findByAssignedAgentId(Long agentId);
    List<Ticket> findByStatus(TicketStatus status);
    List<Ticket> findByCategoryId(Long categoryId);
    Optional<Ticket> findByTitleAndUserId(String title, Long userId);

    List<Ticket> findByAssignedAgentIdAndStatusIn(Long agentId, List<TicketStatus> statuses);

    long countByAssignedAgentIdAndStatusIn(Long agentId, List<TicketStatus> statuses);
    long countByCategoryId(Long categoryId);
    long countByCategoryIdAndStatus(Long categoryId, TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE " +
           "(t.responseDeadline IS NOT NULL AND t.responseDeadline < :now) OR " +
           "(t.resolutionDeadline IS NOT NULL AND t.resolutionDeadline < :now)")
    List<Ticket> findOverdueTickets(LocalDateTime now);
}
