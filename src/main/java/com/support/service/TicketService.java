package com.support.service;

import com.support.domain.*;
import com.support.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private TicketStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private TicketEscalationRepository escalationRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // 1. ОПЕРАЦИЯ: Назначить агента на тикет с проверкой загруженности
    @Transactional
    public Ticket assignTicketToLeastBusyAgent(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getAssignedAgent() != null) {
            throw new RuntimeException("Ticket already assigned");
        }

        // Находим наименее загруженного активного агента
        List<Agent> activeAgents = agentRepository.findByIsActiveTrue();
        Agent leastBusyAgent = null;
        long minTickets = Long.MAX_VALUE;

        for (Agent agent : activeAgents) {
            long ticketCount = ticketRepository.countByAssignedAgentIdAndStatusNot(
                    agent.getId(), TicketStatus.CLOSED);
            if (ticketCount < minTickets) {
                minTickets = ticketCount;
                leastBusyAgent = agent;
            }
        }

        if (leastBusyAgent == null) {
            throw new RuntimeException("No active agents available");
        }

        ticket.setAssignedAgent(leastBusyAgent);
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        // Сохраняем историю
        saveStatusHistory(ticket, null, "System auto-assignment");

        return ticketRepository.save(ticket);
    }

    // 2. ОПЕРАЦИЯ: Эскалировать просроченный тикет
    @Transactional
    public Ticket escalateOverdueTicket(Long ticketId, String reason) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.isOverdue()) {
            throw new RuntimeException("Ticket is not overdue");
        }

        if (ticket.getAssignedAgent() == null) {
            throw new RuntimeException("Ticket not assigned to any agent");
        }

        Agent currentAgent = ticket.getAssignedAgent();

        // Находим другого активного агента
        List<Agent> otherAgents = agentRepository.findByIsActiveTrueAndIdNot(currentAgent.getId());
        if (otherAgents.isEmpty()) {
            throw new RuntimeException("No other agents available for escalation");
        }

        Agent newAgent = otherAgents.get(0);
        Agent previousAgent = ticket.getAssignedAgent();
        ticket.setAssignedAgent(newAgent);

        // Создаем запись об эскалации
        TicketEscalation escalation = new TicketEscalation();
        escalation.setTicket(ticket);
        escalation.setEscalatedFromAgentId(previousAgent.getId());
        escalation.setEscalatedToAgentId(newAgent.getId());
        escalation.setReason(reason != null ? reason : "Automatic escalation due to overdue");
        escalationRepository.save(escalation);

        // Сохраняем историю
        saveStatusHistory(ticket, "ESCALATED", "Ticket escalated due to overdue");

        return ticketRepository.save(ticket);
    }

    // 3. ОПЕРАЦИЯ: Автоматическое закрытие старых решенных тикетов
    @Transactional
    public int autoCloseResolvedTickets(int daysThreshold) {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(daysThreshold);
        List<Ticket> resolvedTickets = ticketRepository.findResolvedBefore(thresholdDate);

        int closedCount = 0;
        for (Ticket ticket : resolvedTickets) {
            ticket.setStatus(TicketStatus.CLOSED);
            ticket.setClosedAt(LocalDateTime.now());
            ticket.setSolution("Автоматически закрыто по истечении " + daysThreshold + " дней");
            saveStatusHistory(ticket, "AUTO_CLOSED", "Auto-closed after " + daysThreshold + " days in RESOLVED");
            ticketRepository.save(ticket);
            closedCount++;
        }

        return closedCount;
    }

    // 4. ОПЕРАЦИЯ: Сводка по работе агента
    @Transactional
    public AgentStats getAgentStats(Long agentId, LocalDateTime startDate, LocalDateTime endDate) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        AgentStats stats = new AgentStats();
        stats.setAgentName(agent.getName());

        List<Ticket> agentTickets = ticketRepository.findByAssignedAgentIdAndCreatedAtBetween(
                agentId, startDate, endDate);

        long total = agentTickets.size();
        long closed = agentTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.CLOSED)
                .count();
        long overdue = agentTickets.stream()
                .filter(Ticket::isOverdue)
                .count();

        stats.setTotalTickets(total);
        stats.setClosedTickets(closed);
        stats.setOverdueTickets(overdue);
        stats.setAvgResolutionTime(calculateAverageResolutionTime(agentTickets));

        return stats;
    }

    // 5. ОПЕРАЦИЯ: Массовое обновление категории для тикетов пользователя
    @Transactional
    public int bulkUpdateUserTicketsCategory(Long userId, Long oldCategoryId, Long newCategoryId) {
        List<Ticket> userTickets = ticketRepository.findByUserIdAndCategoryId(userId, oldCategoryId);

        int updatedCount = 0;
        for (Ticket ticket : userTickets) {
            if (ticket.getStatus() != TicketStatus.CLOSED) {
                ticket.setCategory(categoryRepository.findById(newCategoryId)
                        .orElseThrow(() -> new RuntimeException("New category not found")));
                ticketRepository.save(ticket);
                updatedCount++;

                saveStatusHistory(ticket, "CATEGORY_CHANGED",
                        "Category changed from " + oldCategoryId + " to " + newCategoryId);
            }
        }

        return updatedCount;
    }

    private void saveStatusHistory(Ticket ticket, String changedBy, String reason) {
        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicket(ticket);
        history.setOldStatus(ticket.getStatus().name());
        history.setNewStatus(ticket.getStatus().name());
        history.setChangedBy(changedBy != null ? changedBy : "System");
        history.setChangeReason(reason);
        statusHistoryRepository.save(history);
    }

    private double calculateAverageResolutionTime(List<Ticket> tickets) {
        return tickets.stream()
                .filter(t -> t.getClosedAt() != null && t.getCreatedAt() != null)
                .mapToLong(t -> java.time.Duration.between(t.getCreatedAt(), t.getClosedAt()).toHours())
                .average()
                .orElse(0.0);
    }

    // Вспомогательный класс для статистики
    public static class AgentStats {
        private String agentName;
        private long totalTickets;
        private long closedTickets;
        private long overdueTickets;
        private double avgResolutionTime;

        // Геттеры и сеттеры
        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }

        public long getTotalTickets() { return totalTickets; }
        public void setTotalTickets(long totalTickets) { this.totalTickets = totalTickets; }

        public long getClosedTickets() { return closedTickets; }
        public void setClosedTickets(long closedTickets) { this.closedTickets = closedTickets; }

        public long getOverdueTickets() { return overdueTickets; }
        public void setOverdueTickets(long overdueTickets) { this.overdueTickets = overdueTickets; }

        public double getAvgResolutionTime() { return avgResolutionTime; }
        public void setAvgResolutionTime(double avgResolutionTime) { this.avgResolutionTime = avgResolutionTime; }
    }
}