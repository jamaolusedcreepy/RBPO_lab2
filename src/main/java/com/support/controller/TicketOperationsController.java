package com.support.controller;

import com.support.domain.Ticket;
import com.support.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tickets/operations")
public class TicketOperationsController {

    @Autowired
    private TicketService ticketService;

    // 1. Автоназначение на наименее загруженного агента
    @PostMapping("/{ticketId}/auto-assign")
    public ResponseEntity<Ticket> autoAssignTicket(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.assignTicketToLeastBusyAgent(ticketId);
        return ResponseEntity.ok(ticket);
    }

    // 2. Эскалация просроченного тикета
    @PostMapping("/{ticketId}/escalate")
    public ResponseEntity<Ticket> escalateTicket(
            @PathVariable Long ticketId,
            @RequestParam(required = false) String reason) {
        Ticket ticket = ticketService.escalateOverdueTicket(ticketId, reason);
        return ResponseEntity.ok(ticket);
    }

    // 3. Автозакрытие старых решенных тикетов
    @PostMapping("/auto-close")
    public ResponseEntity<String> autoCloseResolvedTickets(
            @RequestParam(defaultValue = "7") int daysThreshold) {
        int closedCount = ticketService.autoCloseResolvedTickets(daysThreshold);
        return ResponseEntity.ok("Auto-closed " + closedCount + " tickets");
    }

    // 4. Статистика по агенту
    @GetMapping("/agent/{agentId}/stats")
    public ResponseEntity<?> getAgentStats(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        TicketService.AgentStats stats = ticketService.getAgentStats(agentId, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // 5. Массовое обновление категории
    @PostMapping("/bulk-update-category")
    public ResponseEntity<String> bulkUpdateCategory(
            @RequestParam Long userId,
            @RequestParam Long oldCategoryId,
            @RequestParam Long newCategoryId) {
        int updatedCount = ticketService.bulkUpdateUserTicketsCategory(userId, oldCategoryId, newCategoryId);
        return ResponseEntity.ok("Updated " + updatedCount + " tickets");
    }
}