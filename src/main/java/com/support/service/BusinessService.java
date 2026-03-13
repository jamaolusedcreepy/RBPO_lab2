package com.support.service;

import com.support.domain.*;
import com.support.dto.AgentWorkloadDto;
import com.support.dto.CategoryStatsDto;
import com.support.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BusinessService {

    private static final List<TicketStatus> ACTIVE_STATUSES =
            List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD, TicketStatus.REOPENED);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Бизнес-операция 1: Автоматическое назначение тикета на наименее загруженного активного агента.
     * Затрагивает: Ticket + Agent. Выполняется в одной транзакции.
     */
    @Transactional
    public Ticket autoAssignTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        List<Agent> activeAgents = agentRepository.findByIsActiveTrue();
        if (activeAgents.isEmpty()) {
            throw new RuntimeException("No active agents available for assignment");
        }

        Agent leastLoaded = activeAgents.stream()
                .min(Comparator.comparingLong(agent ->
                        ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(), ACTIVE_STATUSES)))
                .orElseThrow(() -> new RuntimeException("Could not determine least loaded agent"));

        ticket.setAssignedAgent(leastLoaded);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    /**
     * Бизнес-операция 2: Эскалация тикета — смена категории и пересчёт дедлайнов SLA.
     * Затрагивает: Ticket + Category + SLA. Выполняется в одной транзакции.
     */
    @Transactional
    public Ticket escalateTicket(Long ticketId, Long newCategoryId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        Category newCategory = categoryRepository.findById(newCategoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + newCategoryId));

        if (ticket.getCategory().getId().equals(newCategoryId)) {
            throw new RuntimeException("Ticket already belongs to this category");
        }

        // setCategory внутри пересчитывает дедлайны через calculateDeadlines()
        ticket.setCategory(newCategory);
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    /**
     * Бизнес-операция 3: Деактивация агента с переназначением всех его активных тикетов на другого агента.
     * Затрагивает: Agent + Ticket. Выполняется в одной транзакции.
     */
    @Transactional
    public Agent deactivateAgentWithReassignment(Long agentId, Long reassignToAgentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));

        Agent reassignTarget = agentRepository.findById(reassignToAgentId)
                .orElseThrow(() -> new RuntimeException("Reassign target agent not found: " + reassignToAgentId));

        if (!reassignTarget.getIsActive()) {
            throw new RuntimeException("Cannot reassign to inactive agent: " + reassignToAgentId);
        }
        if (agentId.equals(reassignToAgentId)) {
            throw new RuntimeException("Cannot reassign tickets to the same agent");
        }

        List<Ticket> activeTickets = ticketRepository.findByAssignedAgentIdAndStatusIn(agentId, ACTIVE_STATUSES);
        for (Ticket t : activeTickets) {
            t.setAssignedAgent(reassignTarget);
            t.setUpdatedAt(LocalDateTime.now());
        }
        ticketRepository.saveAll(activeTickets);

        agent.setIsActive(false);
        return agentRepository.save(agent);
    }

    /**
     * Бизнес-операция 4: Статистика по категориям — количество тикетов по каждому статусу и параметры SLA.
     * Затрагивает: Category + Ticket + SLA.
     */
    @Transactional(readOnly = true)
    public List<CategoryStatsDto> getCategoryStats() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(cat -> {
            long total = ticketRepository.countByCategoryId(cat.getId());
            Map<TicketStatus, Long> byStatus = Arrays.stream(TicketStatus.values())
                    .collect(Collectors.toMap(
                            s -> s,
                            s -> ticketRepository.countByCategoryIdAndStatus(cat.getId(), s)
                    ));
            String slaName = cat.getSla() != null ? cat.getSla().getName() : null;
            Integer slaResp = cat.getSla() != null ? cat.getSla().getResponseTimeHours() : null;
            Integer slaResol = cat.getSla() != null ? cat.getSla().getResolutionTimeHours() : null;
            return new CategoryStatsDto(cat.getId(), cat.getName(), slaName, slaResp, slaResol, total, byStatus);
        }).collect(Collectors.toList());
    }

    /**
     * Бизнес-операция 5: Повторное открытие закрытого/отменённого тикета с пересчётом дедлайнов SLA.
     * Затрагивает: Ticket + Category + SLA (+ Agent при необходимости). Выполняется в одной транзакции.
     */
    @Transactional
    public Ticket reopenTicket(Long ticketId, Long assignToAgentId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.CANCELLED) {
            throw new RuntimeException("Only CLOSED or CANCELLED tickets can be reopened. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setClosedAt(null);

        // Пересчитываем дедлайны от текущего момента
        if (ticket.getCategory() != null && ticket.getCategory().getSla() != null) {
            SLA sla = ticket.getCategory().getSla();
            LocalDateTime now = LocalDateTime.now();
            ticket.setResponseDeadline(sla.calculateResponseDeadline(now));
            ticket.setResolutionDeadline(sla.calculateResolutionDeadline(now));
        }

        if (assignToAgentId != null) {
            Agent agent = agentRepository.findById(assignToAgentId)
                    .orElseThrow(() -> new RuntimeException("Agent not found: " + assignToAgentId));
            if (!agent.getIsActive()) {
                throw new RuntimeException("Cannot assign to inactive agent: " + assignToAgentId);
            }
            ticket.setAssignedAgent(agent);
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    /**
     * Дополнительный отчёт: нагрузка на агентов (используется в /api/reports/agents).
     * Затрагивает: Agent + Ticket.
     */
    @Transactional(readOnly = true)
    public List<AgentWorkloadDto> getAgentWorkload() {
        List<Agent> agents = agentRepository.findAll();
        return agents.stream().map(agent -> {
            long total = ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(),
                    Arrays.asList(TicketStatus.values()));
            long active = ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(), ACTIVE_STATUSES);
            long resolved = ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(),
                    List.of(TicketStatus.RESOLVED));
            long closed = ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(),
                    List.of(TicketStatus.CLOSED));
            return new AgentWorkloadDto(agent.getId(), agent.getName(), agent.getEmail(),
                    agent.getIsActive(), total, active, resolved, closed);
        }).collect(Collectors.toList());
    }
}
