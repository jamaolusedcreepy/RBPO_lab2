package com.support.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_escalations")
public class TicketEscalation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "escalated_from_agent_id")
    private Long escalatedFromAgentId;

    @Column(name = "escalated_to_agent_id")
    private Long escalatedToAgentId;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }  // ЭТОТ СЕТТЕР ОБЯЗАТЕЛЕН!

    public Long getEscalatedFromAgentId() { return escalatedFromAgentId; }
    public void setEscalatedFromAgentId(Long escalatedFromAgentId) { this.escalatedFromAgentId = escalatedFromAgentId; }

    public Long getEscalatedToAgentId() { return escalatedToAgentId; }
    public void setEscalatedToAgentId(Long escalatedToAgentId) { this.escalatedToAgentId = escalatedToAgentId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}