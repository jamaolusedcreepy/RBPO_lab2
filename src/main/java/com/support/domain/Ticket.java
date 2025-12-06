package com.support.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private Agent assignedAgent;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "solution")
    private String solution;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "resolution_deadline")
    private LocalDateTime resolutionDeadline;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Конструкторы
    public Ticket() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Ticket(String title, String description, User user, Category category) {
        this();
        this.title = title;
        this.description = description;
        this.user = user;
        this.category = category;
        calculateDeadlines();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Agent getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(Agent assignedAgent) {
        this.assignedAgent = assignedAgent;
        this.updatedAt = LocalDateTime.now();
    }

    public Category getCategory() { return category; }
    public void setCategory(Category category) {
        this.category = category;
        calculateDeadlines();
    }

    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(LocalDateTime responseDeadline) { this.responseDeadline = responseDeadline; }

    public LocalDateTime getResolutionDeadline() { return resolutionDeadline; }
    public void setResolutionDeadline(LocalDateTime resolutionDeadline) { this.resolutionDeadline = resolutionDeadline; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    // Методы бизнес-логики
    private void calculateDeadlines() {
        if (category != null && category.getSla() != null) {
            SLA sla = category.getSla();
            this.responseDeadline = sla.calculateResponseDeadline(createdAt);
            this.resolutionDeadline = sla.calculateResolutionDeadline(createdAt);
        }
    }

    public boolean canTransitionTo(TicketStatus newStatus) {
        List<TicketStatus> allowedTransitions = getAllowedTransitions(this.status);
        return allowedTransitions.contains(newStatus);
    }

    private List<TicketStatus> getAllowedTransitions(TicketStatus currentStatus) {
        switch (currentStatus) {
            case OPEN:
                return Arrays.asList(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
            case IN_PROGRESS:
                return Arrays.asList(TicketStatus.RESOLVED, TicketStatus.CANCELLED, TicketStatus.ON_HOLD);
            case ON_HOLD:
                return Arrays.asList(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
            case RESOLVED:
                return Arrays.asList(TicketStatus.CLOSED, TicketStatus.REOPENED);
            case REOPENED:
                return Arrays.asList(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
            case CANCELLED:
                return Arrays.asList(TicketStatus.REOPENED);
            case CLOSED:
                return Arrays.asList();
            default:
                return Arrays.asList();
        }
    }

    public boolean close(String solution) {
        if (this.status != TicketStatus.RESOLVED) {
            return false;
        }
        if (solution == null || solution.trim().isEmpty()) {
            return false;
        }
        this.solution = solution;
        this.status = TicketStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    public boolean isOverdue() {
        LocalDateTime now = LocalDateTime.now();
        return (responseDeadline != null && now.isAfter(responseDeadline)) ||
                (resolutionDeadline != null && now.isAfter(resolutionDeadline));
    }
}
