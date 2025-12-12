package com.support.repository;

import com.support.domain.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {
    List<TicketStatusHistory> findByTicketId(Long ticketId);
}