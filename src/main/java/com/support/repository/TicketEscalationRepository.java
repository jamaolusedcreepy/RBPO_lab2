package com.support.repository;

import com.support.domain.TicketEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketEscalationRepository extends JpaRepository<TicketEscalation, Long> {
    List<TicketEscalation> findByTicketId(Long ticketId);
}