package com.support.controller;

import com.support.domain.Agent;
import com.support.domain.Ticket;
import com.support.dto.AgentWorkloadDto;
import com.support.dto.CategoryStatsDto;
import com.support.service.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BusinessController {

    @Autowired
    private BusinessService businessService;

    /**
     * Бизнес-операция 1: Автоназначение тикета на наименее загруженного активного агента.
     * POST /api/tickets/{id}/auto-assign
     */
    @PostMapping("/api/tickets/{id}/auto-assign")
    public ResponseEntity<?> autoAssignTicket(@PathVariable Long id) {
        try {
            Ticket ticket = businessService.autoAssignTicket(id);
            return ResponseEntity.ok(ticket);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Бизнес-операция 2: Эскалация тикета — смена категории с пересчётом SLA-дедлайнов.
     * PUT /api/tickets/{id}/escalate?categoryId={categoryId}
     */
    @PutMapping("/api/tickets/{id}/escalate")
    public ResponseEntity<?> escalateTicket(@PathVariable Long id, @RequestParam Long categoryId) {
        try {
            Ticket ticket = businessService.escalateTicket(id, categoryId);
            return ResponseEntity.ok(ticket);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Бизнес-операция 3: Деактивация агента с переназначением его тикетов на другого.
     * PUT /api/agents/{id}/deactivate?reassignToAgentId={reassignToAgentId}
     */
    @PutMapping("/api/agents/{id}/deactivate")
    public ResponseEntity<?> deactivateAgent(@PathVariable Long id, @RequestParam Long reassignToAgentId) {
        try {
            Agent agent = businessService.deactivateAgentWithReassignment(id, reassignToAgentId);
            return ResponseEntity.ok(agent);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Бизнес-операция 4: Статистика по категориям (тикеты по статусам + параметры SLA).
     * GET /api/reports/categories
     */
    @GetMapping("/api/reports/categories")
    public List<CategoryStatsDto> getCategoryStats() {
        return businessService.getCategoryStats();
    }

    /**
     * Отчёт по нагрузке агентов.
     * GET /api/reports/agents
     */
    @GetMapping("/api/reports/agents")
    public List<AgentWorkloadDto> getAgentWorkload() {
        return businessService.getAgentWorkload();
    }

    /**
     * Бизнес-операция 5: Повторное открытие закрытого/отменённого тикета с пересчётом дедлайнов SLA.
     * PUT /api/tickets/{id}/reopen?assignToAgentId={agentId} (agentId optional)
     */
    @PutMapping("/api/tickets/{id}/reopen")
    public ResponseEntity<?> reopenTicket(@PathVariable Long id,
                                          @RequestParam(required = false) Long assignToAgentId) {
        try {
            Ticket ticket = businessService.reopenTicket(id, assignToAgentId);
            return ResponseEntity.ok(ticket);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
